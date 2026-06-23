package com.yjtzc.bluelink.ui.mine

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yjtzc.bluelink.ui.mine.components.MineNavScaffold
import com.yjtzc.bluelink.ui.mine.components.MineSectionTitle

// 主界面色板
private val RiceWhite = Color(0xFFFAF7F2)
private val CardBg = Color(0xCCFFFDF8)
private val CardBorder = Color(0xEBE5E0D8)
private val AccentBlue = Color(0xFF0758C7)
private val DividerColor = Color(0xB8D6CFC4)
private val TextPrimary = Color(0xFF10213B)
private val TextSecondary = Color(0xFF66686D)

@Composable
fun AppearanceSettingsScreen(
    viewModel: AppearanceViewModel,
    onBack: () -> Unit
) {
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val dynamicColor by viewModel.dynamicColor.collectAsStateWithLifecycle()
    val fontScale by viewModel.fontScale.collectAsStateWithLifecycle()
    val highContrast by viewModel.highContrast.collectAsStateWithLifecycle()

    MineNavScaffold(title = "外观设置", onBack = onBack) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().background(RiceWhite),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 120.dp)
        ) {
            item { MineSectionTitle("显示模式") }
            item {
                ThemeModeCard(currentMode = themeMode, onModeSelected = viewModel::setThemeMode)
            }
            item { MineSectionTitle("色彩") }
            item {
                SettingSwitchCard(title = "动态取色",
                    subtitle = "使用系统壁纸色彩，但保留克莱因蓝作为品牌强调色",
                    checked = dynamicColor, onCheckedChange = viewModel::setDynamicColor)
            }
            item { MineSectionTitle("字体") }
            item {
                FontScaleCard(fontScale = fontScale, onScaleChange = viewModel::setFontScale)
            }
            item { MineSectionTitle("无障碍") }
            item {
                SettingSwitchCard(title = "高对比模式",
                    subtitle = "增强文字与背景对比度，提升可读性",
                    checked = highContrast, onCheckedChange = viewModel::setHighContrast)
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

// ====== 显示模式卡片 ======

@Composable
private fun ThemeModeCard(currentMode: String, onModeSelected: (String) -> Unit) {
    val modes = listOf(
        "SYSTEM" to "跟随系统",
        "LIGHT" to "浅色",
        "DARK" to "深色"
    )
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp), color = CardBg,
        border = BorderStroke(1.dp, CardBorder)
    ) {
        Column {
            modes.forEachIndexed { index, (mode, label) ->
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { onModeSelected(mode) }
                        .padding(start = 19.dp, end = 19.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = currentMode == mode,
                        onClick = { onModeSelected(mode) },
                        colors = RadioButtonDefaults.colors(selectedColor = AccentBlue)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(label, fontSize = 14.5.sp, color = TextPrimary, fontFamily = FontFamily.Serif)
                    Spacer(Modifier.weight(1f))
                }
                if (index < modes.lastIndex) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(start = 13.dp, end = 13.dp)
                            .height(1.dp).background(DividerColor)
                    )
                }
            }
        }
    }
}

// ====== Switch 设置行 ======

@Composable
private fun SettingSwitchCard(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp), color = CardBg,
        border = BorderStroke(1.dp, CardBorder)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 19.dp, end = 19.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 14.5.sp, color = TextPrimary, fontFamily = FontFamily.Serif)
                Spacer(Modifier.height(2.dp))
                Text(subtitle, fontSize = 12.sp, color = TextSecondary)
            }
            Spacer(Modifier.width(12.dp))
            Switch(
                checked = checked, onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedTrackColor = AccentBlue,
                    checkedThumbColor = Color(0xFFFAF7F2),
                    uncheckedTrackColor = Color(0xFFD8D5CF),
                    uncheckedThumbColor = Color(0xFFA39F98)
                )
            )
        }
    }
}

// ====== 字体大小卡片 ======

@Composable
private fun FontScaleCard(fontScale: Float, onScaleChange: (Float) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp), color = CardBg,
        border = BorderStroke(1.dp, CardBorder)
    ) {
        Column(modifier = Modifier.padding(start = 19.dp, end = 19.dp)) {
            Spacer(Modifier.height(4.dp))
            Text("字体大小", fontSize = 14.5.sp, color = TextPrimary, fontFamily = FontFamily.Serif)
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("小", fontSize = 12.sp, color = TextSecondary)
                Slider(value = fontScale, onValueChange = onScaleChange,
                    valueRange = 0.85f..1.25f,
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = AccentBlue, activeTrackColor = AccentBlue,
                        inactiveTrackColor = Color(0xFFDEDCD8)
                    )
                )
                Text("大", fontSize = 12.sp, color = TextSecondary)
            }
            Spacer(Modifier.height(4.dp))
            // 预览
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = AccentBlue.copy(alpha = 0.08f)
            ) {
                Text("阅读即思考", fontFamily = FontFamily.Serif,
                    fontSize = (16.sp * fontScale), color = TextPrimary,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}
