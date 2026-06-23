package com.yjtzc.bluelink.data.remote

import com.yjtzc.bluelink.BuildConfig
import com.yjtzc.bluelink.data.local.prefs.UserPreferences
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response


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
     * V2.1 MVP 阶段：所有设备使用固定 ID 共享同一份数据。
     * 后续如需多用户，改回随机 UUID 即可。
     */
    private fun getUserId(): String {
        return runBlocking {
            val existing = userPreferences.userId.firstOrNull()
            if (existing.isNullOrBlank()) {
                val newId = "seed-user-0000-0000-0000-000000000000"
                userPreferences.setUserId(newId)
                newId
            } else {
                existing
            }
        }
    }
}
