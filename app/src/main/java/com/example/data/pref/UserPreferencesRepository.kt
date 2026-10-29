package com.example.data.pref

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "zihinpark_preferences")

class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val USER_NAME = stringPreferencesKey("user_name")
        val AVATAR_ID = intPreferencesKey("avatar_id")
        val IS_DARK_MODE = booleanPreferencesKey("is_dark_mode")
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")
        val DAILY_STREAK = intPreferencesKey("daily_streak")
        val LAST_PLAYED_DATE = longPreferencesKey("last_played_date")

        // Diamonds Economy & Pro
        val DIAMOND_BALANCE = intPreferencesKey("diamond_balance")
        val TOTAL_EARNED_DIAMONDS = intPreferencesKey("total_earned_diamonds")
        val TOTAL_SPENT_DIAMONDS = intPreferencesKey("total_spent_diamonds")
        val IS_PRO_MEMBER = booleanPreferencesKey("is_pro_member")

        // Cosmetics & Customization
        val EQUIPPED_FRAME = stringPreferencesKey("equipped_frame")
        val EQUIPPED_THEME = stringPreferencesKey("equipped_theme")
        val OWNED_COSMETICS = stringSetPreferencesKey("owned_cosmetics")

        // In-game Boosters
        val HINTS_COUNT = intPreferencesKey("hints_count")
        val EXTRA_TIME_COUNT = intPreferencesKey("extra_time_count")
        val UNDOS_COUNT = intPreferencesKey("undos_count")

        // Ad Rewarded Daily Count
        val DAILY_REWARD_DATE = longPreferencesKey("daily_reward_date")
        val DAILY_REWARDS_COUNT = intPreferencesKey("daily_rewards_count")
    }

    val userName: Flow<String> = context.dataStore.data.map { it[Keys.USER_NAME] ?: "Zihin Gezgini" }
    val avatarId: Flow<Int> = context.dataStore.data.map { it[Keys.AVATAR_ID] ?: 0 }
    val isDarkMode: Flow<Boolean> = context.dataStore.data.map { it[Keys.IS_DARK_MODE] ?: false }
    val soundEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.SOUND_ENABLED] ?: true }
    val vibrationEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.VIBRATION_ENABLED] ?: true }
    val dailyStreak: Flow<Int> = context.dataStore.data.map { it[Keys.DAILY_STREAK] ?: 1 }
    val lastPlayedDate: Flow<Long> = context.dataStore.data.map { it[Keys.LAST_PLAYED_DATE] ?: 0L }

    val diamondBalance: Flow<Int> = context.dataStore.data.map { it[Keys.DIAMOND_BALANCE] ?: 50 } // initial welcome 50 diamonds
    val totalEarnedDiamonds: Flow<Int> = context.dataStore.data.map { it[Keys.TOTAL_EARNED_DIAMONDS] ?: 50 }
    val totalSpentDiamonds: Flow<Int> = context.dataStore.data.map { it[Keys.TOTAL_SPENT_DIAMONDS] ?: 0 }
    val isProMember: Flow<Boolean> = context.dataStore.data.map { it[Keys.IS_PRO_MEMBER] ?: false }

    val equippedFrame: Flow<String> = context.dataStore.data.map { it[Keys.EQUIPPED_FRAME] ?: "none" }
    val equippedTheme: Flow<String> = context.dataStore.data.map { it[Keys.EQUIPPED_THEME] ?: "default" }
    val ownedCosmetics: Flow<Set<String>> = context.dataStore.data.map { it[Keys.OWNED_COSMETICS] ?: setOf("avatar_0", "frame_none") }

    val hintsCount: Flow<Int> = context.dataStore.data.map { it[Keys.HINTS_COUNT] ?: 3 }
    val extraTimeCount: Flow<Int> = context.dataStore.data.map { it[Keys.EXTRA_TIME_COUNT] ?: 2 }
    val undosCount: Flow<Int> = context.dataStore.data.map { it[Keys.UNDOS_COUNT] ?: 3 }
    val dailyRewardsCount: Flow<Int> = context.dataStore.data.map { it[Keys.DAILY_REWARDS_COUNT] ?: 0 }

    suspend fun setUserName(name: String) {
        context.dataStore.edit { it[Keys.USER_NAME] = name }
    }

    suspend fun setAvatarId(avatarId: Int) {
        context.dataStore.edit { it[Keys.AVATAR_ID] = avatarId }
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { it[Keys.IS_DARK_MODE] = enabled }
    }

    suspend fun setSoundEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SOUND_ENABLED] = enabled }
    }

    suspend fun setVibrationEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.VIBRATION_ENABLED] = enabled }
    }

    suspend fun setProMember(isPro: Boolean) {
        context.dataStore.edit { it[Keys.IS_PRO_MEMBER] = isPro }
    }

    suspend fun addDiamonds(amount: Int) {
        if (amount <= 0) return
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.DIAMOND_BALANCE] ?: 50
            val totalEarned = prefs[Keys.TOTAL_EARNED_DIAMONDS] ?: 50
            prefs[Keys.DIAMOND_BALANCE] = current + amount
            prefs[Keys.TOTAL_EARNED_DIAMONDS] = totalEarned + amount
        }
    }

    suspend fun spendDiamonds(amount: Int): Boolean {
        if (amount <= 0) return true
        var success = false
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.DIAMOND_BALANCE] ?: 50
            if (current >= amount) {
                prefs[Keys.DIAMOND_BALANCE] = current - amount
                val totalSpent = prefs[Keys.TOTAL_SPENT_DIAMONDS] ?: 0
                prefs[Keys.TOTAL_SPENT_DIAMONDS] = totalSpent + amount
                success = true
            }
        }
        return success
    }

    suspend fun buyCosmetic(itemId: String, cost: Int): Boolean {
        var bought = false
        context.dataStore.edit { prefs ->
            val owned = prefs[Keys.OWNED_COSMETICS]?.toMutableSet() ?: mutableSetOf("avatar_0")
            if (owned.contains(itemId)) {
                bought = true // already owned
                return@edit
            }
            val current = prefs[Keys.DIAMOND_BALANCE] ?: 50
            if (current >= cost) {
                prefs[Keys.DIAMOND_BALANCE] = current - cost
                val totalSpent = prefs[Keys.TOTAL_SPENT_DIAMONDS] ?: 0
                prefs[Keys.TOTAL_SPENT_DIAMONDS] = totalSpent + cost
                owned.add(itemId)
                prefs[Keys.OWNED_COSMETICS] = owned
                bought = true
            }
        }
        return bought
    }

    suspend fun equipFrame(frameId: String) {
        context.dataStore.edit { it[Keys.EQUIPPED_FRAME] = frameId }
    }

    suspend fun equipTheme(themeId: String) {
        context.dataStore.edit { it[Keys.EQUIPPED_THEME] = themeId }
    }

    suspend fun addBooster(type: String, count: Int = 1) {
        context.dataStore.edit { prefs ->
            when (type) {
                "hint" -> {
                    val c = prefs[Keys.HINTS_COUNT] ?: 3
                    prefs[Keys.HINTS_COUNT] = c + count
                }
                "time" -> {
                    val c = prefs[Keys.EXTRA_TIME_COUNT] ?: 2
                    prefs[Keys.EXTRA_TIME_COUNT] = c + count
                }
                "undo" -> {
                    val c = prefs[Keys.UNDOS_COUNT] ?: 3
                    prefs[Keys.UNDOS_COUNT] = c + count
                }
            }
        }
    }

    suspend fun useBooster(type: String): Boolean {
        var used = false
        context.dataStore.edit { prefs ->
            when (type) {
                "hint" -> {
                    val c = prefs[Keys.HINTS_COUNT] ?: 3
                    if (c > 0) {
                        prefs[Keys.HINTS_COUNT] = c - 1
                        used = true
                    }
                }
                "time" -> {
                    val c = prefs[Keys.EXTRA_TIME_COUNT] ?: 2
                    if (c > 0) {
                        prefs[Keys.EXTRA_TIME_COUNT] = c - 1
                        used = true
                    }
                }
                "undo" -> {
                    val c = prefs[Keys.UNDOS_COUNT] ?: 3
                    if (c > 0) {
                        prefs[Keys.UNDOS_COUNT] = c - 1
                        used = true
                    }
                }
            }
        }
        return used
    }

    suspend fun recordRewardedAdView(): Boolean {
        val now = System.currentTimeMillis()
        val oneDayMillis = 24 * 60 * 60 * 1000L
        var canReward = false
        context.dataStore.edit { prefs ->
            val lastRewardDate = prefs[Keys.DAILY_REWARD_DATE] ?: 0L
            val currentCount = if ((now - lastRewardDate) > oneDayMillis) 0 else (prefs[Keys.DAILY_REWARDS_COUNT] ?: 0)
            if (currentCount < 3) {
                canReward = true
                prefs[Keys.DAILY_REWARDS_COUNT] = currentCount + 1
                prefs[Keys.DAILY_REWARD_DATE] = now
                // Award 3 diamonds per verified rewarded ad
                val currentBalance = prefs[Keys.DIAMOND_BALANCE] ?: 50
                val totalEarned = prefs[Keys.TOTAL_EARNED_DIAMONDS] ?: 50
                prefs[Keys.DIAMOND_BALANCE] = currentBalance + 3
                prefs[Keys.TOTAL_EARNED_DIAMONDS] = totalEarned + 3
            }
        }
        return canReward
    }

    suspend fun recordGamePlayed() {
        val now = System.currentTimeMillis()
        context.dataStore.edit { prefs ->
            val last = prefs[Keys.LAST_PLAYED_DATE] ?: 0L
            val currentStreak = prefs[Keys.DAILY_STREAK] ?: 0
            val oneDayMillis = 24 * 60 * 60 * 1000L

            if (last == 0L) {
                prefs[Keys.DAILY_STREAK] = 1
            } else {
                val diffDays = (now - last) / oneDayMillis
                if (diffDays == 1L) {
                    prefs[Keys.DAILY_STREAK] = currentStreak + 1
                } else if (diffDays > 1L) {
                    prefs[Keys.DAILY_STREAK] = 1
                }
            }
            prefs[Keys.LAST_PLAYED_DATE] = now
        }
    }

    suspend fun resetAll() {
        context.dataStore.edit { it.clear() }
    }
}
