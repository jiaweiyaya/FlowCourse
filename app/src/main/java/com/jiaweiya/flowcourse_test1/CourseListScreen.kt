package com.jiaweiya.flowcourse_test1

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CourseListScreen(
    timetableId: Int,
    timetables: List<TimetableData>,
    onSave: (List<TimetableData>) -> Unit,
    onBackClick: () -> Unit,
    onNavigateToAddCourse: () -> Unit,
    onNavigateToEditCourse: (Int) -> Unit
) {
    val activeTimetable = timetables.find { it.id == timetableId } ?: timetables.firstOrNull()
    // 按课程名去重，避免多时间段的同一门课显示多个卡片
    val uniqueCourses = activeTimetable?.courses?.distinctBy { it.name } ?: emptyList()

    var isMultiSelectMode by remember { mutableStateOf(false) }
    var selectedCourseNames by remember { mutableStateOf(setOf<String>()) }

    var courseToDelete by remember { mutableStateOf<Course?>(null) } // 单选长按删除
    var showMultiDeleteConfirm by remember { mutableStateOf(false) } // 多选删除确认

    // 核心物理动画规格
    val bouncySpring = spring<androidx.compose.ui.unit.IntSize>(
        dampingRatio = Spring.DampingRatioHighBouncy,
        stiffness = 600f
    )

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0.dp),
                title = { Text("课程管理", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回") }
                },
                actions = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.animateContentSize(animationSpec = bouncySpring)
                    ) {
                        AnimatedContent(
                            targetState = isMultiSelectMode,
                            transitionSpec = { fadeIn(animationSpec = tween(200)) togetherWith fadeOut(animationSpec = tween(200)) },
                            label = "multi_btn"
                        ) { isMulti ->
                            Text(
                                text = if (isMulti) "结束多选" else "多选",
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .clickable {
                                        isMultiSelectMode = !isMultiSelectMode
                                        selectedCourseNames = emptySet()
                                    }
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (isMultiSelectMode) {
                        if (selectedCourseNames.isNotEmpty()) showMultiDeleteConfirm = true
                    } else {
                        onNavigateToAddCourse()
                    }
                },
                containerColor = if (isMultiSelectMode) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
                contentColor = if (isMultiSelectMode) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                AnimatedContent(
                    targetState = isMultiSelectMode,
                    transitionSpec = {
                        scaleIn(animationSpec = tween(200)) togetherWith scaleOut(animationSpec = tween(200))
                    },
                    label = "fab_icon"
                ) { multiSelect ->
                    if (multiSelect) Icon(Icons.Default.Delete, contentDescription = "删除选中")
                    else Icon(Icons.Default.Add, contentDescription = "添加课程")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isMultiSelectMode) {
                    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        Text("全选", fontSize = 14.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.clickable { selectedCourseNames = uniqueCourses.map { it.name }.toSet() }.padding(4.dp))
                        Text("全不选", fontSize = 14.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.clickable { selectedCourseNames = emptySet() }.padding(4.dp))
                        Text("反选选中对象", fontSize = 14.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.clickable {
                            val allNames = uniqueCourses.map { it.name }.toSet()
                            selectedCourseNames = allNames - selectedCourseNames
                        }.padding(4.dp))
                    }
                } else {
                    Text("轻触编辑，长按删除", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // 课程卡片网格
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(uniqueCourses, key = { it.id }) { course ->
                    val isSelected = selectedCourseNames.contains(course.name)
                    val interactionSource = remember { MutableInteractionSource() }
                    val isPressed by interactionSource.collectIsPressedAsState()
                    val scale by animateFloatAsState(
                        targetValue = if (isPressed) 0.9f else 1f,
                        animationSpec = spring(dampingRatio = 0.5f, stiffness = 400f),
                        label = "card_bounce"
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer { scaleX = scale; scaleY = scale }
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(course.bgColor))
                            .combinedClickable(
                                interactionSource = interactionSource,
                                indication = LocalIndication.current,
                                onClick = {
                                    if (isMultiSelectMode) {
                                        selectedCourseNames = if (isSelected) selectedCourseNames - course.name else selectedCourseNames + course.name
                                    } else {
                                        onNavigateToEditCourse(course.id)
                                    }
                                },
                                onLongClick = {
                                    if (!isMultiSelectMode) courseToDelete = course
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        // 课程名文本居中
                        Text(
                            text = course.name,
                            fontSize = 16.sp,
                            lineHeight = 20.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(course.textColor),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 24.dp)
                        )

                        // 多选模式下左上角的 Checkbox
                        if (isMultiSelectMode) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(8.dp)
                            ) {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = null,
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = MaterialTheme.colorScheme.primary,
                                        uncheckedColor = Color(course.textColor).copy(alpha = 0.5f)
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        // 单选长按删除弹窗
        if (courseToDelete != null) {
            AlertDialog(
                onDismissRequest = { courseToDelete = null },
                title = { Text("删除课程", fontWeight = FontWeight.Bold) },
                text = { Text("确定要删除\"" + (courseToDelete?.name ?: "") + "\"吗？这会删除该课程的所有上课时间段，且无法恢复。") },
                confirmButton = {
                    TextButton(onClick = {
                        if (activeTimetable != null && courseToDelete != null) {
                            val newCourses = activeTimetable.courses.filter { it.name != courseToDelete!!.name }
                            val newTimetables = timetables.map { if (it.id == timetableId) it.copy(courses = newCourses) else it }
                            onSave(newTimetables)
                        }
                        courseToDelete = null
                    }) { Text("删除", color = MaterialTheme.colorScheme.error) }
                },
                dismissButton = {
                    TextButton(onClick = { courseToDelete = null }) { Text("取消", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            )
        }

        // 多选批量删除弹窗
        if (showMultiDeleteConfirm) {
            AlertDialog(
                onDismissRequest = { showMultiDeleteConfirm = false },
                title = { Text("批量删除", fontWeight = FontWeight.Bold) },
                text = { Text("确定要删除选中的 ${selectedCourseNames.size} 门课程吗？此操作无法恢复。") },
                confirmButton = {
                    TextButton(onClick = {
                        if (activeTimetable != null) {
                            val newCourses = activeTimetable.courses.filter { it.name !in selectedCourseNames }
                            val newTimetables = timetables.map { if (it.id == timetableId) it.copy(courses = newCourses) else it }
                            onSave(newTimetables)
                        }
                        selectedCourseNames = emptySet()
                        isMultiSelectMode = false
                        showMultiDeleteConfirm = false
                    }) { Text("删除", color = MaterialTheme.colorScheme.error) }
                },
                dismissButton = {
                    TextButton(onClick = { showMultiDeleteConfirm = false }) { Text("取消", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
            )
        }
    }
}