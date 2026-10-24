package com.guxtavon.tvlimpa.tela

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Outline
import android.graphics.Paint
import android.view.View
import android.view.ViewOutlineProvider
import android.view.animation.LinearInterpolator

/**
 * Um botão de atalho: ícone de linha e texto, com fundo quase transparente e fio de 1 px.
 * Com foco fica branco com texto preto e cresce 8% (transform 300 ms na mola; fundo e cor 300 ms
 * no `ease`). O ícone vira uma máscara alfa uma vez só e é pintado com a cor da tinta, então a
 * animação não aloca nada.
 */
class AtalhoView(ctx: Context, iconeRes: Int, texto: String) : View(ctx) {
    var texto: String = texto
        private set
    private var ponto = false

    private val altura = ctx.dp(32f)
    private val margem = ctx.dp(15f)
    private val vao = ctx.dp(7f)
    private val ladoIcone = ctx.dp(13f).toInt()
    private val raioFundo = ctx.dp(7f)
    private val raioPonto = ctx.dp(2.5f)
    private val vaoPonto = ctx.dp(5f)

    private val icone: Bitmap = mascaraDe(ctx, iconeRes, ladoIcone)
    private val fundo = Paint(Paint.ANTI_ALIAS_FLAG)
    private val fio = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1f
    }
    private val tinta = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
        typeface = Estilo.medio(ctx)
        textSize = ctx.dp(12f)
    }
    private val tintaPonto = Paint(Paint.ANTI_ALIAS_FLAG)
    private var larguraTexto = 0f
    private val meioTexto = tinta.fontMetrics.let { -(it.ascent + it.descent) / 2f }

    private var cor = 0f
    private var corDe = 0f
    private var mov = 0f
    private var movDe = 0f
    private var alvo = 0f
    private var anim: ValueAnimator? = null

    init {
        medirTexto()
        outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, raioFundo)
            }
        }
        outlineSpotShadowColor = Color.BLACK
        outlineAmbientShadowColor = Color.BLACK
        pintar()
    }

    /** Troca o texto; [ponto] liga o ponto vermelho antes dele (o "Sem internet"). */
    fun texto(novo: String, ponto: Boolean) {
        if (novo == texto && ponto == this.ponto) return
        texto = novo
        this.ponto = ponto
        medirTexto()
        requestLayout()
        invalidate()
    }

    fun focar(sim: Boolean) {
        val novo = if (sim) 1f else 0f
        if (novo == alvo) return
        alvo = novo
        anim?.cancel()
        corDe = cor
        movDe = mov
        anim = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = DURACAO
            interpolator = LinearInterpolator()
            addUpdateListener {
                val f = it.animatedFraction
                cor = lerp(corDe, alvo, Estilo.EASE.getInterpolation(f))
                mov = lerp(movDe, alvo, Estilo.MOLA.getInterpolation(f))
                pintar()
            }
            start()
        }
    }

    private fun pintar() {
        val e = lerp(1f, ESCALA_FOCO, mov)
        scaleX = e
        scaleY = e
        translationZ = mov * context.dp(9f)
        fundo.color = lerpCor(FUNDO, Estilo.TEXTO, cor)
        fio.color = lerpCor(FIO, FIO and 0xFFFFFF, cor)
        tinta.color = lerpCor(Estilo.CINZA, Estilo.ESCURO, cor)
        tintaPonto.color = lerpCor(Estilo.VERMELHO_CLARO, Estilo.VERMELHO, cor)
        invalidate()
    }

    private fun medirTexto() {
        larguraTexto = tinta.measureText(texto)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val extraPonto = if (ponto) raioPonto * 2 + vaoPonto else 0f
        val w = margem * 2 + ladoIcone + vao + extraPonto + larguraTexto
        setMeasuredDimension(Math.ceil(w.toDouble()).toInt(), altura.toInt())
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        canvas.drawRoundRect(0f, 0f, w, h, raioFundo, raioFundo, fundo)
        canvas.drawRoundRect(.5f, .5f, w - .5f, h - .5f, raioFundo, raioFundo, fio)
        var x = margem
        canvas.drawBitmap(icone, x, (h - ladoIcone) / 2f, tinta)
        x += ladoIcone + vao
        if (ponto) {
            canvas.drawCircle(x + raioPonto, h / 2f, raioPonto, tintaPonto)
            x += raioPonto * 2 + vaoPonto
        }
        canvas.drawText(texto, x, h / 2f + meioTexto, tinta)
    }

    private companion object {
        const val DURACAO = 300L
        const val ESCALA_FOCO = 1.08f

        /** `rgba(255,255,255,.04)`. */
        const val FUNDO = 0x0AFFFFFF

        /** `--linha`: `rgba(255,255,255,.08)`. */
        const val FIO = 0x14FFFFFF
    }
}
