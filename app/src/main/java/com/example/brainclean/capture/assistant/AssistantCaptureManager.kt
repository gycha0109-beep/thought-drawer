package com.example.brainclean.capture.assistant

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.os.Build

class AssistantCaptureManager(context: Context) {
    private val appContext = context.applicationContext

    fun isSupported(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return false
        return roleManager()?.isRoleAvailable(RoleManager.ROLE_ASSISTANT) == true
    }

    fun isEnabled(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
        return roleManager()?.isRoleHeld(RoleManager.ROLE_ASSISTANT) == true
    }

    fun createRoleRequestIntent(): Intent? {
        if (!isSupported()) return null
        return roleManager()?.createRequestRoleIntent(RoleManager.ROLE_ASSISTANT)
    }

    private fun roleManager(): RoleManager? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        return appContext.getSystemService(RoleManager::class.java)
    }
}
