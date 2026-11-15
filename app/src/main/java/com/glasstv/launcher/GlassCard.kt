package com.glasstv.launcher

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.LinearGradient
import android.graphics.Outline
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.Drawable
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewOutlineProvider
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.min

/** A frosted "liquid glass" card: translucent gradient fill, specular top band, bright rim and a sheen sweep on focus. */
open class GlassCard(ctx: Context, cornerDp: Float = 24f) : LinearLayout(ctx) {
    var onFocused: ((GlassCard) -> Unit)? = null
    var icon: ImageView? = null
    var label: TextView? = null

    private val corner = ctx.dp(cornerDp)
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val spec = Paint(Paint.ANTI_ALIAS_FLAG)
    private val sheenPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rim = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val hair = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val rect = RectF()
    private val rimRect = RectF()
    private val clip = Path()
    private var focusAmt = 0f
    private var sheen = 0f
    private var focusAnim: ValueAnimator? = null
    private var sheenAnim: ValueAnimator? = null

    init {
        setWillNotDraw(false)
        isFocusable = true
        isClickable = true
        isLongClickable = true
        clipChildren = false
        clipToPadding = false
    }

    /** Turns this card into an app tile: rounded icon above a one-line label. */
    fun asTile(iconDp: Float): GlassCard {
        orientation = VERTICAL
        gravity = Gravity.CENTER
        setPadding(context.dpi(10f), context.dpi(14f), context.dpi(10f), context.dpi(10f))
        val iv = ImageView(context).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
            clipToOutline = true
            outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    outline.setRoundRect(0, 0, view.width, view.height, view.width * 0.22f)
                }
            }
        }
        val tv = TextView(context).apply {
            tag = "t"
            textSize = 14f
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
            gravity = Gravity.CENTER
            setTextColor(ThemeState.text())
        }
        val size = context.dpi(iconDp)
        addView(iv, LayoutParams(size, size))
        addView(tv, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply { topMargin = context.dpi(10f) })
        icon = iv
        label = tv
        return this
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        rect.set(0f, 0f, w.toFloat(), h.toFloat())
        clip.reset()
        clip.addRoundRect(rect, corner, corner, Path.Direction.CW)
    }

    override fun onFocusChanged(gainFocus: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect)
        focusAnim?.cancel()
        focusAnim = ValueAnimator.ofFloat(focusAmt, if (gainFocus) 1f else 0f).apply {
            duration = 260
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                focusAmt = it.animatedValue as Float
                val s = 1f + 0.09f * focusAmt
                scaleX = s
                scaleY = s
                translationZ = context.dp(12f) * focusAmt
                invalidate()
            }
            start()
        }
        if (gainFocus) {
            sheenAnim?.cancel()
            sheenAnim = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 850
                addUpdateListener {
                    sheen = it.animatedValue as Float
                    invalidate()
                }
                start()
            }
            onFocused?.invoke(this)
        }
    }

    override fun onDraw(c: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return
        val n = ThemeState.night
        val f = focusAmt

        val topA = (lerp(0x90.toFloat(), 0x3C.toFloat(), n) + 0x40 * f).toInt()
        val botA = (lerp(0x58.toFloat(), 0x16.toFloat(), n) + 0x38 * f).toInt()
        fill.shader = LinearGradient(0f, 0f, 0f, h, white(topA), white(botA), Shader.TileMode.CLAMP)
        c.drawRoundRect(rect, corner, corner, fill)

        c.save()
        c.clipPath(clip)
        spec.shader = LinearGradient(
            0f, 0f, 0f, h * 0.5f,
            white(lerp(0x70.toFloat(), 0x38.toFloat(), n).toInt()), white(0), Shader.TileMode.CLAMP
        )
        c.drawRect(0f, 0f, w, h * 0.5f, spec)
        if (sheen > 0f && sheen < 1f) {
            val x = -w * 0.4f + w * 1.8f * sheen
            sheenPaint.shader = LinearGradient(
                x, 0f, x + w * 0.4f, 0f,
                intArrayOf(white(0), white((0x60 * f).toInt()), white(0)),
                floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP
            )
            c.drawRect(0f, 0f, w, h, sheenPaint)
        }
        c.restore()

        hair.strokeWidth = context.dp(1f)
        hair.color = black((0x1A * (1f - n)).toInt())
        c.drawRoundRect(rect, corner, corner, hair)

        val sw = context.dp(1.2f + 1.6f * f)
        rim.strokeWidth = sw
        rim.shader = LinearGradient(
            0f, 0f, w, h,
            white((0xC0 + 0x3F * f).toInt()), white((0x30 + 0x60 * f).toInt()), Shader.TileMode.CLAMP
        )
        rimRect.set(sw / 2f, sw / 2f, w - sw / 2f, h - sw / 2f)
        c.drawRoundRect(rimRect, corner, corner, rim)
    }
}

/** Simple vector glyphs for the dock: 0 = all-apps grid, 1 = settings sliders. Colour follows the theme. */
class GlyphDrawable(private val kind: Int) : Drawable() {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val r = RectF()

    override fun draw(c: Canvas) {
        val b = bounds
        val s = min(b.width(), b.height()).toFloat() * 0.62f
        val x0 = b.exactCenterX() - s / 2f
        val y0 = b.exactCenterY() - s / 2f
        p.color = ThemeState.text()
        if (kind == 0) {
            val cell = s / 3f
            val sq = cell * 0.66f
            p.style = Paint.Style.FILL
            for (i in 0..2) for (j in 0..2) {
                val cx = x0 + cell * (i + 0.5f)
                val cy = y0 + cell * (j + 0.5f)
                r.set(cx - sq / 2f, cy - sq / 2f, cx + sq / 2f, cy + sq / 2f)
                c.drawRoundRect(r, sq * 0.3f, sq * 0.3f, p)
            }
        } else {
            val knobs = floatArrayOf(0.30f, 0.68f, 0.46f)
            for (k in 0..2) {
                val y = y0 + s * (0.16f + 0.34f * k)
                p.style = Paint.Style.STROKE
                p.strokeWidth = s * 0.07f
                p.strokeCap = Paint.Cap.ROUND
                c.drawLine(x0, y, x0 + s, y, p)
                p.style = Paint.Style.FILL
                c.drawCircle(x0 + s * knobs[k], y, s * 0.1f, p)
            }
        }
    }

    override fun setAlpha(alpha: Int) {}
    override fun setColorFilter(colorFilter: ColorFilter?) {}
    @Suppress("OVERRIDE_DEPRECATION")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}
