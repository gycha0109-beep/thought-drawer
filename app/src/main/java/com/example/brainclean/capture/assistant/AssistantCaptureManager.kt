package com.example.brainclean.capture.assistant

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings

class AssistantCaptureManager(context: Context) {
    private val appContext = context.applicationContext

    fun isSupported(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            Build.MANUFACTURER.equals("samsung", ignoreCase = true) &&
            createSideButtonSettingsIntent() != null
    }

    fun createSideButtonSettingsIntent(): Intent? {
        val intent = Intent(Settings.ACTION_SETTINGS)
        return intent.takeIf {
            it.resolveActivity(appContext.packageManager) != null
        }
    }
}
