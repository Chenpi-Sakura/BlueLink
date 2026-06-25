package com.yjtzc.bluelink.ui.navigation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.yjtzc.bluelink.data.local.db.InspirationCardEntity
import com.yjtzc.bluelink.ui.editor.InspirationEditorScreen
import com.yjtzc.bluelink.ui.navigation.BlueLinkViewModelFactory
import com.yjtzc.bluelink.util.LocalAppContainer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 灵感编辑器二级页面（替换原 Overlay.Editor 分支）
 *
 * 保留原 BlueLinkNavGraph Overlay.Editor 分支的所有逻辑：
 * - cardsFlow + cachedCard 防止空 Flow 闪动
 * - produceState 预加载 fullContent（readCardContent 异步 IO）
 * - debounce 150ms 延迟显示 spinner
 * - 卡片不存在时 snackbar + pop
 */
data class EditorVoyagerScreen(val cardId: String) : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val container = LocalAppContainer.current

        // cardsFlow + cachedCard 防止空 Flow 闪动（复制自原 Overlay.Editor 分支）
        val cardsFlow = remember { container.captureRepository.observeAllCards() }
        val cardsState = produceState<List<InspirationCardEntity>?>(initialValue = null, cardsFlow) {
            cardsFlow.collect { value = it }
        }
        val cards = cardsState.value
        val card = cards?.find { it.id == cardId }
        val cachedCard = remember(cardId) { mutableStateOf<InspirationCardEntity?>(null) }
        if (card != null) cachedCard.value = card
        val displayCard = card ?: cachedCard.value

        if (displayCard != null) {
            // 预加载完整内容（JSON → 含图片块）
            val fullContentState = produceState<String?>(initialValue = null, displayCard.id) {
                value = runCatching { container.captureRepository.readCardContent(displayCard) }.getOrNull()
            }
            val fullContent = fullContentState.value

            if (fullContent != null) {
                InspirationEditorScreen(
                    card = displayCard,
                    preloadedContent = fullContent,
                    captureRepository = container.captureRepository,
                    onBack = { navigator.pop() }
                )
            } else {
                // 完整内容加载中：debounce 150ms 显示 spinner
                var showLoading by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) {
                    delay(150)
                    showLoading = true
                }
                Box(
                    modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
                    contentAlignment = Alignment.Center
                ) {
                    if (showLoading) CircularProgressIndicator()
                }
            }
        } else if (cards == null) {
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
        } else {
            // 卡片不存在 — 用 try-catch 包裹 navigator.pop()，避免 Voyager 在 transition 期间抛异常
            LaunchedEffect(cardId) {
                try {
                    navigator.pop()
                } catch (_: Exception) {
                    // Voyager pop 可能因 transition 未完成或栈状态不一致而抛异常，静默忽略
                }
            }
        }
    }
}
