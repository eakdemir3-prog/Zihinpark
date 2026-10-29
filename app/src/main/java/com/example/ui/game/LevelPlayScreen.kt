package com.example.ui.game

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.GameRepository
import com.example.data.repository.LevelOutcome
import com.example.domain.model.GameRegistry
import com.example.game.engine.LevelRules
import com.example.game.engine.RoundResult
import com.example.game.engine.RoundSpec
import com.example.ui.components.AnimatedCounter
import com.example.ui.components.GlassCard
import com.example.ui.components.NeonBackground
import com.example.ui.components.NeonButton
import com.example.ui.components.NeonChip
import com.example.ui.components.NeonProgressBar
import com.example.ui.components.bounceClick
import com.example.ui.components.neonGlow
import com.example.ui.components.shake
import com.example.ui.games.GameCatalog
import com.example.ui.theme.NeonBg
import com.example.ui.theme.NeonBorder
import com.example.ui.theme.NeonCard
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonOrange
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonRed
import com.example.ui.theme.NeonYellow
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.SoundAndVibeManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private enum class Phase { Playing, RoundEnd, Saving, Result }

/**
 * Tüm oyunların ortak seviye çerçevesi: turları sırayla oynatır, efektleri, süreyi,
 * canları, güçlendirmeleri yönetir; sonunda yıldızları hesaplayıp kaydeder.
 *
 * @param level 1..50; [LevelRules.ENDLESS_LEVEL] (0) sonsuz mod.
 */
@Composable
fun LevelPlayScreen(
    gameId: String,
    level: Int,
    repository: GameRepository,
    soundManager: SoundAndVibeManager,
    onExit: () -> Unit,
    onPlayLevel: (Int) -> Unit,
    onOpenShop: () -> Unit = {}
) {
    val activity = androidx.compose.ui.platform.LocalContext.current as? android.app.Activity
    val meta = remember(gameId) { GameRegistry.getGameById(gameId) }
    if (meta == null) {
        LaunchedEffect(Unit) { onExit() }
        return
    }
    val content = remember(gameId) { GameCatalog.contentFor(gameId) }
    val accent = meta.category.color
    val isEndless = level == LevelRules.ENDLESS_LEVEL
    val roundCount = LevelRules.roundsFor(level)
    val scope = rememberCoroutineScope()

    val fx = rememberGameFx()
    val ctl = remember { RoundControls(fx, soundManager) }
    TimerDriver(ctl)

    var round by remember { mutableIntStateOf(0) }
    val results = remember { mutableStateListOf<RoundResult>() }
    var phase by remember { mutableStateOf(Phase.Playing) }
    var lastRoundSuccess by remember { mutableStateOf(true) }
    var outcome by remember { mutableStateOf<LevelOutcome?>(null) }
    var endlessRecord by remember { mutableStateOf(false) }
    var showPause by remember { mutableStateOf(false) }
    var endlessBase by remember { mutableStateOf<Int?>(if (isEndless) null else 0) }
    val startedAt = remember { System.currentTimeMillis() }
    var shakeKey by remember { mutableIntStateOf(0) }

    val seenTutorials by repository.preferences.seenTutorials.collectAsState(initial = null)
    var showTutorial by remember { mutableStateOf(false) }
    // İlk tur, öğretici kapanana kadar başlamaz (sonradan menüden açılan öğretici turu sıfırlamaz)
    var started by remember { mutableStateOf(false) }
    LaunchedEffect(seenTutorials) {
        val seen = seenTutorials ?: return@LaunchedEffect
        if (!started && gameId !in seen) showTutorial = true
    }
    LaunchedEffect(seenTutorials, showTutorial) {
        if (seenTutorials != null && !showTutorial) started = true
    }

    val hints by repository.preferences.hintsCount.collectAsState(initial = 0)
    val extraTimes by repository.preferences.extraTimeCount.collectAsState(initial = 0)
    val undos by repository.preferences.undosCount.collectAsState(initial = 0)
    // Güç bitince açılan "reklam izle, 1 tane al" penceresi
    var refill by remember { mutableStateOf<String?>(null) }

    DisposableEffect(gameId) {
        soundManager.useGameTheme(gameId)
        onDispose { soundManager.useGameTheme(null) }
    }
    LaunchedEffect(Unit) {
        // Kalan turları arka planda hazırla
        launch(kotlinx.coroutines.Dispatchers.Default) {
            if (!isEndless) com.example.game.puzzles.AllGenerators.byId(gameId)?.let { com.example.game.engine.PuzzleBank.prefetch(it, level) }
        }
        repository.preferences.setLastPlayedGame(gameId)
        if (isEndless) endlessBase = repository.preferences.nextEndlessSeedBase()
    }
    LaunchedEffect(fx.wrongPulse) { if (fx.wrongPulse > 0) shakeKey++ }

    val shouldPause = showPause || showTutorial || phase != Phase.Playing || refill != null
    androidx.compose.runtime.SideEffect { ctl.paused = shouldPause }

    val totalScore = results.sumOf { it.points }
    val failedRounds = results.count { !it.success }
    val allowedFails = if (isEndless) 2 else roundCount - LevelRules.passThreshold(roundCount)

    fun finishLevel() {
        phase = Phase.Saving
        val duration = (System.currentTimeMillis() - startedAt) / 1000
        scope.launch {
            if (isEndless) {
                endlessRecord = repository.saveEndlessResult(gameId, results.count { it.success }, totalScore, duration)
                com.example.cloud.CloudSync.addWeeklyScore(totalScore)
                outcome = null
                soundManager.playLevelWin()
                fx.confetti()
            } else {
                val o = repository.saveLevelResult(gameId, level, results.toList(), duration)
                outcome = o
                // Lig puanı: geçilen seviyeler tam, geçilemeyenler yarım puan getirir
                com.example.cloud.CloudSync.addWeeklyScore(if (o.stars > 0) totalScore else totalScore / 2)
                if (o.stars > 0) { fx.confetti(); soundManager.playLevelWin() } else soundManager.playLevelFail()
            }
            phase = Phase.Result
        }
    }

    val onRoundFinished: (Boolean, Int) -> Unit = finished@{ success, quality ->
        if (phase != Phase.Playing) return@finished
        ctl.stopTimer()
        val zone = if (isEndless) 4 else LevelRules.zoneOf(level)
        val q = quality.coerceIn(0, 100)
        val points = if (success) {
            (((40 + q) * (1f + zone * 0.3f)).toInt()) + if (LevelRules.isBoss(level)) 40 else 0
        } else 0
        results += RoundResult(success, if (success) q else 0, points)
        lastRoundSuccess = success
        if (success) {
            soundManager.playRoundClear()
            fx.float("+$points", NeonYellow, 0.5f, 0.3f)
        }
        phase = Phase.RoundEnd
        scope.launch {
            delay(950)
            val fails = results.count { !it.success }
            val done = if (isEndless) fails > allowedFails else (results.size >= roundCount || fails > allowedFails)
            if (done) finishLevel() else {
                ctl.resetForRound()
                round++
                phase = Phase.Playing
            }
        }
    }

    BackHandler(enabled = phase == Phase.Playing || phase == Phase.RoundEnd) { showPause = true }

    NeonBackground(accent = accent, accent2 = NeonCyan) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // ---------- Üst bar ----------
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { showPause = true }) {
                    Icon(Icons.Default.Pause, contentDescription = "Duraklat", tint = TextPrimary)
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        "${meta.iconEmoji}  ${meta.name}",
                        style = MaterialTheme.typography.titleMedium, color = TextPrimary, maxLines = 1
                    )
                    val sub = if (isEndless) "Sonsuz Mod · ${results.count { it.success }} tur"
                    else "Seviye $level · ${LevelRules.ZONE_NAMES[LevelRules.zoneOf(level)]}" +
                            if (LevelRules.isBoss(level)) " · BOSS" else ""
                    Text(sub, style = MaterialTheme.typography.labelMedium, color = accent)
                }
                AnimatedCounter(totalScore, color = NeonYellow, style = MaterialTheme.typography.titleLarge, prefix = "⭐ ")
                Spacer(Modifier.width(10.dp))
            }

            // ---------- Tur göstergesi, süre, canlar ----------
            Column(Modifier.padding(horizontal = 16.dp)) {
                if (!isEndless) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        repeat(roundCount) { i ->
                            val r = results.getOrNull(i)
                            val color = when {
                                r?.success == true -> NeonGreen
                                r?.success == false -> NeonRed
                                i == round -> accent
                                else -> NeonBorder
                            }
                            Box(
                                Modifier
                                    .weight(1f)
                                    .height(6.dp)
                                    .then(if (i == round && r == null) Modifier.neonGlow(color, 6.dp, 50.dp, 0.8f) else Modifier)
                                    .clip(RoundedCornerShape(50))
                                    .background(color)
                            )
                        }
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Can: ", color = TextSecondary, style = MaterialTheme.typography.labelMedium)
                        repeat(allowedFails + 1) { i ->
                            Text(if (i < allowedFails + 1 - failedRounds) "💜" else "🖤", fontSize = 14.sp)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (isEndless) "Tur ${round + 1}" else "Tur ${round + 1} / $roundCount",
                        color = TextSecondary, style = MaterialTheme.typography.labelMedium
                    )
                    if (ctl.maxMistakes > 0) {
                        Spacer(Modifier.width(10.dp))
                        repeat(ctl.maxMistakes) { i ->
                            Text(if (i < ctl.maxMistakes - ctl.mistakes) "❤️" else "🤍", fontSize = 12.sp)
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    ctl.statusText?.let {
                        Text(it, color = TextPrimary, style = MaterialTheme.typography.labelMedium, maxLines = 1)
                    }
                }
                if (ctl.timeLimitMs > 0) {
                    Spacer(Modifier.height(6.dp))
                    val frac = ctl.timeLeftMs.toFloat() / ctl.timeLimitMs
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        NeonProgressBar(
                            frac,
                            color = when {
                                frac > 0.5f -> NeonCyan
                                frac > 0.2f -> NeonOrange
                                else -> NeonRed
                            },
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "${(ctl.timeLeftMs / 1000) + if (ctl.timeLeftMs % 1000 > 0) 1 else 0}s",
                            color = TextPrimary, style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.widthIn(min = 30.dp), textAlign = TextAlign.End
                        )
                    }
                }
            }

            // ---------- Oyun alanı ----------
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .shake(shakeKey, 8.dp)
            ) {
                val base = endlessBase
                if (base != null && started) {
                    AnimatedContent(
                        targetState = round,
                        transitionSpec = {
                            (slideInHorizontally(tween(380)) { it / 2 } + fadeIn(tween(380)) + scaleIn(initialScale = 0.92f))
                                .togetherWith(slideOutHorizontally(tween(320)) { -it / 2 } + fadeOut(tween(260)) + scaleOut(targetScale = 0.92f))
                        },
                        label = "round"
                    ) { r ->
                        val spec = remember(r, base) {
                            if (isEndless) RoundSpec(
                                gameId, level, r, Int.MAX_VALUE,
                                LevelRules.endlessDifficulty(r), isBoss = false, isEndless = true, index = base + r
                            ) else RoundSpec(
                                gameId, level, r, roundCount, LevelRules.difficulty(level, r),
                                LevelRules.isBoss(level), isEndless = false, index = LevelRules.globalRoundIndex(level, r)
                            )
                        }
                        Box(Modifier.fillMaxSize()) {
                            key(r) { content(spec, ctl, onRoundFinished) }
                        }
                    }
                }
                RoundBanner(round, if (isEndless) null else roundCount, accent, visibleKey = "$round-${showTutorial}")
                androidx.compose.animation.AnimatedVisibility(
                    visible = phase == Phase.RoundEnd,
                    enter = scaleIn(spring(Spring.DampingRatioMediumBouncy)) + fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    RoundEndBadge(lastRoundSuccess)
                }
            }

            // ---------- Güçlendirmeler ----------
            val hintAction = ctl.hint
            val undoAction = ctl.undo
            val hasTimer = ctl.timeLimitMs > 0
            if (phase == Phase.Playing && (hintAction != null || hasTimer || undoAction != null)) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                ) {
                    val bm = Modifier.weight(1f, fill = false)
                    if (hintAction != null) BoosterButton("💡", "İpucu", hints, bm) {
                        if (hints <= 0) { refill = "hint"; return@BoosterButton }
                        val msg = hintAction()
                        if (msg == null) { fx.float("Şu an ipucu gerekmiyor", NeonOrange) }
                        else scope.launch {
                            repository.preferences.useBooster("hint")
                            fx.float("💡 $msg", NeonCyan, 0.5f, 0.55f)
                            soundManager.playCorrect()
                        }
                    }
                    if (undoAction != null) BoosterButton("↩️", "Geri Al", undos, bm) {
                        if (undos <= 0) { refill = "undo"; return@BoosterButton }
                        if (!undoAction()) fx.float("Geri alınacak hamle yok", NeonOrange)
                        else scope.launch { repository.preferences.useBooster("undo"); soundManager.playTap() }
                    }
                    if (hasTimer) BoosterButton("⏱️", "+10 sn", extraTimes, bm) {
                        if (extraTimes <= 0) { refill = "time"; return@BoosterButton }
                        ctl.addTime(10); fx.float("⏱️ +10 saniye", NeonCyan); soundManager.playCorrect()
                        scope.launch { repository.preferences.useBooster("time") }
                    }
                }
            }
        }

        FxOverlay(fx)

        // ---------- Öğretici ----------
        if (showTutorial) {
            Dim {
                GlassCard(accent = accent, glow = true, modifier = Modifier.padding(24.dp)) {
                    Text("${meta.iconEmoji}  Nasıl Oynanır?", style = MaterialTheme.typography.headlineSmall, color = TextPrimary)
                    Spacer(Modifier.height(12.dp))
                    meta.howTo.forEachIndexed { i, s ->
                        Row(Modifier.padding(vertical = 5.dp)) {
                            Box(
                                Modifier.size(24.dp).clip(CircleShape).background(accent.copy(alpha = 0.2f))
                                    .border(1.dp, accent, CircleShape),
                                contentAlignment = Alignment.Center
                            ) { Text("${i + 1}", color = accent, fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                            Spacer(Modifier.width(10.dp))
                            Text(s, color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Seviyede ${if (isEndless) "sınırsız" else "$roundCount"} tur var. " +
                            if (isEndless) "3 tur kaçırınca oyun biter." else "Geçmek için en az ${LevelRules.passThreshold(roundCount)} tur başarmalısın.",
                        color = TextSecondary, style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(Modifier.height(16.dp))
                    NeonButton("Başla", onClick = {
                        showTutorial = false
                        scope.launch { repository.preferences.markTutorialSeen(gameId) }
                    }, color = accent, icon = Icons.Default.PlayArrow, modifier = Modifier.fillMaxWidth())
                }
            }
        }

        // ---------- Duraklatma ----------
        if (showPause && phase != Phase.Result) {
            Dim(onDismiss = { showPause = false }) {
                GlassCard(accent = accent, glow = true, modifier = Modifier.padding(32.dp)) {
                    Text("Duraklatıldı", style = MaterialTheme.typography.headlineSmall, color = TextPrimary,
                        modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                    Spacer(Modifier.height(16.dp))
                    NeonButton("Devam Et", { showPause = false }, color = accent, icon = Icons.Default.PlayArrow, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(10.dp))
                    NeonButton("Nasıl Oynanır?", { showPause = false; showTutorial = true }, color = NeonCyan, outlined = true, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(10.dp))
                    NeonButton("Yeniden Başlat", { showPause = false; onPlayLevel(level) }, color = NeonYellow, outlined = true, icon = Icons.Default.Refresh, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(10.dp))
                    NeonButton("Seviyelere Dön", { showPause = false; onExit() }, color = NeonPink, outlined = true, icon = Icons.Default.GridView, modifier = Modifier.fillMaxWidth())
                }
            }
        }

        // ---------- Sonuç ----------
        if (phase == Phase.Result) {
            Dim {
                if (isEndless) {
                    EndlessResultCard(
                        rounds = results.count { it.success },
                        score = totalScore,
                        record = endlessRecord,
                        accent = accent,
                        onRetry = { onPlayLevel(LevelRules.ENDLESS_LEVEL) },
                        onExit = onExit
                    )
                } else {
                    outcome?.let { o ->
                        val passedLevel = o.stars > 0
                        // Seviye sonundan çıkarken (kurallara uyarsa) seyrek geçiş reklamı
                        fun leave(action: () -> Unit) {
                            val act = activity
                            if (act == null) action()
                            else scope.launch { com.example.monetize.Ads.maybeInterstitial(act, repository.preferences, passedLevel, action) }
                        }
                        LevelResultCard(
                            level = level,
                            outcome = o,
                            results = results.toList(),
                            accent = accent,
                            sound = soundManager,
                            onNext = if (o.nextUnlocked) { { leave { onPlayLevel(level + 1) } } } else null,
                            nextLocked = o.stars > 0 && level < LevelRules.LEVEL_COUNT && !o.nextUnlocked,
                            onRetry = { leave { onPlayLevel(level) } },
                            onExit = { leave(onExit) },
                            extra = {
                                ResultExtras(o, passedLevel, level, repository, fx, soundManager, onOpenShop)
                            }
                        )
                    }
                }
            }
        }
    }

    DisposableEffect(Unit) { onDispose { ctl.stopTimer() } }
}

@Composable
private fun Dim(onDismiss: (() -> Unit)? = null, content: @Composable () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(NeonBg.copy(alpha = 0.82f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onDismiss?.invoke() },
        contentAlignment = Alignment.Center
    ) {
        Box(Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}) {
            content()
        }
    }
}

@Composable
private fun RoundBanner(round: Int, total: Int?, accent: Color, visibleKey: String) {
    val a = remember { Animatable(0f) }
    LaunchedEffect(visibleKey) {
        a.snapTo(0f)
        a.animateTo(1f, tween(1100))
    }
    val v = a.value
    if (v in 0.001f..0.999f) {
        val alpha = if (v < 0.2f) v / 0.2f else (1f - (v - 0.55f).coerceAtLeast(0f) / 0.45f)
        val scale = 0.7f + 0.3f * (v / 0.3f).coerceAtMost(1f) + v * 0.1f
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                if (total == null) "TUR ${round + 1}" else "TUR ${round + 1}/$total",
                color = accent,
                fontSize = 44.sp,
                fontWeight = FontWeight.ExtraBold,
                style = MaterialTheme.typography.displaySmall.copy(
                    shadow = androidx.compose.ui.graphics.Shadow(accent, blurRadius = 40f)
                ),
                modifier = Modifier.graphicsLayer { this.alpha = alpha.coerceIn(0f, 1f); scaleX = scale; scaleY = scale }
            )
        }
    }
}

@Composable
private fun RoundEndBadge(success: Boolean) {
    val color = if (success) NeonGreen else NeonRed
    Box(
        Modifier
            .neonGlow(color, 24.dp, 60.dp, 0.8f)
            .clip(CircleShape)
            .background(NeonCard)
            .border(2.dp, color, CircleShape)
            .size(120.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(if (success) "✓" else "✕", color = color, fontSize = 48.sp, fontWeight = FontWeight.ExtraBold)
            Text(if (success) "Harika!" else "Olmadı", color = TextPrimary, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun BoosterButton(emoji: String, label: String, count: Int, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    val c = if (count > 0) NeonCyan else TextMuted
    Box(modifier.padding(top = 6.dp, end = 6.dp)) {
        Row(
            Modifier
                .height(44.dp)
                .clip(shape)
                .background(NeonCard)
                .border(1.dp, NeonBorder, shape)
                .bounceClick(onClick = onClick)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(emoji, fontSize = 15.sp)
            Spacer(Modifier.width(5.dp))
            Text(
                label, color = TextPrimary, style = MaterialTheme.typography.labelMedium,
                maxLines = 1, softWrap = false
            )
        }
        // Adet rozeti: butonun sağ üst köşesinde
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .offset(x = 6.dp, y = (-6).dp)
                .size(22.dp)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .background(NeonBg)
                .border(1.5.dp, c, androidx.compose.foundation.shape.CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("$count", color = c, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}

@Composable
private fun LevelResultCard(
    level: Int,
    outcome: LevelOutcome,
    results: List<RoundResult>,
    accent: Color,
    sound: SoundAndVibeManager,
    onNext: (() -> Unit)?,
    nextLocked: Boolean,
    onRetry: () -> Unit,
    onExit: () -> Unit,
    extra: @Composable () -> Unit = {}
) {
    val passed = outcome.stars > 0
    GlassCard(accent = if (passed) accent else NeonRed, glow = true, modifier = Modifier.padding(20.dp)) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                if (passed) (if (LevelRules.isBoss(level)) "BOSS YENİLDİ!" else "SEVİYE $level TAMAM") else "SEVİYE GEÇİLEMEDİ",
                style = MaterialTheme.typography.headlineSmall,
                color = if (passed) TextPrimary else NeonRed,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(14.dp))
            AnimatedStars(outcome.stars, sound)
            Spacer(Modifier.height(10.dp))
            Text(
                "${results.count { it.success }} / ${results.size} tur başarılı",
                color = TextSecondary, style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ResultStat("Puan", outcome.score, NeonYellow)
                if (outcome.diamonds > 0) ResultStat("Elmas", outcome.diamonds, NeonCyan, prefix = "+💎 ")
            }
            Spacer(Modifier.height(10.dp))
            if (outcome.newBest && outcome.previousStars > 0) NeonChip("YENİ REKOR", NeonPink, filled = true)
            if (outcome.perfectBonus) {
                Spacer(Modifier.height(6.dp)); NeonChip("3 yıldız bonusu +1 💎", NeonCyan)
            }
            outcome.unlockedZone?.let {
                Spacer(Modifier.height(8.dp))
                NeonChip("${LevelRules.ZONE_EMOJI[it]} ${LevelRules.ZONE_NAMES[it]} bölgesi açıldı!", NeonGreen, filled = true)
            }
            if (outcome.endlessUnlocked) {
                Spacer(Modifier.height(8.dp)); NeonChip("♾️ Sonsuz Mod açıldı!", NeonPink, filled = true)
            }
            if (!passed) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Geçmek için en az ${LevelRules.passThreshold(results.size.coerceAtLeast(LevelRules.roundsFor(level)))} turu başarmalısın.",
                    color = TextSecondary, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center
                )
            } else if (nextLocked) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Sonraki bölge için bu bölgede ${LevelRules.ZONE_UNLOCK_STARS} ⭐ topla.",
                    color = NeonOrange, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center
                )
            } else if (!outcome.firstClear && outcome.diamonds == 0) {
                Spacer(Modifier.height(8.dp))
                Text("Elmas yalnızca ilk geçişte verilir.", color = TextMuted, style = MaterialTheme.typography.bodySmall)
            }
            extra()
            Spacer(Modifier.height(18.dp))
            if (passed && onNext != null && !nextLocked) {
                NeonButton("Sonraki Seviye", onNext, color = accent, icon = Icons.Default.SkipNext, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(10.dp))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                NeonButton("Tekrar", onRetry, color = NeonYellow, outlined = true, icon = Icons.Default.Refresh, modifier = Modifier.weight(1f))
                NeonButton("Seviyeler", onExit, color = NeonPink, outlined = true, icon = Icons.Default.GridView, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun EndlessResultCard(rounds: Int, score: Int, record: Boolean, accent: Color, onRetry: () -> Unit, onExit: () -> Unit) {
    GlassCard(accent = accent, glow = true, modifier = Modifier.padding(20.dp)) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("♾️ Sonsuz Mod Bitti", style = MaterialTheme.typography.headlineSmall, color = TextPrimary)
            Spacer(Modifier.height(14.dp))
            AnimatedCounter(rounds, style = MaterialTheme.typography.displayMedium, color = accent, suffix = " tur")
            Spacer(Modifier.height(8.dp))
            ResultStat("Puan", score, NeonYellow)
            if (record) {
                Spacer(Modifier.height(10.dp)); NeonChip("YENİ REKOR!", NeonPink, filled = true)
            }
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                NeonButton("Tekrar", onRetry, color = accent, icon = Icons.Default.Refresh, modifier = Modifier.weight(1f))
                NeonButton("Çık", onExit, color = NeonPink, outlined = true, icon = Icons.Default.GridView, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ResultStat(label: String, value: Int, color: Color, prefix: String = "") {
    val shape = RoundedCornerShape(14.dp)
    Column(
        Modifier
            .clip(shape)
            .background(color.copy(alpha = 0.08f))
            .border(1.dp, color.copy(alpha = 0.4f), shape)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, color = TextSecondary, style = MaterialTheme.typography.labelSmall)
        AnimatedCounter(value, color = color, style = MaterialTheme.typography.titleLarge, prefix = prefix)
    }
}

/** Yıldızlar sırayla dolar, her biri yaylanarak büyür. */
@Composable
fun AnimatedStars(stars: Int, sound: SoundAndVibeManager?) {
    val scales = remember { List(3) { Animatable(0f) } }
    LaunchedEffect(stars) {
        delay(250)
        for (i in 0 until 3) {
            if (i < stars) {
                sound?.playStar(i)
                scales[i].animateTo(1f, spring(dampingRatio = 0.35f, stiffness = 300f))
            } else {
                scales[i].animateTo(1f, tween(200))
            }
            delay(120)
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
        for (i in 0 until 3) {
            val filled = i < stars
            val size = if (i == 1) 64 else 52
            Box(
                Modifier
                    .size(size.dp)
                    .graphicsLayer {
                        scaleX = scales[i].value; scaleY = scales[i].value
                        rotationZ = (1f - scales[i].value) * -90f
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "★",
                    fontSize = (size * 0.9f).sp,
                    color = if (filled) NeonYellow else NeonBorder,
                    style = MaterialTheme.typography.displaySmall.copy(
                        shadow = if (filled) androidx.compose.ui.graphics.Shadow(NeonYellow, blurRadius = 30f) else null
                    )
                )
            }
        }
    }
}



/** Güç bittiğinde: ödüllü reklamla 1 tane kazan ya da mağazaya git. */
@Composable
private fun BoosterRefillDialog(
    type: String,
    repository: GameRepository,
    fx: GameFxState,
    sound: SoundAndVibeManager,
    onOpenShop: () -> Unit,
    onClose: () -> Unit
) {
    val activity = androidx.compose.ui.platform.LocalContext.current as? android.app.Activity
    val scope = rememberCoroutineScope()
    val left by repository.preferences.rewardedLeftToday.collectAsState(initial = 0)
    val name = when (type) { "hint" -> "İpucu"; "undo" -> "Geri Al"; else -> "+10 sn" }
    val emoji = when (type) { "hint" -> "💡"; "undo" -> "↩️"; else -> "⏱️" }
    com.example.ui.screens.profile.NeonDialog(
        title = "$emoji $name kalmadı",
        onDismiss = onClose,
        confirm = if (left > 0) "▶ Reklam izle, 1 $name al" else "Mağazaya git",
        onConfirm = {
            if (left <= 0 || activity == null) { onOpenShop(); return@NeonDialog }
            scope.launch {
                com.example.monetize.Ads.showRewarded(activity, repository.preferences,
                    onUnavailable = { fx.float(it, NeonOrange); onClose() }) {
                    scope.launch {
                        repository.preferences.addBooster(type, 1)
                        fx.float("$emoji +1 $name", NeonGreen); sound.playCoin()
                    }
                    onClose()
                }
            }
        }
    ) {
        Column {
            Text("Kısa bir reklam izleyerek 1 $name kazanabilirsin. (Bugün kalan: $left)", color = TextSecondary)
            Spacer(Modifier.height(10.dp))
            Text("Mağazaya göz at →", color = NeonCyan, modifier = Modifier.bounceClick(onClick = onOpenShop))
        }
    }
}

/** Sonuç kartının ek bölümü: ödülü 2× yapma (ödüllü reklam) ve seyrek PRO önerisi. */
@Composable
private fun ResultExtras(
    o: LevelOutcome,
    passed: Boolean,
    level: Int,
    repository: GameRepository,
    fx: GameFxState,
    sound: SoundAndVibeManager,
    onOpenShop: () -> Unit
) {
    val activity = androidx.compose.ui.platform.LocalContext.current as? android.app.Activity
    val scope = rememberCoroutineScope()
    val isPro by repository.preferences.isProMember.collectAsState(initial = true)
    val left by repository.preferences.rewardedLeftToday.collectAsState(initial = 0)
    var doubled by remember { mutableStateOf(false) }
    var showPro by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        // PRO önerisi: yalnızca geçilen seviyede, 6. seviyeden sonra, birkaç seviyede bir ve 2 günde en fazla bir kez
        if (passed && level >= 6 && level % 4 == 0 && !repository.preferences.isProMember.first() && repository.preferences.tryProPrompt()) showPro = true
    }
    if (passed && o.diamonds > 0 && !doubled && left > 0 && activity != null) {
        Spacer(Modifier.height(10.dp))
        NeonButton("▶ Reklam izle, ödülü 2× yap (+${o.diamonds} 💎)", {
            scope.launch {
                com.example.monetize.Ads.showRewarded(activity, repository.preferences, onUnavailable = { fx.float(it, NeonOrange) }) {
                    doubled = true
                    scope.launch { repository.preferences.addDiamonds(o.diamonds); sound.playCoin(); fx.float("+${o.diamonds} 💎", NeonCyan) }
                }
            }
        }, color = NeonCyan, outlined = true, height = 44.dp, modifier = Modifier.fillMaxWidth())
    }
    if (showPro && !isPro) {
        Spacer(Modifier.height(10.dp))
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(NeonCard).border(1.dp, NeonYellow.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
                .bounceClick(onClick = onOpenShop).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("👑", fontSize = 22.sp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("PRO ile reklamsız oyna", color = TextPrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                Text("+500 💎 · günlük hediye 2×", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
            }
            Text("✕", color = TextMuted, modifier = Modifier.padding(start = 8.dp).bounceClick { showPro = false })
        }
    }
}
