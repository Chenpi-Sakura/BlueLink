package com.yjtzc.bluelink.ui.bookshelf

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yjtzc.bluelink.domain.model.Document
import com.yjtzc.bluelink.ui.theme.Ink400
import com.yjtzc.bluelink.ui.theme.Ink600
import com.yjtzc.bluelink.ui.theme.Ink900
import com.yjtzc.bluelink.ui.theme.KleinBlue
import com.yjtzc.bluelink.ui.theme.SerifFamily

// 书架专用书封配色
private val BookColors = listOf(
    Color(0xFF5B8DEF), // 蓝
    Color(0xFF6C5CE7), // 紫
    Color(0xFF00B894), // 绿
    Color(0xFFE17055), // 橙
    Color(0xFF0984E3), // 亮蓝
    Color(0xFFA29BFE), // 浅紫
    Color(0xFF55EFC4), // 青
    Color(0xFFFD79A8), // 粉
    Color(0xFFFDCB6E), // 黄
    Color(0xFF2D3436), // 深灰
)

/**
 * 书架页 — 参考小说阅读 APP 的书架网格布局
 */
@Composable
fun BookshelfScreen(
    documents: List<Document>,
    onBookClick: (docId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF0EFEA)) // 暖木色背景
    ) {
        if (documents.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "📚",
                        fontSize = 48.sp
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "书架还是空的",
                        fontSize = 16.sp,
                        color = Ink400,
                        fontFamily = SerifFamily
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "去文库导入你的第一本书吧",
                        fontSize = 13.sp,
                        color = Ink400
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(start = 16.dp, top = 24.dp, end = 16.dp, bottom = 80.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                items(documents, key = { it.id }) { doc ->
                    BookItem(
                        doc = doc,
                        onClick = { onBookClick(doc.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun BookItem(
    doc: Document,
    onClick: () -> Unit
) {
    val colorIndex = doc.id.hashCode().let { if (it < 0) -it else it } % BookColors.size
    val bookColor = BookColors[colorIndex]

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 书封
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.7f) // 书封宽高比
                .clip(RoundedCornerShape(8.dp))
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            bookColor,
                            bookColor.copy(alpha = 0.7f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            // 书名（竖排或横排在书封上）
            Text(
                text = doc.title.take(8),
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(8.dp)
            )

            // 顶部装饰条（模拟书脊）
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .width(4.dp)
                    .height(40.dp)
                    .background(Color.White.copy(alpha = 0.2f))
            )
        }

        Spacer(Modifier.height(8.dp))

        // 书名
        Text(
            text = doc.title,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = Ink900,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )

        // 来源
        if (doc.source != null) {
            Text(
                text = doc.source,
                fontSize = 10.sp,
                color = Ink400,
                maxLines = 1,
                textAlign = TextAlign.Center
            )
        }

        // 阅读进度条（装饰性，后续可对接真实进度）
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(2.dp)
                .background(Ink400.copy(alpha = 0.2f), RoundedCornerShape(1.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.3f)
                    .height(2.dp)
                    .background(bookColor, RoundedCornerShape(1.dp))
            )
        }
    }
}
