package com.glasstv.launcher

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.view.Choreographer
import android.view.View
import java.util.Random
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/** Full-screen wallpaper (light/dark blended) with floating glass bubbles. Runs at ~30 fps for weak TV sticks. */
class SceneView(ctx: Context) : View(ctx), Choreographer.FrameCallback {

    var onThemeChanged: (() -> Unit)? = null
    var bubblesEnabled = true

    private class Bubble(
        var x: Float, var y: Float, var r: Float, var vy: Float, var phase: Float,
        var sway: Float, var alpha: Float, var life: Float, val maxLife: Float, val grow: Float
    )

    private val bubbles = ArrayList<Bubble>()
    private val rnd = Random()
    private var bg: Bitmap? = null
    private var prev: Bitmap? = null
    private var fade = 1f
    private var rIndex = -1
    private var rNight = -1f
    private var lastFrame = 0L
    private var lastCheck = 0L
    private var dirty = true
    private var running = false
    private val paint = Paint(Paint.FILTER_BITMAP_FLAG)
    private val dst = Rect()
    private val src = Rect(0, 0, BUBBLE_PX, BUBBLE_PX)
    private val dstF = RectF()

    companion object {
        private const val BUBBLE_PX = 192
        private var bubbleLight: Bitmap? = null
        private var bubbleDark: Bitmap? = null

        @Synchronized
        private fun bubble(dark: Boolean): Bitmap {
            val cached = if (dark) bubbleDark else bubbleLight
            if (cached != null) return cached
            val s = BUBBLE_PX
            val bmp = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888)
            val c = Canvas(bmp)
            val p = Paint(Paint.ANTI_ALIAS_FLAG)
            val cx = s / 2f
            val r = s / 2f - 4f
            p.shader = RadialGradient(
                cx, cx, r,
                intArrayOf(white(0), white(0x14), white(if (dark) 0x55 else 0x99)),
                floatArrayOf(0f, 0.72f, 1f), Shader.TileMode.CLAMP
            )
            c.drawCircle(cx, cx, r, p)
            p.shader = null
            p.style = Paint.Style.STROKE
            p.strokeWidth = 2.5f
            p.color = if (dark) white(0x66) else black(0x2A)
            c.drawCircle(cx, cx, r, p)
            p.strokeWidth = 2f
            p.color = white(if (dark) 0x40 else 0xB0)
            c.drawCircle(cx, cx, r - 3f, p)
            p.style = Paint.Style.FILL
            p.shader = RadialGradient(cx * 0.68f, cx * 0.55f, r * 0.32f, white(if (dark) 0xB0 else 0xE8), white(0), Shader.TileMode.CLAMP)
            c.drawCircle(cx * 0.68f, cx * 0.55f, r * 0.32f, p)
            p.shader = RadialGradient(cx * 1.35f, cx * 1.45f, r * 0.24f, white(0x44), white(0), Shader.TileMode.CLAMP)
            c.drawCircle(cx * 1.35f, cx * 1.45f, r * 0.24f, p)
            p.shader = null
            if (dark) bubbleDark = bmp else bubbleLight = bmp
            return bmp
        }
    }

    private fun newBubble(initial: Boolean): Bubble {
        val big = rnd.nextFloat()
        val r = context.dp(10f) + big * big * context.dp(62f)
        return Bubble(
            x = rnd.nextFloat() * width,
            y = if (initial) rnd.nextFloat() * height else height + r,
            r = r,
            vy = context.dp(6f) + r * 0.28f,
            phase = rnd.nextFloat() * 6.28f,
            sway = context.dp(8f) + rnd.nextFloat() * context.dp(22f),
            alpha = 0.35f + rnd.nextFloat() * 0.5f,
            life = -1f, maxLife = 1f, grow = 0f
        )
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w > 0 && h > 0) {
            bubbles.clear()
            repeat(14) { bubbles.add(newBubble(true)) }
            rIndex = -1
            dirty = true
        }
    }

    /** Spawns a short burst of small bubbles (used when focus moves). */
    fun burst(cx: Float, cy: Float) {
        if (!bubblesEnabled) return
        repeat(7) {
            val r = context.dp(5f) + rnd.nextFloat() * context.dp(12f)
            bubbles.add(
                Bubble(
                    cx + (rnd.nextFloat() - 0.5f) * context.dp(120f), cy + (rnd.nextFloat() - 0.5f) * context.dp(30f),
                    r, context.dp(50f) + rnd.nextFloat() * context.dp(90f), rnd.nextFloat() * 6.28f,
                    context.dp(6f), 0.9f, 1.5f, 1.5f, context.dp(10f)
                )
            )
        }
        while (bubbles.size > 60) bubbles.removeAt(14.coerceAtMost(bubbles.size - 1))
    }

    /** Forces the wallpaper to be re-evaluated on the next tick (after a settings change). */
    fun reset() {
        rIndex = -1
        rNight = -1f
        lastCheck = 0L
    }

    private fun ensureBg() {
        if (width == 0 || height == 0) return
        val idx = Wallpapers.currentIndex(context)
        val n = ThemeState.night
        if (bg != null && idx == rIndex && abs(n - rNight) < 0.01f) return
        val rw = min(960, width)
        val rh = max(1, rw * height / width)
        val nb = Wallpapers.render(rw, rh, idx, n)
        if (bg != null && (idx != rIndex || abs(n - rNight) > 0.25f)) {
            prev = bg
            fade = 0f
        }
        bg = nb
        rIndex = idx
        rNight = n
        dirty = true
    }

    private fun step(dt: Float) {
        val it = bubbles.iterator()
        while (it.hasNext()) {
            val b = it.next()
            b.y -= b.vy * dt
            b.phase += dt * 0.6f
            if (b.life >= 0f) {
                b.life -= dt
                b.r += b.grow * dt
                if (b.life <= 0f) it.remove()
            } else if (b.y + b.r < 0f) {
                b.y = height + b.r
                b.x = rnd.nextFloat() * width
            }
        }
    }

    fun start() {
        if (running) return
        running = true
        lastFrame = 0L
        Choreographer.getInstance().postFrameCallback(this)
    }

    fun stop() {
        running = false
        Choreographer.getInstance().removeFrameCallback(this)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        start()
    }

    override fun onDetachedFromWindow() {
        stop()
        super.onDetachedFromWindow()
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        if (visibility == VISIBLE) start() else stop()
    }

    override fun doFrame(frameTimeNanos: Long) {
        if (!running) return
        val now = frameTimeNanos / 1_000_000L
        if (lastFrame == 0L) lastFrame = now
        if (now - lastFrame >= 32L) {
            val dt = min((now - lastFrame) / 1000f, 0.1f)
            lastFrame = now
            if (now - lastCheck > 1500L) {
                lastCheck = now
                if (ThemeState.refresh(context)) onThemeChanged?.invoke()
                ensureBg()
            }
            if (fade < 1f) {
                fade = min(1f, fade + dt / 1.4f)
                if (fade >= 1f) prev = null
                dirty = true
            }
            if (bubblesEnabled) step(dt)
            if (bubblesEnabled || dirty) {
                dirty = false
                invalidate()
            }
        }
        Choreographer.getInstance().postFrameCallback(this)
    }

    override fun onDraw(canvas: Canvas) {
        if (bg == null) ensureBg()
        val b = bg ?: run { canvas.drawColor(Color.BLACK); return }
        dst.set(0, 0, width, height)
        val pv = prev
        if (pv != null && fade < 1f) {
            paint.alpha = 255
            canvas.drawBitmap(pv, null, dst, paint)
        }
        paint.alpha = (fade * 255).toInt()
        canvas.drawBitmap(b, null, dst, paint)
        if (!bubblesEnabled) return

        val n = ThemeState.night
        val lightBmp = bubble(false)
        val darkBmp = bubble(true)
        for (bb in bubbles) {
            val a = bb.alpha * (if (bb.life >= 0f) (bb.life / bb.maxLife).coerceIn(0f, 1f) else 1f)
            val x = bb.x + kotlin.math.sin(bb.phase) * bb.sway
            dstF.set(x - bb.r, bb.y - bb.r, x + bb.r, bb.y + bb.r)
            val aLight = (a * 255 * (1f - n)).toInt()
            val aDark = (a * 255 * n).toInt()
            if (aLight > 2) {
                paint.alpha = aLight
                canvas.drawBitmap(lightBmp, src, dstF, paint)
            }
            if (aDark > 2) {
                paint.alpha = aDark
                canvas.drawBitmap(darkBmp, src, dstF, paint)
            }
        }
        paint.alpha = 255
    }
}
