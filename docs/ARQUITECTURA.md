# Arquitectura

```mermaid
flowchart TD
    CLI[Main: consola] --> K[Kernel: PCB y reloj]
    K --> P[Planificador RR: quantum 3]
    K --> M[Memoria: 100 MB y first-fit]
    K --> S[Semáforo: wait y signal]
    K --> R[Recursos exclusivos R1 y R2]
    P --> Q[Cola FIFO de listos]
    S --> B[Cola específica de sección crítica]
    R --> G[Grafo de espera y detección de ciclos]
```

Estados: LISTO → EJECUTANDO → LISTO o TERMINADO. wait/solicitar pueden pasar de LISTO a BLOQUEADO_SEMAFORO o BLOQUEADO_RECURSO. signal/liberar despiertan al primer solicitante y lo devuelven a LISTO. Abortar pasa cualquier proceso activo a ABORTADO y libera sus reservas.

Solo la CPU avanza el reloj. Los comandos representan eventos atómicos entre turnos. El costo temporal de cambio de contexto es cero y cada expiración queda registrada. La memoria es contigua dinámica sin compactación, por lo que puede existir fragmentación externa. El detector cubre los dos recursos exclusivos, no ciclos mixtos con el semáforo.
