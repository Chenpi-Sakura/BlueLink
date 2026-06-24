package com.yjtzc.bluelink.ui.theme

import androidx.compose.ui.graphics.Color

// ====== 蓝链品牌色板（对齐参考样式） ======

// 主色：克莱因蓝 — 按钮、链接、引力线、锚点边框、高亮边线
val KleinBlue = Color(0xFF002FA7)
// 浅克莱因蓝 — 选中态、高亮背景
val KleinBlueLight = Color(0xFF6B8DFF)
// 克莱因蓝 8% 透明 — 按钮/标签背景
val KleinBlueBg = Color(0x14002FA7)

// 背景色：羊皮纸白 — 模拟纸质阅读感
val Parchment50 = Color(0xFFFDFBF7)
// 暖灰 — 卡片边框、标签背景
val Parchment100 = Color(0xFFF4EFE6)
// 深暖灰 — 分割线、强边框
val Parchment200 = Color(0xFFE8DFD0)

// 纯白 — 卡片表面
val PureWhite = Color(0xFFFFFFFF)

// 文字色（参考 ink 体系）
val Ink900 = Color(0xFF2C2B29)      // 主文本
val Ink600 = Color(0xFF66635D)      // 次要文本
val Ink400 = Color(0xFFA39F98)      // 灰态 / 提示文本

// 语义色
val Vermilion = Color(0xFFEA4335)     // 错误 / 质疑 / 偏差
val SuccessGreen = Color(0xFF34A853)  // 成功 / 支撑

// 磨砂效果用半透明白
val GlassWhite = Color(0xB3FDFBF7)   // 70% 白 — 毛玻璃面板

// 危险色
val DangerRed = Color(0xFFB8322A)    // 危险操作文字/描边
val DangerPale = Color(0xFFFFF1ED)   // 危险操作背景

// 背景 / 分割（旧名称保留，值已对齐）
val RiceWhite = Parchment50
val RiceWarm = Parchment100
val DeepInk = Ink900
val MidGray = Ink600
val LightGray = Ink400
val SandLine = Parchment200
val MistGray = KleinBlueBg          // 30% 透明灰 → 8% 克莱因蓝

// ====== 暗黑模式 ======
val DarkBackground = Color(0xFF1E1E2E)
val DarkSurface = Color(0xFF2A2A38)
val DarkSurfaceVariant = Color(0xFF3A3A48)
val DarkOutline = Color(0xFF50505A)

// ====== 图谱专用色 ======
val GraphDocBlue = Color(0xFF5B8DEF)       // 文献节点
val GraphInspirationYellow = Color(0xFFF0C674) // 灵感节点
