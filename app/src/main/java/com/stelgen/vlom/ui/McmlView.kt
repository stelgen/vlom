package com.stelgen.vlom.ui

import android.content.Context
import android.graphics.Canvas
import android.util.Log
import android.view.View
import com.stelgen.vlom.mcml.McmlParser
import com.stelgen.vlom.mcml.render.DesignScale
import com.stelgen.vlom.mcml.render.McmlRenderer
import com.stelgen.vlom.mcml.render.RenderNode
import com.stelgen.vlom.mcml.render.RenderNodeBuilder

/**
 * S1.2: View, рендерящий MCML-страницу из assets через движок фазы 1.
 * Загрузка ленивая, при ошибке парсинга — прозрачный fallback (рисует ничего).
 */
class McmlView(
    context: Context,
    private val assetPage: String,
    private val theme: String = "blue",
) : View(context) {

    private var root: RenderNode? = null
    private val renderer = McmlRenderer(DesignScale())

    init {
        load()
    }

    private fun load() {
        try {
            context.assets.open("mcml/$theme/$assetPage").use { stream ->
                val doc = McmlParser.parse(assetPage, stream)
                root = doc.root?.let { RenderNodeBuilder.buildDocument(it) }
                Log.i(TAG, "page loaded: asset=$assetPage root=${doc.root?.tag}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "failed to load $assetPage", e)
            root = null
        }
    }

    override fun onDraw(canvas: Canvas) {
        val r = root ?: return
        renderer.draw(r, canvas, width, height)
    }

    companion object {
        private const val TAG = "McmlView"
    }
}
