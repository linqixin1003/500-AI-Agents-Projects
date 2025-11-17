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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
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
import com.diabeat.data.model.BloodGlucosePredictionRequest
import com.diabeat.data.model.BloodGlucosePredictionResponse
import com.diabeat.data.model.BloodGlucoseCorrectionResponse
import com.diabeat.data.model.BloodGlucoseCorrectionRequest
import com.diabeat.viewmodel.HomeViewModel
import com.diabeat.ui.dialog.ExerciseRecordDialog
import com.diabeat.ui.dialog.WaterRecordDialog
import com.diabeat.ui.dialog.MedicationRecordDialog
import com.diabeat.ui.dialog.BloodGlucosePredictionDialog
import com.diabeat.ui.dialog.BloodGlucoseCorrectionDialog
import com.diabeat.data.model.ExerciseRecordRequest
import com.diabeat.data.model.WaterRecordRequest
import com.diabeat.data.model.MedicationRecordRequest
import androidx.compose.ui.window.Dialog
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
    onNavigateToFoodSearch: () -> Unit,
    onNavigateToBarcodeScanner: () -> Unit = {}  // 导航到条形码扫描
) {
    val selectedDate by homeViewModel.selectedDate.collectAsState()
    val mealRecords by homeViewModel.mealRecords.collectAsState()
    val insulinRecords by homeViewModel.insulinRecords.collectAsState()
    val isLoadingRecords by homeViewModel.isLoadingRecords.collectAsState()
    val isLoadingNutrition by homeViewModel.isLoadingNutrition.collectAsState()
    val dailyRecommendation by homeViewModel.dailyRecommendation.collectAsState()
    val todayIntake by homeViewModel.todayIntake.collectAsState()
    val currentUser by homeViewModel.user.collectAsState()
    val latestPrediction by homeViewModel.bloodGlucosePrediction.collectAsState()
    val bgCorrections by homeViewModel.bloodGlucoseCorrections.collectAsState()

    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    
    // 对话框状态
    var showExerciseDialog by remember { mutableStateOf(false) }
    var showWaterDialog by remember { mutableStateOf(false) }
    var showMedicationDialog by remember { mutableStateOf(false) }
    var showFoodScanOptions by remember { mutableStateOf(false) }  // 食品扫描选择
    var showBgCorrectionDialog by remember { mutableStateOf(false) }  // 血糖纠正对话框
    var isPredicting by remember { mutableStateOf(false) }
    var isSubmittingCorrection by remember { mutableStateOf(false) }
    var isManualRefreshing by remember { mutableStateOf(false) }
    
    // 智能刷新管理器
    val smartRefreshManager = remember {
        com.diabeat.service.SmartRefreshManager(
            context = context,
            apiService = homeViewModel.apiService,
            viewModel = homeViewModel
        )
    }
    
    // 应用进入前台时启动刷新（首次加载）
    LaunchedEffect(Unit) {
        smartRefreshManager.onAppForegrounded()
    }
    
    // 清理资源
    DisposableEffect(Unit) {
        onDispose {
            smartRefreshManager.cleanup()
        }
    }
    
    LaunchedEffect(homeViewModel) {
        homeViewModel.fetchRecordsForDate(selectedDate)
        homeViewModel.fetchNutritionData(selectedDate)
        // 拉取当天的运动、水分、用药数据
        homeViewModel.fetchExerciseSummary(selectedDate)
        homeViewModel.fetchWaterSummary(selectedDate)
        homeViewModel.fetchMedicationSummary(selectedDate)
        homeViewModel.fetchBloodGlucoseCorrections()
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
                            homeViewModel.fetchExerciseSummary()
                            android.widget.Toast.makeText(
                                context,
                                "运动记录保存成功",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            android.widget.Toast.makeText(
                                context,
                                "保存失败: ${response.message()}",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("NewHomeScreen", "保存运动记录失败: ${e.message}", e)
                        android.widget.Toast.makeText(
                            context,
                            "网络错误，请稍后重试",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        )
    }

    if (isPredicting) {
        Dialog(onDismissRequest = { }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                tonalElevation = 6.dp,
                modifier = Modifier.wrapContentSize()
            ) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = 24.dp, vertical = 20.dp)
                        .widthIn(min = 200.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "AI预测进行中...",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
    
    // 移除旧的对话框方式，改为直接使用真实数据预测
    // 点击AI预测时，自动收集用户的真实记录数据
    
    if (showBgCorrectionDialog) {
        // 如果没有预测记录，创建一个默认对象
        val prediction = latestPrediction ?: com.diabeat.data.model.BloodGlucosePredictionResponse(
            prediction_id = "",
            predictions = emptyList(),
            peak_time = 0,
            peak_value = 0f,
            risk_level = "unknown"
        )
        
        BloodGlucoseCorrectionDialog(
            prediction = prediction,
            onDismiss = { showBgCorrectionDialog = false },
            isSubmitting = isSubmittingCorrection,
            onConfirm = { request ->
                isSubmittingCorrection = true
                coroutineScope.launch {
                    try {
                        val response = homeViewModel.apiService.submitBloodGlucoseCorrection(request)
                        if (response.isSuccessful) {
                                // 直接使用返回的correction数据，立即更新UI
                                val correctionData = response.body()
                                if (correctionData != null) {
                                    homeViewModel.addBloodGlucoseCorrection(correctionData)
                                    android.util.Log.d("NewHomeScreen", "纠正数据已添加: ${correctionData.actual_value}")
                                }
                                
                                // 提交纠正后，使用实测血糖值重新预测
                                android.util.Log.d("NewHomeScreen", "纠正已保存，使用实测血糖 ${request.actual_value} 重新预测")
                                
                                // 使用当前的饮食和用药数据，但用实测血糖作为当前血糖值
                                val totalCarbs = todayIntake?.total_carbs ?: 0f
                                val currentMedicationSummary = homeViewModel.medicationSummary.value
                                val insulinDose = currentMedicationSummary?.medications
                                    ?.filter { it.medication_type == "insulin" }
                                    ?.sumOf { it.dosage.toDouble() }?.toFloat() ?: 0f
                                
                                val currentExerciseSummary = homeViewModel.exerciseSummary.value
                                val activityLevel = when {
                                    currentExerciseSummary == null || currentExerciseSummary.total_duration == 0 -> "sedentary"
                                    currentExerciseSummary.total_duration < 30 -> "light"
                                    currentExerciseSummary.total_duration < 60 -> "moderate"
                                    else -> "vigorous"
                                }
                                
                                // 获取时间信息（关键！）
                                val currentMealRecords = homeViewModel.mealRecords.value
                                val mealTime = currentMealRecords.firstOrNull()?.meal_time
                                val medicationTime = currentMedicationSummary?.medications
                                    ?.firstOrNull()?.let {
                                        try {
                                            it.created_at
                                        } catch (e: Exception) {
                                            null
                                        }
                                    }
                                val currentTime = java.time.LocalDateTime.now().toString()
                                
                                // ✅ 构建历史记录（最近3次）
                                val recentMeals = currentMealRecords.take(3).mapNotNull { meal ->
                                    meal.total_carbs?.let { carbs ->
                                        com.diabeat.data.model.MealHistoryItem(
                                            meal_time = meal.meal_time,
                                            total_carbs = carbs,
                                            meal_type = null,
                                            foods = meal.food_name?.joinToString(", ") ?: meal.food_items?.joinToString(", ") { it.name }
                                        )
                                    }
                                }.takeIf { it.isNotEmpty() }
                                
                                val recentMedications = currentMedicationSummary?.medications?.take(3)?.map { med ->
                                    com.diabeat.data.model.MedicationHistoryItem(
                                        medication_time = med.created_at,
                                        medication_type = med.medication_type,
                                        dosage = med.dosage
                                    )
                                }?.takeIf { it.isNotEmpty() }
                                
                                android.util.Log.d("NewHomeScreen", "重新预测参数: carbs=$totalCarbs, insulin=$insulinDose, bg=${request.actual_value}, recent_meals=${recentMeals?.size}, recent_meds=${recentMedications?.size}")
                                
                                // 使用实测血糖值重新预测（包含完整时间上下文 + 用户基础信息 + 历史记录）
                                val newRequest = com.diabeat.data.model.BloodGlucosePredictionRequest(
                                    total_carbs = if (totalCarbs > 0) totalCarbs else 50f,  // 默认50g碳水
                                    insulin_dose = insulinDose,
                                    current_bg = request.actual_value,  // ✅ 使用实测血糖值（纠正值）
                                    gi_value = null,
                                    activity_level = activityLevel,
                                    // ✅ 时间上下文（关键！）
                                    meal_time = mealTime,
                                    medication_time = medicationTime,
                                    current_time = currentTime,
                                    // ✅ 用户基础信息（个性化预测）
                                    weight = currentUser?.weight,
                                    height = currentUser?.height,
                                    age = currentUser?.age,
                                    gender = currentUser?.gender,
                                    diabetes_type = currentUser?.diabetes_type,
                                    // ✅ 历史记录（AI上下文）
                                    recent_meals = recentMeals,
                                    recent_medications = recentMedications
                                )
                                
                                android.util.Log.d("NewHomeScreen", "🔄 开始调用预测API...")
                                try {
                                    val predictionResponse = homeViewModel.apiService.predictBloodGlucose(newRequest)
                                    android.util.Log.d("NewHomeScreen", "✅ 重新预测响应: ${predictionResponse.code()}")
                                    
                                    if (predictionResponse.isSuccessful) {
                                        val predictionBody = predictionResponse.body()
                                        if (predictionBody != null) {
                                            android.util.Log.d("NewHomeScreen", "🎯 预测结果: 峰值=${predictionBody.peak_value}, 风险=${predictionBody.risk_level}, 点数=${predictionBody.predictions.size}")
                                            homeViewModel.setBloodGlucosePrediction(predictionBody)
                                            android.util.Log.d("NewHomeScreen", "✅ 预测曲线已设置到ViewModel")
                                            showBgCorrectionDialog = false  // ✅ 预测成功后关闭对话框
                                            android.widget.Toast.makeText(
                                                context,
                                                "纠正已保存，预测曲线已更新（基于实测血糖 ${request.actual_value} mmol/L）",
                                                android.widget.Toast.LENGTH_LONG
                                            ).show()
                                        } else {
                                            android.util.Log.e("NewHomeScreen", "重新预测响应体为空")
                                            showBgCorrectionDialog = false  // 即使失败也关闭对话框
                                            android.widget.Toast.makeText(
                                                context,
                                                "纠正已保存，但重新预测响应为空",
                                                android.widget.Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    } else {
                                        val errorBody = predictionResponse.errorBody()?.string()
                                        android.util.Log.e("NewHomeScreen", "重新预测失败: ${predictionResponse.code()}, $errorBody")
                                        showBgCorrectionDialog = false  // 预测失败也关闭对话框
                                        android.widget.Toast.makeText(
                                            context,
                                            "纠正已保存，但重新预测失败: ${predictionResponse.message()}",
                                            android.widget.Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                } catch (e: Exception) {
                                    android.util.Log.e("NewHomeScreen", "重新预测异常", e)
                                    showBgCorrectionDialog = false  // 预测异常也关闭对话框
                                    val errorMsg = when {
                                        e is java.net.SocketTimeoutException -> "纠正已保存。AI预测耗时较长，请稍后点击\"AI预测\"按钮查看结果"
                                        else -> "纠正已保存，但重新预测失败: ${e.message}"
                                    }
                                    android.widget.Toast.makeText(
                                        context,
                                        errorMsg,
                                        android.widget.Toast.LENGTH_LONG
                                    ).show()
                                }
                            } else {
                                showBgCorrectionDialog = false  // 提交失败也关闭对话框
                                android.widget.Toast.makeText(
                                    context,
                                    "提交失败: ${response.message()}",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("NewHomeScreen", "提交纠正失败: ${e.message}", e)
                            showBgCorrectionDialog = false  // 提交异常也关闭对话框
                            android.widget.Toast.makeText(
                                context,
                                "网络错误，请稍后重试",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        } finally {
                            isSubmittingCorrection = false
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
                            // 刷新所有相关数据
                            homeViewModel.refreshNutritionData()
                            homeViewModel.fetchWaterSummary()
                            // 显示成功提示
                            android.widget.Toast.makeText(
                                context,
                                "饮水记录保存成功",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            // 显示错误提示
                            android.widget.Toast.makeText(
                                context,
                                "保存失败: ${response.message()}",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("NewHomeScreen", "保存水分记录失败: ${e.message}", e)
                        // 显示错误提示
                        android.widget.Toast.makeText(
                            context,
                            "网络错误，请稍后重试",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
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
                            // 刷新所有相关数据
                            homeViewModel.refreshNutritionData()
                            homeViewModel.fetchMedicationSummary()
                            // 显示成功提示
                            android.widget.Toast.makeText(
                                context,
                                "用药记录保存成功",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            // 显示错误提示
                            android.widget.Toast.makeText(
                                context,
                                "保存失败: ${response.message()}",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("NewHomeScreen", "保存用药记录失败: ${e.message}", e)
                        // 显示错误提示
                        android.widget.Toast.makeText(
                            context,
                            "网络错误，请稍后重试",
                            android.widget.Toast.LENGTH_SHORT
                            ).show()
                    }
                }
            }
        )
    }
    
    // 食品扫描方式选择对话框
    if (showFoodScanOptions) {
        FoodScanOptionsDialog(
            onDismiss = { showFoodScanOptions = false },
            onBarcodeSelected = {
                showFoodScanOptions = false
                onNavigateToBarcodeScanner()
            },
            onCameraSelected = {
                showFoodScanOptions = false
                onNavigateToCamera(null)
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
                currentTime = getGreeting(),
                homeViewModel = homeViewModel
            )
            
            Spacer(modifier = Modifier.height(20.dp))

            // 🎨 美化的快捷操作按钮组 (2x2网格：运动、饮水、用药、饮食)
            BeautifulQuickActions(
                onExerciseClick = { showExerciseDialog = true },
                onWaterClick = { showWaterDialog = true },
                onMedicationClick = { showMedicationDialog = true },
                onMealClick = { showFoodScanOptions = true }  // 弹出扫描方式选择
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 🎨 美化的主要营养卡片
            BeautifiedMainNutritionCard(
                dailyRecommendation = dailyRecommendation,
                todayIntake = todayIntake
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            val exerciseSummary by homeViewModel.exerciseSummary.collectAsState()
            val waterSummary by homeViewModel.waterSummary.collectAsState()
            val medicationSummary by homeViewModel.medicationSummary.collectAsState()
            
            // 🎨 美化的血糖预测卡片
            BeautifiedBloodGlucosePredictionCard(
                dailyRecommendation = dailyRecommendation,
                todayIntake = todayIntake,
                waterSummary = waterSummary,
                exerciseSummary = exerciseSummary,
                medicationSummary = medicationSummary,
                mealRecords = mealRecords,
                prediction = latestPrediction,
                corrections = bgCorrections,
                isRefreshing = isManualRefreshing,
                onRefreshClick = {
                    if (!isManualRefreshing) {
                        isManualRefreshing = true
                        smartRefreshManager.manualRefresh { success ->
                            isManualRefreshing = false
                            if (success) {
                                android.widget.Toast.makeText(
                                    context,
                                    "预测已更新",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                android.widget.Toast.makeText(
                                    context,
                                    "刷新失败，请稍后重试",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                },
                onPredictClick = {
                    if (isPredicting) {
                        android.widget.Toast.makeText(
                            context,
                            "正在进行AI预测，请稍候...",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                        return@BeautifiedBloodGlucosePredictionCard
                    }
                    // 使用真实记录数据进行AI预测
                    isPredicting = true
                    coroutineScope.launch {
                        try {
                            // 从真实记录中提取数据
                            val totalCarbs = todayIntake?.total_carbs ?: 0f
                            val insulinDose = medicationSummary?.medications
                                ?.filter { it.medication_type == "insulin" }
                                ?.sumOf { it.dosage.toDouble() }?.toFloat() ?: 0f
                            val giValue = null // TODO: 从饮食记录中计算平均GI值
                            
                            // 根据运动记录计算活动水平
                            val exerciseSummaryValue = exerciseSummary
                            val activityLevel = when {
                                exerciseSummaryValue == null || exerciseSummaryValue.total_duration == 0 -> "sedentary"
                                exerciseSummaryValue.total_duration < 30 -> "light"
                                exerciseSummaryValue.total_duration < 60 -> "moderate"
                                else -> "vigorous"
                            }
                            
                            // ✅ 获取最新血糖值：优先使用实测值 > 预测值 > 默认值
                            val currentBg = bgCorrections.firstOrNull()?.actual_value
                                ?: latestPrediction?.predictions?.firstOrNull()?.bg_value
                                ?: 5.6f
                            
                            android.util.Log.d("NewHomeScreen", "使用血糖值: $currentBg (实测=${bgCorrections.firstOrNull()?.actual_value}, 预测=${latestPrediction?.predictions?.firstOrNull()?.bg_value})")
                            
                            // ✅ 获取时间信息
                            val currentMealRecords = homeViewModel.mealRecords.value
                            val mealTime = currentMealRecords.firstOrNull()?.meal_time
                            val medicationTime = medicationSummary?.medications
                                ?.firstOrNull()?.let {
                                    try {
                                        it.created_at
                                    } catch (e: Exception) {
                                        null
                                    }
                                }
                            val currentTime = java.time.LocalDateTime.now().toString()
                            
                            // ✅ 构建历史记录（最近3次）
                            val recentMeals = currentMealRecords.take(3).mapNotNull { meal ->
                                meal.total_carbs?.let { carbs ->
                                    com.diabeat.data.model.MealHistoryItem(
                                        meal_time = meal.meal_time,
                                        total_carbs = carbs,
                                        meal_type = null,
                                        foods = meal.food_name?.joinToString(", ") ?: meal.food_items?.joinToString(", ") { it.name }
                                    )
                                }
                            }.takeIf { it.isNotEmpty() }
                            
                            val recentMedications = medicationSummary?.medications?.take(3)?.map { med ->
                                com.diabeat.data.model.MedicationHistoryItem(
                                    medication_time = med.created_at,
                                    medication_type = med.medication_type,
                                    dosage = med.dosage
                                )
                            }?.takeIf { it.isNotEmpty() }
                            
                            if (totalCarbs > 0) {
                                val request = com.diabeat.data.model.BloodGlucosePredictionRequest(
                                    total_carbs = totalCarbs,
                                    insulin_dose = insulinDose,
                                    current_bg = currentBg,  // ✅ 优先使用实测值
                                    gi_value = giValue,
                                    activity_level = activityLevel,
                                    // ✅ 时间上下文
                                    meal_time = mealTime,
                                    medication_time = medicationTime,
                                    current_time = currentTime,
                                    // ✅ 用户基础信息（个性化预测）
                                    weight = currentUser?.weight,
                                    height = currentUser?.height,
                                    age = currentUser?.age,
                                    gender = currentUser?.gender,
                                    diabetes_type = currentUser?.diabetes_type,
                                    // ✅ 历史记录（AI上下文）
                                    recent_meals = recentMeals,
                                    recent_medications = recentMedications
                                )
                                
                                android.util.Log.d("NewHomeScreen", "AI预测请求: carbs=$totalCarbs, insulin=$insulinDose, bg=$currentBg, activity=$activityLevel, meal_time=$mealTime, user_weight=${currentUser?.weight}, user_age=${currentUser?.age}")
                                
                                val response = homeViewModel.apiService.predictBloodGlucose(request)
                                if (response.isSuccessful && response.body() != null) {
                                    homeViewModel.setBloodGlucosePrediction(response.body())
                                    homeViewModel.fetchBloodGlucoseCorrections()
                                    android.widget.Toast.makeText(
                                        context,
                                        "AI预测成功！基于今日记录: ${totalCarbs.toInt()}g碳水, ${activityLevel}活动",
                                        android.widget.Toast.LENGTH_LONG
                                    ).show()
                                } else {
                                    android.widget.Toast.makeText(
                                        context,
                                        "预测失败: ${response.message()}",
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                }
                            } else {
                                android.widget.Toast.makeText(
                                    context,
                                    "暂无饮食记录，无法进行AI预测",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("NewHomeScreen", "血糖预测失败", e)
                            android.widget.Toast.makeText(
                                context,
                                "预测失败: ${e.message}",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        } finally {
                            isPredicting = false
                        }
                    }
                },
                onCorrectionClick = { showBgCorrectionDialog = true }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 今日记录列表：饮食记录、运动记录、用药记录、饮水记录
            
            // 1. 饮食记录列表（新格式：时间戳 | 食物名称 | 总碳水 | 卡路里）
            MealRecordsListCard(
                mealRecords = mealRecords
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // 2. 运动记录列表
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
            
            // 3. 用药记录列表
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
            
            // 4. 饮水记录列表
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
                text = "剩余 = 每日目标 - 已摄入",
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
    homeViewModel: HomeViewModel  // 新增参数以获取水分数据
) {
    // 从ViewModel获取实时数据
    val waterSummary by homeViewModel.waterSummary.collectAsState()
    val mealRecords by homeViewModel.mealRecords.collectAsState()
    
    // 改进的血糖预测算法 - 考虑碳水和水分摄入
    // 这是一个简化的估算，实际血糖受多种因素影响
    val predictedBloodGlucose = remember(todayIntake, waterSummary, mealRecords) {
        if (todayIntake != null && dailyRecommendation != null) {
            // === 1. 基础血糖值 ===
            val baseGlucose = 5.6f // 空腹正常血糖：5.6 mmol/L
            
            // === 2. 碳水摄入影响 ===
            // 每15g碳水约增加1 mmol/L血糖
            val carbsIntake = todayIntake.total_carbs
            val glucoseFromCarbs = carbsIntake / 15f
            
            // === 3. 水分摄入影响（帮助稳定血糖）===
            // 从水分记录API获取实时数据
            val waterIntake = waterSummary?.total_ml?.toFloat() ?: 2000f
            val waterFactor = when {
                waterIntake >= 2000f -> 1.0f // 充足，最佳状态
                waterIntake >= 1000f -> 0.95f // 轻微不足
                else -> 0.9f // 严重不足，血糖浓缩
            }
            
            // === 4. 时间衰减因子 ===
            // 根据最后一餐时间动态计算
            val timeDecayFactor = calculateTimeDecay(mealRecords.firstOrNull()?.meal_time)
            
            // === 5. 综合计算 ===
            // 预测血糖 = 基础值 + (碳水影响 × 时间衰减 × 水分影响)
            val predicted = baseGlucose + 
                (glucoseFromCarbs * timeDecayFactor * waterFactor)
            
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
                    text = "⚠️ 预测基于碳水摄入和水分摄入综合计算。仅供参考，请以实际血糖监测为准。",
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

/**
 * 计算血糖趋势说明
 */
private fun calculateTrendExplanation(
    mealRecords: List<MealRecordResponse>,
    medicationSummary: com.diabeat.data.model.TodayMedicationSummary?,
    prediction: BloodGlucosePredictionResponse?
): String {
    if (prediction == null || prediction.predictions.isEmpty()) return ""
    
    try {
        // 获取最近一餐时间
        val lastMealTime = mealRecords.firstOrNull()?.meal_time ?: return ""
        val lastMeal = java.time.Instant.parse(lastMealTime)
        val now = java.time.Instant.now()
        val minutesSinceMeal = java.time.Duration.between(lastMeal, now).toMinutes().toInt()
        
        // 检查是否有胰岛素
        val hasInsulin = medicationSummary?.medications?.any { 
            it.medication_type == "胰岛素" 
        } == true
        
        // 分析预测趋势
        val predictions = prediction.predictions.take(3)
        val isRising = predictions.size >= 2 && predictions[1].bg_value > predictions[0].bg_value
        val isFalling = predictions.size >= 2 && predictions[1].bg_value < predictions[0].bg_value
        
        // 根据不同阶段返回不同说明
        return when {
            minutesSinceMeal < 30 && hasInsulin && isRising -> {
                "📈 餐后早期：血糖正在上升（正常现象）。胰岛素15分钟后开始起效"
            }
            minutesSinceMeal < 30 && !hasInsulin && isRising -> {
                "⚠️ 血糖正在快速上升，建议及时注射胰岛素控制血糖"
            }
            minutesSinceMeal in 30..60 && isRising -> {
                "⬆️ 继续上升中，预计60-90分钟达到峰值"
            }
            minutesSinceMeal in 60..120 && hasInsulin -> {
                if (isFalling) {
                    "📉 胰岛素正在发挥作用，血糖开始下降"
                } else {
                    "➡️ 接近峰值，胰岛素作用逐渐增强"
                }
            }
            minutesSinceMeal > 120 && isFalling -> {
                "✅ 餐后吸收期结束，血糖趋于稳定"
            }
            minutesSinceMeal > 240 -> {
                "💡 距离上次进餐已${minutesSinceMeal/60}小时，建议适时补充能量"
            }
            else -> {
                "📊 血糖处于${if (isRising) "上升" else if (isFalling) "下降" else "平稳"}趋势"
            }
        }
    } catch (e: Exception) {
        android.util.Log.e("TrendExplanation", "计算趋势说明失败: ${e.message}", e)
        return ""
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

/**
 * 饮食记录列表卡片 - 显示时间戳、食物名称、总碳水、卡路里
 */
@Composable
private fun MealRecordsListCard(
    mealRecords: List<MealRecordResponse>
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
            // 标题
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Restaurant,
                        contentDescription = "饮食记录",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "饮食记录",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "共 ${mealRecords.size} 条",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            if (mealRecords.isEmpty()) {
                // 空状态提示
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "暂无饮食记录\n点击「饮食」按钮添加",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                // 表头
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp, horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "时间",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(0.8f)
                    )
                    Text(
                        text = "食物",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1.5f)
                    )
                    Text(
                        text = "碳水",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(0.8f),
                        textAlign = TextAlign.End
                    )
                    Text(
                        text = "卡路里",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(0.9f),
                        textAlign = TextAlign.End
                    )
                }
                
                Divider()
                
                // 记录列表
                mealRecords.sortedByDescending { it.meal_time }.forEach { record ->
                    MealRecordItem(record = record)
                    Divider()
                }
            }
        }
    }
}

/**
 * 单条饮食记录项
 */
@Composable
private fun MealRecordItem(record: MealRecordResponse) {
    val time = try {
        java.time.Instant.parse(record.meal_time)
            .atZone(java.time.ZoneId.systemDefault())
            .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
    } catch (e: Exception) {
        record.meal_time.substring(11, 16)
    }
    
    // 从food_name字段获取食物名称
    val foodName = if (!record.food_name.isNullOrEmpty()) {
        if (record.food_name.size == 1) {
            record.food_name[0]
        } else {
            "${record.food_name[0]} 等${record.food_name.size}种"
        }
    } else if (!record.food_items.isNullOrEmpty()) {
        // 如果food_name为空，从food_items获取
        if (record.food_items.size == 1) {
            record.food_items[0].name
        } else {
            "${record.food_items[0].name} 等${record.food_items.size}种"
        }
    } else {
        "未知食物"
    }
    
    // 从API返回的营养信息字段获取
    val totalCarbs = record.total_carbs ?: 0f
    val totalCalories = record.total_calories ?: 0f
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 时间
        Text(
            text = time,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(0.8f)
        )
        
        // 食物名称
        Text(
            text = foodName,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1.5f),
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
        
        // 总碳水（暂时显示为"-"）
        Text(
            text = if (totalCarbs > 0) "${totalCarbs.roundToInt()}g" else "-",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(0.8f),
            textAlign = TextAlign.End
        )
        
        // 总卡路里（暂时显示为"-"）
        Text(
            text = if (totalCalories > 0) "${totalCalories.roundToInt()}" else "-",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.tertiary,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(0.9f),
            textAlign = TextAlign.End
        )
    }
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
    currentTime: String,
    homeViewModel: HomeViewModel
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
            val selectedDate by homeViewModel.selectedDate.collectAsState()
            TodayOverviewCard(
                selectedDate = selectedDate,
                onDateChange = { date -> homeViewModel.selectDate(date) }
            )
        }
    }
}

/**
 * 今日概览卡片 - 美化版，支持日期切换和日历选择
 */
@Composable
private fun TodayOverviewCard(
    selectedDate: java.time.LocalDate,
    onDateChange: (java.time.LocalDate) -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }
    val today = java.time.LocalDate.now()
    val isToday = selectedDate.isEqual(today)
    
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
                    onClick = { onDateChange(selectedDate.minusDays(1)) },
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
                
                // 下一天按钮（如果是今天，禁用并变灰）
                IconButton(
                    onClick = { 
                        if (!isToday) {
                            onDateChange(selectedDate.plusDays(1))
                        }
                    },
                    enabled = !isToday,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowRight,
                        contentDescription = if (isToday) "已是今天" else "下一天",
                        tint = if (isToday) {
                            Color(0xFF667EEA).copy(alpha = 0.3f) // 灰色
                        } else {
                            Color(0xFF667EEA)
                        },
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
                onDateChange(date)
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
            // 运动记录按钮
            QuickActionButton(
                icon = Icons.Default.DirectionsRun,
                label = "运动",
                color = Color(0xFF667EEA),
                onClick = onExerciseClick,
                modifier = Modifier.weight(1f)
            )
            // 饮水记录按钮
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
            // 用药记录按钮
            QuickActionButton(
                icon = Icons.Default.Medication,
                label = "用药",
                color = Color(0xFFEF5350),
                onClick = onMedicationClick,
                modifier = Modifier.weight(1f)
            )
            // 饮食记录按钮
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
 * 美化的血糖预测卡片 - 支持AI预测与纠正
 */
@Composable
private fun BeautifiedBloodGlucosePredictionCard(
    dailyRecommendation: DailyNutritionRecommendation?,
    todayIntake: TodayNutritionIntake?,
    waterSummary: com.diabeat.data.model.TodayWaterSummary?,
    exerciseSummary: com.diabeat.data.model.TodayExerciseSummary?,
    medicationSummary: com.diabeat.data.model.TodayMedicationSummary?,
    mealRecords: List<MealRecordResponse>,
    prediction: BloodGlucosePredictionResponse?,
    corrections: List<BloodGlucoseCorrectionResponse>,
    onPredictClick: () -> Unit,
    onCorrectionClick: () -> Unit,
    onRefreshClick: () -> Unit = {},
    isRefreshing: Boolean = false
) {
    val fallbackPrediction = remember(todayIntake, waterSummary, mealRecords) {
        if (todayIntake != null && dailyRecommendation != null) {
            val baseGlucose = 5.6f
            val carbsIntake = todayIntake.total_carbs
            val glucoseFromCarbs = carbsIntake / 15f
            val waterIntake = waterSummary?.total_ml?.toFloat() ?: 2000f
            val waterFactor = when {
                waterIntake >= 2000f -> 1.0f
                waterIntake >= 1000f -> 0.95f
                else -> 0.9f
            }
            val timeDecayFactor = calculateTimeDecay(mealRecords.firstOrNull()?.meal_time)
            val predicted = baseGlucose +
                (glucoseFromCarbs * timeDecayFactor * waterFactor)
            predicted.coerceIn(3.9f, 11.1f)
        } else {
            5.6f
        }
    }
    
    val latestCorrection = corrections.firstOrNull()
    
    // 调试日志
    android.util.Log.d("BloodGlucoseCard", "corrections数量: ${corrections.size}")
    android.util.Log.d("BloodGlucoseCard", "latestCorrection: ${latestCorrection?.actual_value}")
    android.util.Log.d("BloodGlucoseCard", "prediction当前值: ${prediction?.predictions?.firstOrNull()?.bg_value}")
    android.util.Log.d("BloodGlucoseCard", "fallback: $fallbackPrediction")
    
    // 显示当前血糖值：优先使用实测值 > 预测的起始值 > fallback
    val displayValue = when {
        // 如果有实测数据，显示最新的实测值
        latestCorrection != null -> {
            android.util.Log.d("BloodGlucoseCard", "使用实测值: ${latestCorrection.actual_value}")
            latestCorrection.actual_value
        }
        // 如果有预测数据，显示预测的第一个点（当前血糖）
        prediction != null && prediction.predictions.isNotEmpty() -> {
            val value = prediction.predictions.first().bg_value
            android.util.Log.d("BloodGlucoseCard", "使用预测当前值: $value")
            value
        }
        // 否则使用fallback
        else -> {
            android.util.Log.d("BloodGlucoseCard", "使用fallback: $fallbackPrediction")
            fallbackPrediction
        }
    }
    
    android.util.Log.d("BloodGlucoseCard", "最终displayValue: $displayValue")
    
    val glucoseColors = when {
        displayValue < 3.9f -> listOf(Color(0xFFFF9800), Color(0xFFFF5722))
        displayValue > 7.8f -> listOf(Color(0xFFEF5350), Color(0xFFE91E63))
        else -> listOf(Color(0xFF66BB6A), Color(0xFF4CAF50))
    }
    
    val glucoseStatus = when {
        displayValue < 3.9f -> "偏低"
        displayValue > 7.8f -> "偏高"
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "当前血糖值",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = when {
                                    latestCorrection != null -> "实测值"
                                    prediction != null -> "AI预测当前值"
                                    else -> "基于今日数据估算"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        // 手动刷新按钮
                        IconButton(
                            onClick = onRefreshClick,
                            enabled = !isRefreshing,
                            modifier = Modifier.size(36.dp)
                        ) {
                            if (isRefreshing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "刷新预测",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                    
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = glucoseColors[0].copy(alpha = 0.15f)
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = String.format("%.1f", displayValue),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = glucoseColors[0]
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (prediction != null) prediction.risk_level.uppercase() else glucoseStatus,
                                style = MaterialTheme.typography.labelMedium,
                                color = glucoseColors[1]
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "本日碳水",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${todayIntake?.total_carbs?.roundToInt() ?: 0} g",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "水分摄入",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${waterSummary?.total_ml ?: 0} ml",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                if (prediction != null && prediction.predictions.isNotEmpty()) {
                    // 调试日志
                    android.util.Log.d("PredictionCurve", "显示预测曲线，prediction_id=${prediction.prediction_id}, 点数=${prediction.predictions.size}")
                    prediction.predictions.take(4).forEachIndexed { index, point ->
                        android.util.Log.d("PredictionCurve", "点$index: ${point.time_minutes}分钟 = ${point.bg_value}")
                    }
                    
                    // 计算趋势说明
                    val trendExplanation = remember(mealRecords, medicationSummary, prediction) {
                        calculateTrendExplanation(
                            mealRecords = mealRecords,
                            medicationSummary = medicationSummary,
                            prediction = prediction
                        )
                    }
                    
                    Text(
                        text = "预测曲线（部分节点）",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    // 趋势说明 - 小字体
                    if (trendExplanation.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = trendExplanation,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            lineHeight = 16.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        prediction.predictions.take(4).forEach { point ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${point.time_minutes} 分钟",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${String.format("%.1f", point.bg_value)} mmol/L",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                } else {
                    Text(
                        text = "点击“AI预测”获取更精准的血糖变化曲线。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                Divider()
                Spacer(modifier = Modifier.height(16.dp))
                
                if (latestCorrection != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "实测 ${String.format("%.1f", latestCorrection.actual_value)} mmol/L",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "预测 ${String.format("%.1f", latestCorrection.predicted_value)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "偏差 ${String.format("%.1f", latestCorrection.difference)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (latestCorrection.difference >= 0) Color(0xFFD32F2F) else Color(0xFF388E3C),
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Text(
                        text = "暂无纠正记录，记录一次实测血糖可帮助模型自适应。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onPredictClick,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = glucoseColors[0]
                        )
                    ) {
                        Text("AI预测", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    
                    OutlinedButton(
                        onClick = onCorrectionClick,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.White.copy(alpha = 0.7f)
                        )
                    ) {
                        Text("提交纠正", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

