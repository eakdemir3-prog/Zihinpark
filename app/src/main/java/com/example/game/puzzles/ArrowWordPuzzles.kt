package com.example.game.puzzles

import com.example.game.engine.PuzzleGenerator
import com.example.game.engine.step
import com.example.game.engine.tierOf
import kotlin.random.Random

// =====================================================================
// Ok Bulmacası
// =====================================================================

/** [cells]: kuyruktan başa hücre dizisi (r*n+c). Ok, baş hücrenin [dir] yönünde ilerler. */
data class ArrowPiece(val cells: List<Int>, val dir: Int) {
    val head: Int get() = cells.last()
}

data class ArrowPuzzle(val n: Int, val arrows: List<ArrowPiece>)

object ArrowGenerator : PuzzleGenerator<ArrowPuzzle> {
    override val gameId = "arrow_escape"
    override val independent = true

    /** 0 yukarı, 1 sağ, 2 aşağı, 3 sol */
    val DR = intArrayOf(-1, 0, 1, 0)
    val DC = intArrayOf(0, 1, 0, -1)

    /**
     * Okun önü (başından tahtanın kenarına kadar) boş mu? [occ] hücre -> ok kimliği (-1 boş).
     * Engel varsa engelleyen okun kimliğini, yoksa -1 döner.
     */
    fun blocker(n: Int, occ: IntArray, a: ArrowPiece, self: Int): Int {
        var r = a.head / n + DR[a.dir]
        var c = a.head % n + DC[a.dir]
        while (r in 0 until n && c in 0 until n) {
            val o = occ[r * n + c]
            if (o != -1 && o != self) return o
            r += DR[a.dir]; c += DC[a.dir]
        }
        return -1
    }

    override fun generate(index: Int, difficulty: Float, rng: Random): ArrowPuzzle? {
        val tier = tierOf(difficulty)
        val n = step(difficulty, 5, 6, 6, 7, 7, 8, 8, 9)
        val maxLen = step(difficulty, 3, 3, 4, 4, 5, 5, 6, 7)
        val bends = tier >= 1
        val fill = step(difficulty, 0.72f, 0.78f, 0.84f, 0.88f, 0.92f)
        val occ = IntArray(n * n) { -1 }
        val arrows = ArrayList<ArrowPiece>()
        var filled = 0
        var tries = 0
        // Tersten kurulum: her yeni okun önü, o ana kadar yerleşmiş oklardan boş olmalı.
        // Son eklenen ok ilk çıkarılır; böylece her bulmacanın çözümü kesin vardır.
        while (filled < fill * n * n && tries < n * n * 40) {
            tries++
            val empties = (0 until n * n).filter { occ[it] == -1 }
            if (empties.isEmpty()) break
            var cur = empties[rng.nextInt(empties.size)]
            val body = arrayListOf(cur)
            val len = 2 + rng.nextInt(maxLen - 1)
            var lastDir = rng.nextInt(4)
            while (body.size < len) {
                val opts = (0 until 4).filter { d ->
                    if (!bends && body.size > 1 && d != lastDir) return@filter false
                    val r = cur / n + DR[d]; val c = cur % n + DC[d]
                    r in 0 until n && c in 0 until n && occ[r * n + c] == -1 && (r * n + c) !in body
                }
                if (opts.isEmpty()) break
                val d = if (lastDir in opts && rng.nextFloat() < 0.55f) lastDir else opts[rng.nextInt(opts.size)]
                cur = (cur / n + DR[d]) * n + (cur % n + DC[d])
                body += cur
                lastDir = d
            }
            if (body.size < 2) continue
            val piece = ArrowPiece(body.toList(), lastDir)
            // Önü kendi gövdesinden ve diğer oklardan boş olmalı
            var r = piece.head / n + DR[lastDir]; var c = piece.head % n + DC[lastDir]
            var ok = true
            while (r in 0 until n && c in 0 until n) {
                val k = r * n + c
                if (occ[k] != -1 || k in body) { ok = false; break }
                r += DR[lastDir]; c += DC[lastDir]
            }
            if (!ok) continue
            val id = arrows.size
            arrows += piece
            body.forEach { occ[it] = id }
            filled += body.size
        }
        if (arrows.size < n + 2) return null
        if (filled < fill * n * n * 0.85f) return null
        // Kolay çözülmesin: başlangıçta çıkarılabilecek ok sayısı sınırlı olmalı
        val free = arrows.indices.count { blocker(n, occ, arrows[it], it) == -1 }
        if (tier >= 2 && free > arrows.size / 2) return null
        return ArrowPuzzle(n, arrows)
    }

    override fun fingerprint(puzzle: ArrowPuzzle) =
        "${puzzle.n}:" + puzzle.arrows.map { "${it.cells.joinToString(".")}>${it.dir}" }.sorted().joinToString("|")
}

// =====================================================================
// Karışık Kelime
// =====================================================================

data class ScramblePuzzle(
    val word: String,
    val category: String,
    /** Gösterilecek harfler (karışık; tuzak harfler dahil). */
    val tiles: List<Char>,
    val decoys: Int,
    val timeSec: Int
)

object ScrambleGenerator : PuzzleGenerator<ScramblePuzzle> {
    override val gameId = "word_scramble"

    private val POOL: List<Pair<String, String>> by lazy {
        WordData.CATEGORIES.flatMap { (cat, ws) -> ws.map { it to cat } }
            .filter { (w, _) -> w.length in 3..10 && w.all { ch -> ch in WordData.ALPHABET } }
            .distinctBy { it.first }
    }

    override fun generate(index: Int, difficulty: Float, rng: Random): ScramblePuzzle? {
        val tier = tierOf(difficulty)
        val range = when (tier) {
            0 -> 3..5
            1 -> 4..6
            2 -> 5..7
            3 -> 6..10
            else -> 6..10
        }
        val band = POOL.filter { it.first.length in range }
        val (word, cat) = (if (band.isNotEmpty() && rng.nextFloat() < 0.92f) band else POOL).let { it[rng.nextInt(it.size)] }
        val decoys = when (tier) { 0, 1 -> 0; 2 -> if (rng.nextBoolean()) 1 else 0; 3 -> 1; else -> 2 }
        val extra = List(decoys) { WordData.ALPHABET[rng.nextInt(WordData.ALPHABET.length)] }
        var tiles: List<Char>
        var guard = 0
        do { tiles = (word.toList() + extra).shuffled(rng); guard++ } while (tiles.joinToString("").startsWith(word) && guard < 20)
        val time = 20 + word.length * 6 + decoys * 6
        return ScramblePuzzle(word, cat, tiles, decoys, time)
    }

    override fun fingerprint(puzzle: ScramblePuzzle) = puzzle.word
}
