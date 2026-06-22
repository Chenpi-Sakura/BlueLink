package com.yjtzc.bluelink.ui.mine

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import com.yjtzc.bluelink.ui.theme.KleinBlueBg
import com.yjtzc.bluelink.ui.theme.Parchment100
import com.yjtzc.bluelink.ui.theme.SerifFamily

@Composable
fun AppearanceSettingsScreen(
    viewModel: AppearanceViewModel,
    onBack: () -> Unit
) {
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val dynamicColor by viewModel.dynamicColor.collectAsStateWithLifecycle()
    val fontScale by viewModel.fontScale.collectAsStateWithLifecycle()
    val highContrast by viewModel.highContrast.collectAsStateWithLifecycle()

    MineNavScaffold(
        title = "外观设置",
        onBack = onBack
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // ====== 显示模式 ======
            item { MineSectionTitle("显示模式") }

            item {
                ThemeModeCard(
                    currentMode = themeMode,
                    onModeSelected = viewModel::setThemeMode
                )
                Spacer(Modifier.height(32.dp))
            }

            // ====== 色彩 ======
            item { MineSectionTitle("色彩") }

            item {
                SettingSwitchCard(
                    title = "动态取色",
                    subtitle = "使用系统壁纸色彩，但保留克莱因蓝作为品牌强调色",
                    checked = dynamicColor,
                    onCheckedChange = viewModel::setDynamicColor
                )
                Spacer(Modifier.height(32.dp))
            }

            // ====== 字体 ======
            item { MineSectionTitle("字体") }

            item {
                FontScaleCard(
                    fontScale = fontScale,
                    onScaleChange = viewModel::setFontScale
                )
                Spacer(Modifier.height(32.dp))
            }

            // ====== 无障碍 ======
            item { MineSectionTitle("无障碍") }

            item {
                SettingSwitchCard(
                    title = "高对比模式",
                    subtitle = "增强文字与背景对比度，提升可读性",
                    checked = highContrast,
                    onCheckedChange = viewModel::setHighContrast
                )
                Spacer(Modifier.height(80.dp))
            }
        }
    }
}

// ====== 显示模式卡片 ======

@Composable
private fun ThemeModeCard(
    currentMode: String,
    onModeSelected: (String) -> Unit
) {
    val modes = listOf(
        "SYSTEM" to "跟随系统",
        "LIGHT" to "浅色",
        "DARK" to "深色"
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, Parchment100)
    ) {
        Column {
            modes.forEachIndexed { index, (mode, label) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onModeSelected(mode) }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = currentMode == mode,
                        onClick = { onModeSelected(mode) },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = KleinBlue
                        )
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontFamily = SerifFamily,
                            fontWeight = FontWeight.Medium
                        ),
                        color = Ink900
                    )
                }
                if (index < modes.lastIndex) {
                    HorizontalDivider(
                        color = Parchment100,
                        thickness = 0.5.dp,
                        modifier = Modifier.padding(start = 52.dp)
                    )
                }
            }
        }
    }
}

// ====== Switch 设置行 ======

@Composable
private fun SettingSwitchCard(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
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
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontFamily = SerifFamily,
                        fontWeight = FontWeight.Medium
                    ),
                    color = Ink900
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Ink600
                )
            }
            Spacer(Modifier.width(12.dp))
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
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

// ====== 字体大小卡片 ======

@Composable
private fun FontScaleCard(
    fontScale: Float,
    onScaleChange: (Float) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, Parchment100)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Text(
                text = "字体大小",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontFamily = SerifFamily,
                    fontWeight = FontWeight.Medium
                ),
                color = Ink900
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("小", fontSize = 12.sp, color = Ink600)
                Slider(
                    value = fontScale,
                    onValueChange = onScaleChange,
                    valueRange = 0.85f..1.25f,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = KleinBlue,
                        activeTrackColor = KleinBlue,
                        inactiveTrackColor = Parchment100
                    )
                )
                Text("大", fontSize = 12.sp, color = Ink600)
            }
            Spacer(Modifier.height(12.dp))

            // 预览
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = KleinBlueBg
            ) {
                Text(
                    text = "阅读即思考",
                    fontFamily = SerifFamily,
                    fontSize = (16.sp * fontScale),
                    fontWeight = FontWeight.Medium,
                    color = Ink900,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }
    }
}
