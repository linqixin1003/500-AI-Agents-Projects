package com.diabeat.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.diabeat.network.ApiResult
import com.diabeat.network.RetrofitClient
import com.diabeat.data.model.InsulinRecordRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class InsulinRecordViewModel : ViewModel() {
    private val _result = MutableStateFlow<ApiResult<Unit>>(ApiResult.Loading)
    val result: StateFlow<ApiResult<Unit>> = _result
    
    fun saveInsulinRecord(injectionTime: LocalDateTime, dose: Float, notes: String) {
        viewModelScope.launch {
            _result.value = ApiResult.Loading
            try {
                val request = InsulinRecordRequest(
                    injection_time = injectionTime.format(DateTimeFormatter.ISO_DATE_TIME),
                    actual_dose = dose,
                    notes = notes
                )
                val response = RetrofitClient.apiService.createInsulinRecord(request)
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

