package com.guxtavon.tvlimpa.tela

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.view.View
import android.widget.FrameLayout

/**
 * O brilho na cor do app com foco, mais a vinheta que escurece pra baixo.
 * Duas camadas que trocam com fade de 900 ms (igual ao `b0`/`b1` do protótipo): a nova recebe a
 * cor e acende; a outra apaga. O fade é só a propriedade `alpha` da View (não redesenha nada);
 * os gradientes são criados uma vez por troca, nunca no `onDraw`.
 *
 * (Tentei pôr a vinheta como máscara `ComposeShader` dentro do gradiente pra economizar uma
 * pintura da tela; no Android 9 do emulador o resultado saiu branco chapado. Fica a View.)
 */
class BrilhoView(ctx: Context) : FrameLayout(ctx) {
    private val camadas = arrayOf(Camada(ctx), Camada(ctx))
    private var atual = 0
    private var corAtual = -1
    private var neutroAtual = false

    init {
        for (c in camadas) {
            c.alpha = 0f
            c.visibility = View.INVISIBLE
            addView(c, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        }
        addView(Vinheta(ctx), LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
    }

    /** [cor] em `0xRRGGBB`. [neutro] (atalhos) usa um alfa menor. */
    fun trocar(cor: Int, neutro: Boolean) {
        if (cor == corAtual && neutro == neutroAtual) return
        corAtual = cor
        neutroAtual = neutro
        val nova = camadas[1 - atual]
        val velha = camadas[atual]
        atual = 1 - atual

        nova.pintar(cor, if (neutro) FATOR_NEUTRO else 1f)
        nova.visibility = View.VISIBLE
        nova.fade(1f)
        velha.fade(0f)
    }

    /** Uma camada: o brilho grande em 22% x 28% e o fraco em 85% x 15%. */
    private class Camada(ctx: Context) : View(ctx) {
        private val grande = Paint(Paint.ANTI_ALIAS_FLAG)
        private val fraco = Paint(Paint.ANTI_ALIAS_FLAG)
        private var cor = 0
        private var fator = 0f
        private var gx = 0f
        private var gy = 0f
        private var gr = 0f
        private var fx = 0f
        private var fy = 0f
        private var fr = 0f

        private var anim: Animator? = null

        override fun hasOverlappingRendering() = false

        /**
         * Cada camada tem o seu animador; o novo cancela o anterior na hora (com o
         * `ViewPropertyAnimator`, duas trocas no mesmo quadro juntavam as animações e o
         * "esconder no fim" da primeira apagava a camada que devia acender).
         */
        fun fade(para: Float) {
            anim?.cancel()
            anim = ObjectAnimator.ofFloat(this, View.ALPHA, para).apply {
                duration = DURACAO
                interpolator = Estilo.CURVA_BRILHO
                if (para == 0f) {
                    addListener(object : AnimatorListenerAdapter() {
                        private var cancelado = false
                        override fun onAnimationCancel(animation: Animator) {
                            cancelado = true
                        }

                        override fun onAnimationEnd(animation: Animator) {
                            // Apagada de vez: some do desenho (não pinta a tela à toa).
                            if (!cancelado) visibility = View.INVISIBLE
                        }
                    })
                }
                start()
            }
        }

        fun pintar(cor: Int, fator: Float) {
            this.cor = cor
            this.fator = fator
            montar()
            invalidate()
        }

        override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
            gx = w * .22f
            gy = h * .28f
            gr = w * .60f
            fx = w * .85f
            fy = h * .15f
            fr = w * .48f
            montar()
        }

        private fun montar() {
            if (gr <= 0f) return
            val g = RadialGradient(
                gx, gy, gr,
                intArrayOf(comAlfa(.42f), comAlfa(.10f), comAlfa(0f)),
                floatArrayOf(0f, .45f, .72f),
                Shader.TileMode.CLAMP,
            )
            val f = RadialGradient(
                fx, fy, fr,
                intArrayOf(comAlfa(.14f), comAlfa(0f)),
                floatArrayOf(0f, .70f),
                Shader.TileMode.CLAMP,
            )
            grande.shader = g
            fraco.shader = f
        }

        private fun comAlfa(a: Float): Int = ((a * fator * 255f + .5f).toInt() shl 24) or (cor and 0xFFFFFF)

        override fun onDraw(canvas: Canvas) {
            if (grande.shader == null) return
            // Só o disco onde o gradiente não é zero, em vez da tela inteira.
            canvas.drawCircle(gx, gy, gr * .72f, grande)
            canvas.drawCircle(fx, fy, fr * .70f, fraco)
        }
    }

    /** `linear-gradient(180deg, rgba(7,7,7,0) 0%, rgba(7,7,7,.35) 55%, rgba(7,7,7,.92) 100%)`. */
    private class Vinheta(ctx: Context) : View(ctx) {
        private val tinta = Paint()

        override fun onSizeChanged(w: Int, h: Int, ow: Int, oh: Int) {
            tinta.shader = LinearGradient(
                0f, 0f, 0f, h.toFloat(),
                intArrayOf(0x00070707, 0x59070707, 0xEB070707.toInt()),
                floatArrayOf(0f, .55f, 1f),
                Shader.TileMode.CLAMP,
            )
        }

        override fun onDraw(canvas: Canvas) {
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), tinta)
        }
    }

    private companion object {
        const val DURACAO = 900L

        /** Nos atalhos o brilho é a cor neutra, mais fraco. */
        const val FATOR_NEUTRO = .4f
    }
}
