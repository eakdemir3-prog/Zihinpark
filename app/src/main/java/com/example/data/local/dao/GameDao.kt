package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.local.entity.GameEntity
import kotlinx.coroutines.flow.Flow

/**
 * GameEntity için oyun verilerini kaydetme, güncelleme, silme ve
 * çeşitli kriterlere göre sorgulama metodlarını içeren Room DAO arayüzü.
 */
@Dao
interface GameDao {

    // ==========================================
    // 1. KAYDETME & EKLEME (INSERT & UPSERT)
    // ==========================================

    /**
     * Tek bir oyunu veritabanına ekler. Çakışma durumunda mevcut veriyi günceller.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateGame(game: GameEntity): Long

    /**
     * Birden fazla oyunu toplu olarak ekler veya günceller.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGames(games: List<GameEntity>)

    /**
     * Başlangıç (varsayılan) oyunlarını ekler. Eğer oyun zaten varsa dokunmaz (IGNORE).
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDefaultGames(games: List<GameEntity>)

    // ==========================================
    // 2. GÜNCELLEME (UPDATE)
    // ==========================================

    /**
     * Oyun kaydını bütünüyle günceller.
     */
    @Update
    suspend fun updateGame(game: GameEntity): Int

    /**
     * Sadece en yüksek skoru günceller; yeni skor eskisinden büyükse güncellenir.
     */
    @Query("UPDATE games SET highestScore = :newScore WHERE id = :gameId AND :newScore > highestScore")
    suspend fun updateHighestScore(gameId: String, newScore: Int): Int

    /**
     * Oyunun toplam oynanma sayısını 1 artırır.
     */
    @Query("UPDATE games SET totalPlays = totalPlays + 1 WHERE id = :gameId")
    suspend fun incrementPlayCount(gameId: String): Int

    /**
     * Oyunun açık olan seviyesini günceller; yeni seviye eskisinden büyükse güncellenir.
     */
    @Query("UPDATE games SET unlockedLevel = :newLevel WHERE id = :gameId AND :newLevel > unlockedLevel")
    suspend fun updateUnlockedLevel(gameId: String, newLevel: Int): Int

    /**
     * Oyunun toplam yıldız sayısını günceller.
     */
    @Query("UPDATE games SET totalStars = :stars WHERE id = :gameId")
    suspend fun updateTotalStars(gameId: String, stars: Int): Int

    /**
     * Oyun tamamlandığında skoru, oynanma sayısını, yıldızları ve seviyeyi tek işlemde günceller.
     */
    @Transaction
    suspend fun recordGameSession(gameId: String, score: Int, stars: Int, nextLevel: Int) {
        updateHighestScore(gameId, score)
        incrementPlayCount(gameId)
        updateUnlockedLevel(gameId, nextLevel)
        updateTotalStars(gameId, stars)
    }

    /**
     * Belirli bir oyunun ilerlemesini (skor, seviye, yıldız) sıfırlar.
     */
    @Query("UPDATE games SET highestScore = 0, totalPlays = 0, unlockedLevel = 1, totalStars = 0 WHERE id = :gameId")
    suspend fun resetGameProgress(gameId: String): Int

    /**
     * Tüm oyunların ilerleme verilerini sıfırlar.
     */
    @Query("UPDATE games SET highestScore = 0, totalPlays = 0, unlockedLevel = 1, totalStars = 0")
    suspend fun resetAllGamesProgress(): Int

    // ==========================================
    // 3. SİLME (DELETE)
    // ==========================================

    /**
     * Belirli bir oyunu veritabanından siler.
     */
    @Delete
    suspend fun deleteGame(game: GameEntity): Int

    /**
     * ID'si verilen oyunu siler.
     */
    @Query("DELETE FROM games WHERE id = :gameId")
    suspend fun deleteGameById(gameId: String): Int

    /**
     * Tüm oyunları tablodan siler.
     */
    @Query("DELETE FROM games")
    suspend fun deleteAllGames(): Int

    // ==========================================
    // 4. SORGULAMA METODLARI (SELECT & SEARCH)
    // ==========================================

    /**
     * Tüm oyunları alfabetik isim sırasına göre reaktif Flow olarak getirir.
     */
    @Query("SELECT * FROM games ORDER BY name ASC")
    fun getAllGames(): Flow<List<GameEntity>>

    /**
     * Tüm oyunları senkronize (one-shot) liste olarak getirir.
     */
    @Query("SELECT * FROM games ORDER BY name ASC")
    suspend fun getAllGamesSync(): List<GameEntity>

    /**
     * ID ile tek bir oyunu Flow olarak sorgular.
     */
    @Query("SELECT * FROM games WHERE id = :gameId LIMIT 1")
    fun getGameById(gameId: String): Flow<GameEntity?>

    /**
     * ID ile tek bir oyunu senkronize olarak getirir.
     */
    @Query("SELECT * FROM games WHERE id = :gameId LIMIT 1")
    suspend fun getGameByIdSync(gameId: String): GameEntity?

    /**
     * Belirli bir kategorideki oyunları getirir.
     */
    @Query("SELECT * FROM games WHERE category = :category ORDER BY name ASC")
    fun getGamesByCategory(category: String): Flow<List<GameEntity>>

    /**
     * Belirli bir zorluk seviyesindeki oyunları getirir (Kolay, Orta, Zor).
     */
    @Query("SELECT * FROM games WHERE difficulty = :difficulty ORDER BY name ASC")
    fun getGamesByDifficulty(difficulty: String): Flow<List<GameEntity>>

    /**
     * İsim veya açıklamada arama sorgusu yapar.
     */
    @Query("SELECT * FROM games WHERE name LIKE '%' || :searchQuery || '%' OR description LIKE '%' || :searchQuery || '%' ORDER BY name ASC")
    fun searchGames(searchQuery: String): Flow<List<GameEntity>>

    /**
     * En yüksek skora sahip oyunları sıralı getirir.
     */
    @Query("SELECT * FROM games ORDER BY highestScore DESC LIMIT :limit")
    fun getTopScoringGames(limit: Int = 5): Flow<List<GameEntity>>

    /**
     * En çok oynanan oyunları sıralı getirir.
     */
    @Query("SELECT * FROM games ORDER BY totalPlays DESC LIMIT :limit")
    fun getMostPlayedGames(limit: Int = 5): Flow<List<GameEntity>>

    /**
     * Belirli bir seviyeden daha yüksek seviyeye ulaşılmış oyunları listeler.
     */
    @Query("SELECT * FROM games WHERE unlockedLevel >= :minLevel ORDER BY unlockedLevel DESC")
    fun getGamesUnlockedAbove(minLevel: Int): Flow<List<GameEntity>>

    // ==========================================
    // 5. İSTATİSTİK VE ÖZET SORGULARI (AGGREGATE)
    // ==========================================

    /**
     * Toplam kayıtlı oyun sayısını döner.
     */
    @Query("SELECT COUNT(*) FROM games")
    suspend fun getGamesCount(): Int

    /**
     * Tüm oyunların en yüksek skorlarının toplamını döner.
     */
    @Query("SELECT SUM(highestScore) FROM games")
    fun getTotalHighestScore(): Flow<Int?>

    /**
     * Tüm oyunlarda toplam oynanma sayısını döner.
     */
    @Query("SELECT SUM(totalPlays) FROM games")
    fun getTotalPlaysAllGames(): Flow<Int?>

    /**
     * Tüm oyunlarda toplanan toplam yıldız sayısını döner.
     */
    @Query("SELECT SUM(totalStars) FROM games")
    fun getTotalStarsAllGames(): Flow<Int?>

    /**
     * Sistemde var olan benzersiz kategorileri listeler.
     */
    @Query("SELECT DISTINCT category FROM games ORDER BY category ASC")
    fun getDistinctCategories(): Flow<List<String>>
}
