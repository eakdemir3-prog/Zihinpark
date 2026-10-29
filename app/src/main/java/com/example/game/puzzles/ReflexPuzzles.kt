package com.example.game.puzzles

import com.example.game.engine.PuzzleGenerator
import com.example.game.engine.scaled
import com.example.game.engine.step
import com.example.game.engine.tierOf
import com.example.game.engine.withinTier
import kotlin.random.Random

// =====================================================================
// Hafıza Işıkları
// =====================================================================

data class MemoryLightsPuzzle(val pads: Int, val sequence: List<Int>, val intervalMs: Int)

object MemoryLightsGenerator : PuzzleGenerator<MemoryLightsPuzzle> {
    override val gameId = "memory_lights"

    override fun generate(index: Int, difficulty: Float, rng: Random): MemoryLightsPuzzle? {
        val pads = step(difficulty, 4, 4, 6, 6, 9)
        val len = scaled(difficulty, intArrayOf(3, 5, 5, 6, 7), extraWithin = 2)
        val seq = ArrayList<Int>()
        while (seq.size < len) {
            val v = rng.nextInt(pads)
            // Kolay bölgede art arda aynı ışık yok
            if (tierOf(difficulty) == 0 && seq.lastOrNull() == v) continue
            seq += v
        }
        val interval = (step(difficulty, 720, 620, 560, 480, 420) - withinTier(difficulty) * 60).toInt()
        return MemoryLightsPuzzle(pads, seq, interval)
    }

    override fun fingerprint(puzzle: MemoryLightsPuzzle) = "${puzzle.pads}:" + puzzle.sequence.joinToString("")
}

// =====================================================================
// Refleks Arenası
// =====================================================================

/** Bir deneme: [waitMs] sonra yeşil yanar; öncesinde sahte renkler ([decoys]: zaman, renk). */
data class ReflexTrial(val waitMs: Int, val decoys: List<Pair<Int, Int>>, val targetX: Float, val targetY: Float)

data class ReflexPuzzle(val trials: List<ReflexTrial>, val targetMs: Int, val shrinkTarget: Boolean)

object ReflexGenerator : PuzzleGenerator<ReflexPuzzle> {
    override val gameId = "reflex_arena"

    override fun generate(index: Int, difficulty: Float, rng: Random): ReflexPuzzle? {
        val tier = tierOf(difficulty)
        val count = step(difficulty, 3, 4, 4, 5, 5)
        val trials = List(count) {
            val wait = 1100 + rng.nextInt(2400)
            val decoys = if (tier >= 1 && rng.nextFloat() < 0.35f + tier * 0.12f) {
                val n = if (tier >= 3) 1 + rng.nextInt(2) else 1
                (0 until n).map { k -> ((wait * (k + 1)) / (n + 1)) to rng.nextInt(2) }
            } else emptyList()
            ReflexTrial(wait, decoys, 0.15f + rng.nextFloat() * 0.7f, 0.15f + rng.nextFloat() * 0.7f)
        }
        val target = step(difficulty, 650, 560, 500, 450, 410)
        return ReflexPuzzle(trials, target, shrinkTarget = tier >= 2)
    }

    override fun fingerprint(puzzle: ReflexPuzzle) =
        puzzle.trials.joinToString("|") { t -> "${t.waitMs}:${t.decoys}:${(t.targetX * 100).toInt()},${(t.targetY * 100).toInt()}" }
}

// =====================================================================
// Renk Kaçışı (Stroop)
// =====================================================================

/** Renk indeksleri: 0 Kırmızı, 1 Mavi, 2 Yeşil, 3 Sarı, 4 Mor, 5 Turuncu. */
val STROOP_NAMES = listOf("KIRMIZI", "MAVİ", "YEŞİL", "SARI", "MOR", "TURUNCU")

/** [byMeaning] true ise cevap kelimenin anlamı, değilse yazının rengidir. */
data class StroopItem(val word: Int, val ink: Int, val byMeaning: Boolean) {
    val answer: Int get() = if (byMeaning) word else ink
}

data class StroopPuzzle(val colors: Int, val items: List<StroopItem>, val timeLimitSec: Int, val allowedMistakes: Int)

object StroopGenerator : PuzzleGenerator<StroopPuzzle> {
    override val gameId = "color_stroop"

    override fun generate(index: Int, difficulty: Float, rng: Random): StroopPuzzle? {
        val tier = tierOf(difficulty)
        val colors = step(difficulty, 4, 4, 5, 6, 6)
        val n = scaled(difficulty, intArrayOf(8, 10, 10, 12, 14), extraWithin = 2)
        val items = List(n) {
            val word = rng.nextInt(colors)
            var ink = rng.nextInt(colors)
            // Kolayda bazen uyumlu, zorlaştıkça hep çelişkili
            val congruentChance = step(difficulty, 0.25f, 0.1f, 0f, 0f, 0f)
            if (rng.nextFloat() >= congruentChance) while (ink == word) ink = rng.nextInt(colors)
            val byMeaning = when (tier) {
                0, 1 -> false
                2 -> rng.nextFloat() < 0.3f
                else -> rng.nextFloat() < 0.5f
            }
            StroopItem(word, ink, byMeaning)
        }
        val perItem = step(difficulty, 2.6f, 2.2f, 2.2f, 1.9f, 1.6f)
        return StroopPuzzle(colors, items, (n * perItem).toInt() + 3, allowedMistakes = step(difficulty, 3, 3, 3, 2, 2))
    }

    override fun fingerprint(puzzle: StroopPuzzle) =
        "${puzzle.colors}:" + puzzle.items.joinToString("") { "${it.word}${it.ink}${if (it.byMeaning) "m" else "r"}" }
}

// =====================================================================
// Hedefe Dokun
// =====================================================================

/** Şekil: 0 daire, 1 kare, 2 üçgen, 3 yıldız. Renk: 0 mavi, 1 pembe, 2 yeşil, 3 sarı, 4 mor. */
val TARGET_SHAPES = listOf("daire", "kare", "üçgen", "yıldız")
val TARGET_SHAPES_PLURAL = listOf("daireler", "kareler", "üçgenler", "yıldızlar")
val TARGET_COLORS = listOf("mavi", "pembe", "yeşil", "sarı", "mor")

data class TargetSpawn(
    val atMs: Int, val x: Float, val y: Float, val shape: Int, val color: Int,
    val lifeMs: Int, val vx: Float, val vy: Float
)

/** Kural: [kind] 0 renk, 1 şekil, 2 renk-değil, 3 renk+şekil, 4 renk-ya-da-şekil. */
data class TargetRule(val kind: Int, val color: Int, val shape: Int) {
    fun matches(shape: Int, color: Int): Boolean = when (kind) {
        0 -> color == this.color
        1 -> shape == this.shape
        2 -> color != this.color
        3 -> color == this.color && shape == this.shape
        else -> color == this.color || shape == this.shape
    }

    val text: String
        get() = when (kind) {
            0 -> "Sadece ${TARGET_COLORS[color].uppercase(Turkish.trLocale)} hedeflere dokun"
            1 -> "Sadece ${TARGET_SHAPES_PLURAL[shape].uppercase(Turkish.trLocale)} dokun"
            2 -> "${TARGET_COLORS[color].uppercase(Turkish.trLocale)} OLMAYAN her şeye dokun"
            3 -> "Sadece ${TARGET_COLORS[color].uppercase(Turkish.trLocale)} ${TARGET_SHAPES[shape].uppercase(Turkish.trLocale)}"
            else -> "${TARGET_COLORS[color].uppercase(Turkish.trLocale)} olan YA DA ${TARGET_SHAPES[shape].uppercase(Turkish.trLocale)} olan"
        }
}

data class TargetTapPuzzle(val rule: TargetRule, val spawns: List<TargetSpawn>, val quota: Int, val timeLimitSec: Int)

object TargetTapGenerator : PuzzleGenerator<TargetTapPuzzle> {
    override val gameId = "target_tap"

    override fun generate(index: Int, difficulty: Float, rng: Random): TargetTapPuzzle? {
        val tier = tierOf(difficulty)
        val kind = when (tier) {
            0 -> rng.nextInt(2)
            1 -> rng.nextInt(3)
            2 -> 1 + rng.nextInt(3)
            else -> 2 + rng.nextInt(3)
        }
        val rule = TargetRule(kind, rng.nextInt(TARGET_COLORS.size), rng.nextInt(TARGET_SHAPES.size))
        val quota = scaled(difficulty, intArrayOf(8, 10, 11, 12, 14), extraWithin = 2)
        val time = step(difficulty, 25, 24, 23, 22, 22)
        val life = step(difficulty, 2200, 1900, 1600, 1350, 1150)
        val moving = tier >= 3
        val spawns = ArrayList<TargetSpawn>()
        var t = 300
        var matching = 0
        val gap = step(difficulty, 520, 460, 420, 380, 340)
        while (t < time * 1000 - 600) {
            // Eşleşen hedef oranını yaklaşık %45 tut
            val wantMatch = rng.nextFloat() < 0.45f
            var shape: Int
            var color: Int
            var tries = 0
            do {
                shape = rng.nextInt(TARGET_SHAPES.size)
                color = rng.nextInt(TARGET_COLORS.size)
                tries++
            } while (rule.matches(shape, color) != wantMatch && tries < 40)
            if (rule.matches(shape, color)) matching++
            val vx = if (moving) (rng.nextFloat() - 0.5f) * 0.25f else 0f
            val vy = if (moving) (rng.nextFloat() - 0.5f) * 0.25f else 0f
            spawns += TargetSpawn(t, 0.1f + rng.nextFloat() * 0.8f, 0.1f + rng.nextFloat() * 0.8f, shape, color, life, vx, vy)
            t += gap + rng.nextInt(gap / 2)
        }
        if (matching < quota * 1.5f) return null
        return TargetTapPuzzle(rule, spawns, quota, time)
    }

    override fun fingerprint(puzzle: TargetTapPuzzle) =
        "${puzzle.rule}|" + puzzle.spawns.joinToString(",") { "${it.atMs}${it.shape}${it.color}${(it.x * 50).toInt()}" }
}
