<#
.SYNOPSIS
  Volta a tela inicial de fabrica: religa a original, desinstala a nova e confere o Home.
.EXAMPLE
  .\desfazer-tela.ps1 -Tv <ip-da-tv>:5555 -Simular
#>
param(
    [Parameter(Mandatory = $true)][string]$Tv,
    [switch]$Simular,
    [string]$Adb
)
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'comum.ps1')

Initialize-Adb -Tv $Tv -Simular $Simular.IsPresent -Adb $Adb

Write-Host '1/3 Religando a tela de fabrica...'
$saida = Invoke-Shell @('pm', 'enable', '--user', '0', $script:TelaDeFabrica)
if (-not $Simular -and (($saida -join ' ') -notmatch 'new state: enabled')) {
    throw "Nao consegui religar a tela de fabrica: $($saida -join ' ')"
}

Write-Host '2/3 Desinstalando a tela nova...'
$saida = Invoke-Adb @('uninstall', $script:Pacote)
if (-not $Simular -and (($saida -join ' ') -notmatch 'Success')) {
    Write-Host "Aviso: a desinstalacao respondeu '$($saida -join ' ')' (talvez ja nao estivesse instalada)." -ForegroundColor Yellow
}

Write-Host '3/3 Apertando Home e conferindo...'
Invoke-Shell @('input', 'keyevent', 'KEYCODE_HOME') | Out-Null
Start-Pausa 2
$quem = Get-TelaDoHome
if ($Simular) { exit 0 }
if ($quem -and $quem.StartsWith("$($script:TelaDeFabrica)/")) {
    Write-Host "Pronto: o Home voltou a abrir $quem" -ForegroundColor Green
    exit 0
}
Write-Host "Atencao: o Home respondeu com '$quem'. Confira a TV." -ForegroundColor Red
exit 1
