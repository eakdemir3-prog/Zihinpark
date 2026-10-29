package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Kullanıcının günlük oyun aktivitesini, toplam oyun oynama süresini
 * ve kazanılan başarıları takip eden Room Entity tablosu.
 *
 * @param date ISO-8601 gün formatı (YYYY-MM-DD), birincil anahtar.
 * @param totalPlayTimeSeconds O gün toplam oynanan süre (saniye cinsinden).
 * @param gamesPlayedCount O gün tamamlanan oyun sayısı.
 * @param totalScoreEarned O gün kazanılan toplam zihin puanı.
 * @param starsEarned O gün toplanan yıldız sayısı.
 * @param lastActiveTimestamp En son yapılan aktivitenin zaman damgası (milisaniye).
 */
@Entity(tableName = "user_daily_activity")
data class UserDailyActivityEntity(
    @PrimaryKey
    val date: String,
    val totalPlayTimeSeconds: Long = 0L,
    val gamesPlayedCount: Int = 0,
    val totalScoreEarned: Int = 0,
    val starsEarned: Int = 0,
    val lastActiveTimestamp: Long = System.currentTimeMillis()
)
