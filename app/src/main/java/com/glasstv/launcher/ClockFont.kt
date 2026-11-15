package com.glasstv.launcher

import android.content.Context
import android.graphics.Typeface
import android.os.Build
import android.widget.TextClock
import java.io.File

object ClockFont {
    private fun w(weight: Int, family: String): Typeface =
        if (Build.VERSION.SDK_INT >= 28) {
            Typeface.create(Typeface.SANS_SERIF, weight, false)
        } else {
            Typeface.create(family, if (weight >= 700) Typeface.BOLD else Typeface.NORMAL)
        }

    private fun custom(ctx: Context): Typeface? = try {
        val f = File(ctx.getExternalFilesDir(null), "clock_font.ttf")
        if (f.exists()) Typeface.createFromFile(f) else null
    } catch (e: Exception) {
        null
    }

    fun typeface(ctx: Context): Typeface = when (Prefs.get(ctx, "font")) {
        0 -> w(300, "sans-serif-light")
        1 -> w(100, "sans-serif-thin")
        2 -> w(400, "sans-serif")
        3 -> w(500, "sans-serif-medium")
        4 -> w(700, "sans-serif")
        5 -> Typeface.create("sans-serif-condensed", Typeface.NORMAL)
        6 -> Typeface.create("serif", Typeface.NORMAL)
        else -> custom(ctx) ?: w(300, "sans-serif-light")
    }

    fun scale(ctx: Context): Float = floatArrayOf(0.7f, 0.85f, 1f, 1.2f)[Prefs.get(ctx, "size")]

    /** Applies 12/24h format and pins the clock to IST. */
    fun styleClock(ctx: Context, c: TextClock) {
        val f = if (Prefs.get(ctx, "h24") == 1) "HH:mm" else "h:mm"
        c.format12Hour = f
        c.format24Hour = f
        c.timeZone = Prefs.TZ
    }

    fun styleDate(c: TextClock) {
        c.format12Hour = "EEEE, d MMMM"
        c.format24Hour = "EEEE, d MMMM"
        c.timeZone = Prefs.TZ
    }
}
