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
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.example.ui.theme.PastelBlue
import com.example.ui.theme.PastelLavender
import com.example.ui.theme.PastelPeach
import com.example.ui.theme.PastelYellow
import com.example.ui.theme.PrimaryCoral
import com.example.util.SoundAndVibeManager

// ==========================================
// 9. DETAY AVCISI (DETAIL HUNTER)
// ==========================================
data class DifferenceZone(val id: Int, val name: String, val row: Int, val col: Int)

@Composable
fun DetailHunterGameScreen(
    repository: GameRepository,
    soundManager: SoundAndVibeManager,
    onBack: () -> Unit
) {
    // 4x4 pictorial matrix with 3 differences between Original and Modified Scene
    val differences = remember {
        listOf(
            DifferenceZone(1, "Eksik Bulut", 0, 3),
            DifferenceZone(2, "Farklı Çiçek", 2, 1),
            DifferenceZone(3, "Gizli Yıldız", 3, 2)
        )
    }

    var foundIds by remember { mutableStateOf(setOf<Int>()) }
    var score by remember { mutableIntStateOf(0) }
    var isResultOpen by remember { mutableStateOf(false) }

    val sceneA = listOf(
        listOf("☀️", "🏔️", "🌲", "☁️"),
        listOf("🏡", "🌳", "🐕", "🌾"),
        listOf("🌻", "🌺", "🪴", "🦋"),
        listOf("🌊", "⛵", "🐟", "🏝️")
    )

    val sceneB = listOf(
        listOf("☀️", "🏔️", "🌲", "⬜"), // Diff 1: Missing cloud
        listOf("🏡", "🌳", "🐕", "🌾"),
        listOf("🌻", "🌼", "🪴", "🦋"), // Diff 2: Daisy instead of Hibiscus
        listOf("🌊", "⛵", "⭐", "🏝️")  // Diff 3: Star instead of fish
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        GameHeader(
            title = "Detay Avcısı",
            score = score,
            onBack = onBack,
            statusText = "Bulunan: ${foundIds.size} / ${differences.size}"
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                InfoTipBox(text = "Üstteki Orijinal Sahne ile alttaki Sahne arasındaki 3 farkı bul ve alttaki sahnede farklı olan kareye dokun.")
            }

            // Scene A
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("1. ORİJİNAL SAHNE", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(8.dp))
                        SceneGrid(scene = sceneA, highlightedZones = emptySet(), onCellClick = null)
                    }
                }
            }

            // Scene B (Interactive)
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("2. FARKLILIKLARI BUL (DOKUN)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = PrimaryCoral)
                        Spacer(modifier = Modifier.height(8.dp))
                        SceneGrid(
                            scene = sceneB,
                            highlightedZones = foundIds,
                            onCellClick = { r, c ->
                                val hit = differences.find { it.row == r && it.col == c }
                                if (hit != null && !foundIds.contains(hit.id)) {
                                    soundManager.playSuccessSound()
                                    soundManager.vibrateSuccess()
                                    foundIds = foundIds + hit.id
                                    score += 40
                                    if (foundIds.size == differences.size) {
                                        isResultOpen = true
                                    }
                                } else {
                                    soundManager.playErrorSound()
                                    soundManager.vibrateShort()
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    GameResultDialog(
        isOpen = isResultOpen,
        title = "Tebrikler! Tüm Farkları Buldun",
        message = "Gözlem ve dikkat becerin mükemmel! 3 gizli detayı başarıyla tespit ettin.",
        score = score,
        isSuccess = true,
        onPlayAgain = {
            isResultOpen = false
            foundIds = emptySet()
            score = 0
        },
        onBackToHub = onBack
    )
}

@Composable
fun SceneGrid(
    scene: List<List<String>>,
    highlightedZones: Set<Int>,
    onCellClick: ((Int, Int) -> Unit)?
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        scene.forEachIndexed { r, row ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                row.forEachIndexed { c, emoji ->
                    val isFound = (r == 0 && c == 3 && highlightedZones.contains(1)) ||
                                  (r == 2 && c == 1 && highlightedZones.contains(2)) ||
                                  (r == 3 && c == 2 && highlightedZones.contains(3))
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isFound) MintGreen else PastelLavender.copy(alpha = 0.35f))
                            .then(
                                if (isFound) Modifier.border(2.dp, MintGreenDark, RoundedCornerShape(8.dp))
                                else Modifier
                            )
                            .then(
                                if (onCellClick != null) Modifier.clickable { onCellClick(r, c) }
                                else Modifier
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(emoji, fontSize = 24.sp)
                    }
                }
            }
        }
    }
}

// ==========================================
// 12. KELİME AVI (WORD HUNT)
// ==========================================
data class TargetWord(val word: String, var isFound: Boolean = false)

@Composable
fun WordHuntGameScreen(
    repository: GameRepository,
    soundManager: SoundAndVibeManager,
    onBack: () -> Unit
) {
    // 6x6 Letter Grid
    val letters = listOf(
        listOf('Z', 'İ', 'H', 'İ', 'N', 'P'),
        listOf('M', 'A', 'N', 'T', 'I', 'K'),
        listOf('H', 'A', 'F', 'I', 'Z', 'A'),
        listOf('D', 'İ', 'K', 'K', 'A', 'T'),
        listOf('O', 'Y', 'U', 'N', 'L', 'A'),
        listOf('B', 'E', 'Y', 'İ', 'N', 'S')
    )

    var targets by remember {
        mutableStateOf(
            listOf(
                TargetWord("ZİHİN"),
                TargetWord("MANTIK"),
                TargetWord("HAFIZA"),
                TargetWord("DİKKAT"),
                TargetWord("BEYİN")
            )
        )
    }

    var selectedWordBuffer by remember { mutableStateOf("") }
    var score by remember { mutableIntStateOf(0) }
    var isResultOpen by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        GameHeader(
            title = "Kelime Avı",
            score = score,
            onBack = onBack,
            statusText = "Bulunan: ${targets.count { it.isFound }} / ${targets.size}"
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                InfoTipBox(text = "Harflere sırayla dokunarak hedef kelimeleri oluştur, ardından 'Kelimeyi Onayla' butonuna bas.")
            }

            // Word targets
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    targets.forEach { target ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (target.isFound) MintGreen else MaterialTheme.colorScheme.surfaceVariant)
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = target.word,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (target.isFound) MintGreenDark else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Selection buffer
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = PastelLavender.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (selectedWordBuffer.isEmpty()) "Harfleri seçin..." else selectedWordBuffer,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryCoral
                        )
                        if (selectedWordBuffer.isNotEmpty()) {
                            Text(
                                text = "Temizle",
                                color = Color.Red,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable {
                                    soundManager.playClickSound()
                                    selectedWordBuffer = ""
                                }
                            )
                        }
                    }
                }
            }

            // Grid
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        letters.forEach { rowLetters ->
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                rowLetters.forEach { letter ->
                                    Box(
                                        modifier = Modifier
                                            .size(46.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                            .clickable {
                                                soundManager.playClickSound()
                                                selectedWordBuffer += letter
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = letter.toString(),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 20.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Button(
                    onClick = {
                        val matched = targets.find { it.word == selectedWordBuffer && !it.isFound }
                        if (matched != null) {
                            soundManager.playSuccessSound()
                            soundManager.vibrateSuccess()
                            targets = targets.map { if (it.word == matched.word) it.copy(isFound = true) else it }
                            score += 50
                            selectedWordBuffer = ""
                            if (targets.all { it.isFound }) {
                                isResultOpen = true
                            }
                        } else {
                            soundManager.playErrorSound()
                            soundManager.vibrateError()
                            selectedWordBuffer = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryCoral),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text("Kelimeyi Onayla", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }

    GameResultDialog(
        isOpen = isResultOpen,
        title = "Tüm Kelimeler Avlandı!",
        message = "Harf matrisinde gizlenen zihinsel gelişim kelimelerinin tamamını başarıyla buldun.",
        score = score,
        isSuccess = true,
        onPlayAgain = {
            isResultOpen = false
            targets = targets.map { it.copy(isFound = false) }
            selectedWordBuffer = ""
            score = 0
        },
        onBackToHub = onBack
    )
}

// ==========================================
// 15. RENK KAÇIŞI (COLOR STROOP)
// ==========================================
data class StroopTask(val wordText: String, val inkColor: Color, val inkName: String)

private val STROOP_COLORS = listOf(
    Pair("KIRMIZI", Color(0xFFEF4444)),
    Pair("MAVİ", Color(0xFF3B82F6)),
    Pair("YEŞİL", Color(0xFF10B981)),
    Pair("SARI", Color(0xFFF59E0B))
)

private fun generateStroopTask(): StroopTask {
    val word = STROOP_COLORS.random().first
    val ink = STROOP_COLORS.random()
    return StroopTask(word, ink.second, ink.first)
}

@Composable
fun ColorStroopGameScreen(
    repository: GameRepository,
    soundManager: SoundAndVibeManager,
    onBack: () -> Unit
) {
    var roundCount by remember { mutableIntStateOf(1) }
    val maxRounds = 10
    var currentTask by remember { mutableStateOf(generateStroopTask()) }
    var score by remember { mutableIntStateOf(0) }
    var isResultOpen by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        GameHeader(
            title = "Renk Kaçışı (Stroop)",
            score = score,
            onBack = onBack,
            statusText = "Tur $roundCount / $maxRounds"
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                InfoTipBox(text = "DİKKAT: Kelimenin ne yazdığına değil, YAZILDIĞI MÜREKKEP RENGİNE dokun!")
            }

            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = currentTask.wordText,
                            fontSize = 44.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = currentTask.inkColor
                        )
                    }
                }
            }

            item {
                Text(
                    text = "Mürekkep Rengi Nedir?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            // 4 Color choices
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    val rows = listOf(
                        listOf(STROOP_COLORS[0], STROOP_COLORS[1]),
                        listOf(STROOP_COLORS[2], STROOP_COLORS[3])
                    )
                    rows.forEach { colorRow ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            colorRow.forEach { colorItem ->
                                Button(
                                    onClick = {
                                        val correct = colorItem.first == currentTask.inkName
                                        if (correct) {
                                            soundManager.playSuccessSound()
                                            soundManager.vibrateSuccess()
                                            score += 15
                                        } else {
                                            soundManager.playErrorSound()
                                            soundManager.vibrateError()
                                        }

                                        if (roundCount >= maxRounds) {
                                            isResultOpen = true
                                        } else {
                                            roundCount++
                                            currentTask = generateStroopTask()
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(56.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = colorItem.second),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Text(
                                        text = colorItem.first,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    GameResultDialog(
        isOpen = isResultOpen,
        title = "Stroop Testi Tamamlandı!",
        message = "Beyninizin çelişkili algı uyaranlarını filtreleme ve odaklanma hızı ölçüldü.",
        score = score,
        isSuccess = score >= 90,
        onPlayAgain = {
            isResultOpen = false
            roundCount = 1
            score = 0
            currentTask = generateStroopTask()
        },
        onBackToHub = onBack
    )
}
