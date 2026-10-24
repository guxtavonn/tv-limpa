package com.guxtavon.tvlimpa.tela

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.LinearGradient
import android.graphics.Outline
import android.graphics.Paint
import android.graphics.Shader
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewOutlineProvider
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import android.widget.TextView
import com.guxtavon.tvlimpa.nucleo.AppInfo
import com.guxtavon.tvlimpa.nucleo.COR_NEUTRA
import kotlin.math.cos
import kotlin.math.sin

/**
 * O cartão de um app (16:9), usado na fileira e na grade.
 *
 * Sem foco: brilho 62% e saturação 85%. Com foco: escala 1,14, sobe 5 dp, sombra e o reflexo
 * diagonal. Segurando OK: escala 1,06 e sobe 2 dp. Tudo em 380 ms; o movimento usa a mola
 * `(.2,.8,.2,1)` e o filtro, a sombra e o reflexo usam o `ease` (como as `transition` do protótipo).
 *
 * Desempenho: escala, posição, sombra e o reflexo são propriedades da View (não redesenham).
 * Só durante a animação de foco a capa ganha uma camada de hardware, e o filtro de cor animado
 * vai na tinta dessa camada (`setLayerPaint`), então a capa é desenhada uma vez por animação.
 * Os filtros são pré-calculados; nada é alocado no `onDraw`. A capa (bitmap e cor) chega pronta,
 * preparada em segundo plano pelo [Capas].
 */
class CartaoView(ctx: Context, val app: AppInfo, capa: CapaPronta?) : FrameLayout(ctx) {

    /** Cor do brilho deste app (`0xRRGGBB`), calculada fora da thread principal (ver [Capas]). */
    var cor: Int = capa?.cor ?: COR_NEUTRA
        private set

    /** A capa já chegou (sem ela o cartão fica cinza até [trocarCapa]). */
    var temCapa: Boolean = capa != null
        private set

    private val raio = ctx.dp(9f)
    private val desenho = Capa(ctx, CapaDrawable(ctx, capa, app.nome, raio, ctx.dp(28f), ctx.dp(11f), ctx.dp(6f)))
    private val reflexo = Reflexo(ctx, raio)
    private var legenda: TextView? = null

    private var foco = 0f
    private var focoDe = 0f
    private var focoAlvo = 0f
    private var focoMov = 0f
    private var focoMovDe = 0f
    private var pressao = 0f
    private var pressaoDe = 0f
    private var pressaoAlvo = 0f
    private var animFoco: ValueAnimator? = null
    private var animPressao: ValueAnimator? = null

    init {
        clipChildren = false
        addView(desenho, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        reflexo.alpha = 0f
        addView(reflexo, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, raio)
            }
        }
        outlineSpotShadowColor = Color.BLACK
        outlineAmbientShadowColor = Color.BLACK
        desenho.repouso(FILTROS[0])
    }

    /** Liga ou desliga o foco, com a animação. */
    fun focar(sim: Boolean) {
        val alvo = if (sim) 1f else 0f
        if (alvo == focoAlvo) return
        focoAlvo = alvo
        legenda?.animate()?.alpha(alvo)?.setDuration(ROTULO_MS)?.setInterpolator(Estilo.EASE)
        animFoco?.cancel()
        focoDe = foco
        focoMovDe = focoMov
        desenho.camada(true)
        aplicar()
        animFoco = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = DURACAO
            interpolator = LinearInterpolator()
            addUpdateListener {
                val f = it.animatedFraction
                foco = lerp(focoDe, focoAlvo, Estilo.EASE.getInterpolation(f))
                focoMov = lerp(focoMovDe, focoAlvo, Estilo.MOLA.getInterpolation(f))
                aplicar()
            }
            addListener(object : AnimatorListenerAdapter() {
                private var cancelado = false
                override fun onAnimationCancel(animation: Animator) {
                    cancelado = true
                }

                override fun onAnimationEnd(animation: Animator) {
                    if (cancelado) return
                    foco = focoAlvo
                    focoMov = focoAlvo
                    aplicar()
                    desenho.repouso(if (focoAlvo >= 1f) null else FILTROS[0])
                    desenho.camada(false)
                }
            })
            start()
        }
    }

    /** A capa chegou depois do cartão (preparada em segundo plano). */
    fun trocarCapa(p: CapaPronta) {
        cor = p.cor
        temCapa = true
        desenho.trocar(p)
    }

    /**
     * O nome embaixo do cartão (na grade), que só aparece com foco. Fica dentro do cartão, então
     * cresce e sobe junto com ele, como o `.rot` do protótipo.
     */
    fun rotulo(texto: String) {
        val r = legenda ?: TextView(context).apply {
            typeface = Estilo.regular(context)
            setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11f)
            setTextColor(Estilo.TEXTO)
            includeFontPadding = false
            gravity = Gravity.CENTER_HORIZONTAL
            maxLines = 2
            ellipsize = TextUtils.TruncateAt.END
            alpha = focoAlvo
            translationY = height + context.dp(7f)
            this@CartaoView.addView(this, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
            legenda = this
        }
        r.text = texto
    }

    override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
        super.onSizeChanged(w, h, ow, oh)
        legenda?.translationY = h + context.dp(7f)
    }

    /** O cartão afunda um pouco enquanto o OK está apertado. */
    fun segurar(sim: Boolean) {
        val alvo = if (sim) 1f else 0f
        if (alvo == pressaoAlvo) return
        pressaoAlvo = alvo
        animPressao?.cancel()
        pressaoDe = pressao
        animPressao = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = DURACAO
            interpolator = Estilo.MOLA
            addUpdateListener {
                pressao = lerp(pressaoDe, pressaoAlvo, it.animatedFraction)
                aplicar()
            }
            start()
        }
    }

    private fun aplicar() {
        val escala = lerp(lerp(1f, ESCALA_FOCO, focoMov), ESCALA_SEGURANDO, pressao)
        scaleX = escala
        scaleY = escala
        translationY = lerp(lerp(0f, -context.dp(5f), focoMov), -context.dp(2f), pressao)
        translationZ = foco * context.dp(ELEVACAO_DP)
        reflexo.alpha = foco
        desenho.filtroDaCamada(PINTURAS[indice(foco)])
    }

    /** A capa ([CapaDrawable]) com o filtro de "apagado" e a camada de hardware da animação. */
    private class Capa(ctx: Context, private val d: CapaDrawable) : View(ctx) {
        private var filtroRepouso: ColorFilter? = null

        fun trocar(p: CapaPronta) {
            d.trocar(p)
            invalidate()
        }

        /** Liga a camada de hardware (só durante a animação do foco). */
        fun camada(ligar: Boolean) {
            val tipo = if (ligar) LAYER_TYPE_HARDWARE else LAYER_TYPE_NONE
            if (layerType == tipo) return
            setLayerType(tipo, if (ligar) PINTURAS[0] else null)
            aplicarFiltro()
        }

        fun filtroDaCamada(p: Paint) {
            if (layerType == LAYER_TYPE_HARDWARE) setLayerPaint(p)
        }

        /** O filtro fixo de quando não está animando (nulo = cores normais). */
        fun repouso(f: ColorFilter?) {
            filtroRepouso = f
            aplicarFiltro()
        }

        private fun aplicarFiltro() {
            // Com a camada ligada, o filtro vai na tinta da camada; sem ela, em cada desenho.
            d.colorFilter = if (layerType == LAYER_TYPE_HARDWARE) null else filtroRepouso
            invalidate()
        }

        override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
            d.setBounds(0, 0, w, h)
        }

        override fun onDraw(canvas: Canvas) {
            d.draw(canvas)
        }
    }

    /** O reflexo diagonal (115°, branco 28% até 0 em 38%) e o fio de 1 px do foco. */
    private class Reflexo(ctx: Context, private val raio: Float) : View(ctx) {
        private val luz = Paint(Paint.ANTI_ALIAS_FLAG)
        private val fio = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1f
            color = 0x24FFFFFF
        }

        override fun hasOverlappingRendering() = false

        override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
            // Linha do gradiente do CSS: direção (sen 115°, -cos 115°), comprimento |w sen| + |h cos|.
            val a = Math.toRadians(115.0)
            val dx = sin(a).toFloat()
            val dy = -cos(a).toFloat()
            val comp = w * Math.abs(dx) + h * Math.abs(dy)
            val x0 = w / 2f - dx * comp / 2f
            val y0 = h / 2f - dy * comp / 2f
            luz.shader = LinearGradient(
                x0, y0, x0 + dx * comp * .38f, y0 + dy * comp * .38f,
                0x47FFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP,
            )
        }

        override fun onDraw(canvas: Canvas) {
            val w = width.toFloat()
            val h = height.toFloat()
            canvas.drawRoundRect(0f, 0f, w, h, raio, raio, luz)
            canvas.drawRoundRect(.5f, .5f, w - .5f, h - .5f, raio, raio, fio)
        }
    }

    companion object {
        private const val DURACAO = 380L
        private const val ESCALA_FOCO = 1.14f
        private const val ESCALA_SEGURANDO = 1.06f
        private const val ELEVACAO_DP = 16f
        private const val PASSOS = 32
        private const val ROTULO_MS = 300L

        /** `brightness(b) saturate(s)` do CSS, de "apagado" (0) a "normal" (1), em [PASSOS] passos. */
        private val FILTROS: Array<ColorMatrixColorFilter?> = Array(PASSOS + 1) { i ->
            val t = i / PASSOS.toFloat()
            if (i == PASSOS) null else ColorMatrixColorFilter(matrizApagada(lerp(.62f, 1f, t), lerp(.85f, 1f, t)))
        }

        private val PINTURAS: Array<Paint> = Array(PASSOS + 1) { i -> Paint().apply { colorFilter = FILTROS[i] } }

        private fun indice(t: Float): Int = (t * PASSOS + .5f).toInt().coerceIn(0, PASSOS)

        /** Matriz do `filter: brightness(b) saturate(s)` (fórmulas do feColorMatrix). */
        private fun matrizApagada(b: Float, s: Float): ColorMatrix = ColorMatrix(
            floatArrayOf(
                b * (.213f + .787f * s), b * (.715f - .715f * s), b * (.072f - .072f * s), 0f, 0f,
                b * (.213f - .213f * s), b * (.715f + .285f * s), b * (.072f - .072f * s), 0f, 0f,
                b * (.213f - .213f * s), b * (.715f - .715f * s), b * (.072f + .928f * s), 0f, 0f,
                0f, 0f, 0f, 1f, 0f,
            ),
        )
    }
}
