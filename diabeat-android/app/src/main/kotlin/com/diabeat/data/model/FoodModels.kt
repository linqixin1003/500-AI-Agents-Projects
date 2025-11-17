package com.diabeat.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 统一的食品模型（用于UI显示）
 */
@Serializable
data class FoodProduct(
    val id: String,
    val barcode: String? = null,
    val productName: String,
    val brand: String? = null,
    val imageUrl: String? = null,
    
    // 营养信息（per 100g）
    val calories: Float,           // 卡路里
    val carbs: Float,              // 碳水化合物（g）
    val protein: Float,            // 蛋白质（g）
    val fat: Float,                // 脂肪（g）
    val fiber: Float? = null,      // 纤维（g）
    val sugars: Float? = null,     // 糖分（g）
    val sodium: Float? = null,     // 钠（mg）
    
    // 糖尿病特殊指标
    val giValue: Int? = null,      // GI值（升糖指数）
    val servingSize: Float = 100f, // 建议份量（g）
    
    // 数据来源
    val dataSource: FoodDataSource
)

enum class FoodDataSource {
    USDA,                    // USDA FoodData Central
    OPEN_FOOD_FACTS,        // OpenFoodFacts
    DUAL_VERIFIED           // 双重验证
}

/**
 * USDA FoodData Central API响应
 */
@Serializable
data class USDAFoodResponse(
    val foods: List<USDAFood>? = null,
    @SerialName("fdcId") val fdcId: Int? = null,
    val description: String? = null,
    val brandOwner: String? = null,
    val gtinUpc: String? = null,
    val foodNutrients: List<USDANutrient>? = null
)

@Serializable
data class USDAFood(
    @SerialName("fdcId") val fdcId: Int,
    val description: String,
    val brandOwner: String? = null,
    val gtinUpc: String? = null,
    val foodNutrients: List<USDANutrient>
)

@Serializable
data class USDANutrient(
    val nutrientId: Int,
    val nutrientName: String,
    val nutrientNumber: String? = null,
    val unitName: String,
    val value: Float
)

/**
 * OpenFoodFacts API响应
 */
@Serializable
data class OpenFoodFactsResponse(
    val status: Int,
    val code: String? = null,
    @SerialName("status_verbose") val statusVerbose: String? = null,
    val product: OpenFoodFactsProduct? = null
)

@Serializable
data class OpenFoodFactsProduct(
    @SerialName("product_name") val productName: String? = null,
    val brands: String? = null,
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("image_front_url") val imageFrontUrl: String? = null,
    val nutriments: OpenFoodFactsNutriments? = null,
    @SerialName("nutrient_levels") val nutrientLevels: Map<String, String>? = null,
    @SerialName("serving_size") val servingSize: String? = null
)

@Serializable
data class OpenFoodFactsNutriments(
    @SerialName("energy-kcal_100g") val energyKcal: Float? = null,
    @SerialName("energy-kcal") val energyKcalServing: Float? = null,
    @SerialName("carbohydrates_100g") val carbohydrates: Float? = null,
    @SerialName("proteins_100g") val proteins: Float? = null,
    @SerialName("fat_100g") val fat: Float? = null,
    @SerialName("fiber_100g") val fiber: Float? = null,
    @SerialName("sugars_100g") val sugars: Float? = null,
    @SerialName("sodium_100g") val sodium: Float? = null,
    @SerialName("salt_100g") val salt: Float? = null
)

/**
 * 食品扫描请求（选择扫描方式）
 */
enum class ScanMethod {
    BARCODE,    // 条形码扫描
    CAMERA      // 相机拍照（OCR）
}

/**
 * 食品日志请求
 */
@Serializable
data class FoodLogRequest(
    val productName: String,
    val barcode: String? = null,
    val servings: Float,           // 份数
    val totalCarbs: Float,         // 总碳水（g）
    val totalCalories: Float,      // 总卡路里
    val mealTime: String,          // ISO时间
    val mealType: String? = null,  // 餐次类型（早餐/午餐/晚餐/加餐）
    val imageUrl: String? = null
)
