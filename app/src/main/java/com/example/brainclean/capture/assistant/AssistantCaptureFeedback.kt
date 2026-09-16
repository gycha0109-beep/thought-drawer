package com.example.brainclean.capture.assistant

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.Toast
import com.example.brainclean.R

object AssistantCaptureFeedback {
    fun saved(context: Context) {
        Toast.makeText(
            context,
            context.getString(R.string.assistant_capture_saved),
            Toast.LENGTH_SHORT
        ).show()
        vibrate(context)
    }

    @Suppress("DEPRECATION")
    private fun vibrate(context: Context) {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                val effect = VibrationEffect.createOneShot(
                    35L,
                    VibrationEffect.DEFAULT_AMPLITUDE
                )
                context.getSystemService(VibratorManager::class.java)
                    ?.defaultVibrator
                    ?.vibrate(effect)
            }

            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                val effect = VibrationEffect.createOneShot(
                    35L,
                    VibrationEffect.DEFAULT_AMPLITUDE
                )
                context.getSystemService(Vibrator::class.java)?.vibrate(effect)
            }

            else -> {
                context.getSystemService(Vibrator::class.java)?.vibrate(35L)
            }
        }
    }
}
