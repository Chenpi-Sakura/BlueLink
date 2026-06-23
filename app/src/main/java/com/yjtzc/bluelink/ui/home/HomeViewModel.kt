package com.yjtzc.bluelink.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yjtzc.bluelink.data.local.db.CardType
import com.yjtzc.bluelink.data.local.db.InspirationCardEntity
import com.yjtzc.bluelink.data.local.db.PrivacyLevel
import com.yjtzc.bluelink.data.local.db.TrashItemEntity
import com.yjtzc.bluelink.data.repository.CaptureRepository
import com.yjtzc.bluelink.data.repository.TrashRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.UUID

/**
 * 文库页面 ViewModel
 * - 灵感瀑布流
 * - 文件夹 / 文件管理
 * - 回收站
 */
class HomeViewModel(
    private val captureRepository: CaptureRepository,
    private val trashRepository: TrashRepository
) : ViewModel() {

    // ====== 视图模式 ======
    enum class ViewMode { INSPIRATION, FILE }

    private val _viewMode = MutableStateFlow(ViewMode.INSPIRATION)
    val viewMode: StateFlow<ViewMode> = _viewMode.asStateFlow()

    fun toggleViewMode() {
        _viewMode.value = when (_viewMode.value) {
            ViewMode.INSPIRATION -> ViewMode.FILE
            ViewMode.FILE -> ViewMode.INSPIRATION
        }
    }

    // ====== 用户名称 ======
    private val _userName = MutableStateFlow("用户")
    val userName: StateFlow<String> = _userName.asStateFlow()

    fun setUserName(name: String) {
        _userName.value = name
    }

    // ====== 灵感卡片（按 updatedAt 降序） ======

    private val _allCards = MutableStateFlow<List<InspirationCardEntity>>(emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // 当前选中的文件夹 ID（null = 全部）
    private val _selectedFolderId = MutableStateFlow<String?>(null)
    val selectedFolderId: StateFlow<String?> = _selectedFolderId.asStateFlow()

    val cards: StateFlow<List<InspirationCardEntity>> = combine(
        _allCards, _searchQuery, _selectedFolderId
    ) { all, query, folderId ->
        val folderFiltered = if (folderId == null) all
        else all.filter { it.folderId == folderId }
        if (query.isBlank()) folderFiltered
        else folderFiltered.filter { card ->
            card.contentSnippet.contains(query, ignoreCase = true) ||
                    card.tags.contains(query, ignoreCase = true)
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // ====== 回收站 ======
    val trashItems: StateFlow<List<TrashItemEntity>> = trashRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val trashCount: StateFlow<Int> = trashRepository.observeCount()
        .stateIn(viewModelScope, SharingStarted.Lazily, 0)

    // ====== 文件夹 / 文件管理 ======

    data class FileFolder(
        val id: String,
        val name: String,
        val files: List<FileItem>
    )

    data class FileItem(
        val id: String,
        val name: String,
        val type: String,
        val size: Long = 0,
        val wordCount: Int = 0,
        val updatedAt: Long = System.currentTimeMillis()
    ) {
        val sizeFormatted: String
            get() = when {
                size < 1024 -> "${size}B"
                size < 1024 * 1024 -> "${size / 1024}KB"
                else -> "%.1fMB".format(size / (1024.0 * 1024.0))
            }

        val typeLabel: String
            get() = when (type) {
                "word" -> "Word"
                "md" -> "Markdown"
                "txt" -> "纯文本"
                else -> type.uppercase()
            }
    }

    private val _folders = MutableStateFlow<List<FileFolder>>(emptyList())
    val folders: StateFlow<List<FileFolder>> = _folders.asStateFlow()

    init {
        observeCards()
        cleanExpiredTrash()
    }

    private fun observeCards() {
        viewModelScope.launch {
            captureRepository.observeAllCards().collect { entities ->
                _allCards.value = entities
            }
        }
    }

    suspend fun loadCardContent(card: InspirationCardEntity): String? {
        return try {
            captureRepository.readCardContent(card)
        } catch (_: Exception) { null }
    }

    fun onSearchChange(query: String) {
        _searchQuery.value = query
    }

    /**
     * 删除灵感 → 移入回收站（密文保留，可恢复）
     */
    fun deleteCard(card: InspirationCardEntity) {
        viewModelScope.launch {
            trashRepository.moveToTrash(
                originalId = card.id,
                itemType = "INSPIRATION",
                title = card.contentSnippet.take(30).ifBlank { "未命名灵感" },
                contentRef = card.contentRef,
                contentSnippet = card.contentSnippet,
                metadataJson = JSONObject().apply {
                    put("type", card.type.name)
                    put("tags", card.tags)
                    put("privacyLevel", card.privacyLevel.name)
                    put("folderId", card.folderId ?: "")
                }.toString()
            )
            captureRepository.softDeleteCard(card)  // 保留密文，可恢复
        }
    }

    /**
     * 从回收站恢复灵感卡片（回到原来的文件夹）
     */
    fun restoreFromTrash(item: TrashItemEntity) {
        viewModelScope.launch {
            restoreFromTrashSuspend(item)
        }
    }

    /**
     * 恢复并打开 — 可等待的挂起版本（供抽屉点击调用）
     */
    suspend fun restoreFromTrashAndOpen(item: TrashItemEntity) {
        restoreFromTrashSuspend(item)
    }

    private suspend fun restoreFromTrashSuspend(item: TrashItemEntity) {
        try {
            val meta = JSONObject(item.metadataJson ?: "{}")
            val type = CardType.valueOf(meta.optString("type", "TEXT"))
            val tags = meta.optString("tags", "")
            val privacyLevel = PrivacyLevel.valueOf(meta.optString("privacyLevel", "LOCAL_ONLY"))
            val folderId = meta.optString("folderId", "").ifBlank { null }

            captureRepository.restoreCardFromTrash(
                originalId = item.originalId,
                contentRef = item.contentRef ?: "",
                contentSnippet = item.contentSnippet,
                type = type,
                tags = tags,
                privacyLevel = privacyLevel,
                folderId = folderId
            )
        } catch (_: Exception) {
            captureRepository.restoreCardFromTrash(
                originalId = item.originalId,
                contentRef = item.contentRef ?: "",
                contentSnippet = item.contentSnippet,
                type = CardType.TEXT,
                tags = "",
                privacyLevel = PrivacyLevel.LOCAL_ONLY,
                folderId = null
            )
        }
        trashRepository.deleteFromTrash(item)
    }

    /**
     * 永久删除回收站项目
     */
    fun permanentlyDelete(item: TrashItemEntity) {
        viewModelScope.launch {
            trashRepository.permanentlyDelete(item)
        }
    }

    /**
     * 清理过期回收站项目
     */
    fun cleanExpiredTrash() {
        viewModelScope.launch {
            trashRepository.cleanExpired()
        }
    }

    fun createFolder(name: String) {
        val folder = FileFolder(
            id = UUID.randomUUID().toString(),
            name = name,
            files = emptyList()
        )
        _folders.value = _folders.value + folder
    }

    fun deleteFolder(folderId: String) {
        // 将文件夹内的灵感移出文件夹
        viewModelScope.launch {
            captureRepository.moveCardToFolderByFolderId(folderId)
        }
        _folders.value = _folders.value.filter { it.id != folderId }
    }

    fun addFileToFolder(folderId: String, fileName: String, fileType: String) {
        _folders.value = _folders.value.map { folder ->
            if (folder.id == folderId) {
                folder.copy(
                    files = folder.files + FileItem(
                        id = UUID.randomUUID().toString(),
                        name = fileName,
                        type = fileType,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            } else folder
        }
    }

    fun deleteFile(folderId: String, fileId: String) {
        _folders.value = _folders.value.map { folder ->
            if (folder.id == folderId) {
                folder.copy(files = folder.files.filter { it.id != fileId })
            } else folder
        }
    }

    /**
     * 将灵感卡片移入文件夹
     */
    fun moveCardToFolder(card: InspirationCardEntity, folderId: String?) {
        viewModelScope.launch {
            captureRepository.moveCardToFolder(card.id, folderId)
        }
    }

    // ====== 文件夹筛选 ======

    /**
     * 选择文件夹（在灵感视图显示该文件夹内容）
     * @param folderId null = 全部，其他 = 文件夹 ID
     */
    fun selectFolder(folderId: String?) {
        _selectedFolderId.value = folderId
        _viewMode.value = ViewMode.INSPIRATION  // 切回灵感视图
    }

    // ====== 新建灵感快捷入口（底部栏） ======
    enum class CaptureEntry { TEXT, VOICE, IMAGE }

    private val _pendingCapture = MutableStateFlow<CaptureEntry?>(null)
    val pendingCapture: StateFlow<CaptureEntry?> = _pendingCapture.asStateFlow()

    fun requestCapture(entry: CaptureEntry) {
        _pendingCapture.value = entry
    }

    fun consumeCaptureRequest() {
        _pendingCapture.value = null
    }
}
