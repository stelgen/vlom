package com.stelgen.vlom.shell

import org.json.JSONArray
import org.json.JSONObject

/**
 * S2.2: менеджер тайтлов — пользовательские настройки (T3-T5: добавление,
 * скрытие, пересортировка). Хранение: JSON в SharedPreferences (v1; бэкап
 * экспорт/импорт — v2 тот же формат).
 *
 * Чистая логика отделена от Android-хранилища: TileSettings — Immutable-снимок
 * + операции, возвращающие новый снимок. Тестируется в JVM.
 */
data class TileSettings(
    /** Порядок и состав тайтлов, как видит пользователь. */
    val tiles: List<HomeTile> = BuiltInTiles.defaults,
) {

    val visible: List<HomeTile> get() = tiles.filter { !it.hidden }

    /** T3: добавить приложение (не дублировать по package+activity). */
    fun addApp(id: String, label: String, pkg: String, activity: String?): TileSettings {
        if (tiles.any { it.packageName == pkg && it.activityName == activity }) return this
        return copy(tiles = tiles + HomeTile(id, label, packageName = pkg, activityName = activity))
    }

    /** T5: скрыть/показать. */
    fun setHidden(id: String, hidden: Boolean): TileSettings =
        copy(tiles = tiles.map { if (it.id == id) it.copy(hidden = hidden) else it })

    /** T4: перестановка (переместить tile с позиции from на to). */
    fun move(fromIndex: Int, toIndex: Int): TileSettings {
        if (fromIndex !in tiles.indices || toIndex !in tiles.indices || fromIndex == toIndex) return this
        val mutable = tiles.toMutableList()
        val item = mutable.removeAt(fromIndex)
        mutable.add(toIndex, item)
        return copy(tiles = mutable)
    }

    /** Удалить тайтл приложения (builtIn — только скрытие, по T3/T5). */
    fun remove(id: String): TileSettings =
        copy(tiles = tiles.filter { it.id != id || it.builtIn })

    /** Убрать тайтлы удалённых пользователем приложений (APK больше нет). */
    fun pruneRemovedApps(installed: Set<String>): TileSettings =
        copy(tiles = tiles.filter { it.builtIn || it.packageName in installed })

    companion object {
        data class DiscoveredApp(val label: String, val packageName: String, val activity: String?)
    }
}

object TileSettingsCodec {

    fun encode(s: TileSettings): String = JSONObject().apply {
        put("v", 1)
        put("tiles", JSONArray().also { arr ->
            s.tiles.forEach { t ->
                arr.put(JSONObject().apply {
                    put("id", t.id)
                    put("label", t.label)
                    put("builtIn", t.builtIn)
                    t.packageName?.let { put("pkg", it) }
                    t.activityName?.let { put("act", it) }
                    if (t.hidden) put("hidden", true)
                })
            }
        })
    }.toString()

    fun decode(json: String): TileSettings = try {
        parse(JSONObject(json))
    } catch (e: Exception) {
        // битые/чужие данные — дефолтный старт-скрин (никогда не падаем в HOME)
        TileSettings()
    }

    private fun parse(root: JSONObject): TileSettings {
        val arr = root.optJSONArray("tiles") ?: return TileSettings()
        val tiles = (0 until arr.length()).mapNotNull { i ->
            val o = arr.getJSONObject(i)
            HomeTile(
                id = o.getString("id"),
                label = o.optString("label", o.getString("id")),
                builtIn = o.optBoolean("builtIn", false),
                packageName = o.optStringOrNull("pkg"),
                activityName = o.optStringOrNull("act"),
                hidden = o.optBoolean("hidden", false),
            )
        }
        return TileSettings(tiles)
    }

    private fun JSONObject.optStringOrNull(key: String): String? =
        if (has(key) && !isNull(key)) getString(key) else null
}
