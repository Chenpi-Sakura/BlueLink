package com.yjtzc.bluelink.ui.mine

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yjtzc.bluelink.data.local.crypto.SecurePrefs
import com.yjtzc.bluelink.data.local.prefs.UserPreferences
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ProfileState(
    val major: String = "",
    val terminology: String = "{}",
    val defaultGranularity: String = "PARAGRAPH",
    val directnessLevel: Float = 0.5f,
    val exploreDepth: Boolean = false,
    val privacyMode: String = "LOCAL_ONLY"
)

class MineViewModel(
    private val userPreferences: UserPreferences,
    private val securePrefs: SecurePrefs
) : ViewModel() {

    private val _profile = MutableStateFlow(ProfileState())
    val profile: StateFlow<ProfileState> = _profile.asStateFlow()

    init {
        observePreferences()
    }

    private fun observePreferences() {
        viewModelScope.launch {
            combine(
                userPreferences.userProfileMajor,
                userPreferences.userProfileTerminology,
                userPreferences.defaultGranularity,
                userPreferences.directnessLevel,
                userPreferences.exploreDepth,
                userPreferences.privacyMode
            ) { values ->

                ProfileState(
                    major = values[0] as String,
                    terminology = values[1] as String,
                    defaultGranularity = values[2] as String,
                    directnessLevel = values[3] as Float,
                    exploreDepth = values[4] as Boolean,
                    privacyMode = values[5] as String
                )
            }.collect { _profile.value = it }
        }
    }

    fun setGranularity(granularity: String) {
        viewModelScope.launch { userPreferences.setGranularity(granularity) }
    }

    fun setDirectness(level: Float) {
        viewModelScope.launch { userPreferences.setDirectness(level) }
    }

    fun toggleExploreDepth() {
        viewModelScope.launch {
            userPreferences.setExploreDepth(!_profile.value.exploreDepth)
        }
    }

    fun setPrivacyMode(mode: String) {
        viewModelScope.launch { userPreferences.setPrivacyMode(mode) }
    }

    fun editProfile(major: String, terminology: String) {
        viewModelScope.launch { userPreferences.setProfile(major, terminology) }
    }

    fun exportData() {
        // TODO: V2.1 数据导出：将所有 Room + SecurePrefs 数据导出为加密 JSON
    }

    fun confirmWipe() {
        // TODO: V2.1 永久删除：清空 Room + SecurePrefs + DataStore
    }
}
