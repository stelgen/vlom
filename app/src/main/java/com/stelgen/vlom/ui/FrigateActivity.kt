package com.stelgen.vlom.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.LinearLayout
import android.widget.TextView
import com.stelgen.vlom.R

/**
 * S3.2 (B4): веб-морда Frigate во встроенном браузере — полное управление.
 * URL хранится в SharedPreferences (первый запуск — экран настройки адреса).
 * B1: системный WebView (лёгкий, RAM 3.5 ГБ учтено). WebGL/видео — средствами
 * системного Chromium; ExoPlayer-перехват — отдельный спринт (B3).
 */
class FrigateActivity : Activity() {

    private lateinit var prefs: SharedPreferences
    private var webView: WebView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences("vlom_frigate", MODE_PRIVATE)

        val url = prefs.getString(KEY_URL, null)
        if (url.isNullOrBlank()) {
            setContentView(configScreen())
        } else {
            setContentView(webScreen(url))
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun webScreen(url: String): View {
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val wv = WebView(this).apply {
            setBackgroundColor(Color.BLACK)
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false
            settings.loadWithOverviewMode = true
            settings.useWideViewPort = true
            // локальный Frigate обычно по http — cleartext разрешён в манифесте
            webViewClient = WebViewClient()
            loadUrl(url)
        }
        webView = wv
        root.addView(
            wv,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
        return root
    }

    private fun configScreen(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF06142E.toInt())
            setPadding(px(64), px(48), px(64), px(48))
        }
        val title = TextView(this).apply {
            text = getString(R.string.frigate_title)
            textSize = 36f
            setTextColor(0xFFFFFFFF.toInt())
            typeface = FontLoader.get(this@FrigateActivity, FontLoader.Style.SEMIBOLD)
        }
        val hintText = TextView(this).apply {
            text = getString(R.string.frigate_hint)
            textSize = 22f
            setTextColor(0xFF9DB6D6.toInt())
            typeface = FontLoader.get(this@FrigateActivity)
            setPadding(0, px(24), 0, px(24))
        }
        val input = android.widget.EditText(this).apply {
            hint = getString(R.string.frigate_url_hint)
            setSingleLine(true)
        }
        val go = android.widget.Button(this).apply {
            text = getString(R.string.frigate_open)
            textSize = 20f
            setOnClickListener {
                val u = normalizeUrl(input.text.toString())
                if (u != null) {
                    prefs.edit().putString(KEY_URL, u).apply()
                    setContentView(webScreen(u))
                }
            }
        }
        root.addView(title)
        root.addView(hintText)
        root.addView(input)
        root.addView(go)
        return root
    }

    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        val wv = webView
        if (wv != null && wv.canGoBack()) wv.goBack() else super.onBackPressed()
    }

    override fun onDestroy() {
        webView?.destroy()
        super.onDestroy()
    }

    private fun px(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()

    companion object {
        private const val KEY_URL = "url"

        /** http(s):// обязателен; мусор -> null. */
        fun normalizeUrl(raw: String): String? {
            val t = raw.trim()
            if (t.isEmpty()) return null
            return if (t.startsWith("http://") || t.startsWith("https://")) t
            else "http://$t"
        }
    }
}
