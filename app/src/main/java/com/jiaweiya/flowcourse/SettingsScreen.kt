package com.jiaweiya.flowcourse

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalUriHandler
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
            // 使用 DONT_KILL_APP 防止应用直接崩溃闪退
            PackageManager.DONT_KILL_APP
        )
    }
    Toast.makeText(context, "图标更换成功，可能需要几秒钟在桌面上生效", Toast.LENGTH_SHORT).show()
}

// 保存二维码图片到手机相册
fun saveQrCodeToGallery(context: Context, coroutineScope: CoroutineScope) {
    coroutineScope.launch {
        val success = withContext(Dispatchers.IO) {
            try {
                val bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.qq_qrcode1)
                val contentValues = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, "FlowCourse_QQ_Group_${System.currentTimeMillis()}.png")
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    // 适配 Android 10 及以上：指定保存到 Pictures 文件夹下
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


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    parserId: Int,
    onParserIdChange: (Int) -> Unit,
    themeMode: Int,
    onThemeChange: (Int) -> Unit,
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
    savedBrowserUrl: String,
    onBrowserSettingsSave: (String, Int, Int) -> Unit,
    savedDesktopWidth: Int,
    savedDesktopHeight: Int,
    autoCheckUpdate: Boolean,
    onAutoCheckUpdateChange: (Boolean) -> Unit,
    onManualCheckUpdate: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onNavigateToAgreement: () -> Unit,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val currentAppVersion = remember { try { context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0.0" } catch (e: Exception) { "1.0.0" } }
    val coroutineScope = rememberCoroutineScope()
    var showFeedbackChannelDialog by remember { mutableStateOf(false) } // 控制渠道选择弹窗
    var showQQGroupDialog by remember { mutableStateOf(false) }         // 控制QQ二维码弹窗
    var showThemeDialog by remember { mutableStateOf(false) }

    var showParserDialog by remember { mutableStateOf(false) }
    val parsersList = listOf(Pair(1, "重庆文理学院"))
    val currentParserName = parsersList.find { it.first == parserId }?.second ?: "未知脚本"

    val themeOptions = listOf("跟随系统", "浅色模式", "深色模式")

    val sysDefaultUrl = "http://www.cqwu.edu.cn/redir/redirTmp.jsp"
    val sysDefaultWidth = 1920
    val sysDefaultHeight = 1080

    var tempUrl by remember(savedBrowserUrl) { mutableStateOf(savedBrowserUrl) }
    var tempWidth by remember(savedDesktopWidth) { mutableStateOf(savedDesktopWidth.toString()) }
    var tempHeight by remember(savedDesktopHeight) { mutableStateOf(savedDesktopHeight.toString()) }

    var showColorDialog by remember { mutableStateOf(false) }
    val colorOptions = listOf(
        0xFFFF0000, 0xFFE91E63, 0xFF9C27B0, 0xFF673AB7, 0xFF3F51B5,
        0xFF2196F3, 0xFF03A9F4, 0xFF00BCD4, 0xFF009688, 0xFF4CAF50,
        0xFF8BC34A, 0xFFCDDC39, 0xFFFFEB3B, 0xFFFFC107, 0xFFFF9800, 0xFFFF5722
    ).map { it.toLong() }

    // 图片选择器
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

    val isModified = tempUrl != savedBrowserUrl ||
            tempWidth != savedDesktopWidth.toString() ||
            tempHeight != savedDesktopHeight.toString()

    val isNotDefault = savedBrowserUrl != sysDefaultUrl ||
            savedDesktopWidth != sysDefaultWidth ||
            savedDesktopHeight != sysDefaultHeight

    val buttonState = when {
        isModified -> "SAVE"
        isNotDefault -> "RESET"
        else -> "NONE"
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
                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable {
                            showFeedbackChannelDialog = true
                        }
                        .padding(6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.issue),
                        contentDescription = "反馈问题",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(13.dp))
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "反馈问题",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                }

                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onNavigateToAgreement() }
                        .padding(6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.user_agreement),
                        contentDescription = "用户服务协议",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(13.dp))
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "用户协议",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center
                    )
                }

                Column(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onNavigateToAbout() }
                        .padding(6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.jiaweiya_icon),
                        contentDescription = "关于此应用",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(13.dp))
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "关于此应用",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    ) { innerPadding ->
        val scrollState = rememberScrollState()
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding).verticalScroll(scrollState)) {

            // 应用设置
            Text(
                text = "应用设置",
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
                Text(themeOptions[themeMode], fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            AppIconSettingsRow()

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), modifier = Modifier.padding(horizontal = 16.dp))

            Text("更新", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp))

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
                Text("每天自动检查更新", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                Switch(checked = autoCheckUpdate, onCheckedChange = onAutoCheckUpdateChange, modifier = Modifier.scale(1.0f))
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), modifier = Modifier.padding(horizontal = 16.dp))

            Text("课表解析设置", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp))
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

            // 课表呈现设置
            Text("课表呈现设置", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp))

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
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(Color(conflictColor))
                            .clickable { showColorDialog = true }
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Switch(checked = showConflictWarning, onCheckedChange = onShowConflictWarningChange, modifier = Modifier.scale(1.0f))
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

            // 浏览器设置
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("浏览器设置", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

                AnimatedContent(
                    targetState = buttonState,
                    transitionSpec = { fadeIn(animationSpec = tween(200)) togetherWith fadeOut(animationSpec = tween(200)) },
                    label = "browser_settings_btn"
                ) { state ->
                    when (state) {
                        "SAVE" -> {
                            Text(
                                "保存",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.clickable {
                                    onBrowserSettingsSave(tempUrl, tempWidth.toIntOrNull() ?: sysDefaultWidth, tempHeight.toIntOrNull() ?: sysDefaultHeight)
                                }
                            )
                        }
                        "RESET" -> {
                            Text(
                                "恢复默认",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.clickable {
                                    onBrowserSettingsSave(sysDefaultUrl, sysDefaultWidth, sysDefaultHeight)
                                }
                            )
                        }
                        else -> { Spacer(modifier = Modifier.width(50.dp)) }
                    }
                }
            }

            OutlinedTextField(
                value = tempUrl,
                onValueChange = { tempUrl = it },
                label = { Text("默认浏览器网址") },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = tempWidth,
                    onValueChange = { tempWidth = it },
                    label = { Text("电脑版宽度") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = tempHeight,
                    onValueChange = { tempHeight = it },
                    label = { Text("电脑版高度") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "提示：在电脑模式下，页面将模拟设置的分辨率强制渲染。",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(120.dp))
        }
    }

    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("选择主题", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    themeOptions.forEachIndexed { index, title ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onThemeChange(index)
                                    showThemeDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = themeMode == index, onClick = { onThemeChange(index); showThemeDialog = false })
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(title, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) { Text("取消", color = MaterialTheme.colorScheme.primary) }
            }
        )
    }

    // 解析脚本选择弹窗
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

                    // 点击后触发之前的 showFeedbackChannelDialog
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
        AlertDialog(
            onDismissRequest = { showColorDialog = false },
            title = { Text("选择角标颜色", fontWeight = FontWeight.Bold) },
            text = {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(8.dp)
                ) {
                    items(colorOptions) { colorVal ->
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(colorVal))
                                .clickable {
                                    onConflictColorChange(colorVal)
                                    showColorDialog = false
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (conflictColor == colorVal) {
                                Icon(Icons.Default.Check, contentDescription = "选中", tint = Color.White)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showColorDialog = false }) { Text("取消", color = MaterialTheme.colorScheme.primary) }
            }
        )
    }
    // 反馈渠道选择弹窗
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

    // QQ群二维码弹窗
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
                        contentScale = ContentScale.Fit, // Fit 确保图片等比例完整显示
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 280.dp) // 限制最大高度
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
}

@Composable
fun AppIconSettingsRow() {
    val context = LocalContext.current
    val sharedPrefs = context.getSharedPreferences("FlowCourseDB", Context.MODE_PRIVATE)

    // 从本地读取当前使用的图标 ID，默认是 1
    var currentIconId by remember { mutableIntStateOf(sharedPrefs.getInt("app_icon_id", 1)) }
    var showDialog by remember { mutableStateOf(false) }

    val currentIconData = appIconsList.find { it.id == currentIconId } ?: appIconsList.first()

    // 外层入口卡片
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp)
        ) {
            Image(
                painter = painterResource(id = currentIconData.iconRes),
                contentDescription = null,
                modifier = Modifier.size(50.dp).clip(RoundedCornerShape(12.dp))
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "更换应用图标",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = currentIconData.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Button(
                onClick = { showDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF9E77ED)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("更换", fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }

    // 弹窗部分
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
    val context = LocalContext.current // 获取上下文用于重启应用

    // 控制二次确认弹窗的状态
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
                            .background(Color(0xFFE48AFF).copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                            .border(2.dp, Color(0xFFE48AFF), RoundedCornerShape(16.dp))
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
                targetValue = if (hasChanged) Color(0xFFE48AFF) else MaterialTheme.colorScheme.surfaceVariant,
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

    // 二次确认弹窗及其重启逻辑
    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("确认更换？", fontWeight = FontWeight.Bold) },
            text = { Text("更换应用图标与名称后，应用将自动重启以刷新桌面缓存并立即生效。是否确认更换？") },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDialog = false
                        // 触发外层的保存修改操作(更换底层别名)
                        onSave(appIconsList.first { it.id == selectedId })

                        // 自动重启应用代码
                        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
                        if (intent != null) {
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                            context.startActivity(intent)
                            Runtime.getRuntime().exit(0) // 结束当前进程彻底重启
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE48AFF))
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