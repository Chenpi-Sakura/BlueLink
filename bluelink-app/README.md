# 蓝链 BlueLink

> 端侧 AI 认知溯源引擎 - AI 不生成答案，只做知识索引与原文溯源

## 项目简介

蓝链（BlueLink）是一款面向科研、创作、深度学习的端侧 AI 认知溯源引擎。核心理念是：**AI 不生成答案，只做知识索引与原文溯源**，引导用户回归原始文本，主动思考、自主验证。

### 核心特性

- **溯源检索**：用户提问 → AI 定位原文 → 输出锚点 → 跳转原文片段
- **信息降噪**：文档入库 → 识别重复内容 → 折叠冗余 → 只展示信息增量
- **费曼伴学**：用户复述知识点 → AI 检测认知偏差 → 引力线绑定错误与原文
- **灵感图谱**：灵感/笔记入库 → 自动关联知识 → 构建个人可视化知识星图
- **隐私保护**：所有本地数据端侧处理、本地加密，不上云

## 技术栈

- **前端框架**：Vue 3 + Vite
- **路由管理**：Vue Router 4
- **样式方案**：Tailwind CSS 3.4
- **状态管理**：Pinia（待集成）
- **端侧 AI**：WebLLM / Transformers.js（待集成）

## 项目结构

```
bluelink-app/
├── src/
│   ├── components/          # 公共组件
│   │   ├── StatusBar.vue     # 状态栏
│   │   ├── BottomNav.vue     # 底部导航
│   │   └── DocumentCard.vue  # 文档卡片
│   ├── views/               # 页面组件
│   │   ├── Home.vue          # 首页 - 文库聚合
│   │   ├── Chat.vue          # 对话页 - 线索锚定
│   │   ├── Reader.vue        # 阅读页 - 动态透视
│   │   └── Graph.vue         # 图谱页 - 知识星图
│   ├── router/              # 路由配置
│   ├── data/                # 数据层
│   ├── App.vue              # 根组件
│   ├── main.js              # 入口文件
│   └── style.css            # 全局样式
├── tailwind.config.js       # Tailwind 配置
├── vite.config.js           # Vite 配置
└── package.json
```

## 设计规范

### 色彩系统

| 名称 | 色值 | 用途 |
|------|------|------|
| 米白色底 | `#FDFBF7` | 主背景，纸张质感 |
| 克莱因蓝 | `#002FA7` | 品牌强调色，交互引导 |
| 深灰 | `#2C2B29` | 主要文字 |
| 中灰 | `#66635D` | 次要文字 |
| 浅灰 | `#A39F98` | 辅助文字、禁用态 |

### 字体规范

- **阅读区**：Noto Serif SC（衬线体，古典阅读感）
- **交互区**：Inter（无衬线体，现代清晰）

### 视觉特性

- 大圆角（`border-radius: 1.75rem`）
- 磨砂玻璃效果（`backdrop-filter: blur(20px)`）
- 微阴影（轻量级弥散阴影）
- 多留白（呼吸感设计）

## 快速开始

### 环境要求

- Node.js >= 18.0.0
- npm >= 9.0.0

### 安装依赖

```bash
cd bluelink-app
npm install
```

### 启动开发服务器

```bash
npm run dev
```

访问 http://localhost:5173/

### 构建生产版本

```bash
npm run build
```

## 页面说明

### 首页（文库聚合）

- 瀑布流文献卡片布局
- AI 自动提取文档摘要
- 悬浮快捷输入栏（语音/文字/扫描）

### 对话页（线索锚定）

- 无气泡聊天设计，突出溯源路径
- 锚点卡片展示文献来源
- 点击锚点跳转原文片段

### 阅读页（动态透视）

- **聚光灯模式**：目标高亮，其余弱化
- **增量折叠**：AI 识别重复内容并折叠
- 点击空白解除聚焦，恢复全文阅读

### 图谱页（知识星图）

- 知识节点 + 关联连线可视化
- 点击节点聚焦高亮关联
- 底部信息面板展示详情

## 开发路线

### 第一阶段（当前）

- [x] Vue 项目架构搭建
- [x] 四大核心页面 UI 实现
- [x] 路由配置与页面过渡
- [x] 设计系统（Tailwind 配置）

### 第二阶段

- [ ] 用户登录/注册系统
- [ ] Pinia 状态管理
- [ ] 文档导入/解析（PDF、TXT、Markdown）
- [ ] 本地向量数据库（ChromaDB）

### 第三阶段

- [ ] 端侧 AI 集成（WebLLM / Transformers.js）
- [ ] RAG 检索链路
- [ ] 费曼伴学模式
- [ ] 知识图谱自动生成

### 第四阶段

- [ ] Capacitor 打包 Android APK
- [ ] vivo 手机适配测试
- [ ] 性能优化与内存管理

## 打包 APK

### 使用 Capacitor

```bash
npm install @capacitor/core @capacitor/cli @capacitor/android
npx cap init BlueLink com.bluelink.app
npx cap add android
npm run build
npx cap sync
npx cap open android
```

## 许可证

MIT License

## 贡献指南

欢迎提交 Issue 和 Pull Request！

---

**蓝链** - 让 AI 成为知识的引路人，而非答案的提供者。
