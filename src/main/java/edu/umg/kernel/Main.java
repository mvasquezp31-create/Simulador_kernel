package edu.umg.kernel;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** CLI interactiva y modo de guion: las mismas validaciones se usan en ambos. */
public final class Main {
    private final Kernel kernel = new Kernel();
    private final PrintStream salida;
    public Main(PrintStream salida) { this.salida=salida; }
    public static void main(String[] args) throws IOException {
        if(args.length>2 || (args.length>0 && (args.length!=2 || !args[0].equals("--script")))) {
            System.err.println("Uso: java -jar simulador-kernel.jar [--script archivo.txt]"); System.exit(2);
        }
        boolean script=args.length==2;
        try(BufferedReader lector=script ? Files.newBufferedReader(Path.of(args[1]),StandardCharsets.UTF_8) : new BufferedReader(new InputStreamReader(System.in,StandardCharsets.UTF_8))) {
            Main consola=new Main(System.out); System.out.println("SIMULADOR KERNEL | RAM 100 MB | Round Robin q=3 | escriba ayuda");
            String linea; while(true) {
                if(!script)System.out.print("kernel> ");
                linea=lector.readLine(); if(linea==null)break;
                if(script)System.out.println("kernel> "+linea);
                if(!consola.comando(linea))break;
            }
        }
    }
    public boolean comando(String linea) {
        String texto=linea.trim(); if(texto.isEmpty() || texto.startsWith("#"))return true;
        String[] p=texto.split("\\s+");
        try {
            switch(p[0].toLowerCase(Locale.ROOT)) {
                case "ayuda" -> { cantidad(p,1); salida.println(AYUDA); }
                case "crear" -> { cantidad(p,4); salida.println("Creado PID="+kernel.crear(p[1],numero(p[2]),numero(p[3])).getPid()); }
                case "paso" -> { cantidad(p,1); salida.println(kernel.paso()); }
                case "ejecutar" -> { if(p.length>2)throw new IllegalArgumentException("Uso: ejecutar [maxTurnos]"); kernel.ejecutar(p.length==2?numero(p[1]):1000).forEach(salida::println); }
                case "estado", "procesos", "memoria" -> { cantidad(p,1); salida.println(kernel.estado()); }
                case "wait" -> { cantidad(p,2); salida.println(kernel.wait(numero(p[1]))?"Permiso concedido.":"Proceso bloqueado en semáforo."); }
                case "signal" -> { cantidad(p,2); kernel.signal(numero(p[1])); salida.println("Permiso liberado/transferido."); }
                case "sumar" -> { cantidad(p,3); kernel.incrementar(numero(p[1]),Long.parseLong(p[2])); salida.println("Variable global="+kernel.variableGlobal()); }
                case "solicitar" -> { cantidad(p,3); salida.println(kernel.solicitar(numero(p[1]),numero(p[2]))?"Recurso concedido.":"Proceso bloqueado por recurso."); alerta(); }
                case "liberar" -> { cantidad(p,3); kernel.liberar(numero(p[1]),numero(p[2])); salida.println("Recurso liberado/transferido."); }
                case "deadlock" -> { cantidad(p,3); kernel.forzarDeadlock(numero(p[1]),numero(p[2])); alerta(); }
                case "detectar" -> { cantidad(p,1); alerta(); }
                case "abortar" -> { cantidad(p,2); kernel.abortar(numero(p[1])); salida.println("Proceso abortado; memoria y recursos liberados."); alerta(); }
                case "bitacora" -> { cantidad(p,1); kernel.eventos().forEach(salida::println); }
                case "salir" -> { cantidad(p,1); return false; }
                default -> throw new IllegalArgumentException("Comando desconocido. Escriba ayuda.");
            }
        } catch(IllegalArgumentException | IllegalStateException | ArithmeticException e) { salida.println("ERROR: "+e.getMessage()); }
        return true;
    }
    private void alerta() { salida.println(kernel.recursos().ciclo().isEmpty()?"Sin deadlock de recursos.":"ALERTA DEADLOCK "+kernel.recursos().ciclo()+"; use abortar PID."); }
    private static int numero(String s) { try{return Integer.parseInt(s);}catch(NumberFormatException e){throw new IllegalArgumentException("Se esperaba un entero válido: "+s);} }
    private static void cantidad(String[] p,int n) { if(p.length!=n)throw new IllegalArgumentException("Cantidad incorrecta de argumentos. Escriba ayuda."); }
    private static final String AYUDA="""
        crear NOMBRE RAM_MB RAFAGA   Crear PCB y reservar RAM (sin espacios en nombre).
        paso                        Ejecutar un turno de hasta 3 unidades.
        ejecutar [MAX_TURNOS]        Ejecutar listos; detener si todos están bloqueados.
        estado | procesos | memoria Mostrar PCB, RAM, colas, recursos y métricas.
        wait PID                    Solicitar entrada a la sección crítica.
        sumar PID VALOR              Modificar variable global con permiso del semáforo.
        signal PID                  Liberar/transferir permiso al primer solicitante.
        solicitar PID RECURSO       Solicitar recurso exclusivo 1 o 2.
        liberar PID RECURSO         Liberar/transferir un recurso.
        deadlock PID_A PID_B         Forzar espera circular con R1 y R2 libres.
        detectar                    Detectar ciclos en el grafo de espera de recursos.
        abortar PID                 Abortar, liberar RAM/recursos y despertar esperas.
        bitacora                    Mostrar los últimos 5000 eventos.
        salir                       Cerrar; todo el estado de simulación está en RAM.
        """;
}
