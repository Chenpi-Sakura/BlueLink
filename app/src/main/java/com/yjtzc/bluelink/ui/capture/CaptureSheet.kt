package com.yjtzc.bluelink.ui.capture

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yjtzc.bluelink.ui.navigation.BlueLinkViewModelFactory
import com.yjtzc.bluelink.util.LocalAppContainer

/**
 * 灵感捕获弹窗 — ModalBottomSheet 三 Tab（UI&UX §4.6）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaptureSheet(onDismiss: () -> Unit) {
    val container = LocalAppContainer.current
    val viewModel: CaptureViewModel = viewModel(
        factory = BlueLinkViewModelFactory(container)
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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
                0 -> {
                    // 文字速记
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
                1 -> {
                    // 语音输入（占位）
                    Text(
                        "长按开始录音...",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                    )
                }
                2 -> {
                    // 拍照/扫描（占位）
                    Text(
                        "点击拍照或选择图片...",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                    )
                }
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
                enabled = state.textContent.isNotBlank() && !state.isSaving
            ) {
                Text(if (state.isSaving) "保存中..." else "收录为灵感卡片")
            }
        }
    }
}
