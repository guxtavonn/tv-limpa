package com.guxtavon.tvlimpa.nucleo

import java.text.Collator
import java.util.Locale

/** Um app instalado; [componente] no formato `pacote/classe`. */
data class AppInfo(val pacote: String, val nome: String, val componente: String)

/**
 * Lista da grade "Todos os apps": sem os [escondidos], sem pacote repetido (fica o primeiro)
 * e em ordem alfabética do português, sem diferença de maiúscula nem de acento.
 */
fun listaGrade(apps: List<AppInfo>, escondidos: Set<String>): List<AppInfo> {
    val colador = Collator.getInstance(Locale.forLanguageTag("pt-BR")).apply { strength = Collator.PRIMARY }
    return apps
        .filter { it.pacote !in escondidos }
        .distinctBy { it.pacote }
        .sortedWith { a, b -> colador.compare(a.nome, b.nome) }
}
