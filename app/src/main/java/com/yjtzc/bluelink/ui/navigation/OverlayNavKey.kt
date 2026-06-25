package com.yjtzc.bluelink.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * 覆盖层 Nav3 路由键。
 *
 * 每个覆盖层目的地（阅读器 / 灵感编辑器 / 6 个「我的」子页）都对应一个 data object 或 data class，
 * 由 [androidx.navigation3.ui.NavDisplay] 通过 `entryProvider` 注册并渲染。
 *
 * **为什么覆盖层和底部 Tab 分开**：
 * - 底部 4 Tab 切换是「在同一组根目的地之间跳转」，不是 back stack 行为（点 Chat tab 不应该把 Home 压栈）
 * - 覆盖层（Reader / Editor / Mine 子页）才符合 back stack 模型（层层压栈，系统返回出栈）
 * - 混在一起需要处理 Tab 切换的特殊语义，反而复杂
 *
 * **为什么 [Serializable]**：
 * Nav3 的 [androidx.navigation3.runtime.rememberNavBackStack] 通过 SavedStateRegistry 自动持久化
 * back stack，进程死亡 / 旋转后自动恢复。`@Serializable` 让所有 entry 类型可被 kotlinx-serialization
 * 写入 SavedStateRegistry（无需手写 Saver）。
 *
 * @see OverlayNavGraph 渲染这些 key
 */
@Serializable
sealed interface OverlayNavKey : NavKey {

    /**
     * 覆盖层为空的占位 entry。
     *
     * back stack 始终至少包含 1 个 entry（初始为 [NoOverlay]）。这样设计的好处：
     * - push/pop 都有「上一层」做动画源，双向过渡都自然
     * - 避免「最后一个 entry 直接消失」的无动画退出
     * - 进程死亡恢复时栈总是合法状态（至少 1 个 entry）
     */
    @Serializable
    data object NoOverlay : OverlayNavKey

    /**
     * 阅读器目的地。
     *
     * @param docId 文档 ID（来自 Chat 锚点点击后 segment→doc 的异步解析）
     * @param spotlightSegmentId 高亮片段 ID（可选；非空时进入阅读器后高亮该片段）
     */
    @Serializable
    data class ReaderRoute(
        val docId: String,
        val spotlightSegmentId: String? = null
    ) : OverlayNavKey

    /**
     * 灵感编辑器目的地。
     *
     * @param cardId 灵感卡 ID（来自 Home 卡片点击）
     */
    @Serializable
    data class EditorRoute(
        val cardId: String
    ) : OverlayNavKey

    // ===== 「我的」子页：6 个平铺的 data object =====
    //
    // 不再用 MineRoute sealed interface + mineFromRoute 指针 —— Nav3 back stack 自身编码
    // 「上一层是谁」。例如 PrivacySecurity → PermissionManagement → 系统返回：back stack 自动
    // 从 [NoOverlay, PrivacySecurity, PermissionManagement] 变回 [NoOverlay, PrivacySecurity]，
    // 露出 PrivacySecurity 页面。

    /** 外观设置 */
    @Serializable
    data object Appearance : OverlayNavKey

    /** 认知设置（粒度 / 陪伴度） */
    @Serializable
    data object CognitiveSettings : OverlayNavKey

    /** 隐私与安全（含跳转到「权限管理 / 数据导出 / 永久删除」的入口） */
    @Serializable
    data object PrivacySecurity : OverlayNavKey

    /** 权限管理（系统权限状态） */
    @Serializable
    data object PermissionManagement : OverlayNavKey

    /** 数据导出 */
    @Serializable
    data object DataExport : OverlayNavKey

    /** 永久删除（危险操作） */
    @Serializable
    data object PermanentDelete : OverlayNavKey
}
