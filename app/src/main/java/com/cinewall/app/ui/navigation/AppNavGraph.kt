package com.cinewall.app.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.IntOffset
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.cinewall.app.repository.MediaRepository
import com.cinewall.app.ui.components.MainTabs
import com.cinewall.app.ui.player.PlayerScreen
import com.cinewall.app.ui.screen.DetailScreen
import com.cinewall.app.ui.screen.HomeScreen
import com.cinewall.app.ui.screen.LibraryScreen
import com.cinewall.app.ui.screen.SettingsScreen

object Routes {
    const val HOME = "home"
    const val SETTINGS = "settings"
    const val LIBRARY = "library/{kind}"
    const val DETAIL = "detail/{mediaId}"
    const val PLAYER = "player/{mediaId}"

    fun library(kind: String) = "library/$kind"
    fun detail(mediaId: String) = "detail/$mediaId"
    fun player(mediaId: String) = "player/$mediaId"
}

@Composable
fun CineWallNavHost(repository: MediaRepository) {
    val navController = rememberNavController()

    fun navigateTab(route: String) {
        val destination = if (route == MainTabs.LIBRARY) Routes.library("all") else route
        navController.navigate(destination) {
            popUpTo(Routes.HOME) { inclusive = false }
            launchSingleTop = true
        }
    }

    val pageSlide = tween<IntOffset>(durationMillis = 220)
    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
        // 前进：新页面从右侧进入，旧页面向左退出。
        enterTransition = {
            slideInHorizontally(animationSpec = pageSlide, initialOffsetX = { it })
        },
        exitTransition = {
            slideOutHorizontally(animationSpec = pageSlide, targetOffsetX = { -it })
        },
        // 返回：旧页面（如播放器）向右滑出，新页面从左侧进入。
        popEnterTransition = {
            slideInHorizontally(animationSpec = pageSlide, initialOffsetX = { -it })
        },
        popExitTransition = {
            slideOutHorizontally(animationSpec = pageSlide, targetOffsetX = { it })
        }
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                repository = repository,
                onOpenDetail = { item -> navController.navigate(Routes.detail(item.id)) },
                onPlay = { item -> navController.navigate(Routes.player(item.id)) },
                onNavigate = { route -> navigateTab(route) },
                onNavigateToLibrary = { kind ->
                    navController.navigate(Routes.library(kind)) {
                        popUpTo(Routes.HOME) { inclusive = false }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(
            route = Routes.LIBRARY,
            arguments = listOf(navArgument("kind") { type = NavType.StringType })
        ) { entry ->
            LibraryScreen(
                repository = repository,
                initialKind = entry.arguments?.getString("kind") ?: "all",
                onOpenDetail = { item -> navController.navigate(Routes.detail(item.id)) },
                onNavigate = { route -> navigateTab(route) }
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                repository = repository,
                onNavigate = { route -> navigateTab(route) }
            )
        }

        composable(
            route = Routes.DETAIL,
            arguments = listOf(navArgument("mediaId") { type = NavType.StringType })
        ) { entry ->
            DetailScreen(
                mediaId = entry.arguments?.getString("mediaId").orEmpty(),
                repository = repository,
                onPlay = { item -> navController.navigate(Routes.player(item.id)) },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.PLAYER,
            arguments = listOf(navArgument("mediaId") { type = NavType.StringType })
        ) { entry ->
            PlayerScreen(
                mediaId = entry.arguments?.getString("mediaId").orEmpty(),
                repository = repository,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
