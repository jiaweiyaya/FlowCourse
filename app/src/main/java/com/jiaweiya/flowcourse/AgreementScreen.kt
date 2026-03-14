package com.jiaweiya.flowcourse

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.ui.Alignment

@OptIn(ExperimentalMaterial3Api::class) // 引入实验性 API 用于 TopAppBar
@Composable
fun AgreementScreen(
    isReadOnly: Boolean = false,     // 是否是只读模式（从设置进）
    onBackClick: () -> Unit = {},    // 只读模式下的返回事件
    onAgreeClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    var isExpanded1 by remember { mutableStateOf(false) }
    var isExpanded2 by remember { mutableStateOf(false) }

    // 只有在【非只读模式】（首次进入）时，才拦截返回键直接退出应用
    if (!isReadOnly) {
        BackHandler {
            (context as? Activity)?.finish()
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp), // 清除 Scaffold 边距
        topBar = {
            if (isReadOnly) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "服务协议与隐私政策",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        },
        bottomBar = {
            // 只有在【非只读模式】下，才渲染底部的同意/拒绝按钮
            if (!isReadOnly) {
                Surface(
                    shadowElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = { (context as? Activity)?.finish() },
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text("暂不使用", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Button(onClick = onAgreeClick) {
                            Text("同意并继续")
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()) // 支持长内容垂直滑动
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "用户服务协议与隐私政策",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "欢迎使用 FlowCourse！\n\n" +
                        "本课表程序（以下简称“本应用”），由独立开发者（以下使用“Jiaweiya”或“作者”代之）个人开发并维护。\n" +
                        "在您使用本应用之前，请务必仔细阅读本《用户协议》（以下简称“本协议”）。一旦您点击右下角按钮确认使用本应用，即视为您已阅读并同意本协议的所有内容。如果您不同意本协议的任何内容，请停止使用并卸载本应用。",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "一、开源许可声明\n" +
                        "1. 本应用是基于 GNU 通用公共许可证第三版（GNU General Public License v3.0，简称 GPL-3.0）发布的自由开源软件。\n" +
                        "2. 您有权在遵守 GPL-3.0 协议的前提下，自由地运行、学习、修改和分发本应用的源代码及二进制文件。\n" +
                        "3. 本应用的完整源代码托管于 GitHub 平台。您可以在应用内的“关于”页面或本段文本下方折叠的部分中的跳转链接获取源代码仓库地址。\n" +
                        "4. 若您对本应用进行了修改或二次开发并对外分发，根据 GPL-3.0 协议的条款，您的衍生作品也必须以 GPL-3.0 协议开源，并向接收者提供完整的源代码或其托管仓库链接。\n" +
                        "5. 附加许可（即 GPL-v3 Section 7 例外）：\n" +
                        "   - 本项目的原作者在此授予额外权限：允许将本开源程序与闭源的腾讯 X5 浏览器内核（Tencent TBS SDK）进行链接和编译，并允许发布两者的组合产物。腾讯 X5 SDK 的使用受其自身专属商业授权条款的约束。",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(2.dp))

            // 点击展开仓库地址及GNU GPL-3.0协议地址
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(modifier = Modifier.padding(7.dp)) {
                    Text(
                        text = "点击此处展开仓库地址及GNU GPL-3.0协议地址",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isExpanded1 = !isExpanded1 } // 点击切换展开状态
                            .padding(vertical = 4.dp)
                    )
                    AnimatedVisibility(visible = isExpanded1) {
                        Column(modifier = Modifier.padding(top = 8.dp)) {
                            Text(
                                text = "FlowCourse仓库地址：",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "https://github.com/jiaweiyaya/FlowCourse",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary, // 链接用主题色突出
                                textDecoration = TextDecoration.Underline, // 加上下划线
                                modifier = Modifier
                                    .padding(vertical = 4.dp)
                                    .clickable {
                                        uriHandler.openUri("https://github.com/jiaweiyaya/FlowCourse")
                                    }
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "GNU GPL-3.0协议地址：",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "https://www.gnu.org/licenses/gpl-3.0.txt?spm=5176.28103460.0.0.96a075515FrAZ8&file=gpl-3.0.txt",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary,
                                textDecoration = TextDecoration.Underline,
                                modifier = Modifier
                                    .padding(vertical = 4.dp)
                                    .clickable {
                                        uriHandler.openUri("https://www.gnu.org/licenses/gpl-3.0.txt?spm=5176.28103460.0.0.96a075515FrAZ8&file=gpl-3.0.txt")
                                    }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "二、服务内容与免责声明\n" +
                        "1. 本应用主要功能为显示课程表，旨在为用户提供便捷的课表查看服务。作者致力于提供准确、稳定的服务，但不对课程数据的准确性、及时性或完整性做任何形式的保证。课程数据通常由用户自行导入或通过学校接口获取，作者不对因数据源错误导致的任何后果负责。且由于学校等网站的接口变动等原因，本程序不保证能100%准确的解析并显示课表，请您自行校对本程序显示的课程是否有误，本程序不对任何形式的课程显示错误导致的问题负责。\n" +
                        "2. 在任何情况下，作者均不对因使用或无法使用本应用而导致的任何直接、间接、偶然、特殊或后果性损害（包括但不限于数据丢失、利润损失、业务中断等）承担赔偿责任，即使作者已被告知发生此类损害的可能性。\n" +
                        "3. 本应用的官方分发渠道仅有QQ官群（1群：1074858712）及Github的Release中，不保证持续提供任何形式的售后及持久的更新维护。在法律允许的最大范围内，作者可以明确拒绝任何明示或暗示的保修及售后，包括但不限于对适销性、特定用途适用性和非侵权性的暗示。\n" +
                        "4. 本应用完全免费，不包含任何内购、付费订阅或强制性广告（不包括您在使用内嵌WebView访问网页时，对象网页中的任何内容）。若您在非官方渠道下载到包含收费项目或恶意广告的版本，那并非作者发布，作者对此不承担任何责任。",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "三、隐私与数据保护\n" +
                        "1. 本应用遵循“最小化收集”原则。您的课程表数据、个人信息等均仅存储于您的本地设备中。\n" +
                        "2. 本应用不包含任何第三方广告 SDK 或数据追踪组件。\n" +
                        "3. 本应用不会将您的数据以任何形式上传至任何服务器（使用内嵌WebVew访问网页时和对象网页的数据交互除外），如您发现有非法的数据上传现象，请确认您安装的版本是否是上述官方渠道获取的最新版本，如是，那么请立刻前往Github仓库新建issue反馈给作者。\n" +
                        "4. 本应用可能需要获取以下系统权限以正常运行：\n" +
                        "   - 存储权限（可选）：仅用于将课表导出到您的设备、向相册写入QQ群二维码、从本地导入课表数据、课表背景图片的导入。\n" +
                        "   - 联网权限（可选）：仅用于“从教务导入”按钮的内嵌WebView访问网络使用、从GitHub检查应用程序更新（暂未实装）。\n" +
                        "   - 写入剪贴板权限（可选）：仅用于导出WebView的日志、导出课表至剪贴板\n" +
                        "   - 读取剪贴板权限（可选）：仅用于从剪贴板导入课表\n" +
                        "   - 读取系统时间权限（可选）：仅用于自动根据日期显示对应课表及时间线功能\n" +
                        "   您可以随时在系统设置中关闭这些权限，但这可能会影响对应功能的使用。",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "四、知识产权\n" +
                        "1. 本应用的源代码版权归作者所有，且授权遵循 GNU GPL-3.0 协议的使用。\n" +
                        "2. 本项目内包含的演示图片、原创图标（如主题背景、应用图标、头像等）版权归作者或原画师所有，仅作 UI 展示使用，【不遵循】 GPL-3.0 开源协议，请勿用于任何形式的商业用途。未经作者书面许可，任何人不得将本应用的名称、原创图标等用于商业推广或制作混淆视听的仿冒应用。\n" +
                        "3. 您在使用本应用时生成的个性化课表数据，其所有权归您本人所有。",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "五、协议的变更与终止\n" +
                        "1. 作者保留随时修改本协议条款的权利。更新后的协议将在应用内“设置”页面中的“用户协议”入口中显示，或在 GitHub 仓库中公布，恕不另行通知。\n" +
                        "2. 若您违反本协议的任何条款（比如违反 GPL-3.0 开源协议进行闭源商用），作者有权终止对您使用本应用的授权，并保留追究法律责任的权利。",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "六、其他\n" +
                        "1. 本协议的解释、效力及纠纷的解决，适用中华人民共和国法律。\n" +
                        "2. 若本协议的任何条款被认定为无效或不可执行，不影响其他条款的效力和执行。\n" +
                        "3. 本项目为个人开源爱好项目，作者不保证提供任何形式的客服支持。如有问题、建议或提交 Bug，欢迎通过 GitHub 仓库的 \"Issues\" 板块或官方QQ群：1群：1074858712 提交反馈，作者会在业余时间尽力处理。",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Column(modifier = Modifier.padding(top = 16.dp)) {
                Text(
                    text = "联系我们",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "官方QQ群：\n1群：1074858712",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 22.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "GitHub 项目地址：",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 22.sp
                )

                Text(
                    text = "https://github.com/jiaweiyaya/FlowCourse",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier
                        .padding(vertical = 4.dp)
                        .clickable {
                            uriHandler.openUri("https://github.com/jiaweiyaya/FlowCourse")
                        }
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "开发者邮箱：",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 22.sp
                )

                Text(
                    text = "2652520612@qq.com",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier
                        .padding(vertical = 4.dp)
                        .clickable {
                            // 使用 mailto ，点击后会自动打开手机上的邮件应用
                            uriHandler.openUri("mailto:2652520612@qq.com")
                        }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

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
                            .clickable { isExpanded2 = !isExpanded2 }
                            .padding(vertical = 4.dp)
                    )
                    AnimatedVisibility(visible = isExpanded2) {
                        Column(modifier = Modifier.padding(top = 8.dp)) {
                            Text(
                                text = "作者：春日いづれ\n作品：もっとホシノちゃんといっしょ!～",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "https://www.dlsite.com/maniax/work/=/product_id/RJ01485893.html/?locale=zh_CN",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary,
                                textDecoration = TextDecoration.Underline,
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

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}