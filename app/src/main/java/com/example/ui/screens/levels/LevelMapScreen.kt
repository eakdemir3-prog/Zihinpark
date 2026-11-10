package com.example.ui.screens.levels

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.GameRepository
import com.example.domain.model.GameMeta
import com.example.domain.model.GameRegistry
import com.example.game.engine.GameProgress
import com.example.game.engine.LevelRules
import com.example.ui.components.EmojiBadge
import com.example.ui.components.GlassCard
import com.example.ui.components.InfoBanner
import com.example.ui.components.NeonBackground
import com.example.ui.components.NeonButton
import com.example.ui.components.NeonChip
import com.example.ui.components.NeonProgressBar
import com.example.ui.components.NeonTopBar
import com.example.ui.components.StarRow
import com.example.ui.components.bounceClick
import com.example.ui.components.neonGlow
import com.example.ui.theme.NeonBorder
import com.example.ui.theme.NeonCard
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonOrange
import com.example.ui.theme.NeonPink
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import androidx.compose.ui.graphics.lerp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.NeonYellow
import kotlin.math.roundToInt

@Composable
fun LevelMapScreen(
    gameId: String,
    repository: GameRepository,
    onBack: () -> Unit,
    onPlay: (level: Int) -> Unit
) {
    val meta = remember(gameId) { GameRegistry.getGameById(gameId) } ?: run {
        LaunchedEffect(Unit) { onBack() }
        return
    }
    val progress by repository.progressFor(gameId).collectAsState(initial = GameProgress(emptyMap()))
    val accent = meta.category.color
    var showHowTo by remember { mutableStateOf(false) }
    val next = progress.nextPlayableLevel
    LaunchedEffect(gameId, next) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
            com.example.game.puzzles.AllGenerators.byId(gameId)?.let { com.example.game.engine.PuzzleBank.prefetch(it, next) }
        }
    }

    NeonBackground(accent = accent, pattern = com.example.ui.components.BgPatterns.forGame(gameId)) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            NeonTopBar(
                title = meta.name,
                subtitle = "⭐ ${progress.totalStars} / ${LevelRules.LEVEL_COUNT * 3}  ·  ${progress.clearedCount}/${LevelRules.LEVEL_COUNT} seviye",
                onBack = onBack
            )
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { HeroCard(meta, progress, next, accent, showHowTo, { showHowTo = !showHowTo }) { onPlay(next) } }
                item { EndlessCard(progress) { onPlay(LevelRules.ENDLESS_LEVEL) } }
                for (zone in 0 until LevelRules.ZONE_COUNT) {
                    item(key = "zone$zone") {
                        ZoneSection(zone, progress, accent, next, onPlay)
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroCard(
    meta: GameMeta,
    progress: GameProgress,
    next: Int,
    accent: Color,
    showHowTo: Boolean,
    toggleHowTo: () -> Unit,
    onContinue: () -> Unit
) {
    GlassCard(accent = accent, glow = true) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            EmojiBadge(meta.iconEmoji, accent, size = 64.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                NeonChip(meta.category.title, accent)
                Spacer(Modifier.height(6.dp))
                Text(meta.description, color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
            }
        }
        Spacer(Modifier.height(14.dp))
        NeonProgressBar(progress.clearedCount / LevelRules.LEVEL_COUNT.toFloat(), accent, Modifier.fillMaxWidth())
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            NeonButton(
                if (progress.clearedCount == 0) "Başla · Seviye 1" else "Devam · Seviye $next",
                onContinue, color = accent, icon = Icons.Default.PlayArrow, modifier = Modifier.weight(1f)
            )
            NeonButton(if (showHowTo) "Kapat" else "Nasıl?", toggleHowTo, color = NeonCyan, outlined = true)
        }
        AnimatedVisibility(showHowTo, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            Column(Modifier.padding(top = 12.dp)) {
                meta.howTo.forEachIndexed { i, s ->
                    Text("${i + 1}. $s", color = TextSecondary, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 3.dp))
                }
                Text(
                    "Seviye 1-10: 3 tur · 11-30: 4 tur · 31-50: 5 tur. Her 10. seviye boss! Yeni bölge için önceki bölgede ${LevelRules.ZONE_UNLOCK_STARS}⭐ gerekir.",
                    color = TextMuted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun EndlessCard(progress: GameProgress, onPlay: () -> Unit) {
    val unlocked = progress.isEndlessUnlocked
    GlassCard(
        accent = if (unlocked) NeonPink else NeonBorder,
        glow = unlocked,
        onClick = if (unlocked) onPlay else null
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(if (unlocked) "♾️" else "🔒", fontSize = 28.sp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Sonsuz Mod", style = MaterialTheme.typography.titleMedium, color = if (unlocked) TextPrimary else TextMuted)
                Text(
                    if (unlocked) "Rekorun: ${progress.endlessBest} tur · Her tur yeni, hiç tekrar yok"
                    else "50. seviyeyi geçince açılır",
                    style = MaterialTheme.typography.bodySmall, color = TextSecondary
                )
            }
            if (unlocked) NeonChip("OYNA", NeonPink, filled = true)
        }
    }
}

@Composable
private fun ZoneSection(zone: Int, progress: GameProgress, accent: Color, next: Int, onPlay: (Int) -> Unit) {
    val unlocked = progress.isZoneUnlocked(zone)
    val first = LevelRules.zoneFirstLevel(zone)
    val zoneStars = progress.zoneStars(zone)
    val W = Color.White
    Column {
        val shape = RoundedCornerShape(22.dp)
        Box(
            Modifier.fillMaxWidth().clip(shape)
                .background(if (unlocked) Brush.linearGradient(listOf(accent, NeonPurple, NeonCyan)) else Brush.linearGradient(listOf(NeonBorder, NeonBorder.copy(alpha = 0.6f))))
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Column(Modifier.padding(end = 56.dp)) {
                Text("BÖLGE ${zone + 1} · ${LevelRules.ZONE_NAMES[zone].uppercase()}", color = if (unlocked) W.copy(alpha = 0.9f) else TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
                Text("Seviye $first–${first + 9}", color = if (unlocked) W else TextSecondary, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.height(8.dp))
                Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(W.copy(alpha = 0.3f))) {
                    Box(Modifier.fillMaxWidth((zoneStars / 30f).coerceIn(0.02f, 1f)).height(6.dp).clip(RoundedCornerShape(3.dp)).background(W))
                }
                Text("⭐ $zoneStars / 30 · Boss: Seviye ${first + 9}", color = if (unlocked) W.copy(alpha = 0.9f) else TextSecondary, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp), maxLines = 1)
            }
            Box(
                Modifier.align(Alignment.CenterEnd).size(48.dp).clip(RoundedCornerShape(14.dp)).background(W.copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center
            ) { Text(if (unlocked) LevelRules.ZONE_EMOJI[zone] else "🔒", fontSize = 24.sp) }
        }
        Spacer(Modifier.height(10.dp))
        if (!unlocked && zone > 0) {
            val prevBoss = first - 1
            val prevStars = progress.zoneStars(zone - 1)
            val msg = when {
                !progress.isZoneUnlocked(zone - 1) -> "Önce ${LevelRules.ZONE_NAMES[zone - 1]} bölgesini aç."
                progress.stars(prevBoss) == 0 -> "Seviye $prevBoss boss'unu geç ve ${LevelRules.ZONE_NAMES[zone - 1]} bölgesinde ${LevelRules.ZONE_UNLOCK_STARS}⭐ topla (şu an $prevStars⭐)."
                else -> "${LevelRules.ZONE_NAMES[zone - 1]} bölgesinde ${LevelRules.ZONE_UNLOCK_STARS}⭐ topla (şu an $prevStars⭐). Eski seviyeleri tekrar oynayıp yıldızlarını artır."
            }
            InfoBanner(msg, color = NeonOrange, emoji = "🔒")
            Spacer(Modifier.height(10.dp))
        }
        LevelPath(first, progress, accent, next, onPlay)
    }
}

private val PATH_X = floatArrayOf(0.5f, 0.72f, 0.8f, 0.64f, 0.4f, 0.22f, 0.2f, 0.36f, 0.6f, 0.76f)

/** 10 seviyeyi yukarıdan aşağı zikzak bir yol üzerinde gösterir. */
@Composable
private fun LevelPath(first: Int, progress: GameProgress, accent: Color, next: Int, onPlay: (Int) -> Unit) {
    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) { appear.animateTo(1f, tween(700)) }
    val rowH = 86.dp
    BoxWithConstraints(Modifier.fillMaxWidth().height(rowH * 10 + 24.dp)) {
        val w = constraints.maxWidth.toFloat()
        val density = androidx.compose.ui.platform.LocalDensity.current
        val rowPx = with(density) { rowH.toPx() }
        val top = with(density) { 24.dp.toPx() }
        fun center(i: Int) = Offset(w * PATH_X[i], top + rowPx * i + rowPx / 2)
        val dotColor = NeonBorder
        Canvas(Modifier.fillMaxSize()) {
            val p = androidx.compose.ui.graphics.Path()
            p.moveTo(center(0).x, center(0).y)
            for (i in 1 until 10) {
                val a = center(i - 1); val b = center(i)
                p.cubicTo(a.x, (a.y + b.y) / 2, b.x, (a.y + b.y) / 2, b.x, b.y)
            }
            drawPath(p, dotColor, style = androidx.compose.ui.graphics.drawscope.Stroke(9.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(1f, 18.dp.toPx()))))
        }
        for (i in 0 until 10) {
            val level = first + i
            val c = center(i)
            val current = level == next && progress.isUnlocked(level)
            val node = if (current) 78.dp else 62.dp
            val nodePx = with(density) { node.toPx() }
            val local = ((appear.value - i / 12f) / 0.3f).coerceIn(0f, 1f)
            Box(
                Modifier.offset { IntOffset((c.x - nodePx / 2).roundToInt(), (c.y - nodePx / 2).roundToInt()) }
                    .graphicsLayer { scaleX = local; scaleY = local; alpha = local }
            ) { PathNode(level, progress, accent, current, node) { onPlay(level) } }
            if (current) {
                val bubbleW = with(density) { 84.dp.toPx() }
                Text(
                    "BAŞLA ▾", color = Color(0xFF5A3A00), fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, maxLines = 1,
                    modifier = Modifier.offset { IntOffset((c.x - bubbleW / 2).roundToInt(), (c.y - nodePx / 2 - with(density) { 34.dp.toPx() }).roundToInt()) }
                        .width(84.dp).clip(RoundedCornerShape(12.dp)).background(NeonYellow).padding(vertical = 5.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                val mPx = with(density) { 86.dp.toPx() }
                val mx = if (PATH_X[i] > 0.5f) c.x - nodePx / 2 - mPx - with(density) { 8.dp.toPx() } else c.x + nodePx / 2 + with(density) { 8.dp.toPx() }
                com.example.ui.components.Mascot(
                    com.example.ui.components.MascotMood.Happy, 86.dp,
                    Modifier.offset { IntOffset(mx.roundToInt(), (c.y - mPx / 2).roundToInt()) }
                )
            }
        }
    }
}

@Composable
private fun PathNode(level: Int, progress: GameProgress, accent: Color, isCurrent: Boolean, size: androidx.compose.ui.unit.Dp, onClick: () -> Unit) {
    val unlocked = progress.isUnlocked(level)
    val stars = progress.stars(level)
    val boss = LevelRules.isBoss(level)
    val pulse = if (isCurrent) {
        val t = rememberInfiniteTransition(label = "pulse$level")
        t.animateFloat(1f, 1.07f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "p").value
    } else 1f
    val (top, bottom) = when {
        !unlocked && boss -> Color(0xFFFFB3C6) to Color(0xFFE88AA6)
        !unlocked -> NeonBorder.copy(alpha = 0.55f) to NeonBorder
        boss -> Color(0xFFFF8AA8) to Color(0xFFE0406A)
        isCurrent -> accent to NeonPurple
        stars > 0 -> Color(0xFF5FD98A) to Color(0xFF2FAE5F)
        else -> accent.copy(alpha = 0.7f) to accent
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(size).graphicsLayer { scaleX = pulse; scaleY = pulse }) {
            // 3B gölge
            Box(Modifier.matchParentSize().offset(y = 5.dp).clip(CircleShape).background(lerp(bottom, Color.Black, 0.25f)))
            Box(
                Modifier.matchParentSize().clip(CircleShape)
                    .background(Brush.verticalGradient(listOf(top, bottom)))
                    .then(if (isCurrent) Modifier.border(4.dp, Color.White.copy(alpha = 0.7f), CircleShape) else Modifier)
                    .then(if (unlocked) Modifier.bounceClick(onClick = onClick) else Modifier),
                contentAlignment = Alignment.Center
            ) {
                when {
                    boss -> Text(if (unlocked) "👑" else "🔒", fontSize = if (isCurrent) 28.sp else 24.sp)
                    !unlocked -> Text("🔒", fontSize = 18.sp)
                    else -> Text("$level", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = if (isCurrent) 26.sp else 21.sp)
                }
            }
        }
        Spacer(Modifier.height(7.dp))
        if (unlocked && stars > 0) StarRow(stars, size = 11.dp)
    }
}
