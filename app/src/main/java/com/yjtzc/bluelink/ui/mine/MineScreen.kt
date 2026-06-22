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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yjtzc.bluelink.ui.mine.components.MineSectionTitle
import com.yjtzc.bluelink.ui.theme.DangerPale
import com.yjtzc.bluelink.ui.theme.DangerRed
import com.yjtzc.bluelink.ui.theme.Ink600
import com.yjtzc.bluelink.ui.theme.KleinBlue

// 原型色板
private val RiceWhite = Color(0xFFFAF7F2)
private val CardBg = Color(0xCCFFFDF8)       // rgba(255,253,248,.80)
private val CardBorder = Color(0xEBE5E0D8)   // rgba(229,224,216,.92)
private val DeepBlue = Color(0xFF082653)
private val KleinBlue = Color(0xFF0758C7)
private val InkBlue = Color(0xFF10213B)
private val IconGray = Color(0xFF5B7EB5)
private val DividerColor = Color(0xB8D6CFC4)  // rgba(214,207,196,.72)
private val SliderInactive = Color(0xFFDEDCD8)
private val SwitchOff = Color(0xFFD8D5CF)

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

    val granularityIndex = when (profile.defaultGranularity) {
        "PARAGRAPH" -> 0; "SENTENCE" -> 2; else -> 1
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(RiceWhite),
        contentPadding = PaddingValues(bottom = 120.dp)
    ) {
        // 页面标题
        item {
            Text(
                text = "我的",
                color = DeepBlue,
                fontSize = 28.sp,
                fontWeight = FontWeight(780),
                fontFamily = FontFamily.Serif,
                letterSpacing = (-1.2).sp,
                modifier = Modifier.padding(start = 16.dp, top = 58.dp, bottom = 0.dp)
            )
        }

        // 身份卡
        item {
            HeroIdentityCard(onEditClick = onNavigateToAppearance)
            Spacer(Modifier.height(8.dp))
        }

        // 认知设置
        item {
            MineSectionTitle("认知设置")
            CognitiveSettingsPanel(
                granularityIndex = granularityIndex,
                directness = profile.directnessLevel,
                exploreDepth = profile.exploreDepth,
                companionStyleLabel = companionStyleLabel(profile.companionStyle),
                onGranularityChange = { idx ->
                    viewModel.setGranularity(
                        when (idx) { 0 -> "PARAGRAPH"; 2 -> "SENTENCE"; else -> profile.defaultGranularity }
                    )
                },
                onDirectnessChange = viewModel::setDirectness,
                onExploreDepthToggle = viewModel::toggleExploreDepth,
                onClickCompanion = onNavigateToCognitive
            )
            Spacer(Modifier.height(4.dp))
        }

        // 隐私与安全
        item {
            MineSectionTitle("隐私与安全")
            PrivacySettingsPanel(
                privacyModeLabel = privacyModeLabel(profile.privacyMode),
                onClickPrivacy = onNavigateToPrivacySecurity,
                onClickPermission = onNavigateToPermission,
                onClickExport = onNavigateToDataExport
            )
            Spacer(Modifier.height(4.dp))
        }

        // 数据管理
        item {
            MineSectionTitle("数据管理")
            DangerCard(onClick = onNavigateToPermanentDelete)
            Spacer(Modifier.height(80.dp))
        }
    }
}

// ============================================================
// 身份卡 — 对齐 HTML .hero
// ============================================================

@Composable
private fun HeroIdentityCard(onEditClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(130.dp)
            .shadow(
                elevation = 13.dp,
                shape = RoundedCornerShape(26.dp),
                ambientColor = Color(0xFF192436),
                spotColor = Color(0xFF192436),
                // approximate 0 13px 28px rgba(25,36,54,.13)
            )
    ) {
        Surface(
            modifier = Modifier.matchParentSize(),
            shape = RoundedCornerShape(26.dp),
            color = Color(0xC2FFFDF8), // rgba(255,253,248,.76)
            border = BorderStroke(1.dp, Color(0xBDDFDAD1)) // rgba(223,218,209,.74)
        ) {
            Box {
                // 装饰渐变
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color(0xADE3EDFC),
                                    Color(0xB8FFFFFF),
                                    Color(0x80DBE8FE)
                                ),
                                start = androidx.compose.ui.geometry.Offset.Zero,
                                end = androidx.compose.ui.geometry.Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                            )
                        )
                )
                // 内容
                Row(
                    modifier = Modifier.fillMaxSize().padding(end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 圆形链环图标
                    Surface(
                        modifier = Modifier
                            .padding(start = 24.dp)
                            .size(78.dp),
                        shape = CircleShape,
                        color = Color(0xF0FCFAF4),
                        shadowElevation = 8.dp,
                        border = BorderStroke(2.dp, Color(0xBFFFFFFF))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.Link,
                                contentDescription = null,
                                tint = IconGray,
                                modifier = Modifier.size(47.dp)
                            )
                        }
                    }

                    Spacer(Modifier.width(22.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "本机知识库",
                            fontSize = 22.sp,
                            fontWeight = FontWeight(750),
                            fontFamily = FontFamily.Serif,
                            letterSpacing = (-1).sp,
                            color = Color(0xFF092651)
                        )
                        Spacer(Modifier.height(7.dp))
                        Text(
                            text = "匿名身份 · 本地优先",
                            fontSize = 13.sp,
                            color = Color(0xFF7E7D7B),
                            letterSpacing = 0.8.sp
                        )
                    }

                    // 编辑图标
                    IconButton(onClick = onEditClick) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "编辑",
                            tint = KleinBlue,
                            modifier = Modifier.size(23.dp)
                        )
                    }
                }
            }
        }
    }
}

// ============================================================
// 认知设置面板
// ============================================================

@Composable
private fun CognitiveSettingsPanel(
    granularityIndex: Int,
    directness: Float,
    exploreDepth: Boolean,
    companionStyleLabel: String,
    onGranularityChange: (Int) -> Unit,
    onDirectnessChange: (Float) -> Unit,
    onExploreDepthToggle: () -> Unit,
    onClickCompanion: () -> Unit
) {
    SettingsCard {
        // 默认溯源粒度
        SettingRow(
            icon = "○",
            showDivider = true
        ) {
            Text("默认溯源粒度", style = rowTextStyle)
            Spacer(Modifier.width(8.dp))
            GranularitySeg(
                selectedIndex = granularityIndex,
                onSelected = onGranularityChange
            )
        }

        // 提示明确度
        SettingRow(
            icon = "☰",
            showDivider = true
        ) {
            Text("提示明确度", style = rowTextStyle)
            Spacer(Modifier.width(8.dp))
            MiniSlider(
                value = directness,
                onValueChange = onDirectnessChange
            )
        }

        // 探索深度
        SettingRow(
            icon = "◇",
            showDivider = true
        ) {
            Text("探索深度", style = rowTextStyle)
            Spacer(Modifier.width(8.dp))
            MiniSwitch(checked = exploreDepth, onToggle = onExploreDepthToggle)
        }

        // 伴读风格
        SettingRow(
            icon = "♧",
            showDivider = false,
            onClick = onClickCompanion
        ) {
            Text("伴读风格", style = rowTextStyle)
            Spacer(Modifier.weight(1f))
            Text(
                text = companionStyleLabel,
                color = Color(0xFF4B5363),
                fontSize = 14.sp,
                fontFamily = FontFamily.Serif
            )
            Text(
                text = "›",
                color = KleinBlue,
                fontSize = 28.sp,
                fontWeight = FontWeight.Light,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

// ============================================================
// 隐私与安全面板
// ============================================================

@Composable
private fun PrivacySettingsPanel(
    privacyModeLabel: String,
    onClickPrivacy: () -> Unit,
    onClickPermission: () -> Unit,
    onClickExport: () -> Unit
) {
    SettingsCard {
        SettingRow(
            icon = "盾",
            showDivider = true,
            onClick = onClickPrivacy
        ) {
            Text("当前模式：", style = rowTextStyle)
            Text(
                text = privacyModeLabel,
                color = KleinBlue,
                fontSize = 13.sp,
                fontFamily = FontFamily.Serif,
                letterSpacing = 0.7.sp
            )
            Text(
                text = "›",
                color = KleinBlue,
                fontSize = 28.sp,
                fontWeight = FontWeight.Light,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        SettingRow(
            icon = "锁",
            showDivider = true,
            onClick = onClickPermission
        ) {
            Text("权限管理", style = rowTextStyle)
            Spacer(Modifier.weight(1f))
            Text("›", color = KleinBlue, fontSize = 28.sp, fontWeight = FontWeight.Light)
        }
        SettingRow(
            icon = "↓",
            showDivider = false,
            onClick = onClickExport
        ) {
            Text("数据导出", style = rowTextStyle)
            Spacer(Modifier.weight(1f))
            Text("›", color = KleinBlue, fontSize = 28.sp, fontWeight = FontWeight.Light)
        }
    }
}

// ============================================================
// 危险卡片
// ============================================================

@Composable
private fun DangerCard(onClick: () -> Unit) {
    SettingsCard {
        SettingRow(
            icon = "✕",
            showDivider = false,
            onClick = onClick,
            isDanger = true
        ) {
            Text("永久删除所有数据",
                color = Color(0xFFD9605B),
                fontSize = 14.sp,
                fontFamily = FontFamily.Serif)
            Spacer(Modifier.weight(1f))
            Text("›", color = Color(0xFFD9605B), fontSize = 28.sp, fontWeight = FontWeight.Light)
        }
    }
}

// ============================================================
// 复用控件
// ============================================================

private val rowTextStyle = androidx.compose.ui.text.TextStyle(
    fontSize = 14.5.sp,
    color = Color(0xFF10213B),
    fontFamily = FontFamily.Serif
)

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(18.dp),
        color = CardBg,
        shadowElevation = 0.dp,
        border = BorderStroke(1.dp, CardBorder)
    ) {
        Column(content = content)
    }
}

@Composable
private fun SettingRow(
    icon: String,
    showDivider: Boolean,
    onClick: (() -> Unit)? = null,
    isDanger: Boolean = false,
    content: @Composable RowScope.() -> Unit
) {
    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.5.dp)
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(start = 50.dp, end = 19.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
        // 图标
        Box(
            modifier = Modifier
                .padding(start = 19.dp, top = 0.dp)
                .size(21.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = icon,
                color = if (isDanger) Color(0xFFD9605B) else KleinBlue,
                fontSize = 21.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        // 分割线
        if (showDivider) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 13.dp, end = 13.dp)
                    .height(1.dp)
                    .align(Alignment.BottomCenter)
                    .background(DividerColor)
            )
        }
    }
}

// ============================================================
// 控件：三段式分段按钮
// ============================================================

@Composable
private fun GranularitySeg(
    selectedIndex: Int,
    onSelected: (Int) -> Unit
) {
    val options = listOf("精简", "适中", "详尽")
    val shape = RoundedCornerShape(8.dp)

    Row(
        modifier = Modifier
            .width(154.dp)
            .height(30.dp)
            .clip(shape)
            .background(Color(0xC2FAF8F4)) // rgba(250,248,244,.76)
            .padding(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        options.forEachIndexed { i, label ->
            val selected = i == selectedIndex
            val bgMod = if (selected) {
                Modifier.background(
                    Brush.verticalGradient(listOf(Color(0xFF075BC5), Color(0xFF003E9D))),
                    RoundedCornerShape(6.dp)
                )
            } else Modifier
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(if (selected) 6.dp else 0.dp))
                    .then(bgMod)
                    .clickable { onSelected(i) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    color = if (selected) Color.White else Color(0xFF20252E),
                    fontSize = 12.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    fontFamily = FontFamily.Serif
                )
            }
        }
    }
}

// ============================================================
// 控件：Slider（对齐 HTML .slider-wrap）
// ============================================================

@Composable
private fun MiniSlider(
    value: Float,
    onValueChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier.width(188.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(22.dp)
        ) {
            // 轨道
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .align(Alignment.CenterStart)
                    .clip(RoundedCornerShape(8.dp))
                    .background(SliderInactive)
            )
            // 已填充
            Box(
                modifier = Modifier
                    .fillMaxWidth(value)
                    .height(4.dp)
                    .align(Alignment.CenterStart)
                    .clip(RoundedCornerShape(8.dp))
                    .background(KleinBlue)
            )
            // 滑块
            Box(
                modifier = Modifier
                    .size(15.dp)
                    .align(Alignment.CenterStart)
                    .offset(x = (188.dp * value) - 7.dp)
                    .clip(CircleShape)
                    .background(KleinBlue)
            )
        }
        // 刻度
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("低", fontSize = 10.sp, color = Color(0xFF777777))
            Text("中", fontSize = 10.sp, color = Color(0xFF777777))
            Text("高", fontSize = 10.sp, color = Color(0xFF777777))
        }
    }
}

// ============================================================
// 控件：Switch
// ============================================================

@Composable
private fun MiniSwitch(
    checked: Boolean,
    onToggle: () -> Unit
) {
    val switchBg = if (checked) {
        Modifier.background(
            Brush.horizontalGradient(listOf(Color(0xFF0047B9), Color(0xFF015BD3))),
            RoundedCornerShape(40.dp)
        )
    } else {
        Modifier.background(SwitchOff, RoundedCornerShape(40.dp))
    }
    Box(
        modifier = Modifier
            .width(42.dp)
            .height(23.dp)
            .clip(RoundedCornerShape(40.dp))
            .then(switchBg)
            .clickable(onClick = onToggle),
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .padding(2.dp)
                .size(19.dp)
                .clip(CircleShape)
                .background(Color(0xFFFAF7F2))
        )
    }
}

// ============================================================
// 辅助函数
// ============================================================

private fun companionStyleLabel(value: String): String = when (value) {
    "GENTLE" -> "温和启发"
    "DIRECT" -> "直指核心"
    else -> value
}

private fun privacyModeLabel(value: String): String = when (value) {
    "LOCAL_ONLY" -> "仅本地 LOCAL_ONLY"
    "LOCAL_FIRST" -> "优先本地 LOCAL_FIRST"
    "CLOUD_OK" -> "允许云端 CLOUD_OK"
    else -> value
}
