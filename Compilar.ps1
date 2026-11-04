$ErrorActionPreference='Stop'
$jdkKernel=$null
if($env:JAVA_HOME -and (Test-Path -LiteralPath "$env:JAVA_HOME\bin\jar.exe")){$jdkKernel=$env:JAVA_HOME}
if(!$jdkKernel){$candidatoKernel=Get-ChildItem -LiteralPath "$env:ProgramFiles\Java" -Directory | Where-Object {Test-Path -LiteralPath (Join-Path $_.FullName 'bin\jar.exe')} | Sort-Object LastWriteTime -Descending | Select-Object -First 1; if($candidatoKernel){$jdkKernel=$candidatoKernel.FullName}}
if(!$jdkKernel){throw 'Se requiere JDK 17 o posterior; configura JAVA_HOME.'}
$salidaKernel=Join-Path $PSScriptRoot 'target\classes'
New-Item -ItemType Directory -Path $salidaKernel -Force | Out-Null
$fuentesKernel=@(Get-ChildItem -LiteralPath (Join-Path $PSScriptRoot 'src\main\java') -Recurse -Filter '*.java' | ForEach-Object {$_.FullName})
& "$jdkKernel\bin\javac.exe" '--release' '17' '-encoding' 'UTF-8' '-d' $salidaKernel @fuentesKernel
if($LASTEXITCODE -ne 0){throw 'Falló la compilación Java.'}
& "$jdkKernel\bin\jar.exe" '--create' '--file' (Join-Path $PSScriptRoot 'target\simulador-kernel.jar') '--main-class' 'edu.umg.kernel.Main' '-C' $salidaKernel '.'
if($LASTEXITCODE -ne 0){throw 'Falló la creación del JAR.'}
Write-Output 'Compilación correcta: target/simulador-kernel.jar'
