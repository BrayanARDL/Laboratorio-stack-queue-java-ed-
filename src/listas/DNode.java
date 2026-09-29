package listas;

/**
 * Nodo para listas DOBLEMENTE enlazadas.
 * Además del siguiente (next) conoce al anterior (prev), lo que permite
 * recorrer la lista en ambos sentidos y desenlazar un nodo en O(1).
 */
public class DNode<T> {
    T key;
    DNode<T> next;
    DNode<T> prev;

    DNode(T key) {
        this.key = key;
    }

    public T getKey() {
        return key;
    }
}
