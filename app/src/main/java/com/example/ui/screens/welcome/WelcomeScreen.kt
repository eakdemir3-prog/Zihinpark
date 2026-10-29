package com.example.ui.screens.welcome

import android.app.Activity
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.cloud.CloudSync
import com.example.ui.components.GlassCard
import com.example.ui.components.NeonBackground
import com.example.ui.components.NeonButton
import com.example.ui.components.bounceClick
import com.example.ui.components.neonGlow
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.NeonRed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch

/**
 * İlk açılışta gösterilen karşılama ve giriş ekranı. Giriş isteğe bağlıdır:
 * "Misafir olarak devam et" ile oyunlar hemen açılır.
 */
@Composable
fun WelcomeScreen(onDone: () -> Unit) {
    val context = LocalContext.current
    val phase by CloudSync.phase.collectAsState()
    val g = com.example.cloud.rememberGoogleSignIn(onSuccess = onDone)
    val t = rememberInfiniteTransition(label = "w")
    val float by t.animateFloat(-8f, 8f, infiniteRepeatable(tween(2200), RepeatMode.Reverse), label = "f")

    NeonBackground(accent = NeonPurple, accent2 = NeonCyan) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painterResource(R.drawable.ic_launcher_fg), null,
                Modifier.size(150.dp).graphicsLayer { translationY = float }
            )
            Spacer(Modifier.height(8.dp))
            Text("ZihinPark", color = TextPrimary, fontSize = 40.sp, fontWeight = FontWeight.ExtraBold)
            Text("Düşün. Oyna. Kazan.", color = TextSecondary, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(28.dp))
            GlassCard(accent = NeonCyan, glow = true) {
                listOf(
                    "🧠" to "30 zekâ oyunu, 1.500+ seviye",
                    "🌍" to "300 seviyelik Kelime Diyarı",
                    "☁️" to "İlerlemen buluta yedeklenir",
                    "🏆" to "Haftalık ligde yarış, elmas kazan"
                ).forEach { (e, s) ->
                    Text("$e  $s", color = TextPrimary, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(vertical = 5.dp))
                }
            }
            Spacer(Modifier.height(28.dp))
            if (CloudSync.isConfigured) {
                NeonButton(
                    if (g.busy) (phase.ifBlank { "Bağlanıyor..." }) else "G   Google ile giriş yap",
                    onClick = { g.signIn() },
                    color = NeonCyan, enabled = !g.busy,
                    modifier = Modifier.fillMaxWidth().neonGlow(NeonCyan, 14.dp, 26.dp, 0.5f)
                )
                if (g.showAlternative) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Pencere açılmadı mı? Başka yöntemle dene →", color = NeonCyan, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clip(RoundedCornerShape(10.dp)).bounceClick { g.signInAlternative() }.padding(8.dp)
                    )
                }
                g.error?.let {
                    Spacer(Modifier.height(10.dp))
                    Text(it, color = NeonRed, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                }
                Spacer(Modifier.height(16.dp))
            }
            Text(
                "Misafir olarak devam et",
                color = NeonPink, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clip(RoundedCornerShape(12.dp)).bounceClick(onClick = onDone).padding(12.dp)
            )
            Spacer(Modifier.height(6.dp))
            Text("Giriş yapmadan da tüm oyunlar açık. İstediğin zaman Profil'den giriş yapabilirsin.",
                color = TextMuted, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
        }
    }
}
