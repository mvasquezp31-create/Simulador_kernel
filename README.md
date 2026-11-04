# Simulador de Funciones del Kernel

Proyecto individual de **Marli Xiomara Vásquez Pérez**, Sistemas Operativos I, Universidad Mariano Gálvez de Guatemala.

Java 17+, consola CLI, Round Robin q=3, RAM contigua de 100 MB, semáforo binario, variable compartida y deadlock de dos recursos. No necesita MySQL, servidor web ni librerías externas. Es independiente del proyecto Taller Mecánico.

## Ejecutar

Desde PowerShell en la carpeta del proyecto:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\Ejecutar.ps1
```

Escribe ayuda. Ejemplo:

```text
crear A 10 5
crear B 10 4
crear C 10 2
ejecutar
estado
salir
```

También puede abrirse pom.xml en NetBeans; clase principal: edu.umg.kernel.Main. Los scripts usan directamente el JDK, con JAVA_HOME o el JDK instalado en Program Files/Java. Se verificó con JDK 26.0.2 y compatibilidad Java 17.

## Guiones de demostración

```powershell
powershell -ExecutionPolicy Bypass -File .\Ejecutar.ps1 -Guion ejemplos/round-robin.txt
powershell -ExecutionPolicy Bypass -File .\Ejecutar.ps1 -Guion ejemplos/memoria.txt
powershell -ExecutionPolicy Bypass -File .\Ejecutar.ps1 -Guion ejemplos/semaforo.txt
powershell -ExecutionPolicy Bypass -File .\Ejecutar.ps1 -Guion ejemplos/deadlock.txt
```

Cada comando abre una simulación nueva. Los errores de los guiones memoria/semaforo son intencionales para demostrar validación.

## Probar y generar el reporte

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\Verificar.ps1
java -jar .\target\simulador-kernel.jar
```

Verificar.ps1 compila, ejecuta **65 comprobaciones**, realiza una prueba de estrés con **500 operaciones**, genera transcripciones reales y el reporte PDF, y crea un punto de guardado con SHA-256. No necesita descargas ni servicios externos. Maven también permite compilar y empaquetar, pero las pruebas sin frameworks se ejecutan con Verificar.ps1.

## Documentación

- [Reporte técnico PDF](entrega/Reporte-Tecnico.pdf)
- [Fuente editable del reporte](docs/REPORTE_TECNICO.txt)
- [Evidencias de pruebas](docs/evidencias.txt)
- [Arquitectura](docs/ARQUITECTURA.md)
- [Guion individual de 15 minutos](docs/DEMOSTRACION.md)
- [Cobertura del enunciado](docs/REQUISITOS.md)

Completa carné y sección en el reporte editable y ejecuta Verificar.ps1 para regenerar el PDF. Repositorio: https://github.com/mvasquezp31-create/Simulador_kernel.

## Continuación y límites

Estado: ESTADO.md. Para continuar una verificación interrumpida, repite Verificar.ps1. Si falta terminar la creación de un archivo, solicita en este chat retomar desde ESTADO.md conservando los cambios actuales. Los generadores de creación se archivan en herramientas y no deben ejecutarse encima de modificaciones posteriores.

El estado simulado es volátil. Se usan MB enteros, nombres sin espacios, ráfagas de 1 a 1000000 y un límite configurable de turnos. Solo la CPU avanza el reloj; cambio de contexto con costo temporal cero. Los bloqueados conservan RAM. Se usa first-fit sin compactación. Se detectan ciclos R1/R2, no ciclos mixtos con semáforo. La bitácora conserva los últimos 5000 eventos; los PCB finalizados permanecen como histórico de la sesión.

El documento menciona cinco componentes pero lista A–D. La modalidad individual fue indicada por la autora, aunque el enunciado pide presentación grupal. Conviene confirmar esas dos inconsistencias con el docente.
