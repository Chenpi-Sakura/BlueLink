# BlueLink API 接口清单

## 基本信息

| 项目 | 值 |
|------|-----|
| 基础地址 | `http://<backend>:8000/api/v1/` |
| 认证方式 | 匿名 UUID，通过 Header `X-User-Id` 传递 |
| 客户端标识 | `X-Client-Version`: 版本号, `X-Platform`: `"android"` |
| 数据格式 | JSON，UTF-8 |

---

## 1. 文档管理

### 1.1 上传文档

```
POST /api/v1/documents/upload
Content-Type: multipart/form-data
```

**请求参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| `file` | File | 是 | 上传的文件（PDF / TXT / Markdown） |
| `privacy_level` | String | 是 | `LOCAL_ONLY` / `LOCAL_FIRST` / `CLOUD_OK` |

**响应 `200`：**
```json
{
  "id": "uuid-string",
  "title": "文档标题",
  "privacyLevel": "LOCAL_FIRST",
  "source": "文件名.pdf",
  "createdAt": 1700000000000,
  "conceptBeacon": "概念路标（可选）"
}
```

### 1.2 获取文档列表

```
GET /api/v1/documents?cursor=<cursor>&limit=50
```

**查询参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| `cursor` | String | 否 | 分页游标（首次不传） |
| `limit` | Int | 否 | 每页数量，默认 50 |

**响应 `200`：**
```json
{
  "items": [
    {
      "id": "uuid",
      "title": "标题",
      "privacyLevel": "LOCAL_FIRST",
      "source": null,
      "createdAt": 1700000000000,
      "conceptBeacon": "概念路标"
    }
  ],
  "nextCursor": "下一页游标（null 表示没有更多）"
}
```

### 1.3 获取文档分段

```
GET /api/v1/documents/{doc_id}/segments
```

**路径参数：**

| 参数名 | 类型 | 说明 |
|--------|------|------|
| `doc_id` | String | 文档 UUID |

**响应 `200`：**
```json
{
  "segments": [
    {
      "id": "segment-uuid",
      "index": 0,
      "summary": "分段摘要/前30字",
      "folded": false
    }
  ]
}
```

### 1.4 计算增量（去重）

```
POST /api/v1/documents/{doc_id}/compute_delta
```

**路径参数：**

| 参数名 | 类型 | 说明 |
|--------|------|------|
| `doc_id` | String | 文档 UUID |

**响应 `200`：**
```json
{
  "foldedRanges": [
    {
      "segmentIndexStart": 3,
      "segmentIndexEnd": 7,
      "reason": "与已有文档《XXX》内容重复"
    }
  ],
  "newContentRatio": 0.85
}
```

---

## 2. 溯源提问

### 2.1 提问

```
POST /api/v1/questions/ask
Content-Type: application/json
```

**请求体：**
```json
{
  "query": "用户的问题",
  "granularity": "SENTENCE",
  "scopeDocIds": ["doc-id-1", "doc-id-2"],
  "localSegmentIds": ["local-seg-id-1"]
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `query` | String | 是 | 用户提问文本 |
| `granularity` | String | 否 | `"SENTENCE"` 句词级 / `"PARAGRAPH"` 文章级 |
| `scopeDocIds` | List\<String\> | 否 | 限定检索的文档范围（null=全部） |
| `localSegmentIds` | List\<String\> | 否 | 本地私密文档命中分段 ID 列表（仅 ID，不含原文） |

**响应 `200`：**
```json
{
  "introduction": "引导引言，最多3句",
  "anchors": [
    {
      "anchorId": "锚点唯一ID",
      "docTitle": "文档标题",
      "snippet": "片段前30字摘要",
      "segmentId": "对应分段ID",
      "score": 0.92,
      "isLocal": false
    }
  ]
}
```

---

## 3. 费曼伴学评估

### 3.1 提交费曼评估

```
POST /api/v1/feynman/evaluate
Content-Type: application/json
```

**请求体：**
```json
{
  "userExplanation": "用户用自己的话复述概念",
  "targetConcept": "目标概念名称",
  "contextSegmentIds": ["seg-id-1", "seg-id-2"]
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `userExplanation` | String | 是 | 用户对概念的理解/复述 |
| `targetConcept` | String | 是 | 要评估的概念（来自锚点 docTitle） |
| `contextSegmentIds` | List\<String\> | 是 | 原文分段 ID 列表，作为评估依据 |

**响应 `200`：**
```json
{
  "summary": "总体评估总结",
  "deviations": [
    {
      "userSegment": "用户表述中出问题的部分",
      "deviationType": "OMISSION",
      "explanation": "偏差说明",
      "originalSnippet": "原文对应片段",
      "anchorSegmentId": "关联分段ID"
    }
  ],
  "gravityLines": [
    {
      "from": 12,
      "toSegmentId": "seg-id"
    }
  ]
}
```

`deviationType` 取值：
- `OMISSION` — 遗漏
- `CONTRADICTION` — 矛盾
- `OVER_EXTENSION` — 过度推论

---

## 4. 知识图谱

### 4.1 获取图谱数据

```
GET /api/v1/graph?cursor=<cursor>&limit=500
```

**查询参数：**

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| `cursor` | String | 否 | 分页游标 |
| `limit` | Int | 否 | 每页数量，默认 500 |

**响应 `200`：**
```json
{
  "nodes": [
    {
      "id": "node-uuid",
      "label": "节点显示名",
      "type": "DOCUMENT",
      "refId": null
    }
  ],
  "edges": [
    {
      "source": "source-node-id",
      "target": "target-node-id",
      "relation": "SUPPORT",
      "confidence": 0.85,
      "isManual": false
    }
  ],
  "nextCursor": null
}
```

`type` 取值：`DOCUMENT` / `INSPIRATION` / `CONCEPT`

`relation` 取值：

| 值 | 说明 | 前端样式 |
|----|------|---------|
| `SUPPORT` | 支持 | 绿色虚线 |
| `CHALLENGE` | 挑战/反驳 | 红色虚线 |
| `SUPPLEMENT` | 补充 | 克莱因蓝实线 |
| `CITE` | 引用 | 灰色点线 |

---

## 5. 灵感卡片

### 5.1 创建灵感卡片

```
POST /api/v1/cards
Content-Type: application/json
```

**请求体：**
```json
{
  "content": "脱敏后的文本内容或文件路径",
  "type": "TEXT",
  "privacyLevel": "LOCAL_ONLY",
  "tags": ["voice", "idea"]
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `content` | String | 是 | 脱敏后的内容（文字内容 / 文件路径） |
| `type` | String | 是 | `TEXT` 文字 / `VOICE` 语音 / `IMAGE` 图片 |
| `privacyLevel` | String | 是 | `LOCAL_ONLY` / `LOCAL_FIRST` / `CLOUD_OK` |
| `tags` | List\<String\> | 否 | 标签列表 |

**响应 `200`：**
```json
{
  "id": "card-uuid",
  "content": "脱敏后的内容",
  "type": "TEXT",
  "privacyLevel": "LOCAL_ONLY",
  "tags": ["voice", "idea"],
  "createdAt": 1700000000000
}
```

---

## 6. 离线同步

### 6.1 批量同步

```
POST /api/v1/sync/batch
Content-Type: application/json
```

**请求体：**
```json
[
  {
    "operation": "CREATE_DOC",
    "localRefId": "本地UUID",
    "payloadJson": "{\"title\":\"...\",\"segments\":[...]}"
  }
]
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `operation` | String | `CREATE_DOC` / `CREATE_CARD` / `UPDATE_PROFILE` / `FOLD_SEGMENT` / `DELETE_DOC` / `DELETE_CARD` |
| `localRefId` | String | 客户端本地生成的 UUID |
| `payloadJson` | String | 操作数据的 JSON 字符串 |

**响应 `200`：**
```json
{
  "synced": 1,
  "failed": 0,
  "results": [
    {
      "localRefId": "本地UUID",
      "serverRefId": "服务端返回的ID",
      "success": true,
      "error": null
    }
  ]
}
```

---

## 附录：认证说明

所有请求自动携带以下 Header：

```
X-User-Id:       匿名 UUID（首次启动生成，持久化到本地）
X-Client-Version: 1.0.0
X-Platform:       android
```

V2.1 MVP 阶段使用匿名 UUID 做用户识别，无需登录 / Token。后续版本可升级为 Bearer Token + OAuth2。
