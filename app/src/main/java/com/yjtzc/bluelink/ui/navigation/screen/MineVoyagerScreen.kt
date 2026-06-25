package com.yjtzc.bluelink.ui.navigation.screen

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import com.yjtzc.bluelink.ui.mine.MineScreen
import com.yjtzc.bluelink.ui.navigation.BlueLinkViewModelFactory
import com.yjtzc.bluelink.util.LocalAppContainer

class MineVoyagerScreen : Tab {
    @Composable
    override fun Content() {
        val container = LocalAppContainer.current
        MineScreen(
            viewModel = viewModel(factory = BlueLinkViewModelFactory(container)),
            onNavigateToAppearance = { },
            onNavigateToCognitive = { },
            onNavigateToPrivacySecurity = { },
            onNavigateToPermission = { },
            onNavigateToDataExport = { },
            onNavigateToPermanentDelete = { },
            modifier = Modifier.fillMaxSize()
        )
    }

    override val options: TabOptions
        @Composable get() = TabOptions(index = 3u, title = "我的", icon = null)
}
