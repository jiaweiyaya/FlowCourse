package com.jiaweiya.flowcourse_test1

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTimetableScreen(
    timetableId: Int,
    timetables: List<TimetableData>,
    timeProfiles: List<TimeProfile>,
    onSaveTimetable: (TimetableData) -> Unit,
    onNavigateToEditProfile: (Int) -> Unit,
    onDeleteProfile: (Int) -> Unit,
    onBackClick: () -> Unit
) {
    val currentTimetable = timetables.find { it.id == timetableId }
    if (currentTimetable == null) {
        onBackClick()
        return
    }

    var name by remember { mutableStateOf(currentTimetable.name) }
    var selectedProfileId by remember { mutableStateOf(currentTimetable.timeProfileId) }
    var totalWeeksStr by remember { mutableStateOf(currentTimetable.totalWeeks.toString()) }
    var showProfileDropdown by remember { mutableStateOf(false) }

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0.dp),
                title = { Text("课表属性", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    TextButton(onClick = {
                        val updated = currentTimetable.copy(
                            name = name.ifBlank { "未命名课表" },
                            timeProfileId = selectedProfileId,
                            totalWeeks = totalWeeksStr.toIntOrNull()?.coerceIn(1, 50) ?: 20
                        )
                        onSaveTimetable(updated)
                        onBackClick()
                    }) {
                        Text("保存", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    ) { paddingValues ->
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("课表名称") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = totalWeeksStr,
                onValueChange = { totalWeeksStr = it },
                label = { Text("总计周数") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // 开学日期展示（由修改当前周计算得出）
            Column {
                Text("开学日期 (第一周周一)", fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = currentTimetable.termStart ?: "尚未设置，采用默认计算",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "提示：在课表主界面通过点击“修改当前周”可自动校准开学日期。",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            // 时间配置文件选择
            Column {
                Text("上课时间配置", fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))

                Box {
                    val selectedProfileName = timeProfiles.find { it.id == selectedProfileId }?.name ?: "默认配置"
                    OutlinedTextField(
                        value = selectedProfileName,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showProfileDropdown = true },
                        enabled = false,
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )

                    DropdownMenu(
                        expanded = showProfileDropdown,
                        onDismissRequest = { showProfileDropdown = false },
                        modifier = Modifier.fillMaxWidth(0.9f)
                    ) {
                        timeProfiles.forEach { profile ->
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(profile.name)
                                        IconButton(
                                            onClick = {
                                                onDeleteProfile(profile.id)
                                                if (selectedProfileId == profile.id && timeProfiles.size > 1) {
                                                    selectedProfileId = timeProfiles.first { it.id != profile.id }.id
                                                }
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "删除配置",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    selectedProfileId = profile.id
                                    showProfileDropdown = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { onNavigateToEditProfile(-1) }) {
                        Text("新建配置")
                    }
                    Button(onClick = { onNavigateToEditProfile(selectedProfileId) }) {
                        Text("编辑选中的配置")
                    }
                }
            }
        }
    }
}