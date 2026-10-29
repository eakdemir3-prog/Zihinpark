package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.GameEntity
import com.example.data.repository.GameRepository
import com.example.domain.model.GameSortOption
import com.example.data.local.RoomDatabaseModule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class GameListUiState(
    val games: List<GameEntity> = emptyList(),
    val filteredGames: List<GameEntity> = emptyList(),
    val searchQuery: String = "",
    val selectedCategory: String? = null,
    val selectedDifficulty: String? = null,
    val sortOption: GameSortOption = GameSortOption.NAME_ASC,
    val diamondBalance: Int = 50,
    val totalStars: Int = 0,
    val totalHighestScore: Int = 0,
    val totalPlays: Int = 0,
    val isLoading: Boolean = false,
    val userFeedbackMessage: String? = null
)

open class GameListViewModel(
    val repository: GameRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _selectedCategory = MutableStateFlow<String?>(null)
    private val _selectedDifficulty = MutableStateFlow<String?>(null)
    private val _sortOption = MutableStateFlow(GameSortOption.NAME_ASC)
    private val _feedbackMessage = MutableStateFlow<String?>(null)

    /**
     * Room veritabanından çekilen 16 temel beyin ve mantık oyunu listesi.
     */
    val base16Games: StateFlow<List<GameEntity>> = repository.allGames.map { list ->
        val base16Ids = RoomDatabaseModule.BASE_16_GAMES.map { it.id }.toSet()
        val filtered = list.filter { it.id in base16Ids }
        if (filtered.isNotEmpty()) filtered else RoomDatabaseModule.BASE_16_GAMES
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = RoomDatabaseModule.BASE_16_GAMES
    )

    /**
     * Room veritabanındaki tüm oyunların reaktif StateFlow akışı.
     */
    val games: StateFlow<List<GameEntity>> = repository.allGames.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val uiState: StateFlow<GameListUiState> = combine(
        repository.allGames,
        _searchQuery,
        _selectedCategory,
        _selectedDifficulty,
        _sortOption,
        repository.preferences.diamondBalance,
        repository.totalStars,
        repository.totalHighestScore
    ) { params ->
        @Suppress("UNCHECKED_CAST")
        val games = params[0] as List<GameEntity>
        val query = params[1] as String
        val category = params[2] as String?
        val difficulty = params[3] as String?
        val sort = params[4] as GameSortOption
        val diamonds = params[5] as Int
        val stars = (params[6] as Int?) ?: 0
        val totalHigh = (params[7] as Int?) ?: 0

        val filtered = games.filter { game ->
            val matchesQuery = query.isEmpty() ||
                    game.name.contains(query, ignoreCase = true) ||
                    game.description.contains(query, ignoreCase = true)
            val matchesCategory = category == null || game.category == category
            val matchesDifficulty = difficulty == null || game.difficulty == difficulty
            matchesQuery && matchesCategory && matchesDifficulty
        }.let { list ->
            when (sort) {
                GameSortOption.NAME_ASC -> list.sortedBy { it.name }
                GameSortOption.HIGHEST_SCORE -> list.sortedByDescending { it.highestScore }
                GameSortOption.POPULAR -> list.sortedByDescending { it.totalPlays }
                GameSortOption.DIFFICULTY -> list.sortedBy {
                    when (it.difficulty) {
                        "Kolay" -> 1
                        "Orta" -> 2
                        else -> 3
                    }
                }
            }
        }

        GameListUiState(
            games = games,
            filteredGames = filtered,
            searchQuery = query,
            selectedCategory = category,
            selectedDifficulty = difficulty,
            sortOption = sort,
            diamondBalance = diamonds,
            totalStars = stars,
            totalHighestScore = totalHigh,
            totalPlays = games.sumOf { it.totalPlays },
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = GameListUiState(isLoading = true)
    )

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setCategory(category: String?) {
        _selectedCategory.value = category
    }

    fun setDifficulty(difficulty: String?) {
        _selectedDifficulty.value = difficulty
    }

    fun setSortOption(sort: GameSortOption) {
        _sortOption.value = sort
    }

    fun clearFeedback() {
        _feedbackMessage.value = null
    }

    fun completeGameLevel(
        gameId: String,
        gameName: String,
        category: String,
        score: Int,
        levelNumber: Int,
        stars: Int,
        durationSeconds: Long,
        onDiamondsEarned: (Int) -> Unit = {}
    ) {
        viewModelScope.launch {
            val earnedDiamonds = repository.saveGameResult(
                gameId = gameId,
                gameName = gameName,
                category = category,
                score = score,
                durationSeconds = durationSeconds,
                levelNumber = levelNumber,
                starsEarned = stars
            )
            if (earnedDiamonds > 0) {
                _feedbackMessage.value = "Tebrikler! Seviyeyi ilk kez tamamlayarak +$earnedDiamonds Elmas kazandın! 💎"
            }
            onDiamondsEarned(earnedDiamonds)
        }
    }

    fun claimRewardedAd(onSuccess: (Boolean) -> Unit) {
        viewModelScope.launch {
            val rewarded = repository.preferences.recordRewardedAdView()
            if (rewarded) {
                _feedbackMessage.value = "Ödüllü reklam tamamlandı: +3 Elmas hesabına eklendi! 💎"
            } else {
                _feedbackMessage.value = "Günlük reklam izleme limitine (3/3) ulaştınız. Yarın tekrar bekleriz!"
            }
            onSuccess(rewarded)
        }
    }

    fun purchaseCosmetic(itemId: String, cost: Int, onSuccess: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = repository.preferences.buyCosmetic(itemId, cost)
            if (success) {
                _feedbackMessage.value = "Öğe başarıyla satın alındı ve envantere eklendi! 🎉"
            } else {
                _feedbackMessage.value = "Yetersiz elmas bakiyesi! Seviyeleri tamamlayarak elmas kazanabilirsin."
            }
            onSuccess(success)
        }
    }
}
