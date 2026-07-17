package com.jiaweiya.flowcourse

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun MainDrawerSheet(
    currentAppVersion: String,
    activeProfileId: Int,
    activeTimetableId: Int,
    totalWeeks: Int,
    currentViewedWeek: Int,
    realTimeSlider: Boolean,
    timetables: List<TimetableData>,
    onCloseDrawer: () -> Unit,
    hasCredentials: Boolean,
    onShowAutoUpdateDialog: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToEditTimeProfile: (Int) -> Unit,
    onNavigateToAddCourse: () -> Unit,
    onNavigateToCourseList: (Int) -> Unit,
    onNavigateToEditTimetable: (Int) -> Unit,
    onShowSponsorDialog: () -> Unit,
    onShowImportMenu: () -> Unit,
    onShowShareMenu: () -> Unit,
    onShowSetCurrentWeekDialog: () -> Unit,
    onUpdateFound: (GithubRelease) -> Unit,
    onWeekSliderChange: (Float) -> Unit,
    onWeekSliderChangeFinished: (Int) -> Unit,
    onNewTimetable: () -> Unit,
    onTimetableSelect: (Int) -> Unit,
    onDeleteTimetable: (Int) -> Unit,
    onReorderTimetables: (List<TimetableData>) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var isManageMode by remember { mutableStateOf(false) }
    var timetableToDelete by remember { mutableStateOf<Int?>(null) }
    var draggedIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }

    ModalDrawerSheet(
        modifier = Modifier.width(320.dp),
        drawerContainerColor = MaterialTheme.colorScheme.surface,
        windowInsets = WindowInsets(0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(state = rememberScrollState(), enabled = draggedIndex == null)
                .statusBarsPadding()
        ) {

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onCloseDrawer(); onNavigateToAbout() }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Image(
                                painter = painterResource(id = R.drawable.jiaweiya_icon), // 确保 R 资源正常导入
                                contentDescription = "Jiaweiya",
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Jiaweiya", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("FlowCourse", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("关于此应用", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 4.dp))
                        }
                    }

                    Text(
                        text = "检查更新",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(12.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .clickable {
                                Toast.makeText(context, "正在检查更新...", Toast.LENGTH_SHORT).show()
                                coroutineScope.launch {
                                    val sharedPrefs = context.getSharedPreferences("FlowCourseDB", android.content.Context.MODE_PRIVATE)
                                    val updateChannel = sharedPrefs.getInt("update_channel", 0)

                                    checkAppUpdate(currentVersion = currentAppVersion, channel = updateChannel) { release, isLatest ->
                                        if (release != null) onUpdateFound(release)
                                        else if (isLatest) Toast.makeText(context, "当前已是最新版本", Toast.LENGTH_SHORT).show()
                                        else Toast.makeText(context, "检查失败，请检查网络", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                            .padding(4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
                DrawerMenuItem(Icons.Default.Settings, "应用设置") { onCloseDrawer(); onNavigateToSettings() }
                DrawerMenuItem(Icons.Default.Favorite, "赞助我") { onCloseDrawer(); onShowSponsorDialog() }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            Text("所有课表设置", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 24.dp, top = 8.dp, bottom = 4.dp))
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
                DrawerMenuItem(Icons.Default.DateRange, "上课时间") { onCloseDrawer(); onNavigateToEditTimeProfile(activeProfileId) }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            Text("当前课表设置", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 24.dp, top = 8.dp, bottom = 4.dp))
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
                DrawerMenuItem(Icons.Default.Download, "导入课程") { onCloseDrawer(); onShowImportMenu() }
                DrawerMenuItem(Icons.Default.Share, "导出课程") { onCloseDrawer(); onShowShareMenu() }
            }
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
                DrawerMenuItem(Icons.Default.Add, "添加课程") { onCloseDrawer(); onNavigateToAddCourse() }
                DrawerMenuItem(Icons.Default.List, "已添课程") { onCloseDrawer(); onNavigateToCourseList(activeTimetableId) }
            }
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
                DrawerMenuItem(Icons.Default.Build, "课表属性") { onCloseDrawer(); onNavigateToEditTimetable(activeTimetableId) }
            }
            NavigationDrawerItem(
                label = { Text("更新当前课表") },
                icon = { Icon(Icons.Default.Refresh, contentDescription = "刷新") },
                selected = false,
                onClick = {
                    if (hasCredentials) {
                        onShowAutoUpdateDialog()
                        onCloseDrawer()
                    } else {
                        Toast.makeText(context, "请先在设置中填写学号和密码", Toast.LENGTH_SHORT).show()
                    }
                },
                badge = {
                    if (!hasCredentials) {
                        Text("未配置账号", fontSize = 10.sp, color = MaterialTheme.colorScheme.error)
                    }
                },
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 4.dp)
                    .alpha(if (hasCredentials) 1f else 0.5f)
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            var sliderValue by remember { mutableFloatStateOf(currentViewedWeek.toFloat()) }
            var isDragging by remember { mutableStateOf(false) }

            LaunchedEffect(currentViewedWeek) { if (!isDragging) sliderValue = currentViewedWeek.toFloat() }
            val displayWeek = sliderValue.roundToInt()

            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("周数 (第 ${displayWeek} 周)", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text("修改当前周", fontSize = 13.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.clickable { onCloseDrawer(); onShowSetCurrentWeekDialog() })
            }
            Slider(
                value = sliderValue,
                onValueChange = { newValue ->
                    isDragging = true; sliderValue = newValue
                    if (realTimeSlider) onWeekSliderChange(newValue)
                },
                onValueChangeFinished = {
                    isDragging = false
                    if (!realTimeSlider) onWeekSliderChangeFinished(displayWeek)
                },
                valueRange = 1f..totalWeeks.toFloat(),
                steps = if (totalWeeks > 2) totalWeeks - 2 else 0,
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("课表", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.animateContentSize(animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = 600f))
                ) {
                    Text("新建课表", fontSize = 14.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.clickable { onNewTimetable() })
                    Spacer(modifier = Modifier.width(16.dp))
                    AnimatedContent(
                        targetState = isManageMode,
                        transitionSpec = { fadeIn(animationSpec = tween(200)) togetherWith fadeOut(animationSpec = tween(200)) },
                        label = "manage_btn_anim"
                    ) { isManaging ->
                        Text(
                            text = if(isManaging) "退出管理" else "管理",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable { isManageMode = !isManageMode }
                        )
                    }
                }
            }

            val currentTimetables by rememberUpdatedState(timetables)
            val density = LocalDensity.current
            val itemWidthPx = remember(density) { with(density) { 93.dp.toPx() } }
            val itemHeightPx = remember(density) { with(density) { 97.dp.toPx() } }

            val rowCount = (timetables.size - 1) / 3 + 1
            val gridHeight = (rowCount * 95).dp + ((rowCount - 1) * 12).dp + 16.dp

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.fillMaxWidth().height(gridHeight).padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                userScrollEnabled = false
            ) {
                items(timetables, key = { it.id }) { timetable ->
                    val isSelected = timetable.id == activeTimetableId
                    val isDragged = draggedIndex != null && currentTimetables.getOrNull(draggedIndex!!)?.id == timetable.id
                    val dragScale by animateFloatAsState(if (isDragged) 1.15f else 1f, label = "drag_scale")
                    val infiniteTransition = rememberInfiniteTransition(label = "jiggle")
                    val jiggleAngle by infiniteTransition.animateFloat(
                        initialValue = -2.5f, targetValue = 2.5f,
                        animationSpec = infiniteRepeatable(animation = tween(130, easing = LinearEasing), repeatMode = RepeatMode.Reverse),
                        label = "jiggle_angle"
                    )
                    val actualRotation = if (isManageMode && !isDragged) jiggleAngle else 0f

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.then(if (isDragged) Modifier else Modifier.animateItem()).graphicsLayer {
                            translationX = if (isDragged) dragOffset.x else 0f
                            translationY = if (isDragged) dragOffset.y else 0f
                            scaleX = dragScale; scaleY = dragScale
                            rotationZ = actualRotation
                        }.zIndex(if (isDragged) 1f else 0f).pointerInput(isManageMode, timetable.id) {
                            if (!isManageMode) return@pointerInput
                            detectDragGesturesAfterLongPress(
                                onDragStart = { draggedIndex = currentTimetables.indexOfFirst { it.id == timetable.id } },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    dragOffset += dragAmount
                                    val currentIndex = draggedIndex ?: return@detectDragGesturesAfterLongPress

                                    val deltaX = (dragOffset.x / itemWidthPx).roundToInt()
                                    val deltaY = (dragOffset.y / itemHeightPx).roundToInt()

                                    if (deltaX != 0 || deltaY != 0) {
                                        val targetIndex = (currentIndex + deltaX + deltaY * 3).coerceIn(0, currentTimetables.size - 1)
                                        if (targetIndex != currentIndex) {
                                            val newList = currentTimetables.toMutableList()
                                            val temp = newList[currentIndex]
                                            newList[currentIndex] = newList[targetIndex]
                                            newList[targetIndex] = temp
                                            onReorderTimetables(newList)
                                            draggedIndex = targetIndex
                                            dragOffset -= androidx.compose.ui.geometry.Offset(
                                                x = (targetIndex % 3 - currentIndex % 3) * itemWidthPx,
                                                y = (targetIndex / 3 - currentIndex / 3) * itemHeightPx
                                            )
                                        }
                                    }
                                },
                                onDragEnd = { draggedIndex = null; dragOffset = androidx.compose.ui.geometry.Offset.Zero },
                                onDragCancel = { draggedIndex = null; dragOffset = androidx.compose.ui.geometry.Offset.Zero }
                            )
                        }
                    ) {
                        Box(modifier = Modifier.size(60.dp).clip(RoundedCornerShape(12.dp)).background(if (isSelected && !isManageMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant).clickable {
                            if (isManageMode) { onCloseDrawer(); onNavigateToEditTimetable(timetable.id) }
                            else { onCloseDrawer(); onTimetableSelect(timetable.id) }
                        }) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                if (isManageMode) Icon(Icons.Default.Edit, contentDescription = "编辑", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                else {
                                    if (isSelected) Icon(Icons.Default.Check, contentDescription = "选中", tint = MaterialTheme.colorScheme.onPrimary)
                                    else Text("${timetable.courses.size}门", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            if (isManageMode && timetables.size > 1) {
                                Box(
                                    modifier = Modifier.align(Alignment.TopEnd).size(20.dp).clip(RoundedCornerShape(bottomStart = 20.dp)).background(MaterialTheme.colorScheme.error).clickable { timetableToDelete = timetable.id },
                                    contentAlignment = Alignment.Center
                                ) { Icon(Icons.Default.Close, contentDescription = "删除", tint = MaterialTheme.colorScheme.onError, modifier = Modifier.size(12.dp).offset(x = 2.dp, y = (-2).dp)) }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = timetable.name, fontSize = 12.sp, color = if (isSelected && !isManageMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            Spacer(modifier = Modifier.height(60.dp))
        }
    }

    // 删除课表的弹窗
    if (timetableToDelete != null) {
        val deleteName = timetables.find { it.id == timetableToDelete }?.name ?: ""
        AlertDialog(
            onDismissRequest = { timetableToDelete = null },
            title = { Text("删除课表", fontWeight = FontWeight.Bold) },
            text = { Text("您确定要删除\"$deleteName\"吗？") },
            confirmButton = { TextButton(onClick = { onDeleteTimetable(timetableToDelete!!); timetableToDelete = null }) { Text("删除", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { timetableToDelete = null }) { Text("取消", color = MaterialTheme.colorScheme.onSurfaceVariant) } }
        )
    }
}

@Composable
fun RowScope.DrawerMenuItem(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, onClick: () -> Unit) {
    NavigationDrawerItem(
        icon = { Icon(icon, contentDescription = text, modifier = Modifier.size(22.dp)) },
        label = { Text(text, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        selected = false,
        onClick = onClick,
        modifier = Modifier.weight(1f).padding(horizontal = 4.dp, vertical = 2.dp)
    )
}