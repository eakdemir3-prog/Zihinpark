package com.example.ui.games

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.sp
import com.example.game.engine.PuzzleBank
import com.example.game.engine.PuzzleGenerator
import com.example.game.engine.RoundSpec
import com.example.ui.theme.NeonCyan
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Bulmacayı arka planda üretir; hazır olana kadar neon yükleme göstergesi çizer. */
@Composable
fun <P : Any> PuzzleHost(
    gen: PuzzleGenerator<P>,
    spec: RoundSpec,
    content: @Composable (P) -> Unit
) {
    val puzzle by produceState<P?>(initialValue = null, spec) {
        value = withContext(Dispatchers.Default) { PuzzleBank.forRound(gen, spec) }
    }
    val p = puzzle
    if (p == null) {
        val t = rememberInfiniteTransition(label = "load")
        val r by t.animateFloat(0f, 360f, infiniteRepeatable(tween(900), RepeatMode.Restart), label = "r")
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("◌", color = NeonCyan, fontSize = 48.sp, modifier = Modifier.graphicsLayer { rotationZ = r })
        }
    } else {
        content(p)
    }
}
