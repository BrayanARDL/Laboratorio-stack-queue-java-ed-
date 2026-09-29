package pilacola;

/**
 * Cola implementada sobre un arreglo circular dinámico.
 *
 *   enqueue -> addLast (al final), dequeue -> removeFirst (avanza head), front -> first.
 *
 * Aquí el arreglo circular es indispensable: en un arreglo "normal" sacar el
 * primero obligaría a desplazar todos los demás (O(n)); con el índice head
 * que da la vuelta, dequeue es O(1).
 */
public class ArrayQueue<T> implements MyQueue<T> {

    private final CircularDynamicArray<T> arr;

    public ArrayQueue() {
        this.arr = new CircularDynamicArray<>();
    }

    public ArrayQueue(int initialCapacity) {
        this.arr = new CircularDynamicArray<>(initialCapacity);
    }

    /** @param fixedIncrement 0 = duplicar capacidad; > 0 = crecer de a esa cantidad */
    public ArrayQueue(int initialCapacity, int fixedIncrement) {
        this.arr = new CircularDynamicArray<>(initialCapacity, fixedIncrement);
    }

    /** Constructor de copia, O(n). */
    public ArrayQueue(ArrayQueue<T> other) {
        this.arr = new CircularDynamicArray<>(other.arr);
    }

    /** O(1) amortizado */
    @Override
    public void enqueue(T x) {
        arr.addLast(x);
    }

    /** O(1) */
    @Override
    public T dequeue() {
        return arr.removeFirst();
    }

    /** O(1) */
    @Override
    public T front() {
        return arr.first();
    }

    /** O(1) */
    @Override
    public boolean isEmpty() {
        return arr.isEmpty();
    }

    /** O(1) */
    @Override
    public int size() {
        return arr.size();
    }

    /**
     * O(n): busca desde el frente y, si lo encuentra, desplaza hacia adelante
     * los elementos que estaban detrás de él para cerrar el hueco.
     */
    @Override
    public boolean delete(T n) {
        int i = arr.indexOfFromFirst(n);
        if (i < 0) {
            return false;
        }
        arr.removeAt(i);
        return true;
    }

    /** Capacidad del arreglo interno (para el análisis de crecimiento). */
    public int capacity() {
        return arr.capacity();
    }

    /** Del frente (izquierda) al final (derecha). */
    @Override
    public String toString() {
        return arr.toString();
    }
}
