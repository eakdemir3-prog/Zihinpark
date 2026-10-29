package com.example.game

import com.example.game.engine.GameProgress
import com.example.game.engine.LevelRules
import com.example.game.engine.RoundResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LevelRulesTest {

    @Test
    fun `her oyunda 210 tur var`() {
        assertEquals(210, LevelRules.TOTAL_ROUNDS)
        assertEquals(0, LevelRules.globalRoundIndex(1, 0))
        assertEquals(209, LevelRules.globalRoundIndex(50, 4))
    }

    @Test
    fun `zorluk bolgelerle hizali ve artan`() {
        var prev = -1f
        for (l in 1..50) for (r in 0 until LevelRules.roundsFor(l)) {
            val d = LevelRules.difficulty(l, r)
            assertTrue("$l/$r", d >= prev)
            assertEquals("bölge $l", LevelRules.zoneOf(l), (d * 5).toInt())
            prev = d
        }
    }

    @Test
    fun `yildiz kurallari`() {
        fun r(ok: Boolean, q: Int = 90) = RoundResult(ok, q, 0)
        assertEquals(3, LevelRules.starsFor(listOf(r(true), r(true), r(true))))
        assertEquals(2, LevelRules.starsFor(listOf(r(true, 50), r(true, 50), r(true, 50))))
        assertEquals(2, LevelRules.starsFor(listOf(r(true), r(true), r(false))))
        assertEquals(0, LevelRules.starsFor(listOf(r(true), r(false), r(false))))
        assertEquals(1, LevelRules.starsFor(listOf(r(true), r(true), r(true), r(false), r(false))))
        assertEquals(0, LevelRules.starsFor(listOf(r(true), r(true), r(false), r(false), r(false))))
    }

    @Test
    fun `kilitler ve bolge gecisi`() {
        val empty = GameProgress(emptyMap())
        assertTrue(empty.isUnlocked(1))
        assertFalse(empty.isUnlocked(2))
        assertFalse(empty.isUnlocked(11))

        // 1..10 hepsi 1 yıldız: 10 yıldız < 20, bölge 2 kilitli
        val low = GameProgress((1..10).associateWith { 1 })
        assertTrue(low.isUnlocked(10))
        assertFalse(low.isUnlocked(11))

        // 20 yıldız ve boss geçilmiş: açık
        val ok = GameProgress((1..10).associateWith { 2 })
        assertTrue(ok.isUnlocked(11))
        assertFalse(ok.isUnlocked(12))
        assertEquals(11, ok.nextPlayableLevel)

        // Boss geçilmeden yıldız yetse de kilitli
        val noBoss = GameProgress((1..9).associateWith { 3 })
        assertFalse(noBoss.isUnlocked(11))
        assertFalse(noBoss.isEndlessUnlocked)
    }

    @Test
    fun `elmas yalnizca ilk gecis kurali`() {
        assertEquals(2, LevelRules.firstClearDiamonds(1))
        assertEquals(10, LevelRules.firstClearDiamonds(10))
        assertEquals(3, LevelRules.firstClearDiamonds(21))
    }
}
