package com.glasstv.launcher

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.Typeface
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

/**
 * Cold-start splash: pure black, then a black-and-white starburst mark and "Claude" wordmark slowly brighten
 * up from nothing (like a MacBook powering on), hold briefly, then the whole overlay fades into the home screen.
 *
 * The mark is a simple 12-ray burst drawn in code. To use an official asset, replace onDraw with a drawable.
 */
class BootLogoView(ctx: Context) : View(ctx) {
    private var level = 0f
    private val ray = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val glow = Paint(Paint.ANTI_ALIAS_FLAG)
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
        textAlign = Paint.Align.CENTER
        letterSpacing = 0.05f
    }
    private val lens = floatArrayOf(1f, .72f, .92f, .60f, 1f, .78f, .95f, .64f, .88f, .70f, 1f, .80f)

    init {
        isClickable = true
        isFocusable = true
    }

    fun play(onEnd: () -> Unit) {
        val up = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 2800
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener {
                level = it.animatedValue as Float
                invalidate()
            }
        }
        val out = ObjectAnimator.ofFloat(this, ALPHA, 1f, 0f).apply {
            duration = 900
            startDelay = 800
        }
        AnimatorSet().apply {
            playSequentially(up, out)
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    onEnd()
                }
            })
            start()
        }
    }

    override fun onDraw(c: Canvas) {
        c.drawColor(Color.BLACK)
        val b = level.pow(2.2f) // gamma curve: slow, soft turn-on
        val cx = width / 2f
        val cy = height / 2f - context.dp(26f)
        val unit = context.dp(1f)

        glow.shader = RadialGradient(cx, cy, 240f * unit, white((46 * b).toInt()), white(0), Shader.TileMode.CLAMP)
        c.drawCircle(cx, cy, 240f * unit, glow)

        ray.strokeWidth = 11f * unit
        ray.color = white((255 * b).toInt())
        val inner = 12f * unit
        for (i in 0 until 12) {
            val a = Math.toRadians(i * 30.0 - 90.0)
            val outer = inner + lens[i] * 56f * unit
            c.drawLine(
                cx + cos(a).toFloat() * inner, cy + sin(a).toFloat() * inner,
                cx + cos(a).toFloat() * outer, cy + sin(a).toFloat() * outer, ray
            )
        }

        text.textSize = 40f * resources.displayMetrics.scaledDensity
        text.color = white((255 * b).toInt())
        c.drawText("Claude", cx, cy + 118f * unit, text)
    }
}
