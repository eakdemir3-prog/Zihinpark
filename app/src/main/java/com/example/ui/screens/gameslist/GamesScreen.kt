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
import com.example.i18n.Text
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
    onOpenWow: () -> Unit = {},
    onOpenLingo: () -> Unit = {},
    onOpenBulmaca: () -> Unit = {}
) {
    val progress by repository.allProgress.collectAsState()
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
    val dailyGame = remember {
        val c = java.util.Calendar.getInstance()
        GameRegistry.ALL_GAMES[(c.get(java.util.Calendar.DAY_OF_YEAR) + c.get(java.util.Calendar.YEAR) * 7) % GameRegistry.ALL_GAMES.size]
    }
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
        val browse = query.isBlank() && category == null
        if (browse) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                val p = progress[dailyGame.id] ?: GameProgress(emptyMap())
                DailyBigCard(dailyGame, p) { onOpenGame(dailyGame.id) }
            }
            item(span = { GridItemSpan(maxLineSpan) }) { com.example.ui.wow.WowFeatureCard(repository, onOpenWow) }
            item(span = { GridItemSpan(maxLineSpan) }) { com.example.ui.lingo.LingoFeatureCard(repository, onOpenLingo) }
            if (!com.example.i18n.I18n.isEn) item(span = { GridItemSpan(maxLineSpan) }) { com.example.ui.bulmaca.BulmacaFeatureCard(repository, onOpenBulmaca) }
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
        if (browse) {
            GameCategory.entries.forEach { cat ->
                val list = GameRegistry.ALL_GAMES.filter { it.category == cat }
                if (list.isEmpty()) return@forEach
                item(span = { GridItemSpan(maxLineSpan) }, key = "h_${cat.name}") {
                    Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("${cat.emoji} ${cat.title}", style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, modifier = Modifier.weight(1f), maxLines = 1)
                        Text("${list.size} oyun ›", color = cat.color, style = MaterialTheme.typography.labelMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            modifier = Modifier.bounceClick { category = cat.name })
                    }
                }
                item(span = { GridItemSpan(maxLineSpan) }, key = "r_${cat.name}") {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(list, key = { it.id }) { g -> StripCard(g, progress[g.id] ?: GameProgress(emptyMap())) { onOpenGame(g.id) } }
                    }
                }
            }
        } else itemsIndexed(games, key = { _, g -> g.id }) { index, g ->
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

private fun darker(c: androidx.compose.ui.graphics.Color) = androidx.compose.ui.graphics.lerp(c, androidx.compose.ui.graphics.Color.Black, 0.22f)

@Composable
private fun DailyBigCard(g: GameMeta, p: GameProgress, onClick: () -> Unit) {
    val W = androidx.compose.ui.graphics.Color.White
    val c = g.category.color
    val shape = RoundedCornerShape(26.dp)
    Box(
        Modifier.fillMaxWidth().clip(shape)
            .background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(c, darker(c))))
            .bounceClick(pressedScale = 0.98f, onClick = onClick)
    ) {
        com.example.ui.components.Mascot(com.example.ui.components.MascotMood.Happy, 128.dp, Modifier.align(Alignment.BottomEnd).padding(end = 4.dp, bottom = 4.dp))
        Column(Modifier.padding(18.dp)) {
            Text("GÜNÜN OYUNU", color = W, fontSize = 11.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold,
                modifier = Modifier.clip(RoundedCornerShape(9.dp)).background(W.copy(alpha = 0.22f)).border(1.dp, W.copy(alpha = 0.6f), RoundedCornerShape(9.dp)).padding(horizontal = 8.dp, vertical = 3.dp))
            Spacer(Modifier.height(10.dp))
            Text("${g.iconEmoji} ${g.name}", color = W, style = MaterialTheme.typography.headlineSmall, fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.fillMaxWidth(0.72f))
            Text(g.description, color = W.copy(alpha = 0.92f), style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.fillMaxWidth(0.62f))
            Spacer(Modifier.height(12.dp))
            Box(Modifier.fillMaxWidth(0.55f).height(6.dp).clip(RoundedCornerShape(3.dp)).background(W.copy(alpha = 0.3f))) {
                Box(Modifier.fillMaxWidth((p.clearedCount / LevelRules.LEVEL_COUNT.toFloat()).coerceIn(0.02f, 1f)).height(6.dp).clip(RoundedCornerShape(3.dp)).background(W))
            }
            Text("Seviye ${p.nextPlayableLevel} / ${LevelRules.LEVEL_COUNT} · ⭐ ${p.totalStars}", color = W.copy(alpha = 0.92f), style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 4.dp))
            Spacer(Modifier.height(12.dp))
            Text("Oyna", color = darker(c), fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold, style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.clip(RoundedCornerShape(14.dp)).background(W).padding(horizontal = 26.dp, vertical = 10.dp))
        }
    }
}

@Composable
private fun StripCard(g: GameMeta, p: GameProgress, onClick: () -> Unit) {
    val W = androidx.compose.ui.graphics.Color.White
    val c = g.category.color
    val shape = RoundedCornerShape(20.dp)
    Box(
        Modifier.width(150.dp).height(160.dp).clip(shape)
            .background(androidx.compose.ui.graphics.Brush.linearGradient(listOf(c, darker(c))))
            .bounceClick(onClick = onClick).padding(12.dp)
    ) {
        Text(g.iconEmoji, fontSize = 30.sp)
        if (g.isNew) Text("YENİ", color = W, fontSize = 10.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold,
            modifier = Modifier.align(Alignment.TopEnd).clip(RoundedCornerShape(7.dp)).background(NeonPink).padding(horizontal = 6.dp, vertical = 2.dp))
        Column(Modifier.align(Alignment.BottomStart).padding(bottom = 34.dp)) {
            Text(g.name, color = W, style = MaterialTheme.typography.titleSmall, fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(if (p.clearedCount == 0) "Yeni" else "${p.clearedCount}/${LevelRules.LEVEL_COUNT} · ★${p.totalStars}", color = W.copy(alpha = 0.9f), style = MaterialTheme.typography.labelSmall, maxLines = 1)
        }
        Text("Oyna", color = darker(c), fontSize = 12.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold,
            modifier = Modifier.align(Alignment.BottomEnd).clip(RoundedCornerShape(11.dp)).background(W).padding(horizontal = 10.dp, vertical = 5.dp))
    }
}
