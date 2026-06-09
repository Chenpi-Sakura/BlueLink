package com.yjtzc.bluelink.data.local.crypto

import android.content.Context
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * 安全偏好存储 — API token、WebDAV 凭据、文档原文密文等（V2.1 §1.5）
 *
 * 主密钥由 Android Keystore 硬件保护，永不离开设备。
 * - 通用凭据：putSecret / getSecret / removeSecret
 * - 原文密文（V2.1 新增）：putCipherText / getCipherText / removeCipherText
 */
class SecurePrefs(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "bluelink_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    // ====== 通用凭据 ======

    fun putSecret(key: String, value: String) =
        prefs.edit().putString(key, value).apply()

    fun getSecret(key: String): String? =
        prefs.getString(key, null)

    fun removeSecret(key: String) =
        prefs.edit().remove(key).apply()

    // ====== V2.1 原文密文 ======

    fun putCipherText(key: String, ciphertext: ByteArray) =
        prefs.edit().putString(key, Base64.encodeToString(ciphertext, Base64.NO_WRAP)).apply()

    fun getCipherText(key: String): ByteArray? =
        prefs.getString(key, null)?.let { Base64.decode(it, Base64.NO_WRAP) }

    fun removeCipherText(key: String) =
        prefs.edit().remove(key).apply()

    // ====== V2.1 命名空间工具 ======

    object Keys {
        fun segment(id: String) = "seg:$id"
        fun card(id: String) = "card:$id"
    }
}
