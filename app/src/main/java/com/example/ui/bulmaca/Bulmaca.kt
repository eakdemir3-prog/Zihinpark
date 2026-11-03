package com.example.ui.bulmaca

import android.app.Activity
import android.content.Context
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.example.ui.components.NeonProgressBar
import com.example.ui.components.NeonTopBar
import com.example.ui.components.bounceClick
import com.example.ui.components.neonGlow
import com.example.ui.components.shake
import com.example.ui.game.FxOverlay
import com.example.ui.game.rememberGameFx
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.json.JSONObject

// =====================================================================
// Veri (assets/bulmaca.json, tools/bulmaca/build.py ile üretilir)
// =====================================================================

data class BdRow(val clue: String, val answer: String, val pos: Int)
data class BdPuzzle(val world: Int, val index: Int, val secret: String, val secretClue: String, val len: Int, val rows: List<BdRow>)
data class BdWorld(val key: String, val name: String, val emoji: String, val puzzles: List<BdPuzzle>)

object BulmacaData {
    @Volatile private var cache: List<BdWorld>? = null

    fun load(ctx: Context): List<BdWorld> {
        cache?.let { return it }
        synchronized(this) {
            cache?.let { return it }
            val root = JSONObject(ctx.assets.open("bulmaca.json").bufferedReader(Charsets.UTF_8).use { it.readText() })
            val ws = root.getJSONArray("worlds")
            val list = (0 until ws.length()).map { wi ->
                val w = ws.getJSONObject(wi)
                val ps = w.getJSONArray("puzzles")
                BdWorld(w.getString("key"), w.getString("name"), w.getString("emoji"), (0 until ps.length()).map { pi ->
                    val p = ps.getJSONObject(pi)
                    val rs = p.getJSONArray("rows")
                    BdPuzzle(wi, pi, p.getString("secret"), p.getString("secretClue"), p.optInt("len", 0), (0 until rs.length()).map { ri ->
                        val r = rs.getJSONObject(ri)
                        BdRow(r.getString("c"), r.getString("a"), r.getInt("p"))
                    })
                })
            }
            return list.also { cache = it }
        }
    }

    fun all(ctx: Context): List<BdPuzzle> = load(ctx).flatMap { it.puzzles }
    fun count(ctx: Context) = all(ctx).size
}

private val WORLD_COLORS = listOf(
    Color(0xFFB8860B), Color(0xFF1E88E5), Color(0xFFF57C00), Color(0xFFE53935), Color(0xFF00897B),
    Color(0xFF43A047), Color(0xFF8E24AA), Color(0xFF3949AB), Color(0xFF2E7D32), Color(0xFFD81B60)
)
fun worldColor(i: Int) = WORLD_COLORS[i % WORLD_COLORS.size]

// =====================================================================
// Öne çıkan kart
// =====================================================================

@Composable
fun BulmacaFeatureCard(repository: GameRepository, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val worlds = remember { BulmacaData.load(context) }
    val level by repository.preferences.bdLevel.collectAsState(initial = 1)
    val total = worlds.sumOf { it.puzzles.size }
    val lv = level.coerceAtMost(total)
    val wi = ((lv - 1) / 20).coerceIn(0, worlds.size - 1)
    val w = worlds[wi]
    val shape = RoundedCornerShape(22.dp)
    Box(
        modifier.fillMaxWidth()
            .neonGlow(NeonPurple, 16.dp, 22.dp, 0.35f)
            .clip(shape)
            .background(Brush.linearGradient(listOf(NeonPurple.copy(alpha = 0.5f), NeonCyan.copy(alpha = 0.22f), NeonCard)))
            .border(1.5.dp, NeonPurple.copy(alpha = 0.8f), shape)
            .bounceClick(onClick = onClick)
            .padding(16.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("YENİ", color = OnNeon, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(NeonPink).padding(horizontal = 6.dp, vertical = 2.dp))
                Spacer(Modifier.width(8.dp))
                Text("$total bulmaca · ${worlds.size} dünya", color = TextSecondary, fontSize = 12.sp)
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Bulmaca Dünyası", color = TextPrimary, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                    Text("${w.emoji} ${w.name} · Bulmaca $lv", color = TextPrimary.copy(alpha = 0.85f), style = MaterialTheme.typography.bodyMedium)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    listOf('S', 'O', 'R').forEachIndexed { i, ch ->
                        Box(
                            Modifier.size(22.dp).clip(RoundedCornerShape(5.dp)).background(if (i == 1) NeonPink.copy(alpha = 0.35f) else NeonCard)
                                .border(1.dp, if (i == 1) NeonPink else NeonBorder, RoundedCornerShape(5.dp)),
                            contentAlignment = Alignment.Center
                        ) { Text(ch.toString(), color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1) }
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            NeonProgressBar((lv - 1f) / total, NeonPurple, Modifier.fillMaxWidth())
        }
    }
}

// =====================================================================
// Dünya haritası
// =====================================================================

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BulmacaMapScreen(repository: GameRepository, onBack: () -> Unit, onPlay: (Int) -> Unit, onOpenShop: () -> Unit) {
    val context = LocalContext.current
    val worlds = remember { BulmacaData.load(context) }
    val total = worlds.sumOf { it.puzzles.size }
    val level by repository.preferences.bdLevel.collectAsState(initial = 1)
    val diamonds by repository.preferences.diamondBalance.collectAsState(initial = 0)
    val cur = level.coerceAtMost(total)
    val curWorld = ((cur - 1) / 20).coerceIn(0, worlds.size - 1)
    val listState = rememberLazyListState()
    LaunchedEffect(curWorld) { if (curWorld > 0) listState.scrollToItem(curWorld) }

    NeonBackground(accent = NeonPurple, accent2 = NeonCyan, pattern = com.example.ui.components.BgPatterns.LETTERS) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            NeonTopBar("Bulmaca Dünyası", subtitle = "$total bulmaca · ${worlds.size} dünya", onBack = onBack, trailing = { DiamondPill(diamonds, onOpenShop) })
            LazyColumn(state = listState, contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 32.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                item {
                    val w = worlds[curWorld]
                    GlassCard(accent = worldColor(curWorld), glow = true) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(w.emoji, fontSize = 38.sp)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(if (level > total) "Hepsini bitirdin! 🎉" else "${w.name} · Bulmaca $cur", color = TextPrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                Text("Soruları çöz, pembe kutulardaki gizli kelimeyi bul.", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        NeonButton("Oyna", { onPlay(cur) }, Modifier.fillMaxWidth(), color = worldColor(curWorld))
                    }
                }
                itemsIndexed(worlds) { wi, w ->
                    val first = wi * 20 + 1
                    val locked = first > level
                    val solved = (level - first).coerceIn(0, w.puzzles.size)
                    GlassCard(accent = if (locked) NeonBorder else worldColor(wi)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(46.dp).clip(RoundedCornerShape(14.dp)).background(worldColor(wi).copy(alpha = if (locked) 0.15f else 0.3f)), contentAlignment = Alignment.Center) {
                                Text(if (locked) "🔒" else w.emoji, fontSize = 22.sp)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text("${wi + 1}. ${w.name}", color = TextPrimary, fontWeight = FontWeight.Bold)
                                Text("$solved/${w.puzzles.size} bulmaca", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                            }
                            if (solved == w.puzzles.size) Text("✅", fontSize = 20.sp)
                        }
                        if (!locked) {
                            Spacer(Modifier.height(10.dp))
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                for (k in 0 until w.puzzles.size) {
                                    val lv = first + k
                                    val st = when { lv < level -> 2; lv == level -> 1; else -> 0 }
                                    val c = when (st) { 2 -> NeonGreen; 1 -> worldColor(wi); else -> NeonBorder }
                                    Box(
                                        Modifier.size(34.dp).clip(CircleShape).background(if (st == 1) c else NeonCard).border(1.dp, c, CircleShape)
                                            .bounceClick(enabled = st > 0) { if (st > 0) onPlay(lv) },
                                        contentAlignment = Alignment.Center
                                    ) { Text("${k + 1}", color = if (st == 1) Color.White else if (st == 2) NeonGreen else TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
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
// Oyun ekranı (CodyCross tarzı)
// =====================================================================

private val KB = listOf("QWERTYUIOPĞÜ", "ASDFGHJKLŞİ", "ZXCVBNMÖÇ")

/** Dünyaların arka plan fotoğrafları (drawable adı). */
private val WORLD_BG = listOf(
    "wow_bg_efes", "wow_bg_kapadokya", "jig_animal_04", "jig_food_06", "bd_bg_bilim",
    "bd_bg_spor", "bd_bg_sanat", "jig_wonder_07", "jig_forest_05", "bd_bg_kultur"
)

@Composable
fun BulmacaPlayScreen(level: Int, repository: GameRepository, soundManager: SoundAndVibeManager, onBack: () -> Unit, onNext: (Int) -> Unit, onOpenShop: () -> Unit) {
    val context = LocalContext.current
    val activity = context as? Activity
    val prefs = repository.preferences
    val scope = rememberCoroutineScope()
    val fx = rememberGameFx()
    val density = androidx.compose.ui.platform.LocalDensity.current
    val all = remember { BulmacaData.all(context) }
    val worlds = remember { BulmacaData.load(context) }
    val pz = all[(level - 1).coerceIn(0, all.size - 1)]
    val world = worlds[pz.world]
    val accent = worldColor(pz.world)
    val diamonds by prefs.diamondBalance.collectAsState(initial = 0)
    val hints by prefs.hintsCount.collectAsState(initial = 0)
    val n = pz.rows.size
    val len = pz.rows.maxOf { it.answer.length }
    val secretRows = remember(level) { pz.rows.indices.filter { pz.rows[it].pos >= 0 } }

    val solved = remember(level) { mutableStateListOf<Int>() }
    val revealed = remember(level) { mutableStateMapOf<String, Boolean>() } // "r,i"
    val fresh = remember(level) { mutableStateMapOf<String, Boolean>() }
    val typed = remember(level) { mutableStateMapOf<Int, String>() }
    var sel by remember(level) { mutableIntStateOf(0) }
    var shakeKey by remember { mutableIntStateOf(0) }
    var done by remember(level) { mutableStateOf(false) }
    var loaded by remember(level) { mutableStateOf(false) }
    var reward by remember(level) { mutableIntStateOf(0) }
    val rnd = remember(level) { java.util.Random(level * 7919L + 17) }

    androidx.compose.runtime.DisposableEffect(Unit) {
        soundManager.useGameTheme("cipher_breaker"); onDispose { soundManager.useGameTheme(null) }
    }

    fun persist() = scope.launch { prefs.saveBd(level, solved.map { "$it" }.toSet(), revealed.keys.toSet()) }
    fun isRevealed(r: Int, i: Int) = r in solved || revealed["$r,$i"] == true
    fun openCells(r: Int) = pz.rows[r].answer.indices.filter { !isRevealed(r, it) }
    fun prevOpenRow(from: Int) = ((from - 1 downTo 0) + (n - 1 downTo from)).firstOrNull { it !in solved }
    fun nextOpenRow(from: Int) = (pz.rows.indices.drop(from + 1) + pz.rows.indices.take(from + 1)).firstOrNull { it !in solved }

    LaunchedEffect(level) {
        val (sl, rows, rev) = prefs.bdSave.first()
        if (sl == level && (rows.isNotEmpty() || rev.isNotEmpty())) {
            rows.mapNotNull { it.toIntOrNull() }.filter { it in pz.rows.indices }.forEach { solved += it }
            rev.filter { k -> k.split(",").let { it.size == 2 && (it[0].toIntOrNull() ?: n) < n && (it[1].toIntOrNull() ?: len) < len } }.forEach { revealed[it] = true }
        } else {
            // Başlangıçta rastgele birkaç harf açık gelir
            val cells = pz.rows.indices.flatMap { r -> pz.rows[r].answer.indices.map { r to it } }.shuffled(rnd)
            val count = (n * len * 0.09f).toInt().coerceAtLeast(n / 2)
            val perRow = HashMap<Int, Int>()
            for ((r, i) in cells) {
                if (revealed.size >= count) break
                if ((perRow[r] ?: 0) >= 1 || i == pz.rows[r].pos) continue
                perRow[r] = (perRow[r] ?: 0) + 1; revealed["$r,$i"] = true
            }
            persist()
        }
        sel = pz.rows.indices.firstOrNull { it !in solved } ?: 0
        loaded = true
        if (solved.size == n) done = true
    }

    fun finishPuzzle() {
        done = true
        soundManager.playLevelWin(); fx.confetti()
        scope.launch {
            val first = prefs.completeBd(level)
            if (first) {
                val r = 5 + if (level % 20 == 0) 25 else 0
                reward = r; prefs.addDiamonds(r)
                com.example.cloud.CloudSync.addWeeklyScore(25 + n * 3)
                com.example.cloud.CloudSync.scheduleBackup()
            }
        }
    }

    /** Tamamen açılan satırları çözülmüş say. */
    fun settleRows() {
        for (r in pz.rows.indices) if (r !in solved && openCells(r).isEmpty()) { solved += r; typed.remove(r) }
    }

    /** Bir kelime bulununca diğer satırlara rastgele harf düşer. */
    fun scatter(from: Int): Int {
        val k = if (n >= 13) 3 else 2
        val pool = pz.rows.indices.filter { it != from && it !in solved }
            .flatMap { r -> openCells(r).filter { openCells(r).size > 1 }.map { r to it } }.shuffled(rnd)
        val used = HashSet<Int>(); var c = 0
        for ((r, i) in pool) {
            if (c >= k) break
            if (r in used) continue
            used += r; revealed["$r,$i"] = true; fresh["$r,$i"] = true; typed[r] = ""; c++
        }
        if (c > 0) scope.launch { delay(1800); fresh.clear() }
        return c
    }

    fun rowSolved(r: Int) {
        solved += r; typed.remove(r)
        val c = scatter(r)
        settleRows()
        soundManager.playCorrect()
        fx.float(if (c > 0) "✓ ${pz.rows[r].answer} · $c harf açıldı" else "✓ ${pz.rows[r].answer}", NeonGreen, 0.5f, 0.3f)
        persist()
        if (solved.size == n) finishPuzzle() else sel = nextOpenRow(r) ?: r
    }

    fun checkRow(r: Int) {
        val row = pz.rows[r]
        val cells = openCells(r)
        val t = typed[r].orEmpty()
        if (t.length < cells.size) return
        val built = row.answer.indices.map { i -> if (isRevealed(r, i)) row.answer[i] else t[cells.indexOf(i)] }.joinToString("")
        if (built == row.answer) rowSolved(r)
        else {
            shakeKey++; soundManager.playWrong()
            scope.launch { delay(350); typed[r] = "" }
        }
    }

    fun type(c: Char) {
        if (done || !loaded || sel in solved) return
        val need = openCells(sel).size
        val t = typed[sel].orEmpty()
        if (t.length >= need) return
        typed[sel] = t + c; soundManager.playTap()
        if (t.length + 1 == need) checkRow(sel)
    }

    fun back() { if (done) return; typed[sel] = typed[sel].orEmpty().dropLast(1) }

    fun pay(cost: Int, useBooster: Boolean, apply: () -> Unit) {
        scope.launch {
            val ok = if (useBooster && hints > 0) prefs.useBooster("hint") else prefs.spendDiamonds(cost)
            if (ok) { apply(); return@launch }
            val act = activity ?: return@launch
            com.example.monetize.Ads.showRewarded(act, prefs, onUnavailable = { fx.float("Yetersiz 💎 · $it", NeonRed) }) { apply() }
        }
    }

    fun hintLetter() {
        if (done || sel in solved) return
        val r = sel
        val cells = openCells(r)
        if (cells.isEmpty()) return
        val t = typed[r].orEmpty()
        val i = cells[t.length.coerceAtMost(cells.size - 1)]
        pay(20, true) {
            revealed["$r,$i"] = true; fresh["$r,$i"] = true; typed[r] = t.take(cells.indexOf(i))
            scope.launch { delay(1200); fresh.remove("$r,$i") }
            soundManager.playCorrect()
            if (openCells(r).isEmpty()) rowSolved(r) else persist()
        }
    }

    fun hintWord() {
        if (done || sel in solved) return
        val r = sel
        pay(40, false) { pz.rows[r].answer.indices.forEach { revealed["$r,$it"] = true }; rowSolved(r) }
    }

    fun afterLevel(action: () -> Unit) {
        val act = activity
        if (act == null) action() else scope.launch { com.example.monetize.Ads.maybeInterstitialEvery(act, prefs, 3, action) }
    }

    val inWorld = pz.index + 1
    com.example.ui.components.PhotoBackground(WORLD_BG.getOrNull(pz.world)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            NeonTopBar("${world.emoji} ${world.name} · $inWorld", subtitle = "${solved.size}/$n kelime · $len harf", onBack = onBack, trailing = { DiamondPill(diamonds, onOpenShop) })
            // ---- Gizli kelime şeridi ----
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp).clip(RoundedCornerShape(14.dp))
                    .background(NeonCard.copy(alpha = 0.85f)).border(1.dp, NeonPink.copy(alpha = 0.55f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Gizli\nkelime", color = NeonPink, fontSize = 11.sp, lineHeight = 12.sp, fontWeight = FontWeight.Bold, maxLines = 2)
                Spacer(Modifier.width(8.dp))
                BoxWithConstraints(Modifier.weight(1f)) {
                    val sz = min((maxWidth - 3.dp * secretRows.size) / secretRows.size.coerceAtLeast(1), 30.dp)
                    val fs = with(density) { (sz * 0.55f).toSp() }
                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        secretRows.forEach { r ->
                            val p = pz.rows[r].pos
                            val ok = isRevealed(r, p)
                            Box(
                                Modifier.size(sz).clip(RoundedCornerShape(6.dp)).background(NeonPink.copy(alpha = if (ok) 0.45f else 0.16f))
                                    .border(1.dp, NeonPink.copy(alpha = 0.7f), RoundedCornerShape(6.dp)),
                                contentAlignment = Alignment.Center
                            ) { if (ok) Text(pz.rows[r].answer[p].toString(), color = TextPrimary, fontSize = fs, fontWeight = FontWeight.ExtraBold, maxLines = 1) }
                        }
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            // ---- Izgara ----
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 10.dp)) {
                val gap = 4.dp
                val cell = min((maxWidth - 20.dp - gap * (len - 1)) / len, 46.dp)
                val fs = with(density) { (cell * 0.52f).toSp() }
                val scroll = rememberScrollState()
                val rowPx = with(density) { (cell + gap).toPx() }
                val viewPx = with(density) { maxHeight.toPx() }
                LaunchedEffect(sel, loaded) {
                    if (loaded) scroll.animateScrollTo((sel * rowPx - viewPx / 2 + rowPx).toInt().coerceIn(0, scroll.maxValue.coerceAtLeast(0)))
                }
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(NeonCard.copy(alpha = 0.55f))
                        .border(1.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
                        .verticalScroll(scroll).padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(gap),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    pz.rows.forEachIndexed { r, row ->
                        val isSel = r == sel && !done
                        val ok = r in solved
                        val cells = openCells(r)
                        val t = typed[r].orEmpty()
                        val cursor = if (isSel) cells.getOrNull(t.length) else null
                        Row(
                            Modifier.then(if (isSel) Modifier.shake(shakeKey, 6.dp) else Modifier)
                                .bounceClick(pressedScale = 0.98f) { if (!done && !ok) { sel = r; soundManager.uiTick() } },
                            horizontalArrangement = Arrangement.spacedBy(gap)
                        ) {
                            for (i in row.answer.indices) {
                                val key = "$r,$i"
                                val ch: Char? = if (isRevealed(r, i)) row.answer[i] else t.getOrNull(cells.indexOf(i))
                                val secret = i == row.pos
                                val isNew = fresh[key] == true
                                val bg = when {
                                    i == cursor -> accent
                                    isNew -> NeonYellow.copy(alpha = 0.55f)
                                    ok && secret -> NeonPink.copy(alpha = 0.55f)
                                    ok -> NeonGreen.copy(alpha = 0.30f)
                                    secret -> NeonPink.copy(alpha = 0.22f)
                                    isSel -> accent.copy(alpha = 0.16f)
                                    else -> NeonCard
                                }
                                val bd = when {
                                    isNew -> NeonYellow
                                    isSel -> accent
                                    secret -> NeonPink.copy(alpha = 0.7f)
                                    ok -> NeonGreen.copy(alpha = 0.7f)
                                    else -> NeonBorder
                                }
                                Box(
                                    Modifier.size(cell).clip(RoundedCornerShape(8.dp)).background(bg)
                                        .border(if (isSel || isNew) 1.5.dp else 1.dp, bd, RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (ch != null) Text(
                                        ch.toString(), fontSize = fs, fontWeight = FontWeight.ExtraBold, maxLines = 1,
                                        color = when { ok && !secret -> NeonGreen; i == cursor -> Color.White; else -> TextPrimary }
                                    )
                                }
                            }
                        }
                    }
                }
            }
            if (!done) {
                val row = pz.rows[sel]
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp).clip(RoundedCornerShape(14.dp))
                        .background(NeonCard.copy(alpha = 0.92f)).border(1.5.dp, accent.copy(alpha = 0.7f), RoundedCornerShape(14.dp)).padding(horizontal = 4.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("‹", color = accent, fontSize = 26.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.bounceClick { sel = prevOpenRow(sel) ?: sel }.padding(horizontal = 10.dp))
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(row.clue, color = TextPrimary, textAlign = TextAlign.Center, fontWeight = FontWeight.SemiBold, maxLines = 3)
                        Text("${sel + 1}. soru · ${row.answer.length} harf", color = TextMuted, fontSize = 11.sp, maxLines = 1)
                    }
                    Text("›", color = accent, fontSize = 26.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.bounceClick { sel = nextOpenRow(sel) ?: sel }.padding(horizontal = 10.dp))
                }
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("💡 Harf aç ${if (hints > 0) "($hints)" else "· 20💎"}", color = NeonCyan, fontWeight = FontWeight.SemiBold, maxLines = 1,
                        modifier = Modifier.clip(RoundedCornerShape(10.dp)).bounceClick { hintLetter() }.padding(6.dp))
                    Text("🔎 Kelimeyi aç · 40💎", color = NeonPurple, fontWeight = FontWeight.SemiBold, maxLines = 1,
                        modifier = Modifier.clip(RoundedCornerShape(10.dp)).bounceClick { hintWord() }.padding(6.dp))
                }
                Column(
                    Modifier.fillMaxWidth().background(NeonCard.copy(alpha = 0.6f)).padding(horizontal = 3.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    KB.forEachIndexed { ri, line ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterHorizontally)) {
                            line.forEach { c -> Key(c.toString(), 1f) { type(c) } }
                            if (ri == 2) Key("⌫", 1.6f) { back() }
                        }
                    }
                }
            } else Spacer(Modifier.height(8.dp))
        }
        if (done) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f)).bounceClick(pressedScale = 1f) {}, contentAlignment = Alignment.Center) {
                GlassCard(accent = NeonPink, glow = true, modifier = Modifier.padding(24.dp)) {
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(world.emoji, fontSize = 40.sp)
                        Text("Gizli kelime", color = TextSecondary)
                        Text(pz.secret, color = NeonPink, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
                        Text(pz.secretClue, color = TextPrimary, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium)
                        if (reward > 0) Text("+$reward 💎", color = NeonCyan, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 6.dp))
                        Spacer(Modifier.height(14.dp))
                        if (level < all.size) NeonButton("Sonraki bulmaca", { afterLevel { onNext(level + 1) } }, Modifier.fillMaxWidth(), color = accent)
                        Spacer(Modifier.height(8.dp))
                        NeonButton("Haritaya dön", { afterLevel(onBack) }, Modifier.fillMaxWidth(), color = accent, outlined = true)
                    }
                }
            }
        }
        FxOverlay(fx)
    }
}

@Composable
private fun RowScope.Key(label: String, weight: Float, onClick: () -> Unit) {
    val fs = with(androidx.compose.ui.platform.LocalDensity.current) { 17.dp.toSp() }
    Box(
        Modifier.weight(weight).height(46.dp).clip(RoundedCornerShape(7.dp)).background(NeonCardHigh)
            .border(1.dp, NeonBorder, RoundedCornerShape(7.dp)).bounceClick(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Text(label, color = TextPrimary, fontSize = fs, fontWeight = FontWeight.Bold, maxLines = 1) }
}
