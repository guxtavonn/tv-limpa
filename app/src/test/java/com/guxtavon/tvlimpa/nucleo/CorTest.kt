package com.guxtavon.tvlimpa.nucleo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CorTest {
    private fun pixels(vararg partes: Pair<Int, Int>): IntArray =
        partes.flatMap { (cor, quantidade) -> List(quantidade) { cor } }.toIntArray()

    private fun vermelho(c: Int) = (c shr 16) and 0xFF
    private fun verde(c: Int) = (c shr 8) and 0xFF
    private fun azul(c: Int) = c and 0xFF

    @Test fun netflix_branco_com_vermelho_da_vermelho() {
        val c = corDoBrilho(pixels(0xFFFFFFFF.toInt() to 800, 0xFFE50914.toInt() to 200))
        assertTrue(vermelho(c) > 200 && verde(c) < 60 && azul(c) < 60)
    }

    @Test fun prime_azul_ganha_do_branco() {
        val c = corDoBrilho(pixels(0xFF0078FF.toInt() to 700, 0xFFFFFFFF.toInt() to 300))
        assertTrue(azul(c) > 200 && vermelho(c) < 60)
    }

    @Test fun tudo_cinza_da_neutra() =
        assertEquals(COR_NEUTRA, corDoBrilho(pixels(0xFF808080.toInt() to 500, 0xFF101010.toInt() to 500)))

    @Test fun transparente_e_ignorado() {
        val c = corDoBrilho(pixels(0x00FF0000 to 900, 0xFF00C000.toInt() to 100))
        assertTrue(verde(c) > 150 && vermelho(c) < 60)
    }

    @Test fun vazio_da_neutra() = assertEquals(COR_NEUTRA, corDoBrilho(IntArray(0)))
}
