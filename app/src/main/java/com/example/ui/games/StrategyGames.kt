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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import com.example.game.engine.RoundSpec
import com.example.game.puzzles.ArchitectGenerator
import com.example.game.puzzles.ArchitectPuzzle
import com.example.game.puzzles.JigsawGenerator
import com.example.game.puzzles.JigsawPuzzle
import com.example.game.puzzles.MathGridGenerator
import com.example.game.puzzles.MathGridPuzzle
import com.example.game.puzzles.MazeEscapeGenerator
import com.example.game.puzzles.MazeEscapePuzzle
import com.example.game.puzzles.MergeGenerator
import com.example.game.puzzles.MergePuzzle
import com.example.game.puzzles.PathFinderGenerator
import com.example.game.puzzles.PathFinderPuzzle
import com.example.game.puzzles.RiverGenerator
import com.example.game.puzzles.RiverPuzzle
import com.example.game.puzzles.Shapes
import com.example.ui.components.NeonButton
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
import kotlin.random.Random

private val PAIR_COLORS = listOf(NeonYellow, NeonPink, NeonGreen)

// =====================================================================
// Yolun Sonu
// =====================================================================

@Composable
fun PathFinderRound(spec: RoundSpec, ctl: RoundControls, finish: (Boolean, Int) -> Unit) {
    PuzzleHost(PathFinderGenerator, spec) { p -> PathFinderBoard(p, ctl, rememberFinish(finish)) }
}

@Composable
private fun PathFinderBoard(p: PathFinderPuzzle, ctl: RoundControls, finish: FinishOnce) {
    val scope = rememberCoroutineScope()
    var pos by remember { mutableStateOf(p.start) }
    var keys by remember { mutableIntStateOf(0) }
    val opened = remember { mutableStateListOf<Int>() } // açılan kapı kodları
    var moves by remember { mutableIntStateOf(0) }
    var done by remember { mutableStateOf(false) }
    var bump by remember { mutableIntStateOf(0) }
    val history = remember { mutableStateListOf<Triple<Pair<Int, Int>, Int, Int>>() }

    LaunchedEffect(Unit) {
        ctl.statusText = "Hamle 0 · en iyi ${p.optimalMoves}"
        ctl.startTimer((p.optimalMoves * 1.6f).toInt() + 25) {
            done = true; ctl.mistake("Süre bitti")
            scope.launch { delay(900); finish(false, 0) }
        }
        ctl.undo = {
            history.removeLastOrNull()?.let { (pp, k, mv) ->
                pos = pp; keys = k; moves = mv
                opened.removeAll { door -> (k shr (door - 10)) and 1 == 0 }
            }
        }
    }

    fun move(d: Dir) {
        if (done) return
        val nr = pos.first + d.dr
        val nc = pos.second + d.dc
        if (nr !in 0 until p.n || nc !in 0 until p.n) return
        val t = p.at(nr, nc)
        if (t == 1) { bump++; return }
        if (t in 10..19 && (keys shr (t - 10)) and 1 == 0) {
            bump++
            ctl.fx.float("🔒 Önce anahtar", PAIR_COLORS[t - 10])
            return
        }
        history += Triple(pos, keys, moves)
        if (t in 10..19 && t !in opened) { opened += t; ctl.fx.correct("Kapı açıldı") ; ctl.sound.playCorrect() }
        if (t in 20..29 && (keys shr (t - 20)) and 1 == 0) {
            keys = keys or (1 shl (t - 20))
            ctl.fx.correct("🔑 Anahtar!")
            ctl.sound.playCorrect()
        } else ctl.tap()
        pos = nr to nc
        moves++
        ctl.statusText = "Hamle $moves · en iyi ${p.optimalMoves}"
        if (t == 2) {
            done = true
            ctl.stopTimer()
            ctl.correct("Hedef!")
            val ratio = p.optimalMoves.toFloat() / moves
            val q = (ratio * 100 - 5).toInt().coerceIn(15, 100)
            scope.launch { delay(900); finish(true, q) }
        }
    }

    GameColumn(scroll = false) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (i in 0 until p.pairs) {
                val has = (keys shr i) and 1 == 1
                Text(
                    if (has) "🔑 alındı" else "🔑 aranıyor",
                    color = if (has) PAIR_COLORS[i] else TextMuted,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(PAIR_COLORS[i].copy(alpha = if (has) 0.18f else 0.05f))
                        .border(1.dp, PAIR_COLORS[i].copy(alpha = if (has) 1f else 0.3f), RoundedCornerShape(50))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
        BoxWithConstraints(
            Modifier
                .widthIn(max = 420.dp)
                .fillMaxWidth()
                .aspectRatio(1f)
                .shake(bump, 5.dp)
                .swipeDirections(!done) { move(it) }
        ) {
            val cell = maxWidth / p.n
            val density = LocalDensity.current
            val cellPx = with(density) { cell.toPx() }
            val px by animateFloatAsState(pos.second * cellPx, spring(stiffness = 900f), label = "px")
            val py by animateFloatAsState(pos.first * cellPx, spring(stiffness = 900f), label = "py")
            Canvas(Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp))) {
                drawRect(NeonBg)
                for (r in 0 until p.n) for (c in 0 until p.n) {
                    val t = p.at(r, c)
                    val tl = Offset(c * cellPx, r * cellPx)
                    val sz = Size(cellPx, cellPx)
                    when {
                        t == 1 -> {
                            drawRect(NeonCard, tl, sz)
                            drawRect(NeonPurple.copy(alpha = 0.35f), tl, sz, style = Stroke(1.5f))
                        }
                        t in 10..19 && t !in opened -> {
                            val col = PAIR_COLORS[t - 10]
                            drawRoundRect(col.copy(alpha = 0.35f), tl + Offset(cellPx * 0.08f, cellPx * 0.08f), Size(cellPx * 0.84f, cellPx * 0.84f), CornerRadius(cellPx * 0.15f))
                            drawRoundRect(col, tl + Offset(cellPx * 0.08f, cellPx * 0.08f), Size(cellPx * 0.84f, cellPx * 0.84f), CornerRadius(cellPx * 0.15f), style = Stroke(cellPx * 0.08f))
                        }
                    }
                }
            }
            // Simgeler
            for (r in 0 until p.n) for (c in 0 until p.n) {
                val t = p.at(r, c)
                val emoji = when {
                    t == 2 -> "🏁"
                    t in 20..29 && (keys shr (t - 20)) and 1 == 0 -> "🔑"
                    t in 10..19 && t !in opened -> "🔒"
                    else -> null
                }
                if (emoji != null) {
                    Box(Modifier.offset(cell * c, cell * r).size(cell), contentAlignment = Alignment.Center) {
                        Text(emoji, fontSize = (cell.value * 0.6f).sp,
                            modifier = if (t in 20..29) Modifier.neonGlow(PAIR_COLORS[t - 20], 8.dp, cell, 0.8f) else Modifier)
                    }
                }
            }
            // Oyuncu
            Box(
                Modifier
                    .offset(with(density) { px.toDp() }, with(density) { py.toDp() })
                    .size(cell)
                    .padding(cell * 0.16f)
                    .neonGlow(NeonCyan, 10.dp, cell, 0.9f)
                    .clip(CircleShape)
                    .background(Brush.radialGradient(listOf(TextPrimary, NeonCyan)))
            )
        }
        DPad(onMove = { move(it) }, accent = NeonPurple, size = 52.dp)
    }
}

// =====================================================================
// Labirentten Kaçış
// =====================================================================

@Composable
fun MazeEscapeRound(spec: RoundSpec, ctl: RoundControls, finish: (Boolean, Int) -> Unit) {
    PuzzleHost(MazeEscapeGenerator, spec) { p -> MazeEscapeBoard(p, ctl, rememberFinish(finish)) }
}

@Composable
private fun MazeEscapeBoard(p: MazeEscapePuzzle, ctl: RoundControls, finish: FinishOnce) {
    val scope = rememberCoroutineScope()
    var pos by remember { mutableStateOf(1 to 1) }
    val trail = remember { mutableStateListOf(1 to 1) }
    var done by remember { mutableStateOf(false) }
    var bump by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        ctl.statusText = if (p.fogRadius > 0) "Sis var! Yolu hatırla" else "Çıkışı bul"
        ctl.startTimer(p.timeLimitSec) {
            done = true; ctl.mistake("Süre bitti")
            scope.launch { delay(900); finish(false, 0) }
        }
        ctl.hint = {
            // Çıkışa giden yolun sonraki 6 adımını iz olarak göster
            val path = com.example.game.puzzles.Mazes.path({ r, c -> p.wall(r, c) }, p.n, pos, p.exit)
            path.take(8).forEach { if (it !in trail) trail += it }
        }
    }

    fun open(r: Int, c: Int) = r in 0 until p.n && c in 0 until p.n && !p.wall(r, c)

    fun move(d: Dir) {
        if (done) return
        var (r, c) = pos
        if (!open(r + d.dr, c + d.dc)) { bump++; return }
        // Kavşağa ya da duvara kadar kay
        while (open(r + d.dr, c + d.dc)) {
            r += d.dr; c += d.dc
            if (r to c !in trail) trail += r to c
            if ((r to c) == p.exit) break
            val sides = if (d.dr != 0) listOf(0 to 1, 0 to -1) else listOf(1 to 0, -1 to 0)
            if (sides.any { (sr, sc) -> open(r + sr, c + sc) }) break
        }
        ctl.tap()
        pos = r to c
        if (pos == p.exit) {
            done = true
            ctl.stopTimer()
            ctl.correct("Kaçtın!")
            scope.launch { delay(900); finish(true, (100 - (ctl.timeUsedRatio * 70).toInt()).coerceIn(15, 100)) }
        }
    }

    GameColumn(scroll = false) {
        BoxWithConstraints(
            Modifier
                .widthIn(max = 440.dp)
                .fillMaxWidth()
                .aspectRatio(1f)
                .shake(bump, 4.dp)
                .swipeDirections(!done) { move(it) }
        ) {
            val density = LocalDensity.current
            val cellPx = with(density) { (maxWidth / p.n).toPx() }
            val px by animateFloatAsState(pos.second * cellPx, tween(180), label = "mx")
            val py by animateFloatAsState(pos.first * cellPx, tween(180), label = "my")
            Canvas(Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp))) {
                drawRect(NeonBg)
                val trailSet = trail.toHashSet()
                for (r in 0 until p.n) for (c in 0 until p.n) {
                    val tl = Offset(c * cellPx, r * cellPx)
                    val sz = Size(cellPx, cellPx)
                    if (p.wall(r, c)) {
                        drawRect(NeonCyan.copy(alpha = 0.16f), tl, sz)
                        drawRect(NeonCyan.copy(alpha = 0.55f), tl, sz, style = Stroke(1f))
                    } else if ((r to c) in trailSet) {
                        drawRect(NeonPink.copy(alpha = 0.12f), tl, sz)
                    }
                }
                // Çıkış
                drawRoundRect(NeonGreen, Offset(p.exit.second * cellPx + cellPx * 0.15f, p.exit.first * cellPx + cellPx * 0.15f), Size(cellPx * 0.7f, cellPx * 0.7f), CornerRadius(cellPx * 0.2f))
                // Oyuncu
                val center = Offset(px + cellPx / 2, py + cellPx / 2)
                drawCircle(NeonYellow.copy(alpha = 0.3f), cellPx * 0.6f, center)
                drawCircle(NeonYellow, cellPx * 0.36f, center)
                // Sis
                if (p.fogRadius > 0) {
                    val rad = cellPx * (p.fogRadius + 0.6f)
                    drawRect(
                        Brush.radialGradient(
                            listOf(Color.Transparent, NeonBg.copy(alpha = 0.92f), NeonBg),
                            center = center, radius = rad * 1.4f
                        )
                    )
                }
            }
        }
        DPad(onMove = { move(it) }, accent = NeonCyan, size = 52.dp)
    }
}

// =====================================================================
// İmkânsız Görevler
// =====================================================================

@Composable
fun RiverRound(spec: RoundSpec, ctl: RoundControls, finish: (Boolean, Int) -> Unit) {
    PuzzleHost(RiverGenerator, spec) { p -> RiverBoard(p, ctl, rememberFinish(finish)) }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RiverBoard(p: RiverPuzzle, ctl: RoundControls, finish: FinishOnce) {
    val scope = rememberCoroutineScope()
    val n = p.items.size
    val all = (1 shl n) - 1
    var far by remember { mutableIntStateOf(0) }       // karşı kıyıdakiler
    var boatSide by remember { mutableIntStateOf(0) }  // 0 başlangıç, 1 karşı
    var boat by remember { mutableIntStateOf(0) }      // kayıktakiler
    var moves by remember { mutableIntStateOf(0) }
    var crossing by remember { mutableStateOf(false) }
    var done by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val boatX = remember { Animatable(0f) }
    val history = remember { mutableStateListOf<Triple<Int, Int, Int>>() }

    LaunchedEffect(Unit) {
        ctl.setLives(3)
        ctl.statusText = "Kayık: ${p.capacity} yük · en iyi ${p.optimalMoves} geçiş"
        ctl.undo = {
            if (!crossing) history.removeLastOrNull()?.let { (f, s, m) ->
                far = f; boatSide = s; moves = m; boat = 0
                scope.launch { boatX.animateTo(s.toFloat(), tween(300)) }
            }
        }
    }

    fun onBank(side: Int): Int = if (side == 0) all and far.inv() and boat.inv() else far and boat.inv()

    fun toggle(i: Int) {
        if (done || crossing) return
        val bit = 1 shl i
        if (boat and bit != 0) { boat = boat and bit.inv(); ctl.tap(); return }
        val itemSide = if (far and bit != 0) 1 else 0
        if (itemSide != boatSide) return
        if (Integer.bitCount(boat) >= p.capacity) { ctl.fx.float("Kayık dolu", NeonOrange); return }
        boat = boat or bit
        ctl.tap()
    }

    fun cross() {
        if (done || crossing) return
        crossing = true
        val before = Triple(far, boatSide, moves)
        val newSide = 1 - boatSide
        val newFar = if (newSide == 1) far or boat else far and boat.inv()
        val leftBehind = if (newSide == 1) all and newFar.inv() else newFar
        scope.launch {
            boatX.animateTo(newSide.toFloat(), tween(650))
            val conflict = p.conflict(leftBehind)
            if (conflict != null) {
                val (a, b) = conflict
                message = "${p.items[a].emoji} ${p.items[a].name}, ${p.items[b].emoji} ${p.items[b].name.lowercase(com.example.game.puzzles.Turkish.trLocale)} yedi!"
                if (ctl.mistake("Olmaz!")) {
                    done = true
                    delay(1200); finish(false, 0)
                    return@launch
                }
                delay(900)
                boatX.animateTo(boatSide.toFloat(), tween(400))
                message = null
                crossing = false
                return@launch
            }
            history += before
            far = newFar
            boatSide = newSide
            boat = 0
            moves++
            ctl.statusText = "Geçiş $moves · en iyi ${p.optimalMoves}"
            crossing = false
            if (far == all) {
                done = true
                ctl.correct("Görev tamam!")
                val q = ((p.optimalMoves.toFloat() / moves) * 100 - ctl.mistakes * 15).toInt().coerceIn(15, 100)
                delay(900); finish(true, q)
            }
        }
    }

    GameColumn {
        PromptCard("Herkesi karşı kıyıya geçir", accent = NeonPurple, sub = "Kayıkçı yokken yan yana kalamayanlar: " +
            p.eats.joinToString(", ") { (a, b) -> "${p.items[a].emoji}→${p.items[b].emoji}" })
        Row(Modifier.fillMaxWidth().height(260.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Bank(p, onBank(0), Modifier.weight(1f), boatSide == 0) { toggle(it) }
            // Nehir
            BoxWithConstraints(
                Modifier
                    .weight(1.1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Brush.verticalGradient(listOf(NeonBlue.copy(alpha = 0.35f), NeonCyan.copy(alpha = 0.18f))))
                    .border(1.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
            ) {
                val boatW = 74.dp
                val travel = maxWidth - boatW
                Column(
                    Modifier
                        .align(Alignment.CenterStart)
                        .offset(x = travel * boatX.value)
                        .width(boatW)
                        .neonGlow(NeonYellow, 10.dp, 16.dp, 0.5f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(NeonCard)
                        .border(2.dp, NeonYellow, RoundedCornerShape(16.dp))
                        .padding(6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("🧑‍🌾", fontSize = 24.sp)
                    for (i in 0 until n) if (boat and (1 shl i) != 0) {
                        Text(p.items[i].emoji, fontSize = 24.sp, modifier = Modifier.bounceClick { toggle(i) })
                    }
                    Text("🛶", fontSize = 22.sp)
                }
            }
            Bank(p, onBank(1), Modifier.weight(1f), boatSide == 1) { toggle(it) }
        }
        message?.let { Text(it, color = NeonRed, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center) }
        NeonButton(
            if (boat == 0) "Kayıkçı tek başına geçsin" else "Karşıya Geç",
            onClick = { cross() },
            color = NeonYellow,
            modifier = Modifier.fillMaxWidth(),
            enabled = !crossing && !done
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Bank(p: RiverPuzzle, mask: Int, modifier: Modifier, active: Boolean, onTap: (Int) -> Unit) {
    Column(
        modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.verticalGradient(listOf(NeonGreen.copy(alpha = if (active) 0.22f else 0.1f), NeonCard)))
            .border(1.dp, if (active) NeonGreen else NeonBorder, RoundedCornerShape(18.dp))
            .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            for (i in p.items.indices) if (mask and (1 shl i) != 0) {
                Column(
                    Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(NeonCardHigh)
                        .bounceClick { onTap(i) }
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(p.items[i].emoji, fontSize = 26.sp)
                    Text(p.items[i].name, color = TextSecondary, fontSize = 9.sp)
                }
            }
        }
    }
}

// =====================================================================
// Şekil Mimarı
// =====================================================================

private val PIECE_COLORS = listOf(NeonCyan, NeonPink, NeonGreen, NeonYellow, NeonPurple, NeonOrange, NeonBlue)

@Composable
fun ArchitectRound(spec: RoundSpec, ctl: RoundControls, finish: (Boolean, Int) -> Unit) {
    PuzzleHost(ArchitectGenerator, spec) { p -> ArchitectBoard(p, ctl, rememberFinish(finish)) }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ArchitectBoard(p: ArchitectPuzzle, ctl: RoundControls, finish: FinishOnce) {
    val scope = rememberCoroutineScope()
    val shapes = remember { mutableStateListOf(*p.pieces.toTypedArray()) }      // döndürülebilir hâl
    val placedAt = remember { mutableStateListOf<List<Pair<Int, Int>>?>(*Array(p.pieces.size) { null }) }
    var selected by remember { mutableStateOf<Int?>(0) }
    var done by remember { mutableStateOf(false) }
    var bad by remember { mutableIntStateOf(0) }
    val history = remember { mutableStateListOf<Int>() }

    LaunchedEffect(Unit) {
        ctl.statusText = "${p.pieces.size} parça"
        ctl.startTimer(p.pieces.size * 22 + 20) {
            done = true; ctl.mistake("Süre bitti")
            scope.launch { delay(900); finish(false, 0) }
        }
        ctl.undo = {
            history.removeLastOrNull()?.let { placedAt[it] = null; selected = it }
        }
        ctl.hint = {
            // Yerleşmemiş bir parçayı çözümdeki yerine koy (çakışma yoksa)
            val occupied = placedAt.filterNotNull().flatten().toSet()
            for (i in p.pieces.indices) {
                if (placedAt[i] != null) continue
                val sol = p.solution.firstOrNull { s -> Shapes.canonical(s) == Shapes.canonical(p.pieces[i]) && s.none { it in occupied } }
                if (sol != null) { placedAt[i] = sol; history += i; break }
            }
        }
    }

    fun occupied() = placedAt.filterNotNull().flatten().toSet()

    fun tryPlace(r: Int, c: Int) {
        if (done) return
        // Yerleşmiş parçaya dokunulduysa geri al
        val hit = placedAt.indexOfFirst { it?.contains(r to c) == true }
        if (hit >= 0) { placedAt[hit] = null; selected = hit; ctl.tap(); return }
        val s = selected ?: return
        if (placedAt[s] != null) return
        val shape = shapes[s]
        val anchor = shape.first()
        val cells = shape.map { (it.first - anchor.first + r) to (it.second - anchor.second + c) }
        val occ = occupied()
        if (cells.all { it in p.region && it !in occ }) {
            placedAt[s] = cells
            history += s
            ctl.fx.correct()
            ctl.sound.playTap()
            selected = shapes.indices.firstOrNull { placedAt[it] == null }
            if (occupied().size == p.region.size) {
                done = true
                ctl.stopTimer()
                ctl.correct("Siluet tamam!")
                scope.launch { delay(900); finish(true, (100 - (ctl.timeUsedRatio * 50).toInt()).coerceIn(20, 100)) }
            }
        } else {
            bad++
            ctl.fx.float("Sığmıyor", NeonOrange)
        }
    }

    GameColumn {
        PromptCard("Siluetin tamamını parçalarla doldur", accent = NeonCyan, sub = "Parçayı seç, döndür, sol üst karesini koyacağın yere dokun")
        BoxWithConstraints(Modifier.widthIn(max = 380.dp).fillMaxWidth().shake(bad, 5.dp)) {
            val cell = min(maxWidth / p.cols, 64.dp)
            Column(Modifier.align(Alignment.Center)) {
                for (r in 0 until p.rows) Row {
                    for (c in 0 until p.cols) {
                        val inRegion = (r to c) in p.region
                        val owner = placedAt.indexOfFirst { it?.contains(r to c) == true }
                        val col = if (owner >= 0) PIECE_COLORS[owner % PIECE_COLORS.size] else null
                        Box(
                            Modifier
                                .size(cell)
                                .padding(1.5.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    when {
                                        col != null -> col.copy(alpha = 0.55f)
                                        inRegion -> NeonCardHigh
                                        else -> Color.Transparent
                                    }
                                )
                                .border(1.dp, if (col != null) col else if (inRegion) NeonBorder else Color.Transparent, RoundedCornerShape(6.dp))
                                .then(if (inRegion) Modifier.pointerInput(r, c) { detectTapGestures { tryPlace(r, c) } } else Modifier)
                        )
                    }
                }
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            shapes.forEachIndexed { i, sh ->
                if (placedAt[i] == null) {
                    val color = PIECE_COLORS[i % PIECE_COLORS.size]
                    NeonTile(
                        Modifier.size(78.dp),
                        state = if (selected == i) TileState.Selected else TileState.Normal,
                        accent = color,
                        onClick = { selected = i; ctl.tap() }
                    ) {
                        PolyominoView(sh, color, Modifier.fillMaxSize().padding(8.dp), 4)
                    }
                }
            }
        }
        NeonButton("⟳ Seçili parçayı döndür", onClick = {
            selected?.let { s -> if (placedAt[s] == null) { shapes[s] = Shapes.rotate(shapes[s]); ctl.tap() } }
        }, color = NeonPurple, outlined = true, modifier = Modifier.fillMaxWidth())
    }
}

// =====================================================================
// Sayı Mühendisi
// =====================================================================

@Composable
fun MathGridRound(spec: RoundSpec, ctl: RoundControls, finish: (Boolean, Int) -> Unit) {
    PuzzleHost(MathGridGenerator, spec) { p -> MathGridBoard(p, ctl, rememberFinish(finish)) }
}

@Composable
private fun MathGridBoard(p: MathGridPuzzle, ctl: RoundControls, finish: FinishOnce) {
    val scope = rememberCoroutineScope()
    val inputs = remember { mutableStateListOf(*Array(p.dim * p.dim) { "" }) }
    var selected by remember { mutableStateOf(p.hidden.minOrNull()) }
    var done by remember { mutableStateOf(false) }
    val wrongRows = remember { mutableStateListOf<Int>() }
    val wrongCols = remember { mutableStateListOf<Int>() }
    var shakeKey by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        ctl.setLives(3)
        ctl.statusText = "${p.hidden.size} boşluk"
        ctl.startTimer(p.hidden.size * 25 + 30) {
            done = true; ctl.mistake("Süre bitti")
            scope.launch { delay(900); finish(false, 0) }
        }
        ctl.hint = {
            p.hidden.firstOrNull { inputs[it] != p.values[it].toString() }?.let { inputs[it] = p.values[it].toString() }
        }
    }

    fun valueAt(i: Int): Int? = if (i in p.hidden) inputs[i].toIntOrNull() else p.values[i]

    fun check() {
        if (p.hidden.any { inputs[it].isEmpty() }) return
        wrongRows.clear(); wrongCols.clear()
        for (r in 0 until p.dim) {
            val nums = (0 until p.dim).map { valueAt(r * p.dim + it) ?: return }
            if (MathGridGenerator.eval(nums, p.rowOps[r]) != p.rowResults[r]) wrongRows += r
        }
        for (c in 0 until p.dim) {
            val nums = (0 until p.dim).map { valueAt(it * p.dim + c) ?: return }
            if (MathGridGenerator.eval(nums, p.colOps[c]) != p.colResults[c]) wrongCols += c
        }
        if (wrongRows.isEmpty() && wrongCols.isEmpty()) {
            done = true
            ctl.stopTimer()
            ctl.correct("Denklemler tuttu!")
            scope.launch { delay(900); finish(true, (100 - ctl.mistakes * 25 - (ctl.timeUsedRatio * 35).toInt()).coerceIn(10, 100)) }
        } else {
            shakeKey++
            if (ctl.mistake()) {
                done = true; ctl.stopTimer()
                scope.launch { delay(1000); finish(false, 0) }
            }
        }
    }

    fun press(k: String) {
        if (done) return
        val s = selected ?: return
        when (k) {
            "⌫" -> inputs[s] = inputs[s].dropLast(1)
            else -> if (inputs[s].length < 2) inputs[s] = (inputs[s] + k).trimStart('0').ifEmpty { "0" }
        }
        ctl.tap()
        if (k != "⌫" && (inputs[s].length >= 2 || (inputs[s].toIntOrNull() ?: 0) * 10 > p.maxValue)) {
            // Sonraki boş hücreye geç
            selected = p.hidden.sorted().firstOrNull { inputs[it].isEmpty() } ?: s
        }
        check()
    }

    GameColumn {
        PromptCard("Boşlukları doldur", accent = NeonGreen, sub = "Satırlar soldan sağa, sütunlar yukarıdan aşağıya · sayılar 0–${p.maxValue}")
        val size = if (p.dim == 2) 54.dp else 46.dp
        Column(Modifier.shake(shakeKey), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            for (r in 0 until p.dim) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (c in 0 until p.dim) {
                        val i = r * p.dim + c
                        val hidden = i in p.hidden
                        val isSel = selected == i
                        val bad = r in wrongRows || c in wrongCols
                        Box(
                            Modifier
                                .size(size)
                                .then(if (isSel) Modifier.neonGlow(NeonGreen, 10.dp, 12.dp, 0.8f) else Modifier)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (hidden) NeonGreen.copy(alpha = 0.1f) else NeonCardHigh)
                                .border(if (isSel) 2.dp else 1.dp, if (bad && hidden) NeonRed else if (hidden) NeonGreen else NeonBorder, RoundedCornerShape(12.dp))
                                .then(if (hidden) Modifier.bounceClick { selected = i } else Modifier),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                if (hidden) inputs[i].ifEmpty { "?" } else "${p.values[i]}",
                                color = if (hidden) (if (inputs[i].isEmpty()) TextMuted else NeonGreen) else TextPrimary,
                                fontWeight = FontWeight.ExtraBold, fontSize = 20.sp
                            )
                        }
                        if (c < p.dim - 1) OpText(p.rowOps[r][c])
                    }
                    Text("=", color = TextSecondary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    ResultBox("${p.rowResults[r]}", size, r in wrongRows)
                }
                if (r < p.dim - 1) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        for (c in 0 until p.dim) {
                            Box(Modifier.size(size, 22.dp), contentAlignment = Alignment.Center) { OpText(p.colOps[c][r]) }
                            if (c < p.dim - 1) Spacer(Modifier.width(22.dp))
                        }
                        Spacer(Modifier.width(size + 26.dp))
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                for (c in 0 until p.dim) {
                    Box(Modifier.size(size, 22.dp), contentAlignment = Alignment.Center) { Text("=", color = TextSecondary, fontWeight = FontWeight.Bold) }
                    if (c < p.dim - 1) Spacer(Modifier.width(22.dp))
                }
                Spacer(Modifier.width(size + 26.dp))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                for (c in 0 until p.dim) {
                    ResultBox("${p.colResults[c]}", size, c in wrongCols)
                    if (c < p.dim - 1) Spacer(Modifier.width(22.dp))
                }
                Spacer(Modifier.width(size + 26.dp))
            }
        }
        // Tuş takımı
        val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "⌫", "0")
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.widthIn(max = 320.dp)) {
            keys.chunked(6).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { k ->
                        NeonTile(Modifier.weight(1f).height(48.dp), accent = NeonGreen, onClick = { press(k) }) {
                            Text(k, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        }
                    }
                    repeat(6 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun OpText(op: Char) {
    Text("$op", color = NeonYellow, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, modifier = Modifier.width(18.dp), textAlign = TextAlign.Center)
}

@Composable
private fun ResultBox(text: String, size: Dp, bad: Boolean) {
    Box(
        Modifier
            .size(size)
            .clip(RoundedCornerShape(12.dp))
            .background(NeonYellow.copy(alpha = 0.1f))
            .border(1.dp, if (bad) NeonRed else NeonYellow.copy(alpha = 0.6f), RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) { Text(text, color = NeonYellow, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp) }
}

// =====================================================================
// Sayı Patlatma
// =====================================================================

private fun mergeColor(v: Int): Color = when (v) {
    2 -> NeonBlue
    4 -> NeonCyan
    8 -> NeonGreen
    16 -> NeonYellow
    32 -> NeonOrange
    64 -> NeonPink
    128 -> NeonPurple
    256 -> NeonRed
    else -> TextPrimary
}

@Composable
fun MergeRound(spec: RoundSpec, ctl: RoundControls, finish: (Boolean, Int) -> Unit) {
    PuzzleHost(MergeGenerator, spec) { p -> MergeBoard(p, ctl, rememberFinish(finish)) }
}

@Composable
private fun MergeBoard(p: MergePuzzle, ctl: RoundControls, finish: FinishOnce) {
    val scope = rememberCoroutineScope()
    val board = remember { mutableStateListOf(*p.board.toTypedArray()) }
    val popAt = remember { mutableStateListOf<Int>() }
    val rng = remember { Random(p.spawnSeed) }
    var moves by remember { mutableIntStateOf(0) }
    var done by remember { mutableStateOf(false) }
    val history = remember { mutableStateListOf<List<Int>>() }

    LaunchedEffect(Unit) {
        ctl.statusText = "Hedef ${p.target} · hamle 0/${p.moveLimit}"
        ctl.undo = { history.removeLastOrNull()?.let { prev -> prev.forEachIndexed { i, v -> board[i] = v }; moves-- } }
    }

    fun line(idx: Int, d: Dir): List<Int> {
        val n = p.size
        return (0 until n).map { k ->
            when (d) {
                Dir.LEFT -> idx * n + k
                Dir.RIGHT -> idx * n + (n - 1 - k)
                Dir.UP -> k * n + idx
                Dir.DOWN -> (n - 1 - k) * n + idx
            }
        }
    }

    fun canMove(): Boolean = Dir.entries.any { d -> (0 until p.size).any { MergeGenerator.slideRow(line(it, d).map { i -> board[i] }).third } }

    fun swipe(d: Dir) {
        if (done) return
        val snapshot = board.toList()
        var changed = false
        popAt.clear()
        for (idx in 0 until p.size) {
            val cells = line(idx, d)
            val (out, gained, ch) = MergeGenerator.slideRow(cells.map { board[it] })
            if (ch) {
                changed = true
                cells.forEachIndexed { k, cell ->
                    if (gained > 0 && out[k] != snapshot[cell] && out[k] > 0 && out[k] in snapshot.filter { it > 0 }.map { it * 2 }) popAt += cell
                    board[cell] = out[k]
                }
            }
        }
        if (!changed) return
        history += snapshot
        // Belirlenimci yeni taş
        val empty = board.indices.filter { board[it] == 0 }
        if (empty.isNotEmpty()) board[empty[rng.nextInt(empty.size)]] = if (rng.nextFloat() < 0.85f) 2 else 4
        moves++
        if (popAt.isNotEmpty()) ctl.sound.playTap()
        ctl.statusText = "Hedef ${p.target} · hamle $moves/${p.moveLimit}"
        val best = board.max()
        if (best >= p.target) {
            done = true
            ctl.correct("${p.target}!")
            val q = (100 - (moves.toFloat() / p.moveLimit * 60).toInt()).coerceIn(20, 100)
            scope.launch { delay(900); finish(true, q) }
        } else if (moves >= p.moveLimit || !canMove()) {
            done = true
            ctl.fx.wrong(if (moves >= p.moveLimit) "Hamle bitti" else "Hamle kalmadı")
            ctl.sound.playWrong()
            scope.launch { delay(1000); finish(false, 0) }
        }
    }

    GameColumn(scroll = false) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Hedef", color = TextSecondary, style = MaterialTheme.typography.titleMedium)
            MergeTile(p.target, 54.dp, false)
        }
        BoxWithConstraints(
            Modifier
                .widthIn(max = 400.dp)
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(20.dp))
                .background(NeonCard)
                .border(1.dp, NeonBorder, RoundedCornerShape(20.dp))
                .swipeDirections(!done) { swipe(it) }
                .padding(8.dp)
        ) {
            val cell = maxWidth / p.size
            Column {
                for (r in 0 until p.size) Row {
                    for (c in 0 until p.size) {
                        val i = r * p.size + c
                        Box(Modifier.size(cell).padding(4.dp)) {
                            Box(Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)).background(NeonCardHigh.copy(alpha = 0.6f)))
                            if (board[i] > 0) MergeTile(board[i], cell - 8.dp, i in popAt)
                        }
                    }
                }
            }
        }
        Text("Kaydırarak oyna", color = TextMuted, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun MergeTile(v: Int, size: Dp, pop: Boolean) {
    val c = mergeColor(v)
    val s = remember { Animatable(0.6f) }
    LaunchedEffect(v, pop) {
        s.snapTo(if (pop) 1.2f else 0.85f)
        s.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
    }
    Box(
        Modifier
            .size(size)
            .graphicsLayer { scaleX = s.value; scaleY = s.value }
            .neonGlow(c, 10.dp, 12.dp, 0.55f)
            .clip(RoundedCornerShape(12.dp))
            .background(Brush.verticalGradient(listOf(c.copy(alpha = 0.55f), c.copy(alpha = 0.25f))))
            .border(2.dp, c, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text("$v", color = TextPrimary, fontWeight = FontWeight.ExtraBold, fontSize = if (v >= 100) 20.sp else 24.sp)
    }
}

// =====================================================================
// Parçaları Birleştir
// =====================================================================

private val ART_PALETTES = listOf(
    listOf(NeonCyan, NeonPink, NeonPurple),
    listOf(NeonGreen, NeonYellow, NeonCyan),
    listOf(NeonPink, NeonOrange, NeonYellow),
    listOf(NeonPurple, NeonBlue, NeonCyan),
    listOf(NeonRed, NeonPink, NeonPurple),
    listOf(NeonOrange, NeonGreen, NeonBlue)
)

/** Tohumdan belirlenimci neon tablo çizer (tüm yapboz için tek görüntü). */
fun DrawScope.drawNeonArt(seed: Long, paletteIdx: Int, w: Float, h: Float) {
    val rng = Random(seed)
    val pal = ART_PALETTES[paletteIdx % ART_PALETTES.size]
    drawRect(Brush.linearGradient(listOf(pal[0].copy(alpha = 0.55f), NeonBg, pal[1].copy(alpha = 0.45f)), Offset(0f, 0f), Offset(w, h)), size = Size(w, h))
    // Güneş / gezegen
    val sun = Offset(w * (0.2f + rng.nextFloat() * 0.6f), h * (0.2f + rng.nextFloat() * 0.3f))
    val sr = w * (0.14f + rng.nextFloat() * 0.1f)
    drawCircle(Brush.radialGradient(listOf(pal[2], pal[2].copy(alpha = 0f)), sun, sr * 1.8f), sr * 1.8f, sun)
    drawCircle(pal[2], sr, sun)
    for (k in 0 until 4) drawLine(NeonBg, Offset(sun.x - sr, sun.y + sr * (0.1f + k * 0.2f)), Offset(sun.x + sr, sun.y + sr * (0.1f + k * 0.2f)), strokeWidth = sr * 0.07f)
    // Dağlar
    for (layer in 0 until 2) {
        val path = Path()
        val base = h * (0.62f + layer * 0.12f)
        path.moveTo(0f, h)
        path.lineTo(0f, base)
        var x = 0f
        while (x < w) {
            val peak = base - h * (0.08f + rng.nextFloat() * 0.16f)
            path.lineTo(x + w * 0.09f, peak)
            x += w * 0.18f
            path.lineTo(x, base)
        }
        path.lineTo(w, h); path.close()
        val col = pal[layer % pal.size]
        drawPath(path, NeonBg.copy(alpha = 0.85f))
        drawPath(path, col, style = Stroke(w * 0.008f))
    }
    // Izgara zemini
    val horizon = h * 0.86f
    for (k in 0..8) {
        val y = horizon + (h - horizon) * (k / 8f) * (k / 8f)
        drawLine(pal[0].copy(alpha = 0.6f), Offset(0f, y), Offset(w, y), strokeWidth = 2f)
    }
    for (k in -6..6) drawLine(pal[0].copy(alpha = 0.6f), Offset(w / 2 + k * w * 0.05f, horizon), Offset(w / 2 + k * w * 0.25f, h), strokeWidth = 2f)
    // Yıldızlar ve halkalar
    repeat(24) {
        val p = Offset(rng.nextFloat() * w, rng.nextFloat() * h * 0.55f)
        drawCircle(Color.White.copy(alpha = 0.5f + rng.nextFloat() * 0.5f), 1.5f + rng.nextFloat() * 2.5f, p)
    }
    repeat(3) {
        val p = Offset(rng.nextFloat() * w, rng.nextFloat() * h * 0.6f)
        val r = w * (0.03f + rng.nextFloat() * 0.06f)
        drawCircle(pal[rng.nextInt(pal.size)], r, p, style = Stroke(w * 0.01f))
    }
    // Şimşek / çizgi
    val zig = Path()
    var zx = rng.nextFloat() * w
    var zy = 0f
    zig.moveTo(zx, zy)
    repeat(5) { zx += (rng.nextFloat() - 0.5f) * w * 0.2f; zy += h * 0.09f; zig.lineTo(zx, zy) }
    drawPath(zig, pal[1], style = Stroke(w * 0.012f, cap = StrokeCap.Round))
}

@Composable
fun JigsawRound(spec: RoundSpec, ctl: RoundControls, finish: (Boolean, Int) -> Unit) {
    PuzzleHost(JigsawGenerator, spec) { p -> JigsawBoard(p, ctl, rememberFinish(finish)) }
}

@Composable
private fun JigsawBoard(p: JigsawPuzzle, ctl: RoundControls, finish: FinishOnce) {
    val scope = rememberCoroutineScope()
    val count = p.n * p.n
    // slots[i] = i. yuvadaki parçanın asıl konumu
    val slots = remember { mutableStateListOf(*p.permutation.toTypedArray()) }
    val rot = remember { mutableStateListOf(*p.rotations.toTypedArray()) }
    var sel by remember { mutableStateOf<Int?>(null) }
    var swaps by remember { mutableIntStateOf(0) }
    var done by remember { mutableStateOf(false) }
    var showPreview by remember { mutableStateOf(true) }
    val minSwaps = remember {
        val seen = BooleanArray(count)
        var cycles = 0
        for (i in 0 until count) if (!seen[i]) { cycles++; var j = i; while (!seen[j]) { seen[j] = true; j = p.permutation[j] } }
        count - cycles + p.rotations.count { it != 0 }
    }

    LaunchedEffect(Unit) {
        ctl.statusText = "Hamle 0 · en az $minSwaps"
        delay(1600)
        showPreview = false
        ctl.startTimer(count * 9 + 20) {
            done = true; ctl.mistake("Süre bitti")
            scope.launch { delay(900); finish(false, 0) }
        }
        ctl.hint = {
            val wrong = (0 until count).firstOrNull { slots[it] != it }
            if (wrong != null) {
                val j = slots.indexOf(wrong)
                val t = slots[wrong]; slots[wrong] = slots[j]; slots[j] = t
                rot[wrong] = 0
            }
        }
    }

    fun isRight(i: Int) = slots[i] == i && rot[i] == 0

    fun checkDone() {
        if ((0 until count).all { isRight(it) }) {
            done = true
            ctl.stopTimer()
            ctl.fx.confetti()
            ctl.correct("Tablo tamam!")
            val q = (100 - ((swaps - minSwaps).coerceAtLeast(0) * 8) - (ctl.timeUsedRatio * 20).toInt()).coerceIn(15, 100)
            scope.launch { delay(1100); finish(true, q) }
        }
    }

    fun tap(i: Int) {
        if (done || showPreview || isRight(i)) return
        val s = sel
        if (s == null) { sel = i; ctl.tap(); return }
        if (s == i) {
            // Aynı parçaya ikinci dokunuş: döndür (üst seviyeler)
            if (rot[i] != 0) { rot[i] = (rot[i] + 1) % 4; swaps++; ctl.tap() }
            sel = null
            ctl.statusText = "Hamle $swaps · en az $minSwaps"
            if (isRight(i)) ctl.fx.correct()
            checkDone()
            return
        }
        val t = slots[s]; slots[s] = slots[i]; slots[i] = t
        val tr = rot[s]; rot[s] = rot[i]; rot[i] = tr
        swaps++
        sel = null
        ctl.statusText = "Hamle $swaps · en az $minSwaps"
        if (isRight(i) || isRight(s)) ctl.fx.correct() else ctl.tap()
        checkDone()
    }

    GameColumn(scroll = false) {
        PromptCard(
            if (showPreview) "Tabloyu aklında tut..." else "İki parçaya dokunarak yerlerini değiştir",
            accent = NeonCyan,
            sub = if (p.rotations.any { it != 0 }) "Dönük parçayı çevirmek için ona iki kez dokun" else null
        )
        BoxWithConstraints(Modifier.widthIn(max = 420.dp).fillMaxWidth().aspectRatio(1f)) {
            val density = LocalDensity.current
            val boardPx = with(density) { maxWidth.toPx() }
            val tilePx = boardPx / p.n
            val tile = maxWidth / p.n
            if (showPreview) {
                Canvas(Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp))) { drawNeonArt(p.artSeed, p.palette, size.width, size.height) }
            } else {
                for (i in 0 until count) {
                    val piece = slots[i]
                    val pr = piece / p.n
                    val pc = piece % p.n
                    val r = i / p.n
                    val c = i % p.n
                    val right = isRight(i)
                    val selected = sel == i
                    val rotDeg by animateFloatAsState(rot[i] * 90f, tween(250), label = "jr$i")
                    val border by animateColorAsState(if (right) NeonGreen else if (selected) NeonYellow else NeonBorder, label = "jb$i")
                    Box(
                        Modifier
                            .offset(tile * c, tile * r)
                            .size(tile)
                            .padding(1.5.dp)
                            .then(if (selected) Modifier.neonGlow(NeonYellow, 10.dp, 8.dp, 0.9f) else Modifier)
                            .graphicsLayer { rotationZ = rotDeg; val sc = if (selected) 0.94f else 1f; scaleX = sc; scaleY = sc }
                            .clip(RoundedCornerShape(6.dp))
                            .border(if (right) 2.dp else 1.dp, border, RoundedCornerShape(6.dp))
                            .pointerInput(i) { detectTapGestures { tap(i) } }
                    ) {
                        Canvas(Modifier.fillMaxSize().clipToBounds()) {
                            translate(-pc * tilePx, -pr * tilePx) { drawNeonArt(p.artSeed, p.palette, boardPx, boardPx) }
                        }
                    }
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Önizleme:", color = TextMuted, style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.width(8.dp))
            Canvas(Modifier.size(64.dp).clip(RoundedCornerShape(8.dp))) { drawNeonArt(p.artSeed, p.palette, size.width, size.height) }
        }
    }
}
