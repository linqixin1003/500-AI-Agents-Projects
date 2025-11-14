package com.diabeat.ui.recognition

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import coil.compose.rememberAsyncImagePainter
import com.diabeat.data.model.FoodRecognitionResponse
import com.diabeat.viewmodel.FoodRecognitionViewModel
import com.diabeat.R
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodRecognitionScreen(
    viewModel: FoodRecognitionViewModel,
    imageUri: Uri,
    mealType: String? = null, // 添加餐次类型参数
    onBack: () -> Unit,
    onComplete: (FoodRecognitionResponse) -> Unit,
    onRetakePhoto: () -> Unit = {},
    onSaveSuccess: () -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val recognitionState by viewModel.recognitionState.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    
    // 自动开始识别
    LaunchedEffect(imageUri) {
        viewModel.recognizeFood(context, imageUri)
    }
    
    // 识别成功后加载剩余能量
    LaunchedEffect(recognitionState) {
        recognitionState?.let {
            viewModel.loadRemainingCalories(context)
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // 顶部栏
        TopAppBar(
            title = { 
                Column {
                    Text(stringResource(id = R.string.food_recognition_title))
                    // 显示餐次类型
                    mealType?.let { type ->
                        val mealTypeName = when(type) {
                            "breakfast" -> "早餐"
                            "lunch" -> "午餐"
                            "dinner" -> "晚餐"
                            "snack" -> "加餐"
                            else -> ""
                        }
                        if (mealTypeName.isNotEmpty()) {
                            Text(
                                text = mealTypeName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Text(stringResource(id = R.string.back_button))
                }
            }
        )
        
        // 图片预览
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = rememberAsyncImagePainter(imageUri),
                contentDescription = stringResource(id = R.string.selected_food_image),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            
            // 加载指示器
            if (isLoading) {
                CircularProgressIndicator()
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 识别结果
        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(stringResource(id = R.string.recognizing_food))
                    }
                }
            }
            
            recognitionState != null -> {
                val result = recognitionState!!
                
                // 识别结果标题
                Text(
                    text = stringResource(id = R.string.recognition_result),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(16.dp)
                )
                
                // 置信度
                Text(
                    text = stringResource(id = R.string.confidence_format, ((result.total_confidence ?: 0f) * 100).toInt() ?: 0),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // 食物列表
                result.foods.forEach { food ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            // 食物名称
                            Text(
                                text = food.name,
                                style = MaterialTheme.typography.titleMedium
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            // 基本信息
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                            Text(
                                text = stringResource(id = R.string.weight_format, food.weight?.toInt() ?: 0),
                                style = MaterialTheme.typography.bodyMedium
                            )
                            val confidencePercent = (food.confidence?.times(100f)?.toInt()) ?: 0
                            Text(
                                    text = stringResource(id = R.string.confidence_format, confidencePercent),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            
                            // 营养成分（对糖尿病人重要）
                            if (food.carbs != null || food.protein != null || food.fat != null) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Divider()
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                Text(
                                    text = stringResource(id = R.string.nutrition_info),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                // 营养成分网格
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    // 卡路里
                                    if (food.calories != null) {
                                        NutritionRow(
                                            label = stringResource(id = R.string.calories),
                                            value = "${food.calories.toInt()} ${stringResource(id = R.string.kcal_unit)}"
                                        )
                                    }
                                    
                                    // 碳水化合物
                                    if (food.carbs != null) {
                                        NutritionRow(
                                            label = stringResource(id = R.string.carbs),
                                            value = "${food.carbs.toInt()}g"
                                        )
                                    }
                                    
                                    // 净碳水化合物（对糖尿病人最重要）
                                    if (food.net_carbs != null) {
                                        NutritionRow(
                                            label = stringResource(id = R.string.net_carbs),
                                            value = "${food.net_carbs.toInt()}g",
                                            isImportant = true
                                        )
                                    }
                                    
                                    // 蛋白质
                                    if (food.protein != null) {
                                        NutritionRow(
                                            label = stringResource(id = R.string.protein),
                                            value = "${food.protein.toInt()}g"
                                        )
                                    }
                                    
                                    // 脂肪
                                    if (food.fat != null) {
                                        NutritionRow(
                                            label = stringResource(id = R.string.fat),
                                            value = "${food.fat.toInt()}g"
                                        )
                                    }
                                    
                                    // 纤维
                                    if (food.fiber != null) {
                                        NutritionRow(
                                            label = stringResource(id = R.string.fiber),
                                            value = "${food.fiber.toInt()}g"
                                        )
                                    }
                                    
                                    // GI值
                                    if (food.gi_value != null) {
                                        NutritionRow(
                                            label = stringResource(id = R.string.gi_value),
                                            value = food.gi_value.toInt().toString()
                                        )
                                    }
                                    
                                    // GL值（对糖尿病人重要）
                                    if (food.gl_value != null) {
                                        NutritionRow(
                                            label = stringResource(id = R.string.gl_value),
                                            value = food.gl_value.toInt().toString(),
                                            isImportant = true
                                        )
                                    }
                                }
                            }
                            
                            // 烹饪方式
                            if (!food.cooking_method.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = stringResource(id = R.string.cooking_method_format, food.cooking_method),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            
                            // 建议食用量（对糖尿病人重要）
                            food.recommendation?.let { rec ->
                                Spacer(modifier = Modifier.height(16.dp))
                                Divider()
                                Spacer(modifier = Modifier.height(12.dp))
                                
                                // 使用服务端返回的 can_eat_all 字段判断
                                if (rec.can_eat_all == true) {
                                    // 配菜没有超标，简化提示
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                                                shape = MaterialTheme.shapes.medium
                                            )
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "✅",
                                            style = MaterialTheme.typography.titleLarge,
                                            modifier = Modifier.padding(end = 8.dp)
                                        )
                                        Text(
                                            text = "可以全部食用",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                } else {
                                    // 主食或超标情况，显示详细建议
                                    // 建议标题
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "💡 建议食用量",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        
                                        // 调整百分比
                                        val adjustmentText = if (rec.adjustment_percent > 0) {
                                            "+${rec.adjustment_percent.toInt()}%"
                                        } else {
                                            "${rec.adjustment_percent.toInt()}%"
                                        }
                                        Text(
                                            text = adjustmentText,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (rec.adjustment_percent < 0) {
                                                MaterialTheme.colorScheme.error
                                            } else {
                                                MaterialTheme.colorScheme.primary
                                            },
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    // 建议重量
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "建议重量:",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            text = "${rec.recommended_weight.toInt()}g",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    
                                    // 建议碳水
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "建议碳水:",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            text = "${rec.recommended_carbs.toInt()}g",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    
                                    // 建议原因
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = rec.reason,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    
                                    // 警告信息
                                    rec.warning?.let { warning ->
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "⚠️ $warning",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 剩余能量提示
                viewModel.remainingCalories?.let { remaining ->
                    if (remaining < 0) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "⚠️",
                                    style = MaterialTheme.typography.titleLarge,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "今日能量已超量",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    Text(
                                        text = "已超出 ${(-remaining).toInt()} kcal，建议适量运动",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    } else if (remaining < 200) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "💡",
                                    style = MaterialTheme.typography.titleLarge,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "剩余能量较少",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                    Text(
                                        text = "今日剩余 ${remaining.toInt()} kcal，请合理安排饮食",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
                
                // 按钮组：重拍和添加到饮食记录
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 重拍按钮
                    OutlinedButton(
                        onClick = {
                            onRetakePhoto()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                ) {
                        Text(stringResource(id = R.string.retake_photo))
                    }
                    
                    // 添加到饮食记录按钮
                    Button(
                        onClick = {
                            // 添加Toast提示以便调试
                            android.widget.Toast.makeText(
                                context,
                                "开始保存饮食记录...",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                            
                            android.util.Log.d("FoodRecognition", "点击添加到饮食记录按钮")
                            android.util.Log.d("FoodRecognition", "餐次类型: $mealType")
                            android.util.Log.d("FoodRecognition", "识别结果: ${result.foods.size} 个食物")
                            
                            viewModel.saveToMealRecord(context, result, mealType) {
                                // 保存成功后，调用 onSaveSuccess 关闭页面并返回首页
                                android.util.Log.d("FoodRecognition", "保存成功，准备返回首页")
                                android.widget.Toast.makeText(
                                    context,
                                    "保存成功！",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                                onSaveSuccess()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        enabled = !isSaving
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.width(20.dp).height(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(stringResource(id = R.string.add_to_meal_record))
                        }
                    }
                }
            }
            
            else -> {
                // 错误状态
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(id = R.string.recognition_failed),
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(id = R.string.please_retry),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = {
                        viewModel.recognizeFood(context, imageUri)
                    }) {
                        Text(stringResource(id = R.string.retry_button))
                    }
                }
            }
        }
    }
}

// 营养成分行组件
@Composable
fun NutritionRow(
    label: String,
    value: String,
    isImportant: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isImportant) {
                    Modifier
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                } else {
                    Modifier
                }
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isImportant) FontWeight.Bold else FontWeight.Normal,
            color = if (isImportant) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isImportant) FontWeight.Bold else FontWeight.Normal,
            color = if (isImportant) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}

