package com.diabeat.ui.mine

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import kotlinx.coroutines.launch

/**
 * 我的页面 - 完全照搬 rock-android 设计
 * 包含3个Tab: 收藏、心愿单、历史记录
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MineScreen(
    onNavigateToSettings: () -> Unit,
    onNavigateToProfile: () -> Unit
) {
    val pagerState = rememberPagerState(0, 0f) { 3 }
    val scope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize()) {
        // 背景图
        Image(
            painter = painterResource(R.drawable.bg_tab_group),
            contentScale = ContentScale.FillWidth,
            modifier = Modifier.fillMaxWidth(),
            contentDescription = ""
        )

        Column(modifier = Modifier.fillMaxSize()) {
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
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Row(horizontalArrangement = Arrangement.End) {
                    IconButton(onClick = { /* Share */ }) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }

            // 主内容区域 - 圆角背景
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            MaterialTheme.colorScheme.background,
                            RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
                        )
                        .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                ) {
                    // Tab标题行
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                            .padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TabTitle(
                            text = stringResource(R.string.collection_tab),
                            isSelected = pagerState.currentPage == 0,
                            onClick = { scope.launch { pagerState.animateScrollToPage(0) } },
                            modifier = Modifier.weight(1f)
                        )
                        
                        TabTitle(
                            text = stringResource(R.string.wish_list_tab),
                            isSelected = pagerState.currentPage == 1,
                            onClick = { scope.launch { pagerState.animateScrollToPage(1) } },
                            modifier = Modifier.weight(1f)
                        )
                        
                        TabTitle(
                            text = stringResource(R.string.history_tab),
                            isSelected = pagerState.currentPage == 2,
                            onClick = { scope.launch { pagerState.animateScrollToPage(2) } },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // HorizontalPager 内容
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { page ->
                        when (page) {
                            0 -> CollectionPage()
                            1 -> WishListPage()
                            2 -> HistoryPage()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TabTitle(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .padding(horizontal = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 16.sp,
            maxLines = 1,
            modifier = Modifier.clickable { onClick() },
            color = if (isSelected)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.onBackground,
            fontWeight = if (isSelected)
                FontWeight.SemiBold
            else
                FontWeight.Normal
        )
    }
}

@Composable
private fun CollectionPage() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            EmptyStateCard("暂无收藏")
        }
    }
}

@Composable
private fun WishListPage() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            EmptyStateCard("暂无心愿单")
        }
    }
}

@Composable
private fun HistoryPage() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            EmptyStateCard("暂无历史记录")
        }
    }
}

@Composable
private fun EmptyStateCard(message: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(48.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = message,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Normal
            )
        }
    }
}
