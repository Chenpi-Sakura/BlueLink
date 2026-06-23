---
name: style
description: 以 BlueLink 主界面（MineScreen）的视觉风格为基准修复任意页面的字体/间距/卡片/图标排布
metadata:
  type: skill
  tags: [android, compose, ui, style, bluelink]
---

# Style — BlueLink 主界面风格一致性

以 MineScreen（主界面）的视觉参数为基准，统一所有页面的字体、间距、卡片、图标排布。

## 主界面视觉系统 (MineScreen)

### 背景
- 页面背景：`RiceWhite` = `Color(0xFFFAF7F2)`
- 卡片背景：`CardBg` = `Color(0xCCFFFDF8)`（米白半透明）

### 卡片
- 圆角：`RoundedCornerShape(18.dp)`
- 描边：`BorderStroke(1.dp, CardBorder)`
- 卡片内无需额外水平 padding（由外层容器控制）

### 分区标题（MineSectionTitle）
```
fontSize = 16.sp
fontWeight = FontWeight(760)
color = Color(0xFF0A2B5F)
fontFamily = FontFamily.Serif
padding(start = 0.dp, top = 27.dp, bottom = 10.dp)
```

### 设置行文字（rowTS）
```
fontSize = 14.5.sp
color = Color(0xFF10213B)
fontFamily = FontFamily.Serif
```

### 设置行布局 (SettingRow)
- 行高：`54.5.dp`
- 图标尺寸：`21.dp`，颜色 `AccentBlue`（`#0758C7`）
- 图标位置：`padding(start = 19.dp)`，垂直居中 `Alignment.CenterStart`
- 文字区域：`padding(start = 50.dp, end = 19.dp)`
- 分割线：`padding(start = 13.dp, end = 13.dp)`，高度 `1.dp`，`DividerColor`
- 右侧箭头：`"›"`，`fontSize = 28.sp`，`AccentBlue`，`FontWeight.Light`

### 页面标题
```
fontSize = 28.sp
fontWeight = FontWeight(780)
color = DeepBlue (#082653)
fontFamily = FontFamily.Serif
letterSpacing = (-1.2).sp
padding(top = 58.dp)
```

### 页面布局
- LazyColumn `contentPadding` = `PaddingValues(start = 16.dp, end = 16.dp, bottom = 120.dp)`
- 各区段之间间距 = `4.dp`

## 使用方式

在 prompt 末尾加上：`按照 style 修复`

修复流程：

1. **分析目标页面现状**：确定哪些视觉参数偏离主界面标准
2. **对齐字体**：将正文改成 `rowTS`（14.5sp Serif），标题改成 `MineSectionTitle` 风格或 `CardTitle` 风格
3. **对齐间距**：卡片圆角 18dp、分割线偏移 13dp、行高 54.5dp
4. **对齐图标**：21dp 蓝色线性图标，`AccentBlue`，不可用 emoji
5. **对齐颜色**：背景 `RiceWhite`，卡片 `CardBg`，文字 `rowTS` 颜色

## 验收标准

- [ ] 正文使用 14.5sp Serif
- [ ] 分区标题使用 16sp Serif 760 weight
- [ ] 卡片圆角 18dp
- [ ] 图标 21dp AccentBlue 线性 SVG
- [ ] 行高 54.5dp
- [ ] 背景 RiceWhite
- [ ] 无 emoji 图标
- [ ] 构建通过
