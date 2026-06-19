package com.yjtzc.bluelink.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ====== 字体族（UI&UX §6.3 / §8.2） ======

/**
 * 衬线体 — 内容/阅读区域使用。
 * 中文：思源宋体（Noto Serif SC），西文：Georgia（系统自带）
 *
 * V2.1 MVP：为避免 APK 膨胀暂时只用系统默认衬线体；
 * 后续可放入 res/font/ 加载 noto_serif_sc_regular.ttf 等。
 */
val SerifFamily = FontFamily.Serif

/**
 * 无衬线体 — UI 控件、标题使用。
 * 中文：思源黑体 / PingFang SC，西文：SF Pro Text / Inter
 */
val SansFamily = FontFamily.SansSerif

// ====== 排版系统（UI&UX §8.2） ======

val BlueLinkTypography = Typography(
    // 正文（阅读器、对话引言）— 衬线体 16sp，行高 1.8 倍 = 28sp
    bodyLarge = TextStyle(
        fontFamily = SerifFamily,
        fontSize = 16.sp,
        lineHeight = 28.sp,
        fontWeight = FontWeight.Normal,
        color = DeepInk
    ),

    // 卡片标题 — 衬线体 18sp
    titleMedium = TextStyle(
        fontFamily = SerifFamily,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.Medium
    ),

    // 小标题 — 无衬线体 14sp
    titleSmall = TextStyle(
        fontFamily = SansFamily,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.SemiBold
    ),

    // 辅助文本（标签、来源、时间）— 无衬线体 12sp
    bodySmall = TextStyle(
        fontFamily = SansFamily,
        fontSize = 12.sp,
        lineHeight = 18.sp,
        fontWeight = FontWeight.Normal
    ),

    // 极小标签
    labelSmall = TextStyle(
        fontFamily = SansFamily,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.Normal,
        color = MidGray
    )
)
