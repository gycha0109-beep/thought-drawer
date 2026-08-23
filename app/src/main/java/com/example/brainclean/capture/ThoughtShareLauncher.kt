package com.example.brainclean.capture

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.example.brainclean.R

object ThoughtShareLauncher {
    fun share(context: Context, text: String) {
        val trimmedText = text.trim()
        if (trimmedText.isBlank()) return

        val sendIntent = Intent(Intent.ACTION_SEND)
            .setType("text/plain")
            .putExtra(Intent.EXTRA_TEXT, trimmedText)
        val chooser = Intent.createChooser(
            sendIntent,
            context.getString(R.string.share_thought_chooser_title)
        ).apply {
            if (context !is Activity) {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }

        context.startActivity(chooser)
    }
}
