package com.glasstv.launcher

import android.content.Context
import android.os.Handler
import android.os.Looper
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.TimeZone
import kotlin.math.roundToInt

/** Weather from Open-Meteo (free, no API key). Default location: Ghaziabad (Govindpuram). */
object WeatherRepo {
    data class W(val temp: Float, val hi: Float, val lo: Float, val code: Int, val city: String)

    var onUpdated: (() -> Unit)? = null
    private val main = Handler(Looper.getMainLooper())

    private fun sp(c: Context) = c.getSharedPreferences("weather", Context.MODE_PRIVATE)

    fun cached(ctx: Context): W? {
        val s = sp(ctx)
        if (!s.contains("temp")) return null
        return W(
            s.getFloat("temp", 0f), s.getFloat("hi", 0f), s.getFloat("lo", 0f),
            s.getInt("code", 0), s.getString("city", "") ?: ""
        )
    }

    fun refresh(ctx: Context, force: Boolean = false) {
        val app = ctx.applicationContext
        val s = sp(app)
        val cityIdx = Prefs.get(app, "city")
        val stale = System.currentTimeMillis() - s.getLong("ts", 0L) > 20 * 60 * 1000L
        if (!force && !stale && s.getInt("cityIdx", -1) == cityIdx) return
        Thread {
            try {
                val c = Prefs.cities[cityIdx]
                val url = "https://api.open-meteo.com/v1/forecast?latitude=${c.lat}&longitude=${c.lon}" +
                    "&current=temperature_2m,weather_code" +
                    "&daily=temperature_2m_max,temperature_2m_min,sunrise,sunset&timezone=auto&forecast_days=1"
                val j = JSONObject(http(url))
                val cur = j.getJSONObject("current")
                val d = j.getJSONObject("daily")
                val sr = d.getJSONArray("sunrise").getString(0)
                val ss = d.getJSONArray("sunset").getString(0)

                // Sun times come back in the location's timezone; convert to IST minutes-of-day.
                val locOff = j.optInt("utc_offset_seconds", 19800)
                val istOff = TimeZone.getTimeZone(Prefs.TZ).getOffset(System.currentTimeMillis()) / 1000
                val adj = (istOff - locOff) / 60
                fun mins(t: String) = ((t.substring(11, 13).toInt() * 60 + t.substring(14, 16).toInt() + adj) % 1440 + 1440) % 1440
                Prefs.setSun(app, mins(sr), mins(ss))

                s.edit()
                    .putFloat("temp", cur.getDouble("temperature_2m").toFloat())
                    .putInt("code", cur.getInt("weather_code"))
                    .putFloat("hi", d.getJSONArray("temperature_2m_max").getDouble(0).toFloat())
                    .putFloat("lo", d.getJSONArray("temperature_2m_min").getDouble(0).toFloat())
                    .putString("city", c.name.substringBefore(" ("))
                    .putInt("cityIdx", cityIdx)
                    .putLong("ts", System.currentTimeMillis())
                    .apply()
                main.post { onUpdated?.invoke() }
            } catch (e: Exception) {
                // Offline or API hiccup: keep showing the cached values.
            }
        }.start()
    }

    private fun http(u: String): String {
        val conn = URL(u).openConnection() as HttpURLConnection
        conn.connectTimeout = 8000
        conn.readTimeout = 8000
        try {
            return conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    fun describe(code: Int): String = when (code) {
        0 -> "Clear"
        1 -> "Mostly clear"
        2 -> "Partly cloudy"
        3 -> "Overcast"
        45, 48 -> "Foggy"
        in 51..57 -> "Drizzle"
        in 61..67 -> "Rain"
        in 71..77 -> "Snow"
        in 80..82 -> "Showers"
        85, 86 -> "Snow showers"
        in 95..99 -> "Thunderstorm"
        else -> ""
    }

    private fun temp(ctx: Context, c: Float): Int =
        (if (Prefs.get(ctx, "unit") == 1) c * 9f / 5f + 32f else c).roundToInt()

    fun line(ctx: Context, w: W, withCity: Boolean = true): String {
        val base = "${temp(ctx, w.temp)}°  ${describe(w.code)}"
        val hl = "   H ${temp(ctx, w.hi)}°  L ${temp(ctx, w.lo)}°"
        return base + hl + if (withCity && w.city.isNotEmpty()) "  ·  ${w.city}" else ""
    }
}
