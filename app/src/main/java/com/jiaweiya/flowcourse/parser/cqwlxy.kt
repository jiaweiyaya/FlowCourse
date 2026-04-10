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

        val isPortalPage = url.contains("cqwu.edu.cn") && url.contains("new/index.html")

        if (isPortalPage) {
            return """
            javascript:(function() {
                console.log('[JS] 正在跳转教学管理系统...');
                var appId = '5299144291521305'; 
                var timestamp = new Date().getTime();
                
                // 获取当前页面的前缀路径
                // 直连：https://ehall.cqwu.edu.cn/new/index.html -> 拿到 https://ehall.cqwu.edu.cn
                // WebVPN：https://webvpn.xxx/http/77726476/new/index.html -> 拿到 https://webvpn.xxx/http/77726476
                var basePath = window.location.href.split('/new/')[0];
                
                // 发送记录请求，携带完整的当前环境上下文
                var reqUrl = basePath + '/jsonp/sendRecUseApp.json?appId=' + appId + '&_=' + timestamp;
                if (typeof jQuery !== 'undefined') {
                    jQuery.ajax({
                        url: reqUrl,
                        type: 'GET',
                        dataType: 'json',
                        success: function() { console.log('记录发送成功'); }
                    });
                } else {
                    fetch(reqUrl);
                }

                // 延迟跳转，利用 basePath 保证在 WebVPN 环境内跳转
                var targetUrl = basePath + '/appShow?appId=' + appId;
                console.log('[JS] 准备自适应跳转到: ' + targetUrl);
                
                setTimeout(function() {
                    // 尝试在当前页覆盖跳转
                    window.location.replace(targetUrl);
                }, 300);
            })();
        """.trimIndent()
        }
        return null
    }

    // 静默自动更新专用的多级路由跳转脚本
    fun getSilentAutoNavigateScript(url: String): String? {
        val isPortalPage = url.contains("cqwu.edu.cn") && url.contains("new/index.html")
        val isJwmisHome = url.contains("cqwu.edu.cn") && url.contains("cqwljw/frame/homes.action")

        // 如果在学生后台页面，静默跳转到教务系统
        if (isPortalPage) {
            return """
            javascript:(function() {
                console.log('[JS] [静默] 处于学生后台，开始跳转至教学管理系统...');
                var appId = '5299144291521305'; 
                var timestamp = new Date().getTime();
                var basePath = window.location.href.split('/new/')[0];
                var reqUrl = basePath + '/jsonp/sendRecUseApp.json?appId=' + appId + '&_=' + timestamp;
                if (typeof jQuery !== 'undefined') {
                    jQuery.ajax({ url: reqUrl, type: 'GET', dataType: 'json' });
                } else {
                    fetch(reqUrl);
                }
                var targetUrl = basePath + '/appShow?appId=' + appId;
                setTimeout(function() { window.location.replace(targetUrl); }, 300);
            })();
            """.trimIndent()
        }

        // 如果已经进入教务系统主页，直接跳跃到课表所在页面
        if (isJwmisHome) {
            return """
            javascript:(function() {
                console.log('[JS] [静默] 进入教学管理系统，跳转至课表页...');
                var basePath = window.location.href.split('/cqwljw/')[0];
                var targetUrl = basePath + '/cqwljw/student/xkjg.wdkb.jsp?menucode=S20301';
                setTimeout(function() { window.location.replace(targetUrl); }, 500);
            })();
            """.trimIndent()
        }
        return null
    }

    // 静默自动更新专用的提取课表与回传脚本
    fun getSilentExtractScript(url: String): String? {
        val isTimetablePage = url.contains("xkjg.wdkb.jsp")

        // 3. 如果在课表页面，轮询表格并主动通过 AndroidBridge 传回安卓端
        if (isTimetablePage) {
            return """
            javascript:(function() {
                console.log('[JS] [静默] 已抵达课表页，等待数据...');
                
                // 深度遍历寻找课表
                function findTable(doc) {
                    if(!doc) return null;
                    var t = doc.getElementById('mytable');
                    if(t) return t.outerHTML;
                    var ts = doc.getElementsByTagName('table');
                    for(var i=0; i<ts.length; i++){
                        if(ts[i].innerText.indexOf('星期一') > -1 && ts[i].innerText.indexOf('星期二') > -1) {
                            return ts[i].outerHTML;
                        }
                    }
                    return null;
                }
                
                function walk(win, depth) {
                    if(depth > 5) return null;
                    try {
                        var r = findTable(win.document);
                        if(r) return r;
                    } catch(e){}
                    try {
                        for(var i=0; i<win.frames.length; i++){
                            var fr = walk(win.frames[i], depth+1);
                            if(fr) return fr;
                        }
                    } catch(e){}
                    return null;
                }

                var checkCount = 0;
                var timer = setInterval(function() {
                    checkCount++;
                    // 利用递归穿透寻找课表的 HTML
                    var tableHtml = walk(window, 0);
                    
                    if (tableHtml) {
                        clearInterval(timer);
                        console.log('✅ [JS] [静默] 提取成功！正在回传...');
                        if (window.AndroidBridge) {
                            window.AndroidBridge.onTimetableExtracted(tableHtml);
                        }
                    } else if (checkCount > 30) {
                        clearInterval(timer);
                        console.log('❌ [JS] [静默] 超时：未找到课表表格');
                    }
                }, 500);
            })();
            """.trimIndent()
        }
        return null
    }
}