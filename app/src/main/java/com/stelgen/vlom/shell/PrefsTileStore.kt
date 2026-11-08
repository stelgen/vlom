package com.stelgen.vlom.shell

import android.content.Context
import android.content.SharedPreferences

/**
 * S2.2 (обвязка): хранение TileSettings в SharedPreferences + автоматический
 * merge с PackageManager при загрузке.
 *
 * Правила загрузки (канон HOME: не падать, не терять пользовательский выбор):
 *  1. битый JSON -> дефолты (safe-decode);
 *  2. builtIn-тайтлы всегда присутствуют (если юзер удалил ключ/версию — восстанавливаем);
 *  3. тайтлы удалённых APK вычищаются (pruneRemovedApps);
 *  4. порядок/скрытия/добавления юзера сохраняются как есть.
 */
interface TileStore {
    fun load(): TileSettings
    fun save(settings: TileSettings)
}

class SharedPreferencesTileStore(context: Context) : TileStore {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun load(): TileSettings {
        val raw = prefs.getString(KEY, null) ?: return TileSettings()
        return TileSettingsCodec.decode(raw)
    }

    override fun save(settings: TileSettings) {
        prefs.edit().putString(KEY, TileSettingsCodec.encode(settings)).apply()
    }

    companion object {
        private const val PREFS_NAME = "vlom_shell"
        private const val KEY = "tile_settings_v1"
    }
}

/** Слои: хранилище юзера + обнаруженные приложения. */
object TileRepository {

    /** Загрузить актуальные настройки с автомержем. */
    fun load(store: TileStore, installedPackages: Set<String>): TileSettings {
        val stored = store.load()
        val withBuiltIns = ensureBuiltIns(stored)
        val pruned = withBuiltIns.pruneRemovedApps(installedPackages)
        if (pruned != stored) store.save(pruned)
        return pruned
    }

    /** Восстановить builtIn-тайтлы (в оригинальном порядке), если их нет. */
    fun ensureBuiltIns(s: TileSettings): TileSettings {
        if (s.tiles.any { it.id == BuiltInTiles.MUSIC }) return s // маркер целостности
        val missing = BuiltInTiles.defaults.filter { def ->
            s.tiles.none { it.id == def.id }
        }
        if (missing.isEmpty()) return s
        return TileSettings(missing + s.tiles)
    }
}
