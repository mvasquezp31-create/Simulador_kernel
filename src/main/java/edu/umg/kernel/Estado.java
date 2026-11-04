package edu.umg.kernel;

/** Estados visibles del PCB. Los dos bloqueos tienen colas diferentes. */
public enum Estado { LISTO, EJECUTANDO, BLOQUEADO_SEMAFORO, BLOQUEADO_RECURSO, TERMINADO, ABORTADO }
