param([string]$Guion)
$ErrorActionPreference='Stop'
& (Join-Path $PSScriptRoot 'Compilar.ps1')
$argumentosKernel=@('-Dstdout.encoding=UTF-8','-Dstderr.encoding=UTF-8','-jar',(Join-Path $PSScriptRoot 'target\simulador-kernel.jar'))
if($Guion){$rutaGuionKernel=$Guion;if(![IO.Path]::IsPathRooted($Guion)){$rutaGuionKernel=Join-Path $PSScriptRoot $Guion};$argumentosKernel+=@('--script',$rutaGuionKernel)}
& java @argumentosKernel
if($LASTEXITCODE -ne 0){throw "El simulador terminó con código $LASTEXITCODE"}
