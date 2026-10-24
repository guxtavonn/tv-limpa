package com.guxtavon.tvlimpa.nucleo

import org.junit.Assert.assertEquals
import org.junit.Test

class FileiraTest {
    private val f = Fileira(listOf("a", "b", "c"))

    @Test fun moverEsquerda_troca_com_o_vizinho() = assertEquals(listOf("b", "a", "c"), f.moverEsquerda(1).pacotes)
    @Test fun moverEsquerda_no_primeiro_nao_muda() = assertEquals(f, f.moverEsquerda(0))
    @Test fun moverDireita_no_ultimo_nao_muda() = assertEquals(f, f.moverDireita(2))
    @Test fun tirar_remove() = assertEquals(listOf("a", "c"), f.tirar(1).pacotes)
    @Test fun tirar_o_unico_nao_deixa_vazia() = assertEquals(listOf("a"), Fileira(listOf("a")).tirar(0).pacotes)
    @Test fun por_vai_pro_fim_e_nao_duplica() {
        assertEquals(listOf("a", "b", "c", "d"), f.por("d").pacotes)
        assertEquals(f, f.por("b"))
    }
    @Test fun reconciliar_tira_o_que_sumiu() = assertEquals(listOf("a", "c"), f.reconciliar(setOf("a", "c", "z")).pacotes)
    @Test fun reconciliar_vazia_volta_pro_padrao_disponivel() =
        assertEquals(listOf("com.netflix.ninja"), Fileira(listOf("x")).reconciliar(setOf("com.netflix.ninja")).pacotes)
    @Test fun reconciliar_sem_nada_disponivel_fica_vazia() = assertEquals(emptyList<String>(), f.reconciliar(emptySet()).pacotes)
    @Test fun ler_e_serializar_ida_e_volta() = assertEquals(f, Fileira.ler(f.serializar()))
    @Test fun ler_nulo_ou_vazio_da_o_padrao() {
        assertEquals(Padrao.FILEIRA, Fileira.ler(null).pacotes)
        assertEquals(Padrao.FILEIRA, Fileira.ler("").pacotes)
    }
    @Test fun ler_ignora_lixo_e_duplicado() = assertEquals(listOf("a", "b"), Fileira.ler(" a,,b,a ").pacotes)
}
