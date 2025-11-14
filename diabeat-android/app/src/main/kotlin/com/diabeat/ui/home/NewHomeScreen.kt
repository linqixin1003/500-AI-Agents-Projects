package com.diabeat.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import com.diabeat.R
import com.diabeat.data.model.DailyNutritionRecommendation
import com.diabeat.data.model.TodayNutritionIntake
import com.diabeat.data.model.MealRecordResponse
import com.diabeat.viewmodel.HomeViewModel
import com.diabeat.ui.dialog.ExerciseRecordDialog
import com.diabeat.ui.dialog.WaterRecordDialog
import com.diabeat.ui.dialog.MedicationRecordDialog
import com.diabeat.data.model.ExerciseRecordRequest
import com.diabeat.data.model.WaterRecordRequest
import com.diabeat.data.model.MedicationRecordRequest
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

/**
 * 新版首页 - 糖尿病日记风格
 * 参考减肥应用布局，但针对糖尿病患者优化
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun NewHomeScreen(
    homeViewModel: HomeViewModel,
    onNavigateToCamera: (mealType: String?) -> Unit, // 修改为接受餐次类型参数
    onNavigateToFoodSearch: () -> Unit
) {
    val selectedDate by homeViewModel.selectedDate.collectAsState()
    val mealRecords by homeViewModel.mealRecords.collectAsState()
    val insulinRecords by homeViewModel.insulinRecords.collectAsState()
    val isLoadingRecords by homeViewModel.isLoadingRecords.collectAsState()
    val isLoadingNutrition by homeViewModel.isLoadingNutrition.collectAsState()
    val dailyRecommendation by homeViewModel.dailyRecommendation.collectAsState()
    val todayIntake by homeViewModel.todayIntake.collectAsState()
    val currentUser by homeViewModel.user.collectAsState()

    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    
    // 对话框状态
    var showExerciseDialog by remember { mutableStateOf(false) }
    var showWaterDialog by remember { mutableStateOf(false) }
    var showMedicationDialog by remember { mutableStateOf(false) }
    
    LaunchedEffect(homeViewModel) {
        homeViewModel.fetchRecordsForDate(selectedDate)
        homeViewModel.fetchNutritionData()
        // 拉取当天的运动、水分、用药数据
        homeViewModel.fetchExerciseSummary()
        homeViewModel.fetchWaterSummary()
        homeViewModel.fetchMedicationSummary()
    }

    val isRefreshing = isLoadingRecords || isLoadingNutrition
    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = {
            homeViewModel.refreshNutritionData()
        }
    )
    
    // 对话框组件
    if (showExerciseDialog) {
        ExerciseRecordDialog(
            onDismiss = { showExerciseDialog = false },
            onConfirm = { request ->
                coroutineScope.launch {
                    try {
                        val response = homeViewModel.apiService.createExerciseRecord(request)
                        if (response.isSuccessful) {
                            showExerciseDialog = false
                            homeViewModel.refreshNutritionData()
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("NewHomeScreen", "保存运动记录失败: ${e.message}", e)
                    }
                }
            }
        )
    }
    
    if (showWaterDialog) {
        WaterRecordDialog(
            onDismiss = { showWaterDialog = false },
            onConfirm = { request ->
                coroutineScope.launch {
                    try {
                        val response = homeViewModel.apiService.createWaterRecord(request)
                        if (response.isSuccessful) {
                            showWaterDialog = false
                            homeViewModel.refreshNutritionData()
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("NewHomeScreen", "保存水分记录失败: ${e.message}", e)
                    }
                }
            }
        )
    }
    
    if (showMedicationDialog) {
        MedicationRecordDialog(
            onDismiss = { showMedicationDialog = false },
            onConfirm = { request ->
                coroutineScope.launch {
                    try {
                        val response = homeViewModel.apiService.createMedicationRecord(request)
                        if (response.isSuccessful) {
                            showMedicationDialog = false
                            homeViewModel.refreshNutritionData()
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("NewHomeScreen", "保存用药记录失败: ${e.message}", e)
                    }
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .pullRefresh(pullRefreshState)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            // 🎨 美化的顶部Banner
            BeautifulHeaderBanner(
                userName = currentUser?.name ?: "用户",
                currentTime = getGreeting()
            )
            
            Spacer(modifier = Modifier.height(20.dp))

            // 🎨 美化的快捷操作按钮组 (2x2网格)
            BeautifulQuickActions(
                onExerciseClick = { showExerciseDialog = true },
                onWaterClick = { showWaterDialog = true },
                onMedicationClick = { showMedicationDialog = true },
                onMealClick = { onNavigateToCamera(null) }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 🎨 美化的主要营养卡片
            BeautifiedMainNutritionCard(
                dailyRecommendation = dailyRecommendation,
                todayIntake = todayIntake
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 🎨 美化的血糖预测卡片
            BeautifiedBloodGlucosePredictionCard(
                dailyRecommendation = dailyRecommendation,
                todayIntake = todayIntake,
                homeViewModel = homeViewModel
            )

            Spacer(modifier = Modifier.height(16.dp))
            
            // 今日运动、水分、用药记录列表
            val exerciseSummary by homeViewModel.exerciseSummary.collectAsState()
            val waterSummary by homeViewModel.waterSummary.collectAsState()
            val medicationSummary by homeViewModel.medicationSummary.collectAsState()
            
            // 运动记录列表
            val exerciseSummaryValue = exerciseSummary
            if (exerciseSummaryValue != null && exerciseSummaryValue.exercises.isNotEmpty()) {
                TodayRecordsListCard(
                    title = "运动记录",
                    icon = Icons.Default.DirectionsRun,
                    iconColor = MaterialTheme.colorScheme.tertiary,
                    summaryText = "共 ${exerciseSummaryValue.exercise_count} 次 · ${exerciseSummaryValue.total_calories.roundToInt()} 大卡 · ${exerciseSummaryValue.total_duration} 分钟",
                    records = exerciseSummaryValue.exercises.map { exercise ->
                        val time = try {
                            java.time.Instant.parse(exercise.exercise_time)
                                .atZone(java.time.ZoneId.systemDefault())
                                .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
                        } catch (e: Exception) {
                            exercise.exercise_time
                        }
                        val exerciseTypeName = when (exercise.exercise_type) {
                            "walking" -> "步行"
                            "running" -> "跑步"
                            "cycling" -> "骑行"
                            "swimming" -> "游泳"
                            "gym" -> "健身房"
                            "yoga" -> "瑜伽"
                            "dancing" -> "跳舞"
                            else -> exercise.exercise_type
                        }
                        RecordItem(
                            time = time,
                            title = exerciseTypeName,
                            subtitle = "${exercise.duration_minutes} 分钟 · ${exercise.calories_burned.roundToInt()} 大卡"
                        )
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            // 水分记录列表
            val waterSummaryValue = waterSummary
            if (waterSummaryValue != null && waterSummaryValue.records.isNotEmpty()) {
                TodayRecordsListCard(
                    title = "饮水记录",
                    icon = Icons.Default.WaterDrop,
                    iconColor = MaterialTheme.colorScheme.primary,
                    summaryText = "共 ${waterSummaryValue.record_count} 次 · ${waterSummaryValue.total_ml} ml (${waterSummaryValue.progress_percentage.roundToInt()}%)",
                    records = waterSummaryValue.records.map { water ->
                        val time = try {
                            java.time.Instant.parse(water.record_time)
                                .atZone(java.time.ZoneId.systemDefault())
                                .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
                        } catch (e: Exception) {
                            water.record_time
                        }
                        val waterTypeName = when (water.water_type) {
                            "water" -> "白开水"
                            "tea" -> "茶"
                            "coffee" -> "咖啡"
                            "juice" -> "果汁"
                            else -> water.water_type
                        }
                        RecordItem(
                            time = time,
                            title = waterTypeName,
                            subtitle = "${water.amount_ml} ml"
                        )
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            // 用药记录列表
            val medicationSummaryValue = medicationSummary
            if (medicationSummaryValue != null && medicationSummaryValue.medications.isNotEmpty()) {
                TodayRecordsListCard(
                    title = "用药记录",
                    icon = Icons.Default.Medication,
                    iconColor = MaterialTheme.colorScheme.error,
                    summaryText = "共 ${medicationSummaryValue.total_count} 次" + 
                                 if (medicationSummaryValue.insulin_count > 0) " (胰岛素 ${medicationSummaryValue.insulin_count})" else "",
                    records = medicationSummaryValue.medications.map { medication ->
                        val time = try {
                            java.time.Instant.parse(medication.medication_time)
                                .atZone(java.time.ZoneId.systemDefault())
                                .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
                        } catch (e: Exception) {
                            medication.medication_time
                        }
                        val medicationTypeName = when (medication.medication_type) {
                            "insulin" -> "胰岛素"
                            "oral_medication" -> "口服药物"
                            else -> medication.medication_type
                        }
                        RecordItem(
                            time = time,
                            title = "$medicationTypeName · ${medication.medication_name}",
                            subtitle = "${medication.dosage} ${medication.dosage_unit}"
                        )
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 餐次记录区
            MealSectionsCard(
                mealRecords = mealRecords,
                insulinRecords = insulinRecords,
                onAddMeal = onNavigateToCamera
            )

            Spacer(modifier = Modifier.height(80.dp)) // 底部导航栏预留空间
        }

        PullRefreshIndicator(
            refreshing = isRefreshing,
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

@Composable
private fun DiaryHeader(
    selectedDate: LocalDate,
    onDateChange: (LocalDate) -> Unit
) {
    val isToday = selectedDate.isEqual(LocalDate.now())
    val isFutureDate = selectedDate.isAfter(LocalDate.now())
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "My Diary",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            
            IconButton(onClick = { /* TODO: 通知或设置 */ }) {
                Icon(
                    imageVector = Icons.Filled.Notifications,
                    contentDescription = "通知",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 日期选择器 - 带左右切换
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 前一天按钮
            IconButton(
                onClick = { onDateChange(selectedDate.minusDays(1)) }
            ) {
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowLeft,
                    contentDescription = "前一天",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            // 日期显示
            Text(
                text = when {
                    isToday -> "今天, ${selectedDate.format(DateTimeFormatter.ofPattern("MM月dd日"))}"
                    selectedDate.isEqual(LocalDate.now().minusDays(1)) -> "昨天, ${selectedDate.format(DateTimeFormatter.ofPattern("MM月dd日"))}"
                    else -> selectedDate.format(DateTimeFormatter.ofPattern("EEE, dd MMM"))
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            // 后一天按钮（今天及之后禁用）
            IconButton(
                onClick = { 
                    if (!isToday && !isFutureDate) {
                        onDateChange(selectedDate.plusDays(1))
                    }
                },
                enabled = !isToday && !isFutureDate
            ) {
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowRight,
                    contentDescription = if (isToday) "已是今天" else "后一天",
                    tint = if (isToday || isFutureDate) {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                )
            }
        }
        
        // 快速跳转到今天
        if (!isToday) {
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(
                onClick = { onDateChange(LocalDate.now()) },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text(
                    text = "回到今天",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun MainNutritionCard(
    dailyRecommendation: DailyNutritionRecommendation?,
    todayIntake: TodayNutritionIntake?
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // 标题和说明
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "碳水化合物",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                IconButton(onClick = { /* TODO: 信息提示 */ }) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = "信息",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            Text(
                text = "剩余 = 每日目标 + 运动消耗 - 已摄入",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 主要展示 - 碳水剩余量
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // 左侧 - 大圆形进度
                val carbsRemaining = if (dailyRecommendation != null && todayIntake != null) {
                    dailyRecommendation.daily_carbs - todayIntake.total_carbs
                } else 0f
                
                val carbsProgress = if (dailyRecommendation != null && todayIntake != null) {
                    (todayIntake.total_carbs / dailyRecommendation.daily_carbs).coerceIn(0f, 1f)
                } else 0f

                Box(
                    modifier = Modifier.size(140.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressWithValue(
                        progress = carbsProgress,
                        value = carbsRemaining.roundToInt(),
                        unit = "g",
                        label = "剩余"
                    )
                }

                Spacer(modifier = Modifier.width(20.dp))

                // 右侧 - 详细数据
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    NutritionDetailItem(
                        icon = Icons.Filled.Flag,
                        label = "每日目标",
                        value = dailyRecommendation?.daily_carbs?.roundToInt() ?: 0,
                        unit = "g",
                        color = MaterialTheme.colorScheme.primary
                    )

                    NutritionDetailItem(
                        icon = Icons.Filled.Restaurant,
                        label = "已摄入",
                        value = todayIntake?.total_carbs?.roundToInt() ?: 0,
                        unit = "g",
                        color = MaterialTheme.colorScheme.tertiary
                    )

                    NutritionDetailItem(
                        icon = Icons.Filled.LocalFireDepartment,
                        label = "运动消耗",
                        value = 0,
                        unit = "g",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 卡路里小摘要
            CaloriesSummary(
                dailyRecommendation = dailyRecommendation,
                todayIntake = todayIntake
            )
        }
    }
}

@Composable
private fun CircularProgressWithValue(
    progress: Float,
    value: Int,
    unit: String,
    label: String
) {
    val colorScheme = MaterialTheme.colorScheme
    val progressColor = when {
        progress > 1f -> colorScheme.error
        progress > 0.8f -> colorScheme.tertiary
        else -> colorScheme.primary
    }
    val backgroundColor = colorScheme.onSurface.copy(alpha = 0.1f)

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = size.minDimension / 2 - 12.dp.toPx()

            // 背景圆
            drawCircle(
                color = backgroundColor,
                radius = radius,
                center = center
            )

            // 进度弧
            if (progress > 0) {
                val sweepAngle = (progress.coerceAtMost(1f) * 360f)
                drawArc(
                    color = progressColor,
                    startAngle = -90f,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round),
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            // 不显示 "g" 单位
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun NutritionDetailItem(
    icon: ImageVector,
    label: String,
    value: Int,
    unit: String,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.height(IntrinsicSize.Min) // 确保图标完整显示
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(24.dp) // 增大图标尺寸确保显示完整
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "$value $unit",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun CaloriesSummary(
    dailyRecommendation: DailyNutritionRecommendation?,
    todayIntake: TodayNutritionIntake?
) {
    val caloriesRemaining = if (dailyRecommendation != null && todayIntake != null) {
        dailyRecommendation.daily_calories - todayIntake.total_calories
    } else 0f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.LocalFireDepartment,
                contentDescription = "卡路里",
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "热量剩余",
                style = MaterialTheme.typography.bodyMedium
            )
        }
        
        Text(
            text = "${caloriesRemaining.roundToInt()} kcal",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.secondary
        )
    }
}

@Composable
private fun SecondaryNutritionRow(
    dailyRecommendation: DailyNutritionRecommendation?,
    todayIntake: TodayNutritionIntake?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 蛋白质
        SecondaryNutritionCard(
            modifier = Modifier.weight(1f),
            label = "蛋白质",
            current = todayIntake?.total_protein?.roundToInt() ?: 0,
            target = dailyRecommendation?.daily_protein?.roundToInt() ?: 0,
            unit = "g",
            color = MaterialTheme.colorScheme.tertiary
        )

        // 脂肪
        SecondaryNutritionCard(
            modifier = Modifier.weight(1f),
            label = "脂肪",
            current = todayIntake?.total_fat?.roundToInt() ?: 0,
            target = dailyRecommendation?.daily_fat?.roundToInt() ?: 0,
            unit = "g",
            color = MaterialTheme.colorScheme.secondary // 改为secondary色
        )
    }
}

@Composable
private fun SecondaryNutritionCard(
    modifier: Modifier = Modifier,
    label: String,
    current: Int,
    target: Int,
    unit: String,
    color: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 进度条
            val progress = if (target > 0) (current.toFloat() / target).coerceIn(0f, 1f) else 0f
            val percentage = if (target > 0) (current.toFloat() / target) * 100 else 0f
            
            // 超过100%才显示红色
            val displayColor = if (percentage > 100f) {
                MaterialTheme.colorScheme.error
            } else {
                color
            }
            
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = displayColor,
                trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "$current/$target $unit",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                val percentage = if (target > 0) ((current.toFloat() / target) * 100).roundToInt() else 0
                Text(
                    text = "$percentage%",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }
        }
    }
}

@Composable
private fun BloodGlucosePredictionCard(
    dailyRecommendation: DailyNutritionRecommendation?,
    todayIntake: TodayNutritionIntake?,
    homeViewModel: HomeViewModel  // 新增参数以获取运动和水分数据
) {
    // 从ViewModel获取实时数据
    val exerciseSummary by homeViewModel.exerciseSummary.collectAsState()
    val waterSummary by homeViewModel.waterSummary.collectAsState()
    val mealRecords by homeViewModel.mealRecords.collectAsState()
    
    // 改进的血糖预测算法 - 考虑碳水、运动和水分摄入
    // 这是一个简化的估算，实际血糖受多种因素影响
    val predictedBloodGlucose = remember(todayIntake, exerciseSummary, waterSummary, mealRecords) {
        if (todayIntake != null && dailyRecommendation != null) {
            // === 1. 基础血糖值 ===
            val baseGlucose = 5.6f // 空腹正常血糖：5.6 mmol/L
            
            // === 2. 碳水摄入影响 ===
            // 每15g碳水约增加1 mmol/L血糖
            val carbsIntake = todayIntake.total_carbs
            val glucoseFromCarbs = carbsIntake / 15f
            
            // === 3. 运动量影响（降低血糖）=== ✅ 已完成TODO
            // 从运动记录API获取实时数据
            val exerciseCalories = exerciseSummary?.total_calories ?: 0f
            val glucoseReductionFromExercise = when {
                exerciseCalories < 150f -> exerciseCalories / 300f // 0-0.5
                exerciseCalories < 300f -> 0.5f + (exerciseCalories - 150f) / 300f // 0.5-1.0
                else -> 1.0f + (exerciseCalories - 300f).coerceAtMost(200f) / 200f // 1.0-2.0
            }
            
            // === 4. 水分摄入影响（帮助稳定血糖）=== ✅ 已完成TODO
            // 从水分记录API获取实时数据
            val waterIntake = waterSummary?.total_ml?.toFloat() ?: 2000f
            val waterFactor = when {
                waterIntake >= 2000f -> 1.0f // 充足，最佳状态
                waterIntake >= 1000f -> 0.95f // 轻微不足
                else -> 0.9f // 严重不足，血糖浓缩
            }
            
            // === 5. 时间衰减因子 === ✅ 已完成TODO
            // 根据最后一餐时间动态计算
            val timeDecayFactor = calculateTimeDecay(mealRecords.firstOrNull()?.meal_time)
            
            // === 6. 综合计算 ===
            // 预测血糖 = 基础值 + (碳水影响 × 时间衰减 × 水分影响) - 运动降低
            val predicted = baseGlucose + 
                (glucoseFromCarbs * timeDecayFactor * waterFactor) - 
                glucoseReductionFromExercise
            
            // 限制范围在合理区间 (3.9-11.1 mmol/L)
            predicted.coerceIn(3.9f, 11.1f)
        } else {
            5.6f // 默认空腹血糖
        }
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = "预测血糖",
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "预测当前血糖",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "基于今日数据估算",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // 预测值（基于今日摄入）
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = String.format("%.1f", predictedBloodGlucose),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            predictedBloodGlucose < 3.9f -> MaterialTheme.colorScheme.error // 低血糖
                            predictedBloodGlucose > 7.8f -> MaterialTheme.colorScheme.error // 高血糖
                            else -> MaterialTheme.colorScheme.tertiary // 正常
                        }
                    )
                    Text(
                        text = "mmol/L",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 免责声明
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = "提示",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "⚠️ 预测基于碳水摄入、运动消耗和水分摄入综合计算。仅供参考，请以实际血糖监测为准。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

/**
 * 根据最后一餐时间计算时间衰减因子
 * 用于血糖预测算法
 */
private fun calculateTimeDecay(lastMealTime: String?): Float {
    if (lastMealTime == null) return 0.8f
    
    try {
        // 解析ISO时间格式
        val lastMeal = java.time.Instant.parse(lastMealTime)
        val now = java.time.Instant.now()
        val hoursSinceMeal = java.time.Duration.between(lastMeal, now).toMinutes() / 60f
        
        // 根据餐后时间计算衰减因子
        return when {
            hoursSinceMeal < 1f -> 1.0f      // 餐后1小时内，血糖峰值
            hoursSinceMeal < 2f -> 0.9f      // 1-2小时，开始下降
            hoursSinceMeal < 3f -> 0.8f      // 2-3小时，持续下降
            hoursSinceMeal < 4f -> 0.6f      // 3-4小时，显著降低
            else -> 0.5f                      // 4小时以上，接近基础值
        }
    } catch (e: Exception) {
        android.util.Log.e("NewHomeScreen", "解析最后一餐时间失败: ${e.message}", e)
        return 0.8f // 默认值
    }
}

// 根据时间段筛选餐次
private fun filterMealsByTime(
    mealRecords: List<MealRecordResponse>,
    mealType: MealType
): List<MealRecordResponse> {
    return mealRecords.filter { record ->
        try {
            // 解析meal_time字符串为LocalDateTime
            val mealTime = LocalDateTime.parse(record.meal_time, DateTimeFormatter.ISO_DATE_TIME)
            val hour = mealTime.hour
            
            when (mealType) {
                MealType.BREAKFAST -> hour in 5..10    // 5:00 - 10:59
                MealType.LUNCH -> hour in 11..14       // 11:00 - 14:59
                MealType.DINNER -> hour in 17..21      // 17:00 - 21:59
                MealType.SNACK -> hour in 0..4 || hour in 15..16 || hour in 22..23  // 其他时间
            }
        } catch (e: Exception) {
            false
        }
    }
}

private enum class MealType {
    BREAKFAST, LUNCH, DINNER, SNACK
}

@Composable
private fun MealSectionsCard(
    mealRecords: List<MealRecordResponse>,
    insulinRecords: List<*>,
    onAddMeal: (mealType: String?) -> Unit // 修改为接受餐次类型参数
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // 早餐 (5:00 - 10:59)
            MealSection(
                icon = "🍞",
                mealName = "Breakfast",
                mealRecords = filterMealsByTime(mealRecords, MealType.BREAKFAST),
                onAdd = { onAddMeal("breakfast") }
            )

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // 午餐 (11:00 - 14:59)
            MealSection(
                icon = "🍗",
                mealName = "Lunch",
                mealRecords = filterMealsByTime(mealRecords, MealType.LUNCH),
                onAdd = { onAddMeal("lunch") }
            )

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // 晚餐 (17:00 - 21:59)
            MealSection(
                icon = "🥗",
                mealName = "Dinner",
                mealRecords = filterMealsByTime(mealRecords, MealType.DINNER),
                onAdd = { onAddMeal("dinner") }
            )

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // 加餐 (其他时间)
            MealSection(
                icon = "🍿",
                mealName = "Snack",
                mealRecords = filterMealsByTime(mealRecords, MealType.SNACK),
                onAdd = { onAddMeal("snack") }
            )

            Divider(
                modifier = Modifier.padding(vertical = 12.dp),
                thickness = 2.dp,
                color = MaterialTheme.colorScheme.outlineVariant
            )

            // 水分追踪
            WaterTrackerSection()
            
            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // 运动记录
            ActivitySection()
        }
    }
}

@Composable
private fun WaterTrackerSection() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { /* TODO: 打开水分追踪 */ },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "💧",
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Water Tracker",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "今日饮水 0/2000 ml",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        IconButton(onClick = { /* TODO: 添加饮水记录 */ }) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = "添加",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
private fun ActivitySection() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { /* TODO: 打开运动记录 */ },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "🔥",
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Activities",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "今日运动 0 分钟",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        IconButton(onClick = { /* TODO: 添加运动记录 */ }) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = "添加",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
private fun MealSection(
    icon: String,
    mealName: String,
    mealRecords: List<MealRecordResponse>,
    onAdd: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = icon,
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = mealName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                if (mealRecords.isNotEmpty()) {
                    // FoodItemInput 没有 calories 字段，简化显示
                    val foodCount = mealRecords.sumOf { it.food_items?.size ?: 0 }
                    Text(
                        text = "$foodCount 项食物",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (mealRecords.isEmpty()) {
            IconButton(onClick = onAdd) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "添加",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }
        } else {
            // 显示食物摘要
            Text(
                text = mealRecords.firstOrNull()?.food_items?.firstOrNull()?.name ?: "",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}


/**
 * 快捷记录按钮组件
 * 提供运动、水分、用药的快速记录入口
 */
@Composable
private fun QuickRecordButtons(
    onExerciseClick: () -> Unit,
    onWaterClick: () -> Unit,
    onMedicationClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "快捷记录",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 运动记录按钮
                QuickRecordButton(
                    icon = Icons.Default.DirectionsRun,
                    label = "运动",
                    color = MaterialTheme.colorScheme.tertiary,
                    onClick = onExerciseClick,
                    modifier = Modifier.weight(1f)
                )
                
                // 水分记录按钮
                QuickRecordButton(
                    icon = Icons.Default.WaterDrop,
                    label = "饮水",
                    color = MaterialTheme.colorScheme.primary,
                    onClick = onWaterClick,
                    modifier = Modifier.weight(1f)
                )
                
                // 用药记录按钮
                QuickRecordButton(
                    icon = Icons.Default.Medication,
                    label = "用药",
                    color = MaterialTheme.colorScheme.error,
                    onClick = onMedicationClick,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun QuickRecordButton(
    icon: ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(72.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = color.copy(alpha = 0.1f),
            contentColor = color
        ),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(28.dp),
                tint = color
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/**
 * 今日汇总卡片 - 显示运动、水分、用药的实时数据
 */
@Composable
private fun TodaySummaryCard(
    exerciseSummary: com.diabeat.data.model.TodayExerciseSummary?,
    waterSummary: com.diabeat.data.model.TodayWaterSummary?,
    medicationSummary: com.diabeat.data.model.TodayMedicationSummary?
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "今日健康数据",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 运动数据
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.DirectionsRun,
                        contentDescription = "运动",
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "运动消耗",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = if (exerciseSummary != null) {
                        "${exerciseSummary.total_calories.roundToInt()} 大卡 · ${exerciseSummary.total_duration} 分钟"
                    } else {
                        "暂无记录"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 水分数据
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.WaterDrop,
                        contentDescription = "饮水",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "饮水量",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                if (waterSummary != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${waterSummary.total_ml} ml",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "(${waterSummary.progress_percentage.roundToInt()}%)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Text(
                        text = "暂无记录",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 用药数据
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Medication,
                        contentDescription = "用药",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "用药记录",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                if (medicationSummary != null && medicationSummary.total_count > 0) {
                    Text(
                        text = "共 ${medicationSummary.total_count} 次" + 
                               if (medicationSummary.insulin_count > 0) " (胰岛素 ${medicationSummary.insulin_count})" else "",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    Text(
                        text = "暂无记录",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

/**
 * 记录项数据类
 */
private data class RecordItem(
    val time: String,
    val title: String,
    val subtitle: String
)

/**
 * 美化的今日记录列表卡片 - 玻璃态设计 + 左侧彩色条
 */
@Composable
private fun TodayRecordsListCard(
    title: String,
    icon: ImageVector,
    iconColor: Color,
    summaryText: String,
    records: List<RecordItem>
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(24.dp),
                spotColor = iconColor.copy(alpha = 0.2f)
            ),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            // 左侧彩色指示条
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                iconColor,
                                iconColor.copy(alpha = 0.6f)
                            )
                        )
                    )
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(20.dp)
            ) {
                // 标题行 - 美化版
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // 图标背景圆
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    color = iconColor.copy(alpha = 0.15f),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = title,
                                tint = iconColor,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    // 记录数量徽章
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = iconColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "${records.size}",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = iconColor
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // 汇总信息 - 美化版
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Text(
                        text = summaryText,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // 记录列表
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    records.forEachIndexed { index, record ->
                        RecordListItem(
                            time = record.time,
                            title = record.title,
                            subtitle = record.subtitle,
                            iconColor = iconColor,
                            showDivider = index < records.size - 1 // 最后一项不显示分隔线
                        )
                    }
                }
            }
        }
    }
}

/**
 * 记录列表项组件
 */
@Composable
private fun RecordListItem(
    time: String,
    title: String,
    subtitle: String,
    iconColor: Color,
    showDivider: Boolean = true
) {
    Column {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.Transparent
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 时间 - 美化版
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = iconColor.copy(alpha = 0.1f)
                ) {
                    Text(
                        text = time,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = iconColor,
                        textAlign = TextAlign.Center
                    )
                }
                
                // 内容
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // 箭头指示器
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = null,
                    tint = iconColor.copy(alpha = 0.4f),
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        
        // 分隔线（最后一项不显示）
        if (showDivider) {
            Divider(
                modifier = Modifier.padding(start = 90.dp, top = 4.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                thickness = 0.5.dp
            )
        }
    }
}

// ==================== 🎨 美化组件库 ====================

/**
 * 获取问候语
 */
private fun getGreeting(): String {
    val hour = java.time.LocalTime.now().hour
    return when {
        hour < 6 -> "凌晨好"
        hour < 9 -> "早上好"
        hour < 12 -> "上午好"
        hour < 14 -> "中午好"
        hour < 18 -> "下午好"
        hour < 22 -> "晚上好"
        else -> "夜深了"
    }
}

/**
 * 美化的顶部Banner - 重新设计版
 * 左侧：问候语和称呼
 * 右侧：今日统计卡片
 */
@Composable
private fun BeautifulHeaderBanner(
    userName: String,
    currentTime: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
    ) {
        // 渐变背景
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF667EEA),
                            Color(0xFF764BA2)
                        )
                    )
                )
        )
        
        // 装饰性圆形元素
        Box(
            modifier = Modifier
                .size(180.dp)
                .offset(x = (-60).dp, y = (-40).dp)
                .background(
                    color = Color.White.copy(alpha = 0.08f),
                    shape = CircleShape
                )
        )
        
        Box(
            modifier = Modifier
                .size(120.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 40.dp, y = 40.dp)
                .background(
                    color = Color.White.copy(alpha = 0.08f),
                    shape = CircleShape
                )
        )
        
        // 内容 - 左右布局
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左侧：问候区域
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = currentTime,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White.copy(alpha = 0.9f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                // 如果用户名为空或是"用户"，显示友好称呼
                val displayName = if (userName.isBlank() || userName == "用户") {
                    "亲爱的"
                } else {
                    userName
                }
                
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // 健康状态标签
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White.copy(alpha = 0.25f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = null,
                            tint = Color(0xFFFF6B6B),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "健康管理中",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // 右侧：今日概览卡片
            TodayOverviewCard()
        }
    }
}

/**
 * 今日概览卡片 - 美化版，支持日期切换和日历选择
 */
@Composable
private fun TodayOverviewCard() {
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf(java.time.LocalDate.now()) }
    
    Surface(
        onClick = { showDatePicker = true },
        modifier = Modifier
            .width(140.dp)
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(20.dp),
                spotColor = Color.Black.copy(alpha = 0.2f)
            ),
        shape = RoundedCornerShape(20.dp),
        color = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 日期切换区域
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 上一天按钮
                IconButton(
                    onClick = { selectedDate = selectedDate.minusDays(1) },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowLeft,
                        contentDescription = "上一天",
                        tint = Color(0xFF667EEA),
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                // 日期显示
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = selectedDate.format(
                            java.time.format.DateTimeFormatter.ofPattern("MM/dd")
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFF667EEA),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = getDayOfWeek(selectedDate),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // 下一天按钮
                IconButton(
                    onClick = { selectedDate = selectedDate.plusDays(1) },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowRight,
                        contentDescription = "下一天",
                        tint = Color(0xFF667EEA),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 点击提示图标
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF667EEA).copy(alpha = 0.15f),
                                Color(0xFF667EEA).copy(alpha = 0.05f)
                            )
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Today,
                    contentDescription = "选择日期",
                    tint = Color(0xFF667EEA),
                    modifier = Modifier.size(28.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 提示文字
            Text(
                text = "点击选择",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
    
    // 日期选择器对话框
    if (showDatePicker) {
        DatePickerDialog(
            selectedDate = selectedDate,
            onDateSelected = { date ->
                selectedDate = date
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }
}

/**
 * 获取星期几
 */
private fun getDayOfWeek(date: java.time.LocalDate): String {
    return when (date.dayOfWeek) {
        java.time.DayOfWeek.MONDAY -> "周一"
        java.time.DayOfWeek.TUESDAY -> "周二"
        java.time.DayOfWeek.WEDNESDAY -> "周三"
        java.time.DayOfWeek.THURSDAY -> "周四"
        java.time.DayOfWeek.FRIDAY -> "周五"
        java.time.DayOfWeek.SATURDAY -> "周六"
        java.time.DayOfWeek.SUNDAY -> "周日"
    }
}

/**
 * 日期选择器对话框
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerDialog(
    selectedDate: java.time.LocalDate,
    onDateSelected: (java.time.LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
    )
    
    androidx.compose.material3.DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                datePickerState.selectedDateMillis?.let { millis ->
                    val instant = java.time.Instant.ofEpochMilli(millis)
                    val date = java.time.LocalDateTime.ofInstant(instant, java.time.ZoneId.systemDefault()).toLocalDate()
                    onDateSelected(date)
                }
            }) {
                Text("确定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    ) {
        DatePicker(
            state = datePickerState,
            title = {
                Text(
                    text = "选择日期",
                    modifier = Modifier.padding(16.dp)
                )
            },
            colors = DatePickerDefaults.colors(
                selectedDayContainerColor = Color(0xFF667EEA),
                todayContentColor = Color(0xFF667EEA),
                todayDateBorderColor = Color(0xFF667EEA)
            )
        )
    }
}

/**
 * 美化的快捷操作按钮组 - 2x2网格布局
 */
@Composable
private fun BeautifulQuickActions(
    onExerciseClick: () -> Unit,
    onWaterClick: () -> Unit,
    onMedicationClick: () -> Unit,
    onMealClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickActionButton(
                icon = Icons.Default.DirectionsRun,
                label = "运动",
                color = Color(0xFF667EEA),
                onClick = onExerciseClick,
                modifier = Modifier.weight(1f)
            )
            QuickActionButton(
                icon = Icons.Default.WaterDrop,
                label = "饮水",
                color = Color(0xFF4FC3F7),
                onClick = onWaterClick,
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickActionButton(
                icon = Icons.Default.Medication,
                label = "用药",
                color = Color(0xFFEF5350),
                onClick = onMedicationClick,
                modifier = Modifier.weight(1f)
            )
            QuickActionButton(
                icon = Icons.Default.Restaurant,
                label = "饮食",
                color = Color(0xFF66BB6A),
                onClick = onMealClick,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun QuickActionButton(
    icon: ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )
    
    Surface(
        onClick = {
            isPressed = true
            onClick()
        },
        modifier = modifier
            .height(100.dp)
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(20.dp),
                spotColor = color.copy(alpha = 0.3f)
            ),
        shape = RoundedCornerShape(20.dp),
        color = color.copy(alpha = 0.12f)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .background(
                            color = color.copy(alpha = 0.2f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = color,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = color
                )
            }
        }
    }
    
    LaunchedEffect(isPressed) {
        if (isPressed) {
            kotlinx.coroutines.delay(100)
            isPressed = false
        }
    }
}

/**
 * 美化的主要营养卡片 - 玻璃态设计
 */
@Composable
private fun BeautifiedMainNutritionCard(
    dailyRecommendation: DailyNutritionRecommendation?,
    todayIntake: TodayNutritionIntake?
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(24.dp),
                spotColor = Color.Black.copy(alpha = 0.1f)
            ),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "今日营养",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(20.dp))
            
            // 碳水和卡路里
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 碳水化合物
                NutritionStatCard(
                    icon = Icons.Default.Restaurant,
                    label = "碳水",
                    current = todayIntake?.total_carbs?.roundToInt() ?: 0,
                    target = dailyRecommendation?.daily_carbs?.roundToInt() ?: 180,
                    unit = "g",
                    color = Color(0xFF667EEA),
                    modifier = Modifier.weight(1f)
                )
                
                // 卡路里
                NutritionStatCard(
                    icon = Icons.Default.LocalFireDepartment,
                    label = "卡路里",
                    current = todayIntake?.total_calories?.roundToInt() ?: 0,
                    target = dailyRecommendation?.daily_calories?.roundToInt() ?: 2000,
                    unit = "kcal",
                    color = Color(0xFFFF6B6B),
                    modifier = Modifier.weight(1f)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 蛋白质和脂肪
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 蛋白质
                SmallNutritionItem(
                    label = "蛋白质",
                    current = todayIntake?.total_protein?.roundToInt() ?: 0,
                    target = dailyRecommendation?.daily_protein?.roundToInt() ?: 80,
                    unit = "g",
                    color = Color(0xFF4CAF50),
                    modifier = Modifier.weight(1f)
                )
                
                // 脂肪
                SmallNutritionItem(
                    label = "脂肪",
                    current = todayIntake?.total_fat?.roundToInt() ?: 0,
                    target = dailyRecommendation?.daily_fat?.roundToInt() ?: 60,
                    unit = "g",
                    color = Color(0xFFFF9800),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun NutritionStatCard(
    icon: ImageVector,
    label: String,
    current: Int,
    target: Int,
    unit: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    val progress = if (target > 0) (current.toFloat() / target).coerceIn(0f, 1f) else 0f
    
    Surface(
        modifier = modifier.height(140.dp),
        shape = RoundedCornerShape(20.dp),
        color = color.copy(alpha = 0.1f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Column {
                Text(
                    text = "$current",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                Text(
                    text = "/ $target $unit",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = color,
                trackColor = color.copy(alpha = 0.2f)
            )
        }
    }
}

@Composable
private fun SmallNutritionItem(
    label: String,
    current: Int,
    target: Int,
    unit: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    val progress = if (target > 0) (current.toFloat() / target).coerceIn(0f, 1f) else 0f
    
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "$current / $target $unit",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = color
            )
        }
        
        Box(
            modifier = Modifier.size(48.dp),
            contentAlignment = Alignment.Center
        ) {
            // 背景圆环
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    color = color.copy(alpha = 0.2f),
                    radius = size.minDimension / 2,
                    style = Stroke(width = 4.dp.toPx())
                )
            }
            // 进度圆环
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawArc(
                    color = color,
                    startAngle = -90f,
                    sweepAngle = 360f * progress,
                    useCenter = false,
                    style = Stroke(
                        width = 4.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                )
            }
            Text(
                text = "${(progress * 100).roundToInt()}%",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

/**
 * 美化的血糖预测卡片 - 动态渐变色
 */
@Composable
private fun BeautifiedBloodGlucosePredictionCard(
    dailyRecommendation: DailyNutritionRecommendation?,
    todayIntake: TodayNutritionIntake?,
    homeViewModel: HomeViewModel
) {
    val exerciseSummary by homeViewModel.exerciseSummary.collectAsState()
    val waterSummary by homeViewModel.waterSummary.collectAsState()
    val mealRecords by homeViewModel.mealRecords.collectAsState()
    
    val predictedBloodGlucose = remember(todayIntake, exerciseSummary, waterSummary, mealRecords) {
        if (todayIntake != null && dailyRecommendation != null) {
            val baseGlucose = 5.6f
            val carbsIntake = todayIntake.total_carbs
            val glucoseFromCarbs = carbsIntake / 15f
            val exerciseCalories = exerciseSummary?.total_calories ?: 0f
            val glucoseReductionFromExercise = when {
                exerciseCalories < 150f -> exerciseCalories / 300f
                exerciseCalories < 300f -> 0.5f + (exerciseCalories - 150f) / 300f
                else -> 1.0f + (exerciseCalories - 300f).coerceAtMost(200f) / 200f
            }
            val waterIntake = waterSummary?.total_ml?.toFloat() ?: 2000f
            val waterFactor = when {
                waterIntake >= 2000f -> 1.0f
                waterIntake >= 1000f -> 0.95f
                else -> 0.9f
            }
            val timeDecayFactor = calculateTimeDecay(mealRecords.firstOrNull()?.meal_time)
            val predicted = baseGlucose + 
                (glucoseFromCarbs * timeDecayFactor * waterFactor) - 
                glucoseReductionFromExercise
            predicted.coerceIn(3.9f, 11.1f)
        } else {
            5.6f
        }
    }
    
    // 根据血糖值动态选择颜色
    val glucoseColors = when {
        predictedBloodGlucose < 3.9f -> listOf(Color(0xFFFF9800), Color(0xFFFF5722)) // 橙红
        predictedBloodGlucose > 7.8f -> listOf(Color(0xFFEF5350), Color(0xFFE91E63)) // 红粉
        else -> listOf(Color(0xFF66BB6A), Color(0xFF4CAF50)) // 绿色
    }
    
    val glucoseStatus = when {
        predictedBloodGlucose < 3.9f -> "偏低"
        predictedBloodGlucose > 7.8f -> "偏高"
        else -> "正常"
    }
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(24.dp),
                spotColor = glucoseColors[0].copy(alpha = 0.3f)
            ),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Box {
            // 渐变背景装饰
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = glucoseColors.map { it.copy(alpha = 0.1f) }
                        )
                    )
            )
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "预测血糖值",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "基于今日数据综合分析",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = glucoseColors[0].copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = glucoseStatus,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = glucoseColors[0]
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // 血糖值显示
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = String.format("%.1f", predictedBloodGlucose),
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.Bold,
                        color = glucoseColors[0]
                    )
                    Text(
                        text = "mmol/L",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 提示信息
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "预测基于碳水、运动、水分综合计算，仅供参考",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }
    }
}

