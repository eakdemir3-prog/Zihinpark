package com.example.game.puzzles

import com.example.game.engine.PuzzleGenerator
import com.example.game.engine.pick
import com.example.game.engine.scaled
import com.example.game.engine.step
import com.example.game.engine.tierOf
import kotlin.random.Random

// =====================================================================
// Gizli Resim (Nonogram)
// =====================================================================

data class NonogramPuzzle(val size: Int, val cells: List<Boolean>, val rowClues: List<List<Int>>, val colClues: List<List<Int>>) {
    fun filled(r: Int, c: Int) = cells[r * size + c]
    val filledCount: Int get() = cells.count { it }
}

object NonogramGenerator : PuzzleGenerator<NonogramPuzzle> {
    override val gameId = "nonogram"
    override val independent = true

    fun clues(line: List<Boolean>): List<Int> {
        val out = ArrayList<Int>()
        var run = 0
        for (b in line) {
            if (b) run++ else if (run > 0) { out += run; run = 0 }
        }
        if (run > 0) out += run
        return if (out.isEmpty()) listOf(0) else out
    }

    /** İpucuna uyan tüm satır yerleşimleri. */
    private fun arrangements(len: Int, clue: List<Int>): List<BooleanArray> {
        val blocks = clue.filter { it > 0 }
        val out = ArrayList<BooleanArray>()
        if (blocks.isEmpty()) { out += BooleanArray(len); return out }
        fun rec(bi: Int, start: Int, cur: BooleanArray) {
            if (bi == blocks.size) { out += cur.copyOf(); return }
            val remaining = blocks.drop(bi + 1).sum() + (blocks.size - bi - 1)
            for (s in start..(len - remaining - blocks[bi])) {
                for (k in 0 until blocks[bi]) cur[s + k] = true
                rec(bi + 1, s + blocks[bi] + 1, cur)
                for (k in 0 until blocks[bi]) cur[s + k] = false
            }
        }
        rec(0, 0, BooleanArray(len))
        return out
    }

    /** Yalnızca satır/sütun mantığıyla çözülebiliyorsa true (tahmin gerektirmez). */
    fun lineSolvable(size: Int, rows: List<List<Int>>, cols: List<List<Int>>): Boolean {
        // 0 bilinmiyor, 1 dolu, 2 boş
        val grid = Array(size) { IntArray(size) }
        val rowArr = rows.map { arrangements(size, it).toMutableList() }
        val colArr = cols.map { arrangements(size, it).toMutableList() }
        var changed = true
        while (changed) {
            changed = false
            for (r in 0 until size) {
                val list = rowArr[r]
                list.retainAll { a -> (0 until size).all { c -> grid[r][c] == 0 || (grid[r][c] == 1) == a[c] } }
                if (list.isEmpty()) return false
                for (c in 0 until size) if (grid[r][c] == 0) {
                    val allT = list.all { it[c] }
                    val allF = list.none { it[c] }
                    if (allT) { grid[r][c] = 1; changed = true } else if (allF) { grid[r][c] = 2; changed = true }
                }
            }
            for (c in 0 until size) {
                val list = colArr[c]
                list.retainAll { a -> (0 until size).all { r -> grid[r][c] == 0 || (grid[r][c] == 1) == a[r] } }
                if (list.isEmpty()) return false
                for (r in 0 until size) if (grid[r][c] == 0) {
                    val allT = list.all { it[r] }
                    val allF = list.none { it[r] }
                    if (allT) { grid[r][c] = 1; changed = true } else if (allF) { grid[r][c] = 2; changed = true }
                }
            }
        }
        return grid.all { row -> row.all { it != 0 } }
    }

    override fun generate(index: Int, difficulty: Float, rng: Random): NonogramPuzzle? {
        val size = step(difficulty, 5, 6, 7, 8, 9)
        val density = 0.5f + rng.nextFloat() * 0.16f
        // Simetrik desenler daha "resim" gibi görünür; bazılarını aynala
        val mirror = rng.nextFloat() < 0.4f
        val cells = MutableList(size * size) { false }
        for (r in 0 until size) for (c in 0 until size) {
            if (mirror && c >= (size + 1) / 2) cells[r * size + c] = cells[r * size + (size - 1 - c)]
            else cells[r * size + c] = rng.nextFloat() < density
        }
        val rows = (0 until size).map { r -> clues((0 until size).map { c -> cells[r * size + c] }) }
        val cols = (0 until size).map { c -> clues((0 until size).map { r -> cells[r * size + c] }) }
        if (rows.any { it == listOf(0) } && tierOf(difficulty) < 2) return null
        if (!lineSolvable(size, rows, cols)) return null
        return NonogramPuzzle(size, cells, rows, cols)
    }

    override fun fingerprint(puzzle: NonogramPuzzle): String =
        "${puzzle.size}:" + puzzle.cells.joinToString("") { if (it) "1" else "0" }
}

// =====================================================================
// Zihin Döndürme
// =====================================================================

/** Hücre listesi (satır, sütun) olarak şekil. */
typealias Cells = List<Pair<Int, Int>>

object Shapes {
    fun normalize(cells: Cells): Cells {
        val minR = cells.minOf { it.first }
        val minC = cells.minOf { it.second }
        return cells.map { (it.first - minR) to (it.second - minC) }.sortedWith(compareBy({ it.first }, { it.second }))
    }

    /** Saat yönünde 90°. */
    fun rotate(cells: Cells): Cells = normalize(cells.map { (r, c) -> c to -r })

    fun rotate(cells: Cells, times: Int): Cells {
        var x = normalize(cells)
        repeat(((times % 4) + 4) % 4) { x = rotate(x) }
        return x
    }

    fun mirror(cells: Cells): Cells = normalize(cells.map { (r, c) -> r to -c })

    fun key(cells: Cells): String = normalize(cells).joinToString(";") { "${it.first},${it.second}" }

    /** Döndürme ve aynalama altında en küçük anahtar (eşdeğer şekiller aynı anahtar). */
    fun canonical(cells: Cells): String {
        val all = (0..3).flatMap { listOf(rotate(cells, it), rotate(mirror(cells), it)) }
        return all.map { key(it) }.min()
    }

    /** Rastgele bağlı polyomino. */
    fun randomPolyomino(k: Int, rng: Random, maxSpan: Int): Cells {
        val set = linkedSetOf(0 to 0)
        while (set.size < k) {
            val (r, c) = set.toList().pick(rng)
            val d = listOf(0 to 1, 1 to 0, 0 to -1, -1 to 0).pick(rng)
            val n = (r + d.first) to (c + d.second)
            val trial = set + n
            val spanR = trial.maxOf { it.first } - trial.minOf { it.first }
            val spanC = trial.maxOf { it.second } - trial.minOf { it.second }
            if (spanR < maxSpan && spanC < maxSpan) set += n
        }
        return normalize(set.toList())
    }

    fun width(cells: Cells) = cells.maxOf { it.second } + 1
    fun height(cells: Cells) = cells.maxOf { it.first } + 1
}

data class RotationPuzzle(val shape: Cells, val turns: Int, val options: List<Cells>, val answer: Int) {
    val angleText: String get() = when (turns) { 1 -> "90° saat yönünde"; 2 -> "180°"; else -> "90° saat yönünün tersine" }
}

object RotationGenerator : PuzzleGenerator<RotationPuzzle> {
    override val gameId = "spatial_rotation"

    override fun generate(index: Int, difficulty: Float, rng: Random): RotationPuzzle? {
        val k = scaled(difficulty, intArrayOf(4, 5, 6, 7, 8), extraWithin = 1)
        val span = step(difficulty, 3, 4, 4, 4, 5)
        val shape = Shapes.randomPolyomino(k, rng, span)
        val rots = (0..3).map { Shapes.key(Shapes.rotate(shape, it)) }
        val mirrors = (0..3).map { Shapes.key(Shapes.rotate(Shapes.mirror(shape), it)) }
        // Şekil kiral olmalı (ayna görüntüsü hiçbir dönmüş hâle eşit olmamalı) ve 4 dönüşü farklı olmalı
        if (rots.toSet().size < 4) return null
        if (mirrors.any { it in rots }) return null
        val turns = if (tierOf(difficulty) == 0) 1 + rng.nextInt(2) else listOf(1, 2, 3).pick(rng)
        val correct = Shapes.rotate(shape, turns)
        val distractors = ArrayList<Cells>()
        // Ayna tuzakları + yanlış açı
        distractors += Shapes.rotate(Shapes.mirror(shape), turns)
        distractors += Shapes.rotate(shape, (turns + 2) % 4)
        distractors += Shapes.rotate(Shapes.mirror(shape), (turns + 1) % 4)
        if (tierOf(difficulty) >= 3) distractors += Shapes.rotate(Shapes.mirror(shape), (turns + 3) % 4)
        val keys = (distractors.map { Shapes.key(it) } + Shapes.key(correct))
        if (keys.toSet().size != keys.size) return null
        val opts = (distractors + listOf(correct)).shuffled(rng)
        val answer = opts.indexOfFirst { Shapes.key(it) == Shapes.key(correct) }
        return RotationPuzzle(shape, turns, opts, answer)
    }

    override fun fingerprint(puzzle: RotationPuzzle): String = Shapes.key(puzzle.shape) + "#" + puzzle.turns
}

// =====================================================================
// Desen Ustası (3x3 matris)
// =====================================================================

/** shape 0..4, color 0..4, count 1..3, rot 0..3 (×45°) */
data class PatternCell(val shape: Int, val color: Int, val count: Int, val rot: Int)

data class PatternPuzzle(val grid: List<PatternCell>, val options: List<PatternCell>, val answer: Int, val rules: String) {
    val solution: PatternCell get() = grid[8]
}

object PatternGenerator : PuzzleGenerator<PatternPuzzle> {
    override val gameId = "pattern_master"

    private enum class Rule { CONST, PROGRESS, DISTRIBUTE }

    /** Bir özellik için 3x3 değer matrisi üretir. */
    private fun attrGrid(rule: Rule, values: Int, rng: Random, offsetBase: Int = 0): IntArray {
        val g = IntArray(9)
        when (rule) {
            Rule.CONST -> {
                // Her satırda sabit, satırdan satıra farklı
                val rowVals = (0 until values).shuffled(rng).take(3)
                for (r in 0 until 3) for (c in 0 until 3) g[r * 3 + c] = rowVals[r % rowVals.size]
            }
            Rule.PROGRESS -> {
                val stepV = 1
                for (r in 0 until 3) {
                    val start = (rng.nextInt(values) + offsetBase) % values
                    for (c in 0 until 3) g[r * 3 + c] = (start + c * stepV) % values
                }
            }
            Rule.DISTRIBUTE -> {
                val three = (0 until values).shuffled(rng).take(3)
                val shift = if (rng.nextBoolean()) 1 else 2
                for (r in 0 until 3) for (c in 0 until 3) g[r * 3 + c] = three[(c + r * shift) % 3]
            }
        }
        return g
    }

    override fun generate(index: Int, difficulty: Float, rng: Random): PatternPuzzle? {
        val tier = tierOf(difficulty)
        val varying = when (tier) { 0 -> 1; 1 -> 2; 2 -> 2; 3 -> 3; else -> 4 }
        // Özellikler: 0 şekil, 1 renk, 2 adet, 3 açı
        val attrs = (if (tier >= 3) listOf(0, 1, 2, 3) else listOf(0, 1, 2)).shuffled(rng).take(varying)
        val rules = HashMap<Int, Rule>()
        for (a in attrs) {
            rules[a] = when {
                a == 2 -> listOf(Rule.PROGRESS, Rule.DISTRIBUTE).pick(rng)
                tier == 0 -> listOf(Rule.CONST, Rule.DISTRIBUTE).pick(rng)
                else -> Rule.entries.pick(rng)
            }
        }
        val fixedShape = rng.nextInt(5)
        val fixedColor = rng.nextInt(5)
        val fixedCount = 1 + rng.nextInt(3)
        val shapeG = rules[0]?.let { attrGrid(it, 5, rng) } ?: IntArray(9) { fixedShape }
        val colorG = rules[1]?.let { attrGrid(it, 5, rng) } ?: IntArray(9) { fixedColor }
        val countG = rules[2]?.let { attrGrid(it, 3, rng).map { v -> v + 1 }.toIntArray() } ?: IntArray(9) { fixedCount }
        val rotG = rules[3]?.let { attrGrid(it, 4, rng) } ?: IntArray(9) { 0 }
        val grid = (0 until 9).map { PatternCell(shapeG[it], colorG[it], countG[it], rotG[it]) }
        val sol = grid[8]
        // Çeldiriciler: cevaptan tek bir özellikte farklı
        val nOpts = if (tier <= 1) 4 else 6
        val cands = LinkedHashSet<PatternCell>()
        var guard = 0
        while (cands.size < nOpts - 1 && guard++ < 200) {
            val which = (if (tier >= 3) listOf(0, 1, 2, 3) else listOf(0, 1, 2)).pick(rng)
            val alt = when (which) {
                0 -> sol.copy(shape = (sol.shape + 1 + rng.nextInt(4)) % 5)
                1 -> sol.copy(color = (sol.color + 1 + rng.nextInt(4)) % 5)
                2 -> sol.copy(count = 1 + (sol.count + rng.nextInt(2)) % 3)
                else -> sol.copy(rot = (sol.rot + 1 + rng.nextInt(3)) % 4)
            }
            if (alt != sol) cands += alt
        }
        if (cands.size < nOpts - 1) return null
        // Izgarada olan çeldiriciler kafa karıştırır, ama birebir aynısı ızgara kuralına uymasın
        val opts = (cands.toList() + sol).shuffled(rng)
        // Sıfır bilgi riskini azalt: cevap ilk 8 hücrede aynen varsa ve kural sabit değilse sorun yok
        val ruleText = attrs.joinToString(", ") { a ->
            val name = listOf("şekil", "renk", "adet", "açı")[a]
            val r = when (rules[a]) { Rule.CONST -> "satırda sabit"; Rule.PROGRESS -> "adım adım artıyor"; Rule.DISTRIBUTE -> "her satırda üç farklı değer"; null -> "" }
            "$name: $r"
        }
        return PatternPuzzle(grid, opts, opts.indexOf(sol), ruleText)
    }

    override fun fingerprint(puzzle: PatternPuzzle): String =
        puzzle.grid.joinToString("") { "${it.shape}${it.color}${it.count}${it.rot}" }
}

// =====================================================================
// Detay Avcısı (fark bulma)
// =====================================================================

private val DETAIL_POOLS = listOf(
    listOf("🍎", "🍏", "🍐", "🍊", "🍋", "🍌", "🍉", "🍇", "🍓", "🍒", "🍑", "🥝"),
    listOf("🐶", "🐱", "🐭", "🐹", "🐰", "🦊", "🐻", "🐼", "🐨", "🐯", "🦁", "🐮"),
    listOf("⚽", "🏀", "🏈", "⚾", "🎾", "🏐", "🏉", "🎱", "🏓", "🏸", "🥏", "🎳"),
    listOf("🚗", "🚕", "🚙", "🚌", "🚎", "🏎️", "🚓", "🚑", "🚒", "🚐", "🛻", "🚚"),
    listOf("🌞", "🌝", "🌚", "🌛", "🌜", "⭐", "🌟", "✨", "☄️", "🪐", "🌍", "🌙"),
    listOf("😀", "😃", "😄", "😁", "😆", "😅", "😂", "🙂", "😉", "😊", "😇", "🙃"),
    listOf("🌸", "🌼", "🌻", "🌺", "🌷", "🌹", "🥀", "💐", "🌱", "🍀", "🍁", "🍂"),
    listOf("🔴", "🟠", "🟡", "🟢", "🔵", "🟣", "🟤", "⚫", "⚪", "🟥", "🟦", "🟩")
)

data class DetailPuzzle(val size: Int, val original: List<String>, val modified: List<String>, val diffs: List<Int>, val timeLimitSec: Int)

object DetailGenerator : PuzzleGenerator<DetailPuzzle> {
    override val gameId = "detail_hunter"
    override val independent = true

    override fun generate(index: Int, difficulty: Float, rng: Random): DetailPuzzle? {
        val size = step(difficulty, 4, 5, 5, 6, 6)
        val k = scaled(difficulty, intArrayOf(2, 3, 3, 4, 5), extraWithin = 1)
        val tier = tierOf(difficulty)
        // Zorlaştıkça birbirine benzeyen semboller (aynı havuz, az çeşit)
        val pool = DETAIL_POOLS.pick(rng).shuffled(rng).take(step(difficulty, 10, 9, 8, 7, 6))
        val original = List(size * size) { pool.pick(rng) }
        val diffs = (0 until size * size).shuffled(rng).take(k).sorted()
        val modified = original.toMutableList()
        for (d in diffs) {
            var v: String
            do { v = pool.pick(rng) } while (v == original[d])
            modified[d] = v
        }
        val time = (k * step(difficulty, 14, 13, 12, 11, 10) + 10 - tier)
        return DetailPuzzle(size, original, modified, diffs, time)
    }

    override fun fingerprint(puzzle: DetailPuzzle) = puzzle.original.joinToString("") + "|" + puzzle.diffs
}

// =====================================================================
// Gizli Nesneyi Bul
// =====================================================================

data class SceneItem(val emoji: String, val x: Float, val y: Float, val size: Float, val rot: Float)

data class HiddenObjectPuzzle(val items: List<SceneItem>, val targets: List<Int>, val timeLimitSec: Int, val theme: String)

private val SCENES = listOf(
    "Mutfak" to listOf("🍳", "🥄", "🍴", "🔪", "🥣", "🧂", "🍞", "🧀", "🥚", "🥕", "🧅", "🧄", "🍅", "🥛", "☕", "🫖", "🍯", "🥐", "🍪", "🧁"),
    "Oyun Odası" to listOf("🧸", "🪀", "🪁", "🎲", "🧩", "🎮", "🕹️", "🎯", "🪄", "🎈", "🎨", "🖍️", "📚", "🚂", "🤖", "🦖", "⚽", "🏀", "🎸", "🥁"),
    "Deniz" to listOf("🐠", "🐟", "🐡", "🦈", "🐙", "🦑", "🦀", "🦞", "🐚", "🪸", "🐳", "🐬", "🦭", "🐢", "⚓", "🧜", "🌊", "🪼", "🦐", "🏝️"),
    "Uzay" to listOf("🚀", "🛸", "🛰️", "🌍", "🌙", "⭐", "☄️", "🪐", "👽", "👾", "🔭", "🌌", "🌞", "🧑‍🚀", "📡", "🌟", "💫", "🌑", "🌕", "🛰"),
    "Orman" to listOf("🌲", "🌳", "🍄", "🦔", "🦉", "🐿️", "🦊", "🐻", "🦌", "🐺", "🍂", "🌰", "🪵", "🐝", "🐞", "🦋", "🐌", "🌿", "🪺", "🦡"),
    "Market" to listOf("🍎", "🍌", "🍇", "🥖", "🥩", "🧃", "🥫", "🧈", "🥦", "🌽", "🍫", "🍬", "🧴", "🧻", "🛒", "🥥", "🍍", "🥑", "🫑", "🍋"),
    "Atölye" to listOf("🔨", "🪛", "🔧", "🪚", "⚙️", "🔩", "🧰", "🪜", "📏", "📐", "✂️", "🧲", "🪝", "⛓️", "🔦", "🪣", "🧱", "🪤", "🔌", "💡"),
    "Müzik" to listOf("🎹", "🎸", "🎻", "🎺", "🎷", "🥁", "🪘", "🪗", "🎤", "🎧", "📻", "🎼", "🎵", "🎶", "🪕", "📯", "🔔", "🪇", "💿", "📀")
)

object HiddenObjectGenerator : PuzzleGenerator<HiddenObjectPuzzle> {
    override val gameId = "hidden_object"
    override val independent = true

    override fun generate(index: Int, difficulty: Float, rng: Random): HiddenObjectPuzzle? {
        val (theme, pool) = SCENES.pick(rng)
        val count = scaled(difficulty, intArrayOf(18, 26, 32, 40, 48), extraWithin = 6)
        val targetsN = step(difficulty, 3, 4, 4, 5, 6)
        val baseSize = step(difficulty, 0.13f, 0.115f, 0.1f, 0.09f, 0.08f)
        // Hedefler sahnede tek olmalı; diğerleri birden fazla olabilir
        val targetEmojis = pool.shuffled(rng).take(targetsN)
        val others = pool.filter { it !in targetEmojis }
        val items = ArrayList<SceneItem>()
        val emojis = targetEmojis + List(count - targetsN) { others.pick(rng) }
        val placed = ArrayList<Pair<Float, Float>>()
        for (e in emojis) {
            var tries = 0
            var x: Float
            var y: Float
            val s = baseSize * (0.8f + rng.nextFloat() * 0.5f)
            do {
                x = 0.04f + rng.nextFloat() * 0.92f
                y = 0.04f + rng.nextFloat() * 0.92f
                tries++
            } while (tries < 60 && placed.any { (px, py) -> (px - x) * (px - x) + (py - y) * (py - y) < (s * 0.75f) * (s * 0.75f) })
            placed += x to y
            items += SceneItem(e, x, y, s, (rng.nextFloat() - 0.5f) * step(difficulty, 20f, 40f, 60f, 90f, 120f))
        }
        val order = items.indices.shuffled(rng)
        val shuffled = order.map { items[it] }
        val targets = targetEmojis.map { t -> shuffled.indexOfFirst { it.emoji == t } }
        val time = targetsN * step(difficulty, 14, 12, 11, 10, 9) + 8
        return HiddenObjectPuzzle(shuffled, targets, time, theme)
    }

    override fun fingerprint(puzzle: HiddenObjectPuzzle) =
        puzzle.theme + puzzle.items.joinToString("") { it.emoji + (it.x * 20).toInt() + (it.y * 20).toInt() }
}
