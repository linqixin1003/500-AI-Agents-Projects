package com.diabeat.ui.mine

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.diabeat.R

/**
 * 我的页面 - 模仿 rock-android 设计
 */
@Composable
fun MineScreenNew(
    onNavigateToSettings: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToHistory: () -> Unit = {},
    onNavigateToAbout: () -> Unit = {},
    onShare: () -> Unit = {}
) {
    val scrollState = rememberScrollState()

    Box(modifier = Modifier.fillMaxSize()) {
        // 背景图片
        Image(
            painter = painterResource(R.drawable.bg_tab_group), // 可以换成其他背景
            contentScale = ContentScale.FillWidth,
            modifier = Modifier.fillMaxWidth(),
            contentDescription = ""
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            // 顶部状态栏空间
            Spacer(Modifier.height(40.dp))

            // 头部：标题和操作按钮
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .height(80.dp)
                    .padding(horizontal = 20.dp)
                    .fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.tab_mine),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
                Row(horizontalArrangement = Arrangement.End) {
                    IconButton(onClick = onShare) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(Modifier.width(4.dp))
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // 主要内容卡片
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    // 用户资料部分
                    UserProfileSection(onClick = onNavigateToProfile)

                    Spacer(Modifier.height(24.dp))

                    // 功能列表
                    Text(
                        text = stringResource(R.string.settings),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    MenuItemRow(
                        icon = Icons.Default.Person,
                        title = stringResource(R.string.user_parameters),
                        subtitle = stringResource(R.string.user_parameters_desc),
                        onClick = onNavigateToProfile
                    )

                    Divider(modifier = Modifier.padding(vertical = 8.dp))

                    MenuItemRow(
                        icon = Icons.Default.History,
                        title = stringResource(R.string.meal_history),
                        subtitle = stringResource(R.string.meal_history_desc),
                        onClick = onNavigateToHistory
                    )

                    Divider(modifier = Modifier.padding(vertical = 8.dp))

                    MenuItemRow(
                        icon = Icons.Default.Settings,
                        title = stringResource(R.string.settings),
                        subtitle = stringResource(R.string.settings_desc),
                        onClick = onNavigateToSettings
                    )

                    Divider(modifier = Modifier.padding(vertical = 8.dp))

                    MenuItemRow(
                        icon = Icons.Default.Info,
                        title = stringResource(R.string.about),
                        subtitle = stringResource(R.string.app_version),
                        onClick = onNavigateToAbout
                    )

                    Spacer(Modifier.height(80.dp)) // 底部导航栏空间
                }
            }
        }
    }
}

@Composable
private fun UserProfileSection(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 用户头像
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .background(
                        MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(30.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Profile",
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(Modifier.width(16.dp))

            // 用户信息
            Column {
                Text(
                    text = stringResource(R.string.user_profile),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.click_to_edit),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun MenuItemRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(28.dp)
        )
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = "Navigate",
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            modifier = Modifier.size(20.dp)
        )
    }
}
