---
name: compose-ui-fix
description: 根据详细的文字规格说明修复 Android Compose UI 页面，不依赖截图，不改 HTML/后端
metadata:
  type: skill
  tags: [android, compose, ui, fix]
---

# Compose UI Fix

根据纯文字规格说明修复 Android Jetpack Compose UI 代码，不依赖截图、不改 HTML、不用 Python、不改后端。

## 使用方式

在 prompt 中提供以下内容：

1. **目标页面**：页面名称和文件路径
2. **当前问题**：列出具体的 UI 问题（如文字被裁切、间距错误、组件样式不对等）
3. **规格说明**：详细的视觉/交互规格，包含具体数值（dp/sp/颜色值）

## 工作流程

### 第一步：理解问题

仔细阅读用户提供的规格说明，提取关键参数：
- 尺寸（dp、sp）
- 颜色（Hex）
- 间距（padding、margin、gap）
- 组件行为（点击、切换、折叠等）

### 第二步：定位代码

搜索目标页面文件，理解当前实现结构：
- 主 Composable 函数
- 子组件拆分
- 状态管理
- 数据流向

### 第三步：逐项修复

按优先级修复：

1. **裁切/不可见问题**（最高优先级）
   - 检查 `height` 是否过小
   - 检查 `clip`/`clipToBounds` 是否裁切内容
   - 检查 `lineHeight` 是否导致文字被截断
   - 检查 `maxLines` + `overflow` 组合
   - 检查父容器是否有固定高度限制

2. **对齐与间距问题**
   - 使用 `Arrangement.Center` 垂直居中
   - 使用 `verticalAlignment = Alignment.CenterVertically`
   - 标题与副标题间距 2.dp ~ 4.dp
   - 组件的 padding 使用规格中的具体数值

3. **组件样式问题**
   - 胶囊按钮：`height(24.dp)` + `RoundedCornerShape(40.dp)`，不用 `fillMaxWidth`
   - 自定义 Check：`CircleShape` + 21.dp 尺寸 + 自定义背景
   - 折叠箭头：用 `›` 字符 + `rotate` 旋转
   - 字母块：24.dp × 24.dp + 浅蓝底 + 8.dp 圆角

4. **颜色 Token 化**
   - 优先使用项目中已有的 Token/Theme 颜色
   - 没有现成 Token 的用 Hex 直接定义 `Color(0xFF......)`

5. **间距紧凑化**
   - 标题/文字间 `Spacer(3.dp)` 或 `2.dp`
   - `lineHeight` 设小（如 14.sp 文字用 17.sp lineHeight）
   - 行高不设死，或用刚好容纳内容的数值

6. **交互修复**
   - 文档勾选切换
   - 分组折叠/展开
   - 字母筛选

### 第四步：自检清单

完成后逐项检查：

- [ ] 所有文字完整可见，无裁切
- [ ] 标题与副标题间距正确（〜3dp）
- [ ] 胶囊按钮高度 24dp，不铺满全行
- [ ] 自定义 Check 为 21dp 正圆
- [ ] 分组按 A-Z 英文首字母
- [ ] 分组可折叠展开，箭头方向正确
- [ ] 各区域之间有明显隔离（分割线/间距）
- [ ] 确认按钮在条件不满足时灰色禁用
- [ ] 未改 HTML/后端
- [ ] 代码构建通过

### 第五步：验收

让用户确认修复效果，按需调整。
