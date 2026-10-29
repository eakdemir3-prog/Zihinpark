package com.example.ui.games

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import com.example.game.engine.RoundSpec
import com.example.game.puzzles.MemoryCardsGenerator
import com.example.game.puzzles.MemoryCardsPuzzle
import com.example.ui.components.bounceClick
import com.example.ui.components.neonGlow
import com.example.ui.game.RoundControls
import com.example.ui.theme.NeonCard
import com.example.ui.theme.NeonCardHigh
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.NeonRed
import com.example.ui.theme.TextPrimary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun MemoryCardsRound(spec: RoundSpec, ctl: RoundControls, finish: (Boolean, Int) -> Unit) {
    PuzzleHost(MemoryCardsGenerator, spec) { p -> MemoryCardsBoard(p, ctl, rememberFinish(finish)) }
}

private enum class CardFace { Down, Up, Matched }

@Composable
private fun MemoryCardsBoard(p: MemoryCardsPuzzle, ctl: RoundControls, finish: FinishOnce) {
    val scope = rememberCoroutineScope()
    val faces = remember { mutableStateListOf(*Array(p.cards.size) { CardFace.Down }) }
    val wrongFlash = remember { mutableStateListOf<Int>() }
    var peeking by remember { mutableStateOf(p.peekMs > 0) }
    var busy by remember { mutableStateOf(false) }
    var first by remember { mutableStateOf<Int?>(null) }
    var moves by remember { mutableIntStateOf(0) }
    var matched by remember { mutableIntStateOf(0) }
    var done by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        ctl.statusText = "Hamle: 0"
        if (p.peekMs > 0) {
            delay(p.peekMs.toLong() + 600)
            peeking = false
        }
        ctl.startTimer(p.timeLimitSec) {
            done = true
            ctl.mistake("Süre bitti")
            scope.launch { delay(900); finish(false, 0) }
        }
        ctl.hint = hint@{
            if (done || busy || peeking) return@hint null
            // Açık tek kart varsa onun eşini, yoksa bir çifti eşleştir
            val f = first
            val target = f ?: faces.indices.firstOrNull { faces[it] == CardFace.Down } ?: return@hint null
            val pair = faces.indices.firstOrNull { it != target && faces[it] != CardFace.Matched && p.cards[it] == p.cards[target] } ?: return@hint null
            first = null
            faces[target] = CardFace.Matched; faces[pair] = CardFace.Matched
            matched++
            if (matched == p.pairs) {
                done = true
                ctl.stopTimer()
                scope.launch { delay(900); finish(true, (85 - (ctl.timeUsedRatio * 15).toInt()).coerceIn(15, 100)) }
            }
            "${p.symbols[p.cards[target]]} çifti eşleşti"
        }
    }

    fun onTap(i: Int) {
        if (done || busy || peeking || faces[i] != CardFace.Down) return
        ctl.tap()
        faces[i] = CardFace.Up
        val f = first
        if (f == null) {
            first = i
            return
        }
        moves++
        ctl.statusText = "Hamle: $moves"
        first = null
        if (p.cards[f] == p.cards[i]) {
            faces[f] = CardFace.Matched; faces[i] = CardFace.Matched
            matched++
            ctl.correct(if (matched == p.pairs) null else "Eşleşti!")
            if (matched == p.pairs) {
                done = true
                ctl.stopTimer()
                val extra = (moves - p.pairs).coerceAtLeast(0)
                val q = (100 - extra * (180 / p.pairs.coerceAtLeast(1)) / 3 - (ctl.timeUsedRatio * 15).toInt()).coerceIn(15, 100)
                scope.launch { delay(900); finish(true, q) }
            }
        } else {
            busy = true
            scope.launch {
                delay(450)
                wrongFlash += f; wrongFlash += i
                ctl.fx.wrong()
                delay(450)
                faces[f] = CardFace.Down; faces[i] = CardFace.Down
                wrongFlash.clear()
                busy = false
            }
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp), contentAlignment = Alignment.Center) {
        val gap = 8.dp
        val cellW = (maxWidth - gap * (p.cols - 1)) / p.cols
        val cellH = (maxHeight - gap * (p.rows - 1)) / p.rows
        val cell = min(cellW, cellH / 1.15f)
        Column(verticalArrangement = Arrangement.spacedBy(gap)) {
            for (r in 0 until p.rows) {
                Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                    for (c in 0 until p.cols) {
                        val i = r * p.cols + c
                        FlipCard(
                            symbol = p.symbols[p.cards[i]],
                            faceUp = peeking || faces[i] != CardFace.Down,
                            matched = faces[i] == CardFace.Matched,
                            wrong = i in wrongFlash,
                            width = cell,
                            onClick = { onTap(i) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FlipCard(symbol: String, faceUp: Boolean, matched: Boolean, wrong: Boolean, width: androidx.compose.ui.unit.Dp, onClick: () -> Unit) {
    val rot by animateFloatAsState(if (faceUp) 180f else 0f, tween(380), label = "flip")
    val scale by animateFloatAsState(if (matched) 1.06f else 1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "m")
    val shape = RoundedCornerShape(14.dp)
    val showFront = rot > 90f
    val color = when {
        matched -> NeonGreen
        wrong -> NeonRed
        showFront -> NeonCyan
        else -> NeonPurple
    }
    Box(
        Modifier
            .size(width, width * 1.15f)
            .graphicsLayer {
                rotationY = rot
                cameraDistance = 12f * density
                scaleX = scale; scaleY = scale
            }
            .then(if (matched || wrong) Modifier.neonGlow(color, 12.dp, 14.dp, 0.8f) else Modifier)
            .clip(shape)
            .background(
                if (showFront) Brush.verticalGradient(listOf(NeonCardHigh, NeonCard))
                else Brush.linearGradient(listOf(NeonPurple.copy(alpha = 0.55f), NeonPink.copy(alpha = 0.35f)))
            )
            .border(if (matched || wrong) 2.dp else 1.dp, color.copy(alpha = 0.8f), shape)
            .bounceClick(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (showFront) {
            Text(symbol, fontSize = (width.value * 0.45f).sp, modifier = Modifier.graphicsLayer { rotationY = 180f })
        } else {
            Text("✦", color = TextPrimary.copy(alpha = 0.7f), fontSize = (width.value * 0.3f).sp, fontWeight = FontWeight.Bold)
        }
    }
}
