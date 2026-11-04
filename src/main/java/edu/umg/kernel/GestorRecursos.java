package edu.umg.kernel;

import java.util.*;

/** Dos recursos exclusivos y grafo de espera: solicitante -> propietario. */
public final class GestorRecursos {
    private final Map<Integer, Integer> propietarios = new TreeMap<>();
    private final Map<Integer, Integer> solicitudes = new TreeMap<>();
    private final Map<Integer, Deque<Integer>> colas = Map.of(1, new ArrayDeque<>(), 2, new ArrayDeque<>());
    private void validar(int recurso) { if (recurso != 1 && recurso != 2) throw new IllegalArgumentException("El recurso debe ser 1 o 2."); }
    public boolean solicitar(int pid, int recurso) {
        validar(recurso);
        if (solicitudes.containsKey(pid)) throw new IllegalArgumentException("El proceso ya espera un recurso.");
        if (Objects.equals(propietarios.get(recurso), pid)) throw new IllegalArgumentException("El proceso ya posee ese recurso.");
        if (!propietarios.containsKey(recurso)) { propietarios.put(recurso, pid); return true; }
        solicitudes.put(pid, recurso); colas.get(recurso).addLast(pid); return false;
    }
    public Integer liberar(int pid, int recurso) {
        validar(recurso);
        if (!Objects.equals(propietarios.get(recurso), pid)) throw new IllegalArgumentException("El proceso no posee ese recurso.");
        propietarios.remove(recurso);
        Integer siguiente = colas.get(recurso).pollFirst();
        if (siguiente != null) { solicitudes.remove(siguiente); propietarios.put(recurso, siguiente); }
        return siguiente;
    }
    List<Integer> retirar(int pid) {
        solicitudes.remove(pid); colas.values().forEach(c -> c.remove(pid));
        List<Integer> despiertos = new ArrayList<>();
        for (int recurso : List.of(1, 2)) if (Objects.equals(propietarios.get(recurso), pid)) {
            Integer siguiente = liberar(pid, recurso);
            if (siguiente != null) despiertos.add(siguiente);
        }
        return despiertos;
    }
    /** Sigue aristas del grafo hasta encontrar un nodo repetido (espera circular). */
    public List<Integer> ciclo() {
        for (int inicio : solicitudes.keySet()) {
            List<Integer> ruta = new ArrayList<>(); Integer actual = inicio;
            while (actual != null) {
                int repetido = ruta.indexOf(actual);
                if (repetido >= 0) { List<Integer> ciclo = new ArrayList<>(ruta.subList(repetido, ruta.size())); ciclo.add(actual); return ciclo; }
                ruta.add(actual);
                Integer recurso = solicitudes.get(actual);
                actual = recurso == null ? null : propietarios.get(recurso);
            }
        }
        return List.of();
    }
    public Map<Integer, Integer> propietarios() { return Map.copyOf(propietarios); }
    public Map<Integer, Integer> solicitudes() { return Map.copyOf(solicitudes); }
    public boolean vacio() { return propietarios.isEmpty() && solicitudes.isEmpty(); }
    @Override public String toString() { return "Recursos (recurso=PID): " + propietarios + "; solicitudes (PID=recurso): " + solicitudes; }
}
