package com.yjtzc.bluelink.ui.mine

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yjtzc.bluelink.ui.mine.components.MineNavScaffold
import com.yjtzc.bluelink.ui.mine.components.MineSectionTitle
import com.yjtzc.bluelink.ui.theme.Ink600
import com.yjtzc.bluelink.ui.theme.Ink900
import com.yjtzc.bluelink.ui.theme.KleinBlue
import com.yjtzc.bluelink.ui.theme.Parchment100
import com.yjtzc.bluelink.ui.theme.SerifFamily
import org.json.JSONArray

private val ALL_TERMINOLOGY_TAGS = listOf("计算机", "认知科学", "论文阅读", "产品设计")

@Composable
fun CognitiveSettingsScreen(
    viewModel: MineViewModel,
    onBack: () -> Unit
) {
    val profile by viewModel.profile.collectAsStateWithLifecycle()

    // 解析术语标签
    val selectedTags = remember(profile.terminologyTags) {
        try {
            val arr = JSONArray(profile.terminologyTags)
            (0 until arr.length()).map { arr.getString(it) }.toSet()
        } catch (_: Exception) {
            emptySet()
        }
    }

    MineNavScaffold(
        title = "认知设置",
        onBack = onBack
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // 说明文案
            item {
                Text(
                    text = "调整蓝链提供线索的方式，让 AI 更像索引，而不是答案生成器。",
                    style = MaterialTheme.typography.bodySmall,
                    color = Ink600,
                    modifier = Modifier.padding(bottom = 20.dp, start = 4.dp)
                )
            }

            // ====== 溯源粒度 ======
            item { MineSectionTitle("溯源粒度") }

            item {
                GranularityCard(
                    current = profile.defaultGranularity,
                    onSelect = viewModel::setGranularity
                )
                Spacer(Modifier.height(32.dp))
            }

            // ====== 提示风格 ======
            item { MineSectionTitle("提示风格") }

            item {
                DirectnessCard(
                    value = profile.directnessLevel,
                    onValueChange = viewModel::setDirectness
                )
                Spacer(Modifier.height(32.dp))
            }

            // ====== 探索行为 ======
            item { MineSectionTitle("探索行为") }

            item {
                ExploreDepthCard(
                    enabled = profile.exploreDepth,
                    onToggle = viewModel::toggleExploreDepth
                )
                Spacer(Modifier.height(32.dp))
            }

            // ====== 伴读风格 ======
            item { MineSectionTitle("伴读风格") }

            item {
                CompanionStyleCard(
                    current = profile.companionStyle,
                    onSelect = viewModel::setCompanionStyle
                )
                Spacer(Modifier.height(32.dp))
            }

            // ====== 领域术语 ======
            item { MineSectionTitle("领域术语") }

            item {
                TerminologyCard(
                    selectedTags = selectedTags,
                    onTagsChanged = { tags ->
                        val json = JSONArray(tags.toList()).toString()
                        viewModel.setTerminologyTags(json)
                    }
                )
                Spacer(Modifier.height(80.dp))
            }
        }
    }
}

// ====== 溯源粒度 ======

@Composable
private fun GranularityCard(
    current: String,
    onSelect: (String) -> Unit
) {
    SettingsCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "默认溯源粒度",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontFamily = SerifFamily,
                        fontWeight = FontWeight.Medium
                    ),
                    color = Ink900
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "AI 返回锚点的定位精度",
                    style = MaterialTheme.typography.bodySmall,
                    color = Ink600
                )
            }
            Spacer(Modifier.width(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = current == "SENTENCE",
                    onClick = { onSelect("SENTENCE") },
                    label = { Text("句词级") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = KleinBlue,
                        selectedLabelColor = MaterialTheme.colorScheme.surface
                    )
                )
                FilterChip(
                    selected = current == "PARAGRAPH",
                    onClick = { onSelect("PARAGRAPH") },
                    label = { Text("文章级") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = KleinBlue,
                        selectedLabelColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        }
    }
}

// ====== 提示明确度 ======

@Composable
private fun DirectnessCard(
    value: Float,
    onValueChange: (Float) -> Unit
) {
    SettingsCard {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "提示明确度",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontFamily = SerifFamily,
                    fontWeight = FontWeight.Medium
                ),
                color = Ink900
            )
            Spacer(Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "温和启发",
                    fontSize = 11.sp,
                    color = Ink600,
                    modifier = Modifier.width(56.dp)
                )
                Slider(
                    value = value,
                    onValueChange = onValueChange,
                    valueRange = 0f..1f,
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = KleinBlue,
                        activeTrackColor = KleinBlue,
                        inactiveTrackColor = Parchment100
                    )
                )
                Text(
                    text = "直指核心",
                    fontSize = 11.sp,
                    color = Ink600,
                    modifier = Modifier.width(56.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = when {
                        value < 0.3f -> "当前：温和启发"
                        value < 0.7f -> "当前：均衡"
                        else -> "当前：直指核心"
                    },
                    fontSize = 11.sp,
                    color = KleinBlue
                )
            }
        }
    }
}

// ====== 探索深度 ======

@Composable
private fun ExploreDepthCard(
    enabled: Boolean,
    onToggle: () -> Unit
) {
    SettingsCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "探索深度",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontFamily = SerifFamily,
                        fontWeight = FontWeight.Medium
                    ),
                    color = Ink900
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "开启后会优先显示关联争议和对应观点",
                    style = MaterialTheme.typography.bodySmall,
                    color = Ink600
                )
            }
            Spacer(Modifier.width(12.dp))
            Switch(
                checked = enabled,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(
                    checkedTrackColor = KleinBlue,
                    checkedThumbColor = MaterialTheme.colorScheme.surface,
                    uncheckedTrackColor = Parchment100,
                    uncheckedThumbColor = Ink600
                )
            )
        }
    }
}

// ====== 伴读风格 ======

@Composable
private fun CompanionStyleCard(
    current: String,
    onSelect: (String) -> Unit
) {
    SettingsCard {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "伴读风格",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontFamily = SerifFamily,
                            fontWeight = FontWeight.Medium
                        ),
                        color = Ink900
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "AI 与你对话的语调偏好",
                        style = MaterialTheme.typography.bodySmall,
                        color = Ink600
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = current == "GENTLE",
                    onClick = { onSelect("GENTLE") },
                    label = { Text("温和启发") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = KleinBlue,
                        selectedLabelColor = MaterialTheme.colorScheme.surface
                    )
                )
                FilterChip(
                    selected = current == "DIRECT",
                    onClick = { onSelect("DIRECT") },
                    label = { Text("直指核心") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = KleinBlue,
                        selectedLabelColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        }
    }
}

// ====== 领域术语 ======

@Composable
private fun TerminologyCard(
    selectedTags: Set<String>,
    onTagsChanged: (Set<String>) -> Unit
) {
    SettingsCard {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "术语偏好",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontFamily = SerifFamily,
                    fontWeight = FontWeight.Medium
                ),
                color = Ink900
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "选择你的专业领域，让 AI 更精准地理解上下文",
                style = MaterialTheme.typography.bodySmall,
                color = Ink600
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ALL_TERMINOLOGY_TAGS.forEach { tag ->
                    val isSelected = tag in selectedTags
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            val updated = if (isSelected) selectedTags - tag
                            else selectedTags + tag
                            onTagsChanged(updated)
                        },
                        label = { Text(tag, fontSize = 13.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = KleinBlue,
                            selectedLabelColor = MaterialTheme.colorScheme.surface
                        )
                    )
                }
            }
        }
    }
}

// ====== 通用卡片 ======

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, Parchment100)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            content = content
        )
    }
}
