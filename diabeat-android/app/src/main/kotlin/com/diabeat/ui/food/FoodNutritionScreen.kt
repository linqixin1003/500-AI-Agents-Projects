package com.diabeat.ui.food

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.diabeat.data.model.FoodDataSource
import com.diabeat.data.model.FoodProduct

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodNutritionScreen(
    product: FoodProduct,
    onLogFood: (Float) -> Unit,  // servings
    onBack: () -> Unit
) {
    var servings by remember { mutableStateOf(1.0f) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Food Details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 产品图片
            if (product.imageUrl != null) {
                AsyncImage(
                    model = product.imageUrl,
                    contentDescription = product.productName,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop
                )
            }
            
            // 产品名称和品牌
            Text(
                text = product.productName,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            
            if (product.brand != null) {
                Text(
                    text = product.brand,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // 数据来源标签
            DataSourceChip(product.dataSource)
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 营养环形图 + 数值
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 左侧：环形图
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    NutritionDonut(
                        carbs = product.carbs * servings,
                        protein = product.protein * servings,
                        fat = product.fat * servings
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "${(product.calories * servings).toInt()}",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "cal",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // 右侧：营养数值
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(start = 16.dp)
                ) {
                    NutrientRow(
                        emoji = "🟡",
                        name = "Carbs",  // ⚠️ 糖尿病关键指标
                        value = "${(product.carbs * servings).format(1)}g",
                        color = Color(0xFFFFB74D),
                        isHighlight = true  // 碳水高亮
                    )
                    NutrientRow(
                        emoji = "🔵",
                        name = "Protein",
                        value = "${(product.protein * servings).format(1)}g",
                        color = Color(0xFF64B5F6)
                    )
                    NutrientRow(
                        emoji = "🔴",
                        name = "Fats",
                        value = "${(product.fat * servings).format(1)}g",
                        color = Color(0xFFFF8A80)
                    )
                    
                    // 额外信息
                    if (product.fiber != null) {
                        NutrientRow(
                            emoji = "🟢",
                            name = "Fiber",
                            value = "${(product.fiber * servings).format(1)}g",
                            color = Color(0xFF81C784)
                        )
                    }
                    if (product.sugars != null) {
                        NutrientRow(
                            emoji = "⚪",
                            name = "Sugars",
                            value = "${(product.sugars * servings).format(1)}g",
                            color = Color(0xFFE0E0E0),
                            isWarning = product.sugars > 10  // 高糖警告
                        )
                    }
                }
            }
            
            // 糖尿病友好度评分
            DiabetesFriendlyScore(product)
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 份数调整器
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Number of Servings",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        IconButton(
                            onClick = { if (servings > 0.5f) servings -= 0.5f },
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(Icons.Default.Remove, null, tint = Color.White)
                        }
                        
                        Text(
                            "$servings",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.widthIn(min = 40.dp)
                        )
                        
                        IconButton(
                            onClick = { servings += 0.5f },
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(Icons.Default.Add, null, tint = Color.White)
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Log Food按钮
            Button(
                onClick = { onLogFood(servings) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4CAF50)
                ),
                shape = RoundedCornerShape(28.dp)
            ) {
                Icon(Icons.Default.Add, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Log Food", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * 营养环形图
 */
@Composable
fun NutritionDonut(
    carbs: Float,
    protein: Float,
    fat: Float
) {
    val total = carbs * 4 + protein * 4 + fat * 9  // 卡路里计算
    
    // 角度（从-90度开始，顺时针）
    val carbsAngle = (carbs * 4 / total) * 360f
    val proteinAngle = (protein * 4 / total) * 360f
    val fatAngle = (fat * 9 / total) * 360f
    
    // 动画
    val animatedCarbsAngle by animateFloatAsState(
        targetValue = carbsAngle,
        animationSpec = tween(1000, easing = FastOutSlowInEasing)
    )
    val animatedProteinAngle by animateFloatAsState(
        targetValue = proteinAngle,
        animationSpec = tween(1000, delayMillis = 200, easing = FastOutSlowInEasing)
    )
    val animatedFatAngle by animateFloatAsState(
        targetValue = fatAngle,
        animationSpec = tween(1000, delayMillis = 400, easing = FastOutSlowInEasing)
    )
    
    Canvas(modifier = Modifier.fillMaxSize()) {
        val strokeWidth = 20f
        val radius = (size.minDimension - strokeWidth) / 2
        val center = Offset(size.width / 2, size.height / 2)
        
        var startAngle = -90f
        
        // 碳水（黄色）
        drawArc(
            color = Color(0xFFFFB74D),
            startAngle = startAngle,
            sweepAngle = animatedCarbsAngle,
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
        startAngle += animatedCarbsAngle
        
        // 蛋白质（蓝色）
        drawArc(
            color = Color(0xFF64B5F6),
            startAngle = startAngle,
            sweepAngle = animatedProteinAngle,
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
        startAngle += animatedProteinAngle
        
        // 脂肪（红色）
        drawArc(
            color = Color(0xFFFF8A80),
            startAngle = startAngle,
            sweepAngle = animatedFatAngle,
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
    }
}

@Composable
fun NutrientRow(
    emoji: String,
    name: String,
    value: String,
    color: Color,
    isHighlight: Boolean = false,
    isWarning: Boolean = false
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(emoji, fontSize = 20.sp)
        Text(
            name,
            fontSize = 16.sp,
            fontWeight = if (isHighlight) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.width(70.dp)
        )
        Text(
            value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = if (isWarning) Color.Red else color
        )
    }
}

@Composable
fun DataSourceChip(source: FoodDataSource) {
    val (text, color) = when (source) {
        FoodDataSource.USDA -> "USDA Verified" to Color(0xFF4CAF50)
        FoodDataSource.OPEN_FOOD_FACTS -> "OpenFoodFacts" to Color(0xFF2196F3)
        FoodDataSource.DUAL_VERIFIED -> "Dual Verified ✓✓" to Color(0xFFFF9800)
    }
    
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.2f)
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            color = color,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun DiabetesFriendlyScore(product: FoodProduct) {
    // 糖尿病友好度评分（0-100）
    val score = calculateDiabetesScore(product)
    val (rating, color) = when {
        score >= 75 -> "Excellent" to Color(0xFF4CAF50)
        score >= 50 -> "Good" to Color(0xFFFFB74D)
        score >= 25 -> "Moderate" to Color(0xFFFF9800)
        else -> "Caution" to Color(0xFFF44336)
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Diabetes Friendly Score",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    rating,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 进度条
            LinearProgressIndicator(
                progress = score / 100f,
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = color,
                trackColor = color.copy(alpha = 0.2f)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 建议
            if (product.carbs > 30) {
                Text(
                    "⚠️ High carb food - Monitor glucose closely",
                    fontSize = 12.sp,
                    color = Color.Red
                )
            }
            if (product.sugars != null && product.sugars > 10) {
                Text(
                    "⚠️ High sugar content - Limit serving size",
                    fontSize = 12.sp,
                    color = Color.Red
                )
            }
        }
    }
}

/**
 * 计算糖尿病友好度评分
 */
private fun calculateDiabetesScore(product: FoodProduct): Int {
    var score = 100
    
    // 碳水含量（每100g）
    if (product.carbs > 50) score -= 30
    else if (product.carbs > 30) score -= 20
    else if (product.carbs > 15) score -= 10
    
    // 糖分
    product.sugars?.let {
        if (it > 15) score -= 25
        else if (it > 10) score -= 15
        else if (it > 5) score -= 5
    }
    
    // 纤维（加分）
    product.fiber?.let {
        if (it > 5) score += 10
        else if (it > 3) score += 5
    }
    
    // GI值
    product.giValue?.let {
        if (it > 70) score -= 20
        else if (it > 55) score -= 10
        else score += 10  // 低GI加分
    }
    
    return score.coerceIn(0, 100)
}

// 扩展函数：格式化float
private fun Float.format(decimals: Int): String = "%.${decimals}f".format(this)
