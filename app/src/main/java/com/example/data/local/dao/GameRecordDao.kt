package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.GameRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GameRecordDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: GameRecordEntity): Long

    @Query("SELECT * FROM game_records ORDER BY timestamp DESC")
    fun getAllRecords(): Flow<List<GameRecordEntity>>

    @Query("SELECT * FROM game_records ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentRecords(limit: Int): Flow<List<GameRecordEntity>>

    @Query("SELECT MAX(score) FROM game_records WHERE gameId = :gameId")
    fun getHighScore(gameId: String): Flow<Int?>

    @Query("SELECT COUNT(DISTINCT gameId) FROM game_records")
    fun getPlayedGamesCount(): Flow<Int>

    @Query("SELECT SUM(score) FROM game_records")
    fun getTotalScore(): Flow<Int?>

    @Query("SELECT COUNT(*) FROM game_records")
    fun getTotalPlayedCount(): Flow<Int>

    @Query("DELETE FROM game_records")
    suspend fun clearAllRecords()
}
