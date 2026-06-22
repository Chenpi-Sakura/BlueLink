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
    content: @Composable () -> Unit
) {
    Box(modifier = modifier.fillMaxSize().background(Color(0xFFFAF7F2))) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier.fillMaxWidth().height(84.dp).padding(top = 24.dp)
            ) {
                Box(
                    modifier = Modifier.size(38.dp).offset(x = 0.dp).clip(CircleShape).clickable(onClick = onBack),
                    contentAlignment = Alignment.Center
                ) {
                    Text("‹", color = Color(0xFF082653), fontSize = 24.sp, fontWeight = FontWeight.Light)
                }
                Text(
                    text = title,
                    color = titleColor, fontSize = titleSize,
                    fontWeight = FontWeight(760), fontFamily = FontFamily.Serif,
                    modifier = Modifier.padding(start = 48.dp).align(Alignment.CenterStart)
                )
            }
            Box(modifier = Modifier.fillMaxSize()) { content() }
        }
    }
}
