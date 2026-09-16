package com.example.brainclean.capture.assistant

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings

class AssistantCaptureManager(context: Context) {
    private val appContext = context.applicationContext

    fun isSupported(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return false
        if (!Build.MANUFACTURER.equals("samsung", ignoreCase = true)) return false
        return roleManager()?.isRoleAvailable(RoleManager.ROLE_ASSISTANT) == true &&
            createAssistantSettingsIntent() != null
    }

    fun isEnabled(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
        return roleManager()
            ?.getRoleHolders(RoleManager.ROLE_ASSISTANT)
            ?.contains(appContext.packageName) == true
    }

    fun createAssistantSettingsIntent(): Intent? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null

        val candidates = listOf(
            Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS),
            Intent(Settings.ACTION_VOICE_INPUT_SETTINGS),
            Intent(Settings.ACTION_SETTINGS)
        )

        return candidates.firstOrNull { intent ->
            intent.resolveActivity(appContext.packageManager) != null
        }
    }

    private fun roleManager(): RoleManager? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        return appContext.getSystemService(RoleManager::class.java)
    }
}
