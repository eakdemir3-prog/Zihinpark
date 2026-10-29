package com.example.ui.wow

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import com.example.data.repository.GameRepository
import com.example.ui.components.DiamondPill
import com.example.ui.components.GlassCard
import com.example.ui.components.NeonBackground
import com.example.ui.components.NeonButton
import com.example.ui.components.NeonProgressBar
import com.example.ui.components.NeonTopBar
import com.example.ui.components.bounceClick
import com.example.ui.components.neonGlow
import com.example.ui.components.shake
import com.example.ui.game.FxOverlay
import com.example.ui.game.rememberGameFx
import com.example.ui.theme.NeonBg
import com.example.ui.theme.NeonBorder
import com.example.ui.theme.NeonCard
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonOrange
import com.example.ui.theme.NeonRed
import com.example.ui.theme.NeonYellow
import com.example.ui.theme.OnNeon
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.SoundAndVibeManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

// =====================================================================
// Bölüm haritası
// =====================================================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WowMapScreen(repository: GameRepository, onBack: () -> Unit, onPlay: (Int) -> Unit, onOpenShop: () -> Unit) {
    val context = LocalContext.current
    val data = remember { WowData.load(context) }
    val (chapters, _) = data
    val current by repository.preferences.wowLevel.collectAsState(initial = 1)
    val diamonds by repository.preferences.diamondBalance.collectAsState(initial = 0)
    val bonus by repository.preferences.wowBonus.collectAsState(initial = 0)
    val curChapter = ((current.coerceAtMost(WowData.LEVEL_COUNT) - 1) / WowData.PER_CHAPTER).coerceIn(0, chapters.size - 1)
    val listState = rememberLazyListState()
    LaunchedEffect(curChapter) { if (curChapter > 0) listState.scrollToItem(curChapter) }

    NeonBackground(accent = chapters[curChapter].colors.first, accent2 = chapters[curChapter].colors.second) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            NeonTopBar("Kelime Diyarı", subtitle = "300 seviye · 15 durak", onBack = onBack, trailing = { DiamondPill(diamonds, onOpenShop) })
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 32.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    val done = current > WowData.LEVEL_COUNT
                    val ch = chapters[curChapter]
                    GlassCard(accent = ch.colors.first, glow = true) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(ch.emoji, fontSize = 40.sp)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(if (done) "Tüm duraklar bitti!" else "${ch.name} · Seviye $current", color = TextPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("Bonus kelime: $bonus · her 10 bonusa 10 💎", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                                Spacer(Modifier.height(6.dp))
                                NeonProgressBar(((current - 1) % WowData.PER_CHAPTER) / WowData.PER_CHAPTER.toFloat(), ch.colors.first, Modifier.fillMaxWidth())
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        NeonButton(if (done) "Son seviyeyi tekrar oyna" else "Oyna", { onPlay(current.coerceAtMost(WowData.LEVEL_COUNT)) }, Modifier.fillMaxWidth(), color = ch.colors.first)
                    }
                }
                itemsIndexed(chapters) { _, ch ->
                    val locked = ch.firstLevel > current
                    val solved = (current - ch.firstLevel).coerceIn(0, WowData.PER_CHAPTER)
                    GlassCard(accent = if (locked) NeonBorder else ch.colors.first, modifier = Modifier.graphicsLayer { alpha = if (locked) 0.55f else 1f }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier.size(48.dp).clip(RoundedCornerShape(14.dp))
                                    .background(Brush.linearGradient(listOf(ch.colors.first.copy(alpha = 0.5f), ch.colors.second.copy(alpha = 0.3f)))),
                                contentAlignment = Alignment.Center
                            ) { Text(if (locked) "🔒" else ch.emoji, fontSize = 24.sp) }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text("${ch.index + 1}. ${ch.name}", color = TextPrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                Text("Seviye ${ch.firstLevel}–${ch.lastLevel} · $solved/${WowData.PER_CHAPTER}", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                            }
                            if (solved == WowData.PER_CHAPTER) Text("✅", fontSize = 20.sp)
                        }
                        if (!locked) {
                            Spacer(Modifier.height(10.dp))
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                for (lv in ch.firstLevel..ch.lastLevel) {
                                    val state = when { lv < current -> 2; lv == current -> 1; else -> 0 }
                                    val c = when (state) { 2 -> NeonGreen; 1 -> ch.colors.first; else -> NeonBorder }
                                    Box(
                                        Modifier.size(34.dp)
                                            .then(if (state == 1) Modifier.neonGlow(c, 8.dp, 17.dp, 0.8f) else Modifier)
                                            .clip(CircleShape)
                                            .background(if (state == 1) c else NeonCard)
                                            .border(1.dp, c, CircleShape)
                                            .bounceClick(enabled = state > 0) { if (state > 0) onPlay(lv) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("$lv", color = if (state == 1) OnNeon else if (state == 2) NeonGreen else TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// =====================================================================
// Oyun ekranı
// =====================================================================

@Composable
fun WowPlayScreen(
    level: Int,
    repository: GameRepository,
    soundManager: SoundAndVibeManager,
    onBack: () -> Unit,
    onNext: (Int) -> Unit,
    onOpenShop: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? android.app.Activity
    val (chapters, levels) = remember { WowData.load(context) }
    val lv = levels[(level - 1).coerceIn(0, levels.size - 1)]
    val ch = chapters[lv.chapter]
    val accent = ch.colors.first
    val scope = rememberCoroutineScope()
    val fx = rememberGameFx()
    val prefs = repository.preferences

    val diamonds by prefs.diamondBalance.collectAsState(initial = 0)
    val hints by prefs.hintsCount.collectAsState(initial = 0)

    val found = remember { mutableStateListOf<String>() }
    val bonusFound = remember { mutableStateListOf<String>() }
    val revealed = remember { mutableStateListOf<String>() } // "r,c"
    var loaded by remember { mutableStateOf(false) }
    var order by remember { mutableStateOf(lv.letters.indices.toList()) }
    val sel = remember { mutableStateListOf<Int>() }
    var finger by remember { mutableStateOf<Offset?>(null) }
    var shakeKey by remember { mutableIntStateOf(0) }
    var lastFoundKey by remember { mutableIntStateOf(0) }
    var preview by remember { mutableStateOf("") }
    var previewColor by remember { mutableStateOf(TextPrimary) }
    var complete by remember { mutableStateOf(false) }
    var reward by remember { mutableIntStateOf(0) }

    DisposableEffectTheme(soundManager)

    LaunchedEffect(level) {
        val (savedLevel, save) = prefs.wowSave.first()
        if (savedLevel == level) {
            found.addAll(save.found.filter { w -> lv.words.any { it.text == w } })
            bonusFound.addAll(save.bonusWords)
            revealed.addAll(save.revealed)
        }
        loaded = true
    }

    fun persist() {
        scope.launch { prefs.saveWowProgress(level, found.toSet(), bonusFound.toSet(), revealed.toSet()) }
    }

    fun cellShown(rc: Pair<Int, Int>): Boolean =
        "${rc.first},${rc.second}" in revealed || lv.words.any { it.text in found && rc in it.cells() }

    fun checkComplete() {
        // Harf açarak tamamen görünen kelimeler de bulunmuş sayılır
        lv.words.forEach { w -> if (w.text !in found && w.cells().all { cellShown(it) }) found += w.text }
        if (found.size == lv.words.size && !complete) {
            complete = true
            soundManager.playLevelWin()
            fx.confetti()
            scope.launch {
                val first = prefs.completeWowLevel(level)
                var r = 0
                if (first) {
                    r = 3 + if (level % WowData.PER_CHAPTER == 0) 40 else 0
                    prefs.addDiamonds(r)
                    com.example.cloud.CloudSync.addWeeklyScore(20 + lv.words.size * 4)
                    com.example.cloud.CloudSync.scheduleBackup()
                }
                reward = r
            }
        }
    }

    fun submit() {
        val word = sel.joinToString("") { lv.letters[order[it]].toString() }
        sel.clear(); finger = null
        if (word.length < 2 || complete) { preview = ""; return }
        when {
            lv.words.any { it.text == word } && word !in found -> {
                found += word; lastFoundKey++
                soundManager.playCorrect()
                previewColor = NeonGreen
                checkComplete()
                persist()
            }
            word in found -> {
                previewColor = NeonOrange; shakeKey++
                fx.float("Zaten bulundu", NeonOrange, 0.5f, 0.55f)
                soundManager.playTap()
            }
            word in lv.bonus && word !in bonusFound -> {
                bonusFound += word
                previewColor = NeonYellow
                soundManager.playCoin()
                fx.float("⭐ Bonus: $word", NeonYellow, 0.5f, 0.55f)
                scope.launch {
                    if (prefs.addWowBonus()) {
                        prefs.addDiamonds(10)
                        fx.float("10 bonus! +10 💎", NeonCyan, 0.5f, 0.45f)
                    }
                }
                persist()
            }
            word in bonusFound -> {
                previewColor = NeonOrange
                fx.float("Bonus zaten bulundu", NeonOrange, 0.5f, 0.55f)
            }
            else -> {
                previewColor = NeonRed; shakeKey++
                soundManager.playWrong()
            }
        }
        scope.launch { delay(650); preview = ""; previewColor = TextPrimary }
    }

    fun revealLetter() {
        if (complete) return
        val target = lv.words.firstOrNull { it.text !in found } ?: return
        val cell = target.cells().firstOrNull { !cellShown(it) } ?: return
        scope.launch {
            val ok = if (hints > 0) prefs.useBooster("hint") else prefs.spendDiamonds(25)
            if (!ok) {
                val act = activity
                if (act == null) { fx.float("Yetersiz 💎", NeonRed); return@launch }
                // Elmas yoksa: ödüllü reklamla bedava harf
                com.example.monetize.Ads.showRewarded(act, prefs, onUnavailable = { fx.float("Yetersiz 💎 · $it", NeonRed) }) {
                    revealed += "${cell.first},${cell.second}"
                    soundManager.playCorrect()
                    checkComplete()
                    persist()
                }
                return@launch
            }
            revealed += "${cell.first},${cell.second}"
            soundManager.playCorrect()
            checkComplete()
            persist()
        }
    }

    NeonBackground(accent = accent, accent2 = ch.colors.second) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            NeonTopBar(
                "${ch.emoji} ${ch.name}",
                subtitle = "Seviye $level · ${found.size}/${lv.words.size} kelime",
                onBack = onBack,
                trailing = { DiamondPill(diamonds, onOpenShop) }
            )
            // ---------- Bulmaca ızgarası ----------
            BoxWithConstraints(
                Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                val cell = min(min(maxWidth / lv.cols, maxHeight / lv.rows), 48.dp)
                Box(Modifier.size(cell * lv.cols, cell * lv.rows)) {
                    lv.cellMap.forEach { (rc, chr) ->
                        val shown = loaded && cellShown(rc)
                        val bg by animateColorAsState(if (shown) accent.copy(alpha = 0.9f) else NeonCard, tween(300), label = "c")
                        val sc by animateFloatAsState(if (shown) 1f else 0.92f, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "s")
                        Box(
                            Modifier
                                .offset(cell * rc.second, cell * rc.first)
                                .size(cell)
                                .padding(2.dp)
                                .graphicsLayer { scaleX = sc; scaleY = sc }
                                .clip(RoundedCornerShape(8.dp))
                                .background(bg)
                                .border(1.dp, if (shown) accent else NeonBorder, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (shown) Text(chr.toString(), color = OnNeon, fontWeight = FontWeight.ExtraBold, fontSize = (cell.value * 0.5f).sp)
                        }
                    }
                }
            }
            // ---------- Seçilen kelime önizlemesi ----------
            val live = sel.joinToString("") { lv.letters[order[it]].toString() }
            Box(Modifier.fillMaxWidth().height(44.dp), contentAlignment = Alignment.Center) {
                val t = if (live.isNotEmpty()) live else preview
                if (t.isNotEmpty()) {
                    Text(
                        t, color = if (live.isNotEmpty()) TextPrimary else previewColor, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.shake(shakeKey, 8.dp).clip(RoundedCornerShape(12.dp)).background(NeonCard.copy(alpha = 0.8f)).padding(horizontal = 14.dp, vertical = 2.dp)
                    )
                }
            }
            // ---------- Harf çarkı + düğmeler ----------
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    RoundBtn("🔀", "Karıştır") { order = order.shuffled(); soundManager.playTap() }
                    RoundBtn("⭐", "${bonusFound.size}") { fx.float(if (bonusFound.isEmpty()) "Izgarada olmayan kelimeler bonus sayılır" else bonusFound.joinToString(", "), NeonYellow, 0.5f, 0.5f) }
                }
                BoxWithConstraints(Modifier.weight(1f).height(280.dp), contentAlignment = Alignment.Center) {
                    val s = min(maxWidth, maxHeight)
                    val density = LocalDensity.current
                    val sPx = with(density) { s.toPx() }
                    val n = lv.letters.length
                    val radius = sPx * 0.34f
                    val bubble = sPx * (if (n >= 7) 0.1f else 0.12f)
                    val centers = remember(n, sPx) {
                        List(n) { i ->
                            val a = -Math.PI / 2 + i * 2 * Math.PI / n
                            Offset(sPx / 2 + radius * cos(a).toFloat(), sPx / 2 + radius * sin(a).toFloat())
                        }
                    }
                    fun hit(p: Offset): Int? = centers.indices.firstOrNull { hypot(p.x - centers[it].x, p.y - centers[it].y) < bubble * 1.25f }
                    Box(
                        Modifier
                            .size(s)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.06f))
                            .border(1.5.dp, accent.copy(alpha = 0.5f), CircleShape)
                            .pointerInput(order, complete) {
                                detectDragGestures(
                                    onDragStart = { p -> if (!complete) { sel.clear(); hit(p)?.let { sel += it; soundManager.playTap() }; finger = p } },
                                    onDrag = { change, _ ->
                                        val p = change.position
                                        finger = p
                                        val h = hit(p)
                                        if (h != null) {
                                            if (sel.isEmpty()) { sel += h; soundManager.playTap() }
                                            else if (h !in sel) { sel += h; soundManager.playPad(sel.size % 9) }
                                            else if (sel.size >= 2 && sel[sel.size - 2] == h) sel.removeAt(sel.size - 1)
                                        }
                                    },
                                    onDragEnd = { submit() },
                                    onDragCancel = { submit() }
                                )
                            }
                    ) {
                        Canvas(Modifier.fillMaxSize()) {
                            for (k in 1 until sel.size) drawLine(accent, centers[sel[k - 1]], centers[sel[k]], strokeWidth = bubble * 0.32f, cap = StrokeCap.Round)
                            val f = finger
                            if (f != null && sel.isNotEmpty()) drawLine(accent.copy(alpha = 0.6f), centers[sel.last()], f, strokeWidth = bubble * 0.25f, cap = StrokeCap.Round)
                        }
                        for (i in 0 until n) {
                            val c = centers[i]
                            val on = i in sel
                            val bubbleDp = with(density) { (bubble * 2).toDp() }
                            Box(
                                Modifier
                                    .offset { IntOffset((c.x - bubble).toInt(), (c.y - bubble).toInt()) }
                                    .size(bubbleDp)
                                    .clip(CircleShape)
                                    .background(if (on) accent else NeonBg.copy(alpha = 0.85f))
                                    .border(2.dp, accent, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(lv.letters[order[i]].toString(), color = if (on) OnNeon else TextPrimary, fontSize = (bubble / density.density * 0.95f).sp, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    RoundBtn("💡", if (hints > 0) "$hints" else "25💎") { revealLetter() }
                    RoundBtn("🗺️", "Harita") { onBack() }
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        // ---------- Seviye bitti ----------
        AnimatedVisibility(complete, enter = fadeIn() + scaleIn(initialScale = 0.85f), exit = fadeOut()) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f)), contentAlignment = Alignment.Center) {
                GlassCard(accent = accent, glow = true, modifier = Modifier.padding(28.dp)) {
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(ch.emoji, fontSize = 52.sp)
                        Text("Harika!", color = TextPrimary, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                        Text("Seviye $level tamamlandı", color = TextSecondary)
                        if (reward > 0) {
                            Spacer(Modifier.height(8.dp))
                            Text("+$reward 💎", color = NeonCyan, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        }
                        if (level % WowData.PER_CHAPTER == 0 && level < WowData.LEVEL_COUNT) {
                            Spacer(Modifier.height(6.dp))
                            Text("${ch.name} durağı bitti! Sıradaki: ${chapters[lv.chapter + 1].emoji} ${chapters[lv.chapter + 1].name}", color = NeonYellow, textAlign = TextAlign.Center)
                        }
                        Spacer(Modifier.height(16.dp))
                        if (level < WowData.LEVEL_COUNT) NeonButton("Sonraki seviye", { onNext(level + 1) }, Modifier.fillMaxWidth(), color = accent)
                        else Text("🎉 Kelime Diyarı'nın tamamını bitirdin!", color = NeonYellow, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(8.dp))
                        NeonButton("Haritaya dön", onBack, Modifier.fillMaxWidth(), color = accent, outlined = true)
                    }
                }
            }
        }
        FxOverlay(fx)
    }
}

@Composable
private fun DisposableEffectTheme(sound: SoundAndVibeManager) {
    androidx.compose.runtime.DisposableEffect(Unit) {
        sound.useGameTheme("word_scramble")
        onDispose { sound.useGameTheme(null) }
    }
}

@Composable
private fun RoundBtn(emoji: String, label: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(52.dp).clip(CircleShape).background(NeonCard).border(1.dp, NeonBorder, CircleShape).bounceClick(onClick = onClick),
            contentAlignment = Alignment.Center
        ) { Text(emoji, fontSize = 22.sp) }
        Text(label, color = TextSecondary, fontSize = 11.sp, maxLines = 1)
    }
}

/** Ana sayfa ve Oyunlar sayfasındaki büyük Kelime Diyarı kartı. */
@Composable
fun WowFeatureCard(repository: GameRepository, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val chapters = remember { WowData.load(context).first }
    val current by repository.preferences.wowLevel.collectAsState(initial = 1)
    val lvl = current.coerceAtMost(WowData.LEVEL_COUNT)
    val ch = chapters[((lvl - 1) / WowData.PER_CHAPTER).coerceIn(0, chapters.size - 1)]
    val (a, b) = ch.colors
    val shape = RoundedCornerShape(22.dp)
    Box(
        modifier
            .fillMaxWidth()
            .neonGlow(a, 16.dp, 22.dp, 0.45f)
            .clip(shape)
            .background(Brush.linearGradient(listOf(a.copy(alpha = 0.55f), b.copy(alpha = 0.25f), NeonBg)))
            .border(1.5.dp, a.copy(alpha = 0.8f), shape)
            .bounceClick(onClick = onClick)
            .padding(16.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("ÖNE ÇIKAN", color = OnNeon, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(NeonYellow).padding(horizontal = 6.dp, vertical = 2.dp))
                Spacer(Modifier.width(8.dp))
                Text("300 seviye · 15 durak", color = TextSecondary, fontSize = 12.sp)
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Kelime Diyarı", color = TextPrimary, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                    Text("${ch.emoji} ${ch.name} · Seviye $lvl", color = TextPrimary.copy(alpha = 0.85f), style = MaterialTheme.typography.bodyMedium)
                }
                // Mini harf çarkı
                Box(Modifier.size(64.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.1f)).border(1.dp, a, CircleShape)) {
                    val letters = "KELİME"
                    letters.forEachIndexed { i, c ->
                        val ang = -Math.PI / 2 + i * 2 * Math.PI / letters.length
                        Text(c.toString(), color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.align(Alignment.Center).offset((22 * cos(ang)).dp, (22 * sin(ang)).dp))
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            NeonProgressBar((lvl - 1) / WowData.LEVEL_COUNT.toFloat(), a, Modifier.fillMaxWidth())
        }
    }
}
