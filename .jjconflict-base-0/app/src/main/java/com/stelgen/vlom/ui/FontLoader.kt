package com.stelgen.vlom.ui

import android.content.Context
import android.graphics.Typeface

/**
 * S1.5: Selawik (open-source, метрически совместим с Segoe UI; U5 — шрифты
 * Microsoft в APK запрещены). Кириллица поддерживается. Fallback — system
 * sans-serif, если asset отсутствует (не должно случаться, CI-сборка фиксирует).
 */
object FontLoader {

    private val cache = mutableMapOf<Style, Typeface?>()

    enum class Style { REGULAR, BOLD, SEMIBOLD }

    fun get(context: Context, style: Style = Style.REGULAR): Typeface =
        cache[style] ?: load(context, style).also { cache[style] = it }

    private fun load(context: Context, style: Style): Typeface {
        val asset = when (style) {
            Style.REGULAR -> "fonts/selawk.ttf"
            Style.BOLD -> "fonts/selawkb.ttf"
            Style.SEMIBOLD -> "fonts/selawksb.ttf"
        }
        return try {
            Typeface.createFromAsset(context.assets, asset)
        } catch (e: Exception) {
            android.util.Log.w("FontLoader", "fallback to system font for $asset", e)
            Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }
    }
}
