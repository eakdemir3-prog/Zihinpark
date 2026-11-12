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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.example.ui.components.Mascot
import com.example.ui.components.MascotMood
import com.example.ui.components.MascotSays
import com.example.ui.components.bounceClick
import com.example.ui.game.FxOverlay
import com.example.ui.theme.NeonBorder
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar

@Composable
fun HomeScreen(
    repository: GameRepository,
    onOpenGame: (gameId: String) -> Unit,
    onPlayLevel: (gameId: String, level: Int) -> Unit,
    onOpenCategory: (categoryName: String) -> Unit,
    onOpenShop: () -> Unit,
    onOpenWow: () -> Unit = {},
    onOpenLingo: () -> Unit = {},
    onOpenBulmaca: () -> Unit = {}
) {
    val prefs = repository.preferences
    val userName by prefs.userName.collectAsState(initial = "Zihin Gezgini")
    val streak by prefs.dailyStreak.collectAsState(initial = 0)
    val diamonds by prefs.diamondBalance.collectAsState(initial = 0)
    val lastGame by prefs.lastPlayedGame.collectAsState(initial = null)
    val progress by repository.allProgress.collectAsState()
    val today by prefs.todayInfo.collectAsState(initial = com.example.data.pref.UserPreferencesRepository.TodayInfo(0, false, emptySet()))
    val playDays by prefs.playDays.collectAsState(initial = emptySet())
    val lingo by prefs.lingoDaily.collectAsState(initial = null)
    val giftClaimed by prefs.dailyGiftClaimed.collectAsState(initial = true)
    val giftStep by prefs.giftStep.collectAsState(initial = 1)
    val scope = rememberCoroutineScope()
    var showGift by remember { mutableStateOf(false) }
    LaunchedEffect(giftClaimed) { if (!giftClaimed && !HomeDebug.noGift) { delay(700); showGift = true } }

    // Günün oyunu: tarihe göre her gün değişir
    val dailyGame = remember {
        val day = Calendar.getInstance().get(Calendar.DAY_OF_YEAR) + Calendar.getInstance().get(Calendar.YEAR) * 7
        GameRegistry.ALL_GAMES[day % GameRegistry.ALL_GAMES.size]
    }
    val continueGame = lastGame?.let { GameRegistry.getGameById(it) }
    val mainGame = continueGame ?: dailyGame
    val goal = DAILY_GOAL
    val hour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val hello = when (hour) { in 5..11 -> "Günaydın"; in 12..17 -> "İyi günler"; in 18..22 -> "İyi akşamlar"; else -> "İyi geceler" }
    val firstName = if (userName == "Zihin Gezgini") userName else userName.substringBefore(' ').ifBlank { userName }

    LazyColumn(
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("✦ ZİHİNPARK", color = NeonPurple, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f), maxLines = 1)
                StreakPill(streak)
                Spacer(Modifier.width(8.dp))
                DiamondPill(diamonds, onOpenShop)
            }
        }
        item {
            val done = today.levels >= goal
            MascotSays(
                title = "$hello $firstName! 👋",
                text = if (done) "Bugünkü hedefini tamamladın, harikasın! 🎉" else "Bugün ${goal - today.levels} seviye daha. Hadi ısınalım!",
                mood = if (done) MascotMood.Cheer else MascotMood.Wink
            )
        }
        item {
            val p = progress[mainGame.id] ?: GameProgress(emptyMap())
            BigContinueCard(
                label = if (continueGame != null) "KALDIĞIN YERDEN" else "GÜNÜN OYUNUYLA BAŞLA",
                name = mainGame.name, emoji = mainGame.iconEmoji, accent = mainGame.category.color, p = p
            ) { onPlayLevel(mainGame.id, p.nextPlayableLevel) }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.height(androidx.compose.foundation.layout.IntrinsicSize.Min)) {
                WeekCard(streak, playDays, prefs.today(), Modifier.weight(1f).fillMaxHeight())
                GoalCard(today.levels, goal, Modifier.weight(1f).fillMaxHeight())
            }
        }
        item {
            Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Bugün için", style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text("3 görev · 💎 35", color = NeonCyan, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            }
        }
        item {
            GlassCard(accent = NeonPurple, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)) {
                val tasks = listOf(
                    DailyTask("lingo", "🟩", NeonGreen, "Günün kelimesini çöz", "Lingo", lingo?.playedToday == true, 10, onOpenLingo),
                    DailyTask("levels", "🏁", NeonOrange, "3 seviye geç", "${today.levels.coerceAtMost(3)}/3", today.levels >= 3, 10) { onPlayLevel(mainGame.id, (progress[mainGame.id] ?: GameProgress(emptyMap())).nextPlayableLevel) },
                    DailyTask("bulmaca", "🧩", NeonPurple, "1 bulmaca çöz", "Bulmaca Dünyası", today.bdSolved, 15, onOpenBulmaca)
                )
                tasks.forEachIndexed { i, t ->
                    TaskRow(t, claimed = t.id in today.claimed) { scope.launch { prefs.claimTask(t.id, t.gems) } }
                    if (i < tasks.lastIndex) Box(Modifier.fillMaxWidth().height(1.dp).background(NeonBorder.copy(alpha = 0.5f)))
                }
            }
        }

        item { com.example.ui.wow.WowFeatureCard(repository, onOpenWow) }
        item { com.example.ui.lingo.LingoFeatureCard(repository, onOpenLingo) }
        item { com.example.ui.bulmaca.BulmacaFeatureCard(repository, onOpenBulmaca) }

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
    }

    if (showGift) {
        DailyGiftSheet(
            step = giftStep, rewards = prefs.giftRewards,
            onClaim = { scope.launch { prefs.claimDailyGift(); delay(900); showGift = false } },
            onDismiss = { showGift = false }
        )
    }
}

const val DAILY_GOAL = 5

/** Ekran görüntüsü otomasyonu için (yalnızca debug). */
object HomeDebug { @Volatile var noGift = false }

private class DailyTask(val id: String, val emoji: String, val color: Color, val title: String, val sub: String, val done: Boolean, val gems: Int, val go: () -> Unit)

@Composable
private fun TaskRow(t: DailyTask, claimed: Boolean, onClaim: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(t.color.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) { Text(t.emoji, fontSize = 18.sp) }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(t.title, color = TextPrimary, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(t.sub, color = TextSecondary, style = MaterialTheme.typography.labelSmall, maxLines = 1)
        }
        Spacer(Modifier.width(8.dp))
        val shape = RoundedCornerShape(12.dp)
        when {
            claimed -> Text("✓ Alındı", color = NeonGreen, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
            t.done -> Text(
                "+${t.gems}💎 Al", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.clip(shape).background(Brush.horizontalGradient(listOf(NeonPurple, NeonCyan))).bounceClick(onClick = onClaim).padding(horizontal = 12.dp, vertical = 7.dp)
            )
            else -> Text(
                "Git ›", color = NeonPurple, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.clip(shape).border(1.dp, NeonPurple.copy(alpha = 0.5f), shape).bounceClick(onClick = t.go).padding(horizontal = 12.dp, vertical = 7.dp)
            )
        }
    }
}

@Composable
private fun BigContinueCard(label: String, name: String, emoji: String, accent: Color, p: GameProgress, onPlay: () -> Unit) {
    val shape = RoundedCornerShape(26.dp)
    Box(
        Modifier.fillMaxWidth().neonGlow(accent, 18.dp, 26.dp, 0.5f).clip(shape)
            .background(Brush.linearGradient(listOf(accent, NeonPurple, NeonCyan)))
            .bounceClick(pressedScale = 0.98f, onClick = onPlay)
            .padding(18.dp)
    ) {
        Text(emoji, fontSize = 92.sp, modifier = Modifier.align(Alignment.TopEnd).graphicsLayer { alpha = 0.25f; rotationZ = 12f; translationX = 30f; translationY = -20f })
        Column {
            Text(label, color = Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Text("$emoji $name", color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("Seviye ${p.nextPlayableLevel} · ⭐ ${p.totalStars}", color = Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(12.dp))
            val frac = p.clearedCount / LevelRules.LEVEL_COUNT.toFloat()
            Box(Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(4.dp)).background(Color.White.copy(alpha = 0.3f))) {
                Box(Modifier.fillMaxWidth(frac.coerceIn(0.02f, 1f)).fillMaxHeight().clip(RoundedCornerShape(4.dp)).background(Color.White))
            }
            Spacer(Modifier.height(5.dp))
            Row {
                Text("%${(frac * 100).toInt()} tamamlandı", color = Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
                Text("${p.clearedCount}/${LevelRules.LEVEL_COUNT}", color = Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.labelSmall)
            }
            Spacer(Modifier.height(12.dp))
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White).padding(vertical = 13.dp),
                contentAlignment = Alignment.Center
            ) { Text("DEVAM ET  ›", color = Color(0xFF2A1D6E), fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleSmall) }
        }
    }
}

@Composable
private fun WeekCard(streak: Int, days: Set<Long>, today: Long, modifier: Modifier) {
    GlassCard(accent = NeonOrange, modifier = modifier, contentPadding = PaddingValues(12.dp)) {
        Text("GÜNLÜK SERİ", color = TextSecondary, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, maxLines = 1)
        Row(verticalAlignment = Alignment.Bottom) {
            Text("🔥 $streak", color = TextPrimary, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
            Text(" gün", color = TextSecondary, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(bottom = 4.dp))
        }
        Spacer(Modifier.height(8.dp))
        // Pazartesiden başlayan bu hafta
        val dow = (Calendar.getInstance().get(Calendar.DAY_OF_WEEK) + 5) % 7 // Pzt=0
        val monday = today - dow
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            listOf("Pt", "Sa", "Ça", "Pe", "Cu", "Ct", "Pz").forEachIndexed { i, d ->
                val day = monday + i
                val on = day in days
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    val shape = RoundedCornerShape(6.dp)
                    Box(
                        Modifier.fillMaxWidth().height(16.dp).clip(shape)
                            .background(if (on) NeonOrange else NeonBorder.copy(alpha = 0.35f))
                            .then(if (day == today && !on) Modifier.border(1.5.dp, NeonOrange, shape) else Modifier)
                    )
                    Text(d, color = TextSecondary, fontSize = 9.sp, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun GoalCard(done: Int, goal: Int, modifier: Modifier) {
    GlassCard(accent = NeonPurple, modifier = modifier, contentPadding = PaddingValues(12.dp)) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("BUGÜNKÜ HEDEF", color = TextSecondary, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, maxLines = 1)
            Spacer(Modifier.height(6.dp))
            val frac by androidx.compose.animation.core.animateFloatAsState((done.toFloat() / goal).coerceIn(0f, 1f), tween(800), label = "goal")
            val track = NeonBorder.copy(alpha = 0.4f)
            Box(Modifier.size(64.dp), contentAlignment = Alignment.Center) {
                androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
                    val sw = 8.dp.toPx()
                    val st = androidx.compose.ui.graphics.drawscope.Stroke(sw, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                    val inset = sw / 2
                    val sz = androidx.compose.ui.geometry.Size(size.width - sw, size.height - sw)
                    val tl = androidx.compose.ui.geometry.Offset(inset, inset)
                    drawArc(track, 0f, 360f, false, tl, sz, style = st)
                    drawArc(Brush.sweepGradient(listOf(NeonPurple, NeonCyan, NeonPurple)), -90f, 360f * frac, false, tl, sz, style = st)
                }
                Text("${done.coerceAtMost(goal)}/$goal", color = TextPrimary, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleSmall)
            }
            Spacer(Modifier.height(4.dp))
            Text(if (done >= goal) "Tamamlandı 🎉" else "${goal - done} seviye kaldı", color = TextSecondary, style = MaterialTheme.typography.labelSmall, maxLines = 1)
        }
    }
}

@Composable
private fun DailyGiftSheet(step: Int, rewards: List<com.example.data.pref.UserPreferencesRepository.GiftReward>, onClaim: () -> Unit, onDismiss: () -> Unit) {
    val fx = com.example.ui.game.rememberGameFx()
    var claimed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(250); fx.confetti() }
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss, properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().bounceClick(pressedScale = 1f, onClick = onDismiss), contentAlignment = Alignment.BottomCenter) {
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)).background(NeonCard)
                    .bounceClick(pressedScale = 1f) {}.navigationBarsPadding().padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(Modifier.width(44.dp).height(5.dp).clip(RoundedCornerShape(3.dp)).background(NeonBorder))
                Spacer(Modifier.height(6.dp))
                Mascot(MascotMood.Cheer, 120.dp)
                Text("$step. gün hediyen!", color = TextPrimary, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                Text("Her gün gel, hediye büyüsün. 7. günde sandık seni bekliyor.", color = TextSecondary, style = MaterialTheme.typography.bodySmall, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                Spacer(Modifier.height(12.dp))
                for (rowIdx in 0..1) {
                    Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val range = if (rowIdx == 0) 0..3 else 4..6
                        for (i in range) {
                            val d = i + 1
                            val r = rewards[i]
                            val isNow = d == step
                            val past = d < step || (isNow && claimed)
                            val shape = RoundedCornerShape(14.dp)
                            val bg = when {
                                past -> NeonGreen.copy(alpha = 0.18f)
                                isNow -> NeonPurple
                                d == 7 -> NeonYellow.copy(alpha = 0.22f)
                                else -> NeonBorder.copy(alpha = 0.22f)
                            }
                            Column(
                                Modifier.weight(if (d == 7) 2f else 1f).clip(shape).background(bg)
                                    .border(1.dp, if (past) NeonGreen else if (isNow) NeonPurple else NeonBorder, shape).padding(vertical = 8.dp, horizontal = 2.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                val c = if (isNow && !past) Color.White else TextSecondary
                                Text("$d. gün", color = c, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, maxLines = 1)
                                Text(if (past) "✓" else r.emoji, fontSize = 20.sp, color = if (past) NeonGreen else Color.Unspecified)
                                Text(r.label, color = c, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                val label = rewards.getOrNull(step - 1)?.label ?: ""
                NeonButton(
                    if (claimed) "Eklendi! 🎉" else "Harika, al! $label",
                    onClick = { if (!claimed) { claimed = true; fx.confetti(); onClaim() } },
                    modifier = Modifier.fillMaxWidth(), color = NeonPurple
                )
            }
            FxOverlay(fx)
        }
    }
}

@Composable
private fun StreakPill(streak: Int) {
    com.example.ui.components.StatPill("🔥", streak, NeonOrange)
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
