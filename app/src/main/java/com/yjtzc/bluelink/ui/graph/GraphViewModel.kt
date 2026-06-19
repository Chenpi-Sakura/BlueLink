package com.yjtzc.bluelink.ui.graph

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yjtzc.bluelink.data.repository.GraphRepository
import com.yjtzc.bluelink.domain.model.GraphData
import com.yjtzc.bluelink.domain.model.toDomain
import com.yjtzc.bluelink.ui.common.AppError
import com.yjtzc.bluelink.ui.common.UiState
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class GraphViewModel(
    private val graphRepository: GraphRepository
) : ViewModel() {

    private val _graphState = MutableStateFlow<UiState<GraphData>>(UiState.Loading)
    val graphState: StateFlow<UiState<GraphData>> = _graphState.asStateFlow()

    init {
        loadGraph()
    }

    fun loadGraph() {
        viewModelScope.launch {
            _graphState.value = UiState.Loading

            // 先展示本地缓存
            val localNodes = graphRepository.getLocalNodes()
            val localEdges = graphRepository.getLocalEdges()
            if (localNodes.isNotEmpty()) {
                _graphState.value = UiState.Success(
                    GraphData(
                        nodes = localNodes.map { it.toDomain() },
                        edges = localEdges.map { it.toDomain() }
                    )
                )
            }

            // 再从远端拉取最新
            graphRepository.fetchFromRemote()
                .onSuccess { dto ->
                    val nodes = graphRepository.getLocalNodes().map { it.toDomain() }
                    val edges = graphRepository.getLocalEdges().map { it.toDomain() }
                    _graphState.value = UiState.Success(GraphData(nodes, edges))
                }
                .onFailure { e ->
                    if (localNodes.isEmpty()) {
                        _graphState.value = UiState.Error(
                            AppError.AIServiceError(e.message ?: "")
                        )
                    }
                }
        }
    }

    fun onGraphNodeClicked(nodeId: String) {
        // 节点点击后的处理：跳转到对应文档/灵感，或高亮邻接节点
        // 由 GraphScreen 中的回调处理导航
    }
}
