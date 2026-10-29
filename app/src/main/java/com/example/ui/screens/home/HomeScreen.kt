package com.example.ui.screens.home

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.GameRepository
import com.example.domain.model.GameCategory
import com.example.domain.model.GameRegistry
import com.example.game.engine.GameProgress
import com.example.game.engine.LevelRules
import com.example.ui.components.AnimatedCounter
import com.example.ui.components.DiamondPill
import com.example.ui.components.EmojiBadge
import com.example.ui.components.GlassCard
import com.example.ui.components.NeonButton
import com.example.ui.components.NeonChip
import com.example.ui.components.NeonProgressBar
import com.example.ui.components.SectionTitle
import com.example.ui.components.neonGlow
import com.example.ui.theme.NeonCard
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonOrange
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.NeonYellow
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Calendar

@Composable
fun HomeScreen(
    repository: GameRepository,
    onOpenGame: (gameId: String) -> Unit,
    onPlayLevel: (gameId: String, level: Int) -> Unit,
    onOpenCategory: (categoryName: String) -> Unit,
    onOpenShop: () -> Unit,
    onOpenWow: () -> Unit = {}
) {
    val userName by repository.preferences.userName.collectAsState(initial = "Zihin Gezgini")
    val streak by repository.preferences.dailyStreak.collectAsState(initial = 0)
    val diamonds by repository.preferences.diamondBalance.collectAsState(initial = 0)
    val lastGame by repository.preferences.lastPlayedGame.collectAsState(initial = null)
    val progress by repository.allProgress.collectAsState(initial = emptyMap())

    val totalStars = progress.values.sumOf { it.totalStars }
    val cleared = progress.values.sumOf { it.clearedCount }
    val totalLevels = GameRegistry.ALL_GAMES.size * LevelRules.LEVEL_COUNT

    // Günün oyunu: tarihe göre her gün değişir
    val dailyGame = remember {
        val day = Calendar.getInstance().get(Calendar.DAY_OF_YEAR) + Calendar.getInstance().get(Calendar.YEAR) * 7
        GameRegistry.ALL_GAMES[day % GameRegistry.ALL_GAMES.size]
    }
    val continueGame = lastGame?.let { GameRegistry.getGameById(it) }
    val newGame = GameRegistry.ALL_GAMES.firstOrNull { it.isNew }

    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Merhaba,", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                    Text(
                        userName, style = MaterialTheme.typography.headlineMedium, color = TextPrimary,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
                StreakPill(streak)
                Spacer(Modifier.width(8.dp))
                DiamondPill(diamonds, onOpenShop)
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile("Yıldız", totalStars, "⭐", NeonYellow, Modifier.weight(1f))
                StatTile("Seviye", cleared, "🏁", NeonGreen, Modifier.weight(1f), suffix = "/$totalLevels")
                StatTile("Seri", streak, "🔥", NeonOrange, Modifier.weight(1f), suffix = " gün")
            }
        }

        item { com.example.ui.wow.WowFeatureCard(repository, onOpenWow) }

        if (continueGame != null) {
            item {
                val p = progress[continueGame.id] ?: GameProgress(emptyMap())
                ContinueCard(continueGame.name, continueGame.iconEmoji, continueGame.category.color, p) {
                    onPlayLevel(continueGame.id, p.nextPlayableLevel)
                }
            }
        }

        item {
            DailyCard(dailyGame.name, dailyGame.description, dailyGame.iconEmoji) {
                onOpenGame(dailyGame.id)
            }
        }

        item { SectionTitle("Zekâ Alanları") }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(GameCategory.entries.toList()) { cat ->
                    val count = GameRegistry.ALL_GAMES.count { it.category == cat }
                    val stars = GameRegistry.ALL_GAMES.filter { it.category == cat }.sumOf { progress[it.id]?.totalStars ?: 0 }
                    GlassCard(
                        accent = cat.color,
                        onClick = { onOpenCategory(cat.name) },
                        modifier = Modifier.width(150.dp),
                        contentPadding = PaddingValues(14.dp)
                    ) {
                        EmojiBadge(cat.emoji, cat.color, size = 40.dp)
                        Spacer(Modifier.height(10.dp))
                        Text(cat.title, style = MaterialTheme.typography.titleSmall, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("$count oyun · ★ $stars", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    }
                }
            }
        }

        if (newGame != null) {
            item {
                GlassCard(accent = NeonPink, glow = true, onClick = { onOpenGame(newGame.id) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        EmojiBadge(newGame.iconEmoji, NeonPink, size = 52.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            NeonChip("YENİ OYUN", NeonPink, filled = true)
                            Spacer(Modifier.height(4.dp))
                            Text(newGame.name, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                            Text(newGame.description, style = MaterialTheme.typography.bodySmall, color = TextSecondary, maxLines = 2)
                        }
                    }
                }
            }
        }

        item { SectionTitle("Senin İçin") }
        val picks = GameRegistry.ALL_GAMES
            .sortedBy { (progress[it.id]?.clearedCount ?: 0) }
            .filter { it.id != dailyGame.id }
            .take(4)
        items(picks, key = { it.id }) { g ->
            val p = progress[g.id] ?: GameProgress(emptyMap())
            GlassCard(accent = g.category.color, onClick = { onOpenGame(g.id) }, contentPadding = PaddingValues(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    EmojiBadge(g.iconEmoji, g.category.color, size = 44.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(g.name, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                        Spacer(Modifier.height(4.dp))
                        NeonProgressBar(p.clearedCount / LevelRules.LEVEL_COUNT.toFloat(), g.category.color, Modifier.fillMaxWidth(), height = 4.dp)
                    }
                    Spacer(Modifier.width(12.dp))
                    Text("${p.clearedCount}/50", color = TextSecondary, style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        item {
            GlassCard(accent = NeonGreen, contentPadding = PaddingValues(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("💡", fontSize = 22.sp)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Her gün farklı alanlardan birkaç seviye oynamak dikkat, hafıza ve problem çözmeyi birlikte çalıştırır.",
                        style = MaterialTheme.typography.bodySmall, color = TextPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun StreakPill(streak: Int) {
    val shape = RoundedCornerShape(50)
    Row(
        Modifier
            .clip(shape)
            .background(NeonCard)
            .border(1.dp, NeonOrange.copy(alpha = 0.6f), shape)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("🔥", fontSize = 14.sp)
        Spacer(Modifier.width(4.dp))
        Text("$streak", color = NeonOrange, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun StatTile(label: String, value: Int, emoji: String, color: Color, modifier: Modifier, suffix: String = "") {
    GlassCard(accent = color, modifier = modifier, contentPadding = PaddingValues(12.dp)) {
        Text(emoji, fontSize = 18.sp)
        Spacer(Modifier.height(4.dp))
        AnimatedCounter(value, color = TextPrimary, style = MaterialTheme.typography.titleLarge)
        Text(label + suffix, style = MaterialTheme.typography.labelSmall, color = TextSecondary, maxLines = 1)
    }
}

@Composable
private fun ContinueCard(name: String, emoji: String, accent: Color, p: GameProgress, onPlay: () -> Unit) {
    GlassCard(accent = accent, glow = true) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            EmojiBadge(emoji, accent, size = 52.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Kaldığın yerden devam", style = MaterialTheme.typography.labelMedium, color = accent)
                Text(name, style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                Text("Seviye ${p.nextPlayableLevel} · ⭐ ${p.totalStars}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
        }
        Spacer(Modifier.height(12.dp))
        NeonButton("Seviye ${p.nextPlayableLevel}'i Oyna", onPlay, color = accent, icon = Icons.Default.PlayArrow, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun DailyCard(name: String, desc: String, emoji: String, onPlay: () -> Unit) {
    val t = rememberInfiniteTransition(label = "daily")
    val glow by t.animateFloat(0.35f, 0.85f, infiniteRepeatable(tween(1600), RepeatMode.Reverse), label = "g")
    val shape = RoundedCornerShape(24.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .neonGlow(NeonPurple, 22.dp, 24.dp, glow)
            .clip(shape)
            .background(Brush.linearGradient(listOf(NeonPurple.copy(alpha = 0.55f), NeonPink.copy(alpha = 0.35f), NeonCard)))
            .border(1.dp, Brush.linearGradient(listOf(NeonPink, NeonCyan)), shape)
            .padding(18.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                NeonChip("GÜNÜN OYUNU", NeonYellow, filled = true)
                Spacer(Modifier.weight(1f))
                Text(emoji, fontSize = 40.sp, modifier = Modifier.graphicsLayer { rotationZ = (glow - 0.6f) * 12f })
            }
            Spacer(Modifier.height(6.dp))
            Text(name, style = MaterialTheme.typography.headlineSmall, color = TextPrimary)
            Text(desc, style = MaterialTheme.typography.bodySmall, color = TextPrimary.copy(alpha = 0.85f), maxLines = 2)
            Spacer(Modifier.height(14.dp))
            NeonButton("Hemen Oyna", onPlay, color = NeonCyan, icon = Icons.Default.PlayArrow, modifier = Modifier.fillMaxWidth())
        }
    }
}
