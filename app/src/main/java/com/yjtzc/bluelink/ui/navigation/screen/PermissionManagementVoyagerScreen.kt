package com.yjtzc.bluelink.ui.navigation.screen

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.yjtzc.bluelink.ui.mine.PermissionManagementScreen

class PermissionManagementVoyagerScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        PermissionManagementScreen(onBack = { navigator.pop() })
    }
}
