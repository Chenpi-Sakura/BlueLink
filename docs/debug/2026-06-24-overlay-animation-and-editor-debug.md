# 2026-06-24 覆盖层动画 + InspirationEditor 调试踩坑

> 一次调试跨越多个迭代，最终放弃 AnimatedContent 重写覆盖层，并修掉 InspirationEditor 进入闪动 + 图片丢失。
> 改动文件：`app/src/main/java/com/yjtzc/bluelink/ui/navigation/BlueLinkNavGraph.kt`、`app/src/main/java/com/yjtzc/bluelink/ui/editor/InspirationEditorScreen.kt`

## 背景

feat/animation 分支要做 iOS push 风格的覆盖层动画（一级→二级→三级），同时修复 InspirationEditor 进入时的一系列 bug。本次调试大约经历 14 轮迭代才把动画方向、闪动、图片丢失等问题全部解决。

---

## 阶段 1：动画方向原理探索

**现象**：用户反馈"二级页面从左上角逐步扩展"，不是预期的右滑覆盖。

**踩坑**：
- 一开始用 `AnimatedContent` + `transitionSpec { slideInHorizontally + fadeOut }`，以为逻辑是对的
- 真正根因是**新版 Compose 默认推断 `sizeTransform`**，让 content 从 `(0, 0)` 缩放到全屏——叠加 slide 之后视觉就是"从左上角扩展"

**修复**：
```kotlin
ContentTransform(
    targetContentEnter = slideInHorizontally(...) { it },
    initialContentExit = fadeOut(...),
    sizeTransform = null  // ← 关键：禁掉 size 动画
)
```

**教训**：新版 Compose 的 `togetherWith` / `ContentTransform` 不显式禁掉 `sizeTransform`，默认会让 container size 从 0 动画到 full，导致叠加在 slide 之上的"扩展"伪影。

---

## 阶段 2：退出动画缺失

**现象**：退出二级页面直接闪现，无任何过渡。

**踩坑**：
- `when (target)` 没有 `else` 分支，target = null 时 `when` 命中不到任何分支 → content lambda 不返回任何 Compose 节点
- AnimatedContent 看到 `targetContent` 是"空渲染"，**直接 dispose 旧 content**（不等 exit 动画）

**修复**：
```kotlin
when (target) {
    is Overlay.Reader -> { ... }
    is Overlay.Editor -> { ... }
    is Overlay.Mine -> { ... }
    null -> {
        // 退出覆盖层时：占位 Box 保持 fillMaxSize，让 exit 动画正常跑
    }
}
```
外面再包一层 `Box(Modifier.fillMaxSize())` 保证 targetContent 永远有 size。

**教训**：`AnimatedContent` 的 content lambda 不能返回空——否则 initialContent 会被立即 dispose，exit 动画根本没机会跑。

---

## 阶段 3：嵌套切换无动画

**现象**："我的"→"隐私与安全"→"权限管理"时，"权限管理"无任何动画直接出现。

**踩坑**：
- `overlay: String?` 用 `"reader"` / `"editor"` / `"mine"` 三种 String
- mineRoute 从 PrivacySecurity → PermissionManagement 时 overlay 还是 `"mine"`，**targetState 不变，AnimatedContent 不触发 transition**

**修复**：用 sealed class Overlay，每个 MineRoute 单独的 `Overlay.Mine` 实例（data class 按字段 equals）：
```kotlin
sealed interface Overlay {
    data object Reader : Overlay
    data class Editor(val cardId: String) : Overlay
    data class Mine(val route: MineRoute) : Overlay  // ← 每个 MineRoute 不同实例
}
```

**教训**：AnimatedContent 的 `targetState` 比较用 `equals`。字符串 `"mine"` 永远 equals 自己；data class 按字段 equals，不同 `MineRoute` 就是不同值。

---

## 阶段 4：方向判断 + `None` 的真相

**现象**：用户反馈"二级界面应当保持在原处不做任何效果，直接滑动覆盖"——但 forward 时旧页"直接消失"，backward 时新页"直接出现"。

**踩坑（最大）**：
- 我以为 `ExitTransition.None` = "旧页保持原位不动"
- 实际上新版 Compose 的 `ExitTransition.None` 和 `EnterTransition.None` **真的让 content 立即 dispose / 立即出现**——**不是"保持原位"**
- 这是 `AnimatedContent` API 的根本限制，**没有任何 spec 能实现"旧页保持原位被覆盖"**

**最终方案**：**完全放弃 AnimatedContent**，改用手动 `Box` + `graphicsLayer` 同时管理两个 overlay slot，各自应用独立的 alpha + translationX。

**教训**：用 API 之前必须理解其边界条件。`None` ≠ "无动画"，在 AnimatedContent 中 `None` 是"立即 dispose"。遇到 API 限制时不要硬撑，重写架构比叠 hack 更稳。

---

## 阶段 5：重写覆盖层 — Animatable 替代 animate

**踩坑**：第一版用 `animate(initial, target, tween) { v, _ -> state = v }`，build 失败：
```
Cannot access 'suspend fun <T, V : AnimationVector> AnimationState<T, V>.animate(...)': it is internal in file.
```
`animate()` 是 internal API。

**修复**：用 `Animatable<T>` 类：
```kotlin
val translationX = remember(disp) { Animatable(0f) }
// 动画
LaunchedEffect(activeDirection) {
    translationX.animateTo(screenWidthPx, tween(300))
}
// 读取
Box(.graphicsLayer { this.translationX = translationX.value })
```

**教训**：Compose 动画 API 优先用 `Animatable` / `animate*AsState` / `animateTo` 这种公开 API，不要用 `animate()` 这种 internal 函数。

---

## 阶段 6：首次进入无动画

**现象**：一级→二级直接出现，无 slideIn。

**踩坑**：`AnimatedContent` 的 initial 状态处理写错了——首次进入时 `displayedOverlay == null`，走了"直接显示"分支，绕过了 `pendingOverlay` 流程。

**修复**：
```kotlin
LaunchedEffect(targetOverlay, direction) {
    if (direction != null) {
        pendingOverlay = targetOverlay
        activeDirection = direction
    }
}
// 新 slot slideIn 完成后
if (displayedOverlay == null) {
    displayedOverlay = pend  // 首次进入接管
    pendingOverlay = null
}
```

**教训**：状态机设计时把所有 transition 都走同一套流程，不要为"首次进入"或"没有旧 content"开特殊分支。

---

## 阶段 7：抽动（闪烁）

**现象**：forward 时二级界面"抽动"——用户看到新页瞬间出现 → 跳到屏幕外 → slideIn 回来。

**踩坑**：新 slot 的 `translationX` 初始值是 `0f`（屏幕中央）。第一次渲染时新 slot 已经在屏幕中央（覆盖旧 slot），然后 LaunchedEffect 启动后 `snapTo(screenWidthPx)` 把它跳到屏幕外，旧 slot 露出，最后 `animateTo(0f)` slideIn —— 视觉上"闪 → 露出 → 滑入"。

**修复**：新 slot 的 translationX 初始值就是 `screenWidthPx`（屏幕外右侧）：
```kotlin
val translationX = remember(pend) { Animatable(screenWidthPx) }
// LaunchedEffect 内直接 animateTo(0f)
```

**教训**：动画初始位置要**匹配 enter animation 的起点**。如果 enter 是 slideIn(from right)，初始位置必须在 right，不能是 center。

---

## 阶段 8：zIndex 切换 — backward 时旧 slot 被遮挡

**现象**："三级退出抽动闪现"——PermissionManagement 闪一下就消失。

**踩坑**：新 slot 写在后面，默认 z-order 更高 → backward 时新 slot（PrivacySecurity）覆盖在旧 slot（PermissionManagement）之上 → 旧 slot 的 slideOut + fadeOut 被遮挡，用户只看到新 slot"突然出现"。

**修复**：用 `Modifier.zIndex` 动态控制：
```kotlin
.zIndex(if (activeDirection == Backward) -1f else 1f)
```
- Forward：新 slot zIndex = 1（在旧 slot 之上，slideIn 可见）
- Backward：新 slot zIndex = -1（在旧 slot 之下，旧 slot slideOut 可见，露出新 slot）

**教训**：z-order 不能假设默认顺序符合 iOS push 行为。必须根据 transition 方向**动态控制 zIndex**——backward 时旧 slot 必须在上方才能看到 slideOut。

---

## 阶段 9：dim 效果 — 透明度 vs 遮罩

**用户反馈**：第一次要求"降低透明度做 dim"，第二次更正为"加半透明的黑色遮罩"。

**第一次实现（错的）**：把旧 slot 的 alpha 从 1 渐变到 0.6——但这其实是"变透明"，不是 iOS 风格的 dim。

**第二次修复**：在旧 slot 上加一个独立 alpha 控制的 `Color.Black.copy(alpha)` Box（scrim 遮罩）：
```kotlin
Box(.background(Color.Black.copy(alpha = scrimAlpha.value)))
```

**再修复**：scrim 移到 OverlayLayer 顶层 Box —— 因为首次进入（一级→二级）没有旧 slot，scrim 必须放在最外层才能在所有 transition 中生效。

**教训**：iOS push 的层级感来自**半透明黑色 scrim**，不是降低下方 view 的 alpha。scrim 必须放在**层级结构的正确位置**（底层 Scaffold 之上、覆盖层之下）。

---

## 阶段 10：InspirationEditor 错误提示

**现象**：点击灵感卡，下方弹"灵感不存在或已被删除"，但卡片实际存在。

**踩坑**：
```kotlin
val cards by cardsFlow.collectAsStateWithLifecycle(initialValue = emptyList())
val card = cards.find { it.id == cardId }
if (card != null) { ... } else { showSnackbar("不存在") }
```
`collectAsStateWithLifecycle` 的 `initialValue` 必须是 non-null，首次渲染时 `cards = emptyList()` → `card = null` → 误判。

**修复**：用 `produceState` 把 cards 包成 nullable：
```kotlin
val cardsState = produceState<List<InspirationCardEntity>?>(initialValue = null, cardsFlow) {
    cardsFlow.collect { value = it }
}
// null = 加载中，non-null = 已加载
```

**教训**：`collectAsStateWithLifecycle` 无法表达"加载中 vs 已加载但为空"两种状态。需要 nullable 时用 `produceState` 或 `Flow<T?>`。

---

## 阶段 11：转圈

**现象**：进入卡片后闪一下 `CircularProgressIndicator`。

**踩坑**：`InspirationEditorScreen` 内部的 `if (!isLoaded) CircularProgressIndicator()` —— `isLoaded = false` 初始，进入时显示转圈。

**修复**：用 `card.contentSnippet` **同步初始化 blocks** + `isLoaded = true` 初始：
```kotlin
val blocksState = remember(card.id) {
    mutableStateOf<List<ContentBlock>>(
        try { card.contentSnippet.parseContentBlocks(card.type.name) }
        catch (_: Exception) { emptyList() }
    )
}
var isLoaded by remember { mutableStateOf(true) }
```

**教训**：能用同步 fallback 数据就不要显示 loading spinner。`contentSnippet` 是元数据里的摘要，本来就够用，后台异步加载完整内容是锦上添花。

---

## 阶段 12：图片丢失 + 闪动

**现象**：转圈消失，但又闪一下，图片文件不见。

**根因（最重要）**：
- `card.contentSnippet` 是 **前 30 字摘要**（`Entities.kt:66: // V2.1: 前 30 字摘要`）
- `parseContentBlocks(contentSnippet)` → **1 个 text block**
- `parseContentBlocks(readCardContent())` → **N 个 blocks**（含图片/语音）
- blocks 数从 1 → N → UI 跳变（"闪一下"）+ 图片 block 后续才出现

**修复**：在 OverlayLayer 内**预加载** readCardContent 完整内容，传给 InspirationEditorScreen，让首次渲染就用完整 blocks：
```kotlin
val fullContentState = produceState<String?>(initialValue = null, displayCard.id) {
    value = runCatching { container.captureRepository.readCardContent(displayCard) }.getOrNull()
}
if (fullContent != null) {
    InspirationEditorScreen(card, preloadedContent = fullContent, ...)
}
```

**教训**：摘要数据 ≠ 完整数据。如果读取完整内容是 IO 操作，必须**阻塞等它完成**再渲染，不能先用摘要渲染再用完整内容覆盖。

---

## 阶段 13：Flow emit 空列表 — 闪回主界面

**现象**：进入卡片后短暂看到 HOME 然后回到卡片。

**踩坑**：Room 的 `observeAllCards()` Flow 在数据库 query 中**可能短暂 emit 空列表 `[]`**（不是 `null`，但 `find` 找不到）：
```kotlin
val cards = cardsState.value  // null → [] → [含 card]
val card = cards?.find { it.id == cardId }  // null → null → found
// [] 时进入 else 分支 → LaunchedEffect 关闭 overlay → 闪回 HOME
```

**修复**：用 `remember` **缓存 card**，Flow emit `[]` 时仍用缓存的 card 渲染：
```kotlin
val cachedCard = remember(cardId) { mutableStateOf<InspirationCardEntity?>(null) }
if (card != null) cachedCard.value = card
val displayCard = card ?: cachedCard.value
```

**教训**：Room Flow 在 query 中间态可能短暂 emit 空列表。任何依赖 Flow 结果做"判定卡片是否存在"的逻辑都会被这种中间态坑到。**乐观缓存**是稳妥做法。

---

## 阶段 14：Transparent Box — 最后一道坎

**现象**：缓存 card 后还是闪动，看到 HOME。

**最终根因**：新 slot loading 时显示 `Box(Modifier.fillMaxSize())` —— **没有 background modifier → 默认 transparent**。HOME 直接透过显示（即使有 scrim 蒙黑，transparent Box 让用户透过看到底层的 HOME）。

**修复**：给所有 loading Box 加 opaque background：
```kotlin
Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
```

**教训**：`Box` 默认 transparent。**任何作为"覆盖层"或"loading 占位"的 Box 必须显式给 background**，否则就是一层空气，下层内容会透过来。

---

## 关键经验沉淀

| 类别 | 教训 |
|---|---|
| **API 边界** | 用任何 API 前必须理解其 `None` / 空 / 边界情况的真实行为，不要靠"猜" |
| **状态机设计** | 所有 transition 走同一套流程，不要为"首次进入"或"没有旧 content"开特殊分支 |
| **z-order** | 不能假设默认 composition 顺序符合 iOS push 行为——动态 `zIndex` 控制 |
| **动画初始值** | 初始 position 必须匹配 enter animation 起点，否则首次渲染会闪 |
| **乐观缓存** | Room Flow 在 query 中间态可能短暂 emit 空列表 / 空值，需要缓存兜底 |
| **数据完整性** | 摘要 ≠ 完整数据。IO 操作需要阻塞等它完成，不要先渲染摘要再用完整内容覆盖 |
| **Opaque 覆盖** | 任何作为"覆盖层"或"loading 占位"的 Box 必须显式给 background——默认 transparent |
| **commit 粒度** | 按"逻辑单元"提交，不要攒很多功能一起 commit |
| **重写决策** | 遇到 API 限制时不要硬撑 hack——重写架构比叠 hack 更稳 |

## 相关文件

- `app/src/main/java/com/yjtzc/bluelink/ui/navigation/BlueLinkNavGraph.kt` —— 主改动文件（覆盖层 + OverlayLayer + scrim + InspirationEditor 入口）
- `app/src/main/java/com/yjtzc/bluelink/ui/editor/InspirationEditorScreen.kt` —— `preloadedContent` 参数 + 移除 LaunchedEffect
- `CLAUDE.md` —— 第 6 节分支策略更新为 develop
- `data/local/db/Entities.kt` —— `contentSnippet` 是 30 字摘要（设计决策，影响 IO 加载策略）
- `data/repository/CaptureRepository.kt` —— `readCardContent` 是 suspend 函数

## 后续建议

1. **`contentSnippet` 设计**：当前是 30 字摘要，应该改成**完整 JSON 短串**或**JSON 标识**——避免摘要和完整内容解析结果不一致导致 UI 跳变。或者在数据库增加 `cachedFullContent` 字段预存完整内容。
2. **Room Flow 中间态**：考虑在 Repository 层加 `firstOrNull()` + retry 逻辑，避免 Flow emit 空列表直接被 UI 误用。
3. **重写覆盖层的可复用性**：当前 `OverlayLayer` 是 hardcoded 给 mine/reader/editor 三种类型的。如果未来有更多覆盖层场景，建议抽象成 generic `TwoSlotOverlay` composable。
