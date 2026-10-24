package com.guxtavon.tvlimpa.tela

import android.animation.ObjectAnimator
import android.animation.TimeInterpolator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Typeface
import android.util.Property
import android.view.View
import android.view.animation.PathInterpolator
import com.guxtavon.tvlimpa.R

/** Cores, curvas e fontes do protótipo (`prototipo/modelo-tela.html`), divididas entre as Views da tela. */
internal object Estilo {
    const val TEXTO = 0xFFFAFAFA.toInt()
    const val CINZA = 0xFFA1A1AA.toInt()
    const val ESCURO = 0xFF09090B.toInt()
    const val CARTAO_VAZIO = 0xFF18181B.toInt()
    const val TEXTO_CARTAO_VAZIO = 0xFFE4E4E7.toInt()
    const val VERMELHO_CLARO = 0xFFFCA5A5.toInt()
    const val VERMELHO = 0xFFB91C1C.toInt()

    /** `cubic-bezier(.2,.8,.2,1)`, a curva padrão (`--mola`). */
    val MOLA = PathInterpolator(.2f, .8f, .2f, 1f)

    /** `ease` do CSS. */
    val EASE = PathInterpolator(.25f, .1f, .25f, 1f)

    /** A curva do fade do brilho: `cubic-bezier(.2,.7,.2,1)`. */
    val CURVA_BRILHO = PathInterpolator(.2f, .7f, .2f, 1f)

    private var regular: Typeface? = null
    private var medio: Typeface? = null
    private var negrito: Typeface? = null

    fun regular(ctx: Context): Typeface = regular ?: carregar(ctx, R.font.inter_regular).also { regular = it }
    fun medio(ctx: Context): Typeface = medio ?: carregar(ctx, R.font.inter_medium).also { medio = it }
    fun negrito(ctx: Context): Typeface = negrito ?: carregar(ctx, R.font.inter_bold).also { negrito = it }

    private fun carregar(ctx: Context, id: Int): Typeface = try {
        ctx.applicationContext.resources.getFont(id)
    } catch (e: Exception) {
        Typeface.DEFAULT
    }
}

/** dp pra px (na TV, 1 dp = 2 px). */
internal fun Context.dp(valor: Float): Float = valor * resources.displayMetrics.density

/** Interpolação linear. */
internal fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t

/** Interpolação de cor ARGB, sem alocar. */
internal fun lerpCor(a: Int, b: Int, t: Float): Int {
    fun canal(c: Int, s: Int) = (c ushr s) and 0xFF
    fun mistura(s: Int) = (canal(a, s) + (canal(b, s) - canal(a, s)) * t + .5f).toInt().coerceIn(0, 255)
    return (mistura(24) shl 24) or (mistura(16) shl 16) or (mistura(8) shl 8) or mistura(0)
}

/** O vetor vira uma máscara ALPHA_8: desenhada com uma tinta, sai na cor da tinta. */
internal fun mascaraDe(ctx: Context, id: Int, lado: Int): Bitmap {
    val cheio = Bitmap.createBitmap(lado, lado, Bitmap.Config.ARGB_8888)
    ctx.getDrawable(id)?.let {
        it.setBounds(0, 0, lado, lado)
        it.draw(Canvas(cheio))
    }
    val alfa = cheio.extractAlpha()
    cheio.recycle()
    return alfa
}

/** Um `ObjectAnimator` de uma propriedade de View até [para], em [ms] com a [curva]. */
internal fun anima(v: View, p: Property<View, Float>, para: Float, ms: Long, curva: TimeInterpolator): ObjectAnimator =
    ObjectAnimator.ofFloat(v, p, para).apply {
        duration = ms
        interpolator = curva
    }
