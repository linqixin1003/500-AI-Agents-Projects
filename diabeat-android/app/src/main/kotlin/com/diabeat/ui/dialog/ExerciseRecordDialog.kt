package com.diabeat.ui.dialog

import androidx.compose.foundation.layout.*
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
import com.diabeat.data.model.ExerciseRecordRequest
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseRecordDialog(
    onDismiss: () -> Unit,
    onConfirm: (ExerciseRecordRequest) -> Unit
) {
    var exerciseType by remember { mutableStateOf("walking") }
    var durationMinutes by remember { mutableStateOf("") }
    var intensity by remember { mutableStateOf("moderate") }
    var notes by remember { mutableStateOf("") }
    var isExpanded by remember { mutableStateOf(false) }
    var intensityExpanded by remember { mutableStateOf(false) }
    
    val exerciseTypes = mapOf(
        "walking" to "步行",
        "running" to "跑步",
        "cycling" to "骑行",
        "swimming" to "游泳",
        "gym" to "健身房",
        "yoga" to "瑜伽",
        "dancing" to "跳舞",
        "other" to "其他"
    )
    
    val intensityLevels = mapOf(
        "light" to "轻度",
        "moderate" to "中度",
        "vigorous" to "高强度"
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
                        text = "记录运动",
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
                
                // 运动类型选择
                ExposedDropdownMenuBox(
                    expanded = isExpanded,
                    onExpandedChange = { isExpanded = !isExpanded }
                ) {
                    OutlinedTextField(
                        value = exerciseTypes[exerciseType] ?: "步行",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("运动类型") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = isExpanded,
                        onDismissRequest = { isExpanded = false }
                    ) {
                        exerciseTypes.forEach { (key, value) ->
                            DropdownMenuItem(
                                text = { Text(value) },
                                onClick = {
                                    exerciseType = key
                                    isExpanded = false
                                }
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 运动时长
                OutlinedTextField(
                    value = durationMinutes,
                    onValueChange = { durationMinutes = it },
                    label = { Text("运动时长（分钟）") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 运动强度
                ExposedDropdownMenuBox(
                    expanded = intensityExpanded,
                    onExpandedChange = { intensityExpanded = !intensityExpanded }
                ) {
                    OutlinedTextField(
                        value = intensityLevels[intensity] ?: "中度",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("运动强度") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = intensityExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = intensityExpanded,
                        onDismissRequest = { intensityExpanded = false }
                    ) {
                        intensityLevels.forEach { (key, value) ->
                            DropdownMenuItem(
                                text = { Text(value) },
                                onClick = {
                                    intensity = key
                                    intensityExpanded = false
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
                        .height(100.dp),
                    maxLines = 3
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
                            val duration = durationMinutes.toIntOrNull()
                            if (duration != null && duration > 0) {
                                val request = ExerciseRecordRequest(
                                    exercise_time = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME),
                                    exercise_type = exerciseType,
                                    duration_minutes = duration,
                                    intensity = intensity,
                                    notes = notes.ifBlank { null }
                                )
                                onConfirm(request)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = durationMinutes.toIntOrNull() != null && durationMinutes.toIntOrNull()!! > 0
                    ) {
                        Text("保存")
                    }
                }
            }
        }
    }
}

