package com.diabeat.ui.food

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.diabeat.data.model.FoodLogRequest
import com.diabeat.data.model.FoodProduct
import com.diabeat.data.repository.FoodRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDateTime

class FoodScanViewModel(
    private val foodRepository: FoodRepository
) : ViewModel() {
    
    companion object {
        private const val TAG = "FoodScanViewModel"
    }
    
    private val _uiState = MutableStateFlow<FoodScanUiState>(FoodScanUiState.Idle)
    val uiState: StateFlow<FoodScanUiState> = _uiState.asStateFlow()
    
    private val _currentProduct = MutableStateFlow<FoodProduct?>(null)
    val currentProduct: StateFlow<FoodProduct?> = _currentProduct.asStateFlow()
    
    /**
     * 扫描条形码
     */
    fun scanBarcode(barcode: String) {
        viewModelScope.launch {
            _uiState.value = FoodScanUiState.Loading
            Log.d(TAG, "开始扫描条形码: $barcode")
            
            val result = foodRepository.scanBarcode(barcode)
            
            result.fold(
                onSuccess = { product ->
                    Log.d(TAG, "✅ 扫描成功: ${product.productName}")
                    _currentProduct.value = product
                    _uiState.value = FoodScanUiState.Success(product)
                },
                onFailure = { error ->
                    Log.e(TAG, "❌ 扫描失败", error)
                    _uiState.value = FoodScanUiState.Error(
                        error.message ?: "Failed to find product"
                    )
                }
            )
        }
    }
    
    /**
     * 记录食品到日志
     */
    fun logFood(product: FoodProduct, servings: Float) {
        viewModelScope.launch {
            try {
                val request = FoodLogRequest(
                    productName = product.productName,
                    barcode = product.barcode,
                    servings = servings,
                    totalCarbs = product.carbs * servings,
                    totalCalories = product.calories * servings,
                    mealTime = LocalDateTime.now().toString(),
                    mealType = determineMealType(),
                    imageUrl = product.imageUrl
                )
                
                // TODO: 调用后端API记录
                // homeViewModel.logMeal(...)
                
                Log.d(TAG, "✅ 食品已记录: ${request.productName}, ${request.totalCarbs}g碳水")
                _uiState.value = FoodScanUiState.Logged
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ 记录失败", e)
                _uiState.value = FoodScanUiState.Error("Failed to log food")
            }
        }
    }
    
    /**
     * 根据当前时间判断餐次
     */
    private fun determineMealType(): String {
        val hour = LocalDateTime.now().hour
        return when (hour) {
            in 5..10 -> "breakfast"
            in 11..14 -> "lunch"
            in 17..20 -> "dinner"
            else -> "snack"
        }
    }
    
    /**
     * 重置状态
     */
    fun reset() {
        _uiState.value = FoodScanUiState.Idle
        _currentProduct.value = null
    }
}

/**
 * UI状态
 */
sealed class FoodScanUiState {
    object Idle : FoodScanUiState()
    object Loading : FoodScanUiState()
    data class Success(val product: FoodProduct) : FoodScanUiState()
    data class Error(val message: String) : FoodScanUiState()
    object Logged : FoodScanUiState()
}
