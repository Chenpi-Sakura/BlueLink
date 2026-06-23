package com.yjtzc.bluelink.ui.mine

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.yjtzc.bluelink.ui.mine.components.MineNavScaffold
import com.yjtzc.bluelink.ui.mine.components.scaledFontSize

private val AccentBlue = Color(0xFF002FA7)
private val GrayText = Color(0xFF777777)

data class PermInfo(val label: String, val granted: Boolean, val isBattery: Boolean)

@Composable
fun PermissionManagementScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current

    // 权限状态改为可变，支持 ON_RESUME 刷新
    var permissions by remember { mutableStateOf(computePermissions(context)) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        permissions = computePermissions(context)
    }

    MineNavScaffold(title = "权限管理", onBack = onBack) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth().heightIn(min = 104.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = Color.Transparent,
                    border = BorderStroke(1.dp, Color(0xEBE5E0D8))
                ) {
                    Box(modifier = Modifier.fillMaxSize().background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xB3EBF4FF), Color(0xD1FFFDF8)),
                            start = androidx.compose.ui.geometry.Offset.Zero,
                            end = androidx.compose.ui.geometry.Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                        ), RoundedCornerShape(24.dp)
                    )) {
                        Text("为了保证灵感捕获、OCR 和同步提醒正常工作，\n请检查以下权限。",
                            fontSize = scaledFontSize(14.5.sp), lineHeight = 24.sp,
                            color = Color(0xFF10213B), fontFamily = FontFamily.Serif,
                            modifier = Modifier.padding(24.dp, 22.dp))
                    }
                }
                Spacer(Modifier.height(24.dp))
            }

            item {
                Surface(
                    shape = RoundedCornerShape(23.dp), color = Color(0xCCFFFDF8),
                    border = BorderStroke(1.dp, Color(0xEBE5E0D8))
                ) {
                    Column(modifier = Modifier.padding(18.dp, 20.dp)) {
                        permissions.forEachIndexed { index, p ->
                            PermItem(p, onClick = {
                                if (p.isBattery) openBatterySettings(context)
                                else openAppSettings(context)
                            })
                            if (index < permissions.lastIndex) {
                                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xB8D6CFC4)))
                            }
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
            }

            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp),
                    color = Color.Transparent, border = BorderStroke(1.dp, Color(0xE6CBF1F4))
                ) {
                    Box(modifier = Modifier.fillMaxSize().background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xEBE9F3FF), Color(0xE0FAFDFF)),
                            start = androidx.compose.ui.geometry.Offset.Zero,
                            end = androidx.compose.ui.geometry.Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                        ), RoundedCornerShape(18.dp)
                    )) {
                        Row(modifier = Modifier.padding(18.dp, 20.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(modifier = Modifier.size(34.dp), shape = RoundedCornerShape(40.dp),
                                color = AccentBlue.copy(alpha = 0.1f)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("i", color = AccentBlue, fontSize = scaledFontSize(13.sp), fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Text("Vivo / OriginOS 可能会限制后台同步，建议加入电池优化白名单。",
                                fontSize = scaledFontSize(14.5.sp), lineHeight = 22.sp, fontFamily = FontFamily.Serif,
                                color = Color(0xFF47688F))
                        }
                    }
                }
                Spacer(Modifier.height(80.dp))
            }
        }
    }
}

@Composable
private fun PermItem(p: PermInfo, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().height(52.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Text(if (p.isBattery) "🔋" else if (p.label.contains("相机")) "📷" else if (p.label.contains("麦克")) "🎤" else "🔔",
                fontSize = scaledFontSize(13.sp))
            Spacer(Modifier.width(8.dp))
            Text(p.label, fontSize = scaledFontSize(14.5.sp), fontWeight = FontWeight.SemiBold,
                color = Color(0xFF10213B), fontFamily = FontFamily.Serif)
        }
        Text(if (p.granted) "已允许" else "未开启",
            fontSize = scaledFontSize(13.sp), color = if (p.granted) AccentBlue else GrayText,
            modifier = Modifier.width(64.dp))
        if (!p.granted) {
            Button(onClick = onClick,
                modifier = Modifier.height(34.dp).widthIn(min = 70.dp),
                shape = RoundedCornerShape(40.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (p.label.contains("通知")) AccentBlue else Color.Transparent
                ),
                contentPadding = PaddingValues(horizontal = 12.dp)
            ) {
                Text(if (p.label.contains("通知")) "开启" else "去设置",
                    fontSize = scaledFontSize(13.sp),
                    color = if (p.label.contains("通知")) Color.White else AccentBlue,
                    fontWeight = FontWeight.Medium)
            }
        } else {
            Box(modifier = Modifier.height(34.dp).widthIn(min = 70.dp), contentAlignment = Alignment.Center) {
                Text("已允许", fontSize = scaledFontSize(13.sp), color = GrayText)
            }
        }
    }
}

private fun computePermissions(context: Context): List<PermInfo> = listOf(
    PermInfo("相机权限", ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED, false),
    PermInfo("麦克风权限", ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED, false),
    PermInfo("通知权限",
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED
        else true, false),
    PermInfo("电池优化白名单", {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        pm.isIgnoringBatteryOptimizations(context.packageName)
    }(), true)
)

private fun openAppSettings(context: Context) {
    context.startActivity(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            .setData(Uri.parse("package:${context.packageName}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    )
}

private fun openBatterySettings(context: Context) {
    try {
        context.startActivity(
            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                .setData(Uri.parse("package:${context.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    } catch (e: Exception) {
        context.startActivity(
            Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}
