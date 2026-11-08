package com.stelgen.vlom

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.os.StrictMode
import com.stelgen.vlom.shell.AppRepository
import com.stelgen.vlom.shell.BuiltInTiles
import com.stelgen.vlom.shell.CrashGuard
import com.stelgen.vlom.shell.CrashGuardInstaller
import com.stelgen.vlom.shell.SharedPreferencesTileStore
import com.stelgen.vlom.shell.TileRepository
import com.stelgen.vlom.shell.TileSettings
import com.stelgen.vlom.ui.FrigateActivity
import com.stelgen.vlom.ui.HomeView
import com.stelgen.vlom.ui.RecoveryActivity
import com.stelgen.vlom.ui.SettingsActivity
import com.stelgen.vlom.ui.WeatherActivity

/**
 * Старт-скрин / home-сетка Vista MCE (S2.1).
 *
 * S5.2: CrashGuard — счётчик падений; 2+ подряд -> RecoveryActivity
 * (запуск стокового лаунчера / сброс настроек), пользователь не заперт.
 * S5.1: замер холодного старта (attach -> первый кадр home) в логкат.
 */
class MainActivity : Activity() {

    private var createdAtMs = 0L
    private var lastSettingsJson: String? = null
    private var healthyMarked = false

    override fun onCreate(savedInstanceState: Bundle?) {
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder().detectAll().penaltyLog().build()
            )
        }
        super.onCreate(savedInstanceState)
        createdAtMs = SystemClock.uptimeMillis()

        CrashGuardInstaller.install(this)
        val guard = CrashGuardInstaller.guard(this)
        if (guard.isRecoveryMode()) {
            startActivity(Intent(this, RecoveryActivity::class.java))
            finish()
            return
        }
        showHome(guard)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_SETTINGS) showHome(CrashGuardInstaller.guard(this))
    }

    override fun onResume() {
        super.onResume()
        // вернулись из настроек/добавления приложения — перечитать, если менялось
        val current = TileSettingsCodecJson(loadSettings())
        if (current != lastSettingsJson) showHome(CrashGuardInstaller.guard(this))
    }

    private fun loadSettings(): TileSettings {
        val store = SharedPreferencesTileStore(this)
        val installed = AppRepository(this).launchableApps().map { it.packageName }.toSet()
        return TileRepository.load(store, installed)
    }

    private fun showHome(guard: CrashGuard) {
        val settings = loadSettings()
        lastSettingsJson = TileSettingsCodecJson(settings)
        setContentView(
            HomeView(this, settings) { tile ->
                when (tile.id) {
                    BuiltInTiles.SETTINGS ->
                        startActivityForResult(
                            Intent(this, SettingsActivity::class.java), REQUEST_SETTINGS,
                        )
                    BuiltInTiles.FRIGATE ->
                        startActivity(Intent(this, FrigateActivity::class.java))
                    BuiltInTiles.WEATHER ->
                        startActivity(Intent(this, WeatherActivity::class.java))
                    // Музыка/Картинки/Видео — внутренние экраны (медиабраузер, backlog v2)
                    else -> android.util.Log.i("MainActivity", "builtin tile: ${tile.id}")
                }
            }
        )
        // успешный полноценный запуск: сброс счётчика сбоев + замер старта
        if (!healthyMarked) {
            guard.onHealthyHome()
            healthyMarked = true
            android.util.Log.i(
                "StartupTimer",
                "cold start (attach -> home shown): ${SystemClock.uptimeMillis() - createdAtMs} ms",
            )
        }
    }

    private fun TileSettingsCodecJson(s: TileSettings): String =
        com.stelgen.vlom.shell.TileSettingsCodec.encode(s)

    companion object {
        private const val REQUEST_SETTINGS = 1
    }
}
