package com.yjtzc.bluelink.ui.mine

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yjtzc.bluelink.ui.mine.DocumentPickerSection
import com.yjtzc.bluelink.ui.mine.components.MineNavScaffold
import com.yjtzc.bluelink.ui.mine.components.MineSectionTitle
import com.yjtzc.bluelink.ui.mine.components.scaledFontSize

private val RiceWhite = Color(0xFFFAF7F2)
private val CardBg = Color(0xCCFFFDF8)
private val CardBorder = Color(0xEBE5E0D8)
private val DangerRed = Color(0xFFB8322A)
private val DividerColor = Color(0xB8D6CFC4)
private val TextPrimary = Color(0xFF10213B)
private val TextSecondary = Color(0xFF66686D)

@Composable
fun PermanentDeleteScreen(
    viewModel: DataManagementViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.deleteState.collectAsStateWithLifecycle()
    val dbDocs by viewModel.allDocuments.collectAsStateWithLifecycle(initialValue = emptyList())
    val documents = remember(dbDocs) {
        if (dbDocs.isNotEmpty()) dbDocs
        else listOf(
            com.yjtzc.bluelink.data.local.db.DocumentEntity(id = "deep-work", title = "Deep Work 深度工作笔记", privacyLevel = com.yjtzc.bluelink.data.local.db.PrivacyLevel.LOCAL_FIRST),
            com.yjtzc.bluelink.data.local.db.DocumentEntity(id = "design-thinking", title = "Design Thinking 产品设计", privacyLevel = com.yjtzc.bluelink.data.local.db.PrivacyLevel.LOCAL_FIRST),
            com.yjtzc.bluelink.data.local.db.DocumentEntity(id = "feynman", title = "Feynman 费曼学习法资料", privacyLevel = com.yjtzc.bluelink.data.local.db.PrivacyLevel.LOCAL_FIRST),
            com.yjtzc.bluelink.data.local.db.DocumentEntity(id = "rag-notes", title = "RAG 知识库方案", privacyLevel = com.yjtzc.bluelink.data.local.db.PrivacyLevel.LOCAL_FIRST),
            com.yjtzc.bluelink.data.local.db.DocumentEntity(id = "sleep-platform", title = "Sleep Platform 睡眠平台材料", privacyLevel = com.yjtzc.bluelink.data.local.db.PrivacyLevel.LOCAL_FIRST)
        )
    }

    MineNavScaffold(title = "永久删除", onBack = onBack) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().background(RiceWhite),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 120.dp)
        ) {
            item { WarningCard() }
            item { MineSectionTitle("删除范围") }
            item { DeleteScopeCard(scope = state.scope, onScopeChange = viewModel::setDeleteScope) }

            if (state.scope == ItemScope.SELECTED) {
                item { MineSectionTitle("选择文档删除") }
                item { DocumentPickerSection(documents = documents, selectedIds = state.selectedDocumentIds, onToggleDoc = viewModel::toggleDeleteDocument) }
            }

            item { MineSectionTitle("将删除") }
            item { DeleteTargetCard(scope = state.scope) }

            item {
                OutlinedTextField(
                    value = state.confirmText,
                    onValueChange = viewModel::setConfirmText,
                    label = { Text("输入 DELETE 确认删除", fontSize = scaledFontSize(14.sp)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = DangerRed, cursorColor = DangerRed),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = scaledFontSize(14.sp))
                )

                if (!state.hasTypedConfirm && state.confirmText.isNotEmpty()) {
                    Text("请输入 DELETE 确认删除。", color = DangerRed, fontSize = scaledFontSize(12.sp))
                }
                if (state.scope == ItemScope.SELECTED && state.selectedDocumentIds.isEmpty()) {
                    Text("请选择至少一个要删除的文档，或切换为删除全部数据。", color = DangerRed, fontSize = scaledFontSize(12.sp))
                }

                Spacer(Modifier.height(20.dp))

                // 永久删除按钮
                Surface(
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                        .then(if (state.canDelete) Modifier.clickable(onClick = viewModel::performDelete) else Modifier),
                    shape = RoundedCornerShape(18.dp),
                    color = if (state.canDelete) DangerRed else DangerRed.copy(alpha = 0.3f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("永久删除", color = Color.White, fontSize = scaledFontSize(15.sp), fontWeight = FontWeight.Bold)
                    }
                }

                val errMsg = state.errorMessage
                if (errMsg != null) { Text(text = errMsg, color = DangerRed, fontSize = scaledFontSize(12.sp)) }
                Spacer(Modifier.height(80.dp))
            }
        }
    }
}

@Composable
private fun WarningCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp), color = CardBg,
        border = BorderStroke(1.dp, DangerRed.copy(alpha = 0.2f))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            Icon(Icons.Outlined.Warning, contentDescription = null, tint = DangerRed, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("此操作不可撤销", fontSize = scaledFontSize(16.sp), fontWeight = FontWeight.Bold,
                    color = DangerRed, fontFamily = FontFamily.Serif)
                Spacer(Modifier.height(4.dp))
                Text("将删除本机上的指定数据。删除后无法从本机恢复。",
                    fontSize = scaledFontSize(12.sp), color = TextSecondary)
            }
        }
    }
}

@Composable
private fun DeleteScopeCard(scope: ItemScope, onScopeChange: (ItemScope) -> Unit) {
    Surface(
        shape = RoundedCornerShape(18.dp), color = CardBg,
        border = BorderStroke(1.dp, CardBorder)
    ) {
        Column {
            ScopeRow("删除全部数据", "删除本机上的所有数据", scope == ItemScope.ALL) { onScopeChange(ItemScope.ALL) }
            Box(modifier = Modifier.fillMaxWidth().padding(start = 13.dp, end = 13.dp).height(1.dp).background(DividerColor))
            ScopeRow("选择文档删除", "仅删除指定文档及其关联数据", scope == ItemScope.SELECTED) { onScopeChange(ItemScope.SELECTED) }
        }
    }
}

@Composable
private fun ScopeRow(title: String, desc: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(start = 19.dp, end = 19.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick, colors = RadioButtonDefaults.colors(selectedColor = DangerRed))
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = scaledFontSize(14.5.sp), color = TextPrimary, fontFamily = FontFamily.Serif)
            Text(desc, fontSize = scaledFontSize(12.sp), color = TextSecondary)
        }
    }
}

@Composable
private fun DeleteTargetCard(scope: ItemScope) {
    val items = if (scope == ItemScope.ALL) {
        listOf("Room 本地数据库", "加密存储内容", "用户偏好设置", "图谱节点与关系", "待同步队列", "本地缓存文件")
    } else {
        listOf("选中文档及对应 SegmentEntity", "文档关联的 SecurePrefs 密文", "文档关联的 AnchorEntity 缓存",
            "文档关联的图谱节点与关系", "关联的 PendingSync 记录")
    }
    Surface(
        shape = RoundedCornerShape(18.dp), color = CardBg,
        border = BorderStroke(1.dp, CardBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // 两列布局
            val mid = (items.size + 1) / 2
            val leftCol = items.take(mid)
            val rightCol = items.drop(mid)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    leftCol.forEach { item ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("•", color = DangerRed, fontSize = scaledFontSize(12.sp))
                            Spacer(Modifier.width(8.dp))
                            Text(item, fontSize = scaledFontSize(12.sp), color = TextSecondary)
                        }
                        if (item != leftCol.last()) Spacer(Modifier.height(6.dp))
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    rightCol.forEach { item ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("•", color = DangerRed, fontSize = scaledFontSize(12.sp))
                            Spacer(Modifier.width(8.dp))
                            Text(item, fontSize = scaledFontSize(12.sp), color = TextSecondary)
                        }
                        if (item != rightCol.last()) Spacer(Modifier.height(6.dp))
                    }
                }
            }
        }
    }
}
