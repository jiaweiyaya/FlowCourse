package com.jiaweiya.flowcourse

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.core.*
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Brightness3
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalContext
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.text.style.TextOverflow
import kotlin.math.roundToInt
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.graphics.graphicsLayer

// 定义图标数据类
data class AppIconData(val id: Int, val alias: String, val iconRes: Int, val name: String)
val appIconsList = listOf(
    AppIconData(1, "com.jiaweiya.flowcourse.Alias1", R.drawable.app_icon, "默认图标"),
    AppIconData(2, "com.jiaweiya.flowcourse.Alias2", R.drawable.app_icon2, "Jiaweiya"),
    AppIconData(3, "com.jiaweiya.flowcourse.Alias3", R.drawable.app_icon3, "JM地狱！"),
    AppIconData(4, "com.jiaweiya.flowcourse.Alias4", R.drawable.app_icon4, "待定"),
    AppIconData(5, "com.jiaweiya.flowcourse.Alias5", R.drawable.app_icon5, "待定"),
    AppIconData(6, "com.jiaweiya.flowcourse.Alias6", R.drawable.app_icon6, "待定")
)

// 全局静态常量颜色选项，避免重组时重复计算分配内存
val colorOptions = listOf(
    0xFF9E77ED, 0xFFFF0000, 0xFFE91E63, 0xFF9C27B0,
    0xFF673AB7, 0xFF3F51B5, 0xFF03A9F4, 0xFF00BCD4,
    0xFF009688, 0xFF4CAF50, 0xFF8BC34A, 0xFFCDDC39,
    0xFFFFEB3B, 0xFFFFC107, 0xFFFF9800, 0xFFFF5722
).map { it.toLong() }

// 更换应用图标的方法
fun changeAppIcon(context: Context, targetAlias: String) {
    val pm = context.packageManager
    appIconsList.forEach { iconData ->
        val state = if (iconData.alias == targetAlias) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        } else {
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        }
        pm.setComponentEnabledSetting(
            ComponentName(context.packageName, iconData.alias),
            state,
            PackageManager.DONT_KILL_APP
        )
    }
    Toast.makeText(context, "图标更换成功，可能需要几秒钟在桌面上生效", Toast.LENGTH_SHORT).show()
}

// 保存二维码到手机相册
fun saveQrCodeToGallery(context: Context, coroutineScope: CoroutineScope) {
    coroutineScope.launch {
        val success = withContext(Dispatchers.IO) {
            try {
                val bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.qq_qrcode1)
                val contentValues = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, "FlowCourse_QQ_Group_${System.currentTimeMillis()}.png")
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/FlowCourse")
                    }
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { outputStream ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                    }
                    true
                } else false
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
        Toast.makeText(context, if (success) "二维码已保存到相册" else "保存失败", Toast.LENGTH_SHORT).show()
    }
}

// 同时保存多张赞助二维码到相册
fun saveImagesToGallery(context: Context, coroutineScope: CoroutineScope, imageResIds: List<Int>) {
    coroutineScope.launch {
        var successCount = 0
        withContext(Dispatchers.IO) {
            imageResIds.forEach { resId ->
                try {
                    val bitmap = BitmapFactory.decodeResource(context.resources, resId)
                    val contentValues = ContentValues().apply {
                        put(MediaStore.Images.Media.DISPLAY_NAME, "FlowCourse_Sponsor_${System.currentTimeMillis()}_$resId.png")
                        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/FlowCourse")
                        }
                    }
                    val resolver = context.contentResolver
                    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                    if (uri != null) {
                        resolver.openOutputStream(uri)?.use { outputStream ->
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                        }
                        successCount++
                    }
                } catch (e: Exception) { e.printStackTrace() }
            }
        }
        if (successCount == imageResIds.size) {
            Toast.makeText(context, "图片已全部保存到相册", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "保存完成，成功: $successCount, 失败: ${imageResIds.size - successCount}", Toast.LENGTH_SHORT).show()
        }
    }
}

@Composable
fun ScrollFadeIn(content: @Composable () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        visible = true
    }

    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(350),
        label = "scroll_fade_alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { this.alpha = alpha }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    parserId: Int,
    showWatermark: Boolean,
    onShowWatermarkChange: (Boolean) -> Unit,
    onParserIdChange: (Int) -> Unit,
    themeMode: Int,
    onThemeChange: (Int) -> Unit,
    autoCheckUpdate: Boolean,
    onAutoCheckUpdateChange: (Boolean) -> Unit,
    onManualCheckUpdate: () -> Unit,
    showBgImage: Boolean,
    onShowBgImageChange: (Boolean) -> Unit,
    bgImageUri: String?,
    onBgImageUriChange: (String?) -> Unit,
    bgOpacity: Float,
    onBgOpacityChange: (Float) -> Unit,
    highlightToday: Boolean,
    onHighlightTodayChange: (Boolean) -> Unit,
    showTimeLine: Boolean,
    onShowTimeLineChange: (Boolean) -> Unit,
    showConflictWarning: Boolean,
    onShowConflictWarningChange: (Boolean) -> Unit,
    conflictColor: Long,
    onConflictColorChange: (Long) -> Unit,
    realTimeSlider: Boolean,
    onRealTimeSliderChange: (Boolean) -> Unit,
    onNavigateToAbout: () -> Unit,
    onNavigateToAgreement: () -> Unit,
    onNavigateToWebViewSettings: () -> Unit,
    onNavigateToAutoLoginSettings: () -> Unit,
    onBackClick: () -> Unit,
    showCourseBorder: Boolean,
    onShowCourseBorderChange: (Boolean) -> Unit,
    courseBorderColor: Long,
    onCourseBorderColorChange: (Long) -> Unit,
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val coroutineScope = rememberCoroutineScope()
    var showFeedbackChannelDialog by remember { mutableStateOf(false) }
    var showQQGroupDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showSponsorDialog by remember { mutableStateOf(false) }

    var showParserDialog by remember { mutableStateOf(false) }
    val parsersList = listOf(Pair(1, "重庆文理学院"))
    val currentParserName = parsersList.find { it.first == parserId }?.second ?: "未知脚本"

    val themeOptions = listOf("跟随系统", "浅色模式", "深色模式")

    var currentAppVersion by remember { mutableStateOf("获取中...") }
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            currentAppVersion = try { context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0.0" } catch (e: Exception) { "1.0.0" }
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            onBgImageUriChange(uri.toString())
            onShowBgImageChange(true)
        }
    }

    var showColorDialog by remember { mutableStateOf(false) }
    var showBorderColorDialog by remember { mutableStateOf(false) }

    // 使用 rememberSaveable，在从子页面返回时瞬间加载
    var isTransitionFinished by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(450)
        isTransitionFinished = true
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0.dp),
                title = { Text("设置", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        },
        floatingActionButton = {
            Row(
                modifier = Modifier.offset(x = 4.dp, y = 6.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // 1. 反馈问题按钮
                FloatingScaleButton(
                    imageRes = R.drawable.issue,
                    text = "反馈问题",
                    onClick = { showFeedbackChannelDialog = true }
                )

                // 2. 用户协议按钮
                FloatingScaleButton(
                    imageRes = R.drawable.user_agreement,
                    text = "用户协议",
                    onClick = { onNavigateToAgreement() }
                )

                // 3. 关于此应用按钮
                FloatingScaleButton(
                    imageRes = R.drawable.jiaweiya_icon,
                    text = "关于此应用",
                    onClick = { onNavigateToAbout() }
                )
            }
        }
    ) { innerPadding ->
        val focusManager = androidx.compose.ui.platform.LocalFocusManager.current

        Crossfade(
            targetState = isTransitionFinished,
            animationSpec = tween(durationMillis = 400),
            label = "settings_fade"
        ) { finished ->
            if (!finished) {
                Box(modifier = Modifier.fillMaxSize())
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .pointerInput(Unit) {
                            detectTapGestures(onTap = { focusManager.clearFocus() })
                        }
                ) {
                    item {
                        ScrollFadeIn {
                            Text(
                                text = "主题与外观",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showThemeDialog = true }
                                    .padding(horizontal = 16.dp, vertical = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("切换主题", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val themeIcons = listOf(
                                        Icons.Default.BrightnessAuto,
                                        Icons.Default.WbSunny,
                                        Icons.Default.Brightness3
                                    )
                                    Icon(
                                        imageVector = themeIcons[themeMode],
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(themeOptions[themeMode], fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }

                            AppIconSettingsRow()

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), modifier = Modifier.padding(horizontal = 16.dp))
                        }
                    }

                    item {
                        ScrollFadeIn {
                            Text(
                                text = "应用更新",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onManualCheckUpdate() }
                                    .padding(horizontal = 16.dp, vertical = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("立即检查更新", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                                Text(
                                    text = "当前版本 $currentAppVersion",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("每天首次打开时检查更新", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                                Switch(checked = autoCheckUpdate, onCheckedChange = onAutoCheckUpdateChange, modifier = Modifier.scale(1.0f))
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), modifier = Modifier.padding(horizontal = 16.dp))
                        }
                    }

                    item {
                        ScrollFadeIn {
                            Text("课表解析", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showParserDialog = true }
                                    .padding(horizontal = 16.dp, vertical = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("课表解析脚本", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                                Text(currentParserName, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), modifier = Modifier.padding(horizontal = 16.dp))
                        }
                    }

                    item {
                        ScrollFadeIn {
                            Text("课表呈现", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("显示水印", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                                Switch(
                                    checked = showWatermark,
                                    onCheckedChange = { checked ->
                                        onShowWatermarkChange(checked)
                                        if (!checked) {
                                            showSponsorDialog = true
                                        }
                                    },
                                    modifier = Modifier.scale(1.0f)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("高亮当天与时间线", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("高亮", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(end = 4.dp))
                                    Switch(checked = highlightToday, onCheckedChange = onHighlightTodayChange, modifier = Modifier.scale(0.8f))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("时间线", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(end = 4.dp))
                                    Switch(checked = showTimeLine, onCheckedChange = onShowTimeLineChange, modifier = Modifier.scale(0.8f))
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("显示课程冲突警告角标", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(6.dp))
                                            .clip(RoundedCornerShape(6.dp))
                                            .clickable { showColorDialog = true },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(3.dp)
                                                .clip(RoundedCornerShape(3.dp))
                                                .background(Color(conflictColor))
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Switch(checked = showConflictWarning, onCheckedChange = onShowConflictWarningChange, modifier = Modifier.scale(1.0f))
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("显示课程卡片框线", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(6.dp))
                                            .clip(RoundedCornerShape(6.dp))
                                            .clickable { showBorderColorDialog = true },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(3.dp)
                                                .clip(RoundedCornerShape(3.dp))
                                                .background(Color(courseBorderColor))
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Switch(checked = showCourseBorder, onCheckedChange = onShowCourseBorderChange, modifier = Modifier.scale(1.0f))
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("周数滑块即时更新", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                                Switch(
                                    checked = realTimeSlider,
                                    onCheckedChange = onRealTimeSliderChange,
                                    modifier = Modifier.scale(1.0f)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("课表背景图片", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                                    Text(if (bgImageUri == null) "未选择" else "已选择", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "选择图片",
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier
                                            .clickable { imagePickerLauncher.launch(arrayOf("image/*")) }
                                            .padding(end = 16.dp)
                                    )
                                    Switch(
                                        checked = showBgImage,
                                        onCheckedChange = onShowBgImageChange,
                                        enabled = bgImageUri != null
                                    )
                                }
                            }

                            if (showBgImage && bgImageUri != null) {
                                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                                    Text("背景透明度", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Slider(
                                        value = bgOpacity,
                                        onValueChange = onBgOpacityChange,
                                        valueRange = 0.1f..1.0f
                                    )
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), modifier = Modifier.padding(horizontal = 16.dp))
                        }
                    }

                    item {
                        ScrollFadeIn {
                            Text(
                                text = "WebView与自动登录",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
                            )

                            SettingsRow(
                                title = "WebView配置",
                                subtitle = "配置默认教务网址、自适应分辨率以及默认加载模式",
                                onClick = onNavigateToWebViewSettings,
                                trailingContent = {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = "进入",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            )

                            SettingsRow(
                                title = "自动登录配置",
                                subtitle = "管理学号与密码的自动填充、回车登录以及自动跳转功能",
                                onClick = onNavigateToAutoLoginSettings,
                                trailingContent = {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = "进入",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            )

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), modifier = Modifier.padding(horizontal = 16.dp))
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(120.dp))
                    }
                }
            }
        }
    }

    val isSystemDark = isSystemInDarkTheme()

    if (showThemeDialog) {
        ThemeSelectionDialog(
            currentTheme = themeMode,
            onDismiss = { showThemeDialog = false },
            onSave = { newTheme ->
                onThemeChange(newTheme)

                if (newTheme == 2 || (newTheme == 0 && isSystemDark)) {
                    onShowCourseBorderChange(false)
                } else if (newTheme == 1 || (newTheme == 0 && !isSystemDark)) {
                    onShowCourseBorderChange(true)
                }

                showThemeDialog = false
            }
        )
    }

    if (showParserDialog) {
        AlertDialog(
            onDismissRequest = { showParserDialog = false },
            title = { Text("选择课表解析脚本", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    parsersList.forEach { (id, name) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onParserIdChange(id)
                                    showParserDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = parserId == id, onClick = { onParserIdChange(id); showParserDialog = false })
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(name, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "找不到你的学校的解析脚本？点我联系Jiaweiya",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showParserDialog = false
                                showFeedbackChannelDialog = true
                            }
                            .padding(vertical = 8.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showParserDialog = false }) { Text("取消", color = MaterialTheme.colorScheme.primary) }
            }
        )
    }

    if (showColorDialog) {
        ColorSelectionDialog(
            currentColor = conflictColor,
            colorOptions = colorOptions,
            onDismiss = { showColorDialog = false },
            onSave = {
                onConflictColorChange(it)
                showColorDialog = false
            }
        )
    }

    if (showBorderColorDialog) {
        ColorSelectionDialog(
            currentColor = courseBorderColor,
            colorOptions = colorOptions,
            onDismiss = { showBorderColorDialog = false },
            onSave = {
                onCourseBorderColorChange(it)
                showBorderColorDialog = false
            }
        )
    }

    if (showFeedbackChannelDialog) {
        AlertDialog(
            onDismissRequest = { showFeedbackChannelDialog = false },
            title = { Text("反馈问题", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Button(
                        onClick = {
                            showFeedbackChannelDialog = false
                            uriHandler.openUri("https://github.com/jiaweiyaya/FlowCourse/issues/new")
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("在Github中提交issue")
                    }
                    Button(
                        onClick = {
                            showFeedbackChannelDialog = false
                            showQQGroupDialog = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("在QQ群中反馈问题")
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showFeedbackChannelDialog = false }) {
                    Text("取消", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    if (showQQGroupDialog) {
        AlertDialog(
            onDismissRequest = { showQQGroupDialog = false },
            title = { Text("加入QQ群", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.qq_qrcode1),
                        contentDescription = "QQ群二维码",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 280.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "1群：1074858712",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showQQGroupDialog = false }) {
                    Text("关闭", color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { saveQrCodeToGallery(context, coroutineScope) }) {
                    Text("保存到相册", color = MaterialTheme.colorScheme.primary)
                }
            }
        )
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
                TextButton(onClick = { showSponsorDialog = false }) {
                    Text("关闭", color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    saveImagesToGallery(context, coroutineScope, listOf(R.drawable.wechatcode, R.drawable.alpaycode))
                }) {
                    Text("保存到相册", color = MaterialTheme.colorScheme.primary)
                }
            }
        )
    }
}

@Composable
fun AppIconSettingsRow() {
    val context = LocalContext.current
    val sharedPrefs = context.getSharedPreferences("FlowCourseDB", Context.MODE_PRIVATE)

    var currentIconId by remember { mutableIntStateOf(1) }
    var showDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            currentIconId = sharedPrefs.getInt("app_icon_id", 1)
        }
    }

    val currentIconData = appIconsList.find { it.id == currentIconId } ?: appIconsList.first()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showDialog = true }
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("更换应用图标", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Image(
                painter = painterResource(id = currentIconData.iconRes),
                contentDescription = null,
                modifier = Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(5.dp))
            )
            Text(currentIconData.name, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }

    if (showDialog) {
        IconSelectionDialog(
            currentId = currentIconId,
            onDismiss = { showDialog = false },
            onSave = { newIcon ->
                currentIconId = newIcon.id
                sharedPrefs.edit().putInt("app_icon_id", newIcon.id).commit()
                changeAppIcon(context, newIcon.alias)
                showDialog = false
            }
        )
    }
}

@Composable
fun IconSelectionDialog(currentId: Int, onDismiss: () -> Unit, onSave: (AppIconData) -> Unit) {
    var selectedId by remember { mutableIntStateOf(currentId) }

    val itemBoundsInRoot = remember { mutableStateMapOf<Int, Rect>() }
    var boxBoundsInRoot by remember { mutableStateOf(Rect.Zero) }
    val density = LocalDensity.current
    val context = LocalContext.current

    var showConfirmDialog by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择图标", fontWeight = FontWeight.Bold) },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { coords ->
                        boxBoundsInRoot = coords.boundsInRoot()
                    }
            ) {
                val targetItemRoot = itemBoundsInRoot[selectedId] ?: Rect.Zero
                val targetRelative = if (targetItemRoot != Rect.Zero && boxBoundsInRoot != Rect.Zero) {
                    targetItemRoot.translate(-boxBoundsInRoot.left, -boxBoundsInRoot.top)
                } else {
                    Rect.Zero
                }

                if (targetRelative != Rect.Zero) {
                    val padding = 8.dp
                    val paddingPx = with(density) { padding.toPx() }
                    val animSpec = spring<Float>(dampingRatio = 0.65f, stiffness = 400f)

                    val animLeft by animateFloatAsState(targetRelative.left - paddingPx, animSpec, label = "X")
                    val animTop by animateFloatAsState(targetRelative.top - paddingPx, animSpec, label = "Y")
                    val animWidth by animateFloatAsState(targetRelative.width + paddingPx * 2, animSpec, label = "W")
                    val animHeight by animateFloatAsState(targetRelative.height + paddingPx * 2, animSpec, label = "H")

                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .offset { IntOffset(animLeft.roundToInt(), animTop.roundToInt()) }
                            .size(with(density) { animWidth.toDp() }, with(density) { animHeight.toDp() })
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                            .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp))
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentWidth(Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val chunkedIcons = appIconsList.chunked(3)
                    chunkedIcons.forEach { rowItems ->
                        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                            rowItems.forEach { item ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .onGloballyPositioned { coords ->
                                            itemBoundsInRoot[item.id] = coords.boundsInRoot()
                                        }
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) {
                                            selectedId = item.id
                                        }
                                        .width(60.dp)
                                ) {
                                    Image(
                                        painter = painterResource(id = item.iconRes),
                                        contentDescription = item.name,
                                        modifier = Modifier
                                            .size(60.dp)
                                            .clip(RoundedCornerShape(13.dp))
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = item.name,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            val hasChanged = selectedId != currentId

            val animatedContainerColor by animateColorAsState(
                targetValue = if (hasChanged) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                animationSpec = tween(300), label = "btnBgAnim"
            )
            val animatedTextColor by animateColorAsState(
                targetValue = if (hasChanged) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                animationSpec = tween(300), label = "btnTxtAnim"
            )

            Button(
                onClick = {
                    showConfirmDialog = true
                },
                enabled = hasChanged,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = animatedContainerColor,
                    contentColor = animatedTextColor,
                    disabledContainerColor = animatedContainerColor,
                    disabledContentColor = animatedTextColor
                )
            ) {
                Text(text = "保存并应用")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("确认更换？", fontWeight = FontWeight.Bold) },
            text = { Text("更换应用图标与名称后，应用将自动重启以刷新桌面缓存并立即生效。是否确认更换？") },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDialog = false
                        onSave(appIconsList.first { it.id == selectedId })

                        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                        if (intent != null) {
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                            context.startActivity(intent)
                            Runtime.getRuntime().exit(0)
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("确认重启")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("取消", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }
}

@Composable
fun ColorSelectionDialog(
    currentColor: Long,
    colorOptions: List<Long>,
    onDismiss: () -> Unit,
    onSave: (Long) -> Unit
) {
    var selectedColor by remember { mutableLongStateOf(currentColor) }
    val itemBoundsInRoot = remember { mutableStateMapOf<Long, Rect>() }
    var boxBoundsInRoot by remember { mutableStateOf(Rect.Zero) }
    val density = LocalDensity.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择颜色", fontWeight = FontWeight.Bold) },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .onGloballyPositioned { coords -> boxBoundsInRoot = coords.boundsInRoot() }
            ) {
                val targetItemRoot = itemBoundsInRoot[selectedColor] ?: Rect.Zero
                val targetRelative = if (targetItemRoot != Rect.Zero && boxBoundsInRoot != Rect.Zero) {
                    targetItemRoot.translate(-boxBoundsInRoot.left, -boxBoundsInRoot.top)
                } else Rect.Zero

                if (targetRelative != Rect.Zero) {
                    val padding = 8.dp
                    val paddingPx = with(density) { padding.toPx() }
                    val animSpec = spring<Float>(dampingRatio = 0.65f, stiffness = 400f)

                    val animLeft by animateFloatAsState(targetRelative.left - paddingPx, animSpec, label = "X")
                    val animTop by animateFloatAsState(targetRelative.top - paddingPx, animSpec, label = "Y")
                    val animWidth by animateFloatAsState(targetRelative.width + paddingPx * 2, animSpec, label = "W")
                    val animHeight by animateFloatAsState(targetRelative.height + paddingPx * 2, animSpec, label = "H")

                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .offset { IntOffset(animLeft.roundToInt(), animTop.roundToInt()) }
                            .size(with(density) { animWidth.toDp() }, with(density) { animHeight.toDp() })
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                            .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp))
                    )
                }

                Column(
                    modifier = Modifier.fillMaxWidth().wrapContentWidth(Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    val chunkedColors = colorOptions.chunked(4)
                    chunkedColors.forEach { rowItems ->
                        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                            rowItems.forEach { colorVal ->
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .onGloballyPositioned { coords -> itemBoundsInRoot[colorVal] = coords.boundsInRoot() }
                                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                            selectedColor = colorVal
                                        }
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(colorVal)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (currentColor == colorVal) {
                                        Icon(Icons.Default.Check, contentDescription = "选中", tint = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            val hasChanged = selectedColor != currentColor

            val animatedContainerColor by animateColorAsState(
                targetValue = if (hasChanged) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                animationSpec = tween(300), label = "btnBgAnim"
            )
            val animatedTextColor by animateColorAsState(
                targetValue = if (hasChanged) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                animationSpec = tween(300), label = "btnTxtAnim"
            )

            Button(
                onClick = { onSave(selectedColor) },
                enabled = hasChanged,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = animatedContainerColor,
                    contentColor = animatedTextColor,
                    disabledContainerColor = animatedContainerColor,
                    disabledContentColor = animatedTextColor
                )
            ) {
                Text(text = "保存并应用")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}

@Composable
fun ThemeSelectionDialog(
    currentTheme: Int,
    onDismiss: () -> Unit,
    onSave: (Int) -> Unit
) {
    var selectedTheme by remember { mutableIntStateOf(currentTheme) }
    val itemBoundsInRoot = remember { mutableStateMapOf<Int, Rect>() }
    var boxBoundsInRoot by remember { mutableStateOf(Rect.Zero) }
    val density = LocalDensity.current

    val themeOptions = listOf(
        Triple("跟随系统", Icons.Default.BrightnessAuto, 0),
        Triple("浅色模式", Icons.Default.WbSunny, 1),
        Triple("深色模式", Icons.Default.Brightness3, 2)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择主题", fontWeight = FontWeight.Bold) },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .onGloballyPositioned { coords -> boxBoundsInRoot = coords.boundsInRoot() }
            ) {
                val targetItemRoot = itemBoundsInRoot[selectedTheme] ?: Rect.Zero
                val targetRelative = if (targetItemRoot != Rect.Zero && boxBoundsInRoot != Rect.Zero) {
                    targetItemRoot.translate(-boxBoundsInRoot.left, -boxBoundsInRoot.top)
                } else Rect.Zero

                if (targetRelative != Rect.Zero) {
                    val animSpec = spring<Float>(dampingRatio = 0.65f, stiffness = 400f)

                    val animLeft by animateFloatAsState(targetRelative.left, animSpec, label = "X")
                    val animTop by animateFloatAsState(targetRelative.top, animSpec, label = "Y")
                    val animWidth by animateFloatAsState(targetRelative.width, animSpec, label = "W")
                    val animHeight by animateFloatAsState(targetRelative.height, animSpec, label = "H")

                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .offset { IntOffset(animLeft.roundToInt(), animTop.roundToInt()) }
                            .size(with(density) { animWidth.toDp() }, with(density) { animHeight.toDp() })
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                            .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                    )
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    themeOptions.forEach { (title, icon, index) ->
                        val isSelected = selectedTheme == index

                        val itemBgColor by animateColorAsState(
                            targetValue = if (isSelected) Color.Transparent
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                            animationSpec = tween(300),
                            label = "itemBgAnim"
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .onGloballyPositioned { coords -> itemBoundsInRoot[index] = coords.boundsInRoot() }
                                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                    selectedTheme = index
                                }
                                .clip(RoundedCornerShape(12.dp))
                                .background(itemBgColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = title,
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = title,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            val hasChanged = selectedTheme != currentTheme

            val animatedContainerColor by animateColorAsState(
                targetValue = if (hasChanged) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                animationSpec = tween(300), label = "btnBgAnim"
            )
            val animatedTextColor by animateColorAsState(
                targetValue = if (hasChanged) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                animationSpec = tween(300), label = "btnTxtAnim"
            )

            Button(
                onClick = { onSave(selectedTheme) },
                enabled = hasChanged,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = animatedContainerColor,
                    contentColor = animatedTextColor,
                    disabledContainerColor = animatedContainerColor,
                    disabledContentColor = animatedTextColor
                )
            ) {
                Text(text = "保存并应用")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}

@Composable
fun SettingsRow(
    title: String,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (trailingContent != null) {
            Spacer(modifier = Modifier.width(16.dp))
            trailingContent()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebViewSettingsScreen(
    savedUrl: String,
    savedWidth: Int,
    savedHeight: Int,
    savedDesktopMode: Boolean,
    onValueChange: (String, Int, Int, Boolean) -> Unit,
    onBackClick: () -> Unit
) {
    val sysDefaultUrl = "http://www.cqwu.edu.cn/redir/redirTmp.jsp"
    val sysDefaultWidth = 1920
    val sysDefaultHeight = 1080
    val sysDefaultDesktop = false

    var showModeDialog by remember { mutableStateOf(false) }

    val isNotDefault = savedUrl != sysDefaultUrl ||
            savedWidth != sysDefaultWidth ||
            savedHeight != sysDefaultHeight ||
            savedDesktopMode != sysDefaultDesktop

    // 获取当前页面的 focusManager
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0.dp),
                title = { Text("WebView配置", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    AnimatedVisibility(
                        visible = isNotDefault,
                        enter = fadeIn() + expandHorizontally(),
                        exit = fadeOut() + shrinkHorizontally()
                    ) {
                        TextButton(onClick = {
                            onValueChange(sysDefaultUrl, sysDefaultWidth, sysDefaultHeight, sysDefaultDesktop)
                        }) {
                            Text("恢复默认", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        // 使用 Box 替代原有的 Column 根布局，并添加点击手势捕捉
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { focusManager.clearFocus() })
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = savedUrl,
                    onValueChange = { onValueChange(it, savedWidth, savedHeight, savedDesktopMode) },
                    label = { Text("默认浏览器网址") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = savedWidth.toString(),
                        onValueChange = {
                            val widthVal = it.toIntOrNull() ?: sysDefaultWidth
                            onValueChange(savedUrl, widthVal, savedHeight, savedDesktopMode)
                        },
                        label = { Text("电脑版宽度") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = savedHeight.toString(),
                        onValueChange = {
                            val heightVal = it.toIntOrNull() ?: sysDefaultHeight
                            onValueChange(savedUrl, savedWidth, heightVal, savedDesktopMode)
                        },
                        label = { Text("电脑版高度") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Text(
                    "提示：在电脑模式下，页面将模拟设置的分辨率强制渲染。",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showModeDialog = true }
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("默认加载模式", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (savedDesktopMode) Icons.Default.Computer else Icons.Default.Phone,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (savedDesktopMode) "电脑版" else "手机版",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    if (showModeDialog) {
        DesktopModeSelectionDialog(
            currentMode = savedDesktopMode,
            onDismiss = { showModeDialog = false },
            onSave = { isDesktop ->
                onValueChange(savedUrl, savedWidth, savedHeight, isDesktop)
                showModeDialog = false
            }
        )
    }
}

@Composable
fun DesktopModeSelectionDialog(
    currentMode: Boolean,
    onDismiss: () -> Unit,
    onSave: (Boolean) -> Unit
) {
    var selectedIndex by remember { mutableIntStateOf(if (currentMode) 1 else 0) }
    val itemBoundsInRoot = remember { mutableStateMapOf<Int, Rect>() }
    var boxBoundsInRoot by remember { mutableStateOf(Rect.Zero) }
    val density = LocalDensity.current

    val modeOptions = listOf(
        Triple("手机版", Icons.Default.Phone, 0),
        Triple("电脑版", Icons.Default.Computer, 1)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择模式", fontWeight = FontWeight.Bold) },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .onGloballyPositioned { coords -> boxBoundsInRoot = coords.boundsInRoot() }
            ) {
                val targetItemRoot = itemBoundsInRoot[selectedIndex] ?: Rect.Zero
                val targetRelative = if (targetItemRoot != Rect.Zero && boxBoundsInRoot != Rect.Zero) {
                    targetItemRoot.translate(-boxBoundsInRoot.left, -boxBoundsInRoot.top)
                } else Rect.Zero

                if (targetRelative != Rect.Zero) {
                    val animSpec = spring<Float>(dampingRatio = 0.65f, stiffness = 400f)
                    val animLeft by animateFloatAsState(targetRelative.left, animSpec, label = "X")
                    val animTop by animateFloatAsState(targetRelative.top, animSpec, label = "Y")
                    val animWidth by animateFloatAsState(targetRelative.width, animSpec, label = "W")
                    val animHeight by animateFloatAsState(targetRelative.height, animSpec, label = "H")

                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .offset { IntOffset(animLeft.roundToInt(), animTop.roundToInt()) }
                            .size(with(density) { animWidth.toDp() }, with(density) { animHeight.toDp() })
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                            .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                    )
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    modeOptions.forEach { (title, icon, index) ->
                        val isSelected = selectedIndex == index
                        val itemBgColor by animateColorAsState(
                            targetValue = if (isSelected) Color.Transparent
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                            animationSpec = tween(300),
                            label = "itemBgAnim"
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .onGloballyPositioned { coords -> itemBoundsInRoot[index] = coords.boundsInRoot() }
                                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                    selectedIndex = index
                                }
                                .clip(RoundedCornerShape(12.dp))
                                .background(itemBgColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = title,
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = title,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            val hasChanged = (selectedIndex == 1) != currentMode
            val animatedContainerColor by animateColorAsState(
                targetValue = if (hasChanged) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                animationSpec = tween(300), label = "btnBgAnim"
            )
            val animatedTextColor by animateColorAsState(
                targetValue = if (hasChanged) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                animationSpec = tween(300), label = "btnTxtAnim"
            )

            Button(
                onClick = { onSave(selectedIndex == 1) },
                enabled = hasChanged,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = animatedContainerColor,
                    contentColor = animatedTextColor,
                    disabledContainerColor = animatedContainerColor,
                    disabledContentColor = animatedTextColor
                )
            ) {
                Text(text = "保存并应用")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoLoginSettingsScreen(
    savedUsername: String,
    savedPassword: String,
    savedAutoLogin: Boolean,
    savedAutoNavigate: Boolean,
    onValueChange: (String, String, Boolean, Boolean) -> Unit,
    onBackClick: () -> Unit
) {
    val sysDefaultUser = ""
    val sysDefaultPass = ""
    val sysDefaultLogin = false
    val sysDefaultNav = false

    val isNotDefault = savedUsername != sysDefaultUser ||
            savedPassword != sysDefaultPass ||
            savedAutoLogin != sysDefaultLogin ||
            savedAutoNavigate != sysDefaultNav

    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current

    // 用于记录两个输入框上一次的焦点状态，从而准确判断是否为“退出输入”（即从 hasFocus == true 变为 false）
    var usernameHasFocus by remember { mutableStateOf(false) }
    var passwordHasFocus by remember { mutableStateOf(false) }

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0.dp),
                title = { Text("自动登录配置", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    AnimatedVisibility(
                        visible = isNotDefault,
                        enter = fadeIn() + expandHorizontally(),
                        exit = fadeOut() + shrinkHorizontally()
                    ) {
                        TextButton(onClick = {
                            onValueChange(sysDefaultUser, sysDefaultPass, sysDefaultLogin, sysDefaultNav)
                        }) {
                            Text("恢复默认", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { focusManager.clearFocus() })
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 账号输入框
                OutlinedTextField(
                    value = savedUsername,
                    onValueChange = {
                        val loginVal = if (it.isEmpty()) false else savedAutoLogin
                        val navVal = if (it.isEmpty()) false else savedAutoNavigate
                        onValueChange(it, savedPassword, loginVal, navVal)
                    },
                    label = { Text("教务系统账号") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { focusState ->
                            // 当从有焦点变为无焦点（即退出输入）
                            if (usernameHasFocus && !focusState.isFocused) {
                                if (savedUsername.isNotEmpty() && savedPassword.isNotEmpty()) {
                                    onValueChange(savedUsername, savedPassword, true, true)
                                }
                            }
                            usernameHasFocus = focusState.isFocused
                        },
                    singleLine = true
                )

                // 密码输入框
                OutlinedTextField(
                    value = savedPassword,
                    onValueChange = {
                        val loginVal = if (it.isEmpty()) false else savedAutoLogin
                        onValueChange(savedUsername, it, loginVal, savedAutoNavigate)
                    },
                    label = { Text("教务系统密码") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { focusState ->
                            // 当从有焦点变为无焦点（即退出输入）
                            if (passwordHasFocus && !focusState.isFocused) {
                                if (savedUsername.isNotEmpty() && savedPassword.isNotEmpty()) {
                                    onValueChange(savedUsername, savedPassword, true, true)
                                }
                            }
                            passwordHasFocus = focusState.isFocused
                        },
                    singleLine = true
                )

                // 自动回车登录开关
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("自动回车登录", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                        Text("填入密码后自动尝试执行登录", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = savedAutoLogin,
                        onCheckedChange = { checked ->
                            // 如果开关被打开，且账号和密码都不为空，则自动打开下面的开关
                            val navVal = if (checked) {
                                if (savedUsername.isNotEmpty() && savedPassword.isNotEmpty()) true else savedAutoNavigate
                            } else {
                                false
                            }
                            onValueChange(savedUsername, savedPassword, checked, navVal)
                        },
                        enabled = savedUsername.isNotEmpty() && savedPassword.isNotEmpty(),
                        modifier = Modifier.scale(1.0f)
                    )
                }

                // 登录后自动打开教务系统开关
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("登录后自动打开教务系统", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                        Text("需开启自动回车登录，自动跳转至“教学管理系统”", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = savedAutoNavigate,
                        onCheckedChange = { onValueChange(savedUsername, savedPassword, savedAutoLogin, it) },
                        enabled = savedAutoLogin,
                        modifier = Modifier.scale(1.0f)
                    )
                }
            }
        }
    }
}

@Composable
fun FloatingScaleButton(
    imageRes: Int,
    text: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    // 实时收集按压状态
    val isPressed by interactionSource.collectIsPressedAsState()

    // 弹性缩放动画：按下时缩放到 0.9 倍，松开时恢复 1.0 倍
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy, // 中等弹性
            stiffness = Spring.StiffnessMedium              // 中等刚度，反馈更灵敏
        ),
        label = "floating_button_scale"
    )

    Column(
        modifier = Modifier
            // 1. 使用 graphicsLayer 控制缩放，避免不必要的重新布局，渲染性能极佳
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(16.dp))
            // 2. 将点击事件与按压监听绑定
            .clickable(
                interactionSource = interactionSource,
                indication = androidx.compose.foundation.LocalIndication.current, // 保留原有的水波纹效果
                onClick = onClick
            )
            .padding(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = imageRes),
            contentDescription = text,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(60.dp)
                .clip(RoundedCornerShape(13.dp))
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )
    }
}