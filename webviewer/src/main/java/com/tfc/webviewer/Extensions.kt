package com.tfc.webviewer

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator

/**
 * author @fobidlim
 */
fun Context.vibrate() = (getSystemService(Context.VIBRATOR_SERVICE) as Vibrator).run {
    val duration = resources.getInteger(R.integer.vibrator_duration).toLong()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
    } else {
        @Suppress("DEPRECATION")
        vibrate(duration)
    }
}