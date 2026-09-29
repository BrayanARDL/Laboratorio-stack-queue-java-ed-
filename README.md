# Listas, Pilas y Colas en Java — Implementación y Análisis de Complejidad

**Estructuras de Datos 2026-2** · Universidad Nacional de Colombia  
Profesor: David Herrera · Monitora: Ángela Camila Siabato Londoño  
Estudiante: Brallan Esteban Ardila Osorio

Implementación desde cero (sin `java.util` en las estructuras) de:

- **Listas enlazadas** en cuatro variantes: simplemente enlazada sin cola, simplemente enlazada con cola,
  doblemente enlazada sin cola y doblemente enlazada con cola. Métodos: `pushFront`, `pushBack`,
  `popFront`, `popBack`, `topFront`, `topBack`, `find`, `erase`, `addBefore`, `addAfter`, `isEmpty`, `size`.
- **`MyStack<T>`** y **`MyQueue<T>`** sobre un **arreglo circular dinámico** que duplica su capacidad al llenarse.

Incluye el benchmark que mide el tiempo de cada método para tamaños de 10¹ a 10⁸, el script que grafica
los resultados y el informe en LaTeX.

## Estructura del repositorio

```
src/
  listas/      LinkedListADT, SNode, DNode y las 4 implementaciones de lista
  pilacola/    MyStack, MyQueue, CircularDynamicArray, ArrayStack, ArrayQueue
  pruebas/     Pruebas de correctitud (comparación contra java.util con operaciones aleatorias)
  benchmark/   Medición de tiempos con System.nanoTime() -> resultados/tiempos.csv
analisis/
  graficar.py  Lee el CSV y genera gráficas (matplotlib) y tablas (LaTeX)
resultados/
  tiempos.csv  Tiempos crudos en nanosegundos (una fila por repetición)
  resumen.csv  Promedio recortado, mediana, mínimo y máximo por método y tamaño
informe/
  informe.tex  Fuente del informe; figuras/ y tablas/ se generan con graficar.py
```

## Uso de librerías

Las estructuras (`src/listas` y `src/pilacola`) **no importan ninguna librería**: no usan `java.util`
(ni `ArrayList`, `LinkedList`, `ArrayDeque`, `Stack`, etc.). Se pueden compilar solas:
`javac src/listas/*.java src/pilacola/*.java`.

`java.util` aparece únicamente fuera de las estructuras, como permite el enunciado:

| Archivo | Qué usa | Para qué |
|---|---|---|
| `src/benchmark/Benchmark.java` | `Random`, `Locale`, `function.Supplier`, `java.io`, `java.nio` | Generar las entradas aleatorias, crear cada estructura y escribir el CSV de tiempos (medición) |
| `src/pruebas/Pruebas.java` | `ArrayList`, `List`, `Random`, `Objects`, `function.Supplier` | Pruebas de correctitud: `ArrayList` es solo la referencia contra la que se comparan los resultados; las estructuras nunca la usan |
| `analisis/graficar.py` | `pandas`, `matplotlib` | Graficación, separada de la medición |

## Complejidad teórica (peor caso)

| Método     | Simple sin cola | Simple con cola | Doble sin cola | Doble con cola | Pila / Cola (arreglo circular) |
|------------|:---:|:---:|:---:|:---:|:---:|
| PushFront  | O(1) | O(1) | O(1) | O(1) | `push`: O(1) amortizado |
| PushBack   | O(n) | O(1) | O(n) | O(1) | `enqueue`: O(1) amortizado |
| PopFront   | O(1) | O(1) | O(1) | O(1) | `pop` / `dequeue`: O(1) |
| PopBack    | O(n) | O(n) | O(n) | O(1) | — |
| TopFront   | O(1) | O(1) | O(1) | O(1) | `peek` / `front`: O(1) |
| TopBack    | O(n) | O(1) | O(n) | O(1) | — |
| Find       | O(n) | O(n) | O(n) | O(n) | — |
| Erase      | O(n) | O(n) | O(n) | O(n) | `delete`: O(n) |
| AddBefore  | O(n) | O(n) | O(1) | O(1) | — |
| AddAfter   | O(1) | O(1) | O(1) | O(1) | — |
| Empty/Size | O(1) | O(1) | O(1) | O(1) | O(1) |

## Cómo ejecutar

Requisitos: Java 8 o superior (probado con 8, 11, 17 y 21) y Python 3 con `pandas` y `matplotlib` (`pip install pandas matplotlib`).

```bash
# Linux / macOS
bash ejecutar.sh     # todo: compilar, probar, medir (hasta 10^8) y graficar
bash ejecutar.sh 6   # igual, pero solo hasta 10^6 (unos segundos, poca memoria)
```

```bat
REM Windows
ejecutar.bat
ejecutar.bat 6
```

Paso a paso (en Windows, compilar con
`javac -encoding UTF-8 -d out src\listas\*.java src\pilacola\*.java src\benchmark\*.java src\pruebas\*.java`):

```bash
javac -encoding UTF-8 -d out $(find src -name "*.java")
java -cp out pruebas.Pruebas
java -Xms5g -Xmx5g -XX:+UseParallelGC -XX:+AlwaysPreTouch -cp out benchmark.Benchmark 8 resultados/tiempos.csv
python3 analisis/graficar.py resultados/tiempos.csv
```

Para 10⁸ se necesitan unos 6 GB de RAM libres (la lista de 10⁸ nodos ocupa ~2,4 GB).
Si el equipo tiene menos memoria, use `maxExp = 7`.

**Desde un IDE (IntelliJ, VS Code, Eclipse):** ejecute el `main` de `pruebas.Pruebas` para verificar las
estructuras y el de `benchmark.Benchmark` para medir. Si la JVM no tiene memoria suficiente para 10⁸
(por ejemplo, sin la opción `-Xmx5g`), el benchmark lo avisa y mide automáticamente hasta el mayor tamaño
que quepa, en lugar de fallar.

## Metodología de medición (resumen)

- **Nanosegundos** con `System.nanoTime()`: las operaciones O(1) duran decenas de nanosegundos, y con
  milisegundos todas aparecerían como 0. Los resultados se reportan en microsegundos.
- Se mide **una sola operación** sobre una estructura que ya tiene *n* elementos aleatorios. Preparar el
  caso y deshacer la operación ocurre **fuera** del intervalo medido, así la estructura conserva exactamente *n* elementos.
- Se mide el **peor caso** de cada método (por ejemplo, `find` de un valor inexistente o `addBefore` del último nodo).
- Hay **calentamiento** de la JVM (JIT) y cada medición se repite (50 veces para n ≤ 10⁵, 5 para 10⁸).
  Se reporta el promedio recortado al 10 %.
- La **graficación está separada** de la medición: Java escribe un CSV y Python grafica después.
