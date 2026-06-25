package com.yjtzc.bluelink.ui.navigation.screen

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import com.yjtzc.bluelink.ui.graph.GraphScreen
import com.yjtzc.bluelink.ui.navigation.BlueLinkViewModelFactory
import com.yjtzc.bluelink.util.LocalAppContainer

class GraphVoyagerScreen : Tab {
    @Composable
    override fun Content() {
        val container = LocalAppContainer.current
        GraphScreen(
            viewModel = viewModel(factory = BlueLinkViewModelFactory(container)),
            modifier = Modifier.fillMaxSize()
        )
    }

    override val options: TabOptions
        @Composable get() = TabOptions(index = 2u, title = "图谱", icon = null)
}
