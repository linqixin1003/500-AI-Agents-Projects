package com.diabeat.ui.base

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import com.diabeat.utils.LanguageManager

/**
 * 基础Activity类
 * 提供多语言支持和国际化功能
 * 所有Activity都应该继承此类以获得语言切换支持
 */
abstract class BaseActivity : ComponentActivity() {
    
    override fun attachBaseContext(newBase: Context) {
        // 在Activity创建时应用保存的语言设置
        val languageCode = LanguageManager.getCurrentLanguage(newBase)
        val context = LanguageManager.createLanguageContext(newBase, languageCode)
        super.attachBaseContext(context)
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 确保语言设置正确应用
        val currentLanguage = LanguageManager.getCurrentLanguage(this)
        LanguageManager.updateAppLanguage(this, currentLanguage)
    }
    
    /**
     * 刷新Activity以应用语言更改
     * 在语言切换后调用
     */
    fun recreateActivity() {
        recreate()
    }
}