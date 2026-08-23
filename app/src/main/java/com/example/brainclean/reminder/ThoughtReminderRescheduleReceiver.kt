package com.example.brainclean.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.brainclean.data.BrainCleanDatabase
import com.example.brainclean.data.ThoughtRepository
import com.example.brainclean.domain.ThoughtCommandService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ThoughtReminderRescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

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
                commandService.syncExplicitReminders()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
