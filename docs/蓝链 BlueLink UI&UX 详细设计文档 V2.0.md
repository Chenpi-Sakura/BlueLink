**版本**：2.0
**日期**：2026年6月5日
**团队**：Eau
**设计工具**：Figma + Jetpack Compose（Material3）
**目标平台**：Android（minSdk 24 / targetSdk 36）

---

## V2.0 修订说明

V2.0 在 V1.0 视觉与交互设计完整保留的基础上，把**所有组件实现细节从 uni-app (Vue 3 / rpx / `<view>`)** 迁移到 **Jetpack Compose (`@Composable` / `Modifier` / dp/sp)**。V1.0 的设计原则、色彩体系、字体规范、交互范式、动效定义全部沿用，本版本主要差异：

| 章节 | V1.0 现状 | V2.0 调整 |
|------|----------|----------|
| §1 概述 | 适用平台"uni-app 小程序（Vivo Android）" | 明确 **Android 原生 App** |
| §2 设计语言 | uni-app 风格 + `rpx` 单位 | 改为 **dp/sp 单位 + Material3 主题** |
| §4.1 启动与引导 | 三屏引导卡 + 隐私弹窗 | 保留全部内容，补充 **Android 12+ Splash Screen API** 说明 |
| §4.2 文库首页 | 瀑布流 / 长按菜单 | 改用 `LazyVerticalStaggeredGrid` / `combinedClickable` |
| §4.3 对话页 | AI 卡片 / 锚点列表 / 粒度切换 | 改为 Compose `Card` / `LazyColumn` / `FilterChip` |
| §4.4 阅读器 | 聚光灯 / 折叠条 / 进度 | 改为 `Modifier.alpha(0.3f)` / `AnimatedVisibility` |
| §4.5 知识图谱 | 深灰底 / 三种节点 / 关系连线 | **保留视觉规范**，实现改为 **WebView + ECharts**（详细设计 §9.4） |
| §4.6 灵感捕获 | 自底部滑出弹窗 | 改用 `ModalBottomSheet` |
| §4.7 设置与隐私 | 列表式设置 | 改用 Compose `ListItem` + `Switch` + `Slider` |
| §5 核心组件 | Vue 组件 Props / emit | 改为 **Compose `@Composable` 函数签名** |
| §5.2 聚光灯蒙版 | `<view>` 叠加 + `v-for` | 改用 `Box` + `Modifier.alpha` + `derivedStateOf` |
| §5.3 折叠提示条 | `max-height` 过渡 | 改用 `AnimatedContent` / `AnimatedVisibility` |
| §5.4 引力线 | CSS 动画 | 改用 `Canvas` 绘制 + `infiniteTransition` 脉冲 |
| §5.5 磨砂悬浮栏 | `backdrop-filter: blur(20px)` | 改用 `Modifier.blur(20.dp)`（API 31+） + 低版本降级半透明 |
| §6 动效 | CSS transition / spring | 改用 Compose `animate*AsState` / `tween` / `spring` |
| §7 适配 | 小程序 `rpx` / 安全区 | 改用 Android **Material3 动态取色 / 厂商适配 / WindowInsets** |

> 本版本 **不覆盖**：V1.0 §2.1 设计原则、§2.2 视觉语言、§6.1-6.5 视觉规范、§8.1-8.3 设计资源（色彩速查表 / 字体 / 间距）—— **这些业务/视觉资产全部沿用**。

---

## 1. 概述
本文档定义蓝链 BlueLink Android 客户端的界面与交互设计规范，覆盖信息架构、页面布局、组件行为、视觉表现与 Android 平台特定适配。设计目标延续 V1.0：在移动端高效呈现"溯源引导"与"降噪阅读"的双重体验，贯彻"人文底色，极简透视"的理念。

适用平台：**Android App（Kotlin + Jetpack Compose，Material3）**。

## 2. 设计原则与设计语言
### 2.1 设计原则
1. **路径优先，拒绝答案**：界面中永远不直接展示"最终答案"，一切信息输出均为引导性的线索（锚点、引力线、提问式引言）。
2. **降噪聚焦**：利用折叠、模糊、灰态等手法让用户注意力集中在当前核心任务上。
3. **一触即达**：关键操作（如跳转原文）不超过一次点击，缩短路径。
4. **主动可控**：用户始终能调节溯源颗粒度、提示明确度、折叠状态，保留掌控感。
5. **隐私可视**：私密数据在界面上有明确区分标识，让用户放心。

### 2.2 视觉语言（沿用 V1.0，V2.0 不变）
- **色彩**：米白基底 (#FAF7F2) 营造纸质阅读的温润感；克莱因蓝 (#002FA7) 用于所有可交互元素、高亮、引力线，形成品牌跳脱感；灰色 (#B0B0B0) 用于非焦点文本和弱化层。
- **字体**：内容区使用衬线体（中文：思源宋体/方正书宋；英文：Georgia），UI 控件使用无衬线体（中文：思源黑体/PingFang SC；英文：SF Pro Text / Inter）。
- **形状**：大圆角卡片 (16.dp)、胶囊按钮 (24.dp)、磨砂玻璃 (`Modifier.blur(20.dp)`)。
- **阴影**：轻量弥散阴影 `elevation = 2.dp`（Compose `Card` 默认），表达层级但不干扰阅读。

#### 2.2.1 V2.0 新增：Material3 主题映射
将 V1.0 的设计 token 映射到 Material3 ColorScheme：

| V1.0 设计 token | V1.0 值 | Material3 角色 | 用途 |
|----------------|---------|---------------|------|
| 米白背景 | `#FAF7F2` | `surface` / `background` | 页面底色、卡片底色 |
| 克莱因蓝 | `#002FA7` | `primary` | 强调按钮、链接、引力线 |
| 克莱因蓝（淡化） | `#002FA7` @ 50% | `primaryContainer` | 折叠条、增量高亮背景 |
| 主文本 | `#2C2C2C` | `onSurface` | 正文 |
| 次要文本 | `#6B6B6B` | `onSurfaceVariant` | 标签、来源、时间 |
| 浅灰（折叠/灰态） | `#B0B0B0` @ 30% | `surfaceVariant` | 聚光灯非焦点段落 |
| 错误 | `#EA4335` | `error` | 质疑关系、删除 |
| 成功 | `#34A853` | `tertiary` | 支撑关系、确认 |
| 分割线 | `#E5E0D8` | `outlineVariant` | 卡片分隔、列表项 |

```kotlin
// Color.kt（V2.0 实现，遵循 Material3）
val BlueLinkLightColors = lightColorScheme(
    primary = KleinBlue,
    onPrimary = RiceWhite,
    primaryContainer = KleinBluePale,
    onPrimaryContainer = KleinBlue,
    secondary = RiceWarm,
    onSecondary = DeepInk,
    background = RiceWhite,
    onBackground = DeepInk,
    surface = RiceWhite,
    onSurface = DeepInk,
    surfaceVariant = MistGray,
    onSurfaceVariant = MidGray,
    error = Vermilion,
    onError = RiceWhite,
    outline = SandLine,
    outlineVariant = SandLine
)

val KleinBlue = Color(0xFF002FA7)
val RiceWhite = Color(0xFFFAF7F2)
val KleinBluePale = Color(0x80002FA7)
val DeepInk = Color(0xFF2C2C2C)
val MidGray = Color(0xFF6B6B6B)
val MistGray = Color(0x4DB0B0B0)  // 30% 透明
val Vermilion = Color(0xFFEA4335)
val SandLine = Color(0xFFE5E0D8)
```

#### 2.2.2 V2.0 暗黑模式（V1.0 暂不支持，V2.0 同步实现）
```kotlin
val BlueLinkDarkColors = darkColorScheme(
    primary = KleinBluePale,        // 深色下用淡化的克莱因蓝
    onPrimary = RiceWhite,
    background = Color(0xFF1E1E2E),  // 与图谱页背景一致
    surface = Color(0xFF2A2A38),
    onSurface = RiceWhite,
    surfaceVariant = Color(0xFF3A3A48),
    onSurfaceVariant = LightGray,
    outline = Color(0xFF50505A)
)
```

## 3. 信息架构与导航（沿用 V1.0）
底部 4 Tab 主导航：文库 / 对话 / 图谱 / 我的。Material3 `NavigationBar` 实现。

```kotlin
@Composable
fun BlueLinkBottomBar(currentRoute: String, onNavigate: (String) -> Unit) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
        listOf(
            BottomDest.Home,
            BottomDest.Chat,
            BottomDest.Graph,
            BottomDest.Mine
        ).forEach { dest ->
            NavigationBarItem(
                selected = currentRoute == dest.route,
                onClick = { onNavigate(dest.route) },
                icon = { Icon(painterResource(dest.icon), contentDescription = null) },
                label = { Text(dest.label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    }
}
```

## 4. 页面设计详述

### 4.1 启动与引导（V2.0 补充 Splash Screen）
**V1.0 沿用**：三张引导卡片 → 隐私授权弹窗 → 进入空状态文库。
**V2.0 新增**：Android 12+ 自动使用 Splash Screen API，无需额外代码；低版本在 `themes.xml` 中配 `windowBackground = @color/rice_white`。

```kotlin
// MainActivity.kt
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Android 12+ Splash Screen 自动接管
        installSplashScreen()
        super.onCreate(savedInstanceState)
        // 取 Application 中的 AppContainer（详见详细设计 §4.1.1）
        val container = (application as BlueLinkApp).container
        setContent {
            CompositionLocalProvider(LocalAppContainer provides container) {
                BlueLinkTheme {
                    BlueLinkNavGraph()
                }
            }
        }
    }
}

// LocalAppContainer 声明（与 AppContainer 同文件 / util 包均可）
val LocalAppContainer = compositionLocalOf<AppContainer> {
    error("AppContainer not provided. Wrap your Composable in CompositionLocalProvider.")
}
```

**首屏引导**（OnboardingScreen）：
```kotlin
@Composable
fun OnboardingScreen(onFinished: () -> Unit) {
    val pages = listOf(
        OnboardingPage("AI 只提供锚点", "不直接给答案，让思考回归原文", R.drawable.illu_anchor),
        OnboardingPage("隐私不外泄", "敏感数据本地处理，原始想法绝对安全", R.drawable.illu_privacy),
        OnboardingPage("阅读即思考", "聚光灯 + 折叠，让每一次阅读都有信息增量", R.drawable.illu_focus)
    )
    val pagerState = rememberPagerState { pages.size }
    HorizontalPager(state = pagerState) { page ->
        OnboardingPageView(pages[page], isLast = page == pages.lastIndex, onStart = onFinished)
    }
}
```

### 4.2 文库首页（瀑布流卡片，V2.0 Compose 化）

#### 布局
```kotlin
@Composable
fun HomeScreen(viewModel: HomeViewModel = viewModel(factory = LocalAppContainer.current.factory)) {
    val docs by viewModel.documents.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { HomeSearchBar(query = searchQuery, onQueryChange = viewModel::onSearchChange) },
        bottomBar = { BlueLinkBottomBar(currentRoute = "home", onNavigate = ::navigate) },
        floatingActionButton = { CaptureFAB(onClick = ::showCaptureSheet) }
    ) { padding ->
        if (docs.isEmpty()) {
            EmptyLibraryState()
        } else {
            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 80.dp),
                verticalItemSpacing = 12.dp,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(padding)
            ) {
                items(docs, key = { it.id }) { doc ->
                    DocumentCard(
                        doc = doc,
                        onClick = { navigateToReader(doc.id) },
                        onLongClick = { showDocMenu(doc) }  // combinedClickable 实现
                    )
                }
            }
        }
    }
}
```

#### 卡片组件
```kotlin
@Composable
fun DocumentCard(
    doc: Document,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 标题（衬线体，1.2em）
            Text(
                text = doc.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = SerifFamily,
                    fontSize = 18.sp,
                    lineHeight = 24.sp
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(8.dp))
            // 概念路标（灰色小字，2 行截断）
            doc.conceptBeacon?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(12.dp))
            // 底部：来源 + 时间 + 私密角标
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (doc.privacyLevel == PrivacyLevel.LOCAL_ONLY) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "私密",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                }
                Text(
                    text = "${doc.source} · ${doc.createdAt.relativeTime()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
```

#### 底部悬浮快捷栏（FAB 展开）
```kotlin
@Composable
fun CaptureFAB(onClick: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    FloatingActionButton(
        onClick = { expanded = true },
        containerColor = MaterialTheme.colorScheme.primary
    ) {
        Icon(
            imageVector = if (expanded) Icons.Default.Close else Icons.Default.Add,
            contentDescription = "捕获灵感"
        )
    }
    if (expanded) {
        CaptureSheet(onDismiss = { expanded = false })  // ModalBottomSheet
    }
}
```

### 4.3 对话溯源页（锚点穿梭，V2.0 Compose 化）

#### 整体结构
```kotlin
@Composable
fun ChatScreen(viewModel: ChatViewModel = viewModel(factory = ...)) {
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val isAsking by viewModel.isAsking.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("对话") },
                actions = { FeynmanModeToggle(viewModel.isFeynmanMode, viewModel::toggleFeynman) }
            )
        },
        bottomBar = { ChatInputBar(viewModel) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                when (msg) {
                    is UserMessage -> UserBubble(msg)
                    is AiResponse -> AiResponseCard(msg, onAnchorClick = ::navigateToAnchor)
                    is FeynmanReport -> FeynmanDeviationCard(msg)
                }
            }
            if (isAsking) item { AskingSkeleton() }
        }
    }
}
```

#### AI 回复卡片（磨砂基底 + 引言 + 锚点列表）
```kotlin
@Composable
fun AiResponseCard(
    msg: AiResponse,
    onAnchorClick: (Anchor) -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 引言（衬线体，最多 3 句）
            Text(
                text = msg.introduction,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = SerifFamily,
                    fontSize = 16.sp,
                    lineHeight = 28.sp  // 1.8 倍
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(12.dp))
            // 锚点列表
            msg.anchors.forEach { anchor ->
                AnchorCard(anchor = anchor, onClick = { onAnchorClick(anchor) })
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}
```

#### 锚点卡片
```kotlin
@Composable
fun AnchorCard(anchor: Anchor, onClick: () -> Unit) {
    val alpha by animateFloatAsState(if (anchor.isRead) 0.6f else 1f, label = "anchor-alpha")
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { this.alpha = alpha }
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(anchor.docTitle, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(2.dp))
                Text(
                    anchor.snippet,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            Spacer(Modifier.width(8.dp))
            // 相关性圆环
            ScoreRing(score = anchor.score)
        }
    }
}
```

#### 底部输入栏（粒度切换 + 输入框 + 发送/语音）
```kotlin
@Composable
fun ChatInputBar(viewModel: ChatViewModel) {
    var inputText by remember { mutableStateOf("") }
    var isVoiceMode by remember { mutableStateOf(false) }
    val granularity by viewModel.granularity.collectAsStateWithLifecycle()

    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp).navigationBarsPadding(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 粒度切换
            FilterChip(
                selected = granularity == Granularity.SENTENCE,
                onClick = { viewModel.toggleGranularity() },
                label = { Text(if (granularity == Granularity.SENTENCE) "句词级" else "文章级") }
            )
            Spacer(Modifier.width(8.dp))
            // 输入框
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = { Text("在此输入你的疑问...") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp),
                maxLines = 4
            )
            Spacer(Modifier.width(8.dp))
            // 发送/语音切换
            IconButton(onClick = {
                if (isVoiceMode) startVoiceInput() else viewModel.askQuestion(inputText)
            }) {
                Icon(
                    imageVector = if (isVoiceMode) Icons.Default.Mic else Icons.Default.Send,
                    contentDescription = if (isVoiceMode) "语音输入" else "发送"
                )
            }
        }
    }
}
```

### 4.4 智能阅读器（聚光灯 & 增量折叠，V2.0 Compose 化）

#### 聚光灯模式实现
```kotlin
@Composable
fun ReaderScreen(viewModel: ReaderViewModel = viewModel(factory = ...)) {
    val doc by viewModel.document.collectAsStateWithLifecycle()
    val targetSegmentId by viewModel.spotlightTarget.collectAsStateWithLifecycle()
    val foldedSegments by viewModel.foldedSegments.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 80.dp)
    ) {
        doc.segments.forEach { seg ->
            item(key = seg.id) {
                val isTarget = seg.id == targetSegmentId
                val isFolded = seg.id in foldedSegments
                SegmentView(
                    segment = seg,
                    isSpotlightTarget = isTarget,
                    isDimmed = targetSegmentId != null && !isTarget,
                    isFolded = isFolded,
                    onToggleFold = { viewModel.toggleFold(seg.id) }
                )
            }
        }
    }
}

@Composable
fun SegmentView(
    segment: Segment,
    isSpotlightTarget: Boolean,
    isDimmed: Boolean,
    isFolded: Boolean,
    onToggleFold: () -> Unit
) {
    val alpha by animateFloatAsState(
        targetValue = if (isDimmed) 0.3f else 1f,
        animationSpec = tween(durationMillis = 250),
        label = "segment-dim"
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        if (isFolded) {
            FoldBar(foldedCount = segment.text.length, onClick = onToggleFold)
        } else {
            Row(modifier = Modifier.graphicsLayer { this.alpha = alpha }) {
                if (isSpotlightTarget) {
                    // 左侧克莱因蓝竖线（引力线标识）
                    GravityLine(modifier = Modifier.width(2.dp).fillMaxHeight())
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    text = segment.text,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontFamily = SerifFamily,
                        fontSize = 16.sp,
                        lineHeight = 28.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (isSpotlightTarget) 1f else 0.7f),
                    modifier = Modifier
                        .background(
                            if (isSpotlightTarget) MaterialTheme.colorScheme.surface.copy(alpha = 0.95f) else Color.Transparent,
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 4.dp)
                )
            }
        }
    }
}
```

#### 折叠提示条
```kotlin
@Composable
fun FoldBar(foldedCount: Int, onClick: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .clickable { expanded = !expanded; onClick() }
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (expanded) "收起重复背景" else "AI 已折叠 $foldedCount 字重复背景，点击展开",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.rotate(if (expanded) 180f else 0f)
            )
        }
    }
}
```

### 4.5 知识图谱（V2.0 WebView + ECharts）

#### Compose 容器
```kotlin
@Composable
fun GraphScreen(viewModel: GraphViewModel = viewModel(factory = ...)) {
    val graphData by viewModel.graphData.collectAsStateWithLifecycle()
    var webView by remember { mutableStateOf<WebView?>(null) }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF1E1E2E))) {
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.allowFileAccess = true
                    settings.allowContentAccess = true
                    // 禁用外网
                    settings.blockNetworkLoads = true
                    addJavascriptInterface(GraphJsBridge { nodeId ->
                        viewModel.onGraphNodeClicked(nodeId)
                    }, "BlueLinkBridge")
                    loadUrl("file:///android_asset/graph.html")
                    webView = this
                }
            },
            update = { wv ->
                // 当数据变化时推送给 WebView
                val json = graphData.toJson()
                wv.evaluateJavascript("window.updateGraph($json)", null)
            },
            modifier = Modifier.fillMaxSize()
        )
        // 底部布局切换按钮
        GraphControls(
            onLayoutChange = { type -> webView?.evaluateJavascript("window.changeLayout('$type')", null) }
        )
    }
}
```

#### graph.html（assets 中）
```html
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8" />
    <script src="echarts.min.js"></script>
    <style>html,body,#m{margin:0;padding:0;height:100%;width:100%;background:#1E1E2E;}</style>
</head>
<body>
<div id="m"></div>
<script>
    const chart = echarts.init(document.getElementById('m'));
    const option = {
        backgroundColor: '#1E1E2E',
        tooltip: { trigger: 'item' },
        series: [{
            type: 'graph',
            layout: 'force',
            roam: true,
            label: { show: true, color: '#FAF7F2' },
            force: { repulsion: 200, edgeLength: 80 },
            emphasis: { focus: 'adjacency' },
            lineStyle: { color: '#6B6B6B', curveness: 0.1 },
            data: [],
            links: []
        }]
    };
    chart.setOption(option);
    window.updateGraph = (data) => {
        option.series[0].data = data.nodes.map(n => ({
            id: n.id, name: n.label,
            symbol: n.type === 'DOCUMENT' ? 'circle' : n.type === 'INSPIRATION' ? 'diamond' : 'roundRect',
            itemStyle: { color: n.type === 'CONCEPT' ? '#002FA7' : n.type === 'INSPIRATION' ? '#F0C674' : '#5B8DEF' }
        }));
        option.series[0].links = data.edges.map(e => ({
            source: e.source, target: e.target,
            lineStyle: { color: relationColor(e.relation), type: relationType(e.relation) }
        }));
        chart.setOption(option);
    };
    // 节点点击 → 回调到 Compose
    chart.on('click', (params) => { if (params.dataType === 'node') window.BlueLinkBridge.onNodeClick(params.data.id); });
    // 接收 Compose 消息
    window.changeLayout = (type) => {
        option.series[0].layout = type;
        chart.setOption(option);
    };
    function relationColor(r) {
        return { SUPPORT: '#34A853', CHALLENGE: '#EA4335', SUPPLEMENT: '#002FA7', CITE: '#888' }[r] || '#888';
    }
    function relationType(r) {
        return { SUPPORT: 'dashed', CHALLENGE: 'dashed', SUPPLEMENT: 'solid', CITE: 'dotted' }[r] || 'solid';
    }
</script>
</body>
</html>
```

#### 节点类型视觉规范（沿用 V1.0）
| 类型 | 形状 | 颜色 | 图标 |
|------|------|------|------|
| 文献节点 | 圆形 | 淡蓝 `#5B8DEF` | 文档图标 |
| 灵感节点 | 菱形 | 暖黄 `#F0C674` | 灯泡图标 |
| 概念节点 | 圆角方形 | 克莱因蓝 `#002FA7` | 概念名 |

| 关系类型 | 颜色 | 线型 |
|---------|------|------|
| 支撑 | 绿 `#34A853` | 虚线 |
| 质疑 | 红 `#EA4335` | 虚线 |
| 补充 | 克莱因蓝 `#002FA7` | 实线 |
| 引用 | 灰 `#888` | 点线 |

### 4.6 灵感快捷捕获（V2.0 ModalBottomSheet）

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaptureSheet(onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var tabIndex by remember { mutableStateOf(0) }
    var textContent by remember { mutableStateOf("") }
    var isPrivate by remember { mutableStateOf(true) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp).navigationBarsPadding()) {
            // Tab 切换
            TabRow(selectedTabIndex = tabIndex) {
                listOf("文字", "语音", "拍摄").forEachIndexed { i, label ->
                    Tab(selected = tabIndex == i, onClick = { tabIndex = i }, text = { Text(label) })
                }
            }
            Spacer(Modifier.height(16.dp))
            when (tabIndex) {
                0 -> {
                    OutlinedTextField(
                        value = textContent,
                        onValueChange = { textContent = it },
                        placeholder = { Text("记录一个想法...") },
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
                1 -> VoiceInputView(onResult = { textContent = it })
                2 -> CameraInputView(onResult = { textContent = it })
            }
            Spacer(Modifier.height(12.dp))
            // 私密开关
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("私密（仅本地）", modifier = Modifier.weight(1f))
                Switch(checked = isPrivate, onCheckedChange = { isPrivate = it })
            }
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { saveInspiration(textContent, isPrivate); onDismiss() },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(24.dp)
            ) {
                Text("收录为灵感卡片")
            }
        }
    }
}
```

### 4.7 设置与隐私（V2.0 Compose `ListItem`）
```kotlin
@Composable
fun MineScreen(viewModel: MineViewModel = viewModel(factory = ...)) {
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val privacyMode by viewModel.privacyMode.collectAsStateWithLifecycle()
    val directness by viewModel.directness.collectAsStateWithLifecycle()

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item { UserHeader(profile = profile, onEdit = viewModel::editProfile) }

        item { SectionHeader("认知设置") }
        item {
            ListItem(
                headlineContent = { Text("默认溯源粒度") },
                trailingContent = {
                    FilterChip(
                        selected = profile.defaultGranularity == Granularity.SENTENCE,
                        onClick = { viewModel.toggleGranularity() },
                        label = { Text(if (profile.defaultGranularity == Granularity.SENTENCE) "句词级" else "文章级") }
                    )
                }
            )
        }
        item {
            ListItem(
                headlineContent = { Text("提示明确度") },
                supportingContent = { Text(directness.description) },
                trailingContent = {
                    Slider(
                        value = directness.value,
                        onValueChange = viewModel::setDirectness,
                        valueRange = 0f..1f
                    )
                }
            )
        }
        item {
            ListItem(
                headlineContent = { Text("探索深度") },
                trailingContent = {
                    Switch(checked = profile.exploreDepth, onCheckedChange = viewModel::toggleExploreDepth)
                }
            )
        }

        item { SectionHeader("隐私与安全") }
        item {
            ListItem(
                headlineContent = { Text("当前模式") },
                supportingContent = { Text(privacyMode.label) }
            )
        }
        item {
            ListItem(
                headlineContent = { Text("数据导出") },
                modifier = Modifier.clickable { viewModel.exportData() }
            )
        }
        item {
            ListItem(
                headlineContent = { Text("永久删除所有数据", color = MaterialTheme.colorScheme.error) },
                modifier = Modifier.clickable { viewModel.confirmWipe() }
            )
        }
    }
}
```

## 5. 核心交互组件设计（V2.0 Compose 化）

### 5.1 锚点卡片（AnchorCard）
详见 §4.3。

### 5.2 聚光灯蒙版（SpotlightOverlay）
V2.0 实现：不再用叠加层，而是**直接对每个段落应用 `Modifier.alpha(0.3f)`**。详见 §4.4 `SegmentView`。

```kotlin
// 关键点：用 derivedStateOf 避免无谓重组
@Composable
fun ReaderSpotlight(target: String?, segments: List<Segment>) {
    val isDimmedMap by remember(segments, target) {
        derivedStateOf { segments.associate { it.id to (target != null && it.id != target) } }
    }
    // 列表渲染时根据 isDimmedMap[seg.id] 决定 alpha
}
```

### 5.3 折叠提示条（FoldBar）
详见 §4.4 `FoldBar`。

### 5.4 引力线（GravityLine，Canvas 绘制）
```kotlin
@Composable
fun GravityLine(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 0.5f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "pulse-alpha"
    )
    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.primary)
            .drawWithCache {
                onDrawBehind {
                    drawCircle(
                        color = Color.White,
                        radius = 4.dp.toPx(),
                        center = Offset(size.width / 2, 4.dp.toPx()),
                        alpha = pulseAlpha
                    )
                }
            }
    )
}
```

### 5.5 磨砂悬浮栏（V2.0 用 `Modifier.blur`，低版本降级）
```kotlin
@Composable
fun GlassBottomBar(content: @Composable RowScope.() -> Unit) {
    val sdk = Build.VERSION.SDK_INT
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (sdk >= Build.VERSION_CODES.S) {
                    Modifier.blur(20.dp)
                } else {
                    Modifier.background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
                }
            )
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}
```

## 6. 动效与反馈（V2.0 Compose Animation）

| 场景 | V1.0 实现 | V2.0 实现 |
|------|----------|----------|
| 页面转场（对话 → 阅读） | CSS `slide-left 300ms` | `AnimatedContent` + `slideInHorizontally` |
| 锚点卡片加载骨架 | CSS 闪烁脉冲 | `Shimmer(modifier = Modifier.shimmer())`（开源库） |
| 按钮点击 | `transform: scale(0.95)` | `Modifier.pointerInput { detectTapGestures }` + `animateFloatAsState` |
| 图谱节点入场 | CSS transform | ECharts `animationDuration: 1000` + `animationEasing: 'elasticOut'` |
| 聚光灯切换 | 0.25s fade | `animateFloatAsState(tween(250))` |
| FAB 展开 | stagger 50ms | `AnimatedVisibility` 顺序显示 |
| 折叠条展开/收起 | `max-height` transition | `AnimatedContent` + `expandVertically` / `shrinkVertically` |

```kotlin
// 示例：页面转场
@Composable
fun ChatToReaderTransition() {
    AnimatedContent(
        targetState = currentScreen,
        transitionSpec = {
            (slideInHorizontally(initialOffsetX = { it }) + fadeIn()).togetherWith(
                slideOutHorizontally(targetOffsetX = { -it }) + fadeOut()
            )
        },
        label = "screen-transition"
    ) { screen -> when (screen) { ... } }
}
```

## 7. 适配与无障碍（V2.0 Android 平台特定）

### 7.1 屏幕适配
- **sw320dp ~ sw480dp**：默认手机布局（瀑布流 2 列）
- **sw600dp+**（平板，V1.0 暂不支持，V2.0 留位）：用 `WindowSizeClass`，改 3-4 列 + 双栏阅读器

```kotlin
val windowSizeClass = calculateWindowSizeClass(this)
when (windowSizeClass.widthSizeClass) {
    WindowWidthSizeClass.Compact -> /* 2 列瀑布流 */
    WindowWidthSizeClass.Medium, WindowWidthSizeClass.Expanded -> /* 3-4 列 */
}
```

### 7.2 暗黑模式
- `isSystemInDarkTheme()` 决定 `BlueLinkTheme` 选 light / dark ColorScheme。
- V2.0 暗黑模式配色见 §2.2.2。

### 7.3 动态取色（Android 12+ Material You）
- `dynamicColor: Boolean` 参数；用户可在设置中开关。
- 启用时从 `dynamicLightColorScheme(context)` / `dynamicDarkColorScheme(context)` 取 wallpaper 配色，覆盖克莱因蓝为辅助强调色（克莱因蓝保留为品牌色，但**主操作按钮**使用 dynamic primary）。

```kotlin
@Composable
fun BlueLinkTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colors = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(LocalContext.current)
            else dynamicLightColorScheme(LocalContext.current)
        darkTheme -> BlueLinkDarkColors
        else -> BlueLinkLightColors
    }
    MaterialTheme(colorScheme = colors, typography = BlueLinkTypography, content = content)
}
```

### 7.4 安全区域 & 刘海屏
- 所有 Scaffold 内部 content 加 `Modifier.systemBarsPadding()` 或单独处理 `WindowInsets.systemBars`。
- 底部栏用 `Modifier.navigationBarsPadding()` 适配全面屏。

### 7.5 无障碍
- **TalkBack**：所有非文本元素加 `contentDescription`（"私密文档"、"跳转到原文段落"、"展开折叠"）。
- **字体缩放**：使用 `sp` 单位（不强制 `dp`），跟随系统 `fontScale`。
- **最小点击区域**：48.dp × 48.dp（Compose `IconButton` 默认 48dp）。
- **高对比度**：`MaterialTheme.colorScheme` 的 on* 颜色与 background 对比度 ≥ 4.5:1（WCAG AA）。

### 7.6 厂商适配（V2.0 关键）
| 厂商 | 后台限制 | 权限弹窗 | V2.0 应对 |
|------|---------|---------|----------|
| 小米（MIUI/HyperOS） | 后台杀进程严重 | 通知、自启动、电池优化 | 设置页提供"电池优化白名单引导" |
| 华为（HarmonyOS） | 后台冻结 | 通知、关联启动 | 同上 |
| OPPO（ColorOS） | 后台限制 | 自启动、省电模式 | 同上 |
| vivo（OriginOS） | 后台限制 | 通知、高耗电提醒 | 同上 |
| 三星（OneUI） | 较宽松 | 标准 | 无需特殊处理 |

**实现**：
```kotlin
// 引导用户到电池优化设置
fun openBatteryOptimizationSettings(context: Context) {
    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
        .setData(Uri.parse("package:${context.packageName}"))
    context.startActivity(intent)
}
```

### 7.7 横屏
- V1.0 暂未明确，V2.0 简化：阅读器横屏时切**双栏布局**（左原文 + 右锚点列表）。
- 文库 / 对话 / 图谱横屏时直接拉伸（不做特殊处理）。

## 8. 附录：设计资源与标注

### 8.1 色彩速查（沿用 V1.0）
| 用途 | 色值 | 备注 |
|------|------|------|
| 页面背景 | #FAF7F2 | 米白 |
| 主强调色 | #002FA7 | 克莱因蓝 |
| 主文本 | #2C2C2C | 深灰 |
| 次要文本 | #6B6B6B | 中灰 |
| 灰态文本 | #B0B0B0 | 浅灰（用于非焦点） |
| 成功/支撑关系 | #34A853 | 绿色 |
| 质疑/错误 | #EA4335 | 红色 |
| 磨砂背景 | `surface @ 0.6f` + `Modifier.blur(20.dp)` | API 31+ |
| 磨砂背景（低版本） | `surface @ 0.92f` | API < 31 |
| 分割线 | #E5E0D8 | 浅米色线 |

### 8.2 字体排版（V2.0 Compose 版）
```kotlin
val SerifFamily = FontFamily(
    Font(R.font.noto_serif_sc_regular),
    Font(R.font.noto_serif_sc_bold, FontWeight.Bold)
)
val SansFamily = FontFamily(
    Font(R.font.noto_sans_sc_regular),
    Font(R.font.noto_sans_sc_medium, FontWeight.Medium),
    Font(R.font.noto_sans_sc_bold, FontWeight.Bold)
)

val BlueLinkTypography = Typography(
    bodyLarge = TextStyle(
        fontFamily = SerifFamily, fontSize = 16.sp, lineHeight = 28.sp, // 1.8 倍
        color = DeepInk
    ),
    titleMedium = TextStyle(
        fontFamily = SerifFamily, fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.Medium
    ),
    titleSmall = TextStyle(
        fontFamily = SansFamily, fontSize = 14.sp, fontWeight = FontWeight.SemiBold
    ),
    bodySmall = TextStyle(
        fontFamily = SansFamily, fontSize = 12.sp, lineHeight = 18.sp
    ),
    labelSmall = TextStyle(
        fontFamily = SansFamily, fontSize = 11.sp, color = MidGray
    )
)
```

### 8.3 间距与圆角（V2.0 dp 版）
- 卡片内边距：`16.dp`（V1.0 `24rpx` ≈ `16dp`）
- 页面水平边距：`16.dp`（V1.0 `32rpx` ≈ `16dp`）
- 卡片圆角：`16.dp`
- 按钮圆角：`24.dp`（胶囊形）
- 锚点卡片圆角：`12.dp`
- 输入框高度：`48.dp`（标准 Material 触控高度）

---

**文档变更记录**

| 版本 | 日期 | 修订内容 | 作者 |
| --- | --- | --- | --- |
| 1.0 | 2026-05-26 | 初始版本，uni-app + Vue 3 实现 | Eau团队 |
| 2.0 | 2026-06-05 | 全面 Compose 化；设计 token 映射到 Material3 ColorScheme；组件实现改为 @Composable 函数；新增 Material You 动态取色、厂商适配、Compose 动效章节 | Eau团队 |
