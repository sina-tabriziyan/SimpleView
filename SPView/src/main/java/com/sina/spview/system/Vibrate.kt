package com.sina.spview.system

fun Fragment.vibrate(durationMillis: Long = 500) {
    val vibrator = requireContext().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val effect = VibrationEffect.createOneShot(durationMillis, VibrationEffect.DEFAULT_AMPLITUDE)
        vibrator.vibrate(effect)
    } else {
        @Suppress("DEPRECATION")
        vibrator.vibrate(durationMillis)
    }
}