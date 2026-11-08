package com.stelgen.vlom.ui

import android.app.Activity
import android.content.SharedPreferences
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.EditText
import com.stelgen.vlom.R
import com.stelgen.vlom.weather.GeoPlace
import com.stelgen.vlom.weather.WeatherApi
import com.stelgen.vlom.weather.WeatherSnapshot
import kotlin.concurrent.thread

/**
 * S3.1: тайл/экран погоды в стиле MCE.
 * W2: гео по IP при первом запуске -> подтверждение/смена города (диалог с вводом).
 * Загрузка в фоне, обновление UI на главном потоке. Ошибки сети -> мягкий текст.
 */
class WeatherActivity : Activity() {

    private lateinit var prefs: SharedPreferences
    private lateinit var status: TextView
    private lateinit var current: TextView
    private lateinit var daily: TextView

    private var place: GeoPlace? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences("vlom_weather", MODE_PRIVATE)
        setContentView(buildUi())
        resolvePlaceAndLoad()
    }

    private fun buildUi(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xFF06142E.toInt())
            setPadding(px(64), px(48), px(64), px(48))
        }
        val header = TextView(this).apply {
            text = getString(R.string.weather_title)
            textSize = 40f
            setTextColor(0xFFFFFFFF.toInt())
            typeface = FontLoader.get(this@WeatherActivity, FontLoader.Style.SEMIBOLD)
            setPadding(0, 0, 0, px(24))
        }
        root.addView(header)

        status = TextView(this).apply {
            textSize = 24f
            setTextColor(0xFF9DB6D6.toInt())
            typeface = FontLoader.get(this@WeatherActivity)
        }
        root.addView(status)

        current = TextView(this).apply {
            textSize = 72f
            setTextColor(0xFFFFFFFF.toInt())
            typeface = FontLoader.get(this@WeatherActivity, FontLoader.Style.SEMIBOLD)
            setPadding(0, px(24), 0, 0)
        }
        root.addView(current)

        daily = TextView(this).apply {
            textSize = 24f
            setTextColor(0xFFCFE0F5.toInt())
            typeface = FontLoader.get(this@WeatherActivity)
            setPadding(0, px(24), 0, 0)
        }
        root.addView(daily)

        val changeBtn = Button(this).apply {
            text = getString(R.string.weather_change_city)
            textSize = 20f
            setOnClickListener { promptCity() }
        }
        root.addView(changeBtn, LinearLayout.LayoutParams(WRAP, WRAP).apply { topMargin = px(32) })

        return root
    }

    private fun resolvePlaceAndLoad() {
        val savedCity = prefs.getString(KEY_CITY, null)
        thread {
            try {
                val resolved = savedCity?.let { WeatherApi.geocode(it) }
                    ?: run {
                        val ip = WeatherApi.locateByIp()
                        if (ip.city.isNotBlank()) {
                            GeoPlace(ip.city, ip.latitude, ip.longitude, "")
                        } else {
                            WeatherApi.geocode("Москва") // безопасный дефолт
                        }
                    }
                place = resolved
                val snapshot = resolved?.let { WeatherApi.forecast(it) }
                runOnUiThread {
                    status.text = resolved?.let { "${it.name}, ${it.country}".trimEnd(',', ' ') }
                        ?: getString(R.string.weather_no_city)
                    render(snapshot)
                }
            } catch (e: Exception) {
                runOnUiThread {
                    status.text = getString(R.string.weather_offline)
                    current.text = "—".let { it }
                    daily.text = ""
                }
            }
        }
    }

    private fun render(snapshot: WeatherSnapshot?) {
        if (snapshot == null) {
            current.text = "—".let { it }
            daily.text = ""
            return
        }
        current.text = getString(
            R.string.weather_current_fmt,
            snapshot.temperatureC.toInt().toString(),
            snapshot.description(),
        )
        val min = snapshot.dailyMinC.take(5).map { it.toInt() }
        val max = snapshot.dailyMaxC.take(5).map { it.toInt() }
        daily.text = if (min.size == max.size && min.isNotEmpty()) {
            getString(
                R.string.weather_days_fmt,
                min.zip(max).joinToString("  ") { "${it.second}° / ${it.first}°" },
            )
        } else ""
    }

    private fun promptCity() {
        val input = EditText(this).apply {
            hint = getString(R.string.weather_city_hint)
            setSingleLine(true)
        }
        AlertDialogs.confirm(
            this, getString(R.string.weather_dialog_title), input
        ) {
            val city = input.text.toString().trim()
            if (city.isNotEmpty()) {
                prefs.edit().putString(KEY_CITY, city).apply()
                resolvePlaceAndLoad()
            }
        }
    }

    private fun px(dp: Int): Int = (dp * resources.displayMetrics.density).toInt()

    companion object {
        private const val KEY_CITY = "city"
        private const val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT
    }
}

/** Мини-хелпер диалогов (без XML-разметки). */
object AlertDialogs {
    fun confirm(activity: Activity, title: String, view: View, onOk: () -> Unit) {
        android.app.AlertDialog.Builder(activity)
            .setTitle(title)
            .setView(view)
            .setPositiveButton(R.string.btn_ok) { _, _ -> onOk() }
            .setNegativeButton(R.string.btn_cancel, null)
            .show()
    }
}
