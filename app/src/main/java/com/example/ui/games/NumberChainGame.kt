package com.example.ui.games

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.engine.RoundSpec
import com.example.game.engine.step
import com.example.game.puzzles.NumberChainGenerator
import com.example.game.puzzles.NumberChainPuzzle
import com.example.ui.components.neonGlow
import com.example.ui.components.pop
import com.example.ui.game.RoundControls
import com.example.ui.theme.NeonCard
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonYellow
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun NumberChainRound(spec: RoundSpec, ctl: RoundControls, finish: (Boolean, Int) -> Unit) {
    PuzzleHost(NumberChainGenerator, spec) { p -> NumberChainBoard(p, spec, ctl, rememberFinish(finish)) }
}

@Composable
private fun NumberChainBoard(p: NumberChainPuzzle, spec: RoundSpec, ctl: RoundControls, finish: FinishOnce) {
    val scope = rememberCoroutineScope()
    val states = remember { mutableStateListOf(*Array(p.options.size) { TileState.Normal }) }
    var solved by remember { mutableStateOf(false) }
    var popKey by remember { mutableIntStateOf(0) }
    val accent = NeonGreen

    LaunchedEffect(Unit) {
        ctl.setLives(2)
        ctl.startTimer(step(spec.difficulty, 30, 28, 26, 24, 22)) {
            solved = true
            ctl.mistake("Süre bitti")
            scope.launch { delay(900); finish(false, 0) }
        }
        ctl.hint = {
            val wrong = p.options.indices.filter { p.options[it] != p.answer && states[it] == TileState.Normal }
            wrong.take(2).forEach { states[it] = TileState.Disabled }
        }
    }

    GameColumn {
        PromptCard("Dizideki ? yerine hangi sayı gelir?", accent = accent)
        // Zincir
        val t = rememberInfiniteTransition(label = "q")
        val glow by t.animateFloat(0.3f, 1f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "g")
        val small = p.terms.size > 6 || p.terms.any { it >= 1000 }
        Row(
            Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(if (small) 4.dp else 6.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            p.terms.forEachIndexed { i, v ->
                val isQ = i == p.missing
                val color = if (isQ) (if (solved) NeonGreen else NeonYellow) else accent
                Box(
                    Modifier
                        .weight(1f)
                        .height(if (small) 50.dp else 58.dp)
                        .then(if (isQ) Modifier.neonGlow(color, 12.dp, 14.dp, if (solved) 0.9f else glow) else Modifier)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isQ) color.copy(alpha = 0.15f) else NeonCard)
                        .border(if (isQ) 2.dp else 1.dp, color.copy(alpha = if (isQ) 1f else 0.45f), RoundedCornerShape(14.dp))
                        .then(if (isQ) Modifier.pop(popKey, 0.25f) else Modifier),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (isQ && !solved) "?" else "$v",
                        color = if (isQ) color else TextPrimary,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = when {
                            "$v".length >= 5 -> 11.sp
                            "$v".length >= 4 -> 13.sp
                            small -> 15.sp
                            else -> 18.sp
                        },
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                }
            }
        }
        OptionGrid(
            options = p.options.map { it.toString() },
            states = states,
            columns = 2,
            accent = accent,
            tileHeight = 70.dp,
            fontSize = 24.sp,
            onPick = { i ->
                if (solved || states[i] != TileState.Normal) return@OptionGrid
                if (p.options[i] == p.answer) {
                    solved = true
                    states[i] = TileState.Correct
                    popKey++
                    ctl.stopTimer()
                    ctl.correct("Doğru!")
                    val q = (100 - ctl.mistakes * 40 - (ctl.timeUsedRatio * 35).toInt()).coerceIn(10, 100)
                    scope.launch { delay(1100); finish(true, q) }
                } else {
                    states[i] = TileState.Wrong
                    if (ctl.mistake()) {
                        solved = true
                        ctl.stopTimer()
                        val correctIdx = p.options.indexOf(p.answer)
                        states[correctIdx] = TileState.Correct
                        scope.launch { delay(1300); finish(false, 0) }
                    }
                }
            }
        )
        AnimatedVisibility(solved, enter = fadeIn() + scaleIn(initialScale = 0.9f)) {
            Text("Kural: ${p.rule}", color = TextSecondary, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
        }
    }
}

