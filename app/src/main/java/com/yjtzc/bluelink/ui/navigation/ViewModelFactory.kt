package com.yjtzc.bluelink.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.yjtzc.bluelink.AppContainer
import com.yjtzc.bluelink.ui.chat.ChatViewModel
import com.yjtzc.bluelink.ui.home.HomeViewModel
import com.yjtzc.bluelink.ui.reader.ReaderViewModel
import com.yjtzc.bluelink.ui.graph.GraphViewModel
import com.yjtzc.bluelink.ui.capture.CaptureViewModel
import com.yjtzc.bluelink.ui.mine.AppearanceViewModel
import com.yjtzc.bluelink.ui.mine.DataManagementViewModel
import com.yjtzc.bluelink.ui.mine.MineViewModel

/**
 * 统一 ViewModel 工厂 — 从 AppContainer 注入 Repository（V2.0 §4.1.1）
 */
class BlueLinkViewModelFactory(
    private val container: AppContainer
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when {
        modelClass.isAssignableFrom(HomeViewModel::class.java) ->
            HomeViewModel(container.captureRepository, container.trashRepository) as T

        modelClass.isAssignableFrom(ChatViewModel::class.java) ->
            ChatViewModel(container.anchorRepository, container.feynmanRepository) as T

        modelClass.isAssignableFrom(ReaderViewModel::class.java) ->
            ReaderViewModel(container.documentRepository) as T

        modelClass.isAssignableFrom(GraphViewModel::class.java) ->
            GraphViewModel(container.graphRepository) as T

        modelClass.isAssignableFrom(CaptureViewModel::class.java) ->
            CaptureViewModel(container.captureRepository) as T

        modelClass.isAssignableFrom(MineViewModel::class.java) ->
            MineViewModel(container.userPreferences, container.securePrefs) as T

        modelClass.isAssignableFrom(AppearanceViewModel::class.java) ->
            AppearanceViewModel(container.userPreferences) as T

        modelClass.isAssignableFrom(DataManagementViewModel::class.java) ->
            DataManagementViewModel(container.database, container.userPreferences, container.securePrefs) as T

        else -> error("Unknown ViewModel: ${modelClass.name}")
    }
}
