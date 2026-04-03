package com.jiaweiya.flowcourse.parser

import android.content.Context
import android.net.Uri
import com.jiaweiya.flowcourse.Course
import java.io.BufferedReader
import java.io.InputStreamReader

object CqwlxyParser {
    fun parseCourseFromHtml(content: String): List<Course> {
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
        return mergeCourses(courses)
    }

    fun parseCourseFromFile(context: Context, uri: Uri): List<Course> {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return emptyList()
            val reader = BufferedReader(InputStreamReader(inputStream, "UTF-8"))
            val content = reader.readText()
            reader.close()
            parseCourseFromHtml(content)
        } catch (e: Exception) { e.printStackTrace(); emptyList() }
    }

    private fun mergeCourses(courses: List<Course>): List<Course> {
        var current = courses
        var changed = true
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
                        val sameNodes = c1.startNode == c2.startNode && c1.endNode == c2.endNode
                        var overlappingOrAdjacentWeeks = false
                        if (sameNodes) {
                            for (w1 in c1.weekList) {
                                for (w2 in c2.weekList) {
                                    if (w1 - w2 in -1..1) {
                                        overlappingOrAdjacentWeeks = true
                                        break
                                    }
                                }
                                if (overlappingOrAdjacentWeeks) break
                            }
                        }

                        if ((sameWeeks && overlappingOrAdjacentNodes) || (sameNodes && overlappingOrAdjacentWeeks)) {
                            c1 = c1.copy(
                                startNode = minOf(c1.startNode, c2.startNode),
                                endNode = maxOf(c1.endNode, c2.endNode),
                                weekList = (c1.weekList + c2.weekList).distinct().sorted()
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

    private fun parseWeeks(weeksStr: String): List<Int> {
        val weeks = mutableListOf<Int>()
        val parts = weeksStr.split(",")
        for (p in parts) {
            if (p.contains("-")) {
                val bounds = p.split("-")
                val start = bounds[0].toIntOrNull()
                val end = bounds[1].toIntOrNull()
                if (start != null && end != null) weeks.addAll(start..end)
            } else {
                p.toIntOrNull()?.let { weeks.add(it) }
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

    fun getAutoFillScript(url: String, username: String, password: String): String? {
        // 根据截图特征，判断当前是否为重庆文理学院的 WebVPN 或 统一身份认证登录页
        val isTargetLoginPage = url.contains("myvpn.cqwu.edu.cn") && url.contains("authserver/login")

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
                        
                        // 触发 input 和 change 事件，防止现代前端框架(Vue/React)检测不到值变动导致登录按钮灰色
                        var eventInput = new Event('input', { bubbles: true });
                        var eventChange = new Event('change', { bubbles: true });
                        userField.dispatchEvent(eventInput);
                        userField.dispatchEvent(eventChange);
                        passField.dispatchEvent(eventInput);
                        passField.dispatchEvent(eventChange);
                    }
                })();
            """.trimIndent()
        }

        // 如果不是目标页面，返回 null，浏览器不会执行任何注入
        return null
    }
}