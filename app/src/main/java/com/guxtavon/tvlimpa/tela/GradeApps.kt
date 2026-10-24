package com.guxtavon.tvlimpa.tela

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.content.Context
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import com.guxtavon.tvlimpa.nucleo.AppInfo
import com.guxtavon.tvlimpa.nucleo.Tecla

/**
 * A grade "Todos os apps" (`#grade` do protótipo): título, a dica e 5 colunas do mesmo cartão da
 * fileira, com o nome embaixo do que tem foco ("· na tela inicial" quando estiver na fileira).
 * Entra subindo 20 dp com fade (opacidade 450 ms, movimento 550 ms, na mola) e sai fazendo o
 * contrário. O foco anda como no `mexe` do protótipo; com mais de 3 linhas a grade rola pra a
 * linha do foco ficar entre as 3 de cima (a 4ª aparece cortada embaixo, sinal de que tem mais).
 *
 * O brilho de trás é da [TelaInicial]: [aoFocar] avisa a cor do app com foco.
 */
class GradeApps(
    ctx: Context,
    private val aoOk: (AppInfo) -> Unit,
    private val aoSegurar: (AppInfo) -> Unit,
) : FrameLayout(ctx) {

    /** Cor do brilho (`0xRRGGBB`) do app que ganhou o foco. */
    var aoFocar: ((Int) -> Unit)? = null

    private val titulo = TextView(ctx).apply {
        typeface = Estilo.negrito(ctx)
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 32f)
        letterSpacing = -.035f
        setTextColor(Estilo.TEXTO)
        includeFontPadding = false
        text = TITULO
    }
    private val sub = TextView(ctx).apply {
        typeface = Estilo.regular(ctx)
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12f)
        setTextColor(Estilo.CINZA)
        includeFontPadding = false
        text = DICA
    }

    /**
     * A área da grade, que começa logo abaixo da dica: as linhas que rolam pra cima são recortadas
     * aqui e não passam por cima do título. (No Android quem recorta um filho nos limites dele é o
     * `clipChildren` do pai: esta View é recortada pela [GradeApps]; dentro dela nada é recortado.)
     */
    private val janela = FrameLayout(ctx).apply {
        clipChildren = false
        clipToPadding = false
    }
    private val malha = Malha(ctx)

    private var apps: List<AppInfo> = emptyList()
    private var cartoes: List<CartaoView> = emptyList()
    private var gi = 0
    private var primeiraLinha = 0
    private var malhaAlvo = 0f
    private var apertado: CartaoView? = null
    private var anim: Animator? = null

    /** Aberta (ou abrindo); fechando já conta como fechada. */
    var aberta = false
        private set

    init {
        visibility = View.INVISIBLE
        alpha = 0f
        clipChildren = true
        clipToPadding = false
        val wc = LayoutParams.WRAP_CONTENT
        val mm = LayoutParams.MATCH_PARENT
        addView(titulo, LayoutParams(wc, wc).apply {
            leftMargin = px(60f)
            topMargin = px(48f)
        })
        addView(sub, LayoutParams(wc, wc).apply {
            leftMargin = px(61f)
            topMargin = px(90f)
        })
        janela.addView(malha, LayoutParams(mm, wc).apply {
            leftMargin = px(60f)
            rightMargin = px(60f)
            topMargin = px(FOLGA_DP)
        })
        addView(janela, LayoutParams(mm, mm).apply { topMargin = px(TOPO_DP - FOLGA_DP) })
    }

    /** Monta (ou atualiza) a grade, mantendo o foco no mesmo lugar. */
    fun mostrar(apps: List<AppInfo>, naFileira: Set<String>) {
        if (apps != this.apps) {
            apertar(false)
            val antigos = cartoes.associateBy { it.app }
            cartoes = apps.map { app -> antigos[app] ?: CartaoView(context, app, Capas.pronta(app.pacote)) }
            malha.removeAllViews()
            for (c in cartoes) malha.addView(c)
            this.apps = apps
        }
        for (c in cartoes) c.rotulo(if (c.app.pacote in naFileira) c.app.nome + NA_TELA else c.app.nome)
        gi = gi.coerceIn(0, maxOf(0, apps.size - 1))
        desenhar()
    }

    /** Põe nos cartões as capas que ficaram prontas depois (preparadas em segundo plano). */
    fun atualizarCapas() {
        var focado = false
        for ((i, c) in cartoes.withIndex()) {
            if (c.temCapa) continue
            val p = Capas.pronta(c.app.pacote) ?: continue
            c.trocarCapa(p)
            if (i == gi) focado = true
        }
        if (focado && aberta) aoFocar?.invoke(cartoes[gi].cor)
    }

    /** Entra com o foco no primeiro app. */
    fun abrir() {
        gi = 0
        primeiraLinha = 0
        malhaAlvo = 0f
        malha.animate().cancel()
        malha.translationY = 0f
        aberta = true
        desenhar()
        animar(true)
    }

    fun fechar() {
        if (!aberta) return
        aberta = false
        apertar(false)
        animar(false)
    }

    /** Some na hora (o Home). */
    fun fecharJa() {
        aberta = false
        apertar(false)
        anim?.cancel()
        anim = null
        setLayerType(LAYER_TYPE_NONE, null)
        alpha = 0f
        visibility = View.INVISIBLE
    }

    /** Uma tecla com a grade aberta. O voltar fica com a activity (fecha a grade). */
    fun tecla(t: Tecla): Boolean {
        val n = apps.size
        when (t) {
            Tecla.DIREITA -> if (gi < n - 1) gi++ else return false
            Tecla.ESQUERDA -> if (gi > 0) gi-- else return false
            Tecla.BAIXO -> if (gi + COLUNAS < n) gi += COLUNAS else return false
            Tecla.CIMA -> if (gi - COLUNAS >= 0) gi -= COLUNAS else return false
            Tecla.OK -> {
                aoOk(apps.getOrNull(gi) ?: return false)
                return true
            }
            Tecla.VOLTAR -> return false
        }
        apertar(false)
        desenhar()
        return true
    }

    /** OK apertado (ainda sem soltar): o cartão com foco afunda um pouco. */
    fun apertar(sim: Boolean) {
        val alvo = if (sim && aberta) cartoes.getOrNull(gi) else null
        if (alvo === apertado) return
        apertado?.segurar(false)
        apertado = alvo
        alvo?.segurar(true)
    }

    /** OK segurado: chama `aoSegurar` com o app do foco. */
    fun segurar(): Boolean {
        apertar(false)
        val app = apps.getOrNull(gi) ?: return false
        aoSegurar(app)
        return true
    }

    private fun desenhar() {
        cartoes.forEachIndexed { i, c -> c.focar(i == gi) }
        if (aberta) cartoes.getOrNull(gi)?.let { aoFocar?.invoke(it.cor) }
        rolar()
    }

    private fun rolar() {
        val linha = gi / COLUNAS
        if (linha < primeiraLinha) {
            primeiraLinha = linha
        } else if (linha > primeiraLinha + LINHAS_VISIVEIS - 1) {
            primeiraLinha = linha - LINHAS_VISIVEIS + 1
        }
        val ultima = maxOf(0, (apps.size - 1) / COLUNAS - LINHAS_VISIVEIS + 1)
        primeiraLinha = primeiraLinha.coerceIn(0, ultima)
        val y = -primeiraLinha * malha.passoLinha().toFloat()
        if (y != malhaAlvo) {
            malhaAlvo = y
            malha.animate().translationY(y).setDuration(ROLAR_MS).setInterpolator(Estilo.MOLA)
        }
    }

    /** `.tela`: opacidade 450 ms e movimento 550 ms, os dois na mola. */
    private fun animar(entrando: Boolean) {
        anim?.cancel()
        visibility = View.VISIBLE
        if (entrando && alpha == 0f) translationY = context.dp(DESCE_DP)
        setLayerType(LAYER_TYPE_HARDWARE, null)
        anim = AnimatorSet().apply {
            playTogether(
                anima(this@GradeApps, View.ALPHA, if (entrando) 1f else 0f, OPACIDADE_MS, Estilo.MOLA),
                anima(this@GradeApps, View.TRANSLATION_Y, if (entrando) 0f else context.dp(DESCE_DP), MOVIMENTO_MS, Estilo.MOLA),
            )
            addListener(object : AnimatorListenerAdapter() {
                private var cancelado = false
                override fun onAnimationCancel(animation: Animator) {
                    cancelado = true
                }

                override fun onAnimationEnd(animation: Animator) {
                    if (cancelado) return
                    setLayerType(LAYER_TYPE_NONE, null)
                    if (!entrando) visibility = View.INVISIBLE
                }
            })
            start()
        }
    }

    private fun px(dp: Float): Int = context.dp(dp).toInt()

    /** As 5 colunas: cartões 16:9 com vão de 20 dp entre colunas e 28 dp entre linhas. */
    private class Malha(ctx: Context) : ViewGroup(ctx) {
        private val vaoColuna = ctx.dp(20f).toInt()
        private val vaoLinha = ctx.dp(28f).toInt()
        private var largura = 0
        private var altura = 0

        init {
            clipChildren = false
            clipToPadding = false
        }

        fun passoLinha(): Int = altura + vaoLinha

        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            val w = MeasureSpec.getSize(widthMeasureSpec)
            largura = (w - vaoColuna * (COLUNAS - 1)) / COLUNAS
            altura = largura * 9 / 16
            val ew = MeasureSpec.makeMeasureSpec(largura, MeasureSpec.EXACTLY)
            val eh = MeasureSpec.makeMeasureSpec(altura, MeasureSpec.EXACTLY)
            for (i in 0 until childCount) getChildAt(i).measure(ew, eh)
            val linhas = (childCount + COLUNAS - 1) / COLUNAS
            // Mais alta que a janela quando há muitas linhas: as de baixo ficam fora e são recortadas.
            setMeasuredDimension(w, maxOf(0, linhas * passoLinha() - vaoLinha))
        }

        override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
            for (i in 0 until childCount) {
                val x = (i % COLUNAS) * (largura + vaoColuna)
                val y = (i / COLUNAS) * passoLinha()
                getChildAt(i).layout(x, y, x + largura, y + altura)
            }
        }
    }

    private companion object {
        const val TITULO = "Todos os apps"
        const val DICA = "segure OK num app pra pôr na tela inicial"
        const val NA_TELA = " · na tela inicial"
        const val COLUNAS = 5
        const val LINHAS_VISIVEIS = 3

        /** Topo da grade (280 px do protótipo). */
        const val TOPO_DP = 140f

        /** Espaço acima da grade dentro do recorte, pra o cartão com foco (1,14 e 5 dp acima) não ser cortado. */
        const val FOLGA_DP = 16f
        const val DESCE_DP = 20f
        const val OPACIDADE_MS = 450L
        const val MOVIMENTO_MS = 550L
        const val ROLAR_MS = 380L
    }
}
