package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.LevelProgressEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LevelProgressDao {
    @Query("SELECT * FROM level_progress WHERE gameId = :gameId ORDER BY levelNumber ASC")
    fun getLevelsForGame(gameId: String): Flow<List<LevelProgressEntity>>

    @Query("SELECT * FROM level_progress WHERE gameId = :gameId AND levelNumber = :levelNumber LIMIT 1")
    suspend fun getLevel(gameId: String, levelNumber: Int): LevelProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateLevel(level: LevelProgressEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDefaultLevels(levels: List<LevelProgressEntity>)

    @Query("SELECT SUM(stars) FROM level_progress WHERE gameId = :gameId")
    fun getGameStars(gameId: String): Flow<Int?>

    @Query("SELECT SUM(stars) FROM level_progress")
    fun getTotalStars(): Flow<Int?>

    @Query("SELECT COUNT(*) FROM level_progress WHERE isRewardClaimed = 1")
    fun getCompletedLevelsCount(): Flow<Int>

    @Query("UPDATE level_progress SET isUnlocked = 1 WHERE gameId = :gameId AND levelNumber = :levelNumber")
    suspend fun unlockLevel(gameId: String, levelNumber: Int)

    @Query("UPDATE level_progress SET isRewardClaimed = 1 WHERE gameId = :gameId AND levelNumber = :levelNumber")
    suspend fun markRewardClaimed(gameId: String, levelNumber: Int)
}
