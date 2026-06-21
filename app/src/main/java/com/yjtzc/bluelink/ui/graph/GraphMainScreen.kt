package com.yjtzc.bluelink.ui.graph

import android.annotation.SuppressLint
import android.app.Activity
import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.WebView
import android.webkit.WebChromeClient
import android.webkit.WebViewClient
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.DonutLarge
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.core.view.WindowCompat
import com.yjtzc.bluelink.BuildConfig
import com.yjtzc.bluelink.R
import com.yjtzc.bluelink.data.local.db.NodeType
import com.yjtzc.bluelink.domain.model.GraphNode
import com.yjtzc.bluelink.ui.common.UiState
import com.yjtzc.bluelink.ui.theme.Ink900
import com.yjtzc.bluelink.ui.theme.KleinBlue
import com.yjtzc.bluelink.ui.theme.Parchment50
import com.yjtzc.bluelink.ui.theme.SerifFamily
import kotlinx.coroutines.launch

private val GraphBackground = Color(0xFF1E1E2E)
private val GraphGlass = Color(0xC9111827)
private val GraphGlassBorder = Color(0x47FAF7F2)
private val GraphText = Color(0xE0FAF7F2)
private val GraphMutedText = Color(0x99FAF7F2)

private fun nodeTypeDisplayName(type: NodeType): String = when (type) {
    NodeType.DOCUMENT -> "文献"
    NodeType.INSPIRATION -> "灵感"
    NodeType.CONCEPT -> "概念"
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun GraphScreen(
    viewModel: GraphViewModel,
    modifier: Modifier = Modifier
) {
    val graphState by viewModel.graphState.collectAsStateWithLifecycle()
    val selectedNode by viewModel.selectedNode.collectAsStateWithLifecycle()
    val selectedEdges by viewModel.selectedNodeEdges.collectAsStateWithLifecycle()
    val adjacentNodes by viewModel.selectedAdjacentNodes.collectAsStateWithLifecycle()
    val currentLayout by viewModel.currentLayout.collectAsStateWithLifecycle()
    val dataSource by viewModel.dataSource.collectAsStateWithLifecycle()
    val lastError by viewModel.lastError.collectAsStateWithLifecycle()
    val renderedNodeCount by viewModel.renderedNodeCount.collectAsStateWithLifecycle()
    val renderedEdgeCount by viewModel.renderedEdgeCount.collectAsStateWithLifecycle()
    val scriptStatus by viewModel.scriptStatus.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var webView by remember { mutableStateOf<WebView?>(null) }
    var graphPushed by remember { mutableStateOf(false) }
    var showDebug by remember { mutableStateOf(false) }
    var isWebViewReady by remember { mutableStateOf(false) }
    val view = LocalView.current

    DisposableEffect(view) {
        val window = (view.context as? Activity)?.window
        val previousStatusBarColor = window?.statusBarColor
        val insetsController = window?.let { WindowCompat.getInsetsController(it, view) }
        val previousLightStatusBars = insetsController?.isAppearanceLightStatusBars
        window?.statusBarColor = android.graphics.Color.rgb(7, 17, 38)
        insetsController?.isAppearanceLightStatusBars = false
        onDispose {
            if (previousStatusBarColor != null) window.statusBarColor = previousStatusBarColor
            if (previousLightStatusBars != null) {
                insetsController?.isAppearanceLightStatusBars = previousLightStatusBars
            }
        }
    }

    fun runGraphScript(script: String) {
        if (isWebViewReady) webView?.evaluateJavascript(script, null)
    }

    fun pushGraphData(target: WebView, graphJson: String) {
        val script = """
            (function () {
                if (typeof window.updateGraph !== 'function') return 'missing-updateGraph';
                try {
                    window.updateGraph($graphJson);
                    var main = document.getElementById('main');
                    var currentOption = chart.getOption();
                    var series = currentOption.series && currentOption.series[0];
                    var n = (series && series.data) ? series.data.length : 0;
                    var e = (series && series.links) ? series.links.length : 0;
                    if (window.BlueLinkBridge && window.BlueLinkBridge.onGraphRendered) {
                        window.BlueLinkBridge.onGraphRendered(n, e);
                    }
                    return 'ok ' + Math.max(main.clientWidth || 0, window.innerWidth || 0) +
                        'x' + Math.max(main.clientHeight || 0, window.innerHeight || 0) +
                        ' nodes=' + n + ' edges=' + e;
                } catch (error) {
                    var message = String(error && (error.stack || error.message) || error);
                    if (window.BlueLinkBridge && window.BlueLinkBridge.onGraphError) {
                        window.BlueLinkBridge.onGraphError(message);
                    }
                    return 'error:' + message;
                }
            })();
        """.trimIndent()
        target.evaluateJavascript(script) { result ->
            viewModel.onGraphScriptResult(result)
            // Also try to parse node/edge counts from the result
            if (result != null && result.startsWith("ok ")) {
                val parts = result.split(" ")
                var n = 0; var e = 0
                for (p in parts) {
                    if (p.startsWith("nodes=")) n = p.substringAfter("=").toIntOrNull() ?: 0
                    if (p.startsWith("edges=")) e = p.substringAfter("=").toIntOrNull() ?: 0
                }
                if (n > 0) viewModel.onGraphRendered(n, e)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(GraphBackground)
    ) {
        Image(
            painter = painterResource(R.drawable.graph_starfield),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        when (val state = graphState) {
            is UiState.Loading -> GraphLoadingOverlay()
            is UiState.Error -> GraphErrorState(onRetry = viewModel::loadGraph)
            is UiState.Success -> {
                if (state.data.nodes.isEmpty()) {
                    GraphEmptyState(onRefresh = viewModel::loadGraph)
                } else {
                    val graphJson = remember(state.data) { buildGraphJsonSafe(state.data) }
                    AndroidView(
                        factory = { context ->
                            WebView(context).apply {
                                WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG)
                                isWebViewReady = false
                                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                                alpha = 1f
                                visibility = android.view.View.VISIBLE
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = false
                                settings.allowFileAccess = true
                                settings.allowContentAccess = false
                                settings.blockNetworkLoads = true
                                webViewClient = WebViewClient()
                                webChromeClient = object : WebChromeClient() {
                                    override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
                                        if (consoleMessage.messageLevel() == ConsoleMessage.MessageLevel.ERROR) {
                                            val message = "${consoleMessage.message()} @${consoleMessage.lineNumber()}"
                                            Log.e("BlueLinkGraph", message)
                                            viewModel.onGraphRenderError(message)
                                        }
                                        return true
                                    }
                                }
                                addJavascriptInterface(
                                    GraphJsBridge(
                                        onNodeClick = viewModel::onGraphNodeClicked,
                                        onReady = {
                                            isWebViewReady = true
                                            graphPushed = true
                                            pushGraphData(this, graphJson)
                                            viewModel.onGraphRendered(state.data.nodes.size, state.data.edges.size)
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
                        update = { view ->
                            webView = view
                            // 不重复推送：onReady 已调用 pushGraphData
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            is UiState.Idle -> Unit
        }

        Column(
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Spacer(Modifier.windowInsetsTopHeight(WindowInsets.statusBars))
            GraphTopBar(
                onSearch = { scope.launch { snackbarHostState.showSnackbar("图谱搜索将在后续版本开放") } },
                onRefresh = viewModel::loadGraph,
                onDebugToggle = { showDebug = !showDebug },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
            )
            selectedNode?.let { node ->
                SelectedNodeMiniCard(
                    node = node,
                    edgeCount = selectedEdges.size,
                    adjacentCount = adjacentNodes.size,
                    modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 10.dp)
                )
            }
        }

        if (showDebug) {
            val graphData = (graphState as? UiState.Success)?.data
            GraphDebugOverlay(
                nodeCount = graphData?.nodes?.size ?: 0,
                edgeCount = graphData?.edges?.size ?: 0,
                renderedNodeCount = renderedNodeCount,
                renderedEdgeCount = renderedEdgeCount,
                scriptStatus = scriptStatus,
                source = dataSource.debugLabel,
                webReady = isWebViewReady,
                lastError = lastError,
                onLoadDemo = viewModel::loadDemoGraph,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 100.dp)
            )
        }

        GraphControlBar(
            currentLayout = currentLayout,
            onReset = { runGraphScript("window.resetView && window.resetView()") },
            onFilter = { scope.launch { snackbarHostState.showSnackbar("图谱筛选将在下一轮实现") } },
            onToggleLayout = {
                val next = if (currentLayout == GraphLayout.FORCE) {
                    GraphLayout.CIRCULAR
                } else {
                    GraphLayout.FORCE
                }
                viewModel.onLayoutSelected(next)
                runGraphScript("window.changeLayout('${next.jsValue}')")
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        )

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 24.dp, vertical = 104.dp)
        )
    }
}

@Composable
private fun GraphTopBar(
    onSearch: () -> Unit,
    onRefresh: () -> Unit,
    onDebugToggle: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .shadow(12.dp, RoundedCornerShape(28.dp)),
        shape = RoundedCornerShape(28.dp),
        color = Parchment50.copy(alpha = 0.94f)
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "知识图谱",
                color = Ink900,
                fontFamily = SerifFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 20.sp,
                letterSpacing = 0.2.sp,
                modifier = Modifier.pointerInput(Unit) {
                    detectTapGestures(
                        onLongPress = { onDebugToggle() }
                    )
                }
            )
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onSearch) {
                Icon(
                    Icons.Default.Search,
                    contentDescription = "搜索图谱",
                    tint = Color(0xFF5D6068),
                    modifier = Modifier.size(27.dp)
                )
            }
            IconButton(onClick = onRefresh) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = "刷新图谱",
                    tint = Color(0xFF5D6068),
                    modifier = Modifier.size(27.dp)
                )
            }
        }
    }
}

@Composable
private fun GraphControlBar(
    currentLayout: GraphLayout,
    onReset: () -> Unit,
    onFilter: () -> Unit,
    onToggleLayout: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth(0.84f)
            .height(68.dp)
            .shadow(22.dp, RoundedCornerShape(28.dp))
            .border(BorderStroke(1.dp, GraphGlassBorder), RoundedCornerShape(28.dp)),
        color = GraphGlass,
        shape = RoundedCornerShape(28.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GraphControlButton(
                icon = { Icon(Icons.Default.CenterFocusStrong, null) },
                label = "重置视图",
                onClick = onReset,
                modifier = Modifier.weight(1.1f)
            )
            GraphControlDivider()
            GraphControlButton(
                icon = { Icon(Icons.Default.FilterAlt, null) },
                label = "筛选",
                onClick = onFilter,
                modifier = Modifier.weight(0.85f)
            )
            GraphControlDivider()
            GraphControlButton(
                icon = { Icon(Icons.Default.DonutLarge, null) },
                label = if (currentLayout == GraphLayout.CIRCULAR) "环形布局" else "力导向",
                onClick = onToggleLayout,
                selected = currentLayout == GraphLayout.CIRCULAR,
                modifier = Modifier.weight(1.15f)
            )
        }
    }
}

@Composable
private fun GraphControlButton(
    icon: @Composable () -> Unit,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false
) {
    TextButton(
        onClick = onClick,
        modifier = modifier.height(58.dp),
        shape = RoundedCornerShape(20.dp),
        contentPadding = PaddingValues(horizontal = 2.dp),
        colors = ButtonDefaults.textButtonColors(
            containerColor = if (selected) KleinBlue.copy(alpha = 0.35f) else Color.Transparent,
            contentColor = if (selected) Color(0xFF8FB1FF) else GraphText
        )
    ) {
        Box(Modifier.size(23.dp), contentAlignment = Alignment.Center) { icon() }
        Spacer(Modifier.width(5.dp))
        Text(
            text = label,
            color = if (selected) Color(0xFFB4C8FF) else GraphText,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
    }
}

@Composable
private fun GraphControlDivider() {
    VerticalDivider(
        modifier = Modifier.height(40.dp).width(1.dp),
        color = Color(0x33DCE5F8)
    )
}

@Composable
private fun SelectedNodeMiniCard(
    node: GraphNode,
    edgeCount: Int,
    adjacentCount: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = GraphGlass),
        border = BorderStroke(1.dp, GraphGlassBorder)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(KleinBlue),
                contentAlignment = Alignment.Center
            ) {}
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "已选中：${node.label}",
                    color = GraphText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${nodeTypeDisplayName(node.type)} · 关联 $edgeCount · 相邻节点 $adjacentCount",
                    color = GraphMutedText,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun GraphDebugOverlay(
    nodeCount: Int,
    edgeCount: Int,
    renderedNodeCount: Int,
    renderedEdgeCount: Int,
    scriptStatus: String,
    source: String,
    webReady: Boolean,
    lastError: String?,
    onLoadDemo: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onLoadDemo,
        modifier = modifier,
        color = Color(0xB30B1220),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(0.5.dp, Color(0x33FAF7F2))
    ) {
        Column(Modifier.padding(horizontal = 9.dp, vertical = 6.dp)) {
            Text(
                "nodes: $nodeCount · edges: $edgeCount · source: $source · webReady: $webReady",
                color = Color(0xBFFAF7F2),
                fontSize = 9.sp,
                lineHeight = 11.sp
            )
            Text(
                "rendered: $renderedNodeCount / $renderedEdgeCount · script: $scriptStatus · 点击加载演示图谱",
                color = Color(0x99FAF7F2),
                fontSize = 9.sp,
                lineHeight = 11.sp
            )
            if (!lastError.isNullOrBlank()) {
                Text(
                    "error: ${lastError.take(72)}",
                    color = Color(0xCCF0C674),
                    fontSize = 9.sp,
                    lineHeight = 11.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun GraphLoadingOverlay() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(color = Color(0xFF82A7FF), strokeWidth = 2.dp)
        Spacer(Modifier.height(14.dp))
        Text("正在连接知识星图…", color = GraphMutedText, fontSize = 13.sp)
    }
}

@Composable
private fun GraphEmptyState(onRefresh: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 40.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.DonutLarge,
            contentDescription = null,
            tint = Color(0xFF7892C7),
            modifier = Modifier.size(52.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text("知识星图还是空的", color = GraphText, fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Text("添加文献或灵感后，关联会在这里逐渐生长。", color = GraphMutedText, fontSize = 13.sp)
        Spacer(Modifier.height(12.dp))
        TextButton(onClick = onRefresh) { Text("重新加载", color = Color(0xFF8FB1FF)) }
    }
}

@Composable
private fun GraphErrorState(onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 40.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("暂时无法加载图谱", color = GraphText, fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Text("请检查网络后重试。", color = GraphMutedText, fontSize = 13.sp)
        Spacer(Modifier.height(12.dp))
        TextButton(onClick = onRetry) { Text("重新加载", color = Color(0xFF8FB1FF)) }
    }
}
