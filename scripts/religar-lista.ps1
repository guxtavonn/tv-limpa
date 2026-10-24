<#
.SYNOPSIS
  Religa os apps de uma lista (o contrario de desligar-lista.ps1).
.EXAMPLE
  .\religar-lista.ps1 -Tv <ip-da-tv>:5555 -Lista ..\listas\tcl-beyondtv-lf1v625.txt -Simular
#>
param(
    [Parameter(Mandatory = $true)][string]$Tv,
    [Parameter(Mandatory = $true)][string]$Lista,
    [switch]$Simular,
    [string]$Adb
)
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'comum.ps1')

$pacotes = Read-Lista $Lista
Initialize-Adb -Tv $Tv -Simular $Simular.IsPresent -Adb $Adb

$feitos = 0; $recusados = 0; $falhas = 0
foreach ($p in $pacotes) {
    if (-not (Test-NomeDePacote $p)) {
        Write-Host "RECUSADO  $p  (nao parece um nome de pacote)" -ForegroundColor Yellow
        $recusados++; continue
    }
    # Religar nunca faz mal, entao a lista de protegidos nao barra aqui: so avisa.
    if (Test-Protegido $p) {
        Write-Host "AVISO     $p  (esta na lista 'Nao desligar nunca'; religar e inofensivo)" -ForegroundColor Yellow
    }
    $saida = Invoke-Shell @('pm', 'enable', '--user', '0', $p)
    if ($Simular) { $feitos++; continue }
    $texto = ($saida -join ' ').Trim()
    if ($texto -match 'new state: enabled') {
        Write-Host "RELIGADO  $p" -ForegroundColor Green; $feitos++
    } else {
        Write-Host "FALHOU    $p  -> $texto" -ForegroundColor Red; $falhas++
    }
}
$verbo = 'religados'
if ($Simular) { $verbo = 'a religar' }
Write-Host ("Resumo: {0} {1}, {2} recusados, {3} falhas." -f $feitos, $verbo, $recusados, $falhas)
if ($falhas -gt 0) { exit 1 }
