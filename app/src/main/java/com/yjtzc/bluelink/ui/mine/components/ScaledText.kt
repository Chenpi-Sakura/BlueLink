package com.yjtzc.bluelink.ui.mine.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.TextUnit
import com.yjtzc.bluelink.util.LocalFontScale

/**
 * 获取当前缩放后的字号
 */
@Composable
fun scaledFontSize(base: TextUnit): TextUnit = base * LocalFontScale.current

/**
 * 获取当前缩放后的行高
 */
@Composable
fun scaledLineHeight(base: TextUnit): TextUnit = base * LocalFontScale.current

/**
 * 创建一个缩放的 TextStyle
 */
@Composable
fun scaledTextStyle(
    baseFontSize: TextUnit,
    fontFamily: FontFamily? = null,
    baseLineHeight: TextUnit = TextUnit.Unspecified
): TextStyle {
    val scale = LocalFontScale.current
    return TextStyle(
        fontSize = baseFontSize * scale,
        fontFamily = fontFamily,
        lineHeight = if (baseLineHeight != TextUnit.Unspecified) baseLineHeight * scale else TextUnit.Unspecified
    )
}
