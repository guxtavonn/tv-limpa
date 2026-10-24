package com.guxtavon.tvlimpa.nucleo

/** Valores de fábrica da tela inicial. */
object Padrao {
    /** Fileira padrão, nesta ordem. */
    val FILEIRA: List<String> = listOf(
        "com.netflix.ninja",
        "com.amazon.amazonvideo.livingroom",
        "com.google.android.youtube.tv",
        "com.globo.globotv",
        "com.disney.disneyplus",
    )

    /** Apps que a TCL religa e que a faxina fecha. */
    val TEIMOSOS: List<String> = listOf(
        "com.tcl.smartalexa",
        "com.tcl.MultiScreenInteraction_TV",
        "com.tcl.notereminder",
        "com.tcl.tv.tclhome_master",
    )

    /** Não aparecem na grade "Todos os apps". */
    val ESCONDIDOS: Set<String> = TEIMOSOS.toSet() + setOf(
        "com.tcl.tvweishi",
        "com.tcl.partnercustomizer",
        "com.guxtavon.tvlimpa",
    )
}
