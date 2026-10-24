<#
.SYNOPSIS
  Instala a tela inicial nova, confere que ela pode responder ao Home e que abre de verdade,
  desliga a de fabrica e confere que o botao Home agora cai na nova (e que ela esta na frente).
  Se a tela nova nao abrir, para ANTES de desligar qualquer coisa. Se o Home nao cair nela
  (ou se algo falhar no meio), religa a de fabrica e CONFERE que religou.
.EXAMPLE
  .\instalar-tela.ps1 -Tv <ip-da-tv>:5555 -Apk ..\app\build\outputs\apk\release\app-release.apk -Simular
#>
param(
    [Parameter(Mandatory = $true)][string]$Tv,
    [Parameter(Mandatory = $true)][string]$Apk,
    [switch]$Simular,
    [string]$Adb
)
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'comum.ps1')

if (-not (Test-Path -LiteralPath $Apk)) {
    if ($Simular) { Write-Host "[simular] aviso: o APK $Apk nao existe (de verdade, o script pararia aqui)." }
    else { throw "Nao achei o APK: $Apk" }
}
Initialize-Adb -Tv $Tv -Simular $Simular.IsPresent -Adb $Adb

function Stop-AntesDeDesligar {
    param([string]$Motivo)
    Write-Host $Motivo -ForegroundColor Red
    Write-Host 'Parei ANTES de desligar a tela de fabrica: nada foi desligado, a TV continua como estava.' -ForegroundColor Yellow
    exit 1
}

# 0. A tela nova so sabe substituir a tela inicial classica do Android TV (com.google.android.tvlauncher).
# Google TV (launcherx) e telas proprias de fabricante ficam de fora: nesse caso para antes de instalar.
# Se a tela atual ja e a nossa, e uma atualizacao e pode seguir.
Write-Host '0/5 Conferindo qual e a tela inicial desta TV...'
$atual = Get-TelaDoHome
if ($Simular) {
    Write-Host "[simular] se a tela do Home nao for $($script:TelaDeFabrica) nem $($script:Pacote), o script para aqui (nada instalado)."
} elseif (-not $atual) {
    Write-Host 'Nao consegui ler qual e a tela inicial desta TV. Nada foi instalado nem mudado.' -ForegroundColor Red
    exit 1
} elseif (-not ($atual.StartsWith("$($script:TelaDeFabrica)/") -or $atual.StartsWith("$($script:Pacote)/"))) {
    Write-Host "Esta TV usa outra tela inicial: $atual" -ForegroundColor Yellow
    Write-Host "O tv-limpa so troca a tela inicial classica do Android TV ($($script:TelaDeFabrica)). Nada foi instalado nem mudado." -ForegroundColor Yellow
    if ($atual -like '*ResolverActivity*') {
        Write-Host 'O Home esta caindo no seletor de telas (talvez de uma instalacao que parou no meio). Rode o desfazer-tela.ps1 e tente de novo.' -ForegroundColor Yellow
    }
    exit 1
}

Write-Host '1/5 Instalando...'
$saida = Invoke-Adb @('install', '-r', $Apk)
if (-not $Simular -and (($saida -join ' ') -notmatch 'Success')) {
    throw "A instalacao falhou: $($saida -join ' ')"
}

Write-Host '2/5 Conferindo que a tela nova aparece entre as telas iniciais...'
$naLista = Test-NossaEntreAsDoHome
if ($Simular) {
    Write-Host "[simular] se nenhuma linha comecar com $($script:Pacote)/, o script para aqui (nada desligado)."
} elseif (-not $naLista) {
    Stop-AntesDeDesligar "O Android nao lista $($script:Pacote) como tela inicial (query-activities HOME). A instalacao pode nao ter dado certo."
}

Write-Host '3/5 Abrindo a tela nova uma vez e conferindo que ela roda...'
Invoke-Shell @('am', 'start', '-n', "$($script:Pacote)/.MainActivity") | Out-Null
Start-Pausa 2
$rodando = Test-NossaRodando -Segundos 6
if ($Simular) {
    Write-Host "[simular] repete o pidof por ate 6 s; se nunca vier um numero, o script para aqui (nada desligado)."
} elseif (-not $rodando) {
    Stop-AntesDeDesligar "A tela nova nao ficou rodando depois de abrir (pidof $($script:Pacote) vazio). Ela pode estar fechando sozinha."
}

# A partir daqui a tela de fabrica pode ficar desligada. Se qualquer coisa der errado
# (erro, adb que caiu, resposta que nao deu pra ler, Ctrl+C), o finally religa e confere.
$concluido = $false
try {
    Write-Host '4/5 Desligando a tela de fabrica...'
    $saida = Invoke-Shell @('pm', 'disable-user', '--user', '0', $script:TelaDeFabrica)
    if (-not $Simular -and ((@($saida) -join ' ') -notmatch 'new state: disabled')) {
        throw "Nao confirmei que a tela de fabrica foi desligada (resposta: $((@($saida) -join ' ').Trim()))."
    }

    Write-Host '5/5 Apertando Home e conferindo quem responde e quem esta na frente...'
    Invoke-Shell @('input', 'keyevent', 'KEYCODE_HOME') | Out-Null
    Start-Pausa 2
    $quem = Get-TelaDoHome
    $responde = ($quem -and $quem.StartsWith("$($script:Pacote)/"))
    $naFrente = $false
    if ($responde -or $Simular) { $naFrente = Test-NossaNaFrente -Segundos 5 }
    if ($Simular) {
        Write-Host "[simular] se a tela do Home nao comecar com $($script:Pacote)/, ou se a atividade em primeiro plano (mResumedActivity/ResumedActivity) nao for dela em 5 s, ou se o adb cair no meio, o script religa a de fabrica e confere:"
        $null = Restore-TelaDeFabrica
        $concluido = $true
    } elseif ($responde -and $naFrente) {
        Write-Host "Pronto: o Home agora abre $quem, e ela esta na frente." -ForegroundColor Green
        $concluido = $true
    } elseif ($responde) {
        Write-Host "O Home aponta pra $quem, mas a tela nova nao apareceu na frente." -ForegroundColor Red
    } else {
        Write-Host "O Home respondeu com '$quem' e nao com a tela nova." -ForegroundColor Red
    }
} finally {
    if (-not $concluido) {
        Write-Host 'Religando a tela de fabrica...' -ForegroundColor Red
        if (Restore-TelaDeFabrica) {
            Write-Host 'A tela de fabrica foi religada (confirmado). Nada ficou quebrado.' -ForegroundColor Yellow
        }
    }
}
if (-not $concluido) { exit 1 }
exit 0
