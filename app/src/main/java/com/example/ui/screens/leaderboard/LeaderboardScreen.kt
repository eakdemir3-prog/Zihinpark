package com.example.ui.screens.leaderboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.GameRepository
import com.example.ui.theme.MintGreen
import com.example.ui.theme.MintGreenDark
import com.example.ui.theme.PastelPeach
import com.example.ui.theme.PastelYellow
import com.example.ui.theme.PrimaryCoral

data class LeaderboardPlayer(
    val rank: Int,
    val name: String,
    val avatar: String,
    val score: Int,
    val reward: String
)

@Composable
fun LeaderboardScreen(
    repository: GameRepository
) {
    val userName by repository.preferences.userName.collectAsState(initial = "Zihin Gezgini")
    val totalScore by repository.totalScore.collectAsState(initial = 0)

    val topPlayers = remember(userName, totalScore) {
        listOf(
            LeaderboardPlayer(1, "Eren (Kıdemli)", "🧠", maxOf(totalScore ?: 0, 840) + 120, "1.000 💎"),
            LeaderboardPlayer(2, userName, "🧑‍🔬", totalScore ?: 0, "600 💎"),
            LeaderboardPlayer(3, "Selin", "🦉", maxOf((totalScore ?: 0) - 50, 420), "300 💎"),
            LeaderboardPlayer(4, "Barış", "⚡", 380, "100 💎"),
            LeaderboardPlayer(5, "Gizem", "🎨", 310, "50 💎")
        ).sortedByDescending { it.score }.mapIndexed { index, p -> p.copy(rank = index + 1) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Haftalık Liderlik", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("En Yüksek Performans Sıralaması", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(PastelPeach.copy(alpha = 0.5f))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Timer, contentDescription = null, tint = PrimaryCoral, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Kalan: 4 Gün", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = PrimaryCoral)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Podium Card (Top 3)
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("HAFTANIN ŞAMPİYONLARI", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            val second = topPlayers.getOrNull(1)
                            val first = topPlayers.getOrNull(0)
                            val third = topPlayers.getOrNull(2)

                            // 2nd Place
                            if (second != null) {
                                PodiumColumn(player = second, podiumHeight = 80.dp, podiumColor = Color(0xFFE2E8F0), medal = "🥈")
                            }
                            // 1st Place
                            if (first != null) {
                                PodiumColumn(player = first, podiumHeight = 110.dp, podiumColor = PastelYellow, medal = "🥇")
                            }
                            // 3rd Place
                            if (third != null) {
                                PodiumColumn(player = third, podiumHeight = 65.dp, podiumColor = PastelPeach, medal = "🥉")
                            }
                        }
                    }
                }
            }

            item {
                Text("Tüm Sıralama", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            }

            items(topPlayers) { player ->
                val isMe = player.name == userName
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isMe) PrimaryCoral.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface
                    ),
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
                            Text(
                                text = "#${player.rank}",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = if (player.rank <= 3) PrimaryCoral else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(player.avatar, fontSize = 28.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = player.name + if (isMe) " (Sen)" else "",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = "${player.score} Puan",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(PastelYellow)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(player.reward, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFF92400E))
                        }
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
fun PodiumColumn(
    player: LeaderboardPlayer,
    podiumHeight: androidx.compose.ui.unit.Dp,
    podiumColor: Color,
    medal: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(medal, fontSize = 24.sp)
        Text(player.avatar, fontSize = 28.sp)
        Spacer(modifier = Modifier.height(2.dp))
        Text(player.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall, maxLines = 1)
        Text("${player.score} P", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .width(80.dp)
                .height(podiumHeight)
                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                .background(podiumColor),
            contentAlignment = Alignment.Center
        ) {
            Text("#${player.rank}", fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}
