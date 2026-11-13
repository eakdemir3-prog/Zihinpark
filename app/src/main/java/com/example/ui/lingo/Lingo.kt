package com.example.ui.lingo

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import com.example.i18n.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import com.example.data.repository.GameRepository
import com.example.ui.components.DiamondPill
import com.example.ui.components.GlassCard
import com.example.ui.components.NeonBackground
import com.example.ui.components.NeonButton
import com.example.ui.components.NeonTopBar
import com.example.ui.components.bounceClick
import com.example.ui.components.neonGlow
import com.example.ui.components.shake
import com.example.ui.game.FxOverlay
import com.example.ui.game.rememberGameFx
import com.example.ui.theme.NeonBg
import com.example.ui.theme.NeonBorder
import com.example.ui.theme.NeonCard
import com.example.ui.theme.NeonCardHigh
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonOrange
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.NeonRed
import com.example.ui.theme.NeonYellow
import com.example.ui.theme.OnNeon
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.SoundAndVibeManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject

// =====================================================================
// Veri
// =====================================================================

object LingoData {
    @Volatile private var cache: Triple<List<String>, List<String>, Set<String>>? = null
    @Volatile private var cacheLang: String = ""

    fun load(context: Context): Triple<List<String>, List<String>, Set<String>> {
        val lang = if (com.example.i18n.I18n.isEn) "en" else "tr"
        if (cacheLang == lang) cache?.let { return it }
        synchronized(this) {
            if (cacheLang == lang) cache?.let { return it }
            val o = JSONObject(context.assets.open(if (lang == "en") "lingo_en.json" else "lingo.json").bufferedReader(Charsets.UTF_8).use { it.readText() })
            fun arr(k: String) = o.getJSONArray(k).let { a -> (0 until a.length()).map { a.getString(it) } }
            val a5 = arr("a5"); val a6 = arr("a6")
            val valid = (arr("valid") + a5 + a6).toHashSet()
            return Triple(a5, a6, valid).also { cache = it; cacheLang = lang }
        }
    }

    /** Seviye kelimesi: ilk 100 seviye 5 harf; sonra her 3 seviyede bir 6 harf; 250'den sonra çoğunlukla 6 harf. */
    fun wordForLevel(ctx: Context, level: Int): String {
        val (a5, a6, _) = load(ctx)
        val six = when {
            level <= 100 -> false
            level <= 250 -> level % 3 == 0
            else -> level % 3 != 0
        }
        val n6 = (1..level).count { l -> if (l <= 100) false else if (l <= 250) l % 3 == 0 else l % 3 != 0 }
        return if (six) a6[(n6 - 1) % a6.size] else a5[(level - n6 - 1) % a5.size]
    }

    fun triesForLevel(level: Int) = if (level <= 150) 6 else 5

    /** Günün kelimesi: herkes için aynı (tarih tabanlı), seviye kelimelerinin sonundan seçilir. */
    fun dailyWord(ctx: Context, epochDay: Long): String {
        val (a5, _, _) = load(ctx)
        val idx = Math.floorMod(epochDay * 7919L + 13L, a5.size.toLong()).toInt()
        return a5[a5.size - 1 - idx]
    }

    /** 2 = doğru yer, 1 = kelimede var, 0 = yok. Tekrarlı harfleri doğru sayar. */
    fun score(guess: String, answer: String): IntArray {
        val res = IntArray(guess.length)
        val left = HashMap<Char, Int>()
        for (i in answer.indices) {
            if (guess[i] == answer[i]) res[i] = 2 else left[answer[i]] = (left[answer[i]] ?: 0) + 1
        }
        for (i in guess.indices) {
            if (res[i] == 2) continue
            val c = left[guess[i]] ?: 0
            if (c > 0) { res[i] = 1; left[guess[i]] = c - 1 }
        }
        return res
    }
}

private val ROWS_TR = listOf("ERTYUIOPĞÜ", "ASDFGHJKLŞİ", "ZCVBNMÖÇ")
private val ROWS_EN = listOf("QWERTYUIOP", "ASDFGHJKL", "ZXCVBNM")
private val ROWS get() = if (com.example.i18n.I18n.isEn) ROWS_EN else ROWS_TR

// =====================================================================
// Ana sayfa / Oyunlar sayfasındaki öne çıkan kart
// =====================================================================

@Composable
fun LingoFeatureCard(repository: GameRepository, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val level by repository.preferences.lingoLevel.collectAsState(initial = 1)
    val daily by repository.preferences.lingoDaily.collectAsState(initial = null)
    val shape = RoundedCornerShape(22.dp)
    Box(
        modifier
            .fillMaxWidth()
            .neonGlow(NeonGreen, 16.dp, 22.dp, 0.35f)
            .clip(shape)
            .background(Brush.linearGradient(listOf(NeonGreen.copy(alpha = 0.45f), NeonYellow.copy(alpha = 0.22f), NeonCard)))
            .border(1.5.dp, NeonGreen.copy(alpha = 0.8f), shape)
            .bounceClick(onClick = onClick)
            .padding(16.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("ÖNE ÇIKAN", color = OnNeon, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(NeonGreen).padding(horizontal = 6.dp, vertical = 2.dp))
                Spacer(Modifier.width(8.dp))
                val d = daily
                Text(
                    if (d == null || !d.playedToday) "🗓️ Günün kelimesi seni bekliyor" else if (d.won) "✅ Günün kelimesi bulundu · 🔥 ${d.streak}" else "Yarın yeni kelime",
                    color = TextSecondary, fontSize = 12.sp
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Lingo", color = TextPrimary, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                    Text("5 harf · 6 deneme · Seviye $level", color = TextPrimary.copy(alpha = 0.85f), style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    listOf('L' to 2, 'İ' to 1, 'N' to 0, 'G' to 2, 'O' to 2).forEach { (c, s) -> MiniTile(c, s) }
                }
            }
        }
    }
}

@Composable
private fun MiniTile(c: Char, s: Int) {
    Box(
        Modifier.size(22.dp).clip(RoundedCornerShape(5.dp)).background(stateColor(s)),
        contentAlignment = Alignment.Center
    ) { Text(c.toString(), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold) }
}

private fun stateColor(s: Int): Color = when (s) {
    2 -> Color(0xFF1FA855)
    1 -> Color(0xFFE0A800)
    0 -> Color(0xFF6B6F86)
    else -> Color.Transparent
}

// =====================================================================
// Lingo merkezi: günün kelimesi + seviyeler
// =====================================================================

@Composable
fun LingoHubScreen(repository: GameRepository, onBack: () -> Unit, onPlayLevel: (Int) -> Unit, onPlayDaily: () -> Unit, onOpenShop: () -> Unit) {
    val prefs = repository.preferences
    val level by prefs.lingoLevel.collectAsState(initial = 1)
    val wins by prefs.lingoWins.collectAsState(initial = 0)
    val daily by prefs.lingoDaily.collectAsState(initial = null)
    val diamonds by prefs.diamondBalance.collectAsState(initial = 0)
    NeonBackground(accent = NeonGreen, accent2 = NeonYellow, pattern = com.example.ui.components.BgPatterns.LETTERS) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            NeonTopBar("Lingo", subtitle = "Kelimeyi bul, seriyi koru", onBack = onBack, trailing = { DiamondPill(diamonds, onOpenShop) })
            LazyColumn(contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 32.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                item {
                    val d = daily
                    GlassCard(accent = NeonYellow, glow = d?.playedToday != true) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🗓️", fontSize = 34.sp)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Günün Lingo'su", color = TextPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(
                                    when {
                                        d == null || !d.playedToday -> "Herkes için aynı kelime · +10 💎"
                                        d.won -> "Bugün ${d.tries}. denemede buldun! Yarın yine gel."
                                        else -> "Bugün bulamadın. Yarın yeni kelime!"
                                    }, color = TextSecondary, style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("🔥 ${d?.streak ?: 0}", color = NeonOrange, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                                Text("seri", color = TextMuted, fontSize = 11.sp)
                            }
                        }
                        if (d?.playedToday != true) {
                            Spacer(Modifier.height(12.dp))
                            NeonButton("Günün kelimesini çöz", onPlayDaily, Modifier.fillMaxWidth(), color = NeonYellow)
                        }
                    }
                }
                item {
                    GlassCard(accent = NeonGreen, glow = true) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("🎯", fontSize = 34.sp)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text("Seviye $level", color = TextPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(
                                    when {
                                        level <= 100 -> "5 harfli kelimeler · 6 deneme"
                                        level <= 150 -> "Araya 6 harfli kelimeler giriyor"
                                        else -> "Zor mod · 5 deneme"
                                    }, color = TextSecondary, style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Text("✅ $wins", color = NeonGreen, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(12.dp))
                        NeonButton("Oyna", { onPlayLevel(level) }, Modifier.fillMaxWidth(), color = NeonGreen)
                    }
                }
                item {
                    GlassCard(accent = NeonCyan) {
                        Text("Nasıl oynanır?", color = TextPrimary, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        listOf(
                            "İlk harf verilir; kelimeyi tahmin edip Gir'e bas.",
                            "🟩 Harf doğru yerde · 🟨 Kelimede var ama yeri yanlış · ⬛ Kelimede yok",
                            "Tahminler gerçek Türkçe kelime olmalı.",
                            "💡 İpucu bir harfin yerini gösterir."
                        ).forEach { Text("• $it", color = TextSecondary, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 2.dp)) }
                    }
                }
            }
        }
    }
}

// =====================================================================
// Oyun ekranı
// =====================================================================

/** [level] = 0 ise Günün Lingo'su oynanır. */
@Composable
fun LingoPlayScreen(
    level: Int,
    repository: GameRepository,
    soundManager: SoundAndVibeManager,
    onBack: () -> Unit,
    onNext: (Int) -> Unit,
    onOpenShop: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val prefs = repository.preferences
    val scope = rememberCoroutineScope()
    val fx = rememberGameFx()
    val isDaily = level == 0
    val today = remember { prefs.epochDayNow() }
    val answer = remember(level) { if (isDaily) LingoData.dailyWord(context, today) else LingoData.wordForLevel(context, level) }
    val n = answer.length
    val maxTries = if (isDaily) 6 else LingoData.triesForLevel(level)
    val valid = remember { LingoData.load(context).third }
    val diamonds by prefs.diamondBalance.collectAsState(initial = 0)
    val hints by prefs.hintsCount.collectAsState(initial = 0)

    val guesses = remember(level) { mutableStateListOf<String>() }
    val scores = remember(level) { mutableStateListOf<IntArray>() }
    var current by remember(level) { mutableStateOf(answer.first().toString()) }
    val revealed = remember(level) { mutableStateMapOf(0 to answer.first()) }
    val keyState = remember(level) { mutableStateMapOf<Char, Int>() }
    var shakeKey by remember { mutableIntStateOf(0) }
    var done by remember(level) { mutableStateOf<Boolean?>(null) } // true kazandı, false kaybetti
    var reward by remember(level) { mutableIntStateOf(0) }
    val flip = remember(level) { mutableStateMapOf<Int, Float>() }

    DisposableTheme(soundManager)

    fun finish(won: Boolean) {
        done = won
        scope.launch {
            if (won) {
                soundManager.playLevelWin(); fx.confetti()
                val r = if (isDaily) 10 else (if (guesses.size <= 3) 3 else 2)
                reward = r
                prefs.addDiamonds(r)
                com.example.cloud.CloudSync.addWeeklyScore(if (isDaily) 40 else 15 + (maxTries - guesses.size) * 5)
            } else soundManager.playLevelFail()
            if (isDaily) prefs.recordLingoDaily(won, guesses.size) else prefs.completeLingoLevel(level, won)
            com.example.cloud.CloudSync.scheduleBackup()
        }
    }

    fun submit() {
        if (done != null) return
        if (current.length < n) { shakeKey++; fx.float("Kelime $n harfli olmalı", NeonOrange); soundManager.playWrong(); return }
        if (current !in valid && current != answer) { shakeKey++; fx.float("Sözlükte yok", NeonOrange); soundManager.playWrong(); return }
        val sc = LingoData.score(current, answer)
        val row = guesses.size
        guesses += current; scores += sc
        current.forEachIndexed { i, c -> keyState[c] = maxOf(keyState[c] ?: -1, sc[i]) ; if (sc[i] == 2) revealed[i] = c }
        scope.launch {
            for (i in 0 until n) {
                val a = Animatable(0f)
                launch { a.animateTo(1f, tween(260)) { flip[row * 10 + i] = value } }
                soundManager.playPad(sc[i] * 3 + i % 3)
                delay(110)
            }
        }
        val won = current == answer
        current = answer.first().toString()
        if (won) scope.launch { delay(n * 110L + 300); finish(true) }
        else if (guesses.size >= maxTries) scope.launch { delay(n * 110L + 300); finish(false) }
    }

    fun type(c: Char) {
        if (done != null || current.length >= n) return
        current += c; soundManager.playTap()
    }

    fun backspace() {
        if (done != null || current.length <= 1) return
        current = current.dropLast(1)
    }

    fun useHint() {
        if (done != null) return
        val idx = (0 until n).firstOrNull { it !in revealed } ?: return
        scope.launch {
            val ok = if (hints > 0) prefs.useBooster("hint") else prefs.spendDiamonds(20)
            if (ok) { revealed[idx] = answer[idx]; fx.float("💡 ${idx + 1}. harf: ${answer[idx]}", NeonCyan); soundManager.playCorrect(); return@launch }
            val act = activity ?: return@launch
            com.example.monetize.Ads.showRewarded(act, prefs, onUnavailable = { fx.float("Yetersiz 💎 · $it", NeonRed) }) {
                revealed[idx] = answer[idx]; fx.float("💡 ${idx + 1}. harf: ${answer[idx]}", NeonCyan)
            }
        }
    }

    fun afterLevel(action: () -> Unit) {
        val act = activity
        if (act == null) action() else scope.launch { com.example.monetize.Ads.maybeInterstitialEvery(act, prefs, 3, action) }
    }

    NeonBackground(accent = NeonGreen, accent2 = NeonYellow, pattern = com.example.ui.components.BgPatterns.LETTERS) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            NeonTopBar(
                if (isDaily) "Günün Lingo'su" else "Lingo · Seviye $level",
                subtitle = "$n harf · ${maxTries - guesses.size} deneme kaldı",
                onBack = onBack,
                trailing = { DiamondPill(diamonds, onOpenShop) }
            )
            // ---- Izgara ----
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 20.dp), contentAlignment = Alignment.Center) {
                val cell = min(min((maxWidth - 6.dp * (n - 1)) / n, (maxHeight - 6.dp * (maxTries - 1)) / maxTries), 62.dp)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (r in 0 until maxTries) {
                        val isCur = r == guesses.size && done == null
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = if (isCur) Modifier.shake(shakeKey, 8.dp) else Modifier) {
                            for (i in 0 until n) {
                                val g = guesses.getOrNull(r)
                                val ch: Char? = when {
                                    g != null -> g[i]
                                    isCur -> current.getOrNull(i) ?: revealed[i]
                                    else -> null
                                }
                                val ghost = isCur && current.getOrNull(i) == null && revealed[i] != null
                                val s = scores.getOrNull(r)?.get(i) ?: -1
                                val f = flip[r * 10 + i] ?: if (g != null) 1f else 0f
                                val shown = if (g != null && f > 0.5f) s else -1
                                Box(
                                    Modifier
                                        .size(cell)
                                        .graphicsLayer { rotationX = if (g != null) (1f - kotlin.math.abs(f * 2 - 1)) * 90f else 0f }
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (shown >= 0) stateColor(shown) else NeonCard)
                                        .border(
                                            2.dp,
                                            when { shown >= 0 -> stateColor(shown); isCur && ch != null && !ghost -> NeonGreen; else -> NeonBorder },
                                            RoundedCornerShape(10.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (ch != null) Text(
                                        ch.toString(),
                                        color = if (shown >= 0) Color.White else if (ghost) TextMuted else TextPrimary,
                                        fontSize = with(androidx.compose.ui.platform.LocalDensity.current) { (cell * 0.5f).toSp() }, fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }
                    }
                }
            }
            // ---- Sonuç / klavye ----
            val d = done
            if (d != null) {
                GlassCard(accent = if (d) NeonGreen else NeonRed, glow = true, modifier = Modifier.padding(16.dp)) {
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        com.example.ui.components.Mascot(if (d) com.example.ui.components.MascotMood.Cheer else com.example.ui.components.MascotMood.Sad, 72.dp)
                        Text(if (d) "Harika! 🎉" else "Kelime: $answer", color = TextPrimary, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                        Text(if (d) "${guesses.size}. denemede buldun" + if (reward > 0) " · +$reward 💎" else "" else "Bu sefer olmadı, sıradakinde!", color = TextSecondary, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(12.dp))
                        if (isDaily) {
                            NeonButton("Sonucu paylaş", {
                                val grid = scores.joinToString("\n") { row -> row.joinToString("") { when (it) { 2 -> "🟩"; 1 -> "🟨"; else -> "⬛" } } }
                                val text = "ZihinPark Lingo ${if (d) guesses.size else "X"}/$maxTries\n$grid"
                                context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text), "Paylaş"))
                            }, Modifier.fillMaxWidth(), color = NeonYellow)
                            Spacer(Modifier.height(8.dp))
                            NeonButton("Lingo'ya dön", onBack, Modifier.fillMaxWidth(), color = NeonGreen, outlined = true)
                        } else {
                            NeonButton(if (d) "Sonraki kelime" else "Tekrar dene", { afterLevel { onNext(if (d) level + 1 else level) } }, Modifier.fillMaxWidth(), color = NeonGreen)
                            Spacer(Modifier.height(8.dp))
                            NeonButton("Lingo'ya dön", { afterLevel(onBack) }, Modifier.fillMaxWidth(), color = NeonGreen, outlined = true)
                        }
                    }
                }
            } else {
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.End) {
                    Text(
                        "💡 İpucu ${if (hints > 0) "($hints)" else "· 20💎"}", color = NeonCyan, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clip(RoundedCornerShape(10.dp)).bounceClick { useHint() }.padding(8.dp)
                    )
                }
                Column(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ROWS.forEachIndexed { ri, row ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally)) {
                            if (ri == 2) Key("GİR", 1.6f, NeonGreen) { submit() }
                            row.forEach { c ->
                                val st = keyState[c] ?: -1
                                Key(c.toString(), 1f, if (st >= 0) stateColor(st) else null) { type(c) }
                            }
                            if (ri == 2) Key("⌫", 1.4f, null) { backspace() }
                        }
                    }
                }
            }
        }
        FxOverlay(fx)
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.Key(label: String, weight: Float, bg: Color?, onClick: () -> Unit) {
    Box(
        Modifier
            .weight(weight)
            .height(50.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bg ?: NeonCardHigh)
            .border(1.dp, if (bg == null) NeonBorder else bg, RoundedCornerShape(8.dp))
            .bounceClick(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = if (bg != null) Color.White else TextPrimary, fontSize = if (label.length > 1) 13.sp else 17.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun DisposableTheme(sound: SoundAndVibeManager) {
    androidx.compose.runtime.DisposableEffect(Unit) {
        sound.useGameTheme("word_scramble")
        onDispose { sound.useGameTheme(null) }
    }
}
