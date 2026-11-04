$ErrorActionPreference='Stop'
$faseKernel='compilación'
Push-Location $PSScriptRoot
try {
    Add-Content ESTADO.md -Encoding UTF8 -Value "EN CURSO: $faseKernel. Continuar con Verificar.ps1."
    & (Join-Path $PSScriptRoot 'Compilar.ps1')
    $faseKernel='pruebas'
    $pruebasKernel=@(Get-ChildItem src/test/java -Recurse -Filter '*.java' | ForEach-Object {$_.FullName})
    New-Item -ItemType Directory -Path target/test-classes -Force | Out-Null
    & javac '--release' '17' '-encoding' 'UTF-8' '-cp' 'target/classes' '-d' 'target/test-classes' @pruebasKernel
    if($LASTEXITCODE -ne 0){throw 'Falló la compilación de pruebas.'}
    & java '-Dstdout.encoding=UTF-8' '-Dstderr.encoding=UTF-8' '-cp' 'target/classes;target/test-classes' 'edu.umg.kernel.Pruebas'
    if($LASTEXITCODE -ne 0){throw 'Fallaron las pruebas.'}
    $faseKernel='transcripciones CLI'
    New-Item -ItemType Directory -Path docs/trazas -Force | Out-Null
    $encodingAnteriorKernel=$OutputEncoding
    $consolaAnteriorKernel=[Console]::OutputEncoding
    try {
        $OutputEncoding=[Text.UTF8Encoding]::new($false)
        [Console]::OutputEncoding=[Text.UTF8Encoding]::new($false)
        foreach($ejemploKernel in @('memoria','round-robin','semaforo','deadlock')){
            & java '-Dstdout.encoding=UTF-8' '-Dstderr.encoding=UTF-8' '-jar' target/simulador-kernel.jar '--script' "ejemplos/$ejemploKernel.txt" | Set-Content "docs/trazas/$ejemploKernel.txt" -Encoding UTF8
            if($LASTEXITCODE -ne 0){throw "Falló el guion $ejemploKernel"}
        }
    } finally {$OutputEncoding=$encodingAnteriorKernel;[Console]::OutputEncoding=$consolaAnteriorKernel}
    $faseKernel='reporte PDF'
    & java '-Dstdout.encoding=UTF-8' '-cp' 'target/classes' 'edu.umg.kernel.GenerarReporte'
    if($LASTEXITCODE -ne 0){throw 'Falló la generación del reporte.'}
    $faseKernel='punto de guardado'
    $respaldoKernel=Join-Path $PSScriptRoot ('puntos_guardado\verificado-'+(Get-Date -Format 'yyyyMMdd-HHmmss'))
    New-Item -ItemType Directory -Path $respaldoKernel -Force | Out-Null
    Copy-Item -LiteralPath src,docs,ejemplos,entrega -Destination $respaldoKernel -Recurse
    Get-ChildItem -LiteralPath $PSScriptRoot -File | Copy-Item -Destination $respaldoKernel
    @('# Estado del proyecto','','Implementación y verificación COMPLETADAS.','Fase 1: módulos Java y CLI completos.','Fase 2: 65 comprobaciones y estrés de 500 operaciones correctos.','Fase 3: reporte PDF y transcripciones completas.','Fase 4: JAR y punto de guardado creados.','','Ejecutar: powershell -ExecutionPolicy Bypass -File .\Ejecutar.ps1','Continuar/verificar: powershell -ExecutionPolicy Bypass -File .\Verificar.ps1','','Pendientes: completar carné/sección y realizar demostración individual en vivo.',"Punto de guardado: $respaldoKernel") | Set-Content ESTADO.md -Encoding UTF8
    Copy-Item -LiteralPath ESTADO.md -Destination $respaldoKernel -Force
    Get-ChildItem -LiteralPath $respaldoKernel -Recurse -File | Get-FileHash -Algorithm SHA256 | ForEach-Object {$_.Hash+'  '+$_.Path.Substring($respaldoKernel.Length+1)} | Set-Content "$respaldoKernel\SHA256.txt" -Encoding UTF8
    Write-Output 'Verificación completa. Reporte: entrega/Reporte-Tecnico.pdf'
} catch {
    Add-Content ESTADO.md -Encoding UTF8 -Value "DETENIDO en fase $faseKernel. $($_.Exception.Message). Continuar con Verificar.ps1."
    throw
} finally {Pop-Location}
