package com.glasstv.launcher

import android.content.Context
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import java.util.Calendar
import java.util.TimeZone
import kotlin.math.abs

fun Context.dp(v: Float): Float = v * resources.displayMetrics.density
fun Context.dpi(v: Float): Int = (v * resources.displayMetrics.density + 0.5f).toInt()

fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t

fun lerpColor(a: Int, b: Int, t: Float): Int = Color.argb(
    lerp(Color.alpha(a).toFloat(), Color.alpha(b).toFloat(), t).toInt(),
    lerp(Color.red(a).toFloat(), Color.red(b).toFloat(), t).toInt(),
    lerp(Color.green(a).toFloat(), Color.green(b).toFloat(), t).toInt(),
    lerp(Color.blue(a).toFloat(), Color.blue(b).toFloat(), t).toInt()
)

fun white(a: Int): Int = Color.argb(a.coerceIn(0, 255), 255, 255, 255)
fun black(a: Int): Int = Color.argb(a.coerceIn(0, 255), 0, 0, 0)
fun withAlpha(c: Int, a: Int): Int =
    Color.argb(a.coerceIn(0, 255), Color.red(c), Color.green(c), Color.blue(c))

/** Shared day/night state. night: 0 = full day, 1 = full night. */
object ThemeState {
    @Volatile
    var night = 0f

    fun text(): Int = lerpColor(0xFF1D1D1F.toInt(), 0xFFF5F5F7.toInt(), night)
    fun sub(): Int = lerpColor(0xFF6E6E73.toInt(), 0xFFB0B0B8.toInt(), night)

    /** Returns true if the value changed noticeably. */
    fun refresh(ctx: Context): Boolean {
        val n = when (Prefs.get(ctx, "theme")) {
            1 -> 0f
            2 -> 1f
            else -> auto(ctx)
        }
        val changed = abs(n - night) > 0.004f
        night = n
        return changed
    }

    private fun auto(ctx: Context): Float {
        val cal = Calendar.getInstance(TimeZone.getTimeZone(Prefs.TZ))
        val m = cal.get(Calendar.HOUR_OF_DAY) * 60f + cal.get(Calendar.MINUTE) + cal.get(Calendar.SECOND) / 60f
        val sr = Prefs.sunrise(ctx)
        val ss = Prefs.sunset(ctx)
        fun ramp(x: Float, a: Float, b: Float) = ((x - a) / (b - a)).coerceIn(0f, 1f)
        val t = if (m < 720f) 1f - ramp(m, sr - 20f, sr + 20f) else ramp(m, ss - 20f, ss + 20f)
        return t * t * (3f - 2f * t)
    }

    /** Re-colours tagged text ("t" primary, "s" secondary) and redraws glass views. */
    fun apply(v: View) {
        if (v is TextView) {
            when (v.tag) {
                "t" -> v.setTextColor(text())
                "s" -> v.setTextColor(sub())
            }
        }
        if (v is GlassCard || v is ImageView) v.invalidate()
        if (v is ViewGroup) {
            for (i in 0 until v.childCount) apply(v.getChildAt(i))
        }
    }
}
