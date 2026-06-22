package com.yjtzc.bluelink.ui.mine

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.BatterySaver
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.yjtzc.bluelink.ui.mine.components.MineNavScaffold
import com.yjtzc.bluelink.ui.mine.components.MineSectionTitle
import com.yjtzc.bluelink.ui.theme.Ink600
import com.yjtzc.bluelink.ui.theme.Ink900
import com.yjtzc.bluelink.ui.theme.KleinBlue
import com.yjtzc.bluelink.ui.theme.Parchment100
import com.yjtzc.bluelink.ui.theme.SerifFamily

data class PermissionItem(
    val icon: ImageVector,
    val title: String,
    val isGranted: Boolean,
    val grantedLabel: String = "已允许",
    val deniedLabel: String = "未开启",
    val actionLabel: String = "去设置"
)

@Composable
fun PermissionManagementScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val permissions = remember(context) {
        listOf(
            PermissionItem(
                icon = Icons.Outlined.CameraAlt,
                title = "相机权限",
                isGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                        android.content.pm.PackageManager.PERMISSION_GRANTED
            ),
            PermissionItem(
                icon = Icons.Outlined.Mic,
                title = "麦克风权限",
                isGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                        android.content.pm.PackageManager.PERMISSION_GRANTED
            ),
            PermissionItem(
                icon = Icons.Outlined.Notifications,
                title = "通知权限",
                isGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                            android.content.pm.PackageManager.PERMISSION_GRANTED
                } else {
                    true // 低版本默认允许
                },
                actionLabel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) "开启" else "系统默认"
            ),
            PermissionItem(
                icon = Icons.Outlined.BatterySaver,
                title = "电池优化白名单",
                isGranted = {
                    val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
                    pm.isIgnoringBatteryOptimizations(context.packageName)
                }(),
                grantedLabel = "已加入",
                deniedLabel = "建议开启"
            )
        )
    }

    MineNavScaffold(
        title = "权限管理",
        onBack = onBack
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            item {
                Text(
                    text = "为了保证灵感捕获、OCR 和同步提醒正常工作，请检查以下权限。",
                    style = MaterialTheme.typography.bodySmall,
                    color = Ink600,
                    modifier = Modifier.padding(bottom = 20.dp, start = 4.dp)
                )
            }

            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, Parchment100)
                ) {
                    Column {
                        permissions.forEachIndexed { index, perm ->
                            PermissionRow(
                                item = perm,
                                onActionClick = {
                                    openAppSettings(context)
                                }
                            )
                            if (index < permissions.lastIndex) {
                                HorizontalDivider(
                                    color = Parchment100,
                                    thickness = 0.5.dp
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(Modifier.height(20.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Text(
                        text = "Vivo / OriginOS 可能会限制后台同步，建议加入电池优化白名单。",
                        style = MaterialTheme.typography.bodySmall,
                        color = Ink600,
                        modifier = Modifier.padding(12.dp)
                    )
                }
                Spacer(Modifier.height(80.dp))
            }
        }
    }
}

@Composable
private fun PermissionRow(
    item: PermissionItem,
    onActionClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = null,
            tint = if (item.isGranted) KleinBlue else Ink600,
            modifier = Modifier.size(28.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontFamily = SerifFamily,
                    fontWeight = FontWeight.Medium
                ),
                color = Ink900
            )
            Text(
                text = if (item.isGranted) item.grantedLabel else item.deniedLabel,
                style = MaterialTheme.typography.bodySmall,
                color = if (item.isGranted) KleinBlue else Ink600
            )
        }
        Spacer(Modifier.width(8.dp))

        if (!item.isGranted) {
            TextButton(onClick = onActionClick) {
                Text(
                    text = item.actionLabel,
                    color = KleinBlue,
                    fontSize = 13.sp
                )
            }
        } else {
            Text(
                text = item.grantedLabel,
                fontSize = 12.sp,
                color = KleinBlue
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = Ink600,
            modifier = Modifier.size(20.dp)
        )
    }
}

private fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        .setData(Uri.parse("package:${context.packageName}"))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
}
