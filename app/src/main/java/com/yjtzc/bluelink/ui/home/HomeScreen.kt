package com.yjtzc.bluelink.ui.home

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items as lazyItems
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items as staggeredItems
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.NoteAdd
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.rememberAsyncImagePainter
import com.yjtzc.bluelink.data.local.db.InspirationCardEntity
import java.io.File
import com.yjtzc.bluelink.ui.capture.CaptureSheet
import com.yjtzc.bluelink.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * 文库首页 — 对齐参考样式
 *
 * 1. 顶部标题栏：搜索图标（左）+ 黑夜模式切换（右）
 * 2. 问候语：时段问候 + 用户名字
 * 3. 瀑布流灵感卡片
 * 4. 底部悬浮圆润操作栏：录音（左）+ 文字新建（中）+ 图片（右）
 * 5. 文件夹管理在左侧抽屉中
 */
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    isDarkMode: Boolean,
    onSearch: () -> Unit,
    onOpenDrawer: () -> Unit,
    onToggleDarkMode: () -> Unit,
    onOpenInspiration: (cardId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val cards by viewModel.cards.collectAsStateWithLifecycle()
    val userName by viewModel.userName.collectAsStateWithLifecycle()

    var showCaptureSheet by remember { mutableStateOf(false) }
    var captureTabIndex by remember { mutableIntStateOf(0) }

    val greeting = getTimeGreetingWith(userName)

    // 处理底部栏触发的捕获请求
    val pendingCapture by viewModel.pendingCapture.collectAsStateWithLifecycle()
    LaunchedEffect(pendingCapture) {
        when (pendingCapture) {
            HomeViewModel.CaptureEntry.TEXT -> {
                captureTabIndex = 0
                showCaptureSheet = true
                viewModel.consumeCaptureRequest()
            }
            HomeViewModel.CaptureEntry.VOICE -> {
                captureTabIndex = 1
                showCaptureSheet = true
                viewModel.consumeCaptureRequest()
            }
            HomeViewModel.CaptureEntry.IMAGE -> {
                captureTabIndex = 2
                showCaptureSheet = true
                viewModel.consumeCaptureRequest()
            }
            null -> {}
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ====== 1. 顶部标题栏（参考样式：glass-panel 磨砂玻璃） ======
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                        .windowInsetsTopHeight(WindowInsets.statusBars),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 汉堡菜单（最左侧）
                    IconButton(onClick = onOpenDrawer) {
                        Icon(
                            Icons.Outlined.Menu,
                            contentDescription = "菜单",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    // 搜索图标
                    IconButton(onClick = onSearch) {
                        Icon(
                            Icons.Outlined.Search,
                            contentDescription = "搜索",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    // 黑夜/白天模式切换（最右侧）
                    IconButton(onClick = onToggleDarkMode) {
                        Icon(
                            if (isDarkMode) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
                            contentDescription = if (isDarkMode) "切换白天模式" else "切换黑夜模式",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), thickness = 0.5.dp)

            // ====== 2/3. 瀑布流（问候语在网格内，随内容滚动） ======
            InspirationContentView(
                cards = cards,
                greeting = greeting,
                viewModel = viewModel,
                onOpenInspiration = onOpenInspiration
            )
        }

        // ====== 4. 底部悬浮圆润操作栏 ======
        BottomFloatingBar(
            onTextClick = { viewModel.requestCapture(HomeViewModel.CaptureEntry.TEXT) },
            onVoiceClick = { viewModel.requestCapture(HomeViewModel.CaptureEntry.VOICE) },
            onImageClick = { viewModel.requestCapture(HomeViewModel.CaptureEntry.IMAGE) },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 36.dp)
        )
    }

    if (showCaptureSheet) {
        CaptureSheet(
            initialTabIndex = captureTabIndex,
            onDismiss = { showCaptureSheet = false }
        )
    }
}

// ====================================================================
// 灵感视图
// ====================================================================

@Composable
private fun InspirationContentView(
    cards: List<InspirationCardEntity>,
    greeting: String,
    viewModel: HomeViewModel,
    onOpenInspiration: (cardId: String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        if (cards.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("✨", fontSize = 48.sp)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "还没有灵感",
                        fontFamily = SerifFamily,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    Text("点击底部 + 号新建灵感", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Fixed(2),
                contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 120.dp),
                verticalItemSpacing = 12.dp,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 问候语占整行宽度，随滚动消失
                item(span = StaggeredGridItemSpan.FullLine) {
                    Text(
                        greeting,
                        fontFamily = SerifFamily,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 12.dp)
                    )
                }

                staggeredItems(cards, key = { it.id }) { card ->
                    InspirationCard(
                        card = card,
                        viewModel = viewModel,
                        onClick = { onOpenInspiration(card.id) },
                        onDelete = { viewModel.deleteCard(card) }
                    )
                }
            }
        }
    }
}

// ====================================================================
// 文件视图
// ====================================================================

@Composable
private fun FileContentView(
    folders: List<HomeViewModel.FileFolder>,
    viewModel: HomeViewModel,
    onOpenDocument: (docId: String) -> Unit,
    onToggleView: () -> Unit
) {
    var selectedFolderId by remember { mutableStateOf<String?>(null) }
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var showCreateFileDialog by remember { mutableStateOf(false) }
    var showUploadFileDialog by remember { mutableStateOf(false) }
    var folderToAddFile by remember { mutableStateOf<String?>(null) }

    // 文件选择器
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            // 从 URI 解析文件名
            val fileName = it.lastPathSegment ?: "导入文件"
            val fileType = when {
                fileName.endsWith(".doc") || fileName.endsWith(".docx") -> "word"
                fileName.endsWith(".md") -> "md"
                fileName.endsWith(".txt") -> "txt"
                else -> "txt"
            }
            folderToAddFile?.let { folderId ->
                viewModel.addFileToFolder(folderId, fileName, fileType)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // 视图切换按钮（左侧，灯泡=灵感，文件夹=文件）
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ViewToggleButton(
                currentView = HomeViewModel.ViewMode.FILE,
                onToggle = onToggleView
            )
        }

        if (selectedFolderId == null) {
            // 文件夹列表
            FolderGridView(
                folders = folders,
                onFolderClick = { selectedFolderId = it.id },
                onFolderLongClick = { folder ->
                    // 长按文件夹 → 删除 / 重命名
                    viewModel.deleteFolder(folder.id)
                },
                onCreateFolder = { showCreateFolderDialog = true },
                modifier = Modifier.weight(1f)
            )
        } else {
            // 文件夹内部文件列表
            val folder = folders.find { it.id == selectedFolderId }
            if (folder != null) {
                FileListView(
                    folder = folder,
                    onBack = { selectedFolderId = null },
                    onFileClick = { onOpenDocument(it.id) },
                    onFileLongClick = { file ->
                        viewModel.deleteFile(folder.id, file.id)
                    },
                    onCreateFile = {
                        folderToAddFile = folder.id
                        showCreateFileDialog = true
                    },
                    onUploadFile = {
                        folderToAddFile = folder.id
                        filePickerLauncher.launch("*/*")
                    },
                    modifier = Modifier.weight(1f)
                )
            } else {
                // 文件夹被删除，返回列表
                LaunchedEffect(Unit) { selectedFolderId = null }
            }
        }
    }

    // 新建文件夹对话框
    if (showCreateFolderDialog) {
        CreateFolderDialog(
            onDismiss = { showCreateFolderDialog = false },
            onConfirm = { name ->
                viewModel.createFolder(name)
                showCreateFolderDialog = false
            }
        )
    }

    // 新建文件对话框
    if (showCreateFileDialog) {
        CreateFileDialog(
            onDismiss = { showCreateFileDialog = false },
            onConfirm = { name, type ->
                folderToAddFile?.let { folderId ->
                    viewModel.addFileToFolder(folderId, name, type)
                }
                showCreateFileDialog = false
            }
        )
    }
}

// ====================================================================
// 文件夹网格
// ====================================================================

@Composable
private fun FolderGridView(
    folders: List<HomeViewModel.FileFolder>,
    onFolderClick: (HomeViewModel.FileFolder) -> Unit,
    onFolderLongClick: (HomeViewModel.FileFolder) -> Unit,
    onCreateFolder: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 120.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.fillMaxSize()
    ) {
        // 新建文件夹卡片
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = KleinBlueBg,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clickable(onClick = onCreateFolder)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Outlined.CreateNewFolder,
                            contentDescription = null,
                            tint = KleinBlue,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(Modifier.height(6.dp))
                        Text("新建文件夹", fontSize = 13.sp, color = KleinBlue, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }

        gridItems(folders, key = { it.id }) { folder ->
            FolderCard(
                folder = folder,
                onClick = { onFolderClick(folder) },
                onLongClick = { onFolderLongClick(folder) }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FolderCard(
    folder: HomeViewModel.FileFolder,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = { showMenu = true }
            )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 文件夹图标
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Text("📁", fontSize = 20.sp)
            }

            Column {
                Text(
                    text = folder.name,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "${folder.files.size} 个文件",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    DropdownMenu(
        expanded = showMenu,
        onDismissRequest = { showMenu = false }
    ) {
        DropdownMenuItem(
            text = { Text("🗑 删除文件夹") },
            onClick = {
                showMenu = false
                onLongClick()
            }
        )
    }
}

// ====================================================================
// 文件列表（文件夹内部）
// ====================================================================

@Composable
private fun FileListView(
    folder: HomeViewModel.FileFolder,
    onBack: () -> Unit,
    onFileClick: (HomeViewModel.FileItem) -> Unit,
    onFileLongClick: (HomeViewModel.FileItem) -> Unit,
    onCreateFile: () -> Unit,
    onUploadFile: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        // 文件夹标题栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = MaterialTheme.colorScheme.onSurface)
            }
            Spacer(Modifier.width(4.dp))
            Text(
                text = folder.name,
                fontFamily = SerifFamily,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            // 新建文件
            IconButton(onClick = onCreateFile) {
                Icon(Icons.AutoMirrored.Outlined.NoteAdd, contentDescription = "新建文件", tint = KleinBlue)
            }
            Spacer(Modifier.width(4.dp))
            // 上传文件
            IconButton(onClick = onUploadFile) {
                Icon(Icons.Outlined.FileUpload, contentDescription = "上传文件", tint = KleinBlue)
            }
        }

        if (folder.files.isEmpty()) {
            // 空状态
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📂", fontSize = 48.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("文件夹为空", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    Text("点击右上角新建或上传文件", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, top = 4.dp, end = 16.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                lazyItems(folder.files, key = { it.id }) { file ->
                    FileItemCard(
                        file = file,
                        onClick = { onFileClick(file) },
                        onLongClick = { onFileLongClick(file) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FileItemCard(
    file: HomeViewModel.FileItem,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = { showMenu = true }
            )
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 文件类型图标
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        when (file.type) {
                            "word" -> KleinBlueBg
                            "md" -> KleinBlueBg.copy(alpha = 0.7f)
                            "txt" -> MaterialTheme.colorScheme.surfaceVariant
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    when (file.type) {
                        "word" -> "W"
                        "md" -> "MD"
                        else -> "T"
                    },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = KleinBlue
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "${file.typeLabel} · ${file.sizeFormatted}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    DropdownMenu(
        expanded = showMenu,
        onDismissRequest = { showMenu = false }
    ) {
        DropdownMenuItem(
            text = { Text("🗑 删除") },
            onClick = {
                showMenu = false
                onLongClick()
            }
        )
        DropdownMenuItem(
            text = { Text("📤 导出") },
            onClick = {
                showMenu = false
                // 导出功能（预留）
            }
        )
        DropdownMenuItem(
            text = { Text("ℹ 文件信息") },
            onClick = {
                showMenu = false
                // TODO: 显示文件详情弹窗
            }
        )
    }
}

// ====================================================================
// 底部悬浮圆润操作栏
// ====================================================================

@Composable
private fun BottomFloatingBar(
    onTextClick: () -> Unit,
    onVoiceClick: () -> Unit,
    onImageClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
        shadowElevation = 6.dp,
        border = androidx.compose.foundation.BorderStroke(
            0.5.dp,
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ),
        modifier = modifier.padding(horizontal = 40.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // 左侧：录音
            IconButton(onClick = onVoiceClick) {
                Icon(
                    Icons.Outlined.Mic,
                    contentDescription = "从录音新建灵感",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
            }

            // 中间分隔线
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(24.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )

            // 中间：+ 号（从文字新建）
            Surface(
                shape = CircleShape,
                color = KleinBlue,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    IconButton(onClick = onTextClick) {
                        Icon(
                            Icons.Filled.Add,
                            contentDescription = "从文字新建灵感",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // 中间分隔线
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(24.dp)
                    .background(MaterialTheme.colorScheme.outlineVariant)
            )

            // 右侧：图片
            IconButton(onClick = onImageClick) {
                Icon(
                    Icons.Outlined.Image,
                    contentDescription = "从图片新建灵感",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

// ====================================================================
// 视图切换按钮
// ====================================================================

@Composable
private fun ViewToggleButton(
    currentView: HomeViewModel.ViewMode,
    onToggle: () -> Unit
) {
    val (iconText, label, desc) = if (currentView == HomeViewModel.ViewMode.INSPIRATION) {
        Triple("📁", "文件", "切换到文件视图")
    } else {
        Triple("💡", "灵感", "切换到灵感视图")
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = KleinBlueBg,
        modifier = Modifier
            .clickable(onClick = onToggle)
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(iconText, fontSize = 14.sp)
            Spacer(Modifier.width(4.dp))
            Text(
                label,
                fontSize = 12.sp,
                color = KleinBlue,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// ====================================================================
// 灵感卡片
// ====================================================================

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun InspirationCard(
    card: InspirationCardEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit = {},
    viewModel: HomeViewModel? = null
) {
    val context = LocalContext.current

    val tags = card.tags.split(",").filter { it.isNotBlank() }
    var showMenu by remember { mutableStateOf(false) }
    var showFolderPicker by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val folders by viewModel?.let { it.folders.collectAsState() } ?: remember { mutableStateOf(emptyList()) }

    // 从内容加载缩略图路径和文字标题
    var imageFilePath by remember { mutableStateOf<String?>(null) }
    var textTitle by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(card.id) {
        if (viewModel != null) {
            val raw = viewModel.loadCardContent(card)
            if (raw != null) {
                // 尝试解析 JSON 块
                try {
                    val arr = org.json.JSONArray(raw)
                    var firstImage: String? = null
                    var firstText: String? = null
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        when (obj.getString("type")) {
                            "image" -> if (firstImage == null) firstImage = obj.getString("data")
                            "text" -> if (firstText == null) firstText = obj.getString("data")
                        }
                    }
                    imageFilePath = firstImage
                    textTitle = firstText?.take(30)?.ifBlank { null }
                } catch (_: Exception) {
                    // 旧格式：单内容（纯文本或文件路径）
                    if (raw.isNotBlank()) {
                        when (card.type.name) {
                            "IMAGE" -> {
                                imageFilePath = raw
                                // 不设 textTitle，由下方"无文字标题"分支处理
                            }
                            "TEXT" -> {
                                textTitle = raw.take(30)
                            }
                            // VOICE：不设置 textTitle 或 imageFilePath，由下方 VOICE 分支显示录音占位
                        }
                    }
                }
            }
        }
    }

    // 标题：有文字用第一行文字；TEXT 卡用内容摘要兜底（避开 JSON 摘要）
    val displayTitle = textTitle ?: if (card.type.name == "TEXT") {
        card.contentSnippet.take(30).let { snippet ->
            if (snippet.isBlank() || snippet.startsWith("[") || snippet.startsWith("{")) null else snippet
        }
    } else null

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        shadowElevation = 1.dp,
        border = androidx.compose.foundation.BorderStroke(
            0.5.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = { showMenu = true }
            )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // 1. 最近修改时间（参考样式：font-sans text-[11px] text-ink-400）
            Text(
                text = formatCardDate(card.updatedAt),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))

            // 2. 标题 / 类型标签（有文字才显示）
            if (displayTitle != null) {
                Text(
                    text = displayTitle,
                    fontFamily = SerifFamily,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            }

            // 3. 内容 — 有图显示缩略图，VOICE 显示图标，TEXT 显示片段
            if (imageFilePath != null) {
                val file = File(imageFilePath!!).takeIf { it.exists() }
                if (file != null) {
                    Image(
                        painter = rememberAsyncImagePainter(model = file),
                        contentDescription = "图片预览",
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
            } else if (card.type.name == "VOICE") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .background(KleinBlueBg, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🎤", fontSize = 24.sp)
                }
            } else {
                Text(
                    text = card.contentSnippet.ifBlank { "无内容" },
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // 无文字时显示灰色提示
            if (displayTitle == null) {
                Text(
                    text = "无文字标题",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // 标签（参考样式：bg-parchment-50 rounded-lg）
            if (tags.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    tags.take(3).forEach { tag ->
                        Box(
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("#$tag", fontSize = 11.sp, color = KleinBlue)
                        }
                    }
                }
            }
        }
    }

    // 长按上下文菜单
    DropdownMenu(
        expanded = showMenu,
        onDismissRequest = { showMenu = false }
    ) {
        DropdownMenuItem(
            text = { Text("🗑 删除") },
            onClick = {
                showMenu = false
                showDeleteConfirm = true
            }
        )
        DropdownMenuItem(
            text = { Text("📤 导出") },
            onClick = {
                showMenu = false
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, card.contentSnippet)
                    putExtra(Intent.EXTRA_SUBJECT, displayTitle)
                }
                context.startActivity(Intent.createChooser(shareIntent, "导出灵感"))
            }
        )
        DropdownMenuItem(
            text = { Text("📁 移入文件夹") },
            onClick = {
                showMenu = false
                showFolderPicker = true
            }
        )
    }

    // 文件夹选择对话框
    if (showFolderPicker && viewModel != null) {
        MoveToFolderDialog(
            folders = folders,
            currentFolderId = card.folderId,
            onSelectFolder = { folderId ->
                viewModel.moveCardToFolder(card, folderId)
                showFolderPicker = false
            },
            onCreateFolder = { name ->
                viewModel.createFolder(name)
            },
            onDismiss = { showFolderPicker = false }
        )
    }

    // 删除确认对话框
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("删除灵感") },
            text = { Text("确定将此灵感移入回收站？15 天内可恢复。") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Vermilion)
                ) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("取消") }
            }
        )
    }
}

// ====================================================================
// 对话框
// ====================================================================

@Composable
private fun CreateFolderDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
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
                onClick = { if (name.isNotBlank()) onConfirm(name.trim()) },
                enabled = name.isNotBlank()
            ) {
                Text("创建")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
private fun CreateFileDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("md") }
    val fileTypes = listOf("md" to "Markdown", "txt" to "纯文本", "word" to "Word")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新建文件") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text("文件名（不含后缀）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                Text("文件类型", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    fileTypes.forEach { (type, label) ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = { selectedType = type },
                            label = { Text(label, fontSize = 12.sp) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val fullName = if (name.isNotBlank()) {
                        "$name.${selectedType}"
                    } else "新建文件.${selectedType}"
                    onConfirm(fullName, selectedType)
                },
                enabled = name.isNotBlank()
            ) {
                Text("创建")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

// ====================================================================
// 移入文件夹对话框
// ====================================================================

@Composable
private fun MoveToFolderDialog(
    folders: List<HomeViewModel.FileFolder>,
    currentFolderId: String?,
    onSelectFolder: (folderId: String?) -> Unit,
    onCreateFolder: (name: String) -> Unit,
    onDismiss: () -> Unit
) {
    var showNewFolderInput by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("移入文件夹") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // 移出文件夹选项（当前已在文件夹中时显示）
                if (currentFolderId != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectFolder(null) }
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("📂", fontSize = 16.sp)
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text("移出文件夹", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                Text("将灵感移回未归档", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    Spacer(Modifier.height(12.dp))
                }

                // 文件夹列表
                if (folders.isEmpty()) {
                    Text(
                        "还没有文件夹，请先创建",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    folders.forEach { folder ->
                        val isSelected = folder.id == currentFolderId
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) KleinBlueBg else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectFolder(folder.id) }
                                .padding(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("📁", fontSize = 16.sp)
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        folder.name,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (isSelected) KleinBlue else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        "${folder.files.size} 个文件",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (isSelected) {
                                    Text("✓", fontSize = 14.sp, color = KleinBlue, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                    }
                }

                Spacer(Modifier.height(8.dp))

                // 新建文件夹
                if (!showNewFolderInput) {
                    TextButton(
                        onClick = { showNewFolderInput = true },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Icon(Icons.Outlined.CreateNewFolder, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("新建文件夹", fontSize = 13.sp, color = KleinBlue)
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = newFolderName,
                            onValueChange = { newFolderName = it },
                            placeholder = { Text("文件夹名称", fontSize = 13.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = KleinBlue,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            )
                        )
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (newFolderName.isNotBlank()) {
                                    onCreateFolder(newFolderName.trim())
                                    newFolderName = ""
                                    showNewFolderInput = false
                                }
                            },
                            enabled = newFolderName.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = KleinBlue),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("创建", fontSize = 12.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

// ====================================================================
// 工具函数
// ====================================================================

private fun getTimeGreetingWith(userName: String): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greeting = when (hour) {
        in 5..11 -> "早上好"
        in 12..13 -> "中午好"
        else -> "晚上好"
    }
    return "$greeting，$userName"
}

private fun formatCardDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("M月d日 HH:mm", Locale.CHINESE)
    return sdf.format(Date(timestamp))
}
