package com.yjtzc.bluelink.ui.mine

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yjtzc.bluelink.ui.mine.components.ConfirmPrivacyChangeDialog
import com.yjtzc.bluelink.ui.mine.components.MineNavScaffold
import com.yjtzc.bluelink.ui.mine.components.MineSectionTitle
import com.yjtzc.bluelink.ui.mine.components.PrivacyLevelPickerSheet
import com.yjtzc.bluelink.ui.theme.DangerRed
import com.yjtzc.bluelink.ui.theme.Ink600
import com.yjtzc.bluelink.ui.theme.Ink900
import com.yjtzc.bluelink.ui.theme.KleinBlue
import com.yjtzc.bluelink.ui.theme.Parchment100
import com.yjtzc.bluelink.ui.theme.SerifFamily

@Composable
fun PrivacySecurityScreen(
    viewModel: MineViewModel,
    onBack: () -> Unit,
    onNavigateToPermission: () -> Unit,
    onNavigateToDataExport: () -> Unit,
    onNavigateToPermanentDelete: () -> Unit
) {
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val showPicker by viewModel.showPrivacyLevelPicker.collectAsStateWithLifecycle()
    val showConfirmDialog by viewModel.showConfirmPrivacyDialog.collectAsStateWithLifecycle()

    MineNavScaffold(
        title = "隐私与安全",
        onBack = onBack
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // 顶部信息卡
            item {
                PrivacyStatusCard(
                    mode = profile.privacyMode
                )
                Spacer(Modifier.height(32.dp))
            }

            // 隐私等级
            item { MineSectionTitle("隐私等级") }

            item {
                PrivacyLevelCard(
                    currentLevel = profile.privacyMode,
                    onClick = viewModel::showPrivacyLevelPicker
                )
                Spacer(Modifier.height(32.dp))
            }

            // 权限
            item { MineSectionTitle("权限") }

            item {
                PermissionStatusCard(onClick = onNavigateToPermission)
                Spacer(Modifier.height(32.dp))
            }

            // 数据管理
            item { MineSectionTitle("数据管理") }

            item {
                DataActionsCard(
                    onClickExport = onNavigateToDataExport,
                    onClickDelete = onNavigateToPermanentDelete
                )
                Spacer(Modifier.height(80.dp))
            }
        }
    }

    // BottomSheet
    if (showPicker) {
        PrivacyLevelPickerSheet(
            currentLevel = profile.privacyMode,
            onLevelSelected = viewModel::onPrivacyLevelSelected,
            onDismiss = viewModel::hidePrivacyLevelPicker
        )
    }

    // 二次确认 Dialog
    if (showConfirmDialog) {
        ConfirmPrivacyChangeDialog(
            onConfirm = viewModel::confirmPrivacyChangeToCloud,
            onDismiss = viewModel::cancelPrivacyChange
        )
    }
}

@Composable
private fun PrivacyStatusCard(mode: String) {
    val (label, desc) = when (mode) {
        "LOCAL_ONLY" -> "仅本地" to "原文永不上云"
        "LOCAL_FIRST" -> "优先本地" to "原文默认保留在本机，必要时仅上传脱敏索引"
        "CLOUD_OK" -> "允许云端" to "可用于跨设备同步"
        else -> mode to ""
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = KleinBlue.copy(alpha = 0.06f),
        border = BorderStroke(1.dp, Parchment100)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Shield,
                contentDescription = null,
                tint = KleinBlue,
                modifier = Modifier.size(32.dp)
            )
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "当前模式：$label",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontFamily = SerifFamily,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = KleinBlue,
                    letterSpacing = 0.5.sp
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = desc,
                    style = MaterialTheme.typography.bodySmall,
                    color = Ink600
                )
            }
        }
    }
}

@Composable
private fun PrivacyLevelCard(
    currentLevel: String,
    onClick: () -> Unit
) {
    val levels = listOf(
        Triple("LOCAL_ONLY", "仅本地", "原文永不上云"),
        Triple("LOCAL_FIRST", "优先本地", "原文默认保留本机，必要时仅上传脱敏索引"),
        Triple("CLOUD_OK", "允许云端", "可用于跨设备同步")
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, Parchment100)
    ) {
        Column {
            levels.forEachIndexed { index, (key, label, desc) ->
                val isSelected = currentLevel == key
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onClick() }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = isSelected,
                        onClick = { onClick() },
                        colors = RadioButtonDefaults.colors(selectedColor = KleinBlue)
                    )
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontFamily = SerifFamily,
                                fontWeight = FontWeight.Medium
                            ),
                            color = if (isSelected) KleinBlue else Ink900
                        )
                        Text(
                            text = desc,
                            style = MaterialTheme.typography.bodySmall,
                            color = Ink600
                        )
                    }
                }
                if (index < levels.lastIndex) {
                    HorizontalDivider(
                        color = Parchment100,
                        thickness = 0.5.dp,
                        modifier = Modifier.padding(start = 48.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionStatusCard(onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, Parchment100)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Lock,
                contentDescription = null,
                tint = KleinBlue,
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "权限管理",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontFamily = SerifFamily,
                        fontWeight = FontWeight.Medium
                    ),
                    color = Ink900
                )
                Text(
                    text = "相机 / 麦克风 / 通知 / 电池优化",
                    style = MaterialTheme.typography.bodySmall,
                    color = Ink600
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = KleinBlue,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun DataActionsCard(
    onClickExport: () -> Unit,
    onClickDelete: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, Parchment100)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClickExport)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.FileDownload,
                    contentDescription = null,
                    tint = KleinBlue,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "导出本地数据",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontFamily = SerifFamily,
                            fontWeight = FontWeight.Medium
                        ),
                        color = Ink900
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = KleinBlue,
                    modifier = Modifier.size(24.dp)
                )
            }
            HorizontalDivider(color = Parchment100, thickness = 0.5.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onClickDelete)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.DeleteForever,
                    contentDescription = null,
                    tint = DangerRed,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "永久删除所有数据",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontFamily = SerifFamily,
                            fontWeight = FontWeight.Medium
                        ),
                        color = DangerRed
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = DangerRed,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
