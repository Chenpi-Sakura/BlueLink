package com.yjtzc.bluelink

import android.content.Context
import com.yjtzc.bluelink.data.local.crypto.SecurePrefs
import com.yjtzc.bluelink.data.local.db.AppDatabase
import com.yjtzc.bluelink.data.local.prefs.UserPreferences
import com.yjtzc.bluelink.data.remote.BlueLinkApi
import com.yjtzc.bluelink.data.remote.AuthInterceptor
import com.yjtzc.bluelink.data.repository.AnchorRepository
import com.yjtzc.bluelink.data.repository.CaptureRepository
import com.yjtzc.bluelink.data.repository.DocumentRepository
import com.yjtzc.bluelink.data.repository.FeynmanRepository
import com.yjtzc.bluelink.data.repository.GraphRepository
import com.yjtzc.bluelink.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

/**
 * 手写 DI 容器 — 替代 Hilt（V2.0 §4.1.1）
 *
 * 整个 App 唯一的依赖注入入口，在 [BlueLinkApplication.onCreate] 中实例化。
 * 优点：对 AI 友好、对人友好、总共不到 100 行。
 */
class AppContainer(private val appContext: Context) {

    // ====== 0. JSON 解析器 ======
    val moshi: Moshi by lazy {
        Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }

    // ====== 1. 偏好存储 ======
    val userPreferences: UserPreferences by lazy { UserPreferences(appContext) }
    val securePrefs: SecurePrefs by lazy { SecurePrefs(appContext) }

    // ====== 2. OkHttp + Retrofit ======
    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(userPreferences))
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = if (BuildConfig.DEBUG)
                        HttpLoggingInterceptor.Level.BODY
                    else
                        HttpLoggingInterceptor.Level.NONE
                }
            )
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.BACKEND_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    val api: BlueLinkApi by lazy { retrofit.create(BlueLinkApi::class.java) }

    // ====== 3. Room 数据库 ======
    val database: AppDatabase by lazy {
        AppDatabase.build(appContext)
    }

    // ====== 4. Repository（单例） ======
    val documentRepository: DocumentRepository by lazy {
        DocumentRepository(api, database.documentDao(), database.segmentDao(), securePrefs)
    }
    val anchorRepository: AnchorRepository by lazy {
        AnchorRepository(api, database.anchorDao(), database.segmentDao())
    }
    val graphRepository: GraphRepository by lazy {
        GraphRepository(api, database.graphNodeDao(), database.graphEdgeDao())
    }
    val captureRepository: CaptureRepository by lazy {
        CaptureRepository(api, database.inspirationDao(), securePrefs)
    }
    val feynmanRepository: FeynmanRepository by lazy {
        FeynmanRepository(api)
    }

    // ====== 5. 原生能力 DataSource（懒加载，用到才初始化） ======
    // OCR 和 STT 在需要时才初始化（避免 Application.onCreate 过重）
    // 后续在 CaptureViewModel 中通过 container 获取
}
