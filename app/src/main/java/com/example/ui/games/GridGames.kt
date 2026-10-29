package com.example.ui.games

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import com.example.game.engine.RoundSpec
import com.example.game.puzzles.Cells
import com.example.game.puzzles.DetailGenerator
import com.example.game.puzzles.DetailPuzzle
import com.example.game.puzzles.HiddenObjectGenerator
import com.example.game.puzzles.HiddenObjectPuzzle
import com.example.game.puzzles.NonogramGenerator
import com.example.game.puzzles.NonogramPuzzle
import com.example.game.puzzles.PatternCell
import com.example.game.puzzles.PatternGenerator
import com.example.game.puzzles.PatternPuzzle
import com.example.game.puzzles.RotationGenerator
import com.example.game.puzzles.RotationPuzzle
import com.example.game.puzzles.Shapes
import com.example.ui.components.NeonChip
import com.example.ui.components.bounceClick
import com.example.ui.components.neonGlow
import com.example.ui.components.pop
import com.example.ui.game.RoundControls
import com.example.ui.theme.NeonBlue
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

// =====================================================================
// Gizli Resim (Nonogram)
// =====================================================================

@Composable
fun NonogramRound(spec: RoundSpec, ctl: RoundControls, finish: (Boolean, Int) -> Unit) {
    PuzzleHost(NonogramGenerator, spec) { p -> NonogramBoard(p, ctl, rememberFinish(finish)) }
}

@Composable
private fun NonogramBoard(p: NonogramPuzzle, ctl: RoundControls, finish: FinishOnce) {
    val scope = rememberCoroutineScope()
    // 0 boş, 1 dolu, 2 çarpı, 3 hatalı dolu (gösterim)
    val marks = remember { mutableStateListOf(*Array(p.size * p.size) { 0 }) }
    var fillMode by remember { mutableStateOf(true) }
    var nonoDoneRef: (() -> Unit)? = null
    var found by remember { mutableIntStateOf(0) }
    var done by remember { mutableStateOf(false) }
    val accent = NeonCyan

    LaunchedEffect(Unit) {
        ctl.setLives(3)
        ctl.statusText = "0/${p.filledCount}"
        ctl.startTimer(p.size * p.size * 2 + 20) {
            done = true
            ctl.mistake("Süre bitti")
            scope.launch { delay(900); finish(false, 0) }
        }
        ctl.hint = hint@{
            if (done) return@hint null
            val missing = (0 until p.size * p.size).filter { p.cells[it] && marks[it] != 1 }
            if (missing.isEmpty()) return@hint null
            val row = missing.first() / p.size
            val inRow = missing.filter { it / p.size == row }.take(3)
            inRow.forEach { marks[it] = 1; found++ }
            ctl.statusText = "$found/${p.filledCount}"
            nonoDoneRef?.invoke()
            "${row + 1}. satırda ${inRow.size} kare boyandı"
        }
    }

    fun rowDone(r: Int) = (0 until p.size).all { c -> !p.filled(r, c) || marks[r * p.size + c] == 1 }

    fun checkNonoDone() {
        if (done || found < p.filledCount) return
        done = true
        ctl.stopTimer()
        // Kalan boşlukları çarpıyla doldur (tamamlanma görüntüsü)
        for (k in marks.indices) if (marks[k] == 0) marks[k] = 2
        ctl.correct("Resim tamam!")
        val q = (100 - ctl.mistakes * 25 - (ctl.timeUsedRatio * 30).toInt()).coerceIn(10, 100)
        scope.launch { delay(1100); finish(true, q) }
    }
    nonoDoneRef = { checkNonoDone() }
    fun colDone(c: Int) = (0 until p.size).all { r -> !p.filled(r, c) || marks[r * p.size + c] == 1 }

    fun tap(i: Int) {
        if (done || marks[i] == 1) return
        val r = i / p.size
        val c = i % p.size
        if (fillMode) {
            if (p.cells[i]) {
                marks[i] = 1
                found++
                ctl.tap()
                ctl.statusText = "$found/${p.filledCount}"
                if (rowDone(r) || colDone(c)) ctl.fx.correct()
                checkNonoDone()
            } else {
                marks[i] = 3
                if (ctl.mistake()) {
                    done = true
                    ctl.stopTimer()
                    scope.launch { delay(1000); finish(false, 0) }
                } else scope.launch { delay(600); if (marks[i] == 3) marks[i] = 2 }
            }
        } else {
            marks[i] = if (marks[i] == 2) 0 else 2
            ctl.tap()
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 6.dp)) {
        val clueRowW = 14.dp * (p.size / 2 + 1).coerceAtLeast(2) + 10.dp
        val clueColH = 15.dp * (p.size / 2 + 1).coerceAtLeast(2) + 6.dp
        val avail = min(maxWidth - clueRowW, maxHeight - clueColH - 80.dp)
        val cell = (avail / p.size).coerceAtMost(48.dp)
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Row {
                Spacer(Modifier.width(clueRowW))
                for (c in 0 until p.size) {
                    val doneC = colDone(c)
                    Column(
                        Modifier.width(cell).height(clueColH),
                        verticalArrangement = Arrangement.Bottom,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        p.colClues[c].forEach {
                            Text("$it", color = if (doneC) TextMuted else NeonPink, fontSize = 12.sp, fontWeight = FontWeight.Bold, lineHeight = 14.sp)
                        }
                    }
                }
            }
            for (r in 0 until p.size) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val doneR = rowDone(r)
                    Row(Modifier.width(clueRowW).padding(end = 6.dp), horizontalArrangement = Arrangement.End) {
                        Text(
                            p.rowClues[r].joinToString(" "), color = if (doneR) TextMuted else NeonPink,
                            fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1
                        )
                    }
                    for (c in 0 until p.size) {
                        val i = r * p.size + c
                        NonoCell(marks[i], cell, accent, thickRight = c % 5 == 4 && c != p.size - 1, thickBottom = r % 5 == 4 && r != p.size - 1) { tap(i) }
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                NeonTile(Modifier.size(120.dp, 48.dp), state = if (fillMode) TileState.Selected else TileState.Normal, accent = accent, onClick = { fillMode = true }) {
                    Text("■ Doldur", color = TextPrimary, fontWeight = FontWeight.Bold)
                }
                NeonTile(Modifier.size(120.dp, 48.dp), state = if (!fillMode) TileState.Selected else TileState.Normal, accent = NeonPink, onClick = { fillMode = false }) {
                    Text("✕ İşaretle", color = TextPrimary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun NonoCell(mark: Int, size: Dp, accent: Color, thickRight: Boolean, thickBottom: Boolean, onTap: () -> Unit) {
    val fill by animateColorAsState(
        when (mark) { 1 -> accent; 3 -> NeonRed; else -> NeonCard }, tween(180), label = "nc"
    )
    val scale by animateFloatAsState(if (mark == 1) 1f else 0.92f, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "ns")
    Box(
        Modifier
            .size(size)
            .padding(end = if (thickRight) 2.dp else 0.dp, bottom = if (thickBottom) 2.dp else 0.dp)
            .border(0.5.dp, NeonBorder)
            .pointerInput(Unit) { detectTapGestures { onTap() } },
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .padding(2.dp)
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .clip(RoundedCornerShape(4.dp))
                .background(if (mark == 1 || mark == 3) fill else Color.Transparent)
        )
        if (mark == 2) Text("✕", color = TextMuted, fontSize = (size.value * 0.45f).sp)
    }
}

// =====================================================================
// Zihin Döndürme
// =====================================================================

@Composable
fun RotationRound(spec: RoundSpec, ctl: RoundControls, finish: (Boolean, Int) -> Unit) {
    PuzzleHost(RotationGenerator, spec) { p -> RotationBoard(p, ctl, rememberFinish(finish)) }
}

@Composable
fun PolyominoView(cells: Cells, color: Color, modifier: Modifier = Modifier, gridSpan: Int = 5) {
    Canvas(modifier) {
        val n = maxOf(gridSpan, Shapes.width(cells), Shapes.height(cells))
        val cell = size.minDimension / n
        val offX = (size.width - Shapes.width(cells) * cell) / 2
        val offY = (size.height - Shapes.height(cells) * cell) / 2
        for ((r, c) in cells) {
            val tl = Offset(offX + c * cell + cell * 0.06f, offY + r * cell + cell * 0.06f)
            val sz = Size(cell * 0.88f, cell * 0.88f)
            drawRoundRect(color.copy(alpha = 0.3f), tl, sz, CornerRadius(cell * 0.18f))
            drawRoundRect(color, tl, sz, CornerRadius(cell * 0.18f), style = Stroke(cell * 0.09f))
        }
    }
}

@Composable
private fun RotationBoard(p: RotationPuzzle, ctl: RoundControls, finish: FinishOnce) {
    val scope = rememberCoroutineScope()
    val states = remember { mutableStateListOf(*Array(p.options.size) { TileState.Normal }) }
    var solved by remember { mutableStateOf(false) }
    val spin = remember { Animatable(0f) }
    val span = maxOf(p.options.maxOf { maxOf(Shapes.width(it), Shapes.height(it)) }, Shapes.width(p.shape), Shapes.height(p.shape))

    LaunchedEffect(Unit) {
        ctl.setLives(2)
        ctl.startTimer(28) {
            solved = true; ctl.mistake("Süre bitti")
            scope.launch { delay(900); finish(false, 0) }
        }
        ctl.hint = { eliminateOptions(states, p.answer, 2, solved) }
    }

    GameColumn {
        PromptCard("Şekli ${p.angleText} döndürünce hangisi olur?", accent = NeonCyan, sub = "Ayna görüntülerine dikkat!")
        Box(
            Modifier
                .size(150.dp)
                .neonGlow(NeonCyan, 16.dp, 24.dp, 0.4f)
                .clip(RoundedCornerShape(24.dp))
                .background(NeonCard)
                .border(1.dp, NeonCyan.copy(alpha = 0.6f), RoundedCornerShape(24.dp))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            val turnDeg = when (p.turns) { 1 -> 90f; 2 -> 180f; else -> -90f }
            PolyominoView(p.shape, NeonCyan, Modifier.fillMaxSize().graphicsLayer { rotationZ = spin.value * turnDeg }, span)
        }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            p.options.indices.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { i ->
                        NeonTile(
                            Modifier.weight(1f).aspectRatio(1.35f), state = states[i], accent = NeonCyan,
                            onClick = {
                                if (solved || !states[i].pickable) return@NeonTile
                                if (i == p.answer) {
                                    solved = true
                                    states[i] = TileState.Correct
                                    ctl.stopTimer()
                                    ctl.correct("Doğru!")
                                    scope.launch {
                                        spin.animateTo(1f, tween(700))
                                        delay(400)
                                        finish(true, (100 - ctl.mistakes * 40 - (ctl.timeUsedRatio * 35).toInt()).coerceIn(10, 100))
                                    }
                                } else {
                                    states[i] = TileState.Wrong
                                    if (ctl.mistake("Ayna tuzağı!")) {
                                        solved = true
                                        ctl.stopTimer()
                                        states[p.answer] = TileState.Correct
                                        scope.launch { spin.animateTo(1f, tween(700)); delay(500); finish(false, 0) }
                                    }
                                }
                            }
                        ) {
                            PolyominoView(p.options[i], if (states[i] == TileState.Correct) NeonGreen else NeonPurple, Modifier.fillMaxSize().padding(12.dp), span)
                        }
                    }
                }
            }
        }
    }
}

// =====================================================================
// Desen Ustası
// =====================================================================

private val PATTERN_COLORS = listOf(NeonCyan, NeonPink, NeonGreen, NeonYellow, NeonPurple)

@Composable
fun PatternRound(spec: RoundSpec, ctl: RoundControls, finish: (Boolean, Int) -> Unit) {
    PuzzleHost(PatternGenerator, spec) { p -> PatternBoard(p, ctl, rememberFinish(finish)) }
}

@Composable
fun PatternCellView(cell: PatternCell, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val n = cell.count
        val color = PATTERN_COLORS[cell.color]
        val w = size.width / n
        val s = minOf(w, size.height) * 0.92f
        for (k in 0 until n) {
            val center = Offset(w * k + w / 2, size.height / 2)
            rotate(cell.rot * 45f, center) {
                drawNeonShape(cell.shape, color, filled = true, scale = s / size.minDimension, center = center)
            }
        }
    }
}

@Composable
private fun PatternBoard(p: PatternPuzzle, ctl: RoundControls, finish: FinishOnce) {
    val scope = rememberCoroutineScope()
    val states = remember { mutableStateListOf(*Array(p.options.size) { TileState.Normal }) }
    var solved by remember { mutableStateOf(false) }
    var popKey by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        ctl.setLives(2)
        ctl.startTimer(40) {
            solved = true; ctl.mistake("Süre bitti")
            scope.launch { delay(900); finish(false, 0) }
        }
        ctl.hint = { eliminateOptions(states, p.answer, 2, solved) }
    }

    GameColumn {
        PromptCard("Eksik deseni bul", accent = NeonCyan, sub = "Satırlardaki kuralı çöz")
        Column(
            Modifier
                .widthIn(max = 340.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(NeonCard.copy(alpha = 0.7f))
                .border(1.dp, NeonBorder, RoundedCornerShape(20.dp))
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            for (r in 0 until 3) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (c in 0 until 3) {
                        val i = r * 3 + c
                        val isQ = i == 8
                        Box(
                            Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isQ) NeonYellow.copy(alpha = 0.08f) else NeonCardHigh)
                                .border(if (isQ) 2.dp else 1.dp, if (isQ) (if (solved) NeonGreen else NeonYellow) else NeonBorder, RoundedCornerShape(14.dp))
                                .then(if (isQ) Modifier.pop(popKey, 0.15f) else Modifier),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isQ && !solved) Text("?", color = NeonYellow, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)
                            else PatternCellView(p.grid[i], Modifier.fillMaxSize().padding(10.dp))
                        }
                    }
                }
            }
        }
        val cols = if (p.options.size <= 4) 4 else 3
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            p.options.indices.chunked(cols).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { i ->
                        NeonTile(
                            Modifier.weight(1f).aspectRatio(1f), state = states[i], accent = NeonYellow,
                            onClick = {
                                if (solved || !states[i].pickable) return@NeonTile
                                if (i == p.answer) {
                                    solved = true; states[i] = TileState.Correct; popKey++
                                    ctl.stopTimer(); ctl.correct("Doğru!")
                                    scope.launch { delay(1000); finish(true, (100 - ctl.mistakes * 40 - (ctl.timeUsedRatio * 30).toInt()).coerceIn(10, 100)) }
                                } else {
                                    states[i] = TileState.Wrong
                                    if (ctl.mistake()) {
                                        solved = true; ctl.stopTimer(); states[p.answer] = TileState.Correct
                                        scope.launch { delay(1200); finish(false, 0) }
                                    }
                                }
                            }
                        ) { PatternCellView(p.options[i], Modifier.fillMaxSize().padding(10.dp)) }
                    }
                    repeat(cols - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
        if (solved) Text("Kural → ${p.rules}", color = TextSecondary, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
    }
}

// =====================================================================
// Detay Avcısı
// =====================================================================

@Composable
fun DetailRound(spec: RoundSpec, ctl: RoundControls, finish: (Boolean, Int) -> Unit) {
    PuzzleHost(DetailGenerator, spec) { p -> DetailBoard(p, ctl, rememberFinish(finish)) }
}

@Composable
private fun DetailBoard(p: DetailPuzzle, ctl: RoundControls, finish: FinishOnce) {
    val scope = rememberCoroutineScope()
    val found = remember { mutableStateListOf<Int>() }
    val wrongCells = remember { mutableStateListOf<Int>() }
    var done by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        ctl.setLives(3)
        ctl.statusText = "Fark 0/${p.diffs.size}"
        ctl.startTimer(p.timeLimitSec) {
            done = true; ctl.mistake("Süre bitti")
            scope.launch { delay(900); finish(false, 0) }
        }
        ctl.hint = hint@{
            if (done) return@hint null
            val d = p.diffs.firstOrNull { it !in found } ?: return@hint null
            found += d
            ctl.statusText = "Fark ${found.size}/${p.diffs.size}"
            if (found.size == p.diffs.size) {
                done = true; ctl.stopTimer()
                scope.launch { delay(800); finish(true, (100 - ctl.mistakes * 25 - (ctl.timeUsedRatio * 35).toInt() - 15).coerceIn(10, 100)) }
            }
            "Bir fark işaretlendi"
        }
    }

    fun tap(i: Int) {
        if (done || i in found) return
        if (i in p.diffs) {
            found += i
            ctl.correct()
            ctl.statusText = "Fark ${found.size}/${p.diffs.size}"
            if (found.size == p.diffs.size) {
                done = true; ctl.stopTimer()
                scope.launch { delay(800); finish(true, (100 - ctl.mistakes * 25 - (ctl.timeUsedRatio * 35).toInt()).coerceIn(10, 100)) }
            }
        } else {
            wrongCells += i
            scope.launch { delay(500); wrongCells.remove(i) }
            if (ctl.mistake()) {
                done = true; ctl.stopTimer()
                scope.launch { delay(900); finish(false, 0) }
            }
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 4.dp)) {
        val boardSide = min(maxWidth, (maxHeight - 40.dp) / 2)
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("ORİJİNAL", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
            EmojiGrid(p.size, p.original, boardSide * 0.92f, found, emptyList(), NeonCyan, null)
            Text("FARKLARI BURADA BUL", color = NeonYellow, style = MaterialTheme.typography.labelSmall)
            EmojiGrid(p.size, p.modified, boardSide * 0.92f, found, wrongCells, NeonYellow) { tap(it) }
        }
    }
}

@Composable
private fun EmojiGrid(size: Int, cells: List<String>, side: Dp, found: List<Int>, wrong: List<Int>, accent: Color, onTap: ((Int) -> Unit)?) {
    val cell = side / size
    Column(
        Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(NeonCard.copy(alpha = 0.8f))
            .border(1.dp, accent.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
    ) {
        for (r in 0 until size) Row {
            for (c in 0 until size) {
                val i = r * size + c
                val isFound = i in found
                val isWrong = i in wrong
                Box(
                    Modifier
                        .size(cell)
                        .then(if (onTap != null) Modifier.pointerInput(i) { detectTapGestures { onTap(i) } } else Modifier),
                    contentAlignment = Alignment.Center
                ) {
                    Text(cells[i], fontSize = (cell.value * 0.55f).sp)
                    if (isFound) Box(Modifier.size(cell * 0.92f).border(3.dp, NeonGreen, CircleShape).pop(1, 0.2f))
                    if (isWrong) Box(Modifier.size(cell * 0.9f).border(3.dp, NeonRed, CircleShape))
                }
            }
        }
    }
}

// =====================================================================
// Gizli Nesneyi Bul
// =====================================================================

@Composable
fun HiddenObjectRound(spec: RoundSpec, ctl: RoundControls, finish: (Boolean, Int) -> Unit) {
    PuzzleHost(HiddenObjectGenerator, spec) { p -> HiddenObjectBoard(p, ctl, rememberFinish(finish)) }
}

@Composable
private fun HiddenObjectBoard(p: HiddenObjectPuzzle, ctl: RoundControls, finish: FinishOnce) {
    val scope = rememberCoroutineScope()
    val found = remember { mutableStateListOf<Int>() }
    var wrongAt by remember { mutableStateOf<Int?>(null) }
    var hintAt by remember { mutableStateOf<Int?>(null) }
    var done by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        ctl.setLives(3)
        ctl.statusText = "${p.theme} · 0/${p.targets.size}"
        ctl.startTimer(p.timeLimitSec) {
            done = true; ctl.mistake("Süre bitti")
            scope.launch { delay(900); finish(false, 0) }
        }
        ctl.hint = hint@{
            if (done) return@hint null
            val t = p.targets.firstOrNull { it !in found } ?: return@hint null
            scope.launch { hintAt = t; delay(3000); if (hintAt == t) hintAt = null }
            "${p.items[t].emoji} parlayan yerde"
        }
    }

    fun tap(i: Int) {
        if (done || i in found) return
        if (i in p.targets) {
            found += i
            ctl.correct()
            ctl.statusText = "${p.theme} · ${found.size}/${p.targets.size}"
            if (found.size == p.targets.size) {
                done = true; ctl.stopTimer()
                scope.launch { delay(800); finish(true, (100 - ctl.mistakes * 25 - (ctl.timeUsedRatio * 35).toInt()).coerceIn(10, 100)) }
            }
        } else {
            wrongAt = i
            scope.launch { delay(450); if (wrongAt == i) wrongAt = null }
            if (ctl.mistake()) {
                done = true; ctl.stopTimer()
                scope.launch { delay(900); finish(false, 0) }
            }
        }
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(22.dp))
                .background(Brush.linearGradient(listOf(NeonPurple.copy(alpha = 0.18f), NeonBlue.copy(alpha = 0.12f), NeonCard)))
                .border(1.dp, NeonBorder, RoundedCornerShape(22.dp))
        ) {
            val side = min(maxWidth, maxHeight)
            val w = maxWidth
            val h = maxHeight
            p.items.forEachIndexed { i, it ->
                val s = side * it.size
                val isFound = i in found
                val alpha by animateFloatAsState(if (isFound) 0.25f else 1f, tween(300), label = "ho$i")
                Box(
                    Modifier
                        .offset(w * it.x - s / 2, h * it.y - s / 2)
                        .size(s)
                        .graphicsLayer { rotationZ = it.rot; this.alpha = alpha }
                        .then(if (hintAt == i) Modifier.neonGlow(NeonYellow, 22.dp, s, 1f).border(3.dp, NeonYellow, CircleShape).pop(hintAt ?: 0, 0.35f) else Modifier)
                        .then(if (isFound) Modifier.border(2.dp, NeonGreen, CircleShape) else Modifier)
                        .then(if (wrongAt == i) Modifier.border(2.dp, NeonRed, CircleShape) else Modifier)
                        .pointerInput(i) { detectTapGestures { tap(i) } },
                    contentAlignment = Alignment.Center
                ) {
                    Text(it.emoji, fontSize = (s.value * 0.72f).sp)
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), modifier = Modifier.fillMaxWidth()) {
            p.targets.forEach { t ->
                val isFound = t in found
                Box(
                    Modifier
                        .size(48.dp)
                        .then(if (!isFound) Modifier.neonGlow(NeonYellow, 8.dp, 14.dp, 0.4f) else Modifier)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isFound) NeonGreen.copy(alpha = 0.25f) else NeonCard)
                        .border(1.dp, if (isFound) NeonGreen else NeonYellow, RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(p.items[t].emoji, fontSize = 24.sp, modifier = Modifier.graphicsLayer { alpha = if (isFound) 0.5f else 1f })
                    if (isFound) Text("✓", color = NeonGreen, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
                }
            }
        }
    }
}

/** Bölüm başlığı chip'i (ızgara oyunları için). */
@Composable
fun TinyLabel(text: String, color: Color = NeonOrange) = NeonChip(text, color)
