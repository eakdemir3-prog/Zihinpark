package com.example.ui.screens.games

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
import com.example.ui.theme.PastelLavender
import com.example.ui.theme.PastelPeach
import com.example.ui.theme.PrimaryCoral
import com.example.util.SoundAndVibeManager

// ==========================================
// 8. SAYI MÜHENDİSİ (MATH ENGINEER)
// ==========================================
@Composable
fun MathEngineerGameScreen(
    repository: GameRepository,
    soundManager: SoundAndVibeManager,
    onBack: () -> Unit
) {
    // 3x3 Equation Grid:
    // [4] [+] [5] [=] [9]
    // [*]     [+]     [*]
    // [2] [+] [?] [=] [8]  -> ? is 6  (2 + 6 = 8, 5 + 6 = 11, 9 * 8 = 72, 4 * 2 = 8)
    // [=]     [=]     [=]
    // [8]    [11]    [72]
    var userInputValue by remember { mutableStateOf("") }
    var score by remember { mutableIntStateOf(0) }
    var isResultOpen by remember { mutableStateOf(false) }
    var isSuccess by remember { mutableStateOf(false) }

    val matrixRows = listOf(
        listOf("4", "+", "5", "=", "9"),
        listOf("×", " ", "+", " ", "×"),
        listOf("2", "+", if (userInputValue.isEmpty()) "?" else userInputValue, "=", "8"),
        listOf("=", " ", "=", " ", "="),
        listOf("8", " ", "11", " ", "72")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        GameHeader(
            title = "Sayı Mühendisi",
            score = score,
            onBack = onBack,
            statusText = "Kesişen Denklem Matrisi"
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                InfoTipBox(text = "Hem yatay (2 + ? = 8) hem dikey (5 + ? = 11) denklemleri sağlayan eksik sayıyı tuşlayın.")
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
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        matrixRows.forEachIndexed { rIdx, row ->
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                row.forEachIndexed { cIdx, cellText ->
                                    val isTarget = rIdx == 2 && cIdx == 2
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                when {
                                                    isTarget -> PrimaryCoral.copy(alpha = 0.25f)
                                                    cellText == " " -> Color.Transparent
                                                    cellText in listOf("+", "×", "=") -> PastelLavender.copy(alpha = 0.4f)
                                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                                }
                                            )
                                            .then(
                                                if (isTarget) Modifier.border(2.dp, PrimaryCoral, RoundedCornerShape(8.dp))
                                                else Modifier
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = cellText,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = if (cellText.length > 2) 15.sp else 18.sp,
                                            color = if (isTarget) PrimaryCoral else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Keypad
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val rows = listOf(
                            listOf("1", "2", "3"),
                            listOf("4", "5", "6"),
                            listOf("7", "8", "9"),
                            listOf("C", "0", "OK")
                        )
                        rows.forEach { keyRow ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                keyRow.forEach { key ->
                                    Button(
                                        onClick = {
                                            soundManager.playClickSound()
                                            when (key) {
                                                "C" -> userInputValue = ""
                                                "OK" -> {
                                                    val correct = userInputValue == "6"
                                                    isSuccess = correct
                                                    val added = if (correct) 120 else 20
                                                    score += added
                                                    if (correct) {
                                                        soundManager.playSuccessSound()
                                                        soundManager.vibrateSuccess()
                                                    } else {
                                                        soundManager.playErrorSound()
                                                        soundManager.vibrateError()
                                                    }
                                                    isResultOpen = true
                                                }
                                                else -> if (userInputValue.length < 2) userInputValue += key
                                            }
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (key == "OK") PrimaryCoral else if (key == "C") Color(0xFFEF4444) else MaterialTheme.colorScheme.surface
                                        ),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = key,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp,
                                            color = if (key in listOf("OK", "C")) Color.White else MaterialTheme.colorScheme.onSurface
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
        title = if (isSuccess) "Mükemmel Hesaplama!" else "Matematiksel Uyumsuzluk",
        message = if (isSuccess) "2 + 6 = 8 ve 5 + 6 = 11! Her iki doğrultudaki denklem matrisini kusursuz çözdün." else "Yatay ve dikey işlemlerin her ikisini de sağlayan sayıyı bulmalısın (İpucu: 2 + ? = 8).",
        score = if (isSuccess) 120 else 20,
        isSuccess = isSuccess,
        onPlayAgain = {
            isResultOpen = false
            userInputValue = ""
        },
        onBackToHub = onBack
    )
}

// ==========================================
// 11. SAYI ZİNCİRİ (NUMBER CHAIN)
// ==========================================
data class ChainLevel(
    val numbers: List<String>,
    val correctAnswer: Int,
    val ruleHint: String,
    val options: List<Int>
)

private val CHAIN_LEVELS = listOf(
    ChainLevel(
        numbers = listOf("3", "7", "15", "31", "?"),
        correctAnswer = 63,
        ruleHint = "Kural: Bir önceki sayıyı 2 ile çarpıp 1 ekle (n * 2 + 1). (31 * 2 + 1 = 63)",
        options = listOf(59, 61, 63, 65)
    ),
    ChainLevel(
        numbers = listOf("1", "4", "9", "16", "25", "?"),
        correctAnswer = 36,
        ruleHint = "Kural: Sıralı tam kareler (1², 2², 3², 4², 5², 6² = 36)",
        options = listOf(30, 34, 36, 49)
    ),
    ChainLevel(
        numbers = listOf("2", "3", "5", "8", "13", "?"),
        correctAnswer = 21,
        ruleHint = "Kural: Fibonacci dizisi; her sayı kendinden önceki iki sayının toplamıdır (8 + 13 = 21)",
        options = listOf(19, 21, 23, 25)
    )
)

@Composable
fun NumberChainGameScreen(
    repository: GameRepository,
    soundManager: SoundAndVibeManager,
    onBack: () -> Unit
) {
    var levelIdx by remember { mutableIntStateOf(0) }
    val level = CHAIN_LEVELS[levelIdx % CHAIN_LEVELS.size]
    var selectedOption by remember { mutableStateOf<Int?>(null) }
    var score by remember { mutableIntStateOf(0) }
    var isResultOpen by remember { mutableStateOf(false) }
    var isSuccess by remember { mutableStateOf(false) }

    LaunchedEffect(levelIdx) {
        selectedOption = null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        GameHeader(
            title = "Sayı Zinciri",
            score = score,
            onBack = onBack,
            statusText = "Dizi ${levelIdx + 1} / ${CHAIN_LEVELS.size}"
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                InfoTipBox(text = "Sayılar arasındaki artış örüntüsünü çöz. Soru işaretinin yerine gelecek sayıyı bul.")
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
                        Text("MATEMATİKSEL DİZİ", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            level.numbers.forEach { numStr ->
                                val isTarget = numStr == "?"
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isTarget) PrimaryCoral.copy(alpha = 0.25f) else MintGreen.copy(alpha = 0.6f))
                                        .then(if (isTarget) Modifier.border(2.dp, PrimaryCoral, RoundedCornerShape(10.dp)) else Modifier),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = numStr,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isTarget) PrimaryCoral else MintGreenDark
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Text("Soru İşareti Yerine Hangi Sayı Gelmeli?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    level.options.forEach { option ->
                        val isSelected = selectedOption == option
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
                                    selectedOption = option
                                }
                                .padding(vertical = 12.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Text(
                                    text = option.toString(),
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) PrimaryCoral else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        val correct = selectedOption == level.correctAnswer
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
                        .testTag("submit_chain_button")
                ) {
                    Text("Cevabı Onayla", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }

    GameResultDialog(
        isOpen = isResultOpen,
        title = if (isSuccess) "Tebrikler! Sayı Zincirini Çözdün" else "Hatalı Tahmin",
        message = level.ruleHint,
        score = if (isSuccess) 90 else 15,
        isSuccess = isSuccess,
        onPlayAgain = {
            isResultOpen = false
            levelIdx++
        },
        onBackToHub = onBack
    )
}
