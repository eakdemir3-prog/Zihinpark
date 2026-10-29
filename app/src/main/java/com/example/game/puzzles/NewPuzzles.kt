package com.example.game.puzzles

import com.example.game.engine.PuzzleGenerator
import com.example.game.engine.pick
import com.example.game.engine.scaled
import com.example.game.engine.step
import com.example.game.engine.tierOf
import kotlin.random.Random

// =====================================================================
// Renk Sıralama (tüplerde top sıralama)
// =====================================================================

/** Her tüp alttan üste renk listesi. */
data class SortPuzzle(val tubes: List<List<Int>>, val capacity: Int, val colors: Int, val scrambleMoves: Int)

object SortGenerator : PuzzleGenerator<SortPuzzle> {
    override val gameId = "color_sort"
    override val independent = true

    /** Bir hamle kuralı: kaynak boş değil, hedefte yer var, hedef boş ya da üstü aynı renk. */
    fun canMove(tubes: List<List<Int>>, from: Int, to: Int, cap: Int): Boolean {
        if (from == to) return false
        val a = tubes[from]; val b = tubes[to]
        if (a.isEmpty() || b.size >= cap) return false
        return b.isEmpty() || b.last() == a.last()
    }

    /** Üstteki aynı renkli topların hepsini (yer olduğu kadar) taşır. */
    fun move(tubes: List<List<Int>>, from: Int, to: Int, cap: Int): List<List<Int>> {
        val out = tubes.map { it.toMutableList() }
        val color = out[from].last()
        while (out[from].isNotEmpty() && out[from].last() == color && out[to].size < cap) {
            out[to] += out[from].removeAt(out[from].size - 1)
        }
        return out
    }

    fun solved(tubes: List<List<Int>>, cap: Int): Boolean =
        tubes.all { it.isEmpty() || (it.size == cap && it.all { c -> c == it[0] }) }

    /** BFS/DFS ile çözülebilirlik (küçük durum uzayları için sınırlı arama). */
    fun solvable(start: List<List<Int>>, cap: Int, limit: Int = 60_000): Boolean {
        val seen = HashSet<String>()
        val stack = ArrayDeque<List<List<Int>>>()
        stack.add(start)
        while (stack.isNotEmpty()) {
            val s = stack.removeLast()
            if (solved(s, cap)) return true
            val key = s.map { it.joinToString(",") }.sorted().joinToString("|")
            if (!seen.add(key)) continue
            if (seen.size > limit) return false
            for (f in s.indices) for (t in s.indices) {
                if (!canMove(s, f, t, cap)) continue
                // Tamamlanmış tüpü ya da tek renkli tüpü boş tüpe taşımak anlamsız
                if (s[t].isEmpty() && s[f].all { it == s[f][0] }) continue
                stack.add(move(s, f, t, cap))
            }
        }
        return false
    }

    override fun generate(index: Int, difficulty: Float, rng: Random): SortPuzzle? {
        val colors = scaled(difficulty, intArrayOf(3, 5, 6, 8, 9), extraWithin = 1).coerceAtMost(10)
        val cap = if (tierOf(difficulty) >= 4) 5 else 4
        val empty = if (colors >= 9) 2 else 2
        val balls = (0 until colors).flatMap { c -> List(cap) { c } }.shuffled(rng)
        val tubes = (0 until colors).map { t -> balls.subList(t * cap, (t + 1) * cap).toList() } + List(empty) { emptyList() }
        // Zaten çözülmüş ya da neredeyse çözülmüş tüpleri reddet
        if (tubes.count { it.size == cap && it.all { c -> c == it[0] } } > 0) return null
        val mixedTop = tubes.count { it.size >= 2 && it[it.size - 1] == it[it.size - 2] }
        if (tierOf(difficulty) >= 2 && mixedTop > colors / 2) return null
        if (!solvable(tubes, cap)) return null
        return SortPuzzle(tubes.shuffled(rng), cap, colors, 0)
    }

    override fun fingerprint(puzzle: SortPuzzle) = puzzle.tubes.map { it.joinToString("") }.sorted().joinToString("|")
}

// =====================================================================
// Blok Patlat
// =====================================================================

/** Parça şekilleri (satır, sütun). */
object BlockShapes {
    val ALL: List<Cells> = listOf(
        listOf(0 to 0),
        listOf(0 to 0, 0 to 1),
        listOf(0 to 0, 1 to 0),
        listOf(0 to 0, 0 to 1, 0 to 2),
        listOf(0 to 0, 1 to 0, 2 to 0),
        listOf(0 to 0, 0 to 1, 1 to 0, 1 to 1),
        listOf(0 to 0, 1 to 0, 1 to 1),
        listOf(0 to 1, 1 to 0, 1 to 1),
        listOf(0 to 0, 0 to 1, 1 to 0),
        listOf(0 to 0, 0 to 1, 1 to 1),
        listOf(0 to 0, 0 to 1, 0 to 2, 0 to 3),
        listOf(0 to 0, 1 to 0, 2 to 0, 3 to 0),
        listOf(0 to 0, 1 to 0, 2 to 0, 2 to 1),
        listOf(0 to 1, 1 to 1, 2 to 1, 2 to 0),
        listOf(0 to 0, 0 to 1, 0 to 2, 1 to 1),
        listOf(0 to 0, 0 to 1, 0 to 2, 1 to 0, 1 to 1, 1 to 2, 2 to 0, 2 to 1, 2 to 2),
        listOf(0 to 0, 0 to 1, 0 to 2, 0 to 3, 0 to 4),
        listOf(0 to 0, 1 to 0, 2 to 0, 3 to 0, 4 to 0),
        listOf(0 to 1, 1 to 0, 1 to 1, 1 to 2)
    )
    /** Zorluk arttıkça büyük parçaların gelme olasılığı artar. */
    fun weightFor(i: Int, tier: Int): Int {
        val size = ALL[i].size
        return when {
            size <= 2 -> 6 - tier
            size == 3 -> 5
            size == 4 -> 3 + tier / 2
            else -> 1 + tier / 2
        }.coerceAtLeast(1)
    }
}

data class BlockBlastPuzzle(val size: Int, val initial: List<Int>, val targetLines: Int, val pieceSeed: Long, val tier: Int)

object BlockBlastGenerator : PuzzleGenerator<BlockBlastPuzzle> {
    override val gameId = "block_blast"
    override val independent = true

    override fun generate(index: Int, difficulty: Float, rng: Random): BlockBlastPuzzle? {
        val tier = tierOf(difficulty)
        val size = 8
        val target = scaled(difficulty, intArrayOf(4, 6, 8, 10, 12), extraWithin = 2)
        // Başlangıçta dağınık engel blokları (renk 1..6); tam dolu satır/sütun olmasın
        val fill = step(difficulty, 6, 10, 14, 18, 22)
        val board = MutableList(size * size) { 0 }
        repeat(fill) { board[rng.nextInt(size * size)] = 1 + rng.nextInt(6) }
        for (r in 0 until size) if ((0 until size).all { board[r * size + it] != 0 }) return null
        for (c in 0 until size) if ((0 until size).all { board[it * size + c] != 0 }) return null
        return BlockBlastPuzzle(size, board, target, rng.nextLong(), tier)
    }

    override fun fingerprint(puzzle: BlockBlastPuzzle) = puzzle.initial.joinToString("") + "#" + puzzle.pieceSeed
}

// =====================================================================
// Neon Üçlü (eşleştir-3)
// =====================================================================

/** Hedef: [color] renginden [count] adet topla. */
data class MatchGoal(val color: Int, val count: Int)

data class Match3Puzzle(val size: Int, val colors: Int, val board: List<Int>, val goals: List<MatchGoal>, val moves: Int, val refillSeed: Long)

object Match3Generator : PuzzleGenerator<Match3Puzzle> {
    override val gameId = "neon_match"
    override val independent = true

    fun hasMatchAt(b: List<Int>, n: Int, i: Int): Boolean {
        val r = i / n; val c = i % n; val v = b[i]
        if (v < 0) return false
        var h = 1
        var k = c - 1; while (k >= 0 && b[r * n + k] == v) { h++; k-- }
        k = c + 1; while (k < n && b[r * n + k] == v) { h++; k++ }
        var vv = 1
        k = r - 1; while (k >= 0 && b[k * n + c] == v) { vv++; k-- }
        k = r + 1; while (k < n && b[k * n + c] == v) { vv++; k++ }
        return h >= 3 || vv >= 3
    }

    fun hasAnyMove(b: List<Int>, n: Int): Boolean {
        val m = b.toMutableList()
        for (i in 0 until n * n) {
            val r = i / n; val c = i % n
            for (j in listOf(if (c + 1 < n) i + 1 else -1, if (r + 1 < n) i + n else -1)) {
                if (j < 0) continue
                val t = m[i]; m[i] = m[j]; m[j] = t
                val ok = hasMatchAt(m, n, i) || hasMatchAt(m, n, j)
                m[j] = m[i]; m[i] = t
                if (ok) return true
            }
        }
        return false
    }

    /** Eşleşme içermeyen, en az bir geçerli hamlesi olan tahta. */
    fun freshBoard(n: Int, colors: Int, rng: Random): List<Int> {
        while (true) {
            val b = MutableList(n * n) { -1 }
            for (i in 0 until n * n) {
                var v: Int
                var guard = 0
                do { v = rng.nextInt(colors); b[i] = v; guard++ } while (hasMatchAt(b, n, i) && guard < 20)
            }
            if (hasAnyMove(b, n)) return b
        }
    }

    override fun generate(index: Int, difficulty: Float, rng: Random): Match3Puzzle? {
        val tier = tierOf(difficulty)
        val n = 7
        val colors = step(difficulty, 5, 5, 6, 6, 6)
        val goalsN = if (tier >= 2) 2 else 1
        val gColors = (0 until colors).shuffled(rng).take(goalsN)
        val per = scaled(difficulty, intArrayOf(12, 16, 14, 18, 22), extraWithin = 4)
        val goals = gColors.map { MatchGoal(it, per) }
        val moves = step(difficulty, 18, 18, 22, 22, 24) - (if (tier >= 1) rng.nextInt(3) else 0)
        return Match3Puzzle(n, colors, freshBoard(n, colors, rng), goals, moves, rng.nextLong())
    }

    override fun fingerprint(puzzle: Match3Puzzle) = puzzle.board.joinToString("") + "/" + puzzle.goals
}

// =====================================================================
// Sudoku
// =====================================================================

data class SudokuPuzzle(val n: Int, val boxR: Int, val boxC: Int, val solution: List<Int>, val givens: List<Boolean>) {
    fun given(i: Int) = givens[i]
}

object SudokuGenerator : PuzzleGenerator<SudokuPuzzle> {
    override val gameId = "sudoku"
    override val independent = true

    private fun boxDims(n: Int) = when (n) { 4 -> 2 to 2; 6 -> 2 to 3; else -> 3 to 3 }

    fun candidates(g: IntArray, n: Int, br: Int, bc: Int, i: Int): List<Int> {
        val r = i / n; val c = i % n
        val used = BooleanArray(n + 1)
        for (k in 0 until n) { used[g[r * n + k]] = true; used[g[k * n + c]] = true }
        val r0 = r / br * br; val c0 = c / bc * bc
        for (rr in r0 until r0 + br) for (cc in c0 until c0 + bc) used[g[rr * n + cc]] = true
        return (1..n).filter { !used[it] }
    }

    private fun fill(g: IntArray, n: Int, br: Int, bc: Int, rng: Random): Boolean {
        val i = g.indexOfFirst { it == 0 }
        if (i < 0) return true
        for (v in candidates(g, n, br, bc, i).shuffled(rng)) {
            g[i] = v
            if (fill(g, n, br, bc, rng)) return true
        }
        g[i] = 0
        return false
    }

    /** Çözüm sayısı (en fazla [limit]); en az adaylı hücreden dallanır. */
    fun countSolutions(g: IntArray, n: Int, br: Int, bc: Int, limit: Int = 2): Int {
        var best = -1; var bestC: List<Int>? = null
        for (i in g.indices) if (g[i] == 0) {
            val c = candidates(g, n, br, bc, i)
            if (c.isEmpty()) return 0
            if (bestC == null || c.size < bestC.size) { best = i; bestC = c; if (c.size == 1) break }
        }
        if (best < 0) return 1
        var total = 0
        for (v in bestC!!) {
            g[best] = v
            total += countSolutions(g, n, br, bc, limit - total)
            if (total >= limit) break
        }
        g[best] = 0
        return total
    }

    override fun generate(index: Int, difficulty: Float, rng: Random): SudokuPuzzle? {
        val tier = tierOf(difficulty)
        val n = when (tier) { 0 -> 4; 1 -> 6; 2 -> 6; else -> 9 }
        val (br, bc) = boxDims(n)
        val g = IntArray(n * n)
        if (!fill(g, n, br, bc, rng)) return null
        val solution = g.toList()
        val targetGivens = when (tier) {
            0 -> 8 - (difficulty * 25).toInt().coerceAtMost(2)          // 4x4: 6..8
            1 -> 18 - (difficulty * 5).toInt()                           // 6x6: ~17
            2 -> 15 - ((difficulty - 0.4f) * 15).toInt()                 // 6x6: ~12..14
            3 -> 36 - ((difficulty - 0.6f) * 30).toInt()                 // 9x9: ~30..36
            else -> 30 - ((difficulty - 0.8f) * 25).toInt()              // 9x9: ~25..30
        }
        val order = (0 until n * n).shuffled(rng)
        var givens = n * n
        for (i in order) {
            if (givens <= targetGivens) break
            val keep = g[i]
            g[i] = 0
            if (countSolutions(g.copyOf(), n, br, bc) != 1) g[i] = keep else givens--
        }
        if (givens > targetGivens + 3) return null
        return SudokuPuzzle(n, br, bc, solution, g.map { it != 0 })
    }

    override fun fingerprint(puzzle: SudokuPuzzle) =
        puzzle.solution.joinToString("") + "/" + puzzle.givens.joinToString("") { if (it) "1" else "0" }
}

// =====================================================================
// Neon Bağla (Flow)
// =====================================================================

/** [ends]: her renk için iki uç hücre indeksi. [solution]: her hücrenin rengi. */
data class FlowPuzzle(val n: Int, val ends: List<Pair<Int, Int>>, val solution: List<Int>, val paths: List<List<Int>>)

object FlowGenerator : PuzzleGenerator<FlowPuzzle> {
    override val gameId = "neon_connect"
    override val independent = true

    /** Izgarayı yılan biçiminde dolaşan Hamilton yolu, "backbite" hamleleriyle karıştırılır. */
    private fun randomHamiltonian(n: Int, rng: Random): MutableList<Int> {
        val path = ArrayList<Int>()
        for (r in 0 until n) {
            val cols = if (r % 2 == 0) 0 until n else (n - 1 downTo 0)
            for (c in cols) path += r * n + c
        }
        val steps = n * n * 10
        repeat(steps) {
            val fromEnd = rng.nextBoolean()
            if (fromEnd) path.reverse()
            // path[0] ucundan: komşulardan biri yolda path[k] ise, path[0..k-1] ters çevrilir
            val head = path[0]
            val hr = head / n; val hc = head % n
            val nbrs = listOf(hr - 1 to hc, hr + 1 to hc, hr to hc - 1, hr to hc + 1)
                .filter { (r, c) -> r in 0 until n && c in 0 until n }
                .map { (r, c) -> r * n + c }
                .filter { it != path[1] }
            if (nbrs.isNotEmpty()) {
                val nb = nbrs.pick(rng)
                val k = path.indexOf(nb)
                path.subList(0, k).reverse()
            }
            if (fromEnd) path.reverse()
        }
        return path
    }

    private fun adjacent(a: Int, b: Int, n: Int): Boolean {
        val ar = a / n; val ac = a % n; val br = b / n; val bc = b % n
        return kotlin.math.abs(ar - br) + kotlin.math.abs(ac - bc) == 1
    }

    override fun generate(index: Int, difficulty: Float, rng: Random): FlowPuzzle? {
        val n = step(difficulty, 5, 6, 7, 8, 9)
        val k = scaled(difficulty, intArrayOf(4, 5, 6, 7, 8), extraWithin = 2).coerceAtMost(n + 1)
        val path = randomHamiltonian(n, rng)
        // Yolu k parçaya böl (her parça en az 3 hücre)
        val total = n * n
        val cuts = (1 until total).shuffled(rng).take(k - 1).sorted()
        val bounds = listOf(0) + cuts + listOf(total)
        val segs = (0 until k).map { path.subList(bounds[it], bounds[it + 1]).toList() }
        if (segs.any { it.size < 3 }) return null
        // Uçlar birbirine komşuysa (kısa devre) bulmaca anlamsızlaşır
        if (segs.any { adjacent(it.first(), it.last(), n) }) return null
        val solution = MutableList(total) { -1 }
        segs.forEachIndexed { color, s -> s.forEach { solution[it] = color } }
        // Bir parça kendine komşu olup "kısa yol" veriyorsa tahta yine de çözülebilir; kabul edilir.
        val ends = segs.map { it.first() to it.last() }
        return FlowPuzzle(n, ends, solution, segs)
    }

    override fun fingerprint(puzzle: FlowPuzzle) =
        "${puzzle.n}:" + puzzle.ends.map { listOf(it.first, it.second).sorted() }.sortedBy { it[0] }.joinToString(";")
}
