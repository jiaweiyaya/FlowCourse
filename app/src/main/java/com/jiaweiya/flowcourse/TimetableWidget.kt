@file:SuppressLint("RestrictedApi")
package com.jiaweiya.flowcourse.widget

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.color.ColorProvider
import androidx.glance.layout.*
import androidx.glance.text.*
import androidx.glance.unit.ColorProvider
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.jiaweiya.flowcourse.Course
import com.jiaweiya.flowcourse.MainActivity
import com.jiaweiya.flowcourse.NodeTime
import com.jiaweiya.flowcourse.TimeProfile
import com.jiaweiya.flowcourse.TimetableData
import com.jiaweiya.flowcourse.nodeTimes // 引入默认的时间配置
import java.time.LocalDate
import java.time.temporal.ChronoUnit

// 1. 小组件接收器
class TimetableWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TimetableWidget()
}

// 2. 点击刷新按钮的回调动作
class RefreshWidgetAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        // 【修复5】去除了所有限制，无论应用在不在后台，点击必定强行刷新！
        TimetableWidget().updateAll(context)
    }
}

// 3. 小组件 UI 构建类
class TimetableWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val todayCourses = getTodayCourses(context)
        val dateInfo = getTodayDateInfo(context)
        val profileNodes = getActiveProfileNodes(context) // 获取当前的时间表配置

        provideContent {
            GlanceTheme {
                WidgetContent(context, dateInfo, todayCourses, profileNodes)
            }
        }
    }

    @Composable
    private fun WidgetContent(context: Context, dateInfo: String, courses: List<Course>, profileNodes: List<NodeTime>) {
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(day = Color.White, night = Color(0xFF1E1E1E)))
                .padding(12.dp)
                // 【修复2】点击空白处打开应用。
                // 之前失效是因为 LazyColumn 撑满了屏幕挡住了点击，现在已经改了！
                .clickable(actionStartActivity(openAppIntent))
        ) {
            // --- 头部：日期 和 刷新按钮 ---
            Row(
                modifier = GlanceModifier.fillMaxWidth().padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dateInfo,
                    style = TextStyle(
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = ColorProvider(day = Color.Black, night = Color.White)
                    ),
                    modifier = GlanceModifier.defaultWeight()
                )

                Image(
                    provider = ImageProvider(android.R.drawable.ic_popup_sync),
                    contentDescription = "刷新",
                    modifier = GlanceModifier
                        .size(24.dp)
                        .padding(2.dp)
                        .clickable(actionRunCallback<RefreshWidgetAction>()),
                    colorFilter = ColorFilter.tint(ColorProvider(day = Color.DarkGray, night = Color(0xFFDDDDDD)))
                )
            }

            // --- 课程列表 ---
            if (courses.isEmpty()) {
                Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = "今天没有课，好好休息！", style = TextStyle(fontSize = 14.sp, color = ColorProvider(Color.Gray)))
                }
            } else {
                // 【修复2重点】去掉了 fillMaxSize()，让列表只占实际内容高度，多余的空白区域就归属外层 Column，这样点击底部空白才能生效。
                LazyColumn(modifier = GlanceModifier.fillMaxWidth()) {
                    items(courses) { course ->
                        CourseItemWidget(context, course, profileNodes)
                    }
                }
            }
        }
    }

    @Composable
    private fun CourseItemWidget(context: Context, course: Course, profileNodes: List<NodeTime>) {
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

        // 【修复4】获取该课程对应的开始和结束时间
        val startTime = profileNodes.getOrNull(course.startNode - 1)?.start ?: ""
        val endTime = profileNodes.getOrNull(course.endNode - 1)?.end ?: ""

        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                // 【修复3】在这里添加 bottom padding，就会在课程之间产生空隙！
                .padding(bottom = 8.dp)
                .background(ColorProvider(Color(course.bgColor)))
                .cornerRadius(8.dp)
                .padding(8.dp)
                .clickable(actionStartActivity(openAppIntent)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左侧：具体时间 和 节次
            Column(
                modifier = GlanceModifier.width(48.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 上方：开始时间
                Text(
                    text = startTime,
                    style = TextStyle(fontSize = 10.sp, color = ColorProvider(Color(course.textColor)))
                )
                // 中间：节次（去掉了“节”字）
                Text(
                    text = "${course.startNode}-${course.endNode}",
                    style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp, color = ColorProvider(Color(course.textColor)))
                )
                // 下方：结束时间
                Text(
                    text = endTime,
                    style = TextStyle(fontSize = 10.sp, color = ColorProvider(Color(course.textColor)))
                )
            }

            Spacer(modifier = GlanceModifier.width(8.dp))

            // 中间：课程名与地点（最多显示2行）
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    text = course.name,
                    style = TextStyle(fontWeight = FontWeight.Bold, fontSize = 14.sp, color = ColorProvider(Color(course.textColor))),
                    maxLines = 2
                )
                Spacer(modifier = GlanceModifier.height(4.dp))
                Text(
                    text = course.room,
                    style = TextStyle(fontSize = 11.sp, color = ColorProvider(Color(course.textColor))),
                    maxLines = 2
                )
            }

            Spacer(modifier = GlanceModifier.width(4.dp))

            // 【修复1】右侧：永远将任课教师名字垂直显示在右边缘
            // 原理：将名字拆分成单个字，并在每个字中间插入换行符 "\n"
            val verticalTeacher = course.teacher.map { it.toString() }.joinToString("\n")
            Text(
                text = verticalTeacher,
                style = TextStyle(fontSize = 10.sp, color = ColorProvider(Color(course.textColor)), textAlign = TextAlign.Center),
                modifier = GlanceModifier.padding(start = 2.dp)
            )
        }
    }

    // --- 数据获取与计算逻辑 ---

    // 获取当前处于激活状态的时间段配置（为了拿到每节课的具体时间）
    private fun getActiveProfileNodes(context: Context): List<NodeTime> {
        val sharedPrefs = context.getSharedPreferences("FlowCourseDB", Context.MODE_PRIVATE)
        val gson = Gson()

        // 1. 先查出当前激活的课表用的是哪个 profileId
        val timetablesJson = sharedPrefs.getString("timetables_data", null)
        val activeId = sharedPrefs.getInt("active_id", 1)
        var profileId = 1
        try {
            val type = object : TypeToken<List<TimetableData>>() {}.type
            val timetables = gson.fromJson<List<TimetableData>>(timetablesJson, type) ?: emptyList()
            profileId = timetables.find { it.id == activeId }?.timeProfileId ?: 1
        } catch (e: Exception) { }

        // 2. 然后去时间配置表里把那个配置的时间节点查出来
        val profilesJson = sharedPrefs.getString("time_profiles_data", null)
        val defaultNodes = nodeTimes
        if (profilesJson == null) return defaultNodes
        return try {
            val type = object : TypeToken<List<TimeProfile>>() {}.type
            val profiles = gson.fromJson<List<TimeProfile>>(profilesJson, type) ?: emptyList()
            profiles.find { it.id == profileId }?.nodes ?: defaultNodes
        } catch (e: Exception) {
            defaultNodes
        }
    }

    private fun getTodayDateInfo(context: Context): String {
        val sharedPrefs = context.getSharedPreferences("FlowCourseDB", Context.MODE_PRIVATE)
        val gson = Gson()
        val timetablesJson = sharedPrefs.getString("timetables_data", null)
        val activeId = sharedPrefs.getInt("active_id", 1)

        val timetables = try {
            val type = object : TypeToken<List<TimetableData>>() {}.type
            gson.fromJson<List<TimetableData>>(timetablesJson, type) ?: emptyList()
        } catch (e: Exception) { emptyList() }

        val activeTimetable = timetables.find { it.id == activeId } ?: timetables.firstOrNull()

        val today = LocalDate.now()
        val defaultTermStart = today.minusDays((today.dayOfWeek.value - 1).toLong())
        val termStartDateStr = activeTimetable?.termStart ?: defaultTermStart.toString()
        val termStartDate = try { LocalDate.parse(termStartDateStr) } catch(e: Exception) { defaultTermStart }

        val daysDiff = ChronoUnit.DAYS.between(termStartDate, today)
        val currentActualWeek = ((daysDiff / 7).toInt() + 1).coerceIn(1, activeTimetable?.totalWeeks ?: 20)

        val weekDays = listOf("一", "二", "三", "四", "五", "六", "日")
        return "第 $currentActualWeek 周 - 周${weekDays[today.dayOfWeek.value - 1]}"
    }

    private fun getTodayCourses(context: Context): List<Course> {
        val sharedPrefs = context.getSharedPreferences("FlowCourseDB", Context.MODE_PRIVATE)
        val gson = Gson()
        val timetablesJson = sharedPrefs.getString("timetables_data", null)
        val activeId = sharedPrefs.getInt("active_id", 1)

        val timetables = try {
            val type = object : TypeToken<List<TimetableData>>() {}.type
            gson.fromJson<List<TimetableData>>(timetablesJson, type) ?: emptyList()
        } catch (e: Exception) { return emptyList() }

        val activeTimetable = timetables.find { it.id == activeId } ?: return emptyList()

        val today = LocalDate.now()
        val defaultTermStart = today.minusDays((today.dayOfWeek.value - 1).toLong())
        val termStartDateStr = activeTimetable?.termStart ?: defaultTermStart.toString()
        val termStartDate = try { LocalDate.parse(termStartDateStr) } catch(e: Exception) { defaultTermStart }

        val daysDiff = ChronoUnit.DAYS.between(termStartDate, today)
        val currentActualWeek = ((daysDiff / 7).toInt() + 1).coerceIn(1, activeTimetable?.totalWeeks ?: 20)
        val todayDayOfWeek = today.dayOfWeek.value

        return activeTimetable.courses.filter {
            it.dayOfWeek == todayDayOfWeek && it.weekList.contains(currentActualWeek)
        }.sortedBy { it.startNode }
    }
}