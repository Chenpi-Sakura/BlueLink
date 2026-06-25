package com.yjtzc.bluelink.ui.navigation.screen

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.yjtzc.bluelink.ui.mine.MineScreen
import com.yjtzc.bluelink.ui.navigation.BlueLinkViewModelFactory
import com.yjtzc.bluelink.ui.mine.MineViewModel
import com.yjtzc.bluelink.util.LocalAppContainer

/**
 * 我的 Tab 根页面
 *
 * 内嵌 Navigator 的根 Screen，在 Content() 中调用原 MineScreen，
 * 6 个 onNavigateToXxx 回调都用 navigator.push(XXXVoyagerScreen()) 替代原 state 设置
 */
class MineMainScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val container = LocalAppContainer.current
        MineScreen(
            viewModel = viewModel(factory = BlueLinkViewModelFactory(container)),
            onNavigateToAppearance = { navigator.push(AppearanceVoyagerScreen()) },
            onNavigateToCognitive = { navigator.push(CognitiveVoyagerScreen()) },
            onNavigateToPrivacySecurity = { navigator.push(PrivacySecurityVoyagerScreen()) },
            onNavigateToPermission = { navigator.push(PermissionManagementVoyagerScreen()) },
            onNavigateToDataExport = { navigator.push(DataExportVoyagerScreen()) },
            onNavigateToPermanentDelete = { navigator.push(PermanentDeleteVoyagerScreen()) },
            modifier = Modifier.fillMaxSize()
        )
    }
}
