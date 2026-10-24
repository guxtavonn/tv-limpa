package com.guxtavon.tvlimpa.tela

import android.content.Context
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.Drawable
import android.text.TextPaint
import android.text.TextUtils

/**
 * A capa de um app: o banner com cantos arredondados (`object-fit: cover`), ou, sem banner, o
 * cartão cinza com o ícone e o nome (o `.gen` do protótipo). Usada no cartão, no modal e na tela
 * de abrindo, cada um com o seu raio e tamanhos. Nada é alocado no `draw`.
 */
class CapaDrawable(
    ctx: Context,
    pronta: CapaPronta?,
    private val nome: String,
    private val raio: Float,
    private val ladoIcone: Float,
    tamanhoTexto: Float,
    private val vao: Float,
) : Drawable() {
    private val tinta = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val tintaIcone = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val texto = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Estilo.medio(ctx)
        textSize = tamanhoTexto
        color = Estilo.TEXTO_CARTAO_VAZIO
        textAlign = Paint.Align.CENTER
    }
    private val matriz = Matrix()
    private val caixa = RectF()
    private val caixaIcone = RectF()
    private var pronta: CapaPronta? = null
    private var nomeCurto = nome
    private var baseTexto = 0f

    init {
        trocar(pronta)
    }

    /** A capa chegou (ou mudou): troca o que é desenhado. */
    fun trocar(p: CapaPronta?) {
        pronta = p
        val banner = p?.banner
        if (banner != null) {
            tinta.shader = BitmapShader(banner, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        } else {
            tinta.shader = null
            tinta.color = Estilo.CARTAO_VAZIO
        }
        configurar()
        invalidateSelf()
    }

    override fun onBoundsChange(b: Rect) = configurar()

    private fun configurar() {
        val b = bounds
        val w = b.width()
        val h = b.height()
        caixa.set(b)
        if (w <= 0 || h <= 0) return
        val banner = pronta?.banner
        if (banner != null) {
            val esc = maxOf(w / banner.width.toFloat(), h / banner.height.toFloat())
            matriz.setScale(esc, esc)
            matriz.postTranslate(b.left + (w - banner.width * esc) / 2f, b.top + (h - banner.height * esc) / 2f)
            tinta.shader?.setLocalMatrix(matriz)
        } else {
            val fm = texto.fontMetrics
            val alturaTexto = fm.descent - fm.ascent
            val temIcone = pronta?.icone != null
            val bloco = if (temIcone) ladoIcone + vao + alturaTexto else alturaTexto
            val topo = b.top + (h - bloco) / 2f
            val esq = b.left + (w - ladoIcone) / 2f
            caixaIcone.set(esq, topo, esq + ladoIcone, topo + ladoIcone)
            baseTexto = (if (temIcone) topo + ladoIcone + vao else topo) - fm.ascent
            nomeCurto = TextUtils.ellipsize(nome, texto, w - ladoIcone * .6f, TextUtils.TruncateAt.END).toString()
        }
    }

    override fun draw(canvas: Canvas) {
        canvas.drawRoundRect(caixa, raio, raio, tinta)
        val p = pronta
        if (p?.banner == null) {
            p?.icone?.let { canvas.drawBitmap(it, null, caixaIcone, tintaIcone) }
            canvas.drawText(nomeCurto, caixa.centerX(), baseTexto, texto)
        }
    }

    override fun setAlpha(alpha: Int) {
        tinta.alpha = alpha
        tintaIcone.alpha = alpha
        texto.alpha = alpha
        invalidateSelf()
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        tinta.colorFilter = colorFilter
        tintaIcone.colorFilter = colorFilter
        texto.colorFilter = colorFilter
        invalidateSelf()
    }

    @Deprecated("Drawable.getOpacity", ReplaceWith("PixelFormat.TRANSLUCENT"))
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}
