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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.example.ui.theme.PastelBlue
import com.example.ui.theme.PastelBlueDark
import com.example.ui.theme.PastelLavender
import com.example.ui.theme.PastelPeach
import com.example.ui.theme.PastelYellow
import com.example.ui.theme.PrimaryCoral
import com.example.util.SoundAndVibeManager

// ==========================================
// 2. ZİHİN DÖNDÜRME (SPATIAL ROTATION)
// ==========================================
private fun rotateGrid90(grid: List<List<Boolean>>): List<List<Boolean>> {
    val size = grid.size
    return List(size) { row ->
        List(size) { col ->
            grid[size - 1 - col][row]
        }
    }
}

private fun rotateGrid180(grid: List<List<Boolean>>): List<List<Boolean>> = rotateGrid90(rotateGrid90(grid))
private fun rotateGrid270(grid: List<List<Boolean>>): List<List<Boolean>> = rotateGrid90(rotateGrid180(grid))

data class SpatialQuestion(
    val baseGrid: List<List<Boolean>>,
    val targetDegrees: Int,
    val options: List<List<List<Boolean>>>,
    val correctIndex: Int
)

private fun generateSpatialQuestion(): SpatialQuestion {
    val base = listOf(
        listOf(true, true, false),
        listOf(false, true, false),
        listOf(false, true, true)
    )
    val r90 = rotateGrid90(base)
    val r180 = rotateGrid180(base)
    val r270 = rotateGrid270(base)
    val options = listOf(r90, r180, r270).shuffled()
    val targetDegrees = 90
    val correctIndex = options.indexOfFirst { it == r90 }
    return SpatialQuestion(base, targetDegrees, options, correctIndex)
}

@Composable
fun SpatialRotationGameScreen(
    repository: GameRepository,
    soundManager: SoundAndVibeManager,
    onBack: () -> Unit
) {
    var question by remember { mutableStateOf(generateSpatialQuestion()) }
    var selectedOption by remember { mutableStateOf<Int?>(null) }
    var isResultOpen by remember { mutableStateOf(false) }
    var isSuccess by remember { mutableStateOf(false) }
    var score by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        GameHeader(
            title = "Zihin Döndürme",
            score = score,
            onBack = onBack,
            statusText = "Hedef Açısı: ${question.targetDegrees}° Saat Yönünde"
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                InfoTipBox(text = "Orijinal şekli zihninde ${question.targetDegrees}° saat yönünde döndür. Seçeneklerden doğru hali işaretle.")
            }

            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("ORİJİNAL ŞEKİL (0°)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(12.dp))
                        SpatialGridRenderer(grid = question.baseGrid, cellSize = 36.dp, activeColor = PrimaryCoral)
                    }
                }
            }

            item {
                Text("Hangisi ${question.targetDegrees}° Döndürülmüş Hali?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    question.options.forEachIndexed { index, optionGrid ->
                        val isSelected = selectedOption == index
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) PrimaryCoral.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface
                            ),
                            modifier = Modifier
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) PrimaryCoral else MaterialTheme.colorScheme.outline,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    soundManager.playClickSound()
                                    selectedOption = index
                                }
                                .padding(8.dp)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(4.dp)
                            ) {
                                SpatialGridRenderer(grid = optionGrid, cellSize = 24.dp, activeColor = if (isSelected) PrimaryCoral else PastelBlueDark)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Şekil ${index + 1}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        val correct = selectedOption == question.correctIndex
                        isSuccess = correct
                        val added = if (correct) 100 else 20
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
                        .testTag("submit_spatial_button")
                ) {
                    Text("Cevabı Onayla", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }

    GameResultDialog(
        isOpen = isResultOpen,
        title = if (isSuccess) "Mükemmel Uzamsal Algı!" else "Açı Yanılgısı!",
        message = if (isSuccess) "Zihninde şekli doğru açıyla başarıyla döndürdün." else "Saat yönünde döndürürken satır ve sütunların yer değişimine dikkat et.",
        score = if (isSuccess) 100 else 20,
        isSuccess = isSuccess,
        onPlayAgain = {
            isResultOpen = false
            selectedOption = null
            question = generateSpatialQuestion()
        },
        onBackToHub = onBack
    )
}

@Composable
fun SpatialGridRenderer(
    grid: List<List<Boolean>>,
    cellSize: androidx.compose.ui.unit.Dp,
    activeColor: Color
) {
    Column {
        grid.forEach { row ->
            Row {
                row.forEach { filled ->
                    Box(
                        modifier = Modifier
                            .size(cellSize)
                            .padding(2.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (filled) activeColor else MaterialTheme.colorScheme.surfaceVariant)
                    )
                }
            }
        }
    }
}

// ==========================================
// 3. GİZLİ RESİM (NONOGRAM)
// ==========================================
enum class NonogramState { EMPTY, FILLED, CROSS }

@Composable
fun NonogramGameScreen(
    repository: GameRepository,
    soundManager: SoundAndVibeManager,
    onBack: () -> Unit
) {
    // 5x5 Heart Pattern
    val solution = remember {
        listOf(
            listOf(false, true, false, true, false),
            listOf(true, true, true, true, true),
            listOf(true, true, true, true, true),
            listOf(false, true, true, true, false),
            listOf(false, false, true, false, false)
        )
    }

    var grid by remember {
        mutableStateOf(List(5) { List(5) { NonogramState.EMPTY } })
    }
    var score by remember { mutableIntStateOf(0) }
    var isResultOpen by remember { mutableStateOf(false) }
    var isSuccess by remember { mutableStateOf(false) }

    val rowClues = listOf(listOf(1, 1), listOf(5), listOf(5), listOf(3), listOf(1))
    val colClues = listOf(listOf(2), listOf(4), listOf(4), listOf(4), listOf(2))

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        GameHeader(
            title = "Gizli Resim (Nonogram)",
            score = score,
            onBack = onBack,
            statusText = "5x5 Piksel Bulmaca"
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                InfoTipBox(text = "Hücreye dokun: [BOŞ -> DOLU -> ÇARPI]. Satır ve sütundaki sayılar ardışık dolu blok sayısını gösterir.")
            }

            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Column Clues Header
                        Row(modifier = Modifier.padding(start = 48.dp)) {
                            colClues.forEach { clue ->
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .padding(2.dp),
                                    contentAlignment = Alignment.BottomCenter
                                ) {
                                    Text(
                                        text = clue.joinToString("\n"),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = PastelBlueDark,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }

                        // Grid with Row Clues
                        grid.forEachIndexed { r, rowCells ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Row clue
                                Box(
                                    modifier = Modifier
                                        .width(44.dp)
                                        .padding(end = 4.dp),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    Text(
                                        text = rowClues[r].joinToString(" "),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = PastelBlueDark
                                    )
                                }

                                rowCells.forEachIndexed { c, cellState ->
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .padding(2.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(
                                                when (cellState) {
                                                    NonogramState.FILLED -> PrimaryCoral
                                                    NonogramState.CROSS -> PastelLavender.copy(alpha = 0.5f)
                                                    NonogramState.EMPTY -> MaterialTheme.colorScheme.surfaceVariant
                                                }
                                            )
                                            .clickable {
                                                soundManager.playClickSound()
                                                val newRow = grid[r].toMutableList()
                                                newRow[c] = when (cellState) {
                                                    NonogramState.EMPTY -> NonogramState.FILLED
                                                    NonogramState.FILLED -> NonogramState.CROSS
                                                    NonogramState.CROSS -> NonogramState.EMPTY
                                                }
                                                val newGrid = grid.toMutableList()
                                                newGrid[r] = newRow
                                                grid = newGrid
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (cellState == NonogramState.CROSS) {
                                            Text("✕", color = Color.Gray, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            soundManager.playClickSound()
                            grid = List(5) { List(5) { NonogramState.EMPTY } }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Temizle")
                    }

                    Button(
                        onClick = {
                            var allCorrect = true
                            for (r in 0 until 5) {
                                for (c in 0 until 5) {
                                    val isFilled = grid[r][c] == NonogramState.FILLED
                                    if (isFilled != solution[r][c]) {
                                        allCorrect = false
                                        break
                                    }
                                }
                            }
                            isSuccess = allCorrect
                            val added = if (allCorrect) 150 else 30
                            score += added
                            if (allCorrect) {
                                soundManager.playSuccessSound()
                                soundManager.vibrateSuccess()
                            } else {
                                soundManager.playErrorSound()
                                soundManager.vibrateError()
                            }
                            isResultOpen = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryCoral),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Kontrol Et", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    GameResultDialog(
        isOpen = isResultOpen,
        title = if (isSuccess) "Tebrikler! Gizli Resim: KALP ❤️" else "Eksik veya Hatalı Hücreler Var",
        message = if (isSuccess) "Nonogram mantığını eksiksiz çözdün ve gizli kalp figürünü ortaya çıkardın!" else "Satır ve sütun ipucu sayılarını kontrol ederek tekrar dene.",
        score = if (isSuccess) 150 else 30,
        isSuccess = isSuccess,
        onPlayAgain = {
            isResultOpen = false
            grid = List(5) { List(5) { NonogramState.EMPTY } }
        },
        onBackToHub = onBack
    )
}

// ==========================================
// 6. ŞEKİL MİMARI (SHAPE ARCHITECT)
// ==========================================
data class ArchitectSlot(val id: Int, val targetName: String, var assignedName: String? = null)

@Composable
fun ShapeArchitectGameScreen(
    repository: GameRepository,
    soundManager: SoundAndVibeManager,
    onBack: () -> Unit
) {
    var pieces by remember {
        mutableStateOf(
            listOf(
                Pair("Üçgen (90°)", "🔺"),
                Pair("Kare (Düz)", "🟩"),
                Pair("Paralelkenar", "🔷")
            )
        )
    }
    var slotAssignments by remember {
        mutableStateOf(mapOf<Int, String?>())
    }
    var selectedPiece by remember { mutableStateOf<String?>(null) }
    var score by remember { mutableIntStateOf(0) }
    var isResultOpen by remember { mutableStateOf(false) }
    var isSuccess by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        GameHeader(
            title = "Şekil Mimarı",
            score = score,
            onBack = onBack,
            statusText = "Geometrik Tangram İnşası"
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                InfoTipBox(text = "Aşağıdaki parçalardan birini seç, ardından hedef siluetin uygun yuvasına dokunarak yerleştir.")
            }

            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("HEDEF MİMARİ SİLÜET (EV)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(16.dp))

                        // Roof Slot (Triangle)
                        val roof = slotAssignments[1]
                        Box(
                            modifier = Modifier
                                .size(90.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (roof != null) MintGreen else PastelPeach.copy(alpha = 0.5f))
                                .border(2.dp, PrimaryCoral, RoundedCornerShape(12.dp))
                                .clickable {
                                    if (selectedPiece != null) {
                                        soundManager.playClickSound()
                                        slotAssignments = slotAssignments + (1 to selectedPiece)
                                        selectedPiece = null
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(roof ?: "1. Yuva (Çatı)", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Body Slot (Square)
                        val body = slotAssignments[2]
                        Box(
                            modifier = Modifier
                                .size(90.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (body != null) MintGreen else PastelPeach.copy(alpha = 0.5f))
                                .border(2.dp, PrimaryCoral, RoundedCornerShape(12.dp))
                                .clickable {
                                    if (selectedPiece != null) {
                                        soundManager.playClickSound()
                                        slotAssignments = slotAssignments + (2 to selectedPiece)
                                        selectedPiece = null
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(body ?: "2. Yuva (Gövde)", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }

            item {
                Text("Kullanılabilir Parçalar:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    pieces.forEach { piece ->
                        val isSelected = selectedPiece == piece.first
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) PrimaryCoral.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) PrimaryCoral else MaterialTheme.colorScheme.outline,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    soundManager.playClickSound()
                                    selectedPiece = piece.first
                                }
                                .padding(8.dp)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp)
                            ) {
                                Text(piece.second, fontSize = 28.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = piece.first,
                                    style = MaterialTheme.typography.labelSmall,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            item {
                Button(
                    onClick = {
                        val correct = slotAssignments[1]?.contains("Üçgen") == true &&
                                      slotAssignments[2]?.contains("Kare") == true
                        isSuccess = correct
                        val added = if (correct) 100 else 20
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
                    enabled = slotAssignments.size >= 2,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryCoral),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text("Yapıyı Tamamla", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }

    GameResultDialog(
        isOpen = isResultOpen,
        title = if (isSuccess) "Harika Mimari Başarı!" else "Hatalı Parça Yerleşimi",
        message = if (isSuccess) "Geometrik parçaları doğru açılar ve yuvalarla eşleştirerek evi inşa ettin!" else "Çatı için üçgen, gövde için kare formunu kullanmayı dene.",
        score = if (isSuccess) 100 else 20,
        isSuccess = isSuccess,
        onPlayAgain = {
            isResultOpen = false
            slotAssignments = emptyMap()
            selectedPiece = null
        },
        onBackToHub = onBack
    )
}

// ==========================================
// 16. DESEN USTASI (PATTERN MASTER)
// ==========================================
data class SymbolPattern(
    val sequence: List<String>,
    val options: List<String>,
    val correctIndex: Int,
    val ruleExplanation: String
)

private val PATTERNS = listOf(
    SymbolPattern(
        sequence = listOf("▲", "■", "▲", "■", "?"),
        options = listOf("▲", "■", "●", "★"),
        correctIndex = 0,
        ruleExplanation = "Kural: Üçgen ve Kare ardışık olarak tekrarlanmaktadır."
    ),
    SymbolPattern(
        sequence = listOf("🌕", "🌖", "🌗", "🌘", "?"),
        options = listOf("🌑", "🌕", "🌓", "🌟"),
        correctIndex = 0,
        ruleExplanation = "Kural: Ay evreleri dolunaydan yeni aya doğru küçülmektedir."
    ),
    SymbolPattern(
        sequence = listOf("🔵", "🔴", "🔴", "🔵", "🔴", "?"),
        options = listOf("🔴", "🔵", "🟡", "🟢"),
        correctIndex = 0,
        ruleExplanation = "Kural: Bir mavi, iki kırmızı dizilimi (M-K-K-M-K-K)."
    )
)

@Composable
fun PatternMasterGameScreen(
    repository: GameRepository,
    soundManager: SoundAndVibeManager,
    onBack: () -> Unit
) {
    var patternIndex by remember { mutableIntStateOf(0) }
    val pattern = PATTERNS[patternIndex % PATTERNS.size]
    var selectedOption by remember { mutableStateOf<Int?>(null) }
    var score by remember { mutableIntStateOf(0) }
    var isResultOpen by remember { mutableStateOf(false) }
    var isSuccess by remember { mutableStateOf(false) }

    LaunchedEffect(patternIndex) {
        selectedOption = null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        GameHeader(
            title = "Desen Ustası",
            score = score,
            onBack = onBack,
            statusText = "Dizi ${patternIndex + 1} / ${PATTERNS.size}"
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                InfoTipBox(text = "Sembollerin dönüşüm ve sıralama kuralını analiz et, soru işaretinin yerine gelecek doğru sembolü bul.")
            }

            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("SEMBOL DİZİSİ", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            pattern.sequence.forEach { symbol ->
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (symbol == "?") PrimaryCoral.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = symbol,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (symbol == "?") PrimaryCoral else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Text("Sırada Hangi Sembol Olmalı?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    pattern.options.forEachIndexed { index, option ->
                        val isSelected = selectedOption == index
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) PrimaryCoral.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) PrimaryCoral else MaterialTheme.colorScheme.outline,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    soundManager.playClickSound()
                                    selectedOption = index
                                }
                                .padding(vertical = 12.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Text(option, fontSize = 28.sp)
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        val correct = selectedOption == pattern.correctIndex
                        isSuccess = correct
                        val added = if (correct) 90 else 15
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
                ) {
                    Text("Cevabı Gönder", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }

    GameResultDialog(
        isOpen = isResultOpen,
        title = if (isSuccess) "Tebrikler! Deseni Çözdün" else "Örüntü Yanılgısı",
        message = pattern.ruleExplanation,
        score = if (isSuccess) 90 else 15,
        isSuccess = isSuccess,
        onPlayAgain = {
            isResultOpen = false
            patternIndex++
        },
        onBackToHub = onBack
    )
}
