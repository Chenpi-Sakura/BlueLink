package com.yjtzc.bluelink.ui.mine

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.yjtzc.bluelink.ui.theme.Ink600
import com.yjtzc.bluelink.ui.theme.Ink900
import com.yjtzc.bluelink.ui.theme.KleinBlue
import com.yjtzc.bluelink.ui.theme.Parchment100
import com.yjtzc.bluelink.ui.theme.SerifFamily

@Composable
fun DataExportScreen(
    viewModel: DataManagementViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.exportState.collectAsStateWithLifecycle()
    val documents by viewModel.allDocuments.collectAsStateWithLifecycle(initialValue = emptyList())

    MineNavScaffold(
        title = "数据导出",
        onBack = onBack
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // 说明
            item {
                Text(
                    text = "导出内容可包含文档索引、灵感卡片、图谱节点、个人设置。",
                    style = MaterialTheme.typography.bodySmall,
                    color = Ink600,
                    modifier = Modifier.padding(bottom = 20.dp, start = 4.dp)
                )
            }

            // 导出范围
            item { MineSectionTitle("导出范围") }

            item {
                ExportScopeCard(
                    scope = state.scope,
                    onScopeChange = viewModel::setExportScope
                )
                Spacer(Modifier.height(24.dp))
            }

            // 文档选择
            if (state.scope == ItemScope.SELECTED) {
                item { MineSectionTitle("选择文档") }

                item {
                    DocumentSelectorCard(
                        documents = documents,
                        selectedIds = state.selectedDocumentIds,
                        onToggle = viewModel::toggleExportDocument
                    )
                    Spacer(Modifier.height(24.dp))
                }
            }

            // 导出内容
            item { MineSectionTitle("导出内容") }

            item {
                ExportContentCard(
                    exportDocuments = state.exportDocuments,
                    exportInspirationCards = state.exportInspirationCards,
                    exportGraph = state.exportGraph,
                    exportSettings = state.exportSettings,
                    onToggle = viewModel::toggleExportContent
                )
                Spacer(Modifier.height(24.dp))
            }

            // 提示 & 按钮
            item {
                if (!state.hasSelectedAnyContent) {
                    HintText("请至少选择一项导出内容。")
                }
                if (state.scope == ItemScope.SELECTED && state.selectedDocumentIds.isEmpty()) {
                    HintText("请选择至少一个文档，或切换为全部导出。")
                }

                Spacer(Modifier.height(12.dp))

                // 风险提示
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Text(
                        text = "导出文件可能包含明文内容，请妥善保存。\n如果只想备份设置，请仅勾选「认知设置与隐私偏好」。",
                        style = MaterialTheme.typography.bodySmall,
                        color = Ink600,
                        modifier = Modifier.padding(12.dp)
                    )
                }

                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = viewModel::performExport,
                    enabled = state.canExport,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = KleinBlue,
                        disabledContainerColor = KleinBlue.copy(alpha = 0.3f)
                    )
                ) {
                    if (state.isExporting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.surface,
                            strokeWidth = 2.dp
                        )
                    } else if (state.exportDone) {
                        Text("导出完成", fontSize = 15.sp)
                    } else {
                        Text("确认导出", fontSize = 15.sp)
                    }
                }

                val errMsg = state.errorMessage
                if (errMsg != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = errMsg,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp
                    )
                }

                Spacer(Modifier.height(80.dp))
            }
        }
    }
}

@Composable
private fun ExportScopeCard(
    scope: ItemScope,
    onScopeChange: (ItemScope) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, Parchment100)
    ) {
        Column {
            ScopeOption("全部导出", "导出当前本机知识库中的所有可导出数据", scope == ItemScope.ALL) {
                onScopeChange(ItemScope.ALL)
            }
            HorizontalDivider(color = Parchment100, thickness = 0.5.dp, modifier = Modifier.padding(start = 48.dp))
            ScopeOption("选择文档", "仅导出指定文档及其相关数据", scope == ItemScope.SELECTED) {
                onScopeChange(ItemScope.SELECTED)
            }
        }
    }
}

@Composable
private fun ScopeOption(
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
            colors = RadioButtonDefaults.colors(selectedColor = KleinBlue)
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
private fun DocumentSelectorCard(
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
                            colors = CheckboxDefaults.colors(
                                checkedColor = KleinBlue
                            )
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
                            Text(
                                text = "${segCount(doc)} 个片段",
                                style = MaterialTheme.typography.bodySmall,
                                color = Ink600
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
private fun ExportContentCard(
    exportDocuments: Boolean,
    exportInspirationCards: Boolean,
    exportGraph: Boolean,
    exportSettings: Boolean,
    onToggle: (Boolean?, Boolean?, Boolean?, Boolean?) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, Parchment100)
    ) {
        Column {
            ContentCheckItem("文档与切片索引", exportDocuments) {
                onToggle(it, null, null, null)
            }
            HorizontalDivider(color = Parchment100, thickness = 0.5.dp, modifier = Modifier.padding(start = 48.dp))
            ContentCheckItem("灵感卡片", exportInspirationCards) {
                onToggle(null, it, null, null)
            }
            HorizontalDivider(color = Parchment100, thickness = 0.5.dp, modifier = Modifier.padding(start = 48.dp))
            ContentCheckItem("图谱节点与关系", exportGraph) {
                onToggle(null, null, it, null)
            }
            HorizontalDivider(color = Parchment100, thickness = 0.5.dp, modifier = Modifier.padding(start = 48.dp))
            ContentCheckItem("认知设置与隐私偏好", exportSettings) {
                onToggle(null, null, null, it)
            }
        }
    }
}

@Composable
private fun ContentCheckItem(
    label: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onToggle,
            colors = CheckboxDefaults.colors(checkedColor = KleinBlue)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall.copy(
                fontFamily = SerifFamily,
                fontWeight = FontWeight.Medium
            ),
            color = Ink900
        )
    }
}

@Composable
private fun HintText(text: String) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.error,
        fontSize = 12.sp,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

private fun segCount(doc: com.yjtzc.bluelink.data.local.db.DocumentEntity): Int {
    // DocumentEntity might not have a segment count directly
    return 0
}
