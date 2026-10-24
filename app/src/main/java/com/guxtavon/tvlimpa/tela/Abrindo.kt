package com.guxtavon.tvlimpa.tela

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.Keyframe
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView

/**
 * A tela de abrindo (`.abrindo` do protótipo): fundo preto, o banner no meio, "abrindo <nome>" e
 * três pontinhos pulsando. Entra e sai com fade de 350 ms. Sai sozinha em 3 s se o app não abrir
 * (quem chama também manda [esconder] no `onPause`). Com [erro], troca o texto por
 * "não deu pra abrir <nome>" e sai em 2 s.
 *
 * Os pontinhos são as únicas animações sem fim do app: só rodam enquanto esta tela aparece.
 */
class Abrindo(ctx: Context) : FrameLayout(ctx) {
    private val capa = View(ctx)
    private val texto = TextView(ctx).apply {
        typeface = Estilo.regular(ctx)
        setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13f)
        setTextColor(Estilo.CINZA)
        includeFontPadding = false
        setSingleLine(true)
    }
    private val pontos = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
    private val pulsos: List<ValueAnimator>
    private var fade: Animator? = null
    private val esconderDepois = Runnable { esconder() }

    /** Na tela (ou entrando); saindo já conta como fora. */
    var visivel = false
        private set

    init {
        visibility = View.GONE
        setBackgroundColor(Color.BLACK)
        // Toque/OK não passam pra tela de baixo enquanto esta aparece.
        isClickable = true
        val lado = px(4f)
        pulsos = List(3) { i ->
            val p = View(ctx).apply {
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(PONTO)
                }
                alpha = .25f
            }
            pontos.addView(p, LinearLayout.LayoutParams(lado, lado).apply {
                leftMargin = ctx.dp(2.5f).toInt()
                rightMargin = ctx.dp(2.5f).toInt()
            })
            // @keyframes pulsa {0%,100% {opacity:.25} 50% {opacity:1}}, 1 s, ease em cada trecho.
            val valores = PropertyValuesHolder.ofKeyframe(
                View.ALPHA,
                Keyframe.ofFloat(0f, .25f),
                Keyframe.ofFloat(.5f, 1f).apply { interpolator = Estilo.EASE },
                Keyframe.ofFloat(1f, .25f).apply { interpolator = Estilo.EASE },
            )
            ObjectAnimator.ofPropertyValuesHolder(p, valores).apply {
                duration = 1000
                startDelay = i * 150L
                repeatCount = ValueAnimator.INFINITE
                interpolator = LinearInterpolator()
            }
        }

        val coluna = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            addView(capa, LinearLayout.LayoutParams(px(160f), px(90f)).apply { bottomMargin = px(14f) })
            addView(texto, LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT))
            addView(pontos, LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply { topMargin = px(14f) })
        }
        addView(coluna, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.CENTER))
    }

    fun mostrar(nome: String, capa: Drawable?) {
        removeCallbacks(esconderDepois)
        this.capa.background = capa
        this.capa.visibility = if (capa == null) View.GONE else View.VISIBLE
        texto.text = "abrindo $nome"
        pontos.visibility = View.VISIBLE
        pulsar(true)
        entrar()
        postDelayed(esconderDepois, ESPERA_MS)
    }

    /** O app não abriu: avisa por 2 s e sai. Sem [mostrar] antes, aparece só o texto. */
    fun erro(nome: String) {
        removeCallbacks(esconderDepois)
        if (!visivel) {
            capa.background = null
            capa.visibility = View.GONE
        }
        texto.text = "não deu pra abrir $nome"
        pontos.visibility = View.INVISIBLE
        pulsar(false)
        entrar()
        postDelayed(esconderDepois, ERRO_MS)
    }

    /** Sai com fade (o app abriu, deu o tempo, ou o voltar). */
    fun esconder() {
        removeCallbacks(esconderDepois)
        if (!visivel) return
        visivel = false
        fade?.cancel()
        fade = anima(this, View.ALPHA, 0f, FADE_MS, Estilo.EASE).apply {
            addListener(object : AnimatorListenerAdapter() {
                private var cancelado = false
                override fun onAnimationCancel(animation: Animator) {
                    cancelado = true
                }

                override fun onAnimationEnd(animation: Animator) {
                    if (!cancelado) soltar()
                }
            })
            start()
        }
    }

    /** Some na hora (o Home, ou a activity parou). */
    fun sumir() {
        removeCallbacks(esconderDepois)
        visivel = false
        fade?.cancel()
        fade = null
        soltar()
    }

    override fun onDetachedFromWindow() {
        sumir()
        super.onDetachedFromWindow()
    }

    private fun entrar() {
        val jaEstava = visivel
        visivel = true
        visibility = View.VISIBLE
        if (jaEstava && fade == null) return
        fade?.cancel()
        if (!jaEstava) alpha = 0f
        fade = anima(this, View.ALPHA, 1f, FADE_MS, Estilo.EASE).apply {
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    if (fade === animation) fade = null
                }
            })
            start()
        }
    }

    private fun soltar() {
        visibility = View.GONE
        alpha = 0f
        pulsar(false)
        capa.background = null
    }

    private fun pulsar(sim: Boolean) {
        for ((i, a) in pulsos.withIndex()) {
            if (sim) {
                if (!a.isStarted) a.start()
            } else {
                a.cancel()
                pontos.getChildAt(i).alpha = .25f
            }
        }
    }

    private fun px(dp: Float): Int = context.dp(dp).toInt()

    companion object {
        private const val FADE_MS = 350L
        private const val ESPERA_MS = 3000L
        private const val ERRO_MS = 2000L

        /** `#71717a`. */
        private const val PONTO = 0xFF71717A.toInt()

        /** A capa no tamanho da tela de abrindo (160x90 dp, raio 11 dp; sem banner, ícone de 28 dp e nome de 13 dp). */
        fun capa(ctx: Context, pronta: CapaPronta?, nome: String): Drawable =
            CapaDrawable(ctx, pronta, nome, ctx.dp(11f), ctx.dp(28f), ctx.dp(13f), ctx.dp(6f))
    }
}
