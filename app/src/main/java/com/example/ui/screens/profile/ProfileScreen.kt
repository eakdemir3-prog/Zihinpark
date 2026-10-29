package com.example.ui.screens.profile

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.repository.GameRepository
import com.example.notification.NotificationHelper
import com.example.notification.ReminderPreferences
import com.example.ui.theme.MintGreen
import com.example.ui.theme.PastelPeach
import com.example.ui.theme.PastelYellow
import com.example.ui.theme.PrimaryCoral
import com.example.util.SoundAndVibeManager
import kotlinx.coroutines.launch

private val AVATARS = listOf("🧠", "🧑‍🔬", "🕵️", "🦉", "🚀", "💡", "🎨", "🧩")

@Composable
fun ProfileScreen(
    repository: GameRepository,
    soundManager: SoundAndVibeManager
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val reminderPrefs = remember { ReminderPreferences.getInstance(context) }

    val userName by repository.preferences.userName.collectAsState(initial = "Zihin Gezgini")
    val avatarId by repository.preferences.avatarId.collectAsState(initial = 0)
    val totalStars by repository.totalStars.collectAsState(initial = 0)
    val clearedLevels by repository.completedLevelsCount.collectAsState(initial = 0)
    val soundEnabled by repository.preferences.soundEnabled.collectAsState(initial = true)
    val vibrationEnabled by repository.preferences.vibrationEnabled.collectAsState(initial = true)

    val isReminderEnabled by reminderPrefs.isReminderEnabledFlow.collectAsState()
    val reminderHour by reminderPrefs.reminderHourFlow.collectAsState()
    val reminderMinute by reminderPrefs.reminderMinuteFlow.collectAsState()

    var showTimePickerDialog by remember { mutableStateOf(false) }
    var showNameEditDialog by remember { mutableStateOf(false) }
    var tempName by remember { mutableStateOf("") }
    var showResetDialog by remember { mutableStateOf(false) }

    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
        if (isGranted) {
            reminderPrefs.isReminderEnabled = true
            NotificationHelper.scheduleDailyReminder(context, reminderHour, reminderMinute)
            Toast.makeText(context, "Bildirim izni verildi! Günlük hatırlatıcı aktif.", Toast.LENGTH_SHORT).show()
        } else {
            reminderPrefs.isReminderEnabled = false
            NotificationHelper.cancelDailyReminder(context)
            Toast.makeText(context, "Bildirim izni verilmedi. Hatırlatıcı kapalı.", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Profil & Ayarlar",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // User Card
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Current Avatar
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(PastelPeach),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = AVATARS.getOrElse(avatarId) { "🧠" },
                                fontSize = 44.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = userName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(onClick = {
                                tempName = userName
                                showNameEditDialog = true
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "İsmi Düzenle",
                                    tint = PrimaryCoral,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Text(
                            text = "${rankTitle(totalStars ?: 0)} • ⭐ ${totalStars ?: 0} • $clearedLevels seviye",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text("Avatarını Değiştir:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AVATARS.forEachIndexed { index, avatar ->
                                val isSelected = avatarId == index
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) PrimaryCoral.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceVariant)
                                        .clickable {
                                            soundManager.playClickSound()
                                            coroutineScope.launch {
                                                repository.preferences.setAvatarId(index)
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(avatar, fontSize = 18.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Preferences Switches
            item {
                Text(
                    text = "Uygulama Tercihleri",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Sound Effects Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.VolumeUp, contentDescription = null, tint = PrimaryCoral)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Ses Efektleri", fontWeight = FontWeight.SemiBold)
                                    Text("Doğru/yanlış ve buton sesleri", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Switch(
                                checked = soundEnabled,
                                onCheckedChange = {
                                    coroutineScope.launch {
                                        repository.preferences.setSoundEnabled(it)
                                        if (it) soundManager.playSuccessSound(true)
                                    }
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = PrimaryCoral, checkedTrackColor = PrimaryCoral.copy(alpha = 0.4f))
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Vibration Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Vibration, contentDescription = null, tint = PrimaryCoral)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Dokunsal Titreşim", fontWeight = FontWeight.SemiBold)
                                    Text("Tuş vuruşları ve başarı titreşimi", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Switch(
                                checked = vibrationEnabled,
                                onCheckedChange = {
                                    coroutineScope.launch {
                                        repository.preferences.setVibrationEnabled(it)
                                        if (it) soundManager.vibrateSuccess(true)
                                    }
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = PrimaryCoral, checkedTrackColor = PrimaryCoral.copy(alpha = 0.4f))
                            )
                        }
                    }
                }
            }

            // Daily Reminder Settings Card
            item {
                Text(
                    text = "⏰ Günlük Antrenman Hatırlatıcısı",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(
                                    imageVector = if (isReminderEnabled) Icons.Default.NotificationsActive else Icons.Default.Notifications,
                                    contentDescription = "Hatırlatıcı",
                                    tint = PrimaryCoral
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Günlük Hatırlatıcı", fontWeight = FontWeight.SemiBold)
                                    Text(
                                        text = if (isReminderEnabled) "Aktif • Her gün ${reminderPrefs.getFormattedTime()}" else "Kapalı",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isReminderEnabled) MintGreen else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Switch(
                                checked = isReminderEnabled,
                                onCheckedChange = { enable ->
                                    soundManager.playClickSound()
                                    if (enable) {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
                                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        } else {
                                            reminderPrefs.isReminderEnabled = true
                                            NotificationHelper.scheduleDailyReminder(context, reminderHour, reminderMinute)
                                            Toast.makeText(context, "Günlük hatırlatıcı kuruldu: ${reminderPrefs.getFormattedTime()}", Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
                                        reminderPrefs.isReminderEnabled = false
                                        NotificationHelper.cancelDailyReminder(context)
                                        Toast.makeText(context, "Günlük hatırlatıcı kapatıldı.", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.testTag("reminder_switch"),
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = PrimaryCoral,
                                    checkedTrackColor = PrimaryCoral.copy(alpha = 0.4f)
                                )
                            )
                        }

                        if (isReminderEnabled) {
                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.AccessTime,
                                        contentDescription = "Saat",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Hatırlatma Saati:",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = PrimaryCoral.copy(alpha = 0.15f),
                                    modifier = Modifier
                                        .clickable {
                                            soundManager.playClickSound()
                                            showTimePickerDialog = true
                                        }
                                        .testTag("reminder_time_picker")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = reminderPrefs.getFormattedTime(),
                                            fontWeight = FontWeight.Bold,
                                            color = PrimaryCoral,
                                            fontSize = 16.sp
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Saati Değiştir",
                                            tint = PrimaryCoral,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            OutlinedButton(
                                onClick = {
                                    soundManager.playClickSound()
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
                                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        val sent = NotificationHelper.sendImmediateTestNotification(context)
                                        if (sent) {
                                            Toast.makeText(context, "🔔 Test bildirimi gönderildi!", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "⚠️ Bildirimler sistem ayarlarından kapalı.", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(42.dp)
                                    .testTag("test_notification_button")
                            ) {
                                Icon(Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Test Bildirimi Gönder", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "💡 Her gün belirlediğin saatte hafıza ve mantık egzersizlerini hatırlatır, günlük serini korumanı sağlar.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // About ZihinPark Card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = PrimaryCoral)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("ZihinPark Hakkında", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "ZihinPark v2.0 — Düşün. Keşfet. Geliş.\n" +
                                   "16 bağımsız zekâ, mantık ve analitik oyunu içeren %100 çevrimdışı beyin egzersizi platformu. Tüm veriler cihazınızda Room ve DataStore ile güvenle saklanır.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            // Reset All Data Button
            item {
                Button(
                    onClick = { showResetDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = com.example.ui.theme.NeonRed),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text("Tüm Verileri ve İlerlemeyi Sıfırla", fontWeight = FontWeight.Bold, color = com.example.ui.theme.OnNeon)
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Name edit modal
    if (showNameEditDialog) {
        AlertDialog(
            onDismissRequest = { showNameEditDialog = false },
            title = { Text("Kullanıcı Adını Değiştir") },
            text = {
                OutlinedTextField(
                    value = tempName,
                    onValueChange = { tempName = it },
                    label = { Text("İsim") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (tempName.isNotBlank()) {
                        coroutineScope.launch {
                            repository.preferences.setUserName(tempName.trim())
                            showNameEditDialog = false
                        }
                    }
                }) {
                    Text("Kaydet", color = PrimaryCoral, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showNameEditDialog = false }) {
                    Text("İptal")
                }
            }
        )
    }

    // Reset confirmation modal
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Verileri Sıfırla") },
            text = { Text("Tüm seviyeler, yıldızlar, skorlar, günlük seri ve tercihler tamamen sıfırlanacaktır. Bu işlem geri alınamaz.") },
            confirmButton = {
                TextButton(onClick = {
                    coroutineScope.launch {
                        repository.resetAllProgress()
                        repository.preferences.resetAll()
                        showResetDialog = false
                    }
                }) {
                    Text("Tümünü Sil", color = com.example.ui.theme.NeonRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Vazgeç")
                }
            }
        )
    }

    // Reminder Time Picker Dialog
    if (showTimePickerDialog) {
        var selectedHour by remember { mutableIntStateOf(reminderHour) }
        var selectedMinute by remember { mutableIntStateOf(reminderMinute) }

        val presetTimes = listOf(
            Pair(9, 0) to "09:00",
            Pair(13, 0) to "13:00",
            Pair(18, 0) to "18:00",
            Pair(20, 0) to "20:00",
            Pair(21, 30) to "21:30"
        )

        AlertDialog(
            onDismissRequest = { showTimePickerDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AccessTime, contentDescription = null, tint = PrimaryCoral)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Hatırlatma Saatini Seç", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Her gün bu saatte beyin antrenmanı bildirimi alırsınız.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                IconButton(onClick = {
                                    selectedHour = (selectedHour + 1) % 24
                                }) {
                                    Icon(Icons.Default.Add, contentDescription = "Saati artır")
                                }
                                Text(
                                    text = String.format("%02d", selectedHour),
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryCoral
                                )
                                Text("Saat", style = MaterialTheme.typography.labelSmall)
                                IconButton(onClick = {
                                    selectedHour = if (selectedHour == 0) 23 else selectedHour - 1
                                }) {
                                    Icon(Icons.Default.Remove, contentDescription = "Saati azalt")
                                }
                            }

                            Text(
                                text = ":",
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                IconButton(onClick = {
                                    selectedMinute = (selectedMinute + 5) % 60
                                }) {
                                    Icon(Icons.Default.Add, contentDescription = "Dakikayı artır")
                                }
                                Text(
                                    text = String.format("%02d", selectedMinute),
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryCoral
                                )
                                Text("Dakika", style = MaterialTheme.typography.labelSmall)
                                IconButton(onClick = {
                                    selectedMinute = if (selectedMinute < 5) 55 else selectedMinute - 5
                                }) {
                                    Icon(Icons.Default.Remove, contentDescription = "Dakikayı azalt")
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Hızlı Saat Seçenekleri:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        presetTimes.forEach { (time, _) ->
                            val isSelected = selectedHour == time.first && selectedMinute == time.second
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) PrimaryCoral else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.clickable {
                                    selectedHour = time.first
                                    selectedMinute = time.second
                                }
                            ) {
                                Text(
                                    text = String.format("%02d:%02d", time.first, time.second),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) com.example.ui.theme.OnNeon else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    reminderPrefs.setReminderTime(selectedHour, selectedMinute)
                    if (isReminderEnabled) {
                        NotificationHelper.scheduleDailyReminder(context, selectedHour, selectedMinute)
                    }
                    showTimePickerDialog = false
                    Toast.makeText(
                        context,
                        "Hatırlatıcı ${String.format("%02d:%02d", selectedHour, selectedMinute)} olarak ayarlandı.",
                        Toast.LENGTH_SHORT
                    ).show()
                }) {
                    Text("Kaydet", color = PrimaryCoral, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePickerDialog = false }) {
                    Text("İptal")
                }
            }
        )
    }
}


/** Toplam yıldıza göre unvan. */
fun rankTitle(stars: Int): String = when {
    stars >= 2500 -> "Zihin Efsanesi"
    stars >= 1500 -> "Büyük Usta"
    stars >= 800 -> "Usta Stratejist"
    stars >= 400 -> "Keskin Zekâ"
    stars >= 150 -> "Mantık Avcısı"
    stars >= 30 -> "Zihin Kaşifi"
    else -> "Çaylak"
}
