package com.stelgen.vlom.mcml.render

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader

/**
 * S1.2: Canvas-рендер RenderNode-дерева в базисе с масштабированием.
 *
 * Фаза 1 — геометрия и иерархия. Пиксельная верность скинов (цвета/ NinePatch
 * из PNG-ресурсов темы) — фаза 4 (S4.1), палитра ниже — стоп-кадр Vista Blue.
 */
class McmlRenderer(
    private val scale: DesignScale = DesignScale(),
    private val palette: Palette = Palette.VISTA_BLUE,
) {

    /** Палитра темы (заготовка; заменяется ресурсами ehres в фазе 4). */
    data class Palette(val backgroundTop: Int, val backgroundBottom: Int, val text: Int) {
        companion object {
            val VISTA_BLUE = Palette(0xFF0B3D91.toInt(), 0xFF06142E.toInt(), 0xFFFFFFFF.toInt())
            val VISTA_BLACK = Palette(0xFF101010.toInt(), 0xFF000000.toInt(), 0xFFEDEDED.toInt())
            val VISTA_GREEN = Palette(0xFF0B3D1E.toInt(), 0xFF062E14.toInt(), 0xFFFFFFFF.toInt())
        }
    }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    fun draw(root: RenderNode, canvas: Canvas, screenWidth: Int, screenHeight: Int) {
        val factor = scale.factor(screenWidth, screenHeight)
        val dx = scale.offsetX(screenWidth, factor).toFloat()
        val dy = scale.offsetY(screenHeight, factor).toFloat()

        drawBackdrop(canvas, screenWidth, screenHeight)
        canvas.save()
        canvas.translate(dx, dy)
        canvas.scale(factor, factor)
        drawNode(root, canvas, 0, 0)
        canvas.restore()
    }

    private fun drawBackdrop(canvas: Canvas, w: Int, h: Int) {
        paint.shader = LinearGradient(
            0f, 0f, 0f, h.toFloat(),
            palette.backgroundTop, palette.backgroundBottom, Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)
        paint.shader = null
    }

    private fun drawNode(node: RenderNode, canvas: Canvas, parentX: Int, parentY: Int) {
        val g = node.geometry
        val x = parentX + (g.left ?: 0)
        val y = parentY + (g.top ?: 0)
        val w = g.width
        val h = g.height

        if (w != null && h != null) {
            paint.style = Paint.Style.FILL
            paint.color = panelColor(node)
            paint.alpha = if (node is RenderNode.Panel) 40 else 70
            canvas.drawRect(x.toFloat(), y.toFloat(), (x + w).toFloat(), (y + h).toFloat(), paint)
        }

        when (node) {
            is RenderNode.Text -> drawText(node, canvas, x, y, w)
            is RenderNode.Button -> node.label?.let { drawLabel(it, canvas, x, y, w) }
            else -> {} // Panel/Image: до декодирования PNG-ресурсов (фаза 4)
        }

        node.children.forEach { drawNode(it, canvas, x, y) }
    }

    private fun drawText(node: RenderNode.Text, canvas: Canvas, x: Int, y: Int, w: Int?) {
        drawLabel(node.value, canvas, x, y, w)
    }

    private fun drawLabel(value: String, canvas: Canvas, x: Int, y: Int, w: Int?) {
        if (value.isEmpty()) return
        paint.color = palette.text
        paint.textSize = 24f
        paint.textAlign = Paint.Align.LEFT
        // "#Resource.Key" — локализуемые строки: до фазы 2 показываем ключ как есть
        canvas.drawText(value, x.toFloat(), y + 24f, paint)
    }

    private fun panelColor(node: RenderNode): Int = when {
        node.cls?.startsWith("Panel") == true -> Color.parseColor("#3A6BC4")
        node.cls?.startsWith("Button") == true -> Color.parseColor("#5B8FD9")
        node is RenderNode.Image -> Color.parseColor("#2C4E86")
        else -> Color.parseColor("#3A6BC4")
    }
}
