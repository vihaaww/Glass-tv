package com.glasstv.launcher

import android.app.Activity
import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent

/** In-launcher screensaver; works on Fire TV where system screensavers can't be swapped. Any input closes it. */
class ScreensaverActivity : Activity() {
    private lateinit var view: ScreensaverView

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        view = ScreensaverView(this)
        setContentView(view)
        window.decorView.systemUiVisibility = android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
            android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            android.view.View.SYSTEM_UI_FLAG_FULLSCREEN or
            android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
    }

    override fun onResume() {
        super.onResume()
        view.start()
    }

    override fun onPause() {
        view.stop()
        super.onPause()
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) finish()
        return true
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        finish()
        return true
    }

    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        finish()
        return true
    }
}
