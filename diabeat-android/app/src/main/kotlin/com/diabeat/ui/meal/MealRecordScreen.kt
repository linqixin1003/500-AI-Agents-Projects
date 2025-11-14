package com.diabeat.ui.meal

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.diabeat.viewmodel.MealRecordViewModel
import com.diabeat.R
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealRecordScreen(
    viewModel: MealRecordViewModel,
    onBack: () -> Unit,
    onSave: () -> Unit
) {
    var mealTime by remember { mutableStateOf(LocalDateTime.now()) }
    var notes by remember { mutableStateOf("") }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        TopAppBar(
            title = { Text(stringResource(id = R.string.meal_record_title)) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Text(stringResource(id = R.string.back_button))
                }
            }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 用餐时间
        OutlinedTextField(
            value = mealTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
            onValueChange = { },
            label = { Text(stringResource(id = R.string.meal_time)) },
            modifier = Modifier.fillMaxWidth(),
            readOnly = true
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 备注
        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text(stringResource(id = R.string.notes)) },
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            maxLines = 4
        )
        
        Spacer(modifier = Modifier.weight(1f))
        
        // 保存按钮
        Button(
            onClick = {
                viewModel.saveMealRecord(mealTime, notes)
                onSave()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text(stringResource(id = R.string.save_button))
        }
    }
}

