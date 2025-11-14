package com.diabeat.viewmodel

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.diabeat.data.model.FoodRecognitionResponse
import com.diabeat.data.model.FoodItemInput
import com.diabeat.data.model.NutritionCalculationRequest
import com.diabeat.data.model.MealRecordRequest
import com.diabeat.network.ApiService
import com.diabeat.network.RetrofitClient
import com.diabeat.utils.ImageUtil
import com.diabeat.utils.TokenManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class FoodRecognitionViewModel : ViewModel() {
    private val apiService: ApiService = RetrofitClient.apiService
    
    private val _recognitionState = MutableStateFlow<FoodRecognitionResponse?>(null)
    val recognitionState: StateFlow<FoodRecognitionResponse?> = _recognitionState.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()
    
    private val _saveError = MutableStateFlow<String?>(null)
    val saveError: StateFlow<String?> = _saveError.asStateFlow()
    
    var remainingCalories: Float? = null
        private set
    
    fun recognizeFood(context: Context, imageUri: Uri) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                TokenManager.ensureAuthenticated(context.applicationContext, apiService)

                withContext(Dispatchers.IO) {
                    // 将 Uri 转换为 File
                    val bitmap = ImageUtil.getBitmapFromUri(context, imageUri)
                    if (bitmap == null) {
                        throw Exception("无法读取图片")
                    }
                    
                    // 保存为临时文件
                    val tempFile = File(context.cacheDir, "temp_image_${System.currentTimeMillis()}.jpg")
                    ImageUtil.saveBitmapToFile(bitmap, tempFile)
                    
                    // 创建 MultipartBody.Part
                    val requestFile = tempFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
                    val imagePart = MultipartBody.Part.createFormData("image", tempFile.name, requestFile)
                    
                    // 调用 API
                    val response = apiService.recognizeFood(imagePart)
                    
                    if (response.isSuccessful && response.body() != null) {
                        _recognitionState.value = response.body()!!
                    } else {
                        throw Exception("识别失败: ${response.message()}")
                    }
                    
                    // 清理临时文件
                    tempFile.delete()
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "未知错误"
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun loadRemainingCalories(context: Context, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                TokenManager.ensureAuthenticated(context.applicationContext, apiService)
                
                withContext(Dispatchers.IO) {
                    // 获取每日推荐营养
                    val recommendationResponse = apiService.getDailyRecommendation()
                    // 获取今日摄入
                    val intakeResponse = apiService.getTodayIntake()
                    
                    if (recommendationResponse.isSuccessful && recommendationResponse.body() != null &&
                        intakeResponse.isSuccessful && intakeResponse.body() != null) {
                        val recommendation = recommendationResponse.body()!!
                        val intake = intakeResponse.body()!!
                        
                        remainingCalories = recommendation.daily_calories - intake.total_calories
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                onComplete()
            }
        }
    }
    
    fun saveToMealRecord(
        context: Context,
        recognitionResult: FoodRecognitionResponse,
        mealType: String? = null, // 添加餐次类型参数
        onSuccess: () -> Unit
    ) {
        android.util.Log.d("FoodRecognitionVM", "开始保存到饮食记录")
        android.util.Log.d("FoodRecognitionVM", "餐次: $mealType, 食物数: ${recognitionResult.foods.size}")
        viewModelScope.launch {
            _isSaving.value = true
            _saveError.value = null
            
            try {
                withContext(Dispatchers.IO) {
                    // 1. 计算营养成分
                    val foodInputs = recognitionResult.foods.map { food ->
                        FoodItemInput(
                            name = food.name,
                            weight = food.weight ?: 100f,
                            cooking_method = food.cooking_method
                        )
                    }
                    
                    val nutritionRequest = NutritionCalculationRequest(foods = foodInputs)
                    android.util.Log.d("FoodRecognitionVM", "调用营养计算API")
                    val nutritionResponse = apiService.calculateNutrition(
                        nutritionRequest,
                        foodRecognitionId = recognitionResult.recognition_id
                    )
                    
                    android.util.Log.d("FoodRecognitionVM", "营养计算响应: ${nutritionResponse.code()}")
                    if (!nutritionResponse.isSuccessful || nutritionResponse.body() == null) {
                        throw Exception("计算营养成分失败: ${nutritionResponse.message()}")
                    }
                    
                    val nutritionResult = nutritionResponse.body()!!
                    android.util.Log.d("FoodRecognitionVM", "营养记录ID: ${nutritionResult.nutrition_record_id}")
                    
                    // 2. 保存用餐记录（使用返回的 nutrition_record_id）
                    val mealTime = LocalDateTime.now()
                    
                    // 根据餐次类型生成备注
                    val mealTypeName = when(mealType) {
                        "breakfast" -> "早餐"
                        "lunch" -> "午餐"
                        "dinner" -> "晚餐"
                        "snack" -> "加餐"
                        else -> null
                    }
                    val notes = if (mealTypeName != null) {
                        "通过食物识别添加 - $mealTypeName"
                    } else {
                        "通过食物识别添加"
                    }
                    
                    val mealRequest = MealRecordRequest(
                        meal_time = mealTime.format(DateTimeFormatter.ISO_DATE_TIME),
                        food_recognition_id = recognitionResult.recognition_id,
                        nutrition_record_id = nutritionResult.nutrition_record_id, // 使用计算营养时返回的记录ID
                        notes = notes,
                        meal_type = mealType // 传递餐次类型
                    )
                    
                    android.util.Log.d("FoodRecognitionVM", "调用保存饮食记录API")
                    android.util.Log.d("FoodRecognitionVM", "请求: meal_type=$mealType, notes=$notes")
                    val mealResponse = apiService.createMealRecord(mealRequest)
                    
                    android.util.Log.d("FoodRecognitionVM", "保存响应: ${mealResponse.code()}")
                    if (mealResponse.isSuccessful) {
                        android.util.Log.d("FoodRecognitionVM", "保存成功，准备更新剩余能量")
                        // 更新剩余能量（使用context）
                        loadRemainingCalories(context) {
                            android.util.Log.d("FoodRecognitionVM", "剩余能量更新完成，触发成功回调")
                            onSuccess()
                        }
                    } else {
                        throw Exception("保存饮食记录失败: ${mealResponse.message()}")
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("FoodRecognitionVM", "保存失败: ${e.message}", e)
                _saveError.value = e.message ?: "保存失败"
                e.printStackTrace()
                
                // 添加错误Toast提示
                android.widget.Toast.makeText(
                    context,
                    "保存失败: ${e.message}",
                    android.widget.Toast.LENGTH_LONG
                ).show()
            } finally {
                _isSaving.value = false
                android.util.Log.d("FoodRecognitionVM", "保存流程结束")
            }
        }
    }
}

