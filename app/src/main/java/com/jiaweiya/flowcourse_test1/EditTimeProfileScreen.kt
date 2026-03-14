package com.jiaweiya.flowcourse_test1

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTimeProfileScreen(
    profileId: Int,
    timeProfiles: List<TimeProfile>,
    onSave: (TimeProfile) -> Unit,
    onBackClick: () -> Unit
) {
    val defaultNodes = nodeTimes
    val currentProfile = timeProfiles.find { it.id == profileId }

    var profileName by remember { mutableStateOf(currentProfile?.name ?: "未命名配置") }
    var editedNodes by remember {
        mutableStateOf(currentProfile?.nodes?.map { it.copy() } ?: defaultNodes.map { it.copy() })
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0.dp),
                title = { Text("编辑上课时间", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    TextButton(onClick = {
                        val newProfile = TimeProfile(
                            id = if (profileId == -1) (timeProfiles.maxOfOrNull { it.id } ?: 0) + 1 else profileId,
                            name = profileName.ifBlank { "未命名配置" },
                            nodes = editedNodes
                        )
                        onSave(newProfile)
                    }) {
                        Text("保存", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            OutlinedTextField(
                value = profileName,
                onValueChange = { profileName = it },
                label = { Text("配置文件名称") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider()

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                itemsIndexed(editedNodes) { index, node ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 左侧：节次名称和开关
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.width(55.dp)
                        ) {
                            Text(
                                text = "第 ${node.label} 节",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Visible,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            Switch(
                                checked = node.isVisible,
                                onCheckedChange = { isChecked ->
                                    editedNodes = editedNodes.toMutableList().apply {
                                        this[index] = node.copy(isVisible = isChecked)
                                    }
                                },
                                modifier = Modifier.scale(0.7f).height(24.dp)
                            )
                        }

                        OutlinedTextField(
                            value = node.start,
                            onValueChange = { newVal ->
                                editedNodes = editedNodes.toMutableList().apply { this[index] = node.copy(start = newVal) }
                            },
                            label = { Text("开始时间") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii)
                        )

                        OutlinedTextField(
                            value = node.end,
                            onValueChange = { newVal ->
                                editedNodes = editedNodes.toMutableList().apply { this[index] = node.copy(end = newVal) }
                            },
                            label = { Text("结束时间") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii)
                        )
                    }
                }
            }
        }
    }
}