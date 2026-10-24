package com.guxtavon.tvlimpa.sistema

import android.content.Context
import com.guxtavon.tvlimpa.nucleo.Fileira

/** O que a tela guarda entre execuções (SharedPreferences `tela`). */
class Guardado(ctx: Context) {
    private val prefs = ctx.getSharedPreferences("tela", Context.MODE_PRIVATE)

    var fileira: Fileira
        get() = try {
            Fileira.ler(prefs.getString(CHAVE, null))
        } catch (e: Exception) {
            Fileira.ler(null)
        }
        set(valor) {
            prefs.edit().putString(CHAVE, valor.serializar()).apply()
        }

    private companion object {
        const val CHAVE = "fileira"
    }
}
