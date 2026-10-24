package com.guxtavon.tvlimpa.nucleo

import org.junit.Assert.assertEquals
import org.junit.Test

class GradeTest {
    @Test fun grade_esconde_e_ordena() {
        val apps = listOf(
            AppInfo("y", "YouTube", "y/A"),
            AppInfo("t", "TCL Alexa", "t/A"),
            AppInfo("d", "Disney+", "d/A"),
            AppInfo("g", "globoplay", "g/A"),
            AppInfo("y", "YouTube", "y/B"),
        )
        assertEquals(listOf("Disney+", "globoplay", "YouTube"), listaGrade(apps, setOf("t")).map { it.nome })
    }

    @Test fun grade_acento_nao_atrapalha() = assertEquals(
        listOf("Ábaco", "Banana"),
        listaGrade(listOf(AppInfo("b", "Banana", "b/A"), AppInfo("a", "Ábaco", "a/A")), emptySet()).map { it.nome },
    )
}
