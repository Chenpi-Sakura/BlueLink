package com.yjtzc.bluelink.ui.mine.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 分区标题 — 对齐 HTML 原型
 * 字号 16px, 字重 760, 字色 #0A2B5F, 衬线体
 */
@Composable
fun MineSectionTitle(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        color = Color(0xFF0A2B5F),
        fontSize = 16.sp,
        fontWeight = FontWeight(760),
        fontFamily = FontFamily.Serif,
        modifier = modifier.padding(start = 0.dp, top = 27.dp, bottom = 10.dp)
    )
}
