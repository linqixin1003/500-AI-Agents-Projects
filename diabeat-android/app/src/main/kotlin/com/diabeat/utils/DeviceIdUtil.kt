package com.diabeat.utils

import android.content.Context
import android.os.Build
import android.provider.Settings
import java.security.MessageDigest

object DeviceIdUtil {

    private var deviceId: String? = null

    fun init(context: Context) {
        if (deviceId == null) {
            deviceId = generateUniqueDeviceId(context)
        }
    }

    fun getDeviceId(): String {
        return deviceId ?: throw IllegalStateException("DeviceIdUtil must be initialized with context before use.")
    }

    private fun generateUniqueDeviceId(context: Context): String {
        val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        val manufacturer = Build.MANUFACTURER
        val model = Build.MODEL

        val combinedId = androidId + manufacturer + model
        return sha256(combinedId)
    }

    private fun sha256(input: String): String {
        return try {
            val bytes = input.toByteArray(Charsets.UTF_8)
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(bytes)
            digest.fold("", { str, it -> str + "%02x".format(it) })
        } catch (e: Exception) {
            // Fallback or error handling in case of hashing failure
            // For now, return a simpler hash or a constant to avoid crashes
            "fallback_device_id_hash_" + input.hashCode()
        }
    }
}
