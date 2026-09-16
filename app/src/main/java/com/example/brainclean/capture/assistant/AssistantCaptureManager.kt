package com.example.brainclean.capture.assistant

import android.app.role.RoleManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.service.voice.VoiceInteractionService

class AssistantCaptureManager(context: Context) {
    private val appContext = context.applicationContext

    fun isSupported(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return false
        if (roleManager()?.isRoleAvailable(RoleManager.ROLE_ASSISTANT) != true) return false
        return createAssistantSettingsIntent() != null
    }

    fun isEnabled(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return false
        val service = ComponentName(
            appContext,
            ThoughtVoiceInteractionService::class.java
        )
        return VoiceInteractionService.isActiveService(appContext, service)
    }

    fun createAssistantSettingsIntent(): Intent? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null

        val candidateIntents = listOf(
            Intent(Settings.ACTION_VOICE_INPUT_SETTINGS),
            Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS),
            Intent(Settings.ACTION_SETTINGS)
        )

        return candidateIntents.firstOrNull { intent ->
            intent.resolveActivity(appContext.packageManager) != null
        }
    }

    private fun roleManager(): RoleManager? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        return appContext.getSystemService(RoleManager::class.java)
    }
}
