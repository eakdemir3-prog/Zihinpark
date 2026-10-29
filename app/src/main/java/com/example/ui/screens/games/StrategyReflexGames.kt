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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ==========================================
// 5. İMKÂNSIZ GÖREVLER (RIVER CROSSING)
// ==========================================
enum class RiverBank { LEFT, RIGHT }
data class RiverCargo(val name: String, val icon: String, var bank: RiverBank)

@Composable
fun RiverCrossingGameScreen(
    repository: GameRepository,
    soundManager: SoundAndVibeManager,
    onBack: () -> Unit
) {
    var items by remember {
        mutableStateOf(
            listOf(
                RiverCargo("Kurt", "🐺", RiverBank.LEFT),
                RiverCargo("Koyun", "🐑", RiverBank.LEFT),
                RiverCargo("Ot", "🥬", RiverBank.LEFT)
            )
        )
    }
    var boatBank by remember { mutableStateOf(RiverBank.LEFT) }
    var moveCount by remember { mutableIntStateOf(0) }
    var statusMessage by remember { mutableStateOf("Kayık hazır. Karşı kıyıya taşımak için bir eşya seçin.") }
    var isFailed by remember { mutableStateOf(false) }
    var isSuccess by remember { mutableStateOf(false) }
    var isResultOpen by remember { mutableStateOf(false) }
    var score by remember { mutableIntStateOf(0) }

    fun checkRules(newItems: List<RiverCargo>, newBoat: RiverBank) {
        val wolf = newItems.find { it.name == "Kurt" }!!.bank
        val sheep = newItems.find { it.name == "Koyun" }!!.bank
        val cabbage = newItems.find { it.name == "Ot" }!!.bank

        if (wolf == sheep && wolf != newBoat) {
            isFailed = true
            statusMessage = "BAŞARISIZ! Kayık yokken Kurt Koyunu yedi!"
            soundManager.playErrorSound()
            soundManager.vibrateError()
            isResultOpen = true
            return
        }
        if (sheep == cabbage && sheep != newBoat) {
            isFailed = true
            statusMessage = "BAŞARISIZ! Kayık yokken Koyun Otu yedi!"
            soundManager.playErrorSound()
            soundManager.vibrateError()
            isResultOpen = true
            return
        }

        if (newItems.all { it.bank == RiverBank.RIGHT }) {
            isSuccess = true
            statusMessage = "TEBRİKLER! Tüm yolcular güvenle karşı kıyıya geçti!"
            score = maxOf(150 - (moveCount * 5), 50)
            soundManager.playSuccessSound()
            soundManager.vibrateSuccess()
            isResultOpen = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        GameHeader(
            title = "İmkânsız Görevler (Nehir)",
            score = score,
            onBack = onBack,
            statusText = "Hamle Sayısı: $moveCount"
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                InfoTipBox(text = "Kurallar: Kurt ile Koyun, Koyun ile Ot yalnız bırakılamaz. Kayık sadece seninle birlikte en fazla 1 yolcu taşır.")
            }

            // River visual scene
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Left Bank
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(PastelPeach.copy(alpha = 0.5f))
                                    .padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("SOL KIYI", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                                Spacer(modifier = Modifier.height(8.dp))
                                items.filter { it.bank == RiverBank.LEFT }.forEach { item ->
                                    Button(
                                        onClick = {
                                            if (boatBank == RiverBank.LEFT && !isFailed && !isSuccess) {
                                                soundManager.playClickSound()
                                                val updated = items.map { if (it.name == item.name) it.copy(bank = RiverBank.RIGHT) else it }
                                                items = updated
                                                boatBank = RiverBank.RIGHT
                                                moveCount++
                                                checkRules(updated, RiverBank.RIGHT)
                                            }
                                        },
                                        enabled = boatBank == RiverBank.LEFT,
                                        modifier = Modifier.padding(vertical = 4.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface)
                                    ) {
                                        Text("${item.icon} ${item.name}", color = MaterialTheme.colorScheme.onSurface)
                                    }
                                }
                            }

                            // Middle River
                            Column(
                                modifier = Modifier
                                    .width(64.dp)
                                    .padding(horizontal = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("🌊", fontSize = 24.sp)
                                Text(if (boatBank == RiverBank.LEFT) "🛶 ➔" else "🠔 🛶", fontSize = 20.sp)
                                Text("🌊", fontSize = 24.sp)
                            }

                            // Right Bank
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MintGreen.copy(alpha = 0.5f))
                                    .padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("SAĞ KIYI (HEDEF)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                                Spacer(modifier = Modifier.height(8.dp))
                                items.filter { it.bank == RiverBank.RIGHT }.forEach { item ->
                                    Button(
                                        onClick = {
                                            if (boatBank == RiverBank.RIGHT && !isFailed && !isSuccess) {
                                                soundManager.playClickSound()
                                                val updated = items.map { if (it.name == item.name) it.copy(bank = RiverBank.LEFT) else it }
                                                items = updated
                                                boatBank = RiverBank.LEFT
                                                moveCount++
                                                checkRules(updated, RiverBank.LEFT)
                                            }
                                        },
                                        enabled = boatBank == RiverBank.RIGHT,
                                        modifier = Modifier.padding(vertical = 4.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface)
                                    ) {
                                        Text("${item.icon} ${item.name}", color = MaterialTheme.colorScheme.onSurface)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Move boat empty button
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            if (!isFailed && !isSuccess) {
                                soundManager.playClickSound()
                                val newBoat = if (boatBank == RiverBank.LEFT) RiverBank.RIGHT else RiverBank.LEFT
                                boatBank = newBoat
                                moveCount++
                                checkRules(items, newBoat)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryCoral),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Kayığı Boş Geçir", fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    OutlinedButton(
                        onClick = {
                            items = listOf(
                                RiverCargo("Kurt", "🐺", RiverBank.LEFT),
                                RiverCargo("Koyun", "🐑", RiverBank.LEFT),
                                RiverCargo("Ot", "🥬", RiverBank.LEFT)
                            )
                            boatBank = RiverBank.LEFT
                            moveCount = 0
                            isFailed = false
                            isSuccess = false
                            statusMessage = "Görev sıfırlandı."
                        },
                        modifier = Modifier.height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Sıfırla")
                    }
                }
            }
        }
    }

    GameResultDialog(
        isOpen = isResultOpen,
        title = if (isSuccess) "Harika Strateji!" else "Görev Başarısız!",
        message = statusMessage,
        score = if (isSuccess) score else 10,
        isSuccess = isSuccess,
        onPlayAgain = {
            isResultOpen = false
            items = listOf(
                RiverCargo("Kurt", "🐺", RiverBank.LEFT),
                RiverCargo("Koyun", "🐑", RiverBank.LEFT),
                RiverCargo("Ot", "🥬", RiverBank.LEFT)
            )
            boatBank = RiverBank.LEFT
            moveCount = 0
            isFailed = false
            isSuccess = false
        },
        onBackToHub = onBack
    )
}

// ==========================================
// 10. YOLUN SONU (PATH FINDER)
// ==========================================
enum class PathTile { EMPTY, WALL, KEY, DOOR, DESTINATION }

@Composable
fun PathFinderGameScreen(
    repository: GameRepository,
    soundManager: SoundAndVibeManager,
    onBack: () -> Unit
) {
    val mapSize = 6
    var playerPos by remember { mutableStateOf(Pair(0, 0)) }
    var hasKey by remember { mutableStateOf(false) }
    var moveSteps by remember { mutableIntStateOf(0) }
    var isResultOpen by remember { mutableStateOf(false) }
    var score by remember { mutableIntStateOf(0) }

    // Map configuration
    var mapGrid by remember {
        val g = Array(mapSize) { Array(mapSize) { PathTile.EMPTY } }
        g[0][2] = PathTile.WALL
        g[1][2] = PathTile.WALL
        g[2][2] = PathTile.WALL
        g[1][4] = PathTile.WALL
        g[3][1] = PathTile.KEY
        g[3][4] = PathTile.DOOR
        g[4][2] = PathTile.WALL
        g[5][5] = PathTile.DESTINATION
        mutableStateOf(g)
    }

    fun move(dr: Int, dc: Int) {
        val nr = playerPos.first + dr
        val nc = playerPos.second + dc
        if (nr in 0 until mapSize && nc in 0 until mapSize) {
            val tile = mapGrid[nr][nc]
            if (tile == PathTile.WALL) {
                soundManager.playErrorSound()
                soundManager.vibrateShort()
                return
            }
            if (tile == PathTile.DOOR && !hasKey) {
                soundManager.playErrorSound()
                soundManager.vibrateShort()
                return
            }
            if (tile == PathTile.KEY) {
                hasKey = true
                soundManager.playSuccessSound()
                soundManager.vibrateSuccess()
                mapGrid[nr][nc] = PathTile.EMPTY
            }
            if (tile == PathTile.DOOR && hasKey) {
                soundManager.playSuccessSound()
                mapGrid[nr][nc] = PathTile.EMPTY
            }
            if (tile == PathTile.DESTINATION) {
                soundManager.playSuccessSound()
                soundManager.vibrateSuccess()
                score = maxOf(120 - moveSteps * 2, 40)
                isResultOpen = true
            }

            soundManager.playClickSound()
            playerPos = Pair(nr, nc)
            moveSteps++
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        GameHeader(
            title = "Yolun Sonu (Labirent)",
            score = score,
            onBack = onBack,
            statusText = if (hasKey) "Anahtar Alındı 🔑 | Adım: $moveSteps" else "Anahtar Aranıyor 🗝️ | Adım: $moveSteps"
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                InfoTipBox(text = "Önce sarı anahtarı (🔑) al, kilitli kapıyı (🚪) aç ve hedef bayrağa (🏁) ulaş!")
            }

            // Grid render
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
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        for (r in 0 until mapSize) {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                for (c in 0 until mapSize) {
                                    val isPlayer = playerPos.first == r && playerPos.second == c
                                    val tile = mapGrid[r][c]
                                    Box(
                                        modifier = Modifier
                                            .size(46.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                when {
                                                    isPlayer -> PrimaryCoral.copy(alpha = 0.3f)
                                                    tile == PathTile.WALL -> Color(0xFF4B5563)
                                                    tile == PathTile.DESTINATION -> MintGreen
                                                    tile == PathTile.DOOR -> PastelPeach
                                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                                }
                                            )
                                            .then(
                                                if (isPlayer) Modifier.border(2.dp, PrimaryCoral, RoundedCornerShape(8.dp))
                                                else Modifier
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = when {
                                                isPlayer -> "🧭"
                                                tile == PathTile.WALL -> "🧱"
                                                tile == PathTile.KEY -> "🔑"
                                                tile == PathTile.DOOR -> "🚪"
                                                tile == PathTile.DESTINATION -> "🏁"
                                                else -> ""
                                            },
                                            fontSize = 22.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // D-Pad Controls
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    IconButton(
                        onClick = { move(-1, 0) },
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
                            onClick = { move(0, -1) },
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Sol")
                        }
                        IconButton(
                            onClick = { move(0, 1) },
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
                        onClick = { move(1, 0) },
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
        title = "Hedefe Ulaşıldı!",
        message = "$moveSteps adımda kilitli kapıyı açıp labirenti başarıyla tamamladın.",
        score = score,
        isSuccess = true,
        onPlayAgain = {
            isResultOpen = false
            playerPos = Pair(0, 0)
            hasKey = false
            moveSteps = 0
        },
        onBackToHub = onBack
    )
}

// ==========================================
// 13. HAFIZA IŞIKLARI (MEMORY LIGHTS)
// ==========================================
@Composable
fun MemoryLightsGameScreen(
    repository: GameRepository,
    soundManager: SoundAndVibeManager,
    onBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var sequence by remember { mutableStateOf(listOf(0, 2)) }
    var userIndex by remember { mutableIntStateOf(0) }
    var isUserTurn by remember { mutableStateOf(false) }
    var activePadIndex by remember { mutableStateOf<Int?>(null) }
    var roundStreak by remember { mutableIntStateOf(1) }
    var score by remember { mutableIntStateOf(0) }
    var isResultOpen by remember { mutableStateOf(false) }
    var isSuccess by remember { mutableStateOf(false) }

    val padColors = listOf(
        Pair("Kırmızı", Color(0xFFEF4444)),
        Pair("Mavi", Color(0xFF3B82F6)),
        Pair("Yeşil", Color(0xFF10B981)),
        Pair("Sarı", Color(0xFFF59E0B))
    )

    fun playSequence() {
        coroutineScope.launch {
            isUserTurn = false
            delay(500)
            for (pad in sequence) {
                activePadIndex = pad
                soundManager.playTone(30 + pad, 120)
                delay(600)
                activePadIndex = null
                delay(200)
            }
            userIndex = 0
            isUserTurn = true
        }
    }

    LaunchedEffect(Unit) {
        playSequence()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        GameHeader(
            title = "Hafıza Işıkları",
            score = score,
            onBack = onBack,
            statusText = if (isUserTurn) "Sıra Sende: Işıkları tekrarla" else "İzle: Sırayı aklında tut..."
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                InfoTipBox(text = "Simon Says: Yanıp sönen ışık ve ses sırasını aklında tut, ardından butonlara aynı sırayla dokun.")
            }

            item {
                Text(
                    text = "Aşama $roundStreak (${sequence.size} Işık)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            // 2x2 Pad Layout
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            MemoryPad(
                                color = padColors[0].second,
                                isActive = activePadIndex == 0,
                                isEnabled = isUserTurn,
                                onClick = {
                                    handlePadClick(0, sequence, userIndex, soundManager, {
                                        score += 25
                                        roundStreak++
                                        sequence = sequence + (0..3).random()
                                        playSequence()
                                    }, {
                                        isSuccess = false
                                        isResultOpen = true
                                    }, { userIndex++ })
                                }
                            )
                            MemoryPad(
                                color = padColors[1].second,
                                isActive = activePadIndex == 1,
                                isEnabled = isUserTurn,
                                onClick = {
                                    handlePadClick(1, sequence, userIndex, soundManager, {
                                        score += 25
                                        roundStreak++
                                        sequence = sequence + (0..3).random()
                                        playSequence()
                                    }, {
                                        isSuccess = false
                                        isResultOpen = true
                                    }, { userIndex++ })
                                }
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            MemoryPad(
                                color = padColors[2].second,
                                isActive = activePadIndex == 2,
                                isEnabled = isUserTurn,
                                onClick = {
                                    handlePadClick(2, sequence, userIndex, soundManager, {
                                        score += 25
                                        roundStreak++
                                        sequence = sequence + (0..3).random()
                                        playSequence()
                                    }, {
                                        isSuccess = false
                                        isResultOpen = true
                                    }, { userIndex++ })
                                }
                            )
                            MemoryPad(
                                color = padColors[3].second,
                                isActive = activePadIndex == 3,
                                isEnabled = isUserTurn,
                                onClick = {
                                    handlePadClick(3, sequence, userIndex, soundManager, {
                                        score += 25
                                        roundStreak++
                                        sequence = sequence + (0..3).random()
                                        playSequence()
                                    }, {
                                        isSuccess = false
                                        isResultOpen = true
                                    }, { userIndex++ })
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    GameResultDialog(
        isOpen = isResultOpen,
        title = "Oyun Bitti!",
        message = "Toplam $roundStreak aşama hafıza sırasını başarıyla tamamladın.",
        score = score,
        isSuccess = isSuccess,
        onPlayAgain = {
            isResultOpen = false
            sequence = listOf(0, 2)
            roundStreak = 1
            score = 0
            playSequence()
        },
        onBackToHub = onBack
    )
}

private fun handlePadClick(
    pad: Int,
    sequence: List<Int>,
    userIndex: Int,
    soundManager: SoundAndVibeManager,
    onRoundSuccess: () -> Unit,
    onGameOver: () -> Unit,
    onNextStep: () -> Unit
) {
    soundManager.playTone(30 + pad, 80)
    soundManager.vibrateShort()

    if (sequence[userIndex] == pad) {
        if (userIndex + 1 == sequence.size) {
            soundManager.playSuccessSound()
            onRoundSuccess()
        } else {
            onNextStep()
        }
    } else {
        soundManager.playErrorSound()
        soundManager.vibrateError()
        onGameOver()
    }
}

@Composable
fun MemoryPad(
    color: Color,
    isActive: Boolean,
    isEnabled: Boolean,
    onClick: () -> Unit
) {
    val animatedAlpha by animateColorAsState(
        targetValue = if (isActive) color else color.copy(alpha = 0.35f),
        label = "padColor"
    )

    Box(
        modifier = Modifier
            .size(110.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(animatedAlpha)
            .border(
                width = if (isActive) 3.dp else 1.dp,
                color = if (isActive) Color.White else Color.Transparent,
                shape = RoundedCornerShape(18.dp)
            )
            .clickable(enabled = isEnabled, onClick = onClick)
    )
}

// ==========================================
// 14. REFLEKS ARENASI (REFLEX ARENA)
// ==========================================
sealed class ReflexState {
    object Waiting : ReflexState()
    object Ready : ReflexState() // Red: Wait...
    object Triggered : ReflexState() // Green: TAP NOW!
    object EarlyTap : ReflexState()
    data class Success(val ms: Long) : ReflexState()
}

@Composable
fun ReflexArenaGameScreen(
    repository: GameRepository,
    soundManager: SoundAndVibeManager,
    onBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var state by remember { mutableStateOf<ReflexState>(ReflexState.Waiting) }
    var startTimeMs by remember { mutableLongStateOf(0L) }
    var score by remember { mutableIntStateOf(0) }
    var bestTimeMs by remember { mutableStateOf<Long?>(null) }

    fun startReflexRound() {
        state = ReflexState.Ready
        val delayTime = (2000..4500).random().toLong()
        coroutineScope.launch {
            delay(delayTime)
            if (state == ReflexState.Ready) {
                startTimeMs = System.currentTimeMillis()
                state = ReflexState.Triggered
                soundManager.playTone(40, 100)
                soundManager.vibrateShort()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        GameHeader(
            title = "Refleks Arenası",
            score = score,
            onBack = onBack,
            statusText = if (bestTimeMs != null) "En İyi: ${bestTimeMs}ms" else "Milisaniye Hız Testi"
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            InfoTipBox(text = "Butona basarak başla. Ekran KIRMIZI olduğunda bekle, YEŞİL olduğunda en hızlı şekilde dokun!")

            // Reaction tap area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        when (state) {
                            is ReflexState.Waiting -> MaterialTheme.colorScheme.surfaceVariant
                            is ReflexState.Ready -> Color(0xFFEF4444) // Red: Wait
                            is ReflexState.Triggered -> Color(0xFF10B981) // Green: TAP!
                            is ReflexState.EarlyTap -> Color(0xFFF59E0B) // Amber
                            is ReflexState.Success -> MintGreen
                        }
                    )
                    .clickable {
                        when (state) {
                            is ReflexState.Ready -> {
                                soundManager.playErrorSound()
                                soundManager.vibrateError()
                                state = ReflexState.EarlyTap
                            }
                            is ReflexState.Triggered -> {
                                val reaction = System.currentTimeMillis() - startTimeMs
                                soundManager.playSuccessSound()
                                soundManager.vibrateSuccess()
                                state = ReflexState.Success(reaction)
                                bestTimeMs = minOf(bestTimeMs ?: 9999L, reaction)
                                val earned = when {
                                    reaction < 220 -> 100
                                    reaction < 300 -> 75
                                    reaction < 400 -> 50
                                    else -> 25
                                }
                                score += earned
                            }
                            else -> {}
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    when (state) {
                        is ReflexState.Waiting -> {
                            Text("⚡", fontSize = 48.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("BAŞLAMAK İÇİN AŞAĞIDAKİ BUTONA BASIN", fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        }
                        is ReflexState.Ready -> {
                            Text("🛑", fontSize = 48.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("BEKLE...", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                            Text("Yeşil olana kadar dokunma!", color = Color.White.copy(alpha = 0.8f))
                        }
                        is ReflexState.Triggered -> {
                            Text("🎯", fontSize = 54.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("ŞİMDİ DOKUN!", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                        }
                        is ReflexState.EarlyTap -> {
                            Text("⚠️", fontSize = 48.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("ERKEN DOKUNDUN!", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text("Yeşil rengi görmeden dokunmamalısın.", color = Color.White)
                        }
                        is ReflexState.Success -> {
                            val time = (state as ReflexState.Success).ms
                            Text("🏆", fontSize = 48.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("${time} ms", fontSize = 36.sp, fontWeight = FontWeight.ExtraBold, color = MintGreenDark)
                            Text(
                                text = when {
                                    time < 220 -> "İnanılmaz! Efsanevi Refleks ⚡"
                                    time < 300 -> "Harika! Çok Hızlısın 🚀"
                                    time < 400 -> "İyi Refleks 👍"
                                    else -> "Biraz Daha Hızlı Olabilirsin 😊"
                                },
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Action Button
            Button(
                onClick = {
                    soundManager.playClickSound()
                    startReflexRound()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryCoral),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = if (state is ReflexState.Waiting) "Testi Başlat" else "Yeni Tur Başlat",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 16.sp
                )
            }
        }
    }
}
