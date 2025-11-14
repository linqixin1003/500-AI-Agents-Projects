package com.diabeat.ui.onboarding

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.diabeat.network.RetrofitClient
import kotlinx.coroutines.launch

/**
 * 引导页 - 收集用户基本信息
 * 每个信息一页，使用滚动选择器
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    onComplete: (UserOnboardingData) -> Unit,
    onSkip: () -> Unit
) {
    var currentStep by remember { mutableStateOf(0) }
    
    // 默认值：美国30岁女性，2型糖尿病
    var height by remember { mutableStateOf(163) }  // 163 cm (5'4")
    var weight by remember { mutableStateOf(70) }   // 70 kg (154 lbs)
    var age by remember { mutableStateOf(30) }
    var gender by remember { mutableStateOf("female") }
    var diabetesType by remember { mutableStateOf("type2") }
    
    // 标记每个字段是否使用了默认值
    var heightIsDefault by remember { mutableStateOf(true) }
    var weightIsDefault by remember { mutableStateOf(true) }
    var ageIsDefault by remember { mutableStateOf(true) }
    var genderIsDefault by remember { mutableStateOf(true) }
    var diabetesTypeIsDefault by remember { mutableStateOf(true) }
    
    val totalSteps = 6  // 身高、体重、年龄、性别、糖尿病类型、完成
    
    // 跳过当前页面，使用默认值并进入下一步
    val skipCurrentStep = {
        when (currentStep) {
            0 -> {
                height = 163
                heightIsDefault = true
            }
            1 -> {
                weight = 70
                weightIsDefault = true
            }
            2 -> {
                age = 30
                ageIsDefault = true
            }
            3 -> {
                gender = "female"
                genderIsDefault = true
            }
            4 -> {
                diabetesType = "type2"
                diabetesTypeIsDefault = true
            }
        }
        // 跳到下一步
        if (currentStep < totalSteps - 1) {
            currentStep++
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "欢迎使用 DiabEat",
                        style = MaterialTheme.typography.titleLarge
                    ) 
                },
                actions = {
                    if (currentStep < totalSteps - 1) {
                        TextButton(onClick = skipCurrentStep) {
                            Text(
                                "跳过",
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // 进度指示器
            StepProgressIndicator(
                currentStep = currentStep,
                totalSteps = totalSteps,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
            )
            
            // 内容区域
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (currentStep) {
                    0 -> HeightPickerStep(
                        selectedHeight = height,
                        onHeightChange = { 
                            height = it
                            heightIsDefault = false
                        }
                    )
                    1 -> WeightPickerStep(
                        selectedWeight = weight,
                        onWeightChange = { 
                            weight = it
                            weightIsDefault = false
                        }
                    )
                    2 -> AgePickerStep(
                        selectedAge = age,
                        onAgeChange = { 
                            age = it
                            ageIsDefault = false
                        }
                    )
                    3 -> GenderSelectionStep(
                        selectedGender = gender,
                        onGenderChange = { 
                            gender = it
                            genderIsDefault = false
                        }
                    )
                    4 -> DiabetesTypeStep(
                        diabetesType = diabetesType,
                        onDiabetesTypeChange = { 
                            diabetesType = it
                            diabetesTypeIsDefault = false
                        }
                    )
                    5 -> CompletionStep(
                        height = height,
                        weight = weight,
                        age = age,
                        gender = gender,
                        diabetesType = diabetesType,
                        onSave = {
                            onComplete(
                                UserOnboardingData(
                                    height = height.toFloat(),
                                    weight = weight.toFloat(),
                                    age = age,
                                    gender = gender,
                                    diabetesType = diabetesType,
                                    isDefaultData = heightIsDefault && weightIsDefault && 
                                                    ageIsDefault && genderIsDefault && 
                                                    diabetesTypeIsDefault
                                )
                            )
                        }
                    )
                }
            }
            
            // 底部按钮（最后一步不显示，因为完成页有自己的保存按钮）
            if (currentStep < totalSteps - 1) {
                BottomNavigationButtons(
                    currentStep = currentStep,
                    totalSteps = totalSteps,
                    onNext = {
                        currentStep++
                    },
                    onBack = {
                        if (currentStep > 0) {
                            currentStep--
                        }
                    },
                    modifier = Modifier.padding(24.dp)
                )
            }
        }
    }
}

@Composable
private fun StepProgressIndicator(
    currentStep: Int,
    totalSteps: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        repeat(totalSteps) { index ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(4.dp)
                    .padding(horizontal = 4.dp)
                    .background(
                        color = if (index <= currentStep) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        shape = RoundedCornerShape(2.dp)
                    )
            )
        }
    }
}

@Composable
private fun HeightPickerStep(
    selectedHeight: Int,
    onHeightChange: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "📏 你的身高是？",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "选择你的身高",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        // 滚动选择器
        NumberPicker(
            value = selectedHeight,
            onValueChange = onHeightChange,
            range = 100..250,
            unit = "cm"
        )
    }
}

@Composable
private fun WeightPickerStep(
    selectedWeight: Int,
    onWeightChange: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "⚖️ 你的体重是？",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "选择你的体重",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        NumberPicker(
            value = selectedWeight,
            onValueChange = onWeightChange,
            range = 30..200,
            unit = "kg"
        )
    }
}

@Composable
private fun AgePickerStep(
    selectedAge: Int,
    onAgeChange: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "🎂 你的年龄是？",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "选择你的年龄",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        NumberPicker(
            value = selectedAge,
            onValueChange = onAgeChange,
            range = 1..120,
            unit = "岁"
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NumberPicker(
    value: Int,
    onValueChange: (Int) -> Unit,
    range: IntRange,
    unit: String,
    modifier: Modifier = Modifier
) {
    val values = range.toList()
    val initialIndex = values.indexOf(value).coerceAtLeast(0)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    
    // 计算中间可见项的索引
    val centerItemIndex by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val viewportCenter = layoutInfo.viewportStartOffset + layoutInfo.viewportSize.height / 2
            
            layoutInfo.visibleItemsInfo
                .minByOrNull { itemInfo ->
                    val itemCenter = itemInfo.offset + itemInfo.size / 2
                    kotlin.math.abs(itemCenter - viewportCenter)
                }?.index ?: 0
        }
    }
    
    // 监听滚动停止，更新选中值
    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress && centerItemIndex in values.indices) {
            onValueChange(values[centerItemIndex])
        }
    }
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(250.dp),
        contentAlignment = Alignment.Center
    ) {
        // 渐变遮罩 - 顶部
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            Color.Transparent
                        )
                    )
                )
        )
        
        // 渐变遮罩 - 底部
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
        )
        
        // 选中指示器
        Box(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(60.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                .align(Alignment.Center)
        )
        
        // 数字列表
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 95.dp),
            flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
        ) {
            items(values.size) { index ->
                val itemValue = values[index]
                val isSelected = index == centerItemIndex
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = itemValue.toString(),
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = if (isSelected) 48.sp else 32.sp,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            },
                            modifier = Modifier.alpha(if (isSelected) 1f else 0.4f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = unit,
                            style = MaterialTheme.typography.titleLarge,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            },
                            modifier = Modifier.alpha(if (isSelected) 1f else 0.4f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GenderSelectionStep(
    selectedGender: String,
    onGenderChange: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "👤 你的性别是？",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "选择你的性别",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        // 性别选择
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SelectionCard(
                icon = "👨",
                label = "男性",
                isSelected = selectedGender == "male",
                onClick = { onGenderChange("male") }
            )
            
            SelectionCard(
                icon = "👩",
                label = "女性",
                isSelected = selectedGender == "female",
                onClick = { onGenderChange("female") }
            )
            
            SelectionCard(
                icon = "🧑",
                label = "其它",
                isSelected = selectedGender == "other",
                onClick = { onGenderChange("other") }
            )
        }
    }
}

@Composable
private fun DiabetesTypeStep(
    diabetesType: String,
    onDiabetesTypeChange: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "🏥 糖尿病类型",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "选择你的糖尿病类型",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SelectionCard(
                icon = "1️⃣",
                label = "1型糖尿病",
                description = "胰岛素依赖型",
                isSelected = diabetesType == "type1",
                onClick = { onDiabetesTypeChange("type1") }
            )
            
            SelectionCard(
                icon = "2️⃣",
                label = "2型糖尿病",
                description = "最常见类型",
                isSelected = diabetesType == "type2",
                onClick = { onDiabetesTypeChange("type2") }
            )
            
            SelectionCard(
                icon = "🤰",
                label = "妊娠糖尿病",
                description = "孕期糖尿病",
                isSelected = diabetesType == "gestational",
                onClick = { onDiabetesTypeChange("gestational") }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectionCard(
    icon: String,
    label: String,
    description: String? = null,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(
                2.dp,
                MaterialTheme.colorScheme.primary
            )
        } else null,
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 6.dp else 2.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = icon,
                style = MaterialTheme.typography.displaySmall,
                fontSize = 48.sp,
                modifier = Modifier.padding(end = 16.dp)
            )
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                if (description != null) {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            if (isSelected) {
                Text(
                    text = "✓",
                    style = MaterialTheme.typography.displaySmall,
                    fontSize = 36.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun CompletionStep(
    height: Int,
    weight: Int,
    age: Int,
    gender: String,
    diabetesType: String,
    onSave: () -> Unit
) {
    var isSaving by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val apiService = RetrofitClient.apiService
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "✅",
            style = MaterialTheme.typography.displayLarge,
            fontSize = 80.sp
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "确认你的信息",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "请确认以下信息无误",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // 信息摘要卡片
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SummaryRow(icon = "📏", label = "身高", value = "$height cm")
                Divider()
                SummaryRow(icon = "⚖️", label = "体重", value = "$weight kg")
                Divider()
                SummaryRow(icon = "🎂", label = "年龄", value = "$age 岁")
                Divider()
                SummaryRow(
                    icon = "👤", 
                    label = "性别", 
                    value = when(gender) {
                        "male" -> "男性"
                        "female" -> "女性"
                        else -> "其它"
                    }
                )
                Divider()
                SummaryRow(
                    icon = "🏥", 
                    label = "糖尿病类型", 
                    value = when(diabetesType) {
                        "type1" -> "1型糖尿病"
                        "type2" -> "2型糖尿病"
                        "gestational" -> "妊娠糖尿病"
                        else -> diabetesType
                    }
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // 错误提示
        if (saveError != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "⚠️",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(end = 12.dp)
                    )
                    Column {
                        Text(
                            text = "保存失败",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = saveError ?: "未知错误",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // 保存按钮
        Button(
            onClick = {
                isSaving = true
                saveError = null
                
                // 调用API保存用户信息
                coroutineScope.launch {
                    try {
                        val response = apiService.completeOnboarding(
                            height = height.toFloat(),
                            weight = weight.toFloat(),
                            age = age,
                            gender = gender,
                            diabetesType = diabetesType
                        )
                        
                        if (response.isSuccessful) {
                            // API成功，调用回调关闭引导页
                            onSave()
                        } else {
                            // API失败，显示错误信息
                            isSaving = false
                            saveError = "保存失败: ${response.code()} - ${response.message()}"
                        }
                    } catch (e: Exception) {
                        // API失败，显示错误信息，允许用户重试
                        isSaving = false
                        saveError = e.message ?: "网络错误，请重试"
                    }
                }
            },
            enabled = !isSaving,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            if (isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text("保存中...")
            } else {
                Text(
                    "保存并开始",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "我们将为你生成个性化的营养建议",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun SummaryRow(
    icon: String,
    label: String,
    value: String
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
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(end = 12.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun BottomNavigationButtons(
    currentStep: Int,
    totalSteps: Int,
    onNext: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 返回按钮
        if (currentStep > 0 && currentStep < totalSteps - 1) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.weight(1f)
            ) {
                Text("返回")
            }
        }
        
        // 下一步/完成按钮
        Button(
            onClick = onNext,
            modifier = Modifier.weight(if (currentStep > 0 && currentStep < totalSteps - 1) 1f else 1f)
        ) {
            Text(
                text = when (currentStep) {
                    totalSteps - 1 -> "完成设置"
                    else -> "下一步"
                },
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
    }
}

data class UserOnboardingData(
    val height: Float,
    val weight: Float,
    val age: Int,
    val gender: String,
    val diabetesType: String,
    val isDefaultData: Boolean = false  // 标记是否全部使用默认值
)
