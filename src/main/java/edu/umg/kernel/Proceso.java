package edu.umg.kernel;

/** PCB: la consola puede consultarlo, pero solo el núcleo modifica sus campos. */
public final class Proceso {
    final int pid, memoria, rafagaTotal;
    final String nombre;
    final long llegada;
    int restante;
    Estado estado = Estado.LISTO;
    long espera, bloqueo, primeraEjecucion = -1, fin = -1;

    Proceso(int pid, String nombre, int memoria, int rafaga, long llegada) {
        this.pid = pid; this.nombre = nombre; this.memoria = memoria;
        this.rafagaTotal = rafaga; this.restante = rafaga; this.llegada = llegada;
    }
    public int getPid() { return pid; }
    public Estado getEstado() { return estado; }
    public int getRestante() { return restante; }
    public int getMemoria() { return memoria; }
    public int getRafagaTotal() { return rafagaTotal; }
    public long getEspera() { return espera; }
    public long getBloqueo() { return bloqueo; }
    public long getFin() { return fin; }
    public long getLlegada() { return llegada; }
    public long getRespuesta() { return primeraEjecucion < 0 ? -1 : primeraEjecucion - llegada; }
    boolean activo() { return estado != Estado.TERMINADO && estado != Estado.ABORTADO; }
    @Override public String toString() {
        return String.format("%3d %-15s %-20s RAM=%3d MB CPU=%d/%d espera=%d bloqueo=%d respuesta=%d fin=%d",
            pid, nombre, estado, memoria, restante, rafagaTotal, espera, bloqueo, getRespuesta(), fin);
    }
}
