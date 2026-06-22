# BlueLink 开发规范

## 核心工作流

每个 Task 遵循以下循环：

```
Plan → Code → Build → (Review?) → Commit → 进入下一个 Task
```

---

## 1. Plan — 先计划

**任何代码改动前必须先做计划：**

- 小改动（1 个文件，< 50 行）：脑中过一遍方案即可，但要在回复中说清楚要怎么做
- 中等改动（2-4 个文件／50-100 行／跨 2 层）：用自然语言描述改动方案，用户确认后再动手
- 大改动（5+ 个文件／100+ 行）：使用 `EnterPlanMode` 工具，输出结构化方案，获批后实施

---

## 2. Code — 编码规范

### 2.1 注释要求

**每个函数、类、接口必须写 KDoc 注释**，包含：

```kotlin
/**
 * 做什么（必有）
 *
 * 为什么这样设计 / 注意事项（可选但建议）
 *
 * @param xxx 参数说明
 * @return 返回值说明
 * @throws XxxException 什么情况下抛
 */
```

**例外：** 一行纯委托、getter/setter、极简单的 `toggle` 函数（如 `fun toggleVisibility()`）可只写行尾注释。

### 2.2 设计原则

| 原则 | 含义 | 代码信号 |
|------|------|----------|
| **高内聚** | 一个类只封装一类职责。一个函数只做一件事。 | 函数 > 30 行要考虑拆分；类中有多个无关联的职责要考虑拆分 |
| **低耦合** | 通过接口 / 抽象依赖，不直接 new 具体实现。 | 构造函数参数应该都是接口或值对象；避免 `object` 单例直接耦合 |
| **显式 > 隐式** | 不依赖魔法数字、隐式 null、副作用。 | 使用 sealed class / Result / 显式错误类型 |

### 2.3 命名

- 包名：按层+功能，如 `data.repository`、`domain.usecase`
- 类名：名词，如 `CaptureRepository`、`AskQuestionUseCase`
- 函数名：动词或动词短语，如 `saveInspiration()`、`observeAllCards()`
- 测试：`` `featureName_scenario_expectedResult` ``（反引号包围）

### 2.4 与现有代码风格保持一致

- 已有项目中用 `runCatching` + `Result` 包装，新代码也这样
- 已有项目中用 `StateFlow` + `UiState`，新代码也这样
- 已有项目中 Entity / Domain 分离 + `toDomain()` 映射，新代码也这样

---

## 3. Build — 构建测试

**每次代码改动后，无论改动量多少，必须执行一次构建测试。**

```bash
cd D:/Projects/BlueLink
./gradlew assembleDebug
```

- 构建失败 → 修复 → 再次构建 → 通过后才能继续
- 如果改动涉及逻辑变更（不只是加注释 / 重命名），也应考虑跑相关单测：
  ```bash
  ./gradlew testDebugUnitTest
  ```

---

## 4. Review — 代码审查（大改动触发）

### 触发条件（满足任一即触发）

1. **新增或修改 5+ 个文件**
2. **代码净增/净改 100+ 行**（git diff --stat）
3. **跨架构层改动**（同时修改了 data / domain / ui 中的 2+ 层）
4. **涉及 API 接口签名变更**（改了 Retrofit 接口的参数/路径）

### CR 执行方式

派 SubAgent 执行 Code Review，审查清单：

#### ✅ 正确性
- 跨文件一致性：改了接口是否改了所有调用方？改了 Entity 是否改了 `toDomain()` 映射？
- 边界情况：null 安全、空列表、网络超时、非法参数是否处理？
- 线程安全：挂起函数是否在正确的 CoroutineContext 上调用？

#### ✅ 设计质量
- 是否遵循高内聚低耦合？有没有引入循环依赖？
- 是否存在重复代码（DRY）？
- 是否与项目的既有模式一致（`Result<>`、`UiState`、`toDomain()` 等）？

#### ✅ 注释完整性
- 新增的 public / internal 函数是否有 KDoc？
- 复杂业务逻辑是否有行内注释解释"为什么"？

### CR 后处理

- CR 发现问题 → 修复后重新 Build → 通过后继续
- CR 无问题 → 直接提交

---

## 5. Commit — 频繁提交

### 原则

- **小粒度提交**：一个逻辑单元完成就 commit，不攒多个功能一起提
- **构建通过才提交**：确保每个 commit 编译通过（`./gradlew assembleDebug` 通过）
- **message 规范**：

```
<type>: <简要描述>

<可选：详细说明>
```

| type | 场景 |
|------|------|
| feat | 新功能 |
| fix | 修 bug |
| refactor | 重构（不增功能不改 bug） |
| docs | 文档 |
| test | 测试 |
| chore | 构建/配置/工具链 |

示例：
```
feat: CaptureRepository 添加离线缓存支持

- Room 缓存卡片列表
- 无网络时降级读取本地缓存
- 网络恢复后同步差异
```

### 提交频率示意

```
feat: 添加 CaptureRepository 接口定义        ← 写完接口就提
feat: CaptureRepository 实现本地保存逻辑     ← 本地功能完成就提
feat: CaptureRepository 实现云端同步逻辑     ← 同步完成就提
```

不要把三件事揉成一个 300 行的 commit。

---

## 6. 分支策略

**开发不要直接在 `backend` 上改，而是从它拉一条 `feat/xxx` 分支：**

```bash
# 从 backend 拉开发分支
git checkout -b feat/backend-dev backend

# 开发完成后合并回 backend
git checkout backend
git merge feat/backend-dev
git push origin backend

# 远程分支可保留或删除
git push origin --delete feat/backend-dev   # 如果不需要了
```

**分支命名规则：**
| 分支 | 用途 | 从哪拉 |
|------|------|--------|
| `backend` | 后端主分支，保持稳定 | — |
| `feat/xxx` | 具体功能开发 | `backend` |
| `fix/xxx` | 修 bug | `backend` |

> 所有 push 仍遵循第 7 条规则——需用户批准。

---

## 7. Push — 需要时经用户批准

**Push 不是每次循环的必经步骤。** 当需要推送到远程时，必须先问用户。

```bash
# 正确做法
# 用户没提 → 不问不推
# 想推送了 → "可以推吗？" → 用户同意 → 执行

# 例外：用户明确说了"直接推"或"你看着办"
```

---

## 8. 开发循环（总结）

```
┌─────────────────────────────────────────────────────────┐
│  ① Plan     明确方案，用户批准                           │
│  ② Code     写好注释，高内聚低耦合，20-30 行考虑拆分    │
│  ③ Build    ./gradlew assembleDebug 必须通过             │
│  ④ Review   大改动（5文件/100行/跨层/改API）→ SubAgent  │
│  ⑤ Commit   小粒度 message 规范，build 通过才提          │
│  ⑥ 回到 ①   进入下一个 Task                            │
└─────────────────────────────────────────────────────────┘

> Push：需要推时先问用户，不在循环中强制。**Commit 是循环终点，Push 是独立操作。**
```

---

## 10. 后端开发踩坑记录

> 已踩过的坑，避免重复。

| # | 问题 | 解决方案 |
|---|------|----------|
| 1 | `.env` 含 API Key 被提交到 git | `.gitignore` 加 `.env`，`git rm --cached`；暴露的 key 去后台撤销 |
| 2 | Dockerfile `pip install` 时 `requirements.txt` 还没复制 | 先 `COPY requirements.txt .` 再 pip，最后 `COPY . .` |
| 3 | 国内服务器 pip/apt 下载慢 | 默认阿里云 pip 镜像 + 清华 apt 镜像 |
| 4 | `tests/` 被 `.dockerignore` 排除导致容器里没测试 | 移除 `.dockerignore` 中的 `tests/` |
| 5 | `pgvector.Vector(1024)` 类型 SQLite 不兼容 | **全栈只用 PostgreSQL，彻底移除 SQLite** |
| 6 | Android camelCase ↔ Python snake_case 字段不一致 | Pydantic `alias_generator=to_camel` + `response_model_by_alias=True` |
| 7 | `pytest-mock` 未预装 | 改用内置 `monkeypatch` |
| 8 | FastAPI Header 校验缺参返回 422 不是 400 | 测试预期写 422 |
| 9 | Debian trixie 无 `libgl1-mesa-glx` | 后端不需要 GL 渲染，直接移除该依赖 |
