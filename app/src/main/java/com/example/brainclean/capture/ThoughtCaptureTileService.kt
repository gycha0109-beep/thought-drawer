package com.example.brainclean.capture

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.TileService
import com.example.brainclean.QuickAddActivity
import com.example.brainclean.model.CaptureSource

class ThoughtCaptureTileService : TileService() {
    override fun onClick() {
        super.onClick()

        val launchQuickAdd = Runnable {
            val intent = Intent(this, QuickAddActivity::class.java)
                .putExtra(
                    QuickAddActivity.EXTRA_CAPTURE_SOURCE,
                    CaptureSource.QUICK_SETTINGS.name
                )
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val pendingIntent = PendingIntent.getActivity(
                    this,
                    QUICK_ADD_REQUEST_CODE,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                startActivityAndCollapse(pendingIntent)
            } else {
                startLegacyActivityAndCollapse(intent)
            }
        }

        if (isLocked) {
            unlockAndRun(launchQuickAdd)
        } else {
            launchQuickAdd.run()
        }
    }

    @SuppressLint("StartActivityAndCollapseDeprecated")
    @Suppress("DEPRECATION")
    private fun startLegacyActivityAndCollapse(intent: Intent) {
        startActivityAndCollapse(intent)
    }

    companion object {
        private const val QUICK_ADD_REQUEST_CODE = 42_001
    }
}
