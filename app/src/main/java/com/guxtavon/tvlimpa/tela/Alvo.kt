package com.guxtavon.tvlimpa.tela

import com.guxtavon.tvlimpa.nucleo.AppInfo

/** O que está com foco quando o OK é apertado. */
sealed class Alvo {
    data class App(val app: AppInfo) : Alvo()
    object Tv : Alvo()
    object Hdmi : Alvo()
    object Wifi : Alvo()
    object Apps : Alvo()
    object Configuracoes : Alvo()
}
