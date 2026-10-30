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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import androidx.compose.runtime.setValue
import com.example.data.repository.GameRepository
import com.example.ui.navigation.ZihinParkAppNavigation
import com.example.ui.theme.ZihinParkTheme
import com.example.util.SoundAndVibeManager

class MainActivity : ComponentActivity() {
    override fun onStop() {
        super.onStop()
        com.example.cloud.CloudSync.flush()
    }

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

        com.example.cloud.CloudSync.init(applicationContext)
        com.example.monetize.Billing.init(applicationContext)
        com.example.monetize.Ads.playedLevelsProvider = {
            com.example.data.local.ZihinParkDatabase.getInstance(applicationContext).levelProgressDao()
                .getAllLevels().first().count { it.stars > 0 }
        }
        com.example.monetize.Ads.init(this)

        // Yalnızca test derlemesinde: otomatik ekran görüntüsü için doğrudan bir ekranı açar
        val debugRoute = if (BuildConfig.DEBUG) intent?.getStringExtra("zp_route") else null
        val skipTutorials = BuildConfig.DEBUG && intent?.getBooleanExtra("zp_skip_tutorial", false) == true

        // Tema tercihi ilk karede doğru olsun diye açılışta okunur
        val startPrefs = com.example.data.pref.UserPreferencesRepository(applicationContext)
        val light = kotlinx.coroutines.runBlocking { startPrefs.lightTheme.first() }
        // İlk açılışta karşılama/giriş ekranı (zaten giriş yapılmışsa ya da test rotası varsa gösterme)
        val needWelcome = debugRoute == null && !skipTutorials &&
            kotlinx.coroutines.runBlocking { !startPrefs.welcomeDone.first() } &&
            com.example.cloud.CloudSync.user.value == null
        val forceLight = BuildConfig.DEBUG && intent?.getBooleanExtra("zp_light", false) == true
        com.example.ui.theme.ZpColors.palette = if (light || forceLight) com.example.ui.theme.LightPalette else com.example.ui.theme.NeonPalette

        setContent {
            val repository = remember { GameRepository(applicationContext) }
            LaunchedEffect(skipTutorials) {
                if (skipTutorials) com.example.domain.model.GameRegistry.ALL_GAMES.forEach { repository.preferences.markTutorialSeen(it.id) }
            }
            val soundManager = remember { SoundAndVibeManager(applicationContext) }

            val soundOn by repository.preferences.soundEnabled.collectAsState(initial = true)
            val vibrationOn by repository.preferences.vibrationEnabled.collectAsState(initial = true)
            LaunchedEffect(soundOn, vibrationOn) {
                soundManager.soundOn = soundOn
                soundManager.vibrationOn = vibrationOn
            }

            var showWelcome by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(needWelcome) }
            val scope = androidx.compose.runtime.rememberCoroutineScope()
            val signedOut by com.example.cloud.CloudSync.signedOut.collectAsState()
            LaunchedEffect(signedOut) { if (signedOut > 0) showWelcome = true }
            ZihinParkTheme {
                if (showWelcome) {
                    com.example.ui.screens.welcome.WelcomeScreen(onDone = {
                        showWelcome = false
                        scope.launch { repository.preferences.setWelcomeDone() }
                    })
                } else {
                    ZihinParkAppNavigation(
                        repository = repository,
                        soundManager = soundManager,
                        startRoute = debugRoute
                    )
                }
            }
        }
    }
}
