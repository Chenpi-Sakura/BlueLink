package com.yjtzc.bluelink.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yjtzc.bluelink.R
import com.yjtzc.bluelink.ui.chat.ChatScreen
import com.yjtzc.bluelink.ui.graph.GraphScreen
import com.yjtzc.bluelink.ui.home.HomeScreen
import com.yjtzc.bluelink.ui.mine.MineScreen
import com.yjtzc.bluelink.util.LocalAppContainer

/**
 * 底部 4 Tab 导航目的地
 */
enum class NavDest(val label: String, val icon: Int) {
    HOME("文库", R.drawable.ic_home),
    CHAT("对话", R.drawable.ic_favorite),     // TODO: 替换为对话图标
    GRAPH("图谱", R.drawable.ic_favorite),    // TODO: 替换为图谱图标
    MINE("我的", R.drawable.ic_account_box)
}

/**
 * App 主导航骨架 — NavigationSuiteScaffold + 4 Tab（V2.0 §3 / UI&UX §3）
 */
@Composable
fun BlueLinkNavGraph() {
    var currentDest by remember { mutableStateOf(NavDest.HOME) }
    val container = LocalAppContainer.current

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            NavDest.entries.forEach { dest ->
                item(
                    icon = {
                        Icon(
                            painterResource(dest.icon),
                            contentDescription = dest.label
                        )
                    },
                    label = { Text(dest.label) },
                    selected = currentDest == dest,
                    onClick = { currentDest = dest }
                )
            }
        }
    ) {
        Scaffold { innerPadding ->
            when (currentDest) {
                NavDest.HOME -> HomeScreen(
                    viewModel = viewModel(
                        factory = BlueLinkViewModelFactory(container)
                    ),
                    modifier = Modifier.padding(innerPadding)
                )
                NavDest.CHAT -> ChatScreen(
                    viewModel = viewModel(
                        factory = BlueLinkViewModelFactory(container)
                    ),
                    modifier = Modifier.padding(innerPadding)
                )
                NavDest.GRAPH -> GraphScreen(
                    viewModel = viewModel(
                        factory = BlueLinkViewModelFactory(container)
                    ),
                    modifier = Modifier.padding(innerPadding)
                )
                NavDest.MINE -> MineScreen(
                    viewModel = viewModel(
                        factory = BlueLinkViewModelFactory(container)
                    ),
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }
}
