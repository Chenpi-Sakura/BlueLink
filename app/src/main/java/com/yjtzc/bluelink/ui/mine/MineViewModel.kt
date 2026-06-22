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
    val privacyMode: String = "LOCAL_ONLY",
    val companionStyle: String = "GENTLE",
    val terminologyTags: String = "[]"
)

class MineViewModel(
    private val userPreferences: UserPreferences,
    private val securePrefs: SecurePrefs
) : ViewModel() {

    private val _profile = MutableStateFlow(ProfileState())
    val profile: StateFlow<ProfileState> = _profile.asStateFlow()

    private val _showPrivacyLevelPicker = MutableStateFlow(false)
    val showPrivacyLevelPicker: StateFlow<Boolean> = _showPrivacyLevelPicker.asStateFlow()

    private val _showConfirmPrivacyDialog = MutableStateFlow(false)
    val showConfirmPrivacyDialog: StateFlow<Boolean> = _showConfirmPrivacyDialog.asStateFlow()

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
                userPreferences.privacyMode,
                userPreferences.companionStyle,
                userPreferences.terminologyTags
            ) { values ->
                ProfileState(
                    major = values[0] as String,
                    terminology = values[1] as String,
                    defaultGranularity = values[2] as String,
                    directnessLevel = values[3] as Float,
                    exploreDepth = values[4] as Boolean,
                    privacyMode = values[5] as String,
                    companionStyle = values[6] as String,
                    terminologyTags = values[7] as String
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

    fun setCompanionStyle(style: String) {
        viewModelScope.launch { userPreferences.setCompanionStyle(style) }
    }

    fun setTerminologyTags(tagsJson: String) {
        viewModelScope.launch { userPreferences.setTerminologyTags(tagsJson) }
    }

    fun editProfile(major: String, terminology: String) {
        viewModelScope.launch { userPreferences.setProfile(major, terminology) }
    }

    // ====== 隐私等级切换 ======

    fun showPrivacyLevelPicker() {
        _showPrivacyLevelPicker.value = true
    }

    fun hidePrivacyLevelPicker() {
        _showPrivacyLevelPicker.value = false
    }

    fun onPrivacyLevelSelected(level: String) {
        _showPrivacyLevelPicker.value = false
        val current = _profile.value.privacyMode

        // 从低风险切到 CLOUD_OK 需要二次确认
        if ((current == "LOCAL_ONLY" || current == "LOCAL_FIRST") && level == "CLOUD_OK") {
            _showConfirmPrivacyDialog.value = true
        } else {
            setPrivacyMode(level)
        }
    }

    fun confirmPrivacyChangeToCloud() {
        _showConfirmPrivacyDialog.value = false
        setPrivacyMode("CLOUD_OK")
    }

    fun cancelPrivacyChange() {
        _showConfirmPrivacyDialog.value = false
    }

    fun exportData() {
        // TODO: V2.1 数据导出：将所有 Room + SecurePrefs 数据导出为加密 JSON
    }

    fun confirmWipe() {
        // TODO: V2.1 永久删除：清空 Room + SecurePrefs + DataStore
    }
}
