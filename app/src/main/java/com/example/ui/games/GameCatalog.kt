package com.example.ui.games

import com.example.ui.game.RoundContent

/** Oyun kimliği -> tur içeriği eşlemesi. */
object GameCatalog {
    private val map: Map<String, RoundContent> = mapOf(
        "logic_detective" to { s, c, f -> DetectiveRound(s, c, f) },
        "number_chain" to { s, c, f -> NumberChainRound(s, c, f) },
        "memory_cards" to { s, c, f -> MemoryCardsRound(s, c, f) },
        "memory_lights" to { s, c, f -> MemoryLightsRound(s, c, f) },
        "reflex_arena" to { s, c, f -> ReflexRound(s, c, f) },
        "color_stroop" to { s, c, f -> StroopRound(s, c, f) },
        "target_tap" to { s, c, f -> TargetTapRound(s, c, f) },
        "nonogram" to { s, c, f -> NonogramRound(s, c, f) },
        "spatial_rotation" to { s, c, f -> RotationRound(s, c, f) },
        "pattern_master" to { s, c, f -> PatternRound(s, c, f) },
        "detail_hunter" to { s, c, f -> DetailRound(s, c, f) },
        "hidden_object" to { s, c, f -> HiddenObjectRound(s, c, f) },
        "cipher_breaker" to { s, c, f -> CipherRound(s, c, f) },
        "word_hunt" to { s, c, f -> WordHuntRound(s, c, f) },
        "logic_doors" to { s, c, f -> DoorsRound(s, c, f) },
        "reverse_thinking" to { s, c, f -> ReverseRound(s, c, f) },
    )

    fun contentFor(gameId: String): RoundContent =
        map[gameId] ?: { s, c, f -> NumberChainRound(s, c, f) }

    val ids: Set<String> get() = map.keys
}
