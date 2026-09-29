package benchmark;

import listas.*;
import pilacola.*;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Random;
import java.util.function.Supplier;

/**
 * Medición de tiempos de ejecución.
 *
 * METODOLOGÍA (resumen; el informe la explica en detalle)
 *  1. Para cada tamaño n se construye la estructura con n valores ALEATORIOS.
 *  2. Se mide UNA sola operación con System.nanoTime() justo antes y justo después.
 *  3. Cualquier trabajo auxiliar (preparar el caso, deshacer la operación para que
 *     la estructura vuelva a tener n elementos) se hace FUERA del intervalo medido.
 *  4. Cada medición se repite varias veces; las primeras ("calentamiento") se descartan
 *     para que el compilador JIT de la JVM ya haya optimizado el código.
 *  5. Se mide el PEOR CASO de cada método, porque eso es lo que describe Big-O.
 *  6. Los tiempos crudos (ns) se escriben en un CSV. La graficación se hace después,
 *     en un programa aparte (analisis/graficar.py), para no sesgar la medición.
 *
 * USO:  java -Xms5g -Xmx5g -XX:+UseParallelGC -cp out benchmark.Benchmark [maxExp] [archivo.csv]
 *       maxExp = exponente del tamaño máximo (por defecto 8 => hasta 10^8)
 */
public class Benchmark {

    // ------------------------------------------------------------------
    // Datos de entrada aleatorios
    // ------------------------------------------------------------------
    private static final int POOL_SIZE = 1 << 20;          // 1.048.576 valores
    private static final int POOL_MASK = POOL_SIZE - 1;
    /**
     * Banco de valores aleatorios en [0, 10^9). Se reutilizan los mismos objetos
     * Integer para no crear 10^8 objetos adicionales (ahorra ~1,6 GB de memoria);
     * los valores siguen siendo aleatorios.
     */
    private static final Integer[] POOL = new Integer[POOL_SIZE];
    private static int poolIdx = 0;

    /** Centinelas negativos: nunca aparecen entre los valores aleatorios. */
    private static final Integer NO_EXISTE = -1;   // para find en peor caso (recorre todo)
    private static final Integer OBJETIVO = -2;    // elemento que se borra en erase/delete
    private static final Integer ANCLA = -3;       // nodo de referencia para addBefore/addAfter

    /**
     * Memoria aproximada que exige el benchmark por elemento en el tamaño máximo
     * (nodo de 24 bytes + margen para el recolector y los arreglos de la pila/cola).
     */
    private static final double BYTES_POR_ELEMENTO = 45;

    /** Evita que el JIT elimine operaciones cuyo resultado no se usa. */
    public static volatile Object sink;

    private static PrintWriter csv;                // null durante el calentamiento global
    private static final long[] BUFFER = new long[1000];

    static {
        Random rnd = new Random(2026);
        for (int i = 0; i < POOL_SIZE; i++) {
            POOL[i] = rnd.nextInt(1_000_000_000);
        }
    }

    private static Integer nextValue() {
        poolIdx = (poolIdx + 1) & POOL_MASK;
        return POOL[poolIdx];
    }

    // ------------------------------------------------------------------
    // Programa principal
    // ------------------------------------------------------------------
    public static void main(String[] args) throws IOException {
        Locale.setDefault(Locale.US);
        int maxExp = args.length > 0 ? Integer.parseInt(args[0]) : 8;
        Path salida = Path.of(args.length > 1 ? args[1] : "resultados/tiempos.csv");
        if (salida.getParent() != null) Files.createDirectories(salida.getParent());

        long memoria = Runtime.getRuntime().maxMemory();
        log("Memoria máxima de la JVM: %.1f GB", memoria / 1e9);

        // Si la JVM no tiene memoria para el tamaño pedido (p. ej. al ejecutar desde un IDE
        // sin -Xmx), se reduce el tamaño máximo en lugar de fallar con OutOfMemoryError.
        int pedido = maxExp;
        while (maxExp > 1 && BYTES_POR_ELEMENTO * Math.pow(10, maxExp) > memoria) {
            maxExp--;
        }
        if (maxExp < pedido) {
            log("AVISO: 10^%d necesita unos %.1f GB de memoria y la JVM tiene %.1f GB.",
                    pedido, BYTES_POR_ELEMENTO * Math.pow(10, pedido) / 1e9, memoria / 1e9);
            log("       Se medirá hasta 10^%d. Para llegar a 10^%d ejecute con -Xms5g -Xmx5g (ver README).",
                    maxExp, pedido);
        }
        log("Calentando la JVM (JIT)...");
        calentamiento();

        try (PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(salida.toFile())))) {
            csv = out;
            csv.println("parte,estructura,metodo,n,rep,ns");

            // Referencia: costo de medir "nada" (resolución/overhead de nanoTime)
            medir("calibracion", "nanoTime", "vacio", 0, 100, 1000, () -> {}, () -> {}, () -> {});

            for (int e = 1; e <= maxExp; e++) {
                int n = (int) Math.pow(10, e);
                log("===== n = 10^%d =====", e);
                benchLista("SLL sin cola", SinglyLinkedList::new, n);
                benchLista("SLL con cola", SinglyLinkedListTail::new, n);
                benchLista("DLL sin cola", DoublyLinkedList::new, n);
                benchLista("DLL con cola", DoublyLinkedListTail::new, n);
                benchPila(n);
                benchCola(n);
                benchCrecimiento(n);
                csv.flush();
            }
        }
        log("Listo. Resultados en %s", salida);
    }

    /** Ejecuta todo con tamaños pequeños SIN guardar resultados, para que el JIT compile el código. */
    private static void calentamiento() {
        csv = null;
        for (int ronda = 0; ronda < 3; ronda++) {
            for (int n : new int[]{1_000, 10_000}) {
                benchLista("w", SinglyLinkedList::new, n);
                benchLista("w", SinglyLinkedListTail::new, n);
                benchLista("w", DoublyLinkedList::new, n);
                benchLista("w", DoublyLinkedListTail::new, n);
                benchPila(n);
                benchCola(n);
                benchCrecimiento(n);
            }
        }
    }

    // ------------------------------------------------------------------
    // Número de repeticiones según el tamaño
    // ------------------------------------------------------------------
    private static int reps(int n) {
        if (n <= 100_000) return 50;
        if (n <= 1_000_000) return 20;
        if (n <= 10_000_000) return 10;
        return 5;
    }

    private static int warm(int n) {
        if (n <= 1_000_000) return 5;
        if (n <= 10_000_000) return 2;
        return 1;
    }

    // ------------------------------------------------------------------
    // Núcleo de la medición
    // ------------------------------------------------------------------

    /**
     * Mide 'operacion' (warm + reps) veces. En cada repetición:
     *   preparar  -> (fuera de la medición)
     *   t0 = nanoTime; operacion; t1 = nanoTime   <- SOLO esto se mide
     *   deshacer  -> (fuera de la medición)
     * Los tiempos se guardan en memoria y se escriben al CSV al final,
     * así la escritura a disco tampoco interfiere.
     */
    private static void medir(String parte, String estructura, String metodo, int n,
                              int warm, int reps,
                              Runnable preparar, Runnable operacion, Runnable deshacer) {
        for (int r = 0; r < warm + reps; r++) {
            preparar.run();
            long t0 = System.nanoTime();
            operacion.run();
            long t1 = System.nanoTime();
            deshacer.run();
            if (r >= warm) BUFFER[r - warm] = t1 - t0;
        }
        if (csv != null) {
            for (int r = 0; r < reps; r++) {
                csv.printf("%s,%s,%s,%d,%d,%d%n", parte, estructura, metodo, n, r, BUFFER[r]);
            }
        }
    }

    private static final Runnable NADA = () -> {};

    /** Estado mutable compartido entre las lambdas de una medición. */
    private static final class Estado<N> {
        N nodo;
        Integer valor;
    }

    // ------------------------------------------------------------------
    // PARTE 1: listas enlazadas
    // ------------------------------------------------------------------
    private static <N> void benchLista(String nombre, Supplier<LinkedListADT<Integer, N>> fabrica, int n) {
        int W = warm(n), R = reps(n);
        if (csv != null) log("  Lista %-13s", nombre);

        LinkedListADT<Integer, N> L = fabrica.get();
        for (int i = 0; i < n; i++) {
            L.pushFront(nextValue());               // O(1) en las 4 => construir es rápido
        }
        Estado<N> e = new Estado<>();
        String p = "lista";

        // Inserciones: se deshacen retirando lo insertado
        medir(p, nombre, "PushFront", n, W, R,
                () -> e.valor = nextValue(), () -> L.pushFront(e.valor), () -> L.popFront());
        medir(p, nombre, "PushBack", n, W, R,
                () -> e.valor = nextValue(), () -> L.pushBack(e.valor), () -> L.popBack());

        // Eliminaciones: se deshacen reinsertando lo eliminado
        medir(p, nombre, "PopFront", n, W, R,
                NADA, () -> e.valor = L.popFront(), () -> L.pushFront(e.valor));
        medir(p, nombre, "PopBack", n, W, R,
                NADA, () -> e.valor = L.popBack(), () -> L.pushBack(e.valor));

        // Consultas en los extremos
        medir(p, nombre, "TopFront", n, W, R, NADA, () -> sink = L.topFront(), NADA);
        medir(p, nombre, "TopBack", n, W, R, NADA, () -> sink = L.topBack(), NADA);

        // Find en peor caso: el valor no existe => recorre los n nodos
        medir(p, nombre, "Find", n, W, R, NADA, () -> sink = L.find(NO_EXISTE), NADA);

        // Erase en peor caso: el elemento a borrar está al final
        medir(p, nombre, "Erase", n, W, R,
                () -> L.pushBack(OBJETIVO), () -> sink = L.erase(OBJETIVO), NADA);

        // AddBefore / AddAfter respecto al ÚLTIMO nodo (peor caso para AddBefore en listas simples).
        // La referencia al nodo se obtiene con find ANTES de medir.
        medir(p, nombre, "AddBefore", n, W, R,
                () -> { L.pushBack(ANCLA); e.nodo = L.find(ANCLA); e.valor = nextValue(); },
                () -> L.addBefore(e.nodo, e.valor),
                () -> { L.popBack(); L.popBack(); });     // quita ANCLA y el valor insertado
        medir(p, nombre, "AddAfter", n, W, R,
                () -> { L.pushBack(ANCLA); e.nodo = L.find(ANCLA); e.valor = nextValue(); },
                () -> L.addAfter(e.nodo, e.valor),
                () -> { L.popBack(); L.popBack(); });     // quita el valor insertado y ANCLA

        medir(p, nombre, "Empty", n, W, R, NADA, () -> sink = L.isEmpty(), NADA);
        medir(p, nombre, "Size", n, W, R, NADA, () -> sink = L.size(), NADA);

        verificarTamano(nombre, L.size(), n);
        liberarMemoria();
    }

    // ------------------------------------------------------------------
    // PARTE 2: pila y cola con arreglo circular dinámico
    // ------------------------------------------------------------------
    private static void benchPila(int n) {
        int W = warm(n), R = reps(n);
        if (csv != null) log("  Pila (arreglo circular)");
        String p = "pilacola", nombre = "ArrayStack";
        Estado<Void> e = new Estado<>();

        ArrayStack<Integer> S = new ArrayStack<>();
        for (int i = 0; i < n; i++) S.push(nextValue());

        medir(p, nombre, "Push", n, W, R, () -> e.valor = nextValue(), () -> S.push(e.valor), () -> S.pop());
        medir(p, nombre, "Pop", n, W, R, NADA, () -> e.valor = S.pop(), () -> S.push(e.valor));
        medir(p, nombre, "Peek", n, W, R, NADA, () -> sink = S.peek(), NADA);
        medir(p, nombre, "IsEmpty", n, W, R, NADA, () -> sink = S.isEmpty(), NADA);
        medir(p, nombre, "Size", n, W, R, NADA, () -> sink = S.size(), NADA);
        verificarTamano(nombre, S.size(), n);

        // Delete en peor caso: el valor está en la BASE (se busca desde la cima y
        // luego se desplazan todos los de encima). Cada repetición trabaja sobre una
        // copia recién hecha de la pila base (la copia se hace fuera de la medición).
        ArrayStack<Integer> base = new ArrayStack<>();
        base.push(OBJETIVO);
        for (int i = 1; i < n; i++) base.push(nextValue());
        @SuppressWarnings("unchecked")
        ArrayStack<Integer>[] copia = new ArrayStack[1];
        medir(p, nombre, "Delete", n, W, R,
                () -> copia[0] = new ArrayStack<>(base),
                () -> sink = copia[0].delete(OBJETIVO),
                () -> copia[0] = null);
        liberarMemoria();
    }

    private static void benchCola(int n) {
        int W = warm(n), R = reps(n);
        if (csv != null) log("  Cola (arreglo circular)");
        String p = "pilacola", nombre = "ArrayQueue";
        Estado<Void> e = new Estado<>();

        ArrayQueue<Integer> Q = new ArrayQueue<>();
        for (int i = 0; i < n; i++) Q.enqueue(nextValue());

        // Deshacer enqueue con dequeue mantiene n elementos y hace que el contenido
        // "gire" dentro del arreglo circular (head da la vuelta).
        medir(p, nombre, "Enqueue", n, W, R, () -> e.valor = nextValue(), () -> Q.enqueue(e.valor), () -> Q.dequeue());
        medir(p, nombre, "Dequeue", n, W, R, NADA, () -> e.valor = Q.dequeue(), () -> Q.enqueue(e.valor));
        medir(p, nombre, "Front", n, W, R, NADA, () -> sink = Q.front(), NADA);
        medir(p, nombre, "IsEmpty", n, W, R, NADA, () -> sink = Q.isEmpty(), NADA);
        medir(p, nombre, "Size", n, W, R, NADA, () -> sink = Q.size(), NADA);

        // Delete en peor caso de búsqueda: el valor está al FINAL (se recorren los n).
        medir(p, nombre, "Delete", n, W, R,
                () -> Q.enqueue(OBJETIVO), () -> sink = Q.delete(OBJETIVO), NADA);
        verificarTamano(nombre, Q.size(), n);
        liberarMemoria();
    }

    // ------------------------------------------------------------------
    // Análisis del crecimiento del arreglo dinámico
    // ------------------------------------------------------------------
    private static void benchCrecimiento(int n) {
        String p = "crecimiento";
        int R = Math.max(3, reps(n) / 5), W = (n <= 1_000_000) ? 2 : 1;
        if (csv != null) log("  Crecimiento del arreglo");

        // (a) Costo TOTAL de n inserciones desde vacío (capacidad inicial 8).
        //     Dividido entre n da el costo amortizado por inserción.
        @SuppressWarnings("unchecked")
        ArrayStack<Integer>[] s = new ArrayStack[1];
        @SuppressWarnings("unchecked")
        ArrayQueue<Integer>[] q = new ArrayQueue[1];

        medir(p, "Duplicar (x2)", "Push total", n, W, R,
                () -> s[0] = new ArrayStack<>(),
                () -> { ArrayStack<Integer> st = s[0]; for (int i = 0; i < n; i++) st.push(POOL[i & POOL_MASK]); },
                () -> s[0] = null);
        medir(p, "Duplicar (x2)", "Enqueue total", n, W, R,
                () -> q[0] = new ArrayQueue<>(),
                () -> { ArrayQueue<Integer> qu = q[0]; for (int i = 0; i < n; i++) qu.enqueue(POOL[i & POOL_MASK]); },
                () -> q[0] = null);

        // Estrategia alternativa: crecer de a 1000 posiciones. Costo total O(n^2):
        // solo hasta 10^6 porque para 10^7 tardaría varios minutos por repetición.
        if (n <= 1_000_000) {
            medir(p, "Incremento fijo (+1000)", "Push total", n, W, R,
                    () -> s[0] = new ArrayStack<>(CircularDynamicArray.DEFAULT_CAPACITY, 1000),
                    () -> { ArrayStack<Integer> st = s[0]; for (int i = 0; i < n; i++) st.push(POOL[i & POOL_MASK]); },
                    () -> s[0] = null);
        }

        // (b) Un solo push justo cuando el arreglo está LLENO: dispara la copia O(n).
        medir(p, "Duplicar (x2)", "Push con redimension", n, W, R,
                () -> { s[0] = new ArrayStack<>(n); for (int i = 0; i < n; i++) s[0].push(POOL[i & POOL_MASK]); },
                () -> s[0].push(ANCLA),
                () -> s[0] = null);
        liberarMemoria();
    }

    // ------------------------------------------------------------------
    // Utilidades
    // ------------------------------------------------------------------
    private static void verificarTamano(String nombre, int real, int esperado) {
        if (real != esperado) {
            throw new IllegalStateException(nombre + ": el tamaño cambió durante la medición ("
                    + real + " != " + esperado + ")");
        }
    }

    /** Sugiere al recolector de basura liberar la estructura anterior (fuera de toda medición). */
    private static void liberarMemoria() {
        System.gc();
    }

    private static void log(String fmt, Object... args) {
        System.out.printf("[%tT] " + fmt + "%n", prepend(System.currentTimeMillis(), args));
        System.out.flush();
    }

    private static Object[] prepend(Object first, Object[] rest) {
        Object[] all = new Object[rest.length + 1];
        all[0] = first;
        System.arraycopy(rest, 0, all, 1, rest.length);
        return all;
    }
}
