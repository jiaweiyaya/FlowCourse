package com.jiaweiya.flowcourse

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.jiaweiya.flowcourse.ui.theme.FlowCourseTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt
import androidx.compose.ui.res.painterResource
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import android.util.Base64
import androidx.compose.foundation.border
import androidx.compose.ui.platform.LocalUriHandler
import java.net.URL
import java.net.HttpURLConnection
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.graphics.luminance
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.rememberDrawerState
import androidx.compose.ui.draw.blur
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.lazy.grid.items

import com.jiaweiya.flowcourse.widget.TimetableWidget
import com.jiaweiya.flowcourse.parser.CqwlxyParser

// 数据结构定义
data class NodeTime(val label: String, val start: String, val end: String, val isVisible: Boolean = true)
data class TimeProfile(val id: Int, val name: String, val nodes: List<NodeTime>)

// GitHub API 数据结构
data class GithubRelease(
    val tag_name: String,
    val body: String,
    val html_url: String
)

val nodeTimes = listOf(
    NodeTime("1", "08:10", "08:55"),
    NodeTime("2", "09:05", "09:50"),
    NodeTime("3", "10:20", "11:05"),
    NodeTime("4", "11:15", "12:00"),
    NodeTime("午1", "12:15", "13:00"),
    NodeTime("午2", "13:00", "13:45"),
    NodeTime("午3", "13:45", "14:30"),
    NodeTime("5", "14:30", "15:15"),
    NodeTime("6", "15:25", "16:10"),
    NodeTime("7", "16:20", "17:05"),
    NodeTime("8", "17:15", "18:00"),
    NodeTime("傍1", "18:00", "19:20", isVisible = false),
    NodeTime("9", "19:20", "20:05"),
    NodeTime("10", "20:15", "21:00"),
    NodeTime("11", "21:10", "21:55"),
    NodeTime("12", "22:05", "22:50")
)

data class TimetableData(
    val id: Int,
    val name: String,
    val courses: List<Course>,
    val termStart: String? = null,
    val timeProfileId: Int = 1,
    val totalWeeks: Int = 20
)

data class Course(
    val id: Int, val name: String, val room: String, val teacher: String,
    val dayOfWeek: Int, val startNode: Int, val endNode: Int, val weekList: List<Int>,
    val bgColor: Long = 0xFFE8EAF6, val textColor: Long = 0xFF000000,
    val credits: String = "", val notes: String = ""
)

val mockCourses = listOf(
    Course(1, "课程样例", "样例教室\nJM622-092^_^", "Jiaweiya", 1, 1, 2, listOf(1, 2, 3, 4, 5), 0xFFE3F2FD, 0xFF000000),
)

// 加密与生成分享数据
fun encodeShareData(context: Context, courses: List<Course>): String {
    val version = try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName
    } catch (e: Exception) { "1.0.0" }

    val json = Gson().toJson(courses)
    val bos = ByteArrayOutputStream()
    // GZip 压缩极大减小体积
    GZIPOutputStream(bos).use { it.write(json.toByteArray(Charsets.UTF_8)) }
    val compressed = Base64.encodeToString(bos.toByteArray(), Base64.NO_WRAP)

    return "这是FlowCourse导出的分享文件，可以通过https://github.com/jiaweiyaya/FlowCourse/releases下载FlowCourse课程表软件来解析和显示课表。分享者软件版本：$version\n---\n$compressed"
}

// 解析与解密分享数据
fun decodeShareData(data: String): List<Course>? {
    return try {
        val parts = data.split("\n---\n")
        val payload = if (parts.size > 1) parts[1].trim() else parts[0].trim()
        val bytes = Base64.decode(payload, Base64.NO_WRAP)
        val bis = ByteArrayInputStream(bytes)
        val json = GZIPInputStream(bis).bufferedReader(Charsets.UTF_8).use { it.readText() }
        val type = object : TypeToken<List<Course>>() {}.type
        Gson().fromJson(json, type)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun updateAppWidget(context: Context) {
    kotlinx.coroutines.GlobalScope.launch(Dispatchers.Main) {
        try {
            TimetableWidget().updateAll(context)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

// 检查更新的网络请求函数
suspend fun checkAppUpdate(currentVersion: String, onResult: (GithubRelease?, Boolean) -> Unit) {
    withContext(Dispatchers.IO) {
        try {
            val url = URL("https://api.github.com/repos/jiaweiyaya/FlowCourse/releases/latest")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            if (connection.responseCode == 200) {
                val json = connection.inputStream.bufferedReader().readText()
                val release = Gson().fromJson(json, GithubRelease::class.java)

                // 提取版本号数字部分进行比较 (去掉 "v" 等前缀)
                val remoteVersion = release.tag_name.replace(Regex("[^0-9.]"), "")
                val localVersion = currentVersion.replace(Regex("[^0-9.]"), "")

                fun toInts(v: String) = v.split(".").map { it.toIntOrNull() ?: 0 }
                val remoteParts = toInts(remoteVersion)
                val localParts = toInts(localVersion)

                var isNewer = false
                for (i in 0 until maxOf(remoteParts.size, localParts.size)) {
                    val r = remoteParts.getOrNull(i) ?: 0
                    val l = localParts.getOrNull(i) ?: 0
                    if (r > l) { isNewer = true; break }
                    if (r < l) { break }
                }

                withContext(Dispatchers.Main) {
                    if (isNewer) onResult(release, false) else onResult(null, true)
                }
            } else {
                withContext(Dispatchers.Main) { onResult(null, false) }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) { onResult(null, false) }
        }
    }
}

@Composable
fun rememberBitmapFromUri(uriString: String?): ImageBitmap? {
    val context = LocalContext.current
    var bitmap by remember(uriString) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(uriString) {
        if (uriString != null) {
            withContext(Dispatchers.IO) {
                try {
                    val uri = Uri.parse(uriString)
                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        android.graphics.BitmapFactory.decodeStream(inputStream)?.let {
                            bitmap = it.asImageBitmap()
                        }
                    }
                } catch (e: Exception) { e.printStackTrace() }
            }
        } else {
            bitmap = null
        }
    }
    return bitmap
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        com.tencent.smtt.sdk.QbSdk.initX5Environment(this, object : com.tencent.smtt.sdk.QbSdk.PreInitCallback {
            override fun onCoreInitFinished() {}
            override fun onViewInitFinished(isX5Core: Boolean) {}
        })

        val sharedPrefs = getSharedPreferences("FlowCourseDB", Context.MODE_PRIVATE)
        val gson = Gson()

        setContent {
            // 设置状态
            var themeMode by remember { mutableIntStateOf(sharedPrefs.getInt("theme_mode", 0)) }
            var showBgImage by remember { mutableStateOf(sharedPrefs.getBoolean("show_bg_image", false)) }
            var bgImageUri by remember { mutableStateOf(sharedPrefs.getString("bg_image_uri", null)) }
            var bgOpacity by remember { mutableFloatStateOf(sharedPrefs.getFloat("bg_opacity", 0.5f)) }
            var defaultBrowserUrl by remember { mutableStateOf(sharedPrefs.getString("default_url", "http://www.cqwu.edu.cn/redir/redirTmp.jsp") ?: "http://www.cqwu.edu.cn/redir/redirTmp.jsp") }
            var desktopWidth by remember { mutableIntStateOf(sharedPrefs.getInt("desktop_width", 1920)) }
            var desktopHeight by remember { mutableIntStateOf(sharedPrefs.getInt("desktop_height", 1080)) }
            var highlightToday by remember { mutableStateOf(sharedPrefs.getBoolean("highlight_today", true)) }
            var showTimeLine by remember { mutableStateOf(sharedPrefs.getBoolean("show_time_line", true)) }
            var showConflictWarning by remember { mutableStateOf(sharedPrefs.getBoolean("show_conflict", true)) }
            var conflictColor by remember { mutableLongStateOf(sharedPrefs.getLong("conflict_color", 0xFFFF0000)) }
            var realTimeSlider by remember { mutableStateOf(sharedPrefs.getBoolean("real_time_slider", false)) }    // 记录是否开启滑块实时更新
            var parserId by remember { mutableIntStateOf(sharedPrefs.getInt("parser_id", 1)) }  // 记录当前选择的课表解析脚本 ID

            var autoCheckUpdate by remember { mutableStateOf(sharedPrefs.getBoolean("auto_check_update", true)) }
            var showWatermark by remember { mutableStateOf(sharedPrefs.getBoolean("show_watermark", true)) } // 控制水印显示
            var updateInfo by remember { mutableStateOf<GithubRelease?>(null) }
            val currentAppVersion = try { packageManager.getPackageInfo(packageName, 0).versionName ?: "1.0.0" } catch (e: Exception) { "1.0.0" }

            var showCourseBorder by remember { mutableStateOf(sharedPrefs.getBoolean("show_course_border", true)) }
            var courseBorderColor by remember { mutableLongStateOf(sharedPrefs.getLong("course_border_color", 0xFF9E77ED)) }

            var autoUsername by remember { mutableStateOf(sharedPrefs.getString("auto_username", "") ?: "") }
            var autoPassword by remember { mutableStateOf(sharedPrefs.getString("auto_password", "") ?: "") }
            var isAutoLoginEnabled by remember { mutableStateOf(sharedPrefs.getBoolean("auto_login", false)) }

            // 用来存储用户选择要展示的冲突课程的 ID 集合
            var preferredConflictIds by remember { mutableStateOf(sharedPrefs.getStringSet("preferred_conflict_ids", emptySet())?.mapNotNull { it.toIntOrNull() }?.toSet() ?: emptySet()) }

            val isSystemDark = isSystemInDarkTheme()
            val useDarkTheme = when (themeMode) {
                1 -> false
                2 -> true
                else -> isSystemDark
            }

            var isFirstLaunch by remember { mutableStateOf(sharedPrefs.getBoolean("is_first_launch", true)) }
            var hasAgreed by remember { mutableStateOf(sharedPrefs.getBoolean("has_agreed", false)) }

            val lifecycleOwner = LocalLifecycleOwner.current
            val context = LocalContext.current
            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    // 当应用暂停或停止（退回桌面、切换多任务、关闭应用）时触发刷新
                    if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                        updateAppWidget(context)
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }

            LaunchedEffect(themeMode, defaultBrowserUrl, desktopWidth, desktopHeight, showBgImage,
                bgImageUri, bgOpacity, highlightToday, showTimeLine, showConflictWarning, conflictColor,
                preferredConflictIds, realTimeSlider, autoCheckUpdate, showWatermark,
                showCourseBorder, courseBorderColor,
                autoUsername, autoPassword, isAutoLoginEnabled
            ) {
                sharedPrefs.edit()
                    .putInt("theme_mode", themeMode)
                    .putBoolean("show_bg_image", showBgImage)
                    .putString("bg_image_uri", bgImageUri)
                    .putFloat("bg_opacity", bgOpacity)
                    .putString("default_url", defaultBrowserUrl)
                    .putString("auto_username", autoUsername)
                    .putString("auto_password", autoPassword)
                    .putBoolean("auto_login", isAutoLoginEnabled)
                    .putInt("desktop_width", desktopWidth)
                    .putInt("desktop_height", desktopHeight)
                    .putBoolean("highlight_today", highlightToday)
                    .putBoolean("show_time_line", showTimeLine)
                    .putBoolean("show_conflict", showConflictWarning)
                    .putLong("conflict_color", conflictColor)
                    .putStringSet("preferred_conflict_ids", preferredConflictIds.map { it.toString() }.toSet())
                    .putBoolean("real_time_slider", realTimeSlider)
                    .putInt("parser_id", parserId)
                    .putBoolean("auto_check_update", autoCheckUpdate)
                    .putBoolean("show_watermark", showWatermark)
                    .putBoolean("show_course_border", showCourseBorder)
                    .putLong("course_border_color", courseBorderColor)
                    .apply()
            }

            FlowCourseTheme(darkTheme = useDarkTheme) {
                val navController = rememberNavController()

                // 获取用于切换到主线程的协程作用域
                val coroutineScope = rememberCoroutineScope()

                var timetables by remember {
                    mutableStateOf<List<TimetableData>>(
                        try {
                            val json = sharedPrefs.getString("timetables_data", null)
                            if (json != null) {
                                val type = object : TypeToken<List<TimetableData>>() {}.type
                                gson.fromJson(json, type) ?: listOf(TimetableData(1, "默认课表", mockCourses))
                            } else listOf(TimetableData(1, "默认课表", mockCourses))
                        } catch (e: Exception) { listOf(TimetableData(1, "默认课表", mockCourses)) }
                    )
                }
                var activeTimetableId by remember { mutableIntStateOf(sharedPrefs.getInt("active_id", 1)) }
                var timeProfiles by remember {
                    mutableStateOf<List<TimeProfile>>(
                        try {
                            val json = sharedPrefs.getString("time_profiles_data", null)
                            if (json != null) {
                                val type = object : TypeToken<List<TimeProfile>>() {}.type
                                gson.fromJson(json, type) ?: listOf(TimeProfile(1, "默认配置", nodeTimes))
                            } else listOf(TimeProfile(1, "默认配置", nodeTimes))
                        } catch (e: Exception) { listOf(TimeProfile(1, "默认配置", nodeTimes)) }
                    )
                }
                val activeTimetable = timetables.find { it.id == activeTimetableId } ?: timetables.firstOrNull()

                // 导入相关的全局弹窗状态
                var pendingImportCourses by remember { mutableStateOf<List<Course>?>(null) }
                val context = LocalContext.current

                // 使用 Unit，使得应用每次打开时只执行一次检查
                LaunchedEffect(Unit) {

                    updateAppWidget(context)

                    if (isFirstLaunch) {
                        // 如果是首次打开，先去介绍页
                        navController.navigate("About")
                    } else if (!hasAgreed) {
                        // 如果已经不是首次，但是还没点过同意，直接弹出协议页
                        navController.navigate("Agreement")
                    }
                    if (autoCheckUpdate) {
                        val todayStr = LocalDate.now().toString()
                        val lastCheckDate = sharedPrefs.getString("last_update_check_date", "")
                        // 每天只在第一次打开时检查一次
                        if (lastCheckDate != todayStr) {
                            checkAppUpdate(currentAppVersion) { release, _ ->
                                if (release != null) updateInfo = release
                            }
                            sharedPrefs.edit().putString("last_update_check_date", todayStr).apply()
                        }
                    }
                }

                val today = LocalDate.now()
                val defaultTermStart = today.minusDays((today.dayOfWeek.value - 1).toLong())
                val termStartDateStr = activeTimetable?.termStart ?: defaultTermStart.toString()
                val termStartDate = try { LocalDate.parse(termStartDateStr) } catch(e: Exception) { defaultTermStart }
                val daysDiff = ChronoUnit.DAYS.between(termStartDate, today)
                val currentActualWeek = ((daysDiff / 7).toInt() + 1).coerceIn(1, activeTimetable?.totalWeeks ?: 20)

                LaunchedEffect(timetables, activeTimetableId, timeProfiles) {
                    sharedPrefs.edit()
                        .putString("timetables_data", gson.toJson(timetables))
                        .putInt("active_id", activeTimetableId)
                        .putString("time_profiles_data", gson.toJson(timeProfiles))
                        .apply()
                    updateAppWidget(context)
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        NavHost(navController = navController, startDestination = "Home") {
                            composable(
                                route = "Home",
                                popEnterTransition = { scaleIn(initialScale = 0.9f, animationSpec = tween(400)) + fadeIn(animationSpec = tween(400)) },
                                exitTransition = { scaleOut(targetScale = 0.9f, animationSpec = tween(400)) + fadeOut(animationSpec = tween(400)) }
                            ) {
                                TimetableScreen(
                                    preferredConflictIds = preferredConflictIds,
                                    showWatermark = showWatermark,
                                    onPreferredConflictChange = { preferredConflictIds = it },
                                    timetables = timetables,
                                    activeTimetableId = activeTimetableId,
                                    conflictColor = conflictColor,
                                    timeProfiles = timeProfiles,
                                    currentActualWeek = currentActualWeek,
                                    showBgImage = showBgImage, bgImageUri = bgImageUri, bgOpacity = bgOpacity,
                                    highlightToday = highlightToday, showTimeLine = showTimeLine,
                                    showConflictWarning = showConflictWarning,
                                    realTimeSlider = realTimeSlider,
                                    onSetCurrentWeek = { newWeek ->
                                        val mondayOfThisWeek = today.minusDays((today.dayOfWeek.value - 1).toLong())
                                        val newTermStart = mondayOfThisWeek.minusWeeks((newWeek - 1).toLong())
                                        timetables = timetables.map { if (it.id == activeTimetableId) it.copy(termStart = newTermStart.toString()) else it }
                                    },
                                    onTimetableSelect = { activeTimetableId = it },
                                    onNewTimetable = {
                                        val newId = (timetables.maxOfOrNull { it.id } ?: 0) + 1
                                        timetables = timetables.plus(TimetableData(newId, "未命名课表 $newId", emptyList()))
                                        activeTimetableId = newId
                                    },
                                    onDeleteTimetable = { deleteId ->
                                        if (timetables.size > 1) {
                                            timetables = timetables.filter { it.id != deleteId }
                                            if (activeTimetableId == deleteId) activeTimetableId = timetables.first().id
                                        }
                                    },
                                    onReorderTimetables = { newTimetables -> timetables = newTimetables },
                                    onNavigateToSettings = { navController.navigate("Settings") },
                                    onNavigateToAddCourse = { navController.navigate("AddCourse/-1") },
                                    onNavigateToEditCourse = { courseId -> navController.navigate("AddCourse/$courseId") },
                                    onNavigateToEditTimetable = { id -> navController.navigate("EditTimetable/$id") },
                                    onNavigateToEditTimeProfile = { profileId -> navController.navigate("EditTimeProfile/$profileId") },
                                    onNavigateToCourseList = { id -> navController.navigate("CourseList/$id") },
                                    onNavigateToBrowser = { navController.navigate("Browser") },
                                    onNavigateToAbout = { navController.navigate("About") },
                                    onImportCourses = { courses -> pendingImportCourses = courses },
                                    showCourseBorder = showCourseBorder,
                                    courseBorderColor = courseBorderColor
                                )
                            }

                            composable(
                                route = "Settings",
                                enterTransition = { slideIntoContainer(towards = AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = tween(400)) },
                                popExitTransition = { slideOutOfContainer(towards = AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = tween(400)) }
                            ) {
                                SettingsScreen(
                                    parserId = parserId,
                                    showWatermark = showWatermark,
                                    onShowWatermarkChange = { showWatermark = it },
                                    onParserIdChange = { parserId = it },
                                    themeMode = themeMode, onThemeChange = { themeMode = it },
                                    autoCheckUpdate = autoCheckUpdate,
                                    onAutoCheckUpdateChange = { autoCheckUpdate = it },
                                    onManualCheckUpdate = {
                                        Toast.makeText(context, "正在检查更新...", Toast.LENGTH_SHORT).show()
                                        coroutineScope.launch {
                                            checkAppUpdate(currentAppVersion) { release, isLatest ->
                                                if (release != null) {
                                                    updateInfo = release // 弹出更新对话框
                                                } else if (isLatest) {
                                                    Toast.makeText(context, "当前已是最新版本 ($currentAppVersion)", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    Toast.makeText(context, "检查失败，请检查网络或是否受限(Github可能需要代理)", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                    },
                                    showBgImage = showBgImage, onShowBgImageChange = { showBgImage = it },
                                    bgImageUri = bgImageUri, onBgImageUriChange = { bgImageUri = it },
                                    bgOpacity = bgOpacity, onBgOpacityChange = { bgOpacity = it },
                                    savedBrowserUrl = defaultBrowserUrl,
                                    savedDesktopWidth = desktopWidth,
                                    savedDesktopHeight = desktopHeight,
                                    onBrowserSettingsSave = { url, w, h, user, pass, autoLogin ->
                                        defaultBrowserUrl = url
                                        desktopWidth = w
                                        desktopHeight = h
                                        autoUsername = user
                                        autoPassword = pass
                                        isAutoLoginEnabled = autoLogin
                                    },
                                    savedUsername = autoUsername, savedPassword = autoPassword, savedAutoLogin = isAutoLoginEnabled,
                                    highlightToday = highlightToday, onHighlightTodayChange = { highlightToday = it },
                                    showTimeLine = showTimeLine, onShowTimeLineChange = { showTimeLine = it },
                                    showConflictWarning = showConflictWarning, onShowConflictWarningChange = { showConflictWarning = it },
                                    conflictColor = conflictColor,
                                    onConflictColorChange = { conflictColor = it },
                                    realTimeSlider = realTimeSlider,
                                    onRealTimeSliderChange = { realTimeSlider = it },
                                    onNavigateToAbout = { navController.navigate("About") },
                                    onNavigateToAgreement = { navController.navigate("Agreement?readOnly=true") },
                                    onBackClick = { navController.popBackStack() },
                                    showCourseBorder = showCourseBorder,
                                    onShowCourseBorderChange = { showCourseBorder = it },
                                    courseBorderColor = courseBorderColor,
                                    onCourseBorderColorChange = { courseBorderColor = it }
                                )
                            }

                            composable(
                                route = "About",
                                enterTransition = {
                                    slideIntoContainer(towards = AnimatedContentTransitionScope.SlideDirection.Up, animationSpec = tween(400))
                                },
                                popExitTransition = {
                                    slideOutOfContainer(towards = AnimatedContentTransitionScope.SlideDirection.Down, animationSpec = tween(400))
                                }
                            ) {
                                // 只在 !hasAgreed 时启用拦截
                                // 为适配系统预测性返回手势，所以当 hasAgreed 为 true 时，BackHandler 会被禁用
                                androidx.activity.compose.BackHandler(enabled = !hasAgreed) {
                                    sharedPrefs.edit().putBoolean("is_first_launch", false).apply()
                                    isFirstLaunch = false
                                    navController.popBackStack()
                                    navController.navigate("Agreement")
                                }

                                AboutScreen(onBackClick = {
                                    if (!hasAgreed) {
                                        // 看完介绍，标记 is_first_launch 为 false
                                        sharedPrefs.edit().putBoolean("is_first_launch", false).apply()
                                        isFirstLaunch = false
                                        // 关闭介绍页，并马上跳转到协议页
                                        navController.popBackStack()
                                        navController.navigate("Agreement")
                                    } else {
                                        navController.popBackStack()
                                    }
                                })
                            }

                            composable(
                                route = "Agreement?readOnly={readOnly}",
                                arguments = listOf(navArgument("readOnly") {
                                    type = NavType.BoolType
                                    defaultValue = false
                                }),
                                enterTransition = { slideIntoContainer(towards = AnimatedContentTransitionScope.SlideDirection.Up, animationSpec = tween(400)) },
                                popExitTransition = { slideOutOfContainer(towards = AnimatedContentTransitionScope.SlideDirection.Down, animationSpec = tween(400)) }
                            ) { backStackEntry ->
                                // 提取参数
                                val isReadOnly = backStackEntry.arguments?.getBoolean("readOnly") ?: false

                                AgreementScreen(
                                    isReadOnly = isReadOnly, // 传入状态
                                    onBackClick = { navController.popBackStack() }, // 只读模式的返回
                                    onAgreeClick = {
                                        hasAgreed = true
                                        isFirstLaunch = false
                                        sharedPrefs.edit()
                                            .putBoolean("has_agreed", true)
                                            .putBoolean("is_first_launch", false)
                                            .apply()
                                        navController.popBackStack()
                                    }
                                )
                            }

                            composable(
                                route = "AddCourse/{courseId}",
                                arguments = listOf(navArgument("courseId") { type = NavType.IntType }),
                                enterTransition = { slideIntoContainer(towards = AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = tween(400)) },
                                popExitTransition = { slideOutOfContainer(towards = AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = tween(400)) }
                            ) { backStackEntry ->
                                val courseId = backStackEntry.arguments?.getInt("courseId") ?: -1
                                AddCourseScreen(
                                    courseId = courseId,
                                    timetables = timetables,
                                    activeTimetableId = activeTimetableId,
                                    onSave = { newTimetables ->
                                        timetables = newTimetables
                                        navController.popBackStack()
                                    },
                                    onBackClick = { navController.popBackStack() }
                                )
                            }

                            composable(
                                route = "EditTimeProfile/{profileId}",
                                arguments = listOf(navArgument("profileId") { type = NavType.IntType }),
                                enterTransition = { slideIntoContainer(towards = AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = tween(400)) },
                                popExitTransition = { slideOutOfContainer(towards = AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = tween(400)) }
                            ) { backStackEntry ->
                                val pId = backStackEntry.arguments?.getInt("profileId") ?: -1
                                EditTimeProfileScreen(
                                    profileId = pId,
                                    timeProfiles = timeProfiles,
                                    onSave = { updatedProfile ->
                                        val exists = timeProfiles.any { it.id == updatedProfile.id }
                                        timeProfiles = if (exists) {
                                            timeProfiles.map { if (it.id == updatedProfile.id) updatedProfile else it }
                                        } else {
                                            timeProfiles + updatedProfile
                                        }
                                        if (!exists) {
                                            timetables = timetables.map {
                                                if (it.id == activeTimetableId) it.copy(timeProfileId = updatedProfile.id) else it
                                            }
                                        }
                                        navController.popBackStack()
                                    },
                                    onBackClick = { navController.popBackStack() }
                                )
                            }

                            composable(
                                route = "EditTimetable/{id}",
                                arguments = listOf(navArgument("id") { type = NavType.IntType }),
                                enterTransition = { slideIntoContainer(towards = AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = tween(400)) },
                                popExitTransition = { slideOutOfContainer(towards = AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = tween(400)) }
                            ) { backStackEntry ->
                                val id = backStackEntry.arguments?.getInt("id") ?: -1
                                EditTimetableScreen(
                                    timetableId = id,
                                    timetables = timetables,
                                    timeProfiles = timeProfiles,
                                    onSaveTimetable = { updated ->
                                        timetables = timetables.map { if (it.id == updated.id) updated else it }
                                    },
                                    onNavigateToEditProfile = { profileId ->
                                        navController.navigate("EditTimeProfile/$profileId")
                                    },
                                    onDeleteProfile = { deleteId ->
                                        if (timeProfiles.size > 1) {
                                            timeProfiles = timeProfiles.filter { it.id != deleteId }
                                            if (timetables.any { it.timeProfileId == deleteId }) {
                                                timetables = timetables.map {
                                                    if (it.timeProfileId == deleteId) it.copy(timeProfileId = timeProfiles.first().id) else it
                                                }
                                            }
                                        } else {
                                            Toast.makeText(this@MainActivity, "至少需要保留一个时间配置", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    onBackClick = { navController.popBackStack() }
                                )
                            }

                            composable(
                                route = "CourseList/{timetableId}",
                                arguments = listOf(navArgument("timetableId") { type = NavType.IntType }),
                                enterTransition = { slideIntoContainer(towards = AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = tween(400)) },
                                popExitTransition = { slideOutOfContainer(towards = AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = tween(400)) }
                            ) { backStackEntry ->
                                val id = backStackEntry.arguments?.getInt("timetableId") ?: activeTimetableId
                                CourseListScreen(
                                    timetableId = id,
                                    timetables = timetables,
                                    onSave = { newTimetables -> timetables = newTimetables },
                                    onBackClick = { navController.popBackStack() },
                                    onNavigateToAddCourse = { navController.navigate("AddCourse/-1") },
                                    onNavigateToEditCourse = { cId -> navController.navigate("AddCourse/$cId") }
                                )
                            }

                            composable(
                                route = "Browser",
                                enterTransition = { slideIntoContainer(towards = AnimatedContentTransitionScope.SlideDirection.Up, animationSpec = tween(400)) },
                                popExitTransition = { slideOutOfContainer(towards = AnimatedContentTransitionScope.SlideDirection.Down, animationSpec = tween(400)) }
                            ) {
                                BrowserScreen(
                                    defaultUrl = defaultBrowserUrl, desktopWidth = desktopWidth, desktopHeight = desktopHeight,
                                    autoUsername = autoUsername, autoPassword = autoPassword, autoLogin = isAutoLoginEnabled,
                                    onBackClick = { navController.popBackStack() },
                                    onImportCourses = { importedCourses ->
                                        // 使用协程强制回到主线程操作 UI
                                        coroutineScope.launch {
                                            // 只有当前确实在 Browser 页面时才允许退栈
                                            // 即使网页触发 10 次回调，也只会退栈一次，不会把 Home 界面给干掉
                                            if (navController.currentDestination?.route == "Browser") {
                                                navController.popBackStack()
                                            }

                                            // 弹窗和 Toast 在主线程执行
                                            if (importedCourses.isNotEmpty()) {
                                                pendingImportCourses = importedCourses
                                            } else {
                                                Toast.makeText(context, "未能从网页识别到课表", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                )
                            }
                        }

                        if (updateInfo != null) {
                            val uriHandler = LocalUriHandler.current
                            AlertDialog(
                                onDismissRequest = { updateInfo = null },
                                title = { Text("发现新版本：${updateInfo!!.tag_name}", fontWeight = FontWeight.Bold) },
                                text = {
                                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                                        Text("更新内容：", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(updateInfo!!.body, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                },
                                confirmButton = {
                                    Button(onClick = {
                                        uriHandler.openUri(updateInfo!!.html_url)
                                        updateInfo = null
                                    }) { Text("前往 GitHub 下载") }
                                },
                                dismissButton = {
                                    TextButton(onClick = { updateInfo = null }) { Text("暂不更新", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                                }
                            )
                        }

                        // 检测到解析完成的课表数据时弹出导入方式选择对话框
                        if (pendingImportCourses != null) {
                            val existingCourses = activeTimetable?.courses ?: emptyList()
                            // 预处理导入的课程，如果有同名课程，继承原来的背景色和文字颜色
                            val processedImportCourses = pendingImportCourses!!.map { imported ->
                                val matched = existingCourses.find { it.name == imported.name }
                                if (matched != null) {
                                    imported.copy(bgColor = matched.bgColor, textColor = matched.textColor)
                                } else {
                                    imported
                                }
                            }

                            AlertDialog(
                                onDismissRequest = { pendingImportCourses = null },
                                title = { Text("成功解析 ${pendingImportCourses!!.size} 门课程", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                                text = {
                                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Text("请选择您要将这些课程导入到哪里：", fontSize = 14.sp)
                                        Button(onClick = {
                                            val maxId = timetables.flatMap { it.courses }.maxOfOrNull { it.id } ?: 0
                                            val maxWeek = processedImportCourses.flatMap { it.weekList }.maxOrNull() ?: activeTimetable?.totalWeeks ?: 20
                                            val newCourses = processedImportCourses.mapIndexed { index, c -> c.copy(id = maxId + index + 1) } // 使用处理过的课程
                                            timetables = timetables.map { if (it.id == activeTimetableId) it.copy(courses = newCourses, totalWeeks = maxOf(it.totalWeeks, maxWeek)) else it }
                                            pendingImportCourses = null
                                            Toast.makeText(context, "已替换当前课表", Toast.LENGTH_SHORT).show()
                                        }, modifier = Modifier.fillMaxWidth()) { Text("替换当前课表的课程") }

                                        Button(onClick = {
                                            val maxId = timetables.flatMap { it.courses }.maxOfOrNull { it.id } ?: 0
                                            val maxWeek = processedImportCourses.flatMap { it.weekList }.maxOrNull() ?: activeTimetable?.totalWeeks ?: 20
                                            val newCourses = processedImportCourses.mapIndexed { index, c -> c.copy(id = maxId + index + 1) } // 使用处理过的课程
                                            timetables = timetables.map { if (it.id == activeTimetableId) it.copy(courses = it.courses + newCourses, totalWeeks = maxOf(it.totalWeeks, maxWeek)) else it }
                                            pendingImportCourses = null
                                            Toast.makeText(context, "已追加到当前课表", Toast.LENGTH_SHORT).show()
                                        }, modifier = Modifier.fillMaxWidth()) { Text("强制追加到当前课表") }

                                        Button(onClick = {
                                            val maxId = timetables.flatMap { it.courses }.maxOfOrNull { it.id } ?: 0
                                            val maxWeek = processedImportCourses.flatMap { it.weekList }.maxOrNull() ?: 20
                                            // 新建课表就不需要继承颜色了，使用原始 pendingImportCourses
                                            val newCourses = pendingImportCourses!!.mapIndexed { index, c -> c.copy(id = maxId + index + 1) }
                                            val newId = (timetables.maxOfOrNull { it.id } ?: 0) + 1
                                            timetables = timetables + TimetableData(newId, "导入的新课表", newCourses, totalWeeks = maxWeek)
                                            activeTimetableId = newId
                                            pendingImportCourses = null
                                            Toast.makeText(context, "已新建为新课表", Toast.LENGTH_SHORT).show()
                                        }, modifier = Modifier.fillMaxWidth()) { Text("新建为新课表") }
                                    }
                                },
                                confirmButton = {},
                                dismissButton = { TextButton(onClick = { pendingImportCourses = null }) { Text("取消", color = MaterialTheme.colorScheme.onSurfaceVariant) } }
                            )
                        }
                    }
                }
            }
        }
    }
}

// 课表主界面
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TimetableScreen(
    preferredConflictIds: Set<Int>,
    showWatermark: Boolean,
    showCourseBorder: Boolean,
    courseBorderColor: Long,
    onPreferredConflictChange: (Set<Int>) -> Unit,
    timetables: List<TimetableData>, activeTimetableId: Int, timeProfiles: List<TimeProfile>,
    currentActualWeek: Int, showBgImage: Boolean, bgImageUri: String?, bgOpacity: Float,
    conflictColor: Long,
    highlightToday: Boolean, showTimeLine: Boolean, showConflictWarning: Boolean,
    realTimeSlider: Boolean,
    onSetCurrentWeek: (Int) -> Unit, modifier: Modifier = Modifier,
    onTimetableSelect: (Int) -> Unit, onNewTimetable: () -> Unit, onDeleteTimetable: (Int) -> Unit, onReorderTimetables: (List<TimetableData>) -> Unit,
    onNavigateToSettings: () -> Unit, onNavigateToAddCourse: () -> Unit, onNavigateToEditCourse: (Int) -> Unit,
    onNavigateToEditTimetable: (Int) -> Unit, onNavigateToEditTimeProfile: (Int) -> Unit,
    onNavigateToCourseList: (Int) -> Unit, onNavigateToBrowser: () -> Unit, onNavigateToAbout: () -> Unit,
    onImportCourses: (List<Course>) -> Unit
) {
    val activeTimetable = timetables.find { it.id == activeTimetableId } ?: timetables.firstOrNull()
    val courses = activeTimetable?.courses ?: emptyList()
    val totalWeeks = activeTimetable?.totalWeeks ?: 20
    val activeProfileId = activeTimetable?.timeProfileId ?: 1
    val activeProfileNodes = timeProfiles.find { it.id == activeProfileId }?.nodes ?: nodeTimes

    val pagerState = rememberPagerState(initialPage = (currentActualWeek - 1).coerceIn(0, totalWeeks - 1), pageCount = { totalWeeks })
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var showCourseDetailSheet by remember { mutableStateOf(false) }
    var selectedCourseForDetail by remember { mutableStateOf<Course?>(null) }
    var selectedConflictGroup by remember { mutableStateOf<List<Course>?>(null) }
    var showSetCurrentWeekDialog by remember { mutableStateOf(false) }

    var showWeekSliderSheet by remember { mutableStateOf(false) }
    var showSponsorDialog by remember { mutableStateOf(false) }

    val currentAppVersion = remember { try { context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0.0" } catch (e: Exception) { "1.0.0" } }
    var localUpdateInfo by remember { mutableStateOf<com.jiaweiya.flowcourse.GithubRelease?>(null) }
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    BackHandler(enabled = drawerState.isOpen) {
        coroutineScope.launch { drawerState.close() }
    }

    val htmlImportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                coroutineScope.launch {
                    val importedCourses = withContext(Dispatchers.IO) { com.jiaweiya.flowcourse.parser.CqwlxyParser.parseCourseFromFile(context, uri) }
                    if (importedCourses.isNotEmpty()) onImportCourses(importedCourses) else Toast.makeText(context, "导入失败：未能识别到课程信息", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val shareImportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                coroutineScope.launch {
                    val text = withContext(Dispatchers.IO) { context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } }
                    val imported = text?.let { decodeShareData(it) }
                    if (imported != null && imported.isNotEmpty()) { onImportCourses(imported) } else { Toast.makeText(context, "导入失败：文件格式错误或已损坏", Toast.LENGTH_SHORT).show() }
                }
            }
        }
    }

    val today = LocalDate.now()
    val dateString = "${today.year}/${today.monthValue}/${today.dayOfMonth}"
    val weekDays = listOf("一", "二", "三", "四", "五", "六", "日")
    val todayDayOfWeek = weekDays[today.dayOfWeek.value - 1]
    val bgBitmap = rememberBitmapFromUri(bgImageUri)

    var showShareMenuDialog by remember { mutableStateOf(false) }
    var showImportMenuDialog by remember { mutableStateOf(false) }

    val blurRadius by animateDpAsState(targetValue = if (drawerState.targetValue == DrawerValue.Open) 16.dp else 0.dp, label = "blur")

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            MainDrawerSheet(
                currentAppVersion = currentAppVersion,
                activeProfileId = activeProfileId,
                activeTimetableId = activeTimetableId,
                totalWeeks = totalWeeks,
                currentViewedWeek = pagerState.targetPage + 1,
                realTimeSlider = realTimeSlider,
                timetables = timetables,
                onCloseDrawer = { coroutineScope.launch { drawerState.close() } },
                onNavigateToAbout = onNavigateToAbout,
                onNavigateToSettings = onNavigateToSettings,
                onNavigateToEditTimeProfile = onNavigateToEditTimeProfile,
                onNavigateToAddCourse = onNavigateToAddCourse,
                onNavigateToCourseList = onNavigateToCourseList,
                onNavigateToEditTimetable = onNavigateToEditTimetable,
                onShowSponsorDialog = { showSponsorDialog = true },
                onShowImportMenu = { showImportMenuDialog = true },
                onShowShareMenu = { showShareMenuDialog = true },
                onShowSetCurrentWeekDialog = { showSetCurrentWeekDialog = true },
                onUpdateFound = { localUpdateInfo = it },
                onWeekSliderChange = { newValue ->
                    if (realTimeSlider) coroutineScope.launch { pagerState.scrollToPage(newValue.roundToInt() - 1) }
                },
                onWeekSliderChangeFinished = { displayWeek ->
                    if (!realTimeSlider) coroutineScope.launch { pagerState.animateScrollToPage(displayWeek - 1) }
                },
                onNewTimetable = onNewTimetable,
                onTimetableSelect = onTimetableSelect,
                onDeleteTimetable = onDeleteTimetable,
                onReorderTimetables = onReorderTimetables
            )
        }
    ) {
        Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).blur(blurRadius)) {
            if (showBgImage && bgBitmap != null) {
                Image(bitmap = bgBitmap, contentDescription = "背景图片", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop, alpha = bgOpacity)
            }

            if (showWatermark) {
                Column(modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = 32.dp, end = 32.dp), horizontalAlignment = Alignment.Start) {
                    Text(text = "Flow\nCourse", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = Color.Gray.copy(alpha = 0.6f), textAlign = TextAlign.Start, lineHeight = 30.sp)
                    Text(text = "By:Jiaweiya", fontSize = 19.sp, color = Color.Gray.copy(alpha = 0.4f), textAlign = TextAlign.Start)
                }
            }

            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { coroutineScope.launch { drawerState.open() } }) {
                        Icon(imageVector = Icons.Default.Menu, contentDescription = "打开菜单", modifier = Modifier.size(28.dp), tint = MaterialTheme.colorScheme.onBackground)
                    }

                    Column(
                        horizontalAlignment = Alignment.End,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showWeekSliderSheet = true }
                            .padding(4.dp)
                    ) {
                        Text(
                            text = dateString,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        val viewedWeek = pagerState.currentPage + 1
                        Row(verticalAlignment = Alignment.Bottom) {
                            val subtitleText = if (viewedWeek == currentActualWeek) "周$todayDayOfWeek" else "周$todayDayOfWeek (正在浏览第 $viewedWeek 周)"
                            Text(text = subtitleText, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "第 $currentActualWeek 周", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                    WeekTimetableGrid(
                        conflictColor = conflictColor, showCourseBorder = showCourseBorder, courseBorderColor = courseBorderColor,
                        currentWeek = page + 1, allCourses = courses, profileNodes = activeProfileNodes, termStartStr = activeTimetable?.termStart,
                        highlightToday = highlightToday, showTimeLine = showTimeLine, showConflictWarning = showConflictWarning, preferredConflictIds = preferredConflictIds,
                        onCourseClick = { clickedGroup ->
                            if (clickedGroup.size == 1) {
                                selectedCourseForDetail = clickedGroup.first()
                                showCourseDetailSheet = true
                            } else { selectedConflictGroup = clickedGroup }
                        }
                    )
                }
            }
        }
    }

    if (showWeekSliderSheet) {
        ModalBottomSheet(
            onDismissRequest = { showWeekSliderSheet = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            val currentViewedWeek = pagerState.targetPage + 1
            var sliderValue by remember { mutableFloatStateOf(currentViewedWeek.toFloat()) }
            var isDragging by remember { mutableStateOf(false) }

            LaunchedEffect(currentViewedWeek) {
                if (!isDragging) sliderValue = currentViewedWeek.toFloat()
            }
            val displayWeek = sliderValue.roundToInt()

            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)) {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("周数 (第 ${displayWeek} 周)", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text("修改当前周", fontSize = 14.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.clickable { showWeekSliderSheet = false; showSetCurrentWeekDialog = true })
                }
                Slider(
                    value = sliderValue,
                    onValueChange = { newValue ->
                        isDragging = true
                        sliderValue = newValue
                        if (realTimeSlider) coroutineScope.launch { pagerState.scrollToPage(newValue.roundToInt() - 1) }
                    },
                    onValueChangeFinished = {
                        isDragging = false
                        if (!realTimeSlider) coroutineScope.launch { pagerState.animateScrollToPage(displayWeek - 1) }
                    },
                    valueRange = 1f..totalWeeks.toFloat(),
                    steps = if (totalWeeks > 2) totalWeeks - 2 else 0,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }
        }
    }

    if (showSponsorDialog) {
        AlertDialog(
            onDismissRequest = { showSponsorDialog = false },
            title = { Text("求一个赞助支持", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                ) {
                    Text("作者用爱发电不易，感谢老板的打赏！", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 12.dp))
                    Image(
                        painter = painterResource(id = R.drawable.wechatcode),
                        contentDescription = "微信赞助",
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Image(
                        painter = painterResource(id = R.drawable.alpaycode),
                        contentDescription = "支付宝赞助",
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showSponsorDialog = false }) { Text("关闭", color = MaterialTheme.colorScheme.primary) }
            },
            dismissButton = {
                TextButton(onClick = {
                    // 调用已在 SettingsScreen 中定义过的相册保存函数
                    saveImagesToGallery(context, coroutineScope, listOf(R.drawable.wechatcode, R.drawable.alpaycode))
                }) { Text("保存到相册", color = MaterialTheme.colorScheme.primary) }
            }
        )
    }

    if (showCourseDetailSheet && selectedCourseForDetail != null) {
        ModalBottomSheet(onDismissRequest = { showCourseDetailSheet = false }, containerColor = MaterialTheme.colorScheme.surface) {
            CourseDetailContent(course = selectedCourseForDetail!!, profileNodes = activeProfileNodes, onEditClick = { showCourseDetailSheet = false; onNavigateToEditCourse(selectedCourseForDetail!!.id) })
        }
    }

    if (selectedConflictGroup != null) {
        ModalBottomSheet(onDismissRequest = { selectedConflictGroup = null }, containerColor = MaterialTheme.colorScheme.surface) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp)) {
                Text("发现冲突课程，请选择要在课表上显示哪一门：", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 16.dp))
                selectedConflictGroup!!.forEach { course ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)).clickable {
                            selectedCourseForDetail = course; showCourseDetailSheet = true; selectedConflictGroup = null
                        }.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = course.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "${course.room} | ${course.teacher}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        val isSelected = preferredConflictIds.contains(course.id) || (!preferredConflictIds.any { id -> selectedConflictGroup!!.map{it.id}.contains(id) } && course == selectedConflictGroup!!.first())
                        RadioButton(
                            selected = isSelected,
                            onClick = {
                                val newSet = preferredConflictIds.toMutableSet()
                                newSet.removeAll(selectedConflictGroup!!.map { it.id }.toSet())
                                newSet.add(course.id)
                                onPreferredConflictChange(newSet)
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    if (showSetCurrentWeekDialog) {
        SetCurrentWeekDialog(
            currentActualWeek = currentActualWeek, totalWeeks = totalWeeks,
            onDismiss = { showSetCurrentWeekDialog = false },
            onConfirm = { newWeek -> onSetCurrentWeek(newWeek); showSetCurrentWeekDialog = false; coroutineScope.launch { pagerState.animateScrollToPage(newWeek - 1) } }
        )
    }

    if (showShareMenuDialog) {
        AlertDialog(
            onDismissRequest = { showShareMenuDialog = false },
            title = { Text("分享课表", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = {
                        val content = encodeShareData(context, activeTimetable?.courses ?: emptyList())
                        val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        clipboardManager.setPrimaryClip(android.content.ClipData.newPlainText("FlowCourse", content))
                        Toast.makeText(context, "已复制加密课表到剪切板，去粘贴给好友吧！", Toast.LENGTH_SHORT).show()
                        showShareMenuDialog = false
                    }, modifier = Modifier.fillMaxWidth()) { Text("复制到剪切板") }
                    Button(onClick = {
                        val content = encodeShareData(context, activeTimetable?.courses ?: emptyList())
                        val file = java.io.File(context.cacheDir, "FlowCourse分享课表.flowcourse")
                        file.writeText(content)
                        val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                            type = "application/octet-stream"
                            putExtra(android.content.Intent.EXTRA_STREAM, uri)
                            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(android.content.Intent.createChooser(intent, "分享 .flowcourse 文件"))
                        showShareMenuDialog = false
                    }, modifier = Modifier.fillMaxWidth()) { Text("生成文件并发送至 APP") }
                }
            },
            confirmButton = {}, dismissButton = { TextButton(onClick = { showShareMenuDialog = false }) { Text("取消", color = MaterialTheme.colorScheme.onSurfaceVariant) } }
        )
    }

    if (showImportMenuDialog) {
        AlertDialog(
            onDismissRequest = { showImportMenuDialog = false },
            title = { Text("导入课表", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = { showImportMenuDialog = false; onNavigateToBrowser() }, modifier = Modifier.fillMaxWidth()) { Text("从教务系统导入") }
                    Button(onClick = {
                        val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        val clipText = clipboardManager.primaryClip?.getItemAt(0)?.text?.toString()
                        if (clipText != null) {
                            val imported = decodeShareData(clipText)
                            if (imported != null && imported.isNotEmpty()) { onImportCourses(imported); showImportMenuDialog = false } else { Toast.makeText(context, "剪切板内未发现有效的课表数据", Toast.LENGTH_SHORT).show() }
                        } else { Toast.makeText(context, "剪切板为空", Toast.LENGTH_SHORT).show() }
                    }, modifier = Modifier.fillMaxWidth()) { Text("从剪切板导入") }
                    Button(onClick = {
                        val intent = android.content.Intent(android.content.Intent.ACTION_OPEN_DOCUMENT).apply { addCategory(android.content.Intent.CATEGORY_OPENABLE); type = "*/*" }
                        shareImportLauncher.launch(intent)
                        showImportMenuDialog = false
                    }, modifier = Modifier.fillMaxWidth()) { Text("从文件中导入课表") }
                }
            },
            confirmButton = {}, dismissButton = { TextButton(onClick = { showImportMenuDialog = false }) { Text("取消", color = MaterialTheme.colorScheme.onSurfaceVariant) } }
        )
    }

    if (localUpdateInfo != null) {
        AlertDialog(
            onDismissRequest = { localUpdateInfo = null },
            title = { Text("发现新版本：${localUpdateInfo!!.tag_name}", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text("更新内容：", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(localUpdateInfo!!.body, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = {
                Button(onClick = { uriHandler.openUri(localUpdateInfo!!.html_url); localUpdateInfo = null }) {
                    Text("前往 GitHub 下载")
                }
            },
            dismissButton = {
                TextButton(onClick = { localUpdateInfo = null }) { Text("暂不更新", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        )
    }
}

@Composable
fun SetCurrentWeekDialog(currentActualWeek: Int, totalWeeks: Int, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    var selectedWeek by remember { mutableIntStateOf(currentActualWeek) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("设置当前周", fontWeight = FontWeight.Bold) },
        text = {
            LazyVerticalGrid(
                columns = GridCells.Fixed(5),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(totalWeeks) { i ->
                    val week = i + 1;
                    val isSelected = selectedWeek == week;
                    Box(modifier = Modifier.aspectRatio(1f).clip(CircleShape).background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant).clickable { selectedWeek = week }, contentAlignment = Alignment.Center) { Text("$week", color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant) } }
            } },
        confirmButton = { TextButton(onClick = { onConfirm(selectedWeek) }) { Text("确定", color = MaterialTheme.colorScheme.primary) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消", color = MaterialTheme.colorScheme.onSurfaceVariant) } }
    )
}

@Composable
fun ManagementMenuContent(
    timetables: List<TimetableData>,
    activeTimetableId: Int,
    onTimetableSelect: (Int) -> Unit, onNewTimetable: () -> Unit, onDeleteTimetable: (Int) -> Unit, onReorderTimetables: (List<TimetableData>) -> Unit,
    currentWeek: Int,
    totalWeeks: Int,
    realTimeSlider: Boolean,
    onWeekChange: (Int, Boolean) -> Unit,
    onChangeCurrentWeekClick: () -> Unit,
    onEditTimetableClick: (Int) -> Unit,
    onCourseListClick: () -> Unit,
    onEditTimeProfileClick: () -> Unit,
    onNavigateToAbout: () -> Unit
) {
    var isManageMode by remember { mutableStateOf(false) }
    var timetableToDelete by remember { mutableStateOf<Int?>(null) }
    var draggedIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableStateOf(0f) }
    val density = LocalDensity.current
    val itemWidthPx = remember(density) { with(density) { 72.dp.toPx() } }
    val currentTimetables by rememberUpdatedState(timetables)

    if (timetableToDelete != null) {
        val deleteName = timetables.find { it.id == timetableToDelete }?.name ?: ""
        AlertDialog(
            onDismissRequest = { timetableToDelete = null },
            title = { Text("删除课表", fontWeight = FontWeight.Bold) },
            text = { Text("您确定要删除\"$deleteName\"吗？") },
            confirmButton = { TextButton(onClick = { onDeleteTimetable(timetableToDelete!!); timetableToDelete = null }) { Text("删除", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { timetableToDelete = null }) { Text("取消",
                color = MaterialTheme.colorScheme.onSurfaceVariant) } }
        )
    }

    var sliderValue by remember { mutableFloatStateOf(currentWeek.toFloat()) }
    var isDragging by remember { mutableStateOf(false) } // 记录当前是否正在用手指拖拽

    // 只有在没拖拽滑块的时候，才允许外部页面变化去同步滑块位置
    LaunchedEffect(currentWeek) {
        if (!isDragging) {
            sliderValue = currentWeek.toFloat()
        }
    }

    // 实时计算当前滑块对应的是第几周（四舍五入为整数）
    val displayWeek = sliderValue.roundToInt()

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            // 文字改为读取 displayWeek，无论开没开实时更新，文字都会瞬间跟着滑块变
            Text("周数 (第 ${displayWeek} 周)", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text("修改当前周", fontSize = 14.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.clickable { onChangeCurrentWeekClick() })
        }
        Slider(
            value = sliderValue,
            onValueChange = { newValue ->
                isDragging = true // 标记手指正在触摸拖拽
                sliderValue = newValue // 滑块 UI 实时跟随手指
                if (realTimeSlider) {
                    // 开启实时更新时，用无动画瞬间切换
                    onWeekChange(newValue.roundToInt(), false)
                }
            },
            onValueChangeFinished = {
                isDragging = false // 松手，解除拖拽状态

                // 如果开了实时更新，页面已经切过去了，不再调动画
                if (!realTimeSlider) {
                    // 在没开实时更新时，松手才播放平滑翻页动画
                    onWeekChange(displayWeek, true)
                }
            },
            valueRange = 1f..totalWeeks.toFloat(),
            // 添加 steps 属性，实现自动吸附到整数刻度
            steps = if (totalWeeks > 2) totalWeeks - 2 else 0,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) { Text("课表", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.animateContentSize(
                animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = 600f)
                )
            ) {
                Text("新建课表", fontSize = 14.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.clickable { onNewTimetable() })
                Spacer(modifier = Modifier.width(16.dp))
                AnimatedContent(targetState = isManageMode, label = "") { isManaging -> Text(if(isManaging) "退出管理" else "管理", fontSize = 14.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.clickable { isManageMode = !isManageMode }) }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            itemsIndexed(timetables, key = { _, item -> item.id }) { index, timetable ->
                val isSelected = timetable.id == activeTimetableId
                val isDragged = draggedIndex != null && currentTimetables.getOrNull(draggedIndex!!)?.id == timetable.id
                val dragScale by animateFloatAsState(if (isDragged) 1.15f else 1f, label = "drag_scale")
                // 抖动动画
                val infiniteTransition = rememberInfiniteTransition(label = "jiggle")
                val jiggleAngle by infiniteTransition.animateFloat(
                    initialValue = -2.5f,
                    targetValue = 2.5f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(130, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "jiggle_angle"
                )
                // 只有在管理模式下且没有被拖拽时才抖动，拖拽时固定不动
                val actualRotation = if (isManageMode && !isDragged) jiggleAngle else 0f
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.then(if (isDragged) Modifier else Modifier.animateItem()).graphicsLayer {
                        translationX = if (isDragged) dragOffset else 0f
                        scaleX = dragScale; scaleY = dragScale
                        rotationZ = actualRotation // 抖动实现
                    }.zIndex(if (isDragged) 1f else 0f).pointerInput(isManageMode, timetable.id) {
                        if (!isManageMode)
                            return@pointerInput
                        detectDragGesturesAfterLongPress(
                        onDragStart = { draggedIndex = currentTimetables.indexOfFirst { it.id == timetable.id } },
                        onDrag = { change, dragAmount ->
                            change.consume(); dragOffset += dragAmount.x
                            val currentIndex = draggedIndex ?: return@detectDragGesturesAfterLongPress
                            var targetIndex = currentIndex
                            if (dragOffset > itemWidthPx / 2 && currentIndex < currentTimetables.size - 1) targetIndex = currentIndex + 1
                            else if (dragOffset < -itemWidthPx / 2 && currentIndex > 0) targetIndex = currentIndex - 1
                            if (targetIndex != currentIndex) {
                                val newList = currentTimetables.toMutableList()
                                val temp = newList[currentIndex]
                                newList[currentIndex] = newList[targetIndex]
                                newList[targetIndex] = temp
                                onReorderTimetables(newList)
                                draggedIndex = targetIndex
                                dragOffset -= if (targetIndex > currentIndex) itemWidthPx else -itemWidthPx
                            }
                                 },
                            onDragEnd = { draggedIndex = null; dragOffset = 0f },
                            onDragCancel = { draggedIndex = null; dragOffset = 0f }
                        )
                    }
                ) {
                    Box(modifier = Modifier.size(60.dp).clip(RoundedCornerShape(12.dp)).background(if (isSelected && !isManageMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant).clickable { if (isManageMode) onEditTimetableClick(timetable.id) else onTimetableSelect(timetable.id) }) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            if (isManageMode)
                                Icon(Icons.Default.Edit, contentDescription = "编辑", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            else {
                                if (isSelected)
                                    Icon(Icons.Default.Check, contentDescription = "选中", tint = MaterialTheme.colorScheme.onPrimary)
                                else
                                    Text("${timetable.courses.size}门", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        if (isManageMode && timetables.size > 1) {
                            Box(
                                modifier = Modifier.align(Alignment.TopEnd).size(20.dp).clip(RoundedCornerShape(bottomStart = 20.dp)).background(MaterialTheme.colorScheme.error).clickable { timetableToDelete = timetable.id },
                                contentAlignment = Alignment.Center
                            ) { Icon(Icons.Default.Close, contentDescription = "删除", tint = MaterialTheme.colorScheme.onError, modifier = Modifier.size(12.dp).offset(x = 2.dp, y = (-2).dp))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = timetable.name, fontSize = 12.sp, color = if (isSelected && !isManageMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), modifier = Modifier.padding(horizontal = 24.dp))
        Spacer(modifier = Modifier.height(24.dp))
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.Bottom
        ) {
            MenuIconBtn(icon = Icons.Default.DateRange, text = "上课时间", onClick = onEditTimeProfileClick)
            MenuIconBtn(icon = Icons.Default.Build, text = "课表属性", onClick = { onEditTimetableClick(activeTimetableId) })
            MenuIconBtn(icon = Icons.Default.List, text = "已添课程", onClick = { onCourseListClick() })

            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onNavigateToAbout() }
                    .padding(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = R.drawable.jiaweiya_icon),
                    contentDescription = "关于此应用",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "关于此应用",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        Spacer(modifier = Modifier.height(40.dp))
    }
}
@Composable
fun MenuIconBtn(icon: ImageVector, text: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            modifier = Modifier.size(36.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = text,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun CourseDetailContent(course: Course, profileNodes: List<NodeTime>, onEditClick: () -> Unit) {
    val dayString = listOf("一", "二", "三", "四", "五", "六", "日")[course.dayOfWeek - 1]
    val startNodeConfig = profileNodes.getOrNull(course.startNode - 1)
    val endNodeConfig = profileNodes.getOrNull(course.endNode - 1)
    val startLabel = startNodeConfig?.label ?: ""
    val endLabel = endNodeConfig?.label ?: ""
    val startTime = startNodeConfig?.start ?: ""
    val endTime = endNodeConfig?.end ?: ""

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp)) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = course.name,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f).padding(end = 12.dp)
            )
            IconButton(
                onClick = onEditClick,
                modifier = Modifier.size(32.dp).offset(y = 0.dp)
            ) {
                Icon(Icons.Default.Edit, contentDescription = "编辑课程", tint = MaterialTheme.colorScheme.primary)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 下方的详细信息行
        DetailRow(icon = "👨‍🏫", title = "授课教师", content = if(course.teacher.isNotEmpty()) course.teacher else "未设置")
        DetailRow(icon = "📍", title = "上课地点", content = if(course.room.isNotEmpty()) course.room else "未设置")
        DetailRow(icon = "⏰", title = "上课时间", content = "周$dayString  第 $startLabel-$endLabel 节 ($startTime ~ $endTime)")
        DetailRow(icon = "📅", title = "上课周数", content = "第 ${course.weekList.joinToString(", ")} 周")

        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
fun DetailRow(icon: String, title: String, content: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(icon, fontSize = 20.sp)
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(title, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(content, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}