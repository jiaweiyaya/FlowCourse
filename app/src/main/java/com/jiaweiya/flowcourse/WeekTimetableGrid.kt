package com.jiaweiya.flowcourse

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import java.time.LocalDate
import java.time.LocalTime

// 课表绘制相关常量
val timeSlotHeight = 65.dp
val sideBarWidth = 35.dp

// 根据节点和当前时间计算 Y 轴偏移量
fun calculateTimeLineOffset(currentTime: LocalTime, visibleNodes: List<NodeTime>, slotHeightPx: Float): Float {
    if (visibleNodes.isEmpty()) return -1f

    val currentMins = currentTime.hour * 60 + currentTime.minute

    try {
        val firstStartParts = visibleNodes.first().start.split(":")
        val firstStartMins = firstStartParts[0].toInt() * 60 + firstStartParts[1].toInt()

        val lastEndParts = visibleNodes.last().end.split(":")
        val lastEndMins = lastEndParts[0].toInt() * 60 + lastEndParts[1].toInt()

        if (currentMins < firstStartMins || currentMins > lastEndMins) {
            return -1f
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }

    for (i in visibleNodes.indices) {
        try {
            val node = visibleNodes[i]
            val startParts = node.start.split(":")
            val endParts = node.end.split(":")
            val startMins = startParts[0].toInt() * 60 + startParts[1].toInt()
            val endMins = endParts[0].toInt() * 60 + endParts[1].toInt()

            if (currentMins < startMins) {
                return i * slotHeightPx
            }
            if (currentMins <= endMins) {
                val fraction = (currentMins - startMins).toFloat() / (endMins - startMins).toFloat()
                return i * slotHeightPx + fraction * slotHeightPx
            }
        } catch (e: Exception) { continue }
    }
    return -1f
}

@Composable
fun WeekTimetableGrid(
    preferredConflictIds: Set<Int>,
    onCourseClick: (List<Course>) -> Unit,
    conflictColor: Long,
    showCourseBorder: Boolean,
    courseBorderColor: Long,
    currentWeek: Int,
    allCourses: List<Course>,
    profileNodes: List<NodeTime>,
    termStartStr: String?,
    highlightToday: Boolean,
    showTimeLine: Boolean,
    showConflictWarning: Boolean,
) {
    val scrollState = rememberScrollState()
    val visibleNodes = profileNodes.filter { it.isVisible }
    val todayDate = LocalDate.now()

    val startLocalDate = try {
        if (termStartStr != null) LocalDate.parse(termStartStr) else todayDate.minusDays((todayDate.dayOfWeek.value - 1).toLong())
    } catch(e: Exception) { todayDate.minusDays((todayDate.dayOfWeek.value - 1).toLong()) }
    val viewedMonday = startLocalDate.plusWeeks((currentWeek - 1).toLong())

    var currentTime by remember { mutableStateOf(LocalTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = LocalTime.now()
            kotlinx.coroutines.delay(1000)
        }
    }

    // 预计算每日课程聚类，避免每次渲染重复过滤和冲突计算
    val clusteredDailyCourses = remember(allCourses, currentWeek) {
        (1..7).map { day ->
            val daily = allCourses.filter { it.dayOfWeek == day && it.weekList.contains(currentWeek) }
            val clusters = mutableListOf<MutableList<Course>>()
            for (course in daily.sortedBy { it.startNode }) {
                var added = false
                for (cluster in clusters) {
                    if (cluster.any { it.startNode <= course.endNode && course.startNode <= it.endNode }) {
                        cluster.add(course)
                        added = true
                        break
                    }
                }
                if (!added) {
                    clusters.add(mutableListOf(course))
                }
            }
            clusters
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // 顶部星期栏
        Row(modifier = Modifier.fillMaxWidth().height(45.dp).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)), verticalAlignment = Alignment.CenterVertically) {
            Spacer(modifier = Modifier.width(sideBarWidth))
            Spacer(modifier = Modifier.width(1.dp))

            Row(modifier = Modifier.weight(1f)) {
                val days = listOf("一", "二", "三", "四", "五", "六", "日")
                for (i in 0..6) {
                    val currentDate = viewedMonday.plusDays(i.toLong())
                    val isToday = currentDate.isEqual(todayDate)

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(horizontal = 7.dp, vertical = 5.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .then(
                                if (highlightToday && isToday) Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)) else Modifier
                            ),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy((-10).dp, Alignment.CenterVertically)
                    ){
                        Text(text = "周${days[i]}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(text = "${currentDate.monthValue}/${currentDate.dayOfMonth}", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
                    }
                }
            }
        }

        Box(modifier = Modifier.fillMaxWidth().verticalScroll(scrollState)) {
            Row(modifier = Modifier.fillMaxWidth()) {
                // 左侧时间栏
                Column(modifier = Modifier.width(sideBarWidth).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))) {
                    for (node in visibleNodes) {
                        Box(modifier = Modifier.height(timeSlotHeight).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                Text(text = node.start, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 10.sp)
                                Text(text = node.label, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(vertical = 2.dp), lineHeight = 15.sp)
                                Text(text = node.end, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 10.sp)
                            }
                            HorizontalDivider(modifier = Modifier.align(Alignment.BottomCenter), thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        }
                    }
                }
                Spacer(modifier = Modifier.width(1.dp).height(timeSlotHeight * visibleNodes.size).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)))

                // 右侧课程网格区域
                Box(modifier = Modifier.weight(1f)) {
                    // 背景网格线
                    Column {
                        for (i in visibleNodes.indices) {
                            HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(timeSlotHeight - 0.5.dp))
                        }
                    }
                    // 课程块渲染
                    Row(modifier = Modifier.fillMaxWidth()) {
                        for (day in 1..7) {
                            val currentDate = viewedMonday.plusDays((day - 1).toLong())
                            val isToday = currentDate.isEqual(todayDate)

                            Box(modifier = Modifier.weight(1f).height(timeSlotHeight * visibleNodes.size).then(
                                if (highlightToday && isToday) Modifier.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)) else Modifier
                            )) {
                                val clusters = clusteredDailyCourses[day - 1]

                                clusters.forEach { cluster ->
                                    val displayCourse = cluster.firstOrNull { preferredConflictIds.contains(it.id) } ?: cluster.first()
                                    val isConflicted = cluster.size > 1

                                    val indexedVisibleNodes = profileNodes.withIndex().filter { it.value.isVisible }
                                    val matchingSlots = indexedVisibleNodes.filter { it.index in (displayCourse.startNode - 1)..(displayCourse.endNode - 1) }

                                    if (matchingSlots.isNotEmpty()) {
                                        val startVisualIndex = indexedVisibleNodes.indexOf(matchingSlots.first())
                                        val endVisualIndex = indexedVisibleNodes.indexOf(matchingSlots.last())

                                        val topOffset = startVisualIndex * timeSlotHeight.value
                                        val height = (endVisualIndex - startVisualIndex + 1) * timeSlotHeight.value

                                        CourseBlock(
                                            course = displayCourse,
                                            topOffset = topOffset,
                                            height = height,
                                            isConflicted = isConflicted && showConflictWarning,
                                            conflictColor = conflictColor,
                                            showCourseBorder = showCourseBorder,
                                            courseBorderColor = courseBorderColor,
                                            onClick = { _ -> onCourseClick(cluster) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 绘制当前时间轴
            if (showTimeLine) {
                val density = LocalDensity.current
                val slotHeightPx = with(density) { timeSlotHeight.toPx() }

                TimeLineMarker(
                    currentTimeProvider = { currentTime },
                    visibleNodes = visibleNodes,
                    slotHeightPx = slotHeightPx,
                    density = density
                )
            }
        }
    }
}

@Composable
fun CourseBlock(
    course: Course, topOffset: Float, height: Float, isConflicted: Boolean,
    onClick: (Course) -> Unit, conflictColor: Long,
    showCourseBorder: Boolean, courseBorderColor: Long
) {
    val density = LocalDensity.current
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(targetValue = if (isPressed) 0.85f else 1f, animationSpec = spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessMediumLow), label = "bounce")

    val maxNameLines = if (height >= timeSlotHeight.value * 2 - 10) 5 else 2
    val teacherLineHeightDp = with(density) { 10.sp.toDp() }
    val totalTeacherSpace = teacherLineHeightDp + 4.dp

    Box(
        modifier = Modifier
            .offset(y = topOffset.dp)
            .fillMaxWidth()
            .height(height.dp)
            .padding(2.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .then(
                if (showCourseBorder) Modifier.border(1.dp, Color(courseBorderColor), RoundedCornerShape(8.dp))
                else Modifier
            )
            .clip(RoundedCornerShape(8.dp))
            .pointerInput(key1 = course) {
                detectTapGestures(
                    onPress = { isPressed = true; tryAwaitRelease(); isPressed = false },
                    onTap = { onClick(course) }
                )
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(0.dp)
                .clip(RoundedCornerShape( 8.dp))
                .background(Color(course.bgColor))
                .padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                Text(
                    text = course.name,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(course.textColor),
                    lineHeight = 12.sp,
                    maxLines = maxNameLines,
                    overflow = TextOverflow.Ellipsis
                )
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    if (course.room.isNotEmpty()) {
                        Text(text = "@${course.room}", fontSize = 9.sp, color = Color(course.textColor).copy(alpha = 0.7f), lineHeight = 10.sp, maxLines = Int.MAX_VALUE, overflow = TextOverflow.Ellipsis, modifier = Modifier.fillMaxSize().padding(bottom = totalTeacherSpace))
                    }
                    if (course.teacher.isNotEmpty()) {
                        Text(text = course.teacher, fontSize = 9.sp, color = Color(course.textColor).copy(alpha = 0.7f), lineHeight = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 2.dp))
                    }
                }
            }
        }

        if (isConflicted) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val triangleSize = 14.dp.toPx()
                val cornerOffset = 3.dp.toPx()
                val path = Path().apply {
                    moveTo(size.width - triangleSize - cornerOffset, cornerOffset)
                    lineTo(size.width - cornerOffset, cornerOffset)
                    lineTo(size.width - cornerOffset, triangleSize + cornerOffset)
                    close()
                }
                drawPath(path = path, color = Color(conflictColor).copy(alpha = 0.85f))
            }
        }
    }
}

@Composable
fun TimeLineMarker(
    currentTimeProvider: () -> LocalTime,
    visibleNodes: List<NodeTime>,
    slotHeightPx: Float,
    density: Density
) {
    val currentTime = currentTimeProvider()
    val yOffsetPx = calculateTimeLineOffset(currentTime, visibleNodes, slotHeightPx)
    val totalHeightPx = visibleNodes.size * slotHeightPx

    if (yOffsetPx in 0f..totalHeightPx) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = with(density) { yOffsetPx.toDp() })
                .zIndex(10f)
        ) {
            val timeLineColor = Color(0xFFDB72FA)
            HorizontalDivider(thickness = 2.dp, color = timeLineColor.copy(alpha = 0.8f))

            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = 4.dp, y = (-16).dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = String.format("%02d:%02d", currentTime.hour, currentTime.minute),
                    fontSize = 8.sp,
                    color = timeLineColor,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = timeLineColor,
                    modifier = Modifier.size(15.dp).offset(x = (-3).dp)
                )
            }
        }
    }
}