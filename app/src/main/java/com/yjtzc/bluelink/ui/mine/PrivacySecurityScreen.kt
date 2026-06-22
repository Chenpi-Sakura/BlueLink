package com.yjtzc.bluelink.ui.mine

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yjtzc.bluelink.ui.mine.components.ConfirmPrivacyChangeDialog
import com.yjtzc.bluelink.ui.mine.components.MineNavScaffold
import com.yjtzc.bluelink.ui.mine.components.PrivacyLevelPickerSheet
import com.yjtzc.bluelink.ui.theme.KleinBlue

private val PageBg = Color(0xFFFAF7F2)
private val CardBg = Color(0xCCFFFDF8)
private val CardBorder = Color(0xEBE5E0D8)
private val DividerColor = Color(0xB8D6CFC4)
private val DangerColor = Color(0xFFC43129)

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

    MineNavScaffold(title = "隐私与安全", onBack = onBack) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // 顶部信息卡
            item {
                HeroInfoCard(mode = profile.privacyMode, onClick = viewModel::showPrivacyLevelPicker)
                Spacer(Modifier.height(22.dp))
            }

            // 三种隐私等级
            item {
                BlockCard(title = "三种隐私等级") {
                    Column {
                        PrivacyOption("LOCAL_ONLY", "仅本地", "LOCAL_ONLY", "原文永不上云", profile.privacyMode, onClick = viewModel::showPrivacyLevelPicker)
                        PrivacyOption("LOCAL_FIRST", "优先本地", "LOCAL_FIRST", "原文默认保留本机，必要时仅上传脱敏索引", profile.privacyMode, onClick = viewModel::showPrivacyLevelPicker)
                        PrivacyOption("CLOUD_OK", "允许云端", "CLOUD_OK", "可用于跨设备同步", profile.privacyMode, onClick = viewModel::showPrivacyLevelPicker)
                    }
                }
                Spacer(Modifier.height(22.dp))
            }

            // 权限状态
            item {
                BlockCard(title = "权限状态") {
                    PermissionStateRow("相机", "已允许")
                    PermissionStateRow("麦克风", "已允许")
                    PermissionStateRow("通知", "已允许")
                    PermissionStateRow("电池优化", "未开启", isGood = false)
                }
                Spacer(Modifier.height(22.dp))
            }

            // 底部操作
            item {
                Column {
                    Button(
                        onClick = onNavigateToDataExport,
                        modifier = Modifier.fillMaxWidth().height(58.dp),
                        shape = RoundedCornerShape(40.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = KleinBlue
                        )
                    ) {
                        Text("导出本地数据", fontSize = 18.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.08.sp)
                    }
                    Spacer(Modifier.height(14.dp))
                    TextButton(
                        onClick = onNavigateToPermanentDelete,
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text("永久删除所有数据", color = DangerColor, fontSize = 18.sp)
                    }
                }
                Spacer(Modifier.height(80.dp))
            }
        }
    }

    if (showPicker) {
        PrivacyLevelPickerSheet(
            currentLevel = profile.privacyMode,
            onLevelSelected = viewModel::onPrivacyLevelSelected,
            onDismiss = viewModel::hidePrivacyLevelPicker
        )
    }
    if (showConfirmDialog) {
        ConfirmPrivacyChangeDialog(
            onConfirm = viewModel::confirmPrivacyChangeToCloud,
            onDismiss = viewModel::cancelPrivacyChange
        )
    }
}

@Composable
private fun HeroInfoCard(mode: String, onClick: () -> Unit) {
    val (label, desc) = when (mode) {
        "LOCAL_ONLY" -> "仅本地 LOCAL_ONLY" to "原文永不上云"
        "LOCAL_FIRST" -> "优先本地 LOCAL_FIRST" to "原文默认保留在本机，必要时仅上传脱敏索引"
        "CLOUD_OK" -> "允许云端 CLOUD_OK" to "可用于跨设备同步"
        else -> mode to ""
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(25.dp),
        color = CardBg,
        border = BorderStroke(1.dp, CardBorder)
    ) {
        Row(
            modifier = Modifier.padding(22.dp, 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 大锁图标
            Box(
                modifier = Modifier.size(88.dp).clip(CircleShape)
                    .background(KleinBlue.copy(alpha = 0.06f)),
                contentAlignment = Alignment.Center
            ) {
                Text("🔒", fontSize = 36.sp)
            }
            Spacer(Modifier.width(20.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "当前模式：$label",
                    fontSize = 16.sp, fontWeight = FontWeight(600),
                    fontFamily = FontFamily.Serif, color = KleinBlue
                )
                Spacer(Modifier.height(4.dp))
                Text(text = desc, fontSize = 14.sp, color = Color(0xFF666666))
            }
        }
    }
}

@Composable
private fun BlockCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        shape = RoundedCornerShape(25.dp), color = CardBg, border = BorderStroke(1.dp, CardBorder)
    ) {
        Column(modifier = Modifier.padding(24.dp, 21.dp), content = content)
    }
}

@Composable
private fun PrivacyOption(
    key: String, label: String, value: String, desc: String, current: String,
    onClick: () -> Unit
) {
    val isSelected = current == key
    Surface(
        modifier = Modifier.fillMaxWidth().height(84.dp).padding(top = 12.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) Color(0xCEECF5FF) else Color(0x80FFFDF8),
        border = BorderStroke(
            if (isSelected) 1.5.dp else 1.dp,
            if (isSelected) KleinBlue else CardBorder
        )
    ) {
        Row(
            modifier = Modifier.padding(start = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, fontSize = 16.sp, color = Color(0xFF10213B))
            Text(value, fontSize = 12.sp, color = KleinBlue, letterSpacing = 0.7.sp, fontFamily = FontFamily.Serif, modifier = Modifier.padding(start = 6.dp))
            Spacer(Modifier.weight(1f))
            Column(modifier = Modifier.padding(end = 18.dp)) {
                Text(desc, fontSize = 13.sp, color = Color(0xFF6B6B6B))
            }
            // Radio
            Box(
                modifier = Modifier.size(24.dp).padding(end = 18.dp).clip(CircleShape)
                    .background(if (isSelected) KleinBlue else Color.Transparent)
                    .then(
                        if (isSelected) Modifier
                        else Modifier.border(2.dp, Color(0xFFB5B0A8), CircleShape)
                    )
            )
        }
    }
}

@Composable
private fun PermissionStateRow(name: String, state: String, isGood: Boolean = true) {
    Row(
        modifier = Modifier.fillMaxWidth().height(41.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(name, fontSize = 16.sp, color = Color(0xFF10213B))
        Spacer(Modifier.weight(1f))
        if (isGood) {
            Surface(
                shape = RoundedCornerShape(40.dp),
                color = KleinBlue.copy(alpha = 0.08f)
            ) {
                Text(state, color = KleinBlue, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
            }
        } else {
            Text(state, color = Color(0xFF777777), fontSize = 15.sp)
        }
    }
    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(DividerColor))
}
