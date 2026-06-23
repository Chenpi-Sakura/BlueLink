**版本**：1.0  
**日期**：2026年5月26日  
**团队**：Eau  
**产品定位**：端侧AI认知溯源引擎——不生产答案，只提供直达真相的最短捷径。

---
## 1. 引言
### 1.1 文档目的
本文档基于《蓝链 BlueLink 产品需求文档 V1.0》及比赛快速落地要求，定义系统的技术实现方案、模块划分、接口规范、数据模型以及关键算法流程。新选型充分考虑了开发效率与演示效果，优先采用 uni-app 跨端框架、云端大模型 API 与 Python FastAPI 后端，在保证核心创新可见的前提下，实现最短交付路径。

### 1.2 设计原则
- **演示优先，体验完整**：确保评委可直接扫码体验小程序，核心交互如锚点跳转、聚光灯阅读、知识图谱呈现无删减。
- **隐私兜底，渐进增强**：初始版本通过客户端加密存储 + 云端 API 脱敏调用实现数据保护，未来可平滑下沉端侧模型。
- **AI 模拟，逻辑不变**：用云端大模型 API 精准复现“只给路径不给答案”的溯源逻辑，不因工程简化而弱化产品理念。
- **可扩展**：接口设计预留端侧模型替代方案，云服务模块化，便于决赛阶段升级。

## 2. 系统架构设计
### 2.1 总体架构
```
┌──────────────────────────────────────────────────────────────┐
│           客户端：uni-app（iOS / Android / 微信小程序）         │
│ ┌────────────┐  ┌──────────────┐  ┌────────────────────────┐ │
│ │  UI 层     │  │ 页面逻辑层   │  │ 本地数据与缓存          │ │
│ │ (Vue 组件)│  │ (Vuex/Pinia) │  │ (uni.storage/加密文件)  │ │
│ └─────┬──────┘  └──────┬───────┘  └───────────┬────────────┘ │
│       │                │                     │              │
│ ┌─────┴────────────────┴─────────────────────┴──────────────┐ │
│ │              Service 层（API 封装、锚点解析、图谱数据适配）  │ │
│ └───────────────────────────────┬───────────────────────────┘ │
└─────────────────────────────────┼─────────────────────────────┘
                                  │ HTTPS (TLS 1.3)
┌─────────────────────────────────┼─────────────────────────────┐
│                     后端服务：Python + FastAPI                 │
│ ┌──────────┐  ┌────────────┐  ┌──────────┐  ┌──────────────┐ │
│ │ 路由控制 │  │ 业务逻辑层 │  │ AI 调度器 │  │ 数据持久层   │ │
│ │ (APIRouter)│ │ (Services)│  │ (Moonshot│  │ (SQLite/    │ │
│ │          │  │            │  │ /DeepSeek)│  │ 向量库插件) │ │
│ └──────────┘  └────────────┘  └──────────┘  └──────────────┘ │
└───────────────────────────────────────────────────────────────┘
```

### 2.2 关键说明
- **客户端**：uni-app 负责全部 UI 与用户交互，本地存储文档原文（加密）、灵感卡片、图谱数据（轻量 JSON）。不直接调用大模型，所有 AI 请求通过后端。
- **后端**：FastAPI 提供 RESTful API，集成 Moonshot/DeepSeek SDK，实现文档解析、切片、去重、锚点生成、费曼评估、图谱关系发现等核心 AI 逻辑。
- **隐私保障**：用户私有内容（灵感、标记私密的文档）仅上传脱敏摘要或向量片段进行必要计算，原文不离开客户端。客户端加密本地存储。


## 3. 技术选型
### 3.1 客户端（前端）
| 维度 | 选择 | 理由 |
|------|------|------|
| 跨端框架 | **uni-app** (Vue 3) | 一套代码同时发布微信小程序、iOS、Android；学习成本低，内置 uView/uni-ui 组件库，适配评委扫码体验。 |
| 状态管理 | Pinia | 轻量、模块化，替代 Vuex。 |
| 本地加密存储 | uni.storage + crypto-js | 对灵感、私密文档进行 AES 加密后再存入本地文件系统。 |
| 图谱可视化 | **ECharts** 关系图 或 **AntV G6** (内嵌 WebView) | 直接喂 JSON 数据即可生成带拖拽、缩放的知识图谱，无需自研布局引擎。 |
| UI 组件 | uView Plus | 提供瀑布流、卡片、折叠面板等现成组件，契合“米白+克莱因蓝”设计风格。 |

### 3.2 后端服务
| 维度        | 选择                                   | 理由                                            |
| --------- | ------------------------------------ | --------------------------------------------- |
| Web 框架    | **FastAPI**                          | 异步高性能，自动生成 Swagger 文档，Python 生态强大。            |
| 大模型 API   | **Moonshot (Kimi)** / **DeepSeek**   | Kimi 长文本处理一流，DeepSeek 极致性价比且接口兼容 OpenAI。按需切换。 |
| 向量数据库（可选） | **ChromaDB** 轻量嵌入式 或 **Milvus Lite** | 用于文档切片语义检索，非必需但可提升去重和检索精度。                    |
| 文档解析      | PyMuPDF (PDF), python-docx, markdown | 服务端负责将上传的文档转译为分段文本。                           |
| 任务队列（可选）  | Celery + Redis                       | 处理耗时去重、图谱计算等异步任务。                             |
| 对象存储（可选）  | 本地文件系统或 MinIO                        | 若需存储非敏感文档原文，用于后续去重比对。                         |

### 3.3 部署方案
- 后端部署在自有服务器（Linux），通过 Nginx 反代，开放 HTTPS。
- 客户端 uni-app 打包为微信小程序（体验版），通过条件编译可同时生成 App 安装包。
- 数据库使用 SQLite（轻量，单机足够演示）。


## 4. 模块划分与职责
### 4.1 客户端模块
| 模块名称 | 职责 | 关键接口（内部方法/Store） |
|----------|------|---------------------------|
| DocumentStore | 管理文档列表、上传、解析后的分段数据缓存 | `loadDocs()`, `uploadFile(file)`, `getSegments(docId)` |
| AnchorService | 调用后端获取锚点卡片，本地缓存锚点与段落的映射 | `askQuestion(text, granularity) → anchorList` |
| ReaderController | 控制聚光灯模式状态、折叠状态、书签 | `enterSpotlight(segId)`, `toggleFold(segId)` |
| GraphAdapter | 将后端返回的图谱 JSON 转为 ECharts/G6 可渲染的格式 | `buildGraphData(nodes, edges) → option` |
| CaptureService | 快捷栏文字/语音/图片录入，转为灵感卡片，上传后端 | `capture(type, content) → card` |
| PrivacyManager | 负责客户端数据分级、加密、脱敏预处理 | `classify(doc)`, `encrypt(data)`, `sanitizeForUpload(text)` |
| SyncProxy | 模拟与后端的同步，可对接 iCloud/WebDAV（后期） | `syncLocalToCloud()` |

### 4.2 后端模块
| 模块 | 职责 | 关键类/函数 |
|------|------|-------------|
| DocumentService | 解析上传文档，分段、清洗、基础元数据提取 | `parse_and_chunk(file) → [Segment]` |
| DedupService | 比对历史文档，计算重复段与信息增量，支持异步任务 | `compute_delta(new_doc_id) → DeltaReport` |
| AnchorEngine | 接收用户提问，调用大模型生成引言和锚点卡片 | `generate_anchors(question, segments, granularity) → AnchorResponse` |
| FeynmanEvaluator | 调用大模型比较用户复述与原文，输出偏差与引力线 | `evaluate(user_text, orig_segments) → DeviationResult` |
| GraphBuilder | 抽取实体、计算关系、生成图谱数据 | `build_graph(user_id) → GraphData` |
| PrivacyProxy | 负责接收客户端脱敏数据，标记敏感级别，强制后端不落盘原文 | `handle_data(payload, privacy_level)` |
| API Router | 定义各端点，鉴权（简单 Token），请求/响应格式校验 | `/api/v1/*` 路由 |


## 5. 端云协同机制
### 5.1 数据分级与传输原则
延续 PRD 分级：`LOCAL_ONLY`、`LOCAL_FIRST`、`CLOUD_OK`。

- **LOCAL_ONLY（灵感、私密标记文档）**：客户端绝不将原文上传。若需要进行语义理解，客户端提取关键词并生成局部 SimHash（不含原文）发送后端，或完全本地用规则匹配替代。
- **LOCAL_FIRST（默认文档）**：上传时客户端发送文档分段文本给后端，但后端处理完后立即删除原始文本，仅保留结构化的分段索引和加密向量。
- **CLOUD_OK（用户主动标记公开）**：可全量文本留存在服务器供后续分析。

### 5.2 AI 调用流程中的隐私保护
提问溯源时，用户问题与原文锚点如何产生？
1. 客户端发送用户提问 `q` 与当前知识库中**非私密**文档的分段 ID 列表。
2. 后端检索相关分段（已存储的分段文本或向量），调用大模型生成锚点。
3. 对于私密文档，客户端本地维护倒排索引或轻量关键词索引。客户端自行检索出相关分段 ID，然后**仅将分段 ID 和脱敏的关键词**发给后端，后端根据 ID 调取已存储的向量（若有）或从客户端回传的脱敏特征进行匹配，最终返回锚点卡片信息。此路径可先简化：初赛阶段，私密文档的溯源可通过客户端本地简单匹配实现，后端仅处理非私密部分。

### 5.3 离线支持
- 客户端缓存最近使用的锚点、分段文本，在无网络时仍可浏览已下载的文档与历史对话。
- 纯离线提问只支持基于本地关键词的简单搜索，不生成 AI 引言。


## 6. 数据模型设计
### 6.1 客户端本地数据结构（uni.storage）
```javascript
// 文档列表
docs: [
  {
    id: 'uuid',
    title: 'string',
    privacyLevel: 'LOCAL_ONLY' | 'LOCAL_FIRST' | 'CLOUD_OK',
    segments: [ { id, index, text, folded, vector? } ], // vector 可缺省
    localFilePath: 'string'
  }
]

// 灵感卡片
cards: [
  {
    id: 'uuid',
    content: 'string',
    type: 'text'|'voice'|'image',
    privacyLevel: 'LOCAL_ONLY',
    tags: [],
    createdAt: 'timestamp'
  }
]

// 图谱缓存
graphCache: {
  nodes: [{ id, label, type, refId }],
  edges: [{ source, target, relation, confidence }]
}

// 用户画像
profile: {
  major: 'string',
  interests: [],
  terminology: {},
  style: 'gentle'|'direct'
}
```

### 6.2 后端数据库表（SQLite）
```sql
-- 文档元数据（仅非私密）
CREATE TABLE documents (
    id TEXT PRIMARY KEY,
    user_id TEXT,
    title TEXT,
    privacy_level TEXT,
    created_at INTEGER
);

-- 切片信息（文本可加密存储）
CREATE TABLE segments (
    id TEXT PRIMARY KEY,
    doc_id TEXT,
    index_num INTEGER,
    text_encrypted TEXT,  -- 对 LOCAL_FIRST 文本加密
    vector BLOB,           -- 向量（可选）
    is_folded INTEGER DEFAULT 0
);

-- 锚点缓存
CREATE TABLE anchors (
    id TEXT PRIMARY KEY,
    query_hash TEXT,
    segment_id TEXT,
    snippet TEXT,
    score REAL
);

-- 图谱节点与边
CREATE TABLE graph_nodes (
    id TEXT PRIMARY KEY,
    label TEXT,
    type TEXT,
    ref_id TEXT
);
CREATE TABLE graph_edges (
    id TEXT PRIMARY KEY,
    source_id TEXT,
    target_id TEXT,
    relation_type TEXT,
    confidence REAL,
    is_manual INTEGER DEFAULT 0
);
```

## 7. 接口设计
### 7.1 后端 API 列表
所有接口均以 `/api/v1` 为前缀，请求需带 Header `Authorization: Bearer <user_token>`。

**文档相关**
- `POST /documents/upload`  
  上传文档文件，返回 `document_id` 与解析状态。  
  Body: FormData (file + privacy_level)

- `GET /documents/{doc_id}/segments`  
  获取某文档所有分段（摘要，不含原文）。  
  Response: `{ segments: [{ id, index, summary, folded }] }`

**溯源提问**
- `POST /questions/ask`  
  核心接口。  
  Body:
  ```json
  {
    "query": "用户问题",
    "granularity": "sentence" | "paragraph",
    "scope_ids": ["doc_id1","doc_id2"] // 可选，限定搜索范围
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

**去重与增量**
- `POST /documents/{doc_id}/compute_delta`  
  异步触发去重计算，返回 task_id，后续可通过 `GET /tasks/{task_id}` 查询结果。

**费曼伴学**
- `POST /feynman/evaluate`  
  Body:
  ```json
  {
    "user_explanation": "用户用自己的话解释的内容",
    "target_concept": "概念名称",
    "context_segment_ids": ["seg1","seg2"]
  }
  ```
  Response:
  ```json
  {
    "deviations": [
      {
        "user_segment": "用户表述中可能有误的部分",
        "deviation_type": "contradiction" | "missing",
        "original_snippet": "原文对应片段",
        "anchor_segment_id": "seg-xxx"
      }
    ],
    "summary": "整体评估...",
    "gravity_lines": [...] // 前端绘制引力线的数据
  }
  ```

**知识图谱**
- `GET /graph?user_id=xxx`  
  返回全量图谱节点与边。

**灵感卡片**
- `POST /cards`  
  上传灵感卡片文本（脱敏），后端进行关键词提取、关联发现，返回卡片 ID 和候选链接。

### 7.2 客户端与后端数据交互原则
- 私密文档的原文绝不通过 API 传输。客户端通过 `PrivacyManager` 对私密文档片段进行本地索引，提问时仅将脱敏关键词发给后端，后端配合向量检索（若存在脱敏向量）返回可能的锚点，客户端二次过滤。
- 初赛实现简化：私密文档单独存放在本地，其提问溯源完全由客户端本地规则处理，不经过后端。非私密文档全量走 API。


## 8. 核心功能流程设计
### 8.1 提问溯源流程（重点）
1. 用户在 uni-app 对话页输入问题，选择溯源粒度。
2. 客户端根据当前聚焦的知识库范围（用户可选“全部文档”、“某文集”），收集所有**非私密**文档 ID，加上提问字符串，POST `/questions/ask`。
3. 后端检索相关知识库分段，调用 Moonshot API（Prompt 设计见附录），强制模型仅输出 JSON，包含“引言”与“锚点列表”。
4. 后端返回 JSON，客户端解析为锚点卡片列表。
5. 用户点击锚点卡片，客户端根据 `segment_id` 查找本地缓存的分段（或从后端拉取该分段全文用于展示），跳转至阅读器，触发聚光灯模式。
6. 若提问范围包含私密文档，客户端独立执行本地关键词搜索，生成简化版锚点（无AI引言），与云端锚点合并展示。

### 8.2 入库去重流程
1. 用户选择文档上传，客户端标注隐私级别。
2. 若文档为 `LOCAL_ONLY`，仅本地解析、切片、加密存储，不触发云端去重。
3. 若为 `LOCAL_FIRST` 或 `CLOUD_OK`，文件上传至后端。
4. 后端 `DocumentService` 解析文档并分段，暂存分段文本。
5. 调用 `DedupService`，与同一用户历史非私密分段进行相似度比较（使用 SimHash + 语义向量）。生成 `DeltaReport`。
6. 后端将新分段（含重复标记）加密存储，若文档权限为 `LOCAL_FIRST`，删除原始文本，仅保留向量与摘要。
7. 返回 `DeltaReport` 给客户端，客户端据此显示折叠状态。

### 8.3 费曼伴学偏差检测流程
1. 用户进入费曼模式，选择某概念。客户端从本地或后端获取该概念关联的原文分段（最多3段）。
2. 用户语音或文字输入解释。
3. 客户端调用 `/feynman/evaluate`，传递用户解释和原文分段 ID。
4. 后端调取分段文本，构造 Prompt，让大模型比较解释与原文，找出矛盾或遗漏，并以结构化 JSON 返回。
5. 客户端拿到偏差列表，为每个偏差生成引力线数据，并在阅读器中高亮对应原文位置。

### 8.4 知识图谱更新流程
- 文档入库或灵感捕获后，客户端通知后端。
- 后端异步执行：实体抽取（调用大模型或简单 NER）、关系发现、更新图谱数据库。
- 客户端定期拉取 `/graph` 接口，或通过 WebSocket 接收更新通知，刷新 ECharts 渲染。


## 9. 关键算法与数据结构
### 9.1 锚点生成 Prompt 模板（以 Moonshot 为例）
```
你是一个只提供原文线索而不直接给出答案的助手。用户的问题基于他的个人知识库。
请根据提供的文档片段，完成两件事：
1. 用不超过三句话写一个引导引言，指出问题的关键矛盾或值得注意之处，但不给出最终结论。
2. 找出能够回答该问题的原文片段，为每一个片段生成一个锚点卡片信息，包含：文档标题、片段前20字、片段ID。

必须以如下JSON格式返回，不要包含任何多余文字：
{
  "introduction": "引导引言",
  "anchors": [
    {
      "doc_title": "文档标题",
      "snippet": "原文片段前20字...",
      "segment_id": "对应的分段ID",
      "score": 0.0~1.0 的相关性评分
    }
  ]
}
```
后端将文档分段文本作为 context 填入。

### 9.2 去重算法
- 预处理：每段文本计算 SimHash（64-bit），使用 `simhash` Python 库。
- 匹配：汉明距离 ≤ 3 的候选对，再计算 Jaccard 相似度或语义向量余弦相似度（阈值 0.9），确认为重复。
- 合并：连续重复段合并为折叠区间。

### 9.3 局部隐私保护检索
当私密文档需要溯源时，客户端使用以下轻量方案：
- 将所有私密文档分段，建立倒排索引（分词后构建）。
- 用户提问经过相同分词，检索匹配的分段 ID。
- 仅将匹配的分段 ID 发送给后端（或直接本地展示锚点，不生成 AI 引言），后端返回这些分段 ID 对应的摘要（如果之前通过脱敏向量同步过），否则客户端直接使用原文片段生成简单锚点。

### 9.4 知识图谱可视化
ECharts 关系图配置示例核心：
```javascript
option = {
  series: [{
    type: 'graph',
    layout: 'force',
    data: nodes, // {id, name, symbolSize, category}
    links: edges, // {source, target, label}
    roam: true,
    focusNodeAdjacency: true,
    force: { repulsion: 200 }
  }]
}
```


## 10. 隐私与安全设计
### 10.1 数据保护措施
- **传输加密**：全链路 HTTPS。
- **本地存储加密**：uni.storage 中敏感字段使用 AES-256-CBC 加密，密钥由用户生物特征或口令派生。
- **后端不落原文**：对于 `LOCAL_FIRST` 文档，处理完即刻删除分段原文，仅保留加密摘要和向量。
- **API 鉴权**：简单的用户 Token 机制，每个用户独立隔离。
- **脱敏上传**：灵感卡片上传后端用于关联推荐时，客户端自动替换人名、具体数字为占位符。

### 10.2 用户数据控制
- 提供“数据导出”功能，将所有本地加密数据以明文形式导出（需用户二次验证）。
- 提供“永久删除”选项，服务器端清空对应用户所有数据。


## 11. 异常处理与降级策略
- **大模型 API 超时/不可用**：后端重试3次后，返回错误码，客户端提示“AI 暂时无法响应，请稍后重试”，并展示本地关键词匹配的锚点。
- **文档解析失败**：返回具体错误信息，如“PDF 文件已加密，无法解析”。
- **去重任务过长**：异步执行，客户端显示“去重分析中，当前展示原文”，完成后刷新。
- **图谱数据量过大**：后端限制最多返回 500 个节点及一阶边，客户端使用节点度裁剪，保持渲染流畅。


## 12. 非功能特性实现方案
### 12.1 性能
- 利用 uni-app 的 `v-if` 和虚拟列表优化瀑布流渲染。
- 后端对大模型调用做缓存（同一问题+同一知识库指纹，1小时内复用结果）。
- 锚点跳转定位借助 `scroll-into-view` 属性，结合 `createSelectorQuery` 实现。

### 12.2 兼容性
- 微信小程序基础库 2.20.0+。
- iOS 13+，Android 8+。
- 后端 FastAPI 基于 Python 3.10+，单机可承载 50+ 并发。

### 12.3 可扩展性
- 后端 API 路由按模块拆分，便于后续引入微服务。
- 大模型调用抽象为 `LLMProvider` 接口，可随时切换 Kimi/DeepSeek/端侧。
- 图谱可视化组件可平滑升级为 G6 以获得更丰富交互。


## 13. 附录
### 13.1 依赖库清单
**客户端（package.json）**
- vue: 3.x
- pinia: 2.x
- uview-plus: 最新
- crypto-js: 4.x

**后端（requirements.txt）**
- fastapi
- uvicorn
- openai (用于 DeepSeek 兼容调用)
- python-multipart
- PyMuPDF
- python-docx
- chromadb (可选)
- celery[redis] (可选)

### 13.2 项目目录结构
```
blue-link/
├── client/                # uni-app 项目
│   ├── pages/
│   │   ├── index/         # 瀑布流文库
│   │   ├── chat/          # 对话溯源
│   │   ├── reader/        # 智能阅读器
│   │   └── graph/         # 知识图谱
│   ├── store/             # Pinia 状态
│   ├── services/          # API 封装
│   └── utils/             # 加密、隐私处理工具
├── server/                # FastAPI 后端
│   ├── app/
│   │   ├── api/           # 路由
│   │   ├── services/      # 业务逻辑
│   │   ├── models/        # 数据库模型
│   │   └── llm/           # 大模型调用封装
│   ├── requirements.txt
│   └── main.py
└── docs/                  # 文档
```

### 13.3 比赛演示 Checklist
- [ ] 扫码进入小程序，首页瀑布流展示示例文档。
- [ ] 上传一篇 PDF，自动去重折叠演示。
- [ ] 对话页面提问，展示引言+锚点卡片。
- [ ] 点击锚点跳转，聚光灯高亮，退出恢复。
- [ ] 费曼模式，语音输入解释，看到偏差引力线。
- [ ] 知识图谱页，展示动态节点和连线，可缩放聚焦。
- [ ] 设置页体现隐私开关和认知分层选项。