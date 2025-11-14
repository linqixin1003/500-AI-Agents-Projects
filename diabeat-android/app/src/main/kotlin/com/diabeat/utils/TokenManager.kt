package com.diabeat.utils

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import com.diabeat.data.model.DeviceAuthRequest
import com.diabeat.data.model.LoginResponse
import com.diabeat.network.ApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object TokenManager {
    private const val PREFS_NAME = "diabeat_prefs"
    private const val KEY_TOKEN = "access_token"
    private const val KEY_USER_ID = "user_id"
    private const val DEFAULT_DIABETES_TYPE = "type2"

    @Volatile
    private var cachedToken: String? = null
    @Volatile
    private var cachedUserId: String? = null

    fun init(context: Context) {
        val prefs = getSharedPreferences(context)
        cachedToken = prefs.getString(KEY_TOKEN, null)
        cachedUserId = prefs.getString(KEY_USER_ID, null)
    }

    private fun getSharedPreferences(context: Context): SharedPreferences {
        val appContext = context.applicationContext ?: context
        return appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun saveToken(context: Context, token: String) {
        cachedToken = token
        getSharedPreferences(context).edit().putString(KEY_TOKEN, token).apply()
    }

    fun getToken(context: Context): String? {
        val existing = cachedToken
        if (!existing.isNullOrBlank()) {
            return existing
        }
        val stored = getSharedPreferences(context).getString(KEY_TOKEN, null)
        cachedToken = stored
        return stored
    }

    fun clearToken(context: Context) {
        cachedToken = null
        cachedUserId = null
        getSharedPreferences(context).edit().remove(KEY_TOKEN).remove(KEY_USER_ID).apply()
    }

    fun saveUserId(context: Context, userId: String) {
        cachedUserId = userId
        getSharedPreferences(context).edit().putString(KEY_USER_ID, userId).apply()
    }

    fun getUserId(context: Context): String? {
        val existing = cachedUserId
        if (!existing.isNullOrBlank()) {
            return existing
        }
        val stored = getSharedPreferences(context).getString(KEY_USER_ID, null)
        cachedUserId = stored
        return stored
    }

    suspend fun ensureAuthenticated(
        context: Context,
        apiService: ApiService,
        forceRefresh: Boolean = false
    ): LoginResponse = withContext(Dispatchers.IO) {
        val appContext = context.applicationContext ?: context
        if (!forceRefresh) {
            val token = getToken(appContext)
            val userId = getUserId(appContext)
            if (!token.isNullOrBlank() && !userId.isNullOrBlank()) {
                runCatching { apiService.getCurrentUser() }
                    .onSuccess { response ->
                        if (response.isSuccessful && response.body() != null) {
                            return@withContext LoginResponse(
                                access_token = token,
                                token_type = "bearer",
                                user = response.body()!!
                            )
                        }
                    }
                // 如果获取用户信息失败，继续尝试刷新令牌
            }
        }

        // 确保设备ID已初始化
        DeviceIdUtil.init(appContext)
        val deviceId = DeviceIdUtil.getDeviceId()
        val authRequest = DeviceAuthRequest(
            device_id = deviceId,
            diabetes_type = DEFAULT_DIABETES_TYPE,
            name = Build.MODEL
        )
        val response = apiService.deviceAuth(authRequest)
        if (response.isSuccessful && response.body() != null) {
            val body = response.body()!!
            saveToken(appContext, body.access_token)
            saveUserId(appContext, body.user.id)
            return@withContext body
        } else {
            clearToken(appContext)
            val errorMessage = response.errorBody()?.string()?.takeIf { it.isNotBlank() }
                ?: response.message()
            throw IllegalStateException("设备认证失败: $errorMessage")
        }
    }
}
