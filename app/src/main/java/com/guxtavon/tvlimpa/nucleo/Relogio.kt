package com.guxtavon.tvlimpa.nucleo

import java.time.LocalDate
import java.util.Locale

private val DIAS = listOf("segunda", "terça", "quarta", "quinta", "sexta", "sábado", "domingo")

private val MESES = listOf(
    "janeiro", "fevereiro", "março", "abril", "maio", "junho",
    "julho", "agosto", "setembro", "outubro", "novembro", "dezembro",
)

/** "09:05", com algarismos latinos qualquer que seja o idioma do aparelho. */
fun textoHora(hora: Int, minuto: Int): String = String.format(Locale.ROOT, "%02d:%02d", hora, minuto)

/** "terça, 20 de outubro". */
fun textoData(data: LocalDate): String =
    "${DIAS[data.dayOfWeek.value - 1]}, ${data.dayOfMonth} de ${MESES[data.monthValue - 1]}"
