package com.example.ui.screens.games

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import kotlinx.coroutines.delay

// ==========================================
// 17. PARÇALARI BİRLEŞTİR (JIGSAW PUZZLE)
// ==========================================
data class PuzzlePiece(val id: Int, val symbol: String, val correctSlot: Int)

@Composable
fun JigsawPuzzleGameScreen(
    repository: GameRepository,
    soundManager: SoundAndVibeManager,
    onBack: () -> Unit
) {
    var pieces by remember {
        mutableStateOf(
            listOf(
                PuzzlePiece(1, "🌲", 0),
                PuzzlePiece(2, "🏡", 1),
                PuzzlePiece(3, "🌻", 2),
                PuzzlePiece(4, "☀️", 3)
            ).shuffled()
        )
    }
    var placedSlots by remember { mutableStateOf(mapOf<Int, PuzzlePiece?>()) }
    var selectedPiece by remember { mutableStateOf<PuzzlePiece?>(null) }
    var movesCount by remember { mutableIntStateOf(0) }
    var score by remember { mutableIntStateOf(0) }
    var isResultOpen by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        GameHeader(
            title = "Parçaları Birleştir",
            score = score,
            onBack = onBack,
            statusText = "Hamle: $movesCount | 4 Parçalı Manzara"
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                InfoTipBox(text = "Aşağıdan bir parça seç, ardından 2x2 bulmaca ızgarasındaki doğru yuvaya dokunarak yerleştir.")
            }

            // Target Puzzle Grid (2x2)
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        for (r in 0..1) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                for (c in 0..1) {
                                    val slotIndex = r * 2 + c
                                    val placed = placedSlots[slotIndex]
                                    Box(
                                        modifier = Modifier
                                            .size(90.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                if (placed != null) MintGreen else PastelPeach.copy(alpha = 0.5f)
                                            )
                                            .border(
                                                width = 2.dp,
                                                color = if (placed != null) MintGreenDark else PrimaryCoral,
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            .clickable {
                                                if (selectedPiece != null) {
                                                    soundManager.playClickSound()
                                                    placedSlots = placedSlots + (slotIndex to selectedPiece)
                                                    pieces = pieces.filter { it.id != selectedPiece!!.id }
                                                    selectedPiece = null
                                                    movesCount++

                                                    // Check if full
                                                    if (placedSlots.size == 4) {
                                                        val allCorrect = placedSlots.all { (slot, piece) -> piece?.correctSlot == slot }
                                                        score = if (allCorrect) 120 else 40
                                                        if (allCorrect) soundManager.playSuccessSound() else soundManager.playErrorSound()
                                                        isResultOpen = true
                                                    }
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = placed?.symbol ?: "${slotIndex + 1}. Yuva",
                                            fontSize = if (placed != null) 36.sp else 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Available Pieces Tray
            item {
                Text("Kullanılabilir Parçalar:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    pieces.forEach { piece ->
                        val isSelected = selectedPiece?.id == piece.id
                        Box(
                            modifier = Modifier
                                .padding(8.dp)
                                .size(64.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) PrimaryCoral.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant)
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) PrimaryCoral else Color.Transparent,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    soundManager.playClickSound()
                                    selectedPiece = piece
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(piece.symbol, fontSize = 32.sp)
                        }
                    }
                }
            }
        }
    }

    GameResultDialog(
        isOpen = isResultOpen,
        title = if (score >= 100) "Tebrikler! Manzara Tamamlandı" else "Bazı Parçalar Hatalı",
        message = if (score >= 100) "$movesCount hamlede parçaları eksiksiz birleştirdin! +4 Elmas kazandın 💎" else "Parçaları doğru yuvalarla eşleştirmeyi tekrar dene.",
        score = score,
        isSuccess = score >= 100,
        onPlayAgain = {
            isResultOpen = false
            pieces = listOf(
                PuzzlePiece(1, "🌲", 0),
                PuzzlePiece(2, "🏡", 1),
                PuzzlePiece(3, "🌻", 2),
                PuzzlePiece(4, "☀️", 3)
            ).shuffled()
            placedSlots = emptyMap()
            selectedPiece = null
            movesCount = 0
        },
        onBackToHub = onBack
    )
}

// ==========================================
// 18. GİZLİ NESNEYİ BUL (HIDDEN OBJECT)
// ==========================================
data class HiddenTarget(val name: String, val emoji: String, val row: Int, val col: Int, var isFound: Boolean = false)

@Composable
fun HiddenObjectGameScreen(
    repository: GameRepository,
    soundManager: SoundAndVibeManager,
    onBack: () -> Unit
) {
    val sceneGrid = listOf(
        listOf("📚", "🪑", "🕰️", "🪴", "🗝️"),
        listOf("🖼️", "🧸", "🔭", "📦", "☕"),
        listOf("🕯️", "🧭", "🎒", "👒", "👓"),
        listOf("📻", "🗺️", "🪞", "📜", "🎻")
    )

    var targets by remember {
        mutableStateOf(
            listOf(
                HiddenTarget("Antik Anahtar", "🗝️", 0, 4),
                HiddenTarget("Denizci Pusulası", "🧭", 2, 1),
                HiddenTarget("Gezgin Haritası", "🗺️", 3, 1)
            )
        )
    }

    var score by remember { mutableIntStateOf(0) }
    var isResultOpen by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        GameHeader(
            title = "Gizli Nesneyi Bul",
            score = score,
            onBack = onBack,
            statusText = "Hedefler: ${targets.count { it.isFound }} / ${targets.size}"
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                InfoTipBox(text = "Oda içindeki gizli eşyaları bul! Aşağıdaki hedeflere göre sahnedeki doğru nesnelere dokun.")
            }

            // Target badges
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    targets.forEach { target ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (target.isFound) MintGreen else MaterialTheme.colorScheme.surfaceVariant)
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(target.emoji, fontSize = 20.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = target.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (target.isFound) MintGreenDark else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // Scene Matrix
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
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        sceneGrid.forEachIndexed { r, row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                row.forEachIndexed { c, emoji ->
                                    val matchedTarget = targets.find { it.row == r && it.col == c }
                                    val isFound = matchedTarget?.isFound == true

                                    Box(
                                        modifier = Modifier
                                            .size(52.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isFound) MintGreen else PastelLavender.copy(alpha = 0.35f))
                                            .clickable {
                                                if (matchedTarget != null && !matchedTarget.isFound) {
                                                    soundManager.playSuccessSound()
                                                    soundManager.vibrateSuccess()
                                                    targets = targets.map { if (it.name == matchedTarget.name) it.copy(isFound = true) else it }
                                                    score += 40
                                                    if (targets.all { it.isFound }) {
                                                        isResultOpen = true
                                                    }
                                                } else {
                                                    soundManager.playClickSound()
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(emoji, fontSize = 26.sp)
                                    }
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
        title = "Tüm Gizli Nesneler Bulundu!",
        message = "Gözlem ve dikkat yeteneğin sayesinde gizli eşyaları eksiksiz tespit ettin.",
        score = score,
        isSuccess = true,
        onPlayAgain = {
            isResultOpen = false
            targets = targets.map { it.copy(isFound = false) }
            score = 0
        },
        onBackToHub = onBack
    )
}

// ==========================================
// 19. SAYI PATLATMA (NUMBER MERGE)
// ==========================================
@Composable
fun NumberMergeGameScreen(
    repository: GameRepository,
    soundManager: SoundAndVibeManager,
    onBack: () -> Unit
) {
    // 3x3 Grid of numbers (2, 4, 8, 16...)
    var board by remember {
        mutableStateOf(
            listOf(
                mutableListOf(2, 2, 4),
                mutableListOf(4, 8, 8),
                mutableListOf(2, 4, 2)
            )
        )
    }
    var score by remember { mutableIntStateOf(0) }
    var highestTile by remember { mutableIntStateOf(16) }
    var isResultOpen by remember { mutableStateOf(false) }

    fun mergeAdjacent(r: Int, c: Int) {
        val currentVal = board[r][c]
        val neighbors = listOf(Pair(r - 1, c), Pair(r + 1, c), Pair(r, c - 1), Pair(r, c + 1))
        val match = neighbors.find { (nr, nc) ->
            nr in 0..2 && nc in 0..2 && board[nr][nc] == currentVal
        }

        if (match != null) {
            soundManager.playSuccessSound()
            soundManager.vibrateShort()
            val newBoard = board.map { it.toMutableList() }
            val newVal = currentVal * 2
            newBoard[r][c] = newVal
            newBoard[match.first][match.second] = listOf(2, 4).random()
            board = newBoard
            score += newVal
            highestTile = maxOf(highestTile, newVal)

            if (highestTile >= 64) {
                isResultOpen = true
            }
        } else {
            soundManager.playErrorSound()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        GameHeader(
            title = "Sayı Patlatma",
            score = score,
            onBack = onBack,
            statusText = "Hedef: 64 Taşını Oluştur!"
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                InfoTipBox(text = "Komşusunda aynı sayı olan bir taşa dokun. Taşlar birleşerek iki katına çıkar (2+2->4, 4+4->8). Hedef 64!")
            }

            // Board Render
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        board.forEachIndexed { r, row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                row.forEachIndexed { c, num ->
                                    val tileColor = when (num) {
                                        2 -> PastelPeach
                                        4 -> PastelYellow
                                        8 -> MintGreen
                                        16 -> PastelBlue
                                        32 -> PrimaryCoral.copy(alpha = 0.5f)
                                        else -> PrimaryCoral
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(72.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(tileColor)
                                            .clickable { mergeAdjacent(r, c) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = num.toString(),
                                            fontSize = 24.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (num >= 32) Color.White else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
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
        title = "Tebrikler! 64 Taşına Ulaştın!",
        message = "Sayıları stratejik birleştirerek hedefe başarıyla ulaştın.",
        score = score,
        isSuccess = true,
        onPlayAgain = {
            isResultOpen = false
            board = listOf(
                mutableListOf(2, 2, 4),
                mutableListOf(4, 8, 8),
                mutableListOf(2, 4, 2)
            )
            score = 0
            highestTile = 16
        },
        onBackToHub = onBack
    )
}

// ==========================================
// 20. DOĞRU KAPIYI SEÇ (LOGIC DOORS)
// ==========================================
data class LogicDoor(val id: Int, val label: String, val signText: String, val isSafe: Boolean, val reason: String)

@Composable
fun LogicDoorsGameScreen(
    repository: GameRepository,
    soundManager: SoundAndVibeManager,
    onBack: () -> Unit
) {
    val doors = remember {
        listOf(
            LogicDoor(1, "1. Kapı", "Bu kapının arkasında alevler var ve 2. kapı yalan söylüyor.", false, "Alev tuzağına düştün!"),
            LogicDoor(2, "2. Kapı", "Bu kapı güvenli geçiştir ve altın hazineye açılır.", true, "Tebrikler! Mantıksal ipucunu doğru çözdün ve güvenli kapıdan geçtin."),
            LogicDoor(3, "3. Kapı", "Bu kapı 1. kapının aynısıdır.", false, "Çıkmaz tuzağa yakalandın!")
        )
    }

    var selectedDoorId by remember { mutableStateOf<Int?>(null) }
    var score by remember { mutableIntStateOf(0) }
    var isResultOpen by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        GameHeader(
            title = "Doğru Kapıyı Seç",
            score = score,
            onBack = onBack,
            statusText = "Mantık Labirenti Kapıları"
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                InfoTipBox(text = "Üç kapının üzerindeki kitabeleri dikkatle oku. Yalnızca biri güvenli çıkışa açılmaktadır.")
            }

            items(doors) { door ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            selectedDoorId = door.id
                            if (door.isSafe) {
                                soundManager.playSuccessSound()
                                soundManager.vibrateSuccess()
                                score += 100
                            } else {
                                soundManager.playErrorSound()
                                soundManager.vibrateError()
                                score = 20
                            }
                            isResultOpen = true
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🚪", fontSize = 36.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(door.label, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(door.signText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }

    val selected = doors.find { it.id == selectedDoorId }
    GameResultDialog(
        isOpen = isResultOpen,
        title = if (selected?.isSafe == true) "Doğru Kapı!" else "Tuzak Kapı!",
        message = selected?.reason ?: "",
        score = score,
        isSuccess = selected?.isSafe == true,
        onPlayAgain = {
            isResultOpen = false
            selectedDoorId = null
        },
        onBackToHub = onBack
    )
}

// ==========================================
// 21. HEDEFE DOKUN (TARGET TAP)
// ==========================================
@Composable
fun TargetTapGameScreen(
    repository: GameRepository,
    soundManager: SoundAndVibeManager,
    onBack: () -> Unit
) {
    val targetItems = listOf("🔴 Kırmızı", "🔵 Mavi", "🟢 Yeşil", "🟡 Sarı")
    var currentInstruction by remember { mutableStateOf(targetItems.random()) }
    var score by remember { mutableIntStateOf(0) }
    var hits by remember { mutableIntStateOf(0) }
    var isResultOpen by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        GameHeader(
            title = "Hedefe Dokun",
            score = score,
            onBack = onBack,
            statusText = "İsabet: $hits / 8"
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                InfoTipBox(text = "HEDEF TALİMATINI TAKİP ET: Belirtilen renk ve sembole olabildiğince hızlı dokun!")
            }

            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("DOKUNULACAK HEDEF:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(currentInstruction, fontSize = 32.sp, fontWeight = FontWeight.Bold, color = PrimaryCoral)
                    }
                }
            }

            // 4 Tap Targets
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    targetItems.take(2).forEach { item ->
                        Button(
                            onClick = {
                                if (item == currentInstruction) {
                                    soundManager.playSuccessSound()
                                    score += 20
                                    hits++
                                    if (hits >= 8) isResultOpen = true else currentInstruction = targetItems.random()
                                } else {
                                    soundManager.playErrorSound()
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(64.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Text(item, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    targetItems.takeLast(2).forEach { item ->
                        Button(
                            onClick = {
                                if (item == currentInstruction) {
                                    soundManager.playSuccessSound()
                                    score += 20
                                    hits++
                                    if (hits >= 8) isResultOpen = true else currentInstruction = targetItems.random()
                                } else {
                                    soundManager.playErrorSound()
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(64.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Text(item, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    GameResultDialog(
        isOpen = isResultOpen,
        title = "Tebrikler! Refleks Testi Başarılı",
        message = "8 hedef sembolü tam isabetle ve yüksek hızla yakaladın.",
        score = score,
        isSuccess = true,
        onPlayAgain = {
            isResultOpen = false
            hits = 0
            score = 0
            currentInstruction = targetItems.random()
        },
        onBackToHub = onBack
    )
}

// ==========================================
// 22. LABİRENTTEN KAÇIŞ (MAZE ESCAPE)
// ==========================================
@Composable
fun MazeEscapeGameScreen(
    repository: GameRepository,
    soundManager: SoundAndVibeManager,
    onBack: () -> Unit
) {
    val size = 5
    var playerPos by remember { mutableStateOf(Pair(0, 0)) }
    var moves by remember { mutableIntStateOf(0) }
    var score by remember { mutableIntStateOf(0) }
    var isResultOpen by remember { mutableStateOf(false) }

    // Maze walls
    val walls = setOf(Pair(0, 2), Pair(1, 2), Pair(3, 1), Pair(3, 2), Pair(3, 3))

    fun tryMove(dr: Int, dc: Int) {
        val nr = playerPos.first + dr
        val nc = playerPos.second + dc
        if (nr in 0 until size && nc in 0 until size && !walls.contains(Pair(nr, nc))) {
            soundManager.playClickSound()
            playerPos = Pair(nr, nc)
            moves++
            if (nr == 4 && nc == 4) {
                soundManager.playSuccessSound()
                soundManager.vibrateSuccess()
                score = maxOf(150 - moves * 3, 50)
                isResultOpen = true
            }
        } else {
            soundManager.playErrorSound()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        GameHeader(
            title = "Labirentten Kaçış",
            score = score,
            onBack = onBack,
            statusText = "Hamle: $moves | Çıkış: (5, 5)"
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                InfoTipBox(text = "Duvarlara çarpmadan koşucuyu (🏃‍♂️) çıkış kapısına (🏁) ulaştır.")
            }

            // Maze grid
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.padding(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        for (r in 0 until size) {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                for (c in 0 until size) {
                                    val isPlayer = playerPos.first == r && playerPos.second == c
                                    val isExit = r == 4 && c == 4
                                    val isWall = walls.contains(Pair(r, c))

                                    Box(
                                        modifier = Modifier
                                            .size(52.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                when {
                                                    isPlayer -> PrimaryCoral.copy(alpha = 0.35f)
                                                    isWall -> Color(0xFF374151)
                                                    isExit -> MintGreen
                                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                                }
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = when {
                                                isPlayer -> "🏃‍♂️"
                                                isExit -> "🏁"
                                                isWall -> "🧱"
                                                else -> ""
                                            },
                                            fontSize = 24.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Controls
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    IconButton(
                        onClick = { tryMove(-1, 0) },
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Yukarı")
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(36.dp)) {
                        IconButton(
                            onClick = { tryMove(0, -1) },
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Sol")
                        }
                        IconButton(
                            onClick = { tryMove(0, 1) },
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Sağ")
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    IconButton(
                        onClick = { tryMove(1, 0) },
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Aşağı")
                    }
                }
            }
        }
    }

    GameResultDialog(
        isOpen = isResultOpen,
        title = "Labirentten Kaçış Başarılı!",
        message = "$moves hamlede çıkışa ulaştın! +6 Elmas kazandın 💎",
        score = score,
        isSuccess = true,
        onPlayAgain = {
            isResultOpen = false
            playerPos = Pair(0, 0)
            moves = 0
            score = 0
        },
        onBackToHub = onBack
    )
}
