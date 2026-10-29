package com.example.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Bir oyunun tek bir seviyesindeki ilerleme. Satır yalnızca seviye en az bir kez
 * oynandığında oluşur; kilit durumu yıldızlardan hesaplanır (bkz. GameProgress).
 */
@Entity(
    tableName = "level_progress",
    indices = [Index(value = ["gameId", "levelNumber"], unique = true)]
)
data class LevelProgressEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val gameId: String,
    val levelNumber: Int,
    val isUnlocked: Boolean = false,
    val stars: Int = 0,
    val bestScore: Int = 0,
    /** İlk tamamlama elması verildi mi (tekrar oynayınca verilmez). */
    val isRewardClaimed: Boolean = false,
    val completedAt: Long = 0L,
    /** İlk kez 3 yıldız bonusu verildi mi. */
    @ColumnInfo(defaultValue = "0")
    val perfectBonusClaimed: Boolean = false,
    @ColumnInfo(defaultValue = "0")
    val attempts: Int = 0
)
