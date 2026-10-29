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

    NeonBackground(accent = accent) {
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
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(LevelRules.ZONE_EMOJI[zone], fontSize = 18.sp)
            Spacer(Modifier.width(8.dp))
            Text(
                "${LevelRules.ZONE_NAMES[zone]}  ·  $first–${first + 9}",
                style = MaterialTheme.typography.titleMedium,
                color = if (unlocked) TextPrimary else TextMuted,
                modifier = Modifier.weight(1f)
            )
            Text("⭐ $zoneStars/30", color = if (unlocked) com.example.ui.theme.NeonYellow else TextMuted, style = MaterialTheme.typography.labelLarge)
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

/**
 * 10 seviyeyi kıvrımlı bir yol üzerinde (5'li iki sıra, yılan düzeni) gösterir.
 */
@Composable
private fun LevelPath(first: Int, progress: GameProgress, accent: Color, next: Int, onPlay: (Int) -> Unit) {
    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) { appear.animateTo(1f, tween(700)) }
    BoxWithConstraints(Modifier.fillMaxWidth().height(210.dp)) {
        val w = constraints.maxWidth.toFloat()
        val h = constraints.maxHeight.toFloat()
        val cols = 5
        val node = 58.dp
        val density = androidx.compose.ui.platform.LocalDensity.current
        val nodePx = with(density) { node.toPx() }
        fun center(i: Int): Offset {
            val row = i / cols
            val colRaw = i % cols
            val col = if (row % 2 == 0) colRaw else cols - 1 - colRaw
            val x = nodePx / 2 + col * (w - nodePx) / (cols - 1)
            val y = if (row == 0) h * 0.25f else h * 0.72f
            val wobble = if (col % 2 == 0) -8f else 8f
            return Offset(x, y + wobble * density.density)
        }
        Canvas(Modifier.fillMaxSize()) {
            for (i in 0 until 9) {
                val a = center(i)
                val b = center(i + 1)
                val done = progress.stars(first + i) > 0
                drawLine(
                    color = if (done) accent.copy(alpha = 0.8f) else NeonBorder,
                    start = a, end = b,
                    strokeWidth = if (done) 5f else 3f,
                    pathEffect = if (done) null else PathEffect.dashPathEffect(floatArrayOf(14f, 12f))
                )
            }
        }
        for (i in 0 until 10) {
            val level = first + i
            val c = center(i)
            val delayFrac = i / 12f
            val local = ((appear.value - delayFrac) / 0.3f).coerceIn(0f, 1f)
            Box(
                Modifier
                    .offset { IntOffset((c.x - nodePx / 2).roundToInt(), (c.y - nodePx / 2).roundToInt()) }
                    .graphicsLayer { scaleX = local; scaleY = local; alpha = local }
            ) {
                LevelNode(level, progress, accent, isCurrent = level == next, size = node) { onPlay(level) }
            }
        }
    }
}

@Composable
private fun LevelNode(level: Int, progress: GameProgress, accent: Color, isCurrent: Boolean, size: androidx.compose.ui.unit.Dp, onClick: () -> Unit) {
    val unlocked = progress.isUnlocked(level)
    val stars = progress.stars(level)
    val boss = LevelRules.isBoss(level)
    val pulse = if (isCurrent && unlocked) {
        val t = rememberInfiniteTransition(label = "pulse$level")
        t.animateFloat(1f, 1.12f, infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "p").value
    } else 1f
    val ringColor = when {
        !unlocked -> NeonBorder
        boss -> NeonPink
        stars > 0 -> accent
        else -> NeonCyan
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(size)
                .graphicsLayer { scaleX = pulse; scaleY = pulse }
                .then(if (unlocked) Modifier.neonGlow(ringColor, 12.dp, size, if (isCurrent) 0.9f else 0.45f) else Modifier)
                .clip(CircleShape)
                .background(
                    if (stars > 0) Brush.radialGradient(listOf(ringColor.copy(alpha = 0.55f), NeonCard))
                    else Brush.radialGradient(listOf(NeonCard, NeonCard))
                )
                .border(if (isCurrent) 3.dp else 2.dp, ringColor, CircleShape)
                .then(if (unlocked) Modifier.bounceClick(onClick = onClick) else Modifier),
            contentAlignment = Alignment.Center
        ) {
            when {
                !unlocked -> Text("🔒", fontSize = 18.sp)
                else -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (boss) Text("👑", fontSize = 12.sp, lineHeight = 12.sp)
                    Text(
                        "$level",
                        color = if (stars > 0) TextPrimary else NeonCyan,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = if (boss) 16.sp else 19.sp
                    )
                }
            }
        }
        Spacer(Modifier.height(2.dp))
        if (unlocked) StarRow(stars, size = 11.dp) else Spacer(Modifier.height(13.dp))
    }
}
