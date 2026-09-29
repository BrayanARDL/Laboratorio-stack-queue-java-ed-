package pruebas;

import listas.*;
import pilacola.*;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.function.Supplier;

/**
 * Pruebas de correctitud (NO miden tiempo).
 *
 * Cada estructura se somete a miles de operaciones aleatorias y, después de cada
 * una, se compara contra una lista de referencia de Java (java.util.ArrayList).
 * java.util se usa SOLO aquí, como "oráculo" para verificar; las estructuras
 * del taller no dependen de ninguna librería.
 *
 * Ejecutar:  java -cp out pruebas.Pruebas
 */
public class Pruebas {

    private static final int OPERACIONES = 200_000;
    private static final Random RND = new Random(12345);
    private static int fallos = 0;

    public static void main(String[] args) {
        ejemploManual();

        probarLista("SinglyLinkedList (sin cola)", SinglyLinkedList::new);
        probarLista("SinglyLinkedListTail (con cola)", SinglyLinkedListTail::new);
        probarLista("DoublyLinkedList (sin cola)", DoublyLinkedList::new);
        probarLista("DoublyLinkedListTail (con cola)", DoublyLinkedListTail::new);

        probarPila("ArrayStack (duplicando)", () -> new ArrayStack<>(1));
        probarPila("ArrayStack (incremento fijo +3)", () -> new ArrayStack<>(1, 3));
        probarCola("ArrayQueue (duplicando)", () -> new ArrayQueue<>(1));
        probarCola("ArrayQueue (incremento fijo +3)", () -> new ArrayQueue<>(1, 3));

        probarCopias();

        System.out.println();
        System.out.println(fallos == 0 ? "TODAS LAS PRUEBAS PASARON" : ("FALLOS: " + fallos));
        if (fallos > 0) System.exit(1);
    }

    // ------------------------------------------------------------------
    // Ejemplo pequeño y legible
    // ------------------------------------------------------------------
    private static void ejemploManual() {
        System.out.println("=== Ejemplo manual ===");
        DoublyLinkedListTail<Integer> l = new DoublyLinkedListTail<>();
        l.pushFront(2); l.pushFront(1); l.pushBack(3); l.pushBack(4);
        System.out.println("pushFront 2,1 / pushBack 3,4      -> " + l);
        DNode<Integer> n3 = l.find(3);
        l.addBefore(n3, 99); l.addAfter(n3, 77);
        System.out.println("addBefore(3,99) / addAfter(3,77)  -> " + l);
        l.erase(99);
        System.out.println("erase(99)                         -> " + l);
        System.out.println("popFront=" + l.popFront() + " popBack=" + l.popBack() + "           -> " + l);

        ArrayQueue<Integer> q = new ArrayQueue<>(4);
        for (int i = 1; i <= 4; i++) q.enqueue(i);
        q.dequeue(); q.dequeue(); q.enqueue(5); q.enqueue(6);   // 5 y 6 "dan la vuelta" al arreglo
        System.out.println("Cola tras dar la vuelta           -> " + q + " (capacidad " + q.capacity() + ")");
        q.enqueue(7);                                             // se llena -> crece a 8
        System.out.println("enqueue(7) con arreglo lleno      -> " + q + " (capacidad " + q.capacity() + ")");

        ArrayStack<Integer> s = new ArrayStack<>();
        for (int i = 1; i <= 5; i++) s.push(i * 10);
        s.delete(20);
        System.out.println("Pila 10..50 con delete(20)        -> " + s + "  peek=" + s.peek());
        System.out.println();
    }

    // ------------------------------------------------------------------
    // Listas enlazadas
    // ------------------------------------------------------------------
    private static <N> void probarLista(String nombre, Supplier<LinkedListADT<Integer, N>> fabrica) {
        LinkedListADT<Integer, N> lista = fabrica.get();
        List<Integer> ref = new ArrayList<>();

        // Operaciones sobre la lista vacía deben lanzar excepción
        esperarExcepcion(nombre + " popFront vacía", lista::popFront);
        esperarExcepcion(nombre + " popBack vacía", lista::popBack);
        esperarExcepcion(nombre + " topFront vacía", lista::topFront);
        esperarExcepcion(nombre + " topBack vacía", lista::topBack);

        for (int op = 0; op < OPERACIONES; op++) {
            int v = RND.nextInt(50);                 // rango pequeño => muchos repetidos
            int tipo = RND.nextInt(ref.size() > 300 ? 12 : 9); // si crece mucho, favorecer borrados
            switch (tipo) {
                case 0 -> { lista.pushFront(v); ref.add(0, v); }
                case 1 -> { lista.pushBack(v); ref.add(v); }
                case 2 -> { if (!ref.isEmpty()) verificar(nombre, "popFront", ref.remove(0), lista.popFront()); }
                case 3, 9 -> { if (!ref.isEmpty()) verificar(nombre, "popBack", ref.remove(ref.size() - 1), lista.popBack()); }
                case 4 -> {
                    if (!ref.isEmpty()) {
                        verificar(nombre, "topFront", ref.get(0), lista.topFront());
                        verificar(nombre, "topBack", ref.get(ref.size() - 1), lista.topBack());
                    }
                }
                case 5 -> {
                    N nodo = lista.find(v);
                    verificar(nombre, "find existe", ref.contains(v), nodo != null);
                }
                case 6, 10, 11 -> verificar(nombre, "erase", ref.remove(Integer.valueOf(v)), lista.erase(v));
                case 7 -> {
                    int k = RND.nextInt(50);
                    N nodo = lista.find(k);
                    if (nodo != null) {
                        lista.addAfter(nodo, v);
                        ref.add(ref.indexOf(k) + 1, v);
                    }
                }
                case 8 -> {
                    int k = RND.nextInt(50);
                    N nodo = lista.find(k);
                    if (nodo != null) {
                        lista.addBefore(nodo, v);
                        ref.add(ref.indexOf(k), v);
                    }
                }
            }
            verificar(nombre, "size", ref.size(), lista.size());
            verificar(nombre, "isEmpty", ref.isEmpty(), lista.isEmpty());
            if (op % 97 == 0) verificar(nombre, "contenido", ref.toString(), lista.toString());
        }
        // Vaciar alternando extremos: comprueba que head/tail queden consistentes
        while (!ref.isEmpty()) {
            if (RND.nextBoolean()) verificar(nombre, "vaciar popFront", ref.remove(0), lista.popFront());
            else verificar(nombre, "vaciar popBack", ref.remove(ref.size() - 1), lista.popBack());
        }
        lista.pushBack(1); lista.pushFront(0);                  // debe funcionar tras vaciarse
        verificar(nombre, "reutilizar", "[0, 1]", lista.toString());
        System.out.printf("OK  %-40s %,d operaciones aleatorias%n", nombre, OPERACIONES);
    }

    // ------------------------------------------------------------------
    // Pila: la cima es el final de la lista de referencia
    // ------------------------------------------------------------------
    private static void probarPila(String nombre, Supplier<ArrayStack<Integer>> fabrica) {
        ArrayStack<Integer> pila = fabrica.get();
        List<Integer> ref = new ArrayList<>();
        esperarExcepcion(nombre + " pop vacía", pila::pop);
        esperarExcepcion(nombre + " peek vacía", pila::peek);

        for (int op = 0; op < OPERACIONES; op++) {
            int v = RND.nextInt(50);
            int tipo = RND.nextInt(ref.size() > 300 ? 6 : 4);
            switch (tipo) {
                case 0 -> { pila.push(v); ref.add(v); }
                case 1, 4 -> { if (!ref.isEmpty()) verificar(nombre, "pop", ref.remove(ref.size() - 1), pila.pop()); }
                case 2 -> { if (!ref.isEmpty()) verificar(nombre, "peek", ref.get(ref.size() - 1), pila.peek()); }
                case 3, 5 -> {
                    int i = ref.lastIndexOf(v);         // "primer valor que encuentra" desde la cima
                    if (i >= 0) ref.remove(i);
                    verificar(nombre, "delete", i >= 0, pila.delete(v));
                }
            }
            verificar(nombre, "size", ref.size(), pila.size());
            verificar(nombre, "isEmpty", ref.isEmpty(), pila.isEmpty());
            if (op % 97 == 0) verificar(nombre, "contenido", ref.toString(), pila.toString());
        }
        System.out.printf("OK  %-40s %,d operaciones aleatorias%n", nombre, OPERACIONES);
    }

    // ------------------------------------------------------------------
    // Cola: el frente es el inicio de la lista de referencia
    // ------------------------------------------------------------------
    private static void probarCola(String nombre, Supplier<ArrayQueue<Integer>> fabrica) {
        ArrayQueue<Integer> cola = fabrica.get();
        List<Integer> ref = new ArrayList<>();
        esperarExcepcion(nombre + " dequeue vacía", cola::dequeue);
        esperarExcepcion(nombre + " front vacía", cola::front);

        for (int op = 0; op < OPERACIONES; op++) {
            int v = RND.nextInt(50);
            int tipo = RND.nextInt(ref.size() > 300 ? 6 : 4);
            switch (tipo) {
                case 0 -> { cola.enqueue(v); ref.add(v); }
                case 1, 4 -> { if (!ref.isEmpty()) verificar(nombre, "dequeue", ref.remove(0), cola.dequeue()); }
                case 2 -> { if (!ref.isEmpty()) verificar(nombre, "front", ref.get(0), cola.front()); }
                case 3, 5 -> {
                    int i = ref.indexOf(v);
                    if (i >= 0) ref.remove(i);
                    verificar(nombre, "delete", i >= 0, cola.delete(v));
                }
            }
            verificar(nombre, "size", ref.size(), cola.size());
            verificar(nombre, "isEmpty", ref.isEmpty(), cola.isEmpty());
            if (op % 97 == 0) verificar(nombre, "contenido", ref.toString(), cola.toString());
        }
        System.out.printf("OK  %-40s %,d operaciones aleatorias%n", nombre, OPERACIONES);
    }

    /** Los constructores de copia deben producir estructuras independientes. */
    private static void probarCopias() {
        ArrayQueue<Integer> q = new ArrayQueue<>(4);
        for (int i = 0; i < 4; i++) q.enqueue(i);
        q.dequeue(); q.enqueue(4);                       // contenido con vuelta: [1,2,3,4]
        ArrayQueue<Integer> copia = new ArrayQueue<>(q);
        copia.dequeue();
        verificar("copia cola", "original intacto", "[1, 2, 3, 4]", q.toString());
        verificar("copia cola", "copia", "[2, 3, 4]", copia.toString());

        ArrayStack<Integer> s = new ArrayStack<>();
        for (int i = 0; i < 5; i++) s.push(i);
        ArrayStack<Integer> copiaS = new ArrayStack<>(s);
        copiaS.pop();
        verificar("copia pila", "original intacto", 5, s.size());
        verificar("copia pila", "copia", 4, copiaS.size());
        System.out.printf("OK  %-40s%n", "Constructores de copia");
    }

    // ------------------------------------------------------------------
    // Utilidades
    // ------------------------------------------------------------------
    private static void verificar(String estructura, String que, Object esperado, Object obtenido) {
        if (!java.util.Objects.equals(esperado, obtenido)) {
            fallos++;
            if (fallos <= 10) {
                System.out.printf("FALLO %s [%s]: esperado=%s obtenido=%s%n", estructura, que, esperado, obtenido);
            }
            if (fallos == 10) System.out.println("... (se omiten más fallos)");
        }
    }

    private static void esperarExcepcion(String que, Runnable accion) {
        try {
            accion.run();
            fallos++;
            System.out.println("FALLO: se esperaba excepción en " + que);
        } catch (NoSuchElementException e) {
            // correcto
        }
    }
}
