package edu.umg.kernel;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/** Pruebas ejecutables sin dependencias ni servicios externos. */
public final class Pruebas {
    private static int total;
    private static final List<String> evidencia=new ArrayList<>();
    private static void ok(boolean condicion,String nombre) {
        if(!condicion)throw new AssertionError(nombre);
        total++; System.out.println("OK "+nombre); evidencia.add("OK "+nombre);
    }
    private static void error(Runnable accion,String nombre) {
        try { accion.run(); } catch(IllegalArgumentException | IllegalStateException | ArithmeticException e) {
            ok(true,nombre+" -> "+e.getMessage()); return;
        }
        throw new AssertionError("No se rechazó: "+nombre);
    }
    public static void main(String[] args)throws Exception {
        memoria(); roundRobin(); semaforo(); recursos(); consola(); estres();
        evidencia.add("RESULTADO: "+total+" comprobaciones correctas.");
        Files.createDirectories(Path.of("docs"));
        Files.write(Path.of("docs/evidencias.txt"),evidencia,StandardCharsets.UTF_8);
        System.out.println("PRUEBAS CORRECTAS: "+total);
    }
    private static void memoria() {
        evidencia.add("CASO 1. Out of Memory y recuperación de RAM");
        Kernel k=new Kernel(); Proceso a=k.crear("A",60,5);
        error(()->k.crear("Exceso",41,2),"Rechazo inmediato con RAM insuficiente");
        ok(k.memoria().libre()==40&&k.procesos().size()==1,"OOM no deja PCB ni reserva parcial");
        k.abortar(a.pid); ok(k.memoria().libre()==100,"Abortar devuelve toda la RAM");
        Proceso b=k.crear("B",100,1); ok(b.pid==2,"OOM no consume PID");k.paso();
        ok(b.estado==Estado.TERMINADO&&k.memoria().libre()==100,"Terminar libera RAM");
        error(()->k.crear("X",0,1),"Memoria cero");error(()->k.crear("X",-1,1),"Memoria negativa");
        error(()->k.crear("X",Integer.MAX_VALUE,1),"Memoria extrema sin desbordamiento");
        error(()->k.crear("X",1,0),"Ráfaga cero");error(()->k.crear("X",1,1000001),"Ráfaga fuera de rango");
        error(()->k.crear("",1,1),"Nombre vacío");error(()->k.abortar(999),"PID inexistente");
        Kernel f=new Kernel();Proceso f1=f.crear("F1",30,2);Proceso f2=f.crear("F2",30,2);f.crear("F3",30,2);
        f.abortar(f2.pid);error(()->f.crear("Fragmentado",35,1),"Fragmentación: 40 MB libres pero ningún hueco de 35");
        f.abortar(f1.pid);ok(f.crear("Reutilizar",60,1).memoria==60,"Huecos contiguos se reutilizan con first-fit");
        evidencia.add(f.estado());
    }
    private static void roundRobin() {
        evidencia.add("CASO 2. Round Robin q=3; llegadas simultáneas y costo de contexto cero");
        Kernel k=new Kernel();Proceso a=k.crear("A",10,5),b=k.crear("B",10,4),c=k.crear("C",10,2);
        List<String> traza=k.ejecutar(20);evidencia.addAll(traza);
        ok(traza.size()==5,"RR requiere cinco despachos");
        ok(traza.get(0).contains("[0,3) PID=1")&&traza.get(1).contains("[3,6) PID=2")&&traza.get(2).contains("[6,8) PID=3")&&traza.get(3).contains("[8,10) PID=1")&&traza.get(4).contains("[10,11) PID=2"),"Secuencia exacta A B C A B");
        ok(a.fin==10&&b.fin==11&&c.fin==8,"Tiempos de finalización 10, 11, 8");
        ok(a.espera==5&&b.espera==7&&c.espera==6,"Tiempos de espera 5, 7, 6; promedio 6");
        ok(a.getRespuesta()==0&&b.getRespuesta()==3&&c.getRespuesta()==6,"Tiempos de respuesta 0, 3, 6");
        ok(k.reloj()==11&&k.memoria().libre()==100,"CPU total y liberación de RAM");
        ok(k.paso().contains("Sin procesos listos")&&k.reloj()==11,"CPU sin listos no inventa avance temporal");
        error(()->k.wait(a.pid),"Un proceso terminado no vuelve a ejecutarse");
        error(()->k.ejecutar(0),"Límite de turnos inválido");
        Kernel limitado=new Kernel();limitado.crear("L",1,100);ok(limitado.ejecutar(1).size()==2&&limitado.proceso(1).restante==97,"Ejecución acotada mantiene estado para continuar");
        Kernel llegada=new Kernel();llegada.crear("P",1,6);llegada.paso();Proceso nuevo=llegada.crear("Nuevo",1,1);llegada.ejecutar(10);ok(nuevo.llegada==3&&nuevo.fin==7&&nuevo.espera==3,"Llegadas posteriores usan el reloj vigente");
    }
    private static void semaforo() {
        evidencia.add("CASO 3. Sección crítica y cola FIFO del semáforo");
        // Dos lecturas de 0 seguidas por dos escrituras de 1 perderían una actualización.
        long globalSinProteccion=0;long lecturaA=globalSinProteccion,lecturaB=globalSinProteccion;
        globalSinProteccion=lecturaA+1;globalSinProteccion=lecturaB+1;
        evidencia.add("Intercalado sin protección: leer A=0, leer B=0, escribir A=1, escribir B=1. Resultado="+globalSinProteccion+"; esperado=2.");
        Kernel k=new Kernel();Proceso a=k.crear("A",10,12),b=k.crear("B",10,12),c=k.crear("C",10,12);
        error(()->k.incrementar(a.pid,1),"No se modifica variable global sin permiso");
        ok(k.wait(a.pid),"A obtiene wait");ok(!k.wait(b.pid)&&!k.wait(c.pid),"B y C esperan en cola específica");
        ok(k.planificador().cola().equals(List.of(a.pid))&&k.semaforo().cola().equals(List.of(b.pid,c.pid)),"Bloqueados no permanecen en listos");
        error(()->k.incrementar(b.pid,1),"B bloqueado no escribe en sección crítica");error(()->k.signal(b.pid),"Un no propietario no libera semáforo");
        k.incrementar(a.pid,1);k.signal(a.pid);
        ok(Objects.equals(k.semaforo().propietario(),b.pid)&&b.estado==Estado.LISTO,"signal transfiere a B en orden FIFO");
        k.incrementar(b.pid,1);ok(k.variableGlobal()==2,"Con semáforo se conservan ambas actualizaciones");
        k.abortar(b.pid);ok(Objects.equals(k.semaforo().propietario(),c.pid)&&c.estado==Estado.LISTO,"Abortar propietario despierta al siguiente");
        k.signal(c.pid);ok(k.semaforo().valor()==1,"Semáforo vuelve a libre");
        k.wait(a.pid);error(()->k.wait(a.pid),"wait no reentrante rechazado sin corromper colas");
        k.wait(c.pid);k.abortar(c.pid);ok(k.semaforo().cola().isEmpty(),"Abortar solicitante lo retira de la cola");
        k.incrementar(a.pid,Long.MAX_VALUE-2);error(()->k.incrementar(a.pid,1),"Desbordamiento de variable global rechazado");ok(k.variableGlobal()==Long.MAX_VALUE,"Suma fallida conserva el valor anterior");
        evidencia.add(k.estado());
    }
    private static void recursos() {
        evidencia.add("CASO 4. Detección y resolución de Deadlock");
        Kernel k=new Kernel();Proceso a=k.crear("A",40,5),b=k.crear("B",40,5);
        k.forzarDeadlock(a.pid,b.pid);evidencia.add(k.estado());
        ok(k.recursos().ciclo().equals(List.of(a.pid,b.pid,a.pid)),"Grafo detecta ciclo A -> B -> A");
        ok(k.planificador().cola().isEmpty()&&a.estado==Estado.BLOQUEADO_RECURSO&&b.estado==Estado.BLOQUEADO_RECURSO,"Deadlock bloquea ambos PCB");
        ok(k.paso().contains("DEADLOCK"),"Planificador alerta sin quedar en bucle");
        k.abortar(a.pid);ok(k.recursos().ciclo().isEmpty()&&b.estado==Estado.LISTO,"Abortar A rompe el ciclo y despierta B");
        ok(k.memoria().libre()==60&&k.recursos().propietarios().get(1)==b.pid&&k.recursos().propietarios().get(2)==b.pid,"B recibe R1; RAM de A liberada");
        k.ejecutar(10);ok(b.estado==Estado.TERMINADO&&k.recursos().vacio()&&k.memoria().libre()==100,"El superviviente finaliza y libera todos los recursos");
        evidencia.addAll(k.eventos());
        Kernel f=new Kernel();Proceso p=f.crear("P",5,5),q=f.crear("Q",5,5);
        error(()->f.forzarDeadlock(p.pid,p.pid),"Deadlock requiere dos PID diferentes");ok(f.recursos().vacio(),"Fallo de precondición no reserva recursos parciales");
        error(()->f.solicitar(p.pid,3),"Recurso inexistente");f.solicitar(p.pid,1);
        error(()->f.solicitar(p.pid,1),"No volver a solicitar recurso propio");error(()->f.liberar(q.pid,1),"No liberar recurso ajeno");
        f.solicitar(q.pid,1);ok(f.recursos().ciclo().isEmpty(),"Espera simple no es deadlock");f.abortar(q.pid);ok(f.recursos().solicitudes().isEmpty(),"Abortar elimina solicitudes pendientes");
        f.liberar(p.pid,1);ok(f.recursos().vacio(),"Liberación normal deja recursos libres");
    }
    private static void consola()throws Exception {
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();Main cli=new Main(new PrintStream(bytes,true,StandardCharsets.UTF_8));
        ok(cli.comando("crear\tValido\t10\t3"),"Consola admite tabulaciones");
        for(String c:List.of("crear X no 3","crear X 10","crear X 1 9999999999999","inventado","sumar 1 99999999999999999999999999","paso basura","wait -2"))ok(cli.comando(c),"Consola sobrevive a entrada: "+c);
        ok(bytes.toString(StandardCharsets.UTF_8).contains("Creado PID=1")&&bytes.toString(StandardCharsets.UTF_8).contains("ERROR:"),"Consola imprime éxito y errores recuperables");
        ok(!cli.comando("salir"),"Salida explícita");
    }
    private static void estres() {
        Kernel k=new Kernel();Random random=new Random(20261103);int rechazados=0;
        for(int i=0;i<500;i++) {
            if(random.nextBoolean())try{k.crear("P"+i,1+random.nextInt(101),1+random.nextInt(30));}catch(IllegalStateException e){rechazados++;}
            else k.paso();
            int usados=k.procesos().stream().filter(Proceso::activo).mapToInt(p->p.memoria).sum();
            if(usados!=100-k.memoria().libre()||usados>100||k.planificador().cola().stream().distinct().count()!=k.planificador().cola().size())throw new AssertionError("Invariante de estrés");
        }
        ok(rechazados>0,"Estrés reproducible: 500 operaciones mantienen RAM y cola válidas");k.ejecutar(100000);ok(k.memoria().libre()==100,"Estrés: todos los procesos admitidos finalizan");
    }
}
