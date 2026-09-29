package listas;

import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * Lista doblemente enlazada CON cola (guarda head y tail).
 *
 *   null <- [a] <-> [b] <-> [c] -> null
 *            ^               ^
 *           head            tail
 *
 * Es la única de las cuatro en la que TODAS las operaciones en los extremos
 * son O(1), incluido popBack: tail.prev es el nuevo último.
 */
public class DoublyLinkedListTail<T> implements LinkedListADT<T, DNode<T>> {

    private DNode<T> head;
    private DNode<T> tail;
    private int size;

    /** O(1) */
    @Override
    public void pushFront(T key) {
        DNode<T> node = new DNode<>(key);
        node.next = head;
        if (head != null) {
            head.prev = node;
        } else {
            tail = node;           // la lista estaba vacía
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
        DNode<T> first = head;
        unlink(first);
        return first.key;
    }

    /** O(1) */
    @Override
    public void pushBack(T key) {
        DNode<T> node = new DNode<>(key);
        node.prev = tail;
        if (tail != null) {
            tail.next = node;
        } else {
            head = node;
        }
        tail = node;
        size++;
    }

    /** O(1) */
    @Override
    public T topBack() {
        checkNotEmpty();
        return tail.key;
    }

    /** O(1): tail.prev pasa a ser el nuevo tail, sin recorrer nada. */
    @Override
    public T popBack() {
        checkNotEmpty();
        DNode<T> last = tail;
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

    /** O(n) por la búsqueda; el desenlace es O(1). */
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
        } else {
            tail = newNode;        // node era el último
        }
        node.next = newNode;
        size++;
    }

    /** O(1) */
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

    /** O(1): desenlaza node actualizando head/tail si es un extremo. */
    private void unlink(DNode<T> node) {
        if (node.prev != null) {
            node.prev.next = node.next;
        } else {
            head = node.next;
        }
        if (node.next != null) {
            node.next.prev = node.prev;
        } else {
            tail = node.prev;
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
