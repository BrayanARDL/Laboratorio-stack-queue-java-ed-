package pilacola;

import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * Arreglo circular dinámico: la base sobre la que se construyen ArrayStack y ArrayQueue.
 *
 * Los elementos ocupan posiciones consecutivas "dando la vuelta" al arreglo:
 *
 *   índice físico:  0   1   2   3   4   5   6   7
 *   data:         [ e   f   .   .   a   b   c   d ]
 *                           ^       ^
 *              fin lógico --+       +-- head (primer elemento lógico)
 *
 * El elemento lógico i vive en la posición física (head + i) mod capacidad.
 * Así se puede sacar por el inicio (avanzando head) sin mover nada: O(1).
 *
 * Crecimiento: cuando el arreglo se llena se crea uno nuevo y se copian los
 * elementos (O(n)). Por defecto la capacidad se DUPLICA, lo que da inserción
 * O(1) AMORTIZADA. Se admite también un incremento fijo (+c) solo para el
 * experimento que compara ambas estrategias.
 */
public class CircularDynamicArray<T> {

    public static final int DEFAULT_CAPACITY = 8;

    private Object[] data;
    private int head;               // posición física del primer elemento lógico
    private int size;               // número de elementos guardados
    private final int fixedIncrement; // 0 => duplicar; > 0 => sumar esa cantidad

    public CircularDynamicArray() {
        this(DEFAULT_CAPACITY, 0);
    }

    public CircularDynamicArray(int initialCapacity) {
        this(initialCapacity, 0);
    }

    /**
     * @param initialCapacity capacidad inicial (mínimo 1)
     * @param fixedIncrement  0 para duplicar la capacidad; un valor > 0 para crecer de a ese número
     */
    public CircularDynamicArray(int initialCapacity, int fixedIncrement) {
        if (initialCapacity < 1) {
            throw new IllegalArgumentException("La capacidad inicial debe ser >= 1");
        }
        if (fixedIncrement < 0) {
            throw new IllegalArgumentException("El incremento no puede ser negativo");
        }
        this.data = new Object[initialCapacity];
        this.fixedIncrement = fixedIncrement;
    }

    /** Constructor de copia: O(n). Copia los elementos en orden lógico. */
    public CircularDynamicArray(CircularDynamicArray<T> other) {
        this.data = new Object[other.data.length];
        this.fixedIncrement = other.fixedIncrement;
        copyInOrder(other.data, other.head, other.size, this.data);
        this.head = 0;
        this.size = other.size;
    }

    // ---------------------------------------------------------------
    // Operaciones en los extremos
    // ---------------------------------------------------------------

    /** O(1) amortizado; O(n) en el instante en que hay que crecer. */
    public void addLast(T x) {
        if (size == data.length) {
            grow();
        }
        data[physical(size)] = x;
        size++;
    }

    /** O(1): se toma data[head] y head avanza una posición (dando la vuelta si hace falta). */
    @SuppressWarnings("unchecked")
    public T removeFirst() {
        checkNotEmpty();
        T x = (T) data[head];
        data[head] = null;          // evita retener la referencia (fuga de memoria)
        head = (head + 1 == data.length) ? 0 : head + 1;
        size--;
        return x;
    }

    /** O(1) */
    @SuppressWarnings("unchecked")
    public T removeLast() {
        checkNotEmpty();
        int p = physical(size - 1);
        T x = (T) data[p];
        data[p] = null;
        size--;
        return x;
    }

    /** O(1) */
    @SuppressWarnings("unchecked")
    public T first() {
        checkNotEmpty();
        return (T) data[head];
    }

    /** O(1) */
    @SuppressWarnings("unchecked")
    public T last() {
        checkNotEmpty();
        return (T) data[physical(size - 1)];
    }

    // ---------------------------------------------------------------
    // Búsqueda y borrado interno (usado por delete de pila y cola)
    // ---------------------------------------------------------------

    /** O(n): índice lógico de la primera aparición de x desde el inicio, o -1. */
    public int indexOfFromFirst(T x) {
        for (int i = 0; i < size; i++) {
            if (Objects.equals(data[physical(i)], x)) {
                return i;
            }
        }
        return -1;
    }

    /** O(n): índice lógico de la primera aparición de x desde el final, o -1. */
    public int indexOfFromLast(T x) {
        for (int i = size - 1; i >= 0; i--) {
            if (Objects.equals(data[physical(i)], x)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * O(n - i): elimina el elemento lógico i desplazando una posición hacia
     * el inicio todos los que están después de él, para no dejar un hueco.
     */
    public void removeAt(int i) {
        if (i < 0 || i >= size) {
            throw new IndexOutOfBoundsException("Índice " + i + " fuera de rango");
        }
        for (int k = i; k < size - 1; k++) {
            data[physical(k)] = data[physical(k + 1)];
        }
        data[physical(size - 1)] = null;
        size--;
    }

    // ---------------------------------------------------------------
    // Consultas
    // ---------------------------------------------------------------

    /** O(1) */
    public int size() {
        return size;
    }

    /** O(1) */
    public boolean isEmpty() {
        return size == 0;
    }

    /** O(1): capacidad actual del arreglo interno (útil para el análisis). */
    public int capacity() {
        return data.length;
    }

    // ---------------------------------------------------------------
    // Auxiliares privados
    // ---------------------------------------------------------------

    /** Convierte índice lógico en físico. Equivale a (head + i) % capacidad, sin la división. */
    private int physical(int i) {
        int p = head + i;
        return (p >= data.length) ? p - data.length : p;
    }

    /** O(n): crea un arreglo más grande y copia los elementos en orden lógico, desde la posición 0. */
    private void grow() {
        long newCap = (fixedIncrement == 0)
                ? (long) data.length * 2
                : (long) data.length + fixedIncrement;
        if (newCap > Integer.MAX_VALUE - 8) {
            newCap = Integer.MAX_VALUE - 8;
            if (newCap <= data.length) {
                throw new OutOfMemoryError("Capacidad máxima alcanzada");
            }
        }
        Object[] bigger = new Object[(int) newCap];
        copyInOrder(data, head, size, bigger);
        data = bigger;
        head = 0;
    }

    /** Copia 'count' elementos desde src (empezando en start, con vuelta) a dst[0..count-1]. */
    private static void copyInOrder(Object[] src, int start, int count, Object[] dst) {
        int firstPart = Math.min(count, src.length - start);   // desde start hasta el final físico
        System.arraycopy(src, start, dst, 0, firstPart);
        System.arraycopy(src, 0, dst, firstPart, count - firstPart); // lo que dio la vuelta
    }

    private void checkNotEmpty() {
        if (size == 0) {
            throw new NoSuchElementException("La estructura está vacía");
        }
    }

    /** Elementos en orden lógico (solo para depurar). */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < size; i++) {
            sb.append(data[physical(i)]);
            if (i < size - 1) sb.append(", ");
        }
        return sb.append("]").toString();
    }
}
