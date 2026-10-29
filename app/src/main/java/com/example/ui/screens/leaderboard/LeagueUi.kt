package com.example.ui.screens.leaderboard

import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cloud.CloudSync
import com.example.cloud.LeagueBoard
import com.example.cloud.LeagueEntry
import com.example.ui.components.GlassCard
import com.example.ui.components.InfoBanner
import com.example.ui.components.NeonButton
import com.example.ui.components.NeonChip
import com.example.ui.components.neonGlow
import com.example.ui.screens.profile.ProfileAvatar
import com.example.ui.screens.profile.SegmentedTabs
import com.example.ui.theme.NeonCard
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonOrange
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.NeonYellow
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val PODIUM = listOf(NeonYellow, Color(0xFFC9D1E8), NeonOrange)

/** Google ile giriş kartı (Profil'de). */
@Composable
fun AccountCard() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val user by CloudSync.user.collectAsState()
    val sync by CloudSync.syncState.collectAsState()
    var busy by remember { mutableStateOf(false) }
    GlassCard(accent = if (user != null) NeonGreen else NeonCyan, glow = user == null) {
        val u = user
        if (!CloudSync.isConfigured) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("☁️", fontSize = 28.sp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Google ile giriş", color = TextPrimary, style = MaterialTheme.typography.titleSmall)
                    Text("Bulut yedek ve haftalık lig çok yakında açılıyor.", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                }
                NeonChip("YAKINDA", NeonPurple)
            }
        } else if (u == null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("☁️", fontSize = 28.sp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Google ile giriş yap", color = TextPrimary, style = MaterialTheme.typography.titleSmall)
                    Text("İlerlemen buluta yedeklenir, haftalık ligde yarışırsın.", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                }
            }
            Spacer(Modifier.height(12.dp))
            NeonButton(if (busy) "Bağlanıyor..." else "G  Google ile giriş", onClick = {
                val act = context as? Activity ?: return@NeonButton
                if (busy) return@NeonButton
                busy = true
                scope.launch {
                    val err = CloudSync.signIn(act)
                    busy = false
                    Toast.makeText(context, err ?: "Giriş yapıldı ✓", Toast.LENGTH_SHORT).show()
                }
            }, color = NeonCyan, modifier = Modifier.fillMaxWidth(), enabled = !busy)
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("✅", fontSize = 26.sp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(u.name, color = TextPrimary, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text((u.email ?: "") + if (sync.isNotEmpty()) " · $sync" else "", color = TextSecondary, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                NeonButton("Şimdi yedekle", onClick = {
                    scope.launch {
                        runCatching { CloudSync.backupNow() }
                            .onSuccess { Toast.makeText(context, "Yedeklendi ☁️", Toast.LENGTH_SHORT).show() }
                            .onFailure { Toast.makeText(context, "Yedeklenemedi, internetini kontrol et", Toast.LENGTH_SHORT).show() }
                    }
                }, color = NeonGreen, outlined = true, height = 42.dp, modifier = Modifier.weight(1f))
                NeonButton("Çıkış", onClick = { CloudSync.signOut() }, color = NeonPink, outlined = true, height = 42.dp)
            }
        }
    }
}

private fun countdown(ms: Long): String {
    val d = ms / 86_400_000
    val h = (ms % 86_400_000) / 3_600_000
    val m = (ms % 3_600_000) / 60_000
    return when {
        d > 0 -> "$d gün $h sa"
        h > 0 -> "$h sa $m dk"
        else -> "$m dk"
    }
}

/** Haftalık lig sekmesi. */
@Composable
fun LeagueTab() {
    val user by CloudSync.user.collectAsState()
    var week by remember { mutableIntStateOf(0) }
    var board by remember { mutableStateOf<LeagueBoard?>(null) }
    var loading by remember { mutableStateOf(false) }
    var reloadKey by remember { mutableIntStateOf(0) }
    var left by remember { mutableLongStateOf(CloudSync.msUntilWeekEnd()) }

    LaunchedEffect(Unit) { while (true) { left = CloudSync.msUntilWeekEnd(); delay(30_000) } }
    LaunchedEffect(week, user, reloadKey) {
        if (!CloudSync.isConfigured) return@LaunchedEffect
        loading = true
        board = CloudSync.loadBoard(if (week == 0) 0 else -1)
        loading = false
    }

    LazyColumn(contentPadding = PaddingValues(16.dp, 0.dp, 16.dp, 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            GlassCard(accent = NeonYellow, glow = true) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🏆", fontSize = 32.sp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Haftalık Lig", color = TextPrimary, style = MaterialTheme.typography.titleMedium)
                        Text("Bu hafta kazandığın puanla yarış. Pazartesi 00:00'da sıfırlanır.", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    PrizeChip("🥇", 500, PODIUM[0], Modifier.weight(1f))
                    PrizeChip("🥈", 300, PODIUM[1], Modifier.weight(1f))
                    PrizeChip("🥉", 150, PODIUM[2], Modifier.weight(1f))
                    PrizeChip("4-10", 50, NeonCyan, Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                Text("⏳ Bitime: ${countdown(left)}", color = NeonYellow, style = MaterialTheme.typography.labelLarge)
            }
        }
        if (!CloudSync.isConfigured) {
            item { InfoBanner("Haftalık lig çok yakında açılıyor. Şimdiden seviyeleri geçip puan biriktir!", NeonPurple, "🚀") }
            return@LazyColumn
        }
        if (user == null) {
            item { InfoBanner("Ligde yer almak için Profil sekmesinden Google ile giriş yap.", NeonCyan, "🔑") }
        }
        item { SegmentedTabs(listOf("Bu hafta", "Geçen hafta"), week) { week = it } }
        val b = board
        if (loading && b == null) {
            item { Text("Yükleniyor...", color = TextMuted, modifier = Modifier.padding(16.dp)) }
        } else if (b == null) {
            item {
                InfoBanner("Liste yüklenemedi. İnternet bağlantını kontrol et.", NeonOrange, "📡")
                Spacer(Modifier.height(8.dp))
                NeonButton("Tekrar dene", { reloadKey++ }, color = NeonCyan, outlined = true, modifier = Modifier.fillMaxWidth())
            }
        } else {
            if (b.top.isEmpty()) {
                item { InfoBanner(if (week == 0) "Bu hafta henüz kimse puan almadı. İlk sen ol!" else "Geçen hafta puan alan olmadı.", NeonCyan, "✨") }
            } else {
                item { Podium(b.top.take(3), user?.uid) }
                itemsIndexed(b.top.drop(3), key = { _, e -> e.uid }) { i, e -> LeagueRow(i + 4, e, e.uid == user?.uid) }
            }
            if (user != null && (b.myRank == null || b.myRank > b.top.size)) {
                item {
                    GlassCard(accent = NeonCyan, contentPadding = PaddingValues(12.dp)) {
                        Text(
                            if (b.myScore > 0) "Senin sıran: ${b.myRank} · ${b.myScore} puan" else "Bu hafta henüz puanın yok. Bir seviye oyna!",
                            color = TextPrimary, style = MaterialTheme.typography.titleSmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PrizeChip(label: String, diamonds: Int, color: Color, modifier: Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(12.dp)).background(color.copy(alpha = 0.1f)).border(1.dp, color.copy(alpha = 0.6f), RoundedCornerShape(12.dp)).padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, fontSize = 14.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
        Text("💎$diamonds", color = color, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
private fun Podium(top: List<LeagueEntry>, me: String?) {
    Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.Bottom) {
        val order = listOf(1, 0, 2)
        for (idx in order) {
            val e = top.getOrNull(idx)
            val h: Dp = when (idx) { 0 -> 110.dp; 1 -> 80.dp; else -> 62.dp }
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(104.dp)) {
                if (e != null) {
                    ProfileAvatar(e.avatar, e.frame, if (idx == 0) 64.dp else 52.dp)
                    Spacer(Modifier.height(4.dp))
                    Text(e.name, color = if (e.uid == me) NeonCyan else TextPrimary, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
                    Text("${e.score}", color = PODIUM[idx], fontWeight = FontWeight.ExtraBold)
                    Spacer(Modifier.height(4.dp))
                }
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(h)
                        .neonGlow(PODIUM[idx], 12.dp, 12.dp, if (e != null) 0.5f else 0f)
                        .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                        .background(Brush.verticalGradient(listOf(PODIUM[idx].copy(alpha = 0.55f), NeonCard))),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Text(listOf("🥇", "🥈", "🥉")[idx], fontSize = 28.sp, modifier = Modifier.padding(top = 8.dp))
                }
            }
        }
    }
}

@Composable
private fun LeagueRow(rank: Int, e: LeagueEntry, isMe: Boolean) {
    GlassCard(accent = if (isMe) NeonCyan else if (rank <= 10) NeonPurple else Color(0xFF2C2C4A), glow = isMe, contentPadding = PaddingValues(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("$rank", color = TextSecondary, fontWeight = FontWeight.Bold, modifier = Modifier.width(30.dp))
            ProfileAvatar(e.avatar, e.frame, 38.dp)
            Spacer(Modifier.width(10.dp))
            Text(e.name + if (isMe) " (sen)" else "", color = TextPrimary, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (rank <= 10) Text("💎${CloudSync.REWARDS[rank]}", color = NeonCyan, style = MaterialTheme.typography.labelSmall)
            Spacer(Modifier.width(8.dp))
            Text("${e.score}", color = NeonYellow, fontWeight = FontWeight.ExtraBold)
        }
    }
}
