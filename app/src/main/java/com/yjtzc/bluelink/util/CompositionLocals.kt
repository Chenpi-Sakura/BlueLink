package com.yjtzc.bluelink.util

import androidx.compose.runtime.compositionLocalOf
import com.yjtzc.bluelink.AppContainer

/**
 * Compose 环境变量 — 在 Composable 树中传递 AppContainer。
 *
 * 用法：
 * ```
 * @Composable
 * fun MyScreen() {
 *     val container = LocalAppContainer.current
 *     val viewModel = remember { MyViewModel(container.someRepository) }
 * }
 * ```
 */
val LocalAppContainer = compositionLocalOf<AppContainer> {
    error("AppContainer not provided. Make sure to wrap your root Composable with CompositionLocalProvider.")
}
