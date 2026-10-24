package com.guxtavon.tvlimpa.sistema

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import com.guxtavon.tvlimpa.nucleo.AppInfo

/** O que está instalado e pode ser aberto, com capa (banner) e ícone. Nunca lança exceção. */
class Instalados(private val ctx: Context) {

    /**
     * Só os apps feitos pra TV (`LEANBACK_LAUNCHER`). Na TCL, o `LAUNCHER` de celular traz telas
     * internas do sistema (configuração inicial, teste do controle, atualizador) que não são pra
     * abrir pelo controle (visto na TV em 21/10). Um pacote aparece uma vez só.
     */
    fun apps(): List<AppInfo> = try {
        val vistos = LinkedHashMap<String, AppInfo>()
        for (categoria in listOf(Intent.CATEGORY_LEANBACK_LAUNCHER)) {
            val consulta = Intent(Intent.ACTION_MAIN).addCategory(categoria)
            for (r in ctx.packageManager.queryIntentActivities(consulta, 0)) {
                val ai = r.activityInfo ?: continue
                if (ai.packageName in vistos) continue
                val nome = r.loadLabel(ctx.packageManager)?.toString()?.takeIf { it.isNotBlank() } ?: ai.packageName
                vistos[ai.packageName] = AppInfo(
                    pacote = ai.packageName,
                    nome = nome,
                    componente = ComponentName(ai.packageName, ai.name).flattenToString(),
                )
            }
        }
        vistos.values.toList()
    } catch (e: Exception) {
        emptyList()
    }

    /** Banner da activity; senão o do app; senão `null`. */
    fun capa(app: AppInfo): Drawable? {
        val comp = ComponentName.unflattenFromString(app.componente)
        val pm = ctx.packageManager
        val daActivity = try {
            comp?.let { pm.getActivityBanner(it) }
        } catch (e: Exception) {
            null
        }
        if (daActivity != null) return daActivity
        return try {
            pm.getApplicationBanner(app.pacote)
        } catch (e: Exception) {
            null
        }
    }

    /** Ícone da activity (cai no do app sozinho); `null` se não achar. */
    fun icone(app: AppInfo): Drawable? = try {
        val comp = ComponentName.unflattenFromString(app.componente)
        if (comp != null) ctx.packageManager.getActivityIcon(comp) else ctx.packageManager.getApplicationIcon(app.pacote)
    } catch (e: Exception) {
        null
    }
}
