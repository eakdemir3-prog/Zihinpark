package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.NeonBg
import com.example.ui.theme.ZpColors

/** Oyunlara göre arka plan desen sembolleri. Harf/rakam setleri kutucuklu, diğerleri serbest sembol olarak çizilir. */
object BgPatterns {
    const val LETTERS = "ABCÇDEFGĞHIİKLMNOÖPRSŞTUÜVYZ"
    const val NUMBERS = "0123456789+−×÷="
    private const val LOGIC = "?!✓✗≠∴?!"
    private const val SHAPES = "◆▲●■◇△○□"
    private const val ATTENTION = "✦★✧◉✦★"
    private const val MOVE = "➜↗⇄⚡➜↘"
    private const val CARDS = "♠♥♦♣★◆"

    fun forGame(id: String?): String = when (id) {
        "word_hunt", "word_scramble", "cipher_breaker", "lingo", "bulmaca", "wow" -> LETTERS
        "math_engineer", "number_chain", "number_merge", "sudoku" -> NUMBERS
        "logic_detective", "reverse_thinking", "logic_doors" -> LOGIC
        "spatial_rotation", "nonogram", "shape_architect", "pattern_master", "jigsaw_puzzle", "block_blast", "neon_connect" -> SHAPES
        "detail_hunter", "hidden_object", "color_stroop", "neon_match", "memory_lights" -> ATTENTION
        "memory_cards" -> CARDS
        else -> MOVE
    }

    fun tiled(p: String) = p == LETTERS || p == NUMBERS
}

private class PatItem(val x: Float, val y: Float, val rot: Float, val color: Color, val text: TextLayoutResult)

/** Ekrana dağılmış, hafif eğik, soluk semboller. Çizim önbelleğe alınır; animasyon yok. */
@Composable
fun PatternLayer(glyphs: String, colors: List<Color>, modifier: Modifier = Modifier) {
    val tm = rememberTextMeasurer()
    val light = ZpColors.palette.isLight
    val tiled = BgPatterns.tiled(glyphs)
    val chars = remember(glyphs) { glyphs.codePoints().toArray().map { String(Character.toChars(it)) } }
    Box(
        modifier.fillMaxSize().graphicsLayer().drawWithCache {
            val cell = 96.dp.toPx()
            val tile = 36.dp.toPx()
            val cols = (size.width / cell).toInt().coerceAtLeast(3)
            val rows = (size.height / cell).toInt() + 1
            val cw = size.width / cols
            val rnd = java.util.Random(glyphs.hashCode().toLong() * 31 + cols)
            val style = TextStyle(fontSize = if (tiled) 20.sp else 28.sp, fontWeight = FontWeight.ExtraBold)
            val items = ArrayList<PatItem>()
            for (r in 0 until rows) for (c in 0 until cols) {
                if (rnd.nextFloat() > 0.62f) continue
                val x = c * cw + rnd.nextFloat() * (cw - tile)
                val y = r * cell + rnd.nextFloat() * (cell - tile)
                val ch = chars[rnd.nextInt(chars.size)]
                items += PatItem(x, y, rnd.nextFloat() * 50f - 25f, colors[rnd.nextInt(colors.size)], tm.measure(ch, style))
            }
            val tileAlpha = if (light) 0.13f else 0.10f
            val glyphAlpha = if (light) 0.16f else 0.12f
            val radius = CornerRadius(9.dp.toPx())
            onDrawBehind {
                for (it in items) {
                    val center = Offset(it.x + tile / 2, it.y + tile / 2)
                    rotate(it.rot, center) {
                        val tl = Offset(center.x - it.text.size.width / 2f, center.y - it.text.size.height / 2f)
                        if (tiled) {
                            drawRoundRect(it.color.copy(alpha = tileAlpha), Offset(it.x, it.y), Size(tile, tile), radius)
                            drawText(it.text, color = Color.White.copy(alpha = if (light) 0.75f else 0.35f), topLeft = tl)
                        } else {
                            drawText(it.text, color = it.color.copy(alpha = glyphAlpha), topLeft = tl)
                        }
                    }
                }
            }
        }
    )
}

/** Gerçek fotoğraf + okunabilirlik için tema renginde perde (Kelime Diyarı, Bulmaca Dünyası). */
@Composable
fun PhotoBackground(resName: String?, content: @Composable BoxScope.() -> Unit) {
    val context = LocalContext.current
    val res = remember(resName) { if (resName == null) 0 else context.resources.getIdentifier(resName, "drawable", context.packageName) }
    Box(Modifier.fillMaxSize().background(NeonBg)) {
        if (res != 0) Image(painterResource(res), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        val light = ZpColors.palette.isLight
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    if (light) listOf(Color.White.copy(alpha = 0.55f), NeonBg.copy(alpha = 0.72f), NeonBg.copy(alpha = 0.92f))
                    else listOf(Color.Black.copy(alpha = 0.45f), NeonBg.copy(alpha = 0.72f), NeonBg.copy(alpha = 0.93f))
                )
            )
        )
        content()
    }
}
