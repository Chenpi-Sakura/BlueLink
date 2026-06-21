package com.yjtzc.bluelink.ui.graph

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yjtzc.bluelink.BuildConfig
import com.yjtzc.bluelink.data.local.db.NodeType
import com.yjtzc.bluelink.data.local.db.RelationType
import com.yjtzc.bluelink.data.repository.GraphRepository
import com.yjtzc.bluelink.domain.model.GraphData
import com.yjtzc.bluelink.domain.model.GraphEdge
import com.yjtzc.bluelink.domain.model.GraphNode
import com.yjtzc.bluelink.domain.model.toDomain
import com.yjtzc.bluelink.ui.common.AppError
import com.yjtzc.bluelink.ui.common.UiState
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class GraphLayout(val jsValue: String) {
    FORCE("force"),
    CIRCULAR("circular")
}

enum class GraphDataSource(val debugLabel: String) {
    REMOTE("remote"),
    CACHE("cache"),
    DEMO("demo"),
    NONE("none")
}

class GraphViewModel(
    private val graphRepository: GraphRepository
) : ViewModel() {

    private val _graphState = MutableStateFlow<UiState<GraphData>>(UiState.Loading)
    val graphState: StateFlow<UiState<GraphData>> = _graphState.asStateFlow()

    private val _selectedNodeId = MutableStateFlow<String?>(null)
    val selectedNodeId: StateFlow<String?> = _selectedNodeId.asStateFlow()

    val selectedNode: StateFlow<GraphNode?> = combine(_selectedNodeId, _graphState) { nodeId, state ->
        (state as? UiState.Success)?.data?.nodes?.firstOrNull { it.id == nodeId }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val selectedNodeEdges: StateFlow<List<GraphEdge>> =
        combine(_selectedNodeId, _graphState) { nodeId, state ->
            val data = (state as? UiState.Success)?.data
            if (nodeId == null || data == null) emptyList()
            else data.edges.filter { it.sourceId == nodeId || it.targetId == nodeId }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val selectedAdjacentNodes: StateFlow<List<GraphNode>> =
        combine(_selectedNodeId, _graphState) { nodeId, state ->
            val data = (state as? UiState.Success)?.data
            if (nodeId == null || data == null) emptyList()
            else {
                val adjacentIds = data.edges.asSequence()
                    .filter { it.sourceId == nodeId || it.targetId == nodeId }
                    .map { if (it.sourceId == nodeId) it.targetId else it.sourceId }
                    .toSet()
                data.nodes.filter { it.id in adjacentIds }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _currentLayout = MutableStateFlow(GraphLayout.FORCE)
    val currentLayout: StateFlow<GraphLayout> = _currentLayout.asStateFlow()

    private val _dataSource = MutableStateFlow(GraphDataSource.NONE)
    val dataSource: StateFlow<GraphDataSource> = _dataSource.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    private val _renderedNodeCount = MutableStateFlow(0)
    val renderedNodeCount: StateFlow<Int> = _renderedNodeCount.asStateFlow()

    private val _renderedEdgeCount = MutableStateFlow(0)
    val renderedEdgeCount: StateFlow<Int> = _renderedEdgeCount.asStateFlow()

    private val _scriptStatus = MutableStateFlow("idle")
    val scriptStatus: StateFlow<String> = _scriptStatus.asStateFlow()

    init {
        loadGraph()
    }

    fun loadGraph() {
        viewModelScope.launch {
            _graphState.value = UiState.Loading
            _lastError.value = null
            _renderedNodeCount.value = 0
            _renderedEdgeCount.value = 0
            _scriptStatus.value = "idle"

            // 先展示本地缓存
            val localData = runCatching {
                GraphData(
                    nodes = graphRepository.getLocalNodes().map { it.toDomain() },
                    edges = graphRepository.getLocalEdges().map { it.toDomain() }
                )
            }.getOrNull()
            if (localData != null && (localData.nodes.isNotEmpty() || localData.edges.isNotEmpty())) {
                _graphState.value = UiState.Success(localData)
                _dataSource.value = GraphDataSource.CACHE
            }

            // 再从远端拉取最新
            graphRepository.fetchFromRemote()
                .onSuccess {
                    runCatching {
                        GraphData(
                            nodes = graphRepository.getLocalNodes().map { it.toDomain() },
                            edges = graphRepository.getLocalEdges().map { it.toDomain() }
                        )
                    }.onSuccess { data ->
                        if (data.nodes.isNotEmpty() || !BuildConfig.DEBUG) {
                            _graphState.value = UiState.Success(data)
                            _dataSource.value = GraphDataSource.REMOTE
                        } else {
                            showDemoGraph()
                        }
                        if (data.nodes.none { it.id == _selectedNodeId.value }) {
                            _selectedNodeId.value = null
                        }
                    }.onFailure { handleLoadFailure(localData, it) }
                }
                .onFailure { handleLoadFailure(localData, it) }
        }
    }

    private fun handleLoadFailure(localData: GraphData?, error: Throwable) {
        _lastError.value = error.message ?: error::class.java.simpleName
        if (localData == null || localData.nodes.isEmpty()) {
            if (BuildConfig.DEBUG) {
                showDemoGraph()
            } else {
                _dataSource.value = GraphDataSource.NONE
                _graphState.value = UiState.Error(AppError.AIServiceError(error.message.orEmpty()))
            }
        }
    }

    private fun showDemoGraph() {
        if (!BuildConfig.DEBUG) return
        _dataSource.value = GraphDataSource.DEMO
        _selectedNodeId.value = null
        _graphState.value = UiState.Success(createDemoGraph())
    }

    fun loadDemoGraph() {
        if (BuildConfig.DEBUG) showDemoGraph()
    }

    fun onGraphRendered(nodeCount: Int, edgeCount: Int) {
        _renderedNodeCount.value = nodeCount.coerceAtLeast(0)
        _renderedEdgeCount.value = edgeCount.coerceAtLeast(0)
    }

    fun onGraphRenderError(message: String) {
        _lastError.value = "WebView: ${message.take(240)}"
    }

    fun onGraphScriptResult(result: String?) {
        _scriptStatus.value = result?.take(80) ?: "null"
    }

    fun onLayoutSelected(layout: GraphLayout) {
        _currentLayout.value = layout
    }

    fun onGraphNodeClicked(nodeId: String) {
        val data = (_graphState.value as? UiState.Success)?.data ?: return
        _selectedNodeId.value = nodeId.takeIf { id -> data.nodes.any { it.id == id } }
        // 节点点击后的处理：跳转到对应文档/灵感，或高亮邻接节点
        // 由 GraphScreen 中的回调处理导航
    }

    private fun createDemoGraph(): GraphData = GraphData(
        nodes = listOf(
            GraphNode("concept-ai-origin", "AI 溯源", NodeType.CONCEPT, null),
            GraphNode("concept-feynman", "费曼学习法", NodeType.CONCEPT, null),
            GraphNode("concept-info-growth", "信息增量", NodeType.CONCEPT, null),
            GraphNode("concept-cognition", "认知架构", NodeType.CONCEPT, null),
            GraphNode("concept-deep-work", "深度工作", NodeType.CONCEPT, null),
            GraphNode("doc-deep-work", "《深度工作》", NodeType.DOCUMENT, "demo-doc-deep-work"),
            GraphNode("doc-feynman", "《费曼学习技巧》", NodeType.DOCUMENT, "demo-doc-feynman"),
            GraphNode("doc-second-brain", "《第二大脑》", NodeType.DOCUMENT, "demo-doc-second-brain"),
            GraphNode("doc-sapiens", "《人类简史》", NodeType.DOCUMENT, "demo-doc-sapiens"),
            GraphNode("doc-card-notes", "《卡片笔记写作法》", NodeType.DOCUMENT, "demo-doc-card-notes"),
            GraphNode("inspiration-12", "灵感 #12", NodeType.INSPIRATION, "demo-inspiration-12"),
            GraphNode("inspiration-21", "灵感 #21", NodeType.INSPIRATION, "demo-inspiration-21")
        ),
        edges = listOf(
            GraphEdge("concept-ai-origin", "concept-feynman", RelationType.SUPPORT, 0.92f),
            GraphEdge("concept-ai-origin", "concept-info-growth", RelationType.SUPPORT, 0.86f),
            GraphEdge("concept-ai-origin", "concept-cognition", RelationType.SUPPLEMENT, 0.90f),
            GraphEdge("concept-ai-origin", "concept-deep-work", RelationType.SUPPLEMENT, 0.82f),
            GraphEdge("concept-feynman", "doc-feynman", RelationType.CITE, 0.78f),
            GraphEdge("concept-feynman", "inspiration-21", RelationType.SUPPLEMENT, 0.68f),
            GraphEdge("concept-info-growth", "doc-sapiens", RelationType.CITE, 0.58f),
            GraphEdge("concept-info-growth", "inspiration-12", RelationType.SUPPLEMENT, 0.72f),
            GraphEdge("concept-cognition", "doc-second-brain", RelationType.CITE, 0.84f),
            GraphEdge("concept-cognition", "doc-card-notes", RelationType.CITE, 0.76f),
            GraphEdge("concept-deep-work", "doc-deep-work", RelationType.CITE, 0.94f),
            GraphEdge("concept-deep-work", "inspiration-21", RelationType.CHALLENGE, 0.62f),
            GraphEdge("concept-feynman", "concept-deep-work", RelationType.SUPPORT, 0.73f),
            GraphEdge("concept-cognition", "concept-info-growth", RelationType.SUPPORT, 0.66f),
            GraphEdge("inspiration-12", "concept-cognition", RelationType.CHALLENGE, 0.48f),
            GraphEdge("doc-card-notes", "concept-info-growth", RelationType.SUPPORT, 0.55f)
        )
    )
}
