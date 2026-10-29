package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.NeonBg
import com.example.ui.theme.NeonBgDeep
import com.example.ui.theme.NeonBorder
import com.example.ui.theme.NeonCard
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.NeonYellow
import com.example.ui.theme.OnNeon
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

/**
 * Neon ışıma: öğenin arkasına renkli, bulanık bir hale çizer.
 * (Android 9+ donanım hızlandırmada görünür; eski cihazlarda sessizce atlanır.)
 */
fun Modifier.neonGlow(
    color: Color,
    radius: Dp = 14.dp,
    cornerRadius: Dp = 18.dp,
    alpha: Float = 0.55f
): Modifier = this.drawBehind {
    if (alpha <= 0f) return@drawBehind
    drawIntoCanvas { canvas ->
        val paint = Paint()
        val fp = paint.asFrameworkPaint()
        fp.color = color.copy(alpha = 0f).toArgb()
        fp.setShadowLayer(radius.toPx(), 0f, 0f, color.copy(alpha = alpha.coerceIn(0f, 1f)).toArgb())
        canvas.drawRoundRect(
            0f, 0f, size.width, size.height,
            cornerRadius.toPx(), cornerRadius.toPx(), paint
        )
    }
}

/** Basılınca yaylanarak küçülen, bırakınca geri sıçrayan tıklama. */
fun Modifier.bounceClick(
    enabled: Boolean = true,
    pressedScale: Float = 0.94f,
    onClick: () -> Unit
): Modifier = composed {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) pressedScale else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "bounce"
    )
    this
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .clickable(interactionSource = interaction, indication = null, enabled = enabled, onClick = onClick)
}

/** [trigger] her değiştiğinde öğeyi yatay olarak sarsar (yanlış cevap efekti). */
fun Modifier.shake(trigger: Int, strength: Dp = 10.dp): Modifier = composed {
    val offset = remember { Animatable(0f) }
    LaunchedEffect(trigger) {
        if (trigger == 0) return@LaunchedEffect
        val s = strength.value
        for (v in listOf(s, -s, s * 0.7f, -s * 0.7f, s * 0.35f, 0f)) {
            offset.animateTo(v, tween(45))
        }
    }
    this.graphicsLayer { translationX = offset.value * density }
}

/** [trigger] değişince kısa bir büyüyüp küçülme (doğru cevap efekti). */
fun Modifier.pop(trigger: Int, amount: Float = 0.12f): Modifier = composed {
    val s = remember { Animatable(1f) }
    LaunchedEffect(trigger) {
        if (trigger == 0) return@LaunchedEffect
        s.animateTo(1f + amount, tween(110))
        s.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
    }
    this.graphicsLayer { scaleX = s.value; scaleY = s.value }
}

/** Yarı saydam cam kart, ince neon kenarlık ve isteğe bağlı ışıma. */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    accent: Color = NeonBorder,
    glow: Boolean = false,
    shape: Shape = RoundedCornerShape(20.dp),
    contentPadding: PaddingValues = PaddingValues(16.dp),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val base = modifier
        .then(if (glow) Modifier.neonGlow(accent, radius = 16.dp, alpha = 0.45f) else Modifier)
        .clip(shape)
        .background(
            Brush.verticalGradient(listOf(NeonCard.copy(alpha = 0.92f), NeonCard.copy(alpha = 0.72f)))
        )
        .border(1.dp, Brush.linearGradient(listOf(accent.copy(alpha = 0.85f), accent.copy(alpha = 0.18f))), shape)
        .then(if (onClick != null) Modifier.bounceClick(pressedScale = 0.97f, onClick = onClick) else Modifier)
        .padding(contentPadding)
    Column(modifier = base, content = content)
}

/** Neon dolgulu ana buton. */
@Composable
fun NeonButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = NeonCyan,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    outlined: Boolean = false,
    height: Dp = 52.dp
) {
    val shape = RoundedCornerShape(16.dp)
    val bg = when {
        !enabled -> Modifier.background(NeonCard)
        outlined -> Modifier.background(color.copy(alpha = 0.08f))
        else -> Modifier.background(Brush.horizontalGradient(listOf(color, color.copy(alpha = 0.78f))))
    }
    val fg = when {
        !enabled -> TextMuted
        outlined -> color
        else -> OnNeon
    }
    Box(
        modifier = modifier
            .height(height)
            .then(if (enabled && !outlined) Modifier.neonGlow(color, radius = 14.dp, cornerRadius = 16.dp, alpha = 0.5f) else Modifier)
            .clip(shape)
            .then(bg)
            .border(1.dp, if (enabled) color.copy(alpha = if (outlined) 0.8f else 0.4f) else NeonBorder, shape)
            .bounceClick(enabled = enabled, onClick = onClick)
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(text, color = fg, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge, maxLines = 1)
        }
    }
}

/** Elmas sayacı (dokununca mağaza açılır). */
@Composable
fun DiamondPill(count: Int, onClick: (() -> Unit)?, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(50)
    Row(
        modifier = modifier
            .neonGlow(NeonCyan, radius = 10.dp, cornerRadius = 50.dp, alpha = 0.35f)
            .clip(shape)
            .background(NeonCard)
            .border(1.dp, NeonCyan.copy(alpha = 0.6f), shape)
            .then(if (onClick != null) Modifier.bounceClick(onClick = onClick) else Modifier)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("💎", fontSize = 15.sp)
        Spacer(Modifier.width(5.dp))
        AnimatedCounter(value = count, style = MaterialTheme.typography.labelLarge, color = NeonCyan)
        if (onClick != null) {
            Spacer(Modifier.width(6.dp))
            Box(
                Modifier.size(18.dp).clip(CircleShape).background(NeonCyan),
                contentAlignment = Alignment.Center
            ) { Text("+", color = OnNeon, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp) }
        }
    }
}

/** Değer değişince sayarak artan/azalan sayı. */
@Composable
fun AnimatedCounter(
    value: Int,
    modifier: Modifier = Modifier,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.titleMedium,
    color: Color = TextPrimary,
    prefix: String = "",
    suffix: String = ""
) {
    val anim = remember { Animatable(value.toFloat()) }
    LaunchedEffect(value) { anim.animateTo(value.toFloat(), tween(600)) }
    Text(
        text = "$prefix${anim.value.toInt()}$suffix",
        modifier = modifier,
        style = style,
        color = color,
        fontWeight = FontWeight.Bold
    )
}

/** Küçük yıldız satırı (0..3). */
@Composable
fun StarRow(stars: Int, modifier: Modifier = Modifier, size: Dp = 14.dp, max: Int = 3) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(1.dp)) {
        repeat(max) { i ->
            Text(
                if (i < stars) "★" else "☆",
                color = if (i < stars) NeonYellow else TextMuted,
                fontSize = (size.value).sp,
                lineHeight = (size.value + 2).sp
            )
        }
    }
}

/** Ekran üst başlığı (geri butonlu). */
@Composable
fun NeonTopBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri", tint = TextPrimary)
            }
        } else Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleLarge, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        if (trailing != null) {
            trailing()
            Spacer(Modifier.width(8.dp))
        }
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(width = 4.dp, height = 18.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Brush.verticalGradient(listOf(NeonCyan, NeonPurple)))
        )
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.titleMedium, color = TextPrimary, modifier = Modifier.weight(1f))
        if (action != null && onAction != null) {
            Text(
                action, color = NeonCyan, style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onAction).padding(6.dp)
            )
        }
    }
}

/** Renkli küçük etiket. */
@Composable
fun NeonChip(text: String, color: Color, modifier: Modifier = Modifier, filled: Boolean = false) {
    val shape = RoundedCornerShape(50)
    Text(
        text = text,
        color = if (filled) OnNeon else color,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        maxLines = 1,
        modifier = modifier
            .clip(shape)
            .background(if (filled) color else color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.55f), shape)
            .padding(horizontal = 9.dp, vertical = 3.dp)
    )
}

/** İnce neon ilerleme çubuğu. */
@Composable
fun NeonProgressBar(progress: Float, color: Color, modifier: Modifier = Modifier, height: Dp = 6.dp) {
    val p by animateFloatAsState(progress.coerceIn(0f, 1f), tween(700), label = "progress")
    Box(
        modifier
            .height(height)
            .clip(RoundedCornerShape(50))
            .background(NeonBorder.copy(alpha = 0.6f))
    ) {
        Box(
            Modifier
                .fillMaxWidth(p)
                .height(height)
                .neonGlow(color, radius = 6.dp, cornerRadius = 50.dp, alpha = 0.7f)
                .clip(RoundedCornerShape(50))
                .background(Brush.horizontalGradient(listOf(color.copy(alpha = 0.7f), color)))
        )
    }
}

/**
 * Hareketli neon arka plan: yavaşça kayan ızgara ve iki renkli ışık lekesi.
 */
@Composable
fun NeonBackground(
    modifier: Modifier = Modifier,
    accent: Color = NeonPurple,
    accent2: Color = NeonCyan,
    showGrid: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    val t = rememberInfiniteTransition(label = "bg")
    val shift by t.animateFloat(0f, 1f, infiniteRepeatable(tween(14000, easing = LinearEasing), RepeatMode.Restart), label = "shift")
    val drift by t.animateFloat(0f, 1f, infiniteRepeatable(tween(9000), RepeatMode.Reverse), label = "drift")
    Box(
        modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(NeonBg, NeonBgDeep)))
            .drawBehind {
                val w = size.width
                val h = size.height
                drawCircle(
                    Brush.radialGradient(
                        listOf(accent.copy(alpha = 0.22f), Color.Transparent),
                        center = Offset(w * (0.15f + 0.2f * drift), h * 0.12f),
                        radius = w * 0.75f
                    ),
                    radius = w * 0.75f,
                    center = Offset(w * (0.15f + 0.2f * drift), h * 0.12f)
                )
                drawCircle(
                    Brush.radialGradient(
                        listOf(accent2.copy(alpha = 0.14f), Color.Transparent),
                        center = Offset(w * (0.9f - 0.2f * drift), h * 0.85f),
                        radius = w * 0.8f
                    ),
                    radius = w * 0.8f,
                    center = Offset(w * (0.9f - 0.2f * drift), h * 0.85f)
                )
                if (showGrid) {
                    val step = 42.dp.toPx()
                    val off = shift * step
                    val lineColor = accent2.copy(alpha = 0.045f)
                    var y = -step + off
                    while (y < h) {
                        drawLine(lineColor, Offset(0f, y), Offset(w, y), strokeWidth = 1f)
                        y += step
                    }
                    var x = 0f
                    while (x < w) {
                        drawLine(lineColor, Offset(x, 0f), Offset(x, h), strokeWidth = 1f)
                        x += step
                    }
                }
            },
        content = content
    )
}

/** Kilit veya boş durum mesajı kutusu. */
@Composable
fun InfoBanner(text: String, color: Color = NeonCyan, emoji: String = "💡", modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(color.copy(alpha = 0.08f))
            .border(1.dp, color.copy(alpha = 0.35f), shape)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(emoji, fontSize = 18.sp)
        Spacer(Modifier.width(10.dp))
        Text(text, color = TextPrimary, style = MaterialTheme.typography.bodySmall)
    }
}

/** Neon ikon kutusu (emoji ile). */
@Composable
fun EmojiBadge(emoji: String, color: Color, size: Dp = 48.dp, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(size * 0.32f)
    Box(
        modifier
            .size(size)
            .neonGlow(color, radius = 10.dp, cornerRadius = size * 0.32f, alpha = 0.35f)
            .clip(shape)
            .background(Brush.linearGradient(listOf(color.copy(alpha = 0.28f), color.copy(alpha = 0.08f))))
            .border(1.dp, color.copy(alpha = 0.7f), shape),
        contentAlignment = Alignment.Center
    ) {
        Text(emoji, fontSize = (size.value * 0.48f).sp)
    }
}

/** Kenarları yumuşak geçişli dikey boşluk. */
@Composable
fun VSpace(h: Dp) = Spacer(Modifier.height(h))

@Composable
fun HSpace(w: Dp) = Spacer(Modifier.width(w))

/** Cam kart içinde kullanılan kart arka planı (CornerRadius yardımcı). */
internal fun cr(px: Float) = CornerRadius(px, px)
