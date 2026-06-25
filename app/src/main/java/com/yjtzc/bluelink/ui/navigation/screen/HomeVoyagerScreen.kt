package com.yjtzc.bluelink.ui.navigation.screen

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import com.yjtzc.bluelink.ui.home.HomeScreen
import com.yjtzc.bluelink.ui.navigation.BlueLinkViewModelFactory
import com.yjtzc.bluelink.util.LocalAppContainer

class HomeVoyagerScreen : Tab {
    @Composable
    override fun Content() {
        val container = LocalAppContainer.current
        HomeScreen(
            viewModel = viewModel(factory = BlueLinkViewModelFactory(container)),
            isDarkMode = false,
            onSearch = { },
            onOpenDrawer = { },
            onToggleDarkMode = { },
            onOpenInspiration = { },
            modifier = Modifier.fillMaxSize()
        )
    }

    override val options: TabOptions
        @Composable get() = TabOptions(index = 0u, title = "灵感", icon = null)
}
