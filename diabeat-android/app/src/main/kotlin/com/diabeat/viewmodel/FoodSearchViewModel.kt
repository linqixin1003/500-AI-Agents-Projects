package com.diabeat.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.diabeat.data.model.FoodItem
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

class FoodSearchViewModel : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _searchResults = MutableStateFlow<List<FoodItem>>(emptyList())
    val searchResults: StateFlow<List<FoodItem>> = _searchResults

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init {
        @OptIn(FlowPreview::class)
        viewModelScope.launch {
            _searchQuery
                .debounce(300L) // Debounce search query to avoid too many API calls
                .filter { it.isNotBlank() || it.isEmpty() }
                .distinctUntilChanged()
                .collect { query ->
                    if (query.isNotBlank()) {
                        performSearch(query)
                    } else {
                        _searchResults.value = emptyList()
                    }
                }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun clearSearchQuery() {
        _searchQuery.value = ""
        _searchResults.value = emptyList()
    }

    private fun performSearch(query: String) {
        _isLoading.value = true
        _error.value = null
        viewModelScope.launch {
            // TODO: Implement actual API call to search food items
            // For now, use mock data
            kotlinx.coroutines.delay(500) // Simulate network delay
            val mockData = listOf(
                FoodItem(name = "苹果", calories = 52f, weight = 100f),
                FoodItem(name = "香蕉", calories = 89f, weight = 100f),
                FoodItem(name = "橙子", calories = 47f, weight = 100f),
                FoodItem(name = "牛奶", calories = 61f, weight = 100f),
                FoodItem(name = "面包", calories = 265f, weight = 100f),
                FoodItem(name = "米饭", calories = 130f, weight = 100f),
                FoodItem(name = "鸡胸肉", calories = 165f, weight = 100f),
                FoodItem(name = "牛肉", calories = 250f, weight = 100f),
                FoodItem(name = "鸡蛋", calories = 155f, weight = 100f),
                FoodItem(name = "豆腐", calories = 76f, weight = 100f)
            )
            val filteredResults = mockData.filter { it.name.contains(query, ignoreCase = true) }
            _searchResults.value = filteredResults
            _isLoading.value = false
        }
    }
}
