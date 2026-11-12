package com.hongguo.android

import android.content.Context
import android.content.Intent
import androidx.activity.ComponentActivity

internal fun Context.currentDisplayMode(): AppDisplayMode =
    AppDisplayMode.fromKey(getSharedPreferences(ThemePreferences.FILE, Context.MODE_PRIVATE)
        .getString(ThemePreferences.DISPLAY_MODE, null))

internal fun Context.shouldUseTvInterface(): Boolean = when (currentDisplayMode()) {
    AppDisplayMode.AUTO -> isTelevisionDevice()
    AppDisplayMode.PHONE -> false
    AppDisplayMode.TV -> true
}

internal fun ComponentActivity.switchDisplayMode(mode: AppDisplayMode) {
    getSharedPreferences(ThemePreferences.FILE, Context.MODE_PRIVATE).edit()
        .putString(ThemePreferences.DISPLAY_MODE, mode.key).apply()
    val targetIsTv = shouldUseTvInterface()
    if (targetIsTv == (this is TvActivity)) return
    val target = if (targetIsTv) TvActivity::class.java else MainActivity::class.java
    startActivity(Intent(this, target).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK))
    finish()
}
