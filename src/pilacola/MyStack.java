package pilacola;

/** Pila (LIFO): el último en entrar es el primero en salir. */
public interface MyStack<T> {

    /** Inserta un elemento en la cima. */
    void push(T x);

    /** Elimina y retorna el elemento de la cima. */
    T pop();

    /** Retorna el elemento de la cima sin eliminarlo. */
    T peek();

    /** true si la pila está vacía. */
    boolean isEmpty();

    /** Número de elementos en la pila. */
    int size();

    /** Elimina el primer valor n que encuentra (buscando desde la cima). Retorna true si lo encontró. */
    boolean delete(T n);
}
