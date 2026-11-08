package com.stelgen.vlom.mcml.anim

/**
 * S1.4: анимационный движок переходов страниц.
 *
 * Факты из корпуса: панели декларируют именованные переходы
 *   navigateToAnimation="Unified.AreaPanel.Enter"
 *   navigateAwayAnimation="Unified.AreaPanel.Leave"
 *   navigateToBackAnimation="...EnterBack", navigateAwayBackAnimation="...LeaveBack"
 * (ссылки на библиотеку анимаций ehshell; сами определения — в bin/include,
 * реконструируются по видеореференсам). Vista MCE: горизонтальный слайд +
 * fade, ~0.3–0.5 c (уточняется референсами фазы 4).
 *
 * Чистая математика (без android.*) — тестируется в JVM.
 */
enum class TransitionType { ENTER, LEAVE, ENTER_BACK, LEAVE_BACK }

data class PageTransition(
    val type: TransitionType,
    val durationMs: Long = DEFAULT_DURATION_MS,
    val distance: Float = 1f, // в долях ширины базиса
) {
    companion object {
        const val DEFAULT_DURATION_MS = 350L
    }

    /** Прогресс [0..1] по elapsed. Вне длительности — 1 (завершено). */
    fun progress(elapsedMs: Long): Float =
        if (durationMs <= 0) 1f
        else (elapsedMs.toFloat() / durationMs).coerceIn(0f, 1f)

    /**
     * Состояние кадра: альфа и горизонтальный сдвиг в долях базиса.
     * ENTER:      въезд справа  (x: +d→0, alpha: 0→1)
     * LEAVE:      выезд влево   (x: 0→-d, alpha: 1→0)
     * ENTER_BACK: въезд слева   (x: -d→0, alpha: 0→1)
     * LEAVE_BACK: выезд вправо  (x: 0→+d, alpha: 1→0)
     */
    fun frame(elapsedMs: Long, interpolator: (Float) -> Float = { Interpolators.easeOutCubic(it) }): Frame {
        val p = interpolator(progress(elapsedMs))
        return when (type) {
            TransitionType.ENTER -> Frame(offset = distance * (1 - p), alpha = p)
            TransitionType.LEAVE -> Frame(offset = -distance * p, alpha = 1 - p)
            TransitionType.ENTER_BACK -> Frame(offset = -distance * (1 - p), alpha = p)
            TransitionType.LEAVE_BACK -> Frame(offset = distance * p, alpha = 1 - p)
        }
    }

    data class Frame(val offset: Float, val alpha: Float)
}

object Interpolators {
    val linear: (Float) -> Float = { it }

    /** easeOutCubic — быстрый старт, мягкое окончание (ощущение Vista MCE). */
    val easeOutCubic: (Float) -> Float = { p -> 1f - (1f - p).pow3() }

    private fun Float.pow3(): Float = this * this * this
}
