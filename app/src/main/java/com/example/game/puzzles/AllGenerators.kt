package com.example.game.puzzles

import com.example.game.engine.PuzzleGenerator

/** Tüm oyunların bulmaca üreticileri (birim testleri benzersizliği buradan doğrular). */
object AllGenerators {
    val list: List<PuzzleGenerator<*>> = listOf(
        DetectiveGenerator,
        NumberChainGenerator,
        MemoryCardsGenerator,
        MemoryLightsGenerator,
        ReflexGenerator,
        StroopGenerator,
        TargetTapGenerator,
        NonogramGenerator,
        RotationGenerator,
        PatternGenerator,
        DetailGenerator,
        HiddenObjectGenerator,
        CipherGenerator,
        WordHuntGenerator,
        DoorsGenerator,
        ReverseGenerator,
        PathFinderGenerator,
        MazeEscapeGenerator,
        RiverGenerator,
        ArchitectGenerator,
        MathGridGenerator,
        MergeGenerator,
        JigsawGenerator,
        SortGenerator,
        BlockBlastGenerator,
        Match3Generator,
        SudokuGenerator,
        FlowGenerator,
    )
}
