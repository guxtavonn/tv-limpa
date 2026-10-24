package com.guxtavon.tvlimpa.sistema

import android.app.ActivityManager
import android.content.Context
import android.util.Log
import com.guxtavon.tvlimpa.nucleo.Padrao

/** Fecha os processos que a TCL religa sozinha. */
object Faxina {
    private const val TAG = "TelaFaxina"

    fun limpar(ctx: Context) {
        try {
            val gerente = ctx.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            if (gerente == null) {
                Log.w(TAG, "sem ActivityManager")
                return
            }
            // Limite do Android: a partir da API 22 (de verdade na 26+), getRunningAppProcesses
            // só devolve os processos do próprio app. Então "quantos estavam rodando antes" NÃO é
            // observável por um app comum; este número tende a 0 mesmo com os teimosos de pé.
            // A prova real do efeito é o `ps` no aparelho (Task 10). Aqui logamos o que é honesto:
            // o que o sistema deixa ver e cada pedido de encerramento enviado.
            val visiveis = try {
                gerente.runningAppProcesses.orEmpty().flatMap { it.pkgList.toList() }.toSet()
            } catch (e: Exception) {
                emptySet()
            }
            val rodando = Padrao.TEIMOSOS.count { it in visiveis }
            var pedidos = 0
            for (pacote in Padrao.TEIMOSOS) {
                try {
                    gerente.killBackgroundProcesses(pacote)
                    pedidos++
                    Log.i(TAG, "pedi pra encerrar $pacote (visível pro app: ${pacote in visiveis})")
                } catch (e: Exception) {
                    Log.w(TAG, "não consegui pedir pra encerrar $pacote", e)
                }
            }
            Log.i(
                TAG,
                "faxina: $pedidos de ${Padrao.TEIMOSOS.size} pedidos enviados; " +
                    "rodando antes (visível pro app): $rodando (a API 28 só mostra os próprios processos)",
            )
        } catch (e: Exception) {
            Log.w(TAG, "faxina falhou", e)
        }
    }
}
