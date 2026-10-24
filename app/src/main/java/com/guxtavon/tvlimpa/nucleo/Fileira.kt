package com.guxtavon.tvlimpa.nucleo

/** A fileira de apps da tela inicial. Índice fora da faixa não muda nada. */
data class Fileira(val pacotes: List<String>) {

    fun moverEsquerda(i: Int): Fileira = trocar(i, i - 1)

    fun moverDireita(i: Int): Fileira = trocar(i, i + 1)

    /** Tira o app do índice [i]; nunca deixa a fileira vazia por conta própria. */
    fun tirar(i: Int): Fileira {
        if (i !in pacotes.indices || pacotes.size == 1) return this
        return Fileira(pacotes.filterIndexed { idx, _ -> idx != i })
    }

    /** Põe o app no fim, sem duplicar. */
    fun por(pacote: String): Fileira = if (contem(pacote)) this else Fileira(pacotes + pacote)

    fun contem(pacote: String): Boolean = pacote in pacotes

    /**
     * Ajusta a fileira ao que está instalado. Se não sobrar nada, volta pro padrão
     * (só o que estiver disponível); se nada estiver disponível, fica vazia.
     */
    fun reconciliar(disponiveis: Set<String>): Fileira {
        val ficam = pacotes.filter { it in disponiveis }
        return Fileira(if (ficam.isEmpty()) Padrao.FILEIRA.filter { it in disponiveis } else ficam)
    }

    fun serializar(): String = pacotes.joinToString(",")

    private fun trocar(a: Int, b: Int): Fileira {
        if (a !in pacotes.indices || b !in pacotes.indices) return this
        val nova = pacotes.toMutableList()
        nova[a] = pacotes[b]
        nova[b] = pacotes[a]
        return Fileira(nova)
    }

    companion object {
        /** Lê o texto salvo; nulo, vazio ou só lixo dá o padrão. */
        fun ler(texto: String?): Fileira {
            val lidos = texto.orEmpty().split(',').map { it.trim() }.filter { it.isNotEmpty() }.distinct()
            return Fileira(if (lidos.isEmpty()) Padrao.FILEIRA else lidos)
        }
    }
}
