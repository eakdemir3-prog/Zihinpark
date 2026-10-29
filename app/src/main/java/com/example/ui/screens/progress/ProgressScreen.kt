package com.example.ui.screens.progress

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.UserDailyActivityEntity
import com.example.data.repository.GameRepository
import com.example.domain.model.GameCategory
import com.example.domain.model.GameRegistry
import com.example.game.engine.GameProgress
import com.example.game.engine.LevelRules
import com.example.ui.components.EmojiBadge
import com.example.ui.components.GlassCard
import com.example.ui.components.NeonButton
import com.example.ui.components.NeonProgressBar
import com.example.ui.components.NeonTopBar
import com.example.ui.components.SectionTitle
import com.example.ui.components.bounceClick
import com.example.ui.components.neonGlow
import com.example.ui.screens.profile.SegmentedTabs
import com.example.ui.theme.NeonBorder
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonOrange
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.NeonYellow
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/** Son 7 günün (bugün dahil) verisi; oynanmayan günler 0. */
data class DayStat(val label: String, val isToday: Boolean, val seconds: Long, val games: Int, val score: Int, val stars: Int)

fun lastSevenDays(rows: List<UserDailyActivityEntity>): List<DayStat> {
    val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val dayNames = listOf("Paz", "Pzt", "Sal", "Çar", "Per", "Cum", "Cmt")
    val byDate = rows.associateBy { it.date }
    return (6 downTo 0).map { back ->
        val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -back) }
        val key = fmt.format(cal.time)
        val r = byDate[key]
        DayStat(
            label = if (back == 0) "Bugün" else dayNames[cal.get(Calendar.DAY_OF_WEEK) - 1],
            isToday = back == 0,
            seconds = r?.totalPlayTimeSeconds ?: 0,
            games = r?.gamesPlayedCount ?: 0,
            score = r?.totalScoreEarned ?: 0,
            stars = r?.starsEarned ?: 0
        )
    }
}

fun formatDuration(sec: Long): String = when {
    sec >= 3600 -> "${sec / 3600} sa ${(sec % 3600) / 60} dk"
    sec >= 60 -> "${sec / 60} dk"
    else -> "$sec sn"
}

@Composable
fun ProgressScreen(
    repository: GameRepository,
    onNavigateToGames: () -> Unit,
    onNavigateToWeeklyTrends: () -> Unit = {}
) {
    val progress by repository.allProgress.collectAsState(initial = emptyMap())
    val activity by repository.allDailyActivities.collectAsState(initial = emptyList())
    val totalTime by repository.totalPlayTimeSeconds.collectAsState(initial = 0L)
    val days = remember(activity) { lastSevenDays(activity) }
    val totalStars = progress.values.sumOf { it.totalStars }
    val cleared = progress.values.sumOf { it.clearedCount }
    val totalLevels = GameRegistry.ALL_GAMES.size * LevelRules.LEVEL_COUNT

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Tile("⭐", "$totalStars", "Yıldız", NeonYellow, Modifier.weight(1f))
                Tile("🏁", "$cleared", "/$totalLevels seviye", NeonGreen, Modifier.weight(1f))
                Tile("⏱️", formatDuration(totalTime ?: 0), "Toplam süre", NeonCyan, Modifier.weight(1f))
            }
        }
        item {
            WeeklyChartCard(days, compact = true, onMore = onNavigateToWeeklyTrends)
        }
        item { SectionTitle("Zekâ alanları") }
        items(GameCategory.entries.toList(), key = { it.name }) { cat ->
            val games = GameRegistry.ALL_GAMES.filter { it.category == cat }
            val stars = games.sumOf { progress[it.id]?.totalStars ?: 0 }
            val max = games.size * LevelRules.LEVEL_COUNT * 3
            GlassCard(accent = cat.color, contentPadding = PaddingValues(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    EmojiBadge(cat.emoji, cat.color, size = 42.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Row {
                            Text(cat.title, color = TextPrimary, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                            Text("★ $stars / $max", color = NeonYellow, style = MaterialTheme.typography.labelMedium)
                        }
                        Spacer(Modifier.height(6.dp))
                        NeonProgressBar(stars.toFloat() / max, cat.color, Modifier.fillMaxWidth())
                        Spacer(Modifier.height(4.dp))
                        Text("${games.size} oyun · ${games.sumOf { progress[it.id]?.clearedCount ?: 0 }} seviye geçildi", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
        item { SectionTitle("Oyunlar") }
        items(GameRegistry.ALL_GAMES.sortedByDescending { progress[it.id]?.totalStars ?: 0 }, key = { it.id }) { g ->
            val p = progress[g.id] ?: GameProgress(emptyMap())
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color(0x221A1A2E)).padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(g.iconEmoji, fontSize = 22.sp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(g.name, color = TextPrimary, style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(4.dp))
                    NeonProgressBar(p.clearedCount / 50f, g.category.color, Modifier.fillMaxWidth(), height = 4.dp)
                }
                Spacer(Modifier.width(10.dp))
                Text("${p.clearedCount}/50", color = TextSecondary, style = MaterialTheme.typography.labelMedium, modifier = Modifier.width(44.dp), textAlign = TextAlign.End)
            }
        }
        item {
            NeonButton("Oyunlara git", onNavigateToGames, color = NeonCyan, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun Tile(emoji: String, value: String, label: String, color: Color, modifier: Modifier) {
    GlassCard(accent = color, modifier = modifier, contentPadding = PaddingValues(12.dp)) {
        Text(emoji, fontSize = 18.sp)
        Spacer(Modifier.height(4.dp))
        Text(value, color = TextPrimary, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium, maxLines = 1)
        Text(label, color = TextSecondary, style = MaterialTheme.typography.labelSmall, maxLines = 1)
    }
}

/** Haftalık çubuk grafiği: Süre / Puan / Oyun. */
@Composable
fun WeeklyChartCard(days: List<DayStat>, compact: Boolean, onMore: (() -> Unit)? = null) {
    var metric by rememberSaveable { mutableIntStateOf(0) }
    val values = days.map {
        when (metric) { 0 -> it.seconds.toFloat(); 1 -> it.score.toFloat(); else -> it.games.toFloat() }
    }
    val maxV = values.maxOrNull()?.coerceAtLeast(1f) ?: 1f
    val colors = listOf(NeonCyan, NeonPink, NeonPurple)
    val color = colors[metric]
    fun fmt(v: Float) = when (metric) { 0 -> formatDuration(v.toLong()); 1 -> "${v.toInt()}"; else -> "${v.toInt()} oyun" }
    val anim = remember { Animatable(0f) }
    LaunchedEffect(metric, days) { anim.snapTo(0f); anim.animateTo(1f, tween(700)) }

    GlassCard(accent = color) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Bu hafta", color = TextPrimary, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            if (onMore != null) Text("Ayrıntı ›", color = NeonCyan, style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).bounceClick(onClick = onMore).padding(4.dp))
        }
        Spacer(Modifier.height(10.dp))
        SegmentedTabs(listOf("Süre", "Puan", "Oyun"), metric) { metric = it }
        Spacer(Modifier.height(14.dp))
        val today = days.last()
        Text(
            "Bugün: ${fmt(values.last())}" + if (metric != 2) " · ${today.games} oyun · ⭐ ${today.stars}" else " · ⭐ ${today.stars}",
            color = TextSecondary, style = MaterialTheme.typography.bodySmall
        )
        Spacer(Modifier.height(10.dp))
        Row(
            Modifier.fillMaxWidth().height(if (compact) 140.dp else 220.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            days.forEachIndexed { i, d ->
                val frac = (values[i] / maxV).coerceIn(0f, 1f) * anim.value
                Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                    if (values[i] > 0 && !compact) Text(fmt(values[i]), color = TextSecondary, fontSize = 9.sp, maxLines = 1)
                    Box(Modifier.weight(1f, fill = false).fillMaxWidth().fillMaxHeight(0.88f), contentAlignment = Alignment.BottomCenter) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(frac.coerceAtLeast(0.03f))
                                .then(if (d.isToday) Modifier.neonGlow(color, 10.dp, 10.dp, 0.6f) else Modifier)
                                .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp, bottomStart = 4.dp, bottomEnd = 4.dp))
                                .background(
                                    if (values[i] > 0) Brush.verticalGradient(listOf(color, color.copy(alpha = 0.35f)))
                                    else Brush.verticalGradient(listOf(NeonBorder, NeonBorder))
                                )
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(d.label, color = if (d.isToday) color else TextMuted, fontSize = 10.sp, fontWeight = if (d.isToday) FontWeight.Bold else FontWeight.Normal, maxLines = 1)
                }
            }
        }
    }
}

/** Haftalık ayrıntı ekranı. */
@Composable
fun WeeklyTrendScreen(repository: GameRepository, onBackClick: () -> Unit) {
    val activity by repository.allDailyActivities.collectAsState(initial = emptyList())
    val days = remember(activity) { lastSevenDays(activity) }
    val weekSec = days.sumOf { it.seconds }
    val weekGames = days.sumOf { it.games }
    val weekStars = days.sumOf { it.stars }
    val activeDays = days.count { it.games > 0 }
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        NeonTopBar("Haftalık Özet", subtitle = "Son 7 gün", onBack = onBackClick)
        LazyColumn(contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 32.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Tile("⏱️", formatDuration(weekSec), "Bu hafta", NeonCyan, Modifier.weight(1f))
                    Tile("🎮", "$weekGames", "Oyun", NeonPurple, Modifier.weight(1f))
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Tile("⭐", "$weekStars", "Yıldız", NeonYellow, Modifier.weight(1f))
                    Tile("📅", "$activeDays / 7", "Aktif gün", NeonOrange, Modifier.weight(1f))
                }
            }
            item { WeeklyChartCard(days, compact = false) }
            item {
                GlassCard(accent = NeonGreen) {
                    val avg = if (activeDays > 0) weekSec / activeDays else 0
                    Text("💡 Öneri", color = TextPrimary, style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        when {
                            activeDays == 0 -> "Bu hafta henüz oynamadın. Günde 10 dakika ile başlamak harika bir hedef."
                            activeDays < 4 -> "Bu hafta $activeDays gün oynadın. Seriyi korumak için her gün kısa bir seviye dene."
                            avg < 600 -> "Düzenli oynuyorsun! Günlük ortalaman ${formatDuration(avg)}; 10 dakikaya çıkarmayı dene."
                            else -> "Harika bir hafta! Ortalama ${formatDuration(avg)} ile zihnin formda."
                        },
                        color = TextSecondary, style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}
