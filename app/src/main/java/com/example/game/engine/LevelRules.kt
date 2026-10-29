package com.example.game.engine

import kotlin.math.ceil

/**
 * ZihinPark seviye kuralları (tüm oyunlar için ortak).
 *
 * - Her oyunda 50 seviye, 10'arlı 5 bölge.
 * - Seviye 1-10: 3 tur, 11-30: 4 tur, 31-50: 5 tur -> oyun başına 210 benzersiz tur.
 * - Her bölgenin 10. seviyesi boss seviyesidir.
 * - Yeni bölge için önceki bölgede toplam [ZONE_UNLOCK_STARS] yıldız gerekir.
 * - 50. seviye geçilince Sonsuz Mod açılır.
 */
object LevelRules {
    const val LEVEL_COUNT = 50
    const val ZONE_SIZE = 10
    const val ZONE_COUNT = LEVEL_COUNT / ZONE_SIZE
    const val ZONE_UNLOCK_STARS = 20
    const val ENDLESS_LEVEL = 0

    val ZONE_NAMES = listOf("Kolay", "Orta", "Zor", "Uzman", "Usta")
    val ZONE_EMOJI = listOf("🟢", "🔵", "🟣", "🔴", "🟡")

    fun zoneOf(level: Int): Int = ((level.coerceIn(1, LEVEL_COUNT) - 1) / ZONE_SIZE)

    fun roundsFor(level: Int): Int = when {
        level == ENDLESS_LEVEL -> Int.MAX_VALUE
        level <= 10 -> 3
        level <= 30 -> 4
        else -> 5
    }

    fun isBoss(level: Int): Boolean = level > 0 && level % ZONE_SIZE == 0

    /** Bir oyundaki toplam tur sayısı (210). */
    val TOTAL_ROUNDS: Int = (1..LEVEL_COUNT).sumOf { roundsFor(it) }

    /** Seviye ve tur numarasından oyunun 0..209 aralığındaki benzersiz tur sırasını verir. */
    fun globalRoundIndex(level: Int, round: Int): Int {
        require(level in 1..LEVEL_COUNT) { "level $level" }
        require(round in 0 until roundsFor(level)) { "round $round" }
        var idx = 0
        for (l in 1 until level) idx += roundsFor(l)
        return idx + round
    }

    /**
     * 0.0 .. 1.0 arası zorluk. Bölgelerle hizalıdır: z. bölge [z/5, (z+1)/5) aralığını kullanır,
     * böylece üreticilerdeki `step(t, a, b, c, d, e)` doğrudan bölgeye karşılık gelir.
     * Bölge içinde seviye ve tur ilerledikçe değer artar; boss seviyeleri bölgenin en zorudur.
     */
    fun difficulty(level: Int, round: Int): Float {
        if (level == ENDLESS_LEVEL) return 0.9999f
        val zone = zoneOf(level)
        val first = zoneFirstLevel(zone)
        val zoneStart = globalRoundIndex(first, 0)
        val zoneRounds = (first until first + ZONE_SIZE).sumOf { roundsFor(it) }
        val within = (globalRoundIndex(level, round) - zoneStart).toFloat() / zoneRounds
        return ((zone + within) / ZONE_COUNT).coerceIn(0f, 0.9999f)
    }

    /** Sonsuz modda n. turun zorluğu: 0.75'ten başlayıp yavaşça 1.0'a çıkar. */
    fun endlessDifficulty(n: Int): Float = (0.75f + n * 0.01f).coerceAtMost(0.9999f)

    /** Seviyeyi geçmek için gereken en az başarılı tur. */
    fun passThreshold(rounds: Int): Int = ceil(rounds * 0.6).toInt()

    /**
     * Yıldız hesabı.
     * 0 = geçilemedi, 1 = geçti, 2 = en fazla 1 tur kaçırdı, 3 = hepsi başarılı ve ortalama kalite >= 80.
     */
    fun starsFor(results: List<RoundResult>): Int {
        if (results.isEmpty()) return 0
        val n = results.size
        val ok = results.count { it.success }
        if (ok < passThreshold(n)) return 0
        val avgQuality = results.map { it.quality }.average()
        return when {
            ok == n && avgQuality >= 80 -> 3
            ok >= n - 1 -> 2
            else -> 1
        }
    }

    /** İlk tamamlamada verilen elmas. */
    fun firstClearDiamonds(level: Int): Int = when {
        isBoss(level) -> 10
        level <= 20 -> 2
        else -> 3
    }

    const val PERFECT_BONUS_DIAMONDS = 1

    /** Bölgedeki ilk seviyenin numarası. */
    fun zoneFirstLevel(zone: Int): Int = zone * ZONE_SIZE + 1
}

/** Tek bir turun sonucu. quality: 0..100 (hız, hata ve hamle verimliliği). */
data class RoundResult(
    val success: Boolean,
    val quality: Int,
    val points: Int
) {
    companion object {
        fun fail(points: Int = 0) = RoundResult(false, 0, points)
    }
}

/** Bir oyunun seviye ilerlemesi (seviye -> yıldız). */
data class GameProgress(val starsByLevel: Map<Int, Int>, val endlessBest: Int = 0) {

    fun stars(level: Int): Int = starsByLevel[level] ?: 0

    fun zoneStars(zone: Int): Int {
        val first = LevelRules.zoneFirstLevel(zone)
        return (first until first + LevelRules.ZONE_SIZE).sumOf { stars(it) }
    }

    val totalStars: Int get() = starsByLevel.values.sum()

    val clearedCount: Int get() = starsByLevel.count { it.value > 0 }

    /** Bölge kilidi: önceki bölgenin boss'u geçilmiş ve bölgede yeterli yıldız toplanmış olmalı. */
    fun isZoneUnlocked(zone: Int): Boolean {
        if (zone <= 0) return true
        val prevBoss = LevelRules.zoneFirstLevel(zone) - 1
        return stars(prevBoss) > 0 && zoneStars(zone - 1) >= LevelRules.ZONE_UNLOCK_STARS &&
            isZoneUnlocked(zone - 1)
    }

    fun isUnlocked(level: Int): Boolean {
        if (level == LevelRules.ENDLESS_LEVEL) return isEndlessUnlocked
        if (level < 1 || level > LevelRules.LEVEL_COUNT) return false
        if (level == 1) return true
        val zone = LevelRules.zoneOf(level)
        if (!isZoneUnlocked(zone)) return false
        if (level == LevelRules.zoneFirstLevel(zone)) return true
        return stars(level - 1) > 0
    }

    val isEndlessUnlocked: Boolean get() = stars(LevelRules.LEVEL_COUNT) > 0

    /** Oynanabilecek en ileri seviye (devam et butonu için). */
    val nextPlayableLevel: Int
        get() {
            var best = 1
            for (l in 1..LevelRules.LEVEL_COUNT) {
                if (isUnlocked(l)) best = l
            }
            // Açık ama henüz geçilmemiş en düşük seviyeyi tercih et
            val firstUncleared = (1..LevelRules.LEVEL_COUNT).firstOrNull { isUnlocked(it) && stars(it) == 0 }
            return firstUncleared ?: best
        }
}
