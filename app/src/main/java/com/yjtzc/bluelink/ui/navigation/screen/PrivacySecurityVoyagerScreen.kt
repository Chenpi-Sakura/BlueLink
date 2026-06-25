package com.yjtzc.bluelink.ui.navigation.screen

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.yjtzc.bluelink.ui.mine.MineViewModel
import com.yjtzc.bluelink.ui.mine.PrivacySecurityScreen
import com.yjtzc.bluelink.ui.navigation.BlueLinkViewModelFactory
import com.yjtzc.bluelink.util.LocalAppContainer

class PrivacySecurityVoyagerScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val container = LocalAppContainer.current
        PrivacySecurityScreen(
            viewModel = viewModel(factory = BlueLinkViewModelFactory(container)),
            onBack = { navigator.pop() },
            onNavigateToPermission = { navigator.push(PermissionManagementVoyagerScreen()) },
            onNavigateToDataExport = { navigator.push(DataExportVoyagerScreen()) },
            onNavigateToPermanentDelete = { navigator.push(PermanentDeleteVoyagerScreen()) }
        )
    }
}
