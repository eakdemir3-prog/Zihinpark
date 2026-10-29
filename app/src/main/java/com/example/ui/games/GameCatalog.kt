package com.example.ui.games

import com.example.ui.game.RoundContent

/** Oyun kimliği -> tur içeriği eşlemesi. */
object GameCatalog {
    private val map: Map<String, RoundContent> = mapOf(
        "logic_detective" to { s, c, f -> DetectiveRound(s, c, f) },
        "number_chain" to { s, c, f -> NumberChainRound(s, c, f) },
        "memory_cards" to { s, c, f -> MemoryCardsRound(s, c, f) },
    )

    fun contentFor(gameId: String): RoundContent =
        map[gameId] ?: { s, c, f -> NumberChainRound(s, c, f) }

    val ids: Set<String> get() = map.keys
}
