package com.yjtzc.bluelink.ui.mine

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yjtzc.bluelink.ui.mine.components.MineNavScaffold
import com.yjtzc.bluelink.ui.mine.components.MineSectionTitle
import com.yjtzc.bluelink.ui.theme.DangerRed
import com.yjtzc.bluelink.ui.theme.Ink600
import com.yjtzc.bluelink.ui.theme.Ink900
import com.yjtzc.bluelink.ui.theme.KleinBlue
import com.yjtzc.bluelink.ui.theme.Parchment100
import com.yjtzc.bluelink.ui.theme.SerifFamily

@Composable
fun PermanentDeleteScreen(
    viewModel: DataManagementViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.deleteState.collectAsStateWithLifecycle()
    val documents by viewModel.allDocuments.collectAsStateWithLifecycle(initialValue = emptyList())

    MineNavScaffold(
        title = "永久删除",
        onBack = onBack
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // 危险警告
            item {
                WarningCard()
                Spacer(Modifier.height(24.dp))
            }

            // 删除范围
            item { MineSectionTitle("删除范围") }

            item {
                DeleteScopeCard(
                    scope = state.scope,
                    onScopeChange = viewModel::setDeleteScope
                )
                Spacer(Modifier.height(24.dp))
            }

            // 文档选择
            if (state.scope == ItemScope.SELECTED) {
                item { MineSectionTitle("选择文档删除") }

                item {
                    DeleteDocumentSelectorCard(
                        documents = documents,
                        selectedIds = state.selectedDocumentIds,
                        onToggle = viewModel::toggleDeleteDocument
                    )
                    Spacer(Modifier.height(24.dp))
                }
            }

            // 删除对象
            item { MineSectionTitle("将删除") }

            item {
                DeleteTargetCard(scope = state.scope)
                Spacer(Modifier.height(24.dp))
            }

            // 确认输入 & 按钮
            item {
                OutlinedTextField(
                    value = state.confirmText,
                    onValueChange = viewModel::setConfirmText,
                    label = { Text("输入 DELETE 确认删除") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DangerRed,
                        cursorColor = DangerRed
                    )
                )

                if (!state.hasTypedConfirm && state.confirmText.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "请输入 DELETE 确认删除。",
                        color = DangerRed,
                        fontSize = 12.sp
                    )
                }

                if (state.scope == ItemScope.SELECTED && state.selectedDocumentIds.isEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "请选择至少一个要删除的文档，或切换为删除全部数据。",
                        color = DangerRed,
                        fontSize = 12.sp
                    )
                }

                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = viewModel::performDelete,
                    enabled = state.canDelete,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DangerRed,
                        disabledContainerColor = DangerRed.copy(alpha = 0.3f)
                    )
                ) {
                    if (state.isDeleting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.surface,
                            strokeWidth = 2.dp
                        )
                    } else if (state.deleteDone) {
                        Text("已删除", fontSize = 15.sp)
                    } else {
                        Text("永久删除", fontSize = 15.sp)
                    }
                }

                val errMsg = state.errorMessage
                if (errMsg != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = errMsg,
                        color = DangerRed,
                        fontSize = 12.sp
                    )
                }

                Spacer(Modifier.height(80.dp))
            }
        }
    }
}

@Composable
private fun WarningCard() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = DangerRed.copy(alpha = 0.06f),
        border = BorderStroke(1.dp, DangerRed.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Outlined.Warning,
                contentDescription = null,
                tint = DangerRed,
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "此操作不可撤销",
                    fontFamily = SerifFamily,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = DangerRed
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "将删除本机上的指定数据。删除后无法从本机恢复。",
                    style = MaterialTheme.typography.bodySmall,
                    color = Ink600
                )
            }
        }
    }
}

@Composable
private fun DeleteScopeCard(
    scope: ItemScope,
    onScopeChange: (ItemScope) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, Parchment100)
    ) {
        Column {
            ScopeRow("删除全部数据", "删除本机上的所有数据", scope == ItemScope.ALL) {
                onScopeChange(ItemScope.ALL)
            }
            HorizontalDivider(color = Parchment100, thickness = 0.5.dp, modifier = Modifier.padding(start = 48.dp))
            ScopeRow("选择文档删除", "仅删除指定文档及其关联数据", scope == ItemScope.SELECTED) {
                onScopeChange(ItemScope.SELECTED)
            }
        }
    }
}

@Composable
private fun ScopeRow(
    title: String,
    desc: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(selectedColor = DangerRed)
        )
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontFamily = SerifFamily,
                    fontWeight = FontWeight.Medium
                ),
                color = Ink900
            )
            Text(
                text = desc,
                style = MaterialTheme.typography.bodySmall,
                color = Ink600
            )
        }
    }
}

@Composable
private fun DeleteDocumentSelectorCard(
    documents: List<com.yjtzc.bluelink.data.local.db.DocumentEntity>,
    selectedIds: Set<String>,
    onToggle: (String) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, Parchment100)
    ) {
        Column {
            if (documents.isEmpty()) {
                Text(
                    text = "暂无文档",
                    style = MaterialTheme.typography.bodySmall,
                    color = Ink600,
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                documents.forEachIndexed { index, doc ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onToggle(doc.id) }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = doc.id in selectedIds,
                            onCheckedChange = { onToggle(doc.id) },
                            colors = CheckboxDefaults.colors(checkedColor = DangerRed)
                        )
                        Spacer(Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = doc.title.ifBlank { "未命名文档" },
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontFamily = SerifFamily,
                                    fontWeight = FontWeight.Medium
                                ),
                                color = Ink900
                            )
                        }
                    }
                    if (index < documents.lastIndex) {
                        HorizontalDivider(color = Parchment100, thickness = 0.5.dp, modifier = Modifier.padding(start = 48.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun DeleteTargetCard(scope: ItemScope) {
    val items = if (scope == ItemScope.ALL) {
        listOf(
            "Room 本地数据库",
            "加密存储内容",
            "用户偏好设置",
            "图谱节点与关系",
            "待同步队列",
            "本地缓存文件"
        )
    } else {
        listOf(
            "选中文档及对应 SegmentEntity",
            "文档关联的 SecurePrefs 密文",
            "文档关联的 AnchorEntity 缓存",
            "文档关联的图谱节点与关系",
            "关联的 PendingSync 记录"
        )
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, Parchment100)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            items.forEachIndexed { index, item ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("•", color = DangerRed, fontSize = 12.sp)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = item,
                        style = MaterialTheme.typography.bodySmall,
                        color = Ink600
                    )
                }
                if (index < items.lastIndex) {
                    Spacer(Modifier.height(6.dp))
                }
            }
        }
    }
}
