package com.yjtzc.bluelink.ui.capture

import android.Manifest
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yjtzc.bluelink.ui.navigation.BlueLinkViewModelFactory
import com.yjtzc.bluelink.util.LocalAppContainer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

/**
 * 灵感捕获弹窗 — ModalBottomSheet 三 Tab（UI&UX §4.6）
 * 文字 / 语音 / 拍摄 三种灵感录入方式
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaptureSheet(
    initialTabIndex: Int = 0,
    onDismiss: () -> Unit
) {
    val container = LocalAppContainer.current
    val viewModel: CaptureViewModel = viewModel(
        factory = BlueLinkViewModelFactory(container)
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current

    // 设置初始 Tab
    LaunchedEffect(initialTabIndex) {
        viewModel.setTabIndex(initialTabIndex)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .navigationBarsPadding()
        ) {
            // Tab 切换
            TabRow(selectedTabIndex = state.tabIndex) {
                listOf("文字", "语音", "拍摄").forEachIndexed { i, label ->
                    Tab(
                        selected = state.tabIndex == i,
                        onClick = { viewModel.setTabIndex(i) },
                        text = { Text(label) }
                    )
                }
            }
            Spacer(Modifier.height(16.dp))

            when (state.tabIndex) {
                0 -> TextTab(viewModel, state)
                1 -> VoiceTab(viewModel, state, context)
                2 -> ImageTab(viewModel, state, context)
            }

            Spacer(Modifier.height(12.dp))

            // 私密开关
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("私密（仅本地）", modifier = Modifier.weight(1f))
                Switch(
                    checked = state.isPrivate,
                    onCheckedChange = viewModel::setPrivate
                )
            }

            Spacer(Modifier.height(16.dp))

            // 保存按钮
            Button(
                onClick = { viewModel.saveInspiration(onDismiss) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(24.dp),
                enabled = when (state.tabIndex) {
                    0 -> state.textContent.isNotBlank()
                    1 -> state.audioFilePath != null
                    2 -> state.imageFilePath != null
                    else -> false
                } && !state.isSaving
            ) {
                Text(
                    when {
                        state.isSaving -> "保存中..."
                        state.tabIndex == 0 -> "收录为灵感卡片"
                        state.tabIndex == 1 -> "收录语音灵感"
                        else -> "收录图片灵感"
                    }
                )
            }
        }
    }
}

/**
 * 文字速记 Tab
 */
@Composable
private fun TextTab(viewModel: CaptureViewModel, state: CaptureState) {
    OutlinedTextField(
        value = state.textContent,
        onValueChange = viewModel::setTextContent,
        placeholder = { Text("记录一个想法...") },
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        shape = RoundedCornerShape(12.dp)
    )
}

/**
 * 语音录制 Tab — 使用 MediaRecorder
 */
@Composable
private fun VoiceTab(
    viewModel: CaptureViewModel,
    state: CaptureState,
    context: android.content.Context
) {
    // 录音权限请求
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            // 权限已授予，开始录音由按钮触发
        }
    }

    // MediaRecorder 实例（在 composable 生命周期内保持）
    var mediaRecorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var recordingFile by remember { mutableStateOf<File?>(null) }

    // 录音计时
    var elapsedMs by remember { mutableStateOf(0L) }
    val timer = remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    val scope = rememberCoroutineScope()

    // 清理
    DisposableEffect(Unit) {
        onDispose {
            mediaRecorder?.apply {
                if (state.isRecording) {
                    try { stop() } catch (_: Exception) {}
                }
                release()
            }
            timer.value?.cancel()
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (state.isRecording) {
            // 正在录音 — 显示时长 + 停止按钮
            Text(
                text = "录音中 ${elapsedMs / 1000}s",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(Modifier.height(16.dp))
            FilledTonalButton(
                onClick = {
                    // 停止录音
                    mediaRecorder?.apply {
                        try {
                            stop()
                            release()
                        } catch (_: Exception) {}
                    }
                    mediaRecorder = null
                    timer.value?.cancel()
                    elapsedMs = 0
                    viewModel.setRecording(false)
                    recordingFile?.let { viewModel.setAudioFilePath(it.absolutePath) }
                },
                shape = CircleShape,
                modifier = Modifier.size(72.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(Icons.Default.Stop, contentDescription = "停止录音", tint = MaterialTheme.colorScheme.onError)
            }
        } else if (state.audioFilePath != null) {
            // 录音完成 — 可听状态
            Icon(
                Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "录音已就绪",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = {
                viewModel.setAudioFilePath(null)
                recordingFile = null
            }) {
                Text("重新录制")
            }
        } else {
            // 初始状态 — 开始录音按钮
            Text(
                text = "点击开始录音",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))
            FilledTonalButton(
                onClick = {
                    val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                        Manifest.permission.RECORD_AUDIO
                    else
                        Manifest.permission.RECORD_AUDIO

                    if (androidx.core.content.ContextCompat.checkSelfPermission(
                            context, permission
                        ) != android.content.pm.PackageManager.PERMISSION_GRANTED
                    ) {
                        permissionLauncher.launch(permission)
                        return@FilledTonalButton
                    }

                    // 开始录音
                    try {
                        val recordDir = File(context.filesDir, "recordings")
                        recordDir.mkdirs()
                        val file = File(recordDir, "voice_${System.currentTimeMillis()}.3gp")
                        recordingFile = file

                        val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            MediaRecorder(context)
                        } else {
                            MediaRecorder()
                        }
                        recorder.apply {
                            setAudioSource(MediaRecorder.AudioSource.MIC)
                            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                            setOutputFile(file.absolutePath)
                            prepare()
                            start()
                        }
                        mediaRecorder = recorder
                        viewModel.setRecording(true)
                        viewModel.setAudioFilePath(null)

                        // 计时
                        val startTime = System.currentTimeMillis()
                        timer.value = scope.launch {
                            while (true) {
                                delay(100)
                                elapsedMs = System.currentTimeMillis() - startTime
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                },
                shape = CircleShape,
                modifier = Modifier.size(72.dp)
            ) {
                Icon(Icons.Default.Mic, contentDescription = "开始录音")
            }
        }
    }
}

/**
 * 拍照/选图 Tab
 */
@Composable
private fun ImageTab(
    viewModel: CaptureViewModel,
    state: CaptureState,
    context: android.content.Context
) {
    // 拍照
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            photoUri?.let { uri ->
                // 复制到内部存储
                try {
                    val imgDir = File(context.filesDir, "captures")
                    imgDir.mkdirs()
                    val destFile = File(imgDir, "img_${System.currentTimeMillis()}.jpg")
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        destFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    viewModel.setImageFilePath(destFile.absolutePath)
                } catch (_: Exception) {}
            }
        }
    }

    // 相册选择（使用 GetContent 替代 PickVisualMedia，兼容性更好）
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val imgDir = File(context.filesDir, "captures")
                imgDir.mkdirs()
                val destFile = File(imgDir, "img_${System.currentTimeMillis()}.jpg")
                context.contentResolver.openInputStream(it)?.use { input ->
                    destFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                viewModel.setImageFilePath(destFile.absolutePath)
                photoUri = Uri.fromFile(destFile)
            } catch (_: Exception) {}
        }
    }

    // 相机权限
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted && photoUri != null) {
            cameraLauncher.launch(photoUri!!)
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (state.imageFilePath != null) {
            // 显示已选择的图片
            Image(
                painter = rememberAsyncImagePainter(model = File(state.imageFilePath)),
                contentDescription = "已选图片",
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "图片已就绪",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = { viewModel.setImageFilePath(null) }) {
                Text("重新选择")
            }
        } else {
            // 选择方式
            Text(
                text = "选择图片来源",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 拍照按钮
                FilledTonalButton(
                    onClick = {
                        val imgDir = File(context.filesDir, "captures")
                        imgDir.mkdirs()
                        val file = File(imgDir, "camera_${System.currentTimeMillis()}.jpg")
                        photoUri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            file
                        )
                        val permission = Manifest.permission.CAMERA
                        if (androidx.core.content.ContextCompat.checkSelfPermission(
                                context, permission
                            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
                        ) {
                            cameraPermissionLauncher.launch(permission)
                        } else {
                            cameraLauncher.launch(photoUri!!)
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(100.dp).width(120.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null)
                        Spacer(Modifier.height(4.dp))
                        Text("拍照", style = MaterialTheme.typography.labelMedium)
                    }
                }

                // 相册选择按钮
                FilledTonalButton(
                    onClick = { galleryLauncher.launch("image/*") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(100.dp).width(120.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                        Spacer(Modifier.height(4.dp))
                        Text("相册", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}
