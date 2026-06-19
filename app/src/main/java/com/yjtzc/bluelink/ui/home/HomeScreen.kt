package com.yjtzc.bluelink.ui.home

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yjtzc.bluelink.data.local.db.InspirationCardEntity
import com.yjtzc.bluelink.ui.capture.CaptureSheet
import com.yjtzc.bluelink.ui.theme.Ink400
import com.yjtzc.bluelink.ui.theme.Ink600
import com.yjtzc.bluelink.ui.theme.Ink900
import com.yjtzc.bluelink.ui.theme.KleinBlue
import com.yjtzc.bluelink.ui.theme.KleinBlueBg
import com.yjtzc.bluelink.ui.theme.Parchment100
import com.yjtzc.bluelink.ui.theme.Parchment200
import com.yjtzc.bluelink.ui.theme.SerifFamily
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * 灵感首页 — 问候 + 瀑布流卡片 + FAB 速度盘 + 搜索
 */
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onOpenDrawer: () -> Unit = {},
    onOpenInspiration: (cardId: String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val cards by viewModel.cards.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    var showCaptureSheet by remember { mutableStateOf(false) }
    var captureTabIndex by remember { mutableIntStateOf(0) }
    var fabExpanded by remember { mutableStateOf(false) }

    val greeting = getTimeGreeting()

    // 点击空白关闭 FAB 速度盘
    if (fabExpanded) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { fabExpanded = false }
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ====== 顶部导航栏 ======
            Surface(
                color = MaterialTheme.colorScheme.background.copy(alpha = 0.85f),
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .windowInsetsTopHeight(WindowInsets.statusBars),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onOpenDrawer) {
                            Text("☰", fontSize = 20.sp, color = Ink900)
                        }
                        Spacer(Modifier.width(4.dp))
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = viewModel::onSearchChange,
                            placeholder = { Text("搜索灵感...", fontSize = 14.sp, color = Ink400) },
                            modifier = Modifier.weight(1f).height(44.dp),
                            shape = RoundedCornerShape(22.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Parchment200,
                                unfocusedBorderColor = Parchment200.copy(alpha = 0.5f),
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            ),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp)
                        )
                    }
                    HorizontalDivider(color = Parchment200.copy(alpha = 0.4f), thickness = 0.5.dp)
                }
            }

            // ====== 问候语 ======
            Text(
                greeting,
                fontFamily = SerifFamily,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Ink900,
                modifier = Modifier.padding(start = 20.dp, top = 20.dp, bottom = 12.dp)
            )

            // ====== 瀑布流灵感卡片 ======
            if (cards.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("✨", fontSize = 48.sp)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            if (searchQuery.isNotBlank()) "没有找到匹配的灵感" else "还没有灵感",
                            fontFamily = SerifFamily,
                            fontSize = 18.sp,
                            color = Ink600
                        )
                        if (searchQuery.isBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text("点击右下角 + 捕获灵感", fontSize = 13.sp, color = Ink400)
                        }
                    }
                }
            } else {
                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Fixed(2),
                    contentPadding = PaddingValues(start = 16.dp, top = 4.dp, end = 16.dp, bottom = 100.dp),
                    verticalItemSpacing = 12.dp,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(cards, key = { it.id }) { card ->
                        InspirationCard(
                            card = card,
                            onClick = { onOpenInspiration(card.id) },
                            onDelete = { viewModel.deleteCard(card) }
                        )
                    }
                }
            }
        }

        // ====== FAB 速度盘 ======
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 80.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 弹出选项
            AnimatedVisibility(
                visible = fabExpanded,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    FabOption(
                        emoji = "📷",
                        label = "图片",
                        onClick = {
                            fabExpanded = false
                            captureTabIndex = 2
                            showCaptureSheet = true
                        }
                    )
                    FabOption(
                        emoji = "🎤",
                        label = "录音",
                        onClick = {
                            fabExpanded = false
                            captureTabIndex = 1
                            showCaptureSheet = true
                        }
                    )
                    FabOption(
                        emoji = "📝",
                        label = "文字",
                        onClick = {
                            fabExpanded = false
                            captureTabIndex = 0
                            showCaptureSheet = true
                        }
                    )
                }
            }

            // 主 FAB
            FloatingActionButton(
                onClick = { fabExpanded = !fabExpanded },
                containerColor = KleinBlue,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    if (fabExpanded) Icons.Default.Close else Icons.Default.Add,
                    contentDescription = "添加灵感",
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }

    if (showCaptureSheet) {
        CaptureSheet(
            initialTabIndex = captureTabIndex,
            onDismiss = { showCaptureSheet = false }
        )
    }
}

/**
 * FAB 速度盘选项
 */
@Composable
private fun FabOption(
    emoji: String,
    label: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = 4.dp,
        onClick = onClick,
        modifier = Modifier.height(40.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(emoji, fontSize = 16.sp)
            Spacer(Modifier.width(6.dp))
            Text(label, fontSize = 13.sp, color = Ink900, fontWeight = FontWeight.Medium)
        }
    }
}

/**
 * 灵感瀑布流卡片 — 支持长按菜单（删除 / 导出 / 移入文件夹）
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun InspirationCard(
    card: InspirationCardEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit = {}
) {
    val title = card.contentSnippet.take(20).ifBlank { "未命名灵感" }
    val tags = card.tags.split(",").filter { it.isNotBlank() }
    var showMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = { showMenu = true }
            )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 标题
            Text(
                text = title,
                fontFamily = SerifFamily,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Ink900,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(8.dp))

            // 标签
            if (tags.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    tags.take(3).forEach { tag ->
                        Box(
                            modifier = Modifier
                                .background(KleinBlueBg, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("#$tag", fontSize = 10.sp, color = KleinBlue)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            // 时间
            Text(
                text = formatCardDate(card.createdAt),
                fontSize = 11.sp,
                color = Ink400
            )
        }
    }

    // 长按上下文菜单
    DropdownMenu(
        expanded = showMenu,
        onDismissRequest = { showMenu = false }
    ) {
        DropdownMenuItem(
            text = { Text("🗑 删除") },
            onClick = {
                showMenu = false
                onDelete()
            }
        )
        DropdownMenuItem(
            text = { Text("📤 导出") },
            onClick = {
                showMenu = false
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, card.contentSnippet)
                    putExtra(Intent.EXTRA_SUBJECT, title)
                }
                context.startActivity(Intent.createChooser(shareIntent, "导出灵感"))
            }
        )
        DropdownMenuItem(
            text = { Text("📁 移入文件夹") },
            onClick = {
                showMenu = false
                // TODO: V2.x 文件夹管理功能
            }
        )
    }
}

private fun getTimeGreeting(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 5..11 -> "早上好"
        in 12..13 -> "中午好"
        in 14..17 -> "下午好"
        else -> "晚上好"
    }
}

private fun formatCardDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("M月d日 HH:mm", Locale.CHINESE)
    return sdf.format(Date(timestamp))
}
