package com.example.ui.screens.profile

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.repository.GameRepository
import com.example.domain.model.Cosmetics
import com.example.notification.NotificationHelper
import com.example.notification.ReminderPreferences
import com.example.ui.components.GlassCard
import com.example.ui.components.NeonButton
import com.example.ui.components.NeonChip
import com.example.ui.components.SectionTitle
import com.example.ui.components.bounceClick
import com.example.ui.components.neonGlow
import com.example.ui.theme.NeonBorder
import com.example.ui.theme.NeonCard
import com.example.ui.theme.NeonCardHigh
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.NeonRed
import com.example.ui.theme.NeonSurface
import com.example.ui.theme.NeonYellow
import com.example.ui.theme.OnNeon
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.SoundAndVibeManager
import kotlinx.coroutines.launch

/** Avatar ve takılı çerçeve (dönen neon halka). */
@Composable
fun ProfileAvatar(avatarId: Int, frameId: String, size: Dp) {
    val frame = Cosmetics.frame(frameId)
    val animated = frame.id != "none"
    val rot = if (animated) {
        val t = rememberInfiniteTransition(label = "frame")
        t.animateFloat(0f, 360f, infiniteRepeatable(tween(6000, easing = LinearEasing), RepeatMode.Restart), label = "r").value
    } else 0f
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(size)
                .then(if (animated) Modifier.neonGlow(frame.colors.first(), size * 0.18f, size, 0.6f) else Modifier)
                .graphicsLayer { rotationZ = rot }
                .clip(CircleShape)
                .background(
                    if (frame.colors.size > 1) Brush.sweepGradient(frame.colors + frame.colors.first())
                    else Brush.linearGradient(listOf(frame.colors.first(), frame.colors.first()))
                )
        )
        Box(
            Modifier
                .size(size * if (animated) 0.86f else 0.96f)
                .clip(CircleShape)
                .background(NeonCard),
            contentAlignment = Alignment.Center
        ) {
            Text(Cosmetics.avatar(avatarId).emoji, fontSize = (size.value * 0.5f).sp)
        }
    }
}

@Composable
fun ProfileScreen(
    repository: GameRepository,
    soundManager: SoundAndVibeManager,
    onOpenShop: () -> Unit = {},
    accountCard: @Composable () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val reminderPrefs = remember { ReminderPreferences.getInstance(context) }
    val prefs = repository.preferences

    val userName by prefs.userName.collectAsState(initial = "Zihin Gezgini")
    val avatarId by prefs.avatarId.collectAsState(initial = 0)
    val frameId by prefs.equippedFrame.collectAsState(initial = "none")
    val owned by prefs.ownedCosmetics.collectAsState(initial = emptySet())
    val soundEnabled by prefs.soundEnabled.collectAsState(initial = true)
    val lightTheme by prefs.lightTheme.collectAsState(initial = com.example.ui.theme.ZpColors.palette.isLight)
    val vibrationEnabled by prefs.vibrationEnabled.collectAsState(initial = true)
    val totalStars by repository.totalStars.collectAsState(initial = 0)
    val cleared by repository.completedLevelsCount.collectAsState(initial = 0)
    val diamonds by prefs.diamondBalance.collectAsState(initial = 0)
    val streak by prefs.dailyStreak.collectAsState(initial = 0)

    val reminderOn by reminderPrefs.isReminderEnabledFlow.collectAsState()
    val reminderHour by reminderPrefs.reminderHourFlow.collectAsState()
    val reminderMinute by reminderPrefs.reminderMinuteFlow.collectAsState()

    var editName by remember { mutableStateOf(false) }
    var editTime by remember { mutableStateOf(false) }
    var confirmReset by remember { mutableStateOf(false) }

    var hasPermission by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasPermission = granted
        reminderPrefs.isReminderEnabled = granted
        if (granted) NotificationHelper.scheduleDailyReminder(context, reminderHour, reminderMinute)
        else NotificationHelper.cancelDailyReminder(context)
        Toast.makeText(context, if (granted) "Günlük hatırlatıcı açıldı" else "Bildirim izni verilmedi", Toast.LENGTH_SHORT).show()
    }

    val stars = totalStars ?: 0
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            GlassCard(accent = NeonPurple, glow = true) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    ProfileAvatar(avatarId, frameId, 96.dp)
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.bounceClick { editName = true }) {
                        Text(userName, style = MaterialTheme.typography.headlineSmall, color = TextPrimary)
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.Default.Edit, contentDescription = "İsmi düzenle", tint = NeonCyan, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.height(4.dp))
                    NeonChip(rankTitle(stars), NeonYellow)
                    Spacer(Modifier.height(14.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        MiniStat("⭐", "$stars", "Yıldız")
                        MiniStat("🏁", "$cleared", "Seviye")
                        MiniStat("🔥", "$streak", "Seri")
                        MiniStat("💎", "$diamonds", "Elmas")
                    }
                }
            }
        }

        item { accountCard() }

        item {
            SectionTitle("Avatarım", action = "Mağaza ›", onAction = onOpenShop)
            Spacer(Modifier.height(10.dp))
            val mine = Cosmetics.AVATARS.filter { Cosmetics.owns(owned, it) }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(mine, key = { it.id }) { a ->
                    val sel = a.id == avatarId
                    Box(
                        Modifier
                            .size(52.dp)
                            .then(if (sel) Modifier.neonGlow(NeonCyan, 10.dp, 26.dp, 0.7f) else Modifier)
                            .clip(CircleShape)
                            .background(if (sel) NeonCyan.copy(alpha = 0.2f) else NeonCard)
                            .border(if (sel) 2.dp else 1.dp, if (sel) NeonCyan else NeonBorder, CircleShape)
                            .bounceClick { soundManager.uiTick(); scope.launch { prefs.setAvatarId(a.id) } },
                        contentAlignment = Alignment.Center
                    ) { Text(a.emoji, fontSize = 26.sp) }
                }
                item {
                    Box(
                        Modifier.size(52.dp).clip(CircleShape).background(NeonPink.copy(alpha = 0.12f))
                            .border(1.dp, NeonPink, CircleShape).bounceClick(onClick = onOpenShop),
                        contentAlignment = Alignment.Center
                    ) { Text("＋", color = NeonPink, fontSize = 24.sp, fontWeight = FontWeight.Bold) }
                }
            }
        }

        item {
            SectionTitle("Ayarlar")
            Spacer(Modifier.height(10.dp))
            GlassCard(accent = NeonCyan, contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)) {
                SettingRow("☀️", "Açık tema", if (lightTheme) "Lavanta açık tema" else "Neon koyu tema", lightTheme) {
                    com.example.ui.theme.ZpColors.palette = if (it) com.example.ui.theme.LightPalette else com.example.ui.theme.NeonPalette
                    scope.launch { prefs.setLightTheme(it) }
                }
                SettingRow("🔊", "Ses efektleri", "Doğru, yanlış ve buton sesleri", soundEnabled) {
                    scope.launch { prefs.setSoundEnabled(it); if (it) soundManager.playCorrect() }
                }
                SettingRow("📳", "Titreşim", "Dokunuş ve başarı titreşimi", vibrationEnabled) {
                    scope.launch { prefs.setVibrationEnabled(it); if (it) soundManager.vibrateSuccess() }
                }
                SettingRow("⏰", "Günlük hatırlatıcı", if (reminderOn) "Her gün ${reminderPrefs.getFormattedTime()}" else "Kapalı", reminderOn) { enable ->
                    if (enable) {
                        if (!hasPermission) permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        else {
                            reminderPrefs.isReminderEnabled = true
                            NotificationHelper.scheduleDailyReminder(context, reminderHour, reminderMinute)
                        }
                    } else {
                        reminderPrefs.isReminderEnabled = false
                        NotificationHelper.cancelDailyReminder(context)
                    }
                }
                if (reminderOn) {
                    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        NeonButton("Saat: ${reminderPrefs.getFormattedTime()}", { editTime = true }, color = NeonCyan, outlined = true, height = 42.dp, modifier = Modifier.weight(1f))
                        NeonButton("Dene", {
                            val ok = NotificationHelper.sendImmediateTestNotification(context)
                            Toast.makeText(context, if (ok) "🔔 Test bildirimi gönderildi" else "Bildirimler kapalı", Toast.LENGTH_SHORT).show()
                        }, color = NeonPurple, outlined = true, height = 42.dp)
                    }
                }
            }
        }

        item {
            GlassCard(accent = NeonBorder) {
                Text("ZihinPark v2.0", color = TextPrimary, style = MaterialTheme.typography.titleSmall)
                Text("EA Vision Labs · 30 oyun, 1500 seviye + Kelime Diyarı", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(12.dp))
                NeonButton("Tüm ilerlemeyi sıfırla", { confirmReset = true }, color = NeonRed, outlined = true, height = 42.dp, modifier = Modifier.fillMaxWidth())
            }
        }
    }

    if (editName) {
        var temp by remember { mutableStateOf(userName) }
        NeonDialog(
            title = "Kullanıcı adı",
            onDismiss = { editName = false },
            confirm = "Kaydet",
            onConfirm = {
                val t = temp.trim().take(20)
                if (t.isNotEmpty()) scope.launch { prefs.setUserName(t) }
                editName = false
            }
        ) {
            val shape = RoundedCornerShape(12.dp)
            BasicTextField(
                value = temp, onValueChange = { if (it.length <= 20) temp = it }, singleLine = true,
                textStyle = MaterialTheme.typography.titleMedium.copy(color = TextPrimary),
                cursorBrush = SolidColor(NeonCyan),
                modifier = Modifier.fillMaxWidth().clip(shape).background(NeonCardHigh).border(1.dp, NeonCyan, shape).padding(14.dp)
            )
        }
    }

    if (editTime) {
        var h by remember { mutableIntStateOf(reminderHour) }
        var m by remember { mutableIntStateOf(reminderMinute) }
        NeonDialog(
            title = "Hatırlatma saati",
            onDismiss = { editTime = false },
            confirm = "Kaydet",
            onConfirm = {
                reminderPrefs.setReminderTime(h, m)
                if (reminderPrefs.isReminderEnabled) NotificationHelper.scheduleDailyReminder(context, h, m)
                editTime = false
            }
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Spinner(h, 0..23) { h = it }
                Text(" : ", color = TextPrimary, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Spinner(m, 0..55 step 5) { m = it }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                listOf(8 to 0, 12 to 30, 18 to 0, 21 to 0).forEach { (hh, mm) ->
                    Text(
                        "%02d:%02d".format(hh, mm), color = TextPrimary, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f).clip(RoundedCornerShape(10.dp)).background(NeonCard)
                            .border(1.dp, NeonBorder, RoundedCornerShape(10.dp)).bounceClick { h = hh; m = mm }.padding(vertical = 8.dp)
                    )
                }
            }
        }
    }

    if (confirmReset) {
        NeonDialog(
            title = "Her şeyi sıfırla?",
            onDismiss = { confirmReset = false },
            confirm = "Sıfırla",
            confirmColor = NeonRed,
            onConfirm = {
                scope.launch {
                    repository.resetAllProgress()
                    prefs.resetAll()
                    Toast.makeText(context, "İlerleme sıfırlandı", Toast.LENGTH_SHORT).show()
                }
                confirmReset = false
            }
        ) {
            Text(
                "Tüm seviyeler, yıldızlar, elmaslar, görünümler ve ayarlar silinir. Bu işlem geri alınamaz.",
                color = TextSecondary, style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun MiniStat(emoji: String, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(emoji, fontSize = 18.sp)
        Text(value, color = TextPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
        Text(label, color = TextSecondary, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun SettingRow(emoji: String, title: String, sub: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(emoji, fontSize = 22.sp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = TextPrimary, style = MaterialTheme.typography.titleSmall)
            Text(sub, color = if (checked) NeonGreen else TextSecondary, style = MaterialTheme.typography.bodySmall)
        }
        Switch(
            checked = checked, onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = OnNeon, checkedTrackColor = NeonCyan,
                uncheckedThumbColor = TextMuted, uncheckedTrackColor = NeonCardHigh, uncheckedBorderColor = NeonBorder
            )
        )
    }
}

@Composable
private fun Spinner(value: Int, range: IntProgression, onChange: (Int) -> Unit) {
    val values = range.toList()
    val idx = values.indexOf(value).coerceAtLeast(0)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("▲", color = NeonCyan, fontSize = 20.sp, modifier = Modifier.bounceClick { onChange(values[(idx + 1) % values.size]) }.padding(8.dp))
        Text("%02d".format(values[idx]), color = TextPrimary, fontSize = 34.sp, fontWeight = FontWeight.ExtraBold)
        Text("▼", color = NeonCyan, fontSize = 20.sp, modifier = Modifier.bounceClick { onChange(values[(idx - 1 + values.size) % values.size]) }.padding(8.dp))
    }
}

/** Neon temalı onay penceresi. */
@Composable
fun NeonDialog(
    title: String,
    onDismiss: () -> Unit,
    confirm: String,
    onConfirm: () -> Unit,
    confirmColor: androidx.compose.ui.graphics.Color = NeonCyan,
    content: @Composable () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NeonSurface,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        shape = RoundedCornerShape(24.dp),
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        text = { Column { content() } },
        confirmButton = { NeonButton(confirm, onConfirm, color = confirmColor, height = 44.dp) },
        dismissButton = { NeonButton("Vazgeç", onDismiss, color = TextSecondary, outlined = true, height = 44.dp) }
    )
}

/** Toplam yıldıza göre unvan. */
fun rankTitle(stars: Int): String = when {
    stars >= 3000 -> "Zihin Efsanesi"
    stars >= 1800 -> "Büyük Usta"
    stars >= 900 -> "Usta Stratejist"
    stars >= 400 -> "Keskin Zekâ"
    stars >= 150 -> "Mantık Avcısı"
    stars >= 30 -> "Zihin Kaşifi"
    else -> "Çaylak"
}

