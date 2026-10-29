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

        // Seviye sistemi
        val SEEN_TUTORIALS = stringSetPreferencesKey("seen_tutorials")
        val ENDLESS_COUNTER = intPreferencesKey("endless_counter")
        val LAST_PLAYED_GAME = stringPreferencesKey("last_played_game")
        val LAST_STREAK_DAY = longPreferencesKey("last_streak_day")
        val LAST_GIFT_DAY = longPreferencesKey("last_gift_day")

        // Kelime Diyarı
        val WOW_LEVEL = intPreferencesKey("wow_level")
        val WOW_FOUND = stringSetPreferencesKey("wow_found")
        val WOW_FOUND_LEVEL = intPreferencesKey("wow_found_level")
        val WOW_BONUS = intPreferencesKey("wow_bonus")
        val WOW_BONUS_WORDS = stringSetPreferencesKey("wow_bonus_words")
        val WOW_REVEALED = stringSetPreferencesKey("wow_revealed")
    }

    // ---- Kelime Diyarı ----
    /** Oynanacak (açık olan en yüksek) seviye, 1..300; 301 = hepsi bitti. */
    val wowLevel: Flow<Int> = context.dataStore.data.map { it[Keys.WOW_LEVEL] ?: 1 }
    val wowBonus: Flow<Int> = context.dataStore.data.map { it[Keys.WOW_BONUS] ?: 0 }

    data class WowSave(val found: Set<String>, val bonusWords: Set<String>, val revealed: Set<String>)

    /** Yarım kalan seviyenin bulunan kelimeleri ve açılan harfleri. */
    val wowSave: Flow<Pair<Int, WowSave>> = context.dataStore.data.map {
        (it[Keys.WOW_FOUND_LEVEL] ?: 0) to WowSave(it[Keys.WOW_FOUND] ?: emptySet(), it[Keys.WOW_BONUS_WORDS] ?: emptySet(), it[Keys.WOW_REVEALED] ?: emptySet())
    }

    suspend fun saveWowProgress(level: Int, found: Set<String>, bonusWords: Set<String>, revealed: Set<String>) {
        context.dataStore.edit {
            it[Keys.WOW_FOUND_LEVEL] = level
            it[Keys.WOW_FOUND] = found
            it[Keys.WOW_BONUS_WORDS] = bonusWords
            it[Keys.WOW_REVEALED] = revealed
        }
    }

    /** Bonus kelime sayacı; her 10 bonusta true döner (ödül). */
    suspend fun addWowBonus(): Boolean {
        var reward = false
        context.dataStore.edit {
            val b = (it[Keys.WOW_BONUS] ?: 0) + 1
            it[Keys.WOW_BONUS] = b
            reward = b % 10 == 0
        }
        return reward
    }

    /** Seviye bitti: bir sonrakini aç, yarım kayıtları temizle. İlk kez bitiyorsa true. */
    suspend fun completeWowLevel(level: Int): Boolean {
        var first = false
        context.dataStore.edit {
            val cur = it[Keys.WOW_LEVEL] ?: 1
            if (level >= cur) { it[Keys.WOW_LEVEL] = level + 1; first = true }
            it[Keys.WOW_FOUND_LEVEL] = 0
            it[Keys.WOW_FOUND] = emptySet()
            it[Keys.WOW_BONUS_WORDS] = emptySet()
            it[Keys.WOW_REVEALED] = emptySet()
        }
        return first
    }

    suspend fun setWowLevel(level: Int) { context.dataStore.edit { it[Keys.WOW_LEVEL] = level } }

    /** Bugünkü hediye alındı mı. */
    val dailyGiftClaimed: Flow<Boolean> = context.dataStore.data.map { (it[Keys.LAST_GIFT_DAY] ?: -1L) == epochDay(System.currentTimeMillis()) }

    /** Günde bir kez ücretsiz elmas: seriye göre 10..30. Alındıysa 0 döner. */
    suspend fun claimDailyGift(): Int {
        var amount = 0
        val today = epochDay(System.currentTimeMillis())
        context.dataStore.edit { prefs ->
            if ((prefs[Keys.LAST_GIFT_DAY] ?: -1L) == today) return@edit
            val streak = prefs[Keys.DAILY_STREAK] ?: 0
            amount = (10 + streak * 2).coerceAtMost(30)
            prefs[Keys.LAST_GIFT_DAY] = today
            prefs[Keys.DIAMOND_BALANCE] = (prefs[Keys.DIAMOND_BALANCE] ?: 50) + amount
            prefs[Keys.TOTAL_EARNED_DIAMONDS] = (prefs[Keys.TOTAL_EARNED_DIAMONDS] ?: 50) + amount
        }
        return amount
    }

    /** Bulut yedeğinden geri yükleme (yalnızca gelen değerler yazılır). */
    suspend fun restoreFromCloud(
        name: String?, avatar: Int?, frame: String?, diamonds: Int?,
        hints: Int?, times: Int?, undos: Int?, owned: Set<String>?
    ) {
        context.dataStore.edit { p ->
            name?.let { p[Keys.USER_NAME] = it }
            avatar?.let { p[Keys.AVATAR_ID] = it }
            frame?.let { p[Keys.EQUIPPED_FRAME] = it }
            diamonds?.let { p[Keys.DIAMOND_BALANCE] = maxOf(it, p[Keys.DIAMOND_BALANCE] ?: 0) }
            hints?.let { p[Keys.HINTS_COUNT] = maxOf(it, p[Keys.HINTS_COUNT] ?: 0) }
            times?.let { p[Keys.EXTRA_TIME_COUNT] = maxOf(it, p[Keys.EXTRA_TIME_COUNT] ?: 0) }
            undos?.let { p[Keys.UNDOS_COUNT] = maxOf(it, p[Keys.UNDOS_COUNT] ?: 0) }
            owned?.let { p[Keys.OWNED_COSMETICS] = (p[Keys.OWNED_COSMETICS] ?: emptySet()) + it }
        }
    }

    /** Elmasla güç satın alır. */
    suspend fun buyBooster(type: String, count: Int, cost: Int): Boolean {
        var ok = false
        context.dataStore.edit { prefs ->
            val bal = prefs[Keys.DIAMOND_BALANCE] ?: 50
            if (bal < cost) return@edit
            prefs[Keys.DIAMOND_BALANCE] = bal - cost
            prefs[Keys.TOTAL_SPENT_DIAMONDS] = (prefs[Keys.TOTAL_SPENT_DIAMONDS] ?: 0) + cost
            val key = when (type) { "hint" -> Keys.HINTS_COUNT; "time" -> Keys.EXTRA_TIME_COUNT; else -> Keys.UNDOS_COUNT }
            val def = if (type == "time") 2 else 3
            prefs[key] = (prefs[key] ?: def) + count
            ok = true
        }
        return ok
    }

    val seenTutorials: Flow<Set<String>> = context.dataStore.data.map { it[Keys.SEEN_TUTORIALS] ?: emptySet() }
    val lastPlayedGame: Flow<String?> = context.dataStore.data.map { it[Keys.LAST_PLAYED_GAME] }

    suspend fun markTutorialSeen(gameId: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SEEN_TUTORIALS] = (prefs[Keys.SEEN_TUTORIALS] ?: emptySet()) + gameId
        }
    }

    suspend fun setLastPlayedGame(gameId: String) {
        context.dataStore.edit { it[Keys.LAST_PLAYED_GAME] = gameId }
    }

    /** Sonsuz mod için her seferinde artan sayaç: üretilen bulmacalar hiç tekrar etmez. */
    suspend fun nextEndlessSeedBase(): Int {
        var value = 0
        context.dataStore.edit { prefs ->
            value = prefs[Keys.ENDLESS_COUNTER] ?: 0
            prefs[Keys.ENDLESS_COUNTER] = value + 1000
        }
        return value
    }

    val userName: Flow<String> = context.dataStore.data.map { it[Keys.USER_NAME] ?: "Zihin Gezgini" }
    val avatarId: Flow<Int> = context.dataStore.data.map { it[Keys.AVATAR_ID] ?: 0 }
    val isDarkMode: Flow<Boolean> = context.dataStore.data.map { it[Keys.IS_DARK_MODE] ?: false }
    val soundEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.SOUND_ENABLED] ?: true }
    val vibrationEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.VIBRATION_ENABLED] ?: true }
    /** Dün ya da bugün oynanmadıysa seri bozulmuş sayılır ve 0 gösterilir. */
    val dailyStreak: Flow<Int> = context.dataStore.data.map { prefs ->
        val streak = prefs[Keys.DAILY_STREAK] ?: 0
        val lastDay = prefs[Keys.LAST_STREAK_DAY] ?: return@map streak
        if (epochDay(System.currentTimeMillis()) - lastDay > 1L) 0 else streak
    }
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

    /** Günlük seri takvim gününe göre hesaplanır (gece yarısını geçince yeni gün). */
    suspend fun recordGamePlayed() {
        val now = System.currentTimeMillis()
        val today = epochDay(now)
        context.dataStore.edit { prefs ->
            val lastDay = prefs[Keys.LAST_STREAK_DAY] ?: (prefs[Keys.LAST_PLAYED_DATE]?.let { if (it > 0) epochDay(it) else null } ?: -1L)
            val currentStreak = prefs[Keys.DAILY_STREAK] ?: 0
            prefs[Keys.DAILY_STREAK] = when {
                lastDay < 0 -> 1
                today == lastDay -> maxOf(currentStreak, 1)
                today - lastDay == 1L -> currentStreak + 1
                else -> 1
            }
            prefs[Keys.LAST_STREAK_DAY] = today
            prefs[Keys.LAST_PLAYED_DATE] = now
        }
    }

    private fun epochDay(millis: Long): Long {
        val offset = java.util.TimeZone.getDefault().getOffset(millis)
        return Math.floorDiv(millis + offset, 86_400_000L)
    }

    suspend fun resetAll() {
        context.dataStore.edit { it.clear() }
    }
}
