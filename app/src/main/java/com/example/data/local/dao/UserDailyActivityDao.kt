package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.UserDailyActivityEntity
import kotlinx.coroutines.flow.Flow

/**
 * Kullanıcı günlük aktiviteleri ve toplam oyun oynama süresi için Room Data Access Object (DAO).
 */
@Dao
interface UserDailyActivityDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateActivity(activity: UserDailyActivityEntity)

    @Query("SELECT * FROM user_daily_activity WHERE date = :date LIMIT 1")
    fun getActivityByDate(date: String): Flow<UserDailyActivityEntity?>

    @Query("SELECT * FROM user_daily_activity WHERE date = :date LIMIT 1")
    suspend fun getActivityDirect(date: String): UserDailyActivityEntity?

    @Query("SELECT * FROM user_daily_activity ORDER BY date DESC")
    fun getAllActivities(): Flow<List<UserDailyActivityEntity>>

    @Query("SELECT * FROM user_daily_activity ORDER BY date DESC LIMIT :limit")
    fun getRecentActivities(limit: Int): Flow<List<UserDailyActivityEntity>>

    @Query("SELECT SUM(totalPlayTimeSeconds) FROM user_daily_activity")
    fun getTotalPlayTimeSeconds(): Flow<Long?>

    @Query("SELECT SUM(gamesPlayedCount) FROM user_daily_activity")
    fun getTotalGamesPlayedCount(): Flow<Int?>

    @Query("SELECT COUNT(*) FROM user_daily_activity WHERE totalPlayTimeSeconds > 0")
    fun getActiveDaysCount(): Flow<Int>

    @Query("SELECT SUM(totalScoreEarned) FROM user_daily_activity")
    fun getTotalScoreEarned(): Flow<Int?>

    @Query("SELECT SUM(starsEarned) FROM user_daily_activity")
    fun getTotalStarsEarned(): Flow<Int?>

    @Query("DELETE FROM user_daily_activity")
    suspend fun clearAllDailyActivities()
}
