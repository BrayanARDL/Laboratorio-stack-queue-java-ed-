package pilacola;

/**
 * Pila implementada sobre un arreglo circular dinámico.
 *
 * La base de la pila es el inicio lógico del arreglo y la cima es el final lógico:
 *   push -> addLast, pop -> removeLast, peek -> last.
 * Todas son O(1) (push es O(1) amortizado por el crecimiento).
 */
public class ArrayStack<T> implements MyStack<T> {

    private final CircularDynamicArray<T> arr;

    public ArrayStack() {
        this.arr = new CircularDynamicArray<>();
    }

    public ArrayStack(int initialCapacity) {
        this.arr = new CircularDynamicArray<>(initialCapacity);
    }

    /** @param fixedIncrement 0 = duplicar capacidad; > 0 = crecer de a esa cantidad */
    public ArrayStack(int initialCapacity, int fixedIncrement) {
        this.arr = new CircularDynamicArray<>(initialCapacity, fixedIncrement);
    }

    /** Constructor de copia, O(n). */
    public ArrayStack(ArrayStack<T> other) {
        this.arr = new CircularDynamicArray<>(other.arr);
    }

    /** O(1) amortizado */
    @Override
    public void push(T x) {
        arr.addLast(x);
    }

    /** O(1) */
    @Override
    public T pop() {
        return arr.removeLast();
    }

    /** O(1) */
    @Override
    public T peek() {
        return arr.last();
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
     * O(n): busca desde la cima hacia la base y, si lo encuentra, desplaza
     * hacia abajo los elementos que estaban encima para cerrar el hueco.
     * Peor caso: el valor está en la base (se recorre todo y se desplaza todo).
     */
    @Override
    public boolean delete(T n) {
        int i = arr.indexOfFromLast(n);
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

    /** De la base (izquierda) a la cima (derecha). */
    @Override
    public String toString() {
        return arr.toString();
    }
}
