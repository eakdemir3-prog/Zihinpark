package com.example.notification

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages daily training reminder settings synchronously and reactively.
 * Synchronous access is critical for BroadcastReceivers and AlarmManager triggers.
 */
class ReminderPreferences(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _isReminderEnabledFlow = MutableStateFlow(isReminderEnabled)
    val isReminderEnabledFlow: StateFlow<Boolean> = _isReminderEnabledFlow.asStateFlow()

    private val _reminderHourFlow = MutableStateFlow(reminderHour)
    val reminderHourFlow: StateFlow<Int> = _reminderHourFlow.asStateFlow()

    private val _reminderMinuteFlow = MutableStateFlow(reminderMinute)
    val reminderMinuteFlow: StateFlow<Int> = _reminderMinuteFlow.asStateFlow()

    var isReminderEnabled: Boolean
        get() = prefs.getBoolean(KEY_REMINDER_ENABLED, true)
        set(value) {
            prefs.edit().putBoolean(KEY_REMINDER_ENABLED, value).apply()
            _isReminderEnabledFlow.value = value
        }

    var reminderHour: Int
        get() = prefs.getInt(KEY_REMINDER_HOUR, DEFAULT_HOUR)
        set(value) {
            prefs.edit().putInt(KEY_REMINDER_HOUR, value).apply()
            _reminderHourFlow.value = value
        }

    var reminderMinute: Int
        get() = prefs.getInt(KEY_REMINDER_MINUTE, DEFAULT_MINUTE)
        set(value) {
            prefs.edit().putInt(KEY_REMINDER_MINUTE, value).apply()
            _reminderMinuteFlow.value = value
        }

    fun setReminderTime(hour: Int, minute: Int) {
        prefs.edit()
            .putInt(KEY_REMINDER_HOUR, hour)
            .putInt(KEY_REMINDER_MINUTE, minute)
            .apply()
        _reminderHourFlow.value = hour
        _reminderMinuteFlow.value = minute
    }

    fun getFormattedTime(): String {
        return String.format("%02d:%02d", reminderHour, reminderMinute)
    }

    companion object {
        private const val PREFS_NAME = "zihinpark_reminder_preferences"
        private const val KEY_REMINDER_ENABLED = "reminder_enabled"
        private const val KEY_REMINDER_HOUR = "reminder_hour"
        private const val KEY_REMINDER_MINUTE = "reminder_minute"

        const val DEFAULT_HOUR = 20 // 20:00 (8:00 PM)
        const val DEFAULT_MINUTE = 0

        @Volatile
        private var INSTANCE: ReminderPreferences? = null

        fun getInstance(context: Context): ReminderPreferences {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ReminderPreferences(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
