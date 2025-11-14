package com.diabeat.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.diabeat.R
import com.diabeat.ui.BottomTab
import com.diabeat.ui.home.HomeScreen
import com.diabeat.ui.home.NewHomeScreen
import com.diabeat.ui.mine.MineScreen
import com.diabeat.viewmodel.HomeViewModel

/**
 * 主屏幕
 * 包含底部导航栏和页面切换
 * 模仿 rock-android 的设计风格
 */
@Composable
fun MainScreen(
    homeViewModel: HomeViewModel,
    onNavigateToCamera: (mealType: String?) -> Unit, // 修改为接受餐次类型参数
    onNavigateToFoodSearch: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToProfile: () -> Unit
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    Box(
        Modifier.background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(Modifier.fillMaxSize()) {
            // 主内容区域
            Box(Modifier.weight(1f)) {
                when (selectedTabIndex) {
                    0 -> NewHomeScreen(
                        homeViewModel = homeViewModel,
                        onNavigateToCamera = onNavigateToCamera,
                        onNavigateToFoodSearch = onNavigateToFoodSearch
                    )
                    1 -> MineScreen(
                        onNavigateToSettings = onNavigateToSettings,
                        onNavigateToProfile = onNavigateToProfile
                    )
                }
            }
            // 底部导航栏占位空间
            Spacer(Modifier.height(60.dp))
        }

        // 底部导航栏容器
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(94.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            // 底部导航背景（如果有的话）
            Icon(
                painter = painterResource(R.drawable.bg_tab_group),  // 需要添加背景图
                contentDescription = "",
                tint = Color.Unspecified,
                modifier = Modifier.fillMaxSize()
            )

            // 底部导航栏
            BottomTab(selectedTabIndex) { index ->
                selectedTabIndex = index
            }

            // 中间的相机按钮
            Column(
                Modifier
                    .background(Color.Transparent)
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(10.dp))
                Surface(
                    modifier = Modifier.size(68.dp),
                    shape = CircleShape,
                    onClick = { onNavigateToCamera(null) } // 从这里点击相机不指定餐次
                ) {
                    Icon(
                        painterResource(R.drawable.icon_camera),  // 需要添加相机图标
                        modifier = Modifier.fillMaxSize(),
                        tint = Color.Unspecified,
                        contentDescription = "camera"
                    )
                }
            }
        }
    }
}
