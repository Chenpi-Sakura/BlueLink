package com.yjtzc.bluelink.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.yjtzc.bluelink.ui.theme.SerifFamily
import com.yjtzc.bluelink.util.LocalAppContainer
import kotlinx.coroutines.launch

/**
 * 底部 4 Tab 导航目的地
 */
enum class NavDest(val label: String, val icon: Int) {
    HOME("灵感", R.drawable.ic_home),
    CHAT("对话", R.drawable.ic_chat),
    GRAPH("图谱", R.drawable.ic_graph),
    BOOKSHELF("书架", R.drawable.ic_bookshelf)
}

/**
 * 阅读器导航参数
 */
data class ReaderParams(
    val docId: String,
    val spotlightSegmentId: String? = null
)

/**
 * App 主导航骨架
 *
 * - 左侧抽屉：用户信息 + 设置入口（认知设置 / 隐私和安全）
 * - 底部 4 Tab：文库 / 对话 / 图谱 / 书架
 * - 阅读器覆盖层：全屏阅读
 * - 设置覆盖层：认知设置 + 隐私
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlueLinkNavGraph() {
    var currentDest by remember { mutableStateOf(NavDest.HOME) }
    var readerParams by remember { mutableStateOf<ReaderParams?>(null) }
    var showSettings by remember { mutableStateOf(false) }
    var editorCardId by remember { mutableStateOf<String?>(null) }
    val container = LocalAppContainer.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scopeDrawer = rememberCoroutineScope()

    // 书架用文档列表
    val documents by container.documentRepository.observeAll()
        .collectAsStateWithLifecycle(initialValue = emptyList())

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

    // ====== 设置覆盖层 ======
    if (showSettings) {
        BackHandler { showSettings = false }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("设置") },
                    navigationIcon = {
                        IconButton(onClick = { showSettings = false }) {
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
            MineScreen(
                viewModel = viewModel(
                    factory = BlueLinkViewModelFactory(container)
                ),
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

    // ====== 侧滑抽屉 + 底部 Tab 导航 ======
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Parchment50,
                modifier = Modifier.width(280.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .windowInsetsTopHeight(WindowInsets.statusBars)
                ) {
                    // 头像
                    Surface(
                        shape = CircleShape,
                        color = Parchment100,
                        modifier = Modifier.size(64.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                "AI",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Ink600
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "用户",
                        fontFamily = SerifFamily,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Ink900
                    )
                }

                HorizontalDivider(color = Parchment100)
                Spacer(Modifier.height(8.dp))

                NavigationDrawerItem(
                    icon = { Text("⚙", fontSize = 18.sp) },
                    label = {
                        Text("认知设置", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Ink900)
                    },
                    selected = false,
                    onClick = {
                        showSettings = true
                        scopeDrawer.launch { drawerState.close() }
                    },
                    colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent)
                )

                NavigationDrawerItem(
                    icon = { Text("🔒", fontSize = 18.sp) },
                    label = {
                        Text("隐私和安全", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Ink900)
                    },
                    selected = false,
                    onClick = {
                        showSettings = true
                        scopeDrawer.launch { drawerState.close() }
                    },
                    colors = NavigationDrawerItemDefaults.colors(unselectedContainerColor = Color.Transparent)
                )
            }
        }
    ) {
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
                    onOpenDrawer = {
                        scopeDrawer.launch { drawerState.open() }
                    },
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
                NavDest.BOOKSHELF -> BookshelfScreen(
                    documents = documents.map { it.toDomain() },
                    onBookClick = { docId ->
                        readerParams = ReaderParams(docId = docId)
                    },
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }
}
