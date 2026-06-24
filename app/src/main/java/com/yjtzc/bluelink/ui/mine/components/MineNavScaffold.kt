package com.yjtzc.bluelink.ui.mine.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MineNavScaffold(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    titleColor: Color = Color(0xFF082653),
    titleSize: TextUnit = 21.sp,
    titleWeight: FontWeight = FontWeight(760),
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFFAF7F2))
            .systemBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(top = 18.dp)) {
            // 顶部栏：返回按钮 + 标题
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                // 返回按钮
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center
                ) {
                    Text("‹", color = Color(0xFF082653), fontSize = 28.sp, fontWeight = FontWeight.Light)
                }
                // 标题
                Text(
                    text = title,
                    color = titleColor,
                    fontSize = titleSize,
                    fontWeight = titleWeight,
                    fontFamily = FontFamily.Serif,
                    modifier = Modifier.padding(start = 46.dp)
                )
            }

            // 内容区
            Box(modifier = Modifier.fillMaxSize()) { content() }
        }
    }
}
