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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.engine.RoundSpec
import com.example.game.puzzles.MemoryLightsGenerator
import com.example.game.puzzles.MemoryLightsPuzzle
import com.example.game.puzzles.ReflexGenerator
import com.example.game.puzzles.ReflexPuzzle
import com.example.game.puzzles.STROOP_NAMES
import com.example.game.puzzles.StroopGenerator
import com.example.game.puzzles.StroopPuzzle
import com.example.game.puzzles.TargetTapGenerator
import com.example.game.puzzles.TargetTapPuzzle
import com.example.ui.components.bounceClick
import com.example.ui.components.neonGlow
import com.example.ui.components.pop
import com.example.ui.game.RoundControls
import com.example.ui.theme.NeonBlue
import com.example.ui.theme.NeonBorder
import com.example.ui.theme.NeonCard
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonOrange
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.NeonRed
import com.example.ui.theme.NeonYellow
import com.example.ui.theme.OnNeon
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

private val PAD_COLORS = listOf(NeonRed, NeonBlue, NeonGreen, NeonYellow, NeonPurple, NeonOrange, NeonPink, NeonCyan, Color(0xFFFFFFFF))

// =====================================================================
// Hafıza Işıkları
// =====================================================================

@Composable
fun MemoryLightsRound(spec: RoundSpec, ctl: RoundControls, finish: (Boolean, Int) -> Unit) {
    PuzzleHost(MemoryLightsGenerator, spec) { p -> MemoryLightsBoard(p, ctl, rememberFinish(finish)) }
}

@Composable
private fun MemoryLightsBoard(p: MemoryLightsPuzzle, ctl: RoundControls, finish: FinishOnce) {
    val scope = rememberCoroutineScope()
    var active by remember { mutableStateOf<Int?>(null) }
    var showing by remember { mutableStateOf(true) }
    var userIndex by remember { mutableIntStateOf(0) }
    var playKey by remember { mutableIntStateOf(0) }
    var done by remember { mutableStateOf(false) }
    var wrongPad by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(Unit) { ctl.setLives(2) }
    LaunchedEffect(playKey) {
        showing = true
        userIndex = 0
        ctl.statusText = "İzle..."
        delay(900)
        for (v in p.sequence) {
            while (ctl.paused) delay(100)
            active = v
            ctl.sound.playPad(v)
            delay((p.intervalMs * 0.65f).toLong())
            active = null
            delay((p.intervalMs * 0.35f).toLong())
        }
        showing = false
        ctl.statusText = "Sıra sende · 0/${p.sequence.size}"
    }

    fun press(i: Int) {
        if (showing || done) return
        ctl.sound.playPad(i)
        scope.launch { active = i; delay(160); if (active == i) active = null }
        if (p.sequence[userIndex] == i) {
            userIndex++
            ctl.statusText = "Sıra sende · $userIndex/${p.sequence.size}"
            if (userIndex == p.sequence.size) {
                done = true
                ctl.correct("Kusursuz!")
                val q = (100 - ctl.mistakes * 40).coerceIn(10, 100)
                scope.launch { delay(800); finish(true, q) }
            }
        } else {
            wrongPad = i
            if (ctl.mistake("Yanlış sıra")) {
                done = true
                scope.launch { delay(1100); finish(false, 0) }
            } else {
                scope.launch { delay(900); wrongPad = null; playKey++ }
            }
        }
    }

    val cols = when (p.pads) { 4 -> 2; 6 -> 2; else -> 3 }
    GameColumn(scroll = false) {
        PromptCard(if (showing) "Işıkların sırasını aklında tut" else "Aynı sırayla dokun", accent = NeonYellow, sub = "${p.sequence.size} adım")
        Spacer(Modifier.height(8.dp))
        Column(
            Modifier.widthIn(max = 380.dp).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            (0 until p.pads).chunked(cols).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { i ->
                        LightPad(
                            color = PAD_COLORS[i],
                            lit = active == i,
                            wrong = wrongPad == i,
                            enabled = !showing && !done,
                            modifier = Modifier.weight(1f).aspectRatio(if (p.pads == 6) 1.25f else 1f)
                        ) { press(i) }
                    }
                }
            }
        }
    }
}

@Composable
private fun LightPad(color: Color, lit: Boolean, wrong: Boolean, enabled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val c by animateColorAsState(if (wrong) NeonRed else color, label = "pc")
    val glow by animateFloatAsState(if (lit || wrong) 1f else 0f, tween(if (lit) 60 else 260), label = "pg")
    val scale by animateFloatAsState(if (lit) 1.06f else 1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "ps")
    val shape = RoundedCornerShape(22.dp)
    Box(
        modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .neonGlow(c, 22.dp, 22.dp, glow * 0.9f)
            .clip(shape)
            .background(Brush.radialGradient(listOf(c.copy(alpha = 0.25f + glow * 0.75f), c.copy(alpha = 0.12f + glow * 0.4f))))
            .border(2.dp, c.copy(alpha = 0.4f + glow * 0.6f), shape)
            .then(if (enabled) Modifier.bounceClick(onClick = onClick) else Modifier)
    )
}

// =====================================================================
// Refleks Arenası
// =====================================================================

@Composable
fun ReflexRound(spec: RoundSpec, ctl: RoundControls, finish: (Boolean, Int) -> Unit) {
    PuzzleHost(ReflexGenerator, spec) { p -> ReflexBoard(p, ctl, rememberFinish(finish)) }
}

private enum class ReflexPhase { Ready, Waiting, Decoy, Go, Result }

@Composable
private fun ReflexBoard(p: ReflexPuzzle, ctl: RoundControls, finish: FinishOnce) {
    val scope = rememberCoroutineScope()
    var trial by remember { mutableIntStateOf(0) }
    var phase by remember { mutableStateOf(ReflexPhase.Ready) }
    var decoyColor by remember { mutableIntStateOf(0) }
    var goAt by remember { mutableLongStateOf(0L) }
    var lastMs by remember { mutableStateOf<Long?>(null) }
    val times = remember { mutableStateListOf<Long>() }
    var attemptKey by remember { mutableIntStateOf(0) }
    var done by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { ctl.setLives(3) }

    LaunchedEffect(trial, attemptKey) {
        if (done) return@LaunchedEffect
        val t = p.trials[trial]
        phase = ReflexPhase.Ready
        ctl.statusText = "Deneme ${trial + 1}/${p.trials.size} · hedef < ${p.targetMs} ms"
        delay(700)
        phase = ReflexPhase.Waiting
        var elapsed = 0
        for ((at, color) in t.decoys) {
            delay((at - elapsed).toLong().coerceAtLeast(0))
            elapsed = at
            decoyColor = color
            phase = ReflexPhase.Decoy
            delay(450)
            elapsed += 450
            phase = ReflexPhase.Waiting
        }
        delay((t.waitMs - elapsed).toLong().coerceAtLeast(250))
        goAt = System.nanoTime()
        phase = ReflexPhase.Go
    }

    fun tap() {
        if (done) return
        when (phase) {
            ReflexPhase.Go -> {
                val ms = (System.nanoTime() - goAt) / 1_000_000
                times += ms
                lastMs = ms
                phase = ReflexPhase.Result
                if (ms <= p.targetMs) ctl.correct("$ms ms") else ctl.fx.float("$ms ms", NeonOrange)
                scope.launch {
                    delay(700)
                    if (trial + 1 >= p.trials.size) {
                        done = true
                        val avg = times.average()
                        val ok = avg <= p.targetMs
                        val q = (100 - ((avg - p.targetMs * 0.6) / (p.targetMs * 0.6) * 60).toInt() - ctl.mistakes * 15).coerceIn(10, 100)
                        finish(ok, if (ok) q else 0)
                    } else trial++
                }
            }
            ReflexPhase.Waiting, ReflexPhase.Decoy -> {
                val wasDecoy = phase == ReflexPhase.Decoy
                phase = ReflexPhase.Result
                lastMs = null
                if (ctl.mistake(if (wasDecoy) "Sahte sinyal!" else "Erken!")) {
                    done = true
                    scope.launch { delay(900); finish(false, 0) }
                } else scope.launch { delay(800); attemptKey++ }
            }
            else -> Unit
        }
    }

    val bg = when (phase) {
        ReflexPhase.Go -> NeonGreen
        ReflexPhase.Decoy -> if (decoyColor == 0) NeonYellow else NeonPurple
        ReflexPhase.Waiting -> NeonRed
        else -> NeonCard
    }
    val bgAnim by animateColorAsState(bg, tween(if (phase == ReflexPhase.Go) 40 else 200), label = "rb")
    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .padding(16.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(Brush.radialGradient(listOf(bgAnim.copy(alpha = 0.55f), bgAnim.copy(alpha = 0.15f))))
            .border(2.dp, bgAnim, RoundedCornerShape(28.dp))
            .then(if (!p.shrinkTarget) Modifier.clickable(remember { MutableInteractionSource() }, null) { tap() } else Modifier),
        contentAlignment = Alignment.Center
    ) {
        val label = when (phase) {
            ReflexPhase.Ready -> "Hazır ol..."
            ReflexPhase.Waiting -> "Bekle..."
            ReflexPhase.Decoy -> "Bekle!"
            ReflexPhase.Go -> if (p.shrinkTarget) "HEDEFE DOKUN!" else "DOKUN!"
            ReflexPhase.Result -> lastMs?.let { "$it ms" } ?: "Erken!"
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, color = TextPrimary, fontSize = 40.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
            if (times.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text("Ortalama: ${times.average().toInt()} ms", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
            }
        }
        if (p.shrinkTarget && (phase == ReflexPhase.Go || phase == ReflexPhase.Waiting || phase == ReflexPhase.Decoy)) {
            // Üst seviyelerde yalnızca küçük hedefe dokunulur
            val t = p.trials[trial]
            val size = 84.dp
            val x = (maxWidth - size) * t.targetX
            val y = (maxHeight - size) * t.targetY
            Box(
                Modifier
                    .offset(x, y)
                    .align(Alignment.TopStart)
                    .size(size)
                    .neonGlow(bgAnim, 16.dp, size, 0.8f)
                    .clip(CircleShape)
                    .background(bgAnim)
                    .border(3.dp, TextPrimary, CircleShape)
                    .clickable(remember { MutableInteractionSource() }, null) { tap() }
            )
        }
    }
}

// =====================================================================
// Renk Kaçışı
// =====================================================================

private val STROOP_COLORS = listOf(NeonRed, NeonBlue, NeonGreen, NeonYellow, NeonPurple, NeonOrange)

@Composable
fun StroopRound(spec: RoundSpec, ctl: RoundControls, finish: (Boolean, Int) -> Unit) {
    PuzzleHost(StroopGenerator, spec) { p -> StroopBoard(p, ctl, rememberFinish(finish)) }
}

@Composable
private fun StroopBoard(p: StroopPuzzle, ctl: RoundControls, finish: FinishOnce) {
    val scope = rememberCoroutineScope()
    var idx by remember { mutableIntStateOf(0) }
    var done by remember { mutableStateOf(false) }
    var correctCount by remember { mutableIntStateOf(0) }
    var popKey by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        ctl.setLives(p.allowedMistakes + 1)
        ctl.statusText = "0/${p.items.size}"
        ctl.startTimer(p.timeLimitSec) {
            done = true
            ctl.mistake("Süre bitti")
            scope.launch { delay(900); finish(false, 0) }
        }
    }

    fun answer(c: Int) {
        if (done) return
        val item = p.items[idx]
        if (c == item.answer) {
            correctCount++
            ctl.fx.correct()
            ctl.sound.playTap()
        } else {
            if (ctl.mistake()) {
                done = true
                ctl.stopTimer()
                scope.launch { delay(900); finish(false, 0) }
                return
            }
        }
        popKey++
        if (idx + 1 >= p.items.size) {
            done = true
            ctl.stopTimer()
            ctl.correct("Bitti!")
            val q = (100 - ctl.mistakes * 20 - (ctl.timeUsedRatio * 30).toInt()).coerceIn(10, 100)
            scope.launch { delay(700); finish(true, q) }
        } else {
            idx++
        }
        ctl.statusText = "${idx}/${p.items.size}"
    }

    val item = p.items[idx]
    GameColumn(scroll = false) {
        val ruleColor = if (item.byMeaning) NeonPink else NeonCyan
        Text(
            if (item.byMeaning) "KELİMENİN ANLAMI" else "YAZININ RENGİ",
            color = OnNeon, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .neonGlow(ruleColor, 12.dp, 50.dp, 0.6f)
                .clip(RoundedCornerShape(50))
                .background(ruleColor)
                .padding(horizontal = 18.dp, vertical = 8.dp)
                .pop(if (item.byMeaning) idx else 0, 0.08f)
        )
        Box(
            Modifier.fillMaxWidth().height(170.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                STROOP_NAMES[item.word],
                color = STROOP_COLORS[item.ink],
                fontSize = 54.sp,
                fontWeight = FontWeight.ExtraBold,
                style = MaterialTheme.typography.displayMedium.copy(
                    shadow = androidx.compose.ui.graphics.Shadow(STROOP_COLORS[item.ink], blurRadius = 30f)
                ),
                modifier = Modifier.pop(popKey, 0.1f)
            )
        }
        val opts = (0 until p.colors).toList()
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            opts.chunked(if (p.colors <= 4) 2 else 3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { c ->
                        NeonTile(Modifier.weight(1f).height(62.dp), accent = STROOP_COLORS[c], onClick = { answer(c) }) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(16.dp).clip(CircleShape).background(STROOP_COLORS[c]))
                                Spacer(Modifier.size(8.dp))
                                Text(STROOP_NAMES[c], color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        }
        Text("Doğru: $correctCount", color = TextSecondary, style = MaterialTheme.typography.labelMedium)
    }
}

// =====================================================================
// Hedefe Dokun
// =====================================================================

private val TARGET_PALETTE = listOf(NeonBlue, NeonPink, NeonGreen, NeonYellow, NeonPurple)

@Composable
fun TargetTapRound(spec: RoundSpec, ctl: RoundControls, finish: (Boolean, Int) -> Unit) {
    PuzzleHost(TargetTapGenerator, spec) { p -> TargetTapBoard(p, ctl, rememberFinish(finish)) }
}

@Composable
private fun TargetTapBoard(p: TargetTapPuzzle, ctl: RoundControls, finish: FinishOnce) {
    val scope = rememberCoroutineScope()
    var now by remember { mutableLongStateOf(0L) }
    val hitSet = remember { mutableStateListOf<Int>() }
    var hits by remember { mutableIntStateOf(0) }
    var done by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        ctl.setLives(3)
        ctl.statusText = "İsabet 0/${p.quota}"
        ctl.startTimer(p.timeLimitSec) {
            done = true
            ctl.mistake("Süre bitti")
            scope.launch { delay(900); finish(false, 0) }
        }
        var last = withFrameMillis { it }
        while (isActive && !done) {
            val f = withFrameMillis { it }
            if (!ctl.paused) now += (f - last)
            last = f
        }
    }

    fun tapSpawn(i: Int) {
        if (done || i in hitSet) return
        val s = p.spawns[i]
        hitSet += i
        if (p.rule.matches(s.shape, s.color)) {
            hits++
            ctl.fx.correct()
            ctl.sound.playTap()
            ctl.statusText = "İsabet $hits/${p.quota}"
            if (hits >= p.quota) {
                done = true
                ctl.stopTimer()
                ctl.correct("Tam isabet!")
                val q = (100 - ctl.mistakes * 25 - (ctl.timeUsedRatio * 30).toInt()).coerceIn(10, 100)
                scope.launch { delay(700); finish(true, q) }
            }
        } else if (ctl.mistake("Yanlış hedef")) {
            done = true
            ctl.stopTimer()
            scope.launch { delay(900); finish(false, 0) }
        }
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        PromptCard(p.rule.text, accent = NeonPurple)
        Spacer(Modifier.height(10.dp))
        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(24.dp))
                .background(NeonCard.copy(alpha = 0.5f))
                .border(1.dp, NeonBorder, RoundedCornerShape(24.dp))
        ) {
            val size = 58.dp
            val w = maxWidth - size
            val h = maxHeight - size
            p.spawns.forEachIndexed { i, s ->
                val age = now - s.atMs
                if (age in 0..s.lifeMs && i !in hitSet) {
                    val life = age.toFloat() / s.lifeMs
                    val appear = (age / 160f).coerceAtMost(1f)
                    val fade = if (life > 0.8f) (1f - life) / 0.2f else 1f
                    val x = ((s.x + s.vx * age / 1000f).mod(1f))
                    val y = ((s.y + s.vy * age / 1000f).mod(1f))
                    Box(
                        Modifier
                            .offset(w * x, h * y)
                            .size(size)
                            .graphicsLayer { scaleX = appear; scaleY = appear; alpha = fade }
                            .pointerInput(i) { detectTapOnce { tapSpawn(i) } },
                        contentAlignment = Alignment.Center
                    ) {
                        ShapeGlyph(s.shape, TARGET_PALETTE[s.color], Modifier.fillMaxSize().padding(6.dp))
                    }
                }
            }
        }
    }
}

private suspend fun androidx.compose.ui.input.pointer.PointerInputScope.detectTapOnce(onTap: () -> Unit) {
    androidx.compose.foundation.gestures.detectTapGestures(onPress = { onTap() })
}

/** Neon çizgili şekil: 0 daire, 1 kare, 2 üçgen, 3 yıldız, 4 altıgen. */
@Composable
fun ShapeGlyph(shape: Int, color: Color, modifier: Modifier = Modifier, filled: Boolean = true, rotation: Float = 0f) {
    Canvas(modifier.graphicsLayer { rotationZ = rotation }) { drawNeonShape(shape, color, filled) }
}

fun DrawScope.drawNeonShape(shape: Int, color: Color, filled: Boolean = true, scale: Float = 1f, center: Offset = this.center) {
    val r = size.minDimension / 2f * 0.86f * scale
    val stroke = Stroke(width = r * 0.16f)
    val fillColor = color.copy(alpha = if (filled) 0.35f else 0f)
    fun both(path: Path) {
        if (filled) drawPath(path, fillColor)
        drawPath(path, color.copy(alpha = 0.35f), style = Stroke(width = stroke.width * 2.2f))
        drawPath(path, color, style = stroke)
    }
    when (shape) {
        0 -> {
            if (filled) drawCircle(fillColor, r, center)
            drawCircle(color.copy(alpha = 0.35f), r, center, style = Stroke(stroke.width * 2.2f))
            drawCircle(color, r, center, style = stroke)
        }
        1 -> both(Path().apply {
            val s = r * 0.85f
            moveTo(center.x - s, center.y - s); lineTo(center.x + s, center.y - s)
            lineTo(center.x + s, center.y + s); lineTo(center.x - s, center.y + s); close()
        })
        2 -> both(Path().apply {
            moveTo(center.x, center.y - r)
            lineTo(center.x + r * 0.95f, center.y + r * 0.7f)
            lineTo(center.x - r * 0.95f, center.y + r * 0.7f); close()
        })
        3 -> both(Path().apply {
            for (k in 0 until 10) {
                val ang = Math.toRadians((-90 + k * 36).toDouble())
                val rr = if (k % 2 == 0) r else r * 0.45f
                val px = center.x + (rr * cos(ang)).toFloat()
                val py = center.y + (rr * sin(ang)).toFloat()
                if (k == 0) moveTo(px, py) else lineTo(px, py)
            }
            close()
        })
        else -> both(Path().apply {
            for (k in 0 until 6) {
                val ang = Math.toRadians((-90 + k * 60).toDouble())
                val px = center.x + (r * cos(ang)).toFloat()
                val py = center.y + (r * sin(ang)).toFloat()
                if (k == 0) moveTo(px, py) else lineTo(px, py)
            }
            close()
        })
    }
}

