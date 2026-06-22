package com.yjtzc.bluelink.ui.mine.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yjtzc.bluelink.ui.theme.MineCard
import com.yjtzc.bluelink.ui.theme.MineColors
import com.yjtzc.bluelink.ui.theme.MineExport
import com.yjtzc.bluelink.ui.theme.MinePage
import com.yjtzc.bluelink.ui.theme.MinePermission
import com.yjtzc.bluelink.ui.theme.MinePrivacy
import com.yjtzc.bluelink.ui.theme.MineShapes
import com.yjtzc.bluelink.ui.theme.MineTypography

// ====================================================================
// 1. BlueLinkSubPageScaffold — 二级页面通用容器
// ====================================================================

@Composable
fun BlueLinkSubPageScaffold(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MineColors.Rice)
            .systemBarsPadding()
    ) {
        // 顶部栏（Row 布局，更可靠）
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(MinePage.TopBarHeight),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .padding(start = 8.dp)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "‹",
                    color = MineColors.Ink,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Light,
                    fontFamily = FontFamily.Serif
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = title,
                color = MineColors.Ink,
                fontSize = MineTypography.PageTitleSize,
                lineHeight = MineTypography.PageTitleLineHeight,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Serif
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = MinePage.HorizontalPadding),
            content = content
        )
    }
}

// ====================================================================
// 2. BlueLinkCard — 通用卡片容器
// ====================================================================

@Composable
fun BlueLinkCard(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = MineShapes.Card,
    contentPadding: PaddingValues = PaddingValues(MineCard.InnerPadding),
    border: BorderStroke = BorderStroke(MineCard.BorderWidth, MineColors.Line.copy(alpha = 0.55f)),
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        color = MineColors.Paper.copy(alpha = 0.80f),
        border = border
    ) {
        Column(modifier = Modifier.padding(contentPadding), content = content)
    }
}

// ====================================================================
// 3. BlueLinkCardTitle — 卡片标题
// ====================================================================

@Composable
fun BlueLinkCardTitle(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        fontSize = MineTypography.CardTitleSize,
        lineHeight = MineTypography.CardTitleLineHeight,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF0A2B5F),
        fontFamily = FontFamily.Serif,
        modifier = modifier
    )
}

// ====================================================================
// 4. BlueLinkPrivacyOption — 三种隐私等级选项
// ====================================================================

@Composable
fun BlueLinkPrivacyOption(
    title: String,
    code: String,
    desc: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit = {},
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(MinePrivacy.OptionHeight)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(15.dp),
        color = if (selected) Color.Transparent else MineColors.Paper.copy(alpha = 0.56f),
        border = BorderStroke(
            width = if (selected) 1.4.dp else 1.dp,
            color = if (selected) MineColors.ActionBlue else MineColors.Line.copy(alpha = 0.88f)
        )
    ) {
        Box {
            if (selected) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xDBEDF6FF), Color(0xE0FFFDF8))
                            ),
                            RoundedCornerShape(15.dp)
                        )
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 15.dp, end = 15.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(MinePrivacy.IconSize),
                    contentAlignment = Alignment.Center
                ) { icon() }
                Spacer(Modifier.width(14.dp))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = title,
                            fontSize = MineTypography.PrivacyOptionTitleSize,
                            fontWeight = FontWeight(700),
                            color = Color(0xFF1F242C)
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = code,
                            fontSize = 12.sp,
                            color = MineColors.KleinBlue,
                            fontFamily = FontFamily.Serif,
                            letterSpacing = 0.45.sp
                        )
                    }
                    Spacer(Modifier.height(5.dp))
                    Text(
                        text = desc,
                        fontSize = MineTypography.PrivacyOptionDescSize,
                        lineHeight = 18.sp,
                        maxLines = 2,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Clip,
                        color = Color(0xFF4F535B)
                    )
                }
                Spacer(Modifier.width(10.dp))
                // 自定义 Radio
                Box(
                    modifier = Modifier
                        .size(MinePrivacy.RadioSize)
                        .clip(CircleShape)
                        .then(
                            if (selected) Modifier.background(MineColors.ActionBlue)
                            else Modifier.border(2.dp, Color(0xFFB9B1A7), CircleShape)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (selected) {
                        Box(
                            modifier = Modifier
                                .size(MinePrivacy.RadioInnerSize)
                                .clip(CircleShape)
                                .background(MineColors.ActionBlue)
                        )
                    }
                }
            }
        }
    }
}

// ====================================================================
// 5. BlueLinkPermissionStatusRow — 隐私页权限状态行
// ====================================================================

@Composable
fun BlueLinkPermissionStatusRow(
    label: String,
    status: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit = {}
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(MinePrivacy.StatusRowHeight),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(MinePermission.IconSize),
                contentAlignment = Alignment.Center
            ) { icon() }
            Spacer(Modifier.width(13.dp))
            Text(
                text = label,
                fontSize = 15.5.sp,
                color = Color(0xFF202530),
                modifier = Modifier.weight(1f)
            )
            Surface(
                shape = MineShapes.Pill,
                color = if (enabled) MineColors.KleinBlue.copy(alpha = 0.08f)
                else Color(0xB8E9E5DD),
                modifier = Modifier
                    .height(MinePermission.PillHeight)
                    .widthIn(min = 64.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(horizontal = 12.dp)
                ) {
                    Text(
                        text = status,
                        color = if (enabled) MineColors.KleinBlue else Color(0xFF595959),
                        fontSize = MineTypography.SmallTextSize
                    )
                }
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(MineColors.Line.copy(alpha = 0.62f))
        )
    }
}

// ====================================================================
// 6. BlueLinkPermissionManageRow — 权限管理页列表行
// ====================================================================

@Composable
fun BlueLinkPermissionManageRow(
    label: String,
    state: String,
    buttonText: String,
    primary: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(MinePermission.RowHeight),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = MineTypography.PermissionNameSize,
            color = MineColors.TextPrimary,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = state,
            fontSize = MineTypography.PermissionStatusSize,
            color = if (state == "已允许") MineColors.KleinBlue else MineColors.TextMuted,
            modifier = Modifier.width(64.dp)
        )
        if (primary) {
            Surface(
                modifier = Modifier
                    .height(MinePermission.ButtonHeight)
                    .widthIn(min = 70.dp)
                    .clickable(onClick = onClick),
                shape = MineShapes.Pill,
                color = MineColors.KleinBlue
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = buttonText,
                        color = Color.White,
                        fontSize = MineTypography.PermissionStatusSize,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }
            }
        } else {
            Surface(
                modifier = Modifier
                    .height(MinePermission.ButtonHeight)
                    .widthIn(min = 70.dp)
                    .clickable(onClick = onClick),
                shape = MineShapes.Pill,
                color = Color.Transparent,
                border = BorderStroke(1.dp, MineColors.KleinBlue)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = buttonText,
                        color = MineColors.KleinBlue,
                        fontSize = MineTypography.PermissionStatusSize,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }
            }
        }
    }
}

// ====================================================================
// 7. BlueLinkPrimaryButton — 主操作按钮
// ====================================================================

@Composable
fun BlueLinkPrimaryButton(
    text: String,
    enabled: Boolean = true,
    height: Dp = MineExport.ButtonHeight,
    shape: RoundedCornerShape = MineShapes.CardSmall,
    modifier: Modifier = Modifier,
    icon: @Composable (() -> Unit)? = null,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
        shape = shape,
        color = if (enabled) Color.Transparent else Color(0xFFBFC0C4)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (enabled) Modifier.background(
                        Brush.horizontalGradient(listOf(Color(0xFF004FD0), Color(0xFF002FA7))),
                        shape
                    ) else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (icon != null) icon()
                Text(
                    text = text,
                    color = if (enabled) Color.White else Color(0xC7FFFFFF),
                    fontSize = MineTypography.ButtonTextSize,
                    fontWeight = FontWeight(760),
                    letterSpacing = 0.06.sp
                )
            }
        }
    }
}

// ====================================================================
// 8. BlueLinkOutlineButton — 描边按钮
// ====================================================================

@Composable
fun BlueLinkOutlineButton(
    text: String,
    height: Dp = MineExport.CancelHeight,
    shape: RoundedCornerShape = MineShapes.CardSmall,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clickable(onClick = onClick),
        shape = shape,
        color = MineColors.Paper.copy(alpha = 0.62f),
        border = BorderStroke(1.dp, MineColors.KleinBlue.copy(alpha = 0.72f))
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                color = MineColors.KleinBlue,
                fontSize = MineTypography.ButtonTextSize,
                fontWeight = FontWeight(650)
            )
        }
    }
}
