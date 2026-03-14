package com.jiaweiya.flowcourse.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = MyCustomPurple,               // 全局主色调（各种按钮、文字高亮）
    onPrimary = Color.White,                // 主色调上的文字颜色（紫色按钮上的白字）
    primaryContainer = MyCustomPurpleContainer, // 容器颜色（如当天的课表高亮背景）
    onPrimaryContainer = MyCustomPurpleDark,    // 容器上的文字颜色

    // 次要颜色，为了避免出现突兀的蓝/粉色，也换成配套的颜色或保留灰色
    secondary = MyCustomPurpleDark,
    tertiary = MyCustomPurple
)

@Composable
fun FlowCourseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // 🌟 核心修改：把这里的 true 改成 false！
    // 这样 Android 12+ 的手机才会乖乖使用你指定的紫色，而不是强行用壁纸色！
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}