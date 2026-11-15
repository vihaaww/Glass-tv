package com.glasstv.launcher

import android.content.Context

class Opt(val key: String, val title: String, val values: List<String>, val def: Int)
class City(val name: String, val lat: Double, val lon: Double)

object Prefs {
    /** All time, date and sunrise/sunset logic is pinned to India Standard Time. */
    const val TZ = "Asia/Kolkata"

    val cities = listOf(
        City("Ghaziabad (Govindpuram)", 28.6700, 77.4350),
        City("New Delhi", 28.6139, 77.2090),
        City("Noida", 28.5355, 77.3910),
        City("Kolkata", 22.5726, 88.3639),
        City("Mumbai", 19.0760, 72.8777),
        City("Bengaluru", 12.9716, 77.5946),
        City("Chennai", 13.0827, 80.2707),
        City("Hyderabad", 17.3850, 78.4867),
        City("London", 51.5072, -0.1276),
        City("New York", 40.7128, -74.0060),
        City("Dubai", 25.2048, 55.2708)
    )

    val options = listOf(
        Opt("wallpaper", "Wallpaper", listOf("Auto-rotate", "Aurora", "Dune", "Horizon", "Mist", "Ripple"), 0),
        Opt("theme", "Theme", listOf("Auto (sunrise / sunset)", "Always light", "Always dark"), 0),
        Opt("font", "Clock font", listOf("Light", "Thin", "Regular", "Medium", "Bold", "Condensed", "Serif", "Custom file"), 0),
        Opt("size", "Clock size", listOf("Small", "Medium", "Large", "Extra large"), 2),
        Opt("h24", "Time format", listOf("12-hour", "24-hour"), 0),
        Opt("unit", "Temperature", listOf("Celsius", "Fahrenheit"), 0),
        Opt("city", "Weather location", cities.map { it.name }, 0),
        Opt("bubbles", "Bubble animation", listOf("On", "Off"), 0),
        Opt("saver", "Screensaver after", listOf("1 minute", "2 minutes", "5 minutes", "10 minutes", "Off"), 1)
    )

    private fun sp(c: Context) = c.getSharedPreferences("glasstv", Context.MODE_PRIVATE)

    fun get(c: Context, key: String): Int =
        sp(c).getInt(key, options.first { it.key == key }.def)

    fun set(c: Context, key: String, v: Int) {
        sp(c).edit().putInt(key, v).apply()
    }

    fun sunrise(c: Context): Float = sp(c).getInt("sunrise", 6 * 60 + 15).toFloat()
    fun sunset(c: Context): Float = sp(c).getInt("sunset", 17 * 60 + 30).toFloat()

    fun setSun(c: Context, sr: Int, ss: Int) {
        sp(c).edit().putInt("sunrise", sr).putInt("sunset", ss).apply()
    }

    fun pins(c: Context): List<String>? {
        val s = sp(c).getString("pins", null) ?: return null
        return if (s.isEmpty()) emptyList() else s.split(",")
    }

    fun setPins(c: Context, list: List<String>) {
        sp(c).edit().putString("pins", list.joinToString(",")).apply()
    }
}
