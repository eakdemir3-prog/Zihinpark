package com.example.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Oyun verilerini temsil eden Room @Entity veri sınıfı.
 * 
 * Temel alanlar:
 * - [id]: Benzersiz oyun kimliği (Primary Key)
 * - [name]: Oyunun adı (İsim)
 * - [category]: Oyunun kategorisi (Mantık, Uzamsal, Sayısal, Dikkat, Strateji)
 * - [difficulty]: Zorluk seviyesi (Kolay, Orta, Zor)
 * - [highestScore]: Oyuncunun elde ettiği en yüksek skor
 */
@Entity(tableName = "games")
data class GameEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "category")
    val category: String,

    @ColumnInfo(name = "difficulty")
    val difficulty: String,

    @ColumnInfo(name = "highestScore")
    val highestScore: Int = 0,

    @ColumnInfo(name = "totalPlays")
    val totalPlays: Int = 0,

    @ColumnInfo(name = "unlockedLevel")
    val unlockedLevel: Int = 1,

    @ColumnInfo(name = "totalStars")
    val totalStars: Int = 0,

    @ColumnInfo(name = "iconEmoji")
    val iconEmoji: String = "🎮",

    @ColumnInfo(name = "description")
    val description: String = "",

    @ColumnInfo(name = "maxScore")
    val maxScore: Int = 100,

    /** Sonsuz moddaki en iyi tur sayısı. */
    @ColumnInfo(name = "endlessBest", defaultValue = "0")
    val endlessBest: Int = 0
)
