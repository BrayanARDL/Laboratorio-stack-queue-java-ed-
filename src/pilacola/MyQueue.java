package pilacola;

/** Cola (FIFO): el primero en entrar es el primero en salir. */
public interface MyQueue<T> {

    /** Inserta un elemento al final. */
    void enqueue(T x);

    /** Elimina y retorna el primer elemento. */
    T dequeue();

    /** Retorna el primer elemento sin eliminarlo. */
    T front();

    /** true si la cola está vacía. */
    boolean isEmpty();

    /** Número de elementos en la cola. */
    int size();

    /** Elimina el primer valor n que encuentra (buscando desde el frente). Retorna true si lo encontró. */
    boolean delete(T n);
}
