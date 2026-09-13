package com.jiaweiya.flowcourse.parser

import android.content.Context
import android.net.Uri
import com.jiaweiya.flowcourse.Course
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader

// 解析异常信息封装数据类
data class ParseAnomaly(
    val typeKey: String, // 警告类型标识键，用于偏好设置及一键关闭
    val title: String,
    val reason: String,
    val usefulVariables: Map<String, String>,
    val rawCourseJson: String
)

object CqwlxyParser {
    fun parseCourseFromHtml(
        content: String,
        log: ((String) -> Unit)? = null,
        autoMergeAdjacent: Boolean = true,
        onAnomaly: ((ParseAnomaly) -> Unit)? = null
    ): List<Course> {
        val trimmed = content.trim()
        if (trimmed.startsWith("{") && (trimmed.contains("getMyScheduleDetail") || trimmed.contains("arrangedList"))) {
            return parseCourseFromJson(trimmed, log, autoMergeAdjacent, onAnomaly)
        }
        log?.invoke("[解析] 尝试从HTML中匹配表格...")
        val courses = mutableListOf<Course>()
        try {
            val trRegex = Regex("<tr.*?>([\\s\\S]*?)</tr>", setOf(RegexOption.IGNORE_CASE))
            val trMatches = trRegex.findAll(content)

            val colors = listOf(0xFFE3F2FD, 0xFFF3E5F5, 0xFFE8F5E9, 0xFFFFF3E0, 0xFFFFEBEE, 0xFFE0F7FA, 0xFFFBE9E7, 0xFFF0F4C3, 0xFFEDE7F6, 0xFFE8EAF6).map { it.toLong() }
            val courseColors = mutableMapOf<String, Long>()

            for (tr in trMatches) {
                val trContent = tr.groupValues[1]
                val tdRegex = Regex("<td.*?>([\\s\\S]*?)</td>", setOf(RegexOption.IGNORE_CASE))
                val tdMatches = tdRegex.findAll(trContent).toList()

                if (tdMatches.size >= 7) {
                    val daysTds = tdMatches.takeLast(7)
                    for ((dayIndex, td) in daysTds.withIndex()) {
                        val dayOfWeek = dayIndex + 1
                        val divRegex = Regex("<div[^>]*>([\\s\\S]*?)</div>", setOf(RegexOption.IGNORE_CASE))
                        val divMatches = divRegex.findAll(td.groupValues[1])

                        for (div in divMatches) {
                            val divContent = div.groupValues[1]
                            val parts = divContent.split(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE))
                                .map { it.replace(Regex("<[^>]*>"), "").trim() }
                                .filter { it.isNotEmpty() }

                            if (parts.size >= 4) {
                                val name = parts[0]
                                val teacher = parts[1]
                                val timeInfo = parts[2]
                                val room = parts[3]
                                val color = courseColors.getOrPut(name) { colors.random() }

                                val bracketIndex = timeInfo.indexOf('[')
                                if (bracketIndex != -1) {
                                    val weeksStr = timeInfo.substring(0, bracketIndex)
                                    val nodesStr = timeInfo.substring(bracketIndex + 1).removeSuffix("]")
                                    val weekList = parseWeeks(weeksStr)
                                    val (startNode, endNode) = parseNodes(nodesStr)

                                    courses.add(
                                        Course(
                                            id = 0, name = name, room = room, teacher = teacher,
                                            dayOfWeek = dayOfWeek, startNode = startNode, endNode = endNode,
                                            weekList = weekList, bgColor = color, textColor = 0xFF000000
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) { e.printStackTrace() }
        return if (autoMergeAdjacent) {
            mergeAdjacentCourses(courses, emptyMap(), onAnomaly)
        } else {
            mergeCourses(courses, emptyMap(), onAnomaly)
        }
    }

    fun parseCourseFromJson(
        jsonStr: String,
        log: ((String) -> Unit)? = null,
        autoMergeAdjacent: Boolean = true,
        onAnomaly: ((ParseAnomaly) -> Unit)? = null
    ): List<Course> {
        val courses = mutableListOf<Course>()
        val itemRawJsonMap = mutableMapOf<String, String>()
        try {
            log?.invoke("[JSON解析] 开始解析课程JSON结构，数据长度: " + jsonStr.length)
            val root = JSONObject(jsonStr)
            val datas = root.optJSONObject("datas")
            if (datas == null) {
                log?.invoke("[JSON解析错误] JSON中未找到datas节点，内容可能为错误提示: " + jsonStr.take(150))
                return emptyList()
            }
            val getMyScheduleDetail = datas.optJSONObject("getMyScheduleDetail")
            if (getMyScheduleDetail == null) {
                log?.invoke("[JSON解析错误] datas中未找到getMyScheduleDetail节点")
                return emptyList()
            }
            val arrangedList = getMyScheduleDetail.optJSONArray("arrangedList")
            if (arrangedList == null || arrangedList.length() == 0) {
                log?.invoke("[JSON解析提示] arrangedList排课列表为空(长度为0)，请检查学年学期参数或页面是否已生成课表")
                return emptyList()
            }

            log?.invoke("[JSON解析] arrangedList获取成功，包含 " + arrangedList.length() + " 项排课条目")

            val colors = listOf(0xFFE3F2FD, 0xFFF3E5F5, 0xFFE8F5E9, 0xFFFFF3E0, 0xFFFFEBEE, 0xFFE0F7FA, 0xFFFBE9E7, 0xFFF0F4C3, 0xFFEDE7F6, 0xFFE8EAF6).map { it.toLong() }
            val courseColors = mutableMapOf<String, Long>()

            for (i in 0 until arrangedList.length()) {
                val item = arrangedList.optJSONObject(i) ?: continue
                val courseName = item.optString("courseName").trim()
                if (courseName.isEmpty()) continue

                val dayOfWeek = item.optInt("dayOfWeek", -1)
                if (dayOfWeek !in 1..7) continue

                val beginTime = item.optString("beginTime")
                val endTime = item.optString("endTime")
                val beginSection = if (item.has("beginSection") && !item.isNull("beginSection")) item.optInt("beginSection") else null
                val endSection = if (item.has("endSection") && !item.isNull("endSection")) item.optInt("endSection") else null

                val (startNode, endNode) = mapTimeToNodes(beginTime, endTime, beginSection, endSection)
                val credit = item.optString("credit", "")
                val color = courseColors.getOrPut(courseName) { colors.random() }

                val rawWeeksAndTeachers = item.optString("weeksAndTeachers").trim()
                val rawPlaceName = item.optString("placeName").trim()
                val fallbackTeacher = extractTeacherFromHtml(item)

                val rawItemFormattedJson = item.toString(4)
                itemRawJsonMap[courseName] = rawItemFormattedJson

                // 异常检测 1：教师姓名疑似缺失或为空
                val directTeacher = parseTeacher(rawWeeksAndTeachers)
                if (directTeacher.isEmpty()) {
                    if (fallbackTeacher.isNotEmpty()) {
                        onAnomaly?.invoke(
                            ParseAnomaly(
                                typeKey = "warn_teacher_missing",
                                title = "任课教师字段缺失（已从HTML标签中修复）",
                                reason = "教务接口的 weeksAndTeachers 字段在斜杠后未提供教师姓名（原文: '$rawWeeksAndTeachers'），系统已自动从 cellDetail 标签中恢复识别教师为: '$fallbackTeacher'。",
                                usefulVariables = mapOf(
                                    "课程名称" to courseName,
                                    "星期/节次" to "星期$dayOfWeek 第${startNode}-${endNode}节",
                                    "weeksAndTeachers原文" to rawWeeksAndTeachers,
                                    "最终提取到的教师" to fallbackTeacher,
                                    "placeName原文" to rawPlaceName
                                ),
                                rawCourseJson = rawItemFormattedJson
                            )
                        )
                    } else {
                        onAnomaly?.invoke(
                            ParseAnomaly(
                                typeKey = "warn_teacher_missing",
                                title = "任课教师解析为空",
                                reason = "接口返回的 weeksAndTeachers 字段中未包含教师信息，且 HTML 标签中亦未找到任课教师姓名。",
                                usefulVariables = mapOf(
                                    "课程名称" to courseName,
                                    "星期/节次" to "星期$dayOfWeek 第${startNode}-${endNode}节",
                                    "weeksAndTeachers原文" to rawWeeksAndTeachers,
                                    "placeName原文" to rawPlaceName
                                ),
                                rawCourseJson = rawItemFormattedJson
                            )
                        )
                    }
                }

                // 拆分以分号连接的多个排课安排（例如不同的教室与对应周次）
                val wtParts = if (rawWeeksAndTeachers.contains(";")) {
                    rawWeeksAndTeachers.split(";").map { it.trim() }.filter { it.isNotEmpty() }
                } else if (rawWeeksAndTeachers.isNotEmpty()) {
                    listOf(rawWeeksAndTeachers)
                } else emptyList()

                val placeParts = if (rawPlaceName.contains(";")) {
                    rawPlaceName.split(";").map { it.trim() }.filter { it.isNotEmpty() }
                } else if (rawPlaceName.isNotEmpty()) {
                    listOf(rawPlaceName)
                } else emptyList()

                val subCount = maxOf(wtParts.size, placeParts.size)

                // 异常检测 2：多地点/多阶段排课记录（提醒已自动分离绑定）
                if (subCount > 1) {
                    onAnomaly?.invoke(
                        ParseAnomaly(
                            typeKey = "warn_multi_location",
                            title = "检测到多地点/多阶段排课（已自动分离绑定）",
                            reason = "该课程包含分号分隔的多教室或多周次配置（地点: '$rawPlaceName'，周次: '$rawWeeksAndTeachers'）。系统已自动拆分为独立子课程，避免不同周次的教室混淆。",
                            usefulVariables = mapOf(
                                "课程名称" to courseName,
                                "星期/节次" to "星期$dayOfWeek 第${startNode}-${endNode}节",
                                "涉及地点" to rawPlaceName,
                                "周次安排" to rawWeeksAndTeachers
                            ),
                            rawCourseJson = rawItemFormattedJson
                        )
                    )
                }

                // 异常检测 3：检测到辅讲教师单独排课记录
                if (rawWeeksAndTeachers.contains("辅讲")) {
                    onAnomaly?.invoke(
                        ParseAnomaly(
                            typeKey = "warn_co_teachers",
                            title = "检测到辅讲教师排课记录",
                            reason = "教务接口返回了带有[辅讲]标记的独立排课条目（'$rawWeeksAndTeachers'）。系统已自动将其归纳，防止与主讲课程发生冲突。",
                            usefulVariables = mapOf(
                                "课程名称" to courseName,
                                "星期/节次" to "星期$dayOfWeek 第${startNode}-${endNode}节",
                                "教师信息" to (directTeacher.ifEmpty { fallbackTeacher }),
                                "上课地点" to rawPlaceName
                            ),
                            rawCourseJson = rawItemFormattedJson
                        )
                    )
                }

                if (subCount > 1) {
                    for (k in 0 until subCount) {
                        val subWt = wtParts.getOrNull(k) ?: wtParts.firstOrNull() ?: ""
                        val subPlace = placeParts.getOrNull(k) ?: placeParts.firstOrNull() ?: ""
                        val subTeacher = parseTeacher(subWt).ifEmpty { fallbackTeacher }

                        val subWeeks = parseWeeks(subWt)
                        if (subWeeks.isEmpty()) continue

                        // 将不连续的周次切分为多个连续段，且严格绑定当前子教室
                        val continuousSegments = splitContinuousWeeks(subWeeks)
                        for (segWeeks in continuousSegments) {
                            courses.add(
                                Course(
                                    id = 0,
                                    name = courseName,
                                    room = subPlace,
                                    teacher = subTeacher,
                                    dayOfWeek = dayOfWeek,
                                    startNode = startNode,
                                    endNode = endNode,
                                    weekList = segWeeks,
                                    bgColor = color,
                                    textColor = 0xFF000000,
                                    credits = credit
                                )
                            )
                        }
                    }
                } else {
                    val teacher = parseTeacher(rawWeeksAndTeachers).ifEmpty { fallbackTeacher }
                    val room = parseRoom(rawPlaceName)

                    var weekList = if (rawWeeksAndTeachers.isNotEmpty()) parseWeeks(rawWeeksAndTeachers) else emptyList()
                    if (weekList.isEmpty()) {
                        weekList = parseWeeksFromMask(item.optString("week"))
                    }
                    if (weekList.isEmpty()) continue

                    // 将不连续周次拆分为连续段
                    val continuousSegments = splitContinuousWeeks(weekList)

                    // 异常检测 4：非连续周次断档（提醒已切分为连续段）
                    if (continuousSegments.size > 1) {
                        onAnomaly?.invoke(
                            ParseAnomaly(
                                typeKey = "warn_discontinuous_weeks",
                                title = "检测到非连续周次排课（已切分为连续段）",
                                reason = "该课程上课周次存在空周断档（如 $rawWeeksAndTeachers）。系统已将其自动切分为 ${continuousSegments.size} 段独立的连续周次，避免合在一起导致周数显示错误。",
                                usefulVariables = mapOf(
                                    "课程名称" to courseName,
                                    "星期/节次" to "星期$dayOfWeek 第${startNode}-${endNode}节",
                                    "原始周次文本" to rawWeeksAndTeachers,
                                    "切分后的各阶段" to continuousSegments.joinToString(" ; ") { "第${it.first()}-${it.last()}周(共${it.size}周)" },
                                    "上课教室" to room
                                ),
                                rawCourseJson = rawItemFormattedJson
                            )
                        )
                    }

                    for (segWeeks in continuousSegments) {
                        courses.add(
                            Course(
                                id = 0,
                                name = courseName,
                                room = room,
                                teacher = teacher,
                                dayOfWeek = dayOfWeek,
                                startNode = startNode,
                                endNode = endNode,
                                weekList = segWeeks,
                                bgColor = color,
                                textColor = 0xFF000000,
                                credits = credit
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            log?.invoke("[JSON解析异常] " + e.message)
        }
        val merged = if (autoMergeAdjacent) {
            mergeAdjacentCourses(courses, itemRawJsonMap, onAnomaly)
        } else {
            mergeCourses(courses, itemRawJsonMap, onAnomaly)
        }
        log?.invoke("[JSON解析完成] 最终有效生成课程门数: " + merged.size)
        return merged
    }

    private fun mapTimeToNodes(beginTime: String?, endTime: String?, beginSection: Int?, endSection: Int?): Pair<Int, Int> {
        val startFromTime = when (beginTime?.trim()) {
            "08:10" -> 1
            "09:05" -> 2
            "10:20" -> 3
            "11:15" -> 4
            "12:15" -> 5
            "13:00" -> 6
            "13:45" -> 7
            "14:30" -> 8
            "15:25" -> 9
            "16:20" -> 10
            "17:15" -> 11
            "18:00", "18:25" -> 12
            "19:20" -> 13
            "20:15" -> 14
            "21:10" -> 15
            "22:05" -> 16
            else -> null
        }

        val endFromTime = when (endTime?.trim()) {
            "08:55" -> 1
            "09:50" -> 2
            "11:05" -> 3
            "12:00" -> 4
            "13:00" -> 5
            "13:45" -> 6
            "14:30" -> 7
            "15:15" -> 8
            "16:10" -> 9
            "17:05" -> 10
            "18:00" -> 11
            "19:10", "19:20" -> 12
            "20:05" -> 13
            "21:00" -> 14
            "21:55" -> 15
            "22:50" -> 16
            else -> null
        }

        if (startFromTime != null && endFromTime != null) {
            return Pair(startFromTime, endFromTime)
        }

        fun sectionToNode(sec: Int): Int = when (sec) {
            1 -> 1; 2 -> 2; 3 -> 3; 4 -> 4
            5 -> 8; 6 -> 9; 7 -> 10; 8 -> 11
            9 -> 13; 10 -> 14; 11 -> 15; 12 -> 16
            else -> sec.coerceIn(1, 16)
        }

        val s = startFromTime ?: (beginSection?.let { sectionToNode(it) } ?: 1)
        val e = endFromTime ?: (endSection?.let { sectionToNode(it) } ?: s)
        return Pair(s, maxOf(s, e))
    }

    private fun parseWeeksFromMask(weekMask: String?): List<Int> {
        if (weekMask.isNullOrEmpty()) return emptyList()
        val list = mutableListOf<Int>()
        for (i in weekMask.indices) {
            if (weekMask[i] == '1') {
                list.add(i + 1)
            }
        }
        return list
    }

    // 将不连续的周次列表切分为若干段连续的周次列表，例如 [1,2,3,4,6,7] 切分为 [[1,2,3,4], [6,7]]
    private fun splitContinuousWeeks(weeks: List<Int>): List<List<Int>> {
        if (weeks.isEmpty()) return emptyList()
        val sorted = weeks.distinct().sorted()
        val result = mutableListOf<MutableList<Int>>()
        var current = mutableListOf<Int>()
        for (w in sorted) {
            if (current.isEmpty() || w == current.last() + 1) {
                current.add(w)
            } else {
                result.add(current)
                current = mutableListOf(w)
            }
        }
        if (current.isNotEmpty()) {
            result.add(current)
        }
        return result
    }

    // 从 cellDetail 或 titleDetail 等 HTML 中兜底提取教师姓名 (data-kblx="02" 为教师)
    private fun extractTeacherFromHtml(item: JSONObject): String {
        val candidates = mutableListOf<String>()
        fun scanArray(arrayName: String) {
            val arr = item.optJSONArray(arrayName) ?: return
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i)
                val text = obj?.optString("text") ?: arr.optString(i, "")
                if (text.isNotEmpty()) {
                    val teacherRegex = Regex("""data-kblx=["']02["'][^>]*>([^<]+)</a>""")
                    teacherRegex.findAll(text).forEach { m ->
                        candidates.add(m.groupValues[1].trim())
                    }
                }
            }
        }
        scanArray("cellDetail")
        scanArray("cellWeekTeacherClassroomDetail")
        scanArray("titleDetail")
        scanArray("titleWeekTeacherClassroomDetail")
        return candidates.filter { it.isNotEmpty() }.distinct().joinToString(", ")
    }

    private fun parseTeacher(weeksAndTeachers: String?): String {
        if (weeksAndTeachers.isNullOrBlank()) return ""
        return weeksAndTeachers.split(";")
            .map { part ->
                val afterSlash = if (part.contains("/")) part.substringAfter("/") else part
                afterSlash.replace(Regex("\\[.*?\\]"), "").trim()
            }
            .filter { it.isNotEmpty() }
            .distinct()
            .joinToString(", ")
    }

    private fun parseRoom(placeName: String?): String {
        if (placeName.isNullOrBlank()) return ""
        return placeName.split(";")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .joinToString("; ")
    }

    fun parseCourseFromFile(context: Context, uri: Uri, autoMergeAdjacent: Boolean = true): List<Course> {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return emptyList()
            val reader = BufferedReader(InputStreamReader(inputStream, "UTF-8"))
            val content = reader.readText()
            reader.close()
            parseCourseFromHtml(content, autoMergeAdjacent = autoMergeAdjacent)
        } catch (e: Exception) { e.printStackTrace(); emptyList() }
    }

    // 合并同一课程时间槽下的多位教师（如主讲 + 辅讲条目）
    private fun mergeCoTeachers(
        courses: List<Course>,
        itemRawJsonMap: Map<String, String> = emptyMap(),
        onAnomaly: ((ParseAnomaly) -> Unit)? = null
    ): List<Course> {
        val mergedList = mutableListOf<Course>()
        val grouped = courses.groupBy { "${it.name}|${it.room}|${it.dayOfWeek}|${it.startNode}|${it.endNode}|${it.weekList.joinToString(",")}" }

        for ((_, group) in grouped) {
            if (group.size == 1) {
                mergedList.add(group.first())
            } else {
                val individualTeachers = group.map { it.teacher }
                    .flatMap { it.split(",") }
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .distinct()

                val combinedTeacher = individualTeachers.joinToString(", ")
                val first = group.first()

                // 异常检测 5：检测到同课程同时间多位教师（如主讲+辅讲）
                val rawJson = itemRawJsonMap[first.name] ?: "{}"
                onAnomaly?.invoke(
                    ParseAnomaly(
                        typeKey = "warn_co_teachers",
                        title = "检测到同课程同时间多位教师（主讲/辅讲）",
                        reason = "教务系统为该课程在相同时间与地点返回了 ${group.size} 条独立排课记录（如主讲与辅讲老师分别建档）。系统已自动合并为同一门课程以防在课表上误标课程冲突。",
                        usefulVariables = mapOf(
                            "课程名称" to first.name,
                            "星期/节次" to "星期${first.dayOfWeek} 第${first.startNode}-${first.endNode}节",
                            "涉及教师" to individualTeachers.joinToString(" 与 "),
                            "合并后教师" to combinedTeacher,
                            "上课地点" to first.room,
                            "上课周次" to "第${first.weekList.joinToString(",")}周"
                        ),
                        rawCourseJson = rawJson
                    )
                )

                mergedList.add(first.copy(teacher = combinedTeacher))
            }
        }
        return mergedList
    }

    private fun mergeAdjacentCourses(
        courses: List<Course>,
        itemRawJsonMap: Map<String, String> = emptyMap(),
        onAnomaly: ((ParseAnomaly) -> Unit)? = null
    ): List<Course> {
        val coTeacherMerged = mergeCoTeachers(courses, itemRawJsonMap, onAnomaly)
        var current = coTeacherMerged
        var changed = true

        // 仅在完全相同的周次区间内，合并相连或重叠的连续节次
        while (changed) {
            changed = false
            val next = mutableListOf<Course>()
            val consumed = BooleanArray(current.size)

            for (i in current.indices) {
                if (consumed[i]) continue
                var c1 = current[i]

                for (j in i + 1 until current.size) {
                    if (consumed[j]) continue
                    val c2 = current[j]

                    if (c1.name == c2.name && c1.room == c2.room && c1.teacher == c2.teacher && c1.dayOfWeek == c2.dayOfWeek) {
                        val sameWeeks = c1.weekList == c2.weekList
                        val overlappingOrAdjacentNodes = c1.startNode <= c2.endNode + 1 && c2.startNode <= c1.endNode + 1

                        if (sameWeeks && overlappingOrAdjacentNodes) {
                            c1 = c1.copy(
                                startNode = minOf(c1.startNode, c2.startNode),
                                endNode = maxOf(c1.endNode, c2.endNode)
                            )
                            consumed[j] = true
                            changed = true
                        }
                    }
                }
                next.add(c1)
            }
            current = next
        }
        return current
    }

    private fun mergeCourses(
        courses: List<Course>,
        itemRawJsonMap: Map<String, String> = emptyMap(),
        onAnomaly: ((ParseAnomaly) -> Unit)? = null
    ): List<Course> {
        return mergeCoTeachers(courses, itemRawJsonMap, onAnomaly)
    }

    private fun parseWeeks(weeksStr: String): List<Int> {
        val weeks = mutableListOf<Int>()
        // 截取周次部分，去除方括号及其后的教师或课程性质说明（如 "[讲授]/张嘉彤"、"[]/李杰"）
        val cleanStr = when {
            weeksStr.contains("[") -> weeksStr.substringBefore("[")
            weeksStr.contains("/") -> weeksStr.substringBefore("/")
            else -> weeksStr
        }.trim()

        val parts = cleanStr.split(Regex("[,，、]"))
        for (p in parts) {
            val trimmed = p.replace("周", "").replace("第", "").trim()
            if (trimmed.contains("-")) {
                val bounds = trimmed.split("-")
                val start = bounds.getOrNull(0)?.filter { it.isDigit() }?.toIntOrNull()
                val end = bounds.getOrNull(1)?.filter { it.isDigit() }?.toIntOrNull()
                if (start != null && end != null) {
                    weeks.addAll(start..end)
                }
            } else {
                val single = trimmed.filter { it.isDigit() }.toIntOrNull()
                if (single != null) {
                    weeks.add(single)
                }
            }
        }
        return weeks.distinct().sorted()
    }

    private fun parseNodes(nodesStr: String): Pair<Int, Int> {
        val parseSingle = { s: String ->
            val str = s.trim().replace("中午", "午").replace("傍晚", "傍")
            when {
                str == "1" -> 1; str == "2" -> 2; str == "3" -> 3; str == "4" -> 4
                str.contains("午1") -> 5; str.contains("午2") -> 6; str.contains("午3") -> 7
                str == "5" -> 8; str == "6" -> 9; str == "7" -> 10; str == "8" -> 11
                str.contains("傍1") -> 12; str == "9" -> 13; str == "10" -> 14; str == "11" -> 15; str == "12" -> 16
                else -> 1
            }
        }
        if (nodesStr.contains("-")) {
            val parts = nodesStr.split("-")
            return Pair(parseSingle(parts[0]), parseSingle(parts[1]))
        }
        val single = parseSingle(nodesStr)
        return Pair(single, single)
    }

    fun getAutoFillScript(url: String, username: String, password: String, autoLogin: Boolean): String? {

        val isTargetLoginPage = url.contains("cqwu.edu.cn") && url.contains("authserver/login")

        if (isTargetLoginPage) {
            return """
                javascript:(function() {
                    var un = '$username';
                    var pw = '$password';
                    var userField = document.getElementById('username') || document.getElementById('yhm') || document.querySelector('input[type="text"]:not([readonly])');
                    var passField = document.getElementById('password') || document.getElementById('mm') || document.querySelector('input[type="password"]');
                    
                    if (userField && passField) {
                        userField.value = un;
                        passField.value = pw;
                        
                        var eventInput = new Event('input', { bubbles: true });
                        var eventChange = new Event('change', { bubbles: true });
                        userField.dispatchEvent(eventInput);
                        userField.dispatchEvent(eventChange);
                        passField.dispatchEvent(eventInput);
                        passField.dispatchEvent(eventChange);
                        
                        // 判断是否自动登录
                        if ($autoLogin) {
                            setTimeout(function() {
                                // 模拟按下回车键
                                var enterEvent = new KeyboardEvent('keydown', { bubbles: true, cancelable: true, keyCode: 13 });
                                passField.dispatchEvent(enterEvent);
                                
                                // 以防回车无效，顺便尝试点击常见的登录按钮
                                var loginBtn = document.getElementById('login_submit') || document.querySelector('.login_btn') || document.querySelector('button[type="submit"]');
                                if (loginBtn) loginBtn.click();
                            }, 300); // 延迟300毫秒等密码真正响应到页面上
                        }
                    }
                })();
            """.trimIndent()
        }
        return null
    }

    fun getAutoNavigateScript(url: String, autoNavigate: Boolean): String? {
        if (!autoNavigate) return null

        val isHomeApp = url.contains("jwapp/sys/homeapp/home/index.html")
        if (isHomeApp) {
            return """
            javascript:(function() {
                console.log('[JS] 检测到教务主页，正在跳转到课表应用...');
                var basePath = window.location.href.split('/jwapp/')[0] + '/jwapp';
                var targetUrl = basePath + '/sys/kbapp/*default/index.do';
                setTimeout(function() {
                    window.location.replace(targetUrl);
                }, 300);
            })();
            """.trimIndent()
        }

        val isPortalPage = url.contains("cqwu.edu.cn") && url.contains("new/index.html")
        if (isPortalPage) {
            return """
            javascript:(function() {
                console.log('[JS] 处于门户主页，正在自适应跳转新版教务服务...');
                var targetUrl = '';
                if (window.location.href.indexOf('myvpn.cqwu.edu.cn') > -1 || window.location.href.indexOf('/webvpn/') > -1) {
                    var vpnOrigin = window.location.origin;
                    targetUrl = vpnOrigin + '/webvpn/LjE1My4xNzAuMTcyLjE2My4xNjk=/LjE1NS4xNzMuMTU4LjE3MC4xMDAuMTU1LjE2NS4xNjguMTcwLjEwMi4xOTguMTQ5LjE2NS45Ni4xNTMuMTY1/jwapp/sys/homeapp/home/index.html?&av=&contextPath=/jwapp';
                } else {
                    targetUrl = 'https://jwfw.cqwu.edu.cn/jwapp/sys/homeapp/home/index.html?av=&contextPath=/jwapp#/';
                }
                console.log('[JS] 准备自适应跳转到: ' + targetUrl);
                setTimeout(function() {
                    window.location.replace(targetUrl);
                }, 300);
            })();
            """.trimIndent()
        }
        return null
    }

    // 静默自动更新专用的多级路由跳转脚本
    fun getSilentAutoNavigateScript(url: String): String? {
        val isHomeApp = url.contains("jwapp/sys/homeapp/home/index.html")
        if (isHomeApp) {
            return """
            javascript:(function() {
                console.log('[JS] [静默] 处于教务服务主页，开始跳转至我的课表应用...');
                var basePath = window.location.href.split('/jwapp/')[0] + '/jwapp';
                var targetUrl = basePath + '/sys/kbapp/*default/index.do';
                setTimeout(function() { window.location.replace(targetUrl); }, 300);
            })();
            """.trimIndent()
        }

        val isPortalPage = url.contains("cqwu.edu.cn") && url.contains("new/index.html")
        if (isPortalPage) {
            return """
            javascript:(function() {
                console.log('[JS] [静默] 处于门户主页，正在自适应跳转新版教务服务...');
                var targetUrl = '';
                if (window.location.href.indexOf('myvpn.cqwu.edu.cn') > -1 || window.location.href.indexOf('/webvpn/') > -1) {
                    var vpnOrigin = window.location.origin;
                    targetUrl = vpnOrigin + '/webvpn/LjE1My4xNzAuMTcyLjE2My4xNjk=/LjE1NS4xNzMuMTU4LjE3MC4xMDAuMTU1LjE2NS4xNjguMTcwLjEwMi4xOTguMTQ5LjE2NS45Ni4xNTMuMTY1/jwapp/sys/homeapp/home/index.html?&av=&contextPath=/jwapp';
                } else {
                    targetUrl = 'https://jwfw.cqwu.edu.cn/jwapp/sys/homeapp/home/index.html?av=&contextPath=/jwapp#/';
                }
                console.log('[JS] [静默] 准备自适应跳转到: ' + targetUrl);
                setTimeout(function() {
                    window.location.replace(targetUrl);
                }, 300);
            })();
            """.trimIndent()
        }
        return null
    }

    // 静默自动更新专用的提取课表与回传脚本
    fun getSilentExtractScript(url: String): String? {
        val isKbApp = url.contains("jwapp/sys/kbapp")
        if (isKbApp) {
            return """
            javascript:(function() {
                console.log('[JS] [静默] 已抵达课表应用，正在准备获取课程数据...');

                // 1. 如果监听钩子已经缓存了课表，直接回传
                if (window.__FC_SCHEDULE_DATA__) {
                    console.log('[JS] [静默] 命中已缓存的课表数据，直接回传');
                    if (window.AndroidBridge) {
                        window.AndroidBridge.onTimetableExtracted(window.__FC_SCHEDULE_DATA__);
                    }
                    return;
                }

                // 2. 轮询检测：等待页面自身请求完成，或者嗅探学期主动请求
                var retryCount = 0;
                var timer = setInterval(function() {
                    retryCount++;
                    if (window.__FC_SCHEDULE_DATA__) {
                        clearInterval(timer);
                        console.log('[JS] [静默] 自动监听捕获到课表数据，正在回传...');
                        if (window.AndroidBridge) {
                            window.AndroidBridge.onTimetableExtracted(window.__FC_SCHEDULE_DATA__);
                        }
                        return;
                    }

                    // 尝试主动嗅探学年学期代码
                    var xnxqdm = '';
                    try {
                        xnxqdm = sessionStorage.getItem('XNXQDM') || '';
                        if (!xnxqdm) {
                            var el = document.querySelector('[data-name="XNXQDM"]') || document.querySelector('input[name="XNXQDM"]');
                            if (el && el.value) xnxqdm = el.value;
                        }
                        if (!xnxqdm) {
                            var termElem = document.querySelector('.kbappTimeXQText');
                            var textToScan = termElem ? termElem.innerText : (document.body ? document.body.innerText : '');
                            var cnMatch = textToScan.match(/(\d{4}-\d{4})\s*学年\s*第([一二三123])学期/);
                            if (cnMatch) {
                                var xqNum = (cnMatch[2] === '一' || cnMatch[2] === '1') ? '1' : ((cnMatch[2] === '二' || cnMatch[2] === '2') ? '2' : '3');
                                xnxqdm = cnMatch[1] + '-' + xqNum;
                            } else {
                                var match = textToScan.match(/\d{4}-\d{4}-[123]/);
                                if (match) xnxqdm = match[0];
                            }
                        }
                    } catch(e) {}

                    // 如果找到了学期或者重试达到3秒(6次)，发起带有学期参数的主动请求
                    if (xnxqdm || retryCount >= 6) {
                        clearInterval(timer);

                        function fetchSchedule(finalTerm) {
                            console.log('[JS] [静默] 正在主动拉取课表数据，学期代码: ' + finalTerm);
                            var basePath = window.location.href.split('/jwapp/')[0] + '/jwapp';
                            var targetUrl = basePath + '/sys/kbapp/api/wdkbcx/getMyScheduleDetail.do';

                            fetch(targetUrl, {
                                method: 'POST',
                                headers: {
                                    'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8',
                                    'X-Requested-With': 'XMLHttpRequest'
                                },
                                body: 'XNXQDM=' + encodeURIComponent(finalTerm) + '&XQDM='
                            })
                            .then(function(res) { return res.text(); })
                            .then(function(text) {
                                console.log('[JS] [静默] 接口返回数据，正在回传...');
                                if (window.AndroidBridge) {
                                    window.AndroidBridge.onTimetableExtracted(text);
                                }
                            })
                            .catch(function(err) {
                                console.log('[JS] [静默] 提取课表接口异常: ' + err);
                            });
                        }

                        if (xnxqdm) {
                            fetchSchedule(xnxqdm);
                        } else {
                            var basePath = window.location.href.split('/jwapp/')[0] + '/jwapp';
                            var defaultTermUrl = basePath + '/sys/jwpubapp/modules/gg/cxmrxnxq.do';
                            fetch(defaultTermUrl, {
                                method: 'POST',
                                headers: {
                                    'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8',
                                    'X-Requested-With': 'XMLHttpRequest'
                                },
                                body: 'CSDM=SYS&ZCSDM=DQXNXQDM&SFSY=1'
                            })
                            .then(function(res) { return res.json(); })
                            .then(function(data) {
                                var termFromApi = '';
                                try {
                                    termFromApi = data.datas.cxmrxnxq.rows[0].XNXQDM;
                                } catch(e) {}
                                fetchSchedule(termFromApi || '');
                            })
                            .catch(function() {
                                fetchSchedule('');
                            });
                        }
                    }
                }, 500);
            })();
            """.trimIndent()
        }
        return null
    }
}