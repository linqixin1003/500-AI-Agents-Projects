package com.diabeat.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// 深色模式配色方案
private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    primaryContainer = DarkPrimaryDark,
    secondary = Secondary,
    secondaryContainer = SecondaryDark,
    background = DarkBackground,
    surface = DarkSurface,
    error = Error,
    onPrimary = DarkBackground,
    onSecondary = DarkBackground,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onError = DarkBackground
)

// 浅色模式配色方案
private val LightColorScheme = lightColorScheme(
    primary = Primary,
    primaryContainer = PrimaryLight,
    secondary = Secondary,
    secondaryContainer = SecondaryLight,
    background = Background,
    surface = Surface,
    error = Error,
    onPrimary = Surface,
    onSecondary = Surface,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onError = Surface
)

/**
 * DiabEat应用的主题组件，提供浅色和深色模式支持
 * 使用蓝色和绿色作为主色调，代表健康和信任感
 * 橙色作为辅助色，提供活力和能量的视觉感受
 */
@Composable
fun DiabEatTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    
    // 设置系统UI样式，使状态栏与应用主题一致
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            // 确保状态栏文字颜色与背景形成良好对比
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

