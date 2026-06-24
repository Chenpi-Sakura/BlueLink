package com.yjtzc.bluelink.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yjtzc.bluelink.R
import com.yjtzc.bluelink.data.local.db.InspirationCardEntity
import com.yjtzc.bluelink.ui.bookshelf.BookshelfScreen
import com.yjtzc.bluelink.ui.chat.ChatScreen
import com.yjtzc.bluelink.ui.graph.GraphScreen
import com.yjtzc.bluelink.ui.home.HomeScreen
import com.yjtzc.bluelink.ui.mine.AppearanceSettingsScreen
import com.yjtzc.bluelink.ui.mine.CognitiveSettingsScreen
import com.yjtzc.bluelink.ui.mine.MineScreen
import com.yjtzc.bluelink.ui.mine.MineViewModel
import com.yjtzc.bluelink.ui.mine.DataExportScreen
import com.yjtzc.bluelink.ui.mine.PermanentDeleteScreen
import com.yjtzc.bluelink.ui.mine.PermissionManagementScreen
import com.yjtzc.bluelink.ui.mine.PrivacySecurityScreen
import com.yjtzc.bluelink.ui.mine.components.MineNavScaffold
import com.yjtzc.bluelink.ui.reader.ReaderScreen
import com.yjtzc.bluelink.ui.reader.ReaderViewModel
import com.yjtzc.bluelink.ui.theme.Ink400
import com.yjtzc.bluelink.ui.theme.Ink600
import com.yjtzc.bluelink.ui.theme.Ink900
import com.yjtzc.bluelink.ui.theme.KleinBlue
import com.yjtzc.bluelink.ui.theme.Parchment100
import com.yjtzc.bluelink.ui.theme.Parchment50
import com.yjtzc.bluelink.domain.model.toDomain
import com.yjtzc.bluelink.ui.editor.InspirationEditorScreen
import com.yjtzc.bluelink.util.LocalAppContainer
import kotlinx.coroutines.launch

/**
 * 底部 4 Tab 导航目的地
 */
enum class NavDest(val label: String, val icon: Int) {
    HOME("灵感", R.drawable.ic_home),
    CHAT("对话", R.drawable.ic_chat),
    GRAPH("图谱", R.drawable.ic_graph),
    MINE("我的", R.drawable.ic_account_box)
}

/**
 * 阅读器导航参数
 */
data class ReaderParams(
    val docId: String,
    val spotlightSegmentId: String? = null
)

/**
 * 我的模块子页面路由
 */
sealed interface MineRoute {
    data object Appearance : MineRoute
    data object CognitiveSettings : MineRoute
    data object PrivacySecurity : MineRoute
    data object PermissionManagement : MineRoute
    data object DataExport : MineRoute
    data object PermanentDelete : MineRoute
}

/**
 * 覆盖层状态标识（作为 AnimatedContent 的 targetState）
 *
 * 必须用 sealed class 而不是 String，原因：
 * 1. 每个具体 mine route 都是独立的 Overlay.Mine 实例（data class 按字段 equals），
 *    这样切换子页（如 PrivacySecurity → PermissionManagement）时 targetState 不相等，
 *    AnimatedContent 会触发 transition；用 String "mine" 时 targetState 一直相等，嵌套切换无动画。
 * 2. type-safe 的 `is Overlay.X` 检查，避免字符串拼写错误。
 */
sealed interface Overlay {
    data object Reader : Overlay
    data class Editor(val cardId: String) : Overlay
    data class Mine(val route: MineRoute) : Overlay
}

/**
 * 覆盖层切换方向
 *
 * - Forward：嵌套变深（首次进入、隐私→权限）。新 overlay 从右滑入覆盖，旧 overlay 保持原位 + 加 scrim 遮罩（区分层级）。
 * - Backward：嵌套变浅（权限→返回隐私）。旧 overlay 从右滑出 + 淡出 + scrim 渐变回透明，新 overlay 保持原位不动。
 * - Exit：退出覆盖层（任一 overlay → null）。旧 overlay 从右滑出 + 淡出。
 */
private enum class TransitionDirection { Forward, Backward, Exit }

/**
 * 旧 overlay 在 Forward 时叠加的 scrim（半透明黑色遮罩）的目标 alpha 值。
 * 0.4 让旧页面看起来像蒙了一层黑纱，新页面（覆盖在上层）视觉上"在前面"。
 */
private const val SCRIM_ALPHA = 0.4f

/**
 * App 主导航骨架
 *
 * - 底部 4 Tab：灵感 / 对话 / 图谱 / 我的
 * - 覆盖层（阅读器、编辑器、我的子页）使用 AnimatedContent 实现右滑动画
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlueLinkNavGraph() {
    var currentDest by remember { mutableStateOf(NavDest.HOME) }
    var readerParams by remember { mutableStateOf<ReaderParams?>(null) }
    var editorCardId by remember { mutableStateOf<String?>(null) }

    // 我的模块子页面导航
    var mineRoute by remember { mutableStateOf<MineRoute?>(null) }
    var mineFromRoute by remember { mutableStateOf<MineRoute?>(null) }

    val container = LocalAppContainer.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // 返回处理
    BackHandler(enabled = mineRoute != null || editorCardId != null || readerParams != null) {
        when {
            mineRoute != null -> {
                if (mineFromRoute != null) {
                    mineRoute = mineFromRoute
                    mineFromRoute = null
                } else {
                    mineRoute = null
                }
            }
            editorCardId != null -> editorCardId = null
            readerParams != null -> readerParams = null
        }
    }

    // 当前覆盖层状态（用 sealed class，让嵌套切换能触发 AnimatedContent transition）
    val overlay: Overlay? = when {
        readerParams != null -> Overlay.Reader
        editorCardId != null -> Overlay.Editor(cardId = editorCardId!!)
        mineRoute != null -> Overlay.Mine(route = mineRoute!!)
        else -> null
    }

    Box(Modifier.fillMaxSize()) {
        // ====== 底部 Tab 导航（始终组合） ======
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    tonalElevation = 0.dp,
                    modifier = Modifier
                        .navigationBarsPadding()
                        .height(64.dp)
                ) {
                    NavDest.entries.forEach { dest ->
                        val selected = currentDest == dest
                        NavigationBarItem(
                            selected = selected,
                            onClick = { currentDest = dest },
                            icon = {
                                Icon(
                                    painterResource(dest.icon),
                                    contentDescription = dest.label,
                                    modifier = Modifier.size(24.dp)
                                )
                            },
                            label = {
                                Text(
                                    dest.label,
                                    fontSize = 10.sp,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = KleinBlue,
                                selectedTextColor = KleinBlue,
                                unselectedIconColor = Ink400,
                                unselectedTextColor = Ink400,
                                indicatorColor = KleinBlue.copy(alpha = 0.08f)
                            ),
                            alwaysShowLabel = true
                        )
                    }
                }
            }
        ) { innerPadding ->
            when (currentDest) {
                NavDest.HOME -> HomeScreen(
                    viewModel = viewModel(factory = BlueLinkViewModelFactory(container)),
                    onOpenInspiration = { cardId -> editorCardId = cardId },
                    modifier = Modifier.padding(innerPadding)
                )
                NavDest.CHAT -> ChatScreen(
                    viewModel = viewModel(factory = BlueLinkViewModelFactory(container)),
                    onNavigateToReader = { segmentId ->
                        scope.launch {
                            val segment = container.documentRepository.getSegmentById(segmentId)
                            if (segment != null) {
                                readerParams = ReaderParams(
                                    docId = segment.docId,
                                    spotlightSegmentId = segmentId
                                )
                            } else {
                                snackbarHostState.showSnackbar(message = "未找到该锚点对应的文档片段")
                            }
                        }
                    },
                    modifier = Modifier.padding(innerPadding)
                )
                NavDest.GRAPH -> GraphScreen(
                    viewModel = viewModel(factory = BlueLinkViewModelFactory(container)),
                    modifier = Modifier.padding(innerPadding)
                )
                NavDest.MINE -> {
                    val mineViewModel: MineViewModel = viewModel(factory = BlueLinkViewModelFactory(container))
                    MineScreen(
                        viewModel = mineViewModel,
                        onNavigateToAppearance = { mineFromRoute = null; mineRoute = MineRoute.Appearance },
                        onNavigateToCognitive = { mineFromRoute = null; mineRoute = MineRoute.CognitiveSettings },
                        onNavigateToPrivacySecurity = { mineFromRoute = null; mineRoute = MineRoute.PrivacySecurity },
                        onNavigateToPermission = { mineFromRoute = null; mineRoute = MineRoute.PermissionManagement },
                        onNavigateToDataExport = { mineFromRoute = null; mineRoute = MineRoute.DataExport },
                        onNavigateToPermanentDelete = { mineFromRoute = null; mineRoute = MineRoute.PermanentDelete },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }

        // ====== 覆盖层动画 ======
        // 计算 transition direction（基于 previousOverlay + 当前 overlay + mineFromRoute）
        // - Exit：overlay 变 null（旧 overlay 从右滑出 + 淡出）
        // - Forward：previousOverlay = null 或 mineFromRoute != null（嵌套变深；新 overlay 从右滑入覆盖，旧 overlay 保持原位）
        // - Backward：previousOverlay != null 且 mineFromRoute == null（嵌套变浅；旧 overlay 从右滑出 + 淡出，新 overlay 保持原位）
        var previousOverlay by remember { mutableStateOf<Overlay?>(null) }
        val direction: TransitionDirection? = remember(overlay, mineFromRoute) {
            when {
                overlay == previousOverlay -> null
                overlay == null -> TransitionDirection.Exit
                previousOverlay == null -> TransitionDirection.Forward
                mineFromRoute != null -> TransitionDirection.Forward
                else -> TransitionDirection.Backward
            }
        }

        OverlayLayer(
            targetOverlay = overlay,
            direction = direction,
            onTransitionEnd = { previousOverlay = overlay },
            modifier = Modifier.fillMaxSize()
        ) { currentOverlay ->
            when (currentOverlay) {
                is Overlay.Reader -> {
                    // currentOverlay == Overlay.Reader 意味着 readerParams != null，可以安全 !!
                    val params = readerParams!!
                    val readerViewModel: ReaderViewModel = viewModel(factory = BlueLinkViewModelFactory(container))
                    Scaffold(
                        topBar = {
                            TopAppBar(
                                title = { Text("阅读") },
                                navigationIcon = {
                                    IconButton(onClick = { readerParams = null }) {
                                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
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
                            viewModel = readerViewModel,
                            docId = params.docId,
                            spotlightSegmentId = params.spotlightSegmentId,
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                }

                is Overlay.Editor -> {
                    val cardId = currentOverlay.cardId
                    // BackHandler 在 NavGraph 顶层统一处理（启用条件已包含 editorCardId != null）

                    // 用 produceState + nullable initialValue 区分"Flow 还没 emit"和"Flow 已 emit 但卡片不存在"：
                    // collectAsStateWithLifecycle 的 initialValue 必须是 non-null，无法表达"加载中"状态，
                    // 所以这里用 produceState 把 cards 包成 nullable（null = 加载中，non-null = 已加载）。
                    val cardsFlow = remember { container.captureRepository.observeAllCards() }
                    val cardsState = produceState<List<InspirationCardEntity>?>(initialValue = null, cardsFlow) {
                        cardsFlow.collect { value = it }
                    }
                    val cards = cardsState.value
                    val card = cards?.find { it.id == cardId }

                    // 缓存最后找到的 card，避免 cards Flow 中间短暂 emit 空列表时
                    // card = null → 进 else 分支 → LaunchedEffect 触发 editorCardId = null → 关闭 overlay → 闪回主界面
                    val cachedCard = remember(cardId) { mutableStateOf<InspirationCardEntity?>(null) }
                    if (card != null) {
                        cachedCard.value = card
                    }
                    val displayCard = card ?: cachedCard.value

                    if (displayCard != null) {
                        // 预加载完整内容（避免 blocks 数从 1→N 导致 UI 闪动 + 图片文件丢失）
                        // contentSnippet 只是前 30 字摘要，readCardContent 才是完整 JSON（含图片/语音引用）。
                        // 在 OverlayLayer 内预先 await readCardContent，让 InspirationEditorScreen 首次渲染就用完整 blocks。
                        val fullContentState = produceState<String?>(initialValue = null, displayCard.id) {
                            value = runCatching { container.captureRepository.readCardContent(displayCard) }.getOrNull()
                        }
                        val fullContent = fullContentState.value

                        if (fullContent != null) {
                            InspirationEditorScreen(
                                card = displayCard,
                                preloadedContent = fullContent,
                                captureRepository = container.captureRepository,
                                onBack = { editorCardId = null }
                            )
                        } else {
                            // 完整内容加载中，loading 占位（必须有 opaque background，否则透明 Box 会让用户透过看到底层的 HOME）
                            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
                        }
                    } else if (cards == null) {
                        // Flow 还没 emit（正在查询数据库），loading 占位（必须有 opaque background 避免透过看到 HOME）
                        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
                    } else {
                        // Flow 已 emit 且卡片确实不存在（首次进入且缓存也没有）
                        // 用 cardId 作 key，切换到不同 card 时 LaunchedEffect 重新启动
                        LaunchedEffect(cardId) {
                            snackbarHostState.showSnackbar("灵感不存在或已被删除")
                            editorCardId = null
                        }
                    }
                }

                is Overlay.Mine -> {
                    val route = currentOverlay.route
                    // remember(mineFromRoute)：mineFromRoute 不变时 lambda 引用稳定，
                    // 子页面 onBack 参数不变，避免触发不必要的重组
                    val onGoBack = remember(mineFromRoute) {
                        {
                            if (mineFromRoute != null) {
                                mineRoute = mineFromRoute
                                mineFromRoute = null
                            } else {
                                mineRoute = null
                            }
                        }
                    }
                    val mineViewModel: MineViewModel = viewModel(factory = BlueLinkViewModelFactory(container))

                    when (route) {
                        is MineRoute.Appearance -> AppearanceSettingsScreen(
                            viewModel = viewModel(factory = BlueLinkViewModelFactory(container)),
                            onBack = onGoBack
                        )
                        is MineRoute.CognitiveSettings -> CognitiveSettingsScreen(
                            viewModel = viewModel(factory = BlueLinkViewModelFactory(container)),
                            onBack = onGoBack
                        )
                        is MineRoute.PrivacySecurity -> PrivacySecurityScreen(
                            viewModel = viewModel(factory = BlueLinkViewModelFactory(container)),
                            onBack = onGoBack,
                            onNavigateToPermission = { mineFromRoute = MineRoute.PrivacySecurity; mineRoute = MineRoute.PermissionManagement },
                            onNavigateToDataExport = { mineFromRoute = MineRoute.PrivacySecurity; mineRoute = MineRoute.DataExport },
                            onNavigateToPermanentDelete = { mineFromRoute = MineRoute.PrivacySecurity; mineRoute = MineRoute.PermanentDelete }
                        )
                        is MineRoute.PermissionManagement -> PermissionManagementScreen(onBack = onGoBack)
                        is MineRoute.DataExport -> DataExportScreen(
                            viewModel = viewModel(factory = BlueLinkViewModelFactory(container)),
                            onBack = onGoBack
                        )
                        is MineRoute.PermanentDelete -> PermanentDeleteScreen(
                            viewModel = viewModel(factory = BlueLinkViewModelFactory(container)),
                            onBack = onGoBack
                        )
                    }
                }
            }
        }
    }
}

/**
 * 覆盖层容器 - 手动管理 overlay 切换动画
 *
 * 为什么不用 AnimatedContent：
 * AnimatedContent 的 EnterTransition.None / ExitTransition.None 在新版 Compose 中
 * 会让 content 立即出现/消失（不等 enter 动画完成），无法实现 iOS push 双向对称
 * （旧页保持原位被新页覆盖 + 新页保持原位露出）。
 *
 * 这里用手工方式同时渲染两个 overlay slot（z-order 上下两层），各自用 graphicsLayer
 * 控制 alpha 和 translationX，独立应用 enter/exit 动画，完美实现 iOS push 双向对称。
 *
 * @param targetOverlay 即将进入的目标 overlay（null 表示退出覆盖层）
 * @param direction 当前 transition 的方向（null 表示无 transition）
 * @param onTransitionEnd transition 结束后回调（用于父级更新 previousOverlay 等状态）
 * @param content 渲染给定 overlay 的 composable
 */
@Composable
private fun OverlayLayer(
    targetOverlay: Overlay?,
    direction: TransitionDirection?,
    onTransitionEnd: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (Overlay) -> Unit
) {
    val screenWidthPx = with(LocalDensity.current) {
        LocalConfiguration.current.screenWidthDp.dp.toPx()
    }

    // displayedOverlay = 当前显示的旧 overlay（z-order 下方，exit 动画对象）
    // pendingOverlay = 等待显示的新 overlay（z-order 上方，enter 动画对象）
    var displayedOverlay by remember { mutableStateOf<Overlay?>(null) }
    var pendingOverlay by remember { mutableStateOf<Overlay?>(null) }
    var activeDirection by remember { mutableStateOf<TransitionDirection?>(null) }

    // scrim 半透明黑色遮罩 alpha（统一在 OverlayLayer 顶层管理，首次进入一级→二级也有 scrim 效果）
    val scrimAlpha = remember { Animatable(0f) }

    // 启动 transition
    LaunchedEffect(targetOverlay, direction) {
        if (direction != null) {
            // 首次进入和后续切换都走同样的 pendingOverlay 流程：
            // 新 slot 渲染 pendingOverlay + slideIn（首次进入时旧 slot 不存在，新 slot 直接显示）；
            // 后续 transition 时旧 slot 存在，做 exit 动画。
            pendingOverlay = targetOverlay
            activeDirection = direction
        }
    }

    // scrim 遮罩动画（独立于 overlay 槽位，始终覆盖在底层 Scaffold 之上、覆盖层之下）
    LaunchedEffect(activeDirection) {
        when (activeDirection) {
            TransitionDirection.Forward -> {
                // Forward：scrim 渐变到 SCRIM_ALPHA（黑纱盖在底层 + 旧 overlay 上）
                scrimAlpha.animateTo(SCRIM_ALPHA, tween(300))
            }
            TransitionDirection.Backward, TransitionDirection.Exit -> {
                // Backward / Exit：scrim 渐变回 0（底层 + 旧 overlay 恢复明亮）
                scrimAlpha.animateTo(0f, tween(300))
            }
            null -> {}
        }
    }

    Box(modifier) {
        // ===== scrim 半透明黑色遮罩（始终在最下层，覆盖底层 Scaffold）=====
        Box(modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = scrimAlpha.value))
        )

        // ===== 旧 overlay slot（z-order 位于 scrim 之上）=====
        displayedOverlay?.let { disp ->
            key(disp) {
                val alpha = remember(disp) { Animatable(1f) }
                val translationX = remember(disp) { Animatable(0f) }

                LaunchedEffect(pendingOverlay, activeDirection) {
                    when (activeDirection) {
                        TransitionDirection.Forward -> {
                            // Forward：旧 overlay 保持原位不动（scrim 由顶层 effect 处理）
                        }
                        TransitionDirection.Backward, TransitionDirection.Exit -> {
                            // Backward / Exit：旧 overlay 从右滑出 + 淡出
                            val animTrans = launch {
                                translationX.animateTo(screenWidthPx, tween(300))
                            }
                            val animAlpha = launch {
                                alpha.animateTo(0f, tween(300))
                            }
                            animTrans.join()
                            animAlpha.join()
                            displayedOverlay = pendingOverlay
                            pendingOverlay = null
                            activeDirection = null
                            onTransitionEnd()
                        }
                        null -> {} // 没有 transition
                    }
                }

                Box(modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        this.alpha = alpha.value
                        this.translationX = translationX.value
                    }
                ) {
                    content(disp)
                }
            }
        }

        // ===== 新 overlay slot =====
        // z-order 动态控制：Forward 时新 slot 在上（slideIn 可见）；Backward 时新 slot 在下（旧 slot slideOut 可见，露出新 slot）
        pendingOverlay?.let { pend ->
            key(pend) {
                val alpha = remember(pend) { Animatable(1f) }
                // 初始 translationX = screenWidthPx（在屏幕外右侧），避免首次渲染时新 slot 瞬间出现在屏幕中央
                // 覆盖旧 slot 导致"抽动"闪烁。
                // - Forward：LaunchedEffect 内 animateTo(0f) 从屏幕外滑入
                // - Backward：LaunchedEffect 内 snapTo(0f) 跳到屏幕中央（被旧 slot 覆盖，用户看不到跳变）
                val translationX = remember(pend) { Animatable(screenWidthPx) }

                LaunchedEffect(pend, activeDirection) {
                    when (activeDirection) {
                        TransitionDirection.Forward -> {
                            // Forward：从右滑入覆盖（已经在屏幕外，直接 animateTo）
                            translationX.animateTo(0f, tween(300))
                            // 接管：displayedOverlay = pend
                            // - 首次进入：displayedOverlay 从 null 变 pend，旧 slot 接管渲染新 overlay
                            // - 非首次 forward：displayedOverlay 从旧 overlay 变 pend，旧 slot 重新渲染新 overlay
                            displayedOverlay = pend
                            pendingOverlay = null
                            activeDirection = null
                            onTransitionEnd()
                        }
                        TransitionDirection.Backward -> {
                            // Backward：立即跳到屏幕中央（保持原位，被旧 slot 在 z-order 上方覆盖）
                            translationX.snapTo(0f)
                        }
                        TransitionDirection.Exit -> {
                            // Exit 不应该到这里（pendingOverlay 在 Exit 时为 null）
                        }
                        null -> {}
                    }
                }

                Box(modifier = Modifier
                    .fillMaxSize()
                    .zIndex(if (activeDirection == TransitionDirection.Backward) -1f else 1f)
                    .graphicsLayer {
                        this.alpha = alpha.value
                        this.translationX = translationX.value
                    }
                ) {
                    content(pend)
                }
            }
        }
    }
}
