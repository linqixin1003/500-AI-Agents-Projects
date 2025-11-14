package com.diabeat.ui.insulin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.diabeat.viewmodel.InsulinRecordViewModel
import com.diabeat.R
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsulinRecordScreen(
    viewModel: InsulinRecordViewModel,
    onBack: () -> Unit,
    onSave: () -> Unit
) {
    var injectionTime by remember { mutableStateOf(LocalDateTime.now()) }
    var dose by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        TopAppBar(
            title = { Text(stringResource(id = R.string.insulin_record_title)) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Text(stringResource(id = R.string.back_button))
                }
            }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 注射时间
        OutlinedTextField(
            value = injectionTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
            onValueChange = { },
            label = { Text(stringResource(id = R.string.injection_time)) },
            modifier = Modifier.fillMaxWidth(),
            readOnly = true
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 剂量
        OutlinedTextField(
            value = dose,
            onValueChange = { if (it.all { char -> char.isDigit() || char == '.' }) dose = it },
            label = { Text(stringResource(id = R.string.insulin_dose)) },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal
            )
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
                dose.toFloatOrNull()?.let {
                    viewModel.saveInsulinRecord(injectionTime, it, notes)
                    onSave()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = dose.isNotBlank() && dose.toFloatOrNull() != null
        ) {
            Text(stringResource(id = R.string.save_button))
        }
    }
}

