package com.example.brainclean.capture

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import androidx.core.content.ContextCompat
import com.example.brainclean.MainActivity
import com.example.brainclean.R

object CaptureNotificationManager {
    const val ACTION_CAPTURE_THOUGHT = "com.example.brainclean.action.CAPTURE_THOUGHT"
    const val KEY_CAPTURE_TEXT = "capture_text"

    private const val CHANNEL_ID = "capture_entry_v1"
    private const val NOTIFICATION_ID = 41_001
    private const val CAPTURE_REQUEST_CODE = 41_002
    private const val CONTENT_REQUEST_CODE = 41_003

    fun ensureVisible(context: Context) {
        val appContext = context.applicationContext
        createChannel(appContext)

        if (!canPostNotifications(appContext)) return

        val remoteInput = RemoteInput.Builder(KEY_CAPTURE_TEXT)
            .setLabel(appContext.getString(R.string.capture_notification_input_label))
            .build()
        val captureIntent = Intent(appContext, NotificationCaptureReceiver::class.java)
            .setAction(ACTION_CAPTURE_THOUGHT)
            .setPackage(appContext.packageName)
        val capturePendingIntent = PendingIntent.getBroadcast(
            appContext,
            CAPTURE_REQUEST_CODE,
            captureIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
        val captureAction = NotificationCompat.Action.Builder(
            android.R.drawable.ic_input_add,
            appContext.getString(R.string.capture_notification_action),
            capturePendingIntent
        )
            .addRemoteInput(remoteInput)
            .setAllowGeneratedReplies(false)
            .build()
        val contentIntent = PendingIntent.getActivity(
            appContext,
            CONTENT_REQUEST_CODE,
            Intent(appContext, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_input_add)
            .setContentTitle(appContext.getString(R.string.capture_notification_title))
            .setContentText(appContext.getString(R.string.capture_notification_text))
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(captureAction)
            .build()

        NotificationManagerCompat.from(appContext).notify(NOTIFICATION_ID, notification)
    }

    fun hasRuntimePermission(context: Context): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
    }

    fun canPostNotifications(context: Context): Boolean {
        return hasRuntimePermission(context) &&
            NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun openNotificationSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.capture_notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = context.getString(R.string.capture_notification_channel_description)
            setSound(null, null)
            enableVibration(false)
        }
        manager.createNotificationChannel(channel)
    }
}
