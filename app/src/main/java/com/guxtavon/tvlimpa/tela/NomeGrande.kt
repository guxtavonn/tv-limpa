package com.guxtavon.tvlimpa.tela

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Paint
import android.text.TextUtils
import android.util.TypedValue
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView

/**
 * O nome grande do que está com foco. Cada troca cria um `TextView` novo, que entra de +12 dp pra 0
 * com fade em 550 ms; o que estava na tela sai pra -12 dp com fade em 450 ms e é removido no fim da
 * própria animação. Cada camada tem a sua animação (nada de timer compartilhado), então setas
 * rápidas nunca deixam dois nomes "parados" na tela.
 */
class NomeGrande(ctx: Context) : FrameLayout(ctx) {
    private val fonte = Estilo.negrito(ctx)
    private val tamanho = ctx.dp(TAMANHO_DP)
    private val deslocamento = ctx.dp(12f)

    /** `line-height: 1` do CSS: sobe o texto pra a linha ter a altura da fonte, como no protótipo. */
    private val ajusteTopo: Int

    private var visivel: String? = null
    private var entrando: Camada? = null

    init {
        clipChildren = false
        val p = Paint().apply {
            typeface = fonte
            textSize = tamanho
        }
        val fm = p.fontMetrics
        ajusteTopo = ((tamanho - (fm.descent - fm.ascent)) / 2f).toInt()
    }

    fun mostrar(texto: String) {
        if (texto == visivel) return
        visivel = texto
        entrando?.let { sair(it) }

        val n = Camada(context).apply {
            text = texto
            typeface = fonte
            setTextSize(TypedValue.COMPLEX_UNIT_PX, tamanho)
            letterSpacing = -.045f
            setTextColor(Estilo.TEXTO)
            includeFontPadding = false
            setSingleLine(true)
            ellipsize = TextUtils.TruncateAt.END
            alpha = 0f
            translationX = deslocamento
        }
        val lp = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply { topMargin = ajusteTopo }
        addView(n, lp)
        val anim = AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(n, View.TRANSLATION_X, deslocamento, 0f).apply { interpolator = Estilo.MOLA },
                ObjectAnimator.ofFloat(n, View.ALPHA, 0f, 1f).apply { interpolator = Estilo.EASE },
            )
            duration = ENTRA_MS
        }
        n.anim = anim
        anim.start()
        entrando = n
    }

    private fun sair(v: Camada) {
        v.anim?.cancel()
        val anim = AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(v, View.TRANSLATION_X, -deslocamento).apply { interpolator = Estilo.MOLA },
                ObjectAnimator.ofFloat(v, View.ALPHA, 0f).apply { interpolator = Estilo.EASE },
            )
            duration = SAI_MS
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    removeView(v)
                }
            })
        }
        v.anim = anim
        anim.start()
    }

    /** Uma camada de nome, com a animação que está rodando nela. */
    private class Camada(ctx: Context) : TextView(ctx) {
        var anim: Animator? = null
    }

    private companion object {
        const val TAMANHO_DP = 60f
        const val ENTRA_MS = 550L
        const val SAI_MS = 450L
    }
}
