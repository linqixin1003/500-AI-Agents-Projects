package com.diabeat.ui.home

import androidx.compose.animation.AnimatedVisibility as AnimatedVisibilityAnim
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Healing
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Divider
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.*
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import com.diabeat.R
import com.diabeat.data.model.InsulinRecordResponse
import com.diabeat.data.model.MealRecordResponse
import com.diabeat.data.model.DailyNutritionRecommendation
import com.diabeat.data.model.TodayNutritionIntake
import com.diabeat.ui.components.LanguageSelector
import com.diabeat.ui.components.LanguageSwitchButton
import com.diabeat.ui.base.BaseActivity
import com.diabeat.utils.LanguageManager
import com.diabeat.viewmodel.HomeViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun HomeScreen(
    homeViewModel: HomeViewModel,
    onNavigateToCamera: () -> Unit,
    onNavigateToFoodSearch: () -> Unit
) {
    val selectedDate by homeViewModel.selectedDate.collectAsState()
    val mealRecords by homeViewModel.mealRecords.collectAsState()
    val insulinRecords by homeViewModel.insulinRecords.collectAsState()
    val isLoadingRecords by homeViewModel.isLoadingRecords.collectAsState()
    val errorRecords by homeViewModel.errorRecords.collectAsState()
    val isAuthenticated by homeViewModel.isAuthenticated.collectAsState()
    val currentUser by homeViewModel.user.collectAsState()
    val dailyRecommendation by homeViewModel.dailyRecommendation.collectAsState()
    val todayIntake by homeViewModel.todayIntake.collectAsState()
    val isLoadingNutrition by homeViewModel.isLoadingNutrition.collectAsState()

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    )
    var showDatePicker by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    LaunchedEffect(homeViewModel) {
        homeViewModel.fetchRecordsForDate(selectedDate)
        homeViewModel.fetchNutritionData()
    }

    // 更新 selectedDate 当 DatePickerState 改变时
    LaunchedEffect(datePickerState.selectedDateMillis) {
        datePickerState.selectedDateMillis?.let {
            val newDate = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
            homeViewModel.selectDate(newDate)
        }
    }

    val nutritionSummary = remember(mealRecords) { calculateNutritionEstimates(mealRecords) }
    val calorieTarget = 1800f
    val carbTarget = 250f

    val isRefreshing = isLoadingRecords || isLoadingNutrition
    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = { 
            homeViewModel.fetchRecordsForDate(selectedDate)
            homeViewModel.fetchNutritionData()
        }
    )

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        // 创建渐变背景
        val gradientBrush = Brush.verticalGradient(
            colors = listOf(
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                MaterialTheme.colorScheme.background
            )
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pullRefresh(pullRefreshState)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                    Spacer(modifier = Modifier.height(12.dp))
                    // 增强WelcomeSummary组件，添加动画效果
                    AnimatedVisibilityAnim(
                        visible = true,
                        enter = fadeIn(animationSpec = tween(500)) + slideInVertically(animationSpec = tween(500))
                    ) {
                        WelcomeSummary(
                            mealCount = mealRecords.size,
                            insulinCount = insulinRecords.size,
                            calories = nutritionSummary.calories,
                            carbs = nutritionSummary.carbs,
                            calorieTarget = calorieTarget,
                            carbTarget = carbTarget,
                            onNavigateToCamera = onNavigateToCamera,
                            onNavigateToFoodSearch = onNavigateToFoodSearch,
                            modifier = Modifier
                                .animateContentSize(animationSpec = tween(300))
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // 营养概览卡片
                    if (dailyRecommendation != null || todayIntake != null) {
                        NutritionOverviewCard(
                            dailyRecommendation = dailyRecommendation,
                            todayIntake = todayIntake,
                            isLoading = isLoadingNutrition
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    
                SectionTitle(stringResource(id = R.string.calendar))
                // 在Card作用域中获取触觉反馈对象
                val calendarCardHapticFeedback = LocalHapticFeedback.current
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(elevation = 4.dp, shape = MaterialTheme.shapes.large)
                        .clickable {
                            showDatePicker = true
                            // 添加触觉反馈
                            calendarCardHapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                            Icon(Icons.Filled.DateRange, contentDescription = stringResource(id = R.string.select_date), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = selectedDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                
                    AnimatedVisibilityAnim(visible = showDatePicker, enter = fadeIn(), exit = fadeOut()) {
                    DatePickerDialog(
                        onDismissRequest = { showDatePicker = false },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    showDatePicker = false
                                    datePickerState.selectedDateMillis?.let {
                                        val newDate = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                                        homeViewModel.selectDate(newDate)
                                    }
                                },
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Text(stringResource(id = R.string.confirm))
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = { showDatePicker = false },
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            ) {
                                Text(stringResource(id = R.string.cancel))
                            }
                        }
                    ) {
                        DatePicker(state = datePickerState)
                    }
                }

                    Spacer(modifier = Modifier.height(16.dp))
                    var selectedTab by remember { mutableStateOf(0) }
                    
                    // 移除不兼容的参数，使用更简洁的实现
                    TabRow(
                        selectedTabIndex = selectedTab,
                        indicator = { tabPositions ->
                            // 简化Tab指示器，移除不兼容的align调用
                            if (tabPositions.isNotEmpty()) {
                                TabRowDefaults.Indicator(
                                    modifier = Modifier
                                        .offset(x = tabPositions[selectedTab].left)
                                        .width(tabPositions[selectedTab].width),
                                    color = MaterialTheme.colorScheme.primary,
                                    height = 3.dp
                                )
                            }
                        },
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        divider = {
                            Divider(
                                thickness = 1.dp,
                                color = MaterialTheme.colorScheme.outlineVariant
                            )
                        }
                    ) {
                        // 在Tab作用域中获取触觉反馈对象
                        val hapticFeedback = LocalHapticFeedback.current
                        Tab(
                            selected = selectedTab == 0,
                            onClick = {
                                selectedTab = 0
                                // 添加触觉反馈
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                            },
                            text = { Text(stringResource(id = R.string.history_tab)) }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = {
                                selectedTab = 1
                                // 添加触觉反馈
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                            },
                            text = { Text(stringResource(id = R.string.diabetes_info_tab)) }
                        )
                    }

                    Crossfade(
                        targetState = selectedTab,
                        animationSpec = tween(300),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) { tab ->
                        when (tab) {
                            0 -> {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(elevation = 4.dp, shape = MaterialTheme.shapes.large)
                        .animateContentSize(animationSpec = tween(300)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    shape = MaterialTheme.shapes.large
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        if (isLoadingRecords) {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                            Text(stringResource(id = R.string.loading), modifier = Modifier.padding(top = 8.dp))
                        } else if (errorRecords != null) {
                            Text(stringResource(id = R.string.load_records_failed_format, errorRecords ?: ""), color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
                        } else if (mealRecords.isEmpty() && insulinRecords.isEmpty() && isAuthenticated) { // 仅在认证后显示空状态
                                            EmptyStateIllustration()
                        } else if (!isAuthenticated) {
                            // 未认证时显示提示信息
                            Text(
                                stringResource(id = R.string.device_authenticating),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        } else {
                                            LazyColumn(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                itemsIndexed(mealRecords) { index, record ->
                                                    AnimatedListItem(index) {
                                                        MealRecordItem(record)
                                                    }
                                                }
                                                itemsIndexed(insulinRecords) { index, record ->
                                                    AnimatedListItem(index + mealRecords.size) {
                                                        InsulinRecordItem(record)
                                                    }
                                                }
                                }
                            }
                        }
                    }
                }

                            else -> {
                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .shadow(elevation = 4.dp, shape = MaterialTheme.shapes.large)
                                        .animateContentSize(animationSpec = tween(300)),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                                    shape = MaterialTheme.shapes.large
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(id = R.string.diabetes_type),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = currentUser?.diabetes_type ?: stringResource(id = R.string.not_available), // 显示糖尿病类型
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = stringResource(id = R.string.common_symptoms),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(id = R.string.diabetes_symptoms_list),
                            style = MaterialTheme.typography.bodyMedium
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = stringResource(id = R.string.precautions),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            BulletPointText(stringResource(id = R.string.monitor_blood_sugar), MaterialTheme.colorScheme.onSurfaceVariant)
                            BulletPointText(stringResource(id = R.string.control_diet), MaterialTheme.colorScheme.onSurfaceVariant)
                            BulletPointText(stringResource(id = R.string.exercise_regularly), MaterialTheme.colorScheme.onSurfaceVariant)
                            BulletPointText(stringResource(id = R.string.take_medicine_on_time), MaterialTheme.colorScheme.onSurfaceVariant)
                            BulletPointText(stringResource(id = R.string.regular_checkup), MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                        }
                    }
                }

                    Spacer(modifier = Modifier.height(16.dp))
                // 在Composable作用域中获取触觉反馈对象
                val cameraButtonHapticFeedback = LocalHapticFeedback.current
                Button(
            onClick = {
                onNavigateToCamera()
                // 添加触觉反馈
                cameraButtonHapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .shadow(elevation = 2.dp, shape = MaterialTheme.shapes.medium),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = MaterialTheme.shapes.medium,
                    interactionSource = remember { MutableInteractionSource() },
    
                ) {
                    Text(stringResource(id = R.string.start_food_recognition), style = MaterialTheme.typography.titleMedium)
                }
                    Spacer(modifier = Modifier.height(80.dp)) // 留出底部导航栏空间
            }

            PullRefreshIndicator(
                refreshing = isRefreshing,
                state = pullRefreshState,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 12.dp)
            )
        }
    }
}

@Composable
fun SectionTitle(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .animateContentSize()
    )
}

@Composable
fun MealRecordItem(
    record: MealRecordResponse,
    modifier: Modifier = Modifier
) {
    // 使用动画效果增强交互体验
    // 移除scale状态，使用系统默认点击反馈
    
    // 在Composable作用域中获取触觉反馈对象
    val hapticFeedback = LocalHapticFeedback.current
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(
                onClick = { hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress) }
            ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 添加彩色背景图标
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Restaurant, contentDescription = stringResource(id = R.string.meal), modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = stringResource(id = R.string.meal_time_format, record.meal_time), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                record.food_items?.let { foods ->
                            if (foods.isNotEmpty()) {
                                Text(
                                    text = stringResource(id = R.string.foods_format, foods.joinToString { it.name }),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                                // 计算单条记录的总热量
                                val recordCalories = (record.food_items?.sumOf { it.weight.toDouble() } ?: 0.0).toFloat() * 1.2f
                                Text(text = stringResource(id = R.string.total_calories_format, recordCalories.roundToInt()), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                            }
                        }
                record.notes?.let { notes ->
                    Text(
                            text = stringResource(id = R.string.notes_format, notes),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                }
            }
        }
    }
}

@Composable
fun InsulinRecordItem(
    record: InsulinRecordResponse,
    modifier: Modifier = Modifier
) {
    // 使用动画效果增强交互体验
    // 移除scale状态，使用系统默认点击反馈
    
    // 在Composable作用域中获取触觉反馈对象
    val hapticFeedback = LocalHapticFeedback.current
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(
                onClick = { hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress) }
            ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 添加彩色背景图标
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Healing, contentDescription = stringResource(id = R.string.insulin), modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.secondary)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = stringResource(id = R.string.insulin_injection_time_format, record.injection_time), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                
                // 使用标签样式显示剂量
                Box(
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                        .align(Alignment.Start)
                ) {
                    Text(text = stringResource(id = R.string.insulin_dosage_format, record.actual_dose, stringResource(id = R.string.unit)),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                }
                
                record.notes?.let { notes ->
                    Text(
                            text = stringResource(id = R.string.notes_format, notes),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                }
            }
        }
    }
}

@Composable
fun BulletPointText(text: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Filled.Info,
            contentDescription = "Bullet Point",
            modifier = Modifier.size(8.dp),
            tint = color
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = text, style = MaterialTheme.typography.bodySmall, color = color)
    }
}

@Composable
private fun WelcomeSummary(
    mealCount: Int,
    insulinCount: Int,
    calories: Float,
    carbs: Float,
    calorieTarget: Float,
    carbTarget: Float,
    onNavigateToCamera: () -> Unit,
    onNavigateToFoodSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                    )
                )
            )
            .padding(20.dp)
    ) {
        Text(
            stringResource(id = R.string.home_welcome_back),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            stringResource(id = R.string.home_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
        )

        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            NutritionRing(
                title = stringResource(id = R.string.calories_title),
                value = calories,
                target = calorieTarget,
                unit = stringResource(id = R.string.calories_unit),
                modifier = Modifier.weight(1f)
            )
            NutritionRing(
                title = stringResource(id = R.string.carbs_title),
                value = carbs,
                target = carbTarget,
                unit = stringResource(id = R.string.carbs_unit),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatChip(label = stringResource(id = R.string.today_meals), value = mealCount.toString())
                            StatChip(label = stringResource(id = R.string.insulin_records), value = insulinCount.toString())
        }

        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AssistChip(
                onClick = onNavigateToFoodSearch,
                label = { Text(stringResource(id = R.string.search_food)) },
                leadingIcon = { Icon(imageVector = Icons.Filled.Search, contentDescription = null) }
            )
            AssistChip(
                onClick = onNavigateToCamera,
                label = { Text(stringResource(id = R.string.photo_recognition)) },
                leadingIcon = { Icon(imageVector = Icons.Filled.PhotoCamera, contentDescription = null) }
            )
        }
    }
}

@Composable
private fun StatChip(label: String, value: String) {
    AssistChip(
        onClick = { },
        label = { Text("$label：$value") },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = MaterialTheme.colorScheme.surface,
            labelColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

@Composable
private fun NutritionRing(
    title: String,
    value: Float,
    target: Float,
    unit: String,
    modifier: Modifier = Modifier
) {
    val progress = if (target > 0f) (value / target).coerceIn(0f, 1f) else 0f
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.65f))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(80.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                progress = progress,
                strokeWidth = 6.dp,
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
            )
            Text(
                text = "${(progress * 100).roundToInt()}%",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(title, style = MaterialTheme.typography.titleSmall)
        Text(
            text = "${value.roundToInt()} $unit / ${target.roundToInt()}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun EmptyStateIllustration() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Filled.Info,
            contentDescription = "空状态",
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
                text = stringResource(id = R.string.no_records),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(id = R.string.no_records_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
    }
}

@Composable
private fun AnimatedListItem(index: Int, content: @Composable () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay((index * 60L).coerceAtMost(600L))
        visible = true
    }
    AnimatedVisibilityAnim(
        visible = visible,
        enter = fadeIn(animationSpec = tween(250)) + slideInVertically(animationSpec = tween(250)) { it / 4 },
        exit = fadeOut(animationSpec = tween(150))
    ) {
        content()
    }
}

private data class NutritionSummary(val calories: Float, val carbs: Float)

private fun calculateNutritionEstimates(records: List<MealRecordResponse>): NutritionSummary {
    var totalWeight = 0f
    records.forEach { record ->
        val foods = record.food_items
        if (foods.isNullOrEmpty()) {
            // 无食物详情时使用平均值估算
            totalWeight += 250f
        } else {
            totalWeight += foods.sumOf { it.weight.toDouble() }.toFloat()
        }
    }
    if (totalWeight == 0f) {
        return NutritionSummary(
            calories = (records.size * 380f),
            carbs = (records.size * 45f)
        )
    }
    val estimatedCalories = totalWeight * 1.2f // 粗略估算：1g ≈ 1.2 kcal
    val estimatedCarbs = totalWeight * 0.15f // 粗略估算：15% 重量为净碳水
    return NutritionSummary(
        calories = estimatedCalories,
        carbs = estimatedCarbs
    )
}

@Composable
private fun NutritionOverviewCard(
    dailyRecommendation: DailyNutritionRecommendation?,
    todayIntake: TodayNutritionIntake?,
    isLoading: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 4.dp, shape = MaterialTheme.shapes.large),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 标题
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(id = R.string.nutrition_summary),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.width(20.dp).height(20.dp),
                        strokeWidth = 2.dp
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (dailyRecommendation != null && todayIntake != null) {
                // 计算剩余能量
                val remainingCalories = dailyRecommendation.daily_calories - todayIntake.total_calories
                
                // 能量摄入饼状图
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // 卡路里饼状图
                    NutritionPieChart(
                        title = stringResource(id = R.string.calories),
                        current = todayIntake.total_calories,
                        target = dailyRecommendation.daily_calories,
                        unit = "kcal",
                        modifier = Modifier.weight(1f)
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    // 碳水饼状图
                    NutritionPieChart(
                        title = stringResource(id = R.string.carbs),
                        current = todayIntake.total_carbs,
                        target = dailyRecommendation.daily_carbs,
                        unit = "g",
                        modifier = Modifier.weight(1f)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 剩余能量显示
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = when {
                            remainingCalories < 0 -> MaterialTheme.colorScheme.errorContainer
                            remainingCalories < 200 -> MaterialTheme.colorScheme.tertiaryContainer
                            else -> MaterialTheme.colorScheme.primaryContainer
                        }
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = stringResource(id = R.string.remaining_calories),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (remainingCalories >= 0) {
                                    stringResource(id = R.string.calories_remaining, remainingCalories.toInt())
                                } else {
                                    stringResource(id = R.string.calories_over, (-remainingCalories).toInt())
                                },
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Text(
                            text = "${((todayIntake.total_calories / dailyRecommendation.daily_calories) * 100).toInt()}%",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // 详细营养信息
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    NutritionDetailRow(
                        label = stringResource(id = R.string.protein),
                        current = todayIntake.total_protein,
                        target = dailyRecommendation.daily_protein,
                        unit = "g"
                    )
                    NutritionDetailRow(
                        label = stringResource(id = R.string.fat),
                        current = todayIntake.total_fat,
                        target = dailyRecommendation.daily_fat,
                        unit = "g"
                    )
                    NutritionDetailRow(
                        label = stringResource(id = R.string.fiber),
                        current = todayIntake.total_fiber,
                        target = dailyRecommendation.daily_fiber,
                        unit = "g"
                    )
                }
            } else if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                Text(
                    text = "暂无营养数据",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            }
        }
    }
}

@Composable
private fun NutritionPieChart(
    title: String,
    current: Float,
    target: Float,
    unit: String,
    modifier: Modifier = Modifier
) {
    val progress = if (target > 0f) (current / target).coerceIn(0f, 1f) else 0f
    val sweepAngle = progress * 360f
    
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(80.dp),
            contentAlignment = Alignment.Center
        ) {
            val colorScheme = MaterialTheme.colorScheme
            val backgroundColor = colorScheme.onSurface.copy(alpha = 0.1f)
            val progressColor = when {
                progress > 1f -> colorScheme.error
                progress > 0.8f -> colorScheme.tertiary
                else -> colorScheme.primary
            }
            
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2, size.height / 2)
                val radius = size.minDimension / 2 - 8.dp.toPx()
                
                // 绘制背景圆
                drawCircle(
                    color = backgroundColor,
                    radius = radius,
                    center = center
                )
                
                // 绘制进度弧
                if (progress > 0) {
                    drawArc(
                        color = progressColor,
                        startAngle = -90f,
                        sweepAngle = sweepAngle.coerceAtMost(360f),
                        useCenter = false,
                        style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round),
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
                    )
                }
            }
            
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "${(progress * 100).roundToInt()}%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "${current.roundToInt()}/${target.roundToInt()} $unit",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun NutritionDetailRow(
    label: String,
    current: Float,
    target: Float,
    unit: String
) {
    val progress = if (target > 0f) (current / target).coerceIn(0f, 1f) else 0f
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier.width(100.dp).height(6.dp),
                color = when {
                    progress > 1f -> MaterialTheme.colorScheme.error
                    progress > 0.8f -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.primary
                },
                trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
            )
            Text(
                text = "${current.roundToInt()}/${target.roundToInt()} $unit",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

