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
import androidx.compose.foundation.lazy.grid.itemsIndexed
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
import androidx.compose.runtime.mutableIntStateOf

import android.webkit.JavascriptInterface
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.lazy.items
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState

import com.jiaweiya.flowcourse.widget.TimetableWidget
import com.jiaweiya.flowcourse.parser.CqwlxyParser

// 数据结构定义
@Immutable
data class NodeTime(val label: String, val start: String, val end: String, val isVisible: Boolean = true)
@Immutable
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

@Immutable
data class TimetableData(
    val id: Int,
    val name: String,
    val courses: List<Course>,
    val termStart: String? = null,
    val timeProfileId: Int = 1,
    val totalWeeks: Int = 20
)

@Immutable
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
    @OptIn(kotlinx.coroutines.DelicateCoroutinesApi::class)
    kotlinx.coroutines.GlobalScope.launch(Dispatchers.Main) {
        try {
            TimetableWidget().updateAll(context)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

// 检查更新的网络请求函数
suspend fun checkAppUpdate(currentVersion: String, channel: Int, onResult: (GithubRelease?, Boolean) -> Unit) {
    withContext(Dispatchers.IO) {
        try {
            // 正式版（0）请求 latest，CL版（1）请求全量列表 releases
            val urlString = if (channel == 1) {
                "https://api.github.com/repos/jiaweiyaya/FlowCourse/releases"
            } else {
                "https://api.github.com/repos/jiaweiyaya/FlowCourse/releases/latest"
            }

            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            if (connection.responseCode == 200) {
                val json = connection.inputStream.bufferedReader().readText()
                val gson = Gson()

                val release = if (channel == 1) {
                    // CL 频道解析 JSON 数组，获取首个（即最新发布，含预览版）
                    val type = object : TypeToken<List<GithubRelease>>() {}.type
                    val releases = gson.fromJson<List<GithubRelease>>(json, type)
                    releases.firstOrNull()
                } else {
                    // 正式版直接解析单体最新的正式版本
                    gson.fromJson(json, GithubRelease::class.java)
                }

                if (release != null) {
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
    val metrics = context.resources.displayMetrics
    val screenWidth = metrics.widthPixels
    val screenHeight = metrics.heightPixels

    var bitmap by remember(uriString) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(uriString) {
        if (uriString != null) {
            withContext(Dispatchers.IO) {
                try {
                    val uri = Uri.parse(uriString)
                    val options = android.graphics.BitmapFactory.Options().apply {
                        inJustDecodeBounds = true
                    }
                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        android.graphics.BitmapFactory.decodeStream(inputStream, null, options)
                    }

                    val srcWidth = options.outWidth
                    val srcHeight = options.outHeight
                    var sampleSize = 1
                    if (srcWidth > screenWidth || srcHeight > screenHeight) {
                        val widthRatio = Math.round(srcWidth.toFloat() / screenWidth.toFloat())
                        val heightRatio = Math.round(srcHeight.toFloat() / screenHeight.toFloat())
                        sampleSize = Math.max(widthRatio, heightRatio)
                    }

                    options.apply {
                        inJustDecodeBounds = false
                        inSampleSize = sampleSize
                        inPreferredConfig = android.graphics.Bitmap.Config.ARGB_8888
                    }

                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        android.graphics.BitmapFactory.decodeStream(inputStream, null, options)?.let {
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
            var realTimeSlider by remember { mutableStateOf(sharedPrefs.getBoolean("real_time_slider", false)) }
            var parserId by remember { mutableIntStateOf(sharedPrefs.getInt("parser_id", 1)) }

            var updateChannel by remember { mutableIntStateOf(sharedPrefs.getInt("update_channel", 0)) }
            var autoCheckUpdate by remember { mutableStateOf(sharedPrefs.getBoolean("auto_check_update", true)) }
            var showWatermark by remember { mutableStateOf(sharedPrefs.getBoolean("show_watermark", true)) }
            var updateInfo by remember { mutableStateOf<GithubRelease?>(null) }
            val currentAppVersion = try { packageManager.getPackageInfo(packageName, 0).versionName ?: "1.0.0" } catch (e: Exception) { "1.0.0" }

            var showCourseBorder by remember { mutableStateOf(sharedPrefs.getBoolean("show_course_border", true)) }
            var courseBorderColor by remember { mutableLongStateOf(sharedPrefs.getLong("course_border_color", 0xFF9E77ED)) }

            var autoUsername by remember { mutableStateOf(sharedPrefs.getString("auto_username", "") ?: "") }
            var autoPassword by remember { mutableStateOf(sharedPrefs.getString("auto_password", "") ?: "") }
            var isAutoLoginEnabled by remember { mutableStateOf(sharedPrefs.getBoolean("auto_login", false)) }
            var isAutoNavigateEnabled by remember { mutableStateOf(sharedPrefs.getBoolean("auto_navigate", false)) }
            var defaultDesktopMode by remember { mutableStateOf(sharedPrefs.getBoolean("default_desktop_mode", false)) }

            var preferredConflictIds by remember { mutableStateOf(sharedPrefs.getStringSet("preferred_conflict_ids", emptySet())?.mapNotNull { it.toIntOrNull() }?.toSet() ?: emptySet()) }

            val isSystemDark = isSystemInDarkTheme()

            val isAppDark = themeMode == 2 || (themeMode == 0 && isSystemDark)
            val defaultColor = if (isAppDark) 0xFFD0BCFF else 0xFF9E77ED

            var themeColor by remember {
                mutableLongStateOf(
                    sharedPrefs.getLong("theme_color", defaultColor)
                )
            }
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
                    if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                        updateAppWidget(context)
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }

            val bgBitmap = rememberBitmapFromUri(bgImageUri)

            LaunchedEffect(themeMode, defaultBrowserUrl, desktopWidth, desktopHeight, showBgImage,
                bgImageUri, bgOpacity, highlightToday, showTimeLine, showConflictWarning, conflictColor,
                preferredConflictIds, realTimeSlider, autoCheckUpdate, showWatermark,
                showCourseBorder, courseBorderColor,
                autoUsername, autoPassword, isAutoLoginEnabled, isAutoNavigateEnabled, defaultDesktopMode,
                themeColor,
                updateChannel
            ) {
                withContext(Dispatchers.IO) {
                    sharedPrefs.edit()
                        .putInt("theme_mode", themeMode)
                        .putBoolean("show_bg_image", showBgImage)
                        .putString("bg_image_uri", bgImageUri)
                        .putFloat("bg_opacity", bgOpacity)
                        .putString("default_url", defaultBrowserUrl)
                        .putString("auto_username", autoUsername)
                        .putString("auto_password", autoPassword)
                        .putBoolean("auto_login", isAutoLoginEnabled)
                        .putBoolean("auto_navigate", isAutoNavigateEnabled)
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
                        .putBoolean("default_desktop_mode", defaultDesktopMode)
                        .putLong("theme_color", themeColor)
                        .putInt("update_channel", updateChannel)
                        .apply()
                }
            }

            val resolvedThemeColor = resolveThemeColor(themeColor, useDarkTheme)
            FlowCourseTheme(darkTheme = useDarkTheme, themeColor = resolvedThemeColor) {
                val navController = rememberNavController()
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

                var pendingImportCourses by remember { mutableStateOf<List<Course>?>(null) }

                LaunchedEffect(Unit) {
                    updateAppWidget(context)
                    if (isFirstLaunch) {
                        navController.navigate("About")
                    } else if (!hasAgreed) {
                        navController.navigate("Agreement")
                    }
                    if (autoCheckUpdate) {
                        val todayStr = LocalDate.now().toString()
                        val lastCheckDate = sharedPrefs.getString("last_update_check_date", "")
                        if (lastCheckDate != todayStr) {
                            checkAppUpdate(currentAppVersion, updateChannel) { release, _ ->
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
                    withContext(Dispatchers.IO) {
                        sharedPrefs.edit()
                            .putString("timetables_data", gson.toJson(timetables))
                            .putInt("active_id", activeTimetableId)
                            .putString("time_profiles_data", gson.toJson(timeProfiles))
                            .apply()
                    }
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
                                    showBgImage = showBgImage, bgBitmap = bgBitmap, bgOpacity = bgOpacity,
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

                            // 1. 设置主页 (Settings)
                            composable(
                                route = "Settings",
                                // 从主页进入设置时：设置页滑入
                                enterTransition = { slideIntoContainer(towards = AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = tween(400)) },
                                // 进入二级子页面时：设置主页缩小 0.9f 并淡出（保持和主页退出一致）
                                exitTransition = { scaleOut(targetScale = 0.9f, animationSpec = tween(400)) + fadeOut(animationSpec = tween(400)) },
                                // 从二级子页面返回时：设置主页放大并淡入（保持和主页返回一致）
                                popEnterTransition = { scaleIn(initialScale = 0.9f, animationSpec = tween(400)) + fadeIn(animationSpec = tween(400)) },
                                // 返回主页时：设置页滑出
                                popExitTransition = { slideOutOfContainer(towards = AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = tween(400)) }
                            ) {
                                SettingsScreen(
                                    parserId = parserId,
                                    showWatermark = showWatermark,
                                    onShowWatermarkChange = { checked -> showWatermark = checked },
                                    onParserIdChange = { id -> parserId = id },
                                    themeMode = themeMode,
                                    onThemeChange = { theme -> themeMode = theme },
                                    updateChannel = updateChannel,
                                    onUpdateChannelChange = { channel -> updateChannel = channel },
                                    themeColor = resolvedThemeColor,
                                    onThemeColorChange = { color -> themeColor = color },
                                    autoCheckUpdate = autoCheckUpdate,
                                    onAutoCheckUpdateChange = { checked -> autoCheckUpdate = checked },
                                    onManualCheckUpdate = {
                                        Toast.makeText(context, "正在检查更新...", Toast.LENGTH_SHORT).show()
                                        coroutineScope.launch {
                                            checkAppUpdate(currentAppVersion, updateChannel) { release, isLatest ->
                                                if (release != null) {
                                                    updateInfo = release
                                                } else if (isLatest) {
                                                    Toast.makeText(context, "当前已是最新版本 ($currentAppVersion)", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    Toast.makeText(context, "检查失败，请检查网络或是否受限", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                    },
                                    showBgImage = showBgImage,
                                    onShowBgImageChange = { show -> showBgImage = show },
                                    bgImageUri = bgImageUri,
                                    onBgImageUriChange = { uri -> bgImageUri = uri },
                                    bgOpacity = bgOpacity,
                                    onBgOpacityChange = { opacity -> bgOpacity = opacity },
                                    highlightToday = highlightToday,
                                    onHighlightTodayChange = { highlight -> highlightToday = highlight },
                                    showTimeLine = showTimeLine,
                                    onShowTimeLineChange = { show -> showTimeLine = show },
                                    showConflictWarning = showConflictWarning,
                                    onShowConflictWarningChange = { show -> showConflictWarning = show },
                                    conflictColor = conflictColor,
                                    onConflictColorChange = { color -> conflictColor = color },
                                    realTimeSlider = realTimeSlider,
                                    onRealTimeSliderChange = { real -> realTimeSlider = real },
                                    onNavigateToAbout = { navController.navigate("About") },
                                    onNavigateToAgreement = { navController.navigate("Agreement?readOnly=true") },
                                    onNavigateToWebViewSettings = { navController.navigate("WebViewSettings") },
                                    onNavigateToAutoLoginSettings = { navController.navigate("AutoLoginSettings") },
                                    onBackClick = { navController.popBackStack() },
                                    showCourseBorder = showCourseBorder,
                                    onShowCourseBorderChange = { show -> showCourseBorder = show },
                                    courseBorderColor = courseBorderColor,
                                    onCourseBorderColorChange = { color -> courseBorderColor = color }
                                )
                            }

                            // 2. WebView配置页 (WebViewSettings)
                            composable(
                                route = "WebViewSettings",
                                // 进入子页面：滑入
                                enterTransition = { slideIntoContainer(towards = AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = tween(400)) },
                                // 返回主页面：滑出
                                popExitTransition = { slideOutOfContainer(towards = AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = tween(400)) }
                            ) {
                                WebViewSettingsScreen(
                                    savedUrl = defaultBrowserUrl,
                                    savedWidth = desktopWidth,
                                    savedHeight = desktopHeight,
                                    savedDesktopMode = defaultDesktopMode,
                                    onValueChange = { url, w, h, isDesktop ->
                                        defaultBrowserUrl = url
                                        desktopWidth = w
                                        desktopHeight = h
                                        defaultDesktopMode = isDesktop
                                    },
                                    onBackClick = { navController.popBackStack() }
                                )
                            }

                            // 3. 自动登录配置页 (AutoLoginSettings)
                            composable(
                                route = "AutoLoginSettings",
                                // 进入子页面：滑入
                                enterTransition = { slideIntoContainer(towards = AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = tween(400)) },
                                // 返回主页面：滑出
                                popExitTransition = { slideOutOfContainer(towards = AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = tween(400)) }
                            ) {
                                AutoLoginSettingsScreen(
                                    savedUsername = autoUsername,
                                    savedPassword = autoPassword,
                                    savedAutoLogin = isAutoLoginEnabled,
                                    savedAutoNavigate = isAutoNavigateEnabled,
                                    onValueChange = { user, pass, login, nav ->
                                        autoUsername = user
                                        autoPassword = pass
                                        isAutoLoginEnabled = login
                                        isAutoNavigateEnabled = nav
                                    },
                                    onBackClick = { navController.popBackStack() }
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
                                androidx.activity.compose.BackHandler(enabled = !hasAgreed) {
                                    sharedPrefs.edit().putBoolean("is_first_launch", false).apply()
                                    isFirstLaunch = false
                                    navController.popBackStack()
                                    navController.navigate("Agreement")
                                }

                                AboutScreen(onBackClick = {
                                    if (!hasAgreed) {
                                        sharedPrefs.edit().putBoolean("is_first_launch", false).apply()
                                        isFirstLaunch = false
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
                                val isReadOnly = backStackEntry.arguments?.getBoolean("readOnly") ?: false
                                AgreementScreen(
                                    isReadOnly = isReadOnly,
                                    onBackClick = { navController.popBackStack() },
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
                                    autoUsername = autoUsername, autoPassword = autoPassword, autoLogin = isAutoLoginEnabled, autoNavigate = isAutoNavigateEnabled,
                                    defaultDesktopMode = defaultDesktopMode,
                                    onBackClick = { navController.popBackStack() },
                                    onImportCourses = { importedCourses ->
                                        coroutineScope.launch {
                                            if (navController.currentDestination?.route == "Browser") {
                                                navController.popBackStack()
                                            }

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

                        if (pendingImportCourses != null) {
                            val existingCourses = activeTimetable?.courses ?: emptyList()
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
                                            val newCourses = processedImportCourses.mapIndexed { index, c -> c.copy(id = maxId + index + 1) }
                                            timetables = timetables.map { if (it.id == activeTimetableId) it.copy(courses = newCourses, totalWeeks = maxOf(it.totalWeeks, maxWeek)) else it }
                                            pendingImportCourses = null
                                            Toast.makeText(context, "已替换当前课表", Toast.LENGTH_SHORT).show()
                                        }, modifier = Modifier.fillMaxWidth()) { Text("替换当前课表的课程") }

                                        Button(onClick = {
                                            val maxId = timetables.flatMap { it.courses }.maxOfOrNull { it.id } ?: 0
                                            val maxWeek = processedImportCourses.flatMap { it.weekList }.maxOrNull() ?: activeTimetable?.totalWeeks ?: 20
                                            val newCourses = processedImportCourses.mapIndexed { index, c -> c.copy(id = maxId + index + 1) }
                                            timetables = timetables.map { if (it.id == activeTimetableId) it.copy(courses = it.courses + newCourses, totalWeeks = maxOf(it.totalWeeks, maxWeek)) else it }
                                            pendingImportCourses = null
                                            Toast.makeText(context, "已追加到当前课表", Toast.LENGTH_SHORT).show()
                                        }, modifier = Modifier.fillMaxWidth()) { Text("强制追加到当前课表") }

                                        Button(onClick = {
                                            val maxId = timetables.flatMap { it.courses }.maxOfOrNull { it.id } ?: 0
                                            val maxWeek = processedImportCourses.flatMap { it.weekList }.maxOrNull() ?: 20
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
    bgBitmap: ImageBitmap?,
    preferredConflictIds: Set<Int>,
    showWatermark: Boolean,
    showCourseBorder: Boolean,
    courseBorderColor: Long,
    onPreferredConflictChange: (Set<Int>) -> Unit,
    timetables: List<TimetableData>, activeTimetableId: Int, timeProfiles: List<TimeProfile>,
    currentActualWeek: Int, showBgImage: Boolean, bgOpacity: Float,
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

    val blurRadius by animateDpAsState(
        targetValue = if (drawerState.targetValue == DrawerValue.Open) 16.dp else 0.dp,
        label = "blur"
    )

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

    var showShareMenuDialog by remember { mutableStateOf(false) }
    var showImportMenuDialog by remember { mutableStateOf(false) }

    var showAutoUpdateDialog by remember { mutableStateOf(false) }
    val hasCredentials = !context.getSharedPreferences("FlowCourseDB", Context.MODE_PRIVATE).getString("auto_username", "").isNullOrEmpty() && !context.getSharedPreferences("FlowCourseDB", Context.MODE_PRIVATE).getString("auto_password", "").isNullOrEmpty()

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
                hasCredentials = hasCredentials,
                onShowAutoUpdateDialog = { showAutoUpdateDialog = true },
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
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .blur(blurRadius)
        ) {
            if (showBgImage && bgBitmap != null) {
                Image(bitmap = bgBitmap, contentDescription = "背景图片", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background.copy(alpha = 1f - bgOpacity))
                )
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

    if (showAutoUpdateDialog) {
        AutoUpdateTimetableDialog(
            onDismiss = { showAutoUpdateDialog = false },
            onSuccess = { newCourses ->
                showAutoUpdateDialog = false
                val existingCourses = activeTimetable?.courses ?: emptyList()
                val processedCourses = newCourses.mapIndexed { index, imported ->
                    val matched = existingCourses.find { it.name == imported.name }
                    val maxId = existingCourses.maxOfOrNull { it.id } ?: 0
                    imported.copy(
                        id = maxId + index + 1,
                        bgColor = matched?.bgColor ?: imported.bgColor,
                        textColor = matched?.textColor ?: imported.textColor
                    )
                }
                val newWeek = processedCourses.flatMap { it.weekList }.maxOrNull() ?: activeTimetable?.totalWeeks ?: 20

                val newTimetables = timetables.map {
                    if (it.id == activeTimetableId) it.copy(courses = processedCourses, totalWeeks = maxOf(it.totalWeeks, newWeek)) else it
                }
                onReorderTimetables(newTimetables)

                Toast.makeText(context, "课表自动更新完毕！", Toast.LENGTH_SHORT).show()
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
                    val week = i + 1
                    val isSelected = selectedWeek == week
                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(CircleShape)
                            .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { selectedWeek = week },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("$week", color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(selectedWeek) }) { Text("确定", color = MaterialTheme.colorScheme.primary) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消", color = MaterialTheme.colorScheme.onSurfaceVariant) } }
    )
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
            IconButton(onClick = onEditClick, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Edit, contentDescription = "编辑课程", tint = MaterialTheme.colorScheme.primary)
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
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

class JSBridge(val onResult: (String) -> Unit) {
    @JavascriptInterface
    fun onTimetableExtracted(html: String) { onResult(html) }
}

@Composable
fun AutoUpdateTimetableDialog(onDismiss: () -> Unit, onSuccess: (List<Course>) -> Unit) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("FlowCourseDB", Context.MODE_PRIVATE)
    val defaultUrl = prefs.getString("default_url", "http://www.cqwu.edu.cn/redir/redirTmp.jsp") ?: ""
    val autoUsername = prefs.getString("auto_username", "") ?: ""
    val autoPassword = prefs.getString("auto_password", "") ?: ""

    val logs = remember { mutableStateListOf<String>("初始化静默更新引擎...") }
    val coroutineScope = rememberCoroutineScope()
    var showWebView by remember { mutableStateOf(false) }

    fun log(msg: String) {
        coroutineScope.launch(Dispatchers.Main) {
            logs.add(0, msg)
            if (logs.size > 50) logs.removeAt(logs.lastIndex)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("正在自动更新课表", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                val logHeight by animateDpAsState(targetValue = if (showWebView) 100.dp else 250.dp, label = "logHeight")
                androidx.compose.foundation.lazy.LazyColumn(
                    modifier = Modifier
                        .height(logHeight)
                        .fillMaxWidth()
                        .background(Color(0xFF121212), RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    items(logs.size) { index ->
                        val msg = logs[index]
                        val color = when {
                            msg.contains("❌") -> Color(0xFFFF5252)
                            msg.contains("✅") -> Color(0xFF69F0AE)
                            else -> Color(0xFFB0BEC5)
                        }
                        Text(msg, color = color, fontSize = 11.sp, lineHeight = 16.sp, modifier = Modifier.padding(vertical = 2.dp))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                val webViewHeight by animateDpAsState(targetValue = if (showWebView) 350.dp else 1.dp, label = "wvHeight")
                val webViewAlpha by animateFloatAsState(targetValue = if (showWebView) 1f else 0f, label = "wvAlpha")

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(webViewHeight)
                        .alpha(webViewAlpha)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White)
                ) {
                    androidx.compose.ui.viewinterop.AndroidView(
                        factory = { ctx ->
                            com.tencent.smtt.sdk.WebView(ctx).apply {
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.setSupportMultipleWindows(false)
                                settings.setSupportZoom(true)
                                settings.builtInZoomControls = true
                                settings.displayZoomControls = false
                                settings.userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/146.0.0.0 Safari/537.36 Edg/146.0.0.0"

                                addJavascriptInterface(JSBridge { html ->
                                    log("✅ [解析] 解析到课表，正在处理...")
                                    coroutineScope.launch(Dispatchers.IO) {
                                        val courses = CqwlxyParser.parseCourseFromHtml(html)
                                        withContext(Dispatchers.Main) {
                                            if (courses.isNotEmpty()) {
                                                log("✅ [成功] 完美解析出 ${courses.size} 门课程！")
                                                kotlinx.coroutines.delay(600)
                                                onSuccess(courses)
                                            } else {
                                                log("❌ [错误] 未解析到课表")
                                            }
                                        }
                                    }
                                }, "AndroidBridge")

                                webViewClient = object : com.tencent.smtt.sdk.WebViewClient() {
                                    override fun onPageFinished(view: com.tencent.smtt.sdk.WebView?, url: String?) {
                                        super.onPageFinished(view, url)
                                        url?.let {
                                            log("到达: ${it.take(35)}...")
                                            CqwlxyParser.getAutoFillScript(it, autoUsername, autoPassword, true)?.let { script ->
                                                log("注入账号密码...")
                                                view?.evaluateJavascript(script, null)
                                            }
                                            CqwlxyParser.getSilentAutoNavigateScript(it)?.let { script ->
                                                log("执行跳转路由...")
                                                view?.evaluateJavascript(script, null)
                                            }
                                            CqwlxyParser.getSilentExtractScript(it)?.let { script ->
                                                log("放置课表提取探针...")
                                                view?.evaluateJavascript(script, null)
                                            }
                                        }
                                    }
                                }
                                webChromeClient = object : com.tencent.smtt.sdk.WebChromeClient() {
                                    override fun onConsoleMessage(msg: com.tencent.smtt.export.external.interfaces.ConsoleMessage?): Boolean {
                                        if (msg?.message()?.startsWith("[JS]") == true) log("${msg.message()}")
                                        return true
                                    }
                                }
                                log("连接教务系统...")
                                loadUrl(defaultUrl)
                            }
                        }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { showWebView = !showWebView }) {
                Text(if (showWebView) "隐藏网页" else "查看网页", color = MaterialTheme.colorScheme.primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = MaterialTheme.colorScheme.error)
            }
        }
    )
}