package com.example.ui.screens.games

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.GameRepository
import com.example.ui.components.GameHeader
import com.example.ui.components.GameResultDialog
import com.example.ui.components.InfoTipBox
import com.example.ui.theme.MintGreen
import com.example.ui.theme.MintGreenDark
import com.example.ui.theme.PastelLavender
import com.example.ui.theme.PastelPeach
import com.example.ui.theme.PastelYellow
import com.example.ui.theme.PrimaryCoral
import com.example.util.SoundAndVibeManager
import kotlinx.coroutines.delay

// ==========================================
// 1. MANTIK DEDEKTİFİ (LOGIC DETECTIVE)
// ==========================================
data class SuspectItem(val id: Int, val name: String, val role: String, val avatar: String)
data class CaseData(
    val title: String,
    val story: String,
    val suspects: List<SuspectItem>,
    val clues: List<String>,
    val correctSuspectId: Int,
    val explanation: String
)

private val LOGIC_CASES = listOf(
    CaseData(
        title = "Kayıp Saat Vakası",
        story = "Müzedeki tarihi saat tam 14:00'te sergi salonundan kayboldu. Güvenlik kameraları arızalandı ancak 3 personelin hareketleri incelendi.",
        suspects = listOf(
            SuspectItem(1, "Ahmet", "Güvenlik Amiri", "👮‍♂️"),
            SuspectItem(2, "Ayşe", "Müze Rehberi", "👩‍💼"),
            SuspectItem(3, "Mehmet", "Temizlik Sorumlusu", "🧹")
        ),
        clues = listOf(
            "Ahmet, 13:50 ile 14:10 arasında ana kapı kontrol noktasındaydı ve ziyaretçi kayıtlarını inceliyordu.",
            "Ayşe saat 13:55'te sergi salonu yedek anahtarını düşürdüğünü söyledi, fakat yedek anahtar sadece kendi dolabında şifreyle kilitliydi.",
            "Mehmet müzeye vardiya başlangıcı için saat 14:15'te kart basarak giriş yaptı."
        ),
        correctSuspectId = 2,
        explanation = "Ayşe yedek anahtarı kaybettiğini iddia etse de anahtar sadece onda şifreyle saklıydı ve saat 14:00'te salonda tek kişiydi!"
    ),
    CaseData(
        title = "Gece Kasası Gizemi",
        story = "Gece yarısı şirket kasasından önemli evraklar alındı. Alarm çalmadı, yani kasanın 4 haneli şifresi biliniyordu.",
        suspects = listOf(
            SuspectItem(1, "Can", "Genel Müdür", "👨‍💼"),
            SuspectItem(2, "Berna", "Muhasebeci", "👩‍💻"),
            SuspectItem(3, "Kerem", "Sistem Yöneticisi", "🧑‍🔧")
        ),
        clues = listOf(
            "Kasa şifresi en son 2 gün önce sadece Berna tarafından yenilendi.",
            "Can o gece şehir dışı uçuşundaydı ve bilet kontrol kayıtları doğrulandı.",
            "Kerem güvenlik sistem loglarında kasanın şifresinin doğru girildiğini ve zorlanmadığını raporladı."
        ),
        correctSuspectId = 2,
        explanation = "Berna yeni şifreyi kimseye vermediğini söylemesine rağmen şifre doğru girilmişti!"
    )
)

@Composable
fun LogicDetectiveGameScreen(
    repository: GameRepository,
    soundManager: SoundAndVibeManager,
    onBack: () -> Unit
) {
    var caseIndex by remember { mutableIntStateOf(0) }
    val currentCase = LOGIC_CASES[caseIndex % LOGIC_CASES.size]
    var selectedSuspectId by remember { mutableStateOf<Int?>(null) }
    var isResultOpen by remember { mutableStateOf(false) }
    var isSuccess by remember { mutableStateOf(false) }
    var score by remember { mutableIntStateOf(0) }
    var revealedClues by remember { mutableIntStateOf(1) }

    LaunchedEffect(caseIndex) {
        selectedSuspectId = null
        revealedClues = 1
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        GameHeader(
            title = "Mantık Dedektifi",
            score = score,
            onBack = onBack,
            statusText = "Vaka: ${currentCase.title}"
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("📁", fontSize = 24.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = currentCase.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = currentCase.story,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                Text(
                    text = "Dedektif İpuçları ($revealedClues/${currentCase.clues.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            items(currentCase.clues.take(revealedClues)) { clue ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = PastelLavender.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text("🔍", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = clue,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            if (revealedClues < currentCase.clues.size) {
                item {
                    Button(
                        onClick = {
                            soundManager.playClickSound()
                            revealedClues++
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("+ Sonraki İpucunu Aç", color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }

            item {
                Text(
                    text = "Şüpheliler (Suçluyu Seç):",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            items(currentCase.suspects) { suspect ->
                val isSelected = selectedSuspectId == suspect.id
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) PrimaryCoral.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) PrimaryCoral else MaterialTheme.colorScheme.outline,
                            shape = RoundedCornerShape(14.dp)
                        )
                        .clickable {
                            soundManager.playClickSound()
                            selectedSuspectId = suspect.id
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(suspect.avatar, fontSize = 28.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = suspect.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = suspect.role,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(PrimaryCoral),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        val correct = selectedSuspectId == currentCase.correctSuspectId
                        isSuccess = correct
                        val addedScore = if (correct) 100 else 20
                        score += addedScore
                        if (correct) {
                            soundManager.playSuccessSound()
                            soundManager.vibrateSuccess()
                        } else {
                            soundManager.playErrorSound()
                            soundManager.vibrateError()
                        }
                        isResultOpen = true
                    },
                    enabled = selectedSuspectId != null,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryCoral),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("submit_verdict_button")
                ) {
                    Text("Kararımı Açıkla ve Tutukla", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }

    GameResultDialog(
        isOpen = isResultOpen,
        title = if (isSuccess) "Harika Dedektiflik!" else "Yanlış Şüpheli!",
        message = if (isSuccess) currentCase.explanation else "Bu şüphelinin geçerli bir mazereti vardı. Tekrar dene!",
        score = if (isSuccess) 100 else 20,
        isSuccess = isSuccess,
        onPlayAgain = {
            isResultOpen = false
            caseIndex++
        },
        onBackToHub = onBack
    )
}

// ==========================================
// 4. ŞİFRE KIRICI (CIPHER BREAKER)
// ==========================================
private const val TURKISH_ALPHABET = "ABCÇDEFGĞHIİJKLMNOÖPRSŞTUÜVYZ"

data class CipherWord(val original: String, val shift: Int, val hint: String)

private val CIPHER_WORDS = listOf(
    CipherWord("BEYİN", 2, "Düşünme ve idrak organımız (2 adım kaydırıldı)"),
    CipherWord("MANTIK", 3, "Doğru düşünme sanatı ve kuralları (3 adım kaydırıldı)"),
    CipherWord("ZEKA", 1, "Çözüm üretme ve öğrenme kapasitesi (1 adım kaydırıldı)")
)

@Composable
fun CipherBreakerGameScreen(
    repository: GameRepository,
    soundManager: SoundAndVibeManager,
    onBack: () -> Unit
) {
    var levelIndex by remember { mutableIntStateOf(0) }
    val currentLevel = CIPHER_WORDS[levelIndex % CIPHER_WORDS.size]
    var userShift by remember { mutableIntStateOf(0) }
    var userInput by remember { mutableStateOf("") }
    var isResultOpen by remember { mutableStateOf(false) }
    var isSuccess by remember { mutableStateOf(false) }
    var score by remember { mutableIntStateOf(0) }

    // Encrypt word with level shift
    val encryptedText = remember(currentLevel) {
        currentLevel.original.map { c ->
            val idx = TURKISH_ALPHABET.indexOf(c)
            if (idx != -1) {
                TURKISH_ALPHABET[(idx + currentLevel.shift) % TURKISH_ALPHABET.length]
            } else c
        }.joinToString("")
    }

    // Live preview deciphered with user's selected shift
    val previewDecrypted = remember(userShift, encryptedText) {
        encryptedText.map { c ->
            val idx = TURKISH_ALPHABET.indexOf(c)
            if (idx != -1) {
                val newIdx = (idx - userShift + TURKISH_ALPHABET.length) % TURKISH_ALPHABET.length
                TURKISH_ALPHABET[newIdx]
            } else c
        }.joinToString("")
    }

    LaunchedEffect(levelIndex) {
        userShift = 0
        userInput = ""
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        GameHeader(
            title = "Şifre Kırıcı",
            score = score,
            onBack = onBack,
            statusText = "Sezar İkame Çözücü"
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                InfoTipBox(text = "İpucu: ${currentLevel.hint}. Sezar kaydırma sürgüsünü ayarlayarak veya kelimeyi yazarak şifreyi çöz.")
            }

            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("KİLİTLİ ŞİFRELİ METİN", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.Center) {
                            encryptedText.forEach { ch ->
                                Box(
                                    modifier = Modifier
                                        .padding(4.dp)
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(PrimaryCoral.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = ch.toString(), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = PrimaryCoral)
                                }
                            }
                        }
                    }
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = PastelLavender.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Kaydırma Anahtarı (Shift):", fontWeight = FontWeight.Bold)
                            Text("-$userShift Adım", fontWeight = FontWeight.Bold, color = PrimaryCoral)
                        }
                        Slider(
                            value = userShift.toFloat(),
                            onValueChange = {
                                userShift = it.toInt()
                                soundManager.playClickSound()
                            },
                            valueRange = 0f..10f,
                            steps = 9,
                            colors = SliderDefaults.colors(
                                thumbColor = PrimaryCoral,
                                activeTrackColor = PrimaryCoral
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text("Canlı Önizleme: ", style = MaterialTheme.typography.bodyMedium)
                            Text(previewDecrypted, fontWeight = FontWeight.Bold, color = MintGreenDark, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = userInput,
                    onValueChange = { userInput = it.uppercase() },
                    label = { Text("Çözülen Kelimeyi Girin") },
                    placeholder = { Text(previewDecrypted) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            item {
                Button(
                    onClick = {
                        val trimmed = userInput.trim().ifEmpty { previewDecrypted }
                        val correct = trimmed.equals(currentLevel.original, ignoreCase = true)
                        isSuccess = correct
                        val added = if (correct) 120 else 25
                        score += added
                        if (correct) {
                            soundManager.playSuccessSound()
                            soundManager.vibrateSuccess()
                        } else {
                            soundManager.playErrorSound()
                            soundManager.vibrateError()
                        }
                        isResultOpen = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryCoral),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("verify_cipher_button")
                ) {
                    Text("Şifreyi Doğrula", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }

    GameResultDialog(
        isOpen = isResultOpen,
        title = if (isSuccess) "Tebrikler! Şifre Çözüldü" else "Hatalı Çözüm",
        message = if (isSuccess) "Gizli kelime: '${currentLevel.original}'. Sezar algoritmasını başarıyla kırdınız!" else "Harfleri kaydırma katsayısını kontrol edip tekrar deneyin.",
        score = if (isSuccess) 120 else 25,
        isSuccess = isSuccess,
        onPlayAgain = {
            isResultOpen = false
            levelIndex++
        },
        onBackToHub = onBack
    )
}

// ==========================================
// 7. TERSİNE DÜŞÜN (REVERSE THINKING)
// ==========================================
data class RiddleItem(
    val id: Int,
    val question: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String
)

private val RIDDLES = listOf(
    RiddleItem(
        1,
        "Bir adam şiddetli sağanak yağmur altında şapkasız ve şemsiyesiz 20 dakika yürüdü, ancak tek bir saç teli bile ıslanmadı. Bu nasıl mümkündür?",
        listOf("Su geçirmez sprey sıktı", "Adam tamamen keldir", "Ağaçların altından koştu", "Yağmurluk kapüşonu taktı"),
        1,
        "Adam kel olduğu için ıslanacak saç teli yoktur!"
    ),
    RiddleItem(
        2,
        "Elektrikli bir tren kuzeye doğru saatte 80 km hızla gidiyor, rüzgar ise güneye doğru saatte 20 km hızla esiyor. Trenin dumanı hangi yöne savrulur?",
        listOf("Güneye", "Kuzeye", "Doğuya", "Hiçbir yöne, elektrikli tren duman çıkarmaz"),
        3,
        "Tren elektriklidir, dolayısıyla duman üretmez!"
    ),
    RiddleItem(
        3,
        "Bazı aylar 30 gün, bazıları 31 gündür. Kaç ayda 28 gün vardır?",
        listOf("Sadece 1 (Şubat)", "4 ay", "Tüm 12 ayda", "Hiçbirinde"),
        2,
        "Tüm 12 ayın hepsinde en az 28 gün vardır!"
    )
)

@Composable
fun ReverseThinkingGameScreen(
    repository: GameRepository,
    soundManager: SoundAndVibeManager,
    onBack: () -> Unit
) {
    var riddleIndex by remember { mutableIntStateOf(0) }
    val riddle = RIDDLES[riddleIndex % RIDDLES.size]
    var selectedOption by remember { mutableStateOf<Int?>(null) }
    var isResultOpen by remember { mutableStateOf(false) }
    var isSuccess by remember { mutableStateOf(false) }
    var score by remember { mutableIntStateOf(0) }

    LaunchedEffect(riddleIndex) {
        selectedOption = null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        GameHeader(
            title = "Tersine Düşün",
            score = score,
            onBack = onBack,
            statusText = "Soru ${riddleIndex + 1} / ${RIDDLES.size}"
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                InfoTipBox(text = "Yanal Düşünce: İlk aklına gelen sıradan cevaba aldanma, sorunun mantık tuzağını keşfet!")
            }

            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("💡", fontSize = 28.sp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Mantık Paradoksu", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = riddle.question,
                            style = MaterialTheme.typography.bodyLarge,
                            lineHeight = 24.sp
                        )
                    }
                }
            }

            items(riddle.options.indices.toList()) { index ->
                val optionText = riddle.options[index]
                val isSelected = selectedOption == index
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) PrimaryCoral.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) PrimaryCoral else MaterialTheme.colorScheme.outline,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable {
                            soundManager.playClickSound()
                            selectedOption = index
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) PrimaryCoral else MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = ('A' + index).toString(),
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = optionText,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        val correct = selectedOption == riddle.correctIndex
                        isSuccess = correct
                        val added = if (correct) 100 else 10
                        score += added
                        if (correct) {
                            soundManager.playSuccessSound()
                            soundManager.vibrateSuccess()
                        } else {
                            soundManager.playErrorSound()
                            soundManager.vibrateError()
                        }
                        isResultOpen = true
                    },
                    enabled = selectedOption != null,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryCoral),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("submit_riddle_button")
                ) {
                    Text("Cevabımı Gönder", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }

    GameResultDialog(
        isOpen = isResultOpen,
        title = if (isSuccess) "Mükemmel Yanal Düşünce!" else "Ters Köşe Oldun!",
        message = riddle.explanation,
        score = if (isSuccess) 100 else 10,
        isSuccess = isSuccess,
        onPlayAgain = {
            isResultOpen = false
            riddleIndex++
        },
        onBackToHub = onBack
    )
}
