package com.yjtzc.bluelink.ui.reader

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yjtzc.bluelink.data.local.db.SegmentEntity
import com.yjtzc.bluelink.ui.theme.Ink600
import com.yjtzc.bluelink.ui.theme.Ink900
import com.yjtzc.bluelink.ui.theme.KleinBlue
import com.yjtzc.bluelink.ui.theme.KleinBlueBg
import com.yjtzc.bluelink.ui.theme.SerifFamily

/**
 * 智能阅读器 — 对齐参考样式（#page-reader）
 */
@Composable
fun ReaderScreen(
    viewModel: ReaderViewModel,
    docId: String? = null,
    spotlightSegmentId: String? = null,
    modifier: Modifier = Modifier
) {
    val segments by viewModel.segments.collectAsStateWithLifecycle()
    val spotlightId by viewModel.spotlightTargetId.collectAsStateWithLifecycle()
    val foldedIds by viewModel.foldedSegmentIds.collectAsStateWithLifecycle()

    LaunchedEffect(docId, spotlightSegmentId) {
        if (docId != null) {
            viewModel.loadDocument(docId, spotlightSegmentId)
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(start = 20.dp, top = 16.dp, end = 20.dp, bottom = 80.dp)
    ) {
        items(segments, key = { it.id }) { seg ->
            val isTarget = seg.id == spotlightId
            val isDimmed = spotlightId != null && !isTarget
            val isFolded = seg.id in foldedIds

            SegmentView(
                segment = seg,
                isSpotlightTarget = isTarget,
                isDimmed = isDimmed,
                isFolded = isFolded,
                onToggleFold = { viewModel.toggleFold(seg.id) },
                onClickBlank = { viewModel.clearSpotlight() }
            )
        }
    }
}

@Composable
fun SegmentView(
    segment: SegmentEntity,
    isSpotlightTarget: Boolean,
    isDimmed: Boolean,
    isFolded: Boolean,
    onToggleFold: () -> Unit,
    onClickBlank: () -> Unit
) {
    val alpha by animateFloatAsState(
        targetValue = when {
            isSpotlightTarget -> 1f
            isDimmed -> 0.25f
            else -> 1f
        },
        animationSpec = tween(durationMillis = 400),
        label = "segment-alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        if (isFolded) {
            // 折叠条（参考 .fold-strip）
            FoldBarView(
                foldedCount = segment.textSnippet.length,
                onClick = onToggleFold
            )
        } else if (isSpotlightTarget) {
            // 聚光灯目标（参考 .spotlight-target）
            Surface(
                color = KleinBlue.copy(alpha = 0.04f),
                shape = RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onClickBlank() }
            ) {
                Row {
                    // 左侧克莱因蓝边线
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .fillMaxHeight()
                            .background(KleinBlue)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.padding(start = 0.dp, top = 16.dp, end = 16.dp, bottom = 16.dp)) {
                        // "溯源锚点" 标签
                        Text(
                            "溯源锚点",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = KleinBlue,
                            letterSpacing = 1.sp
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = segment.textSnippet,
                            fontFamily = SerifFamily,
                            fontSize = 15.sp,
                            lineHeight = 24.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Ink900
                        )
                    }
                }
            }
        } else {
            // 普通段落（参考 .fade-paragraph）
            Text(
                text = segment.textSnippet,
                fontFamily = SerifFamily,
                fontSize = 15.sp,
                lineHeight = 24.sp,
                color = Ink600,
                modifier = Modifier.graphicsLayer { this.alpha = alpha }
            )
        }
    }
}

@Composable
fun FoldBarView(
    foldedCount: Int,
    onClick: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Surface(
        shape = MaterialTheme.shapes.small,
        color = KleinBlueBg,
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                expanded = !expanded
                onClick()
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = if (expanded) "收起重复背景"
                else "AI 已折叠 $foldedCount 字重复背景，点击查看增量",
                fontSize = 13.sp,
                color = KleinBlue
            )
            Spacer(Modifier.width(4.dp))
            Text(
                if (expanded) "▲" else "▼",
                fontSize = 10.sp,
                color = KleinBlue
            )
        }
    }
}
