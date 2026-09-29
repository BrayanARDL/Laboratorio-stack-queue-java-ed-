package listas;

import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * Lista doblemente enlazada SIN cola (solo guarda head).
 *
 *   null <- [a] <-> [b] <-> [c] -> null
 *            ^
 *           head
 *
 * El enlace prev hace que addBefore y el desenlace de un nodo sean O(1),
 * pero sin tail seguimos teniendo que recorrer la lista para llegar al final.
 */
public class DoublyLinkedList<T> implements LinkedListADT<T, DNode<T>> {

    private DNode<T> head;
    private int size;

    /** O(1) */
    @Override
    public void pushFront(T key) {
        DNode<T> node = new DNode<>(key);
        node.next = head;
        if (head != null) {
            head.prev = node;
        }
        head = node;
        size++;
    }

    /** O(1) */
    @Override
    public T topFront() {
        checkNotEmpty();
        return head.key;
    }

    /** O(1) */
    @Override
    public T popFront() {
        checkNotEmpty();
        T key = head.key;
        head = head.next;
        if (head != null) {
            head.prev = null;
        }
        size--;
        return key;
    }

    /** O(n): sin tail hay que caminar hasta el último. */
    @Override
    public void pushBack(T key) {
        DNode<T> node = new DNode<>(key);
        if (head == null) {
            head = node;
        } else {
            DNode<T> last = lastNode();
            last.next = node;
            node.prev = last;
        }
        size++;
    }

    /** O(n) */
    @Override
    public T topBack() {
        checkNotEmpty();
        return lastNode().key;
    }

    /**
     * O(n): llegar al último cuesta O(n); una vez allí, gracias a prev,
     * desenlazarlo es O(1) (no hace falta buscar al penúltimo por separado).
     */
    @Override
    public T popBack() {
        checkNotEmpty();
        DNode<T> last = lastNode();
        unlink(last);
        return last.key;
    }

    /** O(n) */
    @Override
    public DNode<T> find(T key) {
        DNode<T> cur = head;
        while (cur != null) {
            if (Objects.equals(cur.key, key)) {
                return cur;
            }
            cur = cur.next;
        }
        return null;
    }

    /** O(n) por la búsqueda; el desenlace en sí es O(1). */
    @Override
    public boolean erase(T key) {
        DNode<T> node = find(key);
        if (node == null) {
            return false;
        }
        unlink(node);
        return true;
    }

    /** O(1) */
    @Override
    public void addAfter(DNode<T> node, T key) {
        if (node == null) {
            throw new IllegalArgumentException("El nodo no puede ser null");
        }
        DNode<T> newNode = new DNode<>(key);
        newNode.prev = node;
        newNode.next = node.next;
        if (node.next != null) {
            node.next.prev = newNode;
        }
        node.next = newNode;
        size++;
    }

    /** O(1): a diferencia de la lista simple, node.prev nos da el anterior directamente. */
    @Override
    public void addBefore(DNode<T> node, T key) {
        if (node == null) {
            throw new IllegalArgumentException("El nodo no puede ser null");
        }
        DNode<T> newNode = new DNode<>(key);
        newNode.next = node;
        newNode.prev = node.prev;
        if (node.prev != null) {
            node.prev.next = newNode;
        } else {
            head = newNode;        // node era el primero
        }
        node.prev = newNode;
        size++;
    }

    /** O(1) */
    @Override
    public boolean isEmpty() {
        return head == null;
    }

    /** O(1) */
    @Override
    public int size() {
        return size;
    }

    // ---------------------------------------------------------------
    // Auxiliares privados
    // ---------------------------------------------------------------

    /** O(n): recorre hasta el último nodo. */
    private DNode<T> lastNode() {
        DNode<T> cur = head;
        while (cur.next != null) {
            cur = cur.next;
        }
        return cur;
    }

    /** O(1): conecta al anterior con el siguiente, saltándose node. */
    private void unlink(DNode<T> node) {
        if (node.prev != null) {
            node.prev.next = node.next;
        } else {
            head = node.next;
        }
        if (node.next != null) {
            node.next.prev = node.prev;
        }
        node.next = node.prev = null;
        size--;
    }

    private void checkNotEmpty() {
        if (head == null) {
            throw new NoSuchElementException("La lista está vacía");
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("[");
        DNode<T> cur = head;
        while (cur != null) {
            sb.append(cur.key);
            if (cur.next != null) sb.append(", ");
            cur = cur.next;
        }
        return sb.append("]").toString();
    }
}
