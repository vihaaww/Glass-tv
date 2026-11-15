package com.glasstv.launcher

import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextClock
import android.widget.TextView

/** D-pad friendly settings: focus a row, press Left/Right (or OK) to change the value. */
class SettingsActivity : Activity() {
    private lateinit var root: FrameLayout
    private lateinit var scene: SceneView
    private lateinit var preview: TextClock

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeState.refresh(this)
        root = FrameLayout(this)
        scene = SceneView(this)
        scene.onThemeChanged = { ThemeState.apply(root) }
        scene.bubblesEnabled = Prefs.get(this, "bubbles") == 0
        root.addView(scene, FrameLayout.LayoutParams(-1, -1))

        val col = LinearLayout(this)
        col.orientation = LinearLayout.VERTICAL
        col.clipChildren = false
        col.setPadding(dpi(96f), dpi(36f), dpi(96f), dpi(48f))

        col.addView(TextView(this).apply {
            text = "Settings"
            tag = "t"
            textSize = 32f
            setTextColor(ThemeState.text())
        })
        preview = TextClock(this).apply {
            tag = "t"
            includeFontPadding = false
            setTextColor(ThemeState.text())
        }
        col.addView(preview, LinearLayout.LayoutParams(-2, -2).apply { topMargin = dpi(6f) })

        for (opt in Prefs.options) {
            col.addView(row(opt), LinearLayout.LayoutParams(-1, dpi(60f)).apply { topMargin = dpi(10f) })
        }
        col.addView(TextView(this).apply {
            tag = "s"
            textSize = 14f
            setTextColor(ThemeState.sub())
            text = "Custom font: copy a .ttf named clock_font.ttf into Android/data/com.glasstv.launcher/files/ " +
                "(for example Inter or any SF-style font you own), then pick \"Custom file\". " +
                "Time and weather use India Standard Time."
        }, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dpi(18f) })

        val sv = ScrollView(this).apply {
            isVerticalScrollBarEnabled = false
            clipChildren = false
            clipToPadding = false
            addView(col)
        }
        root.addView(sv, FrameLayout.LayoutParams(-1, -1))
        setContentView(root)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        applyPreview()
        ThemeState.apply(root)
        // Focus the first row.
        col.getChildAt(2).requestFocus()
    }

    private fun applyPreview() {
        ClockFont.styleClock(this, preview)
        preview.typeface = ClockFont.typeface(this)
        preview.textSize = 64f * ClockFont.scale(this)
    }

    private fun onChanged(key: String) {
        when (key) {
            "wallpaper", "theme" -> {
                ThemeState.refresh(this)
                scene.reset()
                ThemeState.apply(root)
            }
            "font", "size", "h24" -> applyPreview()
            "bubbles" -> scene.bubblesEnabled = Prefs.get(this, "bubbles") == 0
            "city" -> WeatherRepo.refresh(this, true)
        }
    }

    private fun row(opt: Opt): GlassCard {
        val card = GlassCard(this, 18f)
        card.orientation = LinearLayout.HORIZONTAL
        card.gravity = Gravity.CENTER_VERTICAL
        card.setPadding(dpi(24f), 0, dpi(24f), 0)
        val title = TextView(this).apply {
            text = opt.title
            tag = "t"
            textSize = 20f
            setTextColor(ThemeState.text())
        }
        val value = TextView(this).apply {
            tag = "s"
            textSize = 20f
            gravity = Gravity.END
            setTextColor(ThemeState.sub())
        }
        card.addView(title, LinearLayout.LayoutParams(0, -2, 1f))
        card.addView(value, LinearLayout.LayoutParams(-2, -2))

        fun refresh() {
            value.text = "‹   ${opt.values[Prefs.get(this, opt.key)]}   ›"
        }

        fun change(d: Int) {
            val n = opt.values.size
            Prefs.set(this, opt.key, (Prefs.get(this, opt.key) + d + n) % n)
            refresh()
            onChanged(opt.key)
        }
        refresh()
        card.setOnClickListener { change(1) }
        card.setOnKeyListener { _, code, ev ->
            if (ev.action != KeyEvent.ACTION_DOWN) {
                false
            } else when (code) {
                KeyEvent.KEYCODE_DPAD_LEFT -> { change(-1); true }
                KeyEvent.KEYCODE_DPAD_RIGHT -> { change(1); true }
                else -> false
            }
        }
        return card
    }
}
