package com.yjtzc.bluelink.ui.navigation.screen

import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.yjtzc.bluelink.ui.chat.ChatScreen
import com.yjtzc.bluelink.ui.navigation.BlueLinkViewModelFactory
import com.yjtzc.bluelink.util.LocalAppContainer
import kotlinx.coroutines.launch

/**
 * 对话 Tab 根页面
 *
 * 处理 ChatScreen 的异步路由：onNavigateToReader 内先异步查 documentRepository.getSegmentById
 * 拿到 docId 后才 push ReaderVoyagerScreen
 */
class ChatRootScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val container = LocalAppContainer.current
        val scope = rememberCoroutineScope()

        ChatScreen(
            viewModel = viewModel(factory = BlueLinkViewModelFactory(container)),
            onNavigateToReader = { segmentId ->
                scope.launch {
                    val segment = container.documentRepository.getSegmentById(segmentId)
                    if (segment != null) {
                        navigator.push(ReaderVoyagerScreen(segment.docId, segmentId))
                    }
                }
            }
        )
    }
}
