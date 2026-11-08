package com.stelgen.vlom.ui

import android.content.Context
import android.graphics.Canvas
import android.util.Log
import android.view.KeyEvent
import android.view.View
import com.stelgen.vlom.mcml.McmlParser
import com.stelgen.vlom.mcml.render.DesignScale
import com.stelgen.vlom.mcml.render.FocusModel
import com.stelgen.vlom.mcml.render.McmlRenderer
import com.stelgen.vlom.mcml.render.RenderNode
import com.stelgen.vlom.mcml.render.RenderNodeBuilder
import com.stelgen.vlom.sound.SoundEngine
import com.stelgen.vlom.sound.SoundMap

/**
 * Фаза 1: полноценное View движка — MCML-страница + D-pad фокус (S1.3) +
 * звуки навигации (S1.4). Переходы страниц подключаются в фазе 2.
 */
class McmlView(
    context: Context,
    private val assetPage: String,
    private val theme: String = "blue",
    private val isFocusableRoot: Boolean = true,
) : View(context) {

    @Suppress("unused")
    constructor(context: Context, attrs: android.util.AttributeSet?) : this(context, "type23_ASPECTPANEL.XML.mcml")

    private var root: RenderNode? = null
    private var focusModel: FocusModel? = null
    private val renderer = McmlRenderer(DesignScale())
    private val sounds = SoundEngine(context)

    init {
        load()
        sounds.loadAll()
    }

    private fun load() {
        try {
            context.assets.open("mcml/$theme/$assetPage").use { stream ->
                val doc = McmlParser.parse(assetPage, stream)
                root = doc.root?.let { RenderNodeBuilder.build(it) }
                focusModel = root?.let { FocusModel(it, doc.root?.attr("initialFocus")) }
                Log.i(
                    TAG,
                    "page loaded: asset=$assetPage focusables=${focusModel?.focusables?.size}",
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "failed to load $assetPage", e)
            root = null
            focusModel = null
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        sounds.play(SoundMap.UiEvent.PAGE_ENTER)
    }

    override fun onDetachedFromWindow() {
        sounds.release()
        super.onDetachedFromWindow()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        val fm = focusModel ?: return super.onKeyDown(keyCode, event)
        val direction = when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP -> FocusModel.Direction.UP
            KeyEvent.KEYCODE_DPAD_DOWN -> FocusModel.Direction.DOWN
            KeyEvent.KEYCODE_DPAD_LEFT -> FocusModel.Direction.LEFT
            KeyEvent.KEYCODE_DPAD_RIGHT -> FocusModel.Direction.RIGHT
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                sounds.play(SoundMap.UiEvent.BUTTON_CLICK)
                performClick()
                return true
            }
            else -> return super.onKeyDown(keyCode, event)
        }
        if (fm.move(direction)) {
            sounds.play(SoundMap.UiEvent.FOCUS_CHANGED)
            invalidate()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onDraw(canvas: Canvas) {
        val r = root ?: return
        renderer.draw(
            r, canvas, width, height,
            focusedId = focusModel?.focused?.id,
            transition = null, // page transitions — фаза 2 (navigateTo/Leave)
        )
    }

    companion object {
        private const val TAG = "McmlView"
    }
}
