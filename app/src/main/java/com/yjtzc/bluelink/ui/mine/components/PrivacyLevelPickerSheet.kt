package com.yjtzc.bluelink.ui.mine.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yjtzc.bluelink.ui.theme.Ink600
import com.yjtzc.bluelink.ui.theme.Ink900
import com.yjtzc.bluelink.ui.theme.KleinBlue
import com.yjtzc.bluelink.ui.theme.KleinBlueBg
import com.yjtzc.bluelink.ui.theme.SerifFamily

data class PrivacyLevelOption(
    val key: String,
    val label: String,
    val description: String
)

private val levels = listOf(
    PrivacyLevelOption("LOCAL_ONLY", "仅本地", "原文永不上云"),
    PrivacyLevelOption("LOCAL_FIRST", "优先本地", "原文默认保留本机，必要时仅上传脱敏索引"),
    PrivacyLevelOption("CLOUD_OK", "允许云端", "可用于跨设备同步")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyLevelPickerSheet(
    currentLevel: String,
    onLevelSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "选择隐私等级",
                fontFamily = SerifFamily,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Ink900
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "切换后数据存储行为将立即变化",
                style = MaterialTheme.typography.bodySmall,
                color = Ink600
            )
            Spacer(Modifier.height(20.dp))

            levels.forEach { option ->
                val isSelected = currentLevel == option.key
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onLevelSelected(option.key) },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) KleinBlueBg else MaterialTheme.colorScheme.surface,
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) KleinBlue.copy(alpha = 0.3f) else MaterialTheme.colorScheme.outlineVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = option.label,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontFamily = SerifFamily,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = Ink900
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = option.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = Ink600
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        RadioButton(
                            selected = isSelected,
                            onClick = { onLevelSelected(option.key) },
                            colors = RadioButtonDefaults.colors(selectedColor = KleinBlue)
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            Spacer(Modifier.height(8.dp))
            Text(
                text = "切换到更高云端等级后，原文可能被解密上传。",
                style = MaterialTheme.typography.bodySmall,
                color = Ink600
            )
        }
    }
}
