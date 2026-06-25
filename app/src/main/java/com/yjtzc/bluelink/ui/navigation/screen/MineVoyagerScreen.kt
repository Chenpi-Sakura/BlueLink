package com.yjtzc.bluelink.ui.navigation.screen

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions

class MineVoyagerScreen : Tab {
    @Composable
    override fun Content() {
        Navigator(MineMainScreen()) {
            val navigator = LocalNavigator.currentOrThrow
            navigator.lastItem.Content()
        }
    }

    override val options: TabOptions
        @Composable get() = TabOptions(index = 3u, title = "我的", icon = null)
}
