package com.example.data.repository

import android.content.Context
import com.example.data.local.ZihinParkDatabase
import com.example.data.local.entity.GameEntity
import com.example.data.local.entity.GameRecordEntity
import com.example.data.local.entity.LevelProgressEntity
import com.example.data.pref.UserPreferencesRepository
import com.example.data.local.entity.UserDailyActivityEntity
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GameRepository(context: Context) {
    private val database = ZihinParkDatabase.getInstance(context)
    private val gameDao = database.gameDao()
    private val levelDao = database.levelProgressDao()
    private val recordDao = database.gameRecordDao()
    private val activityDao = database.userDailyActivityDao()
    val preferences = UserPreferencesRepository(context)

    // Game Table Flows
    val allGames: Flow<List<GameEntity>> = gameDao.getAllGames()
    val allRecords: Flow<List<GameRecordEntity>> = recordDao.getAllRecords()
    val totalScore: Flow<Int?> = recordDao.getTotalScore()
    val totalPlayedCount: Flow<Int> = recordDao.getTotalPlayedCount()
    val playedGamesCount: Flow<Int> = recordDao.getPlayedGamesCount()
    val totalHighestScore: Flow<Int?> = gameDao.getTotalHighestScore()
    val totalStars: Flow<Int?> = levelDao.getTotalStars()

    // Daily Activity & Play Time Flows
    val totalPlayTimeSeconds: Flow<Long?> = activityDao.getTotalPlayTimeSeconds()
    val allDailyActivities: Flow<List<UserDailyActivityEntity>> = activityDao.getAllActivities()
    val recentDailyActivities: Flow<List<UserDailyActivityEntity>> = activityDao.getRecentActivities(7)
    val activeDaysCount: Flow<Int> = activityDao.getActiveDaysCount()
    val totalDailyGamesCount: Flow<Int?> = activityDao.getTotalGamesPlayedCount()

    fun getTodayDate(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    fun getTodayActivity(): Flow<UserDailyActivityEntity?> =
        activityDao.getActivityByDate(getTodayDate())

    fun getGameById(gameId: String): Flow<GameEntity?> = gameDao.getGameById(gameId)

    fun getLevelsForGame(gameId: String): Flow<List<LevelProgressEntity>> = levelDao.getLevelsForGame(gameId)

    fun getHighScore(gameId: String): Flow<Int?> = recordDao.getHighScore(gameId)

    fun getRecentRecords(limit: Int): Flow<List<GameRecordEntity>> = recordDao.getRecentRecords(limit)

    suspend fun saveGameResult(
        gameId: String,
        gameName: String,
        category: String,
        score: Int,
        durationSeconds: Long,
        levelNumber: Int = 1,
        starsEarned: Int = 3
    ): Int {
        // 1. Insert history record
        recordDao.insertRecord(
            GameRecordEntity(
                gameId = gameId,
                gameName = gameName,
                category = category,
                score = score,
                durationSeconds = durationSeconds
            )
        )

        // 2. Update Game Highest Score & Plays
        gameDao.updateHighestScore(gameId, score)
        gameDao.incrementPlayCount(gameId)
        gameDao.updateUnlockedLevel(gameId, levelNumber + 1)

        // 3. Level completion & Diamond reward calculation
        val currentLevel = levelDao.getLevel(gameId, levelNumber)
        var awardedDiamonds = 0

        val isFirstTime = currentLevel?.isRewardClaimed != true
        if (isFirstTime) {
            // Easy: 2 diamonds, Medium: 4 diamonds, Hard: 7 diamonds
            awardedDiamonds = when (category) {
                "Mantık & Çıkarım", "Uzamsal Zekâ" -> 4
                "Sayısal & Analitik", "Strateji & Hız" -> if (score >= 90) 6 else 4
                else -> 3
            }
            preferences.addDiamonds(awardedDiamonds)
        }

        // Save level progress
        levelDao.insertOrUpdateLevel(
            LevelProgressEntity(
                id = currentLevel?.id ?: 0,
                gameId = gameId,
                levelNumber = levelNumber,
                isUnlocked = true,
                stars = maxOf(currentLevel?.stars ?: 0, starsEarned),
                bestScore = maxOf(currentLevel?.bestScore ?: 0, score),
                isRewardClaimed = true,
                completedAt = System.currentTimeMillis()
            )
        )

        // Unlock next level
        levelDao.unlockLevel(gameId, levelNumber + 1)

        // 4. Update daily streak & record user daily activity metrics in Room DB
        preferences.recordGamePlayed()
        recordDailyActivity(
            durationSeconds = durationSeconds,
            score = score,
            starsEarned = starsEarned
        )

        return awardedDiamonds
    }

    suspend fun recordDailyActivity(
        durationSeconds: Long,
        score: Int = 0,
        starsEarned: Int = 0
    ) {
        val todayStr = getTodayDate()
        val existing = activityDao.getActivityDirect(todayStr)
        val validDuration = if (durationSeconds > 0) durationSeconds else 15L // Minimum session floor
        val updated = if (existing != null) {
            existing.copy(
                totalPlayTimeSeconds = existing.totalPlayTimeSeconds + validDuration,
                gamesPlayedCount = existing.gamesPlayedCount + 1,
                totalScoreEarned = existing.totalScoreEarned + maxOf(score, 0),
                starsEarned = existing.starsEarned + maxOf(starsEarned, 0),
                lastActiveTimestamp = System.currentTimeMillis()
            )
        } else {
            UserDailyActivityEntity(
                date = todayStr,
                totalPlayTimeSeconds = validDuration,
                gamesPlayedCount = 1,
                totalScoreEarned = maxOf(score, 0),
                starsEarned = maxOf(starsEarned, 0),
                lastActiveTimestamp = System.currentTimeMillis()
            )
        }
        activityDao.insertOrUpdateActivity(updated)
    }

    suspend fun recordTimeSpentOnly(durationSeconds: Long) {
        if (durationSeconds <= 0) return
        val todayStr = getTodayDate()
        val existing = activityDao.getActivityDirect(todayStr)
        val updated = if (existing != null) {
            existing.copy(
                totalPlayTimeSeconds = existing.totalPlayTimeSeconds + durationSeconds,
                lastActiveTimestamp = System.currentTimeMillis()
            )
        } else {
            UserDailyActivityEntity(
                date = todayStr,
                totalPlayTimeSeconds = durationSeconds,
                gamesPlayedCount = 0,
                totalScoreEarned = 0,
                starsEarned = 0,
                lastActiveTimestamp = System.currentTimeMillis()
            )
        }
        activityDao.insertOrUpdateActivity(updated)
    }

    suspend fun clearHistory() {
        recordDao.clearAllRecords()
        activityDao.clearAllDailyActivities()
    }
}
