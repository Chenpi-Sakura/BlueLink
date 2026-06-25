# 2026-06-25 `navigation3` 分支完整复盘

> 本次分支从 `e78b65f`（`feat/recover-inspiration` 合并点）切出，目标是**用 Google Nav3 替代手写的 OverlayLayer 覆盖层动画**。整条分支横跨 12 个 commit、约 6 个独立的方向调整、4 次方向错误回滚，最终落地一个简洁的 iOS-push + drop shadow 方案。本文档完整记录这条分支的决策演进、踩坑、试错与最终落定方案，供未来类似迁移参考。

---

## 0. 起点：为什么开这条分支

`feat/animation` 分支用 18 轮迭代把 OverlayLayer（`movableContentOf` + `Animatable` + `graphicsLayer` + scrim + 双 slot 状态机）调到了一个用户认可的「iOS-push 风格」动画，但整套手写方案有 4 个痛点：

1. ~250 行复杂状态机代码
2. 多个「我以为是 None 其实是 dispose」的 API 边界踩坑
3. 进程死亡后所有覆盖层状态全丢
4. `mineFromRoute` 这类手工「上一层」指针需要单独维护

用户原话：「我不想自己弄动画样式了，用 Google 原生推荐的标准即可。」即用 Nav3 官方推荐动画替代。

---

## 1. 第一阶段：架构与依赖（commit c5dc405 / 04157ff）

### 决策

- 用 **Nav3 1.1.3 稳定版**（2026-06-17 发布，距离开发日 8 天前）。放弃了早期考虑的 1.0.0-alpha07。
- 混合架构：底部 4 Tab 留现状（不是 back stack 行为），Nav3 只管覆盖层。
- `OverlayNavKey` 用 `@Serializable`（kotlinx-serialization）— Nav3 官方 recipe 用法，嵌套 nullable 参数无坑，进程死亡恢复自动持久化。
- 动画选「标准 Android push」：new 从右滑入 / old 向左滑出，300ms tween。

### 关键文件

- `gradle/libs.versions.toml` — 加 `nav3 = "1.1.3"`、`kotlinx-serialization = "1.7.3"`、`kotlin-serialization` plugin
- `app/build.gradle.kts` — 加 nav3 + serialization 依赖，删 Nav2 (`androidx.navigation.compose`)
- `app/src/main/AndroidManifest.xml` — 加 `android:enableOnBackInvokedCallback="true"`（predictive back 手势，Android 13+）
- **新文件** `OverlayNavKey.kt` — 9 个 `@Serializable` NavKey（NoOverlay 占位 + Reader/Editor + 6 个 Mine 子页）

### 第一阶段踩坑

| # | 问题 | 解决 |
|---|------|------|
| 1 | Nav3 1.1.3 的 `rememberNavBackStack` 返回 `NavBackStack<NavKey>`（自定义类，实现 `List<T>`），不是 `SnapshotStateList<NavKey>`。`NavBackStack<T>` 是 Nav3 自己的类 | 改 OverlayNavGraph 参数类型为 `NavBackStack<NavKey>` |
| 2 | `AppContainer` 实际在 `com.yjtzc.bluelink` 包，不在 `.di` 子包 | 改 import |
| 3 | `tween<Float>(300)` 编译报错：slideIn/slideOut 需要 `FiniteAnimationSpec<IntOffset>` | 显式标注类型为 `FiniteAnimationSpec<IntOffset>` |

---

## 2. 第二阶段：动画与回调收敛（commit fd79dbd / deccd3f / 604863a）

### 决策

- **统一 iOS-push 动画**：transitionSpec（push）、popTransitionSpec（pop）、predictivePopTransitionSpec 都用 iOS-push（new 滑入 / old 驻留）
- **MineScreen / PrivacySecurityScreen 回调收敛**：6 / 3 个 `onNavigateTo*` → 合并为单个 `onNavigate: (OverlayNavKey) -> Unit`
- **删除 `OverlayLayer` 185 行手写代码 + 所有支持类型**（`Overlay` sealed / `TransitionDirection` enum / `MineRoute` sealed / `ReaderParams` data class / `SCRIM_ALPHA` const）
- **删除手写 `BackHandler`**：Nav3 自动处理系统返回手势
- **hoist `BlueLinkViewModelFactory`**：避免每帧 new

### 第二阶段踩坑

| # | 问题 | 解决 |
|---|------|------|
| 1 | `check(targetLevel >= 1) { ... }` 在边缘 case 下抛 `IllegalStateException` 导致 app 闪退 | 移除 `check()`（不是必要的；动画逻辑不依赖运行时检查） |

### 用户中途的反馈与方向调整

用户问：「二级到三级动画不是二级不动三级进入，而是二级三级一起左滑」—— 这是 iOS-push 风格（旧页驻留 + 新页滑入覆盖），与「标准 Android push」不同。最初用户选择了「标准 Android push」，但实测发现与期望不符。

**方向调整 1**：改为「混合动画」—— PrivacySecurity → 嵌套子页用 iOS-push，其他用标准 push。实现：硬编码 `if (initialState.key == PrivacySecurity) { iOS-push } else { standard }`。

但用户接着指出：「我们维护一个层级关系就行吗之后统一对这个层级的应用这个动画不就好了」—— 硬编码条件是糟糕的架构，应该用 `backStack.size` 推算层级。

**方向调整 2**：改用「level-based 动画规则」—— backStack 自动推算 level（size=2 → level 1，size=3+ → level 2+），按 level 决定动画。

---

## 3. 第三阶段：scrim 误入歧途（commit f371f0f / 41fc04b / 8d9208a / 7c2e102）

**完整错误方向**，4 个 commit 全部无效，已在线性历史里保留作反面教材。

### 误入歧途的背景

用户在二级→三级动画稳定后说：「OK，这次功能的实现很完美了，现在就差黑色遮罩了」。

我错误地把「iOS-push 动画」与「iOS-modal 弹窗」混淆，认为应该在覆盖层打开时加全屏半透明黑色 scrim（modal 标准做法），压暗背景 tab 页。

### 4 次试错全过程

| Commit | 方案 | 结果 |
|--------|------|------|
| `f371f0f` | 在 OverlayNavGraph 之上加全局 scrim Box，`zIndex = 2f` 覆盖所有 overlay | scrim 在嵌套子页切换时看不到（被覆盖） |
| `41fc04b` | 调整 scrim 到 `OverlayNavGraph` 之上（zIndex 2f），覆盖嵌套二级和三级 | scrim 在一级 overlay 之上（盖住了 overlay） |
| `8d9208a` | 改为 per-entry scrim（每个 entry 自己的 scrim） | scrim 被自己 entry 的不透明内容完全挡住 |
| `7c2e102` | 调整绘制顺序：scrim 放在 Screen 之前（垫在内容下） | scrim 仍然被 Screen 的不透明背景完全挡住，用户反馈「遮罩完全看不见了」 |

### 致命 UI 错误

我犯的**对 iOS 视觉效果的致命误解**：

> 在真正的 iOS 系统里，无论是左右滑入的页面，还是从下往上弹出的页面，新页面本身绝对是 100% 不透明的。
> - **左右滑动（iOS-push）**：根本没有全屏的半透明黑色遮罩！只有新页面左侧边缘带有一条淡淡的边缘阴影（Drop Shadow），用来体现它盖在旧页面之上。旧页面在退出时，自身亮度会稍微变暗。
> - **底部弹窗（Modal）**：新页面是不透明的卡片，它下方的黑色遮罩是独立的一层，专门用来盖住旧页面的，而不是「透过新页面显示出来」。

**我们做的是 iOS-push（左滑），但被我错误地按 iOS-modal 实现了（加全屏 scrim）。** 这是整个 debug 阶段最致命的概念错误。

### 用户纠错原话

> "如果是做左右滑入的 iOS-push，方向要调整一下。我们不需要在每个 Entry 里加一层 fillMaxSize 的全屏半透明黑色 Scrim，这违背了 Push 动画的物理直觉。
> 请改成这样处理：
> 1. 去掉目前的 OverlayScrim。
> 2. 只要给新页面的 Box 左侧边缘加上一层阴影（Shadow / elevation），让它在滑入时能和底下的页面产生 Z 轴的层次区分即可。
> 3. 既然我们用了 KeepUntilTransitionsFinished，旧页面在动画期间还在，新页面带着边缘阴影直接盖过来，这就是最标准的 iOS-push 效果了。"

---

## 4. 第四阶段：方向修正（commit b0e67a3 / b276124）

### 方向修正

抛弃全屏 scrim 方案，改用**新页面 Box 左侧边缘 drop shadow**：

```kotlin
Box(
    Modifier
        .fillMaxSize()
        .shadow(elevation = OVERLAY_SHADOW_ELEVATION, clip = false)
) {
    // Screen 内容
}
```

- `OVERLAY_SHADOW_ELEVATION = 24.dp` — 阴影扩散范围更广
- `clip = false` — 阴影不被 Box 边界裁剪，能往左延伸到屏幕外
- 默认直角矩形阴影契合 iOS-push 页面边缘直上直下的视觉
- 8 个 entry（Reader / Editor / 6 个 Mine 子页）都加了

### 用户后续微调

「阴影的往外在拉一点？」→ 阴影 elevation 从 8.dp 调到 24.dp（外拉更明显）

---

## 5. 第五阶段（未落地）：predictive back 手势调优

用户对侧滑返回手势提了两个细节要求：

1. **降低手势灵敏度（增加阻尼）**：用 LinearEasing 替代默认的 FastOutSlowInEasing，让 progress → 视觉 offset 的映射 1:1 贴合手指物理距离
2. **限制手势阶段最大偏移量 70%**：手指拖到屏幕右边缘但未松手时，旧页最多滑出 70%，保留 30% 露出上一级

### 第一次实现：LinearEasing + 70% 截断

```kotlin
predictivePopTransitionSpec = { progress ->
    val scale = if (progress in 1..999) GESTURE_MAX_OFFSET_FRACTION else 1f
    EnterTransition.None togetherWith slideOutHorizontally(
        targetOffsetX = { fullWidth -> (fullWidth * scale).toInt() },
        animationSpec = tween(
            durationMillis = PREDICTIVE_POP_DURATION_MS,
            easing = LinearEasing
        )
    )
}
```

`progress` 是 Nav3 1.1.3 在 `predictivePopTransitionSpec` 第二参数暴露的 Int（0..1000），表示 gesture 归一化进度。

### 发现新 bug：「突然消失」

用户实测发现：拖动时 70% 截断 OK，但**松手 commit 时旧页「原地凭空消失」**，不是滑出。

根因：Compose Navigation 看到 `targetOffsetX` 终点是 `0.7 * width`，在 commit 时把动画跑到 70% 终点就判定「动画完成、目标到达」，瞬间把旧页面从组合树中销毁。剩下 30% 没机会滑出屏幕。

### 第二次实现：Spring + 100% 终点

用户提出终极解法：

> 「我们现在的策略是：取消人为截断，让终点回到 100%。动画曲线改用无回弹的 spring。
> 这样页面不仅能完美 1:1 吸附手指（大拇指停在边缘自然就能保留 15% 的画面），而且松手时能继承手势的加速度，非常干脆地滑出屏幕销毁。」

```kotlin
predictivePopTransitionSpec = {
    EnterTransition.None togetherWith slideOutHorizontally(
        targetOffsetX = { fullWidth -> fullWidth },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        )
    )
}
```

- 终点恒为 `fullWidth` → 解决「突然消失」
- Spring 继承手指 Velocity → 松手瞬间「唰」地甩出去 → 解决「不干脆」
- `DampingRatioNoBouncy` 无回弹
- 手指停在屏幕右边缘时，大拇指物理位置不会超过屏幕宽度 → Spring 自然停在「该位置」→ 保留 10%~15% 不需要写死

### 第三次微调：StiffnessLow

用户反馈「返回有点犹豫时，滑动的速度太快了」→ 把 `StiffnessMediumLow` (400f) 降到 `StiffnessLow` (200f)。犹豫松手时（velocity≈0），Spring 拉力更柔和。

### 最终撤回

用户决定「暂不做这个返回手感的效果了」，回滚到标准 push 状态：

```kotlin
predictivePopTransitionSpec = {
    EnterTransition.None togetherWith slideOutHorizontally(
        targetOffsetX = { fullWidth -> fullWidth },
        animationSpec = tween(300)
    )
}
```

---

## 6. 第六阶段：CR 清理（本次 commit）

清理 navigation package 累积的冗余：

### 清理项

| 文件 | 改动 |
|------|------|
| OverlayNavGraph.kt | 删 unused `rememberCoroutineScope` import；改写函数 KDoc 为「现在是什么」语气（去掉「被搬移」「之前被多个屏幕共享」等迁移叙述）；提取 `OVERLAY_ANIM_DURATION: FiniteAnimationSpec<IntOffset> = tween(300)` 常量替换三处 `tween(300)` 重复；删除「层级关系」「扩展性」等空头注释；删除 `/* 空渲染 */`、`predictive pop 同 pop` 等冗余 |
| BlueLinkNavGraph.kt | 删 `V2.2 旧版 ... 已删除` 整段过时脚注（scrim 方案已废止）；删 `Scrim 改为 per-entry` 误导性注释；删底部 18 轮踩坑尾注；补 `NavDest` KDoc「为什么」段；补 `BlueLinkNavGraph` KDoc「为什么 hoist back stack」说明 |

### CR 踩坑

| # | 问题 | 解决 |
|---|------|------|
| 1 | `tween<Float>(300)` 编译报错：slideIn/slideOut 需要 `FiniteAnimationSpec<IntOffset>` | 显式标注 `: FiniteAnimationSpec<IntOffset>`，因为动画值是 `IntOffset` 像素整数不是 Float |
| 2 | `private val OVERLAY_ANIM_DURATION` 加泛型后编译失败「Cannot infer type for this parameter」 | 必须显式标注类型，Kotlin 编译器无法从 `tween(300)` 推断出 IntOffset |
| 3 | 删 `rememberCoroutineScope` import 前先 grep 确认 `scope` 真的没在用 | 全文搜索只匹配到 KDoc 里的文字，可以安全删 |

### 关键设计决定（CR 时保留）

- **不抽辅助函数 `MineSubPageEntry { ... }`**：6 个 Mine 子页 entry 确实是同模板，但抽函数会增加间接层。当前每个 entry 7 行的样板代码可接受，等未来增加到 10+ 子页再考虑。
- **不抽辅助函数 `OverlayShadowBox { ... }`**：8 个 entry 重复 2 行 `shadow` modifier，抽函数不划算。
- **保留 NavDisplay 容器 Box 的 `zIndex(1f)`**：必须保留——否则会被底部 Scaffold（含 NavigationBar）覆盖。
- **保留 `ExitTransition.KeepUntilTransitionsFinished`**：必须保留——否则旧 entry 会在新 entry 滑入时立刻 unmount（卡片图片/动画状态被销毁）。
- **保留 `cachedCard` 优化**：防止 Flow 中间态 `emit(emptyList())` 时误判卡片不存在。
- **保留 `Flow 还没 emit` 的占位 Box**：没有 opaque background 会透出 HOME 屏。

---

## 7. 落地成果

### 最终 commit 历史（按时间顺序）

```
b276124 feat: overlay entry 加 drop shadow 增强 Z 轴层次                ← 阴影外拉 8→24
b0e67a3 feat: 用边缘 drop shadow 替代全屏半透明黑色遮罩（iOS-push 正确方案）  ← 方向修正
7c2e102 fix: 修正 scrim Z 轴层级——必须垫在新页面内容下，不能盖在自己脸上      ← scrim 试错
8d9208a fix: scrim 改为 per-entry 实现（每个 overlay 自己的 scrim）              ← scrim 试错
41fc04b fix: scrim 移到 OverlayNavGraph 之上（zIndex 2f），覆盖嵌套二级和三级页面 ← scrim 试错
f371f0f feat: 添加 iOS-modal 黑色 scrim 遮罩                                     ← scrim 试错（错误起点）
ce15957 refactor: 移除 isAnyOverlayOpen 隐藏 Tab 栏逻辑，改用子页面不透明背景自然覆盖
604863a fix: 移除 transitionSpec / popTransitionSpec 里的 check() 检查
deccd3f refactor: 统一 iOS-push 动画 + 隐藏 NavigationBar
fd79dbd refactor: MineScreen / PrivacySecurityScreen 回调收敛为单个 onNavigate
04157ff refactor: 引入 OverlayNavGraph 替换 OverlayLayer 手写动画
c5dc405 chore: 添加 Nav3 1.1.3 依赖 + OverlayNavKey 定义                  ← 分支起点
```

### 4 个保留作反面教材的 scrim 试错 commit

按时间顺序：`f371f0f` → `41fc04b` → `8d9208a` → `7c2e102`

**为什么保留**：
- commit message 详细记录了每次试错的思路
- git 历史是天然的设计探索日志
- 未来如果有人考虑「加全屏 scrim」，看到这串 commit 会立刻意识到「这个方向是死路」

**未来清理时机**：如果有 rebase 整理需求，可以用 `git rebase -i HEAD~12` 交互式重写为 1 个 commit（`feat: 覆盖层用 iOS-push + drop shadow`），但目前保留。

### 净变更（从 c5dc405 到 b276124）

```
 .idea/deploymentTargetSelector.xml                 |    4 +-
 .idea/misc.xml                                     |    3 +-
 app/build.gradle.kts                               |    5 +-
 app/src/main/AndroidManifest.xml                   |    1 +
 .../yjtzc/bluelink/data/local/db/AppDatabase.kt    |   19 +-
 .../com/yjtzc/bluelink/data/local/db/Entities.kt   |    4 +-
 .../bluelink/data/repository/CaptureRepository.kt  |   56 +-
 .../bluelink/ui/editor/InspirationEditorScreen.kt  |   17 +-
 .../java/com/yjtzc/bluelink/ui/home/HomeScreen.kt  | 1320 +++++++++++++++++---
 .../java/com/yjtzc/bluelink/ui/mine/MineScreen.kt  |   33 +-
 .../bluelink/ui/mine/PrivacySecurityScreen.kt      |   21 +-
 .../bluelink/ui/navigation/BlueLinkNavGraph.kt     |  479 +------   ← 净 -456 行
 .../bluelink/ui/navigation/OverlayNavGraph.kt      |  355 ++++++   ← 新文件
 .../yjtzc/bluelink/ui/navigation/OverlayNavKey.kt  |   90 ++       ← 新文件
 ...026-06-24-overlay-animation-and-editor-debug.md |  150 +++
 gradle/libs.versions.toml                          |   13 +-
 16 files changed, 1894 insertions(+), 676 deletions(-)
```

**核心收益**：
- `BlueLinkNavGraph.kt`：479 行 → 净 -456 行（删除手写 OverlayLayer + 所有支持类型）
- `OverlayNavGraph.kt`：新文件 355 行（Nav3 实现）
- `OverlayNavKey.kt`：新文件 90 行（9 个 NavKey）
- **净效果**：覆盖层从手写 18 轮调试 600+ 行 → Nav3 标准 445 行

---

## 8. 经验教训汇总

| 类别 | 教训 |
|------|------|
| **概念混淆** | iOS-push（左滑）和 iOS-modal（底部弹出）是两种完全不同的视觉范式。前者用 drop shadow，后者用全屏 scrim。**不要混用**。 |
| **方向错误时硬调实现** | scrim 试错阶段我反复调整 zIndex、绘制顺序、Box 嵌套，但概念本身就是错的（iOS-push 不需要全屏 scrim）。调整实现细节无济于事。**先确认视觉原理再写代码**。 |
| **「我以为 = 实际」陷阱** | `ExitTransition.None` 在新版 Compose 不等于「保持原位」，是「立即 dispose」。`Animatable` 不是「保持」，`rememberCoroutineScope` 不一定「记得 scope」。**用 API 前先验证边界行为**。 |
| **硬编码条件是糟糕架构** | `if (initialState.key == PrivacySecurity) { ... }` 是「我猜的情况」。用户正确指出应该用 backStack 层级关系——**维护数据驱动，而不是条件分支**。 |
| **空头支票式注释** | 「扩展性：以后想区分 level 动画……」是空话，CLAUDE.md 反对。**只描述当前代码做什么**。 |
| **构建成功 ≠ 工作正确** | 4 个 scrim commit 都 build 通过，但用户实测「遮罩完全看不见了」。**必须真机/真场景验证**。 |
| **CR 找问题容易，改问题要稳** | CR 报告列出 18+ 个问题。改时一次到位（避免改完又发现新问题再回滚），但要先确认 build 通过再继续。 |
| **手写代码 vs 官方框架** | 18 轮调试手写 OverlayLayer 不如直接用 Nav3 框架。**优先看官方/社区方案，再考虑自研**。 |

---

## 9. 关键文件

- `D:\Projects\BlueLink\app\src\main\java\com\yjtzc\bluelink\ui\navigation\OverlayNavKey.kt` — 9 个 NavKey 定义
- `D:\Projects\BlueLink\app\src\main\java\com\yjtzc\bluelink\ui\navigation\OverlayNavGraph.kt` — 覆盖层 Nav3 容器
- `D:\Projects\BlueLink\app\src\main\java\com\yjtzc\bluelink\ui\navigation\BlueLinkNavGraph.kt` — 主导航骨架
- `D:\Projects\BlueLink\gradle\libs.versions.toml` — Nav3 1.1.3 依赖
- `D:\Projects\BlueLink\app\src\main\AndroidManifest.xml` — predictive back 启用
- `D:\Projects\BlueLink\docs\debug\2026-06-24-overlay-animation-and-editor-debug.md` — 旧 OverlayLayer 18 轮踩坑记录（保留供对比）
- `D:\Projects\BlueLink\docs\debug\2026-06-25-overlay-nav3-cr-cleanup.md` — 本次 CR 详细技术记录

## 10. 后续维护提醒

- 任何「加全屏 scrim 覆盖背景」的提议 → 立刻否决，参考本文第 3 节的失败案例
- 想加新覆盖层目的地 → 在 `OverlayNavKey.kt` 加 data object + 在 `OverlayNavGraph.kt` 的 `entryProvider` 加 `entry<...> { ... }`
- 想修改过渡动画时长 → 改 `OverlayNavGraph.kt` 的 `OVERLAY_ANIM_DURATION` 常量
- 想修改阴影强度 → 改 `OverlayNavGraph.kt` 的 `OVERLAY_SHADOW_ELEVATION` 常量
- 想恢复 predictive back 手势调优 → 见第 5 节「撤回」前的最后一次实现

## 11. 关键事实速查

| 断言 | 证据 |
|------|------|
| Nav3 1.1.3 `predictivePopTransitionSpec` 第二参数是 `Int` 0..1000 | `javap NavDisplay.class` 字节码 |
| Nav3 在手势阶段用 `SeekableTransitionState.seekTo(progress, previousScene)` | `javap -c NavDisplayKt__NavDisplayKt$NavDisplay$11$1.class` |
| `rememberNavBackStack` 返回 `NavBackStack<NavKey>`（自定义类） | `javap NavBackStack.class` |
| `slideInHorizontally` / `slideOutHorizontally` 需要 `FiniteAnimationSpec<IntOffset>` | `javap EnterExitTransitionKt.class` 签名 |
| `NoOverlay` 占位 entry 保证 back stack 始终非空、push/pop 都有动画源 | 架构验证：commit 04157ff |