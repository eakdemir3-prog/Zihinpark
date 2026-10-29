package com.example.game.puzzles

import com.example.game.engine.PuzzleGenerator
import com.example.game.engine.pick
import com.example.game.engine.scaled
import com.example.game.engine.step
import com.example.game.engine.tierOf
import kotlin.random.Random

// =====================================================================
// Şifre Kırıcı
// =====================================================================

data class CipherPuzzle(
    val plain: String,
    val cipher: String,
    val shift: Int,
    val category: String,
    val showShift: Boolean,
    /** Çark çevrilirken çözülmüş metnin canlı gösterilmesi (kolay bölgeler). */
    val liveDecode: Boolean,
    /** Cevap için karışık harf taşları (cevabın harfleri + çeldiriciler). */
    val tiles: List<Char>
)

object CipherGenerator : PuzzleGenerator<CipherPuzzle> {
    override val gameId = "cipher_breaker"

    fun shiftText(text: String, k: Int): String = text.map { ch ->
        val i = WordData.ALPHABET.indexOf(ch)
        if (i < 0) ch else WordData.ALPHABET[((i + k) % WordData.ALPHABET.length + WordData.ALPHABET.length) % WordData.ALPHABET.length]
    }.joinToString("")

    override fun generate(index: Int, difficulty: Float, rng: Random): CipherPuzzle? {
        val tier = tierOf(difficulty)
        val (plain, category) = when (tier) {
            0 -> WordData.ALL.filter { it.first.length in 3..5 }.pick(rng)
            1 -> WordData.ALL.filter { it.first.length in 4..6 }.pick(rng)
            2 -> WordData.ALL.filter { it.first.length in 5..7 }.pick(rng)
            3 -> WordData.ALL.filter { it.first.length >= 7 }.pick(rng)
            else -> {
                val cat = WordData.CATEGORIES.keys.toList().pick(rng)
                val ws = WordData.CATEGORIES.getValue(cat).filter { it.length in 3..6 }.shuffled(rng).take(2)
                (ws.joinToString(" ")) to cat
            }
        }
        val maxShift = step(difficulty, 3, 5, 9, 14, 28)
        val shift = 1 + rng.nextInt(maxShift)
        val cipher = shiftText(plain, shift)
        val decoys = step(difficulty, 2, 3, 3, 4, 4)
        val letters = plain.filter { it != ' ' }.toMutableList()
        repeat(decoys) { letters += WordData.ALPHABET.pick(rng) }
        return CipherPuzzle(
            plain = plain, cipher = cipher, shift = shift, category = category,
            showShift = tier == 0, liveDecode = tier <= 1,
            tiles = letters.shuffled(rng)
        )
    }

    override fun fingerprint(puzzle: CipherPuzzle) = puzzle.plain
}

// =====================================================================
// Kelime Avı
// =====================================================================

data class WordPlacement(val word: String, val row: Int, val col: Int, val dr: Int, val dc: Int) {
    fun cells(): List<Pair<Int, Int>> = word.indices.map { (row + it * dr) to (col + it * dc) }
}

data class WordHuntPuzzle(val size: Int, val letters: List<Char>, val placements: List<WordPlacement>, val theme: String) {
    fun at(r: Int, c: Int) = letters[r * size + c]
}

object WordHuntGenerator : PuzzleGenerator<WordHuntPuzzle> {
    override val gameId = "word_hunt"

    override fun generate(index: Int, difficulty: Float, rng: Random): WordHuntPuzzle? {
        val tier = tierOf(difficulty)
        val size = step(difficulty, 6, 7, 8, 9, 9)
        val count = scaled(difficulty, intArrayOf(4, 5, 6, 7, 8), extraWithin = 1)
        val dirs = when (tier) {
            0 -> listOf(0 to 1, 1 to 0)
            1 -> listOf(0 to 1, 1 to 0, 1 to 1)
            2 -> listOf(0 to 1, 1 to 0, 1 to 1, -1 to 1)
            3 -> listOf(0 to 1, 1 to 0, 1 to 1, -1 to 1, 0 to -1, -1 to 0)
            else -> listOf(0 to 1, 1 to 0, 1 to 1, -1 to 1, 0 to -1, -1 to 0, -1 to -1, 1 to -1)
        }
        val theme = WordData.CATEGORIES.keys.toList().pick(rng)
        val pool = WordData.CATEGORIES.getValue(theme).filter { it.length in 3..size }.shuffled(rng)
        val grid = arrayOfNulls<Char>(size * size)
        val placements = ArrayList<WordPlacement>()
        for (w in pool) {
            if (placements.size >= count) break
            // Kelimeler birbirinin parçası olmasın
            if (placements.any { it.word.contains(w) || w.contains(it.word) }) continue
            var placed = false
            for (attempt in 0 until 60) {
                val (dr, dc) = dirs.pick(rng)
                val r0 = rng.nextInt(size)
                val c0 = rng.nextInt(size)
                val rEnd = r0 + dr * (w.length - 1)
                val cEnd = c0 + dc * (w.length - 1)
                if (rEnd !in 0 until size || cEnd !in 0 until size) continue
                val ok = w.indices.all { k ->
                    val ch = grid[(r0 + dr * k) * size + (c0 + dc * k)]
                    ch == null || ch == w[k]
                }
                if (!ok) continue
                w.indices.forEach { k -> grid[(r0 + dr * k) * size + (c0 + dc * k)] = w[k] }
                placements += WordPlacement(w, r0, c0, dr, dc)
                placed = true
                break
            }
            if (!placed) continue
        }
        if (placements.size < count) return null
        val letters = grid.map { it ?: WordData.FILLER.pick(rng) }
        val puzzle = WordHuntPuzzle(size, letters, placements, theme)
        // Her kelime ızgarada tam olarak bir kez geçmeli (her yönde)
        val allDirs = listOf(0 to 1, 1 to 0, 1 to 1, -1 to 1, 0 to -1, -1 to 0, -1 to -1, 1 to -1)
        for (p in placements) {
            var occ = 0
            for (r in 0 until size) for (c in 0 until size) for ((dr, dc) in allDirs) {
                val rEnd = r + dr * (p.word.length - 1)
                val cEnd = c + dc * (p.word.length - 1)
                if (rEnd !in 0 until size || cEnd !in 0 until size) continue
                if (p.word.indices.all { k -> letters[(r + dr * k) * size + (c + dc * k)] == p.word[k] }) occ++
            }
            if (occ != 1) return null
        }
        return puzzle
    }

    override fun fingerprint(puzzle: WordHuntPuzzle) = puzzle.placements.map { it.word }.sorted().joinToString(",")
}

// =====================================================================
// Doğru Kapıyı Seç
// =====================================================================

/** Kapı yazısı: güvenli kapı numarasına (0-tabanlı) göre doğru/yanlış değer üretir. */
data class DoorStatement(val text: String, val kind: Int, val a: Int, val b: Int) {
    fun truth(self: Int, safe: Int, doors: Int): Boolean = when (kind) {
        0 -> safe == self                 // Ben güvenliyim
        1 -> safe != self                 // Ben güvenli değilim
        2 -> safe == a                    // X güvenli
        3 -> safe != a                    // X güvenli değil
        4 -> safe == a || safe == b       // Güvenli kapı X ya da Y
        5 -> safe % 2 == 0                // Güvenli kapı tek numaralı (1,3,5 -> indeks çift)
        6 -> safe > self                  // Güvenli kapı benden sağda
        7 -> safe < self                  // Güvenli kapı benden solda
        8 -> safe == 0 || safe == doors - 1 // Güvenli kapı kenarda
        9 -> kotlin.math.abs(safe - self) == 1 // Güvenli kapı bana komşu
        10 -> safe != a && safe != b      // Güvenli kapı ne X ne Y
        else -> safe % 2 == 1             // Güvenli kapının numarası çift
    }
}

/** ruleKind 0: tam olarak [trueCount] yazı doğru. 1: yalnızca güvenli kapının yazısı doğru. 2: yalnızca güvenli kapının yazısı yanlış. */
data class DoorsPuzzle(val statements: List<DoorStatement>, val ruleKind: Int, val trueCount: Int, val safe: Int) {
    val ruleText: String
        get() = when (ruleKind) {
            0 -> if (trueCount == 0) "Yazıların HİÇBİRİ doğru değil." else "Yazılardan tam olarak $trueCount tanesi DOĞRU."
            1 -> "Güvenli kapının yazısı DOĞRU, diğerlerininki YANLIŞ."
            else -> "Güvenli kapının yazısı YANLIŞ, diğerlerininki DOĞRU."
        }
}

object DoorsGenerator : PuzzleGenerator<DoorsPuzzle> {
    override val gameId = "logic_doors"

    private fun makeStatement(self: Int, doors: Int, rng: Random, tier: Int): DoorStatement {
        val maxKind = when (tier) { 0 -> 6; 1 -> 8; 2 -> 10; else -> 12 }
        val kind = rng.nextInt(maxKind)
        var a = rng.nextInt(doors)
        var b = rng.nextInt(doors)
        while (b == a) b = rng.nextInt(doors)
        if (a > b) { val t = a; a = b; b = t }
        val text = when (kind) {
            0 -> "Ben güvenliyim."
            1 -> "Ben güvenli değilim."
            2 -> if (a == self) "Ben güvenliyim." else "${a + 1}. kapı güvenli."
            3 -> if (a == self) "Ben güvenli değilim." else "${a + 1}. kapı güvenli değil."
            4 -> "Güvenli kapı ${a + 1}. ya da ${b + 1}. kapı."
            5 -> "Güvenli kapının numarası tek."
            6 -> "Güvenli kapı benim sağımda."
            7 -> "Güvenli kapı benim solumda."
            8 -> "Güvenli kapı en uçtakilerden biri."
            9 -> "Güvenli kapı benim hemen yanımda."
            10 -> "Güvenli kapı ne ${a + 1}. ne de ${b + 1}. kapı."
            else -> "Güvenli kapının numarası çift."
        }
        return DoorStatement(text, kind, a, b)
    }

    override fun generate(index: Int, difficulty: Float, rng: Random): DoorsPuzzle? {
        val tier = tierOf(difficulty)
        val doors = scaled(difficulty, intArrayOf(2, 3, 3, 4, 4), extraWithin = 1)
        val statements = (0 until doors).map { makeStatement(it, doors, rng, tier) }
        if (statements.map { it.text }.toSet().size < doors) return null
        val ruleKind = when (tier) {
            0 -> rng.nextInt(2)
            1 -> rng.nextInt(3)
            else -> if (rng.nextFloat() < 0.6f) 0 else 1 + rng.nextInt(2)
        }
        fun truths(safe: Int) = statements.mapIndexed { i, s -> s.truth(i, safe, doors) }
        // Kural 0 için doğru sayısını, gerçek çözümden türet
        val candidateCounts = if (ruleKind == 0) (0..doors).toList() else listOf(-1)
        for (tc in candidateCounts.shuffled(rng)) {
            val valid = (0 until doors).filter { safe ->
                val t = truths(safe)
                when (ruleKind) {
                    0 -> t.count { it } == tc
                    1 -> t.withIndex().all { (i, v) -> v == (i == safe) }
                    else -> t.withIndex().all { (i, v) -> v == (i != safe) }
                }
            }
            if (valid.size == 1) {
                return DoorsPuzzle(statements, ruleKind, tc.coerceAtLeast(0), valid[0])
            }
        }
        return null
    }

    override fun fingerprint(puzzle: DoorsPuzzle) =
        "${puzzle.ruleKind}/${puzzle.trueCount}:" + puzzle.statements.joinToString("|") { it.text }
}
