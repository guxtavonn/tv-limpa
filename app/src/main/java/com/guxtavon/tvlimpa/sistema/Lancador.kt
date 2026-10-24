package com.guxtavon.tvlimpa.sistema

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import com.guxtavon.tvlimpa.nucleo.AppInfo

/** Abre apps e atalhos. Qualquer falha (app sumiu, componente inexistente) vira `false`. */
class Lancador(private val ctx: Context) {

    /**
     * Abre o app pela entrada de TV (`LEANBACK_LAUNCHER`), senão pela de celular (`LAUNCHER`), como a
     * tela de fábrica: com a categoria certa, o app volta pra tarefa que já estava aberta. Sem
     * nenhuma das duas, pelo componente guardado.
     */
    fun abrir(app: AppInfo): Boolean {
        val pm = ctx.packageManager
        val intent = try {
            pm.getLeanbackLaunchIntentForPackage(app.pacote) ?: pm.getLaunchIntentForPackage(app.pacote)
        } catch (e: Exception) {
            null
        } ?: ComponentName.unflattenFromString(app.componente)?.let { Intent(Intent.ACTION_MAIN).setComponent(it) }
            ?: return false
        return iniciar(intent.addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED))
    }

    fun abrirTv(): Boolean =
        iniciar(Intent(Intent.ACTION_MAIN).setComponent(ComponentName("com.tcl.tv", "com.tcl.tv.TVActivity")))

    /** A TCL abre o seletor de entradas por um serviço; sem esse serviço, `false`. */
    fun abrirHdmi(): Boolean = try {
        val intent = Intent("com.tcl.insrc.SHOW_WINDOW")
            .setComponent(ComponentName("com.tcl.sourcemananger", "com.tcl.insrc.ShowWindowService"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ctx.startService(intent) != null
    } catch (e: Exception) {
        false
    }

    fun abrirWifi(): Boolean = iniciar(Intent(Settings.ACTION_WIFI_SETTINGS))

    fun abrirConfiguracoes(): Boolean = iniciar(Intent(Settings.ACTION_SETTINGS))

    private fun iniciar(intent: Intent): Boolean = try {
        ctx.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (e: Exception) {
        false
    }
}
