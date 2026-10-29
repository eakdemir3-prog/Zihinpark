package com.example.ui.games

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.engine.RoundSpec
import com.example.game.puzzles.ArrowGenerator
import com.example.game.puzzles.ArrowPuzzle
import com.example.game.puzzles.ScrambleGenerator
import com.example.game.puzzles.ScramblePuzzle
import com.example.ui.components.shake
import com.example.ui.game.RoundControls
import com.example.ui.theme.NeonBg
import com.example.ui.theme.NeonBorder
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonOrange
import com.example.ui.theme.NeonYellow
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// =====================================================================
// Ok Bulmacası
// =====================================================================

@Composable
fun ArrowRound(spec: RoundSpec, ctl: RoundControls, finish: (Boolean, Int) -> Unit) {
    PuzzleHost(ArrowGenerator, spec) { p -> ArrowBoard(p, ctl, rememberFinish(finish)) }
}

@Composable
private fun ArrowBoard(p: ArrowPuzzle, ctl: RoundControls, finish: FinishOnce) {
    val scope = rememberCoroutineScope()
    val n = p.n
    val alive = remember { mutableStateListOf(*Array(p.arrows.size) { true }) }
    val leaving = remember { mutableStateMapOf<Int, Float>() }
    val history = remember { mutableStateListOf<Int>() }
    var hinted by remember { mutableStateOf<Int?>(null) }
    var blocked by remember { mutableStateOf<Int?>(null) }
    var bump by remember { mutableIntStateOf(0) }
    var done by remember { mutableStateOf(false) }
    val glow = remember { Animatable(0f) }

    fun occ(): IntArray {
        val o = IntArray(n * n) { -1 }
        p.arrows.forEachIndexed { i, a -> if (alive[i]) a.cells.forEach { o[it] = i } }
        return o
    }
    fun remaining() = alive.count { it }
    fun status() { ctl.statusText = "${p.arrows.size - remaining()}/${p.arrows.size} ok" }

    LaunchedEffect(Unit) {
        ctl.setLives(3)
        status()
        ctl.startTimer(p.arrows.size * 4 + 25) {
            done = true; ctl.mistake("Süre bitti")
            scope.launch { delay(900); finish(false, 0) }
        }
        ctl.hint = hint@{
            if (done) return@hint null
            val o = occ()
            val free = p.arrows.indices.firstOrNull { alive[it] && ArrowGenerator.blocker(n, o, p.arrows[it], it) == -1 } ?: return@hint null
            hinted = free
            scope.launch { glow.snapTo(0f); glow.animateTo(1f, tween(1800)); if (hinted == free) hinted = null }
            "Parlayan ok çıkabilir"
        }
        ctl.undo = undo@{
            if (done) return@undo false
            val last = history.removeLastOrNull() ?: return@undo false
            alive[last] = true
            leaving.remove(last)
            status()
            true
        }
    }

    fun tapCell(cell: Int) {
        if (done) return
        val o = occ()
        val id = o[cell]
        if (id == -1 || id in leaving) return
        val b = ArrowGenerator.blocker(n, o, p.arrows[id], id)
        if (b != -1) {
            blocked = id; bump++
            scope.launch { delay(450); if (blocked == id) blocked = null }
            if (ctl.mistake("Önü kapalı!")) {
                done = true; ctl.stopTimer()
                scope.launch { delay(900); finish(false, 0) }
            }
            return
        }
        ctl.tap()
        history += id
        if (hinted == id) hinted = null
        scope.launch {
            val anim = Animatable(0f)
            leaving[id] = 0f
            anim.animateTo(1f, tween(380)) { leaving[id] = value }
            alive[id] = false
            leaving.remove(id)
            status()
            if (remaining() == 0 && !done) {
                done = true
                ctl.stopTimer()
                ctl.correct("Hepsi çıktı!")
                val q = (100 - ctl.mistakes * 22 - (ctl.timeUsedRatio * 30).toInt()).coerceIn(15, 100)
                delay(800); finish(true, q)
            }
        }
    }

    GameColumn(scroll = false) {
        PromptCard("Oka dokun, gösterdiği yöne kayıp çıksın", accent = NeonCyan, sub = "Önü kapalı oka dokunursan can gider")
        BoxWithConstraints(
            Modifier
                .weight(1f, fill = false)
                .widthIn(max = 460.dp)
                .fillMaxWidth()
                .aspectRatio(1f)
                .shake(bump, 4.dp)
                .clip(RoundedCornerShape(16.dp))
        ) {
            Canvas(
                Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures { pos ->
                            val cs = size.width / n.toFloat()
                            val r = (pos.y / cs).toInt().coerceIn(0, n - 1)
                            val c = (pos.x / cs).toInt().coerceIn(0, n - 1)
                            tapCell(r * n + c)
                        }
                    }
            ) {
                val cs = size.width / n
                drawRect(NeonBg)
                for (r in 0 until n) for (c in 0 until n) {
                    drawCircle(NeonBorder.copy(alpha = 0.6f), cs * 0.06f, Offset(c * cs + cs / 2, r * cs + cs / 2))
                }
                p.arrows.forEachIndexed { i, a ->
                    if (!alive[i]) return@forEachIndexed
                    val prog = leaving[i] ?: 0f
                    val shift = prog * size.width * 1.1f
                    val ox = ArrowGenerator.DC[a.dir] * shift
                    val oy = ArrowGenerator.DR[a.dir] * shift
                    val base = PALETTE10[i % 8]
                    val color = when {
                        blocked == i -> Color(0xFFFF3B5C)
                        else -> base
                    }
                    val alpha = 1f - prog * 0.6f
                    val path = Path()
                    a.cells.forEachIndexed { k, cell ->
                        val x = (cell % n) * cs + cs / 2 + ox
                        val y = (cell / n) * cs + cs / 2 + oy
                        if (k == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    if (hinted == i) {
                        drawPath(path, NeonYellow.copy(alpha = 0.35f + 0.35f * kotlin.math.sin(glow.value * 18f).let { it * it }),
                            style = Stroke(cs * 0.55f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                    }
                    drawPath(path, color.copy(alpha = 0.25f * alpha), style = Stroke(cs * 0.42f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                    drawPath(path, color.copy(alpha = alpha), style = Stroke(cs * 0.2f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                    // Ok başı
                    val hx = (a.head % n) * cs + cs / 2 + ox
                    val hy = (a.head / n) * cs + cs / 2 + oy
                    val dx = ArrowGenerator.DC[a.dir].toFloat()
                    val dy = ArrowGenerator.DR[a.dir].toFloat()
                    val tip = Offset(hx + dx * cs * 0.42f, hy + dy * cs * 0.42f)
                    val back = Offset(hx - dx * cs * 0.02f, hy - dy * cs * 0.02f)
                    val px = -dy; val py = dx
                    val head = Path().apply {
                        moveTo(tip.x, tip.y)
                        lineTo(back.x + px * cs * 0.3f, back.y + py * cs * 0.3f)
                        lineTo(back.x - px * cs * 0.3f, back.y - py * cs * 0.3f)
                        close()
                    }
                    drawPath(head, color.copy(alpha = alpha))
                    // Kuyruk noktası
                    val t = a.cells.first()
                    drawCircle(color.copy(alpha = alpha), cs * 0.13f, Offset((t % n) * cs + cs / 2 + ox, (t / n) * cs + cs / 2 + oy))
                }
            }
        }
    }
}

// =====================================================================
// Karışık Kelime
// =====================================================================

@Composable
fun ScrambleRound(spec: RoundSpec, ctl: RoundControls, finish: (Boolean, Int) -> Unit) {
    PuzzleHost(ScrambleGenerator, spec) { p -> ScrambleBoard(p, ctl, rememberFinish(finish)) }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ScrambleBoard(p: ScramblePuzzle, ctl: RoundControls, finish: FinishOnce) {
    val scope = rememberCoroutineScope()
    val word = p.word
    // slots[i] = kullanılan karo indeksi (ya da -1)
    val slots = remember { mutableStateListOf(*Array(word.length) { -1 }) }
    val locked = remember { mutableStateListOf(*Array(word.length) { false }) }
    var done by remember { mutableStateOf(false) }
    var wrongKey by remember { mutableIntStateOf(0) }
    var success by remember { mutableStateOf(false) }

    fun used(t: Int) = t in slots
    fun current() = slots.joinToString("") { if (it >= 0) p.tiles[it].toString() else "" }

    fun check() {
        if (slots.any { it < 0 }) return
        if (current() == word) {
            done = true; success = true
            ctl.stopTimer()
            ctl.correct("Doğru: $word")
            val q = (100 - ctl.mistakes * 20 - (ctl.timeUsedRatio * 40).toInt()).coerceIn(15, 100)
            scope.launch { delay(1000); finish(true, q) }
        } else {
            wrongKey++
            if (ctl.mistake("Olmadı")) {
                done = true; ctl.stopTimer()
                ctl.statusText = "Cevap: $word"
                scope.launch { delay(1400); finish(false, 0) }
            } else {
                scope.launch {
                    delay(450)
                    for (i in slots.indices) if (!locked[i]) slots[i] = -1
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        ctl.setLives(3)
        ctl.statusText = "${word.length} harf"
        ctl.startTimer(p.timeSec) {
            done = true; ctl.mistake("Süre bitti")
            ctl.statusText = "Cevap: $word"
            scope.launch { delay(1400); finish(false, 0) }
        }
        ctl.hint = hint@{
            if (done) return@hint null
            val i = (0 until word.length).firstOrNull { !locked[it] && (slots[it] < 0 || p.tiles[slots[it]] != word[it]) }
                ?: (0 until word.length).firstOrNull { !locked[it] } ?: return@hint null
            // Bu harfi taşıyan, kilitli olmayan bir karo bul
            for (k in slots.indices) if (!locked[k]) slots[k] = -1
            val t = p.tiles.indices.firstOrNull { p.tiles[it] == word[i] && !used(it) } ?: return@hint null
            slots[i] = t; locked[i] = true
            check()
            "${i + 1}. harf: ${word[i]}"
        }
    }

    GameColumn {
        PromptCard("💡 ${p.category}", accent = NeonOrange, sub = if (p.decoys > 0) "Dikkat: ${p.decoys} fazla harf var" else "Harfleri doğru sıraya diz")
        // Cevap kutuları
        FlowRow(
            Modifier.fillMaxWidth().shake(wrongKey, 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val sz = if (word.length <= 6) 50.dp else if (word.length <= 8) 40.dp else 32.dp
            for (i in word.indices) {
                val t = slots[i]
                NeonTile(
                    Modifier.size(sz),
                    state = when {
                        success -> TileState.Correct
                        locked[i] -> TileState.Hint
                        t >= 0 -> TileState.Selected
                        else -> TileState.Normal
                    },
                    accent = NeonCyan,
                    corner = 10.dp,
                    onClick = { if (!done && !locked[i] && t >= 0) { slots[i] = -1; ctl.tap() } }
                ) {
                    Text(if (t >= 0) p.tiles[t].toString() else "", color = TextPrimary, fontSize = (if (word.length <= 6) 24 else 19).sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Box(Modifier.height(10.dp))
        // Harf karoları
        FlowRow(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            p.tiles.forEachIndexed { idx, ch ->
                val u = used(idx)
                NeonTile(
                    Modifier.size(58.dp).graphicsLayer { alpha = if (u) 0.22f else 1f },
                    state = TileState.Normal,
                    accent = NeonYellow,
                    corner = 16.dp,
                    onClick = {
                        val free = slots.indexOfFirst { it < 0 }
                        if (!done && !u && free >= 0) { slots[free] = idx; ctl.tap(); check() }
                    }
                ) {
                    Text(ch.toString(), color = if (u) TextMuted else TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            NeonTile(Modifier.height(44.dp).widthIn(min = 120.dp), accent = NeonGreen, corner = 12.dp, onClick = {
                if (!done) for (i in slots.indices) if (!locked[i]) slots[i] = -1
            }) { Text("Temizle", color = TextPrimary, fontWeight = FontWeight.SemiBold) }
        }
    }
}
