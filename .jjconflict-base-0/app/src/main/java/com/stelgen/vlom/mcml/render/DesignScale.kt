package com.stelgen.vlom.mcml.render

/**
 * S1.2: масштабирование базиса дизайна на фактический экран (REQ-001 P5).
 *
 * MCML-страницы телевизионной платформы заданы в фиксированном базисе;
 * реальное разрешение устройства — 1366×768 / 1080p / 4K.
 * Стратегия: равномерный масштаб = min(масштабов по осям), рендер центрируется
 * (letterbox по оси с избытком). Точный базис подтверждается скринтюнингом
 * фазы 4 — параметр вынесен в конструктор намеренно.
 */
data class DesignScale(
    val basisWidth: Int = DEFAULT_BASIS_WIDTH,
    val basisHeight: Int = DEFAULT_BASIS_HEIGHT,
) {
    companion object {
        const val DEFAULT_BASIS_WIDTH = 1366
        const val DEFAULT_BASIS_HEIGHT = 768
        const val MIN = 0.001f
    }

    fun factor(screenWidth: Int, screenHeight: Int): Float {
        require(screenWidth > 0 && screenHeight > 0) { "screen size must be positive" }
        require(basisWidth > 0 && basisHeight > 0) { "basis size must be positive" }
        val sx = screenWidth.toFloat() / basisWidth
        val sy = screenHeight.toFloat() / basisHeight
        return maxOf(MIN, minOf(sx, sy))
    }

    /** Смещение по X для центрирования отмасштабированной сцены. */
    fun offsetX(screenWidth: Int, factor: Float): Int =
        ((screenWidth - basisWidth * factor) / 2f).toInt().coerceAtLeast(0)

    /** Смещение по Y для центрирования отмасштабированной сцены. */
    fun offsetY(screenHeight: Int, factor: Float): Int =
        ((screenHeight - basisHeight * factor) / 2f).toInt().coerceAtLeast(0)
}
