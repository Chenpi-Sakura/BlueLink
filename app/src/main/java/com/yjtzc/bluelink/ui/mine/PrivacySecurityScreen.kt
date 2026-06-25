package com.yjtzc.bluelink.ui.mine

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yjtzc.bluelink.ui.mine.components.BlueLinkCard
import com.yjtzc.bluelink.ui.mine.components.BlueLinkCardTitle
import com.yjtzc.bluelink.ui.mine.components.BlueLinkPrimaryButton
import com.yjtzc.bluelink.ui.mine.components.scaledFontSize
import com.yjtzc.bluelink.ui.mine.components.BlueLinkPrivacyOption
import com.yjtzc.bluelink.ui.mine.components.BlueLinkSubPageScaffold
import com.yjtzc.bluelink.ui.mine.components.ConfirmPrivacyChangeDialog
import com.yjtzc.bluelink.ui.mine.components.PrivacyLevelPickerSheet
import com.yjtzc.bluelink.ui.navigation.OverlayNavKey
import com.yjtzc.bluelink.ui.theme.MineColors
import com.yjtzc.bluelink.ui.theme.MinePrivacy
import com.yjtzc.bluelink.ui.theme.MineTypography

private val levelOrder = mapOf("LOCAL_ONLY" to 0, "LOCAL_FIRST" to 1, "CLOUD_OK" to 2)

/**
 * 「隐私与安全」子页面。
 *
 * V2.2 Nav3 迁移：3 个 `onNavigateTo*` 回调收敛为单个 `onNavigate: (OverlayNavKey) -> Unit`，
 * 与 [MineScreen] 保持一致的导航接口风格。子项点击统一通过 `onNavigate(targetKey)` 通知父级。
 *
 * @param viewModel MineViewModel（隐私模式 state + 弹窗控制）
 * @param onBack 返回上一级（PrivacySecurity 弹出 back stack）
 * @param onNavigate 子页面跳转（目标：PermissionManagement / DataExport / PermanentDelete）
 */
@Composable
fun PrivacySecurityScreen(
    viewModel: MineViewModel,
    onBack: () -> Unit,
    onNavigate: (OverlayNavKey) -> Unit
) {
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val showPicker by viewModel.showPrivacyLevelPicker.collectAsStateWithLifecycle()
    val showConfirmDialog by viewModel.showConfirmPrivacyDialog.collectAsStateWithLifecycle()

    BlueLinkSubPageScaffold(title = "隐私与安全", onBack = onBack) {
        // 间距
        Spacer(Modifier.height(8.dp))

        // ====== 当前模式卡片（居中、无图标） ======
        val (heroLabel, heroDesc) = when (profile.privacyMode) {
            "LOCAL_ONLY" -> "仅本地 LOCAL_ONLY" to "原文永不上云"
            "LOCAL_FIRST" -> "优先本地 LOCAL_FIRST" to "原文默认保留在本机，必要时仅上传脱敏索引"
            "CLOUD_OK" -> "允许云端 CLOUD_OK" to "可用于跨设备同步"
            else -> profile.privacyMode to ""
        }
        BlueLinkCard(
            contentPadding = PaddingValues(
                horizontal = MinePrivacy.HeroHorizontalPadding,
                vertical = MinePrivacy.HeroVerticalPadding
            )
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().clickable(onClick = viewModel::showPrivacyLevelPicker),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("当前模式：", fontSize = scaledFontSize(MineTypography.PrivacyHeroTitleSize),
                        fontWeight = FontWeight(700), color = MineColors.TextPrimary,
                        fontFamily = FontFamily.Serif)
                }
                Text(heroLabel, color = MineColors.KleinBlue, fontFamily = FontFamily.Serif,
                    fontSize = scaledFontSize(14.5.sp), fontWeight = FontWeight(760), letterSpacing = 0.35.sp)
                Spacer(Modifier.height(10.dp))
                Text(heroDesc, fontSize = scaledFontSize(MineTypography.SmallTextSize),
                    lineHeight = 21.sp, color = Color(0xFF2F3136), fontFamily = FontFamily.Serif)
            }
        }

        Spacer(Modifier.height(MinePrivacy.SectionGap))

        // ====== 三种隐私等级 ======
        val levels = listOf(
            Triple("LOCAL_ONLY", "仅本地", "原文永不上云") to Icons.Outlined.Lock,
            Triple("LOCAL_FIRST", "优先本地", "原文默认保留本机，必要时仅上传脱敏索引") to Icons.Outlined.Shield,
            Triple("CLOUD_OK", "允许云端", "可用于跨设备同步") to Icons.Outlined.Cloud
        )
        BlueLinkCard(contentPadding = PaddingValues(
            start = MinePrivacy.LevelCardPaddingHorizontal,
            end = MinePrivacy.LevelCardPaddingHorizontal,
            top = MinePrivacy.LevelCardPaddingTop,
            bottom = 12.dp
        )) {
            BlueLinkCardTitle("三种隐私等级", modifier = Modifier.padding(start = 8.dp, bottom = 14.dp))
            levels.forEach { (info, icon) ->
                val (key, label, desc) = info
                val selected = profile.privacyMode == key
                BlueLinkPrivacyOption(
                    title = label,
                    code = key,
                    desc = desc,
                    selected = selected,
                    icon = {
                        Icon(icon, contentDescription = null, tint = MineColors.KleinBlue,
                            modifier = Modifier.size(MinePrivacy.IconSize))
                    },
                    onClick = { viewModel.showPrivacyLevelPicker() },
                    modifier = Modifier.padding(vertical = 5.dp)
                )
            }
        }

        Spacer(Modifier.height(MinePrivacy.SectionGap))

        // ====== 权限管理入口 ======
        BlueLinkCard(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 6.dp),
            modifier = Modifier.clickable(onClick = { onNavigate(OverlayNavKey.PermissionManagement) })
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().height(36.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Outlined.Lock, contentDescription = null,
                    tint = MineColors.KleinBlue, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(13.dp))
                Text("权限管理", fontSize = scaledFontSize(14.5.sp), color = MineColors.TextPrimary,
                    fontFamily = FontFamily.Serif, modifier = Modifier.weight(1f))
                Text("›", color = MineColors.KleinBlue, fontSize = scaledFontSize(24.sp), fontWeight = FontWeight.Light)
            }
        }

        Spacer(Modifier.height(MinePrivacy.SectionGap))

        // ====== 底部操作 ======
        BlueLinkPrimaryButton(
            text = "导出本地数据",
            height = MinePrivacy.ExportButtonHeight,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
            icon = { Icon(Icons.Outlined.FileDownload, contentDescription = null,
                tint = Color.White, modifier = Modifier.size(20.dp)) },
            onClick = { onNavigate(OverlayNavKey.DataExport) }
        )

        Spacer(Modifier.height(14.dp))

        // 永久删除（红色文字入口）
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(MinePrivacy.DeleteEntryHeight)
                .clickable(onClick = { onNavigate(OverlayNavKey.PermanentDelete) }),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Outlined.DeleteForever, contentDescription = null,
                    tint = MineColors.Danger, modifier = Modifier.size(20.dp))
                Text("永久删除所有数据", color = MineColors.Danger,
                    fontSize = MineTypography.ButtonTextSize,
                    fontWeight = FontWeight(760), letterSpacing = 0.08.sp)
            }
        }

        Spacer(Modifier.height(80.dp))
    }

    // BottomSheet & Dialog
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
