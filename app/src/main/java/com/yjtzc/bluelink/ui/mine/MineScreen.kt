package com.yjtzc.bluelink.ui.mine

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.outlined.CenterFocusStrong
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yjtzc.bluelink.ui.mine.components.MineSectionTitle

// 原型色板
private val RiceWhite = Color(0xFFFAF7F2)
private val CardBg = Color(0xCCFFFDF8)
private val CardBorder = Color(0xEBE5E0D8)
private val DeepBlue = Color(0xFF082653)
private val AccentBlue = Color(0xFF0758C7)
private val IconGray = Color(0xFF5B7EB5)
private val DividerColor = Color(0xB8D6CFC4)
private val SliderInactive = Color(0xFFDEDCD8)
private val SwitchOff = Color(0xFFD8D5CF)
private val DangerColor = Color(0xFFD9605B)

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

    LazyColumn(
        modifier = modifier.fillMaxSize().background(RiceWhite),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 120.dp)
    ) {
        item {
            Text(
                text = "我的",
                color = DeepBlue,
                fontSize = 28.sp,
                fontWeight = FontWeight(780),
                fontFamily = FontFamily.Serif,
                letterSpacing = (-1.2).sp,
                modifier = Modifier.padding(top = 58.dp)
            )
        }
        item {
            HeroIdentityCard(onEditClick = onNavigateToAppearance)
            Spacer(Modifier.height(8.dp))
        }
        item {
            MineSectionTitle("认知设置")
            CognitiveSettingsPanel(
                granularityLabel = granularityLabel(profile.defaultGranularity),
                exploreDepth = profile.exploreDepth,
                companionStyleLabel = companionLabel(profile.companionStyle),
                onClickGranularity = onNavigateToCognitive,
                onExploreDepthToggle = viewModel::toggleExploreDepth,
                onClickCompanion = onNavigateToCognitive
            )
        }
        item {
            MineSectionTitle("隐私与安全")
            PrivacySettingsPanel(
                privacyModeLabel = privacyLabel(profile.privacyMode),
                onClickPrivacy = onNavigateToPrivacySecurity,
                onClickPermission = onNavigateToPermission,
                onClickExport = onNavigateToDataExport
            )
        }
        item {
            MineSectionTitle("数据管理")
            DangerCard(onClick = onNavigateToPermanentDelete)
            Spacer(Modifier.height(80.dp))
        }
    }
}

// ============================================================
// 身份卡
// ============================================================

@Composable
private fun HeroIdentityCard(onEditClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
            .shadow(13.dp, RoundedCornerShape(26.dp), ambientColor = Color(0xFF192436), spotColor = Color(0xFF192436))
    ) {
        Surface(
            modifier = Modifier.matchParentSize(),
            shape = RoundedCornerShape(26.dp),
            color = Color(0xC2FFFDF8),
            border = BorderStroke(1.dp, Color(0xBDDFDAD1))
        ) {
            Box {
                Box(
                    modifier = Modifier.matchParentSize().background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xADE3EDFC), Color(0xB8FFFFFF), Color(0x80DBE8FE)),
                            start = androidx.compose.ui.geometry.Offset.Zero,
                            end = androidx.compose.ui.geometry.Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                        )
                    )
                )
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.padding(start = 24.dp).size(78.dp).offset(y = (-2).dp),
                        shape = CircleShape, color = Color(0xF0FCFAF4),
                        shadowElevation = 8.dp,
                        border = BorderStroke(2.dp, Color(0xBFFFFFFF))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.Link, contentDescription = null, tint = IconGray, modifier = Modifier.size(47.dp))
                        }
                    }
                    Spacer(Modifier.width(22.dp))
                    Column(modifier = Modifier.weight(1f).offset(y = 2.dp)) {
                        Text("本机知识库", fontSize = 22.sp, fontWeight = FontWeight(750), fontFamily = FontFamily.Serif, letterSpacing = (-1).sp, color = Color(0xFF092651))
                        Spacer(Modifier.height(7.dp))
                        Text("匿名身份 · 本地优先", fontSize = 13.sp, color = Color(0xFF7E7D7B), letterSpacing = 0.8.sp)
                    }
                    IconButton(onClick = onEditClick) {
                        Icon(Icons.Filled.Edit, contentDescription = "编辑", tint = AccentBlue, modifier = Modifier.size(23.dp))
                    }
                }
            }
        }
    }
}

// ============================================================
// 认知设置面板 (SVG icons + right-aligned controls)
// ============================================================

@Composable
private fun CognitiveSettingsPanel(
    granularityLabel: String, exploreDepth: Boolean,
    companionStyleLabel: String,
    onClickGranularity: () -> Unit,
    onExploreDepthToggle: () -> Unit, onClickCompanion: () -> Unit
) {
    SettingsCard {
        SettingRow(icon = Icons.Outlined.CenterFocusStrong, showDivider = true, onClick = onClickGranularity) {
            Text("默认溯源粒度", style = rowTS)
            Spacer(Modifier.weight(1f))
            Text(granularityLabel, color = Color(0xFF4B5363), fontSize = 14.sp, fontFamily = FontFamily.Serif)
            Spacer(Modifier.width(4.dp))
            Arrow()
        }
        SettingRow(icon = Icons.Outlined.Tune, showDivider = true, onClick = onClickGranularity) {
            Text("提示明确度", style = rowTS)
            Spacer(Modifier.weight(1f))
            Arrow()
        }
        SettingRow(icon = Icons.Outlined.Layers, showDivider = true, onClick = onClickGranularity) {
            Text("探索深度", style = rowTS)
            Spacer(Modifier.weight(1f))
            MiniSwitch(checked = exploreDepth, onToggle = onExploreDepthToggle)
        }
        SettingRow(icon = Icons.Outlined.AutoAwesome, showDivider = false, onClick = onClickCompanion) {
            Text("伴读风格", style = rowTS)
            Spacer(Modifier.weight(1f))
            Text(companionStyleLabel, color = Color(0xFF4B5363), fontSize = 14.sp, fontFamily = FontFamily.Serif)
            Spacer(Modifier.width(4.dp))
            Arrow()
        }
    }
}

// ============================================================
// 隐私与安全面板 (SVG icons)
// ============================================================

@Composable
private fun PrivacySettingsPanel(
    privacyModeLabel: String,
    onClickPrivacy: () -> Unit, onClickPermission: () -> Unit, onClickExport: () -> Unit
) {
    SettingsCard {
        SettingRow(icon = Icons.Outlined.Shield, showDivider = true, onClick = onClickPrivacy) {
            Text("当前模式：", style = rowTS)
            Text(privacyModeLabel, color = AccentBlue, fontSize = 13.sp, fontFamily = FontFamily.Serif, letterSpacing = 0.7.sp)
            Spacer(Modifier.weight(1f))
            Arrow()
        }
        SettingRow(icon = Icons.Outlined.Lock, showDivider = true, onClick = onClickPermission) {
            Text("权限管理", style = rowTS)
            Spacer(Modifier.weight(1f))
            Arrow()
        }
        SettingRow(icon = Icons.Outlined.Download, showDivider = false, onClick = onClickExport) {
            Text("数据导出", style = rowTS)
            Spacer(Modifier.weight(1f))
            Arrow()
        }
    }
}

// ============================================================
// 危险卡片
// ============================================================

@Composable
private fun DangerCard(onClick: () -> Unit) {
    SettingsCard {
        SettingRow(icon = Icons.Outlined.DeleteForever, showDivider = false, onClick = onClick, isDanger = true) {
            Text("永久删除所有数据", color = DangerColor, fontSize = 14.sp, fontFamily = FontFamily.Serif)
            Spacer(Modifier.weight(1f))
            Text("›", color = DangerColor, fontSize = 28.sp, fontWeight = FontWeight.Light)
        }
    }
}

// ============================================================
// 复用组件
// ============================================================

private val rowTS = androidx.compose.ui.text.TextStyle(fontSize = 14.5.sp, color = Color(0xFF10213B), fontFamily = FontFamily.Serif)

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp), color = CardBg,
        shadowElevation = 0.dp, border = BorderStroke(1.dp, CardBorder)
    ) { Column(content = content) }
}

@Composable
private fun SettingRow(
    icon: ImageVector, showDivider: Boolean,
    onClick: (() -> Unit)? = null, isDanger: Boolean = false,
    content: @Composable RowScope.() -> Unit
) {
    Box {
        Row(
            modifier = Modifier.fillMaxWidth().height(54.5.dp)
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(start = 50.dp, end = 19.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
        Box(modifier = Modifier.padding(start = 19.dp).size(21.dp).align(Alignment.CenterStart), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = if (isDanger) DangerColor else AccentBlue, modifier = Modifier.size(21.dp))
        }
        if (showDivider) {
            Box(modifier = Modifier.fillMaxWidth().padding(start = 13.dp, end = 13.dp).height(1.dp)
                .align(Alignment.BottomCenter).background(DividerColor))
        }
    }
}

@Composable
private fun Arrow() {
    Text("›", color = AccentBlue, fontSize = 28.sp, fontWeight = FontWeight.Light)
}

// ============================================================
// 三段式分段按钮
// ============================================================

@Composable
private fun GranularitySeg(selectedIndex: Int, onSelected: (Int) -> Unit) {
    val opts = listOf("精简", "适中", "详尽")
    val shape = RoundedCornerShape(8.dp)
    Row(
        modifier = Modifier.width(154.dp).height(30.dp).clip(shape)
            .background(Color(0xC2FAF8F4)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        opts.forEachIndexed { i, label ->
            val sel = i == selectedIndex
            Box(
                modifier = Modifier
                    .weight(1f).fillMaxHeight()
                    .clickable { onSelected(i) },
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize().padding(2.dp),
                    shape = RoundedCornerShape(6.dp),
                    color = if (sel) Color.Transparent else Color.Transparent
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .then(
                                if (sel) Modifier.background(
                                    Brush.verticalGradient(listOf(Color(0xFF075BC5), Color(0xFF003E9D))),
                                    RoundedCornerShape(6.dp)
                                ) else Modifier
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(label, color = if (sel) Color.White else Color(0xFF20252E), fontSize = 12.sp,
                            fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal, fontFamily = FontFamily.Serif)
                    }
                }
            }
        }
    }
}

// ============================================================
// Slider
// ============================================================

@Composable
private fun MiniSlider(value: Float, onValueChange: (Float) -> Unit) {
    Column(modifier = Modifier.width(188.dp)) {
        Box(modifier = Modifier.fillMaxWidth().height(22.dp)) {
            Box(modifier = Modifier.fillMaxWidth().height(4.dp).align(Alignment.CenterStart)
                .clip(RoundedCornerShape(8.dp)).background(SliderInactive))
            Box(modifier = Modifier.fillMaxWidth(value).height(4.dp).align(Alignment.CenterStart)
                .clip(RoundedCornerShape(8.dp)).background(AccentBlue))
            Box(modifier = Modifier.size(15.dp).align(Alignment.CenterStart)
                .offset(x = (188.dp * value) - 7.dp)
                .clip(CircleShape).background(AccentBlue))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("低", fontSize = 10.sp, color = Color(0xFF777777))
            Text("中", fontSize = 10.sp, color = Color(0xFF777777))
            Text("高", fontSize = 10.sp, color = Color(0xFF777777))
        }
    }
}

// ============================================================
// Switch
// ============================================================

@Composable
private fun MiniSwitch(checked: Boolean, onToggle: () -> Unit) {
    val bg = if (checked)
        Modifier.background(Brush.horizontalGradient(listOf(Color(0xFF0047B9), Color(0xFF015BD3))), RoundedCornerShape(40.dp))
    else Modifier.background(SwitchOff, RoundedCornerShape(40.dp))
    Box(
        modifier = Modifier.width(42.dp).height(23.dp).clip(RoundedCornerShape(40.dp)).then(bg).clickable(onClick = onToggle),
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Box(modifier = Modifier.padding(2.dp).size(19.dp).clip(CircleShape).background(Color(0xFFFAF7F2)))
    }
}

// ============================================================
// 辅助函数
// ============================================================

private fun granularityLabel(v: String) = when (v) {
    "PARAGRAPH" -> "文章级"; "SENTENCE" -> "句词级"; else -> v
}
private fun companionLabel(v: String) = when (v) { "GENTLE" -> "温和启发"; "DIRECT" -> "直指核心"; else -> v }
private fun privacyLabel(v: String) = when (v) {
    "LOCAL_ONLY" -> "仅本地 LOCAL_ONLY"; "LOCAL_FIRST" -> "优先本地 LOCAL_FIRST"; "CLOUD_OK" -> "允许云端 CLOUD_OK"; else -> v
}
