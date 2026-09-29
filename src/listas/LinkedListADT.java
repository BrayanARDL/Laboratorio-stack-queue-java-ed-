package listas;

/**
 * Interfaz común para las cuatro implementaciones de lista enlazada.
 *
 * @param <T> tipo de los elementos (claves)
 * @param <N> tipo de nodo que usa la implementación (SNode o DNode).
 *            find() devuelve una referencia a un nodo de este tipo y
 *            addBefore/addAfter la reciben como punto de inserción.
 */
public interface LinkedListADT<T, N> {

    /** Inserta un elemento al inicio de la lista. */
    void pushFront(T key);

    /** Retorna el primer elemento sin eliminarlo. */
    T topFront();

    /** Elimina y retorna el primer elemento. */
    T popFront();

    /** Inserta un elemento al final de la lista. */
    void pushBack(T key);

    /** Retorna el último elemento sin eliminarlo. */
    T topBack();

    /** Elimina y retorna el último elemento. */
    T popBack();

    /** Retorna la referencia al primer nodo que contiene key, o null si no existe. */
    N find(T key);

    /** Elimina la primera aparición de key. Retorna true si la encontró. */
    boolean erase(T key);

    /**
     * Inserta key inmediatamente antes del nodo indicado.
     * Precondición: el nodo pertenece a esta lista (normalmente se obtiene con find).
     */
    void addBefore(N node, T key);

    /**
     * Inserta key inmediatamente después del nodo indicado.
     * Precondición: el nodo pertenece a esta lista. No se verifica, porque comprobarlo
     * exigiría recorrer la lista y la operación dejaría de ser O(1).
     */
    void addAfter(N node, T key);

    /** true si la lista no tiene elementos. */
    boolean isEmpty();

    /** Número de elementos de la lista. */
    int size();
}
