package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "game_records")
data class GameRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val gameId: String,
    val gameName: String,
    val category: String,
    val score: Int,
    val durationSeconds: Long,
    val timestamp: Long = System.currentTimeMillis()
)
