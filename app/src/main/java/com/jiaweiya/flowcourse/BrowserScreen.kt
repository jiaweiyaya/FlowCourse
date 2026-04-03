package com.jiaweiya.flowcourse

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.tencent.smtt.export.external.interfaces.ConsoleMessage
import com.tencent.smtt.export.external.interfaces.SslError
import com.tencent.smtt.export.external.interfaces.SslErrorHandler
import com.tencent.smtt.export.external.interfaces.WebResourceError
import com.tencent.smtt.export.external.interfaces.WebResourceRequest
import com.tencent.smtt.export.external.interfaces.WebResourceResponse
import com.tencent.smtt.sdk.CookieManager
import com.tencent.smtt.sdk.WebChromeClient
import com.tencent.smtt.sdk.WebView
import com.tencent.smtt.sdk.WebViewClient
import java.io.ByteArrayInputStream
import com.jiaweiya.flowcourse.parser.CqwlxyParser

// 纯净伪装的 User-Agent 字符串
private const val UA_MOBILE = "Mozilla/5.0 (Linux; Android 14; Pixel 8 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Mobile Safari/537.36"
private const val UA_DESKTOP = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36 Edg/122.0.0.0"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserScreen(
    defaultUrl: String,
    desktopWidth: Int,
    desktopHeight: Int,
    autoUsername: String,
    autoPassword: String,
    autoLogin: Boolean,
    onBackClick: () -> Unit,
    onImportCourses: (List<Course>) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var inputText by remember { mutableStateOf(defaultUrl) }
    var isDesktopMode by remember { mutableStateOf(false) }
    var showDebugPanel by remember { mutableStateOf(false) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    val debugLogs = remember { mutableStateListOf<String>() }

    val logger: (String) -> Unit = { msg ->
        coroutineScope.launch(Dispatchers.Main) {
            debugLogs.add(0, msg)
            if (debugLogs.size > 80) debugLogs.removeAt(debugLogs.lastIndex)
        }
    }

    val extractScript = """
        (function() {
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
            return walk(window, 0) || '';
        })();
    """.trimIndent()

    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0.dp),
                title = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        BasicTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            singleLine = true,
                            textStyle = TextStyle(fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回主页") }
                },
                actions = {
                    IconButton(onClick = { showDebugPanel = !showDebugPanel }) {
                        Icon(Icons.Default.Warning, "调试", tint = if (showDebugPanel) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
                    }
                    IconButton(onClick = {
                        val finalUrl = if (!inputText.startsWith("http")) "http://$inputText" else inputText
                        webViewRef?.loadUrl(finalUrl)
                    }) {
                        Icon(Icons.Default.Check, "前往")
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar(
                actions = {
                    IconButton(onClick = { webViewRef?.goBack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "后退") }
                    IconButton(onClick = { webViewRef?.goForward() }) { Icon(Icons.AutoMirrored.Filled.ArrowForward, "前进") }
                    IconButton(onClick = {
                        isDesktopMode = !isDesktopMode
                        webViewRef?.apply {
                            settings.userAgentString = if (isDesktopMode) UA_DESKTOP else UA_MOBILE
                            reload()
                        }
                        Toast.makeText(context, if (isDesktopMode) "切换至电脑版" else "切换至手机版", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(
                            imageVector = if (isDesktopMode) Icons.Default.Phone else Icons.Default.Computer,
                            contentDescription = "模式切换",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                floatingActionButton = {
                    FloatingActionButton(
                        onClick = {
                            webViewRef?.evaluateJavascript(extractScript) { result ->
                                coroutineScope.launch {
                                    if (result.isNullOrBlank() || result == "null" || result == "\"\"") {
                                        Toast.makeText(context, "未提取到课表，请确保页面已经完全加载", Toast.LENGTH_SHORT).show()
                                        return@launch
                                    }

                                    var htmlContent = result
                                    try {
                                        htmlContent = Gson().fromJson(result, String::class.java)
                                    } catch (e: Exception) {
                                        if (htmlContent.startsWith("\"")) {
                                            htmlContent = htmlContent.substring(1, htmlContent.length - 1)
                                                .replace("\\\"", "\"")
                                                .replace("\\n", "\n")
                                                .replace("\\t", "\t")
                                                .replace("\\u003C", "<")
                                        }
                                    }

                                    if (htmlContent.contains("星期一")) {
                                        // 调用从 HTML 解析的方法（parseCourseFromHtml）
                                        val newCourses = withContext(Dispatchers.IO) { CqwlxyParser.parseCourseFromHtml(htmlContent) }
                                        if (newCourses.isNotEmpty()) {
                                            onImportCourses(newCourses)
                                            Toast.makeText(context, "大功告成！导入了 ${newCourses.size} 节课", Toast.LENGTH_SHORT).show()
                                            onBackClick()
                                        } else {
                                            Toast.makeText(context, "解析失败：未能匹配出课程结构", Toast.LENGTH_LONG).show()
                                        }
                                    } else {
                                        Toast.makeText(context, "未找到有效的课表表格数据", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        },
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Icon(Icons.Default.Download, "提取课表")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {

            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    WebView.setWebContentsDebuggingEnabled(true)
                    WebView(ctx).apply {
                        setupWebViewSettings(this, isDesktopMode)
                        setupClients(this, logger, desktopWidth, autoUsername, autoPassword, autoLogin, { isDesktopMode }, { url -> inputText = url })
                        webViewRef = this
                        loadUrl(defaultUrl)
                    }
                },
                update = { webViewRef = it }
            )

            if (showDebugPanel) {
                DebugPanelOverlay(
                    logs = debugLogs,
                    onClose = { showDebugPanel = false },
                    onClearCache = {
                        webViewRef?.clearCache(true)
                        CookieManager.getInstance().removeAllCookies(null)
                        CookieManager.getInstance().flush()
                        debugLogs.clear()
                        logger("[WebView] 缓存和Cookie已被抹除，请刷新。")
                        webViewRef?.reload()
                    },
                    onExport = {
                        val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cb.setPrimaryClip(ClipData.newPlainText("logs", debugLogs.joinToString("\n")))
                        Toast.makeText(context, "日志已复制到剪贴板", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
private fun setupWebViewSettings(webView: WebView, isDesktopMode: Boolean) {
    webView.settings.apply {
        javaScriptEnabled = true
        domStorageEnabled = true
        databaseEnabled = true
        useWideViewPort = true
        loadWithOverviewMode = true

        allowFileAccess = true
        allowContentAccess = true
        mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

        setSupportZoom(true)
        builtInZoomControls = true
        displayZoomControls = false

        // 关闭多窗口支持，让内核自己处理新窗口跳转到当前页面
        setSupportMultipleWindows(false)
        javaScriptCanOpenWindowsAutomatically = false

        userAgentString = if (isDesktopMode) UA_DESKTOP else UA_MOBILE
    }

    CookieManager.getInstance().setAcceptCookie(true)
    CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)
}

private fun setupClients(
    webView: WebView,
    logger: (String) -> Unit,
    desktopWidth: Int,
    autoUsername: String,
    autoPassword: String,
    autoLogin: Boolean,
    isDesktopProvider: () -> Boolean,
    onUrlChanged: (String) -> Unit
) {
    webView.webViewClient = object : WebViewClient() {
        override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
            val urlStr = request?.url?.toString() ?: ""
            if (urlStr.contains("campusphere.cn") || urlStr.contains("track") || urlStr.contains("google-analytics")) {
                return WebResourceResponse("text/plain", "UTF-8", ByteArrayInputStream(ByteArray(0)))
            }
            return super.shouldInterceptRequest(view, request)
        }

        @SuppressLint("WebViewClientOnReceivedSslError")
        override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
            logger("⚠️ [SSL] 忽略证书验证拦截")
            handler?.proceed()
        }

        override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
            super.doUpdateVisitedHistory(view, url, isReload)
            url?.let { onUrlChanged(it) }
        }

        override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
            super.onReceivedError(view, request, error)
            if (request?.isForMainFrame == true) {
                logger("❌ [主框架错误] ${error?.description}")
            }
        }

        override fun onReceivedHttpError(view: WebView?, request: WebResourceRequest?, errorResponse: WebResourceResponse?) {
            super.onReceivedHttpError(view, request, errorResponse)
            val urlStr = request?.url?.toString() ?: ""
            if (!urlStr.contains("campusphere.cn")) {
                 logger("🔴 [HTTP错误] ${errorResponse?.statusCode} -> $urlStr")
            }
        }

        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            super.onPageStarted(view, url, favicon)
            logger("⏳ [加载开始] $url")
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            logger("✅ [页面就绪] $url")

            if (isDesktopProvider()) {
                val js = "javascript:(function(){var m=document.querySelector('meta[name=\"viewport\"]');if(!m){m=document.createElement('meta');m.name='viewport';document.head.appendChild(m);}m.content='width=$desktopWidth';})();"
                view?.evaluateJavascript(js, null)
            }

            // 由解析器脚本控制自动填充位置
            if (autoUsername.isNotEmpty() && autoPassword.isNotEmpty() && url != null) {
                val fillJs = CqwlxyParser.getAutoFillScript(url, autoUsername, autoPassword, autoLogin)

                if (fillJs != null) {
                    view?.evaluateJavascript(fillJs, null)
                    logger("🚀 [自动填充] 匹配到目标登录页，已自动填入账号密码")
                }
            }
        }
    }

    webView.webChromeClient = object : WebChromeClient() {
        override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
            return super.onConsoleMessage(consoleMessage)
        }
    }
}

@Composable
private fun BoxScope.DebugPanelOverlay(
    logs: List<String>,
    onClose: () -> Unit,
    onClearCache: () -> Unit,
    onExport: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.55f)
            .background(Color(0xE6121212))
            .align(Alignment.TopCenter)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("WebView组件日志", color = Color.White, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onExport, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Share, "导出", tint = Color(0xFF69F0AE))
                    }
                    IconButton(onClick = onClearCache, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Delete, "强清缓存", tint = Color(0xFFFF5252))
                    }
                    IconButton(onClick = onClose, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Check, "收起", tint = Color.White)
                    }
                }
            }
            HorizontalDivider(color = Color.DarkGray, modifier = Modifier.padding(vertical = 4.dp))

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(logs) { log ->
                    val textColor = when {
                        log.contains("❌") || log.contains("🔴") || log.contains("ERROR") -> Color(0xFFFF5252)
                        log.contains("⚠️") || log.contains("WARNING") -> Color(0xFFFFD740)
                        log.contains("✅") || log.contains("🔗") -> Color(0xFF69F0AE)
                        log.contains("🚀") -> Color(0xFF40C4FF)
                        else -> Color(0xFFB0BEC5)
                    }
                    SelectionContainer {
                        Text(
                            text = log,
                            color = textColor,
                            fontSize = 11.sp,
                            lineHeight = 14.sp,
                            modifier = Modifier.padding(vertical = 3.dp)
                        )
                    }
                }
            }
        }
    }
}