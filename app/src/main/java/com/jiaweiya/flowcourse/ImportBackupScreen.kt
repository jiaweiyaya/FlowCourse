package com.jiaweiya.flowcourse

import android.annotation.SuppressLint
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class ImportMode {
    ADD,     // 强制追加为新项
    REPLACE  // 同名覆盖已有项
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportBackupScreen(
    backupData: BackupData?,
    currentTimetables: List<TimetableData>,
    currentTimeProfiles: List<TimeProfile>,
    onImportSuccess: (List<TimetableData>, List<TimeProfile>) -> Unit,
    onBackClick: () -> Unit
) {
    if (backupData == null) {
        onBackClick()
        return
    }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val sharedPrefs = remember { context.getSharedPreferences("FlowCourseDB", Context.MODE_PRIVATE) }

    // 准备数据源
    val settingsInBackup = remember(backupData) { backupData.settings?.keys?.toList() ?: emptyList() }
    val timetablesInBackup = remember(backupData) { backupData.timetables ?: emptyList() }
    val profilesInBackup = remember(backupData) { backupData.timeProfiles ?: emptyList() }

    // 状态配置
    val selectedSettingsMap = remember { mutableStateMapOf<String, Boolean>().apply { settingsInBackup.forEach { put(it, true) } } }
    val selectedTimetablesMap = remember { mutableStateMapOf<Int, Boolean>().apply { timetablesInBackup.forEach { put(it.id, true) } } }
    val selectedProfilesMap = remember { mutableStateMapOf<Int, Boolean>().apply { profilesInBackup.forEach { put(it.id, true) } } }

    val timetableImportModeMap = remember(backupData, currentTimetables) {
        mutableStateMapOf<Int, ImportMode>().apply {
            timetablesInBackup.forEach { backupTb ->
                val hasSameName = currentTimetables.any { it.name == backupTb.name }
                put(backupTb.id, if (hasSameName) ImportMode.ADD else ImportMode.REPLACE)
            }
        }
    }

    val profileImportModeMap = remember(backupData, currentTimeProfiles) {
        mutableStateMapOf<Int, ImportMode>().apply {
            profilesInBackup.forEach { backupProfile ->
                val hasSameName = currentTimeProfiles.any { it.name == backupProfile.name }
                put(backupProfile.id, if (hasSameName) ImportMode.ADD else ImportMode.REPLACE)
            }
        }
    }

    var isSettingsExpanded by remember { mutableStateOf(false) }
    var isTimetablesExpanded by remember { mutableStateOf(false) }
    var isProfilesExpanded by remember { mutableStateOf(false) }

    // 动态计算三态复选状态
    val checkedSettingsCount = settingsInBackup.count { selectedSettingsMap[it] == true }
    val settingsToggleState = when {
        checkedSettingsCount == settingsInBackup.size -> ToggleableState.On
        checkedSettingsCount == 0 -> ToggleableState.Off
        else -> ToggleableState.Indeterminate
    }

    val checkedTimetablesCount = timetablesInBackup.count { selectedTimetablesMap[it.id] == true }
    val timetablesToggleState = when {
        checkedTimetablesCount == timetablesInBackup.size -> ToggleableState.On
        checkedTimetablesCount == 0 -> ToggleableState.Off
        else -> ToggleableState.Indeterminate
    }

    val checkedProfilesCount = profilesInBackup.count { selectedProfilesMap[it.id] == true }
    val profilesToggleState = when {
        checkedProfilesCount == profilesInBackup.size -> ToggleableState.On
        checkedProfilesCount == 0 -> ToggleableState.Off
        else -> ToggleableState.Indeterminate
    }

    // 导入核心实现
    fun executeImport() {
        coroutineScope.launch {
            withContext(Dispatchers.IO) {
                val editor = sharedPrefs.edit()

                // 1. 系统设置导入
                if (checkedSettingsCount > 0) {
                    backupData.settings?.forEach { (category, map) ->
                        if (selectedSettingsMap[category] == true) {
                            map.forEach { (key, value) ->
                                when (value) {
                                    is Boolean -> editor.putBoolean(key, value)
                                    is Float -> editor.putFloat(key, value)
                                    is Int -> editor.putInt(key, value)
                                    is Long -> editor.putLong(key, value)
                                    is String -> editor.putString(key, value)
                                    is Double -> {
                                        if (key == "bg_opacity") {
                                            editor.putFloat(key, value.toFloat())
                                        } else if (key == "conflict_color" || key == "course_border_color") {
                                            editor.putLong(key, value.toLong())
                                        } else {
                                            editor.putInt(key, value.toInt())
                                        }
                                    }
                                    is List<*> -> {
                                        if (key == "preferred_conflict_ids") {
                                            editor.putStringSet(key, value.mapNotNull { it?.toString() }.toSet())
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                editor.apply()

                // 2. 时间配置导入
                var masterProfiles = currentTimeProfiles.toMutableList()
                profilesInBackup.forEach { backupProfile ->
                    if (selectedProfilesMap[backupProfile.id] == true) {
                        val mode = profileImportModeMap[backupProfile.id] ?: ImportMode.REPLACE
                        val existIndex = masterProfiles.indexOfFirst { it.name == backupProfile.name }

                        if (mode == ImportMode.REPLACE && existIndex != -1) {
                            val oldId = masterProfiles[existIndex].id
                            masterProfiles[existIndex] = backupProfile.copy(id = oldId)
                        } else {
                            val newId = (masterProfiles.maxOfOrNull { it.id } ?: 0) + 1
                            val existingNames = masterProfiles.map { it.name }
                            val uniqueName = generateUniqueName(backupProfile.name, existingNames)
                            masterProfiles.add(backupProfile.copy(id = newId, name = uniqueName))
                        }
                    }
                }

                // 3. 课表数据导入
                var masterTimetables = currentTimetables.toMutableList()
                timetablesInBackup.forEach { backupTimetable ->
                    if (selectedTimetablesMap[backupTimetable.id] == true) {
                        val mode = timetableImportModeMap[backupTimetable.id] ?: ImportMode.REPLACE
                        val existIndex = masterTimetables.indexOfFirst { it.name == backupTimetable.name }

                        if (mode == ImportMode.REPLACE && existIndex != -1) {
                            val oldId = masterTimetables[existIndex].id
                            masterTimetables[existIndex] = backupTimetable.copy(id = oldId)
                        } else {
                            val newId = (masterTimetables.maxOfOrNull { it.id } ?: 0) + 1
                            val existingNames = masterTimetables.map { it.name }
                            val uniqueName = generateUniqueName(backupTimetable.name, existingNames)
                            masterTimetables.add(backupTimetable.copy(id = newId, name = uniqueName))
                        }
                    }
                }

                withContext(Dispatchers.Main) {
                    onImportSuccess(masterTimetables, masterProfiles)
                    Toast.makeText(context, "导入成功，部分全局配置已应用", Toast.LENGTH_SHORT).show()
                    onBackClick()
                }
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0.dp),
                title = { Text("确认恢复备份", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    TextButton(onClick = { executeImport() }) {
                        Text("确认导入", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(16.dp)
        ) {
            // 1. 系统设置导入项
            if (settingsInBackup.isNotEmpty()) {
                item {
                    ExpandableGroupHeader(
                        title = "系统配置选项",
                        isExpanded = isSettingsExpanded,
                        onExpandClick = { isSettingsExpanded = !isSettingsExpanded },
                        toggleState = settingsToggleState,
                        onToggleClick = {
                            val nextChecked = settingsToggleState != ToggleableState.On
                            settingsInBackup.forEach { selectedSettingsMap[it] = nextChecked }
                        }
                    )

                    AnimatedVisibility(
                        visible = isSettingsExpanded,
                        enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
                        exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp)
                        ) {
                            settingsInBackup.forEach { category ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            val next = !(selectedSettingsMap[category] ?: false)
                                            selectedSettingsMap[category] = next
                                        }
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(category, fontSize = 14.sp)
                                    Checkbox(
                                        checked = selectedSettingsMap[category] ?: false,
                                        onCheckedChange = { checked ->
                                            selectedSettingsMap[category] = checked
                                        }
                                    )
                                }
                            }
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                }
            }

            // 2. 课表数据导入项
            if (timetablesInBackup.isNotEmpty()) {
                item {
                    ExpandableGroupHeader(
                        title = "课表数据 (${timetablesInBackup.size} 个)",
                        isExpanded = isTimetablesExpanded,
                        onExpandClick = { isTimetablesExpanded = !isTimetablesExpanded },
                        toggleState = timetablesToggleState,
                        onToggleClick = {
                            val nextChecked = timetablesToggleState != ToggleableState.On
                            timetablesInBackup.forEach { selectedTimetablesMap[it.id] = nextChecked }
                        }
                    )

                    AnimatedVisibility(
                        visible = isTimetablesExpanded,
                        enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
                        exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            timetablesInBackup.forEach { timetable ->
                                val isSelected = selectedTimetablesMap[timetable.id] ?: false
                                val currentMode = timetableImportModeMap[timetable.id] ?: ImportMode.REPLACE

                                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                val next = !isSelected
                                                selectedTimetablesMap[timetable.id] = next
                                            },
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(timetable.name, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                        Checkbox(
                                            checked = isSelected,
                                            onCheckedChange = { checked ->
                                                selectedTimetablesMap[timetable.id] = checked
                                            }
                                        )
                                    }

                                    if (isSelected) {
                                        Row(
                                            modifier = Modifier.padding(start = 12.dp, top = 4.dp),
                                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                RadioButton(
                                                    selected = currentMode == ImportMode.REPLACE,
                                                    onClick = { timetableImportModeMap[timetable.id] = ImportMode.REPLACE }
                                                )
                                                Text("同名替换", fontSize = 12.sp)
                                            }
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                RadioButton(
                                                    selected = currentMode == ImportMode.ADD,
                                                    onClick = { timetableImportModeMap[timetable.id] = ImportMode.ADD }
                                                )
                                                Text("追加为新", fontSize = 12.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                }
            }

            // 3. 时间配置导入项
            if (profilesInBackup.isNotEmpty()) {
                item {
                    ExpandableGroupHeader(
                        title = "上课时间配置 (${profilesInBackup.size} 个)",
                        isExpanded = isProfilesExpanded,
                        onExpandClick = { isProfilesExpanded = !isProfilesExpanded },
                        toggleState = profilesToggleState,
                        onToggleClick = {
                            val nextChecked = profilesToggleState != ToggleableState.On
                            profilesInBackup.forEach { selectedProfilesMap[it.id] = nextChecked }
                        }
                    )

                    AnimatedVisibility(
                        visible = isProfilesExpanded,
                        enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
                        exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            profilesInBackup.forEach { profile ->
                                val isSelected = selectedProfilesMap[profile.id] ?: false
                                val currentMode = profileImportModeMap[profile.id] ?: ImportMode.REPLACE

                                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                val next = !isSelected
                                                selectedProfilesMap[profile.id] = next
                                            },
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(profile.name, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                        Checkbox(
                                            checked = isSelected,
                                            onCheckedChange = { checked ->
                                                selectedProfilesMap[profile.id] = checked
                                            }
                                        )
                                    }

                                    if (isSelected) {
                                        Row(
                                            modifier = Modifier.padding(start = 12.dp, top = 4.dp),
                                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                RadioButton(
                                                    selected = currentMode == ImportMode.REPLACE,
                                                    onClick = { profileImportModeMap[profile.id] = ImportMode.REPLACE }
                                                )
                                                Text("同名替换", fontSize = 12.sp)
                                            }
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                RadioButton(
                                                    selected = currentMode == ImportMode.ADD,
                                                    onClick = { profileImportModeMap[profile.id] = ImportMode.ADD }
                                                )
                                                Text("追加为新", fontSize = 12.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                }
            }
        }
    }
}

@Composable
private fun ExpandableGroupHeader(
    title: String,
    isExpanded: Boolean,
    onExpandClick: () -> Unit,
    toggleState: ToggleableState,
    onToggleClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onExpandClick() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )

        Icon(
            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.width(12.dp))

        Box(
            modifier = Modifier
                .width(1.dp)
                .height(20.dp)
                .background(MaterialTheme.colorScheme.outlineVariant)
        )

        Spacer(modifier = Modifier.width(12.dp))

        TriStateCheckbox(
            state = toggleState,
            onClick = onToggleClick
        )
    }
}

// 根据已有名字列表，计算出不冲突的唯一新名称
private fun generateUniqueName(baseName: String, existingNames: List<String>): String {
    if (!existingNames.contains(baseName)) {
        return baseName
    }
    var count = 1
    while (true) {
        val suffix = if (count == 1) " (new)" else " (new$count)"
        val candidateName = "$baseName$suffix"
        if (!existingNames.contains(candidateName)) {
            return candidateName
        }
        count++
    }
}