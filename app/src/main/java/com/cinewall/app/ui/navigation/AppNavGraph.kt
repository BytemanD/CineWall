package com.cinewall.app.ui.navigation

import android.util.Log
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.rounded.VideoLibrary
//import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.tv.material3.Tab
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ButtonShape
import androidx.tv.material3.Card
import androidx.tv.material3.Glow
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.TabDefaults
import androidx.tv.material3.TabRow
import androidx.tv.material3.TabRowDefaults
import androidx.tv.material3.Text
import com.cinewall.app.repository.MediaRepository
import com.cinewall.app.ui.components.CineWallLogo
import com.cinewall.app.ui.components.MainTabs
import com.cinewall.app.ui.components.tvTouchClick
import com.cinewall.app.ui.player.PlayerScreen
import com.cinewall.app.ui.screen.DetailScreen
import com.cinewall.app.ui.screen.HomeScreen
import com.cinewall.app.ui.screen.LibraryScreen
import com.cinewall.app.ui.screen.SettingsScreen
import com.cinewall.app.ui.theme.blend
import com.cinewall.app.ui.theme.onColor
import kotlinx.coroutines.selects.select

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

// 1. 定义菜单数据模型
data class TopMenuItem(val route: String, val label: String,
                       var icon: ImageVector);

val menuItems = listOf(
    TopMenuItem(Routes.HOME, "首页", icon = Icons.Filled.Home),
    TopMenuItem(Routes.LIBRARY, "媒体库", icon = Icons.Filled.VideoLibrary),
    TopMenuItem(Routes.SETTINGS, "设置", icon = Icons.Filled.Settings)
)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TvTopNavigationBar(navController: NavController) {
    // 监听当前路由，用于高亮选中的菜单
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    // 使用 Surface 包裹 TopAppBar，设置背景色
    Surface (
        modifier = Modifier.fillMaxWidth(),
    ) {
//        CineWallLogo()
//        Spacer(Modifier.width(24.dp))
        // 使用 ScrollableTabRow 实现可滚动的顶部标签栏
        var selectedIndex = menuItems.indexOfFirst { it.route == currentRoute }.coerceAtLeast(0);
        Row (
            modifier = Modifier.padding(top=40.dp, bottom = 16.dp, start = 16.dp, end=16.dp),
        ) {
            CineWallLogo()
            Spacer(Modifier.width(24.dp))
            menuItems.forEach { item ->
                val isSelected = currentRoute == item.route
                val interactionSource = remember { MutableInteractionSource() }
                val isFocused by interactionSource.collectIsFocusedAsState()
//                var isFocused by remember { mutableStateOf(false) }
                var focusRequester = FocusRequester();
                if (isSelected) {
                    LocalFocusManager.current.clearFocus(true);
                    focusRequester.requestFocus();
                }
                Button(
                    onClick = { navController.navigate(item.route) },
                    modifier = Modifier.tvTouchClick {
//                        isFocused = true
                        focusRequester.requestFocus();

                        navController.navigate(item.route)
                    }.focusRequester(
                        focusRequester
                    ),
                    colors = ButtonDefaults.colors(
                        containerColor = if (isSelected) primary else MaterialTheme.colorScheme.surfaceVariant ,
                        contentColor = secondary,
                        focusedContentColor = secondary,
                        pressedContentColor = secondary,
                        focusedContainerColor = if (isSelected) primary else primary.copy( 0.3f),
                        pressedContainerColor = primary,
                    ),
                    shape = ButtonDefaults.shape(
                        shape = RoundedCornerShape(16.dp)
                    ),
                    glow = ButtonDefaults.glow(
                        focusedGlow = Glow(primary.copy(alpha = 0.5f), 16.dp)
                    ),
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = null,
                        modifier = Modifier.size(26.dp)
                    )
                    Text(item.label, style = MaterialTheme.typography.titleLarge,)
                }
                Spacer(Modifier.width(14.dp))

            }
        }




    }
}
@Composable
fun CineWallNavHost(repository: MediaRepository) {
    val navController = rememberNavController()
    Scaffold (
        modifier = Modifier
            .fillMaxWidth(),
        topBar = {
            TvTopNavigationBar(navController = navController)
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            NavHost(
                navController = navController,
                startDestination = "home"
            ) {
                composable(Routes.HOME) { HomeScreen(
                    repository = repository,
                    onOpenDetail = { item -> navController.navigate(Routes.detail(item.id)) },
                    onPlay = { item -> navController.navigate(Routes.player(item.id)) },
                    onNavigate = { route -> navController.navigate(route) },
                    onNavigateToLibrary = { kind ->
//                        LocalFocusManager.current.clearFocus(true);
                        navController.navigate(Routes.library(kind)) {
                            popUpTo(Routes.HOME) { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                ) }
                composable(Routes.LIBRARY) { entry ->
                    LibraryScreen(
                    repository = repository,
                    initialKind = entry.arguments?.getString("kind") ?: "all",
                    onOpenDetail = { item -> navController.navigate(Routes.detail(item.id)) },
                        onNavigate = { route -> navController.navigate(route) },
                ) }
                composable(Routes.SETTINGS) { SettingsScreen(
                    repository = repository,
                    onNavigate = { route -> navController.navigate(route) },
                ) }
                composable(Routes.DETAIL) { SettingsScreen(
                    repository = repository,
                    onNavigate = { route -> navController.navigate(route) },
                ) }
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
    }

}
