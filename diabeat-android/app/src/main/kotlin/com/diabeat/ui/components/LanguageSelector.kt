package com.diabeat.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.diabeat.utils.LanguageManager

/**
 * 语言选择器组件
 * 用于在应用内切换语言
 * 支持出海应用的多语言需求
 */
@Composable
fun LanguageSelector(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    onLanguageSelected: (String) -> Unit
) {
    if (showDialog) {
        LanguageSelectionDialog(
            onDismiss = onDismiss,
            onLanguageSelected = onLanguageSelected
        )
    }
}

/**
 * 语言选择对话框
 */
@Composable
private fun LanguageSelectionDialog(
    onDismiss: () -> Unit,
    onLanguageSelected: (String) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // 标题
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = "Language",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Select Language",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                // 语言选项
                val languages = LanguageManager.getSupportedLanguages()
                languages.forEach { language ->
                    LanguageOptionItem(
                        languageCode = language.code,
                        displayName = language.displayName,
                        onSelected = {
                            onLanguageSelected(language.code)
                            onDismiss()
                        }
                    )
                    if (language != languages.last()) {
                        Divider(
                            modifier = Modifier.padding(vertical = 4.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 取消按钮
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Cancel")
                }
            }
        }
    }
}

/**
 * 语言选项项
 */
@Composable
private fun LanguageOptionItem(
    languageCode: String,
    displayName: String,
    onSelected: () -> Unit
) {
    val isCurrentLanguage = LanguageManager.getCurrentLanguage(
        LocalContext.current
    ) == languageCode
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelected() }
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 语言图标（使用国旗emoji简化表示）
            Text(
                text = when (languageCode) {
                    "en" -> "🇺🇸"
                    "zh" -> "🇨🇳"
                    else -> "🌍"
                },
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(end = 12.dp)
            )
            
            Column {
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isCurrentLanguage) FontWeight.Bold else FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (isCurrentLanguage) {
                    Text(
                        text = "Current",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        
        if (isCurrentLanguage) {
            Icon(
                imageVector = Icons.Default.Done,
                contentDescription = "Selected",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * 语言切换按钮组件
 * 可以放置在设置页面或工具栏中
 */
@Composable
fun LanguageSwitchButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentLanguage = LanguageManager.getCurrentLanguageDisplayName(
        LocalContext.current
    )
    
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.primary
        )
    ) {
        Icon(
            imageVector = Icons.Default.Language,
            contentDescription = "Language",
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = currentLanguage,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}