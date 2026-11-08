package com.stelgen.vlom

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.view.View

/**
 * Заглушка старт-скрина (фаза 0). Vista Blue-градиент + маркер версии.
 * Пиксель-верность не требуется до фазы 4 (скринтюнинг).
 */
internal class StubView(context: Context) : View(context) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var backdrop: LinearGradient? = null

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        // S5.1: предвычисляем вне onDraw (lint DrawAllocation)
        backdrop = LinearGradient(
            0f, 0f, 0f, h.toFloat(),
            0xFF0B3D91.toInt(), 0xFF06142E.toInt(), Shader.TileMode.CLAMP
        )
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()

        paint.shader = backdrop
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        paint.shader = null
        paint.color = 0xFFFFFFFF.toInt()
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = h * 0.08f
        canvas.drawText("Vista — заглушка HOME", w / 2f, h / 2f, paint)

        paint.alpha = 160
        paint.textSize = h * 0.035f
        canvas.drawText(
            "VLOM ${Version.current} · фаза 0 · MCML-движок в фазе 1",
            w / 2f, h / 2f + h * 0.08f, paint
        )
    }
}
