package com.yjtzc.bluelink.ui.mine

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yjtzc.bluelink.ui.mine.components.MineSectionTitle
import com.yjtzc.bluelink.ui.theme.DangerPale
import com.yjtzc.bluelink.ui.theme.DangerRed
import com.yjtzc.bluelink.ui.theme.Ink600
import com.yjtzc.bluelink.ui.theme.Ink900
import com.yjtzc.bluelink.ui.theme.KleinBlue
import com.yjtzc.bluelink.ui.theme.Parchment100
import com.yjtzc.bluelink.ui.theme.SerifFamily

/**
 * 我的总览页（Tab 主页面）
 *
 * 展示身份卡、认知设置概览、隐私与安全概览、数据管理入口。
 * 采用「设置-总览2」结构，各设置项合并在大卡片内以减少碎片感。
 */
@Composable
fun MineScreen(
    viewModel: MineViewModel,
    modifier: Modifier = Modifier,
    onNavigateToAppearance: () -> Unit = {},
    onNavigateToCognitive: () -> Unit = {},
    onNavigateToPrivacySecurity: () -> Unit = {},
    onNavigateToPermission: () -> Unit = {},
    onNavigateToDataExport: () -> Unit = {},
    onNavigateToPermanentDelete: () -> Unit = {}
) {
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // 页面标题
            item {
                Text(
                    text = "我的",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = SerifFamily,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = Ink900,
                    modifier = Modifier.padding(bottom = 16.dp, top = 8.dp)
                )
            }

            // ====== 身份卡 ======
            item {
                IdentityCard(
                    onEditClick = {
                        // 暂不跳转正式页面
                    }
                )
            }

            // ====== 认知设置 ======
            item { MineSectionTitle("认知设置") }

            item {
                CognitiveSummaryCard(
                    granularity = granularityLabel(profile.defaultGranularity),
                    directness = directnessLabel(profile.directnessLevel),
                    exploreDepth = if (profile.exploreDepth) "已开启" else "已关闭",
                    companionStyle = companionStyleLabel(profile.companionStyle),
                    onClick = onNavigateToCognitive
                )
            }

            // ====== 外观 ======
            item { MineSectionTitle("外观") }

            item {
                AppearanceCard(onClick = onNavigateToAppearance)
            }

            // ====== 隐私与安全 ======
            item { MineSectionTitle("隐私与安全") }

            item {
                PrivacySummaryCard(
                    privacyMode = privacyModeLabel(profile.privacyMode),
                    onClickPrivacySecurity = onNavigateToPrivacySecurity,
                    onClickPermission = onNavigateToPermission,
                    onClickDataExport = onNavigateToDataExport
                )
            }

            // ====== 数据管理 ======
            item { MineSectionTitle("数据管理") }

            item {
                DangerActionCard(onClick = onNavigateToPermanentDelete)
            }

            // 底部留白
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

// ============================================================
// 内部组件
// ============================================================

/**
 * 身份卡：链环图标 + 本机知识库名称 + 匿名身份标示 + 编辑入口
 */
@Composable
private fun IdentityCard(onEditClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = KleinBlue.copy(alpha = 0.06f),
        border = BorderStroke(1.dp, Parchment100),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Link,
                contentDescription = null,
                tint = KleinBlue,
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "本机知识库",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = SerifFamily
                    ),
                    color = Ink900
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "匿名身份 · 本地优先",
                    style = MaterialTheme.typography.bodySmall,
                    color = Ink600
                )
            }
            IconButton(onClick = onEditClick) {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = "编辑",
                    tint = KleinBlue,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * 认知设置摘要卡片：合并显示4项认知设置
 */
@Composable
private fun CognitiveSummaryCard(
    granularity: String,
    directness: String,
    exploreDepth: String,
    companionStyle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, Parchment100)
    ) {
        Column {
            SummaryRow(title = "默认溯源粒度", value = granularity)
            HorizontalDivider(color = Parchment100, thickness = 0.5.dp)
            SummaryRow(title = "提示明确度", value = directness)
            HorizontalDivider(color = Parchment100, thickness = 0.5.dp)
            SummaryRow(title = "探索深度", value = exploreDepth)
            HorizontalDivider(color = Parchment100, thickness = 0.5.dp)
            SummaryRow(title = "伴读风格", value = companionStyle)
        }
    }
}

/**
 * 外观设置卡片：仅单行
 */
@Composable
private fun AppearanceCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, Parchment100)
    ) {
        SummaryRow(title = "主题与字体", value = "主题 / 字号 / 对比度")
    }
}

/**
 * 隐私与安全摘要卡片：合并显示3项
 */
@Composable
private fun PrivacySummaryCard(
    privacyMode: String,
    onClickPrivacySecurity: () -> Unit,
    onClickPermission: () -> Unit,
    onClickDataExport: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, Parchment100)
    ) {
        Column {
            SummaryRow(
                title = "隐私模式",
                value = privacyMode,
                onClick = onClickPrivacySecurity
            )
            HorizontalDivider(color = Parchment100, thickness = 0.5.dp)
            SummaryRow(
                title = "权限管理",
                value = "相机 / 麦克风 / 通知",
                onClick = onClickPermission
            )
            HorizontalDivider(color = Parchment100, thickness = 0.5.dp)
            SummaryRow(
                title = "数据导出",
                value = "加密导出本地数据",
                onClick = onClickDataExport
            )
        }
    }
}

/**
 * 危险操作卡片：永久删除入口
 */
@Composable
private fun DangerActionCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = DangerPale),
        border = BorderStroke(1.dp, DangerRed.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "永久删除所有数据",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontFamily = SerifFamily,
                        fontWeight = FontWeight.Medium
                    ),
                    color = DangerRed
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "此操作不可撤销",
                    style = MaterialTheme.typography.bodySmall,
                    color = DangerRed.copy(alpha = 0.7f)
                )
            }
        }
    }
}

/**
 * 摘要卡片内单行：左侧标题 + 值
 */
@Composable
private fun SummaryRow(
    title: String,
    value: String,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall.copy(
                fontFamily = SerifFamily,
                fontWeight = FontWeight.Medium
            ),
            color = Ink900
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = Ink600,
            modifier = Modifier.weight(1f)
        )
    }
}

// ============================================================
// 标签格式化辅助函数
// ============================================================

private fun granularityLabel(value: String): String = when (value) {
    "SENTENCE" -> "句词级"
    "PARAGRAPH" -> "文章级"
    else -> value
}

private fun directnessLabel(value: Float): String = when {
    value < 0.3f -> "温和启发"
    value < 0.7f -> "均衡"
    else -> "直指核心"
}

private fun companionStyleLabel(value: String): String = when (value) {
    "GENTLE" -> "温和启发"
    "DIRECT" -> "直指核心"
    else -> value
}

private fun privacyModeLabel(value: String): String = when (value) {
    "LOCAL_ONLY" -> "仅本地"
    "LOCAL_FIRST" -> "优先本地"
    "CLOUD_OK" -> "允许云端"
    else -> value
}
