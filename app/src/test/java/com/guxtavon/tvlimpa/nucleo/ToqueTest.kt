package com.guxtavon.tvlimpa.nucleo

import org.junit.Assert.assertEquals
import org.junit.Test

class ToqueTest {
    @Test fun apertada_curta_e_ok() {
        val t = Toque()
        assertEquals(Gesto.NADA, t.descer(0, 1000))
        assertEquals(Gesto.OK, t.subir(false))
    }

    @Test fun segurar_650_ms_pede_segurar_uma_vez_e_o_soltar_nao_abre() {
        val t = Toque()
        t.descer(0, 1000)
        assertEquals(Gesto.NADA, t.descer(1, 1500))
        assertEquals(Gesto.SEGURAR, t.descer(2, 1650))
        t.segurou()
        assertEquals(Gesto.NADA, t.descer(3, 1700))
        assertEquals(Gesto.NADA, t.descer(4, 2400))
        assertEquals(Gesto.NADA, t.subir(false))
    }

    @Test fun repeticoes_do_controle_antes_do_tempo_ainda_sao_ok() {
        val t = Toque()
        t.descer(0, 1000)
        assertEquals(Gesto.NADA, t.descer(1, 1150))
        assertEquals(Gesto.NADA, t.descer(2, 1300))
        assertEquals(Gesto.NADA, t.descer(3, 1450))
        assertEquals(Gesto.OK, t.subir(false))
    }

    @Test fun segurar_sem_uso_vira_ok_ao_soltar() {
        // Num atalho (que não tem o que fazer com o segurar) a apertada longa abre ao soltar.
        val t = Toque()
        t.descer(0, 1000)
        assertEquals(Gesto.SEGURAR, t.descer(1, 1700))
        assertEquals(Gesto.NADA, t.descer(2, 1800))
        assertEquals(Gesto.OK, t.subir(false))
    }

    @Test fun conta_da_primeira_descida_mesmo_se_a_repeticao_vier_com_downTime_novo() {
        // O tempo é o guardado da descida, não o de cada repetição.
        val t = Toque()
        t.descer(0, 1000)
        assertEquals(Gesto.NADA, t.descer(1, 1250))
        assertEquals(Gesto.NADA, t.descer(2, 1500))
        assertEquals(Gesto.SEGURAR, t.descer(3, 1750))
    }

    @Test fun cancelada_nao_abre() {
        val t = Toque()
        t.descer(0, 1000)
        assertEquals(Gesto.NADA, t.subir(true))
    }

    @Test fun sem_a_descida_nada_acontece() {
        val t = Toque()
        assertEquals(Gesto.NADA, t.descer(5, 9000))
        assertEquals(Gesto.NADA, t.subir(false))
    }

    @Test fun soltar_esquece_a_apertada() {
        val t = Toque()
        t.descer(0, 1000)
        t.soltar()
        assertEquals(Gesto.NADA, t.descer(1, 1700))
        assertEquals(Gesto.NADA, t.subir(false))
    }

    @Test fun nova_apertada_recomeca() {
        val t = Toque()
        t.descer(0, 1000)
        t.descer(1, 1700)
        t.segurou()
        t.subir(false)
        t.descer(0, 5000)
        assertEquals(Gesto.OK, t.subir(false))
    }
}
