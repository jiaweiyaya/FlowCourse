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

// 如果你需要保留旧版本的静态配色作为备用，可以保留它们
private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = MyCustomPurple,
    onPrimary = Color.White,
    primaryContainer = MyCustomPurpleContainer,
    onPrimaryContainer = MyCustomPurpleDark,
    secondary = MyCustomPurpleDark,
    tertiary = MyCustomPurple
)

@Composable
fun FlowCourseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    themeColor: Long = 0xFF9E77ED,
    content: @Composable () -> Unit
) {
    // 2. 将传入的 Long 转换为 Color 对象
    val baseColor = Color(themeColor)

    // 3. 动态生成浅色模式配色：基于选中的 baseColor 派生出配套颜色
    val dynamicLightScheme = lightColorScheme(
        primary = baseColor,               // 各种按钮、文字高亮
        onPrimary = Color.White,           // 主色调按钮上的文字颜色
        primaryContainer = baseColor.copy(alpha = 0.15f), // 使用 15% 透明度作为容器背景（如当天的课表高亮背景）
        onPrimaryContainer = baseColor,    // 容器上的文字颜色
        secondary = baseColor,             // 次要颜色（跟随主色调）
        tertiary = baseColor               // 第三颜色（跟随主色调）
    )

    // 4. 动态生成深色模式配色：同样基于 baseColor 派生
    val dynamicDarkScheme = darkColorScheme(
        primary = baseColor,
        onPrimary = Color.Black,           // 深色模式下，主色调按钮上的文字建议使用黑色或深色以保证可读性
        primaryContainer = baseColor.copy(alpha = 0.3f), // 深色模式下使用 30% 透明度作为容器高亮
        onPrimaryContainer = Color.White,
        secondary = baseColor,
        tertiary = baseColor
    )

    // 5. 根据条件选择配色方案
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> dynamicDarkScheme // 使用动态生成的深色模式配色
        else -> dynamicLightScheme     // 使用动态生成的浅色模式配色
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}