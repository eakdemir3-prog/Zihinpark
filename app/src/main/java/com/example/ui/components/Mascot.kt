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

enum class OwlHat { Grad, None, Party, Crown, Wizard, Beanie, Headband, Bandana, Headphones, Helmet }
enum class OwlEyes { Gold, Sun, Mask, Patch, Plain }
enum class OwlExtra { None, Scarf, Cape, Bowtie }

/** Bilge'nin kılığı: renkler + şapka + gözlük + aksesuar. Avatarlar bununla çeşitlenir. */
data class MascotStyle(
    val bodyTop: Color = BodyTop,
    val bodyBottom: Color = BodyBottom,
    val wing: Color = Wing,
    val belly: Color = Belly,
    val hat: OwlHat = OwlHat.Grad,
    val eyes: OwlEyes = OwlEyes.Gold,
    val extra: OwlExtra = OwlExtra.None,
    val accent: Color = Color(0xFFFF4D6D),
    /** Gövde rengi sürekli değişir (yalnızca hareketli avatarlarda). */
    val rainbow: Boolean = false,
    /** Çevresinde parlayan yıldızlar. */
    val sparkle: Boolean = false
) {
    companion object { val Default = MascotStyle() }
}

/**
 * ZihinPark maskotu "Bilge": gözlüklü, mezun şapkalı baykuş.
 * Çizim 210×200'lük bir alanda tanımlıdır ve verilen boyuta ölçeklenir.
 */
@Composable
fun Mascot(
    mood: MascotMood = MascotMood.Happy, size: Dp = 96.dp, modifier: Modifier = Modifier, animate: Boolean = true,
    style: MascotStyle = MascotStyle.Default
) {
    val bob: Float
    val spin: Float
    if (animate) {
        val t = rememberInfiniteTransition(label = "owl")
        val v by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1800), RepeatMode.Reverse), label = "b")
        bob = v
        spin = if (style.rainbow || style.sparkle) {
            val h by t.animateFloat(0f, 1f, infiniteRepeatable(tween(5000, easing = androidx.compose.animation.core.LinearEasing), RepeatMode.Restart), label = "h")
            h
        } else 0f
    } else { bob = 0.5f; spin = 0.15f }
    Canvas(
        modifier.size(size, size * (200f / 210f)).graphicsLayer {
            translationY = (bob - 0.5f) * size.toPx() * 0.04f
            rotationZ = if (mood == MascotMood.Cheer) (bob - 0.5f) * 6f else 0f
        }
    ) {
        val k = this.size.width / 210f
        scale(k, k, pivot = Offset.Zero) { drawOwl(mood, bob, style, spin) }
    }
}

private fun DrawScope.drawOwl(mood: MascotMood, bob: Float, st: MascotStyle, spin: Float) {
    val top = if (st.rainbow) Color.hsv(spin * 360f, 0.45f, 1f) else st.bodyTop
    val bottom = if (st.rainbow) Color.hsv((spin * 360f + 30f) % 360f, 0.70f, 0.88f) else st.bodyBottom
    val wingC = if (st.rainbow) Color.hsv((spin * 360f + 40f) % 360f, 0.78f, 0.75f) else st.wing
    // gölge
    drawOval(bottom.copy(alpha = 0.12f), Offset(53f, 177f), Size(104f, 14f))
    // pelerin (gövdenin arkasında)
    if (st.extra == OwlExtra.Cape) {
        val sway = (bob - 0.5f) * 10f
        drawPath(Path().apply { moveTo(66f, 104f); lineTo(144f, 104f); lineTo(182f + sway, 184f); lineTo(28f + sway, 184f); close() }, st.accent)
    }
    // kulak tüyleri
    drawPath(Path().apply { moveTo(62f, 44f); lineTo(74f, 70f); lineTo(58f, 72f); close() }, bottom)
    drawPath(Path().apply { moveTo(148f, 44f); lineTo(136f, 70f); lineTo(152f, 72f); close() }, bottom)
    // kanatlar (gövdenin arkasında)
    val wingUp = if (mood == MascotMood.Cheer) -48f - bob * 10f else 0f
    val left = Path().apply { moveTo(56f, 112f); quadraticBezierTo(30f, 140f, 52f, 158f); quadraticBezierTo(58f, 132f, 56f, 112f); close() }
    val right = Path().apply { moveTo(154f, 112f); quadraticBezierTo(180f, 140f, 158f, 158f); quadraticBezierTo(152f, 132f, 154f, 112f); close() }
    rotate(wingUp, Offset(56f, 116f)) { drawPath(left, wingC) }
    rotate(-wingUp, Offset(154f, 116f)) { drawPath(right, wingC) }
    // gövde
    drawOval(Brush.verticalGradient(listOf(top, bottom), startY = 44f, endY = 180f), Offset(45f, 44f), Size(120f, 136f))
    drawOval(st.belly, Offset(67f, 96f), Size(76f, 80f))
    for (y in listOf(128f, 144f)) {
        val p = Path().apply {
            moveTo(84f, y)
            relativeQuadraticBezierTo(6f, 6f, 12f, 0f); relativeQuadraticBezierTo(6f, 6f, 12f, 0f); relativeQuadraticBezierTo(6f, 6f, 12f, 0f)
        }
        drawPath(p, top.copy(alpha = 0.55f), style = Stroke(3f, cap = StrokeCap.Round))
    }
    // ayaklar
    drawOval(Beak, Offset(76f, 174f), Size(24f, 12f))
    drawOval(Beak, Offset(110f, 174f), Size(24f, 12f))
    // maske bandı (gözlerin arkasında)
    if (st.eyes == OwlEyes.Mask) {
        drawRoundRect(Ink, Offset(50f, 76f), Size(110f, 36f), androidx.compose.ui.geometry.CornerRadius(18f, 18f))
    }
    if (st.hat == OwlHat.Headband) {
        drawRoundRect(st.accent, Offset(50f, 58f), Size(110f, 13f), androidx.compose.ui.geometry.CornerRadius(6f, 6f))
        drawPath(Path().apply { moveTo(158f, 62f); lineTo(184f, 54f); lineTo(180f, 68f); close() }, st.accent)
        drawPath(Path().apply { moveTo(158f, 66f); lineTo(182f, 78f); lineTo(172f, 84f); close() }, st.accent)
    }
    // gözler
    val (dx, dy) = when (mood) { MascotMood.Think -> 4f to -5f; MascotMood.Sad -> 0f to 4f; else -> 0f to 0f }
    for ((i, cx) in listOf(82f, 128f).withIndex()) {
        if (st.eyes == OwlEyes.Sun) {
            drawCircle(Ink, 23f, Offset(cx, 94f))
            drawLine(Color.White.copy(alpha = 0.55f), Offset(cx - 12f, 86f), Offset(cx - 2f, 80f), 4f, cap = StrokeCap.Round)
            continue
        }
        if (st.eyes == OwlEyes.Patch && i == 1) {
            drawCircle(Ink, 21f, Offset(cx, 94f))
            drawLine(Ink, Offset(60f, 70f), Offset(152f, 84f), 5f)
            continue
        }
        drawCircle(Color.White, if (st.eyes == OwlEyes.Mask) 17f else 22f, Offset(cx, 94f))
        if (mood == MascotMood.Wink && i == 1) {
            val p = Path().apply { moveTo(cx - 11f, 98f); quadraticBezierTo(cx, 86f, cx + 11f, 98f) }
            drawPath(p, Ink, style = Stroke(5f, cap = StrokeCap.Round))
        } else if (mood == MascotMood.Cheer) {
            val p = Path().apply { moveTo(cx - 11f, 98f); quadraticBezierTo(cx, 84f, cx + 11f, 98f) }
            drawPath(p, Ink, style = Stroke(5f, cap = StrokeCap.Round))
        } else {
            val px = cx + (if (i == 0) 2f else -2f) + dx
            drawCircle(Ink, if (st.eyes == OwlEyes.Mask) 9f else 11f, Offset(px, 96f + dy))
            drawCircle(Color.White, 4f, Offset(px + 4f, 91f + dy))
            if (mood == MascotMood.Sad) {
                // düşük göz kapağı
                val lid = Path().apply { moveTo(cx - 22f, 90f); quadraticBezierTo(cx, 76f + (if (i == 0) 6f else 0f), cx + 22f, 90f + (if (i == 0) -4f else 4f)); lineTo(cx + 22f, 70f); lineTo(cx - 22f, 70f); close() }
                drawPath(lid, top)
            }
        }
    }
    // gözlük
    when (st.eyes) {
        OwlEyes.Gold -> { drawCircle(Gold, 24f, Offset(82f, 94f), style = Stroke(5f)); drawCircle(Gold, 24f, Offset(128f, 94f), style = Stroke(5f)) }
        OwlEyes.Sun -> drawLine(Ink, Offset(100f, 90f), Offset(110f, 90f), 5f)
        else -> {}
    }
    // gaga
    drawPath(Path().apply { moveTo(97f, 112f); lineTo(113f, 112f); lineTo(105f, 126f); close() }, Beak)
    if (mood == MascotMood.Happy || mood == MascotMood.Cheer || mood == MascotMood.Wink) {
        drawOval(Color(0xFFFF7A9E).copy(alpha = 0.45f), Offset(52f, 116f), Size(16f, 9f))
        drawOval(Color(0xFFFF7A9E).copy(alpha = 0.45f), Offset(142f, 116f), Size(16f, 9f))
    }
    // aksesuar
    when (st.extra) {
        OwlExtra.Scarf -> {
            drawRoundRect(st.accent, Offset(58f, 124f), Size(94f, 15f), androidx.compose.ui.geometry.CornerRadius(7f, 7f))
            drawRoundRect(st.accent, Offset(122f, 132f), Size(15f, 32f), androidx.compose.ui.geometry.CornerRadius(5f, 5f))
            drawLine(Color.White.copy(alpha = 0.6f), Offset(124f, 156f), Offset(135f, 156f), 3f)
        }
        OwlExtra.Bowtie -> {
            drawPath(Path().apply { moveTo(105f, 136f); lineTo(88f, 127f); lineTo(88f, 145f); close() }, st.accent)
            drawPath(Path().apply { moveTo(105f, 136f); lineTo(122f, 127f); lineTo(122f, 145f); close() }, st.accent)
            drawCircle(Color.White, 4f, Offset(105f, 136f))
        }
        else -> {}
    }
    // şapka
    when (st.hat) {
        OwlHat.Grad -> {
            drawPath(Path().apply { moveTo(70f, 52f); quadraticBezierTo(105f, 34f, 140f, 52f); lineTo(132f, 60f); quadraticBezierTo(105f, 48f, 78f, 60f); close() }, Ink)
            rotate(45f, Offset(105f, 39f)) { drawRect(Ink, Offset(100f, 34f), Size(10f, 10f)) }
            drawLine(Gold, Offset(140f, 52f), Offset(150f, 74f), 3f)
            drawCircle(Gold, 4f, Offset(150f, 76f))
        }
        OwlHat.Party -> {
            drawPath(Path().apply { moveTo(82f, 56f); lineTo(128f, 56f); lineTo(105f, 6f); close() }, st.accent)
            drawLine(Color.White, Offset(92f, 40f), Offset(120f, 46f), 5f)
            drawLine(Gold, Offset(98f, 26f), Offset(113f, 30f), 5f)
            drawCircle(Gold, 7f, Offset(105f, 6f))
        }
        OwlHat.Crown -> {
            drawPath(Path().apply {
                moveTo(74f, 58f); lineTo(74f, 26f); lineTo(90f, 42f); lineTo(105f, 16f); lineTo(120f, 42f); lineTo(136f, 26f); lineTo(136f, 58f); close()
            }, Gold)
            drawRect(Color(0xFFE09A12), Offset(74f, 50f), Size(62f, 8f))
            drawCircle(st.accent, 5f, Offset(105f, 40f)); drawCircle(Color(0xFF45C4FF), 4f, Offset(86f, 48f)); drawCircle(Color(0xFF45C4FF), 4f, Offset(124f, 48f))
        }
        OwlHat.Wizard -> {
            drawPath(Path().apply { moveTo(70f, 56f); lineTo(140f, 56f); quadraticBezierTo(120f, 30f, 122f, 0f); quadraticBezierTo(96f, 24f, 70f, 56f); close() }, Color(0xFF2B1B6B))
            drawOval(Color(0xFF3A2590), Offset(54f, 48f), Size(102f, 18f))
            drawCircle(Gold, 4f, Offset(102f, 38f)); drawCircle(Gold, 3f, Offset(116f, 22f)); drawCircle(Gold, 2.5f, Offset(92f, 50f))
        }
        OwlHat.Beanie, OwlHat.Bandana -> {
            drawArc(st.accent, 180f, 180f, true, Offset(62f, 22f), Size(86f, 76f))
            if (st.hat == OwlHat.Beanie) {
                drawRoundRect(Color.White, Offset(58f, 52f), Size(94f, 13f), androidx.compose.ui.geometry.CornerRadius(6f, 6f))
                drawCircle(Color.White, 9f, Offset(105f, 20f))
            } else {
                drawCircle(Color.White, 3.5f, Offset(86f, 40f)); drawCircle(Color.White, 3.5f, Offset(105f, 32f)); drawCircle(Color.White, 3.5f, Offset(124f, 40f))
                drawPath(Path().apply { moveTo(148f, 56f); lineTo(172f, 50f); lineTo(166f, 66f); close() }, st.accent)
            }
        }
        OwlHat.Headphones -> {
            drawArc(Ink, 180f, 180f, false, Offset(48f, 26f), Size(114f, 130f), style = Stroke(9f, cap = StrokeCap.Round))
            drawRoundRect(st.accent, Offset(36f, 78f), Size(20f, 38f), androidx.compose.ui.geometry.CornerRadius(9f, 9f))
            drawRoundRect(st.accent, Offset(154f, 78f), Size(20f, 38f), androidx.compose.ui.geometry.CornerRadius(9f, 9f))
        }
        OwlHat.Helmet -> {
            drawCircle(Color.White.copy(alpha = 0.22f), 76f, Offset(105f, 92f))
            drawCircle(Color.White, 76f, Offset(105f, 92f), style = Stroke(6f))
            drawArc(Color.White.copy(alpha = 0.7f), 200f, 50f, false, Offset(43f, 30f), Size(124f, 124f), style = Stroke(6f, cap = StrokeCap.Round))
        }
        else -> {}
    }
    if (mood == MascotMood.Think) {
        drawCircle(Color.White, 5f, Offset(176f, 52f)); drawCircle(Color.White, 8f, Offset(188f, 34f))
    }
    if (mood == MascotMood.Cheer || st.sparkle) {
        val s = 0.6f + 0.4f * sin(bob * 3.14f)
        val pts = if (st.sparkle) listOf(22f to 46f, 190f to 34f, 30f to 150f, 184f to 140f) else listOf(24f to 40f, 186f to 30f)
        for ((n, pt) in pts.withIndex()) {
            val r = 5f * if (n % 2 == 0) s else (1.6f - s)
            drawCircle(Gold, r, Offset(pt.first, pt.second))
        }
    }
}

/** Maskot + konuşma balonu. */
@Composable
fun MascotSays(text: String, title: String? = null, mood: MascotMood = MascotMood.Happy, modifier: Modifier = Modifier, size: Dp = 84.dp, sky: Boolean = false) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        if (sky) SkyMascot(mood, size) else Mascot(mood, size)
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

/**
 * Günün saatine göre Sabah Bilgesi (güneşli) ya da Gece Bilgesi (ay ve yıldızlı) görünümü.
 * Uygulama simgelerindeki iki sahnenin uygulama içi karşılığı.
 */
@Composable
fun SkyMascot(mood: MascotMood = MascotMood.Happy, size: Dp = 84.dp, modifier: Modifier = Modifier) {
    val day = SkyDebug.force ?: androidx.compose.runtime.remember { java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY) in 6..17 }
    Box(modifier.size(size * 1.12f), contentAlignment = Alignment.BottomCenter) {
        Canvas(Modifier.size(size * 1.12f)) {
            val w = this.size.width
            val c = Offset(w / 2f, w / 2f)
            if (day) {
                drawCircle(Brush.linearGradient(listOf(Color(0xFFEDE4FF), Color(0xFFFFD9EC), Color(0xFFFFE9B8))), w / 2f, c)
                val sun = Offset(w * 0.80f, w * 0.20f)
                for (i in 0 until 10) {
                    val a = i * (2 * Math.PI / 10)
                    val dx = kotlin.math.cos(a).toFloat(); val dy = sin(a).toFloat()
                    drawLine(Color(0xFFFFC23D), Offset(sun.x + dx * w * 0.115f, sun.y + dy * w * 0.115f), Offset(sun.x + dx * w * 0.155f, sun.y + dy * w * 0.155f), w * 0.02f, cap = StrokeCap.Round)
                }
                drawCircle(Color(0xFFFFC23D), w * 0.085f, sun)
            } else {
                drawCircle(Brush.radialGradient(listOf(Color(0xFF3A2A7A), Color(0xFF140E2E)), center = Offset(w / 2f, w * 0.3f), radius = w * 0.9f), w / 2f, c)
                val moon = Offset(w * 0.78f, w * 0.20f)
                drawCircle(Color(0xFFFFE57A), w * 0.09f, moon)
                drawCircle(Color(0xFF2A1F5E), w * 0.075f, Offset(moon.x + w * 0.04f, moon.y - w * 0.025f))
                for ((x, y, r) in listOf(Triple(0.16f, 0.24f, 0.018f), Triple(0.10f, 0.62f, 0.014f), Triple(0.90f, 0.58f, 0.014f), Triple(0.34f, 0.10f, 0.012f))) {
                    drawCircle(Color(0xFFFFD34D), w * r, Offset(w * x, w * y))
                }
            }
        }
        Mascot(mood, size * 0.86f)
    }
}

/** Ekran görüntüsü testleri için gündüz/gece zorlaması (yalnızca debug). */
object SkyDebug { var force: Boolean? = null }
