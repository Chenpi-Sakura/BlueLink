package com.yjtzc.bluelink.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
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
import androidx.core.view.WindowCompat

// ====== 亮色主题（UI&UX §2.2.1 / §8.1） ======

private val BlueLinkLightColors = lightColorScheme(
    primary = KleinBlue,
    onPrimary = RiceWhite,
    primaryContainer = KleinBluePale,
    onPrimaryContainer = KleinBlue,
    secondary = RiceWarm,
    onSecondary = DeepInk,
    background = RiceWhite,
    onBackground = DeepInk,
    surface = RiceWhite,
    onSurface = DeepInk,
    surfaceVariant = MistGray,
    onSurfaceVariant = MidGray,
    error = Vermilion,
    onError = RiceWhite,
    outline = SandLine,
    outlineVariant = SandLine,
    tertiary = SuccessGreen,
    onTertiary = RiceWhite
)

// ====== 暗黑主题（UI&UX §2.2.2） ======

private val BlueLinkDarkColors = darkColorScheme(
    primary = KleinBluePale,
    onPrimary = RiceWhite,
    primaryContainer = KleinBlue.copy(alpha = 0.3f),
    onPrimaryContainer = KleinBluePale,
    secondary = DarkSurfaceVariant,
    onSecondary = RiceWhite,
    background = DarkBackground,
    onBackground = RiceWhite,
    surface = DarkSurface,
    onSurface = RiceWhite,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = LightGray,
    error = Vermilion,
    onError = RiceWhite,
    outline = DarkOutline,
    outlineVariant = DarkOutline,
    tertiary = SuccessGreen,
    onTertiary = RiceWhite
)

// ====== Theme Composable ======

@Composable
fun BlueLinkTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        // Android 12+ 支持 Material You 动态取色
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
        content = content
    )
}
