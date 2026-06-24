package com.yjtzc.bluelink.ui.mine

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yjtzc.bluelink.data.local.prefs.UserPreferences
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class AppearanceViewModel(
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val _themeMode = MutableStateFlow("SYSTEM")
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    private val _dynamicColor = MutableStateFlow(true)
    val dynamicColor: StateFlow<Boolean> = _dynamicColor.asStateFlow()

    private val _fontScale = MutableStateFlow(1.0f)
    val fontScale: StateFlow<Float> = _fontScale.asStateFlow()

    private val _highContrast = MutableStateFlow(false)
    val highContrast: StateFlow<Boolean> = _highContrast.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                userPreferences.themeMode,
                userPreferences.dynamicColor,
                userPreferences.fontScale,
                userPreferences.highContrast
            ) { modes ->
                AppearanceState(
                    themeMode = modes[0] as String,
                    dynamicColor = modes[1] as Boolean,
                    fontScale = modes[2] as Float,
                    highContrast = modes[3] as Boolean
                )
            }.collect { state ->
                _themeMode.value = state.themeMode
                _dynamicColor.value = state.dynamicColor
                _fontScale.value = state.fontScale
                _highContrast.value = state.highContrast
            }
        }
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch { userPreferences.setThemeMode(mode) }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch { userPreferences.setDynamicColor(enabled) }
    }

    fun setFontScale(scale: Float) {
        _fontScale.value = scale
        viewModelScope.launch { userPreferences.setFontScale(scale) }
    }

    fun setHighContrast(enabled: Boolean) {
        viewModelScope.launch { userPreferences.setHighContrast(enabled) }
    }
}

private data class AppearanceState(
    val themeMode: String = "SYSTEM",
    val dynamicColor: Boolean = true,
    val fontScale: Float = 1.0f,
    val highContrast: Boolean = false
)
