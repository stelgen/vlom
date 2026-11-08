package com.stelgen.vlom.shell

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable

/**
 * S2.3: запуск приложений и сборка списка установленных (T3, T6).
 * Тонкая обёртка над PackageManager — без бизнес-логики (она в TileSettings).
 */
class AppRepository(private val context: Context) {

    data class InstalledApp(
        val label: String,
        val packageName: String,
        val activityName: String,
        val icon: Drawable?,
    )

    /** Все приложения с домашними активити, кроме VLOM и системных лаунчеров. */
    fun launchableApps(): List<InstalledApp> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolve = pm.queryIntentActivities(intent, 0)
        val self = context.packageName
        return resolve.asSequence()
            .map { it.activityInfo }
            .filter { it.packageName != self }
            .filter { !it.packageName.startsWith("com.android.") && !it.packageName.startsWith("com.google.android.") }
            .distinctBy { it.packageName to it.name }
            .map {
                InstalledApp(
                    label = it.loadLabel(pm).toString(),
                    packageName = it.packageName,
                    activityName = it.name,
                    icon = it.loadIcon(pm),
                )
            }
            .sortedBy { it.label.lowercase() }
            .toList()
    }

    /** T3: intent запуска конкретного тайтла. */
    fun launchIntent(tile: HomeTile): Intent? {
        val pkg = tile.packageName ?: return null
        return if (tile.activityName != null) {
            Intent().setComponent(ComponentName(pkg, tile.activityName))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        } else {
            context.packageManager.getLaunchIntentForPackage(pkg)?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
    }

    fun launch(tile: HomeTile): Boolean {
        val intent = launchIntent(tile) ?: return false
        return try {
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            android.util.Log.w("AppRepository", "launch failed for ${tile.id}", e)
            false
        }
    }
}
