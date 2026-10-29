package com.example.game.puzzles

import com.example.game.engine.PuzzleGenerator
import kotlin.random.Random

/** Sayı Zinciri: kuralı bulunacak dizi ve eksik terim. */
data class NumberChainPuzzle(
    val terms: List<Long>,
    val missing: Int,
    val options: List<Long>,
    val rule: String
) {
    val answer: Long get() = terms[missing]
}

object NumberChainGenerator : PuzzleGenerator<NumberChainPuzzle> {
    override val gameId = "number_chain"

    private class Built(val terms: List<Long>, val rule: String, val family: String)

    private fun arithmetic(rng: Random, len: Int, maxStep: Int): Built {
        val d = (rng.nextInt(2, maxStep + 1)) * if (rng.nextFloat() < 0.25f) -1 else 1
        val start = if (d < 0) rng.nextLong(len * -d.toLong() + 5, len * -d.toLong() + 60) else rng.nextLong(1, 40)
        return Built(List(len) { start + it * d.toLong() }, "Her adımda ${if (d > 0) "+" else "−"}${kotlin.math.abs(d)}", "ar$d")
    }

    private fun geometric(rng: Random, len: Int): Built {
        val r = listOf(2L, 3L, 2L, 4L, 5L).random(rng)
        val start = rng.nextLong(1, if (r >= 4) 4 else 8)
        return Built(List(len) { start * pow(r, it) }, "Her adımda ×$r", "geo$r")
    }

    private fun alternating(rng: Random, len: Int): Built {
        val a = rng.nextInt(3, 12)
        var b = rng.nextInt(1, 9)
        if (b == a) b++
        val start = rng.nextLong(5, 40)
        val t = ArrayList<Long>()
        var x = start
        for (i in 0 until len) { t += x; x += if (i % 2 == 0) a else -b }
        return Built(t, "Sırayla +$a ve −$b", "alt$a,$b")
    }

    private fun growingDiff(rng: Random, len: Int): Built {
        val d0 = rng.nextInt(1, 5)
        val inc = rng.nextInt(1, 4)
        val start = rng.nextLong(1, 30)
        val t = ArrayList<Long>()
        var x = start
        var d = d0
        for (i in 0 until len) { t += x; x += d; d += inc }
        return Built(t, "Farklar her adımda $inc artıyor (+$d0, +${d0 + inc}, ...)", "gd$d0,$inc")
    }

    private fun squares(rng: Random, len: Int): Built {
        val off = rng.nextInt(1, 6)
        val add = rng.nextInt(-3, 8)
        return Built(
            List(len) { val n = (it + off).toLong(); n * n + add },
            "n² ${if (add >= 0) "+ $add" else "− ${-add}"} (n = $off, ${off + 1}, ...)", "sq$off,$add"
        )
    }

    private fun cubes(rng: Random, len: Int): Built {
        val off = rng.nextInt(1, 4)
        val add = rng.nextInt(0, 5)
        return Built(List(len) { val n = (it + off).toLong(); n * n * n + add }, "n³ + $add (n = $off, ${off + 1}, ...)", "cu$off,$add")
    }

    private fun fibonacciLike(rng: Random, len: Int): Built {
        val a = rng.nextLong(1, 9)
        val b = rng.nextLong(a, a + 9)
        val t = arrayListOf(a, b)
        while (t.size < len) t += t[t.size - 1] + t[t.size - 2]
        return Built(t, "Her terim önceki iki terimin toplamı", "fib$a,$b")
    }

    private fun interleaved(rng: Random, len: Int): Built {
        val a0 = rng.nextLong(1, 20); val da = rng.nextLong(2, 8)
        val b0 = rng.nextLong(20, 60); val db = -rng.nextLong(1, 6)
        val t = List(len) { i -> if (i % 2 == 0) a0 + (i / 2) * da else b0 + (i / 2) * db }
        return Built(t, "İç içe iki dizi: tek sıralar +$da, çift sıralar ${db}", "il$a0,$da,$b0,$db")
    }

    private fun mulAdd(rng: Random, len: Int): Built {
        val m = listOf(2L, 2L, 3L).random(rng)
        val a = rng.nextLong(-3, 5).let { if (it == 0L) 1L else it }
        val start = rng.nextLong(1, 6)
        val t = arrayListOf(start)
        while (t.size < len) t += t.last() * m + a
        return Built(t, "Her terim: önceki × $m ${if (a > 0) "+ $a" else "− ${-a}"}", "ma$m,$a,$start")
    }

    private fun twoOps(rng: Random, len: Int): Built {
        val m = listOf(2L, 3L).random(rng)
        val s = rng.nextLong(1, 10)
        val start = rng.nextLong(2, 9)
        val t = arrayListOf(start)
        var i = 0
        while (t.size < len) { t += if (i % 2 == 0) t.last() * m else t.last() - s; i++ }
        return Built(t, "Sırayla ×$m ve −$s", "to$m,$s,$start")
    }

    private fun triangular(rng: Random, len: Int): Built {
        val off = rng.nextInt(1, 5)
        val k = rng.nextLong(1, 4)
        return Built(List(len) { val n = (it + off).toLong(); k * n * (n + 1) / 2 }, "$k × n(n+1)/2 (üçgen sayılar)", "tri$off,$k")
    }

    private fun secondOrder(rng: Random, len: Int): Built {
        val start = rng.nextLong(1, 10)
        val d0 = rng.nextLong(1, 4)
        val m = rng.nextLong(2, 3)
        val t = ArrayList<Long>()
        var x = start; var d = d0
        for (i in 0 until len) { t += x; x += d; d *= m }
        return Built(t, "Farklar ×$m büyüyor (+$d0, +${d0 * m}, ...)", "so$start,$d0,$m")
    }

    private fun pow(b: Long, e: Int): Long { var r = 1L; repeat(e) { r *= b }; return r }

    override fun generate(index: Int, difficulty: Float, rng: Random): NumberChainPuzzle? {
        val tier = (difficulty * 5).toInt().coerceIn(0, 4)
        val len = when (tier) { 0 -> 5; 1 -> 6; 2 -> 6; else -> 7 }
        val families: List<(Random, Int) -> Built> = when (tier) {
            0 -> listOf({ r, l -> arithmetic(r, l, 9) }, { r, l -> geometric(r, l) }, { r, l -> arithmetic(r, l, 12) })
            1 -> listOf(::alternating, ::growingDiff, ::squares, { r, l -> geometric(r, l) }, { r, l -> arithmetic(r, l, 25) })
            2 -> listOf(::fibonacciLike, ::interleaved, ::squares, ::growingDiff, ::triangular, ::alternating)
            3 -> listOf(::mulAdd, ::cubes, ::interleaved, ::fibonacciLike, ::secondOrder, ::triangular)
            else -> listOf(::twoOps, ::secondOrder, ::mulAdd, ::cubes, ::interleaved, ::fibonacciLike)
        }
        val built = families[rng.nextInt(families.size)](rng, len)
        if (built.terms.any { kotlin.math.abs(it) > 99_999 }) return null
        val missing = when (tier) {
            0 -> len - 1
            1 -> if (rng.nextBoolean()) len - 1 else rng.nextInt(2, len)
            else -> rng.nextInt(1, len)
        }
        val answer = built.terms[missing]
        // Yakın ve akla yatkın çeldiriciler
        val neighbor = if (missing > 0) built.terms[missing - 1] else built.terms[1]
        val diff = kotlin.math.abs(answer - neighbor).coerceAtLeast(1)
        val cands = linkedSetOf<Long>()
        val pool = listOf(answer + 1, answer - 1, answer + 2, answer - 2, answer + diff, answer - diff,
            answer + 10, answer - 10, answer * 2, answer + diff / 2 + 1, neighbor + diff + 1, answer + 3)
        for (c in pool.shuffled(rng)) if (c != answer && c >= 0) cands += c
        val distractors = cands.take(3)
        if (distractors.size < 3) return null
        val options = (distractors + answer).shuffled(rng)
        return NumberChainPuzzle(built.terms, missing, options, built.rule)
    }

    override fun fingerprint(puzzle: NumberChainPuzzle): String =
        puzzle.terms.joinToString(",") + "#" + puzzle.missing
}
