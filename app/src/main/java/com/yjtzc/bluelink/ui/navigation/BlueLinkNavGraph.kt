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
import cafe.adriel.voyager.navigator.tab.CurrentTab
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import cafe.adriel.voyager.navigator.tab.TabNavigator
import com.yjtzc.bluelink.ui.navigation.screen.ChatVoyagerScreen
import com.yjtzc.bluelink.ui.navigation.screen.GraphVoyagerScreen
import com.yjtzc.bluelink.ui.navigation.screen.HomeVoyagerScreen
import com.yjtzc.bluelink.ui.navigation.screen.MineVoyagerScreen
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
    // ===== 阶段 1：用 Voyager TabNavigator 替换原 currentDest state =====
    // 阶段 1 仅迁移 Tab 切换：底 NavigationBar + 4 个 VoyagerScreen
    // OverlayLayer（readerParams/editorCardId/mineRoute/mineFromRoute 状态机）完全保留——所有 push/pop 走原 OverlayLayer
    // 阶段 2 才把 push/pop 改为 Voyager Navigator.push/pop，删除 state machine
    var readerParams by remember { mutableStateOf<ReaderParams?>(null) }
    var editorCardId by remember { mutableStateOf<String?>(null) }
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
        // ===== 阶段 1：底部 Tab 导航改用 Voyager TabNavigator =====
        TabNavigator(HomeVoyagerScreen()) { tabNavigator ->
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
                        val tabs = remember { listOf(
                            HomeVoyagerScreen(),
                            ChatVoyagerScreen(),
                            GraphVoyagerScreen(),
                            MineVoyagerScreen()
                        ) }
                        val tabIcons = remember { listOf(
                            R.drawable.ic_home,
                            R.drawable.ic_chat,
                            R.drawable.ic_graph,
                            R.drawable.ic_account_box
                        ) }
                        tabs.forEachIndexed { index, tab ->
                            val selected = tabNavigator.current == tab
                            NavigationBarItem(
                                selected = selected,
                                onClick = { tabNavigator.current = tab },
                                icon = { Icon(painterResource(tabIcons.getOrElse(index) { R.drawable.ic_home }), contentDescription = tab.options.title, modifier = Modifier.size(24.dp)) },
                                label = { Text(tab.options.title, fontSize = 10.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium) },
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
                // 阶段 1：Tab 切换用 Voyager TabNavigator，但二级页面回调仍直接设置 BlueLinkNavGraph 的 state
                //（editorCardId / readerParams / mineRoute 等），不走 navigator.push（阶段 2 才接）
                // 这样 Tab 切换 TabNavigator 负责、二级页面行为完全保留
                Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                    when (tabNavigator.current) {
                        is HomeVoyagerScreen -> HomeScreen(
                            viewModel = viewModel(factory = BlueLinkViewModelFactory(container)),
                            isDarkMode = false,
                            onSearch = { },
                            onOpenDrawer = { },
                            onToggleDarkMode = { },
                            onOpenInspiration = { cardId -> editorCardId = cardId },
                            modifier = Modifier.fillMaxSize()
                        )
                        is ChatVoyagerScreen -> ChatScreen(
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
                            modifier = Modifier.fillMaxSize()
                        )
                        is GraphVoyagerScreen -> GraphScreen(
                            viewModel = viewModel(factory = BlueLinkViewModelFactory(container)),
                            modifier = Modifier.fillMaxSize()
                        )
                        is MineVoyagerScreen -> {
                            val mineVM: MineViewModel = viewModel(factory = BlueLinkViewModelFactory(container))
                            MineScreen(
                                viewModel = mineVM,
                                onNavigateToAppearance = { mineFromRoute = null; mineRoute = MineRoute.Appearance },
                                onNavigateToCognitive = { mineFromRoute = null; mineRoute = MineRoute.CognitiveSettings },
                                onNavigateToPrivacySecurity = { mineFromRoute = null; mineRoute = MineRoute.PrivacySecurity },
                                onNavigateToPermission = { mineFromRoute = null; mineRoute = MineRoute.PermissionManagement },
                                onNavigateToDataExport = { mineFromRoute = null; mineRoute = MineRoute.DataExport },
                                onNavigateToPermanentDelete = { mineFromRoute = null; mineRoute = MineRoute.PermanentDelete },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
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
                            // 完整内容加载中：debounce 延迟显示 Spinner
                            // - 前 150ms 保持干净的主题色背景（slideIn 动画掩盖 IO 耗时）
                            // - 超过 150ms 才出 CircularProgressIndicator（避免 100ms Spinner 一闪而过的 FOUC/Loading Flicker）
                            var showLoading by remember { mutableStateOf(false) }
                            LaunchedEffect(Unit) {
                                delay(150)
                                showLoading = true
                            }
                            Box(
                                modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
                                contentAlignment = Alignment.Center
                            ) {
                                if (showLoading) {
                                    CircularProgressIndicator()
                                }
                            }
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
 * 关键设计：旧 slot 用稳定 key "displayed_overlay_slot"（不依赖 disp），
 * 避免 Forward 接管时 key(disp) 变化触发整个旧 slot instance 重建（会导致 100ms 内容消失闪动）。
 * content(disp) 在 disp 变化时重新渲染新 overlay 内容，但 slot instance / Animatable 都保留——无缝接管。
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

    // displayedOverlay = 当前显示的旧 overlay（在旧 slot 稳定 key 块内渲染，content(disp) 在 disp 变化时重新渲染）
    // pendingOverlay = 等待显示的新 overlay（新 slot 跑 slideIn / 保持原位 / slideOut 动画）
    var displayedOverlay by remember { mutableStateOf<Overlay?>(null) }
    var pendingOverlay by remember { mutableStateOf<Overlay?>(null) }
    var activeDirection by remember { mutableStateOf<TransitionDirection?>(null) }

    // scrim 半透明黑色遮罩 alpha（统一在 OverlayLayer 顶层管理，首次进入一级→二级也有 scrim 效果）
    val scrimAlpha = remember { Animatable(0f) }

    // 🌟 核心魔法：movableContentOf 包装 content
    // 当 stableKeySlot 和 pendingOverlaySlot 都调用 movableOverlayContent(overlay) 时，
    // Compose 检测到同一个 movableContent 实例，会直接把整个 LayoutNode 从一个 Box 平移到另一个 Box——
    // 不触发 onDispose、不重新 measure、保留 Coil 图片加载进度、内部 remember state
    // 解决 slot swap 时 InspirationEditorScreen 销毁重建 + transparent Box 空档问题
    val movableOverlayContent = remember {
        movableContentOf<Overlay> { overlay ->
            content(overlay)
        }
    }

    // 启动 transition
    LaunchedEffect(targetOverlay, direction) {
        when {
            targetOverlay != null && direction != null -> {
                // 进入（首次或嵌套变深）：新 slot 跑 slideIn
                pendingOverlay = targetOverlay
                activeDirection = direction
            }
            targetOverlay == null && (pendingOverlay != null || displayedOverlay != null) -> {
                // 退出（覆盖首次进入后退出和嵌套切换后退出两种场景）：
                // - pendingOverlay != null：首次进入后退出，新 slot 自己跑 slideOut
                // - displayedOverlay != null：Forward 完成后 pendingOverlay 已被清空，靠 displayedOverlay 跑 slideOut
                // 关键修复：之前只判断 pendingOverlay != null，Forward 完成后 pendingOverlay = null 但 displayedOverlay != null，
                // 返回时所有分支都不命中 → activeDirection 保持 null → 没有 Exit 动画触发 → 卡片退不出去
                activeDirection = TransitionDirection.Exit
            }
            // 其他场景保持当前状态
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

        // ===== 旧 overlay slot（key 稳定，disp 变化不触发 instance 重建）=====
        // 这是修复"Forward 接管时内容消失 100ms 闪动"的关键：
        // - Forward 完成后，新 slot (pendingOverlay) 接管渲染——但旧 slot 的 key 稳定（不依赖 disp），
        //   所以旧 slot 的 Animatable / GraphicsLayer 都保留，content(disp) 在 disp 变化时重新渲染新 overlay 内容，
        //   同一帧内两个 slot 都在渲染新内容，新 slot dispose 后只剩旧 slot 显示——无缝接管，无空档
        // - Backward 时：旧 slot (displayedOverlay = 旧 overlay) 跑 slideOut + fadeOut
        // - Exit 时（旧 slot 存在时）：同上
        displayedOverlay?.let { disp ->
            key("displayed_overlay_slot") {  // 稳定 key——disp 变化不触发 instance 重建
                val alpha = remember { Animatable(1f) }
                val translationX = remember { Animatable(0f) }

                LaunchedEffect(pendingOverlay, activeDirection) {
                    when (activeDirection) {
                        TransitionDirection.Backward, TransitionDirection.Exit -> {
                            // 旧 overlay 从右滑出 + 淡出
                            val animTrans = launch {
                                translationX.animateTo(screenWidthPx, tween(300))
                            }
                            val animAlpha = launch {
                                alpha.animateTo(0f, tween(300))
                            }
                            animTrans.join()
                            animAlpha.join()
                            displayedOverlay = null
                            activeDirection = null
                            onTransitionEnd()
                        }
                        else -> {} // Forward 和 null：旧 slot 继续渲染（content(disp) 在 disp 变化时重新渲染新 overlay 内容）
                    }
                }

                Box(modifier = Modifier
                    .fillMaxSize()
                    // 不加 .background() 防御性兜底——会让 Box 在退出动画期间遮住底层 HOME
                    // movableContentOf 已经解决了 slideIn 完成时空档问题，不需要 opaque 兜底
                    .graphicsLayer {
                        this.alpha = alpha.value
                        this.translationX = translationX.value
                    }
                ) {
                    // 🌟 关键：调用 movableOverlayContent(disp) 而不是 content(disp)，
                    // 让 Compose 检测 movableContent 实例并平移 LayoutNode 而非销毁重建
                    movableOverlayContent(disp)
                }
            }
        }

        // ===== 新 overlay slot（跑 slideIn / 保持原位 / slideOut）=====
        pendingOverlay?.let { pend ->
            key(pend) {
                val alpha = remember(pend) { Animatable(1f) }
                // 初始 translationX = screenWidthPx（在屏幕外右侧），避免首次渲染时新 slot 瞬间出现在屏幕中央
                // 覆盖旧 slot 导致"抽动"闪烁。
                // - Forward：LaunchedEffect 内 animateTo(0f) 从屏幕外滑入
                // - Backward：LaunchedEffect 内 snapTo(0f) 跳到屏幕中央（被旧 slot 覆盖，用户看不到跳变）
                // - Exit：LaunchedEffect 内 animateTo(screenWidthPx) 从屏幕中央滑出
                val translationX = remember(pend) { Animatable(screenWidthPx) }

                LaunchedEffect(pend, activeDirection) {
                    when (activeDirection) {
                        TransitionDirection.Forward -> {
                            // Forward：从右滑入覆盖（已经在屏幕外，直接 animateTo）
                            translationX.animateTo(0f, tween(300))
                            // Forward 完成：旧 slot 已经显示新 content（content(disp) 重新渲染），
                            // 此时把 displayedOverlay 设为 pend（让旧 slot 用 content(pend) 渲染新 overlay 内容），
                            // 同时清 pendingOverlay（让新 slot dispose）——无缝接管，无 100ms 内容消失
                            displayedOverlay = pend
                            pendingOverlay = null
                            activeDirection = null
                            onTransitionEnd()
                        }
                        TransitionDirection.Backward -> {
                            // Backward：立即跳到屏幕中央（保持原位，被旧 slot 在 z-order 上方覆盖）
                            translationX.snapTo(0f)
                            // 不更新 displayedOverlay——让旧 slot 跑 slideOut 动画
                        }
                        TransitionDirection.Exit -> {
                            // Exit（首次进入后退出：displayedOverlay = null，没有旧 slot 可以跑 exit），
                            // 新 slot 自己跑 slideOut + fadeOut
                            val animTrans = launch {
                                translationX.animateTo(screenWidthPx, tween(300))
                            }
                            val animAlpha = launch {
                                alpha.animateTo(0f, tween(300))
                            }
                            animTrans.join()
                            animAlpha.join()
                            pendingOverlay = null
                            activeDirection = null
                            onTransitionEnd()
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
                    // 🌟 关键：调用 movableOverlayContent(pend) 而不是 content(pend)，
                    // 让 Compose 检测 movableContent 实例并平移 LayoutNode 而非销毁重建
                    movableOverlayContent(pend)
                }
            }
        }
    }
}
