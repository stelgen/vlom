package com.stelgen.vlom

import android.app.Activity
import android.os.Bundle

/**
 * Фаза 0 (S0.4): скелет HOME-приложения.
 * Exit-критерий фазы: APK ставится на все 3 устройства и рисует заглушку поверх Home.
 * MCML-движок (фаза 1) заменит StubView на Panel-рендер.
 */
class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(StubView(this))
    }
}
