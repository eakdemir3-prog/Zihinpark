package com.example

import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.example.data.repository.GameRepository
import com.example.ui.navigation.ZihinParkAppNavigation
import com.example.ui.theme.ZihinParkTheme
import com.example.util.SoundAndVibeManager

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(AndroidColor.TRANSPARENT)
        )

        // Initialize notification channel and verify daily reminder schedule
        com.example.notification.NotificationHelper.createNotificationChannel(applicationContext)
        val reminderPrefs = com.example.notification.ReminderPreferences.getInstance(applicationContext)
        if (reminderPrefs.isReminderEnabled) {
            com.example.notification.NotificationHelper.scheduleDailyReminder(
                applicationContext,
                reminderPrefs.reminderHour,
                reminderPrefs.reminderMinute
            )
        }

        setContent {
            val repository = remember { GameRepository(applicationContext) }
            val soundManager = remember { SoundAndVibeManager(applicationContext) }

            val soundOn by repository.preferences.soundEnabled.collectAsState(initial = true)
            val vibrationOn by repository.preferences.vibrationEnabled.collectAsState(initial = true)
            LaunchedEffect(soundOn, vibrationOn) {
                soundManager.soundOn = soundOn
                soundManager.vibrationOn = vibrationOn
            }

            ZihinParkTheme {
                ZihinParkAppNavigation(
                    repository = repository,
                    soundManager = soundManager
                )
            }
        }
    }
}
