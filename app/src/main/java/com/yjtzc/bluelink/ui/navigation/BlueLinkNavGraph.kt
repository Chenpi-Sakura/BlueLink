package com.yjtzc.bluelink.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yjtzc.bluelink.R
import com.yjtzc.bluelink.ui.navigation.screen.ChatVoyagerScreen
import com.yjtzc.bluelink.ui.navigation.screen.GraphVoyagerScreen
import com.yjtzc.bluelink.ui.navigation.screen.HomeVoyagerScreen
import com.yjtzc.bluelink.ui.navigation.screen.MineVoyagerScreen
import com.yjtzc.bluelink.ui.theme.Ink400
import com.yjtzc.bluelink.ui.theme.KleinBlue
import cafe.adriel.voyager.navigator.tab.CurrentTab
import cafe.adriel.voyager.navigator.tab.TabNavigator

/**
 * App 主导航骨架
 *
 * 阶段 2：Voyager TabNavigator + 内嵌 Navigator
 * - Tab 切换由 TabNavigator 直接管理
 * - 二级页面 push/pop 由每个 Tab 内部 Navigator 管理
 * - 各 Tab 独立 back stack（切换回 Tab 时保留状态）
 * - 阶段 3：自定义 ScreenTransition 替换视觉
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlueLinkNavGraph() {
    val snackbarHostState = remember { SnackbarHostState() }

    Box(Modifier.fillMaxSize()) {
        TabNavigator(HomeVoyagerScreen()) { tabNavigator ->
            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) },
                bottomBar = {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                        tonalElevation = 0.dp,
                        modifier = Modifier
                            .navigationBarsPadding()
                            .height(64.dp)
                    ) {
                        val tabs = remember { listOf(
                            HomeVoyagerScreen(),
                            ChatVoyagerScreen(),
                            GraphVoyagerScreen(),
                            MineVoyagerScreen()
                        ) }
                        val tabIcons = remember { listOf(
                            R.drawable.ic_home,
                            R.drawable.ic_chat,
                            R.drawable.ic_graph,
                            R.drawable.ic_account_box
                        ) }
                        tabs.forEachIndexed { index, tab ->
                            val selected = tabNavigator.current == tab
                            NavigationBarItem(
                                selected = selected,
                                onClick = { tabNavigator.current = tab },
                                icon = {
                                    Icon(
                                        painterResource(tabIcons.getOrElse(index) { R.drawable.ic_home }),
                                        contentDescription = tab.options.title,
                                        modifier = Modifier.size(24.dp)
                                    )
                                },
                                label = {
                                    Text(
                                        tab.options.title,
                                        fontSize = 10.sp,
                                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = KleinBlue,
                                    selectedTextColor = KleinBlue,
                                    unselectedIconColor = Ink400,
                                    unselectedTextColor = Ink400,
                                    indicatorColor = KleinBlue.copy(alpha = 0.08f)
                                ),
                                alwaysShowLabel = true
                            )
                        }
                    }
                }
            ) { innerPadding ->
                Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                    CurrentTab()
                }
            }
        }
    }
}
