package com.glasstv.launcher

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextClock
import android.widget.TextView
import android.widget.Toast

class HomeActivity : Activity() {
    companion object {
        var booted = false
    }

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var root: FrameLayout
    private lateinit var scene: SceneView
    private lateinit var timeView: TextClock
    private lateinit var dateView: TextClock
    private lateinit var weatherView: TextView
    private lateinit var dockScroll: HorizontalScrollView
    private lateinit var dock: LinearLayout
    private var apps: List<AppInfo> = emptyList()
    private var appsKey = ""
    private var lastInput = SystemClock.uptimeMillis()
    private var boot: BootLogoView? = null

    private val idleCheck = object : Runnable {
        override fun run() {
            val mins = intArrayOf(1, 2, 5, 10, -1)[Prefs.get(this@HomeActivity, "saver")]
            if (mins > 0 && boot == null && SystemClock.uptimeMillis() - lastInput > mins * 60_000L) {
                lastInput = SystemClock.uptimeMillis()
                startActivity(Intent(this@HomeActivity, ScreensaverActivity::class.java))
            }
            handler.postDelayed(this, 5000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeState.refresh(this)
        buildUi()
        setContentView(root)
        immersive()
        if (!booted) {
            booted = true
            val b = BootLogoView(this)
            boot = b
            root.addView(b, FrameLayout.LayoutParams(-1, -1))
            b.requestFocus()
            b.play {
                root.removeView(b)
                boot = null
                focusDock()
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun immersive() {
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
    }

    private fun buildUi() {
        root = FrameLayout(this)
        root.clipChildren = false
        scene = SceneView(this)
        scene.onThemeChanged = { ThemeState.apply(root) }
        root.addView(scene, FrameLayout.LayoutParams(-1, -1))

        val info = LinearLayout(this)
        info.orientation = LinearLayout.VERTICAL
        timeView = TextClock(this).apply { tag = "t"; includeFontPadding = false }
        dateView = TextClock(this).apply { tag = "s"; textSize = 24f; includeFontPadding = false }
        weatherView = TextView(this).apply { tag = "s"; textSize = 26f; includeFontPadding = false }
        info.addView(timeView)
        info.addView(dateView, LinearLayout.LayoutParams(-2, -2).apply { topMargin = dpi(6f) })
        info.addView(weatherView, LinearLayout.LayoutParams(-2, -2).apply { topMargin = dpi(14f) })
        root.addView(
            info,
            FrameLayout.LayoutParams(-2, -2, Gravity.TOP or Gravity.START).apply {
                setMargins(dpi(72f), dpi(40f), 0, 0)
            }
        )

        dock = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            clipChildren = false
            clipToPadding = false
            setPadding(dpi(72f), dpi(24f), dpi(72f), dpi(24f))
        }
        dockScroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            clipChildren = false
            clipToPadding = false
            addView(dock)
        }
        root.addView(
            dockScroll,
            FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM).apply { bottomMargin = dpi(28f) }
        )
        ThemeState.apply(root)
    }

    private fun applyStyle() {
        val tf = ClockFont.typeface(this)
        val sc = ClockFont.scale(this)
        ClockFont.styleClock(this, timeView)
        ClockFont.styleDate(dateView)
        timeView.typeface = tf
        dateView.typeface = tf
        weatherView.typeface = tf
        timeView.textSize = 112f * sc
        scene.bubblesEnabled = Prefs.get(this, "bubbles") == 0
    }

    private fun showWeather() {
        val w = WeatherRepo.cached(this)
        weatherView.text = if (w == null) "" else WeatherRepo.line(this, w)
    }

    private fun reloadApps() {
        Thread {
            val list = AppRepo.load(this)
            val key = list.joinToString("|") { it.pkg } + "#" + (Prefs.pins(this)?.joinToString(",") ?: "-")
            runOnUiThread {
                if (key != appsKey) {
                    apps = list
                    appsKey = key
                    buildDock()
                }
            }
        }.start()
    }

    private fun tile(label: String, glyph: Int, app: AppInfo?, click: () -> Unit): GlassCard {
        val card = GlassCard(this).asTile(64f)
        card.label?.text = label
        if (app != null) {
            card.icon?.setImageDrawable(app.icon)
        } else {
            card.icon?.setImageDrawable(GlyphDrawable(glyph))
            card.icon?.clipToOutline = false
        }
        card.setOnClickListener { click() }
        card.onFocused = { c ->
            val loc = IntArray(2)
            c.getLocationInWindow(loc)
            scene.burst(loc[0] + c.width / 2f, loc[1] + c.height / 2f)
        }
        return card
    }

    private fun buildDock() {
        dock.removeAllViews()
        val pins = Prefs.pins(this) ?: AppRepo.defaultPins(apps)
        val byPkg = apps.associateBy { it.pkg }
        val lp = { LinearLayout.LayoutParams(dpi(136f), dpi(150f)).apply { setMargins(dpi(8f), 0, dpi(8f), 0) } }
        for (p in pins) {
            val app = byPkg[p] ?: continue
            val card = tile(app.label, 0, app) { AppRepo.launch(this, app.pkg) }
            card.setOnLongClickListener {
                AppRepo.togglePin(this, apps, app.pkg)
                Toast.makeText(this, "Removed from Home", Toast.LENGTH_SHORT).show()
                appsKey = ""
                reloadApps()
                true
            }
            dock.addView(card, lp())
        }
        dock.addView(tile("All Apps", 0, null) {
            startActivity(Intent(this, DrawerActivity::class.java))
        }, lp())
        dock.addView(tile("Settings", 1, null) {
            startActivity(Intent(this, SettingsActivity::class.java))
        }, lp())
        ThemeState.apply(dock)
        if (boot == null) focusDock()
    }

    private fun focusDock() {
        if (dock.childCount > 0) dock.getChildAt(0).requestFocus()
    }

    override fun onResume() {
        super.onResume()
        immersive()
        lastInput = SystemClock.uptimeMillis()
        ThemeState.refresh(this)
        applyStyle()
        scene.reset()
        ThemeState.apply(root)
        showWeather()
        WeatherRepo.onUpdated = { showWeather() }
        WeatherRepo.refresh(this)
        reloadApps()
        handler.postDelayed(idleCheck, 5000)
    }

    override fun onPause() {
        handler.removeCallbacks(idleCheck)
        WeatherRepo.onUpdated = null
        super.onPause()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        dockScroll.scrollTo(0, 0)
        focusDock()
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        lastInput = SystemClock.uptimeMillis()
        return super.dispatchKeyEvent(event)
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onBackPressed() {
        // A launcher never exits on Back.
    }
}
