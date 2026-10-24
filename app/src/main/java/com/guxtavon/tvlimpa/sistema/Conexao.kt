package com.guxtavon.tvlimpa.sistema

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Handler
import android.os.Looper

/** Estado da internet pro atalho do Wi-Fi. Os avisos chegam na thread principal. */
class Conexao(ctx: Context) {
    private val gerente = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    private var callback: ConnectivityManager.NetworkCallback? = null

    /** Há rede padrão com capacidade de internet (não exige a validação do sistema). */
    fun conectado(): Boolean = try {
        val rede = gerente?.activeNetwork
        val caps = rede?.let { gerente?.getNetworkCapabilities(it) }
        caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    } catch (e: Exception) {
        false
    }

    /** Avisa a cada mudança; chamar de novo troca o ouvinte anterior. */
    fun observar(aoMudar: (Boolean) -> Unit) {
        parar()
        val novo = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) = aoMudar(true)
            override fun onLost(network: Network) = aoMudar(false)
        }
        try {
            gerente?.registerDefaultNetworkCallback(novo, Handler(Looper.getMainLooper()))
            callback = novo
        } catch (e: Exception) {
            callback = null
        }
    }

    fun parar() {
        val atual = callback ?: return
        callback = null
        try {
            gerente?.unregisterNetworkCallback(atual)
        } catch (e: Exception) {
            // já estava solto
        }
    }
}
