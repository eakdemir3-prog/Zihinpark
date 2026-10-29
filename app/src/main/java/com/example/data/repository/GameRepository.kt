package com.example.data.repository

import android.content.Context
import com.example.data.local.ZihinParkDatabase
import com.example.data.local.entity.GameEntity
import com.example.data.local.entity.GameRecordEntity
import com.example.data.local.entity.LevelProgressEntity
import com.example.data.local.entity.UserDailyActivityEntity
import com.example.data.pref.UserPreferencesRepository
import com.example.domain.model.GameRegistry
import com.example.game.engine.GameProgress
import com.example.game.engine.LevelRules
import com.example.game.engine.RoundResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Bir seviye denemesinin sonucu (sonuç ekranında gösterilir). */
data class LevelOutcome(
    val stars: Int,
    val previousStars: Int,
    val score: Int,
    val diamonds: Int,
    val firstClear: Boolean,
    val perfectBonus: Boolean,
    val newBest: Boolean,
    /** Bu denemeyle yeni açılan seviye (yoksa null). */
    val unlockedLevel: Int?,
    /** Bu denemeyle yeni bölge açıldıysa bölge indeksi. */
    val unlockedZone: Int?,
    val endlessUnlocked: Boolean,
    /** Bir sonraki seviye şu an oynanabilir mi. */
    val nextUnlocked: Boolean
)

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
    val completedLevelsCount: Flow<Int> = levelDao.getCompletedLevelsCount()

    // Daily Activity & Play Time Flows
    val totalPlayTimeSeconds: Flow<Long?> = activityDao.getTotalPlayTimeSeconds()
    val allDailyActivities: Flow<List<UserDailyActivityEntity>> = activityDao.getAllActivities()
    val recentDailyActivities: Flow<List<UserDailyActivityEntity>> = activityDao.getRecentActivities(7)
    val activeDaysCount: Flow<Int> = activityDao.getActiveDaysCount()
    val totalDailyGamesCount: Flow<Int?> = activityDao.getTotalGamesPlayedCount()

    /** Tüm oyunların seviye ilerlemesi (oyun id -> ilerleme). */
    val allProgress: Flow<Map<String, GameProgress>> =
        combine(levelDao.getAllLevels(), gameDao.getAllGames()) { levels, games ->
            val byGame = levels.groupBy { it.gameId }
            val endless = games.associate { it.id to it.endlessBest }
            GameRegistry.ALL_GAMES.associate { meta ->
                val rows = byGame[meta.id].orEmpty()
                meta.id to GameProgress(
                    starsByLevel = rows.filter { it.stars > 0 }.associate { it.levelNumber to it.stars },
                    endlessBest = endless[meta.id] ?: 0
                )
            }
        }

    fun progressFor(gameId: String): Flow<GameProgress> =
        combine(levelDao.getLevelsForGame(gameId), gameDao.getGameById(gameId)) { rows, game ->
            GameProgress(
                starsByLevel = rows.filter { it.stars > 0 }.associate { it.levelNumber to it.stars },
                endlessBest = game?.endlessBest ?: 0
            )
        }

    fun levelRowsFor(gameId: String): Flow<Map<Int, LevelProgressEntity>> =
        levelDao.getLevelsForGame(gameId).map { rows -> rows.associateBy { it.levelNumber } }

    fun getTodayDate(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    fun getTodayActivity(): Flow<UserDailyActivityEntity?> =
        activityDao.getActivityByDate(getTodayDate())

    fun getGameById(gameId: String): Flow<GameEntity?> = gameDao.getGameById(gameId)

    fun getLevelsForGame(gameId: String): Flow<List<LevelProgressEntity>> = levelDao.getLevelsForGame(gameId)

    fun getHighScore(gameId: String): Flow<Int?> = recordDao.getHighScore(gameId)

    fun getRecentRecords(limit: Int): Flow<List<GameRecordEntity>> = recordDao.getRecentRecords(limit)

    private suspend fun currentProgress(gameId: String): GameProgress {
        val rows = levelDao.getLevelsForGameSync(gameId)
        return GameProgress(rows.filter { it.stars > 0 }.associate { it.levelNumber to it.stars })
    }

    /**
     * Bir seviye denemesini kaydeder.
     * - Geçilemezse (0 yıldız) seviye tamamlanmış sayılmaz, sonraki seviye açılmaz.
     * - Elmas yalnızca ilk geçişte verilir; ilk 3 yıldızda +1 bonus.
     * - Yıldız ve en iyi skor hiçbir zaman düşmez.
     */
    suspend fun saveLevelResult(
        gameId: String,
        level: Int,
        results: List<RoundResult>,
        durationSeconds: Long
    ): LevelOutcome {
        val meta = GameRegistry.getGameById(gameId)
        val stars = LevelRules.starsFor(results)
        val score = results.sumOf { it.points }
        val before = currentProgress(gameId)
        val existing = levelDao.getLevel(gameId, level)
        val prevStars = existing?.stars ?: 0

        var diamonds = 0
        val firstClear = stars > 0 && existing?.isRewardClaimed != true
        if (firstClear) diamonds += LevelRules.firstClearDiamonds(level)
        val perfectBonus = stars == 3 && existing?.perfectBonusClaimed != true
        if (perfectBonus) diamonds += LevelRules.PERFECT_BONUS_DIAMONDS
        if (diamonds > 0) preferences.addDiamonds(diamonds)

        levelDao.insertOrUpdateLevel(
            LevelProgressEntity(
                id = existing?.id ?: 0,
                gameId = gameId,
                levelNumber = level,
                isUnlocked = true,
                stars = maxOf(prevStars, stars),
                bestScore = maxOf(existing?.bestScore ?: 0, score),
                isRewardClaimed = (existing?.isRewardClaimed == true) || stars > 0,
                completedAt = if (stars > 0) System.currentTimeMillis() else (existing?.completedAt ?: 0L),
                perfectBonusClaimed = (existing?.perfectBonusClaimed == true) || stars == 3,
                attempts = (existing?.attempts ?: 0) + 1
            )
        )

        val after = currentProgress(gameId)
        recordGameSessionAndActivity(gameId, meta?.name ?: gameId, meta?.category?.title ?: "", score, durationSeconds, stars)
        gameDao.updateTotalStars(gameId, after.totalStars)
        gameDao.updateUnlockedLevel(gameId, after.nextPlayableLevel)

        val nextLevel = level + 1
        val unlockedLevel = if (nextLevel <= LevelRules.LEVEL_COUNT && !before.isUnlocked(nextLevel) && after.isUnlocked(nextLevel)) nextLevel else null
        val unlockedZone = (1 until LevelRules.ZONE_COUNT).firstOrNull { !before.isZoneUnlocked(it) && after.isZoneUnlocked(it) }

        return LevelOutcome(
            stars = stars,
            previousStars = prevStars,
            score = score,
            diamonds = diamonds,
            firstClear = firstClear,
            perfectBonus = perfectBonus,
            newBest = score > (existing?.bestScore ?: 0),
            unlockedLevel = unlockedLevel,
            unlockedZone = unlockedZone,
            endlessUnlocked = !before.isEndlessUnlocked && after.isEndlessUnlocked,
            nextUnlocked = nextLevel <= LevelRules.LEVEL_COUNT && after.isUnlocked(nextLevel)
        )
    }

    /** Sonsuz mod sonucunu kaydeder; yeni rekorsa true döner. */
    suspend fun saveEndlessResult(gameId: String, roundsCleared: Int, score: Int, durationSeconds: Long): Boolean {
        val meta = GameRegistry.getGameById(gameId)
        val prev = gameDao.getGameByIdSync(gameId)?.endlessBest ?: 0
        gameDao.updateEndlessBest(gameId, roundsCleared)
        recordGameSessionAndActivity(gameId, meta?.name ?: gameId, meta?.category?.title ?: "", score, durationSeconds, 0)
        return roundsCleared > prev
    }

    private suspend fun recordGameSessionAndActivity(
        gameId: String, gameName: String, category: String, score: Int, durationSeconds: Long, stars: Int
    ) {
        recordDao.insertRecord(
            GameRecordEntity(
                gameId = gameId,
                gameName = gameName,
                category = category,
                score = score,
                durationSeconds = durationSeconds
            )
        )
        gameDao.updateHighestScore(gameId, score)
        gameDao.incrementPlayCount(gameId)
        preferences.recordGamePlayed()
        recordDailyActivity(durationSeconds = durationSeconds, score = score, starsEarned = stars)
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

    /** Tüm seviye ilerlemesini sıfırlar (profil > sıfırla). */
    suspend fun resetAllProgress() {
        levelDao.clearAll()
        gameDao.resetAllGamesProgress()
        clearHistory()
    }
}
