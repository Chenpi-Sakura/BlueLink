package com.yjtzc.bluelink.ui.navigation.screen

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.yjtzc.bluelink.ui.home.HomeScreen
import com.yjtzc.bluelink.ui.navigation.BlueLinkViewModelFactory
import com.yjtzc.bluelink.util.LocalAppContainer

/**
 * 灵感 Tab 根页面
 *
 * 内嵌 Navigator 的根 Screen，onOpenInspiration 用 navigator.push(EditorVoyagerScreen(cardId)) 替代原 editorCardId state 设置
 */
class HomeRootScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val container = LocalAppContainer.current
        HomeScreen(
            viewModel = viewModel(factory = BlueLinkViewModelFactory(container)),
            isDarkMode = false,
            onSearch = { },
            onOpenDrawer = { },
            onToggleDarkMode = { },
            onOpenInspiration = { cardId -> navigator.push(EditorVoyagerScreen(cardId)) },
            modifier = Modifier.fillMaxSize()
        )
    }
}
