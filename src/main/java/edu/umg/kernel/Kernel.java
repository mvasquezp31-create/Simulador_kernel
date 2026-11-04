package edu.umg.kernel;

import java.util.*;

/** Coordinador del simulador. Cada comando es un evento atómico simulado.
 * No ejecuta procesos del equipo ni administra su memoria física.
 */
public final class Kernel {
    private final Map<Integer, Proceso> procesos = new LinkedHashMap<>();
    private final Memoria memoria = new Memoria();
    private final Planificador planificador = new Planificador();
    private final SemaforoBinario semaforo = new SemaforoBinario();
    private final GestorRecursos recursos = new GestorRecursos();
    private final Deque<String> eventos = new ArrayDeque<>();
    private int siguientePid = 1;
    private long reloj, variableGlobal, despachos, cambiosContexto;
    private Integer ultimoPid;

    private void registrar(String texto) {
        if (eventos.size() == 5000) eventos.removeFirst();
        eventos.addLast("t=" + reloj + " " + texto);
    }
    private Proceso activo(int pid) {
        Proceso p = proceso(pid);
        if (!p.activo()) throw new IllegalArgumentException("El proceso ya terminó o fue abortado.");
        return p;
    }
    private Proceso listo(int pid) {
        Proceso p = activo(pid);
        if (p.estado != Estado.LISTO) throw new IllegalArgumentException("El proceso está bloqueado y no puede efectuar esa operación.");
        return p;
    }
    public Proceso crear(String nombre, int ram, int rafaga) {
        if (nombre == null || !nombre.matches("[\\p{L}\\p{N}_-]{1,30}")) throw new IllegalArgumentException("Nombre: 1 a 30 letras, números, _ o -.");
        if (rafaga < 1 || rafaga > 1000000) throw new IllegalArgumentException("Ráfaga: 1 a 1000000 unidades.");
        if (siguientePid == Integer.MAX_VALUE) throw new IllegalStateException("Se agotaron los PID.");
        // Reservar primero: un rechazo no consume PID ni deja un PCB parcial.
        memoria.reservar(siguientePid, ram);
        Proceso p = new Proceso(siguientePid++, nombre, ram, rafaga, reloj);
        procesos.put(p.pid, p); planificador.agregar(p.pid);
        registrar("CREAR PID=" + p.pid + " nombre=" + nombre + " RAM=" + ram + " CPU=" + rafaga);
        return p;
    }
    public String paso() {
        Integer pid = planificador.siguiente();
        if (pid == null) {
            String mensaje = recursos.ciclo().isEmpty() ? "Sin procesos listos; revise bloqueados o cree un proceso." : "DEADLOCK " + recursos.ciclo() + "; use abortar PID.";
            registrar(mensaje); return mensaje;
        }
        Proceso p = activo(pid); p.estado = Estado.EJECUTANDO;
        if (p.primeraEjecucion < 0) p.primeraEjecucion = reloj;
        if (ultimoPid != null && !ultimoPid.equals(pid)) cambiosContexto++;
        ultimoPid = pid; despachos++;
        int consumo = Math.min(Planificador.QUANTUM, p.restante); long inicio = reloj;
        // Solo los procesos listos acumulan espera; los bloqueados acumulan otro contador.
        for (Proceso otro : procesos.values()) if (otro.pid != pid && otro.activo()) {
            if (otro.estado == Estado.LISTO) otro.espera += consumo;
            else otro.bloqueo += consumo;
        }
        reloj += consumo; p.restante -= consumo;
        String turno = "CPU [" + inicio + "," + reloj + ") PID=" + pid + " restante=" + p.restante;
        registrar(turno);
        if (p.restante == 0) cerrar(p, Estado.TERMINADO);
        else { p.estado = Estado.LISTO; planificador.agregar(pid); registrar("FIN QUANTUM: guardar contexto PID=" + pid + " y enviar al final de listos"); }
        return turno;
    }
    public List<String> ejecutar(int maxTurnos) {
        if (maxTurnos < 1 || maxTurnos > 100000) throw new IllegalArgumentException("Turnos: 1 a 100000.");
        List<String> traza = new ArrayList<>();
        for (int i=0; i<maxTurnos && !planificador.cola().isEmpty(); i++) traza.add(paso());
        if (!planificador.cola().isEmpty()) traza.add("Límite de turnos alcanzado; puede continuar con ejecutar.");
        else if (procesos.values().stream().anyMatch(Proceso::activo)) traza.add("Quedan procesos bloqueados. Deadlock: " + recursos.ciclo());
        return traza;
    }
    public boolean wait(int pid) {
        Proceso p = listo(pid); boolean permiso = semaforo.wait(pid);
        if (!permiso) { p.estado = Estado.BLOQUEADO_SEMAFORO; planificador.retirar(pid); }
        registrar("WAIT PID=" + pid + (permiso ? " entra a sección crítica" : " BLOQUEADO en cola de semáforo"));
        return permiso;
    }
    public void signal(int pid) {
        activo(pid); Integer siguiente = semaforo.signal(pid); despertar(siguiente);
        registrar("SIGNAL PID=" + pid + " nuevo propietario=" + siguiente);
    }
    public void incrementar(int pid, long incremento) {
        listo(pid);
        if (!Objects.equals(semaforo.propietario(), pid)) throw new IllegalArgumentException("Ejecute wait PID antes de modificar la variable global.");
        long nuevo = Math.addExact(variableGlobal, incremento);
        variableGlobal = nuevo; registrar("SECCIÓN CRÍTICA PID=" + pid + " variableGlobal=" + variableGlobal);
    }
    public boolean solicitar(int pid, int recurso) {
        Proceso p = listo(pid); boolean concedido = recursos.solicitar(pid, recurso);
        if (!concedido) { p.estado = Estado.BLOQUEADO_RECURSO; planificador.retirar(pid); }
        registrar("SOLICITAR PID=" + pid + " R" + recurso + (concedido ? " concedido" : " BLOQUEADO"));
        if (!recursos.ciclo().isEmpty()) registrar("ALERTA DEADLOCK: " + recursos.ciclo());
        return concedido;
    }
    public void liberar(int pid, int recurso) { activo(pid); despertar(recursos.liberar(pid, recurso)); registrar("LIBERAR PID=" + pid + " R" + recurso); }
    public void forzarDeadlock(int a, int b) {
        listo(a); listo(b);
        if (a == b || !recursos.vacio()) throw new IllegalArgumentException("Seleccione dos PID diferentes y ambos recursos libres.");
        solicitar(a,1); solicitar(b,2); solicitar(a,2); solicitar(b,1);
    }
    public void abortar(int pid) { cerrar(activo(pid), Estado.ABORTADO); }
    private void cerrar(Proceso p, Estado estado) {
        p.estado = estado; p.fin = reloj; planificador.retirar(p.pid);
        memoria.liberar(p.pid);
        despertar(semaforo.retirar(p.pid));
        recursos.retirar(p.pid).forEach(this::despertar);
        registrar(estado + " PID=" + p.pid + ": RAM y recursos liberados");
    }
    private void despertar(Integer pid) {
        if (pid != null) { Proceso p = activo(pid); p.estado = Estado.LISTO; planificador.agregar(pid); registrar("DESPERTAR PID=" + pid); }
    }
    public Proceso proceso(int pid) { Proceso p=procesos.get(pid); if(p==null)throw new IllegalArgumentException("PID inexistente: " + pid); return p; }
    public List<Proceso> procesos() { return List.copyOf(procesos.values()); }
    public List<String> eventos() { return List.copyOf(eventos); }
    public Memoria memoria() { return memoria; }
    public Planificador planificador() { return planificador; }
    public SemaforoBinario semaforo() { return semaforo; }
    public GestorRecursos recursos() { return recursos; }
    public long reloj() { return reloj; }
    public long variableGlobal() { return variableGlobal; }
    public String estado() {
        StringBuilder s=new StringBuilder("Reloj=" + reloj + " q=3 despachos=" + despachos + " cambios entre PID=" + cambiosContexto + "\n");
        s.append(memoria).append("\nListos: ").append(planificador.cola()).append('\n').append(semaforo).append('\n').append(recursos).append("\nVariable global=").append(variableGlobal).append("\nDeadlock: ").append(recursos.ciclo()).append('\n');
        procesos.values().forEach(p->s.append(p).append('\n')); return s.toString();
    }
}
