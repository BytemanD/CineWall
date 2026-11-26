package com.cinewall.app.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Glow
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.cinewall.app.model.GlobalSnackBar
import com.cinewall.app.repository.ConfigRepository
import com.cinewall.app.repository.MediaRepository
import com.cinewall.app.ui.components.CineWallLogo
import com.cinewall.app.ui.components.tvTouchClick
import com.cinewall.app.ui.player.PlayerScreen
import com.cinewall.app.ui.screen.DetailScreen
import com.cinewall.app.ui.screen.HomeScreen
import com.cinewall.app.ui.screen.LibraryScreen
import com.cinewall.app.ui.screen.SettingsScreen
import com.cinewall.app.ui.screen.viewmodels.ConfigViewModel


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
data class TopMenuItem(
    val route: String, val label: String,
    var icon: ImageVector
);

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

    val isPlayScreen = currentBackStackEntry?.destination?.route == Routes.PLAYER
    if (isPlayScreen) {
        return;
    }
    // 使用 Surface 包裹 TopAppBar，设置背景色
    Surface(modifier = Modifier.fillMaxWidth(),) {
//        CineWallLogo()
//        Spacer(Modifier.width(24.dp))
        // 使用 ScrollableTabRow 实现可滚动的顶部标签栏
        var selectedIndex = menuItems.indexOfFirst { it.route == currentRoute }.coerceAtLeast(0);
        Row(
            modifier = Modifier.padding(top = 40.dp, bottom = 16.dp, start = 16.dp, end = 16.dp),
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
                    modifier = Modifier
                        .tvTouchClick {
//                        isFocused = true
                            focusRequester.requestFocus();

                            navController.navigate(item.route)
                        }
                        .focusRequester(
                            focusRequester
                        ),
                    colors = ButtonDefaults.colors(
                        containerColor = if (isSelected) primary else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = secondary,
                        focusedContentColor = secondary,
                        pressedContentColor = secondary,
                        focusedContainerColor = if (isSelected) primary else primary.copy(0.3f),
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
                    Text(item.label, style = MaterialTheme.typography.titleLarge)
                }
                Spacer(Modifier.width(14.dp))

            }
        }
    }
}

@Composable
fun CineWallNavHost(repository: MediaRepository,
                    configRepository: ConfigRepository,
                    configViewModel: ConfigViewModel
) {
    val navController = rememberNavController()

    val hostState = remember { SnackbarHostState() }
    var _loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        configViewModel.load()
        _loading = false
    }

    // 主页面
    Scaffold(
        modifier = Modifier.fillMaxWidth(),
        topBar = { TvTopNavigationBar(navController = navController) },
    ) { innerPadding ->


        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            NavHost(
                navController = navController,
                startDestination = Routes.HOME,
                // 1. 正向导航：新页面从右侧滑入
                enterTransition = {
                    slideInHorizontally(
                        initialOffsetX = { fullWidth -> fullWidth },
                        animationSpec = tween(durationMillis = 300)
                    )
                },
                // 2. 正向导航：旧页面向左侧滑出
                exitTransition = {
                    slideOutHorizontally(
                        targetOffsetX = { fullWidth -> -fullWidth },
                        animationSpec = tween(durationMillis = 300)
                    )
                },
                // 3. 反向导航（返回）：前一个页面从左侧滑入
                popEnterTransition = {
                    slideInHorizontally(
                        initialOffsetX = { fullWidth -> -fullWidth },
                        animationSpec = tween(durationMillis = 300)
                    )
                },
                // 4. 反向导航（返回）：当前页面向右侧滑出
                popExitTransition = {
                    slideOutHorizontally(
                        targetOffsetX = { fullWidth -> fullWidth },
                        animationSpec = tween(durationMillis = 300)
                    )
                }
            ) {
                composable(Routes.HOME) {
                    HomeScreen(
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
                    )
                }
                composable(Routes.LIBRARY) { entry ->
                    LibraryScreen(
                        repository = repository,
                        configRepository = configRepository,
                        initialKind = entry.arguments?.getString("kind") ?: "all",
                        onOpenDetail = { item -> navController.navigate(Routes.detail(item.id)) },
                        onNavigate = { route -> navController.navigate(route) },
                    )
                }
                composable(Routes.SETTINGS) {
                    SettingsScreen(
                        repository = repository,
                        configRepository = configRepository,
                        viewModel = configViewModel
                    )
                }
                composable(Routes.DETAIL) { entry ->
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
            SnackbarHost(
                hostState = hostState,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }

        // 全局唯一收集点：一条显示完/顶掉，自然串行
        LaunchedEffect(Unit) {
            GlobalSnackBar.requests.collect { req ->
                val result = hostState.showSnackbar(
                    message = req.message,
                    actionLabel = req.actionLabel,
                )
                if (result == SnackbarResult.ActionPerformed) {
                    req.onAction()                       // 点了"撤销"才执行
                }
            }
        }
    }
}
