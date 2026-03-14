package com.jiaweiya.flowcourse

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBackClick: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    val currentAppVersion = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }
    }
    var isExpanded by remember { mutableStateOf(false) }

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            TopAppBar(
                title = { Text("关于此应用", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                windowInsets = WindowInsets(0.dp)
            )
        }
    ) { paddingValues ->
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp)
                .verticalScroll(scrollState)
        ) {

            // 顶部区域布局
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically // 垂直居中对齐
            ) {
                // 应用名称和版本号
                Column(modifier = Modifier.wrapContentWidth()) {
                    Text(
                        text = "FlowCourse",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "版本 $currentAppVersion",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // 用 weight(1f) 占满 FlowCourse 右侧到屏幕边缘的剩余空间
                // 用 contentAlignment = Alignment.Center 让头像在这个剩余空间里居中
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Image(
                            painter = painterResource(id = R.drawable.jiaweiya_icon),
                            contentDescription = "开发者Logo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(60.dp)
                                .clip(RoundedCornerShape(16.dp))
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Jiaweiya",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            val annotatedString = buildAnnotatedString {
                append("感谢您愿意体验本应用喵~\n")
                append("这是一个由 Jiaweiya 和一大堆 AI 合作开发和维护的轻量级课表显示应用喵~。\n")
                append("在使用过程中遇到任何问题或者想提出建议，欢迎提交 Issue 喵~\n")
                append("欢迎 Fork 并提 PR 喵，我会抽空审批和 Merge 的喵~\n")

                append("\n欢迎进入QQ群讨论喵~：\n")
                append("1群：1074858712\n")

                append("\n如果您喜欢它（或者喜欢我~），欢迎在 GitHub 上为我点 Star 喵~（这也会给我持续更新和维护的动力哦喵~）：\n")
                pushStringAnnotation(tag = "URL", annotation = "https://github.com/jiaweiyaya/FlowCourse")
                withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary, textDecoration = TextDecoration.Underline)) {
                    append("https://github.com/jiaweiyaya/FlowCourse")
                }
                pop()

                append("\n\n本应用以 GNU GPL-3.0 协议开源，不要不遵守开源协议哦喵~（协议内容详见“用户服务协议与隐私政策”页面）")
            }

            ClickableText(
                text = annotatedString,
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 24.sp
                ),
                onClick = { offset ->
                    annotatedString.getStringAnnotations(tag = "URL", start = offset, end = offset)
                        .firstOrNull()?.let { annotation ->
                            uriHandler.openUri(annotation.item)
                        }
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 点击展开非原创资源借物表
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(modifier = Modifier.padding(7.dp)) {
                    Text(
                        text = "点击展开非原创资源借物表",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isExpanded = !isExpanded }
                            .padding(vertical = 4.dp)
                    )
                    AnimatedVisibility(visible = isExpanded) {
                        Column(modifier = Modifier.padding(top = 8.dp)) {
                            Text(
                                text = "作者：春日いづれ\n作品：もっとホシノちゃんといっしょ!～",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "https://www.dlsite.com/maniax/work/=/product_id/RJ01485893.html/?locale=zh_CN",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary, // 链接用主题色突出
                                textDecoration = TextDecoration.Underline, // 加上下划线
                                modifier = Modifier
                                    .padding(vertical = 4.dp)
                                    .clickable {
                                        uriHandler.openUri("https://www.dlsite.com/maniax/work/=/product_id/RJ01485893.html/?locale=zh_CN")
                                    }
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "作者：純粋な不純物(@parang9494)（NEXON Game & Yostar）\n作品：【ぶるーあーかいぶっ！】第254話",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "https://x.com/i/status/2009475490608980199",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary,
                                textDecoration = TextDecoration.Underline,
                                modifier = Modifier
                                    .padding(vertical = 4.dp)
                                    .clickable {
                                        uriHandler.openUri("https://x.com/i/status/2009475490608980199")
                                    }
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "作者：Jam To Cham\n作品：고양이",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "https://www.pixiv.net/artworks/141654587",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary,
                                textDecoration = TextDecoration.Underline,
                                modifier = Modifier
                                    .padding(vertical = 4.dp)
                                    .clickable {
                                        uriHandler.openUri("https://www.pixiv.net/artworks/141654587")
                                    }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}