package com.stelgen.vlom

import android.app.Activity
import android.os.Bundle
import com.stelgen.vlom.shell.BuiltInTiles
import com.stelgen.vlom.shell.TileSettings
import com.stelgen.vlom.ui.HomeView

/**
 * S2.1: старт-скрин / home-сетка Vista MCE.
 * Состав и порядок тайтлов — TileSettings (S2.2, SharedPreferences в след. шаге),
 * запуск приложений — AppRepository (S2.3). Встроенные тайтлы (Музыка/Видео/
 * Картинки/Наблюдение/Настройки) открывают внутренние экраны (фазы 2/3).
 */
class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(HomeView(this, TileSettings()) { tile ->
            when (tile.id) {
                BuiltInTiles.SETTINGS -> openSettings()
                // Музыка/Картинки/Видео/Наблюдение — внутренние экраны фаз 2/3:
                // сейчас лог в логкат, UI-заглушки придут вместе с медиабраузером
                else -> android.util.Log.i("MainActivity", "builtin tile: ${tile.id}")
            }
        })
    }

    private fun openSettings() {
        // S2.4: экран настроек v1 (добавление/скрытие/сортировка тайтлов)
        android.util.Log.i("MainActivity", "settings: TODO фаза 2")
    }
}
