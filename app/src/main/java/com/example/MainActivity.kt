package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.example.data.repository.GameRepository
import com.example.ui.navigation.ZihinParkAppNavigation
import com.example.ui.theme.ZihinParkTheme
import com.example.ui.viewmodel.GameListViewModel
import com.example.util.SoundAndVibeManager

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

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
            val gameListViewModel = remember { GameListViewModel(repository) }

            val systemDark = isSystemInDarkTheme()
            val userDarkMode by repository.preferences.isDarkMode.collectAsState(initial = systemDark)

            ZihinParkTheme(darkTheme = userDarkMode) {
                ZihinParkAppNavigation(
                    repository = repository,
                    soundManager = soundManager,
                    gameListViewModel = gameListViewModel
                )
            }
        }
    }
}
