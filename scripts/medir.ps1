<#
.SYNOPSIS
  Mede a TV pela regra do projeto e grava medidas\<rotulo>-<aaaa-mm-dd-hhmm>.txt.
.DESCRIPTION
  A regra: TV ligada da tomada, UMA HORA parada na tela inicial, e so depois rodar este
  script. A espera de uma hora e de quem chama; este script nao espera.
  Ele anota: /proc/meminfo, os 15 processos que mais usam memoria, a memoria da tela
  inicial que esta ativa e os 3 tempos de abrir o YouTube a frio (com a mediana).
.EXAMPLE
  .\medir.ps1 -Tv <ip-da-tv>:5555 -Rotulo antes -Simular
#>
param(
    [Parameter(Mandatory = $true)][string]$Tv,
    [Parameter(Mandatory = $true)][string]$Rotulo,
    [switch]$Simular,
    [string]$Adb
)
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'comum.ps1')

if ($Rotulo -notmatch '^[A-Za-z0-9_\-]+$') {
    throw "-Rotulo so aceita letras, numeros, _ e - (recebi: $Rotulo)"
}
Initialize-Adb -Tv $Tv -Simular $Simular.IsPresent -Adb $Adb

$youtube = 'com.google.android.youtube.tv'
$quando = Get-Date
$pasta = Join-Path $script:Raiz 'medidas'
$arquivo = Join-Path $pasta ("{0}-{1}.txt" -f $Rotulo, $quando.ToString('yyyy-MM-dd-HHmm'))

$relatorio = New-Object System.Collections.Generic.List[string]
function Add-Linha { param([string]$Texto) $relatorio.Add($Texto); Write-Host $Texto }

Add-Linha ("Medida: {0}  ({1})" -f $Rotulo, $quando.ToString('yyyy-MM-dd HH:mm'))
Add-Linha 'Regra: TV ligada da tomada, uma hora parada na tela inicial, depois estas medidas.'
Add-Linha ''

# 1. Memoria do sistema
Add-Linha '== /proc/meminfo =='
$saida = Invoke-Shell @('cat', '/proc/meminfo')
if (-not $Simular -and -not ((@($saida) -join '').Trim())) {
    throw "A TV nao devolveu o /proc/meminfo (resposta vazia). Nada foi gravado; confira a conexao e rode de novo."
}
if ($saida) { foreach ($l in $saida) { Add-Linha ("$l".TrimEnd()) } }
Add-Linha ''

# 2. Os 15 processos que mais usam memoria (RSS em KB; a ordenacao e feita aqui no PC)
Add-Linha '== 15 processos que mais usam memoria (RSS em KB) =='
$saida = Invoke-Shell @('ps', '-A', '-o', 'RSS,NAME')
if ($saida) {
    $linhas = @($saida | ForEach-Object { "$_".Trim() } | Where-Object { $_ -match '^\d+\s+\S+' } | ForEach-Object {
        $partes = $_ -split '\s+', 2
        [pscustomobject]@{ Rss = [long]$partes[0]; Nome = $partes[1] }
    })
    foreach ($l in ($linhas | Sort-Object Rss -Descending | Select-Object -First 15)) {
        Add-Linha ("{0,10}  {1}" -f $l.Rss, $l.Nome)
    }
}
Add-Linha ''

# 3. Memoria da tela inicial ativa
Add-Linha '== Tela inicial ativa =='
$quem = Get-TelaDoHome
if ($null -eq $quem) {
    Add-Linha '(no -Simular a tela ativa nao e descoberta; de verdade, o comando acima diz qual e)'
    $pacoteHome = '<pacote-da-tela-ativa>'
} else {
    Add-Linha "Responde ao Home: $quem"
    $pacoteHome = $quem.Split('/')[0]
}
$saida = Invoke-Shell @('dumpsys', 'meminfo', $pacoteHome)
if ($saida) {
    foreach ($l in $saida) {
        if ("$l" -match '^\s*(TOTAL|Applications Memory Usage|\*\* MEMINFO)') { Add-Linha ("$l".TrimEnd()) }
    }
}
Add-Linha ''

# 4. Abrir o YouTube a frio, 3 vezes
Add-Linha '== YouTube aberto a frio (TotalTime em ms) =='
$tempos = New-Object System.Collections.Generic.List[double]
for ($i = 1; $i -le 3; $i++) {
    Invoke-Shell @('am', 'force-stop', $youtube) | Out-Null
    Start-Pausa 3
    $saida = Invoke-Shell @('am', 'start', '-W', '-a', 'android.intent.action.MAIN',
        '-c', 'android.intent.category.LEANBACK_LAUNCHER', '-p', $youtube)
    $total = $null
    if ($saida) {
        foreach ($l in $saida) {
            if ("$l" -match 'TotalTime:\s*(\d+)') { $total = [double]$Matches[1] }
        }
        if ($null -eq $total) { Add-Linha ("tentativa {0}: sem TotalTime na resposta: {1}" -f $i, (($saida -join ' ').Trim())) }
        else { $tempos.Add($total); Add-Linha ("tentativa {0}: {1} ms" -f $i, $total) }
    }
    Invoke-Shell @('input', 'keyevent', 'KEYCODE_HOME') | Out-Null
    Start-Pausa 5
}
Invoke-Shell @('am', 'force-stop', $youtube) | Out-Null
if ($tempos.Count -gt 0) {
    $ord = @($tempos | Sort-Object)
    $meio = [int][math]::Floor($ord.Count / 2)
    if ($ord.Count % 2 -eq 1) { $mediana = $ord[$meio] } else { $mediana = ($ord[$meio - 1] + $ord[$meio]) / 2 }
    Add-Linha ("Mediana: {0} ms (n={1} de 3)" -f $mediana, $ord.Count)
} elseif (-not $Simular) {
    Add-Linha 'Mediana: sem dados (n=0 de 3)'
} else {
    Add-Linha 'Mediana: (de verdade sai algo como "Mediana: 1210 ms (n=3 de 3)", n = tentativas com TotalTime)'
}

if ($Simular) {
    Write-Host ("[simular] gravaria o relatorio em {0}" -f $arquivo)
} else {
    if (-not (Test-Path -LiteralPath $pasta)) { New-Item -ItemType Directory -Path $pasta | Out-Null }
    Set-Content -LiteralPath $arquivo -Value $relatorio -Encoding UTF8
    Write-Host "Gravado em $arquivo"
}
