package com.yjtzc.bluelink.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ====================================================================
// BlueLink「我的」模块设计 Token
// 视觉基准：bluelink_test1_selectandmain_permission_note_gap.html
// ====================================================================

// ====== 颜色 ======
object MineColors {
    val Rice = Color(0xFFFAF7F2)
    val Paper = Color(0xFFFFFDF8)
    val Paper2 = Color(0xFFFFFCF6)

    val KleinBlue = Color(0xFF002FA7)
    val ActionBlue = Color(0xFF0758C7)

    val Ink = Color(0xFF082653)
    val TextPrimary = Color(0xFF10213B)
    val TextSecondary = Color(0xFF66686D)
    val TextMuted = Color(0xFF777777)

    val Line = Color(0xFFE5E0D8)
    val Danger = Color(0xFFD5312B)
    val DangerSoft = Color(0xFFB44D43)

    val PaleBlue = Color(0xFFEEF5FF)
}

// ====== 页面布局 ======
object MinePage {
    val HorizontalPadding = 18.dp
    val SubPageTopPadding = 58.dp
    val TopBarHeight = 88.dp
    val BackButtonSize = 38.dp
    val BackIconSize = 28.dp
}

// ====== 卡片 ======
object MineCard {
    val Radius = 24.dp
    val RadiusMedium = 22.dp
    val RadiusSmall = 18.dp
    val ExportRadius = 26.dp
    val BorderWidth = 1.dp
    val InnerPadding = 18.dp
}

// ====== 隐私与安全页 ======
object MinePrivacy {
    val HeroVerticalPadding = 20.dp
    val HeroHorizontalPadding = 18.dp
    val LevelCardPaddingHorizontal = 11.dp
    val LevelCardPaddingTop = 18.dp
    val OptionHeight = 86.dp
    val OptionGap = 10.dp
    val IconSize = 36.dp
    val RadioSize = 22.dp
    val RadioInnerSize = 10.dp
    val StatusRowHeight = 42.dp
    val ExportButtonHeight = 54.dp
    val DeleteEntryHeight = 64.dp
    val SectionGap = 20.dp
}

// ====== 权限管理页 ======
object MinePermission {
    val InfoMinHeight = 104.dp
    val InfoPaddingHorizontal = 24.dp
    val InfoPaddingVertical = 22.dp
    val ListPaddingHorizontal = 18.dp
    val ListPaddingVertical = 20.dp
    val RowHeight = 64.dp
    val StatusRowHeight = 42.dp
    val IconSize = 24.dp
    val PillHeight = 27.dp
    val ButtonHeight = 34.dp
    val NoteGap = 10.dp
}

// ====== 数据导出页 ======
object MineExport {
    val MainCardTopGap = 10.dp
    val MainCardPaddingHorizontal = 25.dp
    val MainCardPaddingVertical = 32.dp
    val DocBadgeSize = 58.dp
    val DocIconSize = 34.dp
    val ScopeButtonHeight = 38.dp
    val DocPickerRadius = 18.dp
    val DocPickerPadding = 12.dp
    val DocItemHeight = 54.dp
    val ContentItemHeight = 62.dp
    val WarningGap = 30.dp
    val ButtonHeight = 58.dp
    val CancelHeight = 52.dp
    val ButtonGap = 5.dp
}

// ====== 字号 ======
object MineTypography {
    val PageTitleSize = 27.sp
    val PageTitleLineHeight = 36.sp
    val CardTitleSize = 20.sp
    val CardTitleLineHeight = 28.sp
    val BodySize = 15.sp
    val BodyLineHeight = 24.sp
    val SmallTextSize = 13.sp
    val TinyTextSize = 12.sp
    val ButtonTextSize = 18.sp
    val ExportTitleSize = 27.sp
    val PrivacyHeroTitleSize = 16.5.sp
    val PrivacyOptionTitleSize = 15.5.sp
    val PrivacyOptionDescSize = 12.2.sp
    val PermissionNameSize = 18.sp
    val PermissionStatusSize = 15.sp
}

// ====== 形状 ======
object MineShapes {
    val Card = RoundedCornerShape(MineCard.Radius)
    val CardMedium = RoundedCornerShape(MineCard.RadiusMedium)
    val CardSmall = RoundedCornerShape(MineCard.RadiusSmall)
    val Pill = RoundedCornerShape(999.dp)
    val ExportCard = RoundedCornerShape(MineCard.ExportRadius)
}
