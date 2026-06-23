package com.yjtzc.bluelink.ui.navigation

import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yjtzc.bluelink.R
import com.yjtzc.bluelink.data.local.db.CardType
import com.yjtzc.bluelink.data.local.db.InspirationCardEntity
import com.yjtzc.bluelink.ui.bookshelf.BookshelfScreen
import com.yjtzc.bluelink.ui.chat.ChatScreen
import com.yjtzc.bluelink.ui.graph.GraphScreen
import com.yjtzc.bluelink.ui.home.HomeScreen
import com.yjtzc.bluelink.ui.home.HomeViewModel
import com.yjtzc.bluelink.ui.mine.MineScreen
import com.yjtzc.bluelink.ui.reader.ReaderScreen
import com.yjtzc.bluelink.ui.reader.ReaderViewModel
import com.yjtzc.bluelink.ui.search.SearchScreen
import com.yjtzc.bluelink.ui.theme.Ink400
import com.yjtzc.bluelink.ui.theme.Ink600
import com.yjtzc.bluelink.ui.theme.Ink900
import com.yjtzc.bluelink.ui.theme.KleinBlue
import com.yjtzc.bluelink.ui.theme.Parchment100
import com.yjtzc.bluelink.ui.theme.Parchment50
import com.yjtzc.bluelink.domain.model.toDomain
import com.yjtzc.bluelink.ui.editor.InspirationEditorScreen
import com.yjtzc.bluelink.ui.theme.SerifFamily
import com.yjtzc.bluelink.ui.theme.Vermilion
import com.yjtzc.bluelink.data.local.db.TrashItemEntity
import java.util.UUID
import com.yjtzc.bluelink.ui.theme.KleinBlueBg
import com.yjtzc.bluelink.util.LocalAppContainer
import kotlinx.coroutines.launch
import java.io.File

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
 * - 搜索覆盖层：搜索灵感 / 文档 / 录音 / 图片
 * - 阅读器覆盖层：全屏阅读
 * - 设置覆盖层：认知设置 + 隐私
 * - 灵感编辑器覆盖层：编辑灵感
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlueLinkNavGraph(
    isDarkMode: Boolean,
    onToggleDarkMode: () -> Unit
) {
    var currentDest by remember { mutableStateOf(NavDest.HOME) }
    var readerParams by remember { mutableStateOf<ReaderParams?>(null) }
    var showSettings by remember { mutableStateOf(false) }
    var editorCardId by remember { mutableStateOf<String?>(null) }
    var showSearch by remember { mutableStateOf(false) }
    val container = LocalAppContainer.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scopeDrawer = rememberCoroutineScope()

    // 书架用文档列表
    val documents by container.documentRepository.observeAll()
        .collectAsStateWithLifecycle(initialValue = emptyList())

    // 搜索用灵感卡片列表
    val allCards by container.captureRepository.observeAllCards()
        .collectAsStateWithLifecycle(initialValue = emptyList())

    // 共享 HomeViewModel（HomeScreen + 左侧抽屉文件夹管理）
    val homeViewModel: HomeViewModel = viewModel(
        factory = BlueLinkViewModelFactory(container)
    )

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

    // ====== 搜索覆盖层 ======
    if (showSearch) {
        BackHandler { showSearch = false }

        SearchScreen(
            onBack = { showSearch = false },
            onOpenInspiration = { cardId ->
                editorCardId = cardId
                showSearch = false
            },
            onOpenDocument = { docId ->
                readerParams = ReaderParams(docId = docId)
                showSearch = false
            },
            allCards = allCards,
            documents = documents
        )
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
                Column(modifier = Modifier.fillMaxHeight()) {
                    // 用户信息头部
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                            .windowInsetsTopHeight(WindowInsets.statusBars)
                    ) {
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

                    // ====== 文件夹区域（可滚动，占满剩余空间） ======
                    DrawerFolderSection(
                        homeViewModel = homeViewModel,
                        allCards = allCards,
                        onOpenInspiration = { cardId ->
                            editorCardId = cardId
                            scopeDrawer.launch { drawerState.close() }
                        },
                        onSelectFolder = {
                            currentDest = NavDest.HOME
                            scopeDrawer.launch { drawerState.close() }
                        },
                        modifier = Modifier.weight(1f)
                    )

                    HorizontalDivider(color = Parchment100)
                    Spacer(Modifier.height(4.dp))

                    DrawerTrashSection(
                        homeViewModel = homeViewModel,
                        onRestore = { item -> homeViewModel.restoreFromTrash(item) },
                        onPermanentDelete = { item -> homeViewModel.permanentlyDelete(item) },
                        onRestoreAndOpen = { item ->
                            scopeDrawer.launch {
                                homeViewModel.restoreFromTrashAndOpen(item)
                                // 等待 Room Flow 传播到 StateFlow
                                kotlinx.coroutines.delay(300)
                                drawerState.close()
                                editorCardId = item.originalId
                            }
                        }
                    )

                    Spacer(Modifier.height(8.dp))
                }
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
                    viewModel = homeViewModel,
                    isDarkMode = isDarkMode,
                    onSearch = { showSearch = true },
                    onOpenDrawer = { scopeDrawer.launch { drawerState.open() } },
                    onToggleDarkMode = onToggleDarkMode,
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

// ====================================================================
// 左侧抽屉 — 文件夹区域（显示灵感卡片）
// ====================================================================

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DrawerFolderSection(
    homeViewModel: HomeViewModel,
    allCards: List<InspirationCardEntity>,
    onOpenInspiration: (String) -> Unit,
    onSelectFolder: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val folders by homeViewModel.folders.collectAsStateWithLifecycle()
    val importScope = rememberCoroutineScope()
    val captureRepository = LocalAppContainer.current.captureRepository
    var showCreateDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf<String?>(null) }
    var showPlusMenu by remember { mutableStateOf(false) }

    // 获取文件名
    fun getFileName(uri: Uri): String {
        var name = "unknown"
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0 && cursor.moveToFirst()) {
                name = cursor.getString(nameIndex) ?: "unknown"
            }
        }
        return name
    }

    // 文件导入选择器
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            importScope.launch {
                try {
                    val mime = context.contentResolver.getType(it) ?: ""
                    val fileName = getFileName(uri)
                    val ext = fileName.substringAfterLast('.', "").lowercase()

                    // 判断是否支持的类型
                    val supported = when {
                        mime.startsWith("image") || mime.startsWith("audio") -> true
                        ext in listOf("txt", "md", "markdown") -> true
                        ext in listOf("pdf") -> true
                        ext in listOf("doc", "docx") -> true
                        else -> false
                    }

                    if (!supported) {
                        Toast.makeText(context, "不支持打开 ${ext.uppercase()} 文件", Toast.LENGTH_SHORT).show()
                        return@launch
                    }

                    // 根据类型处理
                    when {
                        // 图片 → IMAGE 卡片
                        mime.startsWith("image") -> {
                            val destDir = File(context.filesDir, "captures")
                            destDir.mkdirs()
                            val destFile = File(destDir, "img_${System.currentTimeMillis()}.jpg")
                            context.contentResolver.openInputStream(it)?.use { input ->
                                destFile.outputStream().use { output -> input.copyTo(output) }
                            }
                            captureRepository.saveInspiration(
                                content = destFile.absolutePath,
                                type = CardType.IMAGE
                            )
                        }
                        // 音频 → VOICE 卡片
                        mime.startsWith("audio") -> {
                            val destDir = File(context.filesDir, "recordings")
                            destDir.mkdirs()
                            val destFile = File(destDir, "voice_${System.currentTimeMillis()}.3gp")
                            context.contentResolver.openInputStream(it)?.use { input ->
                                destFile.outputStream().use { output -> input.copyTo(output) }
                            }
                            captureRepository.saveInspiration(
                                content = destFile.absolutePath,
                                type = CardType.VOICE
                            )
                        }
                        // TXT/MD → 读取文字内容，存为 TEXT 卡片
                        ext in listOf("txt", "md", "markdown") -> {
                            val text = context.contentResolver.openInputStream(it)?.use { input ->
                                input.bufferedReader(charset("UTF-8")).readText()
                            } ?: ""
                            Toast.makeText(context, "已导入 $fileName", Toast.LENGTH_SHORT).show()
                            captureRepository.saveInspiration(
                                content = text.ifBlank { "(空文件)" },
                                type = CardType.TEXT
                            )
                        }
                        // PDF/DOCX → 复制文件，存为 TEXT 卡片，显示文件名
                        else -> {
                            val destDir = File(context.filesDir, "documents")
                            destDir.mkdirs()
                            val destFile = File(destDir, "${fileName}")
                            context.contentResolver.openInputStream(it)?.use { input ->
                                destFile.outputStream().use { output -> input.copyTo(output) }
                            }
                            Toast.makeText(context, "已导入 $fileName", Toast.LENGTH_SHORT).show()
                            captureRepository.saveInspiration(
                                content = "📄 $fileName",
                                type = CardType.TEXT
                            )
                        }
                    }
                } catch (_: Exception) {}
            }
        }
    }

    // 按 folderId 分组
    val cardsByFolder = allCards.groupBy { it.folderId }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        // 文件夹标题行
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "文件夹",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Ink600,
                modifier = Modifier.weight(1f)
            )
            // 加号菜单（新建 / 导入）
            Box {
                Surface(
                    shape = CircleShape,
                    color = KleinBlueBg,
                    modifier = Modifier
                        .size(28.dp)
                        .clickable { showPlusMenu = true }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("+", fontSize = 16.sp, color = KleinBlue, fontWeight = FontWeight.Bold)
                    }
                }
                DropdownMenu(
                    expanded = showPlusMenu,
                    onDismissRequest = { showPlusMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("📁 新建文件夹") },
                        onClick = {
                            showPlusMenu = false
                            showCreateDialog = true
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("📄 导入文件") },
                        onClick = {
                            showPlusMenu = false
                            importLauncher.launch("*/*")
                        }
                    )
                }
            }
        }

        // ====== 全部灵感 ======
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color.Transparent,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 2.dp)
                .clickable {
                    homeViewModel.selectFolder(null)  // null = 全部
                    onSelectFolder()
                }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("💡", fontSize = 16.sp)
                Spacer(Modifier.width(8.dp))
                Text(
                    "全部灵感",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Ink900,
                    modifier = Modifier.weight(1f),
                    maxLines = 1
                )
                Text(
                    "${allCards.size}",
                    fontSize = 11.sp,
                    color = Ink400
                )
            }
        }

        // ====== 用户创建的文件夹 ======
        folders.forEach { folder ->
            val folderCards = cardsByFolder[folder.id] ?: emptyList()

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color.Transparent,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 2.dp)
                    .combinedClickable(
                        onClick = {
                            homeViewModel.selectFolder(folder.id)
                            onSelectFolder()
                        },
                        onLongClick = {
                            showDeleteConfirm = folder.id
                        }
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("📁", fontSize = 16.sp)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        folder.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Ink900,
                        modifier = Modifier.weight(1f),
                        maxLines = 1
                    )
                    if (folderCards.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = KleinBlueBg
                        ) {
                            Text(
                                "${folderCards.size}",
                                fontSize = 11.sp,
                                color = KleinBlue,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
    }

    // 新建文件夹对话框
    if (showCreateDialog) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("新建文件夹") },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text("文件夹名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            homeViewModel.createFolder(name.trim())
                            showCreateDialog = false
                        }
                    },
                    enabled = name.isNotBlank()
                ) { Text("创建") }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) { Text("取消") }
            }
        )
    }

    // 删除确认对话框
    if (showDeleteConfirm != null) {
        val folderName = folders.find { it.id == showDeleteConfirm }?.name ?: "此文件夹"
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text("删除文件夹") },
            text = {
                Text("确定删除「${folderName}」吗？文件夹内的灵感不会被删除，将移出此文件夹。")
            },
            confirmButton = {
                Button(
                    onClick = {
                        homeViewModel.deleteFolder(showDeleteConfirm!!)
                        showDeleteConfirm = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Vermilion)
                ) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = null }) { Text("取消") }
            }
        )
    }
}


// ====================================================================
// 左侧抽屉 — 回收站区域
// ====================================================================

@Composable
private fun DrawerTrashSection(
    homeViewModel: HomeViewModel,
    onRestore: (TrashItemEntity) -> Unit,
    onPermanentDelete: (TrashItemEntity) -> Unit,
    onRestoreAndOpen: (TrashItemEntity) -> Unit = {}
) {
    val trashItems by homeViewModel.trashItems.collectAsStateWithLifecycle()
    var expanded by remember { mutableStateOf(false) }
    var permanentDeleteTarget by remember { mutableStateOf<TrashItemEntity?>(null) }

    Column(modifier = Modifier.fillMaxWidth()) {
        // 回收站标题行
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("🗑", fontSize = 16.sp)
            Spacer(Modifier.width(8.dp))
            Text(
                "回收站",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Ink900,
                modifier = Modifier.weight(1f)
            )
            if (trashItems.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Vermilion.copy(alpha = 0.1f)
                ) {
                    Text(
                        "${trashItems.size}",
                        fontSize = 11.sp,
                        color = Vermilion,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Text(if (expanded) "▲" else "▼", fontSize = 10.sp, color = Ink400)
        }

        if (expanded) {
            if (trashItems.isEmpty()) {
                Text(
                    "    回收站为空",
                    fontSize = 12.sp, color = Ink400,
                    modifier = Modifier.padding(start = 44.dp, bottom = 8.dp)
                )
            } else {
                trashItems.forEach { item ->
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 2.dp)
                            .then(
                                if (item.itemType == "INSPIRATION") {
                                    Modifier.clickable { onRestoreAndOpen(item) }
                                } else Modifier
                            )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                when (item.itemType) {
                                    "INSPIRATION" -> "💡"
                                    "DOCUMENT" -> "📄"
                                    else -> "📁"
                                },
                                fontSize = 14.sp
                            )
                            Spacer(Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    item.title.ifBlank { "未命名" },
                                    fontSize = 13.sp, color = Ink900, maxLines = 1
                                )
                                Text(
                                    item.itemType.lowercase().replaceFirstChar { it.uppercase() },
                                    fontSize = 10.sp, color = Ink400
                                )
                            }
                            // 恢复按钮
                            IconButton(
                                onClick = { onRestore(item) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Text("↩", fontSize = 14.sp, color = KleinBlue)
                            }
                            // 永久删除按钮
                            IconButton(
                                onClick = { permanentDeleteTarget = item },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Text("✕", fontSize = 14.sp, color = Vermilion)
                            }
                        }
                    }
                }
            }
        }
    }

    // 永久删除确认
    if (permanentDeleteTarget != null) {
        val item = permanentDeleteTarget!!
        AlertDialog(
            onDismissRequest = { permanentDeleteTarget = null },
            title = { Text("永久删除") },
            text = { Text("确定永久删除「${item.title.ifBlank { "未命名" }}」？此操作不可撤销，内容将被彻底清除。") },
            confirmButton = {
                Button(
                    onClick = {
                        onPermanentDelete(item)
                        permanentDeleteTarget = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Vermilion)
                ) { Text("永久删除") }
            },
            dismissButton = {
                TextButton(onClick = { permanentDeleteTarget = null }) { Text("取消") }
            }
        )
    }
}
