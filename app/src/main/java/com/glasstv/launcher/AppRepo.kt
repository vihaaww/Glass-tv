package com.glasstv.launcher

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.widget.Toast

data class AppInfo(val label: String, val pkg: String, val icon: Drawable)

object AppRepo {
    /** Every launchable app: TV (leanback) apps and normal phone-style apps, de-duplicated. */
    fun load(ctx: Context): List<AppInfo> {
        val pm = ctx.packageManager
        val seen = HashSet<String>()
        val out = ArrayList<AppInfo>()
        for (cat in listOf(Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER)) {
            val i = Intent(Intent.ACTION_MAIN).addCategory(cat)
            for (ri in pm.queryIntentActivities(i, 0)) {
                val pkg = ri.activityInfo.packageName
                if (pkg == ctx.packageName || !seen.add(pkg)) continue
                out.add(AppInfo(ri.loadLabel(pm).toString(), pkg, ri.loadIcon(pm)))
            }
        }
        return out.sortedBy { it.label.lowercase() }
    }

    private val popular = listOf(
        "com.netflix.ninja", "com.google.android.youtube.tv", "com.amazon.firetv.youtube",
        "com.amazon.avod", "in.startv.hotstar", "com.disney.disneyplus", "com.spotify.tv.android",
        "com.sonyliv", "com.graymatics.alphaiptv", "com.plexapp.android", "org.xbmc.kodi"
    )

    fun defaultPins(apps: List<AppInfo>): List<String> {
        val have = apps.map { it.pkg }.toSet()
        val pins = popular.filter { it in have }.take(7).toMutableList()
        for (a in apps) {
            if (pins.size >= 6) break
            if (a.pkg !in pins) pins.add(a.pkg)
        }
        return pins
    }

    fun launch(ctx: Context, pkg: String) {
        val pm = ctx.packageManager
        val intent = pm.getLeanbackLaunchIntentForPackage(pkg) ?: pm.getLaunchIntentForPackage(pkg)
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ctx.startActivity(intent)
        } else {
            Toast.makeText(ctx, "Can't open this app", Toast.LENGTH_SHORT).show()
        }
    }

    fun togglePin(ctx: Context, apps: List<AppInfo>, pkg: String): Boolean {
        val cur = (Prefs.pins(ctx) ?: defaultPins(apps)).toMutableList()
        val nowPinned = if (pkg in cur) { cur.remove(pkg); false } else { cur.add(pkg); true }
        Prefs.setPins(ctx, cur)
        return nowPinned
    }
}
