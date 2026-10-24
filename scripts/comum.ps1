# Funcoes e valores compartilhados pelos scripts. Nao e pra rodar sozinho:
# os outros scripts carregam este arquivo com ". (Join-Path $PSScriptRoot 'comum.ps1')".
# Funciona no Windows PowerShell 5.1, sem precisar de administrador.

$script:Raiz = Split-Path -Parent $PSScriptRoot
$script:Pacote = 'com.guxtavon.tvlimpa'
$script:TelaDeFabrica = 'com.google.android.tvlauncher'
$script:ModoSimular = $false
$script:TvAlvo = ''
$script:AdbExe = ''

# Pacotes que NUNCA sao desligados por estes scripts (lista testada neste firmware).
# Se a sua TV nao e igual, essa lista e o seu seguro: confira antes de apertar Enter.
# A voz (katniss e companhia) fica porque o YouTube usa o reconhecimento de voz do sistema.
$script:NaoDesligarNunca = @(
    'com.tcl.tv',
    'com.tcl.tvinput',
    'com.tcl.tvpassthrough',
    'com.tcl.sourcemananger',
    'com.tcl.keycustomfunctionservice',
    'com.tcl.framework.custom',
    'com.tcl.tcl_bt_rcu_service',
    'com.tcl.autopair',
    'com.google.android.tv.remote.service',
    'com.tcl.inputmethod.international',
    'com.tcl.settings',
    'com.tcl.initsetup',
    'com.tcl.factory.view',
    'com.tcl.showmode',
    'com.tcl.xian.StartandroidService',
    'com.tcl.appstatecontroller',
    'com.realtek.netflix.setting',
    'com.android.WifiCountryService',
    'com.google.android.katniss',
    'com.google.android.speech.pumpkin',
    'com.tcl.assistant',
    'com.tcl.micmanager',
    # Base do Android e do Google, em qualquer marca: sem eles a TV perde a imagem, o controle, a loja ou a rede.
    'android',
    'com.android.systemui',
    'com.android.settings',
    'com.android.tv.settings',
    'com.android.providers.settings',
    'com.android.packageinstaller',
    'com.google.android.packageinstaller',
    'com.android.shell',
    'com.android.bluetooth',
    'com.android.inputdevices',
    'com.android.vending',
    'com.google.android.gms',
    'com.google.android.gsf',
    'com.google.android.webview',
    'com.android.webview',
    'com.google.android.tungsten.setupwraith',
    'com.google.android.apps.tv.launcherx',
    'com.google.android.leanback.ime'
)

# Estes dois so mudam pelos scripts instalar-tela.ps1 e desfazer-tela.ps1,
# que conferem a troca de tela inicial. Pela lista de pacotes, nao.
$script:SoPeloInstalador = @($script:TelaDeFabrica, $script:Pacote)

function Test-Protegido {
    param([string]$Nome)
    foreach ($p in $script:NaoDesligarNunca) {
        if ($p -ieq $Nome) { return $true }
    }
    # Os provedores do Android (canais da TV, midia, downloads, contatos...) nunca saem.
    if ($Nome -like 'com.android.providers.*') { return $true }
    return $false
}

function Test-SoPeloInstalador {
    param([string]$Nome)
    foreach ($p in $script:SoPeloInstalador) {
        if ($p -ieq $Nome) { return $true }
    }
    return $false
}

function Test-NomeDePacote {
    param([string]$Nome)
    return ($Nome -cmatch '^[A-Za-z][A-Za-z0-9_]*(\.[A-Za-z0-9_]+)+$')
}

# Le uma lista: uma linha por pacote, '#' comeca comentario, linhas vazias sao ignoradas.
function Read-Lista {
    param([string]$Arquivo)
    if (-not (Test-Path -LiteralPath $Arquivo)) {
        throw "Nao achei a lista: $Arquivo"
    }
    $pacotes = New-Object System.Collections.Generic.List[string]
    foreach ($linha in (Get-Content -LiteralPath $Arquivo -Encoding UTF8)) {
        $texto = $linha.TrimStart([char]0xFEFF)
        $i = $texto.IndexOf('#')
        if ($i -ge 0) { $texto = $texto.Substring(0, $i) }
        $texto = $texto.Trim()
        if ($texto.Length -gt 0) { $pacotes.Add($texto) }
    }
    return ,$pacotes.ToArray()
}

# Prepara o adb e (fora do -Simular) conecta na TV.
function Initialize-Adb {
    param(
        [string]$Tv,
        [bool]$Simular,
        [string]$Adb
    )
    $script:ModoSimular = $Simular
    $script:TvAlvo = $Tv
    if ($Tv -notmatch '^[A-Za-z0-9.\-]+:\d{1,5}$') {
        throw "-Tv precisa ser no formato ip:porta (por exemplo <ip-da-tv>:5555). Recebi: $Tv"
    }
    if (-not $Adb) {
        if ($env:TVLIMPA_ADB) { $Adb = $env:TVLIMPA_ADB }
        else { $Adb = Join-Path $script:Raiz 'ferramentas\platform-tools\adb.exe' }
    }
    $script:AdbExe = $Adb
    try { [Console]::OutputEncoding = [System.Text.Encoding]::UTF8 } catch { }
    if ($Simular) {
        Write-Host "[simular] nada sera executado; so os comandos aparecem. adb: $Adb"
        Write-Host ("[simular] adb connect {0}" -f $Tv)
        Write-Host ("[simular] adb -s {0} get-state   (precisa responder 'device'; senao o script para aqui)" -f $Tv)
        return
    }
    if (-not (Test-Path -LiteralPath $Adb)) {
        throw "Nao achei o adb em $Adb. Baixe o platform-tools do Google e ponha em ferramentas\platform-tools, ou use -Adb <caminho>."
    }
    $saida = & $Adb connect $Tv
    Write-Host ($saida -join ' ')
    if (($saida -join ' ') -notmatch 'connected') {
        throw "Nao consegui conectar em $Tv. Confira o IP e se a depuracao USB esta ligada na TV."
    }
    Assert-Aparelho
}

# Confere que a TV esta pronta pro adb ("device"). "connected" no adb connect nao basta:
# a TV pode estar "unauthorized" (falta aceitar o aviso na tela) ou "offline".
function Assert-Aparelho {
    # Com 'Continue' o erro do adb (ex.: 'error: device unauthorized') vira texto em vez de parar o script.
    $estado = ''
    $antes = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    try {
        $estado = ((@(& $script:AdbExe -s $script:TvAlvo get-state 2>&1) | ForEach-Object { "$_" }) -join ' ').Trim()
    } catch {
        $estado = ''
    } finally {
        $ErrorActionPreference = $antes
    }
    if ($estado -ne 'device') {
        if (-not $estado) { $estado = 'sem resposta' }
        throw ("A TV em {0} nao esta pronta pro adb (estado: {1}). Se apareceu na TV um aviso pedindo pra permitir a depuracao, aceite (marque 'sempre permitir') e rode de novo. Se continuar, desligue e ligue a depuracao na TV." -f $script:TvAlvo, $estado)
    }
}

# Roda (ou, no -Simular, so mostra) um comando do adb apontado pra TV.
# Devolve as linhas de saida (ou $null no -Simular).
function Invoke-Adb {
    param([string[]]$Argumentos)
    if ($script:ModoSimular) {
        Write-Host ("[simular] adb -s {0} {1}" -f $script:TvAlvo, ($Argumentos -join ' '))
        return $null
    }
    $saida = & $script:AdbExe -s $script:TvAlvo @Argumentos
    return @($saida)
}

function Invoke-Shell {
    param([string[]]$Argumentos)
    return Invoke-Adb (@('shell') + $Argumentos)
}

function Start-Pausa {
    param([double]$Segundos)
    if (-not $script:ModoSimular) { Start-Sleep -Milliseconds ([int]($Segundos * 1000)) }
}

# Quem responde ao botao Home agora. Devolve "pacote/atividade" ou $null.
function Get-TelaDoHome {
    $saida = Invoke-Shell @('cmd', 'package', 'resolve-activity', '--brief',
        '-a', 'android.intent.action.MAIN', '-c', 'android.intent.category.HOME')
    if ($null -eq $saida) { return $null }
    $linhas = @($saida | ForEach-Object { "$_".Trim() } | Where-Object { $_ -match '^[\w.]+/\S+$' })
    if ($linhas.Count -eq 0) { return $null }
    return $linhas[$linhas.Count - 1]
}

# A tela nova aparece entre as que podem responder ao Home? (cmd package query-activities)
function Test-NossaEntreAsDoHome {
    $saida = Invoke-Shell @('cmd', 'package', 'query-activities', '--brief',
        '-a', 'android.intent.action.MAIN', '-c', 'android.intent.category.HOME')
    if ($null -eq $saida) { return $false }
    foreach ($l in $saida) {
        if ("$l".Trim().StartsWith("$($script:Pacote)/")) { return $true }
    }
    return $false
}

# O processo da tela nova esta rodando? (pidof). Tenta por ate $Segundos.
function Test-NossaRodando {
    param([int]$Segundos = 6)
    for ($i = 1; $i -le $Segundos; $i++) {
        $saida = Invoke-Shell @('pidof', $script:Pacote)
        if ($script:ModoSimular) { return $true }
        if ((@($saida) -join ' ') -match '^\s*\d+') { return $true }
        Start-Pausa 1
    }
    return $false
}

# Pacotes das atividades em primeiro plano (mResumedActivity / ResumedActivity no dumpsys).
function Get-PacotesNaFrente {
    $saida = Invoke-Shell @('dumpsys', 'activity', 'activities')
    $pacotes = New-Object System.Collections.Generic.List[string]
    if ($null -eq $saida) { return $pacotes.ToArray() }
    foreach ($l in $saida) {
        if ("$l" -match '\b(m?ResumedActivity)\s*[:=]\s*ActivityRecord\{\S+\s+u\d+\s+([\w.]+)/') {
            $pacotes.Add($Matches[2])
        }
    }
    return $pacotes.ToArray()
}

# A tela nova esta na frente? Toda atividade "resumed" e dela (e ha pelo menos uma).
# Tenta por ate $Segundos (a troca depois do Home leva um instante).
function Test-NossaNaFrente {
    param([int]$Segundos = 5)
    for ($i = 1; $i -le $Segundos; $i++) {
        $pacotes = @(Get-PacotesNaFrente)
        if ($script:ModoSimular) { return $true }
        $outros = @($pacotes | Where-Object { $_ -ne $script:Pacote })
        if ($pacotes.Count -gt 0 -and $outros.Count -eq 0) { return $true }
        Start-Pausa 1
    }
    if ($pacotes.Count -eq 0) { Write-Host 'Nenhuma atividade aparece em primeiro plano no dumpsys.' -ForegroundColor Yellow }
    else { Write-Host ("Em primeiro plano: {0}" -f ($pacotes -join ', ')) -ForegroundColor Yellow }
    return $false
}

# Reconecta o adb na TV. Devolve $true se a resposta diz "connected".
function Connect-Adb {
    if ($script:ModoSimular) {
        Write-Host ("[simular] adb connect {0}" -f $script:TvAlvo)
        return $true
    }
    try {
        $saida = & $script:AdbExe connect $script:TvAlvo
        return ((@($saida) -join ' ') -match 'connected')
    } catch {
        return $false
    }
}

# Religa a tela inicial de fabrica e CONFERE ("new state: enabled"). Tenta ate 3 vezes,
# reconectando o adb antes de cada nova tentativa. Devolve $true so se confirmou.
# Se nao conseguir confirmar, avisa em vermelho como religar na mao.
function Restore-TelaDeFabrica {
    param([int]$Tentativas = 3)
    for ($i = 1; $i -le $Tentativas; $i++) {
        try {
            if ($i -gt 1) {
                Write-Host ("Tentativa {0} de {1}: reconectando e religando a tela de fabrica..." -f $i, $Tentativas) -ForegroundColor Yellow
                Start-Pausa 2
                Connect-Adb | Out-Null
            }
            $saida = Invoke-Shell @('pm', 'enable', '--user', '0', $script:TelaDeFabrica)
            if ($script:ModoSimular) {
                Write-Host '[simular] se a resposta nao trouxer "new state: enabled", repete ate 3 vezes (pausa de 2 s e reconexao antes de cada nova tentativa)'
                Invoke-Shell @('input', 'keyevent', 'KEYCODE_HOME') | Out-Null
                return $true
            }
            if ((@($saida) -join ' ') -match 'new state: enabled') {
                try { Invoke-Shell @('input', 'keyevent', 'KEYCODE_HOME') | Out-Null } catch { }
                return $true
            }
            Write-Host ("A TV respondeu: '{0}'" -f ((@($saida) -join ' ').Trim())) -ForegroundColor Yellow
        } catch {
            Write-Host ("Erro ao religar: {0}" -f $_.Exception.Message) -ForegroundColor Yellow
        }
    }
    Write-Host ''
    Write-Host 'ATENCAO: NAO CONSEGUI CONFIRMAR que a tela de fabrica foi religada.' -ForegroundColor Red
    Write-Host 'Religue na mao, de um terminal no PC (precisa da TV ligada e da depuracao ativa):' -ForegroundColor Red
    Write-Host ("  .\ferramentas\platform-tools\adb.exe connect {0}" -f $script:TvAlvo) -ForegroundColor Red
    Write-Host ("  .\ferramentas\platform-tools\adb.exe -s {0} shell pm enable --user 0 {1}" -f $script:TvAlvo, $script:TelaDeFabrica) -ForegroundColor Red
    Write-Host 'Mesmo assim a TV nao fica sem tela: as telas de emergencia do proprio Android (FallbackHome) continuam ligadas.' -ForegroundColor Red
    return $false
}
