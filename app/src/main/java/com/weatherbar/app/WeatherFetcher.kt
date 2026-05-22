package com.weatherbar.app

import android.content.Context
import org.json.JSONObject
import java.net.URL
import javax.net.ssl.HttpsURLConnection

data class WeatherResult(
    val temperature: Double,
    val weatherCode: Int,
    val description: String,
    val icon: String
)

class WeatherFetcher(private val context: Context) {

    private val prefs = context.getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE)

    fun fetchWeather(): WeatherResult? {
        val city = prefs.getString(MainActivity.KEY_CITY, "Istanbul") ?: "Istanbul"
        val apiType = prefs.getInt(MainActivity.KEY_API_TYPE, MainActivity.API_OPEN_METEO)
        val apiKey = prefs.getString(MainActivity.KEY_API_KEY, "") ?: ""
        val unit = prefs.getString(MainActivity.KEY_UNIT, "C") ?: "C"

        return when (apiType) {
            MainActivity.API_OPENWEATHER -> fetchOpenWeather(city, apiKey, unit)
            MainActivity.API_WEATHERAPI -> fetchWeatherApi(city, apiKey, unit)
            else -> fetchOpenMeteo(city, unit)
        }
    }

    private fun fetchOpenMeteo(city: String, unit: String): WeatherResult? {
        return try {
            val geoUrl = "https://geocoding-api.open-meteo.com/v1/search?name=${encode(city)}&count=1&language=tr"
            val geoJson = fetchUrl(geoUrl) ?: return null
            val results = JSONObject(geoJson).optJSONArray("results") ?: return null
            if (results.length() == 0) return null
            val first = results.getJSONObject(0)
            val lat = first.getDouble("latitude")
            val lon = first.getDouble("longitude")

            val tempUnit = if (unit == "C") "celsius" else "fahrenheit"
            val url = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current=temperature_2m,weather_code&temperature_unit=$tempUnit"
            val json = fetchUrl(url) ?: return null
            val current = JSONObject(json).getJSONObject("current")
            val temp = current.getDouble("temperature_2m")
            val code = current.getInt("weather_code")
            val iconPack = prefs.getInt(MainActivity.KEY_ICON_PACK, 0)
            val (desc, icon) = getWeatherInfo(code, iconPack)
            WeatherResult(temp, code, desc, icon)
        } catch (e: Exception) { e.printStackTrace(); null }
    }

    private fun fetchOpenWeather(city: String, apiKey: String, unit: String): WeatherResult? {
        if (apiKey.isEmpty()) return null
        return try {
            val units = if (unit == "C") "metric" else "imperial"
            val url = "https://api.openweathermap.org/data/2.5/weather?q=${encode(city)}&appid=$apiKey&units=$units&lang=tr"
            val json = fetchUrl(url) ?: return null
            val obj = JSONObject(json)
            if (obj.optInt("cod") != 200) return null
            val temp = obj.getJSONObject("main").getDouble("temp")
            val w = obj.getJSONArray("weather").getJSONObject(0)
            val desc = w.getString("description").replaceFirstChar { it.uppercase() }
            val owmId = w.getInt("id")
            val iconPack = prefs.getInt(MainActivity.KEY_ICON_PACK, 0)
            val (_, icon) = getWeatherInfo(owmToWmo(owmId), iconPack)
            WeatherResult(temp, owmId, desc, icon)
        } catch (e: Exception) { e.printStackTrace(); null }
    }

    private fun fetchWeatherApi(city: String, apiKey: String, unit: String): WeatherResult? {
        if (apiKey.isEmpty()) return null
        return try {
            val url = "https://api.weatherapi.com/v1/current.json?key=$apiKey&q=${encode(city)}&lang=tr"
            val json = fetchUrl(url) ?: return null
            val obj = JSONObject(json)
            val current = obj.getJSONObject("current")
            val temp = if (unit == "C") current.getDouble("temp_c") else current.getDouble("temp_f")
            val condition = current.getJSONObject("condition")
            val desc = condition.getString("text")
            val code = condition.getInt("code")
            val iconPack = prefs.getInt(MainActivity.KEY_ICON_PACK, 0)
            val wmo = wapiToWmo(code)
            val (_, icon) = getWeatherInfo(wmo, iconPack)
            WeatherResult(temp, code, desc, icon)
        } catch (e: Exception) { e.printStackTrace(); null }
    }

    private fun fetchUrl(urlString: String): String? {
        return try {
            val conn = URL(urlString).openConnection() as HttpsURLConnection
            conn.connectTimeout = 10000; conn.readTimeout = 10000; conn.requestMethod = "GET"
            if (conn.responseCode == 200) conn.inputStream.bufferedReader().readText() else null
        } catch (e: Exception) { e.printStackTrace(); null }
    }

    private fun encode(s: String) = s.replace(" ", "+")

    private fun owmToWmo(id: Int) = when (id) {
        800 -> 0; 801 -> 1; 802 -> 2; 803, 804 -> 3
        in 300..399 -> 51; in 500..501 -> 61; in 502..504 -> 65
        in 600..601 -> 71; in 602..604 -> 75; in 611..616 -> 77
        in 700..741 -> 45; in 200..299 -> 95; else -> 0
    }

    private fun wapiToWmo(code: Int) = when (code) {
        1000 -> 0; 1003 -> 1; 1006 -> 2; 1009 -> 3
        1030, 1135, 1147 -> 45; 1063, 1150, 1153, 1180, 1183 -> 61
        1186, 1189 -> 63; 1192, 1195 -> 65; 1066, 1210, 1213 -> 71
        1219 -> 73; 1114, 1216, 1222, 1225 -> 75; 1087, 1273, 1276 -> 95; else -> 0
    }

    companion object {
        private val ICON_PACKS = listOf(
            // Pack 0: Emoji Renkli
            mapOf(0 to "☀️", 1 to "🌤", 2 to "⛅", 3 to "☁️", 45 to "🌫", 51 to "🌦",
                  61 to "🌧", 63 to "🌧", 65 to "🌧", 71 to "🌨", 75 to "❄", 95 to "⛈"),
            // Pack 1: Minimal Sembol
            mapOf(0 to "○", 1 to "◑", 2 to "◑", 3 to "●", 45 to "≋", 51 to "∿",
                  61 to "∿", 63 to "≈", 65 to "≋", 71 to "*", 75 to "**", 95 to "↯"),
            // Pack 2: ASCII
            mapOf(0 to "^o^", 1 to "~o~", 2 to "~O~", 3 to "~~~", 45 to "...", 51 to ":|",
                  61 to ":,", 63 to ":,,", 65 to ":,,,", 71 to ":**", 75 to ":***", 95 to ":/\\"),
            // Pack 3: Unicode Şekil
            mapOf(0 to "✦", 1 to "✧", 2 to "◈", 3 to "▣", 45 to "≀", 51 to "⌇",
                  61 to "⌇⌇", 63 to "⌇⌇", 65 to "⌇⌇⌇", 71 to "❅", 75 to "❄", 95 to "⚡")
        )

        private val DESCRIPTIONS = mapOf(
            0 to "Açık", 1 to "Az Bulutlu", 2 to "Parçalı Bulutlu", 3 to "Bulutlu",
            45 to "Sisli", 51 to "Hafif Çisenti", 53 to "Çisenti", 55 to "Yoğun Çisenti",
            61 to "Hafif Yağmur", 63 to "Orta Yağmur", 65 to "Şiddetli Yağmur",
            71 to "Hafif Kar", 73 to "Orta Kar", 75 to "Yoğun Kar", 77 to "Kar Taneleri",
            80 to "Sağanak", 95 to "Gök Gürültülü Fırtına", 99 to "Dolu ile Fırtına"
        )

        fun getWeatherInfo(code: Int, iconPackIndex: Int): Pair<String, String> {
            val pack = ICON_PACKS.getOrElse(iconPackIndex) { ICON_PACKS[0] }
            val icon = pack[code] ?: pack.entries.minByOrNull { Math.abs(it.key - code) }?.value ?: "?"
            val desc = DESCRIPTIONS[code] ?: "Bilinmiyor"
            return Pair(desc, icon)
        }
    }
}
