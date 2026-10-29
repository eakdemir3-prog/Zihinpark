package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.RoomDatabaseModule
import com.example.data.local.entity.GameEntity
import com.example.domain.model.GameCategory
import com.example.ui.theme.MintGreen
import com.example.ui.theme.MintGreenDark
import com.example.ui.theme.PastelBlue
import com.example.ui.theme.PastelBlueDark
import com.example.ui.theme.PastelLavender
import com.example.ui.theme.PastelPeach
import com.example.ui.theme.PastelYellow
import com.example.ui.theme.PrimaryCoral
import com.example.ui.theme.PrimaryCoralVariant
import com.example.ui.viewmodel.GameViewModel

/**
 * MainGameScreen Composable UI:
 * Room veritabanından GameViewModel aracılığıyla çekilen 16 temel zihin ve
 * mantık oyununu LazyVerticalGrid ızgara görünümünde listeler.
 *
 * @param viewModel Room veritabanı işlemlerini ve oyun akışını yöneten [GameViewModel]
 * @param modifier Composable düzenleyicisi
 * @param onNavigateToGame Bir oyuna tıklandığında oyun rotasına ("game/{id}") yönlendiren lambda
 * @param onGameClick Doğrudan [GameEntity] nesnesi ile oyun seçimi için isteğe bağlı lambda
 */
@Composable
fun MainGameScreen(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier,
    onNavigateToGame: (String) -> Unit = {},
    onGameClick: ((GameEntity) -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    val base16Games by viewModel.base16Games.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var selectedDifficulty by remember { mutableStateOf<String?>(null) }

    // Room veritabanından gelen 16 temel oyunu filtrele
    val displayedGames = remember(base16Games, searchQuery, selectedCategory, selectedDifficulty) {
        base16Games.filter { game ->
            val matchesQuery = searchQuery.isBlank() ||
                    game.name.contains(searchQuery, ignoreCase = true) ||
                    game.description.contains(searchQuery, ignoreCase = true) ||
                    game.category.contains(searchQuery, ignoreCase = true)

            val matchesCategory = selectedCategory == null || game.category == selectedCategory
            val matchesDifficulty = selectedDifficulty == null || game.difficulty == selectedDifficulty

            matchesQuery && matchesCategory && matchesDifficulty
        }
    }

    val categories = remember {
        listOf(
            "Tümü" to null,
            "Mantık & Çıkarım" to "Mantık & Çıkarım",
            "Uzamsal Zekâ" to "Uzamsal Zekâ",
            "Sayısal & Analitik" to "Sayısal & Analitik",
            "Dikkat & Gözlem" to "Dikkat & Gözlem",
            "Strateji & Hız" to "Strateji & Hız"
        )
    }

    val difficulties = remember {
        listOf("Tümü" to null, "Kolay" to "Kolay", "Orta" to "Orta", "Zor" to "Zor")
    }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .testTag("main_game_screen"),
        color = MaterialTheme.colorScheme.background
    ) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 160.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // 1. Üst Başlık & İstatistik Özeti (Grid Span All)
            item(span = { GridItemSpan(maxLineSpan) }) {
                MainGameScreenHeader(
                    totalGamesCount = base16Games.size,
                    diamondBalance = uiState.diamondBalance,
                    totalStars = uiState.totalStars,
                    totalHighestScore = uiState.totalHighestScore
                )
            }

            // 2. Arama Çubuğu (Grid Span All)
            item(span = { GridItemSpan(maxLineSpan) }) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("main_game_search_input"),
                    placeholder = { Text("16 temel oyunda ara (Sudoku, Hafıza, Şifre...)") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Ara",
                            tint = PrimaryCoral
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { searchQuery = "" },
                                modifier = Modifier.testTag("main_game_search_clear")
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Temizle")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }

            // 3. Kategori Filtre Çipleri (Grid Span All)
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column {
                    Text(
                        text = "Zekâ Alanı",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(categories) { (label, value) ->
                            val isSelected = selectedCategory == value
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedCategory = value },
                                label = { Text(label, fontSize = 13.sp) },
                                shape = RoundedCornerShape(12.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = PrimaryCoral,
                                    selectedLabelColor = Color.White
                                ),
                                modifier = Modifier.testTag("filter_category_${label.replace(" ", "_")}")
                            )
                        }
                    }
                }
            }

            // 4. Zorluk Seviyesi Filtre Çipleri (Grid Span All)
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column {
                    Text(
                        text = "Zorluk Derecesi",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(difficulties) { (label, value) ->
                            val isSelected = selectedDifficulty == value
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedDifficulty = value },
                                label = { Text(label, fontSize = 12.sp) },
                                shape = RoundedCornerShape(10.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = when (label) {
                                        "Kolay" -> MintGreenDark
                                        "Orta" -> Color(0xFFD97706)
                                        "Zor" -> PrimaryCoralVariant
                                        else -> PrimaryCoral
                                    },
                                    selectedLabelColor = Color.White
                                ),
                                modifier = Modifier.testTag("filter_difficulty_${label}")
                            )
                        }
                    }
                }
            }

            // 5. Grid Durum Bilgisi (Grid Span All)
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "16 Temel Oyun (${displayedGames.size} listeleniyor)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    // Veritabanı senkronizasyon etiketi
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MintGreen.copy(alpha = 0.5f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(MintGreenDark)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Room Aktif",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MintGreenDark
                        )
                    }
                }
            }

            // 6. Yükleniyor veya Boş Durum
            if (uiState.isLoading && displayedGames.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = PrimaryCoral)
                    }
                }
            } else if (displayedGames.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp)
                            .testTag("empty_games_card"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = "🔍", fontSize = 42.sp)
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Kriterlere Uygun Oyun Bulunamadı",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Arama kelimesini değiştirebilir veya filtreleri sıfırlayabilirsiniz.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    searchQuery = ""
                                    selectedCategory = null
                                    selectedDifficulty = null
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryCoral),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Filtreleri Sıfırla")
                            }
                        }
                    }
                }
            }

            // 7. 16 Temel Oyun Kartları (Lazy Grid Elemanları)
            items(displayedGames, key = { it.id }) { game ->
                GameGridItemCard(
                    game = game,
                    onClick = {
                        onGameClick?.invoke(game)
                        onNavigateToGame("game/${game.id}")
                    }
                )
            }
        }
    }
}

/**
 * Ana ekran başlık ve özet istatistik alanı
 */
@Composable
private fun MainGameScreenHeader(
    totalGamesCount: Int,
    diamondBalance: Int,
    totalStars: Int,
    totalHighestScore: Int
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "ZihinPark",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = PrimaryCoral
                )
                Text(
                    text = "Düşün • Keşfet • Geliş ($totalGamesCount Temel Oyun)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Elmas Bakiyesi
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = PastelYellow,
                modifier = Modifier.testTag("main_game_diamonds_pill")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Diamond,
                        contentDescription = "Elmas",
                        tint = Color(0xFFD97706),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "$diamondBalance",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color(0xFF92400E)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Mini İstatistik Rozetleri
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            HeaderStatBadge(
                icon = "🏆",
                title = "Toplam Skor",
                value = "$totalHighestScore",
                backgroundColor = PastelPeach.copy(alpha = 0.5f),
                modifier = Modifier.weight(1f)
            )
            HeaderStatBadge(
                icon = "⭐",
                title = "Kazanılan Yıldız",
                value = "$totalStars",
                backgroundColor = PastelLavender.copy(alpha = 0.5f),
                modifier = Modifier.weight(1f)
            )
            HeaderStatBadge(
                icon = "🧩",
                title = "Temel Egzersiz",
                value = "$totalGamesCount Adet",
                backgroundColor = PastelBlue.copy(alpha = 0.5f),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun HeaderStatBadge(
    icon: String,
    title: String,
    value: String,
    backgroundColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(icon, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = title,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * 16 Temel Oyun için LazyVerticalGrid kart bileşeni
 */
@Composable
fun GameGridItemCard(
    game: GameEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val categoryColor = when (game.category) {
        "Mantık & Çıkarım" -> PrimaryCoral
        "Uzamsal Zekâ" -> PastelBlueDark
        "Sayısal & Analitik" -> MintGreenDark
        "Dikkat & Gözlem" -> PrimaryCoralVariant
        "Strateji & Hız" -> Color(0xFFD97706)
        else -> PrimaryCoral
    }

    val difficultyColor = when (game.difficulty) {
        "Kolay" -> MintGreen
        "Orta" -> PastelPeach
        "Zor" -> Color(0xFFFFD6D6)
        else -> PastelPeach
    }

    val difficultyTextColor = when (game.difficulty) {
        "Kolay" -> MintGreenDark
        "Orta" -> Color(0xFF92400E)
        "Zor" -> Color(0xFF991B1B)
        else -> Color(0xFF92400E)
    }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("game_card_${game.id}")
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Üst Satır: Emoji İkonu & Zorluk Etiketi
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Emoji Kutusu
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(categoryColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = game.iconEmoji,
                        fontSize = 28.sp
                    )
                }

                // Zorluk Rozeti
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(difficultyColor)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = game.difficulty,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = difficultyTextColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Kategori Etiketi
            Text(
                text = game.category,
                style = MaterialTheme.typography.labelSmall,
                color = categoryColor,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            // Oyun Adı
            Text(
                text = game.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Açıklama
            Text(
                text = game.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                fontSize = 11.sp,
                lineHeight = 15.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Alt Satır: En Yüksek Skor & Başla Butonu
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // En Yüksek Skor Bilgisi
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = "Skor",
                        tint = Color(0xFFD97706),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = if (game.highestScore > 0) "${game.highestScore}" else "-",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Yıldız Göstergesi
                Row(verticalAlignment = Alignment.CenterVertically) {
                    repeat(3) { starIndex ->
                        val isStarEarned = game.totalStars >= (starIndex + 1)
                        Icon(
                            imageVector = if (isStarEarned) Icons.Filled.Star else Icons.Outlined.Star,
                            contentDescription = null,
                            tint = if (isStarEarned) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                // Hızlı Başla Oynatma Butonu
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(PrimaryCoral)
                        .testTag("play_button_${game.id}"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Oyna",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
