package edu.umg.kernel;

import java.util.*;

/** Cola FIFO para Round Robin. El reloj y la ejecución pertenecen al núcleo. */
public final class Planificador {
    public static final int QUANTUM = 3;
    private final Deque<Integer> listos = new ArrayDeque<>();
    void agregar(int pid) { if (!listos.contains(pid)) listos.addLast(pid); }
    Integer siguiente() { return listos.pollFirst(); }
    void retirar(int pid) { listos.remove(pid); }
    public List<Integer> cola() { return List.copyOf(listos); }
}
