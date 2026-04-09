package com.example.brainclean.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.brainclean.data.BrainCleanDatabase
import com.example.brainclean.model.ThoughtStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ThoughtReminderRescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val thoughtDao = BrainCleanDatabase.getDatabase(context).thoughtDao()
                val scheduler = ThoughtReminderScheduler(context)
                thoughtDao.getAllThoughts().forEach { thoughtEntity ->
                    scheduler.syncReminder(thoughtEntity.toThought())
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
