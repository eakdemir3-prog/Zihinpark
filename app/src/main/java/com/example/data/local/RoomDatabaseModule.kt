package com.example.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.entity.GameEntity
import com.example.domain.model.GameRegistry
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Oyun tablosunu GameRegistry'deki oyunlarla eşitler ve şema geçişlerini tutar.
 * Seviye satırları önceden oluşturulmaz; oyuncu bir seviyeyi oynadıkça eklenir.
 */
object RoomDatabaseModule {

    val ALL_GAME_ENTITIES: List<GameEntity>
        get() = GameRegistry.ALL_GAMES.map { meta ->
            GameEntity(
                id = meta.id,
                name = meta.name,
                category = meta.category.title,
                difficulty = meta.difficulty.label,
                iconEmoji = meta.iconEmoji,
                description = meta.description
            )
        }

    suspend fun initializeBaseGames(
        database: ZihinParkDatabase,
        dispatcher: CoroutineDispatcher = Dispatchers.IO
    ) = withContext(dispatcher) {
        val gameDao = database.gameDao()
        gameDao.insertDefaultGames(ALL_GAME_ENTITIES)
        // İsim/açıklama/ikon güncellemelerini mevcut kayıtlara da yansıt (ilerlemeye dokunmadan)
        GameRegistry.ALL_GAMES.forEach { meta ->
            gameDao.updateMeta(meta.id, meta.name, meta.category.title, meta.difficulty.label, meta.iconEmoji, meta.description)
        }
    }

    /** v3 -> v4: seviye satırlarına mükemmel bonus/deneme, oyunlara sonsuz mod rekoru eklendi. */
    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE level_progress ADD COLUMN perfectBonusClaimed INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE level_progress ADD COLUMN attempts INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE games ADD COLUMN endlessBest INTEGER NOT NULL DEFAULT 0")
            // Eski sürümün önceden oluşturduğu boş seviye satırlarını temizle
            db.execSQL("DELETE FROM level_progress WHERE stars = 0 AND bestScore = 0")
        }
    }
}
