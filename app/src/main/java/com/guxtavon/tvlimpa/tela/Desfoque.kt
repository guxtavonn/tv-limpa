package com.guxtavon.tvlimpa.tela

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View

/**
 * O fundo desfocado do modal. No Android 9 não existe `RenderEffect`: a tela é desenhada num bitmap
 * a 1/10 do tamanho (192x108 na TV), que leva duas passadas de média 3x3 e depois é esticado com
 * filtro por quem desenha (ver `ModalOrganizar`). Fica perto do `blur(18px)` do protótipo.
 *
 * O bitmap é um só, reaproveitado a cada abertura (83 KB); nunca se aloca a tela inteira.
 */
object Desfoque {
    private const val FRACAO = 10
    private const val FUNDO = 0xFF070707.toInt()

    private var bitmap: Bitmap? = null
    private var px = IntArray(0)
    private var tmp = IntArray(0)

    /** Desenha [v] reduzido e borrado. Chamar na thread principal, com o modal ainda fora da tela. */
    fun de(v: View): Bitmap {
        val w = maxOf(1, v.width / FRACAO)
        val h = maxOf(1, v.height / FRACAO)
        val b = bitmap?.takeIf { it.width == w && it.height == h } ?: Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).also {
            bitmap = it
            px = IntArray(w * h)
            tmp = IntArray(w * h)
        }
        b.eraseColor(FUNDO)
        if (v.width > 0 && v.height > 0) {
            try {
                val c = Canvas(b)
                c.scale(w / v.width.toFloat(), h / v.height.toFloat())
                v.draw(c)
            } catch (e: Exception) {
                // Algo que não desenha em software: fica o fundo liso.
                b.eraseColor(FUNDO)
            }
        }
        b.getPixels(px, 0, w, 0, 0, w, h)
        repeat(2) {
            media(px, tmp, w, h, 1, 0)
            media(tmp, px, w, h, 0, 1)
        }
        b.setPixels(px, 0, w, 0, 0, w, h)
        return b
    }

    /** Média de 3 vizinhos na direção (dx, dy), canal por canal, com a borda repetida. */
    private fun media(de: IntArray, para: IntArray, w: Int, h: Int, dx: Int, dy: Int) {
        for (y in 0 until h) {
            for (x in 0 until w) {
                val a = de[minOf(h - 1, maxOf(0, y - dy)) * w + minOf(w - 1, maxOf(0, x - dx))]
                val m = de[y * w + x]
                val c = de[minOf(h - 1, y + dy) * w + minOf(w - 1, x + dx)]
                para[y * w + x] = (canal(a, m, c, 24) shl 24) or (canal(a, m, c, 16) shl 16) or
                    (canal(a, m, c, 8) shl 8) or canal(a, m, c, 0)
            }
        }
    }

    private fun canal(a: Int, m: Int, c: Int, s: Int): Int =
        (((a ushr s) and 0xFF) + ((m ushr s) and 0xFF) + ((c ushr s) and 0xFF)) / 3
}
