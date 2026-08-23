package com.example.brainclean.capture

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.RemoteInput
import com.example.brainclean.data.BrainCleanDatabase
import com.example.brainclean.data.ThoughtRepository
import com.example.brainclean.domain.CaptureResult
import com.example.brainclean.domain.ThoughtCommandService
import com.example.brainclean.model.CaptureSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationCaptureReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != CaptureNotificationManager.ACTION_CAPTURE_THOUGHT) return

        val captureText = RemoteInput.getResultsFromIntent(intent)
            ?.getCharSequence(CaptureNotificationManager.KEY_CAPTURE_TEXT)
            ?.toString()
            .orEmpty()

        if (captureText.isBlank()) {
            CaptureNotificationManager.ensureVisible(context)
            return
        }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repository = ThoughtRepository(
                    BrainCleanDatabase.getDatabase(context).thoughtDao()
                )
                val commandService = ThoughtCommandService(
                    context = context.applicationContext,
                    repository = repository
                )

                when (
                    commandService.captureThought(
                        content = captureText,
                        source = CaptureSource.NOTIFICATION
                    )
                ) {
                    CaptureResult.Blank -> Unit
                    is CaptureResult.Success -> Unit
                }
            } finally {
                CaptureNotificationManager.ensureVisible(context)
                pendingResult.finish()
            }
        }
    }
}
