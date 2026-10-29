package com.example.ui.screens.shop

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.GameRepository
import com.example.ui.theme.MintGreen
import com.example.ui.theme.MintGreenDark
import com.example.ui.theme.PastelBlue
import com.example.ui.theme.PastelLavender
import com.example.ui.theme.PastelPeach
import com.example.ui.theme.PastelYellow
import com.example.ui.theme.PrimaryCoral
import com.example.util.SoundAndVibeManager
import kotlinx.coroutines.launch

data class CosmeticItem(
    val id: String,
    val name: String,
    val type: String, // "avatar", "frame", "theme"
    val visual: String,
    val cost: Int,
    val description: String
)

private val SHOP_COSMETICS = listOf(
    CosmeticItem("avatar_robot", "Siber Zihin", "avatar", "🤖", 150, "Mekanik düşünce gücü ve analitik görünüm"),
    CosmeticItem("avatar_galaxy", "Kozmik Beyin", "avatar", "🌌", 250, "Derin evrensel düşünce avatarları"),
    CosmeticItem("avatar_owl", "Bilge Baykuş", "avatar", "🦉", 120, "Antik bilgelik ve odaklanma simgesi"),
    CosmeticItem("avatar_detective", "Büyük Dedektif", "avatar", "🕵️‍♂️", 180, "Çıkarım ve gözlem uzmanı"),
    CosmeticItem("frame_gold", "Altın Kristal Çerçeve", "frame", "👑", 350, "Liderler için parıldayan altın profil kenarlığı"),
    CosmeticItem("frame_neon", "Neon Işıltı Çerçevesi", "frame", "⚡", 280, "Canlı neon mavi ve mercan parıltısı")
)

@Composable
fun ShopScreen(
    repository: GameRepository,
    soundManager: SoundAndVibeManager,
    onBack: (() -> Unit)? = null
) {
    val coroutineScope = rememberCoroutineScope()
    val diamondBalance by repository.preferences.diamondBalance.collectAsState(initial = 50)
    val ownedCosmetics by repository.preferences.ownedCosmetics.collectAsState(initial = emptySet())
    val hintsCount by repository.preferences.hintsCount.collectAsState(initial = 3)
    val extraTimeCount by repository.preferences.extraTimeCount.collectAsState(initial = 2)
    val undosCount by repository.preferences.undosCount.collectAsState(initial = 3)
    val dailyAdsCount by repository.preferences.dailyRewardsCount.collectAsState(initial = 0)
    val isProMember by repository.preferences.isProMember.collectAsState(initial = false)

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Kozmetik, 1: Güçler, 2: Elmas Al & Pro
    var feedbackMsg by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Header with diamond counter
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBack != null) {
                androidx.compose.material3.IconButton(onClick = onBack) {
                    androidx.compose.material3.Icon(
                        androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Geri",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Elmas Mağazası",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Görünümler, Güçler ve Avantajlar",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(PastelYellow)
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Diamond,
                        contentDescription = null,
                        tint = com.example.ui.theme.NeonOrange,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "$diamondBalance Elmas",
                        fontWeight = FontWeight.ExtraBold,
                        color = com.example.ui.theme.NeonYellow,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Tabs Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                label = { Text("Görünüm") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = PrimaryCoral,
                    selectedLabelColor = com.example.ui.theme.OnNeon
                )
            )
            FilterChip(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                label = { Text("Oyun Güçleri") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = PrimaryCoral,
                    selectedLabelColor = com.example.ui.theme.OnNeon
                )
            )
            FilterChip(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                label = { Text("Elmas & Pro") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = PrimaryCoral,
                    selectedLabelColor = com.example.ui.theme.OnNeon
                )
            )
        }

        if (feedbackMsg != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = MintGreen),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(feedbackMsg ?: "", color = MintGreenDark, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            when (selectedTab) {
                0 -> {
                    // Cosmetics
                    items(SHOP_COSMETICS) { item ->
                        val isOwned = ownedCosmetics.contains(item.id)
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(item.visual, fontSize = 36.sp)
                                    Spacer(modifier = Modifier.width(14.dp))
                                    Column {
                                        Text(item.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                        Text(item.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }

                                if (isOwned) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MintGreen)
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text("Sahip Olundu", fontWeight = FontWeight.Bold, color = MintGreenDark, style = MaterialTheme.typography.labelSmall)
                                    }
                                } else {
                                    Button(
                                        onClick = {
                                            coroutineScope.launch {
                                                val success = repository.preferences.buyCosmetic(item.id, item.cost)
                                                if (success) {
                                                    soundManager.playSuccessSound()
                                                    soundManager.vibrateSuccess()
                                                    feedbackMsg = "${item.name} satın alındı!"
                                                } else {
                                                    soundManager.playErrorSound()
                                                    feedbackMsg = "Yetersiz elmas bakiyesi!"
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryCoral),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text("💎 ${item.cost}", color = com.example.ui.theme.OnNeon, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
                1 -> {
                    // Boosters
                    item {
                        BoosterPurchaseCard(
                            title = "İpucu Güçlendirmesi",
                            description = "Zorlandığın seviyelerde doğru yuvayı veya cevabı işaret eder.",
                            icon = "💡",
                            currentCount = hintsCount,
                            cost = 35,
                            onBuy = {
                                coroutineScope.launch {
                                    if (repository.preferences.spendDiamonds(35)) {
                                        repository.preferences.addBooster("hint", 1)
                                        soundManager.playSuccessSound()
                                        feedbackMsg = "+1 İpucu eklendi!"
                                    } else {
                                        soundManager.playErrorSound()
                                        feedbackMsg = "Yetersiz elmas!"
                                    }
                                }
                            }
                        )
                    }
                    item {
                        BoosterPurchaseCard(
                            title = "Ekstra Süre (+30s)",
                            description = "Zamana karşı oynanan refleks ve kaçış oyunlarında +30 saniye ekler.",
                            icon = "⏳",
                            currentCount = extraTimeCount,
                            cost = 50,
                            onBuy = {
                                coroutineScope.launch {
                                    if (repository.preferences.spendDiamonds(50)) {
                                        repository.preferences.addBooster("time", 1)
                                        soundManager.playSuccessSound()
                                        feedbackMsg = "+1 Ekstra Süre gücü eklendi!"
                                    } else {
                                        soundManager.playErrorSound()
                                        feedbackMsg = "Yetersiz elmas!"
                                    }
                                }
                            }
                        )
                    }
                    item {
                        BoosterPurchaseCard(
                            title = "Hata Telafisi / Geri Al",
                            description = "Yanlış yaptığın hamleyi veya yanlış dokunuşu iptal eder.",
                            icon = "↩️",
                            currentCount = undosCount,
                            cost = 40,
                            onBuy = {
                                coroutineScope.launch {
                                    if (repository.preferences.spendDiamonds(40)) {
                                        repository.preferences.addBooster("undo", 1)
                                        soundManager.playSuccessSound()
                                        feedbackMsg = "+1 Geri Alma gücü eklendi!"
                                    } else {
                                        soundManager.playErrorSound()
                                        feedbackMsg = "Yetersiz elmas!"
                                    }
                                }
                            }
                        )
                    }
                }
                2 -> {
                    // Free Rewarded Ad & Diamonds Bundles
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = PastelYellow.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.PlayCircle, contentDescription = null, tint = com.example.ui.theme.NeonOrange, modifier = Modifier.size(28.dp))
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text("Ücretsiz Elmas (Ödüllü Reklam)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                                            Text("Günde en fazla 3 kez izlenebilir ($dailyAdsCount/3)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Button(
                                    onClick = {
                                        coroutineScope.launch {
                                            val rewarded = repository.preferences.recordRewardedAdView()
                                            if (rewarded) {
                                                soundManager.playSuccessSound()
                                                soundManager.vibrateSuccess()
                                                feedbackMsg = "Tebrikler! +3 Elmas kazandın 💎"
                                            } else {
                                                soundManager.playErrorSound()
                                                feedbackMsg = "Günlük reklam limitine (3/3) ulaştınız. Yarın tekrar izleyebilirsiniz."
                                            }
                                        }
                                    },
                                    enabled = dailyAdsCount < 3,
                                    colors = ButtonDefaults.buttonColors(containerColor = com.example.ui.theme.NeonOrange),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Reklamı İzle (+3 Elmas)", color = com.example.ui.theme.OnNeon, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Pro Membership
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = PrimaryCoral),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.WorkspacePremium, contentDescription = null, tint = com.example.ui.theme.OnNeon, modifier = Modifier.size(32.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("ZihinPark PRO Üyelik", fontWeight = FontWeight.ExtraBold, color = com.example.ui.theme.OnNeon, style = MaterialTheme.typography.titleLarge)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Tüm özel avatarlar ve çerçevelere tam erişim, gelişmiş performans analizleri, reklamsız deneyim ve haftalık ekstra görevler.",
                                    color = com.example.ui.theme.OnNeon.copy(alpha = 0.9f),
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                Button(
                                    onClick = {
                                        coroutineScope.launch {
                                            repository.preferences.setProMember(true)
                                            soundManager.playSuccessSound()
                                            feedbackMsg = "ZihinPark PRO üyeliğiniz aktif edildi! 🎉"
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = com.example.ui.theme.NeonBg),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(if (isProMember) "PRO Üyeliğiniz Aktif ✓" else "Pro'ya Geç (79,99 ₺ / Ay)", color = PrimaryCoral, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Google Play Diamond Packages
                    item {
                        Text("Google Play Elmas Paketleri", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    }
                    item {
                        DiamondBundleItem(name = "Başlangıç Paketi", diamonds = 250, price = "29,99 ₺", onBuy = {
                            coroutineScope.launch {
                                repository.preferences.addDiamonds(250)
                                soundManager.playSuccessSound()
                                feedbackMsg = "+250 Elmas hesabına tanımlandı!"
                            }
                        })
                    }
                    item {
                        DiamondBundleItem(name = "Orta Paket", diamonds = 1600, price = "119,99 ₺", onBuy = {
                            coroutineScope.launch {
                                repository.preferences.addDiamonds(1600)
                                soundManager.playSuccessSound()
                                feedbackMsg = "+1.600 Elmas hesabına tanımlandı!"
                            }
                        })
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun BoosterPurchaseCard(
    title: String,
    description: String,
    icon: String,
    currentCount: Int,
    cost: Int,
    onBuy: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Text(icon, fontSize = 32.sp)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text("Mevcut: $currentCount Adet", style = MaterialTheme.typography.labelSmall, color = MintGreenDark, fontWeight = FontWeight.Bold)
                    Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Button(
                onClick = onBuy,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryCoral),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("💎 $cost", color = com.example.ui.theme.OnNeon, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun DiamondBundleItem(
    name: String,
    diamonds: Int,
    price: String,
    onBuy: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("💎", fontSize = 28.sp)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    Text("+$diamonds Elmas", style = MaterialTheme.typography.labelMedium, color = com.example.ui.theme.NeonOrange, fontWeight = FontWeight.Bold)
                }
            }
            Button(
                onClick = onBuy,
                colors = ButtonDefaults.buttonColors(containerColor = MintGreenDark),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(price, color = com.example.ui.theme.OnNeon, fontWeight = FontWeight.Bold)
            }
        }
    }
}
