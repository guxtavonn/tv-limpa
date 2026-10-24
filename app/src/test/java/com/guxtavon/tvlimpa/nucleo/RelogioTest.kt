package com.guxtavon.tvlimpa.nucleo

import java.time.LocalDate
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class RelogioTest {
    @Test fun hora_com_zero() = assertEquals("09:05", textoHora(9, 5))
    @Test fun data_por_extenso() = assertEquals("terça, 20 de outubro", textoData(LocalDate.of(2026, 10, 20)))
    @Test fun hora_nao_depende_do_idioma() {
        val antes = Locale.getDefault()
        try {
            Locale.setDefault(Locale("ar"))
            assertEquals("09:05", textoHora(9, 5))
        } finally {
            Locale.setDefault(antes)
        }
    }

    @Test fun data_sabado_e_marco() = assertEquals("sábado, 7 de março", textoData(LocalDate.of(2026, 3, 7)))
}
