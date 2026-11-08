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
     * Раскладка видимых тайтлов вокруг фокусного.
     * Центры ячеек идут с постоянным шагом (TILE_W+GAP); визуальная позиция =
     * центр ячейки минус половина масштабированной ширины тайла.
     */
    fun stripe(count: Int, focusedIndex: Int): List<Placement> {
        require(count >= 0) { "count must be non-negative" }
        val fi = focusedIndex.coerceIn(0, (count - 1).coerceAtLeast(0))

        val pitch = TILE_W + GAP
        val shift = BASIS_W / 2f - TILE_W / 2f - fi * pitch

        return (0 until count).map { i ->
            val rel = i - fi
            val scale = when {
                rel == 0 -> FOCUS_SCALE
                abs(rel) == 1 -> NEIGHBOUR_SCALE
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
