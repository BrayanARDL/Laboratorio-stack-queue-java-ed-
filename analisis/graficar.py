"""
Análisis y graficación de los tiempos medidos por benchmark.Benchmark.

Este script se ejecuta DESPUÉS del benchmark y de forma totalmente separada:
lee el CSV de tiempos crudos (ns) y produce
  - resultados/resumen.csv          promedio por estructura / método / tamaño (en µs)
  - informe/figuras/*.pdf / *.png   gráficas tiempo vs. tamaño (escala log-log)
  - informe/tablas/*.tex            tablas para el informe en LaTeX

Así la graficación nunca interfiere con la medición de tiempos.

Uso:  python analisis/graficar.py [resultados/tiempos.csv]
"""
import sys
from pathlib import Path

import numpy as np
import pandas as pd
import matplotlib

matplotlib.use("Agg")
import matplotlib.pyplot as plt
from matplotlib.ticker import LogLocator, NullFormatter

RAIZ = Path(__file__).resolve().parent.parent
CSV = Path(sys.argv[1]) if len(sys.argv) > 1 else RAIZ / "resultados" / "tiempos.csv"
FIG = RAIZ / "informe" / "figuras"
TAB = RAIZ / "informe" / "tablas"
FIG.mkdir(parents=True, exist_ok=True)
TAB.mkdir(parents=True, exist_ok=True)

# ----------------------------------------------------------------------
# Estilo: paleta categórica validada (orden fijo, nunca se recicla),
# líneas de 2 px, marcadores >= 8 px, rejilla y ejes discretos.
# ----------------------------------------------------------------------
PALETA = ["#2a78d6", "#eb6834", "#1baf7a", "#eda100",
          "#e87ba4", "#008300", "#4a3aa7", "#e34948"]
MARCADORES = ["o", "s", "^", "D", "v", "P", "X", "*"]   # codificación secundaria (no solo color)
TXT1, TXT2, REJILLA, FONDO = "#0b0b0b", "#52514e", "#e4e3df", "#ffffff"

plt.rcParams.update({
    "font.family": "DejaVu Sans",
    "font.size": 9,
    "axes.edgecolor": "#b5b4ae",
    "axes.labelcolor": TXT2,
    "axes.titlecolor": TXT1,
    "axes.titlesize": 10,
    "axes.titleweight": "bold",
    "axes.facecolor": FONDO,
    "figure.facecolor": FONDO,
    "xtick.color": TXT2,
    "ytick.color": TXT2,
    "legend.frameon": False,
    "legend.fontsize": 8,
    "lines.linewidth": 1.6,
    "lines.markersize": 5.5,
    "savefig.dpi": 200,
    "savefig.bbox": "tight",
})

LISTAS = ["SLL sin cola", "SLL con cola", "DLL sin cola", "DLL con cola"]
NOMBRE_LARGO = {
    "SLL sin cola": "Simplemente enlazada sin cola",
    "SLL con cola": "Simplemente enlazada con cola",
    "DLL sin cola": "Doblemente enlazada sin cola",
    "DLL con cola": "Doblemente enlazada con cola",
    "ArrayStack": "MyStack (arreglo circular)",
    "ArrayQueue": "MyQueue (arreglo circular)",
}
METODOS_LISTA = ["PushFront", "PushBack", "PopFront", "PopBack",
                 "Find", "Erase", "AddBefore", "AddAfter"]
METODOS_LISTA_EXTRA = ["TopFront", "TopBack", "Empty", "Size"]
METODOS_PILA = ["Push", "Pop", "Peek", "IsEmpty", "Size", "Delete"]
METODOS_COLA = ["Enqueue", "Dequeue", "Front", "IsEmpty", "Size", "Delete"]

# Complejidad teórica (peor caso) usada para comparar con lo medido
TEORIA = {
    "SLL sin cola": dict(PushFront="1", PushBack="n", PopFront="1", PopBack="n", Find="n", Erase="n",
                         AddBefore="n", AddAfter="1", TopFront="1", TopBack="n", Empty="1", Size="1"),
    "SLL con cola": dict(PushFront="1", PushBack="1", PopFront="1", PopBack="n", Find="n", Erase="n",
                         AddBefore="n", AddAfter="1", TopFront="1", TopBack="1", Empty="1", Size="1"),
    "DLL sin cola": dict(PushFront="1", PushBack="n", PopFront="1", PopBack="n", Find="n", Erase="n",
                         AddBefore="1", AddAfter="1", TopFront="1", TopBack="n", Empty="1", Size="1"),
    "DLL con cola": dict(PushFront="1", PushBack="1", PopFront="1", PopBack="1", Find="n", Erase="n",
                         AddBefore="1", AddAfter="1", TopFront="1", TopBack="1", Empty="1", Size="1"),
    "ArrayStack": dict(Push="1*", Pop="1", Peek="1", IsEmpty="1", Size="1", Delete="n"),
    "ArrayQueue": dict(Enqueue="1*", Dequeue="1", Front="1", IsEmpty="1", Size="1", Delete="n"),
}


# ----------------------------------------------------------------------
# Carga y resumen
# ----------------------------------------------------------------------
def promedio_recortado(x, frac=0.1):
    """Promedio descartando el 10 % más alto y el 10 % más bajo (atenúa pausas del GC)."""
    x = np.sort(np.asarray(x, dtype=float))
    k = int(len(x) * frac)
    return x[k:len(x) - k].mean() if len(x) > 2 * k else x.mean()


datos = pd.read_csv(CSV)
calib = datos[datos.parte == "calibracion"].ns
OVERHEAD_NS = float(np.median(calib)) if len(calib) else float("nan")
datos = datos[datos.parte != "calibracion"]

resumen = (datos.groupby(["parte", "estructura", "metodo", "n"]).ns
           .agg(promedio_ns=promedio_recortado, mediana_ns="median", minimo_ns="min",
                maximo_ns="max", repeticiones="count")
           .reset_index())
resumen["promedio_us"] = resumen.promedio_ns / 1000.0
resumen.to_csv(RAIZ / "resultados" / "resumen.csv", index=False, float_format="%.4f")
TAMANOS = sorted(resumen.n.unique())


def serie(estructura, metodo, parte=None):
    r = resumen[(resumen.estructura == estructura) & (resumen.metodo == metodo)]
    if parte:
        r = r[r.parte == parte]
    r = r.sort_values("n")
    return r.n.values, r.promedio_us.values


def razon(estructura, metodo, parte=None):
    """
    Razón t(n_max) / t(n_max/10) entre los dos tamaños más grandes.
    Si n se multiplica por 10: O(1) => razón ~1, O(n) => ~10, O(n^2) => ~100.
    Se usa la última década porque ahí los efectos constantes (caché, overhead
    de nanoTime) ya se saturaron y domina el comportamiento asintótico.
    """
    n, t = serie(estructura, metodo, parte)
    if len(t) < 2:
        return float("nan")
    return float(t[-1] / t[-2])


def pendiente(estructura, metodo, parte=None):
    """Pendiente log-log en la última década (= log10 de la razón)."""
    r = razon(estructura, metodo, parte)
    return float(np.log10(r)) if r > 0 else float("nan")


def orden_empirico(p):
    if np.isnan(p):
        return "--"
    if p < 0.5:
        return "O(1)"
    if p < 1.5:
        return "O(n)"
    return "O(n^2)"


# ----------------------------------------------------------------------
# Utilidades de graficación
# ----------------------------------------------------------------------
def ejes_log(ax, titulo=None, ylabel=True):
    ax.set_xscale("log")
    ax.set_yscale("log")
    ax.set_xticks(TAMANOS)
    ax.set_xticklabels([f"$10^{{{int(round(np.log10(v)))}}}$" for v in TAMANOS])
    ax.xaxis.set_minor_formatter(NullFormatter())
    ax.yaxis.set_major_locator(LogLocator(base=10))
    ax.yaxis.set_minor_formatter(NullFormatter())
    # Rango vertical mínimo de 3 décadas: evita que el ruido de pocos ns en un
    # método O(1) parezca una variación grande al autoescalar.
    y0, y1 = ax.get_ylim()
    if np.log10(y1 / y0) < 3:
        c = np.sqrt(y0 * y1)
        ax.set_ylim(c / 10 ** 1.5, c * 10 ** 1.5)
    ax.grid(True, which="major", color=REJILLA, linewidth=0.7)
    ax.grid(False, which="minor")
    for s in ("top", "right"):
        ax.spines[s].set_visible(False)
    ax.set_xlabel("Tamaño de la estructura n")
    if ylabel:
        ax.set_ylabel("Tiempo promedio (µs, escala log)")
    if titulo:
        ax.set_title(titulo, loc="left")


def linea(ax, x, y, i, etiqueta):
    ax.plot(x, y, color=PALETA[i], marker=MARCADORES[i], label=etiqueta,
            markeredgecolor=FONDO, markeredgewidth=0.8, zorder=3)


def guardar(fig, nombre):
    fig.savefig(FIG / f"{nombre}.pdf")
    fig.savefig(FIG / f"{nombre}.png")
    plt.close(fig)


# ----------------------------------------------------------------------
# PARTE 1: listas
# ----------------------------------------------------------------------
# (a) Una gráfica por implementación con sus 8 métodos (como el ejemplo del enunciado)
for est in LISTAS:
    fig, ax = plt.subplots(figsize=(7.2, 4.2))
    for i, met in enumerate(METODOS_LISTA):
        x, y = serie(est, met, "lista")
        linea(ax, x, y, i, met)
    ejes_log(ax, NOMBRE_LARGO[est])
    ax.legend(ncol=4, loc="upper center", bbox_to_anchor=(0.5, -0.16))
    guardar(fig, f"lista_{est.replace(' ', '_')}")

# (b) Un panel por método comparando las 4 implementaciones
fig, axes = plt.subplots(3, 4, figsize=(11, 8.2), sharex=True)
for ax, met in zip(axes.flat, METODOS_LISTA + METODOS_LISTA_EXTRA):
    for i, est in enumerate(LISTAS):
        x, y = serie(est, met, "lista")
        linea(ax, x, y, i, est)
    ejes_log(ax, met, ylabel=False)
    ax.set_xlabel("")
    ax.tick_params(labelsize=7)
for ax in axes[:, 0]:
    ax.set_ylabel("µs (log)")
for ax in axes[-1, :]:
    ax.set_xlabel("n")
h, l = axes[0, 0].get_legend_handles_labels()
fig.legend(h, l, ncol=4, loc="lower center", bbox_to_anchor=(0.5, -0.01))
fig.tight_layout(rect=(0, 0.04, 1, 1))
guardar(fig, "lista_por_metodo")

# ----------------------------------------------------------------------
# PARTE 2: pila y cola
# ----------------------------------------------------------------------
for est, metodos in (("ArrayStack", METODOS_PILA), ("ArrayQueue", METODOS_COLA)):
    fig, ax = plt.subplots(figsize=(7.2, 4.0))
    for i, met in enumerate(metodos):
        x, y = serie(est, met, "pilacola")
        linea(ax, x, y, i, met)
    ejes_log(ax, NOMBRE_LARGO[est])
    ax.legend(ncol=6, loc="upper center", bbox_to_anchor=(0.5, -0.16))
    guardar(fig, f"pilacola_{est}")

# Crecimiento: costo amortizado por inserción y comparación de estrategias
fig, (a1, a2) = plt.subplots(1, 2, figsize=(11, 4.0))
cre = resumen[resumen.parte == "crecimiento"]
curvas = [("Duplicar (x2)", "Push total", "Push, duplicando (x2)"),
          ("Duplicar (x2)", "Enqueue total", "Enqueue, duplicando (x2)"),
          ("Incremento fijo (+1000)", "Push total", "Push, incremento fijo (+1000)")]
for i, (est, met, etq) in enumerate(curvas):
    r = cre[(cre.estructura == est) & (cre.metodo == met)].sort_values("n")
    linea(a1, r.n.values, r.promedio_ns.values / r.n.values, i, etq)
ejes_log(a1, "Costo por inserción (tiempo total / n)", ylabel=False)
a1.set_ylabel("ns por inserción (log)")
a1.legend(loc="upper right")
rp = resumen[(resumen.estructura == "ArrayStack") & (resumen.metodo == "Push")].sort_values("n")
linea(a2, rp.n.values, rp.promedio_us.values, 0, "Push típico (hay espacio libre)")
rr = cre[(cre.estructura == "Duplicar (x2)") & (cre.metodo == "Push con redimension")].sort_values("n")
linea(a2, rr.n.values, rr.promedio_us.values, 1, "Push con el arreglo lleno (copia)")
ejes_log(a2, "Push individual: caso típico vs. redimensionamiento")
a2.legend(loc="upper left")
fig.tight_layout()
guardar(fig, "crecimiento")

# ----------------------------------------------------------------------
# PARTE 3: métodos equivalentes Lista vs Pila/Cola
# Para cada par se elige la implementación de lista con la mejor complejidad
# teórica y, entre las empatadas, la de menor tiempo medido (suma sobre n).
# ----------------------------------------------------------------------
PARES = [  # (método lista, estructura arreglo, método arreglo)
    ("PushFront", "ArrayStack", "Push"),
    ("PopFront", "ArrayStack", "Pop"),
    ("TopFront", "ArrayStack", "Peek"),
    ("PushBack", "ArrayQueue", "Enqueue"),
    ("PopFront", "ArrayQueue", "Dequeue"),
    ("TopFront", "ArrayQueue", "Front"),
    ("Erase", "ArrayStack", "Delete"),
    ("Erase", "ArrayQueue", "Delete"),
    ("Empty", "ArrayStack", "IsEmpty"),
    ("Size", "ArrayStack", "Size"),
]
RANGO = {"1": 0, "1*": 0, "n": 1}
# Desempate entre implementaciones con la misma Big-O: preferir la de nodo más
# liviano y menos referencias que mantener (simple antes que doble, sin cola
# antes que con cola). Las diferencias medidas entre empatadas son de pocos ns (ruido).
PREFERENCIA = ["SLL sin cola", "SLL con cola", "DLL sin cola", "DLL con cola"]


def mejor_lista(met):
    mejor_orden = min(RANGO[TEORIA[e][met]] for e in LISTAS)
    candidatas = [e for e in PREFERENCIA if RANGO[TEORIA[e][met]] == mejor_orden]
    return candidatas[0], candidatas


eleccion = {}
fig, axes = plt.subplots(2, 5, figsize=(13, 5.8), sharex=True)
for ax, (ml, ea, ma) in zip(axes.flat, PARES):
    est, cand = mejor_lista(ml)
    eleccion[(ml, ea, ma)] = (est, cand)
    x, y = serie(est, ml, "lista")
    linea(ax, x, y, 0, f"Lista: {est}")
    x, y = serie(ea, ma, "pilacola")
    linea(ax, x, y, 1, f"{ea}")
    ejes_log(ax, f"{ml} vs {ma}" + ((" (pila)" if ea == "ArrayStack" else " (cola)") if ma == "Delete" else ""), ylabel=False)
    ax.title.set_fontsize(8.5)
    ax.set_xlabel("")
    ax.tick_params(labelsize=7)
    ax.legend(loc="upper left", fontsize=6.5)
for ax in axes[:, 0]:
    ax.set_ylabel("µs (log)")
for ax in axes[-1, :]:
    ax.set_xlabel("n")
fig.tight_layout()
guardar(fig, "comparativa_equivalentes")

# ----------------------------------------------------------------------
# Tablas LaTeX (coma decimal, espacio fino como separador de miles)
# ----------------------------------------------------------------------
def fmt(v):
    if v >= 1000:
        return f"{v:,.0f}".replace(",", "\\,")
    if v >= 10:
        return f"{v:.1f}".replace(".", "{,}")
    return f"{v:.3f}".replace(".", "{,}")


def exp_n(n):
    return f"$10^{{{int(round(np.log10(n)))}}}$"


def big_o(s):
    return {"1": "$O(1)$", "1*": "$O(1)^{*}$", "n": "$O(n)$"}[s]


def tabla_tiempos(est, metodos, parte, archivo):
    cols = "r" * len(metodos)
    lin = [f"\\begin{{tabular}}{{c{cols}}}", "\\toprule",
           "$n$ & " + " & ".join(f"\\textbf{{{m}}}" for m in metodos) + " \\\\", "\\midrule"]
    for n in TAMANOS:
        fila = []
        for m in metodos:
            r = resumen[(resumen.estructura == est) & (resumen.metodo == m) &
                        (resumen.parte == parte) & (resumen.n == n)]
            fila.append(fmt(r.promedio_us.iloc[0]) if len(r) else "--")
        lin.append(exp_n(n) + " & " + " & ".join(fila) + " \\\\")
    lin += ["\\midrule",
            "Teórico & " + " & ".join(big_o(TEORIA[est][m]) for m in metodos) + " \\\\",
            "Razón $\\times 10$ & " + " & ".join(
                f"$\\times${razon(est, m, parte):.1f}".replace(".", "{,}") for m in metodos) + " \\\\",
            "Empírico & " + " & ".join(
                "$" + orden_empirico(pendiente(est, m, parte)).replace("^2", "^{2}") + "$"
                for m in metodos) + " \\\\",
            "\\bottomrule", "\\end{tabular}"]
    (TAB / archivo).write_text("\n".join(lin), encoding="utf-8")


for est in LISTAS:
    tabla_tiempos(est, METODOS_LISTA, "lista", f"lista_{est.replace(' ', '_')}.tex")
    tabla_tiempos(est, METODOS_LISTA_EXTRA, "lista", f"lista_{est.replace(' ', '_')}_extra.tex")
tabla_tiempos("ArrayStack", METODOS_PILA, "pilacola", "pila.tex")
tabla_tiempos("ArrayQueue", METODOS_COLA, "pilacola", "cola.tex")

# Tabla de crecimiento
lin = ["\\begin{tabular}{crrrr}", "\\toprule",
       "$n$ & \\textbf{x2: total} & \\textbf{x2: ns/push} & \\textbf{+1000: total} & "
       "\\textbf{+1000: ns/push} \\\\", "\\midrule"]
for n in TAMANOS:
    fila = [exp_n(n)]
    for est in ("Duplicar (x2)", "Incremento fijo (+1000)"):
        r = cre[(cre.estructura == est) & (cre.metodo == "Push total") & (cre.n == n)]
        if len(r):
            fila += [fmt(r.promedio_us.iloc[0]) + " µs", f"{r.promedio_ns.iloc[0] / n:.1f}".replace(".", "{,}")]
        else:
            fila += ["--", "--"]
    lin.append(" & ".join(fila) + " \\\\")
lin += ["\\bottomrule", "\\end{tabular}"]
(TAB / "crecimiento.tex").write_text("\n".join(lin), encoding="utf-8")

# Tabla de la comparativa de equivalentes (último tamaño medido)
nmax = TAMANOS[-1]
lin = ["\\begin{tabular}{llllrr}", "\\toprule",
       "\\textbf{Lista} & \\textbf{Implementación elegida} & \\textbf{Arreglo circular} & "
       "\\textbf{Big-O (L / A)} & \\textbf{Lista (µs)} & \\textbf{Arreglo (µs)} \\\\", "\\midrule"]
for (ml, ea, ma), (est, cand) in eleccion.items():
    tl = resumen[(resumen.estructura == est) & (resumen.metodo == ml) & (resumen.n == nmax)].promedio_us
    ta = resumen[(resumen.estructura == ea) & (resumen.metodo == ma) & (resumen.n == nmax)].promedio_us
    lin.append(f"{ml} & {est} & {ea}.{ma} & {big_o(TEORIA[est][ml])} / {big_o(TEORIA[ea][ma])} & "
               f"{fmt(tl.iloc[0])} & {fmt(ta.iloc[0])} \\\\")
lin += ["\\bottomrule", "\\end{tabular}"]
(TAB / "equivalentes.tex").write_text("\n".join(lin), encoding="utf-8")

# ----------------------------------------------------------------------
# Resumen en consola (útil para redactar el análisis)
# ----------------------------------------------------------------------
print(f"Overhead de medición (nanoTime vacío, mediana): {OVERHEAD_NS:.0f} ns")
print(f"Tamaños: {[int(v) for v in TAMANOS]}")
print("\nPendientes log-log en la última década (log10 de t(nmax)/t(nmax/10)):")
for est in LISTAS + ["ArrayStack", "ArrayQueue"]:
    mets = TEORIA[est].keys()
    parte = "lista" if est in LISTAS else "pilacola"
    txt = ", ".join(f"{m}={pendiente(est, m, parte):.2f}" for m in mets)
    print(f"  {est:13s} {txt}")
print("\nElección de lista para cada par equivalente:")
for (ml, ea, ma), (est, cand) in eleccion.items():
    print(f"  {ml:9s} vs {ea}.{ma:8s} -> {est}  (candidatas con mejor Big-O: {cand})")
print(f"\nPendiente crecimiento x2 (push total): {pendiente('Duplicar (x2)', 'Push total', 'crecimiento'):.2f}")
print(f"Pendiente crecimiento +1000 (push total): {pendiente('Incremento fijo (+1000)', 'Push total', 'crecimiento'):.2f}")
print(f"Pendiente push con redimension: {pendiente('Duplicar (x2)', 'Push con redimension', 'crecimiento'):.2f}")
