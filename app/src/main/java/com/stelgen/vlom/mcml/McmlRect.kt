package com.stelgen.vlom.mcml

/**
 * Утилиты значений атрибутов MCML. Вендорный формат из ehshell:
 *   padding="Rect(0,35,0,0)"  margin="Rect(left,top,right,bottom)"
 *   LayoutPos="BottomRight", числа в логических пикселях 1080p-базиса (U-движок фазы 1).
 */
data class McmlRect(val left: Int, val top: Int, val right: Int, val bottom: Int) {

    val horizontal: Int get() = left + right
    val vertical: Int get() = top + bottom

    companion object {
        private val RECT_REGEX = Regex("""^Rect\((-?\d+),(-?\d+),(-?\d+),(-?\d+)\)$""")

        fun parse(value: String?): McmlRect? {
            val m = value?.trim()?.let { RECT_REGEX.matchEntire(it) } ?: return null
            val (l, t, r, b) = m.destructured
            return McmlRect(l.toInt(), t.toInt(), r.toInt(), b.toInt())
        }
    }
}
