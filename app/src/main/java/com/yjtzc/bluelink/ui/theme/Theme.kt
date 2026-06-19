package com.yjtzc.bluelink.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

// ====== 亮色主题（对齐参考样式 Parchment + Klein Blue） ======

private val BlueLinkLightColors = lightColorScheme(
    // 主色 — 克莱因蓝
    primary = KleinBlue,
    onPrimary = PureWhite,
    primaryContainer = KleinBlueBg,
    onPrimaryContainer = KleinBlue,

    // 次要 — 暖灰
    secondary = Parchment100,
    onSecondary = Ink900,

    // 背景 — 羊皮纸白
    background = Parchment50,
    onBackground = Ink900,

    // 表面 — 纯白卡片
    surface = PureWhite,
    onSurface = Ink900,
    surfaceVariant = Parchment100,
    onSurfaceVariant = Ink600,

    // 错误
    error = Vermilion,
    onError = PureWhite,

    // 边框
    outline = Parchment200,
    outlineVariant = Parchment100,

    // 成功
    tertiary = SuccessGreen,
    onTertiary = PureWhite
)

// ====== 暗黑主题 ======

private val BlueLinkDarkColors = darkColorScheme(
    primary = KleinBlueLight,
    onPrimary = PureWhite,
    primaryContainer = KleinBlue.copy(alpha = 0.3f),
    onPrimaryContainer = KleinBlueLight,
    secondary = DarkSurfaceVariant,
    onSecondary = PureWhite,
    background = DarkBackground,
    onBackground = PureWhite,
    surface = DarkSurface,
    onSurface = PureWhite,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = LightGray,
    error = Vermilion,
    onError = PureWhite,
    outline = DarkOutline,
    outlineVariant = DarkOutline,
    tertiary = SuccessGreen,
    onTertiary = PureWhite
)

// ====== 圆角系统（对齐参考样式） ======
// 参考：卡片 28dp(rounded-3xl), 消息气泡 20dp(rounded-2xl), 标签 8dp

private val BlueLinkShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),      // 标签、折叠条
    medium = RoundedCornerShape(12.dp),    // 锚点卡片、输入框
    large = RoundedCornerShape(20.dp),     // 消息气泡、对话框
    extraLarge = RoundedCornerShape(28.dp) // 卡片、弹窗
)

// ====== Theme Composable ======

@Composable
fun BlueLinkTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,      // ← 关闭动态颜色，使用品牌色
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> BlueLinkDarkColors
        else -> BlueLinkLightColors
    }

    // 设置状态栏颜色
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = BlueLinkTypography,
        shapes = BlueLinkShapes,
        content = content
    )
}
