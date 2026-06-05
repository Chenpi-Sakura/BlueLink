export const documents = [
  {
    id: 1,
    title: 'LangChain v0.2 核心概念解析',
    excerpt: 'LangChain是一个基于语言模型的开发框架，核心组件包括Model I/O、Retrieval、Chains、Agents和Memory。最新版本对LCEL语法进行了重构，支持更灵活的流式输出。',
    date: '5月9日 上午10:23',
    tags: ['LangChain'],
    category: 'tech'
  },
  {
    id: 2,
    title: 'RAG检索增强生成实战指南',
    excerpt: 'RAG通过检索外部知识库增强LLM的回答质量。关键步骤包括：文档切分策略、嵌入模型选择、向量数据库搭建、检索排序优化以及重排序(Reranker)机制。',
    date: '5月8日 下午3:15',
    tags: ['RAG'],
    category: 'tech'
  },
  {
    id: 3,
    title: '向量数据库对比：Milvus vs Pinecone vs Chroma',
    excerpt: 'Milvus适合大规模企业级部署，Pinecone提供托管服务降低运维成本，Chroma则是轻量级本地开发首选。选型需考虑数据规模、延迟要求和预算限制。',
    date: '5月7日 下午8:42',
    tags: ['向量数据库'],
    category: 'tech'
  },
  {
    id: 4,
    title: 'Advanced Prompt Engineering技巧',
    excerpt: 'Chain-of-Thought prompting引导模型逐步推理，Few-Shot Learning通过示例提升效果，角色扮演提示词可增强特定场景表现力。结构化输出保证格式一致性。',
    date: '5月6日 上午11:30',
    tags: [],
    category: 'tech'
  },
  {
    id: 5,
    title: 'LLM微调技术：LoRA与QLoRA原理',
    excerpt: 'LoRA通过低秩矩阵分解减少微调参数量，QLoRA进一步引入4-bit量化大幅降低显存占用。开源工具如Axolotl和LLaMA-Factory简化了训练流程。',
    date: '5月5日 下午2:18',
    tags: ['模型微调'],
    category: 'tech'
  },
  {
    id: 6,
    title: 'AI Agent开发框架对比分析',
    excerpt: 'AutoGPT专注自主任务拆解，LangGraph提供状态机工作流，MetaGPT模拟多智能体协作。ReAct框架结合推理与行动，适合复杂决策场景。',
    date: '5月4日 下午4:55',
    tags: [],
    category: 'tech'
  }
]

export const chatHistory = [
  {
    type: 'user',
    content: '如何理解微调技术（Fine-tuning）与 RAG 的边界？'
  },
  {
    type: 'ai',
    content: '微调改变的是模型的"表达习惯"，而 RAG 提供的是"事实依据"。微调无法解决实时知识更新问题，而 RAG 的局限在于检索的召回率。',
    anchors: [
      {
        title: '《LLM 架构演进综述》第 4.2 节',
        subtitle: '对比分析微调与检索的权衡策略',
        documentId: 5
      }
    ]
  }
]

export const readerContent = {
  title: 'LLM 架构演进综述',
  paragraphs: [
    {
      text: '在信息爆炸的时代，我们被海量的冗余数据包围。传统的阅读方式往往试图通过"全量阅读"来捕捉知识，但实际上，信息的价值密度远低于其载体体积。大语言模型的出现为我们提供了一种全新的可能性——让AI成为我们的知识引路人。',
      faded: true
    },
    {
      text: '微调（Fine-tuning）技术通过在预训练模型的基础上进行进一步训练，来适应特定的下游任务。这种方法本质上是在改变模型的"参数记忆"，让模型学会特定的表达风格和知识模式。然而，微调有着固有的局限性：它无法解决实时知识更新的问题，也难以追溯信息的来源。',
      faded: true
    },
    {
      type: 'highlight',
      anchor: '信息认知溯源',
      text: '真正的高效，不在于你读了多少，而在于你是否抓住了"信息增量"。当AI能够精准识别已掌握的背景，过滤掉无用的背景噪音，剩下的全是干货。RAG（检索增强生成）正是基于这一理念，它不依赖模型的参数记忆，而是通过实时检索外部知识库来提供事实依据。'
    },
    {
      text: '这种"聚光灯式"的阅读体验，正是蓝链的核心理念。它不干扰你的沉浸式阅读，但在你需要时，它会提供那根通往真相的引力线。微调与RAG并非对立关系，而是互补的技术方案。微调擅长塑造模型的行为习惯，而RAG负责提供准确的事实来源。',
      faded: true
    },
    {
      type: 'fold',
      label: 'AI已折叠 350 字重复背景，点击查看增量',
      content: '知识的掌握，必须是主动构建的。当线索呈现，剩下的理解与思考，必须由用户自己完成。蓝链的设计哲学正是基于这一点：AI是向导，而非答案的提供者。我们相信，真正的学习发生在用户与原文的直接对话之中。'
    },
    {
      text: '在实际应用中，我们建议根据场景选择合适的技术方案。对于需要特定风格输出的场景，可以结合轻量级的LoRA微调；对于需要准确事实依据的问答场景，则应优先使用RAG方案。最佳实践往往是将两者结合，用微调整体风格，用RAG保证事实准确性。',
      faded: true
    }
  ]
}

export const knowledgeGraph = {
  nodes: [
    { id: 'ai', label: '#AI', x: 165, y: 80 },
    { id: 'trace', label: '#溯源', x: 80, y: 180 },
    { id: 'llm', label: '#LLM', x: 250, y: 180 },
    { id: 'data', label: '#数据', x: 165, y: 300 },
    { id: 'blockchain', label: '#区块链', x: 80, y: 420 },
    { id: 'rag', label: '#RAG', x: 250, y: 420 }
  ],
  connections: [
    ['ai', 'trace'], ['ai', 'llm'],
    ['trace', 'data'], ['llm', 'data'],
    ['data', 'blockchain'], ['data', 'rag']
  ],
  nodeInfo: {
    'ai': { title: '人工智能基础', desc: '已关联 12 篇文献，核心概念包括机器学习、深度学习、神经网络等。' },
    'trace': { title: '溯源技术', desc: '信息溯源是确保知识可信度的关键，包含 8 篇相关论文。' },
    'llm': { title: '大语言模型', desc: 'GPT、LLaMA、Qwen 等主流大模型的技术原理与应用对比。' },
    'data': { title: '数据工程', desc: '向量数据库、数据清洗、特征工程的最佳实践集合。' },
    'blockchain': { title: '区块链与数据', desc: '去中心化数据存储与可信计算的交叉研究领域。' },
    'rag': { title: '检索增强生成', desc: 'RAG技术详解，包含向量检索、重排序等关键技术。' }
  }
}
