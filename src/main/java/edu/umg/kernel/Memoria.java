package edu.umg.kernel;

import java.util.*;

/** RAM simulada: particiones dinámicas contiguas y búsqueda first-fit. */
public final class Memoria {
    public static final int CAPACIDAD = 100;
    public record Bloque(int pid, int inicio, int tamanio) { }
    private final TreeMap<Integer, Bloque> bloques = new TreeMap<>();

    public Bloque reservar(int pid, int tamanio) {
        if (pid <= 0 || tamanio <= 0) throw new IllegalArgumentException("PID y memoria deben ser positivos.");
        if (bloques.values().stream().anyMatch(b -> b.pid() == pid)) throw new IllegalArgumentException("PID ya tiene memoria.");
        int inicio = 0;
        for (Bloque bloque : bloques.values()) {
            if (bloque.inicio() - inicio >= tamanio) break;
            inicio = bloque.inicio() + bloque.tamanio();
        }
        // Se resta antes de comparar para no desbordar con entradas enteras grandes.
        if (tamanio > CAPACIDAD - inicio) throw new IllegalStateException(
            "Out of Memory: se necesitan " + tamanio + " MB contiguos; libres=" + libre() + " MB. Puede existir fragmentación externa.");
        Bloque bloque = new Bloque(pid, inicio, tamanio);
        bloques.put(inicio, bloque);
        return bloque;
    }
    public void liberar(int pid) { bloques.values().removeIf(b -> b.pid() == pid); }
    public int libre() { return CAPACIDAD - bloques.values().stream().mapToInt(Bloque::tamanio).sum(); }
    public List<Bloque> bloques() { return List.copyOf(bloques.values()); }
    @Override public String toString() { return "RAM: " + (CAPACIDAD-libre()) + "/100 MB; libres=" + libre() + " MB; bloques=" + bloques(); }
}
