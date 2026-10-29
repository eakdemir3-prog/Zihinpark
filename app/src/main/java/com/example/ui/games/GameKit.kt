package com.example.ui.games

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.bounceClick
import com.example.ui.components.neonGlow
import com.example.ui.theme.NeonBorder
import com.example.ui.theme.NeonCard
import com.example.ui.theme.NeonCardHigh
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonRed
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.abs

/** Seçenek kutusunun görsel durumu. */
enum class TileState { Normal, Selected, Correct, Wrong, Disabled, Hint }

/** Neon kutucuk: seçili/doğru/yanlış durumlarında renk ve ışıma animasyonlu değişir. */
@Composable
fun NeonTile(
    modifier: Modifier = Modifier,
    state: TileState = TileState.Normal,
    accent: Color = NeonCyan,
    corner: Dp = 14.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val target = when (state) {
        TileState.Normal -> NeonBorder
        TileState.Selected -> accent
        TileState.Correct -> NeonGreen
        TileState.Wrong -> NeonRed
        TileState.Disabled -> NeonBorder.copy(alpha = 0.4f)
        TileState.Hint -> NeonCyan
    }
    val border by animateColorAsState(target, tween(220), label = "tileBorder")
    val glow by animateFloatAsState(
        if (state == TileState.Normal || state == TileState.Disabled) 0f else 0.6f, tween(250), label = "tileGlow"
    )
    val scale by animateFloatAsState(
        when (state) { TileState.Selected, TileState.Correct -> 1.04f; TileState.Wrong -> 0.96f; else -> 1f },
        spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "tileScale"
    )
    val shape = RoundedCornerShape(corner)
    Box(
        modifier
            .graphicsLayer { scaleX = scale; scaleY = scale; alpha = if (state == TileState.Disabled) 0.55f else 1f }
            .neonGlow(border, radius = 12.dp, cornerRadius = corner, alpha = glow)
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        if (state == TileState.Normal || state == TileState.Disabled) NeonCardHigh else border.copy(alpha = 0.22f),
                        NeonCard
                    )
                )
            )
            .border(if (state == TileState.Normal) 1.dp else 2.dp, border, shape)
            .then(if (onClick != null && state != TileState.Disabled) Modifier.bounceClick(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        content()
        if (state == TileState.Disabled) {
            Text("✕", color = NeonRed.copy(alpha = 0.85f), fontSize = 34.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

/** Soru/komut kartı. */
@Composable
fun PromptCard(
    text: String,
    modifier: Modifier = Modifier,
    accent: Color = NeonCyan,
    sub: String? = null,
    big: Boolean = false
) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.verticalGradient(listOf(accent.copy(alpha = 0.12f), NeonCard.copy(alpha = 0.6f))))
            .border(1.dp, accent.copy(alpha = 0.5f), shape)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text,
            color = TextPrimary,
            style = if (big) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
        if (sub != null) {
            Spacer(Modifier.height(4.dp))
            Text(sub, color = TextSecondary, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
        }
    }
}

/** Çoktan seçmeli seçenek ızgarası. */
@Composable
fun OptionGrid(
    options: List<String>,
    states: List<TileState>,
    columns: Int,
    onPick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = NeonCyan,
    tileHeight: Dp = 64.dp,
    fontSize: TextUnit = 18.sp
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        options.chunked(columns).forEachIndexed { rowIdx, row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEachIndexed { colIdx, label ->
                    val i = rowIdx * columns + colIdx
                    NeonTile(
                        modifier = Modifier.weight(1f).height(tileHeight),
                        state = states.getOrElse(i) { TileState.Normal },
                        accent = accent,
                        onClick = { onPick(i) }
                    ) {
                        Text(
                            label, color = TextPrimary, fontSize = fontSize, fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center, modifier = Modifier.padding(6.dp), lineHeight = (fontSize.value + 4).sp
                        )
                    }
                }
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/** Oyun alanı için kaydırılabilir dikey düzen. */
@Composable
fun GameColumn(
    modifier: Modifier = Modifier,
    scroll: Boolean = true,
    padding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier
            .fillMaxSize()
            .then(if (scroll) Modifier.verticalScroll(rememberScrollState()) else Modifier)
            .padding(padding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
        content = content
    )
}

enum class Dir(val dr: Int, val dc: Int) { UP(-1, 0), DOWN(1, 0), LEFT(0, -1), RIGHT(0, 1) }

/** Kaydırma hareketini yöne çevirir. */
fun Modifier.swipeDirections(enabled: Boolean = true, onSwipe: (Dir) -> Unit): Modifier = this.pointerInput(enabled) {
    if (!enabled) return@pointerInput
    var total = androidx.compose.ui.geometry.Offset.Zero
    var fired = false
    detectDragGestures(
        onDragStart = { total = androidx.compose.ui.geometry.Offset.Zero; fired = false },
        onDragEnd = { },
        onDragCancel = { }
    ) { change, drag ->
        change.consume()
        if (fired) return@detectDragGestures
        total += drag
        val th = 36.dp.toPx()
        if (abs(total.x) > th || abs(total.y) > th) {
            fired = true
            onSwipe(
                if (abs(total.x) > abs(total.y)) (if (total.x > 0) Dir.RIGHT else Dir.LEFT)
                else (if (total.y > 0) Dir.DOWN else Dir.UP)
            )
        }
    }
}

/** Yön tuşları. */
@Composable
fun DPad(onMove: (Dir) -> Unit, accent: Color = NeonCyan, size: Dp = 56.dp) {
    @Composable
    fun key(d: Dir, icon: androidx.compose.ui.graphics.vector.ImageVector) {
        NeonTile(Modifier.size(size), accent = accent, onClick = { onMove(d) }) {
            Icon(icon, contentDescription = d.name, tint = accent, modifier = Modifier.size(32.dp))
        }
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        key(Dir.UP, Icons.Default.KeyboardArrowUp)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            key(Dir.LEFT, Icons.Default.KeyboardArrowLeft)
            Spacer(Modifier.size(size))
            key(Dir.RIGHT, Icons.Default.KeyboardArrowRight)
        }
        key(Dir.DOWN, Icons.Default.KeyboardArrowDown)
    }
}

/** Kare bir ızgaranın kapsayıcısı (genişliğe göre kare). */
@Composable
fun SquareBoard(modifier: Modifier = Modifier, maxWidth: Dp = 420.dp, content: @Composable () -> Unit) {
    Box(
        modifier
            .widthIn(max = maxWidth)
            .fillMaxWidth()
            .aspectRatio(1f),
        contentAlignment = Alignment.Center
    ) { content() }
}

/** Bir turu yalnızca bir kez bitirmeyi garanti eden yardımcı. */
class FinishOnce(private val finish: (Boolean, Int) -> Unit) {
    var done by mutableStateOf(false)
        private set

    operator fun invoke(success: Boolean, quality: Int) {
        if (done) return
        done = true
        finish(success, quality)
    }
}

@Composable
fun rememberFinish(finish: (Boolean, Int) -> Unit): FinishOnce = remember { FinishOnce(finish) }


/**
 * Çoktan seçmeli ipucu: [n] yanlış şıkkı eler. Elenecek yanlış kalmadıysa doğru şıkkı parlatır.
 * Mesaj döner; yapacak bir şey yoksa null.
 */
fun eliminateOptions(states: MutableList<TileState>, answer: Int, n: Int, finished: Boolean): String? {
    if (finished) return null
    val wrong = states.indices.filter { it != answer && states[it] == TileState.Normal }
    return if (wrong.size > 1) {
        val k = minOf(n, wrong.size - 0).coerceAtMost(wrong.size)
        wrong.shuffled().take(k).forEach { states[it] = TileState.Disabled }
        "$k yanlış şık elendi"
    } else if (states[answer] == TileState.Normal) {
        wrong.forEach { states[it] = TileState.Disabled }
        states[answer] = TileState.Hint
        "Doğru cevap parlıyor"
    } else null
}

/** Dokunulabilir mi (normal ya da ipucuyla parlatılmış). */
val TileState.pickable: Boolean get() = this == TileState.Normal || this == TileState.Hint
