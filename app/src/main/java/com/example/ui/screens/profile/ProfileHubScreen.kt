package com.example.ui.screens.profile

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.repository.GameRepository
import com.example.ui.components.bounceClick
import com.example.ui.components.neonGlow
import com.example.ui.screens.progress.ProgressScreen
import com.example.ui.theme.NeonBorder
import com.example.ui.theme.NeonCard
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.OnNeon
import com.example.ui.theme.TextSecondary
import com.example.util.SoundAndVibeManager

/** Profil sekmesi: "Profil" ve "İlerleme" arasında kayan geçişli iki bölüm. */
@Composable
fun ProfileHubScreen(
    repository: GameRepository,
    soundManager: SoundAndVibeManager,
    onOpenGames: () -> Unit,
    onOpenWeeklyTrends: () -> Unit,
    onOpenShop: () -> Unit = {},
    accountCard: @Composable () -> Unit = {}
) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Box(Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp)) {
            SegmentedTabs(listOf("Profil", "İlerleme"), tab) { tab = it; soundManager.playTap() }
        }
        AnimatedContent(
            targetState = tab,
            transitionSpec = {
                val dir = if (targetState > initialState) 1 else -1
                (slideInHorizontally(tween(320)) { it * dir / 3 } + fadeIn(tween(320)))
                    .togetherWith(slideOutHorizontally(tween(260)) { -it * dir / 3 } + fadeOut(tween(200)))
            },
            label = "profileTabs"
        ) { t ->
            if (t == 0) ProfileScreen(repository, soundManager, onOpenShop, accountCard)
            else ProgressScreen(
                repository = repository,
                onNavigateToGames = onOpenGames,
                onNavigateToWeeklyTrends = onOpenWeeklyTrends
            )
        }
    }
}

/** Kayan neon göstergeli bölüm seçici. */
@Composable
fun SegmentedTabs(labels: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(shape)
            .background(NeonCard)
            .border(1.dp, NeonBorder, shape)
            .padding(4.dp)
    ) {
        val segW = maxWidth / labels.size
        val x by animateDpAsState(segW * selected, spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow), label = "seg")
        Box(
            Modifier
                .offset(x = x)
                .width(segW)
                .fillMaxHeight()
                .neonGlow(NeonCyan, 10.dp, 12.dp, 0.5f)
                .clip(RoundedCornerShape(12.dp))
                .background(Brush.horizontalGradient(listOf(NeonCyan, NeonPurple)))
        )
        Row(Modifier.fillMaxSize()) {
            labels.forEachIndexed { i, l ->
                Box(
                    Modifier.weight(1f).fillMaxHeight().bounceClick { onSelect(i) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        l, color = if (i == selected) OnNeon else TextSecondary,
                        style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
