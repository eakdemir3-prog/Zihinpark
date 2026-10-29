package com.example.ui.screens.leaderboard

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.GameRepository
import com.example.domain.model.GameRegistry
import com.example.game.engine.GameProgress
import com.example.game.engine.LevelRules
import com.example.ui.components.EmojiBadge
import com.example.ui.components.GlassCard
import com.example.ui.components.NeonChip
import com.example.ui.components.NeonProgressBar
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

private data class Badge(val emoji: String, val title: String, val desc: String, val progress: Int, val goal: Int, val color: Color)

/**
 * Kişisel rekorlar: oyun sıralaması (yıldıza göre), oyun başına en iyi skorlar/sonsuz mod
 * rekorları ve ilerlemeye bağlı rozetler. Tüm veriler cihazdaki gerçek oyun kayıtlarından gelir.
 */
@Composable
fun LeaderboardScreen(repository: GameRepository) {
    val progress by repository.allProgress.collectAsState(initial = emptyMap())
    val games by repository.allGames.collectAsState(initial = emptyList())
    val streak by repository.preferences.dailyStreak.collectAsState(initial = 0)
    var tab by rememberSaveable { mutableIntStateOf(0) }

    val bestScores = games.associate { it.id to it.highestScore }
    val totalStars = progress.values.sumOf { it.totalStars }
    val cleared = progress.values.sumOf { it.clearedCount }
    val bosses = progress.values.sumOf { p -> (1..LevelRules.ZONE_COUNT).count { p.stars(it * 10) > 0 } }
    val perfect = progress.values.sumOf { p -> p.starsByLevel.count { it.value == 3 } }
    val gamesStarted = progress.values.count { it.clearedCount > 0 }
    val zonesMastered = progress.values.sumOf { p -> (0 until LevelRules.ZONE_COUNT).count { p.zoneStars(it) == 30 } }
    val endless = progress.values.count { it.isEndlessUnlocked }

    val badges = listOf(
        Badge("🚀", "İlk Adım", "İlk seviyeni geç", cleared, 1, NeonCyan),
        Badge("🧭", "Kâşif", "10 farklı oyunda seviye geç", gamesStarted, 10, NeonGreen),
        Badge("🌐", "Tam Tur", "${GameRegistry.ALL_GAMES.size} oyunun hepsinde seviye geç", gamesStarted, GameRegistry.ALL_GAMES.size, NeonGreen),
        Badge("👑", "Boss Avcısı", "10 boss seviyesini yen", bosses, 10, NeonPink),
        Badge("🐉", "Boss Efsanesi", "50 boss seviyesini yen", bosses, 50, NeonPink),
        Badge("⭐", "Yıldız Toplayıcı", "100 yıldız topla", totalStars, 100, NeonYellow),
        Badge("🌟", "Takımyıldız", "1000 yıldız topla", totalStars, 1000, NeonYellow),
        Badge("💯", "Kusursuz", "25 seviyeyi 3 yıldızla bitir", perfect, 25, NeonPurple),
        Badge("🏛️", "Bölge Ustası", "Bir bölgenin tüm 30 yıldızını al", zonesMastered, 1, NeonOrange),
        Badge("♾️", "Sonsuzluk", "Bir oyunda Sonsuz Mod'u aç", endless, 1, NeonPink),
        Badge("🔥", "Kararlı", "7 gün üst üste oyna", streak, 7, NeonOrange),
        Badge("🧠", "Zihin Efsanesi", "Tüm ${GameRegistry.ALL_GAMES.size * LevelRules.LEVEL_COUNT} seviyeyi geç", cleared, GameRegistry.ALL_GAMES.size * LevelRules.LEVEL_COUNT, NeonCyan)
    )

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text("Liderlik", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
            Text("⭐ $totalStars · $cleared seviye · ${badges.count { it.progress >= it.goal }}/${badges.size} rozet",
                style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            Spacer(Modifier.height(12.dp))
            SegmentedTabs(listOf("Lig", "Oyunlar", "Skorlar", "Rozet"), tab) { tab = it }
        }
        AnimatedContent(tab, transitionSpec = { fadeIn().togetherWith(fadeOut()) }, label = "lb") { t ->
            when (t) {
                0 -> LeagueTab()
                1 -> {
                    val ranked = GameRegistry.ALL_GAMES.sortedWith(
                        compareByDescending<com.example.domain.model.GameMeta> { progress[it.id]?.totalStars ?: 0 }.thenBy { it.name }
                    )
                    LazyColumn(contentPadding = PaddingValues(16.dp, 0.dp, 16.dp, 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        itemsIndexed(ranked, key = { _, g -> g.id }) { i, g ->
                            val p = progress[g.id] ?: GameProgress(emptyMap())
                            GlassCard(accent = if (i < 3 && p.totalStars > 0) NeonYellow else g.category.color, contentPadding = PaddingValues(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        when (i) { 0 -> "🥇"; 1 -> "🥈"; 2 -> "🥉"; else -> "${i + 1}" },
                                        fontSize = if (i < 3) 22.sp else 15.sp, color = TextSecondary, fontWeight = FontWeight.Bold,
                                        modifier = Modifier.width(34.dp)
                                    )
                                    EmojiBadge(g.iconEmoji, g.category.color, size = 40.dp)
                                    Spacer(Modifier.width(10.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(g.name, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                                        Spacer(Modifier.height(4.dp))
                                        NeonProgressBar(p.totalStars / 150f, g.category.color, Modifier.fillMaxWidth(), height = 4.dp)
                                    }
                                    Spacer(Modifier.width(10.dp))
                                    Text("★ ${p.totalStars}", color = NeonYellow, style = MaterialTheme.typography.labelLarge)
                                }
                            }
                        }
                    }
                }
                2 -> LazyColumn(contentPadding = PaddingValues(16.dp, 0.dp, 16.dp, 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    itemsIndexed(GameRegistry.ALL_GAMES, key = { _, g -> g.id }) { _, g ->
                        val p = progress[g.id] ?: GameProgress(emptyMap())
                        GlassCard(accent = g.category.color, contentPadding = PaddingValues(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                EmojiBadge(g.iconEmoji, g.category.color, size = 40.dp)
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(g.name, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                                    Text("En iyi seviye skoru: ${bestScores[g.id] ?: 0}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                }
                                if (p.isEndlessUnlocked) NeonChip("♾️ ${p.endlessBest} tur", NeonPink)
                                else Text("${p.clearedCount}/50", color = TextMuted, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
                else -> LazyColumn(contentPadding = PaddingValues(16.dp, 0.dp, 16.dp, 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    itemsIndexed(badges, key = { _, b -> b.title }) { _, b ->
                        val done = b.progress >= b.goal
                        GlassCard(accent = if (done) b.color else NeonBorder, glow = done, contentPadding = PaddingValues(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                EmojiBadge(if (done) b.emoji else "🔒", if (done) b.color else TextMuted, size = 44.dp)
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(b.title, style = MaterialTheme.typography.titleSmall, color = if (done) TextPrimary else TextSecondary)
                                    Text(b.desc, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                    Spacer(Modifier.height(6.dp))
                                    NeonProgressBar(b.progress.toFloat() / b.goal, b.color, Modifier.fillMaxWidth(), height = 4.dp)
                                }
                                Spacer(Modifier.width(10.dp))
                                Text("${minOf(b.progress, b.goal)}/${b.goal}", color = if (done) b.color else TextMuted, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}
