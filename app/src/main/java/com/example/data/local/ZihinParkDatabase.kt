package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.GameDao
import com.example.data.local.dao.GameRecordDao
import com.example.data.local.dao.LevelProgressDao
import com.example.data.local.dao.UserDailyActivityDao
import com.example.data.local.entity.GameEntity
import com.example.data.local.entity.GameRecordEntity
import com.example.data.local.entity.LevelProgressEntity
import com.example.data.local.entity.UserDailyActivityEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * ZihinPark ana Room veritabanı sınıfı.
 * Oyunlar (GameEntity), Seviye İlerlemeleri (LevelProgressEntity),
 * Oyun Geçmişi (GameRecordEntity) ve Günlük Aktivite/Oynama Süresi (UserDailyActivityEntity)
 * tablolarını yönetir.
 */
@Database(
    entities = [
        GameEntity::class,
        LevelProgressEntity::class,
        GameRecordEntity::class,
        UserDailyActivityEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class ZihinParkDatabase : RoomDatabase() {

    // DAOs
    abstract fun gameDao(): GameDao
    abstract fun levelProgressDao(): LevelProgressDao
    abstract fun gameRecordDao(): GameRecordDao
    abstract fun userDailyActivityDao(): UserDailyActivityDao

    companion object {
        private const val DATABASE_NAME = "zihinpark_database.db"

        @Volatile
        private var INSTANCE: ZihinParkDatabase? = null

        /**
         * Singleton pattern ile ZihinParkDatabase örneğini döner.
         * Thread-safe (çift kilitli senkronizasyon).
         * Oyun tablosunu her açılışta GameRegistry ile eşitler.
         */
        fun getInstance(context: Context): ZihinParkDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ZihinParkDatabase::class.java,
                    DATABASE_NAME
                )
                    .addMigrations(RoomDatabaseModule.MIGRATION_3_4)
                    // Yalnızca bilinmeyen eski şemalarda sıfırla; v3 ve sonrası ilerlemeyi korur.
                    .fallbackToDestructiveMigrationFrom(true, 1, 2)
                    .fallbackToDestructiveMigrationOnDowngrade(true)
                    .build()

                INSTANCE = instance

                // Veritabanı önceden var olsa bile eksik temel oyunları kontrol edip başlat
                CoroutineScope(Dispatchers.IO).launch {
                    RoomDatabaseModule.initializeBaseGames(instance)
                }

                instance
            }
        }
    }
}
