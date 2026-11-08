package com.stelgen.vlom

import android.app.Activity
import android.os.Bundle
import com.stelgen.vlom.ui.McmlView

/**
 * Фаза 0 (S0.4): скелет HOME-приложения.
 * Фаза 1 (S1.2): старт-скрин рендерится MCML-движком из реальной страницы темы.
 * Fallback — Vista Blue-заглушка, если страница не распарсилась.
 */
class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(McmlView(this, DEMO_PAGE))
    }

    companion object {
        // демо-страница фазы 1: простая, без сервис-бус-зависимостей
        const val DEMO_PAGE = "type23_ASPECTPANEL.XML.mcml"
    }
}
