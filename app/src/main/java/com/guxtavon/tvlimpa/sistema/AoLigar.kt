package com.guxtavon.tvlimpa.sistema

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.util.Log

/**
 * No boot: faxina na hora e de novo 60 s depois (a TCL religa os teimosos depois que o sistema sobe).
 * O segundo passo usa um alarme pro mesmo receiver, porque `goAsync` não segura 60 s.
 */
class AoLigar : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                Faxina.limpar(ctx)
                agendarSegunda(ctx)
            }
            ACAO_SEGUNDA -> Faxina.limpar(ctx)
        }
    }

    private fun agendarSegunda(ctx: Context) {
        try {
            val alarme = ctx.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(ctx, AoLigar::class.java).setAction(ACAO_SEGUNDA)
            val pendente = PendingIntent.getBroadcast(ctx, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
            alarme.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + ATRASO_MS, pendente)
        } catch (e: Exception) {
            Log.w("TelaFaxina", "não consegui agendar a segunda faxina", e)
        }
    }

    private companion object {
        const val ACAO_SEGUNDA = "com.guxtavon.tvlimpa.FAXINA_60S"
        const val ATRASO_MS = 60_000L
    }
}
