package com.example.ui.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.repository.GameRepository
import com.example.ui.screens.games.CipherBreakerGameScreen
import com.example.ui.screens.games.ColorStroopGameScreen
import com.example.ui.screens.games.DetailHunterGameScreen
import com.example.ui.screens.games.HiddenObjectGameScreen
import com.example.ui.screens.games.JigsawPuzzleGameScreen
import com.example.ui.screens.games.LogicDetectiveGameScreen
import com.example.ui.screens.games.LogicDoorsGameScreen
import com.example.ui.screens.games.MathEngineerGameScreen
import com.example.ui.screens.games.MazeEscapeGameScreen
import com.example.ui.screens.games.MemoryLightsGameScreen
import com.example.ui.screens.games.NonogramGameScreen
import com.example.ui.screens.games.NumberChainGameScreen
import com.example.ui.screens.games.NumberMergeGameScreen
import com.example.ui.screens.games.PathFinderGameScreen
import com.example.ui.screens.games.PatternMasterGameScreen
import com.example.ui.screens.games.ReflexArenaGameScreen
import com.example.ui.screens.games.ReverseThinkingGameScreen
import com.example.ui.screens.games.RiverCrossingGameScreen
import com.example.ui.screens.games.ShapeArchitectGameScreen
import com.example.ui.screens.games.SpatialRotationGameScreen
import com.example.ui.screens.games.TargetTapGameScreen
import com.example.ui.screens.games.WordHuntGameScreen
import com.example.ui.screens.MainGameScreen
import com.example.ui.screens.gameshub.GamesHubScreen
import com.example.ui.screens.home.HomeScreen
import com.example.ui.screens.leaderboard.LeaderboardScreen
import com.example.ui.screens.profile.ProfileScreen
import com.example.ui.screens.progress.ProgressScreen
import com.example.ui.screens.progress.WeeklyTrendScreen
import com.example.ui.screens.shop.ShopScreen
import com.example.ui.theme.PrimaryCoral
import com.example.ui.viewmodel.GameListViewModel
import com.example.util.SoundAndVibeManager

sealed class MainTab(val route: String, val title: String, val icon: ImageVector) {
    object Home : MainTab("tab_home", "Ana Sayfa", Icons.Default.Home)
    object Games : MainTab("tab_games", "Oyunlar", Icons.Default.Extension)
    object Shop : MainTab("tab_shop", "Mağaza", Icons.Default.ShoppingBag)
    object Leaderboard : MainTab("tab_leaderboard", "Liderlik", Icons.Default.EmojiEvents)
    object Progress : MainTab("tab_progress", "İlerleme", Icons.Default.BarChart)
    object Profile : MainTab("tab_profile", "Profil", Icons.Default.Person)
}

@Composable
fun ZihinParkAppNavigation(
    repository: GameRepository,
    soundManager: SoundAndVibeManager,
    gameListViewModel: GameListViewModel
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    var selectedCategoryFilter by remember { mutableStateOf<String?>(null) }

    val tabs = listOf(
        MainTab.Home,
        MainTab.Games,
        MainTab.Shop,
        MainTab.Leaderboard,
        MainTab.Progress,
        MainTab.Profile
    )

    // Hide bottom bar when inside any game screen or weekly trends chart screen
    val isGameScreen = currentRoute?.startsWith("game/") == true || currentRoute == "weekly_trends"

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (!isGameScreen) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                ) {
                    tabs.forEach { tab ->
                        val isSelected = currentRoute == tab.route
                        NavigationBarItem(
                            icon = { Icon(tab.icon, contentDescription = tab.title) },
                            label = { Text(tab.title, maxLines = 1) },
                            selected = isSelected,
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = PrimaryCoral,
                                selectedTextColor = PrimaryCoral,
                                indicatorColor = PrimaryCoral.copy(alpha = 0.15f)
                            ),
                            onClick = {
                                if (currentRoute != tab.route) {
                                    soundManager.playClickSound()
                                    navController.navigate(tab.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = MainTab.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            // Main Tabs
            composable(MainTab.Home.route) {
                HomeScreen(
                    repository = repository,
                    onNavigateToGame = { route -> navController.navigate(route) },
                    onNavigateToCategory = { categoryName ->
                        selectedCategoryFilter = categoryName
                        navController.navigate(MainTab.Games.route)
                    }
                )
            }
            composable(MainTab.Games.route) {
                MainGameScreen(
                    viewModel = gameListViewModel,
                    onNavigateToGame = { route -> navController.navigate(route) }
                )
            }
            composable(MainTab.Shop.route) {
                ShopScreen(
                    repository = repository,
                    soundManager = soundManager
                )
            }
            composable(MainTab.Leaderboard.route) {
                LeaderboardScreen(
                    repository = repository
                )
            }
            composable(MainTab.Progress.route) {
                ProgressScreen(
                    repository = repository,
                    onNavigateToGames = { navController.navigate(MainTab.Games.route) },
                    onNavigateToWeeklyTrends = { navController.navigate("weekly_trends") }
                )
            }
            composable("weekly_trends") {
                WeeklyTrendScreen(
                    repository = repository,
                    onBackClick = { navController.popBackStack() }
                )
            }
            composable(MainTab.Profile.route) {
                ProfileScreen(
                    repository = repository,
                    soundManager = soundManager
                )
            }

            // --- 22 OYUN ROTALARI ---
            // 1. Logic Detective
            composable("game/logic_detective") {
                LogicDetectiveGameScreen(
                    repository = repository,
                    soundManager = soundManager,
                    onBack = { navController.popBackStack() }
                )
            }
            // 2. Spatial Rotation
            composable("game/spatial_rotation") {
                SpatialRotationGameScreen(
                    repository = repository,
                    soundManager = soundManager,
                    onBack = { navController.popBackStack() }
                )
            }
            // 3. Nonogram
            composable("game/nonogram") {
                NonogramGameScreen(
                    repository = repository,
                    soundManager = soundManager,
                    onBack = { navController.popBackStack() }
                )
            }
            // 4. Cipher Breaker
            composable("game/cipher_breaker") {
                CipherBreakerGameScreen(
                    repository = repository,
                    soundManager = soundManager,
                    onBack = { navController.popBackStack() }
                )
            }
            // 5. River Crossing
            composable("game/river_crossing") {
                RiverCrossingGameScreen(
                    repository = repository,
                    soundManager = soundManager,
                    onBack = { navController.popBackStack() }
                )
            }
            // 6. Shape Architect
            composable("game/shape_architect") {
                ShapeArchitectGameScreen(
                    repository = repository,
                    soundManager = soundManager,
                    onBack = { navController.popBackStack() }
                )
            }
            // 7. Reverse Thinking
            composable("game/reverse_thinking") {
                ReverseThinkingGameScreen(
                    repository = repository,
                    soundManager = soundManager,
                    onBack = { navController.popBackStack() }
                )
            }
            // 8. Math Engineer
            composable("game/math_engineer") {
                MathEngineerGameScreen(
                    repository = repository,
                    soundManager = soundManager,
                    onBack = { navController.popBackStack() }
                )
            }
            // 9. Detail Hunter
            composable("game/detail_hunter") {
                DetailHunterGameScreen(
                    repository = repository,
                    soundManager = soundManager,
                    onBack = { navController.popBackStack() }
                )
            }
            // 10. Path Finder
            composable("game/path_finder") {
                PathFinderGameScreen(
                    repository = repository,
                    soundManager = soundManager,
                    onBack = { navController.popBackStack() }
                )
            }
            // 11. Number Chain
            composable("game/number_chain") {
                NumberChainGameScreen(
                    repository = repository,
                    soundManager = soundManager,
                    onBack = { navController.popBackStack() }
                )
            }
            // 12. Word Hunt
            composable("game/word_hunt") {
                WordHuntGameScreen(
                    repository = repository,
                    soundManager = soundManager,
                    onBack = { navController.popBackStack() }
                )
            }
            // 13. Memory Lights
            composable("game/memory_lights") {
                MemoryLightsGameScreen(
                    repository = repository,
                    soundManager = soundManager,
                    onBack = { navController.popBackStack() }
                )
            }
            // 14. Reflex Arena
            composable("game/reflex_arena") {
                ReflexArenaGameScreen(
                    repository = repository,
                    soundManager = soundManager,
                    onBack = { navController.popBackStack() }
                )
            }
            // 15. Color Stroop
            composable("game/color_stroop") {
                ColorStroopGameScreen(
                    repository = repository,
                    soundManager = soundManager,
                    onBack = { navController.popBackStack() }
                )
            }
            // 16. Pattern Master
            composable("game/pattern_master") {
                PatternMasterGameScreen(
                    repository = repository,
                    soundManager = soundManager,
                    onBack = { navController.popBackStack() }
                )
            }
            // 17. Parçaları Birleştir
            composable("game/jigsaw_puzzle") {
                JigsawPuzzleGameScreen(
                    repository = repository,
                    soundManager = soundManager,
                    onBack = { navController.popBackStack() }
                )
            }
            // 18. Gizli Nesneyi Bul
            composable("game/hidden_object") {
                HiddenObjectGameScreen(
                    repository = repository,
                    soundManager = soundManager,
                    onBack = { navController.popBackStack() }
                )
            }
            // 19. Sayı Patlatma
            composable("game/number_merge") {
                NumberMergeGameScreen(
                    repository = repository,
                    soundManager = soundManager,
                    onBack = { navController.popBackStack() }
                )
            }
            // 20. Doğru Kapıyı Seç
            composable("game/logic_doors") {
                LogicDoorsGameScreen(
                    repository = repository,
                    soundManager = soundManager,
                    onBack = { navController.popBackStack() }
                )
            }
            // 21. Hedefe Dokun
            composable("game/target_tap") {
                TargetTapGameScreen(
                    repository = repository,
                    soundManager = soundManager,
                    onBack = { navController.popBackStack() }
                )
            }
            // 22. Labirentten Kaçış
            composable("game/maze_escape") {
                MazeEscapeGameScreen(
                    repository = repository,
                    soundManager = soundManager,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
