package com.example.game.puzzles

import com.example.game.engine.PuzzleGenerator
import com.example.game.engine.pick
import com.example.game.engine.scaled
import com.example.game.engine.step
import com.example.game.engine.tierOf
import java.util.ArrayDeque
import kotlin.random.Random

// =====================================================================
// Ortak: labirent
// =====================================================================

object Mazes {
    /** (2m+1)x(2m+1) karo ızgarasında kusursuz labirent. true = duvar. */
    fun perfect(m: Int, rng: Random): Array<BooleanArray> {
        val n = 2 * m + 1
        val wall = Array(n) { BooleanArray(n) { true } }
        val visited = Array(m) { BooleanArray(m) }
        val stack = ArrayDeque<Pair<Int, Int>>()
        stack.push(0 to 0)
        visited[0][0] = true
        wall[1][1] = false
        while (stack.isNotEmpty()) {
            val (r, c) = stack.peek()
            val nbrs = listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)
                .map { (dr, dc) -> (r + dr) to (c + dc) }
                .filter { (nr, nc) -> nr in 0 until m && nc in 0 until m && !visited[nr][nc] }
            if (nbrs.isEmpty()) { stack.pop(); continue }
            val (nr, nc) = nbrs.pick(rng)
            visited[nr][nc] = true
            wall[2 * nr + 1][2 * nc + 1] = false
            wall[r + nr + 1][c + nc + 1] = false
            stack.push(nr to nc)
        }
        return wall
    }

    /** Duvarsız karolar üzerinde BFS mesafeleri (-1 ulaşılamaz). */
    fun distances(walls: (Int, Int) -> Boolean, n: Int, start: Pair<Int, Int>): Array<IntArray> {
        val d = Array(n) { IntArray(n) { -1 } }
        val q = ArrayDeque<Pair<Int, Int>>()
        d[start.first][start.second] = 0
        q.add(start)
        while (q.isNotEmpty()) {
            val (r, c) = q.poll()
            for ((dr, dc) in listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)) {
                val nr = r + dr; val nc = c + dc
                if (nr in 0 until n && nc in 0 until n && d[nr][nc] < 0 && !walls(nr, nc)) {
                    d[nr][nc] = d[r][c] + 1
                    q.add(nr to nc)
                }
            }
        }
        return d
    }

    fun path(walls: (Int, Int) -> Boolean, n: Int, start: Pair<Int, Int>, goal: Pair<Int, Int>): List<Pair<Int, Int>> {
        val d = distances(walls, n, start)
        if (d[goal.first][goal.second] < 0) return emptyList()
        val out = arrayListOf(goal)
        var cur = goal
        while (cur != start) {
            val (r, c) = cur
            cur = listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)
                .map { (r + it.first) to (c + it.second) }
                .first { (nr, nc) -> nr in 0 until n && nc in 0 until n && d[nr][nc] == d[r][c] - 1 }
            out += cur
        }
        return out.reversed()
    }
}

// =====================================================================
// Yolun Sonu (anahtar ve kapı)
// =====================================================================

/** Karo kodları: 0 zemin, 1 duvar, 2 hedef, 10+i kapı i, 20+i anahtar i. */
data class PathFinderPuzzle(val n: Int, val tiles: List<Int>, val start: Pair<Int, Int>, val optimalMoves: Int, val pairs: Int) {
    fun at(r: Int, c: Int) = tiles[r * n + c]
}

object PathFinderGenerator : PuzzleGenerator<PathFinderPuzzle> {
    override val gameId = "path_finder"
    override val independent = true

    override fun generate(index: Int, difficulty: Float, rng: Random): PathFinderPuzzle? {
        val m = scaled(difficulty, intArrayOf(3, 4, 5, 6, 7), extraWithin = 1).coerceAtMost(7)
        val pairs = step(difficulty, 1, 1, 2, 2, 3)
        val maze = Mazes.perfect(m, rng)
        val n = 2 * m + 1
        // Zorlaştıkça birkaç ek geçit açarak döngüler oluştur (kapılar yine de atlanamaz olmalı)
        val start = 1 to 1
        val distStart = Mazes.distances({ r, c -> maze[r][c] }, n, start)
        var goal = start
        for (r in 0 until n) for (c in 0 until n) if (distStart[r][c] > distStart[goal.first][goal.second]) goal = r to c
        val mainPath = Mazes.path({ r, c -> maze[r][c] }, n, start, goal)
        if (mainPath.size < 5 + pairs * 3) return null
        val tiles = IntArray(n * n) { if (maze[it / n][it % n]) 1 else 0 }
        tiles[goal.first * n + goal.second] = 2
        // Kapıları ana yola, eşit aralıklarla koy
        val doorIdx = (1..pairs).map { i -> (mainPath.size * i / (pairs + 1)).coerceIn(2, mainPath.size - 2) }
        if (doorIdx.toSet().size != pairs) return null
        doorIdx.forEachIndexed { i, pi -> val (r, c) = mainPath[pi]; tiles[r * n + c] = 10 + i }
        val onMain = mainPath.toSet()
        for (i in 0 until pairs) {
            // i. anahtar: i ve sonraki kapılar kapalıyken ulaşılabilen, ana yol dışındaki en uzak karo
            val blocked = { r: Int, c: Int ->
                val t = tiles[r * n + c]
                t == 1 || (t in 10..19 && t - 10 >= i)
            }
            val d = Mazes.distances(blocked, n, start)
            var best: Pair<Int, Int>? = null
            var bestScore = -1
            for (r in 0 until n) for (c in 0 until n) {
                if (d[r][c] <= 0 || (r to c) in onMain || tiles[r * n + c] != 0) continue
                val score = d[r][c] + rng.nextInt(4)
                if (score > bestScore) { bestScore = score; best = r to c }
            }
            // Yan yol yoksa anahtarı ana yolda, kapıdan önceki bir noktaya koy
            val k = best ?: run {
                val from = if (i == 0) 1 else doorIdx[i - 1] + 1
                val to = doorIdx[i] - 1
                val spots = (from until to).map { mainPath[it] }.filter { tiles[it.first * n + it.second] == 0 }
                if (spots.isEmpty()) return null
                spots.pick(rng)
            }
            tiles[k.first * n + k.second] = 20 + i
        }
        val opt = optimal(n, tiles.toList(), start) ?: return null
        return PathFinderPuzzle(n, tiles.toList(), start, opt, pairs)
    }

    /** (konum, anahtar maskesi) üzerinde BFS ile en kısa hamle sayısı. */
    fun optimal(n: Int, tiles: List<Int>, start: Pair<Int, Int>): Int? {
        val seen = HashSet<Triple<Int, Int, Int>>()
        val q = ArrayDeque<Pair<Triple<Int, Int, Int>, Int>>()
        q.add(Triple(start.first, start.second, 0) to 0)
        seen += Triple(start.first, start.second, 0)
        while (q.isNotEmpty()) {
            val (s, dist) = q.poll()
            val (r, c, mask) = s
            if (tiles[r * n + c] == 2) return dist
            for ((dr, dc) in listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)) {
                val nr = r + dr; val nc = c + dc
                if (nr !in 0 until n || nc !in 0 until n) continue
                val t = tiles[nr * n + nc]
                if (t == 1) continue
                if (t in 10..19 && (mask shr (t - 10)) and 1 == 0) continue
                val nm = if (t in 20..29) mask or (1 shl (t - 20)) else mask
                val ns = Triple(nr, nc, nm)
                if (seen.add(ns)) q.add(ns to dist + 1)
            }
        }
        return null
    }

    /**
     * Şu anki konum ve anahtarlarla en kısa çözümün ilk bölümü: bir sonraki yeni anahtara
     * (ya da anahtar gerekmiyorsa hedefe) kadar olan karolar. Başlangıç karosu hariç.
     */
    fun routeFrom(p: PathFinderPuzzle, from: Pair<Int, Int>, keys: Int): List<Pair<Int, Int>> {
        val n = p.n
        val start = Triple(from.first, from.second, keys)
        val prev = HashMap<Triple<Int, Int, Int>, Triple<Int, Int, Int>?>()
        prev[start] = null
        val q = ArrayDeque<Triple<Int, Int, Int>>(); q.add(start)
        var end: Triple<Int, Int, Int>? = null
        while (q.isNotEmpty()) {
            val s = q.poll()
            val (r, c, mask) = s
            if (p.at(r, c) == 2) { end = s; break }
            for ((dr, dc) in listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)) {
                val nr = r + dr; val nc = c + dc
                if (nr !in 0 until n || nc !in 0 until n) continue
                val t = p.at(nr, nc)
                if (t == 1) continue
                if (t in 10..19 && (mask shr (t - 10)) and 1 == 0) continue
                val nm = if (t in 20..29) mask or (1 shl (t - 20)) else mask
                val ns = Triple(nr, nc, nm)
                if (ns !in prev) { prev[ns] = s; q.add(ns) }
            }
        }
        var cur = end ?: return emptyList()
        val path = ArrayList<Triple<Int, Int, Int>>()
        while (cur != start) { path += cur; cur = prev[cur] ?: break }
        path.reverse()
        val firstKey = path.indexOfFirst { it.third != keys }
        val cut = if (firstKey >= 0) path.take(firstKey + 1) else path
        return cut.map { it.first to it.second }
    }

    override fun fingerprint(puzzle: PathFinderPuzzle) = puzzle.tiles.joinToString("")
}

// =====================================================================
// Labirentten Kaçış
// =====================================================================

data class MazeEscapePuzzle(val n: Int, val walls: List<Boolean>, val exit: Pair<Int, Int>, val fogRadius: Int, val timeLimitSec: Int, val pathLen: Int) {
    fun wall(r: Int, c: Int) = walls[r * n + c]
}

object MazeEscapeGenerator : PuzzleGenerator<MazeEscapePuzzle> {
    override val gameId = "maze_escape"
    override val independent = true

    override fun generate(index: Int, difficulty: Float, rng: Random): MazeEscapePuzzle? {
        val m = step(difficulty, 5, 6, 7, 8, 9)
        val maze = Mazes.perfect(m, rng)
        val n = 2 * m + 1
        val exit = (n - 2) to (n - 2)
        val path = Mazes.path({ r, c -> maze[r][c] }, n, 1 to 1, exit)
        if (path.size < m * 3) return null
        val fog = step(difficulty, 0, 0, 0, 3, 2)
        val time = (path.size * step(difficulty, 0.9f, 0.8f, 0.7f, 0.75f, 0.7f)).toInt() + 12
        return MazeEscapePuzzle(n, maze.flatMap { it.toList() }, exit, fog, time, path.size)
    }

    override fun fingerprint(puzzle: MazeEscapePuzzle) = puzzle.walls.joinToString("") { if (it) "1" else "0" }
}

// =====================================================================
// İmkânsız Görevler (nehir geçişi)
// =====================================================================

data class RiverItem(val emoji: String, val name: String)

private val RIVER_POOL = listOf(
    RiverItem("🐺", "Kurt"), RiverItem("🐑", "Kuzu"), RiverItem("🌿", "Ot"),
    RiverItem("🦊", "Tilki"), RiverItem("🐔", "Tavuk"), RiverItem("🌽", "Mısır"),
    RiverItem("🐱", "Kedi"), RiverItem("🐭", "Fare"), RiverItem("🧀", "Peynir"),
    RiverItem("🐶", "Köpek"), RiverItem("🐻", "Ayı"), RiverItem("🍯", "Bal"),
    RiverItem("🦁", "Aslan"), RiverItem("🦓", "Zebra")
)

/** Avcı -> av (havuz indeksleri). */
private val RIVER_EATS = listOf(
    0 to 1, 1 to 2, 3 to 4, 4 to 5, 6 to 7, 7 to 8, 9 to 6, 10 to 11, 12 to 13, 12 to 1, 13 to 2, 3 to 7, 0 to 4
)

data class RiverPuzzle(val items: List<RiverItem>, val eats: List<Pair<Int, Int>>, val capacity: Int, val optimalMoves: Int) {
    /** Bir kıyıdaki [mask] (bit = eşya orada) sahipsiz kalırsa yenen çifti döner. */
    fun conflict(mask: Int): Pair<Int, Int>? = eats.firstOrNull { (a, b) -> (mask shr a) and 1 == 1 && (mask shr b) and 1 == 1 }
}

object RiverGenerator : PuzzleGenerator<RiverPuzzle> {
    override val gameId = "river_crossing"

    override fun generate(index: Int, difficulty: Float, rng: Random): RiverPuzzle? {
        val tier = tierOf(difficulty)
        // Kayık kapasitesi 1 iken bulmacalar gerçekten düşündürür; 6 eşyada bazen 2 kişilik kayık
        val k = scaled(difficulty, intArrayOf(3, 4, 4, 5, 6), extraWithin = 1).coerceAtMost(6)
        val capacity = if (k == 6 && rng.nextBoolean()) 2 else 1
        val chosen = RIVER_POOL.indices.shuffled(rng).take(k).sorted()
        val local = chosen.withIndex().associate { it.value to it.index }
        val eats = RIVER_EATS.filter { it.first in local && it.second in local }.map { local.getValue(it.first) to local.getValue(it.second) }
        if (eats.size < (if (tier == 0) 1 else 2)) return null
        val p = RiverPuzzle(chosen.map { RIVER_POOL[it] }, eats, capacity, 0)
        val opt = solve(p) ?: return null
        val minMoves = if (capacity == 2) 7 else step(difficulty, 5, 7, 7, 9, 11)
        if (opt < minMoves) return null
        return p.copy(optimalMoves = opt)
    }

    /** Durum: eşyaların karşı kıyıda olup olmadığı (bit) + kayıkçının tarafı. BFS ile en az geçiş. */
    fun solve(p: RiverPuzzle): Int? {
        val n = p.items.size
        val all = (1 shl n) - 1
        val start = 0 to 0 // mask (karşıdakiler), side 0 = başlangıç kıyısı
        val dist = HashMap<Pair<Int, Int>, Int>()
        val q = ArrayDeque<Pair<Int, Int>>()
        dist[start] = 0; q.add(start)
        while (q.isNotEmpty()) {
            val s = q.poll()
            val (mask, side) = s
            if (mask == all && side == 1) return dist[s]
            val hereMask = if (side == 0) all and mask.inv() else mask
            // Kayığa binecek alt kümeler (0..capacity eşya)
            var sub = hereMask
            while (true) {
                if (Integer.bitCount(sub) <= p.capacity) {
                    val newMask = if (side == 0) mask or sub else mask and sub.inv()
                    val newSide = 1 - side
                    val leftBehind = if (newSide == 1) all and newMask.inv() else newMask
                    if (p.conflict(leftBehind) == null) {
                        val ns = newMask to newSide
                        if (ns !in dist) { dist[ns] = dist.getValue(s) + 1; q.add(ns) }
                    }
                }
                if (sub == 0) break
                sub = (sub - 1) and hereMask
            }
        }
        return null
    }

    /** Şu anki durumdan en kısa çözümün ilk hamlesinde kayığa binecekler (bit maskesi). Çözüm yoksa null. */
    fun nextMove(p: RiverPuzzle, far: Int, side: Int): Int? {
        val n = p.items.size
        val all = (1 shl n) - 1
        val start = far to side
        val firstLoad = HashMap<Pair<Int, Int>, Int>()
        val seen = hashSetOf(start)
        val q = ArrayDeque<Pair<Int, Int>>(); q.add(start)
        while (q.isNotEmpty()) {
            val s = q.poll()
            val (mask, sd) = s
            if (mask == all && sd == 1) return firstLoad[s]
            val hereMask = if (sd == 0) all and mask.inv() else mask
            var sub = hereMask
            while (true) {
                if (Integer.bitCount(sub) <= p.capacity) {
                    val newMask = if (sd == 0) mask or sub else mask and sub.inv()
                    val ns = 1 - sd
                    val leftBehind = if (ns == 1) all and newMask.inv() else newMask
                    if (p.conflict(leftBehind) == null) {
                        val st = newMask to ns
                        if (seen.add(st)) { firstLoad[st] = if (s == start) sub else firstLoad.getValue(s); q.add(st) }
                    }
                }
                if (sub == 0) break
                sub = (sub - 1) and hereMask
            }
        }
        return null
    }

    override fun fingerprint(puzzle: RiverPuzzle) = puzzle.items.joinToString("") { it.emoji } + "/" + puzzle.capacity
}

// =====================================================================
// Şekil Mimarı
// =====================================================================

data class ArchitectPuzzle(val rows: Int, val cols: Int, val region: Set<Pair<Int, Int>>, val pieces: List<Cells>, val solution: List<Cells>)

object ArchitectGenerator : PuzzleGenerator<ArchitectPuzzle> {
    override val gameId = "shape_architect"
    override val independent = true

    override fun generate(index: Int, difficulty: Float, rng: Random): ArchitectPuzzle? {
        val tier = tierOf(difficulty)
        val (rows, cols) = step(difficulty, 3 to 4, 4 to 4, 4 to 5, 5 to 5, 5 to 6)
        val k = step(difficulty, 3, 4, 5, 6, 7)
        val region = HashSet<Pair<Int, Int>>()
        for (r in 0 until rows) for (c in 0 until cols) region += r to c
        if (tier >= 3) {
            // Düzensiz siluet: kenarlardan birkaç karo çıkar
            val remove = 1 + rng.nextInt(3)
            repeat(remove) {
                val edge = region.filter { (r, c) -> r == 0 || c == 0 || r == rows - 1 || c == cols - 1 }
                region.remove(edge.pick(rng))
            }
            // Bağlı kalmalı
            if (!connected(region)) return null
        }
        // Rastgele tohumlardan büyüterek bölme
        val owner = HashMap<Pair<Int, Int>, Int>()
        val seeds = region.toList().shuffled(rng).take(k)
        seeds.forEachIndexed { i, s -> owner[s] = i }
        var guard = 0
        while (owner.size < region.size && guard++ < 2000) {
            val i = rng.nextInt(k)
            val size = owner.count { it.value == i }
            if (size >= 6) continue
            val frontier = owner.filter { it.value == i }.keys.flatMap { (r, c) ->
                listOf((r - 1) to c, (r + 1) to c, r to (c - 1), r to (c + 1))
            }.filter { it in region && it !in owner }
            if (frontier.isEmpty()) continue
            owner[frontier.pick(rng)] = i
        }
        if (owner.size < region.size) return null
        val solution = (0 until k).map { i -> owner.filter { it.value == i }.keys.toList() }
        if (solution.any { it.size < 2 || it.size > 6 }) return null
        val pieces = solution.map { cells ->
            val norm = Shapes.normalize(cells)
            if (tier == 0) norm else Shapes.rotate(norm, rng.nextInt(4))
        }.shuffled(rng)
        return ArchitectPuzzle(rows, cols, region, pieces, solution)
    }

    private fun connected(region: Set<Pair<Int, Int>>): Boolean {
        val start = region.firstOrNull() ?: return false
        val seen = hashSetOf(start)
        val q = ArrayDeque<Pair<Int, Int>>(); q.add(start)
        while (q.isNotEmpty()) {
            val (r, c) = q.poll()
            for (n in listOf((r - 1) to c, (r + 1) to c, r to (c - 1), r to (c + 1))) if (n in region && seen.add(n)) q.add(n)
        }
        return seen.size == region.size
    }

    override fun fingerprint(puzzle: ArchitectPuzzle) =
        "${puzzle.rows}x${puzzle.cols}:" + puzzle.region.sortedWith(compareBy({ it.first }, { it.second })).joinToString("") { "${it.first}${it.second}" } +
            "|" + puzzle.solution.map { Shapes.key(it) }.sorted().joinToString("/")
}

// =====================================================================
// Sayı Mühendisi
// =====================================================================

/**
 * dim x dim sayı çekirdeği; her satır soldan sağa, her sütun yukarıdan aşağıya
 * soldan sağa işlem sırasıyla hesaplanır. [hidden] doldurulacak hücreler.
 */
data class MathGridPuzzle(
    val dim: Int,
    val values: List<Int>,
    val rowOps: List<List<Char>>,
    val colOps: List<List<Char>>,
    val rowResults: List<Int>,
    val colResults: List<Int>,
    val hidden: Set<Int>,
    val maxValue: Int
)

object MathGridGenerator : PuzzleGenerator<MathGridPuzzle> {
    override val gameId = "math_engineer"
    override val independent = true

    fun eval(nums: List<Int>, ops: List<Char>): Int? {
        var acc = nums[0]
        for (i in ops.indices) {
            val b = nums[i + 1]
            acc = when (ops[i]) {
                '+' -> acc + b
                '−' -> acc - b
                '×' -> acc * b
                else -> if (b != 0 && acc % b == 0) acc / b else return null
            }
        }
        return acc
    }

    override fun generate(index: Int, difficulty: Float, rng: Random): MathGridPuzzle? {
        val tier = tierOf(difficulty)
        val dim = if (tier >= 3) 3 else 2
        val maxV = step(difficulty, 9, 9, 12, 9, 12)
        val opsPool = when (tier) { 0 -> listOf('+', '−'); 1, 2 -> listOf('+', '−', '×'); else -> listOf('+', '−', '×', '÷') }
        val values = List(dim * dim) { rng.nextInt(1, maxV + 1) }
        val rowOps = List(dim) { List(dim - 1) { opsPool.pick(rng) } }
        val colOps = List(dim) { List(dim - 1) { opsPool.pick(rng) } }
        val rowRes = (0 until dim).map { r -> eval((0 until dim).map { values[r * dim + it] }, rowOps[r]) ?: return null }
        val colRes = (0 until dim).map { c -> eval((0 until dim).map { values[it * dim + c] }, colOps[c]) ?: return null }
        if ((rowRes + colRes).any { it < 0 || it > 200 }) return null
        val hiddenCount = when (tier) { 0 -> 1; 1 -> 2; 2 -> 3; 3 -> 3; else -> 4 }
        val hidden = (0 until dim * dim).shuffled(rng).take(hiddenCount).toSet()
        // Tek çözüm kontrolü (gizli hücreler 0..maxV aralığında)
        val hiddenList = hidden.toList()
        var solutions = 0
        val trial = values.toMutableList()
        fun rec(i: Int) {
            if (solutions > 1) return
            if (i == hiddenList.size) {
                val ok = (0 until dim).all { r -> eval((0 until dim).map { trial[r * dim + it] }, rowOps[r]) == rowRes[r] } &&
                    (0 until dim).all { c -> eval((0 until dim).map { trial[it * dim + c] }, colOps[c]) == colRes[c] }
                if (ok) solutions++
                return
            }
            for (v in 0..maxV) { trial[hiddenList[i]] = v; rec(i + 1) }
        }
        rec(0)
        if (solutions != 1) return null
        return MathGridPuzzle(dim, values, rowOps, colOps, rowRes, colRes, hidden, maxV)
    }

    override fun fingerprint(puzzle: MathGridPuzzle) =
        puzzle.values.joinToString(",") + "|" + puzzle.rowOps + puzzle.colOps + "|" + puzzle.hidden.sorted()
}

// =====================================================================
// Sayı Patlatma (hedefli 2048)
// =====================================================================

data class MergePuzzle(val size: Int, val board: List<Int>, val target: Int, val moveLimit: Int, val spawnSeed: Long)

object MergeGenerator : PuzzleGenerator<MergePuzzle> {
    override val gameId = "number_merge"
    override val independent = true

    override fun generate(index: Int, difficulty: Float, rng: Random): MergePuzzle? {
        val target = step(difficulty, 32, 64, 128, 128, 256)
        val size = 4
        // Hedefe birleşerek ulaşabilecek zincir: T/2, T/4, ..., T/2^L ve bir eş T/2^L
        val chainLen = step(difficulty, 2, 3, 3, 4, 4)
        val tiles = ArrayList<Int>()
        var v = target / 2
        repeat(chainLen) { tiles += v; v /= 2 }
        tiles += tiles.last()
        // Biraz gürültü
        val noise = step(difficulty, 1, 2, 2, 3, 3)
        repeat(noise) { tiles += listOf(2, 4).pick(rng) }
        if (tiles.size > size * size - 4) return null
        val board = MutableList(size * size) { 0 }
        val cells = (0 until size * size).shuffled(rng).take(tiles.size)
        cells.forEachIndexed { i, c -> board[c] = tiles[i] }
        val limit = step(difficulty, 40, 55, 70, 60, 90)
        return MergePuzzle(size, board, target, limit, rng.nextLong())
    }

    override fun fingerprint(puzzle: MergePuzzle) = puzzle.board.joinToString(",") + "→" + puzzle.target

    /** Bir satırı sola kaydırıp birleştirir; (yeni satır, kazanılan puan, değişti mi). */
    fun slideRow(row: List<Int>): Triple<List<Int>, Int, Boolean> {
        val nums = row.filter { it != 0 }
        val out = ArrayList<Int>()
        var gained = 0
        var i = 0
        while (i < nums.size) {
            if (i + 1 < nums.size && nums[i] == nums[i + 1]) {
                out += nums[i] * 2; gained += nums[i] * 2; i += 2
            } else { out += nums[i]; i++ }
        }
        while (out.size < row.size) out += 0
        return Triple(out, gained, out != row)
    }
}

// =====================================================================
// Parçaları Birleştir (yer değiştirmeli yapboz)
// =====================================================================

data class JigsawPuzzle(val n: Int, val artSeed: Long, val palette: Int, val permutation: List<Int>, val rotations: List<Int>)

object JigsawGenerator : PuzzleGenerator<JigsawPuzzle> {
    override val gameId = "jigsaw_puzzle"
    override val independent = true

    override fun generate(index: Int, difficulty: Float, rng: Random): JigsawPuzzle? {
        val tier = tierOf(difficulty)
        val n = step(difficulty, 3, 3, 4, 4, 5)
        val count = n * n
        var perm: List<Int>
        do { perm = (0 until count).shuffled(rng) } while (perm.withIndex().count { it.index == it.value } > count / 4)
        val rotations = if (tier >= 3) List(count) { if (rng.nextFloat() < 0.4f) 1 + rng.nextInt(3) else 0 } else List(count) { 0 }
        return JigsawPuzzle(n, rng.nextLong(), rng.nextInt(6), perm, rotations)
    }

    override fun fingerprint(puzzle: JigsawPuzzle) = "${puzzle.n}:${puzzle.artSeed}"
}
