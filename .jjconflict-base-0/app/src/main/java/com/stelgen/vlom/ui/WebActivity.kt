package com.stelgen.vlom.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.LinearLayout
import android.widget.Toast

/**
 * S3.3 (B1-B4): веб-тайтл — произвольный сайт во встроенном браузере.
 *  - движок: системный WebView (лёгкий, RAM 3.5 ГБ); GeckoView — опционально позже;
 *  - WebGL 2.0 — из коробки Chromium (аппаратное ускорение включено по умолчанию);
 *  - видео: HTML5/HLS средствами Chromium (B3: ExoPlayer-перехват HLS/DASH —
 *    отдельный спринт, требует зависимость media3).
 */
class WebActivity : Activity() {

    private var webView: WebView? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val url = intent.getStringExtra(EXTRA_URL)

        if (url.isNullOrBlank()) {
            Toast.makeText(this, "Нет адреса сайта", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val wv = WebView(this).apply {
            setBackgroundColor(Color.BLACK)
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.mediaPlaybackRequiresUserGesture = false
            settings.loadWithOverviewMode = true
            settings.useWideViewPort = true
            webViewClient = WebViewClient() // навигация внутри тайтла
            loadUrl(url)
        }
        webView = wv
        root.addView(
            wv,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
        setContentView(root)
    }

    override fun onBackPressed() {
        val wv = webView
        if (wv != null && wv.canGoBack()) wv.goBack() else super.onBackPressed()
    }

    override fun onDestroy() {
        webView?.destroy()
        super.onDestroy()
    }

    companion object {
        const val EXTRA_URL = "url"
    }
}
