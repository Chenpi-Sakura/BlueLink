package com.yjtzc.bluelink.data.local.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * 用户偏好（DataStore）— 画像、粒度、隐私开关、WebDAV 配置等（V2.0 §6.2）
 */
class UserPreferences(private val context: Context) {

    private val dataStore: DataStore<Preferences>
        get() = context.dataStore

    // ====== User ID（V2.1 鉴权） ======

    val userId: Flow<String?> = dataStore.data.map { it[KEY_USER_ID] }

    suspend fun setUserId(id: String) {
        dataStore.edit { it[KEY_USER_ID] = id }
    }

    // ====== 用户画像 ======

    val userProfileMajor: Flow<String> = dataStore.data.map { it[KEY_PROFILE_MAJOR] ?: "" }
    val userProfileTerminology: Flow<String> = dataStore.data.map { it[KEY_PROFILE_TERMINOLOGY] ?: "{}" }

    suspend fun setProfile(major: String, terminology: String) {
        dataStore.edit {
            it[KEY_PROFILE_MAJOR] = major
            it[KEY_PROFILE_TERMINOLOGY] = terminology
        }
    }

    // ====== 认知设置 ======

    val defaultGranularity: Flow<String> = dataStore.data.map { it[KEY_GRANULARITY] ?: "PARAGRAPH" }
    val directnessLevel: Flow<Float> = dataStore.data.map { it[KEY_DIRECTNESS] ?: 0.5f }
    val exploreDepth: Flow<Boolean> = dataStore.data.map { it[KEY_EXPLORE_DEPTH] ?: false }

    suspend fun setGranularity(granularity: String) {
        dataStore.edit { it[KEY_GRANULARITY] = granularity }
    }

    suspend fun setDirectness(level: Float) {
        dataStore.edit { it[KEY_DIRECTNESS] = level.coerceIn(0f, 1f) }
    }

    suspend fun setExploreDepth(enabled: Boolean) {
        dataStore.edit { it[KEY_EXPLORE_DEPTH] = enabled }
    }

    // ====== 隐私 ======

    val privacyMode: Flow<String> = dataStore.data.map { it[KEY_PRIVACY_MODE] ?: "LOCAL_ONLY" }

    suspend fun setPrivacyMode(mode: String) {
        dataStore.edit { it[KEY_PRIVACY_MODE] = mode }
    }

    // ====== WebDAV ======

    val webdavEndpoint: Flow<String?> = dataStore.data.map { it[KEY_WEBDAV_ENDPOINT] }

    suspend fun setWebdavEndpoint(endpoint: String) {
        dataStore.edit { it[KEY_WEBDAV_ENDPOINT] = endpoint }
    }

    // ====== 引导标记 ======

    val hasSeenOnboarding: Flow<Boolean> = dataStore.data.map { it[KEY_ONBOARDING_DONE] ?: false }

    suspend fun markOnboardingDone() {
        dataStore.edit { it[KEY_ONBOARDING_DONE] = true }
    }

    // ====== 外观设置 ======

    val themeMode: Flow<String> = dataStore.data.map { it[KEY_THEME_MODE] ?: "SYSTEM" }

    suspend fun setThemeMode(mode: String) {
        dataStore.edit { it[KEY_THEME_MODE] = mode }
    }

    val dynamicColor: Flow<Boolean> = dataStore.data.map { it[KEY_DYNAMIC_COLOR] ?: false }

    suspend fun setDynamicColor(enabled: Boolean) {
        dataStore.edit { it[KEY_DYNAMIC_COLOR] = enabled }
    }

    val fontScale: Flow<Float> = dataStore.data.map { it[KEY_FONT_SCALE] ?: 1.0f }

    suspend fun setFontScale(scale: Float) {
        dataStore.edit { it[KEY_FONT_SCALE] = scale.coerceIn(0.85f, 1.25f) }
    }

    val highContrast: Flow<Boolean> = dataStore.data.map { it[KEY_HIGH_CONTRAST] ?: false }

    suspend fun setHighContrast(enabled: Boolean) {
        dataStore.edit { it[KEY_HIGH_CONTRAST] = enabled }
    }

    // ====== 伴读风格 ======

    val companionStyle: Flow<String> = dataStore.data.map { it[KEY_COMPANION_STYLE] ?: "GENTLE" }

    suspend fun setCompanionStyle(style: String) {
        dataStore.edit { it[KEY_COMPANION_STYLE] = style }
    }

    // ====== 术语偏好（JSON 数组字符串） ======

    val terminologyTags: Flow<String> = dataStore.data.map { it[KEY_TERMINOLOGY_TAGS] ?: "[]" }

    suspend fun setTerminologyTags(tagsJson: String) {
        dataStore.edit { it[KEY_TERMINOLOGY_TAGS] = tagsJson }
    }

    companion object {
        private val KEY_USER_ID = stringPreferencesKey("user_id")
        private val KEY_PROFILE_MAJOR = stringPreferencesKey("profile_major")
        private val KEY_PROFILE_TERMINOLOGY = stringPreferencesKey("profile_terminology")
        private val KEY_GRANULARITY = stringPreferencesKey("default_granularity")
        private val KEY_DIRECTNESS = floatPreferencesKey("directness_level")
        private val KEY_EXPLORE_DEPTH = booleanPreferencesKey("explore_depth")
        private val KEY_PRIVACY_MODE = stringPreferencesKey("privacy_mode")
        private val KEY_WEBDAV_ENDPOINT = stringPreferencesKey("webdav_endpoint")
        private val KEY_ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")

        // 外观设置
        private val KEY_THEME_MODE = stringPreferencesKey("appearance.theme_mode")
        private val KEY_DYNAMIC_COLOR = booleanPreferencesKey("appearance.dynamic_color")
        private val KEY_FONT_SCALE = floatPreferencesKey("appearance.font_scale")
        private val KEY_HIGH_CONTRAST = booleanPreferencesKey("appearance.high_contrast")

        // 认知设置扩展
        private val KEY_COMPANION_STYLE = stringPreferencesKey("cognitive.companion_style")
        private val KEY_TERMINOLOGY_TAGS = stringPreferencesKey("cognitive.terminology_tags")
    }
}

/**
 * DataStore 单例扩展（与 AppContainer.kt 中声明一致）
 */
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "bluelink_settings")
