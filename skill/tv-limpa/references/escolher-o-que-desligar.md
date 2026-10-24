# Escolher o que desligar numa TV que não é a TCL testada

Leia isto quando a TV da pessoa não for a TCL BeyondTV da lista pronta. A ideia é montar uma lista nova, pequena e segura, a partir do que existe na TV dela.

## 1. Ver o que tem na TV

```powershell
.\ferramentas\platform-tools\adb.exe -s <ip-da-tv>:5555 shell pm list packages -e
```

Mostra os apps **ligados** (o `-e`). Os que já vêm desligados de fábrica (`-d`) não interessam.

Pra saber o nome que aparece na tela de cada um e se ele tem ícone na TV:

```powershell
.\ferramentas\platform-tools\adb.exe -s <ip-da-tv>:5555 shell cmd package query-activities --brief -a android.intent.action.MAIN -c android.intent.category.LEANBACK_LAUNCHER
```

E pra ver o que está pesando agora (memória em KB, os maiores primeiro):

```powershell
.\ferramentas\platform-tools\adb.exe -s <ip-da-tv>:5555 shell "ps -A -o RSS,NAME | sort -rn | head -25"
```

## 2. Separar em três pilhas

Leia o nome inteiro de cada pacote e o nome que aparece na tela. **Não decida por pedaço de palavra** (`ad` aparece em `downloads`, `bi` aparece em `com.android.bips`). **Quando um pacote parecer servir pras duas pilhas, ele fica ligado.**

**Nunca desligar** (na dúvida, fica aqui):
- **a base do Android e do Google:** `android`, `com.android.systemui`, `com.android.settings`, `com.android.tv.settings`, `com.android.providers.*`, `com.android.packageinstaller`, `com.android.shell`, `com.android.bluetooth`, `com.android.inputdevices`, `com.android.externalstorage`, os WebView, `com.android.vending` (a loja), `com.google.android.gms` e `com.google.android.gsf` (serviços do Google), `com.google.android.tungsten.setupwraith` (configuração inicial);
- **tudo do fabricante que não seja claramente propaganda ou rastreamento.** Cada marca tem o seu prefixo: TCL `com.tcl.*`, Philips `org.droidtv.*`, Sony `com.sony.dtv.*`, Xiaomi `com.mitv.*`/`com.xiaomi.*`, Hisense `com.hisense.*`/`com.vidaa.*`. Ali estão a imagem, a antena, o HDMI e as entradas, o controle, o som, o modo de imagem e a configuração inicial. Sem um teste na TV dela, não mexa;
- **o teclado** (o pacote de teclado do Google ou do fabricante);
- **a voz**, se ela usa a busca por voz no controle (o YouTube usa a voz do sistema): `com.google.android.katniss` e o que o fabricante tiver de `assistant`/`mic`;
- **o que faz streaming funcionar:** ajustes de DRM e certificados (`netflix.setting`, `widevine`, `drm`, `playready`);
- **o Chromecast**, se ela usa: `com.google.android.apps.mediashell`;
- **a tela inicial atual:** ela só sai pelo `instalar-tela.ps1`, nunca pela lista.

**Pode desligar com segurança** (o mesmo tipo que foi testado na TCL):
- estatística e relatório do Google que a pessoa não usa: `com.google.android.feedback`, `com.google.android.tv.bugreportsender`, `com.google.android.partnersetup`, `com.google.android.onetimeinitializer`;
- as fileiras de recomendação da tela inicial de fábrica: `com.google.android.tvrecommendations`, `com.google.android.leanbacklauncher.recommendations`;
- o app do fabricante que é **só** propaganda, rastreamento ou "conteúdo em destaque", quando o nome na tela deixa isso claro (na TCL: `com.tcl.bi`, `com.tcl.messagebox`, `com.tcl.tv.tclhome_passive`);
- apps de conteúdo que a casa não usa (streamings, jogos, redes sociais que vieram instalados): **pergunte antes**.

**Na dúvida:** fica ligado. Um app a mais rodando custa pouca memória; um app de sistema desligado errado pode tirar o controle, a imagem ou a internet.

## 3. Montar e testar em grupos

1. Crie a lista com o comando `Set-Content` do SKILL.md (passo 6), por exemplo `listas\minha-tv.txt`: **um pacote por linha**, e `#` pro motivo (o script ignora o que vem depois do `#`).
2. Comece pelo grupo mais seguro (rastreamento e propaganda do fabricante), com **no máximo uns 10** pacotes.
3. Rode com `-Simular`, mostre a lista pra pessoa, desligue, **reinicie a TV** e peça pra ela testar antena, HDMI, controle, voz, os apps que ela usa e o Chromecast.
4. Só então o próximo grupo.
5. Depois do reboot, confira se a TV religou algum sozinha (`pm list packages -e`). Se religou, é o fabricante forçando: não insista (sem root não dá), anote e siga.

Se algo quebrar: `religar-lista.ps1` com a mesma lista, e o problema some.

## 4. Pra comparar

A lista testada da TCL (`listas\tcl-beyondtv-lf1v625.txt`) e o projeto [cleanyourtv](https://github.com/Gathin23/cleanyourtv), que tem listas de mais de 100 modelos (Sony, Philips, Xiaomi, Hisense, TCL), servem de referência pra achar o pacote parecido na TV da pessoa. Use como ponto de partida, nunca como lista pronta pra outro firmware.
