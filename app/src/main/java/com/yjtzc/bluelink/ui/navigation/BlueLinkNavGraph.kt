package com.yjtzc.bluelink.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    // 当前覆盖层
    val overlay = when {
        readerParams != null -> "reader"
        editorCardId != null -> "editor"
        mineRoute != null -> "mine"
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
        AnimatedContent(
            targetState = overlay,
            transitionSpec = {
                val duration = 300
                if (targetState != null) {
                    // 进入：从右滑入
                    (slideInHorizontally(animationSpec = tween(duration)) { width -> width } + fadeIn())
                        .togetherWith(slideOutHorizontally(animationSpec = tween(duration)) { width -> -width } + fadeOut())
                } else {
                    // 退出：向右滑出
                    (slideInHorizontally(animationSpec = tween(duration)) { width -> -width } + fadeIn())
                        .togetherWith(slideOutHorizontally(animationSpec = tween(duration)) { width -> width } + fadeOut())
                }
            },
            label = "overlayTransition"
        ) { target ->
            when (target) {
                "reader" -> {
                    val params = readerParams ?: return@AnimatedContent
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

                "editor" -> {
                    val cardId = editorCardId ?: return@AnimatedContent
                    BackHandler { editorCardId = null }

                    val cards by container.captureRepository.observeAllCards()
                        .collectAsStateWithLifecycle(initialValue = emptyList())
                    val card = cards.find { it.id == cardId }

                    if (card != null) {
                        InspirationEditorScreen(
                            card = card,
                            captureRepository = container.captureRepository,
                            onBack = { editorCardId = null }
                        )
                    } else {
                        LaunchedEffect(Unit) {
                            snackbarHostState.showSnackbar("灵感不存在或已被删除")
                            editorCardId = null
                        }
                    }
                }

                "mine" -> {
                    val route = mineRoute ?: return@AnimatedContent
                    val onGoBack: () -> Unit = {
                        if (mineFromRoute != null) {
                            mineRoute = mineFromRoute
                            mineFromRoute = null
                        } else {
                            mineRoute = null
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
