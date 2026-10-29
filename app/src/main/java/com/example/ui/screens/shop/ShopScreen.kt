package com.example.ui.screens.shop

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.GameRepository
import com.example.domain.model.Cosmetics
import com.example.ui.components.DiamondPill
import com.example.ui.components.GlassCard
import com.example.ui.components.InfoBanner
import com.example.ui.components.NeonButton
import com.example.ui.components.NeonChip
import com.example.ui.components.NeonTopBar
import com.example.ui.components.SectionTitle
import com.example.ui.game.ConfettiLayer
import com.example.ui.screens.profile.ProfileAvatar
import com.example.ui.screens.profile.SegmentedTabs
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.NeonYellow
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.SoundAndVibeManager
import kotlinx.coroutines.launch

private data class BoosterOffer(val type: String, val emoji: String, val title: String, val desc: String, val count: Int, val cost: Int, val color: Color)

private val BOOSTERS = listOf(
    BoosterOffer("hint", "💡", "İpucu", "Yanlış şıkları eler, yolu gösterir, parçayı yerine koyar.", 1, 30, NeonYellow),
    BoosterOffer("hint", "💡", "İpucu Paketi ×5", "5 ipucu, %20 indirimli.", 5, 120, NeonYellow),
    BoosterOffer("time", "⏱️", "+10 Saniye", "Süreli turlarda saati 10 saniye uzatır.", 1, 25, NeonCyan),
    BoosterOffer("time", "⏱️", "Süre Paketi ×5", "5 adet ek süre, %20 indirimli.", 5, 100, NeonCyan),
    BoosterOffer("undo", "↩️", "Geri Al", "Labirent, nehir, yapboz gibi oyunlarda son hamleyi geri alır.", 1, 25, NeonPink),
    BoosterOffer("undo", "↩️", "Geri Al Paketi ×5", "5 geri alma, %20 indirimli.", 5, 100, NeonPink)
)

@Composable
fun ShopScreen(
    repository: GameRepository,
    soundManager: SoundAndVibeManager,
    onBack: (() -> Unit)? = null
) {
    val scope = rememberCoroutineScope()
    val prefs = repository.preferences
    val diamonds by prefs.diamondBalance.collectAsState(initial = 0)
    val owned by prefs.ownedCosmetics.collectAsState(initial = emptySet())
    val avatarId by prefs.avatarId.collectAsState(initial = 0)
    val frameId by prefs.equippedFrame.collectAsState(initial = "none")
    val hints by prefs.hintsCount.collectAsState(initial = 0)
    val times by prefs.extraTimeCount.collectAsState(initial = 0)
    val undos by prefs.undosCount.collectAsState(initial = 0)
    val giftClaimed by prefs.dailyGiftClaimed.collectAsState(initial = true)
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var message by rememberSaveable { androidx.compose.runtime.mutableStateOf<String?>(null) }
    var confetti by rememberSaveable { mutableIntStateOf(0) }

    fun toast(text: String, ok: Boolean) {
        message = text
        if (ok) { soundManager.playCorrect(); confetti++ } else soundManager.playWrong()
    }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            NeonTopBar(
                title = "Mağaza",
                subtitle = "Görünümler ve güçler",
                onBack = onBack,
                trailing = { DiamondPill(diamonds, onClick = null) }
            )
            Box(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                SegmentedTabs(listOf("Görünüm", "Güçler", "Elmas"), tab) { tab = it; soundManager.playTap() }
            }
            message?.let {
                InfoBanner(it, color = NeonGreen, emoji = "✨", modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
            }
            AnimatedContent(tab, transitionSpec = { fadeIn().togetherWith(fadeOut()) }, label = "shop") { t ->
                when (t) {
                    0 -> LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 32.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item(span = { GridItemSpan(3) }) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 4.dp)) {
                                ProfileAvatar(avatarId, frameId, 64.dp)
                                Spacer(Modifier.width(14.dp))
                                Column {
                                    Text("Önizleme", color = TextSecondary, style = MaterialTheme.typography.labelMedium)
                                    Text("${Cosmetics.avatar(avatarId).name} · ${Cosmetics.frame(frameId).name}", color = TextPrimary, style = MaterialTheme.typography.titleSmall)
                                }
                            }
                        }
                        item(span = { GridItemSpan(3) }) { SectionTitle("Avatarlar") }
                        items(Cosmetics.AVATARS, key = { "a${it.id}" }) { a ->
                            val has = Cosmetics.owns(owned, a)
                            val equipped = avatarId == a.id
                            CosmeticCell(
                                title = a.name,
                                equipped = equipped,
                                has = has,
                                cost = a.cost,
                                accent = if (a.cost >= 300) NeonYellow else if (a.cost > 0) NeonPink else NeonCyan,
                                visual = { Text(a.emoji, fontSize = 34.sp) },
                                onClick = {
                                    scope.launch {
                                        if (has) { prefs.setAvatarId(a.id); toast("${a.name} avatarın oldu", true) }
                                        else if (prefs.buyCosmetic(a.shopId, a.cost)) { prefs.setAvatarId(a.id); toast("${a.name} alındı ve takıldı!", true) }
                                        else toast("Yetersiz elmas: ${a.cost - diamonds} 💎 daha lazım", false)
                                    }
                                }
                            )
                        }
                        item(span = { GridItemSpan(3) }) { SectionTitle("Çerçeveler", modifier = Modifier.padding(top = 8.dp)) }
                        items(Cosmetics.FRAMES, key = { "f${it.id}" }) { f ->
                            val has = Cosmetics.owns(owned, f)
                            CosmeticCell(
                                title = f.name,
                                equipped = frameId == f.id,
                                has = has,
                                cost = f.cost,
                                accent = f.colors.first(),
                                visual = { ProfileAvatar(avatarId, f.id, 52.dp) },
                                onClick = {
                                    scope.launch {
                                        if (has) { prefs.equipFrame(f.id); toast("${f.name} takıldı", true) }
                                        else if (prefs.buyCosmetic(f.shopId, f.cost)) { prefs.equipFrame(f.id); toast("${f.name} alındı ve takıldı!", true) }
                                        else toast("Yetersiz elmas: ${f.cost - diamonds} 💎 daha lazım", false)
                                    }
                                }
                            )
                        }
                    }
                    1 -> LazyColumn(contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 32.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        item {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Stock("💡", "İpucu", hints, NeonYellow, Modifier.weight(1f))
                                Stock("⏱️", "Süre", times, NeonCyan, Modifier.weight(1f))
                                Stock("↩️", "Geri Al", undos, NeonPink, Modifier.weight(1f))
                            }
                        }
                        items(BOOSTERS.size) { i ->
                            val b = BOOSTERS[i]
                            GlassCard(accent = b.color, contentPadding = PaddingValues(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(b.emoji, fontSize = 30.sp)
                                    Spacer(Modifier.width(12.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(b.title, color = TextPrimary, style = MaterialTheme.typography.titleSmall)
                                        Text(b.desc, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                                    }
                                    Spacer(Modifier.width(10.dp))
                                    PriceButton(b.cost, b.color) {
                                        scope.launch {
                                            if (prefs.buyBooster(b.type, b.count, b.cost)) toast("+${b.count} ${b.title.substringBefore(" Paketi")} eklendi", true)
                                            else toast("Yetersiz elmas: ${b.cost - diamonds} 💎 daha lazım", false)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    else -> LazyColumn(contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 32.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        item {
                            GlassCard(accent = NeonYellow, glow = !giftClaimed) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("🎁", fontSize = 40.sp)
                                    Spacer(Modifier.width(12.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text("Günlük Hediye", color = TextPrimary, style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            if (giftClaimed) "Bugünkü hediyeni aldın. Yarın yine gel!" else "Her gün ücretsiz elmas. Serin uzadıkça hediye büyür (en fazla 30 💎).",
                                            color = TextSecondary, style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                                Spacer(Modifier.height(12.dp))
                                NeonButton(
                                    if (giftClaimed) "Yarın tekrar" else "Hediyeyi Al",
                                    onClick = {
                                        scope.launch {
                                            val got = prefs.claimDailyGift()
                                            if (got > 0) toast("+$got 💎 hediye hesabına eklendi!", true)
                                        }
                                    },
                                    color = NeonYellow, enabled = !giftClaimed, modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                        item { StoreSection(repository) }
                        item {
                            GlassCard(accent = NeonCyan) {
                                Text("💎 Elmas nasıl kazanılır?", color = TextPrimary, style = MaterialTheme.typography.titleSmall)
                                Spacer(Modifier.height(8.dp))
                                listOf(
                                    "Bir seviyeyi ilk kez geç: 2–3 💎",
                                    "Boss seviyesini yen: 10 💎",
                                    "Bir seviyeyi ilk kez 3 yıldızla bitir: +1 💎",
                                    "Haftalık ligde ilk 3'e gir: 150–500 💎",
                                    "Kısa reklam izle: 20 💎 (günde en fazla 10)",
                                    "Her gün hediye kutusunu aç: 10–30 💎"
                                ).forEach {
                                    Text("• $it", color = TextSecondary, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 2.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
        ConfettiLayer(confetti)
    }
}

@Composable
private fun CosmeticCell(
    title: String,
    equipped: Boolean,
    has: Boolean,
    cost: Int,
    accent: Color,
    visual: @Composable () -> Unit,
    onClick: () -> Unit
) {
    GlassCard(
        accent = if (equipped) NeonGreen else accent,
        glow = equipped,
        onClick = onClick,
        contentPadding = PaddingValues(8.dp),
        modifier = Modifier.aspectRatio(0.78f)
    ) {
        Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) { visual() }
        Text(
            title, color = TextPrimary, style = MaterialTheme.typography.labelMedium, maxLines = 1,
            overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(4.dp))
        val (label, color) = when {
            equipped -> "Takılı" to NeonGreen
            has -> "Tak" to NeonCyan
            else -> "💎 $cost" to accent
        }
        Text(
            label, color = if (!has) com.example.ui.theme.OnNeon else color, fontWeight = FontWeight.ExtraBold,
            style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(50))
                .background(if (!has) color else color.copy(alpha = 0.12f))
                .padding(vertical = 4.dp)
        )
    }
}

@Composable
private fun Stock(emoji: String, label: String, count: Int, color: Color, modifier: Modifier) {
    GlassCard(accent = color, modifier = modifier, contentPadding = PaddingValues(10.dp)) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(emoji, fontSize = 22.sp)
            Text("$count", color = TextPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
            Text(label, color = TextSecondary, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun PriceButton(cost: Int, color: Color, onClick: () -> Unit) {
    NeonButton("💎 $cost", onClick, color = color, height = 42.dp)
}
