package com.yjtzc.bluelink.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yjtzc.bluelink.data.remote.dto.AnchorDto
import com.yjtzc.bluelink.ui.theme.Ink400
import com.yjtzc.bluelink.ui.theme.Ink600
import com.yjtzc.bluelink.ui.theme.Ink900
import com.yjtzc.bluelink.ui.theme.KleinBlue
import com.yjtzc.bluelink.ui.theme.KleinBlueBg
import com.yjtzc.bluelink.ui.theme.Parchment200
import com.yjtzc.bluelink.ui.theme.SerifFamily

/**
 * 对话溯源页 — 对齐参考样式（#page-chat）
 */
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    onNavigateToReader: (segmentId: String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val askState by viewModel.askState.collectAsStateWithLifecycle()
    val granularity by viewModel.granularity.collectAsStateWithLifecycle()
    val isFeynmanMode by viewModel.isFeynmanMode.collectAsStateWithLifecycle()
    var inputText by remember { mutableStateOf("") }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ====== 顶部毛玻璃导航栏 ======
            Surface(
                color = MaterialTheme.colorScheme.background.copy(alpha = 0.85f),
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 8.dp)
                            .windowInsetsTopHeight(WindowInsets.statusBars),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { /* 返回首页 */ }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "返回",
                                tint = Ink900
                            )
                        }
                        Spacer(Modifier.weight(1f))
                        Text(
                            "AI 溯源助手",
                            fontFamily = SerifFamily,
                            fontWeight = FontWeight.Bold,
                            color = Ink900,
                            fontSize = 17.sp
                        )
                        Spacer(Modifier.weight(1f))
                        IconButton(onClick = { /* 设置 */ }) {
                            Text("⚙", fontSize = 20.sp, color = Ink600)
                        }
                    }
                    HorizontalDivider(
                        color = Parchment200.copy(alpha = 0.4f),
                        thickness = 0.5.dp
                    )
                }
            }

            // ====== 消息列表 ======
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (messages.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "输入疑问，AI 将帮你找到原文锚点",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Ink400
                            )
                        }
                    }
                }
                items(messages, key = { it.id }) { msg ->
                    if (msg.isUser) {
                        // 用户消息（参考 .message-bubble + brand-accent bg）
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Surface(
                                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 4.dp, bottomStart = 20.dp, bottomEnd = 20.dp),
                                color = KleinBlue,
                                modifier = Modifier.widthIn(max = 300.dp)
                            ) {
                                Text(
                                    text = msg.content,
                                    modifier = Modifier.padding(16.dp),
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    lineHeight = 22.sp
                                )
                            }
                        }
                    } else {
                        // AI 回复（参考 white bg + border + shadow）
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Surface(
                                shape = MaterialTheme.shapes.large,
                                color = Color.White,
                                tonalElevation = 0.dp,
                                shadowElevation = 2.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    // 引言
                                    Text(
                                        text = msg.content,
                                        fontSize = 14.sp,
                                        color = Ink600,
                                        lineHeight = 22.sp,
                                        maxLines = 4,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(Modifier.height(12.dp))

                                    // 锚点列表
                                    msg.anchors.forEach { anchor ->
                                        AnchorCardView(
                                            anchor = anchor,
                                            onClick = { onNavigateToReader(anchor.segmentId) }
                                        )
                                        Spacer(Modifier.height(8.dp))
                                    }

                                    // 费曼报告
                                    msg.feynmanReport?.let { report ->
                                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                                        Text(
                                            text = report.summary,
                                            fontSize = 12.sp,
                                            color = Ink600
                                        )
                                        report.deviations.forEach { dev ->
                                            Text(
                                                text = "• ${dev.explanation}",
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 加载骨架
                if (askState is com.yjtzc.bluelink.ui.common.UiState.Loading) {
                    item {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }
            }

            // ====== 底部输入栏（参考 glass-panel） ======
            Surface(
                color = MaterialTheme.colorScheme.background.copy(alpha = 0.85f),
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    HorizontalDivider(
                        color = Parchment200.copy(alpha = 0.4f),
                        thickness = 0.5.dp
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                            .navigationBarsPadding(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 加号按钮
                        IconButton(onClick = { /* TODO */ }) {
                            Text("+", fontSize = 22.sp, color = Ink600)
                        }

                        // 输入框（参考 rounded-full pill）
                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            placeholder = {
                                Text(
                                    if (isFeynmanMode) "写出你对这个概念的复述..."
                                    else "输入你的疑问...",
                                    fontSize = 13.sp,
                                    color = Ink400
                                )
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(24.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Parchment200,
                                unfocusedBorderColor = Parchment200.copy(alpha = 0.6f),
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            ),
                            maxLines = 4,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp)
                        )

                        Spacer(Modifier.width(8.dp))

                        // 发送按钮（参考 brand-accent circle）
                        Surface(
                            shape = CircleShape,
                            color = KleinBlue,
                            modifier = Modifier.size(44.dp),
                            onClick = {
                                if (inputText.isNotBlank()) {
                                    if (isFeynmanMode) {
                                        val lastAiMsg = messages.lastOrNull { !it.isUser }
                                        if (lastAiMsg != null && lastAiMsg.anchors.isNotEmpty()) {
                                            viewModel.runFeynmanEvaluation(
                                                explanation = inputText,
                                                concept = lastAiMsg.anchors.first().docTitle,
                                                segmentIds = lastAiMsg.anchors.map { it.segmentId }
                                            )
                                        }
                                    } else {
                                        viewModel.askQuestion(inputText)
                                    }
                                    inputText = ""
                                }
                            }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "发送",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        // 粒度切换（折叠在输入栏右侧小按钮）
                        FilterChip(
                            selected = granularity == "SENTENCE",
                            onClick = { viewModel.toggleGranularity() },
                            label = {
                                Text(
                                    if (granularity == "SENTENCE") "句词" else "文章",
                                    fontSize = 10.sp
                                )
                            },
                            modifier = Modifier.padding(start = 4.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = KleinBlueBg
                            )
                        )
                    }
                }
            }
        }
    }
}

/**
 * 锚点卡片 — 对齐参考样式（.anchor-card）
 */
@Composable
fun AnchorCardView(
    anchor: AnchorDto,
    onClick: () -> Unit = {}
) {
    Surface(
        shape = MaterialTheme.shapes.large,
        color = Color.White,
        tonalElevation = 0.dp,
        shadowElevation = 4.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, KleinBlue.copy(alpha = 0.2f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // "溯源引力线" 标签
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(KleinBlue, CircleShape)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "溯源引力线",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = KleinBlue,
                    letterSpacing = 1.sp
                )
            }
            Spacer(Modifier.height(8.dp))
            // 文档标题
            Text(
                anchor.docTitle,
                fontFamily = SerifFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Ink900
            )
            Spacer(Modifier.height(4.dp))
            // 片段摘要
            Text(
                "—— ${anchor.snippet}",
                fontSize = 11.sp,
                color = Ink400
            )
        }
    }
}
