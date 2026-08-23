package com.example.brainclean.reminder

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.brainclean.MainActivity
import com.example.brainclean.R

class ThoughtReminderNotifier(
    context: Context
) {
    private val appContext = context.applicationContext

    fun showExplicitReminder(thoughtId: Long, content: String) {
        showNotification(
            notificationId = thoughtId,
            channelId = EXPLICIT_CHANNEL_ID,
            title = appContext.getString(R.string.explicit_reminder_title),
            thoughtContent = content,
            priority = NotificationCompat.PRIORITY_HIGH,
            defaults = NotificationCompat.DEFAULT_ALL
        )
    }

    fun showStaleInboxReminder(thoughtId: Long, content: String) {
        showNotification(
            notificationId = thoughtId + STALE_NOTIFICATION_ID_OFFSET,
            channelId = STALE_CHANNEL_ID,
            title = appContext.getString(R.string.stale_reminder_title),
            thoughtContent = content,
            priority = NotificationCompat.PRIORITY_DEFAULT,
            defaults = NotificationCompat.DEFAULT_SOUND
        )
    }

    private fun showNotification(
        notificationId: Long,
        channelId: String,
        title: String,
        thoughtContent: String,
        priority: Int,
        defaults: Int
    ) {
        createNotificationChannels(appContext)

        if (!canPostNotifications(appContext)) return

        val contentIntent = PendingIntent.getActivity(
            appContext,
            notificationId.toInt(),
            Intent(appContext, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val publicVersion = NotificationCompat.Builder(appContext, channelId)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(appContext.getString(R.string.app_name))
            .setContentText(appContext.getString(R.string.lockscreen_reminder_public_text))
            .build()
        val builder = NotificationCompat.Builder(appContext, channelId)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText(thoughtContent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(thoughtContent))
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(priority)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(publicVersion)

        createChannelSettingsPendingIntent(channelId)?.let { settingsIntent ->
            builder.addAction(
                android.R.drawable.ic_menu_preferences,
                appContext.getString(R.string.notification_settings_action),
                settingsIntent
            )
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            builder.setDefaults(defaults)
        }

        NotificationManagerCompat.from(appContext)
            .notify(notificationId.toInt(), builder.build())
    }

    private fun createChannelSettingsPendingIntent(channelId: String): PendingIntent? {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, appContext.packageName)
                .putExtra(Settings.EXTRA_CHANNEL_ID, channelId)
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                .setData(Uri.parse("package:${appContext.packageName}"))
        }

        return if (intent.resolveActivity(appContext.packageManager) != null) {
            PendingIntent.getActivity(
                appContext,
                channelId.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        } else {
            null
        }
    }

    companion object {
        private const val EXPLICIT_CHANNEL_ID = "thought_reminders_v2"
        private const val STALE_CHANNEL_ID = "thought_stale_v1"
        private const val STALE_NOTIFICATION_ID_OFFSET = 1_000_000L

        fun createNotificationChannel(context: Context) {
            createNotificationChannels(context)
        }

        fun canPostNotifications(context: Context): Boolean {
            val runtimePermissionGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED

            return runtimePermissionGranted &&
                NotificationManagerCompat.from(context).areNotificationsEnabled()
        }

        private fun createNotificationChannels(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

            val manager = context.getSystemService(NotificationManager::class.java)
            val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val notificationAudioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            val explicitChannel = NotificationChannel(
                EXPLICIT_CHANNEL_ID,
                context.getString(R.string.explicit_reminder_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.explicit_reminder_channel_description)
                enableVibration(true)
                vibrationPattern = longArrayOf(0L, 300L, 180L, 450L)
                setSound(defaultSoundUri, notificationAudioAttributes)
                lockscreenVisibility = Notification.VISIBILITY_PRIVATE
                setShowBadge(true)
            }

            val staleChannel = NotificationChannel(
                STALE_CHANNEL_ID,
                context.getString(R.string.stale_reminder_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.stale_reminder_channel_description)
                enableVibration(false)
                setSound(defaultSoundUri, notificationAudioAttributes)
                lockscreenVisibility = Notification.VISIBILITY_PRIVATE
                setShowBadge(false)
            }

            manager.createNotificationChannels(listOf(explicitChannel, staleChannel))
        }
    }
}
