package com.glasstv.launcher

import android.service.dreams.DreamService

/** System screensaver (Daydream) version of the same minimal clock, for Android TV / Google TV. */
class ClockDreamService : DreamService() {
    private var view: ScreensaverView? = null

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        isInteractive = false
        isFullscreen = true
        val v = ScreensaverView(this)
        view = v
        setContentView(v)
    }

    override fun onDreamingStarted() {
        super.onDreamingStarted()
        view?.start()
    }

    override fun onDreamingStopped() {
        view?.stop()
        super.onDreamingStopped()
    }
}
