package com.stelgen.vlom.shell

import com.stelgen.vlom.mcml.render.DesignScale
import kotlin.math.abs

/**
 * S2.1/S2.2: геометрия home-сетки Vista MCE.
 *
 * Старт-скрин MCE: горизонтальная полоса тайтлов (stripe), фокусный тайтл
 * крупнее, соседние — меньше, полоса сдвигается так, чтобы фокусный тайтл
 * держался в зоне внимания. Все координаты — в базисе 1366×768 (DesignScale
 * растянет на экран). Чистая математика — тестируется в JVM.
 */
object HomeLayout {

    const val BASIS_W = DesignScale.DEFAULT_BASIS_WIDTH
    const val BASIS_H = DesignScale.DEFAULT_BASIS_HEIGHT

    /** Параметры полосы (стоп-кадр Vista; скринтюнинг фазы 4). */
    const val TILE_W = 340f
    const val TILE_H = 230f
    const val GAP = 24f
    const val STRIPE_TOP = 260f
    const val FOCUS_SCALE = 1.18f
    const val NEIGHBOUR_SCALE = 0.94f

    data class Placement(
        val index: Int,
        val x: Float,          // левый край тайла (базис)
        val y: Float,
        val width: Float,
        val height: Float,
        val scale: Float,
    )

    /**
     * Раскладка видимых тайтлов вокруг фокусного (int = дискретный вариант).
     */
    fun stripe(count: Int, focusedIndex: Int): List<Placement> =
        stripe(count, focusedIndex.toFloat())

    /**
     * Раскладка с ПЛАВНЫМ фокусом (S4.2, U4): fractional focus даёт непрерывное
     * скольжение полосы при смене тайтла (анимация 180мс в HomeView).
     * Центры ячеек идут с постоянным шагом; масштаб — интерполяция
     * FOCUS_SCALE (|rel|=0) -> NEIGHBOUR_SCALE (|rel|=1) -> *0.95 дальше.
     */
    fun stripe(count: Int, focused: Float): List<Placement> {
        require(count >= 0) { "count must be non-negative" }
        val fi = focused.coerceIn(0f, (count - 1).coerceAtLeast(0).toFloat())

        val pitch = TILE_W + GAP
        val shift = BASIS_W / 2f - TILE_W / 2f - fi * pitch

        return (0 until count).map { i ->
            val rel = abs(i - fi)
            val scale = when {
                rel <= 1f -> FOCUS_SCALE + (NEIGHBOUR_SCALE - FOCUS_SCALE) * rel
                else -> NEIGHBOUR_SCALE * 0.95f
            }
            val w = TILE_W * scale
            val h = TILE_H * scale
            val cellCenter = shift + TILE_W / 2f + i * pitch
            Placement(i, cellCenter - w / 2f, STRIPE_TOP + (TILE_H - h) / 2f, w, h, scale)
        }
    }

    private fun abs(v: Int): Int = if (v < 0) -v else v
}
