package com.diabeat.viewmodel

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.diabeat.data.model.MealRecordResponse
import com.diabeat.data.model.InsulinRecordResponse
import com.diabeat.data.model.UserResponse
import com.diabeat.data.model.DailyNutritionRecommendation
import com.diabeat.data.model.TodayNutritionIntake
import com.diabeat.network.ApiService
import com.diabeat.network.RetrofitClient
import com.diabeat.utils.TokenManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    val apiService: ApiService = RetrofitClient.apiService  // 改为public以便UI访问
    private val context = application.applicationContext

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    private val _mealRecords = MutableStateFlow<List<MealRecordResponse>>(emptyList())
    val mealRecords: StateFlow<List<MealRecordResponse>> = _mealRecords.asStateFlow()

    private val _insulinRecords = MutableStateFlow<List<InsulinRecordResponse>>(emptyList())
    val insulinRecords: StateFlow<List<InsulinRecordResponse>> = _insulinRecords.asStateFlow()

    private val _isLoadingRecords = MutableStateFlow(false)
    val isLoadingRecords: StateFlow<Boolean> = _isLoadingRecords.asStateFlow()

    private val _errorRecords = MutableStateFlow<String?>(null)
    val errorRecords: StateFlow<String?> = _errorRecords.asStateFlow()

    private val _user = MutableStateFlow<UserResponse?>(null)
    val user: StateFlow<UserResponse?> = _user.asStateFlow()

    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()
    
    private val _dailyRecommendation = MutableStateFlow<DailyNutritionRecommendation?>(null)
    val dailyRecommendation: StateFlow<DailyNutritionRecommendation?> = _dailyRecommendation.asStateFlow()
    
    private val _todayIntake = MutableStateFlow<TodayNutritionIntake?>(null)
    val todayIntake: StateFlow<TodayNutritionIntake?> = _todayIntake.asStateFlow()
    
    private val _isLoadingNutrition = MutableStateFlow(false)
    val isLoadingNutrition: StateFlow<Boolean> = _isLoadingNutrition.asStateFlow()
    
    // ==================== 运动记录相关 ====================
    private val _exerciseSummary = MutableStateFlow<com.diabeat.data.model.TodayExerciseSummary?>(null)
    val exerciseSummary: StateFlow<com.diabeat.data.model.TodayExerciseSummary?> = _exerciseSummary.asStateFlow()
    
    // ==================== 水分记录相关 ====================
    private val _waterSummary = MutableStateFlow<com.diabeat.data.model.TodayWaterSummary?>(null)
    val waterSummary: StateFlow<com.diabeat.data.model.TodayWaterSummary?> = _waterSummary.asStateFlow()
    
    // ==================== 用药记录相关 ====================
    private val _medicationSummary = MutableStateFlow<com.diabeat.data.model.TodayMedicationSummary?>(null)
    val medicationSummary: StateFlow<com.diabeat.data.model.TodayMedicationSummary?> = _medicationSummary.asStateFlow()

    init {
        // 初始化时尝试进行设备认证
        performDeviceAuth()
        
        // 初始化时加载当天所有数据
        viewModelScope.launch {
            if (isAuthenticated.value) {
                fetchExerciseSummary()
                fetchWaterSummary()
                fetchMedicationSummary()
            }
        }
    }

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
        fetchRecordsForDate(date)
    }

    fun performDeviceAuth(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _isLoadingRecords.value = true
            _errorRecords.value = null
            try {
                val authResult = TokenManager.ensureAuthenticated(context, apiService, forceRefresh)
                _user.value = authResult.user
                _isAuthenticated.value = true
                _errorRecords.value = null
                fetchRecordsForDate(_selectedDate.value)
                fetchNutritionData()
            } catch (e: Exception) {
                _isAuthenticated.value = false
                _user.value = null
                TokenManager.clearToken(context)
                _errorRecords.value = "设备认证异常: ${e.message}"
            } finally {
                _isLoadingRecords.value = false
            }
        }
    }

    fun fetchRecordsForDate(date: LocalDate) {
        // 只有在认证成功后才加载数据
        if (!isAuthenticated.value) {
            _errorRecords.value = "用户未认证，正在尝试重新认证..."
            performDeviceAuth(forceRefresh = true)
            return
        }

        viewModelScope.launch {
            _isLoadingRecords.value = true
            _errorRecords.value = null
            try {
                val dateString = date.toString()
                val mealResponse = apiService.getMealRecords(date = dateString)
                val insulinResponse = apiService.getInsulinRecords(date = dateString)

                if (mealResponse.isSuccessful && mealResponse.body() != null) {
                    _mealRecords.value = mealResponse.body()!!
                } else {
                    _errorRecords.value = mealResponse.message()
                }

                if (insulinResponse.isSuccessful && insulinResponse.body() != null) {
                    _insulinRecords.value = insulinResponse.body()!!
                } else {
                    _errorRecords.value = insulinResponse.message()
                }
            } catch (e: Exception) {
                _errorRecords.value = e.message
            } finally {
                _isLoadingRecords.value = false
            }
        }
    }

    fun fetchNutritionData() {
        if (!isAuthenticated.value) {
            return
        }
        
        viewModelScope.launch {
            _isLoadingNutrition.value = true
            try {
                val recommendationResponse = apiService.getDailyRecommendation()
                val intakeResponse = apiService.getTodayIntake()
                
                if (recommendationResponse.isSuccessful && recommendationResponse.body() != null) {
                    _dailyRecommendation.value = recommendationResponse.body()!!
                }
                
                if (intakeResponse.isSuccessful && intakeResponse.body() != null) {
                    _todayIntake.value = intakeResponse.body()!!
                }
            } catch (e: Exception) {
                _errorRecords.value = "获取营养数据失败: ${e.message}"
            } finally {
                _isLoadingNutrition.value = false
            }
        }
    }
    
    fun refreshNutritionData() {
        fetchRecordsForDate(_selectedDate.value)
        fetchNutritionData()
        fetchExerciseSummary()
        fetchWaterSummary()
        fetchMedicationSummary()
    }
    
    // ==================== 运动记录方法 ====================
    
    fun fetchExerciseSummary() {
        if (!isAuthenticated.value) {
            return
        }
        
        viewModelScope.launch {
            try {
                val response = apiService.getTodayExerciseSummary()
                if (response.isSuccessful && response.body() != null) {
                    _exerciseSummary.value = response.body()!!
                } else {
                    android.util.Log.e("HomeViewModel", "获取运动汇总失败: ${response.message()}")
                }
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "获取运动汇总异常: ${e.message}", e)
            }
        }
    }
    
    // ==================== 水分记录方法 ====================
    
    fun fetchWaterSummary() {
        if (!isAuthenticated.value) {
            return
        }
        
        viewModelScope.launch {
            try {
                val response = apiService.getTodayWaterSummary()
                if (response.isSuccessful && response.body() != null) {
                    _waterSummary.value = response.body()!!
                } else {
                    android.util.Log.e("HomeViewModel", "获取水分汇总失败: ${response.message()}")
                }
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "获取水分汇总异常: ${e.message}", e)
            }
        }
    }
    
    // ==================== 用药记录方法 ====================
    
    fun fetchMedicationSummary() {
        if (!isAuthenticated.value) {
            return
        }
        
        viewModelScope.launch {
            try {
                val response = apiService.getTodayMedicationSummary()
                if (response.isSuccessful && response.body() != null) {
                    _medicationSummary.value = response.body()!!
                } else {
                    android.util.Log.e("HomeViewModel", "获取用药汇总失败: ${response.message()}")
                }
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "获取用药汇总异常: ${e.message}", e)
            }
        }
    }
}
