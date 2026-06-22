package com.yjtzc.bluelink.ui.mine.components

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import com.yjtzc.bluelink.ui.theme.KleinBlue
import com.yjtzc.bluelink.ui.theme.SerifFamily

@Composable
fun ConfirmPrivacyChangeDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "确认切换隐私等级？",
                fontFamily = SerifFamily,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = "从「仅本地 / 优先本地」切换到「允许云端」会改变数据存储与上传行为：\n\n" +
                        "• 原文可能被解密并上传至云端\n" +
                        "• AI 服务可能接触到原文内容\n" +
                        "• 此操作需要你明确确认"
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = KleinBlue
                )
            ) {
                Text("确认上传")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
