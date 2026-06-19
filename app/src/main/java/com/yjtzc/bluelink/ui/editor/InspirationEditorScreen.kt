package com.yjtzc.bluelink.ui.editor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yjtzc.bluelink.data.local.db.InspirationCardEntity
import com.yjtzc.bluelink.data.repository.CaptureRepository
import com.yjtzc.bluelink.ui.theme.Ink400
import com.yjtzc.bluelink.ui.theme.Ink600
import com.yjtzc.bluelink.ui.theme.Ink900
import com.yjtzc.bluelink.ui.theme.KleinBlue
import com.yjtzc.bluelink.ui.theme.SerifFamily
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 灵感编辑器 — 自动保存（1 秒防抖）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InspirationEditorScreen(
    card: InspirationCardEntity,
    captureRepository: CaptureRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var content by remember { mutableStateOf("") }
    var originalContent by remember { mutableStateOf("") }
    var isLoaded by remember { mutableStateOf(false) }
    var saveStatus by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // 加载内容
    LaunchedEffect(card.id) {
        try {
            val text = captureRepository.readCardContent(card)
            content = text
            originalContent = text
        } catch (_: Exception) {
            content = card.contentSnippet
            originalContent = card.contentSnippet
        }
        isLoaded = true
    }

    // 自动保存（1 秒防抖）
    LaunchedEffect(content) {
        if (isLoaded && content != originalContent) {
            saveStatus = "未保存"
            delay(1000)
            try {
                captureRepository.updateCardContent(card, content)
                originalContent = content
                saveStatus = "已保存"
            } catch (_: Exception) {
                saveStatus = "保存失败"
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "编辑灵感",
                        fontFamily = SerifFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    if (isLoaded) {
                        Text(
                            saveStatus,
                            fontSize = 12.sp,
                            color = when (saveStatus) {
                                "已保存" -> Ink600
                                "保存失败" -> MaterialTheme.colorScheme.error
                                else -> KleinBlue
                            },
                            modifier = Modifier.padding(end = 12.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            if (!isLoaded) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                // 类型标签
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = KleinBlue.copy(alpha = 0.08f),
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Text(
                        text = when (card.type.name) {
                            "TEXT" -> "📝 文字"
                            "VOICE" -> "🎤 录音"
                            "IMAGE" -> "📷 图片"
                            else -> "📝 文字"
                        },
                        fontSize = 12.sp,
                        color = KleinBlue,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                // 可编辑内容
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    ),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        fontFamily = SerifFamily,
                        fontSize = 16.sp,
                        lineHeight = 28.sp,
                        color = Ink900
                    ),
                    placeholder = { Text("开始书写...", fontFamily = SerifFamily, color = Ink400) }
                )
            }
        }
    }
}
