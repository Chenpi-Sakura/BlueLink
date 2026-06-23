package com.yjtzc.bluelink.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items as lazyItems
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items as staggeredItems
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yjtzc.bluelink.data.local.db.DocumentEntity
import com.yjtzc.bluelink.data.local.db.InspirationCardEntity
import com.yjtzc.bluelink.ui.theme.*

/**
 * 搜索页面
 *
 * - 搜索框下方有标签选项：灵感碎片 / 文档类型 / 录音 / 图片
 * - 文档类型搜索按文件后缀（word/pdf/txt/md）过滤
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onBack: () -> Unit,
    onOpenInspiration: (cardId: String) -> Unit,
    onOpenDocument: (docId: String) -> Unit,
    allCards: List<InspirationCardEntity>,
    documents: List<DocumentEntity>
) {
    var query by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableIntStateOf(0) }

    val filters = listOf("灵感碎片", "文档类型", "录音", "图片")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("搜索", fontFamily = SerifFamily, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.Close, contentDescription = "关闭搜索", tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // ====== 搜索框（参考样式：极简无边框填充） ======
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = {
                    Text(
                        when (selectedFilter) {
                            0 -> "搜索灵感碎片..."
                            1 -> "搜索文档（word/pdf/txt/md）..."
                            2 -> "搜索录音..."
                            3 -> "搜索图片..."
                            else -> "搜索..."
                        },
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                leadingIcon = {
                    Icon(Icons.Outlined.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                },
                trailingIcon = {
                    if (query.isNotBlank()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "清除", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .height(52.dp),
                shape = RoundedCornerShape(26.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp)
            )

            Spacer(Modifier.height(4.dp))

            // ====== 分类标签 ======
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filters.forEachIndexed { index, label ->
                    FilterChip(
                        selected = selectedFilter == index,
                        onClick = { selectedFilter = index },
                        label = {
                            Text(label, fontSize = 12.sp)
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = KleinBlueBg,
                            selectedLabelColor = KleinBlue
                        )
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // ====== 搜索结果 ======
            if (query.isBlank()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Outlined.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            when (selectedFilter) {
                                0 -> "搜索灵感碎片"
                                1 -> "搜索文档（word/pdf/txt/md）"
                                2 -> "搜索录音"
                                3 -> "搜索图片"
                                else -> "输入关键词开始搜索"
                            },
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "在上方搜索框中输入关键字",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                when (selectedFilter) {
                    1 -> {
                        // 文档类型搜索 — 按文件后缀过滤
                        val docResults = documents.filter { doc ->
                            val ext = detectDocType(doc.title)
                            val matchesQuery = doc.title.contains(query, ignoreCase = true) ||
                                    ext.contains(query, ignoreCase = true)
                            matchesQuery
                        }

                        if (docResults.isEmpty()) {
                            emptyResult()
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 32.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                lazyItems(docResults, key = { it.id }) { doc ->
                                    DocumentResultCard(
                                        doc = doc,
                                        onClick = { onOpenDocument(doc.id) }
                                    )
                                }
                            }
                        }
                    }
                    else -> {
                        // 灵感、录音、图片 — 从灵感卡片搜索
                        val filteredResults = allCards.filter { card ->
                            val matchesQuery = card.contentSnippet.contains(query, ignoreCase = true) ||
                                    card.tags.contains(query, ignoreCase = true)
                            when (selectedFilter) {
                                0 -> matchesQuery
                                2 -> matchesQuery && card.type.name == "VOICE"
                                3 -> matchesQuery && card.type.name == "IMAGE"
                                else -> matchesQuery
                            }
                        }

                        if (filteredResults.isEmpty()) {
                            emptyResult()
                        } else {
                            LazyVerticalStaggeredGrid(
                                columns = StaggeredGridCells.Fixed(2),
                                contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 32.dp),
                                verticalItemSpacing = 12.dp,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                staggeredItems(filteredResults, key = { it.id }) { card ->
                                    SearchResultCard(
                                        card = card,
                                        onClick = { onOpenInspiration(card.id) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun emptyResult() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🔍", fontSize = 48.sp)
            Spacer(Modifier.height(12.dp))
            Text(
                "没有找到匹配的结果",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "试试其他关键词或分类",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * 从文件名检测文档类型标签
 */
private fun detectDocType(title: String): String {
    val lower = title.lowercase()
    return when {
        lower.endsWith(".pdf") -> "pdf"
        lower.endsWith(".doc") || lower.endsWith(".docx") -> "word"
        lower.endsWith(".txt") -> "txt"
        lower.endsWith(".md") -> "md"
        lower.endsWith(".rtf") -> "rtf"
        lower.endsWith(".wps") -> "wps"
        else -> "其他"
    }
}

private fun docTypeIcon(type: String): String = when (type) {
    "pdf" -> "📕"
    "word" -> "📘"
    "txt" -> "📄"
    "md" -> "📝"
    else -> "📄"
}

// ====================================================================
// 灵感搜索结果卡片
// ====================================================================

@Composable
private fun SearchResultCard(
    card: InspirationCardEntity,
    onClick: () -> Unit
) {
    val title = card.contentSnippet.take(30).ifBlank { "未命名灵感" }
    val tags = card.tags.split(",").filter { it.isNotBlank() }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = when (card.type.name) {
                        "VOICE" -> "🎤 录音"
                        "IMAGE" -> "📷 图片"
                        else -> "📝 文字"
                    },
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = title,
                fontFamily = SerifFamily,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            if (tags.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    tags.take(2).forEach { tag ->
                        Box(
                            modifier = Modifier
                                .background(KleinBlueBg, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("#$tag", fontSize = 10.sp, color = KleinBlue)
                        }
                    }
                }
            }
        }
    }
}

// ====================================================================
// 文档搜索结果卡片
// ====================================================================

@Composable
private fun DocumentResultCard(
    doc: DocumentEntity,
    onClick: () -> Unit
) {
    val type = detectDocType(doc.title)
    val icon = docTypeIcon(type)

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 文档类型图标
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(KleinBlueBg, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(icon, fontSize = 20.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = doc.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = type.uppercase(),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
