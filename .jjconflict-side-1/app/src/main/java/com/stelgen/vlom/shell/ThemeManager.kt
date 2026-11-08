package com.stelgen.vlom.shell

import android.content.Context
import android.content.SharedPreferences
import com.stelgen.vlom.mcml.render.McmlRenderer

/**
 * S4.3: темы Blue / Black / Green (REQ-001 U2). Выбор хранится в SharedPreferences,
 * палитры — стоп-кадры из ehres (пиксельная верность скинов — по мере скринтюнинга).
 */
object ThemeManager {

    enum class Theme(val id: String, val label: String) {
        BLUE("blue", "Blue"),
        BLACK("black", "Black"),
        GREEN("green", "Green");

        companion object {
            fun fromId(id: String?): Theme = entries.firstOrNull { it.id == id } ?: BLUE
        }
    }

    private const val PREFS = "vlom_shell"
    private const val KEY = "theme"

    fun current(context: Context): Theme =
        prefs(context).getString(KEY, null).let { Theme.fromId(it) }

    fun set(context: Context, theme: Theme) {
        prefs(context).edit().putString(KEY, theme.id).apply()
    }

    fun palette(theme: Theme): McmlRenderer.Palette = when (theme) {
        Theme.BLUE -> McmlRenderer.Palette.VISTA_BLUE
        Theme.BLACK -> McmlRenderer.Palette.VISTA_BLACK
        Theme.GREEN -> McmlRenderer.Palette.VISTA_GREEN
    }

    fun palette(context: Context): McmlRenderer.Palette = palette(current(context))

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
