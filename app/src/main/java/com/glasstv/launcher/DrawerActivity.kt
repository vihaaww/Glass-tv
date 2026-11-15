package com.glasstv.launcher

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

/** Every installed app (TV and phone-style) in one glass grid. Long-press OK to pin/unpin on Home. */
class DrawerActivity : Activity() {
    private lateinit var root: FrameLayout
    private lateinit var scene: SceneView
    private lateinit var grid: RecyclerView
    private lateinit var title: TextView
    private var apps: List<AppInfo> = emptyList()

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
        col.setPadding(dpi(64f), dpi(36f), dpi(64f), 0)
        title = TextView(this).apply {
            text = "All Apps"
            tag = "t"
            textSize = 34f
            typeface = ClockFont.typeface(this@DrawerActivity)
            setTextColor(ThemeState.text())
        }
        grid = RecyclerView(this).apply {
            layoutManager = GridLayoutManager(this@DrawerActivity, 6)
            clipChildren = false
            clipToPadding = false
            itemAnimator = null
            setPadding(0, dpi(18f), 0, dpi(40f))
        }
        col.addView(title)
        col.addView(grid, LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(col, FrameLayout.LayoutParams(-1, -1))
        setContentView(root)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE

        Thread {
            val list = AppRepo.load(this)
            runOnUiThread {
                apps = list
                title.text = "All Apps  ·  ${list.size}"
                grid.adapter = Adapter(list)
                grid.post { grid.findViewHolderForAdapterPosition(0)?.itemView?.requestFocus() }
            }
        }.start()
    }

    private class VH(val frame: FrameLayout, val card: GlassCard) : RecyclerView.ViewHolder(frame)

    private inner class Adapter(val list: List<AppInfo>) : RecyclerView.Adapter<VH>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val frame = FrameLayout(this@DrawerActivity).apply {
                layoutParams = RecyclerView.LayoutParams(-1, dpi(158f))
                setPadding(dpi(8f), dpi(8f), dpi(8f), dpi(8f))
                clipChildren = false
                clipToPadding = false
                descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS
            }
            val card = GlassCard(this@DrawerActivity).asTile(68f)
            card.onFocused = { c ->
                val loc = IntArray(2)
                c.getLocationInWindow(loc)
                scene.burst(loc[0] + c.width / 2f, loc[1] + c.height / 2f)
            }
            frame.addView(card, FrameLayout.LayoutParams(-1, -1))
            return VH(frame, card)
        }

        override fun onBindViewHolder(h: VH, position: Int) {
            val app = list[position]
            h.card.label?.text = app.label
            h.card.label?.setTextColor(ThemeState.text())
            h.card.icon?.setImageDrawable(app.icon)
            h.card.setOnClickListener { AppRepo.launch(this@DrawerActivity, app.pkg) }
            h.card.setOnLongClickListener {
                val pinned = AppRepo.togglePin(this@DrawerActivity, apps, app.pkg)
                Toast.makeText(
                    this@DrawerActivity,
                    if (pinned) "Pinned to Home" else "Removed from Home",
                    Toast.LENGTH_SHORT
                ).show()
                true
            }
        }

        override fun getItemCount() = list.size
    }
}
