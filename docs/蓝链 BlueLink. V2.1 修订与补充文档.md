**版本**：2.1
**日期**：2026年6月7日
**基础版本**：V2.0（2026-06-05）
**团队**：Eau
**目标平台**：Android（**Vivo 高端机优先**，兼容主流旗舰）
**生效范围**：替代 V2.0 中内部矛盾章节；补充 V2.0 缺失的工程化细节

---

## V2.1 修订总览

V2.1 在 V2.0 基础上完成 **决策统一 + 工程化补全 + 设备定位收敛**。**不引入新功能，不调整 P0 范围，不修改 UI/UX 视觉规范**。

| # | 类别 | V2.0 现状 | V2.1 决策 | 决策理由 |
|---|------|----------|----------|----------|
| 1 | **加密策略** | PRD 写 Tink + Room AES 字段；详细设计写 EncryptedSharedPreferences + Room 明文。**两处文档自相矛盾** | **统一**：SecurePrefs 存密文，Room 仅存 `ref` + `snippet` | 消除矛盾；代码改动小；安全性等价 |
| 2 | **OCR 选型** | ML Kit Text Recognition（强依赖 GMS） | **替换为 PaddleOCR Lite**（零系统服务依赖） | Vivo 中国版（OriginOS）多数无 GMS，ML Kit 必挂 |
| 3 | **性能约束** | APK < 30MB、内存 < 200MB、后台 < 5%/h | **取消 APK 体积限制**；内存/耗电放宽 | 高端机 12GB+ RAM，体积/内存不构成瓶颈 |
| 4 | **登录鉴权** | API 要求 Bearer Token，客户端无登录流程设计 | **暂不实现登录**，改匿名 UUID + `X-User-Id` Header | 比赛场景无登录需求；后端字段不变 |
| 5 | **工程化缺失** | 部署变量、Mock 数据、隐私 UI 切换、PDF 渲染定位、同步冲突 5 项均无规范 | **新增 5 个工程化章节**（§5） | 评审演示与开发上手均需补齐 |
| 6 | **Vivo 适配** | V2.0 仅在厂商表中一笔带过 | **新增专章**（§6），覆盖权限/后台/引导/通知 | 目标设备锁定 Vivo，适配需细化 |

> V2.1 与 V2.0 **并存**：V2.0 文档保留作为历史参考；V2.1 为"修订与补充"，对矛盾/缺失部分**具最高优先级**。

---

## 1. 加密策略统一（V2.1 终稿）

> **覆盖范围**：替代 V2.0 PRD §4.7 / §7.2 / §8.2 与详细设计 §5.2 / §9.5 / §10.1 中所有加密相关条款。

### 1.1 决策

**所有文档 / 灵感原文一律经 AES-256-GCM 加密后存入 SecurePrefs（EncryptedSharedPreferences）；Room 仅存元数据与指向 SecurePrefs 的 ref key，不存任何明文原文。**

主密钥由 Android Keystore 硬件保护，密文即使被物理提取也无法解密。

### 1.2 加密链路

```
原文（String）
   ↓  Android Keystore 主密钥
AES-256-GCM 密文（ByteArray）
   ↓  Base64 编码
字符串密文
   ↓  key = "seg:<uuid>" 存 EncryptedSharedPreferences
SecurePrefs
   ↑
Room 存  textRef = "seg:<uuid>"   +   textSnippet = "前 30 字摘要（明文）"
```

### 1.3 关键设计

- **主密钥**：由 `MasterKey.Builder` 生成，`KeyScheme.AES256_GCM`，**永不离开 Keystore**。
- **每段独立 key**：用 `seg:<segmentId>` / `card:<cardId>` 命名空间，避免冲突。
- **摘要字段**：Room 中保留 `textSnippet: String`（前 30 字明文），用于列表展示与缓存命中判断，不构成敏感长文泄露。
- **删除同步**：删除 Room 实体时**必须同步** `securePrefs.removeCipherText(textRef)`，否则产生"删 Room 留密文"的内泄漏。

### 1.4 数据模型变更（Room）

```kotlin
// SegmentEntity —— V2.1 移除 text，新增 textRef + textSnippet
@Entity(
    tableName = "segments",
    indices = [Index("docId"), Index(value = ["docId", "indexInDoc"], unique = true)]
)
data class SegmentEntity(
    @PrimaryKey val id: String,
    val docId: String,
    val indexInDoc: Int,
    val textRef: String,                 // V2.1: 指向 SecurePrefs key
    val textSnippet: String = "",        // V2.1: 前 30 字摘要（明文）
    val vectorBlob: ByteArray? = null,
    val isFolded: Boolean = false,
    val isSpotlightTarget: Boolean = false
)

// InspirationCardEntity —— V2.1 移除 content，新增 contentRef + contentSnippet
@Entity(tableName = "inspiration_cards")
data class InspirationCardEntity(
    @PrimaryKey val id: String,
    val contentRef: String,              // V2.1
    val contentSnippet: String = "",     // V2.1
    val type: CardType,
    val privacyLevel: PrivacyLevel = PrivacyLevel.LOCAL_ONLY,
    val tags: String,
    val createdAt: Long
)

// AnchorEntity —— V2.1 保持不变（snippet 为前 30 字摘要，本身即为派生数据）
// PendingSyncEntity —— V2.1 见 §5.5
```

### 1.5 SecurePrefs 扩展实现

```kotlin
// data/local/crypto/SecurePrefs.kt —— V2.1 扩展
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

    // ====== V2.0 既有：通用凭据（WebDAV 密码、API token 等）======
    fun putSecret(key: String, value: String) =
        prefs.edit().putString(key, value).apply()
    fun getSecret(key: String): String? = prefs.getString(key, null)
    fun removeSecret(key: String) = prefs.edit().remove(key).apply()

    // ====== V2.1 新增：原文密文 ======
    fun putCipherText(key: String, ciphertext: ByteArray) =
        prefs.edit().putString(key, Base64.encodeToString(ciphertext, Base64.NO_WRAP)).apply()

    fun getCipherText(key: String): ByteArray? =
        prefs.getString(key, null)?.let { Base64.decode(it, Base64.NO_WRAP) }

    fun removeCipherText(key: String) = prefs.edit().remove(key).apply()

    // ====== V2.1 新增：命名空间工具 ======
    object Keys {
        fun segment(id: String) = "seg:$id"
        fun card(id: String)    = "card:$id"
    }
}
```

### 1.6 Repository 改造示例

```kotlin
// data/repository/SegmentRepository.kt —— V2.1 写入加密、读取解密
class SegmentRepository(
    private val dao: SegmentDao,
    private val securePrefs: SecurePrefs
) {
    suspend fun saveSegment(text: String, docId: String, indexInDoc: Int): SegmentEntity {
        val id = UUID.randomUUID().toString()
        val ref = SecurePrefs.Keys.segment(id)
        val ciphertext = AeadEngine.encrypt(text.encodeToByteArray())  // Keystore AES-GCM
        securePrefs.putCipherText(ref, ciphertext)
        val entity = SegmentEntity(
            id = id,
            docId = docId,
            indexInDoc = indexInDoc,
            textRef = ref,
            textSnippet = text.take(30)
        )
        dao.insert(entity)
        return entity
    }

    suspend fun readText(segment: SegmentEntity): String {
        val ciphertext = securePrefs.getCipherText(segment.textRef)
            ?: error("密文丢失: ${segment.textRef}")
        return AeadEngine.decrypt(ciphertext).decodeToString()
    }

    suspend fun deleteSegment(entity: SegmentEntity) {
        securePrefs.removeCipherText(entity.textRef)  // 同步删除密文
        dao.delete(entity)
    }
}
```

`AeadEngine` 封装 Android Keystore 的 AES-GCM 加解密（细节略，10 行代码，AI 可直接写）。

### 1.7 删除安全检查（V2.1 新增约定）

所有删除 Room 实体的 DAO 方法**必须包成删除包装函数**，统一处理"先删密文再删 Room"：

```kotlin
// data/repository/SecureDeleteHelper.kt
suspend inline fun <T> secureDelete(
    securePrefs: SecurePrefs,
    refsToDelete: List<String>,
    dbAction: suspend () -> Unit
) {
    dbAction()
    refsToDelete.forEach { securePrefs.removeCipherText(it) }
}
```

> V2.0 详细设计 §9.5 提到的"V2.1 升级方案"在 V2.1 中正式落地。

---

## 2. OCR 选型变更

> **覆盖范围**：替代 V2.0 PRD §7.1.2 与详细设计 §3.1 中"ML Kit Text Recognition"相关条款；同步更新 libs.versions.toml 与 AppContainer。

### 2.1 决策

**V2.1 客户端 OCR 改用 PaddleOCR Lite（ONNX Runtime 后端），不再使用 ML Kit Text Recognition。**

### 2.2 选型对比

| 维度 | ML Kit Text Recognition | PaddleOCR Lite（ONNX） |
|------|------------------------|----------------------|
| GMS 依赖 | **必须** | **不需要** |
| Vivo 中国版支持 | **❌** OriginOS 4+ 默认无 GMS，调用即抛 `ApiException: 17` | ✅ 全部支持，零系统服务依赖 |
| 华为鸿蒙兼容 | ❌ | ✅ |
| 离线识别 | ✅ | ✅ |
| 中文识别准确率 | 高 | **极高**（针对中文优化，PP-OCRv3/v4） |
| 模型体积 | 拉丁 4MB / 中文 8MB | 中英文通用 ~14MB |
| 集成方式 | Google Play Services 自动管理 | Gradle 依赖 + 资产文件 |
| AI 训练数据丰富度 | 极丰富 | 中等（但 PaddleOCRONNX 有成熟文档） |

**核心决策理由**：Vivo 中国版（OriginOS 4+）默认不预装 Google Play Services，ML Kit 首次调用必失败；改用 PaddleOCR 跨厂商一致，比赛演示不翻车。

### 2.3 依赖调整

```toml
# gradle/libs.versions.toml —— V2.1 替换

# 移除：
# mlkit-text-recognition         = { module = "com.google.mlkit:text-recognition", ... }
# mlkit-text-recognition-chinese = { module = "com.google.mlkit:text-recognition-chinese", ... }

# 新增：
onnxruntime = { module = "com.microsoft.onnxruntime:onnxruntime-android", version = "1.18.0" }
paddle-ocr  = { module = "io.github.darwinhc:bazel-paddle-ocr-android", version = "1.0.0" }
```

> 推荐 `com.microsoft.onnxruntime:onnxruntime-android` + 自封装 `PaddleOCRONNX` 工具类（~80 行），完全本地化。

### 2.4 实现接口

```kotlin
// data/local/ocr/PaddleOcrDataSource.kt —— V2.1 新增（替换 MlKitOcrDataSource）
class PaddleOcrDataSource(context: Context) {
    private val ocr = PaddleOCRONNX(
        assetManager = context.assets,
        modelDir = "models/ch_PP-OCRv3/",
        detModelFile = "ch_PP-OCRv3_det_infer.onnx",
        recModelFile = "ch_PP-OCRv3_rec_infer.onnx",
        dictFile = "ppocr_keys.txt"
    )

    /** 同步识别（在 IO 线程调用）。V2.1 MVP 不做 Flow 异步。 */
    fun recognize(bitmap: Bitmap): String {
        return ocr.run(bitmap)
            .joinToString("\n") { it.text }
            .ifBlank { error("未识别到文字") }
    }
}

// AppContainer 中替换
private val ocrDataSource: OcrDataSource by lazy {
    PaddleOcrDataSource(applicationContext)
}
```

### 2.5 资源补充

`app/src/main/assets/models/ch_PP-OCRv3/` 下放置：
- `ch_PP-OCRv3_det_infer.onnx`（检测模型，~3MB）
- `ch_PP-OCRv3_rec_infer.onnx`（识别模型，~10MB）
- `ppocr_keys.txt`（字典，~1MB）
- 共约 14MB，参与 APK 体积（V2.1 已取消体积限制）。

下载来源：PaddleOCR 官方 `PaddleOCR/doc/doc_ch/models_list.md`。

### 2.6 资源回退方案

若 ONNX 模型加载失败（极端低端机或缺资产），降级为**手动输入**：

```kotlin
// CaptureViewModel.onOcrFailed
fun onOcrFailed() {
    _state.update { it.copy(
        showManualInputFallback = true,
        ocrStatus = "未识别到文字，请手动输入"
    ) }
}
```

---

## 3. 性能与设备定位更新

> **覆盖范围**：替代 V2.0 PRD §8.1 与详细设计 §12.1 中的指标条款。

### 3.1 目标设备清单

V2.1 假设目标用户设备为 **Vivo 高端机 + 主流旗舰**：

| 设备 | SoC | RAM | 定位 |
|------|-----|-----|------|
| **Vivo X100 Ultra / X Fold 3 / X100s Pro** | SD8 Gen 3 / 天玑 9300+ | 12GB+ | **主推** |
| **iQOO 12 Pro / Neo 9 Pro+** | SD8 Gen 3 / 天玑 9300+ | 12GB+ | 性能党 |
| 小米 14 Pro / 14 Ultra | SD8 Gen 3 | 12GB+ | 兼容 |
| 华为 Mate 60 Pro+ / Pura 70 Ultra | 麒麟 9000S / 9010 | 12GB | 兼容（无 GMS） |
| OPPO Find X7 Ultra / 一加 12 | 天玑 9300 / SD8 Gen 3 | 12GB+ | 兼容 |
| 三星 Galaxy S24 Ultra | SD8 Gen 3 (for Galaxy) | 12GB | 海外 |

V2.1 **不覆盖**：中低端机（< 8GB RAM）、Android < 12 设备。

### 3.2 指标调整

| 指标 | V2.0 目标 | V2.1 调整 | 说明 |
|------|----------|----------|------|
| 冷启动 < 2.0s | ✅ 保留 | ✅ 保留 | 硬指标 |
| 锚点跳转 < 500ms | ✅ 保留 | ✅ 保留 | 硬指标 |
| 文库滚动 60fps | ✅ 保留 | ✅ 保留 | 硬指标 |
| 图谱 60fps | ✅ 保留 | ✅ 保留（500 节点放宽到 1000 节点） | ECharts 1000 节点在 SD8 Gen 3 下流畅 |
| 本地去重 1MB < 3s | ✅ 保留 | ✅ 保留 | 硬指标 |
| 内存占用 | < 200MB | **< 350MB**（放宽） | 高端机有 12GB+ RAM |
| 后台耗电 | < 5%/h | **< 8%/h**（放宽） | 录音/通知可触发少量耗电 |
| **APK 体积** | **< 30MB** | **❌ 不设限** | PaddleOCR 14MB + 字体 5MB + ECharts 1MB 即可超 30MB，不再约束 |

### 3.3 取消的"低端机"折中

- ❌ 不再按 sw320dp ~ sw480dp 限定手机布局：可放心使用 `WindowSizeClass` 适配平板双栏。
- ❌ 不再"WebView 单独进程隔离"：高端机主进程内存充足，单进程更稳定。
- ❌ 不再"字体按需下载"：思源宋体/黑体全套内置，APK 体积可接受。
- ❌ 不再"ML Kit 走 Dynamic Feature"：V2.1 OCR 已是 PaddleOCR 本地化，不分模块。

---

## 4. 鉴权策略

> **覆盖范围**：替代 V2.0 详细设计 §5 / §7.1 中"Bearer Token"假设，及相关后端鉴权伪代码。

### 4.1 决策

**V2.1 暂不实现登录流程。客户端首次启动生成匿名 UUID 作为 `user_id`，所有 API 请求带 `X-User-Id: <uuid>` Header。后端 `user_id` 字段直接用客户端 UUID，不再校验 Bearer Token。**

### 4.2 客户端实现

```kotlin
// data/identity/UserIdentity.kt —— V2.1 新增
class UserIdentity(context: Context) {
    private val prefs = context.getSharedPreferences("bluelink_identity", Context.MODE_PRIVATE)

    val userId: String by lazy {
        prefs.getString("user_id", null) ?: run {
            val newId = UUID.randomUUID().toString()
            prefs.edit().putString("user_id", newId).apply()
            newId
        }
    }
}

// AppContainer 中注入
val userIdentity: UserIdentity by lazy { UserIdentity(applicationContext) }

// data/remote/AuthInterceptor.kt —— V2.1 修改
class AuthInterceptor(private val userIdentity: UserIdentity) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .addHeader("X-User-Id", userIdentity.userId)
            .addHeader("X-Client-Version", BuildConfig.VERSION_NAME)
            .addHeader("X-Platform", "android")
            .build()
        return chain.proceed(request)
    }
}
```

### 4.3 后端实现

```python
# app/core/auth.py —— V2.1 替换 Bearer Token 校验
from fastapi import Header, HTTPException, Depends

async def get_user_id(
    x_user_id: str = Header(..., min_length=32, max_length=64)
) -> str:
    if not x_user_id.replace("-", "").isalnum():
        raise HTTPException(
            status_code=400,
            detail={"code": "INVALID_USER_ID", "message": "X-User-Id 格式非法"}
        )
    return x_user_id

# 各路由依赖注入
@app.post("/api/v1/documents/upload")
async def upload_doc(
    user_id: str = Depends(get_user_id),
    file: UploadFile = File(...)
):
    # 直接用 user_id 写库，无 token 校验
    ...
```

### 4.4 V2.x 升级路径

V2.x 接入正式登录时，Auth-Token 与 X-User-Id **并存**：

- Header 1：`X-User-Id: <uuid>`（必填，匿名即用）
- Header 2：`Authorization: Bearer <token>`（可选，已登录则后端关联到 user_id）
- 后端 SQLite 增量添加 `users` 表与 `auth_tokens` 表，最小改动。

```
匿名用户：X-User-Id 唯一身份，跨设备无法合并
已登录：X-User-Id + Token → 后端 users 表主键，多设备同 user_id 合并
```

---

## 5. 工程化补充章节

> **覆盖范围**：V2.0 缺失的 5 项工程化规范。

### 5.1 部署环境变量清单

#### 后端 `server/.env.example`

```bash
# ===== LLM API =====
# Moonshot（默认）
MOONSHOT_API_KEY=sk-...
MOONSHOT_BASE_URL=https://api.moonshot.cn/v1
MOONSHOT_MODEL=moonshot-v1-8k
MOONSHOT_EMBED_MODEL=embedding-v1

# DeepSeek（备用）
DEEPSEEK_API_KEY=sk-...
DEEPSEEK_BASE_URL=https://api.deepseek.com/v1
DEEPSEEK_MODEL=deepseek-chat

# 默认 LLM 提供方: moonshot / deepseek
LLM_PROVIDER=moonshot
LLM_TEMPERATURE=0.3
LLM_TIMEOUT_SEC=30

# ===== 数据库与存储 =====
DATABASE_URL=sqlite:///./bluelink.db
VECTORS_DIR=./vectors
UPLOADS_DIR=./uploads

# ===== 安全 =====
# Fernet 对称加密密钥（首次启动自动生成到 .fernet_key）
# 生产环境必须从 KMS / Secrets Manager 注入
BLUELINK_FERNET_KEY=

# ===== 服务 =====
HOST=0.0.0.0
PORT=8000
LOG_LEVEL=INFO
ENV=development  # development / production
WORKERS=1

# ===== CORS =====
CORS_ORIGINS=http://localhost:3000,https://bluelink.example.com

# ===== 限流 =====
RATE_LIMIT_PER_MINUTE=60
```

启动方式：
```bash
# 开发
cp .env.example .env  # 填入 API Key
uvicorn app.main:app --reload

# 生产
gunicorn app.main:app -w 4 -k uvicorn.workers.UvicornWorker -b 0.0.0.0:8000
```

#### 客户端 `local.properties`（不入版本控制）

```properties
# 开发：指向本地后端（10.0.2.2 是 Android 模拟器到宿主机的回环）
bluelink.backend.baseUrl=http://10.0.2.2:8000/

# 联调测试
# bluelink.backend.baseUrl=https://api-staging.bluelink.example.com/

# 生产
# bluelink.backend.baseUrl=https://api.bluelink.example.com/
```

通过 Gradle BuildConfig 注入：

```kotlin
// app/build.gradle.kts
android {
    defaultConfig {
        buildConfigField(
            "String",
            "BACKEND_BASE_URL",
            "\"${project.property("bluelink.backend.baseUrl")}\""
        )
    }
}
```

### 5.2 Seed 脚本（演示数据）

V2.0 缺"评审现场如何看到数据"。V2.1 提供自动 seed。

```python
# server/scripts/seed.py —— V2.1 新增
"""
首次启动自动 seed 演示数据：
- 3 篇示例 PDF（来自 ./seed_data/）
- 10 条示例灵感卡片
- 1 个示例图谱（5 节点 + 8 边）

用法：uvicorn 启动时检测空库自动调用；或手动 `python -m scripts.seed`
"""
import sys
from pathlib import Path
sys.path.append(str(Path(__file__).parent.parent))

from app.models.database import init_db, SessionLocal
from app.models.document import Document
from app.models.segment import Segment
from app.models.inspiration import InspirationCard
from app.models.graph import GraphNode, GraphEdge
from app.services.document_service import DocumentService

SEED_USER_ID = "seed-user-0000-0000-0000-000000000000"

def seed():
    init_db()
    db = SessionLocal()
    if db.query(Document).filter(Document.user_id == SEED_USER_ID).count() > 0:
        print("[seed] 数据库已有 seed 数据，跳过")
        return
    print("[seed] 开始注入演示数据...")

    # 1. 导入 3 篇示例 PDF
    seed_dir = Path(__file__).parent / "seed_data"
    svc = DocumentService()
    for pdf_name in ["deep_work.pdf", "thinking_fast_slow.pdf", "antifragile.pdf"]:
        pdf_path = seed_dir / pdf_name
        if pdf_path.exists():
            doc_id, segs = svc.parse_and_chunk(str(pdf_path), privacy_level="CLOUD_OK")
            db.add(Document(id=doc_id, user_id=SEED_USER_ID,
                            title=pdf_name.replace(".pdf", "").replace("_", " ").title(),
                            privacy_level="CLOUD_OK"))
            for seg in segs:
                db.add(Segment(id=seg["id"], doc_id=doc_id,
                               index_num=seg["index"],
                               text_encrypted=svc.encrypt_text(seg["text"])))
        else:
            print(f"[seed] 警告: {pdf_path} 不存在，跳过")

    # 2. 注入 10 条灵感卡片
    sample_cards = [
        ("费曼学习法的核心是用输出倒逼输入，输出是最强的输入",
         ["费曼", "学习法", "认知"]),
        ("信息增量的本质是减少认知熵，让新信息贴合已有图式",
         ["认知", "熵", "学习"]),
        ("聚光灯阅读让眼睛和大脑都聚焦到关键句段",
         ["阅读", "聚光灯"]),
        ("AI 不应给答案，应给路径——这是蓝链的核心理念",
         ["AI", "产品哲学"]),
        ("增量折叠：用户已知的背景不再重复显示",
         ["增量", "折叠", "阅读"]),
        ("引力线是节点之间关系的可视化语言",
         ["图谱", "引力线"]),
        ("Moonshot 8K 上下文够覆盖一篇长文的所有切片",
         ["Moonshot", "LLM"]),
        ("Room FTS 替代向量库在 MVP 阶段完全够用",
         ["Room", "FTS", "MVP"]),
        ("SecurePrefs 间接存储比 Room 字段加密更稳健",
         ["加密", "SecurePrefs"]),
        ("PaddleOCR 在 Vivo 中国版无 GMS 也能稳定运行",
         ["PaddleOCR", "Vivo", "OCR"]),
    ]
    for content, tags in sample_cards:
        db.add(InspirationCard(
            id=f"seed-card-{sample_cards.index((content, tags)):03d}",
            user_id=SEED_USER_ID,
            content=content,
            content_encrypted=svc.encrypt_text(content),
            tags=",".join(tags),
            privacy_level="LOCAL_FIRST"
        ))

    # 3. 注入示例图谱（5 节点 + 8 边）
    nodes = [
        ("n1", "深度工作", "CONCEPT"),
        ("n2", "费曼学习法", "CONCEPT"),
        ("n3", "信息增量", "CONCEPT"),
        ("n4", "Deep Work", "DOCUMENT"),
        ("n5", "灵感卡片 #1", "INSPIRATION"),
    ]
    for nid, label, ntype in nodes:
        db.add(GraphNode(id=nid, label=label, type=ntype, ref_id=None))
    edges = [
        ("n1", "n4", "CITE",       0.95),
        ("n1", "n2", "SUPPLEMENT", 0.7),
        ("n2", "n5", "SUPPORT",    0.85),
        ("n3", "n1", "CHALLENGE",  0.6),
        ("n3", "n5", "SUPPLEMENT", 0.75),
        ("n4", "n1", "CITE",       0.9),
        ("n2", "n1", "SUPPORT",    0.8),
        ("n5", "n3", "SUPPORT",    0.7),
    ]
    for src, tgt, rel, conf in edges:
        db.add(GraphEdge(
            id=f"e-{src}-{tgt}", source_id=src, target_id=tgt,
            relation_type=rel, confidence=conf, is_manual=0
        ))

    db.commit()
    print(f"[seed] 完成：3 文档 / {len(sample_cards)} 灵感 / {len(nodes)} 节点 / {len(edges)} 边")

if __name__ == "__main__":
    seed()
```

`app/main.py` 启动时自动 seed：
```python
@app.on_event("startup")
async def startup_event():
    from scripts.seed import seed
    seed()
```

### 5.3 隐私分级 UI 切换

V2.0 缺乏"用户在哪儿把文档从 LOCAL_ONLY 改 LOCAL_FIRST"的设计。V2.1 补全。

#### 入口位置

`DocumentCard` 长按菜单 → **"隐私设置"**。

#### 弹窗组件

```kotlin
// ui/component/PrivacyPickerSheet.kt —— V2.1 新增
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPickerSheet(
    current: PrivacyLevel,
    onSelect: (PrivacyLevel) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var pending by remember { mutableStateOf<PrivacyLevel?>(null) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp).navigationBarsPadding()) {
            Text("选择隐私等级", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                "切换后数据存储行为将立即变化",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))

            PrivacyOption(
                icon = "🔒",
                title = "仅本地 (LOCAL_ONLY)",
                desc = "原文加密存本地，永远不上传云端",
                selected = current == PrivacyLevel.LOCAL_ONLY,
                onClick = { pending = PrivacyLevel.LOCAL_ONLY }
            )
            PrivacyOption(
                icon = "🔐",
                title = "优先本地 (LOCAL_FIRST)",
                desc = "向量可上传增强检索，原文默认不上传",
                selected = current == PrivacyLevel.LOCAL_FIRST,
                onClick = { pending = PrivacyLevel.LOCAL_FIRST }
            )
            PrivacyOption(
                icon = "☁️",
                title = "允许云端 (CLOUD_OK)",
                desc = "全量数据可上传用于跨设备同步",
                selected = current == PrivacyLevel.CLOUD_OK,
                onClick = { pending = PrivacyLevel.CLOUD_OK }
            )

            Spacer(Modifier.height(8.dp))
            // 风险提示
            val goingUp = pending != null && pending!!.ordinal > current.ordinal
            if (goingUp) {
                Text(
                    "⚠️ 切换到更高云端等级后，原文将被解密上传，无法完全回退",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(Modifier.height(8.dp))
            }

            Button(
                onClick = {
                    pending?.let { onSelect(it) }
                    onDismiss()
                },
                enabled = pending != null && pending != current,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(24.dp)
            ) { Text("确认切换") }
        }
    }

    // 从 LOCAL_ONLY 切到 CLOUD_OK 时强制二次确认
    if (pending == PrivacyLevel.CLOUD_OK && current == PrivacyLevel.LOCAL_ONLY) {
        ConfirmPrivacyChangeDialog(
            from = current, to = PrivacyLevel.CLOUD_OK,
            onConfirm = { onSelect(PrivacyLevel.CLOUD_OK); onDismiss() },
            onCancel = { /* 用户取消，不切换 */ }
        )
    }
}

@Composable
private fun PrivacyOption(
    icon: String, title: String, desc: String,
    selected: Boolean, onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer
                else Color.Transparent
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(icon, fontSize = 24.sp)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(desc, style = MaterialTheme.typography.bodySmall,
                 color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        RadioButton(selected = selected, onClick = onClick)
    }
}
```

#### 切换确认对话框

```kotlin
// ui/component/ConfirmPrivacyChangeDialog.kt —— V2.1 新增
@Composable
fun ConfirmPrivacyChangeDialog(
    from: PrivacyLevel, to: PrivacyLevel,
    onConfirm: () -> Unit, onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        icon = { Icon(Icons.Default.Warning, contentDescription = null,
                     tint = MaterialTheme.colorScheme.error) },
        title = { Text("确认切换隐私等级？") },
        text = {
            Text("""
                从「${from.label}」切换到「${to.label}」会改变数据存储与上传行为：

                • 原文将解密并上传至云端
                • AI 服务可能接触到原文内容
                • 此操作不可撤销

                是否继续？
            """.trimIndent())
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("确认上传", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = { TextButton(onClick = onCancel) { Text("取消") } }
    )
}
```

### 5.4 PDF 渲染定位

V2.0 选型表写"PDF 渲染走 PdfRenderer"但阅读器流程未明确。V2.1 决策：**MVP 阶段不渲染 PDF 原图，纯文本模式**。

#### 决策

- 后端 PyMuPDF 解析 PDF → 段落文本（含 `page_number` + `bbox`）+ 图片提取
- 客户端**只接收文本**，**不渲染 PDF 原图**（避免 PdfRenderer 兼容性问题与额外 5-10MB 体积）
- 聚光灯定位基于 `indexInDoc`，**不用 bbox**

#### 后续升级（V2.x）

V2.x 引入 PdfRenderer（需用户投票决定是否做）：
- 后端除文本外，返回段落所在页码
- 客户端对当前页 `PdfRenderer.openPage(pageIndex).render(bitmap)` 渲染位图
- 聚光灯时在位图上画高亮矩形
- 切换段落 → 加载新页位图 + 滚动

#### 数据模型补充

```kotlin
// DocumentEntity —— V2.1 新增字段
@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey val id: String,
    val title: String,
    val privacyLevel: PrivacyLevel,
    val source: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val conceptBeacon: String?,
    val isFoldedGlobal: Boolean = false,
    val pageCount: Int? = null,         // V2.1: PDF 页数（为 V2.x PdfRenderer 升级预留）
    val hasOriginalImage: Boolean = false // V2.1: 是否保留 PDF 原图（默认 false）
)
```

### 5.5 同步冲突解决

V2.0 缺乏 `pending_sync` 冲突解决策略。V2.1 补全。

#### 同步状态机

```kotlin
// domain/model/SyncStatus.kt —— V2.1 新增
enum class SyncStatus { PENDING, IN_FLIGHT, SUCCESS, FAILED }

// PendingSyncEntity 扩展
@Entity(tableName = "pending_sync")
data class PendingSyncEntity(
    @PrimaryKey val id: String,
    val operation: SyncOp,
    val localRefId: String,                 // V2.1: 客户端 UUID（必填）
    val serverRefId: String? = null,        // V2.1: 同步成功后回填
    val payloadJson: String,
    val createdAt: Long,
    val retryCount: Int = 0,
    val lastError: String? = null,          // V2.1: 记录上次失败原因
    val status: SyncStatus = SyncStatus.PENDING
)

// SyncOp 扩展：增加删除操作
enum class SyncOp {
    CREATE_DOC, CREATE_CARD, UPDATE_PROFILE,
    FOLD_SEGMENT,
    DELETE_DOC, DELETE_CARD                // V2.1 新增
}
```

#### 同步流程

```
       PENDING
          │  网络恢复 / ON_START
          ↓
      IN_FLIGHT
          │
   ┌──────┼──────┐
   ↓      ↓      ↓
SUCCESS  FAILED  网络异常
   │      │      │
   ↓      ↓      ↓
回填    retry++   保持 IN_FLIGHT
server  上限 3    等下次重试
RefId   次
   │      │
   │      ↓
   │    FAILED
   │   （人工介入或丢弃）
   ↓
 结束
```

#### 删除操作特殊处理

```kotlin
// data/repository/DocumentRepository.kt —— V2.1 删除流程
suspend fun deleteDocument(doc: DocumentEntity) {
    // 1. 本地立即删除（不可逆）
    val refsToDelete = segmentDao.getTextRefsByDocId(doc.id)
    segmentDao.deleteByDocId(doc.id)
    documentDao.delete(doc)
    // 2. 同步清除密文（§1.7 安全约束）
    refsToDelete.forEach { securePrefs.removeCipherText(it) }
    // 3. 排队云端删除
    pendingSyncDao.insert(PendingSyncEntity(
        id = UUID.randomUUID().toString(),
        operation = SyncOp.DELETE_DOC,
        localRefId = doc.id,
        payloadJson = """{"doc_id": "${doc.id}"}""",
        createdAt = System.currentTimeMillis()
    ))
}
```

#### 冲突解决原则

- **本地优先**：UI 立即响应，删除/编辑操作不等待网络
- **最后写入胜出（LWW）**：服务端用 `updated_at` 字段做冲突检测
- **删除不复活**：本地删除的文档，后端收到 DELETE 后立即真删
- **创建可重试**：网络抖动导致 CREATE 失败，重试直到 SUCCESS；SUCCESS 后 `serverRefId` 回填

#### SyncCoordinator 触发点

```kotlin
// util/sync/SyncCoordinator.kt —— V2.1 简化为单一入口
class SyncCoordinator(
    private val pendingDao: PendingSyncDao,
    private val api: BlueLinkApi,
    private val appScope: CoroutineScope
) {
    fun start() {
        // 1. 监听网络恢复
        val cm = appContext.getSystemService(ConnectivityManager::class.java)
        cm.registerDefaultNetworkCallback(object : NetworkCallback() {
            override fun onAvailable(network: Network) {
                appScope.launch { drain() }
            }
        })
        // 2. 监听 App 切回前台
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                appScope.launch { drain() }
            }
        })
    }

    suspend fun drain() {
        val pending = pendingDao.listByStatus(SyncStatus.PENDING, SyncStatus.FAILED)
        for (op in pending) {
            pendingDao.updateStatus(op.id, SyncStatus.IN_FLIGHT)
            val result = runCatching { executeOp(op) }
            if (result.isSuccess) {
                pendingDao.updateStatus(op.id, SyncStatus.SUCCESS, serverRefId = result.getOrNull())
            } else {
                val newRetry = op.retryCount + 1
                if (newRetry >= 3) {
                    pendingDao.updateStatus(op.id, SyncStatus.FAILED, lastError = result.exceptionOrNull()?.message)
                } else {
                    pendingDao.updateStatus(op.id, SyncStatus.PENDING, retryCount = newRetry, lastError = result.exceptionOrNull()?.message)
                }
            }
        }
    }
}
```

---

## 6. Vivo 适配补充

> **覆盖范围**：V2.0 仅在厂商表中一笔带过，V2.1 锚定 Vivo 为目标设备后展开。

V2.1 目标设备以 Vivo 为主，针对 **OriginOS 4+（基于 Android 14）** 补充适配要点。

### 6.1 权限弹窗

| 权限 | OriginOS 4+ 行为 | V2.1 应对 |
|------|------------------|-----------|
| `RECORD_AUDIO` | 首次需手动授权；后台录音需开启"语音唤醒" | 申请时同步跳设置引导 |
| `CAMERA` | 标准运行时权限 | 同 Android 默认 |
| `POST_NOTIFICATIONS`（API 33+） | 默认关闭，需用户开启 | 设置页提供开关引导 |
| `READ_MEDIA_*`（API 33+） | 三态权限（拒绝/部分/全部） | 选图片时引导到 OriginOS "隐私-权限" |
| 后台弹出 Activity | 严格管控 | 用 `FLAG_ACTIVITY_NEW_TASK` 启动设置页 |
| `READ_EXTERNAL_STORAGE` | API 33+ 弃用，改用 Photo Picker | 导入文件用 `ActivityResultContracts.OpenDocument` |

### 6.2 后台限制

Vivo 后台管理（自启动、省电模式、高耗电提醒）会强杀后台。

| 场景 | V2.1 应对 |
|------|----------|
| 后台 30 秒无活动被冻结 | 不接 WorkManager；同步走 `ProcessLifecycleOwner.ON_START` 触发（见 §5.5） |
| 离线 `pending_sync` 同步 | App 切回前台时立即拉起 `SyncCoordinator.drain()` |
| 推送 | V2.1 不接 IM 推送；所有提醒走本地 Notification |
| 录音中后台被杀 | 录音中用 **ForegroundService** 保持（V2.1 必加） |
| ML Kit 调用 | 已替换为 PaddleOCR，不再受 GMS 限制 |

#### ForegroundService 录音服务

```kotlin
// service/RecordingService.kt —— V2.1 必加
class RecordingService : Service() {
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = NotificationCompat.Builder(this, "bluelink_capture")
            .setSmallIcon(R.drawable.ic_mic)
            .setContentTitle("蓝链正在录音")
            .setOngoing(true)
            .build()
        startForeground(NOTIFICATION_ID, notification)
        // ... SpeechRecognizer 启动
        return START_STICKY
    }
    // ... onBind / onDestroy 略
}
```

`AndroidManifest.xml` 注册：
```xml
<service
    android:name=".service.RecordingService"
    android:foregroundServiceType="microphone"
    android:exported="false" />
```

### 6.3 引导页（V2.1 新增第 4 屏）

V2.0 三屏引导 + 隐私授权。V2.1 增加 **"为获得完整体验，请将蓝链加入电池优化白名单"**：

```kotlin
// ui/onboarding/OnboardingScreen.kt —— V2.1 第 4 屏
@Composable
fun OnboardingBatteryPage(
    onSkip: () -> Unit,
    onConfirm: () -> Unit
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Image(painter = painterResource(R.drawable.illu_battery),
              contentDescription = null, modifier = Modifier.size(200.dp))
        Spacer(Modifier.height(32.dp))
        Text("保持后台同步", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text("为确保灵感卡片能及时同步到云端，请将蓝链加入电池优化白名单",
             style = MaterialTheme.typography.bodyLarge,
             color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(48.dp))
        Button(onClick = {
            openBatteryOptimizationSettings(context)
            onConfirm()
        }, modifier = Modifier.fillMaxWidth().height(48.dp)) {
            Text("去设置")
        }
        TextButton(onClick = onSkip, modifier = Modifier.fillMaxWidth()) {
            Text("稍后再说")
        }
    }
}

// 跳转电池优化设置
fun openBatteryOptimizationSettings(context: Context) {
    try {
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
            .setData(Uri.parse("package:${context.packageName}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    } catch (e: Exception) {
        // Vivo/部分厂商不支持此 Intent，降级到通用电池设置
        context.startActivity(
            Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}
```

### 6.4 通知样式

OriginOS 对通知分组较敏感，所有通知走统一 `NotificationChannel`：

```kotlin
// BlueLinkApp.kt
private fun createNotificationChannels() {
    val nm = getSystemService(NotificationManager::class.java)
    nm.createNotificationChannel(
        NotificationChannel("bluelink_sync", "同步与备份",
            NotificationManager.IMPORTANCE_LOW)
            .apply { description = "灵感卡片与文档同步状态" }
    )
    nm.createNotificationChannel(
        NotificationChannel("bluelink_capture", "灵感捕获",
            NotificationManager.IMPORTANCE_DEFAULT)
            .apply { description = "OCR 识别与费曼评估结果" }
    )
    nm.createNotificationChannel(
        NotificationChannel("bluelink_recording", "录音中",
            NotificationManager.IMPORTANCE_LOW)
            .apply { description = "语音灵感录入前台服务" }
    )
}
```

---

## 7. V2.1 vs V2.0 决策差异速查表

| 维度 | V2.0 | V2.1 |
|------|------|------|
| **加密方案** | Tink + Room AES 字段（与详细设计矛盾） | **SecurePrefs 间接存储**（统一） |
| **Room 原文** | 文档中前后矛盾 | **Room 仅存 ref + 摘要**（统一） |
| **OCR 库** | ML Kit Text Recognition | **PaddleOCR Lite**（零 GMS 依赖） |
| **GMS 依赖** | 强依赖（ML Kit） | **零依赖** |
| **APK 体积** | < 30MB | **不设限** |
| **内存占用** | < 200MB | < 350MB（放宽） |
| **后台耗电** | < 5%/h | < 8%/h（放宽） |
| **鉴权方式** | Bearer Token（无登录实现） | **匿名 UUID + X-User-Id Header** |
| **目标设备** | 通用 Android 7.0+ | **Vivo 高端机 + 主流旗舰**（12GB+ RAM） |
| **PDF 渲染** | 选型表写 PdfRenderer | **MVP 纯文本模式**，V2.x 再升级 |
| **部署配置** | 散落各章 | **`.env` 集中** + BuildConfig 注入 |
| **Seed 数据** | 无 | **首次启动自动 seed** |
| **隐私分级 UI** | 后端 enum，无前端 | **ModalBottomSheet + 确认对话框** |
| **同步冲突** | 无 | **LWW + 删除不复活 + 状态机** |
| **Vivo 适配** | 仅厂商表一笔 | **权限/后台/引导/通知 4 节补全** |
| **ForegroundService 录音** | 未提及 | **V2.1 必加**（Vivo 后台管控） |
| | **V2.1+ 新增（2026-06-21）** | |
| **数据库** | SQLite + numpy 向量 | **PostgreSQL 17 + pgvector**（本地可回退 SQLite） |
| **LLM 配置** | Moonshot/DeepSeek 硬编码分支 | **LLMConfig 纯配置驱动**，改 .env 换厂商 |
| **部署方案** | uvicorn 裸启动 | **Docker Compose**（pgvector/pg17 + FastAPI） |

---

## 8. V2.1 文件改动清单

> V2.1 **不新增/删除** V2.0 既有文档文件，**只在代码与配置上做以下改动**：

| 改动类型 | 文件路径 | 操作 | 说明 |
|----------|----------|------|------|
| 加密 | `data/local/entity/SegmentEntity.kt` | 修改 | `text` → `textRef` + `textSnippet` |
| 加密 | `data/local/entity/InspirationCardEntity.kt` | 修改 | `content` → `contentRef` + `contentSnippet` |
| 加密 | `data/local/crypto/SecurePrefs.kt` | 扩展 | 新增 `putCipherText` / `getCipherText` / `removeCipherText` / `Keys` |
| 加密 | `data/local/crypto/AeadEngine.kt` | 新增 | Keystore AES-GCM 加解密封装 |
| 加密 | `data/repository/SegmentRepository.kt` | 修改 | 写入加密，读取解密，删除同步 |
| 加密 | `data/repository/InspirationRepository.kt` | 修改 | 同上 |
| 加密 | `data/repository/SecureDeleteHelper.kt` | 新增 | §1.7 删除安全包装 |
| OCR | `gradle/libs.versions.toml` | 替换 | 移除 ML Kit，新增 onnxruntime |
| OCR | `data/local/ocr/PaddleOcrDataSource.kt` | 新增 | 替换 MlKitOcrDataSource |
| OCR | `data/local/ocr/MlKitOcrDataSource.kt` | 删除 | 已被 PaddleOCR 替代 |
| OCR | `app/src/main/assets/models/ch_PP-OCRv3/` | 新增资源 | 3 个 ONNX 模型 + 字典 |
| OCR | `AppContainer.kt` | 修改 | `mlKitOcr` → `paddleOcr` |
| 鉴权 | `data/identity/UserIdentity.kt` | 新增 | §4.2 匿名 UUID |
| 鉴权 | `data/remote/AuthInterceptor.kt` | 修改 | Bearer → X-User-Id |
| 鉴权 | `AppContainer.kt` | 修改 | 注入 `UserIdentity` |
| 鉴权 | `server/app/core/auth.py` | 替换 | `get_user_id` 依赖 |
| 鉴权 | `server/app/api/*.py` | 修改 | 所有路由 `Depends(get_user_id)` |
| 工程 | `server/.env.example` | 新增 | §5.1 完整环境变量 |
| 工程 | `server/scripts/seed.py` | 新增 | §5.2 自动 seed |
| 工程 | `server/app/main.py` | 修改 | startup 事件调 seed |
| 工程 | `ui/component/PrivacyPickerSheet.kt` | 新增 | §5.3 隐私弹窗 |
| 工程 | `ui/component/ConfirmPrivacyChangeDialog.kt` | 新增 | §5.3 二次确认 |
| 工程 | `data/local/entity/DocumentEntity.kt` | 修改 | 新增 `pageCount` + `hasOriginalImage` |
| 工程 | `data/local/entity/PendingSyncEntity.kt` | 修改 | 新增 `serverRefId` / `lastError` / `status` |
| 工程 | `domain/model/SyncStatus.kt` | 新增 | PENDING / IN_FLIGHT / SUCCESS / FAILED |
| 工程 | `domain/model/SyncOp.kt` | 修改 | 新增 DELETE_DOC / DELETE_CARD |
| 工程 | `util/sync/SyncCoordinator.kt` | 新增 | §5.5 状态机实现 |
| Vivo | `service/RecordingService.kt` | 新增 | §6.2 前台录音 |
| Vivo | `AndroidManifest.xml` | 修改 | 注册 ForegroundService |
| Vivo | `ui/onboarding/OnboardingScreen.kt` | 修改 | 新增电池优化引导页 |
| Vivo | `util/IntentExt.kt` | 新增 | `openBatteryOptimizationSettings` |
| Vivo | `BlueLinkApp.kt` | 修改 | `createNotificationChannels()` |

> V2.0 文档（产品需求 V2.0、详细设计 V2.0、UI&UX V2.0）**保持不变**，作为历史参考。V2.1 与 V2.0 并存。

---

## 9. 后续架构修订（V2.1+）

> 以下修订在 V2.1 定稿后于 2026-06-21 确认，覆盖后端架构与部署方案。
> **生效范围**：替代 V2.0 详细设计 §3.2 / §9.1.5 / §9.7 / §13.2 及 V2.1 §5.1 中相关条款。

### 9.1 数据库：SQLite → PostgreSQL + pgvector

#### 9.1.1 决策

**后端数据库从 SQLite 迁移至 PostgreSQL 17，向量存储从 numpy npy 文件迁移至 pgvector 扩展。**

#### 9.1.2 选型对比

| 维度 | SQLite（V2.1 原方案） | PostgreSQL + pgvector |
|------|----------------------|----------------------|
| 数据模型 | 单文件，无并发 | 连接池（pool_size=10），多请求并发 |
| 向量检索 | numpy 进程内余弦相似度 | pgvector SQL 级 `<->` 算子，支持索引 |
| 运维 | 零运维 | 需 Docker 或云服务 |
| 本地开发 | ⚡ 零配置 | 回退 SQLite 模式（改 DATABASE_URL 即可） |
| 多设备同步 | 不支持 | 天然支持（单数据源） |
| 生产部署 | ❌ 不适用 | ✅ 标准方案 |

#### 9.1.3 自动选择机制

```python
# DATABASE_URL 以 postgresql:// 开头 → PostgreSQL + 连接池
# DATABASE_URL 以 sqlite:/// 开头   → SQLite（本地开发/测试用）
```

本地开发仍可零配置使用 SQLite，切换只需改 `.env` 一行。

#### 9.1.4 向量存储分层

```
VectorStore（抽象接口）
  ├── PgVectorStore（生产）— pgvector 表，SQL 级向量检索
  └── NumpyVectorStore（回退）— 原 numpy npy 方案，保留兼容
```

通过 `create_vector_store()` 工厂函数根据 DATABASE_URL 自动选择实现。

### 9.2 LLM Provider：厂商解耦

#### 9.2.1 决策

**移除 LLMProvider 内部对 Moonshot / DeepSeek 的硬编码分支，改为纯配置驱动。**

#### 9.2.2 配置扁平化

```bash
# 旧（V2.1）：每个厂商一段配置，代码里写 if/else
MOONSHOT_API_KEY=xxx
DEEPSEEK_API_KEY=xxx
LLM_PROVIDER=moonshot

# 新（V2.1+）：纯 OpenAI 兼容协议，厂商无关
LLM_API_KEY=sk-xxx
LLM_BASE_URL=https://api.deepseek.com/v1
LLM_CHAT_MODEL=deepseek-chat
LLM_EMBED_MODEL=embedding-v1
```

#### 9.2.3 新增厂商示例

| 厂商 | LLM_BASE_URL | LLM_CHAT_MODEL |
|------|-------------|---------------|
| DeepSeek | `https://api.deepseek.com/v1` | `deepseek-chat` |
| Moonshot | `https://api.moonshot.cn/v1` | `moonshot-v1-8k` |
| OpenAI | `https://api.openai.com/v1` | `gpt-4o` |
| 硅基流动 | `https://api.siliconflow.cn/v1` | `Qwen/Qwen2.5-7B-Instruct` |

#### 9.2.4 架构变化

```
旧：LLMProvider 内部持 settings.MOONSHOT_*/settings.DEEPSEEK_* 分支
新：LLMProvider 构造时接收 LLMConfig（纯数据类），内部零厂商判断
   └── get_llm() 工厂从 Settings 读取值构造 LLMConfig
```

### 9.3 部署：Docker 容器化

#### 9.3.1 新增文件

```
server/
├── Dockerfile                   # Python 3.12-slim + uvicorn
├── docker-compose.yml           # pgvector/pg17 + FastAPI
├── .dockerignore                # 排除 pycache / .env / .git
└── .env.example                 # 环境变量模板（可入版本库）
```

#### 9.3.2 使用方式

```bash
# 生产部署
cd server
docker compose up -d
# → PostgreSQL 起在 5432，FastAPI 起在 8000

# 查看日志
docker compose logs -f

# 停止（保留数据）
docker compose down

# 停止并清空数据
docker compose down -v

# 本地开发（不依赖 Docker）
uvicorn app.main:app --reload
# → 需本地有 PostgreSQL 或 .env 切 SQLite
```

#### 9.3.3 部署架构

```
                         Nginx（可选反代）
                              │
                         ┌────┴────┐
                         │  FastAPI │ :8000
                         └────┬────┘
                              │ depends_on (healthy)
                         ┌────┴────┐
                         │ pgvector│ :5432
                         │  PG 17  │
                         └─────────┘
```

### 9.4 受影响章节对照

| 原文档章节 | 原内容 | 替代为 |
|-----------|--------|--------|
| V2.0 详细设计 §3.2 后端技术选型 | SQLite + numpy | PostgreSQL + pgvector（本地开发可回退 SQLite） |
| V2.0 详细设计 §9.1.5 LLM Provider | Moonshot/DeepSeek 硬编码 | LLMConfig 纯配置驱动 |
| V2.0 详细设计 §9.7 伪代码骨架 | LLMProvider 构造传特定厂商参数 | `get_llm().chat_json(...)` 单例调用 |
| V2.0 详细设计 §13.2 后端目录 | 无 Docker 相关文件 | 新增 Dockerfile / docker-compose.yml |
| V2.1 §5.1 部署环境变量 | 分 Moonshot/DeepSeek 区块 | 扁平化为 LLM_API_KEY / LLM_BASE_URL / LLM_CHAT_MODEL |

---

## 10. 变更记录

| 版本 | 日期 | 修订内容 | 作者 |
|------|------|----------|------|
| 2.0 | 2026-06-05 | 客户端栈重构为 Android Kotlin + Jetpack Compose | Eau团队 |
| **2.1** | **2026-06-07** | **加密策略统一（SecurePrefs 间接存储）；OCR 改 PaddleOCR Lite（零 GMS 依赖）；取消 APK 体积限制（高端机不设限）；登录改匿名 UUID + X-User-Id；新增 5 个工程化章节（部署/Seed/隐私 UI/PDF 渲染/同步冲突）；新增 Vivo 适配专章** | **Eau团队** |
| **2.1+** | **2026-06-21** | **后端架构修订：数据库 SQLite → PostgreSQL + pgvector；LLM Provider 厂商解耦为纯配置驱动；新增 Docker 容器化部署方案** | **Eau团队** |

---

**V2.1 文档结束**

> 本版本是 V2.0 的 **修订与补充**，与 V2.0 共同构成完整技术档案。代码改动以 §8 文件清单为准；如与 V2.0 文档存在冲突，**以 V2.1 为准**。
