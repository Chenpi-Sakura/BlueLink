package com.yjtzc.bluelink.ui.navigation.screen

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import com.yjtzc.bluelink.ui.chat.ChatScreen
import com.yjtzc.bluelink.ui.navigation.BlueLinkViewModelFactory
import com.yjtzc.bluelink.util.LocalAppContainer

class ChatVoyagerScreen : Tab {
    @Composable
    override fun Content() {
        val container = LocalAppContainer.current
        ChatScreen(
            viewModel = viewModel(factory = BlueLinkViewModelFactory(container)),
            onNavigateToReader = { },
            modifier = Modifier.fillMaxSize()
        )
    }

    override val options: TabOptions
        @Composable get() = TabOptions(index = 1u, title = "对话", icon = null)
}
