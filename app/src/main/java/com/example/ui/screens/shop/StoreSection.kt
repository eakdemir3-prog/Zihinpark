package com.example.ui.screens.shop

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.GameRepository
import com.example.monetize.Ads
import com.example.monetize.Billing
import com.example.monetize.Product
import com.example.ui.components.GlassCard
import com.example.ui.components.NeonButton
import com.example.ui.components.NeonChip
import com.example.ui.components.bounceClick
import com.example.ui.theme.NeonCard
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonOrange
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.NeonYellow
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch

/** Mağaza: ödüllü reklamla elmas, PRO ve elmas paketleri (Google Play). */
@Composable
fun StoreSection(repository: GameRepository) {
    val activity = LocalContext.current as? Activity
    val scope = rememberCoroutineScope()
    val prefs = repository.preferences
    val left by prefs.rewardedLeftToday.collectAsState(initial = 0)
    val isPro by prefs.isProMember.collectAsState(initial = false)
    val prices by Billing.prices.collectAsState()
    var note by remember { mutableStateOf<String?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // ---- Reklam izle, elmas kazan ----
        GlassCard(accent = NeonGreen) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🎬", fontSize = 32.sp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Reklam izle, 20 💎 kazan", color = TextPrimary, style = MaterialTheme.typography.titleSmall)
                    Text("Bugün kalan: $left / 10", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                }
            }
            Spacer(Modifier.height(10.dp))
            NeonButton(if (left > 0) "▶ İzle" else "Yarın yine gel", {
                if (activity == null) return@NeonButton
                scope.launch {
                    Ads.showRewarded(activity, prefs, onUnavailable = { note = it }) {
                        scope.launch { prefs.addDiamonds(20); note = "+20 💎 eklendi!" }
                    }
                }
            }, color = NeonGreen, enabled = left > 0, modifier = Modifier.fillMaxWidth(), height = 44.dp)
            note?.let {
                Spacer(Modifier.height(6.dp))
                Text(it, color = NeonOrange, style = MaterialTheme.typography.bodySmall)
            }
        }

        // ---- PRO ----
        GlassCard(accent = NeonYellow, glow = !isPro) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("👑", fontSize = 36.sp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("ZihinPark PRO", color = TextPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Tek seferlik · ömür boyu", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                }
                if (isPro) NeonChip("AKTİF", NeonGreen, filled = true)
            }
            Spacer(Modifier.height(8.dp))
            listOf("🚫 Seviye arası reklamlar tamamen kalkar", "💎 Hemen +500 elmas", "🎁 Günlük hediye 2 katı", "❤️ Geliştiriciye destek").forEach {
                Text(it, color = TextSecondary, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 2.dp))
            }
            if (!isPro) {
                Spacer(Modifier.height(10.dp))
                NeonButton(
                    prices[Product.PRO.id]?.let { "PRO'ya geç · $it" } ?: "PRO'ya geç",
                    { activity?.let { Billing.buy(it, Product.PRO) } },
                    color = NeonYellow, modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // ---- Elmas paketleri ----
        GlassCard(accent = NeonCyan) {
            Text("💎 Elmas paketleri", color = TextPrimary, style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            listOf(Product.GEMS_S to "🪙", Product.GEMS_M to "💰", Product.GEMS_L to "🧰").forEach { (p, e) ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(NeonCard)
                        .border(1.dp, NeonCyan.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                        .bounceClick { activity?.let { Billing.buy(it, p) } }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(e, fontSize = 26.sp)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(p.title, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                        Text("${p.gems} 💎", color = NeonCyan, style = MaterialTheme.typography.bodySmall)
                    }
                    Text(prices[p.id] ?: "—", color = NeonYellow, fontWeight = FontWeight.Bold)
                }
            }
            if (prices.isEmpty()) {
                Spacer(Modifier.height(6.dp))
                Text("Fiyatlar Google Play'den yükleniyor…", color = TextMuted, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(6.dp))
            Text("Satın alımları geri yükle", color = NeonPurple, style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.bounceClick { Billing.restore(); note = "Satın alımlar kontrol ediliyor…" }.padding(4.dp))
        }
    }
}
