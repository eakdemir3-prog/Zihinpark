package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

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
    val isRewardClaimed: Boolean = false,
    val completedAt: Long = 0L
)
