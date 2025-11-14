package com.diabeat.ui.foodsearch

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.res.stringResource
import com.diabeat.data.model.FoodItem
import com.diabeat.viewmodel.FoodSearchViewModel
import com.diabeat.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodSearchScreen(
    viewModel: FoodSearchViewModel = viewModel(),
    onBack: () -> Unit,
    onFoodAdded: (FoodItem) -> Unit
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.food_search_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(id = R.string.back_button))
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(id = R.string.food_search_hint)) },
                singleLine = true,
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = stringResource(id = R.string.search_button)) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.clearSearchQuery() }) {
                            Icon(Icons.Filled.Clear, contentDescription = stringResource(id = R.string.clear_button))
                        }
                    }
                }
            )

            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(48.dp))
            } else if (error != null) {
                Text(stringResource(id = R.string.search_failed, error ?: ""), color = MaterialTheme.colorScheme.error)
            } else if (searchResults.isEmpty() && searchQuery.isNotEmpty()) {
                Text(stringResource(id = R.string.no_food_found))
            } else if (searchResults.isNotEmpty()) {
                LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    items(searchResults) { foodItem ->
                        FoodSearchResultItem(foodItem = foodItem, onAddClick = onFoodAdded)
                    }
                }
            }
        }
    }
}

@Composable
fun FoodSearchResultItem(foodItem: FoodItem, onAddClick: (FoodItem) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onAddClick(foodItem) },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = foodItem.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = stringResource(id = R.string.calories_format, foodItem.calories?.toInt() ?: 0), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = { onAddClick(foodItem) }) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(id = R.string.add_food), tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
