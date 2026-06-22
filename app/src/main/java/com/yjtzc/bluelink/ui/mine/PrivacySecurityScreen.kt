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

private val PageBg = Color(0xFFFAF7F2)
private val CardBg = Color(0xCCFFFDF8)
private val CardBorder = Color(0xEBE5E0D8)
private val KleinBlue = Color(0xFF002FA7)
private val AccentBlue = Color(0xFF0758C7)
private val DeepInk = Color(0xFF10213B)
private val MidGray = Color(0xFF4F535B)
private val DangerColor = Color(0xFFD5312B)
private val DividerColor = Color(0xB8D6CFC4)

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

    MineNavScaffold(title = "隐私与安全", onBack = onBack, titleSize = 27.sp, titleWeight = FontWeight(820)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // 当前模式卡片（无图标，居中）
            item {
                PrivacyHeroCard(mode = profile.privacyMode, onClick = viewModel::showPrivacyLevelPicker)
                Spacer(Modifier.height(20.dp))
            }

            // 三种隐私等级
            item {
                PrivacyLevelCard(
                    currentLevel = profile.privacyMode,
                    onClick = viewModel::showPrivacyLevelPicker
                )
                Spacer(Modifier.height(20.dp))
            }

            // 权限状态
            item {
                PermissionStatusCard(onClick = onNavigateToPermission)
                Spacer(Modifier.height(18.dp))
            }

            // 底部操作区
            item {
                // 导出按钮
                Button(
                    onClick = onNavigateToDataExport,
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize()
                            .background(
                                Brush.horizontalGradient(listOf(Color(0xFF004FD0), Color(0xFF002FA7))),
                                RoundedCornerShape(14.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("⇩  导出本地数据", color = Color.White, fontSize = 18.sp,
                            fontWeight = FontWeight(760), letterSpacing = 0.06.sp)
                    }
                }
                Spacer(Modifier.height(14.dp))
                // 永久删除入口
                Box(
                    modifier = Modifier.fillMaxWidth().height(64.dp)
                        .clickable(onClick = onNavigateToPermanentDelete),
                    contentAlignment = Alignment.Center
                ) {
                    Text("▱  永久删除所有数据", color = DangerColor, fontSize = 18.sp,
                        fontWeight = FontWeight(760), letterSpacing = 0.08.sp)
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

// ====== 当前模式卡片（居中、去图标） ======

@Composable
private fun PrivacyHeroCard(mode: String, onClick: () -> Unit) {
    val (label, desc) = when (mode) {
        "LOCAL_ONLY" -> "仅本地 LOCAL_ONLY" to "原文永不上云"
        "LOCAL_FIRST" -> "优先本地 LOCAL_FIRST" to "原文默认保留在本机，必要时仅上传脱敏索引"
        "CLOUD_OK" -> "允许云端 CLOUD_OK" to "可用于跨设备同步"
        else -> mode to ""
    }
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        color = CardBg,
        border = BorderStroke(1.dp, CardBorder)
    ) {
        Column(
            modifier = Modifier.padding(20.dp, 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "当前模式：",
                fontSize = 16.5.sp, lineHeight = 24.sp,
                fontWeight = FontWeight(700), color = DeepInk,
                fontFamily = FontFamily.Serif
            )
            Text(
                text = label,
                color = KleinBlue,
                fontFamily = FontFamily.Serif,
                fontSize = 16.sp, fontWeight = FontWeight(760),
                letterSpacing = 0.35.sp
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = desc,
                fontSize = 13.5.sp, lineHeight = 21.sp,
                color = Color(0xFF2F3136),
                fontFamily = FontFamily.Serif
            )
        }
    }
}

// ====== 三种隐私等级 ======

@Composable
private fun PrivacyLevelCard(currentLevel: String, onClick: () -> Unit) {
    data class LevelItem(val key: String, val label: String, val value: String, val desc: String)
    val levels = listOf(
        LevelItem("LOCAL_ONLY", "仅本地", "LOCAL_ONLY", "原文永不上云"),
        LevelItem("LOCAL_FIRST", "优先本地", "LOCAL_FIRST", "原文默认保留本机，必要时仅上传脱敏索引"),
        LevelItem("CLOUD_OK", "允许云端", "CLOUD_OK", "可用于跨设备同步")
    )

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = CardBg,
        border = BorderStroke(1.dp, CardBorder)
    ) {
        Column(modifier = Modifier.padding(start = 11.dp, end = 11.dp, top = 18.dp, bottom = 12.dp)) {
            Text(
                text = "三种隐私等级",
                fontSize = 20.sp, lineHeight = 28.sp,
                fontWeight = FontWeight(760), color = Color(0xFF0A2B5F),
                modifier = Modifier.padding(start = 8.dp, bottom = 14.dp)
            )
            levels.forEach { level ->
                val isSelected = currentLevel == level.key
                PrivacyOption(level.key, level.label, level.value, level.desc, isSelected, onClick)
            }
        }
    }
}

@Composable
private fun PrivacyOption(key: String, label: String, value: String, desc: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().height(74.dp).padding(vertical = 5.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(15.dp),
        color = if (isSelected) Color.Transparent else Color(0x8FFFFDF8),
        border = BorderStroke(
            if (isSelected) 1.4.dp else 1.dp,
            if (isSelected) AccentBlue else Color(0xE0D6CFC4)
        )
    ) {
        Box {
            if (isSelected) {
                Box(
                    modifier = Modifier.matchParentSize().background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xDBEDF6FF), Color(0xE0FFFDF8)),
                            start = androidx.compose.ui.geometry.Offset.Zero,
                            end = androidx.compose.ui.geometry.Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                        ),
                        RoundedCornerShape(15.dp)
                    )
                )
            }
            Row(
                modifier = Modifier.fillMaxSize().padding(start = 15.dp, end = 15.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 图标
                Box(modifier = Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                    Text(
                        when (key) {
                            "LOCAL_ONLY" -> "🔒"
                            "LOCAL_FIRST" -> "🛡"
                            else -> "☁"
                        },
                        fontSize = 18.sp
                    )
                }
                Spacer(Modifier.width(14.dp))
                // 文字区
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(label, fontSize = 15.5.sp, fontWeight = FontWeight(700), color = Color(0xFF1F242C))
                        Text(value, fontSize = 12.sp, color = KleinBlue, fontFamily = FontFamily.Serif,
                            letterSpacing = 0.45.sp, modifier = Modifier.padding(start = 5.dp))
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(desc, fontSize = 12.2.sp, color = MidGray)
                }
                // Radio
                Box(
                    modifier = Modifier.size(22.dp).clip(CircleShape)
                        .then(
                            if (isSelected) Modifier.background(AccentBlue)
                            else Modifier.border(2.dp, Color(0xFFB9B1A7), CircleShape)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Box(modifier = Modifier.size(14.dp).clip(CircleShape).background(AccentBlue))
                    }
                }
            }
        }
    }
}

// ====== 权限状态 ======

@Composable
private fun PermissionStatusCard(onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        color = CardBg,
        border = BorderStroke(1.dp, CardBorder)
    ) {
        Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 8.dp)) {
            Text(
                text = "权限状态",
                fontSize = 20.sp, lineHeight = 28.sp,
                fontWeight = FontWeight(760), color = Color(0xFF0A2B5F),
                modifier = Modifier.padding(start = 8.dp, bottom = 14.dp)
            )
            PermRow("📷", "相机", "已允许", true)
            PermRow("🎤", "麦克风", "已允许", true)
            PermRow("🔔", "通知", "已允许", true)
            PermRow("🔋", "电池优化", "未开启", false)
        }
    }
}

@Composable
private fun PermRow(icon: String, name: String, state: String, isGood: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().height(42.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(icon, fontSize = 16.sp)
        Spacer(Modifier.width(13.dp))
        Text(name, fontSize = 15.5.sp, color = Color(0xFF202530), modifier = Modifier.weight(1f))
        // 状态胶囊
        Surface(
            shape = RoundedCornerShape(40.dp),
            color = if (isGood) KleinBlue.copy(alpha = 0.08f) else Color(0xB8E9E5DD),
            modifier = Modifier.height(27.dp).widthIn(min = 64.dp)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 12.dp)) {
                Text(state, color = if (isGood) KleinBlue else Color(0xFF595959), fontSize = 13.sp)
            }
        }
    }
    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(DividerColor))
}
