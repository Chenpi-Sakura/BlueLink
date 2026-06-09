package com.yjtzc.bluelink.ui.mine

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * 设置与隐私页（UI&UX §4.7）
 */
@Composable
fun MineScreen(
    viewModel: MineViewModel,
    modifier: Modifier = Modifier
) {
    val profile by viewModel.profile.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // 用户头像区
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        "蓝链用户",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "专业领域：${profile.major.ifBlank { "未设置" }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // === 认知设置 ===
        item { SectionHeader("认知设置") }

        item {
            ListItem(
                headlineContent = { Text("默认溯源粒度") },
                supportingContent = { Text("AI 返回锚点的定位精度") },
                trailingContent = {
                    FilterChip(
                        selected = profile.defaultGranularity == "SENTENCE",
                        onClick = {
                            viewModel.setGranularity(
                                if (profile.defaultGranularity == "SENTENCE") "PARAGRAPH"
                                else "SENTENCE"
                            )
                        },
                        label = {
                            Text(
                                if (profile.defaultGranularity == "SENTENCE") "句词级" else "文章级"
                            )
                        }
                    )
                }
            )
        }

        item {
            ListItem(
                headlineContent = { Text("提示明确度") },
                supportingContent = {
                    Text(
                        when {
                            profile.directnessLevel < 0.3f -> "温和启发"
                            profile.directnessLevel < 0.7f -> "均衡"
                            else -> "直指核心"
                        }
                    )
                },
                trailingContent = {
                    Slider(
                        value = profile.directnessLevel,
                        onValueChange = viewModel::setDirectness,
                        valueRange = 0f..1f,
                        modifier = Modifier.width(120.dp)
                    )
                }
            )
        }

        item {
            ListItem(
                headlineContent = { Text("探索深度") },
                supportingContent = { Text("开启后 AI 会推荐关联争议性文献") },
                trailingContent = {
                    Switch(
                        checked = profile.exploreDepth,
                        onCheckedChange = { viewModel.toggleExploreDepth() }
                    )
                }
            )
        }

        // === 隐私 ===
        item { SectionHeader("隐私与安全") }

        item {
            ListItem(
                headlineContent = { Text("当前隐私模式") },
                supportingContent = {
                    Text(
                        when (profile.privacyMode) {
                            "LOCAL_ONLY" -> "仅本地 — 所有数据不出设备"
                            "LOCAL_FIRST" -> "优先本地 — 向量可上传"
                            else -> profile.privacyMode
                        }
                    )
                }
            )
        }

        item {
            ListItem(
                headlineContent = { Text("数据导出") },
                supportingContent = { Text("将所有本地数据导出为加密 JSON 文件") },
                modifier = Modifier.clickable { viewModel.exportData() }
            )
        }

        item {
            ListItem(
                headlineContent = {
                    Text(
                        "永久删除所有数据",
                        color = MaterialTheme.colorScheme.error
                    )
                },
                supportingContent = { Text("此操作不可撤销") },
                modifier = Modifier.clickable { viewModel.confirmWipe() }
            )
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 20.dp, bottom = 8.dp)
    )
}
