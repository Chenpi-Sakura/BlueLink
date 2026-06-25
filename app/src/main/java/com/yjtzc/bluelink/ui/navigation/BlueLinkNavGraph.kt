package com.yjtzc.bluelink.ui.navigation

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.zIndex
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.rememberNavBackStack
import com.yjtzc.bluelink.R
import com.yjtzc.bluelink.ui.chat.ChatScreen
import com.yjtzc.bluelink.ui.graph.GraphScreen
import com.yjtzc.bluelink.ui.home.HomeScreen
import com.yjtzc.bluelink.ui.mine.MineScreen
import com.yjtzc.bluelink.ui.mine.MineViewModel
import com.yjtzc.bluelink.ui.theme.Ink400
import com.yjtzc.bluelink.ui.theme.KleinBlue
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
 * Scrim 黑色遮罩的目标 alpha 值（iOS-modal 风格）
 * 0.4 让底部 tab 页面看起来像蒙了一层黑纱，聚焦到子页面
 */
private const val SCRIM_ALPHA = 0.4f

// V2.2 旧版 ReaderParams / MineRoute / Overlay / TransitionDirection 已删除
// 全部能力由 androidx.navigation3 + OverlayNavKey / OverlayNavGraph 替代

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

    val container = LocalAppContainer.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // ===== V2.2 Nav3 迁移：覆盖层 back stack hoist 到 BlueLinkNavGraph =====
    // 由 OverlayNavGraph 消费（NavDisplay.backStack），tab 回调通过 onNavigate 闭包 push
    val backStack = rememberNavBackStack(OverlayNavKey.NoOverlay)
    val onNavigate: (OverlayNavKey) -> Unit = { key -> backStack.add(key) }

    // ===== Scrim 黑色遮罩 =====
    // 派生状态：是否有 overlay 打开（栈大小 > 1 = 栈顶有 overlay，NoOverlay 是唯一时 = 无 overlay）
    val isAnyOverlayOpen = backStack.size > 1
    // scrim alpha：与 overlay 同步淡入淡出（300ms，与 NavDisplay 动画时长一致）
    val scrimAlpha by animateFloatAsState(
        targetValue = if (isAnyOverlayOpen) SCRIM_ALPHA else 0f,
        animationSpec = tween(300),
        label = "scrim"
    )

    // ===== 复用 ViewModelFactory（避免每帧 new）=====
    val factory = remember(container) { BlueLinkViewModelFactory(container) }

    Box(Modifier.fillMaxSize()) {
        // ====== 底部 Tab 导航（始终组合） ======
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                // 不再根据 overlay 状态隐藏 NavigationBar —— 由 overlay 的不透明背景自然盖住 Tab 栏（iOS-modal 模式）
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
                    viewModel = viewModel(factory = factory),
                    // TODO: 接入暗色模式 state（当前临时硬编码 false，build 通过优先）
                    isDarkMode = false,
                    // TODO: 接入搜索意图（当前 no-op，build 通过优先）
                    onSearch = { /* TODO: 跳转到 SearchScreen */ },
                    // TODO: 接入抽屉打开（当前 no-op，build 通过优先）
                    onOpenDrawer = { /* TODO: 打开侧滑抽屉 */ },
                    // TODO: 接入暗色模式切换（当前 no-op，build 通过优先）
                    onToggleDarkMode = { /* TODO: 切换 dark/light theme */ },
                    onOpenInspiration = { cardId -> onNavigate(OverlayNavKey.EditorRoute(cardId)) },
                    modifier = Modifier.padding(innerPadding)
                )
                NavDest.CHAT -> ChatScreen(
                    viewModel = viewModel(factory = factory),
                    onNavigateToReader = { segmentId ->
                        // 保留异步解析：segment → doc（segmentId 找不到时弹 snackbar 提示用户）
                        scope.launch {
                            val segment = container.documentRepository.getSegmentById(segmentId)
                            if (segment != null) {
                                onNavigate(OverlayNavKey.ReaderRoute(docId = segment.docId, spotlightSegmentId = segmentId))
                            } else {
                                snackbarHostState.showSnackbar(message = "未找到该锚点对应的文档片段")
                            }
                        }
                    },
                    modifier = Modifier.padding(innerPadding)
                )
                NavDest.GRAPH -> GraphScreen(
                    viewModel = viewModel(factory = factory),
                    modifier = Modifier.padding(innerPadding)
                )
                NavDest.MINE -> {
                    val mineViewModel: MineViewModel = viewModel(factory = factory)
                    MineScreen(
                        viewModel = mineViewModel,
                        // 单一回调：6 个子页统一入口，子项 onClick 通过 onNavigate(targetKey) 触发
                        onNavigate = onNavigate,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }

        // ====== 覆盖层 Nav3（替代旧 OverlayLayer 手写动画）======
        // back stack 由本函数 hoist；OverlayNavGraph 渲染栈顶 entry 并处理系统返回手势
        OverlayNavGraph(
            backStack = backStack,
            snackbarHostState = snackbarHostState,
            container = container,
            factory = factory,
            // 栈只剩 NoOverlay 时按返回 —— 父级不需动作（系统 fallback 到 app minimize）
            onEmptyBack = { }
        )

        // ====== Scrim 黑色遮罩（iOS-modal 风格）======
        // 层级：在 OverlayNavGraph（zIndex 1f）之上（zIndex 2f）
        // 动画：与 overlay 同步淡入淡出（300ms）
        // 位置意义：
        // - 一级 overlay 打开时（Tab → 子页）：scrim 在 overlay 之上，统一视觉
        // - 嵌套 overlay 打开时（子页 → 子子页）：scrim 在所有 overlay 之上，二三级都可见
        // - 用户从 overlay 返回时：scrim 同步淡出
        if (scrimAlpha > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(2f)
                    .background(Color.Black.copy(alpha = scrimAlpha))
            )
        }
    }
}

// V2.2 旧版 OverlayLayer composable（movableContentOf + Animatable + graphicsLayer + scrim 双 slot 状态机）
// 已由 androidx.navigation3 + OverlayNavGraph 替代。相关 18 轮踩坑记录保留在
// docs/debug/2026-06-24-overlay-animation-and-editor-debug.md。
