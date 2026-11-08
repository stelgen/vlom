package com.stelgen.vlom.weather

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Тесты S3.1: WMO-карта и парсеры Open-Meteo (чистые, JVM).
 */
class WeatherTest {

    @Test
    fun `wmo codes map to russian`() {
        assertEquals("Ясно", WmoCodes.describe(0))
        assertEquals("Пасмурно", WmoCodes.describe(3))
        assertEquals("Снег", WmoCodes.describe(73))
        assertEquals("Гроза", WmoCodes.describe(95))
    }

    @Test
    fun `unknown wmo code falls back safely`() {
        assertEquals("Погода", WmoCodes.describe(42))
        assertEquals("Погода", WmoCodes.describe(-1))
    }

    @Test
    fun `forecast json parses`() {
        val json = """
        {"current_weather":{"temperature":7.4,"windspeed":3.2,"weathercode":61,"is_day":0},
         "daily":{"temperature_2m_max":[9.1,8.0,7.5,6.2,5.9],"temperature_2m_min":[2.0,1.1,0.4,-0.5,1.2]}}
        """.trimIndent()
        val s = WeatherParser.parseForecast(json)

        assertEquals(7.4, s.temperatureC, 0.001)
        assertEquals(61, s.weatherCode)
        assertEquals(false, s.isDay)
        assertEquals("Небольшой дождь", s.description())
        assertEquals(5, s.dailyMaxC.size)
        assertEquals(5, s.dailyMinC.size)
    }

    @Test
    fun `geocoding json parses first result`() {
        val json = """
        {"results":[{"name":"Москва","latitude":55.7558,"longitude":37.6176,"country":"Россия"}]}
        """.trimIndent()
        val p = WeatherParser.parseGeocoding(json)
        assertEquals("Москва", p?.name)
        assertEquals(55.7558, p?.latitude ?: 0.0, 0.001)
        assertEquals("Россия", p?.country)
    }

    @Test
    fun `geocoding without results is null`() {
        assertNull(WeatherParser.parseGeocoding("""{"results":[]}"""))
        assertNull(WeatherParser.parseGeocoding("""{}"""))
    }
}
