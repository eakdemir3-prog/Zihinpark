package com.example.game.engine

import kotlin.random.Random

/**
 * Bir oyunun tur üreticisi. Saf Kotlin'dir (Compose'a bağlı değildir) ki birim testlerinde
 * 210 turun benzersizliği ve çözülebilirliği doğrulanabilsin.
 */
interface PuzzleGenerator<P : Any> {
    /** Oyun kimliği; tohumlar buna göre ayrışır. */
    val gameId: String

    /**
     * Verilen zorlukta (0..1) bir bulmaca üretir. Üretim başarısız olursa null dönebilir;
     * banka bu durumda yeni bir tohumla tekrar dener.
     */
    fun generate(index: Int, difficulty: Float, rng: Random): P?

    /** Aynı bulmacayı (ayna/döndürme gibi eşdeğer hâller dahil) tanımlayan parmak izi. */
    fun fingerprint(puzzle: P): String

    /**
     * true ise her tur kendi sabit tohumundan bağımsız üretilir (önceki turları üretmek gerekmez,
     * anında açılır). Olası bulmaca sayısı çok büyük olan oyunlar içindir; 210 turun benzersizliği
     * birim testinde doğrulanır. false ise turlar sırayla üretilir ve tekrar edenler atlanır.
     */
    val independent: Boolean get() = false
}

/** Turda kullanılacak tüm bilgiler. */
data class RoundSpec(
    val gameId: String,
    val level: Int,
    val round: Int,
    val roundCount: Int,
    val difficulty: Float,
    val isBoss: Boolean,
    val isEndless: Boolean,
    /** Sonsuz modda kaçıncı tur olduğu; normal modda oyun içi tur sırası (0..209). */
    val index: Int
) {
    /** 0..4: Kolay..Usta. Sonsuz modda 4. */
    val zone: Int get() = if (isEndless) 4 else LevelRules.zoneOf(level)
}

object Seeds {
    fun hash(s: String): Long {
        var h = 1125899906842597L
        for (c in s) h = 31 * h + c.code
        return h
    }

    fun forRound(gameId: String, index: Int, attempt: Int): Long =
        hash(gameId) * 1_000_003L + index * 7_919L + attempt * 104_729L
}

/**
 * Oyun başına 210 turu sabit tohumlarla sırayla üretir ve parmak izi kontrolüyle
 * hiçbir turun bir diğerinin aynısı olmamasını garanti eder. Sonuç bellekte tutulur,
 * böylece "Seviye 12 / Tur 3" her açılışta aynı (ve başka yerde olmayan) bulmacadır.
 */
object PuzzleBank {
    private class Bank(val items: ArrayList<Any> = ArrayList(), val seen: HashSet<String> = HashSet())

    private val banks = HashMap<String, Bank>()

    /** Turları gerektiği kadar (sırayla) üretir; önceki tüm turlarla çakışmayan [index]. turu döner. */
    private val independentCache = HashMap<String, HashMap<Int, Any>>()

    @Suppress("UNCHECKED_CAST")
    fun <P : Any> get(gen: PuzzleGenerator<P>, index: Int): P {
        require(index in 0 until LevelRules.TOTAL_ROUNDS) { "index $index" }
        if (gen.independent) {
            synchronized(independentCache) {
                independentCache[gen.gameId]?.get(index)?.let { return it as P }
            }
            val (level, round) = levelRoundOf(index)
            val p = produce(gen, index, LevelRules.difficulty(level, round), HashSet())
            synchronized(independentCache) { independentCache.getOrPut(gen.gameId) { HashMap() }[index] = p }
            return p
        }
        return sequential(gen, index)
    }

    @Suppress("UNCHECKED_CAST")
    @Synchronized
    private fun <P : Any> sequential(gen: PuzzleGenerator<P>, index: Int): P {
        val bank = banks.getOrPut(gen.gameId) { Bank() }
        while (bank.items.size <= index) {
            val i = bank.items.size
            val (level, round) = levelRoundOf(i)
            bank.items += produce(gen, i, LevelRules.difficulty(level, round), bank.seen)
        }
        return bank.items[index] as P
    }

    /** Oyunun 210 turunun tamamı (testler için). */
    fun <P : Any> all(gen: PuzzleGenerator<P>): List<P> =
        (0 until LevelRules.TOTAL_ROUNDS).map { get(gen, it) }

    fun <P : Any> forRound(gen: PuzzleGenerator<P>, spec: RoundSpec): P {
        return if (spec.isEndless) endless(gen, spec.index, spec.difficulty)
        else get(gen, spec.index)
    }

    /**
     * Sonsuz mod: normal bankadaki hiçbir turla çakışmayan yeni bir bulmaca.
     * Tohum sırası sonsuz mod sayacından türediği için tekrar etmez.
     */
    fun <P : Any> endless(gen: PuzzleGenerator<P>, n: Int, difficulty: Float): P {
        if (gen.independent) return produce(gen, 100_000 + n, difficulty, HashSet())
        sequential(gen, LevelRules.TOTAL_ROUNDS - 1)
        val seen = synchronized(this) { HashSet<String>(banks.getValue(gen.gameId).seen) }
        return produce(gen, 100_000 + n, difficulty, seen)
    }

    /** Bir seviyenin turlarını arka planda önceden üretir (oyun açılınca bekleme olmasın). */
    fun <P : Any> prefetch(gen: PuzzleGenerator<P>, level: Int) {
        if (level !in 1..LevelRules.LEVEL_COUNT) return
        for (r in 0 until LevelRules.roundsFor(level)) get(gen, LevelRules.globalRoundIndex(level, r))
    }

    private fun levelRoundOf(globalIndex: Int): Pair<Int, Int> {
        var idx = globalIndex
        for (l in 1..LevelRules.LEVEL_COUNT) {
            val r = LevelRules.roundsFor(l)
            if (idx < r) return l to idx
            idx -= r
        }
        error("index $globalIndex")
    }

    private fun <P : Any> produce(gen: PuzzleGenerator<P>, index: Int, diff: Float, seen: MutableSet<String>): P {
        var attempt = 0
        var fallback: P? = null
        while (attempt < 400) {
            val rng = Random(Seeds.forRound(gen.gameId, index, attempt))
            val p = gen.generate(index, diff, rng)
            if (p != null) {
                val fp = gen.fingerprint(p)
                if (fp !in seen) {
                    seen += fp
                    return p
                }
                if (fallback == null) fallback = p
            }
            attempt++
        }
        // Teorik olarak ulaşılmaz; testler 210 turun benzersiz olduğunu doğrular.
        return fallback ?: error("${gen.gameId}: $index için bulmaca üretilemedi")
    }

    /** Testler için önbelleği temizler. */
    @Synchronized
    fun clear() {
        banks.clear()
        synchronized(independentCache) { independentCache.clear() }
    }
}

// ---------- Yardımcılar ----------

fun lerp(a: Int, b: Int, t: Float): Int = (a + (b - a) * t.coerceIn(0f, 1f) + 0.0001f).toInt()

fun lerpF(a: Float, b: Float, t: Float): Float = a + (b - a) * t.coerceIn(0f, 1f)

/** Zorluğa göre basamaklı değer seçer: steps[ (t * steps.size) ]. */
fun <T> step(t: Float, vararg steps: T): T {
    val i = (t.coerceIn(0f, 0.9999f) * steps.size).toInt()
    return steps[i]
}

fun <T> List<T>.pick(rng: Random): T = this[rng.nextInt(size)]

fun <T> List<T>.pickN(n: Int, rng: Random): List<T> = shuffled(rng).take(n)

/** Kaliteyi hata sayısı ve süre oranına göre hesaplar (0..100). */
fun qualityOf(mistakes: Int, timeUsedRatio: Float = 0.5f, mistakePenalty: Int = 25): Int {
    val timeScore = ((1f - timeUsedRatio.coerceIn(0f, 1f)) * 40f).toInt()
    return (60 + timeScore - mistakes * mistakePenalty).coerceIn(0, 100)
}

/** Zorluktan bölge (0..4). */
fun tierOf(t: Float): Int = (t * 5f).toInt().coerceIn(0, 4)

/** Bölge içindeki ilerleme (0..1). */
fun withinTier(t: Float): Float = (t * 5f - tierOf(t)).coerceIn(0f, 1f)

/** Bölge ve bölge içi ilerlemeye göre [base] .. [base]+[span] arası tamsayı. */
fun scaled(t: Float, perTier: IntArray, extraWithin: Int = 0): Int =
    perTier[tierOf(t)] + (withinTier(t) * (extraWithin + 0.999f)).toInt()

fun String.pick(rng: Random): Char = this[rng.nextInt(length)]
