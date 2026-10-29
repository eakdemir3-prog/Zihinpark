package com.example.ui.games

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.runtime.mutableStateMapOf
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.engine.RoundSpec
import com.example.game.puzzles.BlockBlastGenerator
import com.example.game.puzzles.BlockBlastPuzzle
import com.example.game.puzzles.BlockShapes
import com.example.game.puzzles.Cells
import com.example.game.puzzles.FlowGenerator
import com.example.game.puzzles.FlowPuzzle
import com.example.game.puzzles.Match3Generator
import com.example.game.puzzles.Match3Puzzle
import com.example.game.puzzles.SortGenerator
import com.example.game.puzzles.SortPuzzle
import com.example.game.puzzles.SudokuGenerator
import com.example.game.puzzles.SudokuPuzzle
import com.example.ui.components.bounceClick
import com.example.ui.components.neonGlow
import com.example.ui.components.pop
import com.example.ui.components.shake
import com.example.ui.game.RoundControls
import com.example.ui.theme.NeonBg
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
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.random.Random

/** Ortak 10 renkli neon paleti. */
val PALETTE10 = listOf(
    NeonPink, NeonCyan, NeonYellow, NeonGreen, NeonPurple, NeonOrange,
    NeonBlue, NeonRed, Color(0xFFFFFFFF), Color(0xFFC08040)
)

// =====================================================================
// Renk Sıralama
// =====================================================================

@Composable
fun SortRound(spec: RoundSpec, ctl: RoundControls, finish: (Boolean, Int) -> Unit) {
    PuzzleHost(SortGenerator, spec) { p -> SortBoard(p, ctl, rememberFinish(finish)) }
}

@Composable
private fun SortBoard(p: SortPuzzle, ctl: RoundControls, finish: FinishOnce) {
    val scope = rememberCoroutineScope()
    val tubes = remember { mutableStateListOf(*p.tubes.toTypedArray()) }
    var sel by remember { mutableStateOf<Int?>(null) }
    var moves by remember { mutableIntStateOf(0) }
    var done by remember { mutableStateOf(false) }
    var bumpTube by remember { mutableStateOf<Int?>(null) }
    var bumpKey by remember { mutableIntStateOf(0) }
    val history = remember { mutableStateListOf<List<List<Int>>>() }
    val completedTubes = remember { mutableStateListOf<Int>() }

    LaunchedEffect(Unit) {
        ctl.statusText = "Hamle 0"
        ctl.startTimer(p.colors * 22 + 30) {
            done = true; ctl.mistake("Süre bitti")
            scope.launch { delay(900); finish(false, 0) }
        }
        ctl.undo = {
            history.removeLastOrNull()?.let { prev ->
                prev.forEachIndexed { i, t -> tubes[i] = t }
                moves--; sel = null
                ctl.statusText = "Hamle $moves"
                true
            } ?: false
        }
        ctl.hint = hint@{
            if (done) return@hint null
            val m = bestSortMove(tubes.toList(), p.capacity) ?: return@hint null
            sel = m.first
            scope.launch { bumpTube = m.second; delay(1600); if (bumpTube == m.second) bumpTube = null }
            "${m.first + 1}. tüpü ${m.second + 1}. tüpe dök"
        }
    }

    fun tapTube(i: Int) {
        if (done) return
        val s = sel
        if (s == null) {
            if (tubes[i].isEmpty()) return
            sel = i; ctl.tap(); return
        }
        if (s == i) { sel = null; return }
        if (!SortGenerator.canMove(tubes, s, i, p.capacity)) {
            bumpKey++; sel = if (tubes[i].isNotEmpty()) i else null
            ctl.sound.playWrong()
            return
        }
        history += tubes.toList()
        val next = SortGenerator.move(tubes, s, i, p.capacity)
        next.forEachIndexed { k, t -> tubes[k] = t }
        moves++
        sel = null
        ctl.statusText = "Hamle $moves"
        val t = tubes[i]
        if (t.size == p.capacity && t.all { it == t[0] } && i !in completedTubes) {
            completedTubes += i
            ctl.fx.correct("Tüp tamam!")
            ctl.sound.playCorrect()
        } else ctl.tap()
        if (SortGenerator.solved(tubes, p.capacity)) {
            done = true
            ctl.stopTimer()
            ctl.correct("Sıralandı!")
            val q = (100 - (ctl.timeUsedRatio * 45).toInt() - (moves - p.colors * 2).coerceAtLeast(0) * 2).coerceIn(15, 100)
            scope.launch { delay(900); finish(true, q) }
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize().padding(12.dp), contentAlignment = Alignment.Center) {
        val n = tubes.size
        val perRow = if (n <= 6) n else (n + 1) / 2
        val rows = (n + perRow - 1) / perRow
        val tubeW = ((maxWidth - 10.dp * (perRow - 1)) / perRow).coerceAtMost(64.dp)
        val tubeH = (tubeW * (p.capacity * 0.95f + 0.6f)).coerceAtMost((maxHeight - 40.dp) / rows - 24.dp)
        Column(verticalArrangement = Arrangement.spacedBy(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            (0 until n).chunked(perRow).forEach { rowIdx ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.shake(bumpKey, 4.dp)) {
                    rowIdx.forEach { i ->
                        Tube(
                            balls = tubes[i], capacity = p.capacity, selected = sel == i,
                            hinted = bumpTube == i, complete = i in completedTubes && tubes[i].size == p.capacity,
                            width = tubeW, height = tubeH
                        ) { tapTube(i) }
                    }
                }
            }
        }
    }
}

/** İpucu için basit sezgisel hamle: tüp tamamlayan > aynı renge döken > boşa döken. */
private fun bestSortMove(t: List<List<Int>>, cap: Int): Pair<Int, Int>? {
    var best: Pair<Int, Int>? = null
    var bestScore = Int.MIN_VALUE
    for (f in t.indices) for (to in t.indices) {
        if (!SortGenerator.canMove(t, f, to, cap)) continue
        if (t[to].isEmpty() && t[f].all { it == t[f][0] }) continue
        val after = SortGenerator.move(t, f, to, cap)
        if (!SortGenerator.solvable(after, cap, 4000)) continue
        val top = t[f].last()
        val run = t[f].takeLastWhile { it == top }.size
        var score = 0
        if (after[to].size == cap && after[to].all { it == top }) score += 100
        if (t[to].isNotEmpty()) score += 30 + run * 5 else score += 5
        if (after[f].isEmpty() || after[f].all { it == after[f][0] }) score += 20
        if (score > bestScore) { bestScore = score; best = f to to }
    }
    return best
}

@Composable
private fun Tube(balls: List<Int>, capacity: Int, selected: Boolean, hinted: Boolean, complete: Boolean, width: androidx.compose.ui.unit.Dp, height: androidx.compose.ui.unit.Dp, onClick: () -> Unit) {
    val lift by animateFloatAsState(if (selected) 1f else 0f, spring(dampingRatio = 0.55f), label = "lift")
    val color = when { complete -> NeonGreen; hinted -> NeonYellow; selected -> NeonCyan; else -> NeonBorder }
    Box(
        Modifier
            .size(width, height)
            .then(if (complete || hinted || selected) Modifier.neonGlow(color, 12.dp, width / 2, 0.6f) else Modifier)
            .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = width / 2, bottomEnd = width / 2))
            .background(Brush.verticalGradient(listOf(NeonCard.copy(alpha = 0.4f), NeonCardHigh)))
            .border(2.dp, color, RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = width / 2, bottomEnd = width / 2))
            .bounceClick(onClick = onClick),
        contentAlignment = Alignment.BottomCenter
    ) {
        val ball = (width - 10.dp).coerceAtMost((height - 16.dp) / capacity - 2.dp)
        Column(
            Modifier.padding(bottom = 6.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.Bottom),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            balls.asReversed().forEachIndexed { k, c ->
                val isTop = k == 0
                val pc = PALETTE10[c % PALETTE10.size]
                Box(
                    Modifier
                        .size(ball)
                        .graphicsLayer { translationY = if (isTop) -lift * 26.dp.toPx() else 0f }
                        .neonGlow(pc, 6.dp, ball / 2, 0.55f)
                        .clip(CircleShape)
                        .background(Brush.radialGradient(listOf(Color.White.copy(alpha = 0.8f), pc, pc.copy(alpha = 0.7f)), center = Offset.Unspecified))
                )
            }
        }
    }
}

// =====================================================================
// Blok Patlat
// =====================================================================

@Composable
fun BlockBlastRound(spec: RoundSpec, ctl: RoundControls, finish: (Boolean, Int) -> Unit) {
    PuzzleHost(BlockBlastGenerator, spec) { p -> BlockBlastBoard(p, ctl, rememberFinish(finish)) }
}

private data class Piece(val id: Int, val shape: Cells, val color: Int)

@Composable
private fun BlockBlastBoard(p: BlockBlastPuzzle, ctl: RoundControls, finish: FinishOnce) {
    val scope = rememberCoroutineScope()
    val n = p.size
    val board = remember { mutableStateListOf(*p.initial.toTypedArray()) }
    val rng = remember { Random(p.pieceSeed) }
    var nextId by remember { mutableIntStateOf(0) }
    fun newPiece(): Piece {
        val weights = BlockShapes.ALL.indices.map { BlockShapes.weightFor(it, p.tier) }
        var r = rng.nextInt(weights.sum())
        var idx = 0
        while (r >= weights[idx]) { r -= weights[idx]; idx++ }
        return Piece(nextId++, BlockShapes.ALL[idx], 1 + rng.nextInt(6))
    }
    val tray = remember { mutableStateListOf<Piece?>() }
    var lines by remember { mutableIntStateOf(0) }
    var combo by remember { mutableIntStateOf(0) }
    var done by remember { mutableStateOf(false) }
    val flashing = remember { mutableStateListOf<Int>() }
    var hintCell by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var hintPiece by remember { mutableStateOf<Int?>(null) }
    // Sürükleme
    var dragIdx by remember { mutableStateOf<Int?>(null) }
    var dragPos by remember { mutableStateOf(Offset.Zero) }
    var boardOrigin by remember { mutableStateOf(Offset.Zero) }
    var cellPx by remember { mutableStateOf(1f) }
    val history = remember { mutableStateListOf<Triple<List<Int>, List<Piece?>, Int>>() }

    fun fits(s: Cells, r: Int, c: Int) = s.all { (dr, dc) ->
        val rr = r + dr; val cc = c + dc
        rr in 0 until n && cc in 0 until n && board[rr * n + cc] == 0
    }
    fun anyFit(s: Cells) = (0 until n).any { r -> (0 until n).any { c -> fits(s, r, c) } }

    LaunchedEffect(Unit) {
        repeat(3) { tray += newPiece() }
        ctl.statusText = "Satır 0/${p.targetLines}"
        ctl.undo = {
            history.removeLastOrNull()?.let { (b, t, l) ->
                b.forEachIndexed { i, v -> board[i] = v }
                tray.clear(); tray.addAll(t); lines = l
                ctl.statusText = "Satır $lines/${p.targetLines}"
                true
            } ?: false
        }
        ctl.hint = hint@{
            if (done) return@hint null
            for ((ti, pc) in tray.withIndex()) {
                if (pc == null) continue
                var best: Pair<Int, Int>? = null; var bestScore = -1
                for (r in 0 until n) for (c in 0 until n) if (fits(pc.shape, r, c)) {
                    val trial = board.toMutableList()
                    pc.shape.forEach { (dr, dc) -> trial[(r + dr) * n + c + dc] = 9 }
                    val full = (0 until n).count { rr -> (0 until n).all { trial[rr * n + it] != 0 } } +
                        (0 until n).count { cc -> (0 until n).all { trial[it * n + cc] != 0 } }
                    val score = full * 100 + pc.shape.size
                    if (score > bestScore) { bestScore = score; best = r to c }
                }
                if (best != null) {
                    scope.launch { hintPiece = ti; hintCell = best; delay(2500); hintCell = null; hintPiece = null }
                    return@hint "Parlayan parçayı işaretli yere koy"
                }
            }
            null
        }
    }

    fun place(ti: Int, r: Int, c: Int): Boolean {
        val pc = tray.getOrNull(ti) ?: return false
        if (!fits(pc.shape, r, c)) return false
        history += Triple(board.toList(), tray.toList(), lines)
        pc.shape.forEach { (dr, dc) -> board[(r + dr) * n + c + dc] = pc.color }
        tray[ti] = null
        val fullRows = (0 until n).filter { rr -> (0 until n).all { board[rr * n + it] != 0 } }
        val fullCols = (0 until n).filter { cc -> (0 until n).all { board[it * n + cc] != 0 } }
        val cleared = fullRows.size + fullCols.size
        if (cleared > 0) {
            val cells = fullRows.flatMap { rr -> (0 until n).map { rr * n + it } } + fullCols.flatMap { cc -> (0 until n).map { it * n + cc } }
            flashing.clear(); flashing.addAll(cells.distinct())
            combo++
            lines += cleared
            ctl.fx.correct(if (cleared > 1) "${cleared}x KOMBO!" else if (combo > 1) "Seri x$combo" else "Patladı!")
            ctl.sound.playCorrect()
            scope.launch {
                delay(260)
                cells.forEach { board[it] = 0 }
                flashing.clear()
            }
        } else { combo = 0; ctl.tap() }
        ctl.statusText = "Satır ${lines.coerceAtMost(p.targetLines)}/${p.targetLines}"
        if (tray.all { it == null }) { tray.clear(); repeat(3) { tray += newPiece() } }
        if (lines >= p.targetLines) {
            done = true
            ctl.correct("Hedef tamam!")
            scope.launch { delay(900); finish(true, (100 - ctl.mistakes * 10 - history.size / 3).coerceIn(30, 100)) }
        } else {
            scope.launch {
                delay(320)
                if (!done && tray.filterNotNull().none { anyFit(it.shape) }) {
                    done = true
                    ctl.fx.wrong("Yer kalmadı")
                    ctl.sound.playWrong()
                    delay(1000); finish(false, 0)
                }
            }
        }
        return true
    }

    var containerOrigin by remember { mutableStateOf(Offset.Zero) }
    Box(Modifier.fillMaxSize().onGloballyPositioned { containerOrigin = it.positionInRoot() }) {
    Column(Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        BoxWithConstraints(
            Modifier
                .widthIn(max = 420.dp)
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(NeonCard)
                .border(1.dp, NeonBorder, RoundedCornerShape(16.dp))
                .onGloballyPositioned { boardOrigin = it.positionInRoot(); cellPx = it.size.width / n.toFloat() }
        ) {
            val cell = maxWidth / n
            // Sürüklenen parçanın gölgesi
            val ghost: List<Int> = dragIdx?.let { ti ->
                val pc = tray.getOrNull(ti) ?: return@let emptyList()
                val (r, c) = dropCell(dragPos, boardOrigin, cellPx, pc.shape)
                if (fits(pc.shape, r, c)) pc.shape.map { (dr, dc) -> (r + dr) * n + c + dc } else emptyList()
            } ?: emptyList()
            val hintCells = hintCell?.let { (r, c) -> tray.getOrNull(hintPiece ?: -1)?.shape?.map { (dr, dc) -> (r + dr) * n + c + dc } } ?: emptyList()
            for (i in 0 until n * n) {
                val v = board[i]
                val color = when {
                    i in flashing -> Color.White
                    v > 0 -> PALETTE10[v - 1]
                    i in ghost -> tray.getOrNull(dragIdx ?: -1)?.let { PALETTE10[it.color - 1].copy(alpha = 0.4f) } ?: NeonCardHigh
                    i in hintCells -> NeonYellow.copy(alpha = 0.35f)
                    else -> NeonCardHigh.copy(alpha = 0.5f)
                }
                Box(
                    Modifier
                        .offset(cell * (i % n), cell * (i / n))
                        .size(cell)
                        .padding(1.5.dp)
                        .then(if (v > 0 || i in flashing) Modifier.neonGlow(color, 4.dp, 6.dp, 0.45f) else Modifier)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (v > 0) Brush.verticalGradient(listOf(color, color.copy(alpha = 0.6f))) else Brush.verticalGradient(listOf(color, color)))
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("Parçayı tahtaya sürükle", color = TextMuted, style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth().height(120.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            tray.forEachIndexed { ti, pc ->
                Box(Modifier.size(100.dp), contentAlignment = Alignment.Center) {
                    if (pc != null) {
                        val dragging = dragIdx == ti
                        val canFit = anyFit(pc.shape)
                        var origin by remember(pc.id) { mutableStateOf(Offset.Zero) }
                        Box(
                            Modifier
                                .onGloballyPositioned { origin = it.positionInRoot() }
                                .then(if (hintPiece == ti) Modifier.neonGlow(NeonYellow, 12.dp, 12.dp, 0.9f) else Modifier)
                                .graphicsLayer {
                                    alpha = if (dragging) 0f else if (canFit) 1f else 0.35f
                                }
                                .pointerInput(pc.id) {
                                    detectDragGestures(
                                        onDragStart = { o ->
                                            if (!done) {
                                                val pw = (pc.shape.maxOf { it.second } + 1) * cellPx
                                                val ph = (pc.shape.maxOf { it.first } + 1) * cellPx
                                                dragIdx = ti
                                                dragPos = origin + o - Offset(pw / 2f, ph + cellPx * 1.2f)
                                            }
                                        },
                                        onDragEnd = {
                                            val di = dragIdx
                                            if (di != null) {
                                                val (r, c) = dropCell(dragPos, boardOrigin, cellPx, pc.shape)
                                                if (!place(di, r, c)) ctl.sound.playWrong()
                                            }
                                            dragIdx = null
                                        },
                                        onDragCancel = { dragIdx = null }
                                    ) { change, d -> change.consume(); dragPos += d }
                                }
                        ) { PieceView(pc, 20.dp) }
                    }
                }
            }
        }
    }
    // Sürüklenen parça (parmağın biraz üstünde)
    dragIdx?.let { ti ->
        val pc = tray.getOrNull(ti) ?: return@let
        val density = LocalDensity.current
        Box(Modifier.offset { IntOffset((dragPos.x - containerOrigin.x).roundToInt(), (dragPos.y - containerOrigin.y).roundToInt()) }) {
            PieceView(pc, with(density) { cellPx.toDp() })
        }
    }
    }
}

/** Sürüklenen parçanın sol üst karesinin düşeceği hücre. */
private fun dropCell(pos: Offset, origin: Offset, cell: Float, @Suppress("UNUSED_PARAMETER") shape: Cells): Pair<Int, Int> {
    val local = pos - origin
    return (local.y / cell).roundToInt() to (local.x / cell).roundToInt()
}

@Composable
private fun PieceView(pc: Piece, cell: androidx.compose.ui.unit.Dp) {
    val w = pc.shape.maxOf { it.second } + 1
    val h = pc.shape.maxOf { it.first } + 1
    val color = PALETTE10[pc.color - 1]
    Box(Modifier.size(cell * w, cell * h)) {
        pc.shape.forEach { (r, c) ->
            Box(
                Modifier
                    .offset(cell * c, cell * r)
                    .size(cell)
                    .padding(1.5.dp)
                    .neonGlow(color, 4.dp, 5.dp, 0.5f)
                    .clip(RoundedCornerShape(5.dp))
                    .background(Brush.verticalGradient(listOf(color, color.copy(alpha = 0.6f))))
            )
        }
    }
}

// =====================================================================
// Neon Üçlü
// =====================================================================

@Composable
fun Match3Round(spec: RoundSpec, ctl: RoundControls, finish: (Boolean, Int) -> Unit) {
    PuzzleHost(Match3Generator, spec) { p -> Match3Board(p, ctl, rememberFinish(finish)) }
}

private const val STRIPE_H = 100
private const val STRIPE_V = 200
private const val BOMB = 300

@Composable
private fun Match3Board(p: Match3Puzzle, ctl: RoundControls, finish: FinishOnce) {
    val scope = rememberCoroutineScope()
    val n = p.size
    // Değer: renk (0..colors-1) + özel taş ofseti; -1 boş
    val board = remember { mutableStateListOf(*p.board.toTypedArray()) }
    val rng = remember { Random(p.refillSeed) }
    val collected = remember { mutableStateMapOf<Int, Int>() }
    var movesLeft by remember { mutableIntStateOf(p.moves) }
    var sel by remember { mutableStateOf<Int?>(null) }
    var busy by remember { mutableStateOf(false) }
    var done by remember { mutableStateOf(false) }
    val popping = remember { mutableStateListOf<Int>() }
    var hintPair by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var swapAnim by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var dropKey by remember { mutableIntStateOf(0) }

    fun colorOf(v: Int) = if (v < 0) -1 else v % 100
    fun kindOf(v: Int) = if (v < 0) 0 else v / 100 * 100

    fun findMatches(): Pair<Set<Int>, List<Pair<Int, Int>>> {
        // Eşleşen hücreler ve oluşacak özel taşlar (konum, değer)
        val out = HashSet<Int>()
        val specials = ArrayList<Pair<Int, Int>>()
        for (r in 0 until n) {
            var c = 0
            while (c < n) {
                val col = colorOf(board[r * n + c]); var e = c
                while (e + 1 < n && col >= 0 && colorOf(board[r * n + e + 1]) == col) e++
                if (col >= 0 && e - c + 1 >= 3) {
                    for (k in c..e) out += r * n + k
                    if (e - c + 1 == 4) specials += (r * n + (c + e) / 2) to (STRIPE_H + col)
                    if (e - c + 1 >= 5) specials += (r * n + (c + e) / 2) to (BOMB + col)
                }
                c = e + 1
            }
        }
        for (c in 0 until n) {
            var r = 0
            while (r < n) {
                val col = colorOf(board[r * n + c]); var e = r
                while (e + 1 < n && col >= 0 && colorOf(board[(e + 1) * n + c]) == col) e++
                if (col >= 0 && e - r + 1 >= 3) {
                    for (k in r..e) out += k * n + c
                    if (e - r + 1 == 4) specials += (((r + e) / 2) * n + c) to (STRIPE_V + col)
                    if (e - r + 1 >= 5) specials += (((r + e) / 2) * n + c) to (BOMB + col)
                }
                r = e + 1
            }
        }
        return out to specials
    }

    /** Özel taşların patlama alanını genişletir. */
    fun expand(cells: Set<Int>): Set<Int> {
        val result = HashSet(cells)
        val queue = ArrayDeque(cells.toList())
        while (queue.isNotEmpty()) {
            val i = queue.removeFirst()
            val v = board[i]
            val extra = when (kindOf(v)) {
                STRIPE_H -> (0 until n).map { (i / n) * n + it }
                STRIPE_V -> (0 until n).map { it * n + i % n }
                BOMB -> board.indices.filter { colorOf(board[it]) == colorOf(v) }
                else -> emptyList()
            }
            for (x in extra) if (result.add(x)) queue.add(x)
        }
        return result
    }

    fun goalsDone() = p.goals.all { (collected[it.color] ?: 0) >= it.count }

    suspend fun resolve(): Int {
        var cascades = 0
        while (true) {
            val (m, specials) = findMatches()
            if (m.isEmpty()) break
            cascades++
            val all = expand(m)
            popping.clear(); popping.addAll(all)
            for (i in all) { val c = colorOf(board[i]); if (c >= 0) collected[c] = (collected[c] ?: 0) + 1 }
            if (cascades > 1) ctl.fx.float("Zincir x$cascades", NeonYellow, 0.5f, 0.2f)
            ctl.sound.playTap()
            delay(230)
            for (i in all) board[i] = -1
            for ((pos, v) in specials) board[pos] = v
            popping.clear()
            // Yerçekimi
            for (c in 0 until n) {
                val col = (0 until n).map { board[it * n + c] }.filter { it >= 0 }
                val fill = n - col.size
                val newCol = List(fill) { rng.nextInt(p.colors) } + col
                for (r in 0 until n) board[r * n + c] = newCol[r]
            }
            dropKey++
            delay(200)
        }
        // Hamle kalmadıysa karıştır
        if (!Match3Generator.hasAnyMove(board.map { colorOf(it) }, n)) {
            val fresh = Match3Generator.freshBoard(n, p.colors, rng)
            fresh.forEachIndexed { i, v -> board[i] = v }
            ctl.fx.float("Karıştırıldı", NeonCyan)
        }
        return cascades
    }

    fun checkEnd() {
        if (goalsDone()) {
            done = true
            ctl.correct("Hedefler tamam!")
            scope.launch { delay(900); finish(true, (50 + movesLeft * 50 / p.moves).coerceIn(20, 100)) }
        } else if (movesLeft <= 0) {
            done = true
            ctl.fx.wrong("Hamle bitti")
            ctl.sound.playWrong()
            scope.launch { delay(1000); finish(false, 0) }
        }
    }

    fun trySwap(a: Int, b: Int) {
        if (busy || done) return
        val ar = a / n; val ac = a % n; val br = b / n; val bc = b % n
        if (abs(ar - br) + abs(ac - bc) != 1) return
        busy = true
        hintPair = null
        scope.launch {
            swapAnim = a to b
            delay(140)
            val t = board[a]; board[a] = board[b]; board[b] = t
            swapAnim = null
            val special = kindOf(board[a]) == BOMB || kindOf(board[b]) == BOMB
            if (special) {
                // Renk bombası, takas edildiği rengin hepsini patlatır
                val bombIdx = if (kindOf(board[a]) == BOMB) a else b
                val other = if (bombIdx == a) b else a
                val target = colorOf(board[other])
                val cells = board.indices.filter { colorOf(board[it]) == target }.toSet() + bombIdx
                val all = expand(cells - bombIdx) + bombIdx
                popping.addAll(all)
                for (i in all) { val c = colorOf(board[i]); if (c >= 0) collected[c] = (collected[c] ?: 0) + 1 }
                ctl.fx.correct("BOMBA!")
                delay(260)
                for (i in all) board[i] = -1
                popping.clear()
                for (c in 0 until n) {
                    val col = (0 until n).map { board[it * n + c] }.filter { it >= 0 }
                    val newCol = List(n - col.size) { rng.nextInt(p.colors) } + col
                    for (r in 0 until n) board[r * n + c] = newCol[r]
                }
                movesLeft--
                resolve()
            } else if (findMatches().first.isEmpty()) {
                delay(120)
                val t2 = board[a]; board[a] = board[b]; board[b] = t2
                ctl.sound.playWrong()
            } else {
                movesLeft--
                val c = resolve()
                if (c >= 2) ctl.sound.playCorrect()
            }
            ctl.statusText = "Hamle: $movesLeft"
            busy = false
            checkEnd()
        }
    }

    LaunchedEffect(Unit) {
        ctl.statusText = "Hamle: $movesLeft"
        ctl.hint = hint@{
            if (done || busy) return@hint null
            val vals = board.map { colorOf(it) }.toMutableList()
            for (i in 0 until n * n) for (j in listOf(if (i % n + 1 < n) i + 1 else -1, if (i / n + 1 < n) i + n else -1)) {
                if (j < 0) continue
                val t = vals[i]; vals[i] = vals[j]; vals[j] = t
                val ok = Match3Generator.hasMatchAt(vals, n, i) || Match3Generator.hasMatchAt(vals, n, j)
                vals[j] = vals[i]; vals[i] = t
                if (ok) {
                    scope.launch { hintPair = i to j; delay(2500); hintPair = null }
                    return@hint "Parlayan iki taşı yer değiştir"
                }
            }
            null
        }
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            p.goals.forEach { g ->
                val got = (collected[g.color] ?: 0).coerceAtMost(g.count)
                val ok = got >= g.count
                Row(
                    Modifier.clip(RoundedCornerShape(50)).background(NeonCard)
                        .border(1.dp, if (ok) NeonGreen else PALETTE10[g.color], RoundedCornerShape(50)).padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Gem(g.color, 0, 22.dp, false)
                    Spacer(Modifier.width(6.dp))
                    Text(if (ok) "✓" else "$got/${g.count}", color = if (ok) NeonGreen else TextPrimary, fontWeight = FontWeight.Bold)
                }
            }
            Text("🎯 $movesLeft hamle", color = if (movesLeft <= 3) NeonRed else NeonYellow, fontWeight = FontWeight.ExtraBold)
        }
        Spacer(Modifier.height(10.dp))
        BoxWithConstraints(
            Modifier
                .widthIn(max = 430.dp)
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(18.dp))
                .background(NeonCard)
                .border(1.dp, NeonBorder, RoundedCornerShape(18.dp))
                .padding(4.dp)
        ) {
            val cell = maxWidth / n
            val cellPx = with(LocalDensity.current) { cell.toPx() }
            Box(
                Modifier.fillMaxSize().pointerInput(Unit) {
                    var start = -1
                    var acc = Offset.Zero
                    detectDragGestures(
                        onDragStart = { o -> start = (o.y / cellPx).toInt().coerceIn(0, n - 1) * n + (o.x / cellPx).toInt().coerceIn(0, n - 1); acc = Offset.Zero },
                        onDragEnd = { start = -1 }
                    ) { ch, d ->
                        ch.consume()
                        if (start < 0) return@detectDragGestures
                        acc += d
                        val th = cellPx * 0.35f
                        if (abs(acc.x) > th || abs(acc.y) > th) {
                            val target = if (abs(acc.x) > abs(acc.y)) {
                                if (acc.x > 0 && start % n < n - 1) start + 1 else if (acc.x < 0 && start % n > 0) start - 1 else -1
                            } else {
                                if (acc.y > 0 && start / n < n - 1) start + n else if (acc.y < 0 && start / n > 0) start - n else -1
                            }
                            if (target >= 0) trySwap(start, target)
                            start = -1
                        }
                    }
                }.pointerInput(Unit) {
                    detectTapGestures { o ->
                        val i = (o.y / cellPx).toInt().coerceIn(0, n - 1) * n + (o.x / cellPx).toInt().coerceIn(0, n - 1)
                        val s = sel
                        if (s == null) sel = i else { sel = null; if (s != i) trySwap(s, i) }
                    }
                }
            ) {
                for (i in 0 until n * n) {
                    val v = board[i]
                    if (v < 0) continue
                    val isHint = hintPair?.let { it.first == i || it.second == i } == true
                    val sw = swapAnim
                    val dx = if (sw != null && (sw.first == i || sw.second == i)) {
                        val other = if (sw.first == i) sw.second else sw.first
                        ((other % n) - (i % n)) * 0.5f to ((other / n) - (i / n)) * 0.5f
                    } else 0f to 0f
                    Box(
                        Modifier
                            .offset(cell * (i % n) + cell * dx.first, cell * (i / n) + cell * dx.second)
                            .size(cell)
                            .padding(3.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Gem(colorOf(v), kindOf(v), cell - 6.dp, i in popping, selected = sel == i || isHint, dropKey = dropKey)
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text("Kaydır ya da iki taşa sırayla dokun · 4'lü = çizgi, 5'li = bomba", color = TextMuted, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
    }
}

@Composable
private fun Gem(color: Int, kind: Int, gemSize: androidx.compose.ui.unit.Dp, popping: Boolean, selected: Boolean = false, dropKey: Int = 0) {
    val c = PALETTE10[color % PALETTE10.size]
    val scale by animateFloatAsState(if (popping) 1.35f else if (selected) 1.1f else 1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "gem")
    val alpha by animateFloatAsState(if (popping) 0f else 1f, tween(220), label = "ga")
    val appear = remember { Animatable(0.4f) }
    LaunchedEffect(dropKey) { if (appear.value < 1f) appear.animateTo(1f, spring(dampingRatio = 0.5f)) }
    Box(
        Modifier
            .size(gemSize)
            .graphicsLayer { scaleX = scale * appear.value; scaleY = scale * appear.value; this.alpha = alpha }
            .then(if (selected || kind != 0) Modifier.neonGlow(if (selected) Color.White else c, 10.dp, gemSize / 2, 0.8f) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val shape = color % 5
            drawNeonShape(if (shape == 4) 4 else shape, c, filled = true)
            if (kind == STRIPE_H || kind == STRIPE_V) {
                for (k in -1..1) {
                    val o = this.size.minDimension * 0.18f * k
                    if (kind == STRIPE_H) drawLine(Color.White, Offset(this.size.width * 0.15f, center.y + o), Offset(this.size.width * 0.85f, center.y + o), strokeWidth = 3f)
                    else drawLine(Color.White, Offset(center.x + o, this.size.height * 0.15f), Offset(center.x + o, this.size.height * 0.85f), strokeWidth = 3f)
                }
            }
            if (kind == BOMB) {
                drawCircle(Color.White, this.size.minDimension * 0.2f, center)
                drawCircle(NeonYellow, this.size.minDimension * 0.42f, center, style = Stroke(4f))
            }
        }
        if (selected) Box(Modifier.fillMaxSize().border(2.dp, Color.White, CircleShape))
    }
}

// =====================================================================
// Sudoku
// =====================================================================

@Composable
fun SudokuRound(spec: RoundSpec, ctl: RoundControls, finish: (Boolean, Int) -> Unit) {
    PuzzleHost(SudokuGenerator, spec) { p -> SudokuBoard(p, ctl, rememberFinish(finish)) }
}

@Composable
private fun SudokuBoard(p: SudokuPuzzle, ctl: RoundControls, finish: FinishOnce) {
    val scope = rememberCoroutineScope()
    val n = p.n
    val values = remember { mutableStateListOf(*Array(n * n) { if (p.given(it)) p.solution[it] else 0 }) }
    val notes = remember { mutableStateMapOf<Int, Set<Int>>() }
    var sel by remember { mutableStateOf<Int?>(null) }
    var noteMode by remember { mutableStateOf(false) }
    val wrongCell = remember { mutableStateListOf<Int>() }
    var done by remember { mutableStateOf(false) }
    var popKey by remember { mutableIntStateOf(0) }
    val history = remember { mutableStateListOf<Pair<Int, Int>>() }

    fun checkDone() {
        if (!done && (0 until n * n).all { values[it] == p.solution[it] }) {
            done = true
            ctl.stopTimer()
            ctl.correct("Sudoku tamam!")
            scope.launch { delay(900); finish(true, (100 - ctl.mistakes * 20 - (ctl.timeUsedRatio * 30).toInt()).coerceIn(15, 100)) }
        }
    }

    LaunchedEffect(Unit) {
        ctl.setLives(3)
        ctl.startTimer(when (n) { 4 -> 60; 6 -> 150; else -> 420 }) {
            done = true; ctl.mistake("Süre bitti")
            scope.launch { delay(900); finish(false, 0) }
        }
        ctl.hint = hint@{
            if (done) return@hint null
            val s = sel
            val target = if (s != null && values[s] == 0) s else (0 until n * n).firstOrNull { values[it] == 0 } ?: return@hint null
            values[target] = p.solution[target]
            notes.remove(target)
            sel = target
            popKey++
            checkDone()
            "${target / n + 1}. satır ${target % n + 1}. sütun: ${p.solution[target]}"
        }
        ctl.undo = {
            history.removeLastOrNull()?.let { (i, old) -> values[i] = old; true } ?: false
        }
    }

    fun input(v: Int) {
        val s = sel ?: return
        if (done || p.given(s)) return
        if (noteMode && v > 0) {
            val cur = notes[s] ?: emptySet()
            notes[s] = if (v in cur) cur - v else cur + v
            ctl.tap(); return
        }
        if (v == 0) { history += s to values[s]; values[s] = 0; return }
        if (p.solution[s] == v) {
            history += s to values[s]
            values[s] = v
            notes.remove(s)
            // Aynı satır/sütun/kutudaki notlardan bu sayıyı sil
            for (k in 0 until n * n) if (sameUnit(k, s, p)) notes[k]?.let { notes[k] = it - v }
            popKey++
            ctl.tap()
            val r = s / n; val c = s % n
            val rowDone = (0 until n).all { values[r * n + it] != 0 }
            val colDone = (0 until n).all { values[it * n + c] != 0 }
            if (rowDone || colDone) ctl.fx.correct()
            checkDone()
        } else {
            wrongCell.clear(); wrongCell += s
            scope.launch { delay(600); wrongCell.remove(s) }
            if (ctl.mistake("$v olmaz")) {
                done = true; ctl.stopTimer()
                scope.launch { delay(1000); finish(false, 0) }
            }
        }
    }

    GameColumn(scroll = false) {
        BoxWithConstraints(Modifier.widthIn(max = 440.dp).fillMaxWidth().aspectRatio(1f)) {
            val cell = maxWidth / n
            val selV = sel?.let { values[it] } ?: 0
            Box(Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)).background(NeonCard).border(2.dp, NeonCyan.copy(alpha = 0.6f), RoundedCornerShape(12.dp))) {
                for (i in 0 until n * n) {
                    val r = i / n; val c = i % n
                    val isSel = sel == i
                    val related = sel != null && sameUnit(i, sel!!, p)
                    val same = selV != 0 && values[i] == selV
                    val bg = when {
                        i in wrongCell -> NeonRed.copy(alpha = 0.4f)
                        isSel -> NeonCyan.copy(alpha = 0.35f)
                        same -> NeonCyan.copy(alpha = 0.18f)
                        related -> NeonCardHigh
                        else -> Color.Transparent
                    }
                    Box(
                        Modifier
                            .offset(cell * c, cell * r)
                            .size(cell)
                            .background(bg)
                            .border(0.5.dp, NeonBorder)
                            .pointerInput(i) { detectTapGestures { sel = i; ctl.tap() } },
                        contentAlignment = Alignment.Center
                    ) {
                        val v = values[i]
                        if (v != 0) {
                            Text(
                                "$v",
                                color = if (p.given(i)) TextPrimary else NeonCyan,
                                fontWeight = if (p.given(i)) FontWeight.ExtraBold else FontWeight.Bold,
                                fontSize = (cell.value * 0.5f).sp,
                                modifier = if (!p.given(i) && isSel) Modifier.pop(popKey, 0.2f) else Modifier
                            )
                        } else notes[i]?.takeIf { it.isNotEmpty() }?.let { ns ->
                            Text(ns.sorted().joinToString(" "), color = TextSecondary, fontSize = (cell.value * 0.2f).sp, textAlign = TextAlign.Center, lineHeight = (cell.value * 0.24f).sp)
                        }
                    }
                }
                // Kutu sınırları
                Canvas(Modifier.fillMaxSize()) {
                    val cp = size.width / n
                    for (k in 1 until n) {
                        if (k % p.boxC == 0) drawLine(NeonCyan.copy(alpha = 0.7f), Offset(k * cp, 0f), Offset(k * cp, size.height), strokeWidth = 4f)
                        if (k % p.boxR == 0) drawLine(NeonCyan.copy(alpha = 0.7f), Offset(0f, k * cp), Offset(size.width, k * cp), strokeWidth = 4f)
                    }
                }
            }
        }
        // Tuşlar
        val perRow = if (n <= 6) n else 5
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.widthIn(max = 420.dp)) {
            (1..n).toList().chunked(perRow).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { v ->
                        val remaining = n - values.count { it == v }
                        NeonTile(Modifier.weight(1f).height(52.dp), state = if (remaining == 0) TileState.Disabled else TileState.Normal, accent = NeonCyan, onClick = { input(v) }) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("$v", color = TextPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
                                if (remaining > 0) Text("$remaining", color = TextMuted, fontSize = 9.sp, lineHeight = 9.sp)
                            }
                        }
                    }
                    if (row.size < perRow) repeat(perRow - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NeonTile(Modifier.weight(1f).height(44.dp), state = if (noteMode) TileState.Selected else TileState.Normal, accent = NeonPurple, onClick = { noteMode = !noteMode }) {
                    Text(if (noteMode) "✏️ Not: AÇIK" else "✏️ Not", color = TextPrimary, fontWeight = FontWeight.Bold)
                }
                NeonTile(Modifier.weight(1f).height(44.dp), accent = NeonPink, onClick = { input(0) }) {
                    Text("⌫ Sil", color = TextPrimary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private fun sameUnit(a: Int, b: Int, p: SudokuPuzzle): Boolean {
    val n = p.n
    val ar = a / n; val ac = a % n; val br = b / n; val bc = b % n
    return ar == br || ac == bc || (ar / p.boxR == br / p.boxR && ac / p.boxC == bc / p.boxC)
}

// =====================================================================
// Neon Bağla
// =====================================================================

@Composable
fun FlowRound(spec: RoundSpec, ctl: RoundControls, finish: (Boolean, Int) -> Unit) {
    PuzzleHost(FlowGenerator, spec) { p -> FlowBoard(p, ctl, rememberFinish(finish)) }
}

@Composable
private fun FlowBoard(p: FlowPuzzle, ctl: RoundControls, finish: FinishOnce) {
    val scope = rememberCoroutineScope()
    val n = p.n
    val k = p.ends.size
    // Her rengin çizilen yolu (hücre listesi, bir uçtan başlar)
    val paths = remember { mutableStateListOf<List<Int>>(*Array(k) { emptyList<Int>() }) }
    var active by remember { mutableStateOf<Int?>(null) }
    var done by remember { mutableStateOf(false) }
    var moves by remember { mutableIntStateOf(0) }
    val endColor = remember { HashMap<Int, Int>().apply { p.ends.forEachIndexed { c, (a, b) -> put(a, c); put(b, c) } } }
    val connectedSet = remember { mutableStateListOf<Int>() }

    fun isConnected(c: Int): Boolean {
        val path = paths[c]
        if (path.size < 2) return false
        val (a, b) = p.ends[c]
        return (path.first() == a && path.last() == b) || (path.first() == b && path.last() == a)
    }
    fun owner(cell: Int): Int = (0 until k).firstOrNull { cell in paths[it] } ?: -1
    fun adjacent(a: Int, b: Int) = abs(a / n - b / n) + abs(a % n - b % n) == 1

    fun checkDone() {
        val allConnected = (0 until k).all { isConnected(it) }
        val filled = (0 until n * n).all { owner(it) >= 0 }
        if (allConnected && filled && !done) {
            done = true
            ctl.stopTimer()
            ctl.correct("Hepsi bağlandı!")
            scope.launch { delay(900); finish(true, (100 - (ctl.timeUsedRatio * 40).toInt() - (moves - k).coerceAtLeast(0) * 3).coerceIn(15, 100)) }
        } else if (allConnected && !filled && !done) {
            ctl.fx.float("Tüm kareleri doldur!", NeonOrange)
        }
    }

    LaunchedEffect(Unit) {
        ctl.statusText = "0/$k bağlı"
        ctl.startTimer(n * n * 2 + 20) {
            done = true; ctl.mistake("Süre bitti")
            scope.launch { delay(900); finish(false, 0) }
        }
        ctl.hint = hint@{
            if (done) return@hint null
            // Çözümle uyuşmayan bir rengi doğru yoluyla değiştir
            val c = (0 until k).firstOrNull { col ->
                val sol = p.solution.indices.filter { p.solution[it] == col }.toSet()
                !isConnected(col) || paths[col].toSet() != sol
            } ?: return@hint null
            val sol = p.paths[c]
            for (o in 0 until k) if (o != c) {
                val cut = paths[o].indexOfFirst { it in sol }
                if (cut >= 0) paths[o] = paths[o].take(cut)
            }
            paths[c] = sol
            connectedSet.clear(); connectedSet.addAll((0 until k).filter { isConnected(it) })
            ctl.statusText = "${connectedSet.size}/$k bağlı"
            checkDone()
            "Bir renk doğru yoluyla bağlandı"
        }
        ctl.undo = {
            val a = active
            if (a != null && paths[a].isNotEmpty()) { paths[a] = emptyList(); true } else false
        }
    }

    fun extend(cell: Int) {
        val c = active ?: return
        val path = paths[c]
        if (path.isEmpty()) return
        val last = path.last()
        if (cell == last) return
        // Geri gitme: kendi yolunda bir hücreye dönünce oradan sonrasını kes
        val idx = path.indexOf(cell)
        if (idx >= 0) { paths[c] = path.take(idx + 1); return }
        if (!adjacent(last, cell)) return
        if (isConnected(c)) return
        val ec = endColor[cell]
        if (ec != null && ec != c) return
        if (ec == c && cell == path.first()) return
        // Başka rengin yolunu kes
        val o = owner(cell)
        if (o >= 0 && o != c) {
            val cut = paths[o].indexOf(cell)
            paths[o] = paths[o].take(cut)
        }
        paths[c] = path + cell
        if (isConnected(c)) {
            if (c !in connectedSet) { connectedSet += c; ctl.fx.correct(); ctl.sound.playCorrect() }
        } else ctl.tap()
    }

    GameColumn(scroll = false) {
        BoxWithConstraints(
            Modifier
                .widthIn(max = 440.dp)
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(NeonBg)
                .border(1.dp, NeonBorder, RoundedCornerShape(16.dp))
        ) {
            val cellPx = with(LocalDensity.current) { (maxWidth / n).toPx() }
            fun cellAt(o: Offset): Int = (o.y / cellPx).toInt().coerceIn(0, n - 1) * n + (o.x / cellPx).toInt().coerceIn(0, n - 1)
            Canvas(
                Modifier.fillMaxSize().pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { o ->
                            if (done) return@detectDragGestures
                            val cell = cellAt(o)
                            val ec = endColor[cell]
                            val o2 = owner(cell)
                            when {
                                ec != null -> {
                                    // Uçtan yeniden başla
                                    paths[ec] = listOf(cell); active = ec; moves++
                                    connectedSet.remove(ec)
                                }
                                o2 >= 0 -> {
                                    // Yolun ortasından devam: o noktadan sonrasını kes
                                    paths[o2] = paths[o2].take(paths[o2].indexOf(cell) + 1); active = o2
                                    connectedSet.remove(o2)
                                }
                                else -> active = null
                            }
                        },
                        onDragEnd = {
                            active = null
                            connectedSet.clear(); connectedSet.addAll((0 until k).filter { isConnected(it) })
                            ctl.statusText = "${connectedSet.size}/$k bağlı"
                            checkDone()
                        },
                        onDragCancel = { active = null }
                    ) { ch, _ -> ch.consume(); extend(cellAt(ch.position)) }
                }
            ) {
                // Izgara
                for (i in 1 until n) {
                    drawLine(NeonBorder, Offset(i * cellPx, 0f), Offset(i * cellPx, size.height), strokeWidth = 1f)
                    drawLine(NeonBorder, Offset(0f, i * cellPx), Offset(size.width, i * cellPx), strokeWidth = 1f)
                }
                fun center(cell: Int) = Offset((cell % n + 0.5f) * cellPx, (cell / n + 0.5f) * cellPx)
                // Dolu hücrelerin hafif boyası
                for (c in 0 until k) {
                    val col = PALETTE10[c % PALETTE10.size]
                    for (cell in paths[c]) drawRect(col.copy(alpha = 0.14f), Offset((cell % n) * cellPx, (cell / n) * cellPx), Size(cellPx, cellPx))
                }
                // Yollar
                for (c in 0 until k) {
                    val col = PALETTE10[c % PALETTE10.size]
                    val path = paths[c]
                    for (j in 1 until path.size) {
                        drawLine(col.copy(alpha = 0.35f), center(path[j - 1]), center(path[j]), strokeWidth = cellPx * 0.55f, cap = StrokeCap.Round)
                        drawLine(col, center(path[j - 1]), center(path[j]), strokeWidth = cellPx * 0.3f, cap = StrokeCap.Round)
                    }
                }
                // Uç noktalar
                p.ends.forEachIndexed { c, (a, b) ->
                    val col = PALETTE10[c % PALETTE10.size]
                    for (e in listOf(a, b)) {
                        drawCircle(col.copy(alpha = 0.3f), cellPx * 0.42f, center(e))
                        drawCircle(col, cellPx * 0.32f, center(e))
                        if (c in connectedSet) drawCircle(Color.White, cellPx * 0.1f, center(e))
                    }
                }
            }
        }
        val filled = (0 until n * n).count { owner(it) >= 0 }
        Text("Doluluk: %${filled * 100 / (n * n)}", color = TextSecondary, style = MaterialTheme.typography.labelLarge)
        Text("Aynı renkleri bağla, çizgiler kesişmesin, tüm kareler dolsun", color = TextMuted, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
    }
}

