package com.diabeat.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.diabeat.R

/**
 * 底部导航栏组件
 * 包含首页和我的两个Tab
 */
@Composable
fun BottomTab(selectedIndex: Int, onTabSelected: (Int) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
    ) {
        // 首页 Tab
        TabItem(
            selected = R.drawable.ic_home_selected,  // 需要添加图标
            unselected = R.drawable.ic_home_unselected,  // 需要添加图标
            text = stringResource(R.string.tab_home),
            isSelected = selectedIndex == 0
        ) {
            onTabSelected(0)
        }
        
        // 我的 Tab
        TabItem(
            selected = R.drawable.ic_mine_selected,  // 需要添加图标
            unselected = R.drawable.ic_mine_unselected,  // 需要添加图标
            text = stringResource(R.string.tab_mine),
            isSelected = selectedIndex == 1
        ) {
            onTabSelected(1)
        }
    }
}

@Composable
fun TabItem(
    @DrawableRes selected: Int,
    @DrawableRes unselected: Int,
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(136.dp)
            .height(60.dp)
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(
                id = if (isSelected) selected else unselected
            ),
            contentDescription = text,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = text,
            fontSize = 12.sp,
            color = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSecondary
            },
            lineHeight = 12.sp,
            fontWeight = FontWeight.Normal
        )
    }
}
