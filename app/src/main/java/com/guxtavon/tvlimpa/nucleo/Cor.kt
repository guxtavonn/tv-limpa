package com.guxtavon.tvlimpa.nucleo

/** Branco, usado quando o banner não tem cor que se destaque. */
const val COR_NEUTRA = 0xFFFFFF

private const val FAIXAS_DE_MATIZ = 24
private const val ALFA_MINIMO = 128
private const val SATURACAO_MINIMA = 0.25f
private const val BRILHO_MINIMO = 0.15f

/**
 * Cor de destaque de um banner: recebe pixels ARGB e devolve `0xRRGGBB`.
 * Ignora transparente, quase cinza e quase preto; agrupa o resto em faixas de matiz
 * (peso saturação x brilho) e devolve a média RGB da faixa de maior peso.
 */
fun corDoBrilho(pixels: IntArray): Int {
    val peso = DoubleArray(FAIXAS_DE_MATIZ)
    val somaR = LongArray(FAIXAS_DE_MATIZ)
    val somaG = LongArray(FAIXAS_DE_MATIZ)
    val somaB = LongArray(FAIXAS_DE_MATIZ)
    val quantos = IntArray(FAIXAS_DE_MATIZ)

    for (p in pixels) {
        if ((p ushr 24) < ALFA_MINIMO) continue
        val r = (p shr 16) and 0xFF
        val g = (p shr 8) and 0xFF
        val b = p and 0xFF
        val maior = maxOf(r, g, b)
        val menor = minOf(r, g, b)
        val v = maior / 255f
        if (maior == 0) continue
        val delta = (maior - menor).toFloat()
        val s = delta / maior
        if (s < SATURACAO_MINIMA || v < BRILHO_MINIMO) continue

        val matiz = when (maior) {
            r -> 60f * (((g - b) / delta) % 6f)
            g -> 60f * ((b - r) / delta + 2f)
            else -> 60f * ((r - g) / delta + 4f)
        }.let { if (it < 0f) it + 360f else it }
        val faixa = ((matiz / 360f) * FAIXAS_DE_MATIZ).toInt().coerceIn(0, FAIXAS_DE_MATIZ - 1)

        peso[faixa] += (s * v).toDouble()
        somaR[faixa] += r.toLong()
        somaG[faixa] += g.toLong()
        somaB[faixa] += b.toLong()
        quantos[faixa]++
    }

    var melhor = -1
    for (i in 0 until FAIXAS_DE_MATIZ) {
        if (quantos[i] > 0 && (melhor == -1 || peso[i] > peso[melhor])) melhor = i
    }
    if (melhor == -1) return COR_NEUTRA

    val n = quantos[melhor]
    return (((somaR[melhor] / n).toInt()) shl 16) or (((somaG[melhor] / n).toInt()) shl 8) or (somaB[melhor] / n).toInt()
}
