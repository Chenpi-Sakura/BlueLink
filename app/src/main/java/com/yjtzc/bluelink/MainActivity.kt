package com.yjtzc.bluelink

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import com.yjtzc.bluelink.ui.navigation.BlueLinkNavGraph
import com.yjtzc.bluelink.ui.theme.BlueLinkTheme
import com.yjtzc.bluelink.util.LocalAppContainer

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = (application as BlueLinkApplication).container

        setContent {
            CompositionLocalProvider(LocalAppContainer provides container) {
                BlueLinkTheme {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        BlueLinkNavGraph()
                    }
                }
            }
        }
    }
}
