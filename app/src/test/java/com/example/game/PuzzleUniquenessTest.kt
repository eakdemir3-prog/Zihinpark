package com.example.game

import com.example.game.engine.LevelRules
import com.example.game.engine.PuzzleBank
import com.example.game.engine.PuzzleGenerator
import com.example.game.puzzles.AllGenerators
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Her oyunun 210 turunun birbirinden farklı olduğunu (parmak iziyle) ve her seferinde
 * aynı sırayla üretildiğini doğrular.
 */
class PuzzleUniquenessTest {

    @Suppress("UNCHECKED_CAST")
    private fun <P : Any> check(gen: PuzzleGenerator<P>) {
        PuzzleBank.clear()
        val t0 = System.currentTimeMillis()
        val all = PuzzleBank.all(gen)
        println("${gen.gameId}: 210 tur ${System.currentTimeMillis() - t0} ms")
        assertEquals(gen.gameId, LevelRules.TOTAL_ROUNDS, all.size)
        val fps = all.map { gen.fingerprint(it) }
        assertEquals("${gen.gameId}: tekrar eden tur var", fps.size, fps.toSet().size)
        // Belirlenimci: yeniden üretim aynı sonucu verir
        PuzzleBank.clear()
        val again = PuzzleBank.all(gen).map { gen.fingerprint(it) }
        assertEquals("${gen.gameId}: üretim belirlenimci değil", fps, again)
        // Sonsuz mod da bankayla çakışmaz
        val endless = (0 until 20).map { gen.fingerprint(PuzzleBank.endless(gen, it, 0.9f)) }
        assertTrue("${gen.gameId}: sonsuz mod bankayla çakışıyor", endless.none { it in fps.toSet() })
    }

    @Test
    fun `tum oyunlarda 210 benzersiz tur`() {
        for (g in AllGenerators.list) check(g as PuzzleGenerator<Any>)
    }
}
