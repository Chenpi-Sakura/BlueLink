package com.yjtzc.bluelink.ui.mine

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yjtzc.bluelink.ui.mine.components.MineNavScaffold
import kotlinx.coroutines.delay
import org.json.JSONArray

private val CardBg = Color(0xCCFFFDF8)
private val CardBorder = Color(0xEBE5E0D8)
private val BlueTitle = Color(0xFF0A3F86)
private val DescText = Color(0xFF707176)
private val DeepText = Color(0xFF10213B)
private val SelectedBlue = Color(0xFF064AA9)
private val SelectedBorder = Color(0xAE0048B4)
private val OffChipBorder = Color(0xE6D6CFC4)
private val AccentBlue = Color(0xFF0758C7)

private val ALL_TERMS = listOf("计算机", "认知科学", "论文阅读", "产品设计")

@Composable
fun CognitiveSettingsScreen(
    viewModel: MineViewModel,
    onBack: () -> Unit
) {
    val profile by viewModel.profile.collectAsStateWithLifecycle()

    val selectedTerms = remember(profile.terminologyTags) {
        try {
            val arr = JSONArray(profile.terminologyTags)
            (0 until arr.length()).map { arr.getString(it) }.toSet()
        } catch (_: Exception) { emptySet() }
    }

    MineNavScaffold(
        title = "认知设置", onBack = onBack,
        titleColor = BlueTitle, titleSize = 30.sp,
        titleWeight = FontWeight.Bold
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // 说明文案（严格两行）
            item {
                Text(
                    text = "调整蓝链提供线索的方式\n让 AI 更像索引，而不是答案生成器",
                    fontSize = 16.sp, lineHeight = 26.sp,
                    color = Color(0xFF66686D),
                    modifier = Modifier.padding(bottom = 24.dp)
                )
            }

            item {
                BlockCard {
                    CardTitle("默认溯源粒度")
                    GranularitySegWide(selected = profile.defaultGranularity, onSelect = viewModel::setGranularity)
                }
            }
            item {
                BlockCard {
                    CardTitle("提示明确度")
                    DirectnessSliderWide(value = profile.directnessLevel, onValueChange = viewModel::setDirectness)
                }
            }
            item {
                BlockCard {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            CardTitle("探索深度")
                            Spacer(Modifier.height(8.dp))
                            Text("开启后会优先显示关联争议和对应观点", fontSize = 14.5.sp, color = DescText, maxLines = 1)
                        }
                        Spacer(Modifier.width(8.dp))
                        CognitiveSwitch(checked = profile.exploreDepth, onToggle = viewModel::toggleExploreDepth)
                    }
                }
            }
            item {
                BlockCard {
                    CardTitle("伴读风格")
                    ToneSegRow(selected = profile.companionStyle, onSelect = viewModel::setCompanionStyle)
                }
            }
            item {
                BlockCard {
                    CardTitle("术语偏好")
                    TerminologyChips(selectedTerms = selectedTerms, onTermsChanged = { tags ->
                        viewModel.setTerminologyTags(JSONArray(tags.toList()).toString())
                    })
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

// ====== 卡片组件 ======

@Composable
private fun BlockCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp), color = CardBg,
        border = BorderStroke(1.dp, CardBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
    Spacer(Modifier.height(18.dp))
}

@Composable
private fun CardTitle(text: String) {
    Text(
        text = text,
        fontSize = 18.5.sp, lineHeight = 26.sp,
        fontWeight = FontWeight(740), color = BlueTitle,
        fontFamily = FontFamily.Serif,
        letterSpacing = (-0.02).sp,
        modifier = Modifier.padding(bottom = 12.dp)
    )
}

// ====== 默认溯源粒度 ======

@Composable
private fun GranularitySegWide(selected: String, onSelect: (String) -> Unit) {
    val options = listOf("文章级" to "PARAGRAPH", "句词级" to "SENTENCE")
    Surface(
        modifier = Modifier.fillMaxWidth().height(46.dp),
        shape = RoundedCornerShape(13.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, Color(0xE6D9D3CA))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            options.forEach { (label, value) ->
                val isSelected = selected == value
                Box(
                    modifier = Modifier.weight(1f).fillMaxHeight()
                        .clip(RoundedCornerShape(12.dp))
                        .then(if (isSelected) Modifier.background(
                            Brush.verticalGradient(listOf(Color(0xFF075BC5), Color(0xFF003E9D))),
                            RoundedCornerShape(12.dp)
                        ) else Modifier)
                        .clickable { onSelect(value) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(label, color = if (isSelected) Color.White else DeepText,
                        fontSize = 16.5.sp, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        fontFamily = FontFamily.Serif, letterSpacing = 0.02.sp)
                }
            }
        }
    }
}

// ====== 提示明确度 ======

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DirectnessSliderWide(value: Float, onValueChange: (Float) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().heightIn(min = 66.dp)) {
        Slider(
            value = value, onValueChange = onValueChange, valueRange = 0f..1f,
            colors = SliderDefaults.colors(
                thumbColor = AccentBlue,
                activeTrackColor = AccentBlue,
                inactiveTrackColor = Color(0xFFDEDCD8)
            ),
            thumb = {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(AccentBlue)
                )
            },
            modifier = Modifier.fillMaxWidth().height(34.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("温和启发", fontSize = 14.sp, color = Color(0xFF666666))
            Text("直指核心", fontSize = 14.sp, color = Color(0xFF666666))
        }
    }
}

// ====== Switch ======

@Composable
private fun CognitiveSwitch(checked: Boolean, onToggle: () -> Unit) {
    val switchBg = if (checked)
        Modifier.background(Brush.horizontalGradient(listOf(Color(0xFF0047B9), Color(0xFF015BD3))), RoundedCornerShape(40.dp))
    else Modifier.background(Color(0xFFD8D5CF), RoundedCornerShape(40.dp))
    Box(
        modifier = Modifier.width(42.dp).height(23.dp).clip(RoundedCornerShape(40.dp)).then(switchBg).clickable(onClick = onToggle),
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Box(modifier = Modifier.padding(2.dp).size(19.dp).clip(CircleShape).background(Color(0xFFFAF7F2)))
    }
}

// ====== 伴读风格 ======

@Composable
private fun ToneSegRow(selected: String, onSelect: (String) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        val options = listOf("温和启发" to "GENTLE", "直指核心" to "DIRECT")
        options.forEach { (label, value) ->
            val isSelected = selected == value
            Surface(
                modifier = Modifier.weight(1f).height(46.dp)
                    .then(if (isSelected) Modifier.background(
                        Brush.verticalGradient(listOf(Color(0xBDEEF6FF), Color(0xB3FFFDF8))),
                        RoundedCornerShape(12.dp)
                    ) else Modifier)
                    .clickable { onSelect(value) },
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) Color.Transparent else Color(0x94FFFDF8),
                border = BorderStroke(1.dp, if (isSelected) SelectedBorder else Color(0xD1D9D3CA))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(label, color = if (isSelected) SelectedBlue else Color(0xFF606164),
                        fontSize = 16.sp, fontWeight = if (isSelected) FontWeight(640) else FontWeight.Normal,
                        fontFamily = FontFamily.Serif)
                }
            }
        }
    }
}

// ====== 术语偏好 ======

@Composable
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
private fun TerminologyChips(selectedTerms: Set<String>, onTermsChanged: (Set<String>) -> Unit) {
    var showEditor by remember { mutableStateOf(false) }
    var inputText by remember { mutableStateOf("") }
    var toastMsg by remember { mutableStateOf<String?>(null) }

    Column {
        androidx.compose.foundation.layout.FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 默认术语 + 用户自定义术语
            val allVisibleTerms = (ALL_TERMS + (selectedTerms - ALL_TERMS.toSet()).toList()).distinct()
            allVisibleTerms.forEach { tag ->
                val isSelected = tag in selectedTerms
                Surface(
                    modifier = Modifier.height(30.dp)
                        .then(if (isSelected) Modifier.background(
                            Brush.verticalGradient(listOf(Color(0xBDEEF6FF), Color(0xB3FFFDF8))),
                            RoundedCornerShape(9.dp)
                        ) else Modifier)
                        .clickable {
                            onTermsChanged(if (isSelected) selectedTerms - tag else selectedTerms + tag)
                        },
                    shape = RoundedCornerShape(9.dp),
                    color = if (isSelected) Color.Transparent else Color(0x40FFFFFF),
                    border = BorderStroke(1.dp, if (isSelected) SelectedBorder else OffChipBorder)
                ) {
                    Text(tag, color = if (isSelected) SelectedBlue else Color(0xFF6B6B6B),
                        fontSize = 14.sp, fontWeight = if (isSelected) FontWeight(560) else FontWeight.Normal,
                        fontFamily = FontFamily.Serif, modifier = Modifier.padding(horizontal = 15.dp))
                }
            }
            // + button
            Surface(
                modifier = Modifier.size(31.dp).clickable { showEditor = true },
                shape = RoundedCornerShape(10.dp),
                color = Color.Transparent,
                border = BorderStroke(1.dp, Color(0x9E0048B4))
            ) {
                Box(
                    modifier = Modifier.fillMaxSize().background(
                        Brush.verticalGradient(listOf(Color(0xAEEEF6FF), Color(0xAFFFFDF8))),
                        RoundedCornerShape(10.dp)
                    ),
                    contentAlignment = Alignment.Center
                ) {
                    Text("＋", color = SelectedBlue, fontSize = 20.sp)
                }
            }
        }

        // 输入编辑器
        if (showEditor) {
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text("添加术语，如：操作系统", fontSize = 14.sp) },
                    singleLine = true,
                    modifier = Modifier.weight(1f).height(38.dp),
                    shape = RoundedCornerShape(12.dp),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentBlue.copy(alpha = 0.55f),
                        unfocusedBorderColor = OffChipBorder
                    )
                )
                Button(
                    onClick = {
                        val trimmed = inputText.trim()
                        if (trimmed.isEmpty()) return@Button
                        val allTerms = ALL_TERMS.toSet() + selectedTerms
                        if (trimmed in allTerms) {
                            toastMsg = "该术语已存在"
                            return@Button
                        }
                        onTermsChanged(selectedTerms + trimmed)
                        inputText = ""
                        showEditor = false
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                    modifier = Modifier.height(38.dp)
                ) {
                    Text("添加", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Surface(
                    modifier = Modifier.height(38.dp).clickable { showEditor = false; inputText = "" },
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Transparent,
                    border = BorderStroke(1.dp, Color(0xCCD6CFC4))
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 16.dp)) {
                        Text("取消", color = Color(0xFF6B6B6B), fontSize = 14.sp)
                    }
                }
            }
        }

        // Toast
        if (toastMsg != null) {
            LaunchedEffect(toastMsg) {
                kotlinx.coroutines.delay(1600)
                toastMsg = null
            }
            Text(
                text = toastMsg!!,
                color = AccentBlue,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}
