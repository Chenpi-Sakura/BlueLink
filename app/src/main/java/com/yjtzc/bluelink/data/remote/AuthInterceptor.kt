package com.yjtzc.bluelink.data.remote

import com.yjtzc.bluelink.BuildConfig
import com.yjtzc.bluelink.data.local.prefs.UserPreferences
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import java.util.UUID

/**
 * OkHttp 拦截器 — 为所有请求注入认证 Header（V2.1 §4.2）
 *
 * V2.1 MVP 阶段：匿名 UUID + X-User-Id，不做 Bearer Token。
 * V2.x 升级路径见 V2.1 §4.4。
 */
class AuthInterceptor(
    private val userPreferences: UserPreferences
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .addHeader("X-User-Id", getUserId())
            .addHeader("X-Client-Version", BuildConfig.VERSION_NAME)
            .addHeader("X-Platform", "android")
            .build()
        return chain.proceed(request)
    }

    /**
     * 获取或生成匿名 User ID。
     * 首次启动生成 UUID 并持久化到 DataStore，之后复用。
     */
    private fun getUserId(): String {
        return runBlocking {
            val existing = userPreferences.userId.firstOrNull()
            if (existing.isNullOrBlank()) {
                val newId = UUID.randomUUID().toString()
                userPreferences.setUserId(newId)
                newId
            } else {
                existing
            }
        }
    }
}
