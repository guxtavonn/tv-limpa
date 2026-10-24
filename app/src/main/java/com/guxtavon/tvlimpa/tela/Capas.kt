package com.guxtavon.tvlimpa.tela

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import com.guxtavon.tvlimpa.nucleo.AppInfo
import com.guxtavon.tvlimpa.nucleo.COR_NEUTRA
import com.guxtavon.tvlimpa.nucleo.corDoBrilho
import com.guxtavon.tvlimpa.sistema.Instalados

/** A capa de um app pronta pra desenhar: banner reduzido, ícone em bitmap (só sem banner) e a cor do brilho. */
class CapaPronta(val banner: Bitmap?, val icone: Bitmap?, val cor: Int)

/**
 * Prepara as capas fora da thread principal (ler banner e ícone do PackageManager, converter em
 * bitmap e calcular a cor pesam na TV) e guarda por pacote, pra fileira, grade, modal e tela de
 * abrindo usarem o mesmo bitmap.
 *
 * Os bitmaps são todos `ARGB_8888` de software (o `Desfoque` desenha a tela num Canvas de software,
 * que não aceita bitmap `HARDWARE`) e no máximo do tamanho em que aparecem: 224x126 dp, o cartão
 * da fileira com 4 apps e foco (195 dp x 1,14).
 */
object Capas {
    private const val MAX_L_DP = 224f
    private const val MAX_A_DP = 126f
    private const val ICONE_DP = 28f

    private val prontas = HashMap<String, CapaPronta>()

    @Synchronized
    fun pronta(pacote: String): CapaPronta? = prontas[pacote]

    /** Esquece as capas dos pacotes que não estão mais instalados. */
    @Synchronized
    fun manter(pacotes: Set<String>) {
        prontas.keys.retainAll(pacotes)
    }

    /** Na thread de fundo. Se já estiver pronta, devolve a guardada. Nunca lança exceção. */
    fun preparar(ctx: Context, app: AppInfo): CapaPronta {
        pronta(app.pacote)?.let { return it }
        val instalados = Instalados(ctx.applicationContext)
        val d = ctx.resources.displayMetrics.density
        val banner = reduzir(instalados.capa(app), (MAX_L_DP * d).toInt(), (MAX_A_DP * d).toInt())
        val icone = if (banner == null) rasterizar(instalados.icone(app), (ICONE_DP * d).toInt()) else null
        val p = CapaPronta(banner, icone, calcularCor(banner, icone))
        synchronized(this) { prontas[app.pacote] = p }
        return p
    }

    /** O banner como bitmap de software, reduzido pra cobrir no máximo [maxL]x[maxA] (object-fit: cover). */
    private fun reduzir(d: Drawable?, maxL: Int, maxA: Int): Bitmap? {
        if (d == null) return null
        return try {
            val original = (d as? BitmapDrawable)?.bitmap
            if (original != null && original.width > 0 && original.height > 0) {
                val soft = if (original.config == Bitmap.Config.HARDWARE) original.copy(Bitmap.Config.ARGB_8888, false) else original
                val esc = minOf(1f, maxOf(maxL / soft.width.toFloat(), maxA / soft.height.toFloat()))
                if (esc >= 1f) return soft
                return Bitmap.createScaledBitmap(
                    soft,
                    (soft.width * esc).toInt().coerceAtLeast(1),
                    (soft.height * esc).toInt().coerceAtLeast(1),
                    true,
                )
            }
            val w = d.intrinsicWidth.takeIf { it > 0 } ?: maxL
            val h = d.intrinsicHeight.takeIf { it > 0 } ?: maxA
            val esc = minOf(1f, maxOf(maxL / w.toFloat(), maxA / h.toFloat()))
            val bw = (w * esc).toInt().coerceAtLeast(1)
            val bh = (h * esc).toInt().coerceAtLeast(1)
            val bmp = Bitmap.createBitmap(bw, bh, Bitmap.Config.ARGB_8888)
            d.setBounds(0, 0, bw, bh)
            d.draw(Canvas(bmp))
            bmp
        } catch (e: Exception) {
            null
        } catch (e: OutOfMemoryError) {
            null
        }
    }

    private fun rasterizar(d: Drawable?, lado: Int): Bitmap? {
        if (d == null || lado <= 0) return null
        return try {
            val bmp = Bitmap.createBitmap(lado, lado, Bitmap.Config.ARGB_8888)
            d.setBounds(0, 0, lado, lado)
            d.draw(Canvas(bmp))
            bmp
        } catch (e: Exception) {
            null
        }
    }

    /** `corDoBrilho` sobre o banner reduzido pra 48x27 (ou sobre o ícone, se não houver banner). */
    private fun calcularCor(banner: Bitmap?, icone: Bitmap?): Int {
        if (banner == null && icone == null) return COR_NEUTRA
        return try {
            val pequeno = Bitmap.createBitmap(48, 27, Bitmap.Config.ARGB_8888)
            val c = Canvas(pequeno)
            val tinta = Paint(Paint.FILTER_BITMAP_FLAG)
            if (banner != null) {
                c.drawBitmap(banner, null, Rect(0, 0, 48, 27), tinta)
            } else if (icone != null) {
                c.drawBitmap(icone, null, Rect(10, 0, 37, 27), tinta)
            }
            val px = IntArray(48 * 27)
            pequeno.getPixels(px, 0, 48, 0, 0, 48, 27)
            pequeno.recycle()
            corDoBrilho(px)
        } catch (e: Exception) {
            COR_NEUTRA
        }
    }
}
