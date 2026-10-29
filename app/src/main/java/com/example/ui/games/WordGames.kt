package com.example.ui.games

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.engine.RoundSpec
import com.example.game.puzzles.CipherGenerator
import com.example.game.puzzles.CipherPuzzle
import com.example.game.puzzles.DoorsGenerator
import com.example.game.puzzles.DoorsPuzzle
import com.example.game.puzzles.ReverseGenerator
import com.example.game.puzzles.ReversePuzzle
import com.example.game.puzzles.WordData
import com.example.game.puzzles.WordHuntGenerator
import com.example.game.puzzles.WordHuntPuzzle
import com.example.ui.components.bounceClick
import com.example.ui.components.neonGlow
import com.example.ui.components.pop
import com.example.ui.components.shake
import com.example.ui.game.RoundControls
import com.example.ui.theme.NeonBorder
import com.example.ui.theme.NeonCard
import com.example.ui.theme.NeonCardHigh
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonOrange
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.NeonRed
import com.example.ui.theme.NeonYellow
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sign
import kotlin.math.sin

// =====================================================================
// Şifre Kırıcı
// =====================================================================

@Composable
fun CipherRound(spec: RoundSpec, ctl: RoundControls, finish: (Boolean, Int) -> Unit) {
    PuzzleHost(CipherGenerator, spec) { p -> CipherBoard(p, ctl, rememberFinish(finish)) }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CipherBoard(p: CipherPuzzle, ctl: RoundControls, finish: FinishOnce) {
    val scope = rememberCoroutineScope()
    var offset by remember { mutableIntStateOf(0) }
    val picked = remember { mutableStateListOf<Int>() } // taş indeksleri
    var done by remember { mutableStateOf(false) }
    var shakeKey by remember { mutableIntStateOf(0) }
    val answerLen = p.plain.count { it != ' ' }
    val accent = NeonPink
    var checkRef: (() -> Unit)? = null

    LaunchedEffect(Unit) {
        ctl.setLives(3)
        ctl.statusText = "Konu: ${p.category}"
        ctl.startTimer(answerLen * 8 + 25) {
            done = true; ctl.mistake("Süre bitti")
            scope.launch { delay(900); finish(false, 0) }
        }
        ctl.hint = hint@{
            if (done) return@hint null
            val target = p.plain.filter { it != ' ' }
            // Yanlış yerleştirilmiş harfleri temizle, sıradaki doğru harfi koy
            var keep = 0
            while (keep < picked.size && p.tiles[picked[keep]] == target[keep]) keep++
            while (picked.size > keep) picked.removeAt(picked.size - 1)
            if (picked.size >= target.length) return@hint null
            val need = target[picked.size]
            val tile = p.tiles.indices.firstOrNull { it !in picked && p.tiles[it] == need } ?: return@hint null
            picked += tile
            offset = p.shift
            if (picked.size == target.length) checkRef?.invoke()
            "Kaydırma ${p.shift}, sıradaki harf: $need"
        }
    }

    fun check() {
        val built = picked.map { p.tiles[it] }.joinToString("")
        val target = p.plain.filter { it != ' ' }
        if (built.length < target.length) return
        if (built == target) {
            done = true
            ctl.stopTimer()
            ctl.correct("Şifre çözüldü!")
            scope.launch { delay(1000); finish(true, (100 - ctl.mistakes * 25 - (ctl.timeUsedRatio * 35).toInt()).coerceIn(10, 100)) }
        } else {
            shakeKey++
            if (ctl.mistake("Yanlış")) {
                done = true; ctl.stopTimer()
                scope.launch { delay(1100); finish(false, 0) }
            } else scope.launch { delay(500); picked.clear() }
        }
    }

    checkRef = { check() }

    GameColumn {
        PromptCard(
            if (p.showShift) "Her harf ${p.shift} adım ileri kaydırılmış." else "Harfler bilinmeyen bir miktarda kaydırılmış.",
            accent = accent, sub = "Çarkı çevir, kelimeyi çöz · Konu: ${p.category}"
        )
        // Şifreli metin
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally), modifier = Modifier.fillMaxWidth()) {
            p.cipher.forEach { ch ->
                if (ch == ' ') Spacer(Modifier.width(12.dp)) else LetterBox(ch, NeonPurple, small = p.cipher.length > 9)
            }
        }
        // Canlı çözüm (kolay bölgelerde)
        if (p.liveDecode) {
            val preview = CipherGenerator.shiftText(p.cipher, -offset)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally), modifier = Modifier.fillMaxWidth()) {
                preview.forEach { ch ->
                    if (ch == ' ') Spacer(Modifier.width(12.dp)) else LetterBox(ch, if (offset == p.shift) NeonGreen else TextMuted, small = p.cipher.length > 9)
                }
            }
        }
        CipherWheel(offset, accent) { offset = ((it % 29) + 29) % 29; ctl.tap() }
        // Cevap yuvaları
        val target = p.plain
        var slot = 0
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
            modifier = Modifier.fillMaxWidth().shake(shakeKey)
        ) {
            target.forEach { ch ->
                if (ch == ' ') Spacer(Modifier.width(12.dp))
                else {
                    val idx = slot++
                    val tile = picked.getOrNull(idx)
                    val shownCh = tile?.let { p.tiles[it] }
                    Box(
                        Modifier
                            .size(if (target.length > 9) 28.dp else 34.dp, 40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (shownCh != null) accent.copy(alpha = 0.18f) else NeonCard)
                            .border(1.dp, if (done) NeonGreen else if (shownCh != null) accent else NeonBorder, RoundedCornerShape(8.dp))
                            .bounceClick { if (!done && tile != null) picked.removeAt(idx).also { while (picked.size > idx) picked.removeAt(picked.size - 1) } },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(shownCh?.toString() ?: "", color = TextPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                    }
                }
            }
        }
        // Harf taşları
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            p.tiles.forEachIndexed { i, ch ->
                val used = i in picked
                NeonTile(
                    Modifier.size(46.dp),
                    state = if (used) TileState.Disabled else TileState.Normal,
                    accent = accent,
                    onClick = {
                        if (done || used) return@NeonTile
                        if (picked.size < answerLen) {
                            picked += i
                            ctl.tap()
                            if (picked.size == answerLen) check()
                        }
                    }
                ) { Text("$ch", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp) }
            }
        }
    }
}

@Composable
private fun LetterBox(ch: Char, color: Color, small: Boolean) {
    Box(
        Modifier
            .size(if (small) 26.dp else 32.dp, 38.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.6f), RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) { Text("$ch", color = color, fontWeight = FontWeight.ExtraBold, fontSize = if (small) 15.sp else 18.sp) }
}

/** Sezar çarkı: dış halka şifreli harf, iç halka geri kaydırılmış (düz) harf. Sürükleyerek veya oklarla çevrilir. */
@Composable
private fun CipherWheel(offset: Int, accent: Color, onChange: (Int) -> Unit) {
    val alphabet = WordData.ALPHABET
    val anim by animateFloatAsState(offset.toFloat(), spring(dampingRatio = 0.7f, stiffness = 300f), label = "wheel")
    var dragAcc by remember { mutableStateOf(0f) }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        NeonTile(Modifier.size(44.dp), accent = accent, onClick = { onChange(offset - 1) }) { Text("◀", color = accent, fontSize = 18.sp) }
        Box(
            Modifier
                .size(190.dp)
                .pointerInput(offset) {
                    detectHorizontalDragGestures(onDragEnd = { dragAcc = 0f }) { change, d ->
                        change.consume()
                        dragAcc += d
                        val stepPx = 22.dp.toPx()
                        if (abs(dragAcc) > stepPx) {
                            onChange(offset + sign(dragAcc).toInt())
                            dragAcc = 0f
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val r = size.minDimension / 2
                drawCircle(NeonCard, r)
                drawCircle(accent.copy(alpha = 0.6f), r, style = Stroke(3f))
                drawCircle(NeonPurple.copy(alpha = 0.6f), r * 0.66f, style = Stroke(2f))
                drawLine(NeonYellow, Offset(center.x, 4f), Offset(center.x, r * 0.5f), strokeWidth = 4f, cap = StrokeCap.Round)
            }
            // Harfler
            for (i in alphabet.indices) {
                val angle = Math.toRadians((i * 360.0 / alphabet.length) - 90.0 - anim * 360.0 / alphabet.length)
                val outerR = 80f
                val innerR = 52f
                val isTop = ((i - offset) % alphabet.length + alphabet.length) % alphabet.length == 0
                Text(
                    "${alphabet[i]}",
                    color = if (isTop) NeonYellow else TextSecondary,
                    fontSize = if (isTop) 15.sp else 11.sp,
                    fontWeight = if (isTop) FontWeight.ExtraBold else FontWeight.Medium,
                    modifier = Modifier.graphicsLayer {
                        translationX = (outerR * cos(angle)).toFloat() * density
                        translationY = (outerR * sin(angle)).toFloat() * density
                    }
                )
                val plainIdx = ((i - offset) % alphabet.length + alphabet.length) % alphabet.length
                Text(
                    "${alphabet[plainIdx]}",
                    color = if (isTop) NeonGreen else TextMuted,
                    fontSize = if (isTop) 14.sp else 10.sp,
                    fontWeight = if (isTop) FontWeight.ExtraBold else FontWeight.Normal,
                    modifier = Modifier.graphicsLayer {
                        translationX = (innerR * cos(angle)).toFloat() * density
                        translationY = (innerR * sin(angle)).toFloat() * density
                    }
                )
            }
            Text("−$offset", color = accent, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
        }
        NeonTile(Modifier.size(44.dp), accent = accent, onClick = { onChange(offset + 1) }) { Text("▶", color = accent, fontSize = 18.sp) }
    }
}

// =====================================================================
// Kelime Avı
// =====================================================================

@Composable
fun WordHuntRound(spec: RoundSpec, ctl: RoundControls, finish: (Boolean, Int) -> Unit) {
    PuzzleHost(WordHuntGenerator, spec) { p -> WordHuntBoard(p, ctl, rememberFinish(finish)) }
}

private val FOUND_COLORS = listOf(NeonCyan, NeonPink, NeonGreen, NeonYellow, NeonPurple, NeonOrange)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WordHuntBoard(p: WordHuntPuzzle, ctl: RoundControls, finish: FinishOnce) {
    val scope = rememberCoroutineScope()
    val found = remember { mutableStateListOf<Int>() } // placement indeksleri
    var dragStart by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var dragEnd by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var done by remember { mutableStateOf(false) }
    var hinted by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(Unit) {
        ctl.setLives(4)
        ctl.statusText = "${p.theme} · 0/${p.placements.size}"
        ctl.startTimer(p.placements.size * 18 + 20) {
            done = true; ctl.mistake("Süre bitti")
            scope.launch { delay(900); finish(false, 0) }
        }
        ctl.hint = hint@{
            if (done) return@hint null
            val i = p.placements.indices.firstOrNull { it !in found && it != hinted } ?: p.placements.indices.firstOrNull { it !in found } ?: return@hint null
            hinted = i
            "${p.placements[i].word}: ilk harfi parlıyor"
        }
    }

    fun lineCells(a: Pair<Int, Int>, b: Pair<Int, Int>): List<Pair<Int, Int>>? {
        val dr = b.first - a.first
        val dc = b.second - a.second
        if (!(dr == 0 || dc == 0 || abs(dr) == abs(dc))) return null
        val n = max(abs(dr), abs(dc))
        val sr = sign(dr.toFloat()).toInt()
        val sc = sign(dc.toFloat()).toInt()
        return (0..n).map { (a.first + it * sr) to (a.second + it * sc) }
    }

    fun release() {
        val a = dragStart ?: return
        val b = dragEnd ?: a
        dragStart = null; dragEnd = null
        if (done || a == b) return
        val cells = lineCells(a, b) ?: return
        val word = cells.map { p.at(it.first, it.second) }.joinToString("")
        val idx = p.placements.indexOfFirst { pl ->
            pl.word.length == cells.size && (pl.cells() == cells || pl.cells().reversed() == cells)
        }
        if (idx >= 0 && idx !in found) {
            found += idx
            if (hinted == idx) hinted = null
            ctl.correct(p.placements[idx].word)
            ctl.statusText = "${p.theme} · ${found.size}/${p.placements.size}"
            if (found.size == p.placements.size) {
                done = true; ctl.stopTimer()
                scope.launch { delay(900); finish(true, (100 - ctl.mistakes * 20 - (ctl.timeUsedRatio * 35).toInt()).coerceIn(10, 100)) }
            }
        } else if (word.length >= 3 && idx < 0) {
            if (ctl.mistake()) {
                done = true; ctl.stopTimer()
                scope.launch { delay(900); finish(false, 0) }
            }
        }
    }

    GameColumn(scroll = false) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally), verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            p.placements.forEachIndexed { i, pl ->
                val f = i in found
                val c = FOUND_COLORS[i % FOUND_COLORS.size]
                Text(
                    pl.word,
                    color = if (f) c else if (hinted == i) NeonYellow else TextPrimary,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelLarge.copy(textDecoration = if (f) androidx.compose.ui.text.style.TextDecoration.LineThrough else null),
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (f) c.copy(alpha = 0.15f) else NeonCard)
                        .border(1.dp, if (f) c else NeonBorder, RoundedCornerShape(50))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
        BoxWithConstraints(Modifier.weight(1f, fill = false).widthIn(max = 440.dp).fillMaxWidth().aspectRatio(1f)) {
            val cellPx = constraints.maxWidth.toFloat() / p.size
            val selected = dragStart?.let { a -> lineCells(a, dragEnd ?: a) } ?: emptyList()
            Box(
                Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
                    .background(NeonCard.copy(alpha = 0.85f))
                    .border(1.dp, NeonBorder, RoundedCornerShape(16.dp))
                    .pointerInput(Unit) {
                        fun cellAt(o: Offset): Pair<Int, Int> =
                            (o.y / cellPx).toInt().coerceIn(0, p.size - 1) to (o.x / cellPx).toInt().coerceIn(0, p.size - 1)
                        detectDragGestures(
                            onDragStart = { o -> if (!done) { dragStart = cellAt(o); dragEnd = dragStart } },
                            onDragEnd = { release() },
                            onDragCancel = { dragStart = null; dragEnd = null }
                        ) { change, _ ->
                            change.consume()
                            dragEnd = cellAt(change.position)
                        }
                    }
            ) {
                // Bulunan kelime çizgileri
                Canvas(Modifier.fillMaxSize()) {
                    found.forEach { i ->
                        val pl = p.placements[i]
                        val c = FOUND_COLORS[i % FOUND_COLORS.size]
                        val a = Offset((pl.col + 0.5f) * cellPx, (pl.row + 0.5f) * cellPx)
                        val e = pl.cells().last()
                        val b = Offset((e.second + 0.5f) * cellPx, (e.first + 0.5f) * cellPx)
                        drawLine(c.copy(alpha = 0.3f), a, b, strokeWidth = cellPx * 0.78f, cap = StrokeCap.Round)
                    }
                    if (selected.size >= 2) {
                        val a = selected.first(); val b = selected.last()
                        drawLine(NeonYellow.copy(alpha = 0.35f), Offset((a.second + 0.5f) * cellPx, (a.first + 0.5f) * cellPx),
                            Offset((b.second + 0.5f) * cellPx, (b.first + 0.5f) * cellPx), strokeWidth = cellPx * 0.78f, cap = StrokeCap.Round)
                    }
                }
                Column {
                    for (r in 0 until p.size) Row {
                        for (c in 0 until p.size) {
                            val inSel = (r to c) in selected
                            val isHintStart = hinted?.let { p.placements[it].row == r && p.placements[it].col == c } == true
                            Box(
                                Modifier.size(with(androidx.compose.ui.platform.LocalDensity.current) { cellPx.toDp() })
                                    .then(if (isHintStart) Modifier.neonGlow(NeonYellow, 10.dp, 10.dp, 0.9f) else Modifier),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "${p.at(r, c)}",
                                    color = if (inSel) NeonYellow else TextPrimary,
                                    fontWeight = if (inSel) FontWeight.ExtraBold else FontWeight.SemiBold,
                                    fontSize = (if (p.size >= 9) 16 else 19).sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// =====================================================================
// Doğru Kapıyı Seç
// =====================================================================

@Composable
fun DoorsRound(spec: RoundSpec, ctl: RoundControls, finish: (Boolean, Int) -> Unit) {
    PuzzleHost(DoorsGenerator, spec) { p -> DoorsBoard(p, ctl, rememberFinish(finish)) }
}

@Composable
private fun DoorsBoard(p: DoorsPuzzle, ctl: RoundControls, finish: FinishOnce) {
    val scope = rememberCoroutineScope()
    var opened by remember { mutableStateOf<Int?>(null) }
    val wrong = remember { mutableStateListOf<Int>() }
    var done by remember { mutableStateOf(false) }
    val started = remember { System.currentTimeMillis() }

    LaunchedEffect(Unit) {
        ctl.setLives(2)
        ctl.statusText = "Tek kapı güvenli"
        ctl.hint = hint@{
            if (done) return@hint null
            val d = p.statements.indices.firstOrNull { it != p.safe && it !in wrong } ?: return@hint null
            if (p.statements.indices.count { it !in wrong } <= 1) return@hint null
            wrong += d
            "${d + 1}. kapı tuzak, elendi"
        }
    }

    GameColumn {
        PromptCard(p.ruleText, accent = NeonPink, sub = "Yalnızca BİR kapının arkası güvenli.")
        val n = p.statements.size
        val perRow = if (n <= 3) n else if (n == 4) 2 else 3
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            p.statements.indices.chunked(perRow).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { i ->
                        DoorView(
                            number = i + 1,
                            text = p.statements[i].text,
                            open = opened == i || (done && i == p.safe),
                            safe = i == p.safe,
                            crossed = i in wrong,
                            modifier = Modifier.weight(1f)
                        ) {
                            if (done || i in wrong) return@DoorView
                            if (i == p.safe) {
                                opened = i; done = true
                                ctl.correct("Güvenli!")
                                val secs = (System.currentTimeMillis() - started) / 1000f
                                scope.launch { delay(1100); finish(true, (100 - ctl.mistakes * 40 - ((secs - 25f).coerceAtLeast(0f) * 0.8f).toInt()).coerceIn(10, 100)) }
                            } else {
                                wrong += i
                                if (ctl.mistake("Tuzak!")) {
                                    done = true
                                    scope.launch { delay(1300); finish(false, 0) }
                                }
                            }
                        }
                    }
                    repeat(perRow - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun DoorView(number: Int, text: String, open: Boolean, safe: Boolean, crossed: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val rot by animateFloatAsState(if (open) -70f else 0f, tween(600), label = "door")
    val color by animateColorAsState(
        when { crossed -> NeonRed; open && safe -> NeonGreen; else -> NeonPurple }, label = "dc"
    )
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text, color = TextPrimary, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(NeonCard)
                .border(1.dp, color.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                .padding(6.dp)
        )
        Spacer(Modifier.height(8.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(0.62f)
                .clip(RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp, bottomStart = 6.dp, bottomEnd = 6.dp))
                .background(Brush.verticalGradient(listOf(if (open && safe) NeonGreen.copy(alpha = 0.5f) else Color(0xFF05050A), NeonCard)))
                .border(2.dp, color, RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp, bottomStart = 6.dp, bottomEnd = 6.dp))
                .bounceClick(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            if (open) Text(if (safe) "🏆" else "💀", fontSize = 34.sp)
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        rotationY = rot
                        transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0.5f)
                        cameraDistance = 14f * density
                    }
                    .clip(RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp, bottomStart = 6.dp, bottomEnd = 6.dp))
                    .background(Brush.verticalGradient(listOf(color.copy(alpha = 0.45f), NeonCardHigh)))
                    .then(if (open) Modifier.graphicsLayer { alpha = 0.25f } else Modifier),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$number", color = TextPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 30.sp)
                    if (crossed) Text("✕", color = NeonRed, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

// =====================================================================
// Tersine Düşün
// =====================================================================

@Composable
fun ReverseRound(spec: RoundSpec, ctl: RoundControls, finish: (Boolean, Int) -> Unit) {
    PuzzleHost(ReverseGenerator, spec) { p -> ReverseBoard(p, ctl, rememberFinish(finish)) }
}

@Composable
private fun ReverseBoard(p: ReversePuzzle, ctl: RoundControls, finish: FinishOnce) {
    val scope = rememberCoroutineScope()
    val states = remember { mutableStateListOf(*Array(p.options.size) { TileState.Normal }) }
    var solved by remember { mutableStateOf(false) }
    val reveal = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        ctl.setLives(2)
        ctl.statusText = when (p.kind) { 0 -> "Bilmece"; 1 -> "Ayna saat"; 2 -> "Tersten işlem"; else -> "Ters kelime" }
        ctl.startTimer(if (p.kind == 0) 40 else 35) {
            solved = true; ctl.mistake("Süre bitti")
            scope.launch { delay(900); finish(false, 0) }
        }
        ctl.hint = { eliminateOptions(states, p.answer, 2, solved) }
        reveal.animateTo(1f, tween(500))
    }

    GameColumn {
        Box(Modifier.graphicsLayer { alpha = reveal.value; translationY = (1f - reveal.value) * 30f }) {
            PromptCard(p.question, accent = NeonPink)
        }
        if (p.clockMinutes >= 0) MirrorClock(p.clockMinutes, Modifier.size(170.dp))
        OptionGrid(
            options = p.options,
            states = states,
            columns = if (p.options.all { it.length <= 8 }) 2 else 1,
            accent = NeonPink,
            tileHeight = if (p.options.all { it.length <= 8 }) 64.dp else 56.dp,
            fontSize = if (p.options.all { it.length <= 8 }) 20.sp else 15.sp,
            onPick = { i ->
                if (solved || !states[i].pickable) return@OptionGrid
                if (i == p.answer) {
                    solved = true; states[i] = TileState.Correct
                    ctl.stopTimer(); ctl.correct("Doğru!")
                    scope.launch { delay(1500); finish(true, (100 - ctl.mistakes * 40 - (ctl.timeUsedRatio * 30).toInt()).coerceIn(10, 100)) }
                } else {
                    states[i] = TileState.Wrong
                    if (ctl.mistake()) {
                        solved = true; ctl.stopTimer(); states[p.answer] = TileState.Correct
                        scope.launch { delay(1800); finish(false, 0) }
                    }
                }
            }
        )
        AnimatedVisibility(solved, enter = fadeIn() + scaleIn(initialScale = 0.9f)) {
            Text("💡 ${p.explanation}", color = TextSecondary, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
        }
    }
}

/** Rakamsız analog saat (aynadaki görüntü). */
@Composable
private fun MirrorClock(minutes: Int, modifier: Modifier) {
    Canvas(modifier.pop(1, 0.05f)) {
        val r = size.minDimension / 2 * 0.92f
        drawCircle(NeonCard, r)
        drawCircle(NeonCyan.copy(alpha = 0.3f), r, style = Stroke(10f))
        drawCircle(NeonCyan, r, style = Stroke(3f))
        for (i in 0 until 12) {
            val a = Math.toRadians(i * 30.0 - 90)
            val inner = if (i % 3 == 0) r * 0.78f else r * 0.86f
            drawLine(
                if (i == 0) NeonYellow else TextSecondary,
                Offset(center.x + (inner * cos(a)).toFloat(), center.y + (inner * sin(a)).toFloat()),
                Offset(center.x + (r * 0.95f * cos(a)).toFloat(), center.y + (r * 0.95f * sin(a)).toFloat()),
                strokeWidth = if (i % 3 == 0) 6f else 3f, cap = StrokeCap.Round
            )
        }
        val h = (minutes / 60) % 12
        val m = minutes % 60
        val hourA = Math.toRadians((h + m / 60.0) * 30.0 - 90)
        val minA = Math.toRadians(m * 6.0 - 90)
        drawLine(NeonPink, center, Offset(center.x + (r * 0.5f * cos(hourA)).toFloat(), center.y + (r * 0.5f * sin(hourA)).toFloat()), strokeWidth = 10f, cap = StrokeCap.Round)
        drawLine(NeonCyan, center, Offset(center.x + (r * 0.78f * cos(minA)).toFloat(), center.y + (r * 0.78f * sin(minA)).toFloat()), strokeWidth = 6f, cap = StrokeCap.Round)
        drawCircle(NeonYellow, 8f, center)
    }
}

