package com.maxeydev.picklejot.ui.notification

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.maxeydev.picklejot.domain.reminder.ReminderNotifier
import com.maxeydev.picklejot.ui.R

object StreakNotification {
    const val CHANNEL_ID = "streak_reminders_v2"
    const val LEGACY_CHANNEL_ID = "streak_reminders"
    const val NOTIFICATION_ID = 2501
    const val EXTRA_OPEN_LOGGING = "com.maxeydev.picklejot.extra.OPEN_LOGGING"
    internal const val LAUNCH_REQUEST_CODE = 2501
    private const val FIRST_RUNTIME_PERMISSION_API = 33

    fun requiresRuntimePermission(): Boolean = Build.VERSION.SDK_INT >= FIRST_RUNTIME_PERMISSION_API

    fun launchIntent(context: Context): Intent? =
        context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_OPEN_LOGGING, true)
        }

    fun wantsLogging(intent: Intent?): Boolean = intent?.getBooleanExtra(EXTRA_OPEN_LOGGING, false) == true
}

class StreakReminderNotifier(
    private val context: Context,
) : ReminderNotifier {
    @SuppressLint("MissingPermission")
    override fun notifyStreakAtRisk(skipAvailable: Boolean): Boolean {
        val manager = NotificationManagerCompat.from(context)
        ensureChannel(manager)
        if (!canPost(manager)) {
            return false
        }
        val notification = build(skipAvailable) ?: return false
        if (StreakNotification.requiresRuntimePermission() &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }
        manager.notify(StreakNotification.NOTIFICATION_ID, notification)
        return true
    }

    fun build(skipAvailable: Boolean): Notification? {
        val launch = StreakNotification.launchIntent(context) ?: return null
        val tap =
            PendingIntent.getActivity(
                context,
                StreakNotification.LAUNCH_REQUEST_CODE,
                launch,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
        val body =
            context.getString(
                if (skipAvailable) R.string.streak_notification_body_skip else R.string.streak_notification_body,
            )
        return NotificationCompat
            .Builder(context, StreakNotification.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_streak)
            .setLargeIcon(BitmapFactory.decodeResource(context.resources, R.drawable.mascot_head_cheer))
            .setContentTitle(context.getString(R.string.streak_notification_title))
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(tap)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
    }

    private fun ensureChannel(manager: NotificationManagerCompat) {
        manager.deleteNotificationChannel(StreakNotification.LEGACY_CHANNEL_ID)
        val sound = Uri.parse("android.resource://${context.packageName}/${R.raw.sfx_reminder_notification}")
        val soundAttributes =
            AudioAttributes
                .Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        val channel =
            NotificationChannelCompat
                .Builder(StreakNotification.CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_DEFAULT)
                .setName(context.getString(R.string.streak_notification_channel_name))
                .setDescription(context.getString(R.string.streak_notification_channel_description))
                .setSound(sound, soundAttributes)
                .build()
        manager.createNotificationChannel(channel)
    }

    private fun canPost(manager: NotificationManagerCompat): Boolean {
        val channel = manager.getNotificationChannelCompat(StreakNotification.CHANNEL_ID)
        val channelBlocked = channel?.importance == NotificationManagerCompat.IMPORTANCE_NONE
        return manager.areNotificationsEnabled() && !channelBlocked
    }
}
