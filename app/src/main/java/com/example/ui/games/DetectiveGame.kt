package com.example.ui.games

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.engine.RoundSpec
import com.example.game.puzzles.DetectiveGenerator
import com.example.game.puzzles.DetectivePuzzle
import com.example.ui.game.RoundControls
import com.example.ui.theme.NeonCard
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonYellow
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun DetectiveRound(spec: RoundSpec, ctl: RoundControls, finish: (Boolean, Int) -> Unit) {
    PuzzleHost(DetectiveGenerator, spec) { p -> DetectiveBoard(p, ctl, rememberFinish(finish)) }
}

@Composable
private fun DetectiveBoard(p: DetectivePuzzle, ctl: RoundControls, finish: FinishOnce) {
    val scope = rememberCoroutineScope()
    val eliminated = remember { mutableStateListOf<Int>() }
    var solved by remember { mutableStateOf(false) }
    val started = remember { System.currentTimeMillis() }

    LaunchedEffect(Unit) {
        ctl.setLives(2)
        ctl.statusText = "Suçlu kim?"
        ctl.hint = {
            val left = p.innocents.filter { it !in eliminated }
            if (left.size > 1) {
                eliminated += left.first()
                ctl.fx.float("Bir masum elendi", NeonYellow)
            }
        }
    }

    GameColumn {
        PromptCard(p.story, accent = NeonPink, sub = null)
        // Suç bilgisi
        val shape = RoundedCornerShape(14.dp)
        Text(
            "🔎 ${p.fact}",
            color = NeonYellow, style = MaterialTheme.typography.titleSmall, textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(NeonYellow.copy(alpha = 0.08f))
                .border(1.dp, NeonYellow.copy(alpha = 0.5f), shape)
                .padding(12.dp)
        )
        // İpuçları sırayla kayarak gelir
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            p.clues.forEachIndexed { i, clue ->
                var visible by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) { delay(120L * i + 150); visible = true }
                AnimatedVisibility(visible, enter = slideInHorizontally(tween(320)) { it / 3 } + fadeIn(tween(320))) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(NeonCard.copy(alpha = 0.8f))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text("${i + 1}", color = NeonPink, fontWeight = FontWeight.ExtraBold, modifier = Modifier.width(22.dp))
                        Text(clue, color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
        Text("Şüpheliler", color = TextSecondary, style = MaterialTheme.typography.labelLarge, modifier = Modifier.fillMaxWidth())
        val cols = if (p.suspects.size <= 3) 3 else if (p.suspects.size == 4) 2 else 3
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            p.suspects.indices.chunked(cols).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    row.forEach { i ->
                        val s = p.suspects[i]
                        val isOut = i in eliminated
                        val state = when {
                            solved && i == p.culprit -> TileState.Correct
                            isOut -> TileState.Wrong
                            else -> TileState.Normal
                        }
                        val bounce = remember { Animatable(0f) }
                        NeonTile(
                            modifier = Modifier.weight(1f).height(118.dp),
                            state = state,
                            accent = NeonPink,
                            onClick = if (solved || isOut) null else ({
                                if (i == p.culprit) {
                                    solved = true
                                    ctl.correct("Yakalandı!")
                                    val secs = (System.currentTimeMillis() - started) / 1000f
                                    val q = (100 - ctl.mistakes * 35 - ((secs - 40f).coerceAtLeast(0f) * 0.4f).toInt()).coerceIn(10, 100)
                                    scope.launch { delay(900); finish(true, q) }
                                } else {
                                    eliminated += i
                                    scope.launch { bounce.snapTo(1f); bounce.animateTo(0f, tween(400)) }
                                    if (ctl.mistake("Masum!")) {
                                        solved = true
                                        scope.launch { delay(1200); finish(false, 0) }
                                    }
                                }
                            })
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(6.dp).graphicsLayer { rotationZ = bounce.value * 8f }
                            ) {
                                Text(if (isOut) "❌" else s.avatar, fontSize = 30.sp)
                                Spacer(Modifier.height(4.dp))
                                Text(s.name, color = if (isOut) TextMuted else TextPrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                Text(s.role, color = TextSecondary, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center, maxLines = 2)
                            }
                        }
                    }
                    repeat(cols - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
        if (solved) {
            Text(
                "Çözüm: ${p.explanation}",
                color = if (ctl.mistakes < 2) NeonGreen else TextSecondary,
                style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center
            )
        }
        Spacer(Modifier.size(8.dp))
    }
}
