package com.example.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kotlinx.coroutines.flow.first
import com.example.data.repository.GameRepository
import com.example.ui.components.NeonBackground
import com.example.ui.components.bounceClick
import com.example.ui.components.neonGlow
import com.example.ui.game.LevelPlayScreen
import com.example.ui.screens.gameslist.GamesScreen
import com.example.ui.screens.home.HomeScreen
import com.example.ui.screens.leaderboard.LeaderboardScreen
import com.example.ui.screens.levels.LevelMapScreen
import com.example.ui.screens.profile.ProfileHubScreen
import com.example.ui.screens.progress.WeeklyTrendScreen
import com.example.ui.screens.shop.ShopScreen
import com.example.ui.theme.NeonCard
import com.example.ui.theme.NeonBlue
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.NeonSurface
import com.example.ui.theme.OnNeon
import com.example.ui.theme.TextMuted
import com.example.util.SoundAndVibeManager

sealed class MainTab(
    val route: String,
    val title: String,
    val icon: ImageVector,
    val iconOutlined: ImageVector
) {
    data object Home : MainTab("tab_home", "Ana Sayfa", Icons.Filled.Home, Icons.Outlined.Home)
    data object Games : MainTab("tab_games", "Oyunlar", Icons.Filled.Extension, Icons.Outlined.Extension)
    data object Records : MainTab("tab_records", "Rekorlar", Icons.Filled.EmojiEvents, Icons.Outlined.EmojiEvents)
    data object Profile : MainTab("tab_profile", "Profil", Icons.Filled.Person, Icons.Outlined.Person)

    companion object {
        val all: List<MainTab> by lazy { listOf(Home, Games, Records, Profile) }
        fun indexOf(route: String?): Int = all.indexOfFirst { it.route == route }
    }
}

object Routes {
    const val SHOP = "shop"
    const val WEEKLY = "weekly_trends"
    const val LEVELS = "levels/{gameId}"
    const val PLAY = "play/{gameId}/{level}"
    fun levels(gameId: String) = "levels/$gameId"
    fun play(gameId: String, level: Int) = "play/$gameId/$level"
}

private fun tabIndex(entry: NavBackStackEntry?): Int = MainTab.indexOf(entry?.destination?.route)

private fun AnimatedContentTransitionScope<NavBackStackEntry>.enterFor(): EnterTransition {
    val from = tabIndex(initialState)
    val to = tabIndex(targetState)
    return if (from >= 0 && to >= 0) {
        val dir = if (to > from) 1 else -1
        slideInHorizontally(tween(340)) { it * dir / 4 } + fadeIn(tween(300))
    } else if (to < 0) {
        slideInHorizontally(tween(360)) { it / 3 } + fadeIn(tween(300)) + scaleIn(initialScale = 0.96f)
    } else {
        fadeIn(tween(300)) + scaleIn(initialScale = 0.97f)
    }
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.exitFor(): ExitTransition {
    val from = tabIndex(initialState)
    val to = tabIndex(targetState)
    return if (from >= 0 && to >= 0) {
        val dir = if (to > from) 1 else -1
        slideOutHorizontally(tween(300)) { -it * dir / 4 } + fadeOut(tween(220))
    } else if (to < 0) {
        fadeOut(tween(250)) + scaleOut(targetScale = 0.96f)
    } else {
        slideOutHorizontally(tween(320)) { it / 3 } + fadeOut(tween(250))
    }
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.popEnterFor(): EnterTransition {
    val to = tabIndex(targetState)
    return if (to >= 0 && tabIndex(initialState) < 0) fadeIn(tween(300)) + scaleIn(initialScale = 0.96f) else enterFor()
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.popExitFor(): ExitTransition {
    val from = tabIndex(initialState)
    return if (from < 0) slideOutHorizontally(tween(320)) { it / 3 } + fadeOut(tween(250)) else exitFor()
}

@Composable
fun ZihinParkAppNavigation(
    repository: GameRepository,
    soundManager: SoundAndVibeManager,
    startRoute: String? = null
) {
    val navController = rememberNavController()
    androidx.compose.runtime.LaunchedEffect(startRoute) {
        if (!startRoute.isNullOrBlank() && startRoute != MainTab.Home.route) {
            // Grafik kurulana kadar bekle
            navController.currentBackStackEntryFlow.first()
            runCatching { navController.navigate(startRoute) }
        }
    }
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    var gamesCategory by rememberSaveable { mutableStateOf<String?>(null) }
    val showBar = MainTab.indexOf(currentRoute) >= 0

    fun goTab(tab: MainTab) {
        if (currentRoute == tab.route) return
        soundManager.playTap()
        navController.navigate(tab.route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun openGame(gameId: String) = navController.navigate(Routes.levels(gameId))

    fun playLevel(gameId: String, level: Int) = navController.navigate(Routes.play(gameId, level))

    NeonBackground {
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.material3.LocalContentColor provides com.example.ui.theme.TextPrimary
        ) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f)) {
                NavHost(
                    navController = navController,
                    startDestination = MainTab.Home.route,
                    enterTransition = { enterFor() },
                    exitTransition = { exitFor() },
                    popEnterTransition = { popEnterFor() },
                    popExitTransition = { popExitFor() }
                ) {
                    composable(MainTab.Home.route) {
                        HomeScreen(
                            repository = repository,
                            onOpenGame = ::openGame,
                            onPlayLevel = ::playLevel,
                            onOpenCategory = { cat ->
                                gamesCategory = cat
                                goTab(MainTab.Games)
                            },
                            onOpenShop = { navController.navigate(Routes.SHOP) }
                        )
                    }
                    composable(MainTab.Games.route) {
                        GamesScreen(
                            repository = repository,
                            initialCategory = gamesCategory,
                            onOpenGame = ::openGame,
                            onOpenShop = { navController.navigate(Routes.SHOP) }
                        )
                    }
                    composable(MainTab.Records.route) {
                        LeaderboardScreen(repository = repository)
                    }
                    composable(MainTab.Profile.route) {
                        ProfileHubScreen(
                            repository = repository,
                            soundManager = soundManager,
                            onOpenGames = { goTab(MainTab.Games) },
                            onOpenWeeklyTrends = { navController.navigate(Routes.WEEKLY) }
                        )
                    }
                    composable(Routes.SHOP) {
                        ShopScreen(repository = repository, soundManager = soundManager, onBack = { navController.popBackStack() })
                    }
                    composable(Routes.WEEKLY) {
                        WeeklyTrendScreen(repository = repository, onBackClick = { navController.popBackStack() })
                    }
                    composable(
                        Routes.LEVELS,
                        arguments = listOf(navArgument("gameId") { type = NavType.StringType })
                    ) { entry ->
                        val gameId = entry.arguments?.getString("gameId").orEmpty()
                        LevelMapScreen(
                            gameId = gameId,
                            repository = repository,
                            onBack = { navController.popBackStack() },
                            onPlay = { level -> playLevel(gameId, level) }
                        )
                    }
                    composable(
                        Routes.PLAY,
                        arguments = listOf(
                            navArgument("gameId") { type = NavType.StringType },
                            navArgument("level") { type = NavType.IntType }
                        ),
                        enterTransition = { slideInVertically(tween(380)) { it / 5 } + fadeIn(tween(300)) },
                        popExitTransition = { slideOutVertically(tween(320)) { it / 5 } + fadeOut(tween(250)) }
                    ) { entry ->
                        val gameId = entry.arguments?.getString("gameId").orEmpty()
                        val level = entry.arguments?.getInt("level") ?: 1
                        LevelPlayScreen(
                            gameId = gameId,
                            level = level,
                            repository = repository,
                            soundManager = soundManager,
                            onExit = { exitToLevels(navController, gameId) },
                            onPlayLevel = { next ->
                                navController.navigate(Routes.play(gameId, next)) {
                                    popUpTo(Routes.PLAY) { inclusive = true }
                                }
                            }
                        )
                    }
                }
            }
            AnimatedVisibility(
                visible = showBar,
                enter = slideInVertically(spring(dampingRatio = Spring.DampingRatioLowBouncy)) { it } + fadeIn(),
                exit = slideOutVertically(tween(220)) { it } + fadeOut(tween(180))
            ) {
                NeonBottomBar(selected = MainTab.indexOf(currentRoute).coerceAtLeast(0), onSelect = { goTab(MainTab.all[it]) })
            }
        }
        }
    }
}

/** Oyundan çıkınca o oyunun seviye haritasına döner (gerekirse haritayı açar). */
private fun exitToLevels(navController: NavHostController, gameId: String) {
    val prev = navController.previousBackStackEntry
    val prevRoute = prev?.destination?.route
    val prevGame = prev?.arguments?.getString("gameId")
    if (prevRoute == Routes.LEVELS && prevGame == gameId) {
        navController.popBackStack()
    } else {
        navController.navigate(Routes.levels(gameId)) {
            popUpTo(Routes.PLAY) { inclusive = true }
        }
    }
}

/**
 * LUMEXA tarzı alt navigasyon: seçili sekme genişleyip ikon + yazılı neon bir hapa dönüşür,
 * diğerleri yalnızca ikon olarak kalır. Genişleme yaylı animasyonla olur.
 */
@Composable
fun NeonBottomBar(selected: Int, onSelect: (Int) -> Unit) {
    val tabs = MainTab.all
    val shape = RoundedCornerShape(36.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(NeonSurface.copy(alpha = 0f), NeonSurface)))
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(68.dp)
                .neonGlow(NeonPurple, 18.dp, 36.dp, 0.25f)
                .clip(shape)
                .background(NeonCard.copy(alpha = 0.97f))
                .border(1.dp, Brush.horizontalGradient(listOf(NeonCyan.copy(alpha = 0.45f), NeonPurple.copy(alpha = 0.45f))), shape)
                .padding(7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEachIndexed { i, tab ->
                val isSel = i == selected
                val weight by animateFloatAsState(
                    if (isSel) 2.6f else 1f,
                    spring(dampingRatio = 0.68f, stiffness = Spring.StiffnessMediumLow),
                    label = "w$i"
                )
                val pill by animateFloatAsState(if (isSel) 1f else 0f, tween(260), label = "p$i")
                val iconScale by animateFloatAsState(if (isSel) 1.08f else 1f, spring(dampingRatio = 0.45f), label = "s$i")
                Box(
                    Modifier
                        .weight(weight)
                        .fillMaxHeight()
                        .padding(horizontal = 2.dp)
                        .then(if (pill > 0.01f) Modifier.neonGlow(NeonPurple, 14.dp, 30.dp, 0.55f * pill) else Modifier)
                        .clip(RoundedCornerShape(30.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(NeonPurple.copy(alpha = pill), NeonBlue.copy(alpha = pill), NeonCyan.copy(alpha = pill))
                            )
                        )
                        .bounceClick { onSelect(i) },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                        Icon(
                            if (isSel) tab.icon else tab.iconOutlined,
                            contentDescription = tab.title,
                            tint = if (isSel) androidx.compose.ui.graphics.Color.White else TextMuted,
                            modifier = Modifier.size(26.dp).graphicsLayer { scaleX = iconScale; scaleY = iconScale }
                        )
                        AnimatedVisibility(
                            visible = isSel,
                            enter = expandHorizontally(spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMediumLow)) + fadeIn(tween(220, delayMillis = 60)),
                            exit = shrinkHorizontally(tween(180)) + fadeOut(tween(120))
                        ) {
                            Text(
                                tab.title,
                                modifier = Modifier.padding(start = 8.dp),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold,
                                color = androidx.compose.ui.graphics.Color.White,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        }
    }
}
