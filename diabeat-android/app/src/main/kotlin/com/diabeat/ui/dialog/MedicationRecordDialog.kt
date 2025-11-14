package com.diabeat.ui.dialog

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.diabeat.data.model.MedicationRecordRequest
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicationRecordDialog(
    onDismiss: () -> Unit,
    onConfirm: (MedicationRecordRequest) -> Unit
) {
    var medicationType by remember { mutableStateOf("insulin") }
    var medicationName by remember { mutableStateOf("") }
    var dosage by remember { mutableStateOf("") }
    var dosageUnit by remember { mutableStateOf("ml") }
    var notes by remember { mutableStateOf("") }
    var typeExpanded by remember { mutableStateOf(false) }
    var unitExpanded by remember { mutableStateOf(false) }
    
    val medicationTypes = mapOf(
        "insulin" to "胰岛素",
        "oral_medication" to "口服药物",
        "other" to "其他"
    )
    
    val dosageUnits = mapOf(
        "mg" to "mg",
        "ml" to "ml",
        "tablets" to "片"
    )
    
    // 常见胰岛素类型
    val commonInsulinTypes = listOf(
        "速效胰岛素",
        "短效胰岛素",
        "中效胰岛素",
        "长效胰岛素",
        "预混胰岛素"
    )
    
    // 常见口服药物
    val commonOralMedications = listOf(
        "二甲双胍",
        "格列美脲",
        "阿卡波糖",
        "西格列汀",
        "达格列净"
    )
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // 标题栏
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "记录用药",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "关闭"
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // 用药类型
                ExposedDropdownMenuBox(
                    expanded = typeExpanded,
                    onExpandedChange = { typeExpanded = !typeExpanded }
                ) {
                    OutlinedTextField(
                        value = medicationTypes[medicationType] ?: "胰岛素",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("用药类型") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = typeExpanded,
                        onDismissRequest = { typeExpanded = false }
                    ) {
                        medicationTypes.forEach { (key, value) ->
                            DropdownMenuItem(
                                text = { Text(value) },
                                onClick = {
                                    medicationType = key
                                    typeExpanded = false
                                    // 自动调整默认单位
                                    dosageUnit = when (key) {
                                        "insulin" -> "ml"
                                        "oral_medication" -> "tablets"
                                        else -> "mg"
                                    }
                                }
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 常用药物快捷选择
                if (medicationType == "insulin" || medicationType == "oral_medication") {
                    Text(
                        text = "常用药物",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    val suggestions = if (medicationType == "insulin") commonInsulinTypes else commonOralMedications
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        suggestions.forEach { suggestion ->
                            FilterChip(
                                selected = medicationName == suggestion,
                                onClick = { medicationName = suggestion },
                                label = { Text(suggestion) }
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                }
                
                // 药物名称
                OutlinedTextField(
                    value = medicationName,
                    onValueChange = { medicationName = it },
                    label = { Text("药物名称") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 剂量和单位（并排显示）
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = dosage,
                        onValueChange = { dosage = it },
                        label = { Text("剂量") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    
                    ExposedDropdownMenuBox(
                        expanded = unitExpanded,
                        onExpandedChange = { unitExpanded = !unitExpanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = dosageUnits[dosageUnit] ?: "单位",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("单位") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = unitExpanded,
                            onDismissRequest = { unitExpanded = false }
                        ) {
                            dosageUnits.forEach { (key, value) ->
                                DropdownMenuItem(
                                    text = { Text(value) },
                                    onClick = {
                                        dosageUnit = key
                                        unitExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 备注
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("备注（可选）") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    maxLines = 3,
                    placeholder = { Text("例如：餐前/餐后，注射部位等") }
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // 按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("取消")
                    }
                    Button(
                        onClick = {
                            val dose = dosage.toFloatOrNull()
                            if (dose != null && dose > 0 && medicationName.isNotBlank()) {
                                val request = MedicationRecordRequest(
                                    medication_time = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME),
                                    medication_type = medicationType,
                                    medication_name = medicationName,
                                    dosage = dose,
                                    dosage_unit = dosageUnit,
                                    notes = notes.ifBlank { null }
                                )
                                onConfirm(request)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = dosage.toFloatOrNull() != null && 
                                  dosage.toFloatOrNull()!! > 0 && 
                                  medicationName.isNotBlank()
                    ) {
                        Text("保存")
                    }
                }
            }
        }
    }
}

