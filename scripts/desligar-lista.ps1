<#
.SYNOPSIS
  Desliga (sem apagar) os apps de uma lista, um por linha, na TV.
.EXAMPLE
  .\desligar-lista.ps1 -Tv <ip-da-tv>:5555 -Lista ..\listas\tcl-beyondtv-lf1v625.txt -Simular
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
    if (Test-Protegido $p) {
        Write-Host "RECUSADO  $p  (esta na lista 'Nao desligar nunca')" -ForegroundColor Yellow
        $recusados++; continue
    }
    if (Test-SoPeloInstalador $p) {
        Write-Host "RECUSADO  $p  (a tela inicial so troca por instalar-tela.ps1 / desfazer-tela.ps1)" -ForegroundColor Yellow
        $recusados++; continue
    }
    $saida = Invoke-Shell @('pm', 'disable-user', '--user', '0', $p)
    if ($Simular) { $feitos++; continue }
    $texto = ($saida -join ' ').Trim()
    if ($texto -match 'new state: disabled') {
        Write-Host "DESLIGADO $p" -ForegroundColor Green; $feitos++
    } else {
        Write-Host "FALHOU    $p  -> $texto" -ForegroundColor Red; $falhas++
    }
}
$verbo = 'desligados'
if ($Simular) { $verbo = 'a desligar' }
Write-Host ("Resumo: {0} {1}, {2} recusados, {3} falhas." -f $feitos, $verbo, $recusados, $falhas)
if ($falhas -gt 0) { exit 1 }
