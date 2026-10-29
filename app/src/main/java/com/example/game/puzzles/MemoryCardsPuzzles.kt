package com.example.game.puzzles

import com.example.game.engine.PuzzleGenerator
import com.example.game.engine.pickN
import com.example.game.engine.step
import kotlin.random.Random

/** Neon Hafıza Kartları: çiftlerin yerleşimi. */
data class MemoryCardsPuzzle(
    val cols: Int,
    val rows: Int,
    /** Her kartın sembol indeksi (her sembol iki kez). */
    val cards: List<Int>,
    val symbols: List<String>,
    /** Başta tüm kartların gösterileceği süre (ms). */
    val peekMs: Int,
    val timeLimitSec: Int
) {
    val pairs: Int get() = cards.size / 2
}

object MemoryCardsGenerator : PuzzleGenerator<MemoryCardsPuzzle> {
    override val gameId = "memory_cards"
    override val independent = true

    val SYMBOLS = listOf(
        "🚀", "🌙", "⚡", "💎", "🔮", "🎧", "🎮", "🛸", "🌈", "🔥", "🍀", "🎯", "🧬", "🪐", "🌵", "🦄",
        "🐙", "🦊", "🐬", "🦋", "🍩", "🍉", "🍒", "🎲", "🎸", "🎨", "🏆", "⭐", "❄️", "🌊", "🌋", "🍄",
        "🤖", "👾", "🧠", "🗝️", "⏳", "📡", "🧲", "🛰️"
    )

    override fun generate(index: Int, difficulty: Float, rng: Random): MemoryCardsPuzzle? {
        val (cols, rows) = step(difficulty, 3 to 4, 4 to 4, 4 to 5, 4 to 6, 5 to 6)
        val pairs = cols * rows / 2
        // Üst seviyelerde birbirine benzeyen sembolleri aynı tura koy
        val symbols = SYMBOLS.pickN(pairs, rng)
        val cards = (0 until pairs).flatMap { listOf(it, it) }.shuffled(rng)
        val peek = step(difficulty, 2200, 1600, 1100, 700, 0)
        val time = pairs * step(difficulty, 9, 8, 7, 6, 5) + 10
        return MemoryCardsPuzzle(cols, rows, cards, symbols, peek, time)
    }

    override fun fingerprint(puzzle: MemoryCardsPuzzle): String =
        "${puzzle.cols}x${puzzle.rows}:" + puzzle.cards.joinToString("") { puzzle.symbols[it] }
}
