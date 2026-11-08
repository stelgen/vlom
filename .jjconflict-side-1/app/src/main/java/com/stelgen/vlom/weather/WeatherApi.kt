package com.stelgen.vlom.weather

import java.net.HttpURLConnection
import java.net.URL

/**
 * S3.1 (W1/W2): сетевые клиенты Open-Meteo и геолокация по IP.
 * Все URL — открытые API без ключей. Чистые парсеры — в WeatherParser (тестируемые),
 * здесь только HTTP-транспорт + сборка URL.
 */
object WeatherApi {

    private const val GEO_BY_IP = "http://ip-api.com/json/?lang=ru&fields=city,lat,lon"
    private const val GEOCODING = "https://geocoding-api.open-meteo.com/v1/search?name=%s&count=1&language=ru&format=json"
    private const val FORECAST =
        "https://api.open-meteo.com/v1/forecast?latitude=%s&longitude=%s" +
            "&current_weather=true&daily=temperature_2m_max,temperature_2m_min&forecast_days=5&timezone=auto"

    data class IpLocation(val city: String, val latitude: Double, val longitude: Double)

    /** W2: гео по IP (ip-api.com, без ключа, clear-text — разрешён в манифесте). */
    fun locateByIp(): IpLocation {
        val json = httpGet(GEO_BY_IP)
        val root = org.json.JSONObject(json)
        return IpLocation(
            city = root.optString("city", ""),
            latitude = root.optDouble("lat", Double.NaN),
            longitude = root.optDouble("lon", Double.NaN),
        )
    }

    /** Поиск города по имени (русский ввод поддержан). */
    fun geocode(city: String): GeoPlace? {
        val url = GEOCODING.format(java.net.URLEncoder.encode(city, "UTF-8"))
        return WeatherParser.parseGeocoding(httpGet(url))
    }

    /** Текущая погода + 5 дней min/max. */
    fun forecast(place: GeoPlace): WeatherSnapshot =
        WeatherParser.parseForecast(
            httpGet(FORECAST.format(place.latitude, place.longitude))
        )

    fun forecast(lat: Double, lon: Double): WeatherSnapshot =
        WeatherParser.parseForecast(httpGet(FORECAST.format(lat, lon)))

    private fun httpGet(url: String, timeoutMs: Int = 8000): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = timeoutMs
        conn.readTimeout = timeoutMs
        conn.setRequestProperty("Accept", "application/json")
        try {
            if (conn.responseCode !in 200..299) error("HTTP ${conn.responseCode} for $url")
            return conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }
}
