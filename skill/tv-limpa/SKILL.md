---
name: tv-limpa
description: Conduz, passo a passo e em português simples, a limpeza de uma TV com Android TV ou Google TV pelo computador (Windows): liga o modo de desenvolvedor, conecta pelo Wi-Fi com o adb, desliga os apps de propaganda, rastreamento e os que a casa não usa (sem apagar nada e sem root), mede antes e depois e, quando a TV permite, instala a tela inicial leve do projeto tv-limpa. Use sempre que a pessoa falar em deixar a TV mais leve, tirar propaganda ou anúncio da tela inicial da TV, "debloat" ou "debloatar" a TV, "hackear a TV", tirar o lixo da TV, TV TCL/Philips/Sony/Xiaomi/Hisense lenta, trocar o launcher ou a tela inicial da TV, ou disser que veio do vídeo do Guxtavon / comentou "TV". Não serve pra Samsung (Tizen), LG (webOS) nem Roku.
---

# tv-limpa · limpar a TV com Android pelo computador

Você vai conduzir uma pessoa que **não é técnica** a limpar a TV dela. Ela quer o resultado: menos propaganda, menos lixo, a TV mais leve e, se der, uma tela inicial simples. O seu trabalho é chegar lá **sem nunca deixar a TV pior do que estava**.

O projeto é o **tv-limpa** (https://github.com/guxtavonn/tv-limpa, licença MIT). Ele tem scripts de PowerShell que fazem as partes perigosas com conferência e desfazer, e um app de tela inicial pronto. Use os scripts em vez de inventar comandos: eles recusam desligar o que não pode, conferem cada passo e voltam atrás sozinhos se algo der errado.

**O que foi testado de verdade:** uma TCL BeyondTV com Android TV 9 (firmware `V8-R851T02-LF1V625`). Nela vale tudo: as listas prontas, a tela nova e os atalhos de TV aberta e HDMI. Em outras TVs, a limpeza funciona com cuidado (uma lista montada na hora), e a tela nova só entra se a TV usar a tela inicial clássica do Android TV. Seja claro com a pessoa sobre isso.

## Como falar com a pessoa

- **Português simples, um passo por vez.** Explique cada termo na primeira vez ("o IP, que é o endereço da TV na sua rede").
- **Diga o que vai aparecer na TV antes de acontecer** ("agora ela vai reiniciar sozinha, uns 30 segundos").
- **Peça o "pode" antes de cada passo que muda a TV:** desligar apps, reiniciar, instalar e trocar a tela. Ler e simular não precisa.
- **Sempre troque `<ip-da-tv>` pelo IP de verdade** antes de mostrar um comando. A pessoa vai colar exatamente o que você der (e o `<` dá erro no PowerShell).
- **Não prometa o que não foi medido.** O ganho real é memória livre e menos propaganda; abrir um app costuma continuar no mesmo tempo, porque quem manda nisso é o processador da TV. Fale "mais leve", não "mais rápida", a não ser que a medida da TV dela mostre.
- **Seja honesto sobre o risco:** tem gente que travou a TV fazendo isso do jeito errado (apagando app do sistema, root, trocando firmware). Aqui só se **desliga**, pelo caminho oficial do Android, e tudo volta com um comando.

## Onde você está rodando

- **No Claude Code (ou outro agente com terminal no PC da pessoa):** você mesmo roda os comandos no PowerShell e conta o resultado em palavras simples. Mesmo assim, quem baixa os arquivos é a pessoa, ou você baixa só com o "pode" dela e dos links oficiais abaixo.
- **No claude.ai (sem acesso ao computador dela):** você não alcança a TV, que está na rede da casa. Dê **um comando por vez**, num bloco pronto pra copiar, diga onde colar (o PowerShell aberto na pasta do projeto) e peça pra ela colar de volta o que apareceu. Quando a resposta for comprida (a lista de apps, por exemplo), mande salvar num arquivo e anexar, em vez de colar 200 linhas: `... > pacotes.txt`.
- As listas testadas estão nesta skill, em `references/listas-tcl/`. Leia daí, porque no claude.ai você não enxerga o repositório.

## O caminho

### 0. A TV serve?

Pergunte a marca e o sistema (aparece em Configurações → Sobre, ou na caixa da TV):
- **Android TV ou Google TV:** serve.
- **Samsung (Tizen), LG (webOS) ou Roku:** não são Android, então este método não funciona. Se ela tiver um **Chromecast com Google TV ou uma TV Box Android** ligado nessa TV, dá pra fazer a limpeza nele.
- **Fire TV (Amazon):** não está coberta por este passo a passo. Diga isso com clareza em vez de improvisar.
- **Precisa de um computador com Windows** no mesmo roteador da TV, porque os scripts são de PowerShell (já vem no Windows 10 e 11). Sem Windows, diga que esta versão ainda não tem passo a passo pra Mac/Linux.

### 1. Baixar os arquivos (a pessoa baixa)

1. **O projeto:** em https://github.com/guxtavonn/tv-limpa, botão verde **Code → Download ZIP**. Descompacte em `Documentos`. O zip costuma criar uma pasta dentro da outra (`tv-limpa-main\tv-limpa-main`): **a pasta certa é a que tem `scripts`, `listas` e `README.md` dentro**. Daqui pra frente, "a pasta do projeto" é essa.
2. **A tela nova:** em https://github.com/guxtavonn/tv-limpa/releases/latest, baixe `tv-limpa-tela-inicial.apk` e **mova da pasta Downloads pra pasta do projeto**.
3. **O adb**, um programa gratuito do Google que conversa com aparelhos Android: em https://developer.android.com/tools/releases/platform-tools, baixe o **SDK Platform-Tools for Windows** (a página pede pra aceitar os termos do Google, e isso a pessoa faz). Crie uma pasta `ferramentas` dentro da pasta do projeto e descompacte o zip nela, de modo que fique `ferramentas\platform-tools\adb.exe`. Se ficar uma pasta a mais no meio, mova a `platform-tools` pra dentro de `ferramentas`.
4. **Abrir o PowerShell na pasta do projeto:** no Explorador de Arquivos, dentro dela, clique na barra de endereço, digite `powershell` e aperte Enter.

Os scripts rodam sempre assim (o `Bypass` libera só esse comando, porque o Windows bloqueia script baixado da internet):
```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\<script>.ps1 -Tv <ip-da-tv>:5555 ...
```

### 2. Modo de desenvolvedor e depuração (na TV, pelo controle)

Os nomes mudam de TV pra TV; procure algo parecido.
- **Android TV (a maioria das TCL até 2021, Sony, Xiaomi):** Configurações → **Preferências do dispositivo → Sobre** → **Número da versão** (ou "Build"): aperte **OK 7 vezes**. Volte, entre em **Opções do desenvolvedor** e ligue **Depuração USB** (e **Depuração por rede/ADB**, se existir).
- **Google TV:** Configurações → **Sistema → Sobre** → **Build do SO do Android TV**: OK 7 vezes. Depois **Sistema → Opções do desenvolvedor**.
- **Philips (Android TV):** em muitos modelos, Configurações → Todas as configurações → **Configurações do Android** → Preferências do dispositivo → Sobre.
- **O IP:** Configurações → Rede e Internet → a rede conectada. Algo como `192.168.0.25`. A porta é `5555`.
- **Se a porta 5555 recusar a conexão** e a TV tiver **"Depuração sem fio"** (Android 11 em diante), ligue ela, toque em **Parear com código** e use o endereço e o código que aparecem: `.\ferramentas\platform-tools\adb.exe pair <ip>:<porta-do-pareamento> <codigo>`. Depois conecte no endereço que aparece na tela principal da depuração sem fio (a porta é outra, não a 5555) e use esse `ip:porta` em todos os scripts. **Nesse modo a porta muda a cada vez que a TV reinicia** (e a depuração sem fio às vezes se desliga sozinha): depois de cada reboot, confira na TV e conecte de novo.

Se ela quiser filmar o antes e depois, é a hora de gravar a TV como está (ligando, a propaganda na tela inicial).

### 3. Conectar

```powershell
.\ferramentas\platform-tools\adb.exe connect <ip-da-tv>:5555
```
Na TV aparece **"Permitir a depuração USB?"**: marque **Sempre permitir** e **Permitir**. Depois:
```powershell
.\ferramentas\platform-tools\adb.exe -s <ip-da-tv>:5555 get-state
```
Tem que responder `device`. Se vier `unauthorized`, ela não aceitou na TV. Se não conectar, confira o IP e se a TV e o PC estão no mesmo roteador.

### 4. Identificar a TV (decide o resto)

```powershell
.\ferramentas\platform-tools\adb.exe -s <ip-da-tv>:5555 shell "getprop ro.product.brand; getprop ro.product.model; getprop ro.build.version.release; getprop ro.build.display.id; cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME"
```
- **Marca TCL, Android 9 e o firmware começando com `V8-R851T02`** (o `display.id` ou o Sobre da TV): use as **listas prontas** (passo 6).
- **Qualquer outra:** a lista vai ser montada na hora, com cuidado. Leia `references/escolher-o-que-desligar.md` antes do passo 6.
- **Google TV:** a propaganda vem da própria tela inicial do Google (`launcherx`), que fica. A limpeza deixa a TV mais leve, mas **a tela vai continuar com recomendações**: diga isso antes de começar. Muitas Google TV têm o **"Modo somente apps"** nas configurações da conta Google da TV (algo como Configurações → Contas e login → a sua conta), que tira as fileiras de filmes; procure com ela na TV, porque o caminho muda de modelo pra modelo.
- **A última linha é a tela inicial atual.** A tela nova do tv-limpa **só entra se ela for `com.google.android.tvlauncher/...`** (a tela clássica do Android TV). Se for `com.google.android.apps.tv.launcherx` (Google TV) ou uma tela do fabricante, **faça só a limpeza** e explique por quê: trocar a tela dessas TVs não foi testado, e o script de instalação recusa sozinho.
- **Fora da TCL, os atalhos "TV aberta" e "HDMI" da tela nova não funcionam** (eles chamam os apps da TCL). Avise antes; nessas TVs ela vai trocar de entrada pelo botão **Source/Entrada** do controle.

### 5. Medir o antes (opcional, mas é o que prova o resultado)

Pra comparação ser justa: **tire a TV da tomada, ligue de novo e deixe uma hora parada na tela inicial** (ninguém usando). Aí:
```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\medir.ps1 -Tv <ip-da-tv>:5555 -Rotulo antes
```
O relatório vai pra `medidas\`. Ele abre o YouTube 3 vezes na TV pra cronometrar; avise antes.

### 6. Desligar, um grupo por vez

**TCL testada:** as listas prontas estão separadas por grupo na pasta `listas\` (e nesta skill, em `references/listas-tcl/`):
1. `tcl-beyondtv-lf1v625-1-rastreio-e-propaganda.txt`: rastreamento da TCL, caixa de mensagens com propaganda, fileiras de recomendação (12).
2. `tcl-beyondtv-lf1v625-2-apps-que-a-casa-nao-usa.txt`: Looke, Telecine, Deezer, Twitch, Facebook, Pac-Man... (9). **Pergunte quais apps ela usa** e tire da lista os que ficam.
3. `tcl-beyondtv-lf1v625-3-teimosos-da-tcl.txt`: 4 apps que a TCL religa sozinha a cada boot (veja abaixo).

**Outra TV:** monte a lista com `references/escolher-o-que-desligar.md`.

Pra **criar uma lista sem Bloco de Notas** (um pacote por linha; o `#` é comentário). O `Set-Content` escreve o arquivo inteiro de novo: pra tirar um app do grupo 2, crie uma lista nova (por exemplo `listas\grupo2-da-casa.txt`) só com os que vão sair:
```powershell
Set-Content -Path .\listas\minha-tv.txt -Encoding UTF8 -Value @('pacote.um  # motivo', 'pacote.dois  # motivo')
```

Pra cada grupo, nesta ordem:
1. **Simule e explique em palavras simples** o que vai sair:
   ```powershell
   powershell -ExecutionPolicy Bypass -File .\scripts\desligar-lista.ps1 -Tv <ip-da-tv>:5555 -Lista .\listas\<lista>.txt -Simular
   ```
   O script recusa sozinho os pacotes da lista "Não desligar nunca": os da TCL testada e a base do Android e do Google (configurações, provedores do sistema, loja, serviços do Google, o teclado do Google, Bluetooth, configuração inicial). Em outra marca, os apps do fabricante **não** estão nessa lista: a proteção ali é o seu cuidado.
2. Com o "pode", rode **sem** `-Simular`.
3. **Reinicie a TV** (`.\ferramentas\platform-tools\adb.exe -s <ip-da-tv>:5555 reboot`), avisando antes. Depois de reiniciar, o IP pode mudar: confira na TV e conecte de novo.
4. Peça pra ela **testar** o que importa: os apps que ela usa, a TV aberta, o HDMI, o controle, a busca por voz e o Chromecast.
5. Se algo parou, **religue o grupo na hora**:
   ```powershell
   powershell -ExecutionPolicy Bypass -File .\scripts\religar-lista.ps1 -Tv <ip-da-tv>:5555 -Lista .\listas\<lista>.txt
   ```

**A TV religando apps sozinha:** na TCL, o `com.tcl.appstatecontroller` religa (e até reinstala) os 4 do grupo 3 a cada boot, e sem root não dá pra impedir. A tela nova pede pra fechar esses 4 toda vez que a TV liga; na TV testada, 2 ficaram fechados e a TCL segurou a Alexa e o MagiConnect. Conte assim mesmo pra pessoa. Se outra marca religar algum app, é a mesma coisa: anote e siga.

### 7. Instalar a tela nova (só se o passo 4 deixou)

O que ela é: uma tela inicial preta, uma fileira só com os apps da casa, atalhos pra TV aberta, HDMI, Wi-Fi, todos os apps e configurações. Segurando OK num app dá pra mudar a ordem ou tirar da fileira. Ela **não usa internet** e não tem propaganda. Não tem "continuar assistindo" na tela inicial (o Android só deixa o fabricante ler isso); cada app continua mostrando o dele.

Se ela quiser filmar, avise antes: este é o momento bonito.
```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\instalar-tela.ps1 -Tv <ip-da-tv>:5555 -Apk .\tv-limpa-tela-inicial.apk -Simular
powershell -ExecutionPolicy Bypass -File .\scripts\instalar-tela.ps1 -Tv <ip-da-tv>:5555 -Apk .\tv-limpa-tela-inicial.apk
```
O script confere a tela inicial atual, instala, confere que a nova roda, só então desliga a de fábrica, aperta Home e confere quem apareceu. **Se falhar no meio, ele religa a de fábrica sozinho** e avisa. Se mostrar a mensagem vermelha de socorro, siga exatamente os comandos que ele mostra.

Depois, peça pra ela usar com o controle: andar pela fileira, abrir os apps, apertar Home de dentro de um app, trocar de entrada (pelo atalho na TCL, pelo botão Source nas outras) e **tirar da tomada e ligar** (a tela nova tem que voltar sozinha). Só diga "pronto" depois disso.

### 8. Medir o depois (se mediu o antes)

Mesma regra (tomada, uma hora parada), com `-Rotulo depois`. Compare linha por linha (memória livre, memória da tela inicial, tempo do YouTube) e diga o que mudou **e o que não mudou**. Se o "antes" foi medido antes da limpeza, a diferença é da limpeza mais a tela nova. Na TV testada, medindo só a troca da tela (a limpeza já feita), a tela inicial caiu de 64 MB pra 14 MB, a memória livre subiu de 599 MB pra 672 MB, e o YouTube abriu no mesmo tempo.

### 9. No fim: desligar a depuração (não pule)

Enquanto a depuração está ligada, **qualquer aparelho da rede Wi-Fi dela pode mandar comandos pra TV**. Peça pra ela voltar em **Opções do desenvolvedor** e desligar a **Depuração USB** (e a de rede ou sem fio). Pra mexer de novo um dia, é só religar.

## Como desfazer

1. **Os apps:** `religar-lista.ps1` com a mesma lista.
2. **A tela nova:** `powershell -ExecutionPolicy Bypass -File .\scripts\desfazer-tela.ps1 -Tv <ip-da-tv>:5555`.
3. **Último recurso:** restaurar de fábrica nas configurações da TV (volta tudo, mas apaga os logins e os apps instalados).

Nunca: root, desbloquear bootloader, trocar firmware ou desinstalar app do sistema. Se a pessoa pedir, explique o risco (garantia, TV travada, Netflix em baixa qualidade) e fique no caminho deste projeto.

Crédito: a ideia de limpar a TV pelo computador com listas por modelo veio do [cleanyourtv](https://github.com/Gathin23/cleanyourtv) (Gathin23, MIT).
