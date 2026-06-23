package com.yjtzc.bluelink.ui.editor

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
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
import coil.compose.rememberAsyncImagePainter
import com.yjtzc.bluelink.data.local.db.InspirationCardEntity
import com.yjtzc.bluelink.data.repository.CaptureRepository
import androidx.core.content.ContextCompat
import com.yjtzc.bluelink.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.Locale
import java.util.UUID

// ====== 内容块数据模型 ======

data class ContentBlock(
    val id: String = UUID.randomUUID().toString(),
    val type: String,   // "text" | "image" | "voice"
    val data: String    // 文本内容 / 文件路径
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("type", type)
        put("data", data)
    }

    companion object {
        fun fromJson(json: JSONObject): ContentBlock = ContentBlock(
            type = json.getString("type"),
            data = json.getString("data")
        )
    }
}

fun List<ContentBlock>.toJsonString(): String =
    JSONArray(map { it.toJson() }).toString()

fun String.parseContentBlocks(cardType: String = "TEXT"): List<ContentBlock> {
    // 先尝试解析为 JSON 数组（新格式）
    try {
        val arr = JSONArray(this)
        return (0 until arr.length()).map { ContentBlock.fromJson(arr.getJSONObject(it)) }
    } catch (_: Exception) {
        // 旧格式：单内容
        val blockType = when (cardType) {
            "IMAGE" -> "image"
            "VOICE" -> "voice"
            else -> "text"
        }
        return listOf(ContentBlock(type = blockType, data = this))
    }
}

// ====== 灵感编辑器 — 支持阅读/编辑双模式 + 多内容块 ======

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InspirationEditorScreen(
    card: InspirationCardEntity,
    captureRepository: CaptureRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 核心状态
    var blocks by remember { mutableStateOf<List<ContentBlock>>(emptyList()) }
    var isLoaded by remember { mutableStateOf(false) }
    var isEditMode by remember { mutableStateOf(false) }

    // 撤回栈
    val undoStack = remember { mutableStateListOf<List<ContentBlock>>() }
    fun pushUndo() {
        if (blocks.isNotEmpty()) undoStack.add(0, blocks.toList())
    }
    fun undo() {
        if (undoStack.isNotEmpty()) {
            blocks = undoStack.removeAt(0)
        }
    }

    // 加载内容
    LaunchedEffect(card.id) {
        try {
            val raw = captureRepository.readCardContent(card)
            blocks = raw.parseContentBlocks(card.type.name)
        } catch (_: Exception) {
            blocks = card.contentSnippet.parseContentBlocks(card.type.name)
        }
        isLoaded = true
    }

    // 自动保存（仅编辑模式退出或手动触发）
    val scope = rememberCoroutineScope()
    fun saveContent() {
        scope.launch {
            try {
                captureRepository.updateCardContent(card, blocks.toJsonString())
            } catch (_: Exception) {}
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    // 图片选择器（编辑模式添加图片用）
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            pushUndo()
            try {
                val imgDir = File(context.filesDir, "captures")
                imgDir.mkdirs()
                val destFile = File(imgDir, "img_${System.currentTimeMillis()}.jpg")
                context.contentResolver.openInputStream(it)?.use { input ->
                    destFile.outputStream().use { output -> input.copyTo(output) }
                }
                blocks = blocks + ContentBlock(type = "image", data = destFile.absolutePath)
            } catch (_: Exception) {}
        }
    }

    // 录音对话框状态
    var showRecordingDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (isEditMode) "编辑灵感" else "灵感",
                        fontFamily = SerifFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (isEditMode) {
                            isEditMode = false
                            saveContent()
                        } else {
                            onBack()
                        }
                    }) {
                        Icon(
                            if (isEditMode) Icons.Filled.Check else Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = if (isEditMode) "完成" else "返回"
                        )
                    }
                },
                actions = {
                    if (!isLoaded) return@TopAppBar

                    // 撤回按钮
                    IconButton(
                        onClick = { undo() },
                        enabled = undoStack.isNotEmpty()
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Undo,
                            contentDescription = "撤回",
                            tint = if (undoStack.isNotEmpty()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                    }

                    // 切换阅读/编辑按钮
                    IconButton(onClick = {
                        if (isEditMode) saveContent()
                        isEditMode = !isEditMode
                    }) {
                        Icon(
                            if (isEditMode) Icons.Outlined.Visibility else Icons.Outlined.Edit,
                            contentDescription = if (isEditMode) "切换到阅读模式" else "切换到编辑模式",
                            tint = KleinBlue
                        )
                    }

                    // 导出按钮（选择格式）
                    var showExportDialog by remember { mutableStateOf(false) }
                    IconButton(onClick = { showExportDialog = true }) {
                        Icon(
                            Icons.Outlined.Share,
                            contentDescription = "导出",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    if (showExportDialog) {
                        ExportFormatDialog(
                            onExportText = {
                                showExportDialog = false
                                exportAsText(blocks, context)
                            },
                            onExportWord = {
                                showExportDialog = false
                                exportAsWord(blocks, context)
                            },
                            onDismiss = { showExportDialog = false }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (!isLoaded) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // 内容区域
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                        .clickable(enabled = !isEditMode) { /* 点击空白不切换 */ }
                ) {
                    if (blocks.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .background(Parchment100, RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("暂无内容，切换编辑模式添加", fontSize = 14.sp, color = Ink400)
                        }
                    } else {
                        blocks.forEachIndexed { index, block ->
                            ContentBlockView(
                                block = block,
                                isEditMode = isEditMode,
                                onUpdate = { newData ->
                                    pushUndo()
                                    blocks = blocks.toMutableList().also { it[index] = block.copy(data = newData) }
                                },
                                onDelete = {
                                    pushUndo()
                                    blocks = blocks.toMutableList().also { it.removeAt(index) }
                                },
                                onDoubleClick = {
                                    if (!isEditMode) isEditMode = true
                                }
                            )
                            Spacer(Modifier.height(12.dp))
                        }
                    }
                }

                // 编辑模式 — 底部添加栏
                if (isEditMode) {
                    EditModeBottomBar(
                        onAddText = {
                            pushUndo()
                            val lastBlock = blocks.lastOrNull()
                            if (lastBlock?.type == "text") {
                                // 已有文字块 → 末尾追加换行，像普通文本编辑一样连续输入
                                val updated = blocks.toMutableList()
                                updated[updated.size - 1] = lastBlock.copy(data = lastBlock.data + "\n")
                                blocks = updated
                            } else {
                                // 没有文字块 → 新建一个
                                blocks = blocks + ContentBlock(type = "text", data = "")
                            }
                        },
                        onAddImage = {
                            imagePickerLauncher.launch("image/*")
                        },
                        onAddVoice = {
                            showRecordingDialog = true
                        }
                    )
                }

                // 录音对话框
                if (showRecordingDialog) {
                    RecordingDialog(
                        context = context,
                        onRecorded = { filePath ->
                            pushUndo()
                            blocks = blocks + ContentBlock(type = "voice", data = filePath)
                            showRecordingDialog = false
                        },
                        onDismiss = { showRecordingDialog = false }
                    )
                }
            }
        }
    }
}

// ====================================================================
// 内容块视图（阅读模式 + 编辑模式）
// ====================================================================

@Composable
private fun ContentBlockView(
    block: ContentBlock,
    isEditMode: Boolean,
    onUpdate: (String) -> Unit,
    onDelete: () -> Unit,
    onDoubleClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = if (isEditMode) 2.dp else 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 类型标签标题（图片/录音块显示）
            val label = when (block.type) {
                "image" -> "📷 图片"
                "voice" -> "🎤 录音"
                else -> null
            }
            if (label != null) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = KleinBlue.copy(alpha = 0.08f),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Text(
                        text = label,
                        fontSize = 12.sp,
                        color = KleinBlue,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            when (block.type) {
                "text" -> {
                    if (isEditMode) {
                        OutlinedTextField(
                            value = block.data,
                            onValueChange = onUpdate,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = KleinBlue,
                                unfocusedBorderColor = Color.Transparent,
                                focusedContainerColor = Parchment50,
                                unfocusedContainerColor = Parchment50
                            ),
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                fontFamily = SerifFamily,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            placeholder = { Text("输入文字...", fontFamily = SerifFamily, color = Ink400) }
                        )
                    } else {
                        Text(
                            text = block.data.ifBlank { "（空白）" },
                            fontFamily = SerifFamily,
                            fontSize = 16.sp,
                            lineHeight = 28.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = !isEditMode) { onDoubleClick() }
                        )
                    }
                }
                "image" -> ImageBlockView(block.data, isEditMode, onUpdate)
                "voice" -> VoiceBlockView(block.data, isEditMode, onUpdate)
            }

            // 编辑模式 — 删除按钮
            if (isEditMode) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.textButtonColors(contentColor = Vermilion)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("删除", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

// ====================================================================
// 图片块
// ====================================================================

@Composable
private fun ImageBlockView(
    filePath: String,
    isEditMode: Boolean,
    onUpdate: (String) -> Unit
) {
    val context = LocalContext.current
    var currentPath by remember { mutableStateOf(filePath) }

    // 图片选择器
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val imgDir = File(context.filesDir, "captures")
                imgDir.mkdirs()
                val destFile = File(imgDir, "img_${System.currentTimeMillis()}.jpg")
                context.contentResolver.openInputStream(it)?.use { input ->
                    destFile.outputStream().use { output -> input.copyTo(output) }
                }
                currentPath = destFile.absolutePath
                onUpdate(destFile.absolutePath)
            } catch (_: Exception) {}
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (currentPath.isNotBlank()) {
            val file = File(currentPath)
            if (file.exists()) {
                Image(
                    painter = rememberAsyncImagePainter(model = file),
                    contentDescription = "图片",
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 150.dp, max = 300.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(enabled = !isEditMode) { },
                    contentScale = ContentScale.Fit
                )
            } else {
                Text("图片文件不存在", fontSize = 13.sp, color = Ink400)
            }
        } else {
            Text("（无图片）", fontSize = 13.sp, color = Ink400)
        }

        if (isEditMode) {
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { galleryLauncher.launch("image/*") },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = KleinBlueBg, contentColor = KleinBlue)
            ) {
                Icon(Icons.Outlined.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("选择图片", fontSize = 13.sp)
            }
        }
    }
}

// ====================================================================
// 录音块
// ====================================================================

@Composable
private fun VoiceBlockView(
    filePath: String,
    isEditMode: Boolean,
    onUpdate: (String) -> Unit
) {
    val context = LocalContext.current

    // 解析文件路径和转文字结果（格式: 文件路径|||转文字内容）
    val SEPARATOR = "|||"
    val actualPath = if (filePath.contains(SEPARATOR)) filePath.substringBefore(SEPARATOR) else filePath
    val transcribedText = if (filePath.contains(SEPARATOR)) filePath.substringAfter(SEPARATOR) else ""

    var currentPath by remember { mutableStateOf(actualPath) }
    var currentTranscribed by remember { mutableStateOf(transcribedText) }

    // 转文字状态
    var isTranscribing by remember { mutableStateOf(false) }
    var transcribeError by remember { mutableStateOf<String?>(null) }

    // MediaPlayer 状态
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }
    var durationMs by remember { mutableIntStateOf(0) }
    var currentMs by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    // SpeechRecognizer
    var speechRecognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.release()
            speechRecognizer?.destroy()
        }
    }

    // 进度更新
    if (isPlaying) {
        LaunchedEffect(Unit) {
            while (isPlaying) {
                mediaPlayer?.let { currentMs = it.currentPosition }
                delay(200)
            }
        }
    }

    // 语音转文字权限申请
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            // 开始转文字流程
            isTranscribing = true
            transcribeError = null
            val sr = SpeechRecognizer.createSpeechRecognizer(context)
            speechRecognizer = sr
            sr.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onError(error: Int) {
                    isTranscribing = false
                    transcribeError = "识别失败（错误码: $error）"
                    sr.destroy()
                }
                override fun onResults(results: Bundle?) {
                    isTranscribing = false
                    val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                    if (!text.isNullOrBlank()) {
                        currentTranscribed = text
                        onUpdate("$currentPath$SEPARATOR$text")
                        transcribeError = null
                    } else {
                        transcribeError = "未能识别到文字"
                    }
                    sr.destroy()
                }
                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
            // 启动语音识别（同时播放音频，通过扬声器+麦克风转写）
            val recognizeIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.CHINESE)
                putExtra(RecognizerIntent.EXTRA_PROMPT, "正在转写录音...")
            }
            sr.startListening(recognizeIntent)
            // 播放录音（声音会通过麦克风被 SpeechRecognizer 捕获）
            if (mediaPlayer == null) {
                try {
                    mediaPlayer = MediaPlayer().apply {
                        setDataSource(currentPath)
                        prepare()
                        durationMs = duration
                        start()
                        isPlaying = true
                        setOnCompletionListener {
                            // 等待识别完成
                        }
                    }
                } catch (_: Exception) {
                    isTranscribing = false
                    transcribeError = "无法播放录音文件"
                }
            } else {
                mediaPlayer?.start()
                isPlaying = true
            }
        } else {
            transcribeError = "需要录音权限才能转文字"
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val file = File(currentPath)
        if (currentPath.isNotBlank() && file.exists()) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(KleinBlueBg, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(if (isPlaying) "🔊" else "🎤", fontSize = 32.sp)
            }
            Spacer(Modifier.height(8.dp))
            if (durationMs > 0) {
                Slider(
                    value = currentMs.toFloat(),
                    onValueChange = { mediaPlayer?.seekTo(it.toInt()); currentMs = it.toInt() },
                    valueRange = 0f..durationMs.toFloat(),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    colors = SliderDefaults.colors(thumbColor = KleinBlue, activeTrackColor = KleinBlue, inactiveTrackColor = Parchment100)
                )
            }
            Text(
                text = "${formatTime(currentMs)} / ${formatTime(durationMs)}",
                fontSize = 12.sp, color = Ink600
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Surface(
                    shape = CircleShape, color = KleinBlue,
                    modifier = Modifier.size(48.dp).clickable {
                        if (isPlaying) {
                            mediaPlayer?.pause()
                            isPlaying = false; isPaused = true
                        } else if (mediaPlayer == null) {
                            mediaPlayer = MediaPlayer().apply {
                                try {
                                    setDataSource(currentPath)
                                    prepare()
                                    durationMs = duration
                                    setOnCompletionListener { isPlaying = false; isPaused = false; currentMs = 0 }
                                    start(); isPlaying = true
                                } catch (_: Exception) {}
                            }
                        } else {
                            mediaPlayer?.start()
                            isPlaying = true; isPaused = false
                        }
                    }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                    }
                }
                if (isPlaying || isPaused) {
                    Surface(
                        shape = CircleShape, color = Parchment100,
                        modifier = Modifier.size(40.dp).clickable {
                            mediaPlayer?.apply { stop(); prepareAsync() }
                            isPlaying = false; isPaused = false; currentMs = 0
                        }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Stop, contentDescription = null, tint = Ink600, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }

            // 显示转文字结果
            if (currentTranscribed.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Parchment50,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("📝 转文字", fontSize = 12.sp, color = KleinBlue, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.height(4.dp))
                        Text(currentTranscribed, fontSize = 14.sp, color = Ink900)
                    }
                }
            }

            // 转文字错误提示
            if (transcribeError != null) {
                Spacer(Modifier.height(6.dp))
                Text(transcribeError!!, fontSize = 12.sp, color = Vermilion)
            }

        } else {
            Text("（无录音）", fontSize = 13.sp, color = Ink400)
        }

        if (isEditMode && currentPath.isNotBlank() && file.exists()) {
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 转文字按钮
                Button(
                    onClick = {
                        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO)
                            != PackageManager.PERMISSION_GRANTED
                        ) {
                            permissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                        } else {
                            isTranscribing = true
                            transcribeError = null
                            val sr = SpeechRecognizer.createSpeechRecognizer(context)
                            speechRecognizer = sr
                            sr.setRecognitionListener(object : RecognitionListener {
                                override fun onReadyForSpeech(params: Bundle?) {}
                                override fun onBeginningOfSpeech() {}
                                override fun onRmsChanged(rmsdB: Float) {}
                                override fun onBufferReceived(buffer: ByteArray?) {}
                                override fun onEndOfSpeech() {}
                                override fun onError(error: Int) {
                                    isTranscribing = false
                                    transcribeError = "识别失败（错误码: $error）"
                                    sr.destroy()
                                }
                                override fun onResults(results: Bundle?) {
                                    isTranscribing = false
                                    val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                                    if (!text.isNullOrBlank()) {
                                        currentTranscribed = text
                                        onUpdate("$currentPath$SEPARATOR$text")
                                        transcribeError = null
                                    } else {
                                        transcribeError = "未能识别到文字"
                                    }
                                    sr.destroy()
                                }
                                override fun onPartialResults(partialResults: Bundle?) {}
                                override fun onEvent(eventType: Int, params: Bundle?) {}
                            })
                            val recognizeIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.CHINESE)
                                putExtra(RecognizerIntent.EXTRA_PROMPT, "正在转写录音...")
                            }
                            sr.startListening(recognizeIntent)
                            if (mediaPlayer == null) {
                                try {
                                    mediaPlayer = MediaPlayer().apply {
                                        setDataSource(currentPath)
                                        prepare()
                                        durationMs = duration
                                        start()
                                        isPlaying = true
                                        setOnCompletionListener { /* 等待识别完成 */ }
                                    }
                                } catch (_: Exception) {
                                    isTranscribing = false
                                    transcribeError = "无法播放录音文件"
                                }
                            } else {
                                mediaPlayer?.start()
                                isPlaying = true
                            }
                        }
                    },
                    enabled = !isTranscribing,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = KleinBlueBg, contentColor = KleinBlue)
                ) {
                    if (isTranscribing) {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                    } else {
                        Text("🎙", fontSize = 16.sp)
                    }
                    Spacer(Modifier.width(4.dp))
                    Text(if (isTranscribing) "转写中..." else "转文字", fontSize = 13.sp)
                }
            }
        }
    }
}

// ====================================================================
// 编辑模式底部栏
// ====================================================================

@Composable
private fun EditModeBottomBar(
    onAddText: () -> Unit,
    onAddImage: () -> Unit,
    onAddVoice: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp)
                .navigationBarsPadding(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 添加文字
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    shape = CircleShape, color = KleinBlueBg,
                    modifier = Modifier.size(44.dp).clickable(onClick = onAddText)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.TextFields, contentDescription = "添加文字", tint = KleinBlue, modifier = Modifier.size(22.dp))
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text("文字", fontSize = 10.sp, color = Ink600)
            }

            // 添加图片
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    shape = CircleShape, color = KleinBlueBg,
                    modifier = Modifier.size(44.dp).clickable(onClick = onAddImage)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.Image, contentDescription = "添加图片", tint = KleinBlue, modifier = Modifier.size(22.dp))
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text("图片", fontSize = 10.sp, color = Ink600)
            }

            // 添加录音
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    shape = CircleShape, color = KleinBlueBg,
                    modifier = Modifier.size(44.dp).clickable(onClick = onAddVoice)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.Mic, contentDescription = "添加录音", tint = KleinBlue, modifier = Modifier.size(22.dp))
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text("录音", fontSize = 10.sp, color = Ink600)
            }
        }
    }
}

// ====================================================================
// 录音对话框
// ====================================================================

@Composable
private fun RecordingDialog(
    context: android.content.Context,
    onRecorded: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var isRecording by remember { mutableStateOf(false) }
    var elapsedMs by remember { mutableStateOf(0L) }
    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var recordedFilePath by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val startRecording: () -> Unit = {
        try {
            val recordDir = File(context.filesDir, "recordings")
            recordDir.mkdirs()
            val file = File(recordDir, "voice_${System.currentTimeMillis()}.3gp")
            recordedFilePath = file.absolutePath

            val r = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                MediaRecorder()
            }
            r.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
            recorder = r
            isRecording = true
            val startTime = System.currentTimeMillis()
            scope.launch {
                while (isRecording) {
                    delay(100)
                    elapsedMs = System.currentTimeMillis() - startTime
                }
            }
        } catch (_: Exception) {}
    }

    val stopRecording: () -> Unit = {
        recorder?.apply {
            try {
                stop()
                release()
            } catch (_: Exception) {}
        }
        recorder = null
        isRecording = false
    }

    // 录音权限申请
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startRecording()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            recorder?.apply {
                try { stop() } catch (_: Exception) {}
                release()
            }
        }
    }

    AlertDialog(
        onDismissRequest = {
            if (isRecording) stopRecording()
            onDismiss()
        },
        title = { Text("录音") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (recordedFilePath != null && !isRecording) {
                    Text("✅ 录音完成", fontSize = 18.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("${elapsedMs / 1000} 秒", fontSize = 14.sp, color = Ink600)
                } else if (isRecording) {
                    Text("🔴 录音中", fontSize = 18.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("${elapsedMs / 1000} 秒", fontSize = 14.sp, color = Vermilion)
                } else {
                    Text("🎤 点击下方按钮开始录音", fontSize = 14.sp, color = Ink600)
                }
                Spacer(Modifier.height(16.dp))
                Surface(
                    shape = CircleShape,
                    color = if (isRecording) Vermilion else KleinBlue,
                    modifier = Modifier
                        .size(64.dp)
                        .clickable {
                            if (isRecording) {
                                stopRecording()
                            } else {
                                val perm = android.Manifest.permission.RECORD_AUDIO
                                if (ContextCompat.checkSelfPermission(context, perm)
                                    != PackageManager.PERMISSION_GRANTED
                                ) {
                                    permissionLauncher.launch(perm)
                                } else {
                                    startRecording()
                                }
                            }
                        }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            if (isRecording) "■" else "🎤",
                            fontSize = 24.sp,
                            color = Color.White
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (recordedFilePath != null && !isRecording) {
                Button(
                    onClick = { onRecorded(recordedFilePath!!) },
                    colors = ButtonDefaults.buttonColors(containerColor = KleinBlue)
                ) { Text("确定添加") }
            } else {
                TextButton(onClick = {
                    if (isRecording) stopRecording()
                    onDismiss()
                }) { Text("取消") }
            }
        },
        dismissButton = {
            if (recordedFilePath != null && !isRecording) {
                TextButton(onClick = {
                    recordedFilePath = null
                    elapsedMs = 0
                }) { Text("重新录制") }
            }
        }
    )
}

// ====================================================================
// 导出格式选择对话框
// ====================================================================

@Composable
private fun ExportFormatDialog(
    onExportText: () -> Unit,
    onExportWord: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("导出灵感") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = KleinBlueBg,
                    modifier = Modifier.fillMaxWidth().clickable(onClick = onExportText).padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("📝", fontSize = 24.sp)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("纯文本 (.txt)", fontSize = 15.sp, fontWeight = FontWeight.Medium)
                            Text("仅导出文字内容，图片标记为 [图片]", fontSize = 12.sp, color = Ink600)
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = KleinBlueBg,
                    modifier = Modifier.fillMaxWidth().clickable(onClick = onExportWord).padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("📄", fontSize = 24.sp)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Word 文档 (.doc)", fontSize = 15.sp, fontWeight = FontWeight.Medium)
                            Text("导出文字和图片，可在 Word 中打开", fontSize = 12.sp, color = Ink600)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

// ====================================================================
// 导出工具函数
// ====================================================================

private fun exportAsText(blocks: List<ContentBlock>, context: android.content.Context) {
    val text = blocks.joinToString("\n\n") { block ->
        when (block.type) {
            "text" -> block.data
            "image" -> "[图片]"
            "voice" -> "[录音]"
            else -> ""
        }
    }
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text.ifBlank { "无内容" })
        putExtra(Intent.EXTRA_SUBJECT, "灵感导出")
    }
    context.startActivity(Intent.createChooser(shareIntent, "导出灵感"))
}

private fun exportAsWord(blocks: List<ContentBlock>, context: android.content.Context) {
    val html = buildString {
        append("""<html><head><meta charset="utf-8"><title>灵感导出</title></head><body style="font-family: serif; font-size: 14pt; line-height: 1.8; padding: 20px;">""")
        blocks.forEach { block ->
            when (block.type) {
                "text" -> {
                    val escaped = block.data
                        .replace("&", "&amp;")
                        .replace("<", "&lt;")
                        .replace(">", "&gt;")
                        .replace("\n", "<br>")
                    append("<p>$escaped</p>")
                }
                "image" -> {
                    val file = File(block.data)
                    if (file.exists()) {
                        try {
                            val bitmap = BitmapFactory.decodeFile(block.data)
                            if (bitmap != null) {
                                // 缩放至合适大小
                                val maxDim = 1024f
                                val scale = minOf(maxDim / bitmap.width, maxDim / bitmap.height, 1f)
                                val w = (bitmap.width * scale).toInt()
                                val h = (bitmap.height * scale).toInt()
                                val scaled = Bitmap.createScaledBitmap(bitmap, w, h, true)
                                val baos = ByteArrayOutputStream()
                                scaled.compress(Bitmap.CompressFormat.JPEG, 80, baos)
                                val base64 = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
                                append("<p><img src='data:image/jpeg;base64,$base64' style='max-width:100%'/></p>")
                                if (scaled !== bitmap) scaled.recycle()
                                bitmap.recycle()
                            }
                        } catch (_: Exception) {
                            append("<p>[图片加载失败]</p>")
                        }
                    } else {
                        append("<p>[图片文件不存在]</p>")
                    }
                }
                "voice" -> append("<p>[录音文件]</p>")
            }
        }
        append("</body></html>")
    }
    // 保存为 .doc（Word 可打开 HTML）
    val file = File(context.cacheDir, "export_${System.currentTimeMillis()}.doc")
    file.writeText(html, Charsets.UTF_8)
    val uri = androidx.core.content.FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "application/msword"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(shareIntent, "导出 Word"))
}

// ====================================================================
// 工具函数
// ====================================================================

private fun formatTime(ms: Int): String {
    val totalSec = ms / 1000
    return "%d:%02d".format(totalSec / 60, totalSec % 60)
}
