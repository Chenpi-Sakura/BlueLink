package com.yjtzc.bluelink.ui.graph

import android.webkit.WebView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yjtzc.bluelink.ui.common.UiState

/**
 * 知识图谱页 — WebView + ECharts 力导向图（UI&UX §4.5）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GraphScreen(
    viewModel: GraphViewModel,
    modifier: Modifier = Modifier
) {
    val graphState by viewModel.graphState.collectAsStateWithLifecycle()
    var webView by remember { mutableStateOf<WebView?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("知识图谱") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1E1E2E),
                    titleContentColor = Color.White
                )
            )
        },
        modifier = modifier
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color(0xFF1E1E2E))
        ) {
            when (val state = graphState) {
                is UiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = Color.White
                    )
                }

                is UiState.Error -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "图谱加载失败",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(Modifier.height(8.dp))
                        TextButton(onClick = { viewModel.loadGraph() }) {
                            Text("点击重试", color = Color.White)
                        }
                    }
                }

                is UiState.Success -> {
                    AndroidView(
                        factory = { context ->
                            WebView(context).apply {
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.allowFileAccess = true
                                settings.allowContentAccess = true
                                settings.blockNetworkLoads = true

                                addJavascriptInterface(
                                    GraphJsBridge { nodeId ->
                                        viewModel.onGraphNodeClicked(nodeId)
                                    },
                                    "BlueLinkBridge"
                                )

                                loadUrl("file:///android_asset/graph.html")
                                webView = this
                            }
                        },
                        update = { wv ->
                            val json = buildGraphJson(state.data)
                            wv.evaluateJavascript("window.updateGraph($json)", null)
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                is UiState.Idle -> Unit
            }

            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("force", "circular").forEach { layout ->
                    FilterChip(
                        selected = false,
                        onClick = {
                            webView?.evaluateJavascript(
                                "window.changeLayout('$layout')",
                                null
                            )
                        },
                        label = { Text(layout, color = Color.White) }
                    )
                }
            }
        }
    }
}

/**
 * JS Bridge — WebView 节点点击 → Compose（V2.0 §9.4.2）
 */
class GraphJsBridge(private val onNodeClick: (String) -> Unit) {
    @android.webkit.JavascriptInterface
    fun onNodeClick(nodeId: String) {
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            onNodeClick(nodeId)
        }
    }
}

/**
 * 将 GraphData 转为 ECharts 所需的 JSON 字符串
 */
private fun buildGraphJson(data: com.yjtzc.bluelink.domain.model.GraphData): String {
    val nodesJson = data.nodes.joinToString(",\n") { node ->
        val symbol = when (node.type) {
            com.yjtzc.bluelink.data.local.db.NodeType.DOCUMENT -> "circle"
            com.yjtzc.bluelink.data.local.db.NodeType.INSPIRATION -> "diamond"
            com.yjtzc.bluelink.data.local.db.NodeType.CONCEPT -> "roundRect"
        }

        val color = when (node.type) {
            com.yjtzc.bluelink.data.local.db.NodeType.DOCUMENT -> "#5B8DEF"
            com.yjtzc.bluelink.data.local.db.NodeType.INSPIRATION -> "#F0C674"
            com.yjtzc.bluelink.data.local.db.NodeType.CONCEPT -> "#002FA7"
        }

        """
            {
                "id": "${node.id}",
                "name": "${node.label}",
                "symbol": "$symbol",
                "type": "${node.type.name}",
                "itemStyle": { "color": "$color" }
            }
        """.trimIndent()
    }

    val edgesJson = data.edges.joinToString(",\n") { edge ->
        val color = when (edge.relation) {
            com.yjtzc.bluelink.data.local.db.RelationType.SUPPORT -> "#34A853"
            com.yjtzc.bluelink.data.local.db.RelationType.CHALLENGE -> "#EA4335"
            com.yjtzc.bluelink.data.local.db.RelationType.SUPPLEMENT -> "#002FA7"
            com.yjtzc.bluelink.data.local.db.RelationType.CITE -> "#888"
        }

        val lineType = when (edge.relation) {
            com.yjtzc.bluelink.data.local.db.RelationType.SUPPORT -> "dashed"
            com.yjtzc.bluelink.data.local.db.RelationType.CHALLENGE -> "dashed"
            com.yjtzc.bluelink.data.local.db.RelationType.CITE -> "dotted"
            com.yjtzc.bluelink.data.local.db.RelationType.SUPPLEMENT -> "solid"
        }

        """
            {
                "source": "${edge.sourceId}",
                "target": "${edge.targetId}",
                "relation": "${edge.relation.name}",
                "lineStyle": { "color": "$color", "type": "$lineType" }
            }
        """.trimIndent()
    }

    return """
        {
            "nodes": [$nodesJson],
            "edges": [$edgesJson]
        }
    """.trimIndent()
}