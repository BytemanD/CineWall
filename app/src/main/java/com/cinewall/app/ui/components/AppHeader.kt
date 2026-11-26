package com.cinewall.app.ui.components

import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.rounded.VideoLibrary

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Glow
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Tab
import androidx.tv.material3.TabDefaults
import androidx.tv.material3.TabRow
import androidx.tv.material3.TabRowDefaults
import androidx.tv.material3.Text
import com.cinewall.app.ui.theme.blend
import com.cinewall.app.ui.theme.onColor

object MainTabs {
    const val HOME = "home"
    const val LIBRARY = "library"
    const val SETTINGS = "settings"
    val items = listOf(
        "首页" to HOME,
        "媒体库" to LIBRARY,
        "设置" to SETTINGS
    )
}

/** Compose for TV 顶部导航：Logo + 大尺寸胶囊高亮 TabRow */
@Composable
fun AppHeader(
    currentTab: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // “设置”单独靠右显示，不参与左侧 TabRow
    val primaryTabs = MainTabs.items.filter { it.second != MainTabs.SETTINGS }
    val selectedIndex = primaryTabs.indexOfFirst { it.second == currentTab }
    val settingsSelected = currentTab == MainTabs.SETTINGS
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(84.dp)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CineWallLogo()
        Spacer(Modifier.width(24.dp))
        primaryTabs.forEachIndexed { index, (label, route) ->
            val interactionSource = remember { MutableInteractionSource() }
            val isFocused by interactionSource.collectIsFocusedAsState()
            val isSelected = selectedIndex == index
            Button (
                onClick = { onNavigate(route) },
                colors = ButtonDefaults.colors(
                    containerColor = if (isSelected) primary else if (isFocused)  primary.copy(alpha = 0.5f) else Color.Transparent ,
                    contentColor = secondary,
                    focusedContainerColor = primary.blend(Color.White, 0.18f),
                    focusedContentColor = secondary,
                    pressedContainerColor = primary.blend(Color.Black, 0.16f),
                    pressedContentColor = secondary
                ),
                glow = ButtonDefaults.glow(
                    focusedGlow = Glow(primary.copy(alpha = 0.5f), 16.dp)
                ),
                modifier = Modifier.focusable(interactionSource=interactionSource)
            ) {

                when (route) {
                    MainTabs.HOME -> Icon(
                        imageVector = Icons.Filled.Home,
                        contentDescription = null,
                        modifier = Modifier.size(26.dp)
                    )
                    MainTabs.LIBRARY -> Icon(
                        imageVector = Icons.Rounded.VideoLibrary,
                        contentDescription = null,
                        modifier = Modifier.size(26.dp)
                    )
                    MainTabs.SETTINGS -> Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = null,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Text(
                    label + " $isFocused",
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1
                )
            }
        }
        TabRow(
            selectedTabIndex = selectedIndex,
            modifier = Modifier.height(40.dp),
            contentColor = MaterialTheme.colorScheme.secondary,
//            containerColor = MaterialTheme.colorScheme.primary,
            indicator = { tabPositions, doesTabRowHaveFocus ->
                tabPositions.getOrNull(selectedIndex)?.let { position ->
                    TabRowDefaults.PillIndicator(
                        currentTabPosition = position,
                        doesTabRowHaveFocus = doesTabRowHaveFocus,
                        // 选中项始终强调，即使焦点在别的控件上也不会消失
                        activeColor = primary,
                        inactiveColor = primary,
                    )
                }
            }
        ) {
            primaryTabs.forEachIndexed { index, (label, route) ->
                val interactionSource = remember { MutableInteractionSource() }
                val isFocused by interactionSource.collectIsFocusedAsState()
                val isSelected = selectedIndex == index

                Tab(
                    selected = index == selectedIndex,
                    onClick = { onNavigate(route) },
                    onFocus = {},
                    colors = TabDefaults.pillIndicatorTabColors(
                        contentColor = secondary,
                        // 选中项仍用主色胶囊上的可读前景色，避免次要色和主色撞色看不清
                        selectedContentColor = secondary,
                        focusedContentColor = secondary,
                        focusedSelectedContentColor = secondary,
                        inactiveContentColor = secondary,
                    ),
                    modifier = Modifier
                        .padding(horizontal = 12.dp)
                        .tvTouchClick { onNavigate(route) }.focusable(interactionSource=interactionSource)
                ) {
                    Button( onClick = {}, modifier = Modifier,
                        colors = ButtonDefaults.colors(
                        contentColor = secondary,
                            containerColor = if (isFocused) primary.copy(alpha = 0.5f) else primary
//                        focusedContainerColor = containerColor.blend(Color.White, 0.18f),
//                        focusedContentColor = onColor(containerColor),
//                        pressedContainerColor = containerColor.blend(Color.Black, 0.16f),
//                        pressedContentColor = onColor(containerColor)
                    )) {
                        Text(
                            text = "$label $isFocused",
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        Spacer(Modifier.weight(1f))

        Button(
            onClick = { onNavigate(MainTabs.SETTINGS) },
            modifier = Modifier.tvTouchClick { onNavigate(MainTabs.SETTINGS) },
            shape = ButtonDefaults.shape(shape = RoundedCornerShape(8.dp)),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            colors = ButtonDefaults.colors(
                containerColor = if (settingsSelected) primary else Color.Transparent,
                contentColor = secondary,
                focusedContainerColor = if (settingsSelected) {
                    primary.blend(Color.White, 0.18f)
                } else {
                    primary.copy(alpha = 0.18f)
                },
                focusedContentColor = if (settingsSelected) {
                    onColor(primary)
                } else {
                    primary.blend(Color.White, 0.4f)
                },
                pressedContainerColor = primary.blend(Color.Black, 0.16f),
                pressedContentColor = onColor(primary)
            ),
            glow = ButtonDefaults.glow(
                focusedGlow = Glow(primary.copy(alpha = 0.3f), 12.dp)
            )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = null,
                    modifier = Modifier.size(26.dp)
                )
                Text(
                    text = "设置",
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1
                )
            }
        }
    }
}
