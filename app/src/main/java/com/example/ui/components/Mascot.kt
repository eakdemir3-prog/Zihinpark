package com.example.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import com.example.i18n.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.NeonBorder
import com.example.ui.theme.NeonCard
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.sin

/** Maskotun yüz ifadeleri. */
enum class MascotMood { Happy, Wink, Think, Sad, Cheer }

private val Ink = Color(0xFF22163F)
private val BodyTop = Color(0xFFA88BFF)
private val BodyBottom = Color(0xFF5B3BE0)
private val Wing = Color(0xFF4B2FC8)
private val Belly = Color(0xFFEDE6FF)
private val Scallop = Color(0xFFC9B8FF)
private val Gold = Color(0xFFFFC23D)
private val Beak = Color(0xFFFF9F43)

/**
 * ZihinPark maskotu "Bilge": gözlüklü, mezun şapkalı baykuş.
 * Çizim 210×200'lük bir alanda tanımlıdır ve verilen boyuta ölçeklenir.
 */
@Composable
fun Mascot(mood: MascotMood = MascotMood.Happy, size: Dp = 96.dp, modifier: Modifier = Modifier, animate: Boolean = true) {
    val bob = if (animate) {
        val t = rememberInfiniteTransition(label = "owl")
        val v by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1800), RepeatMode.Reverse), label = "b")
        v
    } else 0.5f
    Canvas(
        modifier.size(size, size * (200f / 210f)).graphicsLayer {
            translationY = (bob - 0.5f) * size.toPx() * 0.04f
            rotationZ = if (mood == MascotMood.Cheer) (bob - 0.5f) * 6f else 0f
        }
    ) {
        val k = this.size.width / 210f
        scale(k, k, pivot = Offset.Zero) { drawOwl(mood, bob) }
    }
}

private fun DrawScope.drawOwl(mood: MascotMood, bob: Float) {
    // gölge
    drawOval(BodyBottom.copy(alpha = 0.12f), Offset(53f, 177f), Size(104f, 14f))
    // kulak tüyleri
    drawPath(Path().apply { moveTo(62f, 44f); lineTo(74f, 70f); lineTo(58f, 72f); close() }, BodyBottom)
    drawPath(Path().apply { moveTo(148f, 44f); lineTo(136f, 70f); lineTo(152f, 72f); close() }, BodyBottom)
    // kanatlar (gövdenin arkasında)
    val wingUp = if (mood == MascotMood.Cheer) -48f - bob * 10f else 0f
    val left = Path().apply { moveTo(56f, 112f); quadraticBezierTo(30f, 140f, 52f, 158f); quadraticBezierTo(58f, 132f, 56f, 112f); close() }
    val right = Path().apply { moveTo(154f, 112f); quadraticBezierTo(180f, 140f, 158f, 158f); quadraticBezierTo(152f, 132f, 154f, 112f); close() }
    rotate(wingUp, Offset(56f, 116f)) { drawPath(left, Wing) }
    rotate(-wingUp, Offset(154f, 116f)) { drawPath(right, Wing) }
    // gövde
    drawOval(Brush.verticalGradient(listOf(BodyTop, BodyBottom), startY = 44f, endY = 180f), Offset(45f, 44f), Size(120f, 136f))
    drawOval(Belly, Offset(67f, 96f), Size(76f, 80f))
    for (y in listOf(128f, 144f)) {
        val p = Path().apply {
            moveTo(84f, y)
            relativeQuadraticBezierTo(6f, 6f, 12f, 0f); relativeQuadraticBezierTo(6f, 6f, 12f, 0f); relativeQuadraticBezierTo(6f, 6f, 12f, 0f)
        }
        drawPath(p, Scallop, style = Stroke(3f, cap = StrokeCap.Round))
    }
    // ayaklar
    drawOval(Beak, Offset(76f, 174f), Size(24f, 12f))
    drawOval(Beak, Offset(110f, 174f), Size(24f, 12f))
    // gözler
    val (dx, dy) = when (mood) { MascotMood.Think -> 4f to -5f; MascotMood.Sad -> 0f to 4f; else -> 0f to 0f }
    for ((i, cx) in listOf(82f, 128f).withIndex()) {
        drawCircle(Color.White, 22f, Offset(cx, 94f))
        if (mood == MascotMood.Wink && i == 1) {
            val p = Path().apply { moveTo(cx - 11f, 98f); quadraticBezierTo(cx, 86f, cx + 11f, 98f) }
            drawPath(p, Ink, style = Stroke(5f, cap = StrokeCap.Round))
        } else if (mood == MascotMood.Cheer) {
            val p = Path().apply { moveTo(cx - 11f, 98f); quadraticBezierTo(cx, 84f, cx + 11f, 98f) }
            drawPath(p, Ink, style = Stroke(5f, cap = StrokeCap.Round))
        } else {
            val px = cx + (if (i == 0) 2f else -2f) + dx
            drawCircle(Ink, 11f, Offset(px, 96f + dy))
            drawCircle(Color.White, 4f, Offset(px + 4f, 91f + dy))
            if (mood == MascotMood.Sad) {
                // düşük göz kapağı
                val lid = Path().apply { moveTo(cx - 22f, 90f); quadraticBezierTo(cx, 76f + (if (i == 0) 6f else 0f), cx + 22f, 90f + (if (i == 0) -4f else 4f)); lineTo(cx + 22f, 70f); lineTo(cx - 22f, 70f); close() }
                drawPath(lid, BodyTop)
            }
        }
    }
    // gözlük
    drawCircle(Gold, 24f, Offset(82f, 94f), style = Stroke(5f))
    drawCircle(Gold, 24f, Offset(128f, 94f), style = Stroke(5f))
    // gaga
    drawPath(Path().apply { moveTo(97f, 112f); lineTo(113f, 112f); lineTo(105f, 126f); close() }, Beak)
    if (mood == MascotMood.Happy || mood == MascotMood.Cheer || mood == MascotMood.Wink) {
        drawOval(Color(0xFFFF7A9E).copy(alpha = 0.45f), Offset(52f, 116f), Size(16f, 9f))
        drawOval(Color(0xFFFF7A9E).copy(alpha = 0.45f), Offset(142f, 116f), Size(16f, 9f))
    }
    // mezuniyet şapkası
    drawPath(Path().apply { moveTo(70f, 52f); quadraticBezierTo(105f, 34f, 140f, 52f); lineTo(132f, 60f); quadraticBezierTo(105f, 48f, 78f, 60f); close() }, Ink)
    rotate(45f, Offset(105f, 39f)) { drawRect(Ink, Offset(100f, 34f), Size(10f, 10f)) }
    drawLine(Gold, Offset(140f, 52f), Offset(150f, 74f), 3f)
    drawCircle(Gold, 4f, Offset(150f, 76f))
    if (mood == MascotMood.Think) {
        translate(0f, 0f) {
            drawCircle(Color.White, 5f, Offset(176f, 52f)); drawCircle(Color.White, 8f, Offset(188f, 34f))
        }
    }
    if (mood == MascotMood.Cheer) {
        val s = 0.6f + 0.4f * sin(bob * 3.14f)
        for ((x, y) in listOf(24f to 40f, 186f to 30f)) {
            drawCircle(Gold, 5f * s, Offset(x, y))
        }
    }
}

/** Maskot + konuşma balonu. */
@Composable
fun MascotSays(text: String, title: String? = null, mood: MascotMood = MascotMood.Happy, modifier: Modifier = Modifier, size: Dp = 84.dp) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Mascot(mood, size)
        Spacer(Modifier.width(6.dp))
        val shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomEnd = 18.dp, bottomStart = 6.dp)
        Box(
            Modifier.weight(1f).clip(shape).background(NeonCard).border(1.dp, NeonBorder, shape).padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            androidx.compose.foundation.layout.Column {
                if (title != null) Text(title, color = TextPrimary, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(text, color = if (title != null) TextSecondary else TextPrimary, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
