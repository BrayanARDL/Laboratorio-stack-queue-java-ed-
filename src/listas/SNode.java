package listas;

/**
 * Nodo para listas SIMPLEMENTE enlazadas.
 * Solo conoce su valor (key) y al siguiente nodo (next).
 */
public class SNode<T> {
    T key;
    SNode<T> next;

    SNode(T key) {
        this.key = key;
    }

    public T getKey() {
        return key;
    }
}
