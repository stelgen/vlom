package com.stelgen.vlom.weather

import org.json.JSONObject

/**
 * S3.1: модель погоды Open-Meteo (без ключа, W2).
 */
data class WeatherSnapshot(
    val temperatureC: Double,
    val windSpeedMs: Double,
    val weatherCode: Int,
    val isDay: Boolean,
    val dailyMinC: List<Double> = emptyList(),
    val dailyMaxC: List<Double> = emptyList(),
) {
    fun description(): String = WmoCodes.describe(weatherCode)
}

/**
 * WMO weather interpretation codes (0..99) -> русский текст (P6).
 * Подмножество реально отдаваемых Open-Meteo кодов; неизвестные -> "Погода".
 */
object WmoCodes {
    private val RU = mapOf(
        0 to "Ясно",
        1 to "Преимущественно ясно",
        2 to "Переменная облачность",
        3 to "Пасмурно",
        45 to "Туман",
        48 to "Изморозь",
        51 to "Слабая морось",
        53 to "Морось",
        55 to "Сильная морось",
        56 to "Ледяная морось",
        57 to "Сильная ледяная морось",
        61 to "Небольшой дождь",
        63 to "Дождь",
        65 to "Сильный дождь",
        66 to "Ледяной дождь",
        67 to "Сильный ледяной дождь",
        71 to "Небольшой снег",
        73 to "Снег",
        75 to "Сильный снег",
        77 to "Снежные зёрна",
        80 to "Небольшой ливень",
        81 to "Ливень",
        82 to "Сильный ливень",
        85 to "Снежный ливень",
        86 to "Сильный снежный ливень",
        95 to "Гроза",
        96 to "Гроза с градом",
        99 to "Сильная гроза с градом",
    )

    fun describe(code: Int): String = RU[code] ?: "Погода"
}

object WeatherParser {

    /** Парсинг ответа api.open-meteo.com/v1/forecast (current_weather + daily). */
    fun parseForecast(json: String): WeatherSnapshot {
        val root = JSONObject(json)
        val current = root.getJSONObject("current_weather")
        val daily = root.optJSONObject("daily")
        fun doubles(name: String): List<Double> =
            daily?.optJSONArray(name)?.let { arr ->
                (0 until arr.length()).map { arr.optDouble(it, Double.NaN) }.filter { !it.isNaN() }
            } ?: emptyList()
        return WeatherSnapshot(
            temperatureC = current.getDouble("temperature"),
            windSpeedMs = current.optDouble("windspeed", 0.0),
            weatherCode = current.optInt("weathercode", 0),
            isDay = current.optInt("is_day", 1) == 1,
            dailyMinC = doubles("temperature_2m_min"),
            dailyMaxC = doubles("temperature_2m_max"),
        )
    }

    /** Парсинг geocoding-api.open-meteo.com/v1/search — первый результат. */
    fun parseGeocoding(json: String): GeoPlace? {
        val root = JSONObject(json)
        val results = root.optJSONArray("results") ?: return null
        if (results.length() == 0) return null
        val first = results.getJSONObject(0)
        return GeoPlace(
            name = first.getString("name"),
            latitude = first.getDouble("latitude"),
            longitude = first.getDouble("longitude"),
            country = first.optString("country", ""),
        )
    }
}

data class GeoPlace(val name: String, val latitude: Double, val longitude: Double, val country: String)
