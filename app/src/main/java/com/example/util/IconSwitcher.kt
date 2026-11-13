package com.example.util

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.Calendar

/**
 * Uygulama simgesi: Sabah Bilgesi (06:00–17:59) / Gece Bilgesi.
 * Değişim yalnızca uygulama arka plana geçerken yapılır ki kullanım sırasında ekran kapanmasın.
 */
object IconSwitcher {
    private const val PREFS = "zp_icon"
    private const val KEY = "mode"
    private const val NIGHT = "com.example.IconNight"
    private const val DAY = "com.example.IconDay"

    /** "auto" | "day" | "night" */
    var mode by mutableStateOf("auto")
        private set

    fun init(context: Context) {
        mode = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "auto") ?: "auto"
    }

    fun setMode(context: Context, m: String) {
        mode = m
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, m).apply()
    }

    private fun wantDay(): Boolean = when (mode) {
        "day" -> true
        "night" -> false
        else -> Calendar.getInstance().get(Calendar.HOUR_OF_DAY) in 6..17
    }

    /** Gerekliyse simgeyi değiştirir; zaten doğruysa hiçbir şey yapmaz. */
    fun apply(context: Context) {
        runCatching {
            val pm = context.packageManager
            val day = ComponentName(context.packageName, DAY)
            val night = ComponentName(context.packageName, NIGHT)
            val dayOn = pm.getComponentEnabledSetting(day) == PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            val want = wantDay()
            if (dayOn == want) return
            val on = PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            val off = PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            // Önce yenisini aç, sonra eskisini kapat: arada simgesiz kalmasın
            pm.setComponentEnabledSetting(if (want) day else night, on, PackageManager.DONT_KILL_APP)
            pm.setComponentEnabledSetting(if (want) night else day, off, PackageManager.DONT_KILL_APP)
        }
    }
}
