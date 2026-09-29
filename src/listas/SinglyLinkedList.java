package listas;

import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * Lista simplemente enlazada SIN cola (solo guarda head).
 *
 *   head -> [a] -> [b] -> [c] -> null
 *
 * Todo lo que ocurra al final de la lista obliga a recorrerla completa.
 */
public class SinglyLinkedList<T> implements LinkedListADT<T, SNode<T>> {

    private SNode<T> head;
    private int size;   // contador: permite size() en O(1)

    // ---------------------------------------------------------------
    // Operaciones al INICIO: siempre O(1)
    // ---------------------------------------------------------------

    /** O(1): el nuevo nodo apunta al antiguo head y pasa a ser el head. */
    @Override
    public void pushFront(T key) {
        SNode<T> node = new SNode<>(key);
        node.next = head;
        head = node;
        size++;
    }

    /** O(1) */
    @Override
    public T topFront() {
        checkNotEmpty();
        return head.key;
    }

    /** O(1): head avanza al segundo nodo; el primero queda sin referencias (lo recoge el GC). */
    @Override
    public T popFront() {
        checkNotEmpty();
        T key = head.key;
        head = head.next;
        size--;
        return key;
    }

    // ---------------------------------------------------------------
    // Operaciones al FINAL: O(n), no sabemos dónde está el último
    // ---------------------------------------------------------------

    /** O(n): hay que caminar hasta el último nodo para enlazar el nuevo. */
    @Override
    public void pushBack(T key) {
        SNode<T> node = new SNode<>(key);
        if (head == null) {
            head = node;
        } else {
            SNode<T> cur = head;
            while (cur.next != null) {
                cur = cur.next;
            }
            cur.next = node;
        }
        size++;
    }

    /** O(n) */
    @Override
    public T topBack() {
        checkNotEmpty();
        SNode<T> cur = head;
        while (cur.next != null) {
            cur = cur.next;
        }
        return cur.key;
    }

    /** O(n): hay que encontrar el PENÚLTIMO nodo para cortar su enlace. */
    @Override
    public T popBack() {
        checkNotEmpty();
        if (head.next == null) {            // un solo elemento
            T key = head.key;
            head = null;
            size--;
            return key;
        }
        SNode<T> cur = head;
        while (cur.next.next != null) {     // parar en el penúltimo
            cur = cur.next;
        }
        T key = cur.next.key;
        cur.next = null;
        size--;
        return key;
    }

    // ---------------------------------------------------------------
    // Búsqueda y borrado: O(n) en el peor caso
    // ---------------------------------------------------------------

    /** O(n): retorna la REFERENCIA al primer nodo con esa clave, o null. */
    @Override
    public SNode<T> find(T key) {
        SNode<T> cur = head;
        while (cur != null) {
            if (Objects.equals(cur.key, key)) {
                return cur;
            }
            cur = cur.next;
        }
        return null;
    }

    /** O(n): borra la primera aparición de key. */
    @Override
    public boolean erase(T key) {
        if (head == null) {
            return false;
        }
        if (Objects.equals(head.key, key)) {   // caso especial: es el primero
            head = head.next;
            size--;
            return true;
        }
        SNode<T> prev = head;                  // buscamos el nodo ANTERIOR al que se borra
        while (prev.next != null && !Objects.equals(prev.next.key, key)) {
            prev = prev.next;
        }
        if (prev.next == null) {
            return false;                      // no estaba
        }
        prev.next = prev.next.next;            // "saltamos" el nodo
        size--;
        return true;
    }

    // ---------------------------------------------------------------
    // Inserción relativa a un nodo dado (la referencia viene de find)
    // ---------------------------------------------------------------

    /** O(1): ya tenemos el nodo, solo reacomodamos dos referencias. */
    @Override
    public void addAfter(SNode<T> node, T key) {
        if (node == null) {
            throw new IllegalArgumentException("El nodo no puede ser null");
        }
        SNode<T> newNode = new SNode<>(key);
        newNode.next = node.next;
        node.next = newNode;
        size++;
    }

    /** O(n): el nodo no sabe quién lo antecede; hay que buscar al anterior desde head. */
    @Override
    public void addBefore(SNode<T> node, T key) {
        if (node == null) {
            throw new IllegalArgumentException("El nodo no puede ser null");
        }
        if (node == head) {
            pushFront(key);
            return;
        }
        SNode<T> prev = head;
        while (prev != null && prev.next != node) {
            prev = prev.next;
        }
        if (prev == null) {
            throw new IllegalArgumentException("El nodo no pertenece a la lista");
        }
        SNode<T> newNode = new SNode<>(key);
        newNode.next = node;
        prev.next = newNode;
        size++;
    }

    // ---------------------------------------------------------------
    // Auxiliares
    // ---------------------------------------------------------------

    /** O(1) */
    @Override
    public boolean isEmpty() {
        return head == null;
    }

    /** O(1) gracias al contador */
    @Override
    public int size() {
        return size;
    }

    private void checkNotEmpty() {
        if (head == null) {
            throw new NoSuchElementException("La lista está vacía");
        }
    }

    /** Solo para depurar; NO se usa dentro de la medición de tiempos. */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("[");
        SNode<T> cur = head;
        while (cur != null) {
            sb.append(cur.key);
            if (cur.next != null) sb.append(", ");
            cur = cur.next;
        }
        return sb.append("]").toString();
    }
}
