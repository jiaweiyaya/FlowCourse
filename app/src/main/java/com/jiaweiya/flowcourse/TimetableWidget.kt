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
import com.jiaweiya.flowcourse.nodeTimes
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class TimetableWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TimetableWidget()
}

class RefreshWidgetAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        TimetableWidget().updateAll(context)
    }
}

class TimetableWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val todayCourses = getTodayCourses(context)
        val dateInfo = getTodayDateInfo(context)
        val profileNodes = getActiveProfileNodes(context)

        provideContent {
            GlanceTheme {
                WidgetContent(context, dateInfo, todayCourses, profileNodes)
            }
        }
    }

    @Composable
    private fun WidgetContent(
        context: Context,
        dateInfo: String,
        courses: List<Course>,
        profileNodes: List<NodeTime>
    ) {
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ColorProvider(day = Color.White, night = Color(0xFF1E1E1E)))
                .padding(8.dp)
                .clickable(actionStartActivity(openAppIntent))
        ) {
            Row(
                modifier = GlanceModifier.fillMaxWidth().padding(bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dateInfo,
                    style = TextStyle(
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = ColorProvider(day = Color.Black, night = Color.White)
                    ),
                    modifier = GlanceModifier.defaultWeight().padding(start = 8.dp)
                )
                Image(
                    provider = ImageProvider(android.R.drawable.ic_popup_sync),
                    contentDescription = "刷新",
                    modifier = GlanceModifier.size(22.dp)
                        .clickable(actionRunCallback<RefreshWidgetAction>()),
                    colorFilter = ColorFilter.tint(
                        ColorProvider(
                            day = Color.DarkGray,
                            night = Color(0xFFDDDDDD)
                        )
                    )
                )
            }

            if (courses.isEmpty()) {
                Box(modifier = GlanceModifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "今天没有课，好好休息！",
                        style = TextStyle(fontSize = 13.sp, color = ColorProvider(Color.Gray))
                    )
                }
            } else {
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

        val startTime = profileNodes.getOrNull(course.startNode - 1)?.start ?: ""
        val endTime = profileNodes.getOrNull(course.endNode - 1)?.end ?: ""

        Column(
            modifier = GlanceModifier
                .fillMaxWidth()
                .padding(bottom = 5.dp) // 卡片间的垂直间距
        ) {
            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .background(ColorProvider(Color(course.bgColor)))
                    .cornerRadius(10.dp)
                    .padding(
                        start = 10.dp,
                        end = 10.dp,
                        top = 6.dp,
                        bottom = 6.dp
                    )
                    .clickable(actionStartActivity(openAppIntent)),

                verticalAlignment = Alignment.CenterVertically
            ) {
                // 左侧：时间区
                Column(
                    modifier = GlanceModifier.width(46.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = startTime,
                        style = TextStyle(
                            fontSize = 9.sp,
                            color = ColorProvider(Color(course.textColor))
                        )
                    )
                    Text(
                        text = "${course.startNode}-${course.endNode}",
                        style = TextStyle(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = ColorProvider(Color(course.textColor))
                        )
                    )
                    Text(
                        text = endTime,
                        style = TextStyle(
                            fontSize = 9.sp,
                            color = ColorProvider(Color(course.textColor))
                        )
                    )
                }

                Spacer(modifier = GlanceModifier.width(8.dp))

                // 中间：课程与地点
                Column(modifier = GlanceModifier.defaultWeight()) {
                    Text(
                        text = course.name,
                        style = TextStyle(
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = ColorProvider(Color(course.textColor))
                        ),
                        maxLines = 2
                    )
                    Spacer(modifier = GlanceModifier.height(2.dp))
                    Text(
                        text = course.room,
                        style = TextStyle(
                            fontSize = 10.sp,
                            color = ColorProvider(Color(course.textColor))
                        ),
                        maxLines = 2
                    )
                }

                Spacer(modifier = GlanceModifier.width(4.dp))

                // 右侧：垂直教师名字
                val verticalTeacher = course.teacher.map { it.toString() }.joinToString("\n")
                Text(
                    text = verticalTeacher,
                    style = TextStyle(
                        fontSize = 10.sp,
                        color = ColorProvider(Color(course.textColor)),
                        textAlign = TextAlign.Center
                    ),
                    modifier = GlanceModifier.padding(start = 2.dp)
                )
            }
        }
    }

    private fun getActiveProfileNodes(context: Context): List<NodeTime> {
        val sharedPrefs = context.getSharedPreferences("FlowCourseDB", Context.MODE_PRIVATE)
        val gson = Gson()
        val timetablesJson = sharedPrefs.getString("timetables_data", null)
        val activeId = sharedPrefs.getInt("active_id", 1)
        var profileId = 1
        try {
            val type = object : TypeToken<List<TimetableData>>() {}.type
            val timetables = gson.fromJson<List<TimetableData>>(timetablesJson, type) ?: emptyList()
            profileId = timetables.find { it.id == activeId }?.timeProfileId ?: 1
        } catch (e: Exception) {
        }

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
        } catch (e: Exception) {
            emptyList()
        }

        val activeTimetable = timetables.find { it.id == activeId } ?: timetables.firstOrNull()

        val today = LocalDate.now()
        val defaultTermStart = today.minusDays((today.dayOfWeek.value - 1).toLong())
        val termStartDateStr = activeTimetable?.termStart ?: defaultTermStart.toString()
        val termStartDate = try {
            LocalDate.parse(termStartDateStr)
        } catch (e: Exception) {
            defaultTermStart
        }

        val daysDiff = ChronoUnit.DAYS.between(termStartDate, today)
        val currentActualWeek =
            ((daysDiff / 7).toInt() + 1).coerceIn(1, activeTimetable?.totalWeeks ?: 20)

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
        } catch (e: Exception) {
            return emptyList()
        }

        val activeTimetable = timetables.find { it.id == activeId } ?: return emptyList()

        val today = LocalDate.now()
        val defaultTermStart = today.minusDays((today.dayOfWeek.value - 1).toLong())
        val termStartDateStr = activeTimetable?.termStart ?: defaultTermStart.toString()
        val termStartDate = try {
            LocalDate.parse(termStartDateStr)
        } catch (e: Exception) {
            defaultTermStart
        }

        val daysDiff = ChronoUnit.DAYS.between(termStartDate, today)
        val currentActualWeek =
            ((daysDiff / 7).toInt() + 1).coerceIn(1, activeTimetable?.totalWeeks ?: 20)
        val todayDayOfWeek = today.dayOfWeek.value

        return activeTimetable.courses.filter {
            it.dayOfWeek == todayDayOfWeek && it.weekList.contains(currentActualWeek)
        }.sortedBy { it.startNode }
    }
}