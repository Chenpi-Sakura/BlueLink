package com.yjtzc.bluelink.ui.navigation

import androidx.activity.compose.BackHandler
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
import com.yjtzc.bluelink.ui.mine.MineScreen
import com.yjtzc.bluelink.ui.mine.MineViewModel
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
 * - 阅读器覆盖层：全屏阅读
 * - 灵感编辑器覆盖层：全屏编辑
 * - 我的子页面覆盖层：二级设置页
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

    // ====== 阅读器覆盖层 ======
    if (readerParams != null) {
        val params = readerParams!!
        val readerViewModel: ReaderViewModel = viewModel(
            factory = BlueLinkViewModelFactory(container)
        )

        BackHandler { readerParams = null }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("阅读") },
                    navigationIcon = {
                        IconButton(onClick = { readerParams = null }) {
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
                viewModel = readerViewModel,
                docId = params.docId,
                spotlightSegmentId = params.spotlightSegmentId,
                modifier = Modifier.padding(innerPadding)
            )
        }
        return
    }

    // ====== 灵感编辑器覆盖层 ======
    if (editorCardId != null) {
        BackHandler { editorCardId = null }

        // 从仓库查找卡片
        val cards by container.captureRepository.observeAllCards()
            .collectAsStateWithLifecycle(initialValue = emptyList())
        val card = cards.find { it.id == editorCardId }

        if (card != null) {
            InspirationEditorScreen(
                card = card,
                captureRepository = container.captureRepository,
                onBack = { editorCardId = null }
            )
        } else {
            // 卡片不存在或已删除
            LaunchedEffect(Unit) {
                snackbarHostState.showSnackbar("灵感不存在或已被删除")
                editorCardId = null
            }
        }
        return
    }

    // ====== 我的子页面覆盖层 ======
    if (mineRoute != null) {
        val onGoBack: () -> Unit = {
            // 如果有 from 路由则返回对应页面，否则返回总览页
            mineRoute = mineFromRoute
        }

        BackHandler { onGoBack() }

        val mineViewModel: MineViewModel = viewModel(
            factory = BlueLinkViewModelFactory(container)
        )

        when (mineRoute) {
            is MineRoute.Appearance -> {
                MineNavScaffold(
                    title = "外观设置",
                    onBack = onGoBack
                ) {
                    // TODO Phase 2: AppearanceSettingsScreen
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("外观设置", style = MaterialTheme.typography.bodyLarge, color = Ink600)
                    }
                }
            }
            is MineRoute.CognitiveSettings -> {
                MineNavScaffold(
                    title = "认知设置",
                    onBack = onGoBack
                ) {
                    // TODO Phase 3: CognitiveSettingsScreen
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("认知设置", style = MaterialTheme.typography.bodyLarge, color = Ink600)
                    }
                }
            }
            is MineRoute.PrivacySecurity -> {
                MineNavScaffold(
                    title = "隐私与安全",
                    onBack = onGoBack
                ) {
                    // TODO Phase 4: PrivacySecurityScreen
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("隐私与安全", style = MaterialTheme.typography.bodyLarge, color = Ink600)
                    }
                }
            }
            is MineRoute.PermissionManagement -> {
                MineNavScaffold(
                    title = "权限管理",
                    onBack = onGoBack
                ) {
                    // TODO Phase 5: PermissionManagementScreen
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("权限管理", style = MaterialTheme.typography.bodyLarge, color = Ink600)
                    }
                }
            }
            is MineRoute.DataExport -> {
                MineNavScaffold(
                    title = "数据导出",
                    onBack = onGoBack
                ) {
                    // TODO Phase 6: DataExportScreen
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("数据导出", style = MaterialTheme.typography.bodyLarge, color = Ink600)
                    }
                }
            }
            is MineRoute.PermanentDelete -> {
                MineNavScaffold(
                    title = "永久删除",
                    onBack = onGoBack
                ) {
                    // TODO Phase 6: PermanentDeleteScreen
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("永久删除", style = MaterialTheme.typography.bodyLarge, color = Ink600)
                    }
                }
            }
            null -> {}
        }
        return
    }

    // ====== 底部 Tab 导航 ======
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
                    viewModel = viewModel(
                        factory = BlueLinkViewModelFactory(container)
                    ),
                    onOpenInspiration = { cardId ->
                        editorCardId = cardId
                    },
                    modifier = Modifier.padding(innerPadding)
                )
                NavDest.CHAT -> ChatScreen(
                    viewModel = viewModel(
                        factory = BlueLinkViewModelFactory(container)
                    ),
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
                    viewModel = viewModel(
                        factory = BlueLinkViewModelFactory(container)
                    ),
                    modifier = Modifier.padding(innerPadding)
                )
                NavDest.MINE -> {
                    val mineViewModel: MineViewModel = viewModel(
                        factory = BlueLinkViewModelFactory(container)
                    )
                    MineScreen(
                        viewModel = mineViewModel,
                        onNavigateToAppearance = {
                            mineFromRoute = null
                            mineRoute = MineRoute.Appearance
                        },
                        onNavigateToCognitive = {
                            mineFromRoute = null
                            mineRoute = MineRoute.CognitiveSettings
                        },
                        onNavigateToPrivacySecurity = {
                            mineFromRoute = null
                            mineRoute = MineRoute.PrivacySecurity
                        },
                        onNavigateToPermission = {
                            mineFromRoute = null
                            mineRoute = MineRoute.PermissionManagement
                        },
                        onNavigateToDataExport = {
                            mineFromRoute = null
                            mineRoute = MineRoute.DataExport
                        },
                        onNavigateToPermanentDelete = {
                            mineFromRoute = null
                            mineRoute = MineRoute.PermanentDelete
                        },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
