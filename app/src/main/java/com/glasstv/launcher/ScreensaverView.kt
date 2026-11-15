package com.glasstv.launcher

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextClock
import android.widget.TextView
import java.util.Random

/** Minimal screensaver: black, one huge time readout, date and weather beneath. Drifts every minute to avoid burn-in. */
class ScreensaverView(ctx: Context) : FrameLayout(ctx) {
    private val col = LinearLayout(ctx)
    private val time = TextClock(ctx)
    private val date = TextClock(ctx)
    private val info = TextView(ctx)
    private val handler = Handler(Looper.getMainLooper())
    private val rnd = Random()
    private val mover = object : Runnable {
        override fun run() {
            moveSlowly()
            handler.postDelayed(this, 60_000L)
        }
    }

    init {
        setBackgroundColor(Color.BLACK)
        col.orientation = LinearLayout.VERTICAL
        col.gravity = Gravity.CENTER_HORIZONTAL
        for (t in listOf<TextView>(time, date, info)) {
            t.includeFontPadding = false
            t.gravity = Gravity.CENTER
        }
        time.setTextColor(Color.WHITE)
        date.setTextColor(white(0xB4))
        info.setTextColor(white(0x8C))
        col.addView(time)
        col.addView(date, LinearLayout.LayoutParams(-2, -2).apply { topMargin = context.dpi(6f) })
        col.addView(info, LinearLayout.LayoutParams(-2, -2).apply { topMargin = context.dpi(18f) })
        addView(col, LayoutParams(-2, -2, Gravity.CENTER))
        applyStyle()
    }

    private fun applyStyle() {
        val tf = ClockFont.typeface(context)
        val sc = ClockFont.scale(context)
        ClockFont.styleClock(context, time)
        ClockFont.styleDate(date)
        time.typeface = tf
        date.typeface = tf
        info.typeface = tf
        time.textSize = 240f * sc // deliberately huge
        date.textSize = 32f * sc.coerceAtLeast(0.85f)
        info.textSize = 26f * sc.coerceAtLeast(0.85f)
    }

    private fun dim() = lerp(1f, 0.7f, ThemeState.night)

    private fun updateInfo() {
        val w = WeatherRepo.cached(context)
        info.text = if (w == null) "" else WeatherRepo.line(context, w)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w > 0) {
            val center = lerpColor(0xFF1C1A17.toInt(), 0xFF0E1322.toInt(), ThemeState.night)
            background = GradientDrawable().apply {
                gradientType = GradientDrawable.RADIAL_GRADIENT
                gradientRadius = w * 0.6f
                setGradientCenter(0.5f, 0.5f)
                colors = intArrayOf(center, Color.BLACK)
            }
        }
    }

    fun start() {
        ThemeState.refresh(context)
        applyStyle()
        updateInfo()
        col.alpha = dim()
        WeatherRepo.refresh(context)
        handler.removeCallbacks(mover)
        handler.postDelayed(mover, 60_000L)
    }

    fun stop() {
        handler.removeCallbacks(mover)
    }

    private fun moveSlowly() {
        updateInfo()
        val dx = (rnd.nextFloat() - 0.5f) * width * 0.18f
        val dy = (rnd.nextFloat() - 0.5f) * height * 0.22f
        col.animate().alpha(0f).setDuration(1500).withEndAction {
            col.translationX = dx
            col.translationY = dy
            col.animate().alpha(dim()).setDuration(1500).start()
        }.start()
    }
}
