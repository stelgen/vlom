package com.stelgen.vlom.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.util.Log
import android.view.KeyEvent
import android.view.View
import com.stelgen.vlom.mcml.anim.PageTransition
import com.stelgen.vlom.mcml.anim.TransitionType
import com.stelgen.vlom.mcml.render.DesignScale
import com.stelgen.vlom.shell.AppRepository
import com.stelgen.vlom.shell.HomeLayout
import com.stelgen.vlom.shell.HomeTile
import com.stelgen.vlom.shell.ThemeManager
import com.stelgen.vlom.shell.TileIconProvider
import com.stelgen.vlom.shell.TileSettings
import com.stelgen.vlom.sound.SoundEngine
import com.stelgen.vlom.sound.SoundMap

/**
 * S2.1: старт-скрин / home-сетка Vista MCE — горизонтальная полоса тайтлов.
 * Порядок/состав тайтлов — из TileSettings (S2.2, SharedPreferences),
 * приложения запускаются через AppRepository (S2.3), темы — ThemeManager (S4.3).
 */
class HomeView(
    context: Context,
    private val settings: TileSettings,
    private val onTileActivated: (HomeTile) -> Unit = {},
) : View(context) {

    private val visible: List<HomeTile> = settings.visible
    private var focusedIndex: Int = 0

    // S4.2 (U4): анимации — плавный слайд полосы + вход старт-скрина
    private var focusFrom = 0f
    private var focusTo = 0f
    private var focusAnimStart = 0L
    private var entryStart = 0L
    private val entryTransition = PageTransition(TransitionType.ENTER, durationMs = 350)
    private var hasEntryLayer = false
    private var entryLayerId = 0
    private val icons = TileIconProvider(context)

    private val scale = DesignScale()
    private val palette = ThemeManager.palette(context)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val font = FontLoader.get(context)

    private val sounds = SoundEngine(context)
    private val apps = AppRepository(context)

    init {
        isFocusable = true
        isFocusableInTouchMode = true
        sounds.loadAll()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        sounds.play(SoundMap.UiEvent.PAGE_ENTER)
        entryStart = System.currentTimeMillis()
        postInvalidateOnAnimation()
    }

    override fun onDetachedFromWindow() {
        sounds.release()
        super.onDetachedFromWindow()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT -> if (focusedIndex > 0) {
                animateFocusTo(focusedIndex - 1)
                onStripeMoved()
                return true
            }
            KeyEvent.KEYCODE_DPAD_RIGHT -> if (focusedIndex < visible.size - 1) {
                animateFocusTo(focusedIndex + 1)
                onStripeMoved()
                return true
            }
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                activate(visible.getOrNull(focusedIndex) ?: return true)
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun animateFocusTo(target: Int) {
        focusFrom = focusTo
        focusTo = target.toFloat()
        focusAnimStart = System.currentTimeMillis()
        focusedIndex = target
        postInvalidateOnAnimation()
    }

    private fun onStripeMoved() {
        sounds.play(SoundMap.UiEvent.FOCUS_CHANGED)
        invalidate()
    }

    private fun activate(tile: HomeTile) {
        sounds.play(SoundMap.UiEvent.BUTTON_CLICK)
        when {
            tile.isWebTile -> {
                // S3.3: веб-тайтл — встроенный браузер
                context.startActivity(
                    android.content.Intent(context, WebActivity::class.java)
                        .putExtra(WebActivity.EXTRA_URL, tile.webUrl)
                )
            }
            tile.isAppTile -> {
                if (!apps.launch(tile)) {
                    Log.w(TAG, "app launch failed: ${tile.id}")
                }
            }
            else -> onTileActivated(tile) // встроенный тайтл: внутренний экран
        }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val w = width
        val h = height
        val factor = scale.factor(w, h)
        val dx = scale.offsetX(w, factor).toFloat()
        val dy = scale.offsetY(h, factor).toFloat()

        // фон темы (S4.3): градиент из палитры
        paint.shader = LinearGradient(
            0f, 0f, 0f, h.toFloat(),
            palette.backgroundTop, palette.backgroundBottom, Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)
        paint.shader = null

        canvas.save()

        // S4.2: анимация входа старт-скрина (слайд+fade, easeOutCubic)
        val entry = entryTransition.frame(System.currentTimeMillis() - entryStart)
        if (System.currentTimeMillis() - entryStart < entryTransition.durationMs) {
            canvas.translate(entry.offset * scale.basisWidth * factor, 0f)
            val layer = canvas.saveLayerAlpha(0f, 0f, width.toFloat(), height.toFloat(),
                (entry.alpha.coerceIn(0f, 1f) * 255).toInt())
            hasEntryLayer = true
            entryLayerId = layer
        }

        // S4.2: плавный слайд полосы (180мс, easeOutCubic)
        val t = ((System.currentTimeMillis() - focusAnimStart).toFloat() / FOCUS_ANIM_MS).coerceIn(0f, 1f)
        val ease = 1f - (1f - t) * (1f - t) * (1f - t)
        val focusCurrent = focusFrom + (focusTo - focusFrom) * ease
        if (t < 1f) postInvalidateOnAnimation()

        val placements = HomeLayout.stripe(visible.size, focusCurrent)

        canvas.translate(dx, dy)
        canvas.scale(factor, factor)

        placements.forEach { p ->
            val tile = visible.getOrNull(p.index) ?: return@forEach
            drawTile(canvas, tile, p, p.index == focusedIndex)
        }
        // заголовок старт-скрина
        paint.color = palette.text
        paint.typeface = FontLoader.get(context, FontLoader.Style.SEMIBOLD)
        paint.textSize = 42f
        paint.textAlign = Paint.Align.LEFT
        canvas.drawText("Vista", 60f, 120f, paint)

        if (hasEntryLayer) {
            canvas.restoreToCount(entryLayerId)
            hasEntryLayer = false
        }
        canvas.restore()
    }

    private fun drawTile(canvas: Canvas, tile: HomeTile, p: HomeLayout.Placement, focused: Boolean) {
        val rect = RectF(p.x, p.y, p.x + p.width, p.y + p.height)
        // фон тайла (стоп-кадр темы; скин из PNG — фаза 4)
        paint.style = Paint.Style.FILL
        paint.color = when {
            focused -> palette.accentFocus
            tile.builtIn -> palette.accent
            else -> palette.accentOther
        }
        paint.alpha = if (focused) 235 else 170
        canvas.drawRoundRect(rect, 10f, 10f, paint)

        if (focused) {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 4f
            paint.color = 0xFFFFFFFF.toInt()
            paint.alpha = 255
            canvas.drawRoundRect(rect, 10f, 10f, paint)
            paint.style = Paint.Style.FILL
        }

        // подпись тайтла (P6: русский, Selawik)
        paint.color = palette.text
        paint.alpha = 255
        paint.typeface = font
        paint.textSize = if (focused) 30f else 26f
        paint.textAlign = Paint.Align.CENTER
        val cy = p.y + p.height - 26f
        canvas.drawText(tile.label, rect.centerX(), cy, paint)

        // автоиконка (S2.2): APK-иконка или оригинальный PNG темы для builtIn
        icons.iconFor(tile)?.let { drawable ->
            val size = if (focused) 110f else 92f
            val cx = rect.centerX()
            val top = p.y + (p.height - size) / 2f - 22f
            drawable.setBounds(
                (cx - size / 2f).toInt(), top.toInt(),
                (cx + size / 2f).toInt(), (top + size).toInt(),
            )
            drawable.alpha = if (tile.hidden) 150 else 255
            drawable.draw(canvas)
        }
    }

    companion object {
        private const val TAG = "HomeView"
        private const val FOCUS_ANIM_MS = 180L
    }
}
