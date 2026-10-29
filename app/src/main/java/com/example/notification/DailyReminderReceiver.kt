package com.example.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.local.ZihinParkDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * BroadcastReceiver responsible for receiving AlarmManager triggers and BOOT_COMPLETED events.
 * Contextualizes notification copy using Room database activity metrics.
 */
class DailyReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        val prefs = ReminderPreferences.getInstance(context)

        when (action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            "android.intent.action.QUICKBOOT_POWERON" -> {
                // Device rebooted; restore scheduled reminder if enabled
                if (prefs.isReminderEnabled) {
                    NotificationHelper.scheduleDailyReminder(
                        context = context,
                        hour = prefs.reminderHour,
                        minute = prefs.reminderMinute
                    )
                }
            }

            NotificationHelper.ACTION_TRIGGER_DAILY_REMINDER -> {
                if (!prefs.isReminderEnabled) return

                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val db = ZihinParkDatabase.getInstance(context)
                        val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                        val todayActivity = db.userDailyActivityDao().getActivityDirect(todayDate)

                        val title: String
                        val message: String

                        if (todayActivity == null || todayActivity.gamesPlayedCount == 0) {
                            title = "🧠 Günlük Zihin Egzersizi Vakti!"
                            message = "Bugünkü beyin antrenmanını henüz tamamlamadın. 5 dakikanı ayırarak hafıza ve mantık egzersizlerini yap, zihnini zinde tut!"
                        } else {
                            val games = todayActivity.gamesPlayedCount
                            val minutes = (todayActivity.totalPlayTimeSeconds / 60).coerceAtLeast(1)
                            title = "🔥 Günlük Antrenmanın Harika Gidiyor!"
                            message = "Bugün $games oyun tamamladın ($minutes dk). Zihnini daha da keskinleştirmek ve yeni bir rekor kırmak ister misin?"
                        }

                        NotificationHelper.showReminderNotification(context, title, message)

                        // Reschedule next daily reminder for tomorrow
                        NotificationHelper.scheduleDailyReminder(
                            context = context,
                            hour = prefs.reminderHour,
                            minute = prefs.reminderMinute
                        )
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
    }
}
