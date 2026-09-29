package listas;

/**
 * Lista simplemente enlazada CON cola (guarda head y tail).
 *
 *   head -> [a] -> [b] -> [c] -> null
 *                           ^
 *                          tail
 *
 * Gracias a tail, pushBack y topBack pasan a O(1).
 * popBack sigue siendo O(n): para quitar el último hay que dejar a tail
 * apuntando al PENÚLTIMO, y un nodo simple no sabe quién lo antecede.
 */
public class SinglyLinkedListTail<T> implements LinkedListADT<T, SNode<T>> {

    private SNode<T> head;
    private SNode<T> tail;
    private int size;

    /** O(1) */
    @Override
    public void pushFront(T key) {
        SNode<T> node = new SNode<>(key);
        node.next = head;
        head = node;
        if (tail == null) {        // la lista estaba vacía
            tail = node;
        }
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
        if (head == null) {        // quedó vacía
            tail = null;
        }
        size--;
        return key;
    }

    /** O(1): se enlaza directamente después de tail. */
    @Override
    public void pushBack(T key) {
        SNode<T> node = new SNode<>(key);
        if (tail == null) {
            head = tail = node;
        } else {
            tail.next = node;
            tail = node;
        }
        size++;
    }

    /** O(1) */
    @Override
    public T topBack() {
        checkNotEmpty();
        return tail.key;
    }

    /** O(n): hay que encontrar el penúltimo para convertirlo en el nuevo tail. */
    @Override
    public T popBack() {
        checkNotEmpty();
        T key = tail.key;
        if (head == tail) {        // un solo elemento
            head = tail = null;
        } else {
            SNode<T> cur = head;
            while (cur.next != tail) {
                cur = cur.next;
            }
            cur.next = null;
            tail = cur;
        }
        size--;
        return key;
    }

    /** O(n) */
    @Override
    public SNode<T> find(T key) {
        SNode<T> cur = head;
        while (cur != null) {
            if (iguales(cur.key, key)) {
                return cur;
            }
            cur = cur.next;
        }
        return null;
    }

    /** O(n): igual que sin cola, pero si se borra el último hay que actualizar tail. */
    @Override
    public boolean erase(T key) {
        if (head == null) {
            return false;
        }
        if (iguales(head.key, key)) {
            head = head.next;
            if (head == null) {
                tail = null;
            }
            size--;
            return true;
        }
        SNode<T> prev = head;
        while (prev.next != null && !iguales(prev.next.key, key)) {
            prev = prev.next;
        }
        if (prev.next == null) {
            return false;
        }
        if (prev.next == tail) {   // se borra el último
            tail = prev;
        }
        prev.next = prev.next.next;
        size--;
        return true;
    }

    /** O(1): si se inserta después del último, el nuevo nodo pasa a ser tail. */
    @Override
    public void addAfter(SNode<T> node, T key) {
        if (node == null) {
            throw new IllegalArgumentException("El nodo no puede ser null");
        }
        SNode<T> newNode = new SNode<>(key);
        newNode.next = node.next;
        node.next = newNode;
        if (node == tail) {
            tail = newNode;
        }
        size++;
    }

    /** O(n): igual que sin cola, hay que buscar el nodo anterior. */
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

    /**
     * Compara dos claves admitiendo null: son iguales si son la misma
     * referencia o si a.equals(b).
     */
    private static boolean iguales(Object a, Object b) {
        return a == b || (a != null && a.equals(b));
    }

    private void checkNotEmpty() {
        if (head == null) {
            throw new IllegalStateException("La lista está vacía");
        }
    }

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
