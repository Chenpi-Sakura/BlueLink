package com.yjtzc.bluelink.ui.graph

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.CenterFocusStrong
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.squareup.moshi.Moshi
import com.yjtzc.bluelink.BuildConfig
import com.yjtzc.bluelink.data.local.db.NodeType
import com.yjtzc.bluelink.data.local.db.RelationType
import com.yjtzc.bluelink.domain.model.GraphData
import com.yjtzc.bluelink.ui.common.UiState
import kotlinx.coroutines.launch

private val graphJsonAdapter = Moshi.Builder().build().adapter(Any::class.java)

/**
 * 知识图谱页 — WebView + ECharts 力导向图（UI&UX §4.5）
 */
@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun LegacyGraphScreen(
    viewModel: GraphViewModel,
    modifier: Modifier = Modifier
) {
    val graphState by viewModel.graphState.collectAsStateWithLifecycle()
    val selectedNode by viewModel.selectedNode.collectAsStateWithLifecycle()
    val selectedEdges by viewModel.selectedNodeEdges.collectAsStateWithLifecycle()
    val adjacentNodes by viewModel.selectedAdjacentNodes.collectAsStateWithLifecycle()
    val currentLayout by viewModel.currentLayout.collectAsStateWithLifecycle()
    val dataSource by viewModel.dataSource.collectAsStateWithLifecycle()
    val renderedNodeCount by viewModel.renderedNodeCount.collectAsStateWithLifecycle()
    val renderedEdgeCount by viewModel.renderedEdgeCount.collectAsStateWithLifecycle()
    var webView by remember { mutableStateOf<WebView?>(null) }
    var isWebViewReady by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF1E1E2E))
    ) {
        // ── Main content: WebView / Loading / Error ──
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
                val graphJson = remember(state.data) { buildGraphJsonSafe(state.data) }
                AndroidView(
                    factory = { context ->
                        WebView(context).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.allowFileAccess = true
                            settings.allowContentAccess = false
                            settings.blockNetworkLoads = true
                            webViewClient = WebViewClient()

                            addJavascriptInterface(
                                GraphJsBridge(
                                    onNodeClick = viewModel::onGraphNodeClicked,
                                    onReady = {
                                        isWebViewReady = true
                                        evaluateJavascript(
                                            "window.updateGraph($graphJson)",
                                            null
                                        )
                                        evaluateJavascript(
                                            "window.changeLayout('${viewModel.currentLayout.value.jsValue}')",
                                            null
                                        )
                                    },
                                    onGraphRendered = viewModel::onGraphRendered,
                                    onGraphError = viewModel::onGraphRenderError
                                ),
                                "BlueLinkBridge"
                            )

                            loadUrl("file:///android_asset/graph.html")
                            webView = this
                        }
                    },
                    update = { wv ->
                        webView = wv
                        if (isWebViewReady) {
                            wv.evaluateJavascript(
                                "window.updateGraph($graphJson)",
                                null
                            )
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            is UiState.Idle -> Unit
        }

        // ── Top area: status bar + floating top bar + selected node card ──
        Column(modifier = Modifier.align(Alignment.TopCenter)) {
            Spacer(Modifier.statusBarsPadding())

            // Floating pill top bar
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .height(64.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFFFAF7F2).copy(alpha = 0.94f))
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "知识图谱",
                    color = Color(0xFF2C2B29),
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp)
                )
                IconButton(onClick = {
                    scope.launch { snackbarHostState.showSnackbar("搜索功能开发中") }
                }) {
                    Icon(
                        Icons.Outlined.Search,
                        "搜索",
                        tint = Color(0xFF66635D),
                        modifier = Modifier.size(24.dp)
                    )
                }
                IconButton(onClick = { viewModel.loadGraph() }) {
                    Icon(
                        Icons.Outlined.Refresh,
                        "刷新",
                        tint = Color(0xFF66635D),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Selected node info card
            selectedNode?.let { node ->
                val typeDisplay = when (node.type) {
                    NodeType.DOCUMENT -> "文献"
                    NodeType.INSPIRATION -> "灵感"
                    NodeType.CONCEPT -> "概念"
                }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(top = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xE62A2A3D)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(
                            node.label,
                            color = Color.White,
                            style = MaterialTheme.typography.titleSmall
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "$typeDisplay · ${selectedEdges.size} 条关联 · ${adjacentNodes.size} 个相邻节点",
                            color = Color(0xFFB8BED0),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }

        // ── Bottom control bar: frosted dark floating bar ──
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
                .fillMaxWidth(0.84f)
                .height(64.dp)
                .clip(RoundedCornerShape(30.dp))
                .background(Color(0xFF111827).copy(alpha = 0.78f))
                .border(
                    width = 0.5.dp,
                    color = Color(0xFFFAF7F2).copy(alpha = 0.25f),
                    shape = RoundedCornerShape(30.dp)
                )
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Reset view
                BottomControlItem(
                    icon = {
                        Icon(
                            Icons.Outlined.CenterFocusStrong,
                            "重置视图",
                            tint = bottomControlTextColor,
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    label = "重置视图",
                    onClick = { webView?.evaluateJavascript("window.resetView()", null) }
                )

                // Divider
                Box(
                    Modifier
                        .width(1.dp)
                        .height(32.dp)
                        .background(bottomControlDividerColor)
                )

                // Filter
                BottomControlItem(
                    icon = {
                        Icon(
                            Icons.Outlined.FilterList,
                            "筛选",
                            tint = bottomControlTextColor,
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    label = "筛选",
                    onClick = {
                        scope.launch { snackbarHostState.showSnackbar("筛选功能开发中") }
                    }
                )

                // Divider
                Box(
                    Modifier
                        .width(1.dp)
                        .height(32.dp)
                        .background(bottomControlDividerColor)
                )

                // Layout toggle
                val nextLayout = if (currentLayout == GraphLayout.FORCE) GraphLayout.CIRCULAR else GraphLayout.FORCE
                BottomControlItem(
                    icon = {
                        Icon(
                            Icons.Outlined.AccountTree,
                            "布局",
                            tint = bottomControlTextColor,
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    label = if (currentLayout == GraphLayout.FORCE) "力导向" else "环形",
                    onClick = {
                        viewModel.onLayoutSelected(nextLayout)
                        webView?.evaluateJavascript(
                            "window.changeLayout('${nextLayout.jsValue}')",
                            null
                        )
                    }
                )
            }
        }

        // ── Debug overlay (debug build only) ──
        if (BuildConfig.DEBUG) {
            Text(
                text = "nodes: $renderedNodeCount · edges: $renderedEdgeCount " +
                        "· source: ${dataSource.debugLabel} · webReady: $isWebViewReady",
                color = Color(0xFFFAF7F2).copy(alpha = 0.55f),
                fontSize = 11.sp,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(top = 74.dp, start = 16.dp)
            )
        }

        // ── Snackbar host ──
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 100.dp)
        )
    }
}

// ── Bottom control bar item ──

private val bottomControlTextColor = Color(0xFFFAF7F2).copy(alpha = 0.88f)
private val bottomControlDividerColor = Color(0xFFFAF7F2).copy(alpha = 0.20f)

@Composable
private fun RowScope.BottomControlItem(
    icon: @Composable () -> Unit,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .weight(1f)
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp)
    ) {
        icon()
        Text(
            text = label,
            color = bottomControlTextColor,
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 3.dp)
        )
    }
}

/**
 * JS Bridge — WebView 节点点击 → Compose（V2.0 §9.4.2）
 */
class GraphJsBridge(
    private val onNodeClick: (String) -> Unit,
    private val onReady: () -> Unit,
    private val onGraphRendered: (Int, Int) -> Unit = { _, _ -> },
    private val onGraphError: (String) -> Unit = {}
) {
    private val mainHandler = Handler(Looper.getMainLooper())

    @JavascriptInterface
    fun onNodeClick(nodeId: String) {
        mainHandler.post { onNodeClick(nodeId) }
    }

    @JavascriptInterface
    fun onReady() {
        mainHandler.post(onReady)
    }

    @JavascriptInterface
    fun onGraphRendered(nodeCount: Int, edgeCount: Int) {
        mainHandler.post { onGraphRendered(nodeCount, edgeCount) }
    }

    @JavascriptInterface
    fun onGraphError(message: String) {
        mainHandler.post { onGraphError(message) }
    }
}

internal fun buildGraphJsonSafe(data: GraphData): String {
    val nodes = data.nodes.map { node ->
        mapOf(
            "id" to node.id,
            "name" to node.label,
            "type" to node.type.name,
            "refId" to node.refId,
            "symbol" to when (node.type) {
                NodeType.DOCUMENT -> "circle"
                NodeType.INSPIRATION -> "diamond"
                NodeType.CONCEPT -> "roundRect"
            },
            "itemStyle" to mapOf(
                "color" to when (node.type) {
                    NodeType.DOCUMENT -> "#8EC5FF"
                    NodeType.INSPIRATION -> "#F0C674"
                    NodeType.CONCEPT -> "#002FA7"
                },
                "borderColor" to when (node.type) {
                    NodeType.DOCUMENT -> "#D8E7FF"
                    NodeType.INSPIRATION -> "#FFD979"
                    NodeType.CONCEPT -> "#4F8CFF"
                },
                "borderWidth" to 1.5,
                "shadowBlur" to when (node.type) {
                    NodeType.DOCUMENT -> 16
                    NodeType.INSPIRATION -> 20
                    NodeType.CONCEPT -> 24
                },
                "shadowColor" to when (node.type) {
                    NodeType.DOCUMENT -> "rgba(142,197,255,0.45)"
                    NodeType.INSPIRATION -> "rgba(240,198,116,0.55)"
                    NodeType.CONCEPT -> "rgba(54,117,255,0.65)"
                }
            )
        )
    }
    val edges = data.edges.map { edge ->
        mapOf(
            "source" to edge.sourceId,
            "target" to edge.targetId,
            "relation" to edge.relation.name,
            "confidence" to edge.confidence,
            "lineStyle" to mapOf(
                "color" to when (edge.relation) {
                    RelationType.SUPPORT -> "#34A853"
                    RelationType.CHALLENGE -> "#EA4335"
                    RelationType.SUPPLEMENT -> "#002FA7"
                    RelationType.CITE -> "#888888"
                },
                "type" to when (edge.relation) {
                    RelationType.SUPPORT, RelationType.CHALLENGE -> "dashed"
                    RelationType.SUPPLEMENT -> "solid"
                    RelationType.CITE -> "dotted"
                },
                "opacity" to (0.3f + edge.confidence.coerceIn(0f, 1f) * 0.65f),
                "width" to (1.2f + edge.confidence.coerceIn(0f, 1f) * 0.8f)
            )
        )
    }
    return graphJsonAdapter.toJson(mapOf("nodes" to nodes, "edges" to edges))
}
