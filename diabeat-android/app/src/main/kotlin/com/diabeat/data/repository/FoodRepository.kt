package com.diabeat.data.repository

import android.util.Log
import com.diabeat.data.api.OpenFoodFactsApi
import com.diabeat.data.api.USDAFoodDataApi
import com.diabeat.data.model.*
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class FoodRepository(
    private val usdaApi: USDAFoodDataApi,
    private val openFoodFactsApi: OpenFoodFactsApi
) {
    companion object {
        private const val TAG = "FoodRepository"
        // USDA API Key（免费申请: https://fdc.nal.usda.gov/api-key-signup.html）
        private const val USDA_API_KEY = "aemoN1y46TGxk8eZUUOFyoJRHKnahq29wN0JCUqY"
    }
    
    /**
     * 扫描条形码 - 双API互补校验
     * 策略:
     * 1. 并行调用USDA + OpenFoodFacts
     * 2. 优先使用USDA（美国用户为主）
     * 3. OpenFoodFacts补充和验证
     * 4. 数据对比，取最完整的
     */
    suspend fun scanBarcode(barcode: String): Result<FoodProduct> = coroutineScope {
        try {
            Log.d(TAG, "🔍 开始扫描条形码: $barcode")
            
            // 并行调用两个API
            val usdaDeferred = async { fetchFromUSDA(barcode) }
            val offDeferred = async { fetchFromOpenFoodFacts(barcode) }
            
            val usdaResult = usdaDeferred.await()
            val offResult = offDeferred.await()
            
            // 互补校验逻辑
            val product = when {
                // 1. 两个都成功 → 双重验证 ✅
                usdaResult.isSuccess && offResult.isSuccess -> {
                    Log.d(TAG, "✅ 双重验证成功")
                    mergeAndVerify(usdaResult.getOrNull()!!, offResult.getOrNull()!!)
                }
                
                // 2. 仅USDA成功 → 使用USDA（美国食品权威）
                usdaResult.isSuccess -> {
                    Log.d(TAG, "✅ USDA数据可用")
                    usdaResult.getOrNull()!!
                }
                
                // 3. 仅OpenFoodFacts成功 → 使用OFF（全球食品）
                offResult.isSuccess -> {
                    Log.d(TAG, "✅ OpenFoodFacts数据可用")
                    offResult.getOrNull()!!
                }
                
                // 4. 都失败 → 返回错误
                else -> {
                    Log.e(TAG, "❌ 未找到产品信息")
                    return@coroutineScope Result.failure(
                        Exception("Product not found in USDA or OpenFoodFacts")
                    )
                }
            }
            
            Result.success(product)
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ 扫描失败", e)
            Result.failure(e)
        }
    }
    
    /**
     * 从USDA FoodData获取数据
     */
    private suspend fun fetchFromUSDA(barcode: String): Result<FoodProduct> {
        return try {
            val response = usdaApi.searchByBarcode(barcode, apiKey = USDA_API_KEY)
            
            if (response.isSuccessful && response.body() != null) {
                val data = response.body()!!
                val food = data.foods?.firstOrNull()
                
                if (food != null) {
                    val product = convertUSDAToProduct(food, barcode)
                    Log.d(TAG, "USDA found: ${product.productName}")
                    Result.success(product)
                } else {
                    Result.failure(Exception("No USDA data"))
                }
            } else {
                Result.failure(Exception("USDA API error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "USDA fetch error", e)
            Result.failure(e)
        }
    }
    
    /**
     * 从OpenFoodFacts获取数据
     */
    private suspend fun fetchFromOpenFoodFacts(barcode: String): Result<FoodProduct> {
        return try {
            val response = openFoodFactsApi.getProductByBarcode(barcode)
            
            if (response.isSuccessful && response.body() != null) {
                val data = response.body()!!
                
                if (data.status == 1 && data.product != null) {
                    val product = convertOFFToProduct(data.product!!, barcode)
                    Log.d(TAG, "OFF found: ${product.productName}")
                    Result.success(product)
                } else {
                    Result.failure(Exception("No OFF data"))
                }
            } else {
                Result.failure(Exception("OFF API error: ${response.code()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "OFF fetch error", e)
            Result.failure(e)
        }
    }
    
    /**
     * 合并和验证两个数据源
     * 策略:
     * - 基础信息优先USDA
     * - 图片优先OpenFoodFacts
     * - 营养数据取平均值（差异<10%）或USDA
     */
    private fun mergeAndVerify(usda: FoodProduct, off: FoodProduct): FoodProduct {
        // 验证营养数据是否一致（允许10%误差）
        val carbsDiff = kotlin.math.abs(usda.carbs - off.carbs) / usda.carbs
        val proteinDiff = kotlin.math.abs(usda.protein - off.protein) / usda.protein
        val fatDiff = kotlin.math.abs(usda.fat - off.fat) / usda.fat
        
        val isConsistent = carbsDiff < 0.1 && proteinDiff < 0.1 && fatDiff < 0.1
        
        Log.d(TAG, "数据一致性: ${if (isConsistent) "✅" else "⚠️"} (碳水差${(carbsDiff * 100).toInt()}%)")
        
        return FoodProduct(
            id = usda.id,
            barcode = usda.barcode,
            productName = usda.productName,  // USDA名称更标准
            brand = usda.brand ?: off.brand,
            imageUrl = off.imageUrl ?: usda.imageUrl,  // OFF图片更丰富
            
            // 营养数据：一致则取平均，否则取USDA
            calories = if (isConsistent) (usda.calories + off.calories) / 2 else usda.calories,
            carbs = if (isConsistent) (usda.carbs + off.carbs) / 2 else usda.carbs,
            protein = if (isConsistent) (usda.protein + off.protein) / 2 else usda.protein,
            fat = if (isConsistent) (usda.fat + off.fat) / 2 else usda.fat,
            fiber = usda.fiber ?: off.fiber,
            sugars = usda.sugars ?: off.sugars,
            sodium = usda.sodium ?: off.sodium,
            
            giValue = usda.giValue ?: off.giValue,
            servingSize = usda.servingSize,
            
            dataSource = FoodDataSource.DUAL_VERIFIED  // 标记为双重验证
        )
    }
    
    /**
     * 转换USDA数据到统一模型
     */
    private fun convertUSDAToProduct(food: USDAFood, barcode: String): FoodProduct {
        val nutrients = food.foodNutrients.associate { 
            it.nutrientName to it.value 
        }
        
        return FoodProduct(
            id = "usda_${food.fdcId}",
            barcode = barcode,
            productName = food.description,
            brand = food.brandOwner,
            imageUrl = null,  // USDA没有图片
            
            calories = nutrients["Energy"]?.div(4.184f) ?: 0f,  // kJ to kcal
            carbs = nutrients["Carbohydrate, by difference"] ?: 0f,
            protein = nutrients["Protein"] ?: 0f,
            fat = nutrients["Total lipid (fat)"] ?: 0f,
            fiber = nutrients["Fiber, total dietary"],
            sugars = nutrients["Sugars, total including NLEA"],
            sodium = nutrients["Sodium, Na"],
            
            giValue = null,  // USDA没有GI值
            servingSize = 100f,
            dataSource = FoodDataSource.USDA
        )
    }
    
    /**
     * 转换OpenFoodFacts数据到统一模型
     */
    private fun convertOFFToProduct(product: OpenFoodFactsProduct, barcode: String): FoodProduct {
        val nutrients = product.nutriments
        
        return FoodProduct(
            id = "off_$barcode",
            barcode = barcode,
            productName = product.productName ?: "Unknown Product",
            brand = product.brands,
            imageUrl = product.imageFrontUrl ?: product.imageUrl,
            
            calories = nutrients?.energyKcal ?: 0f,
            carbs = nutrients?.carbohydrates ?: 0f,
            protein = nutrients?.proteins ?: 0f,
            fat = nutrients?.fat ?: 0f,
            fiber = nutrients?.fiber,
            sugars = nutrients?.sugars,
            sodium = nutrients?.sodium?.times(1000),  // g to mg
            
            giValue = null,  // OFF也没有GI值（需要单独数据库）
            servingSize = parseServingSize(product.servingSize),
            dataSource = FoodDataSource.OPEN_FOOD_FACTS
        )
    }
    
    /**
     * 解析份量字符串（如"100g", "1 serving (30g)"）
     */
    private fun parseServingSize(servingSize: String?): Float {
        if (servingSize == null) return 100f
        
        val regex = """(\d+\.?\d*)""".toRegex()
        val match = regex.find(servingSize)
        return match?.value?.toFloatOrNull() ?: 100f
    }
}
