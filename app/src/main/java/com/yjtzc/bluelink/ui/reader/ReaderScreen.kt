package com.yjtzc.bluelink.ui.reader

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yjtzc.bluelink.data.local.db.SegmentEntity
import com.yjtzc.bluelink.ui.theme.KleinBlue
import com.yjtzc.bluelink.ui.theme.SerifFamily

/**
 * 智能阅读器 — 聚光灯模式 & 增量折叠模式（UI&UX §4.4）
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
        contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 80.dp)
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
            isDimmed -> 0.3f
            else -> 1f
        },
        animationSpec = tween(durationMillis = 250),
        label = "segment-alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        if (isFolded) {
            // 折叠提示条
            FoldBarView(
                foldedCount = segment.textSnippet.length,
                onClick = onToggleFold
            )
        } else {
            Row(
                modifier = Modifier.graphicsLayer { this.alpha = alpha }
            ) {
                if (isSpotlightTarget) {
                    // 左侧克莱因蓝引力线
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .height(60.dp)
                            .background(KleinBlue)
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    text = segment.textSnippet,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontFamily = SerifFamily
                    ),
                    color = if (isSpotlightTarget)
                        MaterialTheme.colorScheme.onSurface
                    else
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier
                        .clickable { if (isSpotlightTarget) onClickBlank() }
                        .background(
                            if (isSpotlightTarget)
                                MaterialTheme.colorScheme.surface
                            else Color.Transparent,
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 4.dp)
                )
            }
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
        shape = RoundedCornerShape(8.dp),
        color = Color.Transparent,
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .clickable {
                expanded = !expanded
                onClick()
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (expanded) "收起重复背景"
                else "AI 已折叠 $foldedCount 字重复背景，点击展开",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = if (expanded) Icons.Default.KeyboardArrowUp
                else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.rotate(if (expanded) 180f else 0f)
            )
        }
    }
}
