package com.glasstv.launcher

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import java.util.Calendar
import java.util.TimeZone
import kotlin.math.sin

/** Five minimal procedural wallpapers. Each has a light and a dark palette that blend with `night`. */
object Wallpapers {
    private fun pal(vararg v: Long) = IntArray(v.size) { v[it].toInt() }

    private val light = arrayOf(
        pal(0xFFF3F4F8, 0xFFBFD4FF, 0xFFFFD6E8, 0xFFD9F2E6, 0xFFFFFFFF), // Aurora
        pal(0xFFFBF1E6, 0xFFF2D9C0, 0xFFE8C3A3, 0xFFD9AE8D, 0xFFFFE9C7), // Dune
        pal(0xFFDCEBFA, 0xFFFFF6EA, 0xFFFFFFFF, 0xFFC9DDF2, 0xFFA9C6E6), // Horizon
        pal(0xFFEEF1F5, 0xFFD5DEE9, 0xFFFFFFFF, 0xFFC8D6EA, 0xFFFFFFFF), // Mist
        pal(0xFFF5F2EF, 0xFFFFFFFF, 0xFFC9BFB6, 0xFFFFFFFF, 0xFFFFFFFF)  // Ripple
    )
    private val dark = arrayOf(
        pal(0xFF0B0D14, 0xFF2A3C8F, 0xFF6B2C75, 0xFF14635A, 0xFF000000),
        pal(0xFF120E16, 0xFF2A1E2C, 0xFF3A2631, 0xFF4A2F36, 0xFFD8D2F0),
        pal(0xFF05070D, 0xFF1B2340, 0xFFC9D3F2, 0xFF0E1428, 0xFF070A16),
        pal(0xFF0F1318, 0xFF1E2733, 0xFF2E3F55, 0xFF1B3A4A, 0xFF000000),
        pal(0xFF0A0A0C, 0xFF1C1C22, 0xFF4A4A55, 0xFF000000, 0xFF000000)
    )

    fun currentIndex(ctx: Context): Int {
        val p = Prefs.get(ctx, "wallpaper")
        if (p > 0) return p - 1
        val c = Calendar.getInstance(TimeZone.getTimeZone(Prefs.TZ))
        return (c.get(Calendar.DAY_OF_YEAR) + c.get(Calendar.HOUR_OF_DAY) / 6) % 5
    }

    private fun blob(cv: Canvas, paint: Paint, x: Float, y: Float, r: Float, color: Int, a: Int = 0xCC) {
        paint.shader = RadialGradient(x, y, r, withAlpha(color, a), withAlpha(color, 0), Shader.TileMode.CLAMP)
        cv.drawCircle(x, y, r, paint)
        paint.shader = null
    }

    private fun hill(cv: Canvas, paint: Paint, w: Float, h: Float, color: Int, baseY: Float, amp: Float, phase: Float) {
        val path = Path()
        path.moveTo(0f, h)
        path.lineTo(0f, baseY)
        var x = 0f
        while (x <= w) {
            val y = baseY + amp * sin((x / w) * 6.2831853f * 1.1f + phase)
            path.lineTo(x, y)
            x += 6f
        }
        path.lineTo(w, h)
        path.close()
        paint.color = color
        cv.drawPath(path, paint)
    }

    fun render(w: Int, h: Int, index: Int, night: Float): Bitmap {
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val cv = Canvas(bmp)
        val p = IntArray(5) { lerpColor(light[index][it], dark[index][it], night) }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
        val W = w.toFloat()
        val H = h.toFloat()
        when (index) {
            0 -> {
                cv.drawColor(p[0])
                blob(cv, paint, 0.20f * W, 0.30f * H, 0.60f * W, p[1])
                blob(cv, paint, 0.80f * W, 0.25f * H, 0.50f * W, p[2])
                blob(cv, paint, 0.55f * W, 0.85f * H, 0.65f * W, p[3])
            }
            1 -> {
                cv.drawColor(p[0])
                blob(cv, paint, 0.72f * W, 0.28f * H, 0.16f * H, p[4], 0xFF)
                hill(cv, paint, W, H, p[1], 0.62f * H, 0.05f * H, 0.4f)
                hill(cv, paint, W, H, p[2], 0.74f * H, 0.05f * H, 2.1f)
                hill(cv, paint, W, H, p[3], 0.86f * H, 0.04f * H, 4.0f)
            }
            2 -> {
                paint.shader = LinearGradient(0f, 0f, 0f, H, p[0], p[1], Shader.TileMode.CLAMP)
                cv.drawRect(0f, 0f, W, H, paint)
                paint.shader = null
                blob(cv, paint, 0.5f * W, 0.64f * H, 0.35f * H, p[2], 0x90)
                paint.color = p[2]
                cv.drawCircle(0.5f * W, 0.64f * H, 0.07f * H, paint)
                paint.shader = LinearGradient(0f, 0.72f * H, 0f, H, p[3], p[4], Shader.TileMode.CLAMP)
                cv.drawRect(0f, 0.72f * H, W, H, paint)
                paint.shader = null
            }
            3 -> {
                paint.shader = LinearGradient(0f, 0f, W, H, p[0], p[1], Shader.TileMode.CLAMP)
                cv.drawRect(0f, 0f, W, H, paint)
                paint.shader = null
                blob(cv, paint, 0.25f * W, 0.35f * H, 0.45f * W, p[2], 0x70)
                blob(cv, paint, 0.75f * W, 0.70f * H, 0.50f * W, p[3], 0x80)
                blob(cv, paint, 0.60f * W, 0.15f * H, 0.30f * W, p[2], 0x60)
            }
            else -> {
                cv.drawColor(p[0])
                blob(cv, paint, 0.5f * W, 0.5f * H, 0.7f * H, p[1], 0xFF)
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 2f
                for (i in 1..9) {
                    paint.color = withAlpha(p[2], 0x68 - i * 7)
                    cv.drawCircle(0.5f * W, 0.5f * H, i * 0.085f * H, paint)
                }
                paint.style = Paint.Style.FILL
            }
        }
        return bmp
    }
}
