package com.example.ui.wow

import android.content.Context
import androidx.compose.ui.graphics.Color
import org.json.JSONObject

/** Kelime Diyarı verisi: assets/wow.json (tools/gen_wow.py ile üretilir). */
data class WowWord(val text: String, val row: Int, val col: Int, val horizontal: Boolean) {
    fun cells(): List<Pair<Int, Int>> = text.indices.map { if (horizontal) row to (col + it) else (row + it) to col }
}

data class WowLevel(
    val number: Int,
    val chapter: Int,
    val letters: String,
    val words: List<WowWord>,
    val rows: Int,
    val cols: Int,
    val bonus: Set<String>
) {
    val cellMap: Map<Pair<Int, Int>, Char> by lazy {
        val m = HashMap<Pair<Int, Int>, Char>()
        words.forEach { w -> w.cells().forEachIndexed { i, rc -> m[rc] = w.text[i] } }
        m
    }
}

data class WowChapter(val index: Int, val name: String, val emoji: String) {
    val firstLevel get() = index * WowData.PER_CHAPTER + 1
    val lastLevel get() = firstLevel + WowData.PER_CHAPTER - 1
    val colors: Pair<Color, Color> get() = CHAPTER_COLORS[index % CHAPTER_COLORS.size]
}

private val CHAPTER_COLORS = listOf(
    Color(0xFFFF2E97) to Color(0xFF7B2FFF), // İstanbul
    Color(0xFFFF8A00) to Color(0xFFFF2E97), // Kapadokya
    Color(0xFF00F0FF) to Color(0xFFE0F7FF), // Pamukkale
    Color(0xFFFFC857) to Color(0xFFB026FF), // Efes
    Color(0xFFFF5E3A) to Color(0xFF6A3DFF), // Nemrut
    Color(0xFF39FF14) to Color(0xFF00B3A4), // Uzungöl
    Color(0xFF00C2FF) to Color(0xFFFFE600), // Antalya
    Color(0xFFB026FF) to Color(0xFF3D7BFF), // Paris
    Color(0xFFFF8A00) to Color(0xFFFF3B5C), // Roma
    Color(0xFFFFE600) to Color(0xFFFF8A00), // Mısır
    Color(0xFFFF2E97) to Color(0xFFFFFFFF), // Japonya
    Color(0xFF3D7BFF) to Color(0xFF00F0FF), // New York
    Color(0xFF39FF14) to Color(0xFFFFE600), // Rio
    Color(0xFF00F0FF) to Color(0xFF39FF14), // İzlanda
    Color(0xFFB026FF) to Color(0xFF39FF14)  // Machu Picchu
)

object WowData {
    const val PER_CHAPTER = 20

    @Volatile private var cache: Pair<List<WowChapter>, List<WowLevel>>? = null

    fun load(context: Context): Pair<List<WowChapter>, List<WowLevel>> {
        cache?.let { return it }
        synchronized(this) {
            cache?.let { return it }
            val text = context.assets.open("wow.json").bufferedReader(Charsets.UTF_8).use { it.readText() }
            val root = JSONObject(text)
            val ch = root.getJSONArray("chapters")
            val chapters = (0 until ch.length()).map { i ->
                val o = ch.getJSONObject(i)
                WowChapter(i, o.getString("name"), o.getString("emoji"))
            }
            val lv = root.getJSONArray("levels")
            val levels = (0 until lv.length()).map { i ->
                val o = lv.getJSONObject(i)
                val ws = o.getJSONArray("words")
                val bonus = o.getJSONArray("bonus")
                WowLevel(
                    number = o.getInt("n"),
                    chapter = o.getInt("ch"),
                    letters = o.getString("letters"),
                    words = (0 until ws.length()).map { k ->
                        val w = ws.getJSONObject(k)
                        WowWord(w.getString("t"), w.getInt("r"), w.getInt("c"), w.getString("d") == "H")
                    },
                    rows = o.getInt("rows"),
                    cols = o.getInt("cols"),
                    bonus = (0 until bonus.length()).map { bonus.getString(it) }.toSet()
                )
            }
            return (chapters to levels).also { cache = it }
        }
    }

    val LEVEL_COUNT = 300
}
