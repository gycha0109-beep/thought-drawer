package com.example.brainclean.capture.assistant

import android.content.Intent
import android.service.voice.VoiceInteractionService

class ThoughtVoiceInteractionService : VoiceInteractionService() {
    override fun onLaunchVoiceAssistFromKeyguard() {
        val intent = Intent(this, AssistantVoiceCaptureActivity::class.java)
            .addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
            )
        startActivity(intent)
    }
}
