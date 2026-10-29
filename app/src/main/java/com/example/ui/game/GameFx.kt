package com.example.ui.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.sp
import com.example.ui.theme.Exo2
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonOrange
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.NeonRed
import com.example.ui.theme.NeonYellow
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

/**
 * Oyun içi efektlerin ortak durumu. Oyunlar [correct] / [wrong] çağırır;
 * [FxOverlay] ekran kenarı parlaması, uçan puan yazıları ve konfetiyi çizer.
 */
class GameFxState {
    var correctPulse by mutableIntStateOf(0)
        private set
    var wrongPulse by mutableIntStateOf(0)
        private set
    var confettiBurst by mutableIntStateOf(0)
        private set

    internal val floaters = mutableStateListOf<Floater>()
    private var nextId = 0L

    /** Doğru hamle: yeşil parlama ve isteğe bağlı uçan yazı. */
    fun correct(label: String? = null, color: Color = NeonGreen) {
        correctPulse++
        if (label != null) float(label, color)
    }

    /** Yanlış hamle: kırmızı parlama (ekranı sarsmak için [wrongPulse] izlenir). */
    fun wrong(label: String? = null) {
        wrongPulse++
        if (label != null) float(label, NeonRed)
    }

    fun float(label: String, color: Color = NeonCyan, xFraction: Float = 0.5f, yFraction: Float = 0.42f) {
        floaters += Floater(nextId++, label, color, xFraction, yFraction)
    }

    fun confetti() {
        confettiBurst++
    }

    internal fun remove(f: Floater) {
        floaters.remove(f)
    }
}

internal data class Floater(val id: Long, val text: String, val color: Color, val x: Float, val y: Float)

@Composable
fun rememberGameFx(): GameFxState = remember { GameFxState() }

/** Efekt katmanı; oyun içeriğinin üstüne yerleştirilir ve dokunmaları engellemez. */
@Composable
fun FxOverlay(fx: GameFxState, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize()) {
        EdgeFlash(fx.correctPulse, NeonGreen)
        EdgeFlash(fx.wrongPulse, NeonRed)
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val wPx = constraints.maxWidth
            val hPx = constraints.maxHeight
            fx.floaters.forEach { f ->
                androidx.compose.runtime.key(f.id) {
                    FloatingLabel(f, wPx, hPx) { fx.remove(f) }
                }
            }
        }
        ConfettiLayer(fx.confettiBurst)
    }
}

@Composable
private fun EdgeFlash(trigger: Int, color: Color) {
    val a = remember { Animatable(0f) }
    LaunchedEffect(trigger) {
        if (trigger == 0) return@LaunchedEffect
        a.snapTo(0.9f)
        a.animateTo(0f, tween(520, easing = FastOutSlowInEasing))
    }
    if (a.value > 0.01f) {
        Canvas(Modifier.fillMaxSize()) {
            val edge = size.minDimension * 0.22f
            val c = color.copy(alpha = 0.55f * a.value)
            drawRect(Brush.verticalGradient(listOf(c, Color.Transparent), 0f, edge), size = Size(size.width, edge))
            drawRect(
                Brush.verticalGradient(listOf(Color.Transparent, c), size.height - edge, size.height),
                topLeft = Offset(0f, size.height - edge), size = Size(size.width, edge)
            )
            drawRect(Brush.horizontalGradient(listOf(c, Color.Transparent), 0f, edge), size = Size(edge, size.height))
            drawRect(
                Brush.horizontalGradient(listOf(Color.Transparent, c), size.width - edge, size.width),
                topLeft = Offset(size.width - edge, 0f), size = Size(edge, size.height)
            )
        }
    }
}

@Composable
private fun FloatingLabel(f: Floater, wPx: Int, hPx: Int, onDone: () -> Unit) {
    val p = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        p.animateTo(1f, tween(950, easing = LinearEasing))
        onDone()
    }
    val v = p.value
    val alpha = if (v < 0.15f) v / 0.15f else (1f - (v - 0.6f).coerceAtLeast(0f) / 0.4f)
    val scale = if (v < 0.2f) 0.6f + v * 3f else 1.2f - (v - 0.2f) * 0.25f
    Box(Modifier.fillMaxSize()) {
        Text(
            text = f.text,
            color = f.color,
            fontFamily = Exo2,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 30.sp,
            style = androidx.compose.ui.text.TextStyle(
                shadow = androidx.compose.ui.graphics.Shadow(f.color, blurRadius = 24f)
            ),
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset {
                    IntOffset(
                        (wPx * f.x).roundToInt() - 60,
                        (hPx * f.y - v * hPx * 0.14f).roundToInt()
                    )
                }
                .graphicsLayer { this.alpha = alpha.coerceIn(0f, 1f); scaleX = scale; scaleY = scale }
        )
    }
}

private class Particle(
    var x: Float, var y: Float, var vx: Float, var vy: Float,
    var rot: Float, val vr: Float, val color: Color, val w: Float, val h: Float
)

private val CONFETTI_COLORS = listOf(NeonCyan, NeonPink, NeonPurple, NeonGreen, NeonYellow, NeonOrange)

/** Ekranın üstünden yayılan neon konfeti. */
@Composable
fun ConfettiLayer(trigger: Int, modifier: Modifier = Modifier) {
    var particles by remember { mutableStateOf<List<Particle>>(emptyList()) }
    var frame by remember { mutableIntStateOf(0) }
    LaunchedEffect(trigger) {
        if (trigger == 0) return@LaunchedEffect
        val rng = Random(trigger * 7919L)
        particles = List(90) {
            val ang = rng.nextFloat() * Math.PI.toFloat() * 2f
            val speed = 0.35f + rng.nextFloat() * 0.9f
            Particle(
                x = 0.5f, y = 0.3f,
                vx = cos(ang) * speed * 0.6f, vy = sin(ang) * speed - 0.9f,
                rot = rng.nextFloat() * 360f, vr = (rng.nextFloat() - 0.5f) * 720f,
                color = CONFETTI_COLORS[rng.nextInt(CONFETTI_COLORS.size)],
                w = 6f + rng.nextFloat() * 6f, h = 10f + rng.nextFloat() * 10f
            )
        }
        var last = withFrameNanos { it }
        val start = last
        while (true) {
            val now = withFrameNanos { it }
            val dt = ((now - last) / 1_000_000_000f).coerceAtMost(0.05f)
            last = now
            particles.forEach { p ->
                p.vy += 1.6f * dt
                p.vx *= 0.995f
                p.x += p.vx * dt
                p.y += p.vy * dt
                p.rot += p.vr * dt
            }
            frame++
            if ((now - start) > 2_600_000_000L) break
        }
        particles = emptyList()
    }
    if (particles.isNotEmpty()) {
        Canvas(modifier.fillMaxSize()) {
            if (frame < 0) return@Canvas // kare sayacını okuyarak her karede yeniden çiz
            particles.forEach { p ->
                val cx = p.x * size.width
                val cy = p.y * size.height
                if (cy < size.height + 40f) {
                    rotate(p.rot, Offset(cx, cy)) {
                        drawRect(p.color, topLeft = Offset(cx - p.w / 2, cy - p.h / 2), size = Size(p.w, p.h))
                    }
                }
            }
        }
    }
}
