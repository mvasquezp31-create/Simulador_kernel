package edu.umg.kernel;

import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Reporte con anexos obtenidos de ejecuciones reales, no resultados inventados. */
public final class GenerarReporte {
    public static void main(String[] args) throws Exception {
        List<String> lineas=new ArrayList<>(Files.readAllLines(Path.of("docs/REPORTE_TECNICO.txt"),StandardCharsets.UTF_8));
        for(String archivo:List.of("docs/evidencias.txt","docs/trazas/memoria.txt","docs/trazas/round-robin.txt","docs/trazas/semaforo.txt","docs/trazas/deadlock.txt")){
            Path p=Path.of(archivo);
            if(!Files.exists(p))throw new IllegalStateException("Falta evidencia real: "+archivo+". Ejecute Verificar.ps1.");
            lineas.add("");lineas.add("ANEXO: "+archivo);lineas.addAll(Files.readAllLines(p,StandardCharsets.UTF_8));
        }
        Files.createDirectories(Path.of("entrega"));
        Files.write(Path.of("entrega/Reporte-Tecnico.pdf"),PdfTexto.crear(lineas));
        System.out.println("Reporte PDF generado: entrega/Reporte-Tecnico.pdf");
    }
}
