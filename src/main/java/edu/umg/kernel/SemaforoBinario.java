package edu.umg.kernel;

import java.util.*;

/** Semáforo binario simulado con propietario y entrega FIFO del permiso.
 * wait(pid) modela wait(); signal(pid) modela signal(). No bloquea hilos reales.
 */
public final class SemaforoBinario {
    private Integer propietario;
    private final Deque<Integer> espera = new ArrayDeque<>();
    public boolean wait(int pid) {
        if (Objects.equals(propietario, pid)) throw new IllegalArgumentException("El semáforo no es reentrante.");
        if (espera.contains(pid)) throw new IllegalArgumentException("El proceso ya espera el semáforo.");
        if (propietario == null) { propietario = pid; return true; }
        espera.addLast(pid);
        return false;
    }
    public Integer signal(int pid) {
        if (!Objects.equals(propietario, pid)) throw new IllegalArgumentException("Solo el propietario puede ejecutar signal.");
        propietario = espera.pollFirst();
        return propietario;
    }
    Integer retirar(int pid) {
        espera.remove(pid);
        return Objects.equals(propietario, pid) ? signal(pid) : null;
    }
    public Integer propietario() { return propietario; }
    public List<Integer> cola() { return List.copyOf(espera); }
    public int valor() { return propietario == null ? 1 : 0; }
    @Override public String toString() { return "Semáforo=" + valor() + " propietario=" + propietario + " espera=" + cola(); }
}
