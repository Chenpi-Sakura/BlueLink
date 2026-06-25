package com.yjtzc.bluelink.ui.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.graphics.Color
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
import androidx.compose.runtime.rememberCoroutineScope
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
 * 覆盖层 Nav3 容器 —— 替代旧版手写的 `OverlayLayer`（`movableContentOf` + `Animatable` + `graphicsLayer` 双 slot 状态机）。
 *
 * **架构**：
 * - 使用 Nav3 官方 [NavDisplay] + `rememberNavBackStack`，back stack 由父级 [BlueLinkNavGraph] 持有并 hoist
 * - 每个覆盖层目的地用 `entry<NavKey> { ... }` 注册（一个 key 对应一个 composable）
 * - 标准 Android push 动画在 NavDisplay 顶层 `transitionSpec` / `popTransitionSpec` / `predictivePopTransitionSpec` 配置
 *
 * **NoOverlay 占位 entry**：
 * back stack 始终至少包含 [OverlayNavKey.NoOverlay]，保证：
 * - push/pop 双向过渡都有动画源
 * - 进程死亡恢复时 back stack 总是合法状态
 * - 单一渲染路径，无需「if back stack 非空」分支
 *
 * **ViewModel 作用域**：
 * Nav3 把每个 entry 作为 `ViewModelStoreOwner`。`viewModel(factory = factory)` 默认 entry-scoped
 * （与旧版 activity-scoped 行为不同）。`MineViewModel` / `DataManagementViewModel` 之前被多个
 * 屏幕共享，现在每个 entry 一份新实例；这俩 VM 主要读 DataStore 偏好与 DB state，
 * 共享的数据本身在更底层，entry-scoped 没有功能影响。
 *
 * **InspirationEditor 预加载**：
 * 旧版 [BlueLinkNavGraph] 里的「cardsFlow + produceState + cachedCard + readCardContent + 150ms 防闪」
 * 逻辑被搬移进 `entry<EditorRoute>` 块内（与 key 生命周期对齐，结构更紧凑）。
 *
 * @param backStack 父级 hoist 的 [NavBackStack]<[NavKey]>（[BlueLinkNavGraph] 持有，供 tab 回调 push）
 * @param snackbarHostState 共享 SnackbarHostState（与底部 Scaffold 共用，参数传递而非 CompositionLocal）
 * @param container 依赖容器（用于 Editor 预加载 readCardContent）
 * @param factory 复用父级 hoist 的 [BlueLinkViewModelFactory]
 * @param onEmptyBack 栈只剩 [OverlayNavKey.NoOverlay] 时回调（系统返回自动 fallback 到 app minimize）
 */

/**
 * Scrim 黑色遮罩的目标 alpha 值（iOS-modal 风格）
 * 0.4 让上一级内容看起来像蒙了一层黑纱，聚焦到当前 entry
 */
private const val SCRIM_ALPHA = 0.4f

/**
 * Per-entry Scrim 黑色遮罩
 *
 * 放置在每个 overlay entry 自己 Box 的顶部（在 entry 内容之上）。
 * - 当只有 L1 打开：L1 的 scrim 覆盖在 L1 内容之上 → L1 内容被自己的 scrim 蒙黑
 * - 当 L2 打开时：L2 整个 entry（包括 L2 自己的 scrim）压上来，把 L1 整体盖住 → L1 的 scrim 也被盖住
 * - 下一级 entry 进来时，因为整个 entry 是「不透明整体」，本级 scrim 被自然遮住
 *
 * 动画：每次 entry 进入组合时 alpha 从 0 淡入到 SCRIM_ALPHA（300ms）
 */
@Composable
private fun OverlayScrim() {
    // 每次 entry 进入组合时 alpha 从 0 开始，淡入到 SCRIM_ALPHA
    // 用 animateFloatAsState + initialValue 保证淡入效果
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val alpha by animateFloatAsState(
        targetValue = if (visible) SCRIM_ALPHA else 0f,
        animationSpec = tween(300),
        label = "scrim"
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = alpha))
    )
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun OverlayNavGraph(
    backStack: NavBackStack<NavKey>,
    snackbarHostState: SnackbarHostState,
    container: AppContainer,
    factory: BlueLinkViewModelFactory,
    onEmptyBack: () -> Unit
) {
    // NavDisplay 容器覆盖全屏；NoOverlay entry 的 content 是空 Box（透明、不拦截事件）
    Box(Modifier.fillMaxSize().zIndex(1f)) {
        NavDisplay(
            backStack = backStack,
            onBack = {
                if (backStack.size > 1) backStack.removeLastOrNull()
                else onEmptyBack()
            },
            entryProvider = entryProvider {
                // ===== NoOverlay 占位 =====
                // 透明 Box —— 覆盖层为「无」时让底部 tab 透出来
                entry<OverlayNavKey.NoOverlay> { /* 空渲染 */ }

                // ===== Reader =====
                entry<OverlayNavKey.ReaderRoute> { key ->
                    val vm: ReaderViewModel = viewModel(factory = factory)
                    // Per-entry scrim: 在自己 Box 顶部绘制 scrim（覆盖上一级内容），自己内容渲染在 scrim 之下
                    // 下一级 entry 整体压上来时，会把这一级的 scrim 也盖住（正确层级）
                    Box(Modifier.fillMaxSize()) {
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
                        // Scrim：覆盖在内容之上。下一级 entry 进来时整体压上来，本级 scrim 被遮住
                        OverlayScrim()
                    }
                }

                // ===== Editor（含预加载逻辑）=====
                // 搬移自旧 BlueLinkNavGraph.kt 的 cardsFlow + produceState + cachedCard +
                // readCardContent + 150ms 防闪 + "card not found" snackbar 逻辑
                entry<OverlayNavKey.EditorRoute> { key ->
                    Box(Modifier.fillMaxSize()) {
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
                        OverlayScrim()
                    }
                }

                // ===== Mine 子页（6 个）=====
                entry<OverlayNavKey.Appearance> {
                    Box(Modifier.fillMaxSize()) {
                        val vm: AppearanceViewModel = viewModel(factory = factory)
                        AppearanceSettingsScreen(
                            viewModel = vm,
                            onBack = { backStack.removeLastOrNull() }
                        )
                        OverlayScrim()
                    }
                }
                entry<OverlayNavKey.CognitiveSettings> {
                    Box(Modifier.fillMaxSize()) {
                        val vm: MineViewModel = viewModel(factory = factory)
                        CognitiveSettingsScreen(
                            viewModel = vm,
                            onBack = { backStack.removeLastOrNull() }
                        )
                        OverlayScrim()
                    }
                }
                entry<OverlayNavKey.PrivacySecurity> {
                    Box(Modifier.fillMaxSize()) {
                        val vm: MineViewModel = viewModel(factory = factory)
                        // V2.2 统一 onNavigate 回调（3 个 onNavigateTo* 已收敛）
                        PrivacySecurityScreen(
                            viewModel = vm,
                            onBack = { backStack.removeLastOrNull() },
                            onNavigate = { key -> backStack.add(key) }
                        )
                        OverlayScrim()
                    }
                }
                entry<OverlayNavKey.PermissionManagement> {
                    Box(Modifier.fillMaxSize()) {
                        PermissionManagementScreen(onBack = { backStack.removeLastOrNull() })
                        OverlayScrim()
                    }
                }
                entry<OverlayNavKey.DataExport> {
                    Box(Modifier.fillMaxSize()) {
                        val vm: DataManagementViewModel = viewModel(factory = factory)
                        DataExportScreen(
                            viewModel = vm,
                            onBack = { backStack.removeLastOrNull() }
                        )
                        OverlayScrim()
                    }
                }
                entry<OverlayNavKey.PermanentDelete> {
                    Box(Modifier.fillMaxSize()) {
                        val vm: DataManagementViewModel = viewModel(factory = factory)
                        PermanentDeleteScreen(
                            viewModel = vm,
                            onBack = { backStack.removeLastOrNull() }
                        )
                        OverlayScrim()
                    }
                }
            },
            // ===== 统一动画：iOS-push 应用于所有 overlay 过渡（level 1+）=====
            // 层级关系（自动从 backStack.size 推算，NavDisplay 内部已是新状态）：
            // - backStack.size = 1：仅 NoOverlay（无 overlay）
            // - backStack.size = 2：Level 1（一级 overlay，如 Reader/Editor/PrivacySecurity）
            // - backStack.size = 3+：Level 2+（嵌套 overlay，如 PermissionManagement）
            // - 栈顶 entry 的 level = backStack.size - 1
            //
            // 动画规则：所有 overlay 过渡统一 iOS-push（用户决策：彻底视觉统一）
            // - push：新 overlay 从右滑入，旧 entry 驻留（KeepUntilTransitionsFinished）
            // - pop：旧 overlay 向右滑出，新 entry 驻留（EnterTransition.None）
            // - predictive pop：同 pop
            //
            // 扩展性：以后想区分 level 动画，只需在 if 分支里加规则（如 level 1 用 fade, level 2+ 用 slide）
            transitionSpec = {
                // iOS-push: 新 entry 从右滑入，旧 entry 驻留
                // 适用于所有 level 1+ overlay（level 1 = 一级，level 2+ = 嵌套，统一动画）
                slideInHorizontally(
                    initialOffsetX = { it },
                    animationSpec = tween(300)
                ) togetherWith ExitTransition.KeepUntilTransitionsFinished
            },
            popTransitionSpec = {
                // iOS-push pop: 旧 entry 向右滑出，新 entry 驻留
                // 适用于所有 level 1+ overlay pop
                EnterTransition.None togetherWith slideOutHorizontally(
                    targetOffsetX = { it },
                    animationSpec = tween(300)
                )
            },
            predictivePopTransitionSpec = {
                // predictive pop 同 pop
                EnterTransition.None togetherWith slideOutHorizontally(
                    targetOffsetX = { it },
                    animationSpec = tween(300)
                )
            }
        )
    }
}
