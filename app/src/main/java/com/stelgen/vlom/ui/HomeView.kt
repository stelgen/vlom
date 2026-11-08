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
import com.stelgen.vlom.shell.SkinBitmaps
import com.stelgen.vlom.shell.SkinCatalog
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

    @Suppress("unused")
    constructor(context: Context, attrs: android.util.AttributeSet?) : this(context)

    @Suppress("unused")
    constructor(context: Context) : this(context, com.stelgen.vlom.shell.TileSettings())

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
    private val skins = SkinBitmaps(context)
    private val bgRect = RectF()
    private val tileRect = RectF()
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

    // Анимация полосы: пересборка placements каждый кадр (маленький список,-count<20).
    // Кэширование отложено до профилирования (S5.1 на устройстве).
    @android.annotation.SuppressLint("DrawAllocation")
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
        // S4.1: оригинальный фон ehres (COMMON.BACKGROUND) поверх палитры
        skins.get(SkinCatalog.homeBackground)?.let { bg ->
            bgRect.set(0f, 0f, w.toFloat(), h.toFloat())
            canvas.drawBitmap(bg, null, bgRect, paint)
        }

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
        val rect = tileRect
        rect.set(p.x, p.y, p.x + p.width, p.y + p.height)
        val skin = SkinCatalog.forTile(tile)

        if (focused) {
            // S4.1: большой стеклянный тайл фокуса — оригинальный PNG (аспект 1:1 с ячейкой)
            val focusBmp = skin?.focus?.let { skins.get(it) }
                ?: skins.get(SkinCatalog.focusBackground)
            if (focusBmp != null) {
                canvas.drawBitmap(focusBmp, null, rect, paint)

                if (!tile.builtIn) {
                    // app/web-тайтл: иконка APK по центру + русская подпись
                    icons.iconFor(tile)?.let { d ->
                        val size = 96f
                        d.setBounds(
                            (rect.centerX() - size / 2f).toInt(),
                            (rect.centerY() - size / 2f).toInt(),
                            (rect.centerX() + size / 2f).toInt(),
                            (rect.centerY() + size / 2f).toInt(),
                        )
                        d.draw(canvas)
                    }
                    drawLabel(canvas, tile.label, rect.centerX(), rect.bottom - 34f, 26f)
                }
                return
            }
            // fallback: цветная палитра + рамка
            paint.style = Paint.Style.FILL
            paint.color = palette.accentFocus
            paint.alpha = 235
            canvas.drawRoundRect(rect, 10f, 10f, paint)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 4f
            paint.color = 0xFFFFFFFF.toInt()
            canvas.drawRoundRect(rect, 10f, 10f, paint)
            drawLabel(canvas, tile.label, rect.centerX(), rect.centerY(), 30f)
            return
        }

        // не-фокус: маленькая иконка NOFOCUS (builtIn) или APK-иконка
        val iconBmp = skin?.nofocus?.let { skins.get(it) }
        if (iconBmp != null) {
            canvas.drawBitmap(iconBmp, null, rect, paint)
        } else {
            paint.style = Paint.Style.FILL
            paint.color = palette.accent
            paint.alpha = 170
            canvas.drawRoundRect(rect, 10f, 10f, paint)
            paint.alpha = 255
            icons.iconFor(tile)?.let { d ->
                val size = p.height * 0.45f
                d.setBounds(
                    (rect.centerX() - size / 2f).toInt(),
                    (rect.centerY() - size / 2f).toInt(),
                    (rect.centerX() + size / 2f).toInt(),
                    (rect.centerY() + size / 2f).toInt(),
                )
                d.draw(canvas)
            }
            drawLabel(canvas, tile.label, rect.centerX(), rect.bottom - 30f, 24f)
        }
    }

    private fun drawLabel(canvas: Canvas, text: String, cx: Float, cy: Float, size: Float) {
        paint.color = palette.text
        paint.alpha = 255
        paint.typeface = font
        paint.textSize = size
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText(text, cx, cy, paint)
    }

    companion object {
        private const val TAG = "HomeView"
        private const val FOCUS_ANIM_MS = 180L
    }
}
