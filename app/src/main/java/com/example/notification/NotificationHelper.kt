package com.example.notification

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R
import java.util.Calendar

/**
 * Utility helper for managing Notification Channels, scheduling daily reminders via AlarmManager,
 * and displaying rich brain training notification prompts.
 */
object NotificationHelper {

    const val CHANNEL_ID = "daily_brain_reminder_channel"
    const val CHANNEL_NAME = "Günlük Zihin Egzersizi"
    const val CHANNEL_DESC = "ZihinPark günlük zeka, mantık ve hafıza antrenmanı hatırlatıcıları"

    const val ACTION_TRIGGER_DAILY_REMINDER = "com.example.zihinpark.ACTION_TRIGGER_DAILY_REMINDER"
    const val NOTIFICATION_ID = 1001
    const val TEST_NOTIFICATION_ID = 1002
    const val DAILY_REMINDER_REQUEST_CODE = 2001

    /**
     * Initializes notification channel for Android 8.0 (API 26) and above.
     */
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
                enableVibration(true)
                setShowBadge(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.createNotificationChannel(channel)
        }
    }

    /**
     * Calculates next alarm timestamp for specified hour and minute.
     * If the time has already passed today, rolls over to tomorrow.
     */
    fun calculateNextReminderMillis(hour: Int, minute: Int, nowMillis: Long = System.currentTimeMillis()): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= nowMillis) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        return calendar.timeInMillis
    }

    /**
     * Schedules the daily alarm to trigger DailyReminderReceiver at the chosen hour:minute.
     */
    fun scheduleDailyReminder(context: Context, hour: Int, minute: Int) {
        createNotificationChannel(context)

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val nextTriggerMillis = calculateNextReminderMillis(hour, minute)

        val intent = Intent(context, DailyReminderReceiver::class.java).apply {
            action = ACTION_TRIGGER_DAILY_REMINDER
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            DAILY_REMINDER_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        nextTriggerMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        nextTriggerMillis,
                        pendingIntent
                    )
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    nextTriggerMillis,
                    pendingIntent
                )
            } else {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    nextTriggerMillis,
                    pendingIntent
                )
            }
        } catch (_: SecurityException) {
            // Fallback to inexact alarm if exact permission was revoked
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                nextTriggerMillis,
                pendingIntent
            )
        }
    }

    /**
     * Cancels existing scheduled reminder alarms.
     */
    fun cancelDailyReminder(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, DailyReminderReceiver::class.java).apply {
            action = ACTION_TRIGGER_DAILY_REMINDER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            DAILY_REMINDER_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    /**
     * Displays a brain training notification.
     */
    fun showReminderNotification(
        context: Context,
        title: String,
        message: String,
        notificationId: Int = NOTIFICATION_ID
    ): Boolean {
        createNotificationChannel(context)

        val notificationManager = NotificationManagerCompat.from(context)
        if (!notificationManager.areNotificationsEnabled()) {
            return false
        }

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val contentPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        return try {
            notificationManager.notify(notificationId, notification)
            true
        } catch (_: SecurityException) {
            false
        }
    }

    /**
     * Sends an immediate test notification for preview and verification in Settings.
     */
    fun sendImmediateTestNotification(context: Context): Boolean {
        return showReminderNotification(
            context = context,
            title = "🧠 ZihinPark Test Bildirimi",
            message = "Harika! Bildirimler sorunsuz çalışıyor. Her gün belirlediğin saatte zihin egzersizi hatırlatıcın burada belirecek.",
            notificationId = TEST_NOTIFICATION_ID
        )
    }
}
