package com.stelgen.vlom

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.stelgen.vlom.shell.AppRepository
import com.stelgen.vlom.shell.BuiltInTiles
import com.stelgen.vlom.shell.SharedPreferencesTileStore
import com.stelgen.vlom.shell.TileRepository
import com.stelgen.vlom.shell.TileSettings
import com.stelgen.vlom.ui.FrigateActivity
import com.stelgen.vlom.ui.HomeView
import com.stelgen.vlom.ui.SettingsActivity
import com.stelgen.vlom.ui.WeatherActivity

/**
 * S2.1: старт-скрин / home-сетка Vista MCE.
 * Тайтлы: SharedPreferences (S2.2) с автомержем PackageManager (prune удалённых,
 * восстановление builtIn). Настройки — SettingsActivity (S2.4), приложения —
 * AppRepository (S2.3). Обновление сетки после возврата из настроек.
 */
class MainActivity : Activity() {

    private var lastSettingsJson: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showHome()
    }

    override fun onResume() {
        super.onResume()
        // вернулись из настроек/добавления приложения — перечитать, если менялось
        val current = TileSettingsCodecJson(loadSettings())
        if (current != lastSettingsJson) showHome()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_SETTINGS) showHome()
    }

    private fun loadSettings(): TileSettings {
        val store = SharedPreferencesTileStore(this)
        val installed = AppRepository(this).launchableApps().map { it.packageName }.toSet()
        return TileRepository.load(store, installed)
    }

    private fun showHome() {
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
    }

    private fun TileSettingsCodecJson(s: TileSettings): String =
        com.stelgen.vlom.shell.TileSettingsCodec.encode(s)

    companion object {
        private const val REQUEST_SETTINGS = 1
    }
}
