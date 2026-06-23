package com.yjtzc.bluelink.ui.mine

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yjtzc.bluelink.ui.mine.components.MineNavScaffold
import com.yjtzc.bluelink.ui.theme.KleinBlue as KB

private val CardBg = Color(0xCCFFFDF8)
private val CardBorder = Color(0xEBE5E0D8)
private val BlueTitle = Color(0xFF0A3C85)
private val AccentBlue = Color(0xFF002FA7)
private val InkColor = Color(0xFF30343C)
private val MidGray = Color(0xFF4F535B)
private val ErrorColor = Color(0xFFB44D43)
private val DividerColor = Color(0xB8D6CFC4)

@Composable
fun DataExportScreen(
    viewModel: DataManagementViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.exportState.collectAsStateWithLifecycle()
    val dbDocs by viewModel.allDocuments.collectAsStateWithLifecycle(initialValue = emptyList())
    val documents = remember(dbDocs) {
        if (dbDocs.isNotEmpty()) dbDocs
        else listOf(
            com.yjtzc.bluelink.data.local.db.DocumentEntity(
                id = "doc-1", title = "深度工作笔记",
                privacyLevel = com.yjtzc.bluelink.data.local.db.PrivacyLevel.LOCAL_FIRST
            ),
            com.yjtzc.bluelink.data.local.db.DocumentEntity(
                id = "doc-2", title = "费曼学习法资料",
                privacyLevel = com.yjtzc.bluelink.data.local.db.PrivacyLevel.LOCAL_FIRST
            )
        )
    }

    MineNavScaffold(title = "数据导出", onBack = onBack, titleWeight = FontWeight(760)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // 主卡片
            item {
                Spacer(Modifier.height(10.dp))
                ExportMainCard(
                    scope = state.scope,
                    onScopeChange = viewModel::setExportScope,
                    documents = documents,
                    selectedIds = state.selectedDocumentIds,
                    onToggleDoc = viewModel::toggleExportDocument
                )
                Spacer(Modifier.height(30.dp))
            }

            // 导出内容
            item {
                ExportContentSection(
                    exportDocuments = state.exportDocuments,
                    exportInspirationCards = state.exportInspirationCards,
                    exportSettings = state.exportSettings,
                    onToggleDoc = viewModel::setExportDocuments,
                    onToggleInsp = viewModel::setExportInspirationCards,
                    onToggleSet = viewModel::setExportSettings
                )
            }

            // 警告框
            item {
                Spacer(Modifier.height(30.dp))
                WarningBox()
            }

            // 确认导出 + 取消按钮
            item {
                Spacer(Modifier.height(20.dp))

                if (!state.hasSelectedAnyContent) {
                    Text("请至少选择一项导出内容。", color = ErrorColor, fontSize = 12.sp)
                }
                if (state.scope == ItemScope.SELECTED && state.selectedDocumentIds.isEmpty()) {
                    Text("请选择至少一个文档，或切换为全部导出。", color = ErrorColor, fontSize = 12.sp)
                }

                Spacer(Modifier.height(12.dp))

                Button(
                    onClick = viewModel::performExport,
                    enabled = state.canExport,
                    modifier = Modifier.fillMaxWidth().height(58.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent,
                        disabledContainerColor = Color(0xFFBFC0C4)
                    ),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize().then(
                            if (state.canExport) Modifier.background(
                                Brush.horizontalGradient(listOf(Color(0xFF003FBD), Color(0xFF002FA7))),
                                RoundedCornerShape(18.dp)
                            ) else Modifier
                        ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (state.isExporting) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                        } else if (state.exportDone) {
                            Text("导出完成", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight(720), letterSpacing = 0.08.sp)
                        } else {
                            Text("确认导出", color = if (state.canExport) Color.White else Color(0xC7FFFFFF), fontSize = 20.sp, fontWeight = FontWeight(720), letterSpacing = 0.08.sp)
                        }
                    }
                }

                val errMsg = state.errorMessage
                if (errMsg != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(text = errMsg, color = ErrorColor, fontSize = 12.sp)
                }

                Spacer(Modifier.height(5.dp))

                // 取消按钮
                Surface(
                    modifier = Modifier.fillMaxWidth().height(52.dp).clickable(onClick = onBack),
                    shape = RoundedCornerShape(18.dp),
                    color = Color(0x9EFFFDF8),
                    border = BorderStroke(1.dp, AccentBlue.copy(alpha = 0.72f))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("取消", color = AccentBlue, fontSize = 14.5.sp, fontWeight = FontWeight(650))
                    }
                }

                Spacer(Modifier.height(80.dp))
            }
        }
    }
}

// ====== 主卡片 ======

@Composable
private fun ExportMainCard(
    scope: ItemScope, onScopeChange: (ItemScope) -> Unit,
    documents: List<com.yjtzc.bluelink.data.local.db.DocumentEntity>,
    selectedIds: Set<String>, onToggleDoc: (String) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp), color = CardBg, border = BorderStroke(1.dp, CardBorder)
    ) {
        Column(
            modifier = Modifier.padding(25.dp, 28.dp, 25.dp, 28.dp).heightIn(min = 104.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "导出本地知识库\n导出内容将包含文档索引、灵感卡片、图谱节点、个人设置。",
                fontSize = 14.5.sp, lineHeight = 24.sp, color = MidGray,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(Modifier.height(20.dp))

            // 导出范围按钮
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf("全部导出" to ItemScope.ALL, "选择文档" to ItemScope.SELECTED).forEach { (label, s) ->
                    val sel = scope == s
                    Surface(
                        modifier = Modifier.weight(1f).height(38.dp).clickable { onScopeChange(s) },
                        shape = RoundedCornerShape(40.dp),
                        color = if (sel) AccentBlue else Color(0xB3FFFDF8),
                        border = BorderStroke(1.dp, AccentBlue.copy(alpha = 0.25f))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(label, color = if (sel) Color.White else AccentBlue, fontSize = 14.5.sp)
                        }
                    }
                }
            }

            // 选择文档
            if (scope == ItemScope.SELECTED) {
                Spacer(Modifier.height(14.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp), color = Color(0x80FFFDF8),
                    border = BorderStroke(1.dp, CardBorder)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("选择文档", fontSize = 16.sp, fontWeight = FontWeight(720), color = Color(0xFF0A3F86),
                            fontFamily = FontFamily.Serif,
                            modifier = Modifier.padding(start = 4.dp, bottom = 10.dp))
                        if (documents.isEmpty()) {
                            Text("暂无文档", fontSize = 14.5.sp, color = MidGray)
                        } else {
                            documents.forEachIndexed { idx, doc ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().height(54.dp).clickable { onToggleDoc(doc.id) },
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier.size(21.dp).clip(CircleShape)
                                            .background(if (doc.id in selectedIds) AccentBlue else Color.Transparent)
                                            .then(if (doc.id !in selectedIds) Modifier.border(2.dp, Color(0xFFB9B4AC), CircleShape) else Modifier),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (doc.id in selectedIds) Text("✓", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(Modifier.width(13.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(doc.title.ifBlank { "未命名文档" }, fontSize = 14.5.sp, lineHeight = 20.sp, color = InkColor)
                                        Spacer(Modifier.height(4.dp))
                                        Text("文档", fontSize = 11.sp, color = Color(0xFF777777))
                                    }
                                }
                                if (idx < documents.lastIndex) {
                                    Box(modifier = Modifier.fillMaxWidth().padding(start = 34.dp).height(1.dp).background(DividerColor))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ====== 导出内容区域 ======

@Composable
private fun ExportContentSection(
    exportDocuments: Boolean, exportInspirationCards: Boolean, exportSettings: Boolean,
    onToggleDoc: (Boolean) -> Unit, onToggleInsp: (Boolean) -> Unit, onToggleSet: (Boolean) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // 分割线 + 标题
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(DividerColor.copy(alpha = 0.55f)))
        Text("导出内容", fontSize = 16.sp, fontWeight = FontWeight(720), color = Color(0xFF0A3F86),
            fontFamily = FontFamily.Serif,
            modifier = Modifier.padding(top = 14.dp, bottom = 6.dp, start = 4.dp))

        // 导出内容项
        Surface(
            modifier = Modifier.fillMaxWidth().height(62.dp).clickable { onToggleDoc(!exportDocuments) },
            shape = RoundedCornerShape(14.dp), color = Color(0x8CFFFDF8),
            border = BorderStroke(1.dp, Color(0xE6E5E0D8))
        ) {
            Row(modifier = Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(23.dp).clip(CircleShape)
                        .background(if (exportDocuments) AccentBlue else Color.Transparent)
                        .then(if (!exportDocuments) Modifier.border(2.dp, Color(0xFFB9B4AC), CircleShape) else Modifier),
                    contentAlignment = Alignment.Center
                ) { if (exportDocuments) Text("✓", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                Spacer(Modifier.width(13.dp))
                Text("文档与切片索引", fontSize = 14.5.sp, color = InkColor)
            }
        }
        Spacer(Modifier.height(10.dp))
        Surface(
            modifier = Modifier.fillMaxWidth().height(62.dp).clickable { onToggleInsp(!exportInspirationCards) },
            shape = RoundedCornerShape(14.dp), color = Color(0x8CFFFDF8),
            border = BorderStroke(1.dp, Color(0xE6E5E0D8))
        ) {
            Row(modifier = Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(23.dp).clip(CircleShape)
                        .background(if (exportInspirationCards) AccentBlue else Color.Transparent)
                        .then(if (!exportInspirationCards) Modifier.border(2.dp, Color(0xFFB9B4AC), CircleShape) else Modifier),
                    contentAlignment = Alignment.Center
                ) { if (exportInspirationCards) Text("✓", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                Spacer(Modifier.width(13.dp))
                Text("灵感卡片", fontSize = 14.5.sp, color = InkColor)
            }
        }
        Spacer(Modifier.height(10.dp))
        Surface(
            modifier = Modifier.fillMaxWidth().height(62.dp).clickable { onToggleSet(!exportSettings) },
            shape = RoundedCornerShape(14.dp), color = Color(0x8CFFFDF8),
            border = BorderStroke(1.dp, Color(0xE6E5E0D8))
        ) {
            Row(modifier = Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(23.dp).clip(CircleShape)
                        .background(if (exportSettings) AccentBlue else Color.Transparent)
                        .then(if (!exportSettings) Modifier.border(2.dp, Color(0xFFB9B4AC), CircleShape) else Modifier),
                    contentAlignment = Alignment.Center
                ) { if (exportSettings) Text("✓", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                Spacer(Modifier.width(13.dp))
                Text("认知设置与隐私偏好", fontSize = 14.5.sp, color = InkColor)
            }
        }
    }
}

// ====== 警告框 ======

@Composable
private fun WarningBox() {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color(0xB8FFEEE7)
    ) {
        Row(modifier = Modifier.padding(18.dp, 20.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("⚠", fontSize = 16.sp)
            Spacer(Modifier.width(12.dp))
            Text("导出文件可能包含明文内容，请妥善保存。",
                fontSize = 14.5.sp, lineHeight = 22.sp, color = ErrorColor)
        }
    }
}
