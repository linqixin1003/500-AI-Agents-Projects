package com.diabeat.ui.dialog

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import com.diabeat.data.model.WaterRecordRequest
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WaterRecordDialog(
    onDismiss: () -> Unit,
    onConfirm: (WaterRecordRequest) -> Unit
) {
    var amountMl by remember { mutableStateOf("") }
    var waterType by remember { mutableStateOf("water") }
    var notes by remember { mutableStateOf("") }
    var isExpanded by remember { mutableStateOf(false) }
    
    val waterTypes = mapOf(
        "water" to "白开水",
        "tea" to "茶",
        "coffee" to "咖啡",
        "juice" to "果汁",
        "other" to "其他"
    )
    
    // 快捷量选择
    val quickAmounts = listOf(200, 300, 500, 800)
    
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
            ) {
                // 标题栏
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "记录饮水",
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
                
                // 快捷量选择
                Text(
                    text = "快捷选择",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    quickAmounts.forEach { amount ->
                        FilterChip(
                            selected = amountMl == amount.toString(),
                            onClick = { amountMl = amount.toString() },
                            label = { Text("${amount}ml") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 自定义输入
                OutlinedTextField(
                    value = amountMl,
                    onValueChange = { amountMl = it },
                    label = { Text("饮水量（毫升）") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    supportingText = { Text("单次最多2000ml") }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 饮水类型
                ExposedDropdownMenuBox(
                    expanded = isExpanded,
                    onExpandedChange = { isExpanded = !isExpanded }
                ) {
                    OutlinedTextField(
                        value = waterTypes[waterType] ?: "白开水",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("饮水类型") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = isExpanded,
                        onDismissRequest = { isExpanded = false }
                    ) {
                        waterTypes.forEach { (key, value) ->
                            DropdownMenuItem(
                                text = { Text(value) },
                                onClick = {
                                    waterType = key
                                    isExpanded = false
                                }
                            )
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
                        .height(80.dp),
                    maxLines = 2
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
                            val amount = amountMl.toIntOrNull()
                            if (amount != null && amount > 0 && amount <= 2000) {
                                val request = WaterRecordRequest(
                                    record_time = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME),
                                    amount_ml = amount,
                                    water_type = waterType,
                                    notes = notes.ifBlank { null }
                                )
                                onConfirm(request)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = amountMl.toIntOrNull()?.let { it in 1..2000 } == true
                    ) {
                        Text("保存")
                    }
                }
            }
        }
    }
}

