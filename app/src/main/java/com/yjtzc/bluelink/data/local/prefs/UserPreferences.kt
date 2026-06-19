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
    }
}

/**
 * DataStore 单例扩展（与 AppContainer.kt 中声明一致）
 */
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "bluelink_settings")
