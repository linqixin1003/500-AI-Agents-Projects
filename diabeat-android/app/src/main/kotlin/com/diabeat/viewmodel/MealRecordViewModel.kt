package com.diabeat.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.diabeat.network.ApiResult
import com.diabeat.network.RetrofitClient
import com.diabeat.data.model.MealRecordRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class MealRecordViewModel : ViewModel() {
    private val _result = MutableStateFlow<ApiResult<Unit>>(ApiResult.Loading)
    val result: StateFlow<ApiResult<Unit>> = _result
    
    fun saveMealRecord(mealTime: LocalDateTime, notes: String) {
        viewModelScope.launch {
            _result.value = ApiResult.Loading
            try {
                val request = MealRecordRequest(
                    meal_time = mealTime.format(DateTimeFormatter.ISO_DATE_TIME),
                    notes = notes
                )
                val response = RetrofitClient.apiService.createMealRecord(request)
                if (response.isSuccessful) {
                    _result.value = ApiResult.Success(Unit)
                } else {
                    _result.value = ApiResult.Error(Exception("保存失败"))
                }
            } catch (e: Exception) {
                _result.value = ApiResult.Error(e)
            }
        }
    }
}

