package com.example.ui.screens.gameslist

import androidx.compose.animation.core.Animatable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.GameRepository
import com.example.domain.model.GameCategory
import com.example.domain.model.GameMeta
import com.example.domain.model.GameRegistry
import com.example.game.engine.GameProgress
import com.example.game.engine.LevelRules
import com.example.ui.components.DiamondPill
import com.example.ui.components.EmojiBadge
import com.example.ui.components.GlassCard
import com.example.ui.components.NeonChip
import com.example.ui.components.NeonProgressBar
import com.example.ui.components.bounceClick
import com.example.ui.theme.NeonBorder
import com.example.ui.theme.NeonCard
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonYellow
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun GamesScreen(
    repository: GameRepository,
    initialCategory: String?,
    onOpenGame: (gameId: String) -> Unit,
    onOpenShop: () -> Unit,
    onOpenWow: () -> Unit = {}
) {
    val progress by repository.allProgress.collectAsState(initial = emptyMap())
    val diamonds by repository.preferences.diamondBalance.collectAsState(initial = 0)
    var query by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf(initialCategory) }
    LaunchedEffect(initialCategory) { if (initialCategory != null) category = initialCategory }

    val games = remember(query, category) {
        GameRegistry.ALL_GAMES.filter { g ->
            (category == null || g.category.name == category) &&
                (query.isBlank() || g.name.contains(query, true) || g.description.contains(query, true))
        }
    }
    val totalStars = progress.values.sumOf { it.totalStars }
    val maxStars = GameRegistry.ALL_GAMES.size * LevelRules.LEVEL_COUNT * 3

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize().statusBarsPadding()
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Oyunlar", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
                    Text(
                        "${GameRegistry.ALL_GAMES.size} oyun · ${GameRegistry.ALL_GAMES.size * LevelRules.LEVEL_COUNT} seviye · ⭐ $totalStars/$maxStars",
                        style = MaterialTheme.typography.bodySmall, color = TextSecondary
                    )
                }
                DiamondPill(diamonds, onOpenShop)
            }
        }
        if (query.isBlank() && category == null) {
            item(span = { GridItemSpan(maxLineSpan) }) { com.example.ui.wow.WowFeatureCard(repository, onOpenWow) }
        }
        item(span = { GridItemSpan(maxLineSpan) }) { SearchBox(query) { query = it } }
        item(span = { GridItemSpan(maxLineSpan) }) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item { CategoryChip("Tümü", "✨", NeonCyan, category == null) { category = null } }
                items(GameCategory.entries.toList()) { c ->
                    CategoryChip(c.title, c.emoji, c.color, category == c.name) { category = if (category == c.name) null else c.name }
                }
            }
        }
        if (games.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text("Aramana uyan oyun yok.", color = TextMuted, modifier = Modifier.padding(24.dp))
            }
        }
        itemsIndexed(games, key = { _, g -> g.id }) { index, g ->
            GameCard(g, progress[g.id] ?: GameProgress(emptyMap()), index) { onOpenGame(g.id) }
        }
    }
}

@Composable
private fun SearchBox(value: String, onChange: (String) -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(NeonCard)
            .border(1.dp, NeonBorder, shape)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Search, contentDescription = null, tint = NeonCyan)
        Spacer(Modifier.width(10.dp))
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) Text("Oyun ara...", color = TextMuted, style = MaterialTheme.typography.bodyMedium)
            BasicTextField(
                value = value, onValueChange = onChange, singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary),
                cursorBrush = SolidColor(NeonCyan),
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (value.isNotEmpty()) {
            Icon(
                Icons.Default.Close, contentDescription = "Temizle", tint = TextSecondary,
                modifier = Modifier.bounceClick { onChange("") }
            )
        }
    }
}

@Composable
private fun CategoryChip(label: String, emoji: String, color: androidx.compose.ui.graphics.Color, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    Row(
        Modifier
            .clip(shape)
            .background(if (selected) color.copy(alpha = 0.2f) else NeonCard)
            .border(1.dp, if (selected) color else NeonBorder, shape)
            .bounceClick(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(emoji, fontSize = 13.sp)
        Spacer(Modifier.width(6.dp))
        Text(label, color = if (selected) color else TextSecondary, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun GameCard(g: GameMeta, p: GameProgress, index: Int, onClick: () -> Unit) {
    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay((index % 8) * 40L)
        appear.animateTo(1f, tween(380))
    }
    val accent = g.category.color
    GlassCard(
        accent = accent,
        onClick = onClick,
        contentPadding = PaddingValues(14.dp),
        modifier = Modifier.graphicsLayer {
            alpha = appear.value
            translationY = (1f - appear.value) * 40f
        }
    ) {
        Row(verticalAlignment = Alignment.Top) {
            EmojiBadge(g.iconEmoji, accent, size = 46.dp)
            Spacer(Modifier.weight(1f))
            if (g.isNew) NeonChip("YENİ", NeonPink, filled = true)
        }
        Spacer(Modifier.height(10.dp))
        Text(g.name, style = MaterialTheme.typography.titleSmall, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(g.category.title, style = MaterialTheme.typography.labelSmall, color = accent, maxLines = 1)
        Spacer(Modifier.height(10.dp))
        NeonProgressBar(p.clearedCount / LevelRules.LEVEL_COUNT.toFloat(), accent, Modifier.fillMaxWidth(), height = 5.dp)
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("${p.clearedCount}/${LevelRules.LEVEL_COUNT}", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
            Spacer(Modifier.weight(1f))
            Text("★ ${p.totalStars}", color = NeonYellow, style = MaterialTheme.typography.labelSmall)
        }
    }
}
