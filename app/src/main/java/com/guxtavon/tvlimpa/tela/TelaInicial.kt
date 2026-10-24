package com.guxtavon.tvlimpa.tela

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.guxtavon.tvlimpa.R
import com.guxtavon.tvlimpa.nucleo.AppInfo
import com.guxtavon.tvlimpa.nucleo.COR_NEUTRA
import com.guxtavon.tvlimpa.nucleo.Tecla
import com.guxtavon.tvlimpa.nucleo.textoData
import com.guxtavon.tvlimpa.nucleo.textoHora
import com.guxtavon.tvlimpa.sistema.Conexao
import java.time.LocalDateTime

/**
 * A tela inicial do protótipo: brilho, nome grande, dica, hora e data, a fileira e os atalhos.
 * O foco é controlado aqui (linha 0 = fileira, linha 1 = atalhos), com a mesma lógica do `mexe`
 * do protótipo; nenhuma View é focável pelo sistema.
 *
 * Medidas: as do protótipo em px de um palco 1920x1080, divididas por 2 (dp).
 *
 * Com a grade aberta ([cobrir]), o nome, a fileira e os atalhos encolhem pra 0,97 e somem (o
 * `#inicio.fora` do protótipo); o brilho e a hora ficam, e o brilho passa a seguir a grade ([brilhar]).
 */
class TelaInicial(
    ctx: Context,
    private val aoOk: (Alvo) -> Unit,
    private val aoSegurar: (Int) -> Unit,
) : FrameLayout(ctx) {

    private val brilho = BrilhoView(ctx)
    private val hora = texto(ctx, 22f, Estilo.medio(ctx), Estilo.TEXTO).apply {
        letterSpacing = -.02f
        fontFeatureSettings = "'tnum'"
    }
    private val data = texto(ctx, 11f, Estilo.regular(ctx), Estilo.CINZA)
    private val nome = NomeGrande(ctx)
    private val dica = texto(ctx, 13f, Estilo.regular(ctx), Estilo.CINZA).apply {
        text = DICA
        alpha = 0f
    }
    private val trilho = Trilho(ctx)

    /** O que some quando a grade abre: nome, dica, fileira e atalhos (o brilho e a hora ficam). */
    private val conteudo = FrameLayout(ctx).apply {
        clipChildren = false
        clipToPadding = false
    }
    private var coberta = false
    private var animCobrir: Animator? = null
    private val caixaAtalhos = LinearLayout(ctx).apply {
        orientation = LinearLayout.HORIZONTAL
        clipChildren = false
        clipToPadding = false
    }
    private val atalhos = listOf(
        AtalhoView(ctx, R.drawable.ic_tv, "TV aberta"),
        AtalhoView(ctx, R.drawable.ic_hdmi, "HDMI"),
        AtalhoView(ctx, R.drawable.ic_wifi, "Wi-Fi"),
        AtalhoView(ctx, R.drawable.ic_apps, "Apps"),
        AtalhoView(ctx, R.drawable.ic_config, "Configurações"),
    )
    private val alvosAtalho = listOf(Alvo.Tv, Alvo.Hdmi, Alvo.Wifi, Alvo.Apps, Alvo.Configuracoes)

    private val conexao = Conexao(ctx)
    private var ligada = false
    private val relogio = object : BroadcastReceiver() {
        override fun onReceive(c: Context, i: Intent) = atualizarHora()
    }

    private var apps: List<AppInfo> = emptyList()
    private var cartoes: List<CartaoView> = emptyList()
    private var mostrou = false
    private var linha = 0
    private var col = 0
    private var dicaAlvo = 0f
    private var primeiroVisivel = 0
    private var trilhoAlvo = 0f
    private var trilhoPendente = false

    /** Pediram o primeiro app quando a fileira ainda estava vazia (o Home antes da lista chegar). */
    private var irProPrimeiro = false
    private var apertado: CartaoView? = null

    init {
        clipChildren = false
        clipToPadding = false
        val mm = LayoutParams.MATCH_PARENT
        val wc = LayoutParams.WRAP_CONTENT
        addView(brilho, LayoutParams(mm, mm))

        val caixaHora = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.END
            // LinearLayout vertical põe MATCH_PARENT por padrão; aqui cada texto tem a própria largura.
            addView(hora, LinearLayout.LayoutParams(wc, wc))
            addView(data, LinearLayout.LayoutParams(wc, wc))
        }
        addView(caixaHora, LayoutParams(wc, wc, Gravity.TOP or Gravity.END).apply {
            topMargin = px(32f)
            rightMargin = px(48f)
        })

        addView(conteudo, LayoutParams(mm, mm))
        conteudo.addView(nome, LayoutParams(mm, wc).apply {
            leftMargin = px(60f)
            rightMargin = px(48f)
            topMargin = px(165f)
        })
        conteudo.addView(dica, LayoutParams(wc, wc).apply {
            leftMargin = px(62f)
            topMargin = px(240f)
        })
        conteudo.addView(trilho, LayoutParams(mm, wc).apply {
            leftMargin = px(60f)
            rightMargin = px(60f)
            topMargin = px(320f)
        })

        atalhos.forEachIndexed { i, a ->
            caixaAtalhos.addView(a, LinearLayout.LayoutParams(wc, wc).apply { if (i > 0) leftMargin = px(10f) })
        }
        conteudo.addView(caixaAtalhos, LayoutParams(wc, wc).apply {
            leftMargin = px(60f)
            topMargin = px(450f)
        })
    }

    /**
     * Mostra a fileira. Na primeira vez põe o foco no primeiro app; depois mantém onde estava, ou
     * vai pro app [foco] (o que acabou de mudar de lugar no modal).
     */
    fun mostrar(fileira: List<AppInfo>, foco: String? = null) {
        if (mostrou && fileira == apps && foco == null) return
        if (fileira != apps) montarCartoes(fileira)
        apps = fileira
        if (!mostrou || (irProPrimeiro && apps.isNotEmpty())) {
            mostrou = true
            focarPrimeiro()
            return
        }
        val i = if (foco == null) -1 else apps.indexOfFirst { it.pacote == foco }
        if (i >= 0) {
            linha = 0
            col = i
        }
        if (linha == 0) {
            if (apps.isEmpty()) {
                linha = 1
                col = ATALHO_APPS
            } else {
                col = col.coerceIn(0, apps.size - 1)
            }
        }
        desenhar()
    }

    /** Põe nos cartões as capas que ficaram prontas depois (preparadas em segundo plano). */
    fun atualizarCapas() {
        var mudou = false
        for (c in cartoes) {
            if (c.temCapa) continue
            val p = Capas.pronta(c.app.pacote) ?: continue
            c.trocarCapa(p)
            mudou = true
        }
        if (mudou) desenhar()
    }

    /** O primeiro app da fileira; se ela estiver vazia, o atalho "Apps". */
    fun focarPrimeiro() {
        if (apps.isEmpty()) {
            focarAtalho(ATALHO_APPS)
            irProPrimeiro = true
            return
        }
        irProPrimeiro = false
        linha = 0
        col = 0
        primeiroVisivel = 0
        desenhar()
    }

    /** O atalho "Apps" (onde o foco volta quando a grade fecha). */
    fun focarApps() = focarAtalho(ATALHO_APPS)

    fun focarAtalho(i: Int) {
        irProPrimeiro = false
        linha = 1
        col = i.coerceIn(0, atalhos.size - 1)
        desenhar()
    }

    /** Esconde (grade aberta) ou mostra de volta o nome, a fileira e os atalhos. */
    fun cobrir(sim: Boolean, animar: Boolean = true) {
        if (sim == coberta && animCobrir == null) return
        coberta = sim
        apertar(false)
        animCobrir?.cancel()
        animCobrir = null
        val alfa = if (sim) 0f else 1f
        val escala = if (sim) ESCALA_COBERTA else 1f
        conteudo.visibility = View.VISIBLE
        if (!animar) {
            conteudo.setLayerType(LAYER_TYPE_NONE, null)
            conteudo.alpha = alfa
            conteudo.scaleX = escala
            conteudo.scaleY = escala
            if (sim) conteudo.visibility = View.INVISIBLE else desenhar()
            return
        }
        if (!sim) desenhar()
        conteudo.setLayerType(LAYER_TYPE_HARDWARE, null)
        animCobrir = AnimatorSet().apply {
            playTogether(
                anima(conteudo, View.ALPHA, alfa, 450L, Estilo.MOLA),
                anima(conteudo, View.SCALE_X, escala, 550L, Estilo.MOLA),
                anima(conteudo, View.SCALE_Y, escala, 550L, Estilo.MOLA),
            )
            addListener(object : AnimatorListenerAdapter() {
                private var cancelado = false
                override fun onAnimationCancel(animation: Animator) {
                    cancelado = true
                }

                override fun onAnimationEnd(animation: Animator) {
                    if (cancelado) return
                    animCobrir = null
                    conteudo.setLayerType(LAYER_TYPE_NONE, null)
                    if (sim) conteudo.visibility = View.INVISIBLE
                }
            })
            start()
        }
    }

    /** Com a grade aberta, o brilho segue o app com foco nela. */
    fun brilhar(cor: Int) {
        if (coberta) brilho.trocar(cor, false)
    }

    /** Uma tecla do controle. Devolve `true` se mudou alguma coisa ou tratou o OK. */
    fun tecla(t: Tecla): Boolean {
        val max = if (linha == 0) apps.size - 1 else atalhos.size - 1
        when (t) {
            Tecla.DIREITA -> if (col < max) col++ else return false
            Tecla.ESQUERDA -> if (col > 0) col-- else return false
            Tecla.BAIXO -> if (linha == 0) {
                linha = 1
                col = minOf(col, atalhos.size - 1)
            } else {
                return false
            }
            Tecla.CIMA -> if (linha == 1 && apps.isNotEmpty()) {
                linha = 0
                col = minOf(col, apps.size - 1)
            } else {
                return false
            }
            Tecla.OK -> {
                aoOk(alvoAtual() ?: return false)
                return true
            }
            Tecla.VOLTAR -> return false
        }
        irProPrimeiro = false
        apertar(false)
        desenhar()
        return true
    }

    /** OK apertado (ainda sem soltar): o cartão com foco afunda um pouco. */
    fun apertar(sim: Boolean) {
        val alvo = if (sim && linha == 0 && !coberta) cartoes.getOrNull(col) else null
        if (alvo === apertado) return
        apertado?.segurar(false)
        apertado = alvo
        alvo?.segurar(true)
    }

    /**
     * OK segurado: chama `aoSegurar` com o índice do app. Nos atalhos não faz nada e devolve `false`
     * (aí o soltar do OK abre o atalho).
     */
    fun segurar(): Boolean {
        apertar(false)
        if (linha != 0 || col !in apps.indices) return false
        aoSegurar(col)
        return true
    }

    private fun alvoAtual(): Alvo? = if (linha == 0) apps.getOrNull(col)?.let { Alvo.App(it) } else alvosAtalho.getOrNull(col)

    private fun montarCartoes(fileira: List<AppInfo>) {
        apertar(false)
        val antigos = cartoes.associateBy { it.app }
        cartoes = fileira.map { app ->
            antigos[app] ?: CartaoView(context, app, Capas.pronta(app.pacote))
        }
        trilho.removeAllViews()
        for (c in cartoes) trilho.addView(c)
    }

    private fun desenhar() {
        cartoes.forEachIndexed { i, c -> c.focar(linha == 0 && i == col) }
        atalhos.forEachIndexed { i, a -> a.focar(linha == 1 && i == col) }
        if (linha == 0 && col in cartoes.indices) {
            val c = cartoes[col]
            nome.mostrar(c.app.nome)
            if (!coberta) brilho.trocar(c.cor, false)
            dica(1f)
        } else if (linha == 1) {
            nome.mostrar(atalhos[col].texto)
            if (!coberta) brilho.trocar(COR_NEUTRA, true)
            dica(0f)
        }
        rolar()
    }

    private fun dica(alfa: Float) {
        if (alfa == dicaAlvo) return
        dicaAlvo = alfa
        dica.animate().alpha(alfa).setDuration(400).setInterpolator(Estilo.EASE)
    }

    /**
     * Com 7 ou mais apps, a fileira anda pra o foco ficar sempre entre os 6 visíveis. Roda em todo
     * desenho, com o foco na fileira ou nos atalhos: se a fileira encolheu enquanto o foco estava
     * nos atalhos, ela volta pro lugar (sem isso ficava rolada, com o primeiro cartão fora da tela).
     */
    private fun rolar() {
        if (linha == 0 && col in apps.indices) {
            if (col < primeiroVisivel) {
                primeiroVisivel = col
            } else if (col > primeiroVisivel + MAX_VISIVEIS - 1) {
                primeiroVisivel = col - MAX_VISIVEIS + 1
            }
        }
        primeiroVisivel = primeiroVisivel.coerceIn(0, maxOf(0, apps.size - MAX_VISIVEIS))
        if (primeiroVisivel > 0 && trilho.width == 0) {
            // Ainda sem medida (passo zero): posiciona depois do primeiro layout.
            if (!trilhoPendente) {
                trilhoPendente = true
                trilho.post {
                    trilhoPendente = false
                    rolar()
                }
            }
            return
        }
        val x = -primeiroVisivel * trilho.passo().toFloat()
        if (x != trilhoAlvo) {
            trilhoAlvo = x
            trilho.animate().translationX(x).setDuration(380).setInterpolator(Estilo.MOLA)
        }
    }

    // --- hora e Wi-Fi: só enquanto a tela está visível ---

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        if (visibility == View.VISIBLE) ligar() else desligar()
    }

    override fun onDetachedFromWindow() {
        desligar()
        super.onDetachedFromWindow()
    }

    private fun ligar() {
        if (ligada) return
        ligada = true
        val filtro = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
        }
        context.registerReceiver(relogio, filtro)
        atualizarHora()
        conexao.observar { conectado(it) }
        // observar não avisa o estado inicial; pinta agora.
        conectado(conexao.conectado())
    }

    private fun desligar() {
        if (!ligada) return
        ligada = false
        try {
            context.unregisterReceiver(relogio)
        } catch (e: Exception) {
            // já estava solto
        }
        conexao.parar()
    }

    private fun atualizarHora() {
        val agora = LocalDateTime.now()
        hora.text = textoHora(agora.hour, agora.minute)
        data.text = textoData(agora.toLocalDate())
    }

    private fun conectado(sim: Boolean) {
        val wifi = atalhos[ATALHO_WIFI]
        wifi.texto(if (sim) "Wi-Fi" else "Sem internet", ponto = !sim)
        if (linha == 1 && col == ATALHO_WIFI) nome.mostrar(wifi.texto)
    }

    private fun px(dp: Float): Int = context.dp(dp).toInt()

    /**
     * A fileira: cartões 16:9 do mesmo tamanho, divididos na largura (vão de 20 dp). A largura é a
     * de 6 apps a partir de 6; abaixo de 4 apps fica a de 4, pra o cartão não descer sobre os atalhos.
     */
    private class Trilho(ctx: Context) : ViewGroup(ctx) {
        private val vao = ctx.dp(20f).toInt()
        private var largura = 0

        init {
            clipChildren = false
            clipToPadding = false
        }

        fun passo(): Int = largura + vao

        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            val w = MeasureSpec.getSize(widthMeasureSpec)
            val n = childCount.coerceIn(MIN_DIVISAO, MAX_VISIVEIS)
            largura = (w - vao * (n - 1)) / n
            val altura = largura * 9 / 16
            val ew = MeasureSpec.makeMeasureSpec(largura, MeasureSpec.EXACTLY)
            val eh = MeasureSpec.makeMeasureSpec(altura, MeasureSpec.EXACTLY)
            for (i in 0 until childCount) getChildAt(i).measure(ew, eh)
            setMeasuredDimension(w, altura)
        }

        override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
            for (i in 0 until childCount) {
                val x = i * (largura + vao)
                val c = getChildAt(i)
                c.layout(x, 0, x + c.measuredWidth, c.measuredHeight)
            }
        }
    }

    private companion object {
        const val DICA = "aperte OK pra abrir · segure OK pra organizar"
        const val ATALHO_WIFI = 2
        const val ATALHO_APPS = 3
        const val MAX_VISIVEIS = 6
        const val MIN_DIVISAO = 4
        const val ESCALA_COBERTA = .97f

        fun texto(ctx: Context, tamanhoDp: Float, fonte: android.graphics.Typeface, cor: Int) = TextView(ctx).apply {
            typeface = fonte
            setTextSize(TypedValue.COMPLEX_UNIT_DIP, tamanhoDp)
            setTextColor(cor)
            includeFontPadding = false
            setSingleLine(true)
        }
    }
}
