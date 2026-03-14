package com.jiaweiya.flowcourse_test1

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

data class TimeSlotState(
    var weeks: List<Int> = (1..16).toList(),
    var dayOfWeek: Int = 1,
    var startNode: Int = 1,
    var endNode: Int = 2,
    var teacher: String = "",
    var room: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCourseScreen(
    courseId: Int,
    timetables: List<TimetableData>,
    activeTimetableId: Int,
    onSave: (List<TimetableData>) -> Unit,
    onBackClick: () -> Unit
) {
    val isEditMode = courseId != -1
    val activeTimetable = timetables.find { it.id == activeTimetableId } ?: timetables.first()
    val actualTimetableId = activeTimetable.id
    val editingCourse = if (isEditMode) activeTimetable.courses.find { it.id == courseId } else null

    val relatedCourses = if (isEditMode && editingCourse != null) {
        activeTimetable.courses.filter { it.name == editingCourse.name }
    } else emptyList()

    var courseName by remember { mutableStateOf(editingCourse?.name ?: "") }
    var credits by remember { mutableStateOf(editingCourse?.credits ?: "") }
    var notes by remember { mutableStateOf(editingCourse?.notes ?: "") }

    var showColorDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    var selectedBgColor by remember { mutableStateOf(Color(editingCourse?.bgColor ?: 0xFFE8EAF6)) }
    var selectedTextColor by remember { mutableStateOf(Color(editingCourse?.textColor ?: 0xFF000000)) }

    val timeSlots = remember {
        mutableStateListOf(
            *if (isEditMode && relatedCourses.isNotEmpty()) {
                relatedCourses.map {
                    TimeSlotState(it.weekList, it.dayOfWeek, it.startNode, it.endNode, it.teacher, it.room)
                }.toTypedArray()
            } else {
                arrayOf(TimeSlotState())
            }
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0.dp),
                title = { Text(if (isEditMode) "修改课程" else "添加课程", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = { IconButton(onClick = onBackClick) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回") } },
                actions = {
                    TextButton(onClick = {
                        val newCourses = activeTimetable.courses.toMutableList()
                        if (isEditMode && editingCourse != null) {
                            newCourses.removeAll { it.name == editingCourse.name }
                        }

                        val maxId = (timetables.flatMap { it.courses }.maxOfOrNull { it.id } ?: 0)
                        timeSlots.forEachIndexed { index, slot ->
                            newCourses.add(
                                Course(
                                    id = maxId + index + 1,
                                    name = courseName.ifEmpty { "未命名课程" },
                                    room = slot.room, teacher = slot.teacher,
                                    dayOfWeek = slot.dayOfWeek, startNode = slot.startNode, endNode = slot.endNode, weekList = slot.weeks,
                                    bgColor = (selectedBgColor.toArgb().toLong() and 0xFFFFFFFFL),
                                    textColor = (selectedTextColor.toArgb().toLong() and 0xFFFFFFFFL),
                                    credits = credits, notes = notes
                                )
                            )
                        }

                        val newTimetables = timetables.map {
                            if (it.id == actualTimetableId) it.copy(courses = newCourses) else it
                        }
                        onSave(newTimetables)
                    }) {
                        Text("保存", fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                if (isEditMode) {
                    FloatingActionButton(
                        onClick = { showDeleteConfirm = true },
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "删除", tint = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
                FloatingActionButton(onClick = { timeSlots.add(TimeSlotState()) }, containerColor = MaterialTheme.colorScheme.primaryContainer) {
                    Icon(Icons.Default.Add, contentDescription = "添加时间段", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).verticalScroll(rememberScrollState())
        ) {
            EditableCourseRow(icon = "📖", placeholder = "课程名称", value = courseName, onValueChange = { courseName = it })

            Row(
                modifier = Modifier.fillMaxWidth().clickable { showColorDialog = true }.padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "🖍️", fontSize = 20.sp)
                Spacer(modifier = Modifier.width(20.dp))
                Text(text = "点此更改颜色", fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.weight(1f))
                Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(selectedBgColor).border(1.dp, MaterialTheme.colorScheme.outline, CircleShape))
            }

            EditableCourseRow(icon = "🚩", placeholder = "学分 (可不填)", value = credits, onValueChange = { credits = it })
            EditableCourseRow(icon = "📝", placeholder = "备注 (可不填)", value = notes, onValueChange = { notes = it })

            Spacer(modifier = Modifier.height(8.dp))
            Box(modifier = Modifier.fillMaxWidth().height(8.dp).background(MaterialTheme.colorScheme.surfaceVariant))
            Spacer(modifier = Modifier.height(8.dp))

            timeSlots.forEachIndexed { index, slotState ->
                TimeSlotBlock(
                    index = index,
                    state = slotState,
                    onStateChange = { newState -> timeSlots[index] = newState },
                    onClose = { if (timeSlots.size > 1) timeSlots.removeAt(index) },
                    showClose = timeSlots.size > 1
                )
                if (index < timeSlots.size - 1) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), modifier = Modifier.padding(horizontal = 16.dp))
                }
            }
            Spacer(modifier = Modifier.height(100.dp))
        }

        if (showColorDialog) {
            ColorPickerDialog(
                initialBgColor = selectedBgColor, initialTextColor = selectedTextColor,
                onDismiss = { showColorDialog = false },
                onColorSelected = { bg, text -> selectedBgColor = bg; selectedTextColor = text; showColorDialog = false }
            )
        }

        if (showDeleteConfirm) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                title = { Text("确认删除", fontWeight = FontWeight.Bold) },
                text = { Text("您确定要删除这门课程吗？此操作无法撤销。") },
                confirmButton = {
                    TextButton(onClick = {
                        showDeleteConfirm = false
                        val newCourses = activeTimetable.courses.filter { it.name != editingCourse?.name }
                        val newTimetables = timetables.map { if (it.id == actualTimetableId) it.copy(courses = newCourses) else it }
                        onSave(newTimetables)
                    }) {
                        Text("删除", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirm = false }) {
                        Text("取消", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            )
        }
    }
}

@Composable
fun TimeSlotBlock(index: Int, state: TimeSlotState, onStateChange: (TimeSlotState) -> Unit, onClose: () -> Unit, showClose: Boolean) {
    var showWeekDialog by remember { mutableStateOf(false) }
    var showTimeDialog by remember { mutableStateOf(false) }
    val days = listOf("一", "二", "三", "四", "五", "六", "日")

    Column(modifier = Modifier.padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("时间段 ${if(index > 0) index + 1 else ""}", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (showClose) {
                IconButton(onClick = onClose, modifier = Modifier.size(24.dp)) { Icon(Icons.Default.Close, contentDescription = "删除", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth().clickable { showWeekDialog = true }, verticalAlignment = Alignment.CenterVertically) {
            StaticCourseRow(icon = "📅", title = "第 ${state.weeks.minOrNull() ?: 1} - ${state.weeks.maxOrNull() ?: 16} 周  (共${state.weeks.size}周)")
        }

        Row(modifier = Modifier.fillMaxWidth().clickable { showTimeDialog = true }, verticalAlignment = Alignment.CenterVertically) {
            // 安全读取 16 个时间段对应的 label 名称
            val startLabel = nodeTimes.getOrNull(state.startNode - 1)?.label ?: "${state.startNode}"
            val endLabel = nodeTimes.getOrNull(state.endNode - 1)?.label ?: "${state.endNode}"
            StaticCourseRow(icon = "🕒", title = "周${days[state.dayOfWeek-1]}   第 $startLabel - $endLabel 节")
        }

        EditableCourseRow(icon = "👤", placeholder = "授课老师 (可不填)", value = state.teacher, onValueChange = { onStateChange(state.copy(teacher = it)) })
        EditableCourseRow(icon = "🚪", placeholder = "上课地点 (可不填)", value = state.room, onValueChange = { onStateChange(state.copy(room = it)) })
    }

    if (showWeekDialog) {
        WeekSelectionDialog(
            initialWeeks = state.weeks,
            onDismiss = { showWeekDialog = false },
            onConfirm = { newWeeks -> onStateChange(state.copy(weeks = newWeeks)); showWeekDialog = false }
        )
    }

    if (showTimeDialog) {
        TimeSelectionDialog(
            initialDay = state.dayOfWeek, initialStart = state.startNode, initialEnd = state.endNode,
            onDismiss = { showTimeDialog = false },
            onConfirm = { day, start, end -> onStateChange(state.copy(dayOfWeek = day, startNode = start, endNode = end)); showTimeDialog = false }
        )
    }
}

@Composable
fun WeekSelectionDialog(initialWeeks: List<Int>, onDismiss: () -> Unit, onConfirm: (List<Int>) -> Unit) {
    var selectedWeeks by remember { mutableStateOf<Set<Int>>(initialWeeks.toSet()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择上课周数", fontWeight = FontWeight.Bold) },
        text = {
            LazyVerticalGrid(columns = GridCells.Fixed(5), verticalArrangement = Arrangement.spacedBy(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(20) { i ->
                    val week = i + 1
                    val isSelected = selectedWeeks.contains(week)
                    Box(
                        modifier = Modifier.aspectRatio(1f).clip(CircleShape).background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant).clickable {
                            selectedWeeks = if (isSelected) selectedWeeks.minus(week) else selectedWeeks.plus(week)
                        },
                        contentAlignment = Alignment.Center
                    ) { Text("$week", color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(selectedWeeks.toList().sorted()) }) { Text("确定", color = MaterialTheme.colorScheme.primary) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消", color = MaterialTheme.colorScheme.onSurfaceVariant) } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeSelectionDialog(initialDay: Int, initialStart: Int, initialEnd: Int, onDismiss: () -> Unit, onConfirm: (Int, Int, Int) -> Unit) {
    var day by remember { mutableIntStateOf(initialDay) }
    var start by remember { mutableIntStateOf(initialStart) }
    var end by remember { mutableIntStateOf(initialEnd) }
    val days = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择上课时间", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("星期几", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                    items(7) { i ->
                        FilterChip(selected = day == i + 1, onClick = { day = i + 1 }, label = { Text(days[i]) })
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Text("开始节数", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        var expandedStart by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(expanded = expandedStart, onExpandedChange = { expandedStart = !expandedStart }) {
                            val label = nodeTimes.getOrNull(start - 1)?.label ?: "$start"
                            OutlinedTextField(value = "第 $label 节", onValueChange = {}, readOnly = true, modifier = Modifier.width(100.dp).menuAnchor())
                            ExposedDropdownMenu(expanded = expandedStart, onDismissRequest = { expandedStart = false }) {
                                // 适配 16 个时间段
                                (1..16).forEach { n -> DropdownMenuItem(text = { Text("第 ${nodeTimes[n-1].label} 节") }, onClick = { start = n; if(end < start) end = start; expandedStart = false }) }
                            }
                        }
                    }
                    Column {
                        Text("结束节数", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        var expandedEnd by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(expanded = expandedEnd, onExpandedChange = { expandedEnd = !expandedEnd }) {
                            val label = nodeTimes.getOrNull(end - 1)?.label ?: "$end"
                            OutlinedTextField(value = "第 $label 节", onValueChange = {}, readOnly = true, modifier = Modifier.width(100.dp).menuAnchor())
                            ExposedDropdownMenu(expanded = expandedEnd, onDismissRequest = { expandedEnd = false }) {
                                // 适配 16 个时间段
                                (start..16).forEach { n -> DropdownMenuItem(text = { Text("第 ${nodeTimes[n-1].label} 节") }, onClick = { end = n; expandedEnd = false }) }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(day, start, end) }) { Text("确定", color = MaterialTheme.colorScheme.primary) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消", color = MaterialTheme.colorScheme.onSurfaceVariant) } }
    )
}

@Composable
fun EditableCourseRow(icon: String, placeholder: String, value: String, onValueChange: (String) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text = icon, fontSize = 20.sp)
        Spacer(modifier = Modifier.width(20.dp))
        BasicTextField(
            value = value, onValueChange = onValueChange,
            textStyle = TextStyle(fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface),
            decorationBox = { innerTextField ->
                if (value.isEmpty()) Text(text = placeholder, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                innerTextField()
            }, modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun StaticCourseRow(icon: String, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 12.dp)) {
        Text(text = icon, fontSize = 20.sp)
        Spacer(modifier = Modifier.width(20.dp))
        Text(text = title, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun ColorPickerDialog(
    initialBgColor: Color,
    initialTextColor: Color,
    onDismiss: () -> Unit,
    onColorSelected: (Color, Color) -> Unit
) {
    var tempBgColor by remember { mutableStateOf(initialBgColor) }
    var tempTextColor by remember { mutableStateOf(initialTextColor) }
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    val colorPalette = listOf(
        Color(0xFFC62828), Color(0xFFEF6C00), Color(0xFFF9A825), Color(0xFF00838F), Color(0xFF1565C0), Color(0xFF6A1B9A), Color(0xFF2E7D32), Color(0xFF212121),
        Color(0xFFE53935), Color(0xFFFB8C00), Color(0xFFFDD835), Color(0xFF00ACC1), Color(0xFF1E88E5), Color(0xFF8E24AA), Color(0xFF43A047), Color(0xFF616161),
        Color(0xFFEF5350), Color(0xFFFFA726), Color(0xFFFFEE58), Color(0xFF26C6DA), Color(0xFF42A5F5), Color(0xFFAB47BC), Color(0xFF66BB6A), Color(0xFF9E9E9E),
        Color(0xFFEF9A9A), Color(0xFFFFCC80), Color(0xFFFFF59D), Color(0xFF80DEEA), Color(0xFF90CAF9), Color(0xFFCE93D8), Color(0xFFA5D6A7), Color(0xFFE0E0E0),
        Color(0xFFFFEBEE), Color(0xFFFFF3E0), Color(0xFFFFFDE7), Color(0xFFE0F7FA), Color(0xFFE3F2FD), Color(0xFFF3E5F5), Color(0xFFE8F5E9), Color(0xFFFFFFFF)
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("自定义样式", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Spacer(modifier = Modifier.height(8.dp))
                Text("效果预览", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier.fillMaxWidth(0.55f).height(56.dp).clip(RoundedCornerShape(12.dp)).background(tempBgColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text("示例字样", fontSize = 18.sp, color = tempTextColor)
                }
                Spacer(modifier = Modifier.height(16.dp))

                TabRow(
                    selectedTabIndex = selectedTabIndex, containerColor = Color.Transparent, contentColor = MaterialTheme.colorScheme.primary,
                    indicator = { tabPositions ->
                        val currentTab = tabPositions[selectedTabIndex]
                        val indicatorWidth = 40.dp
                        val targetOffset = currentTab.left + (currentTab.width - indicatorWidth) / 2
                        val animatedOffset by animateDpAsState(targetValue = targetOffset, animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow), label = "tabIndicator")
                        Box(modifier = Modifier.fillMaxWidth().wrapContentSize(Alignment.BottomStart).offset(x = animatedOffset).width(indicatorWidth).height(4.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                    },
                    divider = {}
                ) {
                    Tab(selected = selectedTabIndex == 0, onClick = { selectedTabIndex = 0 }, text = { Text("修改背景色", color = if (selectedTabIndex == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 16.sp) })
                    Tab(selected = selectedTabIndex == 1, onClick = { selectedTabIndex = 1 }, text = { Text("修改文字色", color = if (selectedTabIndex == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 16.sp) })
                }
                Spacer(modifier = Modifier.height(16.dp))

                LazyVerticalGrid(
                    columns = GridCells.Fixed(8), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth().height(210.dp)
                ) {
                    items(colorPalette) { color ->
                        val isSelected = if (selectedTabIndex == 0) color == tempBgColor else color == tempTextColor
                        Box(modifier = Modifier.aspectRatio(1f).clip(RoundedCornerShape(4.dp)).background(color).border(width = if (isSelected) 2.dp else 0.dp, color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent, shape = RoundedCornerShape(4.dp)).clickable { if (selectedTabIndex == 0) tempBgColor = color else tempTextColor = color })
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("取消", color = MaterialTheme.colorScheme.primary, fontSize = 16.sp) }
                    Spacer(modifier = Modifier.width(16.dp))
                    TextButton(onClick = { onColorSelected(tempBgColor, tempTextColor) }) { Text("保存", color = MaterialTheme.colorScheme.primary, fontSize = 16.sp) }
                }
            }
        }
    }
}