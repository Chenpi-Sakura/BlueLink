package com.yjtzc.bluelink.ui.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.yjtzc.bluelink.AppContainer
import com.yjtzc.bluelink.data.local.db.InspirationCardEntity
import com.yjtzc.bluelink.ui.editor.InspirationEditorScreen
import com.yjtzc.bluelink.ui.mine.AppearanceSettingsScreen
import com.yjtzc.bluelink.ui.mine.AppearanceViewModel
import com.yjtzc.bluelink.ui.mine.CognitiveSettingsScreen
import com.yjtzc.bluelink.ui.mine.DataExportScreen
import com.yjtzc.bluelink.ui.mine.DataManagementViewModel
import com.yjtzc.bluelink.ui.mine.MineViewModel
import com.yjtzc.bluelink.ui.mine.PermanentDeleteScreen
import com.yjtzc.bluelink.ui.mine.PermissionManagementScreen
import com.yjtzc.bluelink.ui.mine.PrivacySecurityScreen
import com.yjtzc.bluelink.ui.reader.ReaderScreen
import com.yjtzc.bluelink.ui.reader.ReaderViewModel
import kotlinx.coroutines.delay

/**
 * 覆盖层 Nav3 容器
 *
 * **架构**：
 * - 使用 Nav3 [NavDisplay] + `rememberNavBackStack`，back stack 由父级 [BlueLinkNavGraph] 持有并 hoist
 * - 每个覆盖层目的地用 `entry<NavKey> { ... }` 注册（一个 key 对应一个 composable）
 * - 过渡动画在 NavDisplay 顶层 `transitionSpec` / `popTransitionSpec` / `predictivePopTransitionSpec` 配置
 *
 * **NoOverlay 占位 entry**：
 * back stack 始终至少包含 [OverlayNavKey.NoOverlay]，保证：
 * - push/pop 双向过渡都有动画源
 * - 进程死亡恢复时 back stack 总是合法状态
 * - 单一渲染路径，无需「if back stack 非空」分支
 *
 * **ViewModel 作用域**：
 * Nav3 把每个 entry 作为 `ViewModelStoreOwner`。`viewModel(factory = factory)` 默认 entry-scoped
 * （每次 entry 进入都拿到同一 VM 实例，pop 后清除）。`MineViewModel` / `DataManagementViewModel`
 * 共享数据（DataStore 偏好 / DB state）在更底层，entry-scoped 没有功能影响。
 *
 * **InspirationEditor 预加载**：
 * `EditorRoute` entry 内部串接 cards Flow → 命中卡片 → 预加载完整内容 → 150ms debounce
 * Spinner（避免 FOUC）→ 否则 'card not found' 提示。整个 pipeline 与 entry 生命周期对齐。
 *
 * @param backStack 父级 hoist 的 [NavBackStack]<[NavKey]>（[BlueLinkNavGraph] 持有，供 tab 回调 push）
 * @param snackbarHostState 共享 SnackbarHostState（与底部 Scaffold 共用，参数传递而非 CompositionLocal）
 * @param container 依赖容器（用于 Editor 预加载 readCardContent）
 * @param factory 复用父级 hoist 的 [BlueLinkViewModelFactory]
 * @param onEmptyBack 栈只剩 [OverlayNavKey.NoOverlay] 时回调（系统返回自动 fallback 到 app minimize）
 */

/**
 * Overlay 边缘阴影的 elevation 值（iOS-push drop shadow）
 *
 * iOS-push 没有全屏半透明黑色遮罩——通过 `OVERLAY_SHADOW_ELEVATION` 在新页面左侧
 * 边缘绘制 drop shadow 体现 Z 轴层次（结合 `KeepUntilTransitionsFinished` 让旧 entry
 * 驻留）。elevation 越大阴影扩散范围越广，越接近 iOS-push 真实观感。
 */
private val OVERLAY_SHADOW_ELEVATION = 24.dp

/** 覆盖层过渡动画时长（push / pop / predictive pop 共用，保证视觉统一） */
private val OVERLAY_ANIM_DURATION: FiniteAnimationSpec<IntOffset> = tween(durationMillis = 300)

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun OverlayNavGraph(
    backStack: NavBackStack<NavKey>,
    snackbarHostState: SnackbarHostState,
    container: AppContainer,
    factory: BlueLinkViewModelFactory,
    onEmptyBack: () -> Unit
) {
    // NavDisplay 容器覆盖全屏并设 zIndex 1f，保证覆盖在底部 Scaffold（含 NavigationBar）之上
    Box(Modifier.fillMaxSize().zIndex(1f)) {
        NavDisplay(
            backStack = backStack,
            onBack = {
                if (backStack.size > 1) backStack.removeLastOrNull()
                else onEmptyBack()
            },
            entryProvider = entryProvider {
                // ===== NoOverlay 占位（无 composable，覆盖层为「无」时让底部 tab 透出来） =====
                entry<OverlayNavKey.NoOverlay> { }

                // ===== Reader =====
                entry<OverlayNavKey.ReaderRoute> { key ->
                    val vm: ReaderViewModel = viewModel(factory = factory)
                    // iOS-push: 新页面自身不透明，左侧边缘 drop shadow 体现 Z 轴层次
                    Box(
                        Modifier
                            .fillMaxSize()
                            .shadow(elevation = OVERLAY_SHADOW_ELEVATION, clip = false)
                    ) {
                        Scaffold(
                            topBar = {
                                TopAppBar(
                                    title = { Text("阅读") },
                                    navigationIcon = {
                                        IconButton(onClick = { backStack.removeLastOrNull() }) {
                                            Icon(
                                                Icons.AutoMirrored.Filled.ArrowBack,
                                                contentDescription = "返回"
                                            )
                                        }
                                    },
                                    colors = TopAppBarDefaults.topAppBarColors(
                                        containerColor = MaterialTheme.colorScheme.background
                                    )
                                )
                            },
                            snackbarHost = { SnackbarHost(snackbarHostState) }
                        ) { innerPadding ->
                            ReaderScreen(
                                viewModel = vm,
                                docId = key.docId,
                                spotlightSegmentId = key.spotlightSegmentId,
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                    }
                }

                // ===== Editor（含预加载逻辑）=====
                entry<OverlayNavKey.EditorRoute> { key ->
                    Box(
                        Modifier
                            .fillMaxSize()
                            .shadow(elevation = OVERLAY_SHADOW_ELEVATION, clip = false)
                    ) {
                        val cardsFlow = remember { container.captureRepository.observeAllCards() }
                        val cardsState = produceState<List<InspirationCardEntity>?>(
                            initialValue = null,
                            cardsFlow
                        ) {
                            cardsFlow.collect { value = it }
                        }
                        val cards = cardsState.value
                        val card = cards?.find { it.id == key.cardId }

                        // 缓存最后找到的 card，避免 Flow 中间态 emit 空列表时误判卡片不存在
                        val cachedCard = remember(key.cardId) {
                            mutableStateOf<InspirationCardEntity?>(null)
                        }
                        if (card != null) cachedCard.value = card
                        val displayCard = card ?: cachedCard.value

                        if (displayCard != null) {
                            // 预加载完整内容（避免 blocks 数 1→N 跳变）
                            val fullContentState = produceState<String?>(
                                initialValue = null,
                                displayCard.id
                            ) {
                                value = runCatching {
                                    container.captureRepository.readCardContent(displayCard)
                                }.getOrNull()
                            }
                            val fullContent = fullContentState.value

                            if (fullContent != null) {
                                InspirationEditorScreen(
                                    card = displayCard,
                                    preloadedContent = fullContent,
                                    captureRepository = container.captureRepository,
                                    onBack = { backStack.removeLastOrNull() }
                                )
                            } else {
                                // 完整内容加载中：debounce 延迟显示 Spinner（避免 100ms 一闪而过的 FOUC）
                                var showLoading by remember { mutableStateOf(false) }
                                LaunchedEffect(Unit) {
                                    delay(150)
                                    showLoading = true
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(MaterialTheme.colorScheme.background),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (showLoading) CircularProgressIndicator()
                                }
                            }
                        } else if (cards == null) {
                            // Flow 还没 emit（正在查询数据库），loading 占位（必须有 opaque background 避免透出 HOME）
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.background)
                            )
                        } else {
                            // Flow 已 emit 且卡片确实不存在
                            LaunchedEffect(key.cardId) {
                                snackbarHostState.showSnackbar("灵感不存在或已被删除")
                                backStack.removeLastOrNull()
                            }
                        }
                    }
                }

                // ===== Mine 子页（6 个）=====
                entry<OverlayNavKey.Appearance> {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .shadow(elevation = OVERLAY_SHADOW_ELEVATION, clip = false)
                    ) {
                        val vm: AppearanceViewModel = viewModel(factory = factory)
                        AppearanceSettingsScreen(
                            viewModel = vm,
                            onBack = { backStack.removeLastOrNull() }
                        )
                    }
                }
                entry<OverlayNavKey.CognitiveSettings> {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .shadow(elevation = OVERLAY_SHADOW_ELEVATION, clip = false)
                    ) {
                        val vm: MineViewModel = viewModel(factory = factory)
                        CognitiveSettingsScreen(
                            viewModel = vm,
                            onBack = { backStack.removeLastOrNull() }
                        )
                    }
                }
                entry<OverlayNavKey.PrivacySecurity> {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .shadow(elevation = OVERLAY_SHADOW_ELEVATION, clip = false)
                    ) {
                        val vm: MineViewModel = viewModel(factory = factory)
                        PrivacySecurityScreen(
                            viewModel = vm,
                            onBack = { backStack.removeLastOrNull() },
                            onNavigate = { key -> backStack.add(key) }
                        )
                    }
                }
                entry<OverlayNavKey.PermissionManagement> {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .shadow(elevation = OVERLAY_SHADOW_ELEVATION, clip = false)
                    ) {
                        PermissionManagementScreen(onBack = { backStack.removeLastOrNull() })
                    }
                }
                entry<OverlayNavKey.DataExport> {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .shadow(elevation = OVERLAY_SHADOW_ELEVATION, clip = false)
                    ) {
                        val vm: DataManagementViewModel = viewModel(factory = factory)
                        DataExportScreen(
                            viewModel = vm,
                            onBack = { backStack.removeLastOrNull() }
                        )
                    }
                }
                entry<OverlayNavKey.PermanentDelete> {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .shadow(elevation = OVERLAY_SHADOW_ELEVATION, clip = false)
                    ) {
                        val vm: DataManagementViewModel = viewModel(factory = factory)
                        PermanentDeleteScreen(
                            viewModel = vm,
                            onBack = { backStack.removeLastOrNull() }
                        )
                    }
                }
            },
            // ===== 统一动画：所有 overlay 过渡使用 iOS-push（用户决策：彻底视觉统一）=====
            // - push：新 overlay 从右滑入，旧 entry 驻留（KeepUntilTransitionsFinished 避免 unmount 丢状态）
            // - pop：旧 overlay 向右滑出，新 entry 驻留（EnterTransition.None）
            // - predictive pop：与 pop 同语义，时长统一用 OVERLAY_ANIM_DURATION
            transitionSpec = {
                slideInHorizontally(
                    initialOffsetX = { it },
                    animationSpec = OVERLAY_ANIM_DURATION
                ) togetherWith ExitTransition.KeepUntilTransitionsFinished
            },
            popTransitionSpec = {
                EnterTransition.None togetherWith slideOutHorizontally(
                    targetOffsetX = { it },
                    animationSpec = OVERLAY_ANIM_DURATION
                )
            },
            predictivePopTransitionSpec = {
                EnterTransition.None togetherWith slideOutHorizontally(
                    targetOffsetX = { fullWidth -> fullWidth },
                    animationSpec = OVERLAY_ANIM_DURATION
                )
            }
        )
    }
}
