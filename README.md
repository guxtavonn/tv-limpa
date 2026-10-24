# tv-limpa

Uma tela inicial nova e leve para TVs com Android TV, mais um jeito de tirar da TV o que só pesa e atrapalha (rastreamento, propaganda, apps que ninguém usa). Tudo pelo computador, sem abrir a TV, sem root e **dá pra desfazer**.

Licença MIT (veja `LICENSE`).

## Jeito fácil: com o Claude

Se você não mexe com programação, deixe o Claude te guiar. Ele explica cada passo em português simples, confere tudo antes de mexer na TV e sabe desfazer.

1. **Baixe a skill**: em [Releases](https://github.com/guxtavonn/tv-limpa/releases/latest), o arquivo `tv-limpa-skill.zip`.
2. **Coloque no Claude:**
   - **no claude.ai** (site ou app): Configurações → Capacidades → Skills → enviar o `.zip`. O Claude vai te passar os comandos pra você colar no PowerShell, um por vez;
   - **no Claude Code**: descompacte em `%USERPROFILE%\.claude\skills\` (fica `...\skills\tv-limpa\SKILL.md`). Ele mesmo roda os comandos no seu computador.
3. **Peça:** "quero limpar a minha TV" (ou "debloat na minha TV"). Ele começa conferindo se a sua TV serve.

Você vai precisar de um computador com **Windows** na mesma rede da TV e de uma TV com **Android TV ou Google TV** (Samsung e LG não servem). A tela nova já vem pronta na Release (`.apk`): não precisa compilar nada.

## O que é

Muita TV com Android TV (TCL, Philips, Sony, Xiaomi, entre outras) vem lenta e cheia de propaganda. A tela inicial de fábrica ocupa boa parte da memória e obriga a descer fileiras de recomendação até achar o Netflix ou o YouTube.

Este projeto tem duas partes:

1. **Uma tela inicial própria** (o app, em `app/`): preto, uma fileira só com os apps que a casa usa, atalhos para TV aberta, HDMI, Wi-Fi, todos os apps e configurações (TV aberta e HDMI chamam os apps da TCL: em outras marcas, use o botão Source do controle). Pelo controle remoto dá pra trocar a ordem dos apps ou tirar e pôr apps da fileira, sem computador. O app não pede permissão de internet (confira em `app/src/main/AndroidManifest.xml`): ele só olha se a TV está conectada, pra mostrar o atalho do Wi-Fi.
2. **Scripts para o Windows** (em `scripts/`) que falam com a TV pela rede e desligam, em lista, os apps que você não quer, medem a TV antes e depois, instalam a tela nova e desfazem tudo se você se arrepender.

As listas de apps (`listas/`) foram feitas e testadas numa TCL BeyondTV (Android TV 9, firmware V8-R851T02-LF1V625). Cada linha diz por que está lá. A lista completa também vem separada por grupo (`tcl-beyondtv-lf1v625-1-...`, `-2-...`, `-3-...`), pra desligar um grupo de cada vez e testar a TV entre um e outro.

## O que você precisa

- Um computador com **Windows** (os scripts são em PowerShell, que já vem no Windows 10 e 11). Não precisa ser administrador.
- O computador e a TV **na mesma rede Wi-Fi/cabo** (o mesmo roteador).
- Uma TV com **Android TV ou Google TV**. TVs **Samsung (Tizen) e LG (webOS) não servem**: elas não são Android.
- O **adb**, um programa gratuito do Google que conversa com aparelhos Android. Baixe o "SDK Platform-Tools" no site oficial de desenvolvedores do Android, descompacte e deixe a pasta em `ferramentas\platform-tools` (dentro da pasta deste projeto). Ou use `-Adb <caminho>` em qualquer script.
- Nenhum cabo.

## O que não dá

- **"Continuar assistindo" na tela inicial.** A lista existe dentro do Android (chama `watch_next_program`, do TvProvider), mas só um app com a permissão `ACCESS_ALL_EPG_DATA` consegue ler a de todos os apps. Essa permissão só vai pra apps assinados pelo fabricante da TV (é do tipo `signature|privileged`). Um app comum vê só a lista dele mesmo. Na prática: o "continuar assistindo" continua dentro de cada app (Netflix, Prime Video etc. já abrem nele).
- Recomendações e qualquer conteúdo vindo da internet. De propósito.
- Trocar o firmware, ter root ou apagar app do sistema. De propósito (veja os riscos).
- Funcionar em TV que não seja Android.

## Passo a passo

Os nomes dos menus mudam um pouco de TV pra TV. Procure por algo parecido.

### 1. Ligar o modo de desenvolvedor da TV

O "modo de desenvolvedor" é um menu escondido do Android, feito para quem programa. Ele libera a conversa com o computador. Para liberar:

1. Na TV, vá em **Configurações > Preferências do dispositivo > Sobre**.
2. Ache **Número da versão** (às vezes "Build") e aperte **OK 7 vezes seguidas**. A TV avisa "agora você é um desenvolvedor".

### 2. Ligar a depuração

1. Volte para **Preferências do dispositivo** e entre em **Opções do desenvolvedor**.
2. Ligue **Depuração USB** (e, se existir, **Depuração por rede/ADB**). "Depuração" é a permissão de o computador mandar comandos para a TV.

### 3. Achar o IP da TV

O IP é o "endereço" da TV na sua rede, algo como quatro números separados por ponto.

- Na TV: **Configurações > Rede e Internet** > a rede conectada > o endereço de IP.
- A porta quase sempre é `5555`. O endereço completo fica assim: `<ip-da-tv>:5555`.

### 4. Conferir a conexão e dar a primeira permissão

No computador, abra o PowerShell na pasta do projeto e rode:

```powershell
.\ferramentas\platform-tools\adb.exe connect <ip-da-tv>:5555
```

Olhe a TV: vai aparecer um pedido **"Permitir a depuração USB?"**. Marque "Sempre permitir" e aceite.

### 5. Treinar sem mexer em nada (-Simular)

Todos os scripts têm o modo `-Simular`, que só **mostra os comandos** e não toca na TV. Faça sempre isso primeiro:

```powershell
.\scripts\desligar-lista.ps1 -Tv <ip-da-tv>:5555 -Lista .\listas\tcl-beyondtv-lf1v625-1-rastreio-e-propaganda.txt -Simular
```

> Se o PowerShell reclamar de "execução de scripts desabilitada", rode o script assim:
> `powershell -ExecutionPolicy Bypass -File .\scripts\desligar-lista.ps1 ...`

### 6. Medir antes (opcional, mas vale a pena)

Para o "antes e depois" ser justo, use sempre a mesma regra: TV ligada da tomada, **uma hora parada na tela inicial**, e só então:

```powershell
.\scripts\medir.ps1 -Tv <ip-da-tv>:5555 -Rotulo antes
```

O resultado vai para a pasta `medidas\` (memória da TV, os 15 processos mais pesados, a memória da tela inicial e três tempos de abrir o YouTube a frio, com a mediana).

### 7. Desligar o que pesa

**Leia a lista antes.** Ela é desta TV (TCL BeyondTV, firmware V8-R851T02-LF1V625). O grupo 2 depende de quem usa a TV: nesta casa ficam Netflix, Globoplay, Prime Video, YouTube, Disney+, Chromecast e a busca por voz. Tire as linhas dos apps que você usa. Depois rode **um grupo por vez**, sem `-Simular`, reiniciando e testando a TV entre um e outro:

```powershell
.\scripts\desligar-lista.ps1 -Tv <ip-da-tv>:5555 -Lista .\listas\tcl-beyondtv-lf1v625-1-rastreio-e-propaganda.txt
.\scripts\desligar-lista.ps1 -Tv <ip-da-tv>:5555 -Lista .\listas\tcl-beyondtv-lf1v625-2-apps-que-a-casa-nao-usa.txt
.\scripts\desligar-lista.ps1 -Tv <ip-da-tv>:5555 -Lista .\listas\tcl-beyondtv-lf1v625-3-teimosos-da-tcl.txt
```

O `desligar-lista` **recusa** qualquer pacote da lista "Não desligar nunca" (veja em Riscos). O `religar-lista` só avisa, porque religar não faz mal.

Para outra TV, não use a lista de cima sem conferir: parta de uma lista parecida (veja o crédito abaixo), rode `.\ferramentas\platform-tools\adb.exe -s <ip-da-tv>:5555 shell pm list packages -e` pra ver o que está ligado na sua e vá por grupos pequenos, testando a TV a cada grupo.

### 8. Instalar a tela nova

Baixe o app pronto em [Releases](https://github.com/guxtavonn/tv-limpa/releases/latest) (o arquivo `tv-limpa-tela-inicial.apk`) e salve na pasta do projeto. Depois:

```powershell
.\scripts\instalar-tela.ps1 -Tv <ip-da-tv>:5555 -Apk .\tv-limpa-tela-inicial.apk
```

Se preferir compilar você mesmo: precisa de JDK 17 e do Android SDK, e `.\gradlew.bat assembleRelease` gera `app\build\outputs\apk\release\app-release.apk`.

**A tela nova só substitui a tela inicial clássica do Android TV** (`com.google.android.tvlauncher`). Se a sua TV usa a do Google TV (`launcherx`) ou uma tela própria do fabricante, o script para antes de instalar e não muda nada: nessas TVs, faça só a limpeza.

O script confere a tela inicial atual, instala o app, confere que o Android a aceita como tela inicial, abre uma vez e confere que ela ficou rodando. Se alguma dessas falhar, ele para **antes** de desligar qualquer coisa. Depois desliga a tela inicial de fábrica, aperta Home e **confere** que quem responde agora é a tela nova e que ela está na frente. Se não for, ele religa a de fábrica na hora.

### 9. No fim: desligar a depuração

Volte às **Opções do desenvolvedor** da TV e **desligue a depuração**. Enquanto ela está ligada, a porta 5555 da TV fica aberta para qualquer aparelho da sua rede Wi-Fi. Se precisar de uma correção depois, é só religar.

## Como desfazer

Em ordem do mais leve ao mais pesado:

1. **Só os apps desligados:** `.\scripts\religar-lista.ps1 -Tv <ip-da-tv>:5555 -Lista .\listas\<a mesma lista>.txt` (a lista completa `tcl-beyondtv-lf1v625.txt` religa tudo de uma vez)
2. **A tela nova:** `.\scripts\desfazer-tela.ps1 -Tv <ip-da-tv>:5555`. Religa a tela de fábrica, desinstala a nova e confere o Home.
3. **Último recurso, restaurar de fábrica:** nas configurações da TV, "Redefinir para padrão de fábrica". Apaga seus apps e logins, mas volta tudo ao que era.

Nada aqui apaga apps do sistema: eles só são desligados. Por isso volta.

## Riscos, com sinceridade

- Tem gente que travou a TV mexendo no que não devia (desligar o app errado, trocar firmware, usar root). É um risco real, e é por isso que este projeto **só desliga e só pelo caminho oficial do Android**: sem root, sem trocar firmware, sem apagar nada. O que foi desligado liga de volta com um comando.
- A tela inicial é a parte mais perigosa (sem ela, a TV fica sem menu). Por isso a troca é feita em passos: primeiro instala e testa, só depois desliga a de fábrica, e o script confere. As telas de emergência da TCL continuam ligadas.
- **Lista "Não desligar nunca"** (testada neste firmware; o `desligar-lista` recusa estes pacotes e o `religar-lista` só avisa): `com.tcl.tv`, `com.tcl.tvinput`, `com.tcl.tvpassthrough`, `com.tcl.sourcemananger`, `com.tcl.keycustomfunctionservice`, `com.tcl.framework.custom`, `com.tcl.tcl_bt_rcu_service`, `com.tcl.autopair`, `com.google.android.tv.remote.service`, `com.tcl.inputmethod.international`, `com.tcl.settings`, `com.tcl.initsetup`, `com.tcl.factory.view`, `com.tcl.showmode`, `com.tcl.xian.StartandroidService`, `com.tcl.appstatecontroller`, `com.realtek.netflix.setting`, `com.android.WifiCountryService`, e a voz: `com.google.android.katniss`, `com.google.android.speech.pumpkin`, `com.tcl.assistant`, `com.tcl.micmanager`. A voz fica porque o YouTube usa o reconhecimento de voz do sistema; desligar quebra a busca por voz.
- **Se a sua TV não é igual a esta**, a lista acima é só um ponto de partida: antena, HDMI, controle e voz mudam de nome em outras marcas.
- A TCL tem um app de sistema (`com.tcl.appstatecontroller`) que religa sozinho, a cada boot, quatro apps (`com.tcl.smartalexa`, `com.tcl.MultiScreenInteraction_TV`, `com.tcl.notereminder` e `com.tcl.tv.tclhome_master`). Nem desinstalar pelo adb segura. Por isso o app da tela inicial os **fecha** de novo no boot, 60 segundos depois e toda vez que a tela volta. Eles não somem do aparelho, só ficam parados.
- O app é pequeno e sem acesso à internet, mas ele apaga processos de outros apps e vira a tela inicial. Leia o código antes de instalar na TV de alguém.

## Números medidos

Medido numa TCL BeyondTV (Android TV 9, 2 GB de RAM) em 21/10/2026, com a mesma regra nas duas vezes: TV ligada do zero, **uma hora parada na tela inicial**, depois `scripts\medir.ps1`. O "antes" já é com a TV limpa (a lista desliga 25 apps; os 4 teimosos da TCL voltam a cada boot, então ficam 21 desligados de vez); a única diferença entre as duas medidas é a tela inicial.

| | Tela de fábrica (Google TV) | Tela nova |
|---|---|---|
| Memória da tela inicial (PSS, a conta do Android) | 64 MB | **14 MB** |
| Memória da tela inicial (RSS, contando bibliotecas divididas) | 121 MB | 75 MB |
| Memória livre na TV (`MemAvailable`) | 599 MB | **672 MB** |
| Espaço de emergência usado (swap) | 62 MB | 41 MB |
| YouTube abrindo a frio (mediana de 3) | 972 ms | 972 ms |

O que isso quer dizer, sem exagero:
- a tela nova gasta umas **4 vezes menos memória** que a de fábrica, e a TV fica com uns **70 MB a mais livres**;
- **abrir um app continua no mesmo tempo**: quem manda nisso é o processador da TV, não a tela inicial;
- a fluidez da tela nova, medida com `dumpsys gfxinfo`, fica em torno de **25 quadros por segundo** nesse processador.

A TCL religa sozinha quatro apps a cada boot; a tela nova pede pro Android fechar os quatro, mas a Alexa e o MagiConnect da TCL continuaram rodando (o Android não deixa um app comum fechar esses dois).

## Crédito

A ideia de limpar a TV pelo computador, com listas de apps por modelo, veio do projeto **[cleanyourtv](https://github.com/Gathin23/cleanyourtv)** (Gathin23, licença MIT). Ele foi usado como **referência**: a ideia, e as listas de firmwares vizinhos (TCL BeyondTV de outras regiões) para saber o que era seguro desligar. A tela inicial e os scripts deste repositório são escritos do zero, e as listas daqui foram testadas numa TV real.

Fontes e ícones do app: Inter (licença OFL) e Lucide (licença ISC); os textos das licenças estão em `licencas/`.
