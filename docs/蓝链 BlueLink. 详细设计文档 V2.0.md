**版本**：2.0
**日期**：2026年6月5日
**团队**：Eau
**产品定位**：端侧AI认知溯源引擎——不生产答案，只提供直达真相的最短捷径。

---

## V2.0 修订说明

V2.0 在 V1.0 业务逻辑完整保留的基础上,完成 **客户端栈的整体重构**：从 V1.0 的 `uni-app (Vue 3) + Python FastAPI` 跨端小程序方案,迁移到 **`Android Kotlin + Jetpack Compose` 原生 + Python FastAPI 后端** 的方案。V1.0 后端章节与产品逻辑保留,客户端章节全部重写。本版本主要差异：

| 章节 | V1.0 现状 | V2.0 调整 |
|------|----------|----------|
| §1 引言 | 跨端 uni-app 演示方案 | 明确 **Android 原生 + 云端后端** 方案 |
| §2 系统架构 | uni-app / Vuex / FastAPI | Android MVVM / 手写 DI / Retrofit / Room / FastAPI |
| §3 技术选型 | Vue 3 / Pinia / uView / ECharts | Kotlin / Compose / Room / Retrofit / Moshi / ECharts(WebView) |
| §4 模块划分 | 客户端 JS Store + 后端 Services | Android `data / domain / ui` 三层 + 后端 Services（保留） |
| §5 端云协同 | 同 V1.0 框架 | 加密简化为 **EncryptedSharedPreferences**（MVP 不引 Tink） |
| §6 数据模型 | `uni.storage` JS 对象 | **Room 实体 + DataStore** |
| §7 接口设计 | RESTful JSON | RESTful JSON 不变，客户端增加 **离线缓存策略** |
| §8 核心流程 | 流程不变 | 增加 **协程作用域、错误恢复、离线降级** 说明 |
| §9 算法 | Python 实现 | 客户端 / 后端分工明确，Android 端新增 **ML Kit OCR、SpeechRecognizer** |
| §10 隐私安全 | crypto-js AES | **EncryptedSharedPreferences + Room 加密字段** |
| §11 异常降级 | 同 V1.0 | 增加 **Android 特定场景**（无网络、OOM、权限拒绝） |
| §12 非功能 | 微信小程序指标 | **冷启动、内存、APK 体积** |
| §13 附录 | uni-app 目录 | **Android Gradle 标准目录** + 后端目录 |

### V2.0 选型原则（精简 / AI 友好）
本项目由团队使用 AI 辅助开发，且比赛以**创意与演示**为优先，因此 V2.0 客户端栈做了**有意精简**，所有选择都优先考虑"AI 训练数据丰富、Google 官方文档完整"：
- ✅ **保留**：Kotlin / Compose / Room / DataStore / Retrofit / Coil / Navigation Compose / Material3 / Moshi / ECharts(WebView) / ML Kit / SpeechRecognizer
- ❌ **暂不引入**：Hilt（手写 `AppContainer` 替代）、Tink（用 `EncryptedSharedPreferences` 替代）、WorkManager（同步走 `viewModelScope`）、KSP（用 kapt 或反射）、Compose Multiplatform
- ❌ **后端降级**：从 PostgreSQL + pgvector 降为 **SQLite + numpy**（单用户演示够用，未来扩展再升级）

> 本版本 **不覆盖** 的内容：核心业务逻辑（提问溯源、聚光灯、费曼、图谱流程）的产品语义沿用 V1.0，**仅在技术实现层做 Android 化重写**。

---

## 1. 引言
### 1.1 文档目的
本文档基于《蓝链 BlueLink 产品需求文档 V2.0》，定义 **Android 原生客户端** + **Python FastAPI 后端** 的完整技术实现方案。覆盖模块划分、接口规范、数据模型、关键算法流程与端云协同机制。

### 1.2 设计原则
- **原生优先，体验完整**：采用 Android 官方推荐的 Kotlin + Jetpack Compose 技术栈，确保锚点跳转、聚光灯阅读、知识图谱、OCR、STT 等能力均能调用系统原生 API，不因跨端框架损失体验。
- **隐私兜底，渐进增强**：MVP 阶段通过 **Tink + Android Keystore 端侧加密** + **客户端脱敏 + 云端 API** 实现数据保护；架构上预留 `OnDeviceLLMProvider` 接口，未来可平滑下沉端侧模型。
- **云端 AI，端侧交互**：用云端大模型 API 精准复现"只给路径不给答案"的溯源逻辑；客户端只负责 UI、缓存、脱敏、调度，不直接调用大模型。
- **可扩展**：业务逻辑层（UseCase / Repository / ViewModel）保持纯 Kotlin，未来可借助 KMP 共享到 iOS；UI 层未来可借助 Compose Multiplatform 迁移。

### 1.3 新人术语速查（V2.0 补充）

> 第一次接触 Android 的同学可以先扫一遍这张表，再去读后续章节。

| 术语 | 一句话解释 |
|------|-----------|
| **Kotlin** | Android 官方主推的编程语言，类似 Swift 之于 iOS。本项目唯一 Android 端语言。 |
| **Jetpack Compose** | Google 2021 年推出的声明式 UI 框架，类似 React/Vue for Android。写 UI 不再用 XML，全是 `@Composable` 函数。 |
| **`@Composable`** | 标记一个函数是 UI 组件。函数名大写，描述"这块屏幕长什么样"。 |
| **Material3 (M3)** | Google 的最新设计规范库，提供 `Button` / `Card` / `NavigationBar` 等现成组件。 |
| **Kotlin 协程 (Coroutine)** | Kotlin 的异步方案，比 Java 线程轻量百倍。用 `suspend fun` 标记可挂起函数，用 `viewModelScope.launch { ... }` 启动。 |
| **Flow** | 协程版的"响应式数据流"。`StateFlow` 存当前状态，`SharedFlow` 发一次性事件。 |
| **ViewModel** | 屏幕级别的状态容器。屏幕旋转不会丢数据。通过 `StateFlow` 暴露给 UI 订阅。 |
| **Room** | Google 官方的 SQLite ORM。比裸 SQL 安全（编译期校验）、方便（注解 DAO）。 |
| **DataStore** | 替代老的 SharedPreferences。异步、类型安全、Coroutines/Flow 友好。 |
| **Hilt** | Google 官方的依赖注入（DI）框架。**本项目不用**，因为 AI 写 Hilt 容易出错。 |
| **AppContainer（手写 DI）** | 本项目替代 Hilt 的方案：一个单例类，在 `Application.onCreate` 中实例化，持有所有 Repository / DataSource。**总共 50 行**，详见 §4.1.1。 |
| **CompositionLocal** | Compose 的"环境变量"。`LocalContext.current` / `LocalAppContainer.current` 都是在 Composable 树中传递值。本项目用 `LocalAppContainer` 注入 AppContainer。 |
| **Retrofit** | 网络请求库。把 HTTP API 变成 Kotlin 接口。 |
| **OkHttp** | Retrofit 底层的 HTTP 客户端，拦截器链可加 Token / Logging。 |
| **Moshi** | JSON 解析库（与 Retrofit 配合）。DTO 用 `@JsonClass(generateAdapter = true)` 标注。 |
| **Coil** | Compose 版的图片加载库（`AsyncImage`）。 |
| **Modifier** | Compose 修饰符，链式调用给组件加 `padding` / `clickable` / `background` / `alpha` 等。 |
| **`LazyColumn` / `LazyVerticalStaggeredGrid`** | Compose 的"长列表/瀑布流"组件，类似 RecyclerView。 |
| **`derivedStateOf { ... }`** | 性能优化工具。当输入不变时，输出"派生状态"不会触发 UI 重组。本项目聚光灯用。 |
| **`by viewModel(...)`** | 简化 ViewModel 注入的 Compose 语法糖。 |
| **`StateFlow.collectAsStateWithLifecycle()`** | Compose 订阅 StateFlow 的"生命周期安全"写法。 |
| **`viewModelScope.launch { ... }`** | 在 ViewModel 中启动协程的标准写法，ViewModel 销毁时自动取消。 |
| **`hilt-navigation-compose`** | Hilt 提供的 ViewModel 注入库，**本项目不用**。 |
| **kapt / KSP** | Kotlin 注解处理工具（编译期生成代码）。Room / Moshi / Hilt 都靠它。本项目用 **kapt**。 |
| **Splash Screen API** | Android 12+ 自带的启动图能力，无需额外代码。 |
| **Android Keystore** | 系统级密钥保险箱，主密钥永不离开硬件。`EncryptedSharedPreferences` 自动用它。 |
| **MVVM** | Model-View-ViewModel 架构模式。View = Composable，ViewModel = 状态容器，Model = Repository + DataSource。 |
| **Material You / 动态取色** | Android 12+ 根据用户壁纸自动生成主题色。`dynamicLightColorScheme(context)` 一行启用。 |

## 2. 系统架构设计
### 2.1 总体架构
```plain
┌──────────────────────────────────────────────────────────────────┐
│              Android App (Kotlin + Jetpack Compose)              │
│ ┌──────────────────────────────────────────────────────────────┐ │
│ │                    UI 层 (@Composable)                       │ │
│ │  HomeScreen / ChatScreen / ReaderScreen / GraphScreen / Mine │ │
│ │              Navigation Compose 路由 + 状态管理                │ │
│ └──────────────────────────┬───────────────────────────────────┘ │
│                            │ StateFlow / SharedFlow              │
│ ┌──────────────────────────┴───────────────────────────────────┐ │
│ │       ViewModel 层 (AndroidX ViewModel，手写 DI 注入)         │ │
│ │   HomeVM / ChatVM / ReaderVM / GraphVM / CaptureVM / MineVM  │ │
│ └──────────────────────────┬───────────────────────────────────┘ │
│                            │ suspend fun / Flow                  │
│ ┌──────────────────────────┴───────────────────────────────────┐ │
│ │        Domain 层 (UseCase，纯 Kotlin，零 Android 依赖)        │ │
│ │   AskQuestionUseCase / FeynmanEvaluateUseCase / ...           │ │
│ └──────────────────────────┬───────────────────────────────────┘ │
│                            │ Repository 接口                      │
│ ┌──────────────────────────┴───────────────────────────────────┐ │
│ │   Data 层 (Repository 实现 + DataSource + 本地持久化)        │ │
│ │   ┌─────────────┐  ┌──────────────┐  ┌─────────────────┐   │ │
│ │   │ RemoteDS    │  │ LocalDS      │  │ EncryptedPrefs  │   │ │
│ │   │ (Retrofit)  │  │ (Room)       │  │ (androidx.sec.) │   │ │
│ │   └─────────────┘  └──────────────┘  └─────────────────┘   │ │
│ │   ┌─────────────┐  ┌──────────────┐  ┌─────────────────┐   │ │
│ │   │ OCR DataSrc │  │ STT DataSrc  │  │ PrivacyManager  │   │ │
│ │   │ (ML Kit)    │  │ (SpeechRec.) │  │ (脱敏工具)       │   │ │
│ │   └─────────────┘  └──────────────┘  └─────────────────┘   │ │
│ └──────────────────────────────────────────────────────────────┘ │
│                                                                    │
│ ┌──────────────────────────────────────────────────────────────┐ │
│ │         AppContainer（手写 DI 容器，单例）                    │ │
│ │  在 Application.onCreate 中创建，所有 Repository 单例化       │ │
│ └──────────────────────────────────────────────────────────────┘ │
└──────────────────────────┬───────────────────────────────────────┘
                           │ HTTPS (TLS 1.3) / OkHttp
┌──────────────────────────┴───────────────────────────────────────┐
│                     后端服务：Python + FastAPI                    │
│  ┌──────────┐  ┌────────────┐  ┌──────────┐  ┌──────────────┐    │
│  │ APIRouter│  │ Services   │  │ AI 调度器 │  │ 数据持久层   │    │
│  │ (FastAPI)│  │ (业务逻辑) │  │ (Kimi /  │  │ (SQLite +    │    │
│  │          │  │            │  │ DeepSeek)│  │  numpy 向量)  │    │
│  └──────────┘  └────────────┘  └──────────┘  └──────────────┘    │
└──────────────────────────────────────────────────────────────────┘
```

### 2.2 关键说明
- **客户端**：Kotlin + Compose 负责全部 UI 与用户交互。Room 存文档、切片、灵感、图谱等结构化数据；DataStore 存设置；Tink + Keystore 做加密。AI 请求全部通过 Retrofit → FastAPI 后端，客户端不持有 API Key。
- **后端**：FastAPI 提供 RESTful API，集成 Moonshot/DeepSeek SDK，实现文档解析、切片、去重、锚点生成、费曼评估、图谱关系发现等核心 AI 逻辑。
- **隐私保障**：用户私有内容（灵感、标记私密的文档）仅上传脱敏摘要或向量片段进行必要计算，原文不离开客户端。客户端本地数据库全量加密。
- **原生能力直采**：OCR 走 ML Kit、STT 走 SpeechRecognizer、PDF 渲染走 Android `PdfRenderer`、加密走 Android Keystore + Tink，**不绕道 WebView**。

## 3. 技术选型
### 3.1 客户端（Android，V2.0 精简栈）
> **选型原则**：优先 AI 训练数据丰富、Google 官方文档完整的方案；剔除对比赛演示增益低、AI 容易写错的部分。

| 维度 | 选择 | 版本/理由 |
|------|------|-----------|
| 语言 | **Kotlin** | 2.0+；Google 官方主推 |
| UI 框架 | **Jetpack Compose** | BOM 2026.06+；声明式 |
| 架构 | **MVVM + Repository** | UI ↔ ViewModel ↔ UseCase ↔ Repository ↔ DataSource |
| 异步 | **Kotlin 协程 + Flow** | `viewModelScope` / `lifecycleScope` |
| 依赖注入 | **手写 AppContainer**（单例） | ❌ 不用 Hilt，AI 写 Hilt module 易出错；改用一个 `AppContainer` 类持有所有 Repository 单例 |
| 网络 | **Retrofit 2 + OkHttp 4** | 标准；支持 suspend fun |
| JSON | **Moshi**（Kotlin codegen） | ❌ 不用 kotlinx.serialization（AI 训练数据偏少）；Moshi 注解 AI 写得更稳 |
| 数据库 | **Room** | 2.7+；支持 Flow / suspend |
| 偏好设置 | **DataStore (Preferences)** | 异步、类型安全 |
| 加密 | **EncryptedSharedPreferences**（`androidx.security:security-crypto`） | ❌ 不用 Tink；`EncryptedSharedPreferences` API 简单，AI 写起来零失误 |
| 导航 | **Navigation Compose** | 官方 |
| 图像加载 | **Coil 3** | Compose `AsyncImage` |
| 图表 | **WebView + ECharts 5** | MVP 方案最稳 |
| PDF 渲染 | **Android `PdfRenderer`** | 系统 API；零依赖 |
| OCR | **ML Kit Text Recognition** | 端侧、免费 |
| 语音 | **SpeechRecognizer** | 系统 API |
| 后台任务 | **不接 WorkManager** | ❌ 同步逻辑走 `viewModelScope`，简化 MVP |
| 单元测试 | JUnit4 + MockK + Turbine | ViewModel / UseCase 测试 |
| UI 测试 | Compose UI Test + Espresso | 关键流程 |
| 构建 | Gradle KTS + Version Catalog | 已配置 |

### 3.2 后端服务（V2.0 精简）
> **比赛演示场景**：单用户、单设备、无并发压力，V2.0 把 V1.0 的 PostgreSQL + pgvector 降级为 **SQLite + numpy**，**部署只需要一条 `uvicorn` 命令**。

| 维度 | 选择 | 备注 |
|------|------|------|
| Web 框架 | **FastAPI** | Python 3.11+ |
| 大模型 API | **Moonshot (Kimi) / DeepSeek** | 按需切换 |
| 向量存储 | **numpy + pickle**（本地） | ❌ 不引 Qdrant / pgvector；用 `numpy.ndarray` 存向量，进程内余弦相似度即可 |
| 数据库 | **SQLite**（`sqlite3` + `SQLAlchemy` 同步 ORM） | ❌ 不切 PostgreSQL；单文件 `bluelink.db` 随项目走 |
| 文档解析 | PyMuPDF / python-docx / markdown | 客户端不解析 |
| 任务队列 | **不引 Celery** | ❌ 同步处理；去重计算几秒内可完成 |
| 对象存储 | **本地文件系统**（`./uploads/`） | ❌ 不引 MinIO；非敏感文档原文本地存 |
| 部署 | `uvicorn app.main:app --reload` | 一行命令起服务，AI 部署零门槛 |

### 3.3 部署方案
- **后端**：Docker 化部署在云服务器，Nginx 反代，开放 HTTPS。PostgreSQL + Qdrant 独立容器。
- **客户端**：通过 `./gradlew :app:bundleRelease` 生成 AAB，上架 Google Play / 国内应用市场。
- **CI/CD（建议）**：GitHub Actions 跑 lint + test + assemble，CD 到 Play Console Internal Track。

## 4. 模块划分与职责

### 4.1 客户端模块（按 `data / domain / ui` 分层，V2.0 精简）
| 模块（包名） | 职责 | 关键类/接口 |
|--------------|------|------------|
| `com.yjtzc.bluelink.data.remote` | 网络层：Retrofit Service、OkHttp 拦截器、错误转换 | `BlueLinkApi`、`AuthInterceptor`、`NetworkResult` |
| `com.yjtzc.bluelink.data.local.db` | Room 数据库 | `AppDatabase`、`DocumentDao`、`SegmentDao`、`AnchorDao`、`InspirationDao`、`GraphNodeDao`、`GraphEdgeDao` |
| `com.yjtzc.bluelink.data.local.prefs` | DataStore 偏好 | `UserPreferences`（profile、粒度、隐私开关） |
| `com.yjtzc.bluelink.data.local.crypto` | 加密：EncryptedSharedPreferences 封装 | `SecurePrefs`（`putSecret(key, value)` / `getSecret(key)`） |
| `com.yjtzc.bluelink.data.local.ocr` | ML Kit 文字识别 | `MlKitOcrDataSource`（`recognize(bitmap): Flow<String>`） |
| `com.yjtzc.bluelink.data.local.stt` | 语音转文字 | `SpeechRecognizerDataSource`（`start(callback): Unit`） |
| `com.yjtzc.bluelink.data.repository` | 仓库层：合并 remote + local + 缓存策略 | `DocumentRepository`、`AnchorRepository`、`GraphRepository`、`CaptureRepository`、`FeynmanRepository` |
| `com.yjtzc.bluelink.domain.usecase` | 业务用例：纯 Kotlin | `AskQuestionUseCase`、`ComputeDeltaUseCase`、`FeynmanEvaluateUseCase`、`BuildGraphUseCase`、`CaptureInspirationUseCase` |
| `com.yjtzc.bluelink.domain.model` | 领域模型：纯数据类 | `Document`、`Segment`、`Anchor`、`InspirationCard`、`GraphNode`、`GraphEdge`、`Deviation` |
| `com.yjtzc.bluelink.ui.home` | 首页 / 文库瀑布流 | `HomeScreen` + `HomeViewModel` |
| `com.yjtzc.bluelink.ui.chat` | 对话 / 锚点穿梭 | `ChatScreen` + `ChatViewModel` |
| `com.yjtzc.bluelink.ui.reader` | 阅读器 / 聚光灯 / 折叠 | `ReaderScreen` + `ReaderViewModel` |
| `com.yjtzc.bluelink.ui.graph` | 知识图谱 | `GraphScreen` + `GraphViewModel` + WebView 桥接 |
| `com.yjtzc.bluelink.ui.capture` | 灵感快捷捕获 | `CaptureSheet` + `CaptureViewModel` |
| `com.yjtzc.bluelink.ui.mine` | 设置 / 隐私 | `MineScreen` + `MineViewModel` |
| `com.yjtzc.bluelink.ui.theme` | Material3 主题 | `BlueLinkTheme` / `Color` / `Type` |
| `com.yjtzc.bluelink.ui.component` | 通用 Compose 组件 | `AnchorCard`、`SpotlightOverlay`、`FoldBar`、`GravityLine` |
| `com.yjtzc.bluelink.AppContainer` | **手写 DI 容器**（V2.0 替代 Hilt） | 单例类，在 `BlueLinkApp.onCreate` 中实例化；持有所有 Repository / DataSource |
| `com.yjtzc.bluelink.util.privacy` | 隐私工具 | `PrivacyManager`（脱敏、关键词提取、敏感等级判定） |

#### 4.1.1 AppContainer（手写 DI 容器）
**为什么不用 Hilt**：Hilt 的 `@HiltViewModel`、`@Module @Provides` 注解组合是 AI 写代码最容易出错的地方（漏 `@Singleton`、作用域串、生成的 component 难调试）。比赛项目规模下，**手写一个 50 行的单例容器**完全够用，且对 AI 友好、对人友好。

```kotlin
// AppContainer.kt —— 整个 App 唯一的 DI 入口
class AppContainer(applicationContext: Context) {
    // 0. Moshi（JSON 解析器）
    private val moshi: Moshi by lazy {
        Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
    }

    // 1. OkHttp + Retrofit
    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(userPrefs))
            .addInterceptor(HttpLoggingInterceptor().apply { level = BASIC })
            .connectTimeout(15, TimeUnit.SECONDS)
            .build()
    }
    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.BACKEND_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }
    private val api: BlueLinkApi by lazy { retrofit.create(BlueLinkApi::class.java) }

    // 2. Room
    private val database: AppDatabase by lazy {
        Room.databaseBuilder(applicationContext, AppDatabase::class.java, "bluelink.db")
            .fallbackToDestructiveMigration()
            .build()
    }

    // 3. DataStore + EncryptedSharedPreferences
    val userPrefs: UserPreferences by lazy { UserPreferences(applicationContext) }
    val securePrefs: SecurePrefs by lazy { SecurePrefs(applicationContext) }

    // 4. 仓库（单例）
    val documentRepository: DocumentRepository by lazy { DocumentRepository(api, database.documentDao(), securePrefs) }
    val anchorRepository: AnchorRepository by lazy { AnchorRepository(api, database.anchorDao(), database.segmentDao()) }
    val graphRepository: GraphRepository by lazy { GraphRepository(api, database.graphNodeDao(), database.graphEdgeDao()) }
    val captureRepository: CaptureRepository by lazy { CaptureRepository(api, database.inspirationDao(), securePrefs, mlKitOcr, speechRecognizer) }
    val feynmanRepository: FeynmanRepository by lazy { FeynmanRepository(api) }

    // 5. 原生能力
    private val mlKitOcr: MlKitOcrDataSource by lazy { MlKitOcrDataSource() }
    private val speechRecognizer: SpeechRecognizerDataSource by lazy { SpeechRecognizerDataSource(applicationContext) }
}

// ViewModel 工厂：把 AppContainer 注入
class BlueLinkViewModelFactory(private val container: AppContainer) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when {
        modelClass.isAssignableFrom(HomeViewModel::class.java) -> HomeViewModel(container.documentRepository) as T
        modelClass.isAssignableFrom(ChatViewModel::class.java) -> ChatViewModel(container.anchorRepository, container.feynmanRepository) as T
        // ... 其他 VM
        else -> error("Unknown VM: ${modelClass.name}")
    }
}
```

### 4.2 后端模块（沿用 V1.0，V2.0 仅升级数据库）
| 模块 | 职责 | 关键类/函数 |
|------|------|-------------|
| `DocumentService` | 解析上传文档，分段、清洗、基础元数据提取 | `parse_and_chunk(file) → [Segment]` |
| `DedupService` | 比对历史文档，计算重复段与信息增量 | `compute_delta(new_doc_id) → DeltaReport` |
| `AnchorEngine` | 接收用户提问，调用大模型生成引言和锚点卡片 | `generate_anchors(question, segments, granularity) → AnchorResponse` |
| `FeynmanEvaluator` | 调用大模型比较用户复述与原文，输出偏差与引力线 | `evaluate(user_text, orig_segments) → DeviationResult` |
| `GraphBuilder` | 抽取实体、计算关系、生成图谱数据 | `build_graph(user_id) → GraphData` |
| `PrivacyProxy` | 接收客户端脱敏数据，标记敏感级别 | `handle_data(payload, privacy_level)` |
| `APIRouter` | FastAPI 路由 + 鉴权 | `/api/v1/*` |

## 5. 端云协同机制
### 5.1 数据分级与传输原则
延续 PRD 分级：`LOCAL_ONLY`、`LOCAL_FIRST`、`CLOUD_OK`。

- **LOCAL_ONLY（灵感、私密标记文档）**：客户端**绝不**将原文上传。若需要语义理解，客户端提取关键词与 SimHash（不含原文）发送后端，或完全本地用规则匹配替代。Room 中对应记录的 `text` 字段**永不离开设备**。
- **LOCAL_FIRST（默认文档）**：上传时客户端发送文档分段文本给后端；后端处理完后立即删除原始文本，仅保留结构化的分段索引和加密向量。
- **CLOUD_OK（用户主动标记公开）**：可全量文本留存在服务器供后续分析。

### 5.2 AI 调用流程中的隐私保护
提问溯源时，客户端的处理流程：
1. 客户端发送用户提问 `q` 与当前知识库中**非私密**文档的分段 ID 列表。
2. 后端检索相关分段（已存储的分段文本或向量），调用大模型生成锚点。
3. 对于私密文档，客户端本地维护倒排索引（基于 Room FTS4 全文索引）。客户端自行检索出相关分段 ID，然后**仅将分段 ID 和脱敏的关键词**发给后端；后端根据 ID 调取已存储的向量（若有）或从客户端回传的脱敏特征进行匹配，最终返回锚点卡片信息。
4. **MVP 简化**：初赛阶段，私密文档的溯源完全由客户端本地实现（基于 Room FTS），后端不参与；非私密文档全量走 API。

### 5.3 离线支持（V2.0 简化）
- 客户端缓存最近使用的锚点、分段文本（`SecurePrefs` 加密存储），在无网络时仍可浏览已下载的文档与历史对话。
- 纯离线提问只支持基于 Room FTS 的关键词搜索，不生成 AI 引言。
- 离线写入操作（新增灵感、新增文档）进入"待同步队列"（`pending_sync` 表）。**MVP 不接 WorkManager**，由 `viewModelScope` 监听网络恢复（`ConnectivityManager.NetworkCallback`）后立即批量同步。

## 6. 数据模型设计

### 6.1 客户端 Room 实体

```kotlin
// DocumentEntity
@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey val id: String,            // UUID
    val title: String,
    val privacyLevel: PrivacyLevel,        // LOCAL_ONLY / LOCAL_FIRST / CLOUD_OK
    val source: String?,                   // 来源（本地路径 / URL / 录音）
    val createdAt: Long,
    val updatedAt: Long,
    val conceptBeacon: String?,            // 概念路标（云端生成后缓存）
    @ColumnInfo(name = "is_folded_global") val isFoldedGlobal: Boolean = false
)

// SegmentEntity —— 文档切片
@Entity(
    tableName = "segments",
    indices = [Index("docId"), Index(value = ["docId", "indexInDoc"], unique = true)]
)
data class SegmentEntity(
    @PrimaryKey val id: String,            // UUID
    val docId: String,
    val indexInDoc: Int,
    val text: String,                      // V2.0 MVP: 明文存储（依赖设备锁屏 + allowBackup=false）
    val vectorBlob: ByteArray? = null,     // 1024-d 浮点（仅非私密文档；本地生成 or 后端返回后缓存）
    val isFolded: Boolean = false,         // 增量折叠状态
    val isSpotlightTarget: Boolean = false // 聚光灯命中
)

// InspirationCardEntity —— 灵感卡片
@Entity(tableName = "inspiration_cards")
data class InspirationCardEntity(
    @PrimaryKey val id: String,
    val content: String,                   // V2.0 MVP: 明文存储
    val type: CardType,                    // TEXT / VOICE / IMAGE
    val privacyLevel: PrivacyLevel = PrivacyLevel.LOCAL_ONLY,
    val tags: String,                      // 逗号分隔关键词
    val createdAt: Long
)

// AnchorEntity —— 锚点缓存
@Entity(tableName = "anchors")
data class AnchorEntity(
    @PrimaryKey val id: String,
    val queryHash: String,                 // SHA-256(question + scope)
    val segmentId: String,
    val docTitle: String,
    val snippet: String,                   // 片段前 20 字（明文，便于列表展示）
    val score: Float,
    val isRead: Boolean = false,
    val createdAt: Long
)

// GraphNodeEntity / GraphEdgeEntity —— 知识图谱
@Entity(tableName = "graph_nodes")
data class GraphNodeEntity(
    @PrimaryKey val id: String,
    val label: String,
    val type: NodeType,                    // DOCUMENT / INSPIRATION / CONCEPT
    val refId: String?
)

@Entity(
    tableName = "graph_edges",
    primaryKeys = ["sourceId", "targetId"],
    indices = [Index("sourceId"), Index("targetId")]
)
data class GraphEdgeEntity(
    val sourceId: String,
    val targetId: String,
    val relation: RelationType,            // SUPPORT / CHALLENGE / SUPPLEMENT / CITE
    val confidence: Float,
    val isManual: Boolean = false
)

// PendingSyncEntity —— 离线写入队列
@Entity(tableName = "pending_sync")
data class PendingSyncEntity(
    @PrimaryKey val id: String,
    val operation: SyncOp,                 // CREATE_DOC / CREATE_CARD / UPDATE_PROFILE
    val payloadJson: String,               // V2.0 MVP: 同步负载用 JSON 明文，依赖 HTTPS
    val createdAt: Long,
    val retryCount: Int = 0
)

// 枚举
enum class PrivacyLevel { LOCAL_ONLY, LOCAL_FIRST, CLOUD_OK }
enum class CardType { TEXT, VOICE, IMAGE }
enum class NodeType { DOCUMENT, INSPIRATION, CONCEPT }
enum class RelationType { SUPPORT, CHALLENGE, SUPPLEMENT, CITE }
enum class SyncOp { CREATE_DOC, CREATE_CARD, UPDATE_PROFILE, FOLD_SEGMENT }
```

**FTS 全文索引（用于本地私密文档检索）**：
```sql
CREATE VIRTUAL TABLE segments_fts USING fts4(
    content="segments",
    content_rowid=rowid,
    tokenize="unicode61"
);
-- 写入触发器：插入/更新 segments 时同步加密后写入 segments_fts.content_enc
```

### 6.2 DataStore 偏好
| Key | 类型 | 说明 |
|-----|------|------|
| `user_profile_major` | String | 专业 |
| `user_profile_terminology` | String (JSON) | 术语映射表 |
| `default_granularity` | String | `PARAGRAPH` / `SENTENCE` |
| `directness_level` | Float (0..1) | 0=温和启发，1=直指核心 |
| `explore_depth` | Boolean | 深度探索开关 |
| `tone_style` | String | `GENTLE` / `ENCOURAGE` / `SERIOUS` |
| `privacy_mode` | String | `LOCAL_ONLY` / `LOCAL_PLUS_CLOUD` |
| `webdav_endpoint` | String | WebDAV 同步地址（可选） |
| `webdav_credentials_enc` | ByteArray | WebDAV 凭据（AES-GCM 加密后存） |

### 6.3 后端 SQLite 表（V2.0 简化，单文件 `bluelink.db`）
```sql
-- 文档元数据（仅非私密）
CREATE TABLE documents (
    id TEXT PRIMARY KEY,             -- UUID 字符串
    user_id TEXT NOT NULL,
    title TEXT NOT NULL,
    privacy_level TEXT NOT NULL,     -- LOCAL_ONLY / LOCAL_FIRST / CLOUD_OK
    created_at INTEGER NOT NULL
);
CREATE INDEX idx_documents_user ON documents(user_id);

-- 切片信息（文本加密存储，向量存 numpy 不入 DB）
CREATE TABLE segments (
    id TEXT PRIMARY KEY,
    doc_id TEXT NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    index_num INTEGER NOT NULL,
    text_encrypted BLOB,             -- Fernet 对称加密
    is_folded INTEGER DEFAULT 0
);
CREATE INDEX idx_segments_doc ON segments(doc_id);

-- 锚点缓存
CREATE TABLE anchors (
    id TEXT PRIMARY KEY,
    query_hash TEXT NOT NULL,
    segment_id TEXT NOT NULL REFERENCES segments(id),
    snippet TEXT,
    score REAL
);
CREATE INDEX idx_anchors_query ON anchors(query_hash);

-- 图谱节点与边
CREATE TABLE graph_nodes (
    id TEXT PRIMARY KEY,
    label TEXT NOT NULL,
    type TEXT NOT NULL,              -- DOCUMENT / INSPIRATION / CONCEPT
    ref_id TEXT
);
CREATE TABLE graph_edges (
    id TEXT PRIMARY KEY,
    source_id TEXT NOT NULL REFERENCES graph_nodes(id),
    target_id TEXT NOT NULL REFERENCES graph_nodes(id),
    relation_type TEXT NOT NULL,     -- SUPPORT / CHALLENGE / SUPPLEMENT / CITE
    confidence REAL,
    is_manual INTEGER DEFAULT 0
);

-- 文档向量（numpy 序列化，进程内余弦相似度）
-- 不入 SQLite，存为 `vectors/{doc_id}.npy` 文件
```

> **向量存储说明**：V2.0 不引 pgvector / Qdrant。所有文档向量用 `numpy.ndarray` 存为 `./vectors/{doc_id}.npy`，检索时 `np.load` + `sklearn.metrics.pairwise.cosine_similarity`。文档数 < 1000 时性能完全够用；超过后改为 `faiss-cpu` 即可（API 兼容）。
```

## 7. 接口设计
### 7.1 后端 API 列表
所有接口均以 `/api/v1` 为前缀，请求需带 Header `Authorization: Bearer <user_token>`。

#### 文档相关
- `POST /documents/upload`  
  上传文档文件，返回 `document_id` 与解析状态。  
  Body: `multipart/form-data`（file + privacy_level）

- `GET /documents/{doc_id}/segments`  
  获取某文档所有分段（摘要，不含原文）。  
  Response: `{ segments: [{ id, index, summary, folded }] }`

- `GET /documents?cursor=...&limit=50`  
  分页拉取文档列表（云端可见部分）。

#### 溯源提问
- `POST /questions/ask`  
  核心接口。  
  Body:
  ```json
  {
    "query": "用户问题",
    "granularity": "SENTENCE" | "PARAGRAPH",
    "scope_doc_ids": ["doc_id1","doc_id2"]
  }
  ```
  Response:
  ```json
  {
    "introduction": "引导引言...",
    "anchors": [
      {
        "anchor_id": "uuid",
        "doc_title": "xxx",
        "snippet": "原文前20字...",
        "segment_id": "seg-xxx",
        "score": 0.95
      }
    ]
  }
  ```

#### 去重与增量
- `POST /documents/{doc_id}/compute_delta`  
  异步触发去重计算，返回 `task_id`，后续通过 `GET /tasks/{task_id}` 查询。

#### 费曼伴学
- `POST /feynman/evaluate`  
  Body:
  ```json
  {
    "user_explanation": "用户用自己的话解释的内容",
    "target_concept": "概念名称",
    "context_segment_ids": ["seg1","seg2"]
  }
  ```
  Response: `DeviationResult`（deviations 列表 + summary + gravity_lines）

#### 知识图谱
- `GET /graph?cursor=...&limit=500`  
  返回全量图谱节点与边。**V2.0 增加**：支持 cursor 分页，避免大库一次返回卡死客户端。

#### 灵感卡片
- `POST /cards`  
  上传灵感卡片文本（脱敏），后端进行关键词提取、关联发现，返回卡片 ID 和候选链接。

#### 同步相关
- `POST /sync/batch`  
  客户端离线队列批量上传。

### 7.2 客户端网络层设计
```kotlin
// BlueLinkApi.kt —— Retrofit 接口
interface BlueLinkApi {
    @Multipart
    @POST("api/v1/documents/upload")
    suspend fun uploadDocument(
        @Part file: MultipartBody.Part,
        @Part("privacy_level") privacyLevel: PrivacyLevel
    ): Response<DocumentDto>  // 用 Response<T> 包裹，让 Repository 决定如何映射错误

    @POST("api/v1/questions/ask")
    suspend fun askQuestion(@Body request: AskRequest): Response<AskResponse>

    @POST("api/v1/feynman/evaluate")
    suspend fun evaluateFeynman(@Body request: FeynmanRequest): Response<FeynmanResponse>

    @GET("api/v1/graph")
    suspend fun fetchGraph(
        @Query("cursor") cursor: String? = null,
        @Query("limit") limit: Int = 500
    ): Response<GraphDto>

    @POST("api/v1/cards")
    suspend fun createInspiration(@Body request: CreateCardRequest): Response<InspirationDto>

    @GET("api/v1/documents")
    suspend fun listDocuments(
        @Query("cursor") cursor: String? = null,
        @Query("limit") limit: Int = 50
    ): Response<DocumentListDto>
}

// NetworkResult —— 统一错误模型（在 Repository 层把 Response<T> 映射成 Result<T>）
sealed class NetworkResult<out T> {
    data class Success<T>(val data: T) : NetworkResult<T>()
    data class Error(val code: Int, val message: String, val cause: Throwable? = null) : NetworkResult<Nothing>()
    data object NetworkUnavailable : NetworkResult<Nothing>()
}
```

#### 7.2.1 完整 DTO 定义（Moshi @JsonClass，AI 可直接照抄）

```kotlin
// data/remote/dto/CommonDto.kt
@JsonClass(generateAdapter = true)
data class DocumentDto(
    val id: String,
    val title: String,
    val privacyLevel: String,           // LOCAL_ONLY / LOCAL_FIRST / CLOUD_OK
    val source: String?,
    val createdAt: Long,
    val conceptBeacon: String? = null
)

@JsonClass(generateAdapter = true)
data class DocumentListDto(
    val items: List<DocumentDto>,
    val nextCursor: String?
)

// data/remote/dto/AskDto.kt
@JsonClass(generateAdapter = true)
data class AskRequest(
    val query: String,
    val granularity: String,            // SENTENCE / PARAGRAPH
    val scopeDocIds: List<String>? = null,
    val localSegmentIds: List<String>? = null  // 私密文档本地命中的分段 ID（仅 ID，不含原文）
)

@JsonClass(generateAdapter = true)
data class AskResponse(
    val introduction: String,
    val anchors: List<AnchorDto>
)

@JsonClass(generateAdapter = true)
data class AnchorDto(
    val anchorId: String,
    val docTitle: String,
    val snippet: String,                 // 片段前 30 字
    val segmentId: String,
    val score: Float,
    val isLocal: Boolean = false         // true = 本地私密文档命中
)

// data/remote/dto/FeynmanDto.kt
@JsonClass(generateAdapter = true)
data class FeynmanRequest(
    val userExplanation: String,
    val targetConcept: String,
    val contextSegmentIds: List<String>
)

@JsonClass(generateAdapter = true)
data class FeynmanResponse(
    val summary: String,
    val deviations: List<DeviationDto>,
    val gravityLines: List<GravityLineDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class DeviationDto(
    val userSegment: String,
    val deviationType: String,          // OMISSION / CONTRADICTION / OVER_EXTENSION
    val explanation: String,
    val originalSnippet: String,
    val anchorSegmentId: String
)

@JsonClass(generateAdapter = true)
data class GravityLineDto(
    val from: Int,                       // 用户表述中偏差的字符位置
    val toSegmentId: String
)

// data/remote/dto/GraphDto.kt
@JsonClass(generateAdapter = true)
data class GraphDto(
    val nodes: List<GraphNodeDto>,
    val edges: List<GraphEdgeDto>,
    val nextCursor: String? = null
)

@JsonClass(generateAdapter = true)
data class GraphNodeDto(
    val id: String,
    val label: String,
    val type: String,                    // DOCUMENT / INSPIRATION / CONCEPT
    val refId: String? = null
)

@JsonClass(generateAdapter = true)
data class GraphEdgeDto(
    val source: String,
    val target: String,
    val relation: String,                // SUPPORT / CHALLENGE / SUPPLEMENT / CITE
    val confidence: Float,
    val isManual: Boolean = false
)

// data/remote/dto/InspirationDto.kt
@JsonClass(generateAdapter = true)
data class CreateCardRequest(
    val content: String,                 // 脱敏后
    val type: String,                    // TEXT / VOICE / IMAGE
    val privacyLevel: String,
    val tags: List<String> = emptyList()
)

@JsonClass(generateAdapter = true)
data class InspirationDto(
    val id: String,
    val content: String,
    val type: String,
    val privacyLevel: String,
    val tags: List<String>,
    val createdAt: Long
)
```

#### 7.2.2 Repository 层 `Response<T> → Result<T>` 映射模板
```kotlin
// data/repository/BaseRepository.kt
protected suspend fun <T : Any> safeApiCall(call: suspend () -> Response<T>): Result<T> {
    return try {
        val response = call()
        if (response.isSuccessful) {
            val body = response.body() ?: return Result.failure(IllegalStateException("空响应"))
            Result.success(body)
        } else {
            val errorBody = response.errorBody()?.string().orEmpty()
            Result.failure(HttpException(response).apply {
                // 可选：解析 errorBody 中的 error_code
            })
        }
    } catch (e: UnknownHostException) {
        AppEventBus.emit(AppEvent.RequestFailed("网络不可用"))
        Result.failure(e)
    } catch (e: SocketTimeoutException) {
        AppEventBus.emit(AppEvent.RequestFailed("请求超时"))
        Result.failure(e)
    } catch (e: JsonDataException) {
        AppEventBus.emit(AppEvent.DataParseError)
        Result.failure(e)
    } catch (e: Exception) {
        AppEventBus.emit(AppEvent.RequestFailed(e.message ?: "未知错误"))
        Result.failure(e)
    }
}
```

### 7.3 客户端离线缓存策略
| 接口 | 缓存策略 | 实现 |
|------|----------|------|
| `GET /documents` | **Stale-While-Revalidate** | Room `documents` 表 + 后台 `WorkManager` 周期同步 |
| `GET /graph` | **Cache-First with TTL** | Room `graph_*` 表 + TTL 30 分钟 |
| `POST /questions/ask` | **不缓存**，每次拉新 | 命中本地关键词索引的私密文档部分，客户端本地组装 |
| `POST /cards` | **离线可写**，进入 pending_sync | WorkManager 联网后批量上传 |

### 7.4 私密文档的提问流程
私密文档原文绝不出设备。客户端 `PrivacyManager` 工作流程：
1. 用户提问 → 客户端先在 Room FTS 中检索本地私密文档相关分段 ID。
2. 仅将 `query` + `非私密 scope_doc_ids` + `私密 segment_ids` + 脱敏关键词发往后端。
3. 后端处理后返回锚点；客户端再与本地私密分段锚点合并展示。

## 8. 核心功能流程设计

### 8.1 提问溯源流程（重点）
1. 用户在 ChatScreen 输入问题，ChatViewModel 持有 `query: StateFlow<String>`。
2. ChatViewModel 调用 `AskQuestionUseCase`：
   - UseCase 从 `AnchorRepository` 取非私密分段 ID + 调本地 FTS 取私密分段 ID。
   - 合并后调用 `BlueLinkApi.askQuestion`。
3. 后端检索相关知识库分段，调用 Moonshot API（Prompt 模板见 V1.0 §9.1），返回 `AskResponse`。
4. ChatViewModel 写入 `AnchorDao` 缓存锚点（明文 snippet 字段不入密，便于列表展示）。
5. UI 展示锚点卡片。用户点击锚点 → 导航至 `ReaderScreen(segmentId=...)`。
6. ReaderViewModel 根据 `segmentId` 从 `SegmentDao` 取 `text`（明文），直接展示。
7. ReaderScreen 根据 `targetSegmentId` 计算 `SpotlightOverlay` 状态，其余段落 `Modifier.alpha(0.3f)`。

### 8.2 入库去重流程
1. 用户选择文件 → `CaptureViewModel.uploadDocument(file, privacyLevel)`。
2. 若 `LOCAL_ONLY`：客户端调用 ML Kit（如果是图片）或读取纯文本，自行分段、加密、入 Room。**完全不上传**。
3. 若 `LOCAL_FIRST` / `CLOUD_OK`：文件上传至后端 `DocumentService`。
4. 后端 `DocumentService` 解析、暂存分段，调用 `DedupService` 比对历史分段（SimHash + pgvector 余弦相似度）。
5. 后端将新分段（含重复标记）加密存储，`LOCAL_FIRST` 文档处理后**立即删除原始文本**。
6. 返回 `DeltaReport`，客户端据其更新 `SegmentEntity.isFolded`。

### 8.3 费曼伴学偏差检测流程
1. 用户进入费曼模式，选择某概念。客户端从本地或后端获取该概念关联的原文分段（最多 3 段）。
2. 用户语音（SpeechRecognizer 转文字）或键盘输入解释。
3. ChatViewModel 调用 `FeynmanEvaluateUseCase` → `BlueLinkApi.evaluateFeynman`。
4. 后端调取分段文本，构造 Prompt 让大模型做 NLI 比对，返回结构化 `DeviationResult`。
5. 客户端拿到偏差列表，为每个偏差生成 `GravityLine` 数据，导航至 ReaderScreen 并高亮对应位置。

### 8.4 知识图谱更新流程
- 文档入库或灵感捕获后，客户端通知后端（`POST /graph/invalidate`）。
- 后台异步执行实体抽取、关系发现，更新 `graph_nodes` / `graph_edges` 表。
- 客户端下次进入 GraphScreen 时，`GraphRepository` 走 Stale-While-Revalidate 拉取最新数据。
- 实时性要求高的场景可后续引入 **WebSocket / SSE** 推送增量更新（V2.0 不实现）。

### 8.5 OCR 灵感捕获流程
1. 用户拍照 → `CaptureViewModel.onImageCaptured(uri)`。
2. `MlKitOcrDataSource.recognize(bitmap)` 异步返回识别文本。
3. 客户端展示识别结果，用户可编辑、选标签、定隐私级别。
4. 提交后 `CaptureRepository.saveInspiration(content, type=IMAGE, privacyLevel=LOCAL_ONLY)`。
5. 文本 `SecureStorage.encrypt` 后写入 `inspiration_cards` 表。

### 8.6 STT 灵感捕获流程
1. 用户长按录音 → `SpeechRecognizerDataSource.start()`。
2. 实时返回 `partialResult: Flow<String>`，UI 同步显示。
3. 松手 → `stop()`，提交最终文本，走 8.5 第 3 步之后流程。

## 9. 关键算法与数据结构

### 9.1 锚点生成 Prompt 模板（V2.0 完整正文，AI 可直接调用）

#### 9.1.1 锚点生成（后端 `AnchorEngine.generate_anchors`）
后端在调用大模型前，把"用户提问 + 检索到的相关切片 + 用户的画像术语偏好"组装为以下 Prompt：

```python
ANCHOR_SYSTEM_PROMPT = """
你是一个只提供原文线索而不直接给出答案的 AI 助手。
你的唯一职责是：根据用户问题，从给定的文档片段中找到**最相关的原文锚点**，引导用户回到原文自行思考。

【铁律】
1. 严禁直接回答用户问题、严禁总结、严禁给出结论或建议。
2. 引言最多 3 句话，**不出现最终答案**，只指出问题的关键矛盾或值得注意之处。
3. 引言中如需举例，**只能引用原文片段中的字句**，不能自造。
4. 锚点必须来自【文档片段】列表，禁止虚构文档或片段。
5. 如果【文档片段】中没有相关内容，introduction 写"当前知识库中暂未发现相关内容"，anchors 留空数组。
6. 严格按 JSON 格式输出，不要包含任何额外解释、Markdown 代码块标记或前后缀文字。

【输出 JSON 格式】
{
  "introduction": "string, ≤ 3 句话",
  "anchors": [
    {
      "doc_title": "string",
      "snippet": "string, 原文片段前 30 字",
      "segment_id": "string, 必须从【文档片段】中选取",
      "score": "float, 0.0~1.0, 1.0 最相关"
    }
  ]
}
"""

def build_anchor_user_prompt(query: str, segments: list[dict], terminology_hint: str = "") -> str:
    # segments = [{"id": "seg-xxx", "doc_title": "...", "text": "..."}]
    seg_block = "\n\n".join([
        f"--- 片段 ID: {s['id']} | 文档: {s['doc_title']} ---\n{s['text']}"
        for s in segments
    ])
    hint_block = f"\n【用户术语偏好】\n{terminology_hint}\n" if terminology_hint else ""
    return f"""
【用户问题】
{query}
{hint_block}
【文档片段】（共 {len(segments)} 条）
{seg_block}

请严格按系统指令的 JSON 格式输出。
"""
```

#### 9.1.2 费曼偏差评估（后端 `FeynmanEvaluator.evaluate`）

```python
FEYNMAN_SYSTEM_PROMPT = """
你是一个费曼学习法评估助手。用户正在用自己的话解释一个概念，你需要：
1. 不评判用户的解释是否"正确"。
2. 只做"原文比对"：找出用户解释与【原文片段】的**偏差**（遗漏、误解、矛盾、过度延伸）。
3. 每个偏差必须能对应到一段原文作为依据。
4. 用通俗的中文描述偏差（不要说"你错了"，说"原文提到 X，你的表述缺少/改变了 Y"）。
5. 严格按 JSON 格式输出。

【输出 JSON 格式】
{
  "summary": "string, 整体评估，≤ 2 句话",
  "deviations": [
    {
      "user_segment": "string, 用户表述中有偏差的部分",
      "deviation_type": "OMISSION | CONTRADICTION | OVER_EXTENSION",
      "explanation": "string, 偏差的通俗解释",
      "original_snippet": "string, 原文对应片段",
      "anchor_segment_id": "string, 原文片段的 segment_id"
    }
  ],
  "gravity_lines": [
    {
      "from": "string, 用户偏差在解释文本中的位置索引（字符数）",
      "to_segment_id": "string, 对应原文片段"
    }
  ]
}
"""
```

#### 9.1.3 文档去重（后端 `DedupService.compute_delta`）

```python
DEDUP_SYSTEM_PROMPT = """
你是一个文档对比助手。给定一篇新文档的所有切片和用户已有的知识库历史切片摘要，
请识别新文档中**与历史知识重复**的切片段，并标记。
判定标准：表达的核心信息、事实或论证结构相同，即使措辞不同也算重复。
被标记的切片将在用户阅读时折叠。

【输出 JSON 格式】
{
  "folded_ranges": [
    {
      "segment_index_start": "int, 起始切片索引（含）",
      "segment_index_end": "int, 结束切片索引（含）",
      "reason": "string, 折叠原因，如'与《XXX》第 3 段重复讨论 XX 概念'"
    }
  ],
  "new_content_ratio": "float, 0.0~1.0, 新文档中信息增量的比例"
}
"""
```

#### 9.1.4 知识图谱关系发现（后端 `GraphBuilder`）

```python
GRAPH_RELATION_PROMPT = """
你是一个知识图谱构建助手。给定一组节点（文献/灵感/概念），识别它们之间的逻辑关系。
仅输出真实存在的关系，禁止臆测。

【关系类型】
- SUPPORT: A 支撑 B 的观点
- CHALLENGE: A 质疑 B
- SUPPLEMENT: A 补充 B 的细节
- CITE: A 引用 B

【输出 JSON 格式】
{
  "edges": [
    {
      "source_id": "string, 节点 ID",
      "target_id": "string, 节点 ID",
      "relation": "SUPPORT | CHALLENGE | SUPPLEMENT | CITE",
      "confidence": "float, 0.0~1.0"
    }
  ]
}
"""
```

#### 9.1.5 调用实现（OpenAI 兼容协议，Moonshot / DeepSeek 通用）
```python
from openai import OpenAI

class LLMProvider:
    def __init__(self, api_key: str, base_url: str, model: str = "moonshot-v1-8k"):
        self.client = OpenAI(api_key=api_key, base_url=base_url)
        self.model = model

    def chat_json(self, system: str, user: str, temperature: float = 0.3) -> dict:
        """统一返回 dict；失败抛 LLMError。"""
        resp = self.client.chat.completions.create(
            model=self.model,
            messages=[
                {"role": "system", "content": system},
                {"role": "user", "content": user}
            ],
            temperature=temperature,
            response_format={"type": "json_object"}  # Moonshot / DeepSeek 都支持
        )
        import json
        return json.loads(resp.choices[0].message.content)
```

**API Key 与 Model 配置**（`server/app/core/config.py`）：
```python
import os
class Settings:
    # Moonshot
    MOONSHOT_API_KEY = os.getenv("MOONSHOT_API_KEY", "")
    MOONSHOT_BASE_URL = "https://api.moonshot.cn/v1"
    MOONSHOT_MODEL = "moonshot-v1-8k"

    # DeepSeek（备用，OpenAI 兼容）
    DEEPSEEK_API_KEY = os.getenv("DEEPSEEK_API_KEY", "")
    DEEPSEEK_BASE_URL = "https://api.deepseek.com/v1"
    DEEPSEEK_MODEL = "deepseek-chat"
```

### 9.2 去重算法（V2.0 端云分工）
- **客户端**：上传前对每段计算 SimHash（64-bit），用 `simhash` Kotlin 移植实现（轻量，无外部依赖）。
- **后端**：接收分段后，pgvector 检索相似分段（余弦相似度阈值 0.9），合并连续重复段为折叠区间，写回 `is_folded=true`。
- **返回**：`DeltaReport { foldedRanges: List<Range>, newContentRatio: Float }`。

### 9.3 局部隐私保护检索（V2.0 改为 Room FTS）
私密文档检索完全在客户端：
- Room FTS4 全文索引已建立（见 §6.1）。
- 用户提问时 `AnchorRepository.searchLocalPrivate(query, limit=10)` 返回 `List<SegmentEntity>`，仅暴露 `id` + `snippet` 给上层，`text` 不展示（snippet 已是前 30 字摘要）。
- 提问响应中合并本地锚点与云端锚点。

### 9.4 知识图谱可视化（V2.0 改为 WebView + ECharts）

#### 9.4.1 完整 ECharts option（AI 可直接使用）
```javascript
// assets/graph.html 内的 option 配置
const option = {
    backgroundColor: '#1E1E2E',
    tooltip: {
        trigger: 'item',
        formatter: (params) => {
            if (params.dataType === 'node') {
                return `<b>${params.data.name}</b><br/>类型: ${params.data.nodeType}`;
            }
            return `${params.data.source} → ${params.data.target}<br/>${params.data.relation}`;
        }
    },
    legend: [{
        data: ['文献', '灵感', '概念'],
        textStyle: { color: '#FAF7F2' },
        top: 16
    }],
    series: [{
        type: 'graph',
        layout: 'force',
        roam: true,                        // 缩放/拖拽
        draggable: true,
        label: { show: true, color: '#FAF7F2', fontSize: 12, position: 'right' },
        labelLayout: { moveOverlap: 'shiftY' },
        emphasis: {                         // 选中态
            focus: 'adjacency',
            label: { fontSize: 14, fontWeight: 'bold' },
            lineStyle: { width: 3 }
        },
        force: { repulsion: 200, edgeLength: 80, gravity: 0.05 },
        // ⬇️ 三种节点类型完整定义（对应 UI&UX §4.5 视觉规范）
        categories: [
            { name: '文献', itemStyle: { color: '#5B8DEF' }, symbol: 'circle',     symbolSize: 30 },
            { name: '灵感', itemStyle: { color: '#F0C674' }, symbol: 'diamond',   symbolSize: 28 },
            { name: '概念', itemStyle: { color: '#002FA7' }, symbol: 'roundRect', symbolSize: 32 }
        ],
        // ⬇️ 四种关系类型完整定义
        edgeLabel: { show: false, fontSize: 10, color: '#FAF7F2', formatter: '{b}' },
        lineStyle: { color: '#6B6B6B', curveness: 0.1, width: 1.5, opacity: 0.7 },
        data: [],   // 由 window.updateGraph 填充
        links: []   // 由 window.updateGraph 填充
    }],
    animationDuration: 1000,
    animationEasingUpdate: 'quinticInOut'
};

// 节点 / 边 数据格式化（从后端 GraphDto 转 ECharts 格式）
function toEChartsData(graphDto) {
    return {
        nodes: graphDto.nodes.map(n => ({
            id: n.id,
            name: n.label,
            category: n.type === 'DOCUMENT' ? 0 : n.type === 'INSPIRATION' ? 1 : 2,
            nodeType: n.type,
            refId: n.refId,
            symbolSize: n.type === 'CONCEPT' ? 36 : 28,
            value: n.label
        })),
        links: graphDto.edges.map(e => ({
            id: `${e.source}-${e.target}`,
            source: e.source,
            target: e.target,
            relation: e.relation,
            lineStyle: {
                color: e.relation === 'SUPPORT' ? '#34A853' :
                       e.relation === 'CHALLENGE' ? '#EA4335' :
                       e.relation === 'SUPPLEMENT' ? '#002FA7' : '#888',
                type: e.relation === 'SUPPORT' ? 'dashed' :
                      e.relation === 'CHALLENGE' ? 'dashed' :
                      e.relation === 'CITE' ? 'dotted' : 'solid',
                width: (e.confidence * 3) + 0.5,  // 0.5 ~ 3.5 粗细
                opacity: 0.5 + e.confidence * 0.5  // 透明度随置信度
            },
            symbol: ['none', 'arrow'],           // 终点箭头
            symbolSize: 6
        }))
    };
}
```

#### 9.4.2 客户端 → WebView 通信
```kotlin
// GraphWebViewBridge.kt
class GraphJsBridge(private val onNodeClick: (String) -> Unit) {
    @JavascriptInterface
    fun onNodeClick(nodeId: String) {
        // 切回主线程
        Handler(Looper.getMainLooper()).post { onNodeClick(nodeId) }
    }
}

// GraphScreen.kt —— 推数据给 WebView
@Composable
fun GraphScreen(viewModel: GraphViewModel = ...) {
    val graphData by viewModel.graphData.collectAsStateWithLifecycle()
    var webView by remember { mutableStateOf<WebView?>(null) }

    AndroidView(
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.allowFileAccess = true
                settings.blockNetworkLoads = true   // ⬅️ 阻断外网（修复 🔴 矛盾 #1）
                addJavascriptInterface(
                    GraphJsBridge { nodeId -> viewModel.onGraphNodeClicked(nodeId) },
                    "BlueLinkBridge"
                )
                loadUrl("file:///android_asset/graph.html")
                webView = this
            }
        },
        update = { wv ->
            val json = Gson().toJson(graphData)  // GraphDto → JSON 字符串
            wv.evaluateJavascript("window.updateGraph($json)", null)
        }
    )
}
```

#### 9.4.3 双向交互完整流程
1. **Compose → WebView**：图谱数据变化时，`update` 回调内调 `wv.evaluateJavascript("window.updateGraph($json)", null)`。
2. **WebView → Compose**：用户在图谱上点击节点 → `chart.on('click', ...)` 触发 → `window.BlueLinkBridge.onNodeClick(id)` → Kotlin `onNodeClick` 回调 → ViewModel 导航。
3. **布局切换**：底部按钮调 `wv.evaluateJavascript("window.changeLayout('circular')", null)`。

### 9.5 加密策略（V2.0 简化为 EncryptedSharedPreferences）
```kotlin
// SecurePrefs.kt —— 封装 androidx.security:security-crypto
class SecurePrefs(context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "bluelink_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    // 通用 KV（用于 WebDAV 凭据、API token、用户密码派生密钥等）
    fun putSecret(key: String, value: String) = prefs.edit().putString(key, value).apply()
    fun getSecret(key: String): String? = prefs.getString(key, null)
    fun removeSecret(key: String) = prefs.edit().remove(key).apply()
}

// V2.0 MVP：Room 中的 text/content/payloadJson 字段全部为明文 String，依赖：
//   1. 设备锁屏密码
//   2. allowBackup="false"（禁止云端备份）
//   3. 私密文档的原文绝对不上传（仅上传脱敏片段 ID + 关键词）
// SecurePrefs 仅用于存储关键凭据（API Token、WebDAV 密码），不用于文档原文。
// V2.1 升级方案：把 text 字段从 Room 移到 SecurePrefs.putSecret(segId, plaintext) 间接存储。
```

### 9.6 后端 Fernet 加密实现（V2.0 完整代码）

`text_encrypted BLOB` 字段对应后端用 **Fernet** 对称加密（AES-128-CBC + HMAC-SHA256），用于 `LOCAL_FIRST` 文档后端落盘前的处理。

```python
# app/core/crypto.py
from cryptography.fernet import Fernet
import os

class Cipher:
    """单例 Fernet 加密器。生产环境密钥应从 KMS / 环境变量注入。"""
    def __init__(self):
        key = os.getenv("BLUELINK_FERNET_KEY")
        if not key:
            # MVP：首次启动自动生成并持久化到 .fernet_key（仅开发用）
            key_file = "./.fernet_key"
            if os.path.exists(key_file):
                with open(key_file, "rb") as f:
                    key = f.read()
            else:
                key = Fernet.generate_key()
                with open(key_file, "wb") as f:
                    f.write(key)
        self.fernet = Fernet(key)

    def encrypt(self, plaintext: str) -> bytes:
        return self.fernet.encrypt(plaintext.encode("utf-8"))

    def decrypt(self, ciphertext: bytes) -> str:
        return self.fernet.decrypt(ciphertext).decode("utf-8")

cipher = Cipher()  # 全局单例
```

### 9.7 后端核心 Service 伪代码骨架（AI 可直接补全）

```python
# app/services/document_service.py
class DocumentService:
    def parse_and_chunk(self, file_path: str, privacy_level: str) -> list[dict]:
        """PDF/Word/Markdown → 切片列表 [{"index", "text"}]"""
        ext = file_path.rsplit(".", 1)[-1].lower()
        if ext == "pdf":
            text = self._extract_pdf(file_path)
        elif ext in ("docx", "doc"):
            text = self._extract_docx(file_path)
        else:
            text = open(file_path, encoding="utf-8").read()
        return self._chunk_by_paragraph(text)

    def _extract_pdf(self, path: str) -> str:
        import fitz  # PyMuPDF
        doc = fitz.open(path)
        return "\n\n".join(page.get_text() for page in doc)

    def _chunk_by_paragraph(self, text: str, max_len: int = 500) -> list[dict]:
        paragraphs = [p.strip() for p in text.split("\n\n") if p.strip()]
        chunks, buf, idx = [], "", 0
        for p in paragraphs:
            if len(buf) + len(p) > max_len and buf:
                chunks.append({"index": idx, "text": buf})
                idx += 1
                buf = ""
            buf += p + "\n"
        if buf: chunks.append({"index": idx, "text": buf})
        return chunks

# app/services/dedup_service.py
class DedupService:
    def compute_delta(self, new_doc_id: str) -> dict:
        """返回 {folded_ranges, new_content_ratio}"""
        from app.vectors.store import vector_store
        new_segs = SegmentRepo.list_by_doc(new_doc_id)
        new_vectors = vector_store.load_doc(new_doc_id)
        history = vector_store.load_all_except(new_doc_id)  # np.ndarray
        from sklearn.metrics.pairwise import cosine_similarity
        sim_matrix = cosine_similarity(new_vectors, history)  # [N, M]
        folded = []
        for i, row in enumerate(sim_matrix):
            max_sim = row.max()
            if max_sim > 0.9:
                folded.append({"index_start": i, "index_end": i, "reason": f"与历史切片 {row.argmax()} 重复"})
        return {"folded_ranges": folded, "new_content_ratio": 1.0 - len(folded) / max(len(new_segs), 1)}

# app/services/anchor_engine.py
class AnchorEngine:
    def __init__(self):
        from app.core.config import Settings
        self.llm = LLMProvider(api_key=Settings.MOONSHOT_API_KEY, base_url=Settings.MOONSHOT_BASE_URL, model=Settings.MOONSHOT_MODEL)

    def generate_anchors(self, query: str, segments: list[dict], granularity: str, terminology_hint: str = "") -> dict:
        # 1. 检索：粗筛 top-20（按向量相似度）
        relevant = self._retrieve_topk(query, segments, k=20)
        # 2. 调 LLM 生成锚点
        user_prompt = build_anchor_user_prompt(query, relevant, terminology_hint)
        return self.llm.chat_json(ANCHOR_SYSTEM_PROMPT, user_prompt, temperature=0.3)

    def _retrieve_topk(self, query, segments, k):
        from app.vectors.store import vector_store
        from openai import OpenAI
        from app.core.config import Settings
        embed = OpenAI(api_key=Settings.MOONSHOT_API_KEY, base_url=Settings.MOONSHOT_BASE_URL)
        q_vec = np.array(embed.embeddings.create(model="embedding-v1", input=[query]).data[0].embedding)
        seg_vecs = np.array([vector_store.get_segment_embedding(s["id"]) for s in segments])
        sims = cosine_similarity([q_vec], seg_vecs)[0]
        topk = np.argsort(sims)[::-1][:k]
        return [segments[i] for i in topk]

# app/services/feynman_evaluator.py
class FeynmanEvaluator:
    def __init__(self):
        from app.core.config import Settings
        self.llm = LLMProvider(api_key=Settings.MOONSHOT_API_KEY, base_url=Settings.MOONSHOT_BASE_URL, model=Settings.MOONSHOT_MODEL)

    def evaluate(self, user_text: str, target_concept: str, context_segments: list[dict]) -> dict:
        context_block = "\n\n".join([f"--- 片段 {s['id']} ---\n{s['text']}" for s in context_segments])
        user_prompt = f"【用户解释的概念】{target_concept}\n\n【用户解释】\n{user_text}\n\n【原文片段】\n{context_block}\n\n请按系统指令的 JSON 格式输出。"
        return self.llm.chat_json(FEYNMAN_SYSTEM_PROMPT, user_prompt, temperature=0.2)

# app/services/graph_builder.py
class GraphBuilder:
    def __init__(self):
        from app.core.config import Settings
        self.llm = LLMProvider(api_key=Settings.MOONSHOT_API_KEY, base_url=Settings.MOONSHOT_BASE_URL, model=Settings.MOONSHOT_MODEL)

    def build_graph(self, user_id: str) -> dict:
        # 1. 收集所有节点（文档/灵感/概念）
        nodes = GraphNodeRepo.list_all()
        # 2. 调用 LLM 发现关系
        nodes_text = "\n".join([f"- {n.id}: {n.label} ({n.type})" for n in nodes])
        user_prompt = f"【节点列表】\n{nodes_text}\n\n请识别它们之间的关系。"
        result = self.llm.chat_json(GRAPH_RELATION_PROMPT, user_prompt, temperature=0.2)
        # 3. 持久化新边
        for edge in result.get("edges", []):
            GraphEdgeRepo.upsert(edge)
        return {"nodes": [n.to_dict() for n in nodes], "edges": GraphEdgeRepo.list_all()}
```

> **加密策略 V2.0 选择说明**：Tink 的 `Aead` + `KeysetHandle` 链对 AI 不友好，配置 5 行的加密代码 AI 容易把 `AAD`（关联认证数据）写错导致解密失败。`EncryptedSharedPreferences` 是 AndroidX 官方封装的"开箱即用"方案，**主密钥由 Android Keystore 自动保护**，AI 写代码零失误。MVP 阶段文档原文存明文到 Room，依赖设备锁屏密码；后续升级为 SecurePrefs 间接存储。

## 10. 隐私与安全设计（V2.0 精简版）
### 10.1 数据保护措施
- **传输加密**：全链路 HTTPS / TLS 1.3。
- **本地存储加密**：
    - **关键凭据**（API Token、WebDAV 密码等）走 `EncryptedSharedPreferences`（主密钥由 Android Keystore 保护）。
    - **文档 / 灵感原文**：MVP 阶段存 Room 明文，依赖设备锁屏密码与 `allowBackup=false` 保护；后续可升级为 SecurePrefs 间接存储（见 §9.5）。
- **API 鉴权**：Bearer Token；HTTPS Pinning（OkHttp `CertificatePinner`）V2.1 再加。
- **脱敏上传**：灵感卡片上传前，`PrivacyManager.sanitize(text)` 替换人名、电话、URL、邮箱为占位符。
- **WebView 隔离**：ECharts 加载的本地 HTML 走 `file:///android_asset/graph.html`，不访问外网，`<network-security-config>` 中 `domain-config cleartextTrafficPermitted=false`。
- **私密文档原文绝不离开设备**（沿用 V1.0 原则）。

### 10.2 Android 特定安全配置
- **AndroidManifest 权限最小化**：仅申请 `INTERNET`、`RECORD_AUDIO`、`CAMERA`、`POST_NOTIFICATIONS`（Android 13+）。
- **网络配置**：`network_security_config.xml` 禁止明文流量，配置 TLS 1.3。
- **备份策略**：`android:allowBackup="false"` + `data_extraction_rules.xml` 禁止云端备份。
- **调试构建**：仅 debug 包可调试，release 包 `isDebuggable=false`。
- **ProGuard / R8**：release 启用代码混淆与资源压缩。
- **Play Integrity API**：后续接入，校验设备是否被 root / 异常环境。

### 10.3 用户数据控制
- 提供"数据导出"功能，将所有本地数据以**明文 JSON 形式**导出（需用户二次确认）。
- 提供"永久删除"选项：清空 Room 数据库、DataStore、EncryptedSharedPreferences，重启后所有本地痕迹消失。
- 隐私设置页提供"立即清空云端数据"按钮，调用后端 `DELETE /api/v1/users/me`。

## 11. 异常处理与降级策略（V2.0 增加 Android 场景）

### 11.1 场景速查表
| 场景 | 客户端处理 |
|------|------------|
| **大模型 API 超时/不可用** | 后端重试 3 次后返回 `503`；客户端展示本地 FTS 命中的简化锚点（无 AI 引言），Toast 提示"AI 暂时无法响应" |
| **无网络** | 拦截器捕获 `UnknownHostException`，返回 `NetworkUnavailable`；Repository 走本地缓存；写入操作进 `pending_sync` |
| **文档解析失败**（后端） | 返回 `400 { error_code: PARSE_FAILED, reason: "PDF 已加密" }`，UI 展示具体错误并允许重试 |
| **OCR 识别失败**（ML Kit） | `MlKitOcrDataSource` 返回空文本，UI 提示"未识别到文字，请手动输入或重拍" |
| **SpeechRecognizer 不可用** | 检测设备是否支持 STT，不支持则隐藏语音 Tab |
| **权限被拒** | CAMERA / RECORD_AUDIO 拒绝后展示 SnackBar 引导至系统设置 |
| **OOM** | `OutOfMemoryError` 兜底，捕获后释放图片缓存，提示用户关闭其他应用 |
| **数据库损坏** | 启动时检测 Room 完整性，失败则提示"是否恢复出厂设置" |
| **WebView 加载失败**（图谱页） | 降级为静态占位图，提示"网络异常，图谱暂不可用" |

### 11.2 客户端错误处理模板（V2.0 完整，AI 可直接抄）

#### 11.2.1 全局 CoroutineExceptionHandler
```kotlin
// util/GlobalErrorHandler.kt
val appExceptionHandler = CoroutineExceptionHandler { _, throwable ->
    when (throwable) {
        is UnknownHostException, is ConnectException, is SocketTimeoutException ->
            // 网络问题：交给 Repository 的 Stale-While-Revalidate 自动处理，这里只记录
            Log.w("BlueLink", "Network error: ${throwable.message}")
        is HttpException -> {
            val code = throwable.code()
            when (code) {
                401 -> AppEventBus.emit(AppEvent.NeedReLogin)
                503 -> AppEventBus.emit(AppEvent.AIServiceUnavailable)
                in 500..599 -> AppEventBus.emit(AppEvent.ServerError(code))
                else -> AppEventBus.emit(AppEvent.RequestFailed(throwable.message ?: "未知错误"))
            }
        }
        is JsonDataException, is JsonEncodingException ->
            AppEventBus.emit(AppEvent.DataParseError)
        is SQLiteException ->
            AppEventBus.emit(AppEvent.DatabaseError(throwable.message ?: ""))
        else ->
            AppEventBus.emit(AppEvent.UnknownError(throwable.message ?: "未知异常"))
    }
}
```

#### 11.2.2 AppEventBus（极简事件总线，替代 EventBus/LiveData 跨页通知）
```kotlin
// util/AppEventBus.kt —— 单例 SharedFlow
sealed class AppEvent {
    data object NeedReLogin : AppEvent()
    data object AIServiceUnavailable : AppEvent()
    data class ServerError(val code: Int) : AppEvent()
    data class RequestFailed(val message: String) : AppEvent()
    data object DataParseError : AppEvent()
    data class DatabaseError(val detail: String) : AppEvent()
    data class UnknownError(val message: String) : AppEvent()
}

object AppEventBus {
    private val _events = MutableSharedFlow<AppEvent>(extraBufferCapacity = 16)
    val events: SharedFlow<AppEvent> = _events.asSharedFlow()
    fun emit(event: AppEvent) { _events.tryEmit(event) }
}

// 在 AppContainer / 顶层 Activity 订阅
@Composable
fun GlobalEventCollector() {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        AppEventBus.events.collect { event ->
            val msg = when (event) {
                AppEvent.AIServiceUnavailable -> "AI 暂时无法响应，已为你展示本地匹配结果"
                is AppEvent.ServerError -> "服务异常（${event.code}），请稍后重试"
                is AppEvent.RequestFailed -> event.message
                AppEvent.DataParseError -> "数据格式异常"
                is AppEvent.DatabaseError -> "本地数据异常：${event.detail}"
                is AppEvent.UnknownError -> "未知错误：${event.message}"
                AppEvent.NeedReLogin -> "登录已过期，请重新登录"
            }
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }
}
```

#### 11.2.3 ViewModel 错误封装模式
```kotlin
// ui/common/UiState.kt
sealed interface UiState<out T> {
    data object Idle : UiState<Nothing>
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val error: AppError) : UiState<Nothing>
}

sealed class AppError(val message: String) {
    data object NetworkUnavailable : AppError("网络不可用")
    data class AIServiceError(val detail: String) : AppError("AI 响应失败：$detail")
    data class ValidationError(val field: String) : AppError("$field 输入有误")
    data class Unknown(val detail: String) : AppError(detail)
}

// ViewModel 写法模板
class ChatViewModel(private val anchorRepo: AnchorRepository) : ViewModel() {
    private val _state = MutableStateFlow<UiState<AskResponse>>(UiState.Idle)
    val state: StateFlow<UiState<AskResponse>> = _state.asStateFlow()

    fun ask(query: String) {
        _state.value = UiState.Loading
        viewModelScope.launch(appExceptionHandler) {
            anchorRepo.askQuestion(query)
                .onSuccess { _state.value = UiState.Success(it) }
                .onFailure { throwable ->
                    _state.value = UiState.Error(
                        when (throwable) {
                            is UnknownHostException -> AppError.NetworkUnavailable
                            is HttpException -> AppError.AIServiceError(throwable.message())
                            else -> AppError.Unknown(throwable.message ?: "")
                        }
                    )
                }
        }
    }
}

// Screen 写法模板
@Composable
fun ChatScreen(viewModel: ChatViewModel = viewModel(...)) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    when (val s = state) {
        UiState.Idle -> IdleHint()
        UiState.Loading -> LoadingIndicator()
        is UiState.Success -> AnswerView(s.data)
        is UiState.Error -> ErrorView(s.error, onRetry = viewModel::ask)
    }
}
```

#### 11.2.4 Repository try-catch 标准模式
```kotlin
// AnchorRepository.kt
suspend fun askQuestion(query: String): Result<AskResponse> = runCatching {
    val response = api.askQuestion(AskRequest(query = query, granularity = "SENTENCE"))
    if (response.isSuccessful) {
        response.body() ?: throw IllegalStateException("Empty response body")
    } else {
        throw HttpException(response)
    }
}.onFailure { e ->
    Log.w("AnchorRepository", "askQuestion failed", e)
    AppEventBus.emit(AppEvent.RequestFailed(e.message ?: "请求失败"))
}
```

#### 11.2.5 启动期数据库完整性检查
```kotlin
// BlueLinkApp.kt
override fun onCreate() {
    super.onCreate()
    container = AppContainer(this)
    // 启动时轻量检查 Room
    ProcessLifecycleOwner.get().lifecycleScope.launch(appExceptionHandler) {
        try {
            container.database.runInTransaction<Unit> { /* SELECT 1 */ }
        } catch (e: SQLiteException) {
            AppEventBus.emit(AppEvent.DatabaseError("启动时检测到数据库异常，是否恢复？"))
            // 用户确认后调用 container.database.openHelper.writableDatabase 修复或重置
        }
    }
}
```

### 11.3 后端错误处理
```python
# app/core/exceptions.py
from fastapi import HTTPException
class LLMTimeout(HTTPException):
    def __init__(self): super().__init__(status_code=503, detail="LLM 服务超时")
class ParseError(HTTPException):
    def __init__(self, reason: str): super().__init__(status_code=400, detail={"code": "PARSE_FAILED", "reason": reason})

# app/main.py —— 全局异常拦截
@app.exception_handler(Exception)
async def global_exception_handler(request: Request, exc: Exception):
    if isinstance(exc, openai.APITimeoutError):
        return JSONResponse(status_code=503, content={"error_code": "LLM_TIMEOUT", "message": "AI 服务超时"})
    if isinstance(exc, openai.APIError):
        return JSONResponse(status_code=502, content={"error_code": "LLM_ERROR", "message": str(exc)})
    Log.error("Unhandled", exc_info=exc)
    return JSONResponse(status_code=500, content={"error_code": "INTERNAL", "message": "服务异常"})
```

## 12. 非功能特性实现方案（V2.0 精简）

### 12.1 性能（V2.0 完整实现策略，对应 PRD §8.1 全部 8 项）

| PRD 指标 | V2.0 实现策略 |
|---------|--------------|
| **冷启动 < 2.0s**（中端机） | 1. `AppContainer` 在 `BlueLinkApp.onCreate` 中初始化（**所有 Repository by lazy**，首屏不触发任何重计算）。<br/>2. Android 12+ Splash Screen API 自动展示品牌闪屏。<br/>3. `MainActivity` 启动后**只**渲染首页骨架，数据用 `LaunchedEffect(Unit) { viewModel.loadInitial() }` 异步加载。<br/>4. `fallbackToDestructiveMigration()` 避免冷启动时跑 Room migration 检查。<br/>5. ProGuard R8 启用，移除未引用类，缩短类加载。 |
| **锚点跳转 < 500ms** | 1. `AnchorEntity` 与 `SegmentEntity` 用 `docId` 索引，Room 查询走 `WHERE id = ?` 命中主键（O(logN)）。<br/>2. ReaderViewModel **预加载**当前文档的所有 `SegmentEntity.text` 到内存（典型文档 100-200 切片，< 1MB）。<br/>3. `bringIntoViewRequester.bringIntoView()` 异步滚动，**不阻塞主线程**。<br/>4. 聚光灯 alpha 用 `animateFloatAsState(tween(250))`，**动画过程不阻塞点击**。 |
| **文库滚动 60fps**（1000 卡片） | 1. `LazyVerticalStaggeredGrid` 虚拟化，只渲染屏幕内 + 少量缓冲。<br/>2. `items(docs, key = { it.id })` 显式 key 避免重组时重新计算 diff。<br/>3. `DocumentCard` 内部 `Text` 用 `maxLines` + `overflow = TextOverflow.Ellipsis`，避免长文本测量阻塞。<br/>4. 封面图用 Coil `AsyncImage` 默认开启磁盘 + 内存缓存。<br/>5. 私密角标用 `if` 条件渲染，不在 Card 内部嵌套 Box。<br/>6. **测量命令**：`adb shell dumpsys gfxinfo com.yjtzc.bluelink framestats` 看 95th percentile frame time。 |
| **图谱 500 节点 60fps** | 1. ECharts 配置 `large: true, progressive: 1000`，超 500 节点分批渲染。<br/>2. 后端 `GET /graph` 默认 `limit=500`，客户端按需分页（`nextCursor`）。<br/>3. WebView 单独进程隔离（可选）：`android:process=":graph"`，避免主进程 GC 影响图谱渲染。<br/>4. 节点 `symbolSize` 控制在 28-36px，避免过大导致 layout 重算慢。 |
| **本地去重 1MB 文档 < 3s** | 1. 客户端上传前 SimHash 64-bit 预计算，**仅发送 hash 数组**（KB 级）。<br/>2. 后端 `compute_delta` 在内存中完成（SQLite 1 万切片 < 1s）。<br/>3. 异步返回 `task_id`，客户端 UI 立即显示"分析中"骨架屏，完成后刷新。 |
| **内存 < 200MB**（前台空闲） | 1. Coil 配置 `imageLoader.memoryCache { maxSizePercent(0.15) }`（图片缓存 ≤ 30MB）。<br/>2. 文档 / 灵感卡片每页只渲染 20 张，**滚出屏幕立即释放**。<br/>3. `WebView` 单独进程（可选）→ 图谱页销毁时整个进程 kill。<br/>4. LeakCanary 在 debug 包接入，发现泄漏立即报警。<br/>5. 避免在 ViewModel 中持有 `Context` / `View` 引用。 |
| **APK < 30MB** | 1. R8 全模式 + 资源压缩（`isShrinkResources = true`）。<br/>2. 仅打包 `text-recognition`（拉丁），中文识别通过 **Dynamic Feature** 按需下载（V2.1 优化）。<br/>3. ECharts 走 `echarts.min.js`（约 1MB gzip 400KB），不引 ECharts GL 等大模块。<br/>4. Coil / OkHttp 等第三方库**仅打包 release 需要的 ABI**（`splits { abi { enable true } }`）。 |
| **后台耗电 < 5%/h** | 1. MVP 不接 WorkManager，**没有真正后台任务**（所有同步走用户主动触发）。<br/>2. 离线 `pending_sync` 队列仅在 App 切回前台时同步（`ProcessLifecycleOwner.Lifecycle.Event.ON_START` 监听）。<br/>3. WebSocket / 推送**完全不用**，无长连接耗电。<br/>4. ML Kit 模型**不预下载**，首次使用按需下载。 |

**测量工具命令**（AI 写完代码后用这些命令验证）：

```bash
# 1. 冷启动
adb shell am start -W com.yjtzc.bluelink/.MainActivity
# 关注 WaitTime / ThisTime / TotalTime

# 2. 内存
adb shell dumpsys meminfo com.yjtzc.bluelink
# 关注 TOTAL PSS

# 3. 帧率
adb shell dumpsys gfxinfo com.yjtzc.bluelink framestats
# 关注 janky frames < 5%

# 4. APK 体积
./gradlew :app:bundleRelease
ls -lh app/build/outputs/bundle/release/*.aab

# 5. 后台耗电（需 1 小时 + Battery Historian）
adb shell dumpsys batterystats --reset
# ... 1 小时后
adb shell dumpsys batterystats > battery.txt
# 浏览器打开 https://bathist.ef.lc/ 分析
```

### 12.2 兼容性
| 维度 | 范围 |
|------|------|
| minSdk | 24（Android 7.0） |
| targetSdk | 36（Android 16） |
| compileSdk | 36 |
| 架构 | arm64-v8a 必须；armeabi-v7a / x86_64 可选 |
| 厂商适配 | 小米、华为、OPPO、vivo、三星的后台限制与权限弹窗 |
| 字体 | 跟随系统（AppCompat TextView 自动；Compose 用 `FontFamily.Default`） |
| 暗黑模式 | 跟随系统；Material3 `isSystemInDarkTheme()` 动态切换 |
| 横屏 | 暂不强制；阅读器支持横屏（基础适配，瀑布流改双栏） |

### 12.3 可扩展性
- **插件化 AI Provider**：抽象 `LLMProvider` 接口，未来可接入 ChatGLM、文心一言等，运行时通过 Hilt 切换实现。
- **插件化文档解析**：动态功能模块（`Dynamic Feature`）按需下载 PDF / Word 解析器（如未来需要客户端解析）。
- **跨平台预留**：
    - `domain/` 与 `data/repository` 层保持**纯 Kotlin / 零 Android 依赖**，未来 KMP 共享到 iOS。
    - UI 层（Compose）未来 Compose Multiplatform 迁移 iOS / 桌面。
    - V2.0 **不实现 KMP 模块化**，但代码组织上严格分层。

## 13. 附录
### 13.1 依赖库清单

#### 客户端（`gradle/libs.versions.toml`，V2.0 精简版）
```toml
[versions]
agp = "8.7.0"
kotlin = "2.0.21"
compose-bom = "2026.06.00"
retrofit = "2.11.0"
okhttp = "4.12.0"
moshi = "1.15.1"
room = "2.7.0-alpha10"
datastore = "1.1.1"
security-crypto = "1.1.0-alpha06"
coil = "3.0.4"
navigation = "2.8.5"
coroutines = "1.9.0"
mlkit-text = "16.0.1"
junit = "4.13.2"
mockk = "1.13.13"
turbine = "1.2.0"
espresso = "3.6.1"

[libraries]
# Core
androidx-core-ktx = { module = "androidx.core:core-ktx" }
androidx-activity-compose = { module = "androidx.activity:activity-compose" }
androidx-lifecycle-runtime-ktx = { module = "androidx.lifecycle:lifecycle-runtime-ktx" }
androidx-lifecycle-viewmodel-compose = { module = "androidx.lifecycle:lifecycle-viewmodel-compose" }
androidx-navigation-compose = { module = "androidx.navigation:navigation-compose", version.ref = "navigation" }

# Compose BOM
androidx-compose-bom = { module = "androidx.compose:compose-bom", version.ref = "compose-bom" }
androidx-compose-ui = { module = "androidx.compose.ui:ui" }
androidx-compose-ui-graphics = { module = "androidx.compose.ui:ui-graphics" }
androidx-compose-ui-tooling = { module = "androidx.compose.ui:ui-tooling" }
androidx-compose-ui-tooling-preview = { module = "androidx.compose.ui:ui-tooling-preview" }
androidx-compose-material3 = { module = "androidx.compose.material3:material3" }
androidx-compose-material3-adaptive = { module = "androidx.compose.material3.adaptive:adaptive" }

# Network (Moshi 而非 kotlinx.serialization)
retrofit = { module = "com.squareup.retrofit2:retrofit", version.ref = "retrofit" }
retrofit-converter-moshi = { module = "com.squareup.retrofit2:converter-moshi", version.ref = "retrofit" }
moshi = { module = "com.squareup.moshi:moshi", version.ref = "moshi" }
moshi-kotlin = { module = "com.squareup.moshi:moshi-kotlin", version.ref = "moshi" }
moshi-codegen = { module = "com.squareup.moshi:moshi-kotlin-codegen", version.ref = "moshi" }
okhttp = { module = "com.squareup.okhttp3:okhttp", version.ref = "okhttp" }
okhttp-logging = { module = "com.squareup.okhttp3:logging-interceptor", version.ref = "okhttp" }

# Persistence
room-runtime = { module = "androidx.room:room-runtime", version.ref = "room" }
room-ktx = { module = "androidx.room:room-ktx", version.ref = "room" }
room-compiler = { module = "androidx.room:room-compiler", version.ref = "room" }
datastore-preferences = { module = "androidx.datastore:datastore-preferences", version.ref = "datastore" }

# Crypto (EncryptedSharedPreferences)
androidx-security-crypto = { module = "androidx.security:security-crypto", version.ref = "security-crypto" }

# Image
coil-compose = { module = "io.coil-kt.coil3:coil-compose", version.ref = "coil" }
coil-network = { module = "io.coil-kt.coil3:coil-network-okhttp", version.ref = "coil" }

# Async
kotlinx-coroutines-android = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-android", version.ref = "coroutines" }

# ML Kit
mlkit-text-recognition = { module = "com.google.mlkit:text-recognition", version.ref = "mlkit-text" }
mlkit-text-recognition-chinese = { module = "com.google.mlkit:text-recognition-chinese", version.ref = "mlkit-text" }

# Test
junit = { module = "junit:junit", version.ref = "junit" }
mockk = { module = "io.mockk:mockk", version.ref = "mockk" }
turbine = { module = "app.cash.turbine:turbine", version.ref = "turbine" }
androidx-test-junit = { module = "androidx.test.ext:junit", version = "1.2.1" }
espresso-core = { module = "androidx.test.espresso:espresso-core", version.ref = "espresso" }
androidx-compose-ui-test-junit4 = { module = "androidx.compose.ui:ui-test-junit4" }
androidx-compose-ui-test-manifest = { module = "androidx.compose.ui:ui-test-manifest" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
kotlin-kapt = { id = "org.jetbrains.kotlin.kapt", version.ref = "kotlin" }   # Moshi codegen + Room 用 kapt
```

> **V2.0 砍掉的依赖**：Hilt 整套（hilt-android / hilt-compiler / hilt-navigation-compose / hilt-work）、Tink、WorkManager、kotlinx-serialization 插件、KSP 插件。所有注解处理走 **kapt**（AI 写 Room @Dao @Query、Moshi @JsonClass 零失误）。

#### 后端（`requirements.txt`，V2.0 精简版）
```
fastapi
uvicorn[standard]
openai
python-multipart
PyMuPDF
python-docx
SQLAlchemy    # 同步 ORM，V2.0 不引 async / pgvector
numpy         # 向量存储与余弦相似度
scikit-learn  # cosine_similarity
cryptography  # Fernet 对称加密
```

### 13.2 项目目录结构

#### 客户端
```
BlueLink/
├── app/
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── assets/
│       │   │   └── graph.html            # ECharts 入口
│       │   ├── java/com/yjtzc/bluelink/
│       │   │   ├── BlueLinkApp.kt        # Application 子类，实例化 AppContainer
│       │   │   ├── MainActivity.kt       # AppCompatActivity，注入 AppContainer 到 ViewModel 工厂
│       │   │   ├── data/
│       │   │   │   ├── remote/
│       │   │   │   │   ├── BlueLinkApi.kt
│       │   │   │   │   ├── NetworkResult.kt
│       │   │   │   │   ├── AuthInterceptor.kt
│       │   │   │   │   └── dto/
│       │   │   │   ├── local/
│       │   │   │   │   ├── db/
│       │   │   │   │   │   ├── AppDatabase.kt
│       │   │   │   │   │   ├── entity/
│       │   │   │   │   │   │   ├── DocumentEntity.kt
│       │   │   │   │   │   │   ├── SegmentEntity.kt
│       │   │   │   │   │   │   ├── InspirationCardEntity.kt
│       │   │   │   │   │   │   ├── AnchorEntity.kt
│       │   │   │   │   │   │   ├── GraphNodeEntity.kt
│       │   │   │   │   │   │   ├── GraphEdgeEntity.kt
│       │   │   │   │   │   │   └── PendingSyncEntity.kt
│       │   │   │   │   │   └── dao/
│       │   │   │   │   │       ├── DocumentDao.kt
│       │   │   │   │   │       ├── SegmentDao.kt
│       │   │   │   │   │       ├── InspirationDao.kt
│       │   │   │   │   │       ├── AnchorDao.kt
│       │   │   │   │   │       ├── GraphDao.kt
│       │   │   │   │   │       └── PendingSyncDao.kt
│       │   │   │   │   ├── prefs/UserPreferences.kt
│       │   │   │   │   ├── crypto/SecurePrefs.kt
│       │   │   │   │   ├── ocr/MlKitOcrDataSource.kt
│       │   │   │   │   └── stt/SpeechRecognizerDataSource.kt
│       │   │   │   └── repository/
│       │   │   │       ├── DocumentRepository.kt
│       │   │   │       ├── AnchorRepository.kt
│       │   │   │       ├── GraphRepository.kt
│       │   │   │       ├── CaptureRepository.kt
│       │   │   │       └── FeynmanRepository.kt
│       │   │   ├── domain/
│       │   │   │   ├── model/
│       │   │   │   │   ├── Document.kt
│       │   │   │   │   ├── Segment.kt
│       │   │   │   │   ├── Anchor.kt
│       │   │   │   │   ├── InspirationCard.kt
│       │   │   │   │   ├── GraphNode.kt
│       │   │   │   │   ├── GraphEdge.kt
│       │   │   │   │   └── Deviation.kt
│       │   │   │   └── usecase/
│       │   │   │       ├── AskQuestionUseCase.kt
│       │   │   │       ├── ComputeDeltaUseCase.kt
│       │   │   │       ├── FeynmanEvaluateUseCase.kt
│       │   │   │       ├── BuildGraphUseCase.kt
│       │   │   │       └── CaptureInspirationUseCase.kt
│       │   │   ├── ui/
│       │   │   │   ├── theme/
│       │   │   │   │   ├── Color.kt
│       │   │   │   │   ├── Theme.kt
│       │   │   │   │   └── Type.kt
│       │   │   │   ├── component/
│       │   │   │   │   ├── AnchorCard.kt
│       │   │   │   │   ├── SpotlightOverlay.kt
│       │   │   │   │   ├── FoldBar.kt
│       │   │   │   │   ├── GravityLine.kt
│       │   │   │   │   ├── WaterfallGrid.kt
│       │   │   │   │   └── CaptureSheet.kt
│       │   │   │   ├── home/
│       │   │   │   │   ├── HomeScreen.kt
│       │   │   │   │   └── HomeViewModel.kt
│       │   │   │   ├── chat/
│       │   │   │   │   ├── ChatScreen.kt
│       │   │   │   │   └── ChatViewModel.kt
│       │   │   │   ├── reader/
│       │   │   │   │   ├── ReaderScreen.kt
│       │   │   │   │   └── ReaderViewModel.kt
│       │   │   │   ├── graph/
│       │   │   │   │   ├── GraphScreen.kt
│       │   │   │   │   ├── GraphViewModel.kt
│       │   │   │   │   └── GraphWebViewBridge.kt
│       │   │   │   ├── capture/
│       │   │   │   │   ├── CaptureScreen.kt
│       │   │   │   │   └── CaptureViewModel.kt
│       │   │   │   ├── mine/
│       │   │   │   │   ├── MineScreen.kt
│       │   │   │   │   └── MineViewModel.kt
│       │   │   │   └── navigation/BlueLinkNavGraph.kt
│       │   │   ├── AppContainer.kt        # 手写 DI 容器
│       │   │   ├── BlueLinkViewModelFactory.kt
│       │   │   ├── BlueLinkApp.kt        # Application 子类
│       │   │   ├── MainActivity.kt
│       │   │   └── util/
│       │   │       ├── privacy/PrivacyManager.kt
│       │   │       └── simhash/SimHash.kt
│       │   │   └── util/sync/SyncCoordinator.kt  # 网络恢复监听 + viewModelScope 同步
│       │   └── res/
│       │       ├── values/{colors,strings,themes}.xml
│       │       ├── values-night/themes.xml
│       │       ├── drawable/
│       │       ├── mipmap-*/
│       │       └── xml/{backup_rules,data_extraction_rules,network_security_config}.xml
│       ├── test/  (单元测试)
│       └── androidTest/  (UI / Instrumented 测试)
├── gradle/libs.versions.toml
├── settings.gradle.kts
├── build.gradle.kts
└── gradle.properties
```

#### 后端（V2.0 精简：单文件 SQLite + numpy）
```
server/
├── app/
│   ├── main.py            (FastAPI 入口)
│   ├── api/               (FastAPI 路由)
│   │   ├── documents.py
│   │   ├── questions.py
│   │   ├── feynman.py
│   │   ├── graph.py
│   │   └── cards.py
│   ├── services/          (业务逻辑)
│   │   ├── document_service.py
│   │   ├── dedup_service.py
│   │   ├── anchor_engine.py
│   │   ├── feynman_evaluator.py
│   │   └── graph_builder.py
│   ├── llm/               (大模型封装)
│   │   └── provider.py    (Moonshot / DeepSeek 适配)
│   ├── models/            (SQLAlchemy ORM 同步)
│   │   ├── database.py
│   │   ├── document.py
│   │   ├── segment.py
│   │   └── graph.py
│   ├── vectors/           (numpy 向量存储)
│   │   └── store.py       (npy 文件 + cosine_similarity)
│   └── core/              (鉴权 / 配置 / Fernet 加密)
├── uploads/               (本地存储非敏感文档)
├── vectors/               (npy 文件目录)
├── bluelink.db            (SQLite 单文件)
├── tests/
└── requirements.txt
```

### 13.3 比赛演示 Checklist
- [ ] 首页瀑布流展示示例文档。
- [ ] 上传一篇 PDF，自动去重折叠演示。
- [ ] 对话页面提问，展示引言+锚点卡片。
- [ ] 点击锚点跳转，聚光灯高亮，退出恢复。
- [ ] 费曼模式，语音输入解释，看到偏差引力线。
- [ ] 知识图谱页，展示动态节点和连线，可缩放聚焦。
- [ ] 设置页体现隐私开关和认知分层选项。
- [ ] **V2.0 新增**：断开网络后，瀑布流仍可浏览已缓存文档。
- [ ] **V2.0 新增**：标记私密文档后，logcat 确认原文未上传。
- [ ] **V2.0 新增**：拍照 OCR 一段文字，自动生成灵感卡片。

### 13.4 App 图标与启动图资源（V2.0 完整规范，AI 可直接生成）

#### 13.4.1 图标设计规范
- **品牌主元素**：克莱因蓝 (#002FA7) 圆角方形底 + 白色"链环"图形（代表 BlueLink"蓝链"）。
- **形状**：圆角矩形，Android Adaptive Icon 安全区 66dp / 108dp。
- **风格**：极简，线条粗细 2dp 等价。
- **资源位置**：
    - 前层（foreground）：`app/src/main/res/drawable/ic_launcher_foreground.xml`（Vector Drawable）
    - 背景层（background）：`app/src/main/res/drawable/ic_launcher_background.xml`（纯色 #002FA7）
    - 自适应图标（API 26+）：`app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml` + `ic_launcher_round.xml`

#### 13.4.2 启动图（Splash Screen）
Android 12+ 自动用 Splash Screen API，无需写代码；低版本在主题里配 `windowBackground`。

```xml
<!-- res/values/themes.xml -->
<style name="Theme.BlueLink.Splash" parent="Theme.SplashScreen">
    <item name="windowSplashScreenBackground">@color/rice_white</item>
    <item name="windowSplashScreenAnimatedIcon">@drawable/ic_launcher_foreground</item>
    <item name="postSplashScreenTheme">@style/Theme.BlueLink</item>
</style>

<!-- res/values/colors.xml -->
<color name="rice_white">#FAF7F2</color>
<color name="klein_blue">#002FA7</color>
```

```xml
<!-- AndroidManifest.xml 中 MainActivity 的 theme 改为 Splash -->
<activity
    android:name=".MainActivity"
    android:theme="@style/Theme.BlueLink.Splash"
    android:exported="true">
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>
</activity>
```

#### 13.4.3 资源文件清单（AI 照单生成）
| 文件路径 | 类型 | 说明 |
|---------|------|------|
| `res/drawable/ic_launcher_foreground.xml` | Vector | 白色链环图形 |
| `res/drawable/ic_launcher_background.xml` | Vector | 克莱因蓝纯色矩形 |
| `res/mipmap-anydpi-v26/ic_launcher.xml` | Adaptive | 引用前后层 |
| `res/mipmap-anydpi-v26/ic_launcher_round.xml` | Adaptive | 圆形版（同上） |
| `res/mipmap-mdpi/ic_launcher.webp` | 位图 | 48x48 |
| `res/mipmap-hdpi/ic_launcher.webp` | 位图 | 72x72 |
| `res/mipmap-xhdpi/ic_launcher.webp` | 位图 | 96x96 |
| `res/mipmap-xxhdpi/ic_launcher.webp` | 位图 | 144x144 |
| `res/mipmap-xxxhdpi/ic_launcher.webp` | 位图 | 192x192 |
| `res/values/colors.xml` | 颜色 | rice_white / klein_blue / sand_line |
| `res/values/themes.xml` | 主题 | Theme.BlueLink / Theme.BlueLink.Splash |
| `res/values/strings.xml` | 字符串 | `<string name="app_name">蓝链</string>` |
| `res/values-night/themes.xml` | 夜间主题 | 复用 Theme.BlueLink，用 `?attr/colorPrimary` 跟随 Material You |
| `res/xml/network_security_config.xml` | 网络安全 | 禁明文 + TLS 1.3 |
| `res/xml/backup_rules.xml` | 备份 | 禁备份 |
| `res/xml/data_extraction_rules.xml` | 备份 | 禁云端恢复 |
| `assets/echarts.min.js` | JS | ECharts 5.5.0（约 1MB，本地化） |
| `assets/graph.html` | HTML | 知识图谱入口（详见 §9.4） |
| `res/font/noto_serif_sc_regular.otf` | 字体 | 阅读区衬线体 |
| `res/font/noto_serif_sc_bold.otf` | 字体 | 衬线体加粗 |
| `res/font/noto_sans_sc_regular.otf` | 字体 | UI 无衬线体 |
| `res/font/noto_sans_sc_medium.otf` | 字体 | UI 中等粗细 |
| `res/font/noto_sans_sc_bold.otf` | 字体 | UI 加粗 |

#### 13.4.4 启动图动画（可选，V2.0 简化）
- 不做自定义动画，直接用 Android 12+ Splash Screen API 的"图标轻微缩放"默认行为。
- 启动后立即 `setContent { BlueLinkTheme { ... } }`，避免冷启动卡顿。

---

**文档变更记录**

| 版本 | 日期 | 修订内容 | 作者 |
| --- | --- | --- | --- |
| 1.0 | 2026-05-26 | 初始版本，uni-app + FastAPI 跨端方案 | Eau团队 |
| 1.1 | 2026-05-26 | 修订详细设计章节 | Eau团队 |
| 2.0 | 2026-06-05 | 客户端栈重构为 Android Kotlin + Jetpack Compose（手写 AppContainer 替代 Hilt；EncryptedSharedPreferences 替代 Tink；kapt 替代 KSP；Moshi 替代 kotlinx.serialization；移除 WorkManager）；后端降级为 SQLite + numpy（移除 PostgreSQL/pgvector/Celery/MinIO）；图谱用 WebView + ECharts 实现 | Eau团队 |
