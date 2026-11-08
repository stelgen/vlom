package com.stelgen.vlom.mcml.render

import kotlin.math.abs

/**
 * S1.3: фокус-модель D-pad поверх RenderNode-дерева.
 *
 * Факты из корпуса blue-full:
 *  - фокусируемые элементы декларируются атрибутом initialFocus="_id" на корне
 *    страницы (83× "_btnNext", 32× "_ok", ...);
 *  - id фокусируемых элементов, как правило, начинается с "_".
 *
 * Навигация: пространственная по геометрии (left/top/width/height); если у узла
 * геометрии нет — берётся центр (0,0), при равенстве — порядок документа.
 * Чистый Kotlin (без android.*) — юнит-тесты без Robolectric.
 */
class FocusModel(
    root: RenderNode,
    initialFocusId: String? = null,
) {
    /** Фокусируемые узлы в порядке документа. */
    val focusables: List<RenderNode> = collect(root)

    private val indexById: Map<String, Int> =
        focusables.mapIndexedNotNull { i, n -> n.id?.let { it to i } }.toMap()

    val focusedIndexInitial: Int = resolveInitial(initialFocusId)

    var focusedIndex: Int = focusedIndexInitial
        private set

    val focused: RenderNode? get() = focusables.getOrNull(focusedIndex)

    private fun resolveInitial(initialFocusId: String?): Int {
        if (focusables.isEmpty()) return 0
        initialFocusId?.let { id -> indexById[id]?.let { return it } }
        val first = focusables.indexOfFirst { it.id != null }
        return if (first >= 0) first else 0
    }

    enum class Direction { UP, DOWN, LEFT, RIGHT }

    /**
     * Переместить фокус. Возвращает true, если фокус сменился.
     * Пространственный алгоритм: кандидаты в полуплоскости направления,
     * приоритет — расстояние по главной оси, вторично — отклонение по поперечной.
     */
    fun move(direction: Direction): Boolean {
        if (focusables.size < 2) return false
        val cur = focusables[focusedIndex]
        val (cx, cy) = cur.center()

        var bestIdx = -1
        var bestScore = Float.MAX_VALUE
        focusables.forEachIndexed { i, cand ->
            if (i == focusedIndex) return@forEachIndexed
            val (nx, ny) = cand.center()
            val dx = nx - cx
            val dy = ny - cy
            val primary: Float
            val secondary: Float
            val ok: Boolean
            when (direction) {
                Direction.RIGHT -> { ok = dx > 0; primary = dx; secondary = abs(dy) }
                Direction.LEFT -> { ok = dx < 0; primary = -dx; secondary = abs(dy) }
                Direction.DOWN -> { ok = dy > 0; primary = dy; secondary = abs(dx) }
                Direction.UP -> { ok = dy < 0; primary = -dy; secondary = abs(dx) }
            }
            if (ok && primary + secondary * 2f < bestScore) {
                bestScore = primary + secondary * 2f
                bestIdx = i
            }
        }

        if (bestIdx < 0) return false
        focusedIndex = bestIdx
        return true
    }

    private fun RenderNode.center(): Pair<Float, Float> {
        val g = geometry
        val left = g.left ?: 0
        val top = g.top ?: 0
        val w = g.width ?: 0
        val h = g.height ?: 0
        return (left + w / 2f) to (top + h / 2f)
    }

    companion object {
        private fun collect(root: RenderNode): List<RenderNode> {
            val out = mutableListOf<RenderNode>()
            fun walk(node: RenderNode) {
                val isFocusable = node.id?.startsWith("_") == true &&
                    (node is RenderNode.Button || node is RenderNode.Panel)
                if (isFocusable) out += node
                node.children.forEach { walk(it) }
            }
            walk(root)
            return out
        }
    }
}
