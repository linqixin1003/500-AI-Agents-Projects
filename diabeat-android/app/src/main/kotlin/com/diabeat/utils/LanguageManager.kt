package com.diabeat.utils

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.LocaleList
import java.util.*

/**
 * 多语言管理器
 * 用于管理应用的语言切换功能
 * 支持动态语言切换和持久化存储
 */
object LanguageManager {
    
    // 支持的语言列表
    enum class Language(val code: String, val displayName: String) {
        ENGLISH("en", "English"),
        CHINESE("zh", "中文")
    }
    
    // 当前语言设置
    private const val PREF_LANGUAGE_KEY = "app_language_preference"
    private const val DEFAULT_LANGUAGE = "en" // 默认英文
    
    /**
     * 获取当前应用语言
     */
    fun getCurrentLanguage(context: Context): String {
        val sharedPref = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        return sharedPref.getString(PREF_LANGUAGE_KEY, DEFAULT_LANGUAGE) ?: DEFAULT_LANGUAGE
    }
    
    /**
     * 设置应用语言
     */
    fun setLanguage(context: Context, languageCode: String) {
        // 保存语言偏好
        val sharedPref = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        sharedPref.edit().putString(PREF_LANGUAGE_KEY, languageCode).apply()
        
        // 更新应用语言
        updateAppLanguage(context, languageCode)
    }
    
    /**
     * 更新应用语言配置
     */
    fun updateAppLanguage(context: Context, languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        
        val resources = context.resources
        val configuration = Configuration(resources.configuration)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val localeList = LocaleList(locale)
            LocaleList.setDefault(localeList)
            configuration.setLocales(localeList)
        } else {
            configuration.locale = locale
        }
        
        @Suppress("DEPRECATION")
        resources.updateConfiguration(configuration, resources.displayMetrics)
    }
    
    /**
     * 获取系统语言
     */
    fun getSystemLanguage(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            LocaleList.getDefault().get(0).language
        } else {
            Locale.getDefault().language
        }
    }
    
    /**
     * 初始化应用语言
     * 在Application的onCreate中调用
     */
    fun initializeLanguage(context: Context) {
        val savedLanguage = getCurrentLanguage(context)
        updateAppLanguage(context, savedLanguage)
    }
    
    /**
     * 获取语言对应的Locale对象
     */
    fun getLocale(languageCode: String): Locale {
        return when (languageCode) {
            Language.CHINESE.code -> Locale.CHINESE
            else -> Locale.ENGLISH
        }
    }
    
    /**
     * 获取所有支持的语言
     */
    fun getSupportedLanguages(): List<Language> {
        return Language.values().toList()
    }
    
    /**
     * 检查是否支持指定语言
     */
    fun isLanguageSupported(languageCode: String): Boolean {
        return Language.values().any { it.code == languageCode }
    }
    
    /**
     * 获取当前语言的显示名称
     */
    fun getCurrentLanguageDisplayName(context: Context): String {
        val currentLang = getCurrentLanguage(context)
        return getSupportedLanguages().find { it.code == currentLang }?.displayName ?: "English"
    }
    
    /**
     * 创建指定语言的Context
     * 用于Activity重建时的语言保持
     */
    fun createLanguageContext(context: Context, languageCode: String): Context {
        val locale = getLocale(languageCode)
        val configuration = Configuration(context.resources.configuration)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val localeList = LocaleList(locale)
            configuration.setLocales(localeList)
        } else {
            configuration.locale = locale
        }
        
        return context.createConfigurationContext(configuration)
    }
}