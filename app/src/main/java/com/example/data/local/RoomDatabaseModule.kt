package com.example.data.local

import android.content.Context
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.GameDao
import com.example.data.local.dao.GameRecordDao
import com.example.data.local.dao.LevelProgressDao
import com.example.data.local.entity.GameEntity
import com.example.data.local.entity.LevelProgressEntity
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ZihinPark Room Database modülü.
 * Uygulama ilk kez başlatıldığında 16 temel beyin egzersizi, mantık, hafıza
 * ve dikkat oyununu Room veritabanına otomatik olarak yükler ve yönetir.
 */
object RoomDatabaseModule {

    /**
     * Uygulamanın 16 temel oyunu:
     * - Mantık & Çıkarım (Logic)
     * - Uzamsal Zekâ (Spatial)
     * - Sayısal & Analitik (Math)
     * - Dikkat & Gözlem (Attention)
     * - Hafıza (Memory)
     * - Strateji & Hız (Strategy & Reflex)
     */
    val BASE_16_GAMES: List<GameEntity> = listOf(
        // 1. Mantık Dedektifi (Mantık & Çıkarım)
        GameEntity(
            id = "logic_detective",
            name = "Mantık Dedektifi",
            category = "Mantık & Çıkarım",
            difficulty = "Orta",
            highestScore = 0,
            totalPlays = 0,
            unlockedLevel = 1,
            totalStars = 0,
            iconEmoji = "🕵️‍♂️",
            description = "İpuçlarını incele, şüphelileri analiz et ve suçluyu mantıksal çıkarımla bul.",
            maxScore = 100
        ),
        // 2. Zihin Döndürme (Uzamsal Zekâ)
        GameEntity(
            id = "spatial_rotation",
            name = "Zihin Döndürme",
            category = "Uzamsal Zekâ",
            difficulty = "Orta",
            highestScore = 0,
            totalPlays = 0,
            unlockedLevel = 1,
            totalStars = 0,
            iconEmoji = "🔄",
            description = "Şekli zihninde belirtilen açıyla döndür ve doğru yansımayı yakala.",
            maxScore = 100
        ),
        // 3. Gizli Resim (Görsel Mantık / Nonogram)
        GameEntity(
            id = "nonogram",
            name = "Gizli Resim (Nonogram)",
            category = "Uzamsal Zekâ",
            difficulty = "Zor",
            highestScore = 0,
            totalPlays = 0,
            unlockedLevel = 1,
            totalStars = 0,
            iconEmoji = "🖼️",
            description = "Sayı ipuçlarını takip ederek ızgarayı doldur, gizli resmi ortaya çıkar.",
            maxScore = 150
        ),
        // 4. Şifre Kırıcı (Şifre & Analitik)
        GameEntity(
            id = "cipher_breaker",
            name = "Şifre Kırıcı",
            category = "Mantık & Çıkarım",
            difficulty = "Orta",
            highestScore = 0,
            totalPlays = 0,
            unlockedLevel = 1,
            totalStars = 0,
            iconEmoji = "🔐",
            description = "Sezar kaydırma algoritmasıyla şifrelenmiş gizli kelimeleri çöz.",
            maxScore = 120
        ),
        // 5. İmkânsız Görevler (Strateji & Problem Çözme)
        GameEntity(
            id = "river_crossing",
            name = "İmkânsız Görevler",
            category = "Strateji & Hız",
            difficulty = "Zor",
            highestScore = 0,
            totalPlays = 0,
            unlockedLevel = 1,
            totalStars = 0,
            iconEmoji = "🛶",
            description = "Kurt, koyun ve otu kimse birbirini yemeden karşı kıyıya güvenle geçir.",
            maxScore = 150
        ),
        // 6. Şekil Mimarı (Uzamsal Zekâ / Tangram)
        GameEntity(
            id = "shape_architect",
            name = "Şekil Mimarı",
            category = "Uzamsal Zekâ",
            difficulty = "Orta",
            highestScore = 0,
            totalPlays = 0,
            unlockedLevel = 1,
            totalStars = 0,
            iconEmoji = "📐",
            description = "Geometrik parçaları döndür ve hedef silueti eksiksiz tamamla.",
            maxScore = 100
        ),
        // 7. Tersine Düşün (Yaratıcı & Yanal Düşünme)
        GameEntity(
            id = "reverse_thinking",
            name = "Tersine Düşün",
            category = "Mantık & Çıkarım",
            difficulty = "Kolay",
            highestScore = 0,
            totalPlays = 0,
            unlockedLevel = 1,
            totalStars = 0,
            iconEmoji = "💡",
            description = "Alışılmışın dışına çık, paradoksları ve yanal düşünme sorularını yanıtla.",
            maxScore = 100
        ),
        // 8. Sayı Mühendisi (Sayısal Düşünme)
        GameEntity(
            id = "math_engineer",
            name = "Sayı Mühendisi",
            category = "Sayısal & Analitik",
            difficulty = "Zor",
            highestScore = 0,
            totalPlays = 0,
            unlockedLevel = 1,
            totalStars = 0,
            iconEmoji = "🧮",
            description = "Kesişen denklem matrisinde eksik işlem ve sayıları hesaplayarak doldur.",
            maxScore = 120
        ),
        // 9. Detay Avcısı (Dikkat & Gözlem)
        GameEntity(
            id = "detail_hunter",
            name = "Detay Avcısı",
            category = "Dikkat & Gözlem",
            difficulty = "Orta",
            highestScore = 0,
            totalPlays = 0,
            unlockedLevel = 1,
            totalStars = 0,
            iconEmoji = "🔍",
            description = "İki görsel kompozisyon arasındaki gizli farkları gözlemleyerek bul.",
            maxScore = 120
        ),
        // 10. Yolun Sonu (Strateji & Planlama)
        GameEntity(
            id = "path_finder",
            name = "Yolun Sonu",
            category = "Strateji & Hız",
            difficulty = "Orta",
            highestScore = 0,
            totalPlays = 0,
            unlockedLevel = 1,
            totalStars = 0,
            iconEmoji = "🧭",
            description = "Engelleri aş, anahtarı ele geçir, kilitli kapıyı açarak hedefe ulaş.",
            maxScore = 120
        ),
        // 11. Sayı Zinciri (Sayı Örüntüleri)
        GameEntity(
            id = "number_chain",
            name = "Sayı Zinciri",
            category = "Sayısal & Analitik",
            difficulty = "Kolay",
            highestScore = 0,
            totalPlays = 0,
            unlockedLevel = 1,
            totalStars = 0,
            iconEmoji = "🔢",
            description = "Sayı örüntüsündeki gizli matematik kuralını keşfet ve soru işaretini bul.",
            maxScore = 90
        ),
        // 12. Kelime Avı (Dil & Dikkat)
        GameEntity(
            id = "word_hunt",
            name = "Kelime Avı",
            category = "Dikkat & Gözlem",
            difficulty = "Orta",
            highestScore = 0,
            totalPlays = 0,
            unlockedLevel = 1,
            totalStars = 0,
            iconEmoji = "📝",
            description = "Harf matrisi içerisinde gizlenmiş Türkçe kelimeleri seçerek keşfet.",
            maxScore = 150
        ),
        // 13. Hafıza Işıkları (Sıralı Hafıza / Simon Says)
        GameEntity(
            id = "memory_lights",
            name = "Hafıza Işıkları",
            category = "Dikkat & Gözlem",
            difficulty = "Orta",
            highestScore = 0,
            totalPlays = 0,
            unlockedLevel = 1,
            totalStars = 0,
            iconEmoji = "✨",
            description = "Işık ve ton sıralamasını aklında tut, sırayı bozmadan hatasız tekrarla.",
            maxScore = 125
        ),
        // 14. Refleks Arenası (Hız & Refleks)
        GameEntity(
            id = "reflex_arena",
            name = "Refleks Arenası",
            category = "Strateji & Hız",
            difficulty = "Kolay",
            highestScore = 0,
            totalPlays = 0,
            unlockedLevel = 1,
            totalStars = 0,
            iconEmoji = "⚡",
            description = "Işık yeşile döndüğü milisaniyede dokun, tepki hızını test et.",
            maxScore = 100
        ),
        // 15. Renk Kaçışı (Bilişsel Esneklik & Dikkat / Stroop)
        GameEntity(
            id = "color_stroop",
            name = "Renk Kaçışı",
            category = "Dikkat & Gözlem",
            difficulty = "Zor",
            highestScore = 0,
            totalPlays = 0,
            unlockedLevel = 1,
            totalStars = 0,
            iconEmoji = "🎨",
            description = "Yazı rengi ile kelime anlamı çeliştiğinde beyninin hızını ve odağını koru.",
            maxScore = 150
        ),
        // 16. Desen Ustası (Görsel Örüntü)
        GameEntity(
            id = "pattern_master",
            name = "Desen Ustası",
            category = "Uzamsal Zekâ",
            difficulty = "Kolay",
            highestScore = 0,
            totalPlays = 0,
            unlockedLevel = 1,
            totalStars = 0,
            iconEmoji = "🧩",
            description = "Geometrik ve sembolik dönüşüm serisini takip et, sıradaki deseni seç.",
            maxScore = 90
        )
    )

    /**
     * Room Database Singleton erişimi sağlar.
     */
    fun provideDatabase(context: Context): ZihinParkDatabase {
        return ZihinParkDatabase.getInstance(context)
    }

    /**
     * GameDao erişim sağlayıcısı.
     */
    fun provideGameDao(database: ZihinParkDatabase): GameDao {
        return database.gameDao()
    }

    /**
     * LevelProgressDao erişim sağlayıcısı.
     */
    fun provideLevelProgressDao(database: ZihinParkDatabase): LevelProgressDao {
        return database.levelProgressDao()
    }

    /**
     * GameRecordDao erişim sağlayıcısı.
     */
    fun provideGameRecordDao(database: ZihinParkDatabase): GameRecordDao {
        return database.gameRecordDao()
    }

    /**
     * İlk açılışta veritabanında 16 temel oyunun varlığını kontrol eder ve
     * eksik olanları seviye kayıtlarıyla birlikte Room veritabanına ekler.
     */
    suspend fun initializeBaseGames(
        database: ZihinParkDatabase,
        dispatcher: CoroutineDispatcher = Dispatchers.IO
    ) = withContext(dispatcher) {
        val gameDao = database.gameDao()
        val levelDao = database.levelProgressDao()

        // 16 temel oyunu ekle (Mevcut olanlar IGNORE ile korunur)
        gameDao.insertDefaultGames(BASE_16_GAMES)

        // Her oyun için başlangıç seviye ilerleme verilerini hazırla (Seviye 1 açık)
        val initialLevels = BASE_16_GAMES.flatMap { game ->
            (1..20).map { levelNum ->
                LevelProgressEntity(
                    gameId = game.id,
                    levelNumber = levelNum,
                    isUnlocked = levelNum == 1,
                    stars = 0,
                    bestScore = 0,
                    isRewardClaimed = false
                )
            }
        }
        levelDao.insertDefaultLevels(initialLevels)
    }

    /**
     * Room Database Callback: Veritabanı ilk kez oluşturulduğunda
     * 16 temel oyunu otomatik olarak arka planda yükler.
     */
    fun createPreseedCallback(context: Context): RoomDatabase.Callback {
        return object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                CoroutineScope(Dispatchers.IO).launch {
                    val database = provideDatabase(context)
                    initializeBaseGames(database)
                }
            }
        }
    }
}
