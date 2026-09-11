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
import android.webkit.JavascriptInterface
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
    autoNavigate: Boolean,
    autoCapture: Boolean,
    autoMergeAdjacent: Boolean,
    defaultDesktopMode: Boolean,
    showImportButton: Boolean = true,
    onBackClick: () -> Unit,
    onImportCourses: (List<Course>) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var inputText by remember { mutableStateOf(defaultUrl) }
    var isDesktopMode by remember { mutableStateOf(defaultDesktopMode) }
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
            function log(msg) {
                if (window.AndroidBridge && window.AndroidBridge.log) {
                    window.AndroidBridge.log(msg);
                } else {
                    console.log('[JS] ' + msg);
                }
            }

            log('[提取指令] 用户点击提取按钮，当前URL: ' + window.location.href);

            // 跨 frame 递归遍历辅助函数
            function walkFrames(win, callback, depth) {
                if (!win || depth > 5) return null;
                try {
                    var res = callback(win);
                    if (res) return res;
                } catch(e) {}
                try {
                    for (var i = 0; i < win.frames.length; i++) {
                        var childRes = walkFrames(win.frames[i], callback, (depth || 0) + 1);
                        if (childRes) return childRes;
                    }
                } catch(e) {}
                return null;
            }

            // 1. 穿透检查所有 frame 是否已经缓存了课表数据
            var cachedSchedule = walkFrames(window, function(w) {
                return w.__FC_SCHEDULE_DATA__ || null;
            }, 0);

            if (cachedSchedule) {
                log('[提取] 发现页面已监听缓存的课表数据，直接回传');
                if (window.AndroidBridge) {
                    window.AndroidBridge.onTimetableExtracted(cachedSchedule);
                }
                return 'CACHED';
            }

            // 2. 如果在教务系统内部 (jwapp 或 kbapp)
            if (window.location.href.indexOf('jwapp') > -1 || window.location.href.indexOf('kbapp') > -1) {
                var basePath = window.location.href.split('/jwapp/')[0] + '/jwapp';
                var targetUrl = basePath + '/sys/kbapp/api/wdkbcx/getMyScheduleDetail.do';
                var defaultTermUrl = basePath + '/sys/jwpubapp/modules/gg/cxmrxnxq.do';

                // 多策略嗅探当前学年学期参数
                var xnxqdm = '';
                try {
                    // 策略A：跨 frame 查找 sessionStorage
                    xnxqdm = walkFrames(window, function(w) {
                        return (w.sessionStorage && w.sessionStorage.getItem('XNXQDM')) ? w.sessionStorage.getItem('XNXQDM') : null;
                    }, 0) || '';

                    // 策略B：跨 frame 查找指定属性的 input
                    if (!xnxqdm) {
                        xnxqdm = walkFrames(window, function(w) {
                            var el = w.document.querySelector('[data-name="XNXQDM"]') || w.document.querySelector('input[name="XNXQDM"]');
                            return (el && el.value) ? el.value : null;
                        }, 0) || '';
                    }

                    // 策略C：跨 frame 查找学期文本显示元素（包括 .kbappTimeXQText 和正文中的中文学期格式）
                    if (!xnxqdm) {
                        xnxqdm = walkFrames(window, function(w) {
                            var termElem = w.document.querySelector('.kbappTimeXQText');
                            var textToScan = termElem ? termElem.innerText : (w.document.body ? w.document.body.innerText : '');
                            
                            // 匹配类似 "2026-2027学年 第一学期"
                            var cnMatch = textToScan.match(/(\d{4}-\d{4})\s*学年\s*第([一二三123])学期/);
                            if (cnMatch) {
                                var xqNum = (cnMatch[2] === '一' || cnMatch[2] === '1') ? '1' : ((cnMatch[2] === '二' || cnMatch[2] === '2') ? '2' : '3');
                                return cnMatch[1] + '-' + xqNum;
                            }

                            // 匹配标准代号 "2026-2027-1"
                            var termMatch = textToScan.match(/\d{4}-\d{4}-[123]/);
                            if (termMatch) return termMatch[0];

                            return null;
                        }, 0) || '';
                    }
                } catch(err) {
                    log('[学期探测警告] ' + err);
                }

                // 执行最终课表请求
                function executeScheduleFetch(finalTerm) {
                    var postPayload = 'XNXQDM=' + encodeURIComponent(finalTerm) + '&XQDM=';
                    log('[网络请求] 发起POST请求: ' + targetUrl);
                    log('[网络负载] ' + postPayload);

                    fetch(targetUrl, {
                        method: 'POST',
                        headers: {
                            'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8',
                            'X-Requested-With': 'XMLHttpRequest'
                        },
                        body: postPayload
                    })
                    .then(function(res) {
                        log('[收到响应] HTTP状态: ' + res.status);
                        return res.text();
                    })
                    .then(function(text) {
                        log('[响应长度] 收到内容长度: ' + text.length + ' 字符');
                        log('[响应预览] ' + text.substring(0, Math.min(text.length, 260)));
                        if (window.AndroidBridge) {
                            window.AndroidBridge.onTimetableExtracted(text);
                        }
                    })
                    .catch(function(e) {
                        log('[请求异常] fetch失败: ' + e);
                    });
                }

                // 如果已经找到了学期代码，直接请求课表
                if (xnxqdm) {
                    log('[学期探测] 探测到学年学期: ' + xnxqdm);
                    executeScheduleFetch(xnxqdm);
                    return 'FETCHING';
                }

                // 策略D：如果页面中未能嗅探到学期，调用系统默认学年学期接口兜底
                log('[学期探测] 本地未嗅探到有效学期，正在请求默认学期接口: ' + defaultTermUrl);
                fetch(defaultTermUrl, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8',
                        'X-Requested-With': 'XMLHttpRequest'
                    },
                    body: 'CSDM=SYS&ZCSDM=DQXNXQDM&SFSY=1'
                })
                .then(function(res) { return res.json(); })
                .then(function(data) {
                    var termFromApi = '';
                    try {
                        termFromApi = data.datas.cxmrxnxq.rows[0].XNXQDM;
                    } catch(e) {}
                    log('[学期探测] 接口获取到默认学期: ' + termFromApi);
                    executeScheduleFetch(termFromApi || '');
                })
                .catch(function(err) {
                    log('[学期探测] 默认学期请求失败: ' + err + '，尝试使用空参数请求');
                    executeScheduleFetch('');
                });

                return 'FETCHING';
            }

            // 3. 旧版表格DOM回退方案
            log('[提取] 处于非新版教务页面，尝试DOM穿透查找表格...');
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
            var resultTable = walkFrames(window, function(w) {
                return findTable(w.document);
            }, 0) || '';

            log('[表格查找结果] 长度: ' + resultTable.length);
            if (resultTable && window.AndroidBridge) {
                window.AndroidBridge.onTimetableExtracted(resultTable);
            }
            return resultTable;
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
                    if (showImportButton) {
                        val handleCourseImport: (String) -> Unit = { rawResult ->
                            coroutineScope.launch {
                                if (rawResult.isBlank() || rawResult == "null" || rawResult == "\"\"" || rawResult == "\"FETCHING\"" || rawResult == "FETCHING" || rawResult == "\"CACHED\"" || rawResult == "CACHED") {
                                    return@launch
                                }

                                var content = rawResult
                                try {
                                    content = Gson().fromJson(rawResult, String::class.java)
                                } catch (e: Exception) {
                                    if (content.startsWith("\"") && content.endsWith("\"")) {
                                        content = content.substring(1, content.length - 1)
                                            .replace("\\\"", "\"")
                                            .replace("\\n", "\n")
                                            .replace("\\t", "\t")
                                            .replace("\\u003C", "<")
                                    }
                                }

                                logger("[解析入口] 正在提交给解析器处理，长度: " + content.length)
                                val newCourses = withContext(Dispatchers.IO) { CqwlxyParser.parseCourseFromHtml(content, logger, autoMergeAdjacent) }
                                if (newCourses.isNotEmpty()) {
                                    onImportCourses(newCourses)
                                    Toast.makeText(context, "大功告成！导入了 ${newCourses.size} 节课", Toast.LENGTH_SHORT).show()
                                    onBackClick()
                                } else {
                                    logger("[解析失败] 未能识别出课程，请点击右上角警告图标查看日志详情")
                                    Toast.makeText(context, "解析失败：未能识别到有效课程信息", Toast.LENGTH_LONG).show()
                                }
                            }
                        }

                        FloatingActionButton(
                            onClick = {
                                webViewRef?.evaluateJavascript(extractScript) { result ->
                                    if (result != null && result != "\"FETCHING\"" && result != "FETCHING") {
                                        handleCourseImport(result)
                                    }
                                }
                            },
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Icon(Icons.Default.Download, "提取课表")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {

            DisposableEffect(Unit) {
                onDispose {
                    webViewRef?.stopLoading()
                    webViewRef?.destroy()
                    webViewRef = null
                }
            }

            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    WebView.setWebContentsDebuggingEnabled(true)
                    WebView(ctx).apply {
                        setupWebViewSettings(this, isDesktopMode)
                        addJavascriptInterface(object {
                            @JavascriptInterface
                            fun log(msg: String) {
                                logger(msg)
                            }

                            @JavascriptInterface
                            fun isAutoCapture(): Boolean {
                                // 如果当前处于快捷纯浏览模式（showImportButton 为 false），强制禁止任何自动捕获
                                return autoCapture && showImportButton
                            }

                            @JavascriptInterface
                            fun onAutoCaptured(data: String) {
                                if (!showImportButton) return
                                coroutineScope.launch(Dispatchers.Main) {
                                    if (data.isNotBlank()) {
                                        logger("[自动捕获] 成功截获课表POST数据，正在自动解析...")
                                        val newCourses = withContext(Dispatchers.IO) { CqwlxyParser.parseCourseFromHtml(data, logger, autoMergeAdjacent) }
                                        if (newCourses.isNotEmpty()) {
                                            onImportCourses(newCourses)
                                            Toast.makeText(context, "自动捕获课表成功！已导入 " + newCourses.size + " 节课", Toast.LENGTH_SHORT).show()
                                            onBackClick()
                                        } else {
                                            logger("[自动捕获] 拦截到的数据未能解析出有效课程")
                                        }
                                    }
                                }
                            }

                            @JavascriptInterface
                            fun onTimetableExtracted(data: String) {
                                if (!showImportButton) return
                                coroutineScope.launch(Dispatchers.Main) {
                                    if (data.isNotBlank()) {
                                        logger("[Bridge回调] 收到数据传输，准备解析...")
                                        val newCourses = withContext(Dispatchers.IO) { CqwlxyParser.parseCourseFromHtml(data, logger, autoMergeAdjacent) }
                                        if (newCourses.isNotEmpty()) {
                                            onImportCourses(newCourses)
                                            Toast.makeText(context, "大功告成！导入了 ${newCourses.size} 节课", Toast.LENGTH_SHORT).show()
                                            onBackClick()
                                        } else {
                                            logger("[Bridge解析失败] 返回课程为空")
                                            Toast.makeText(context, "解析失败：未能识别到有效课程信息", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                }
                            }
                        }, "AndroidBridge")
                        val effectiveAutoCapture = autoCapture && showImportButton
                        setupClients(this, logger, desktopWidth, autoUsername, autoPassword, autoLogin, autoNavigate, effectiveAutoCapture, { isDesktopMode }, { url -> inputText = url })
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
    autoNavigate: Boolean,
    autoCapture: Boolean,
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
            logger("[页面就绪] " + (url ?: ""))

            // 注入网络监听钩子，页面自主发起课表请求时会被立即捕获
            val interceptScript = """
                javascript:(function() {
                    function hookContext(targetWin) {
                        try {
                            if (!targetWin || targetWin.__fc_injected) return;
                            targetWin.__fc_injected = true;
                            var origOpen = targetWin.XMLHttpRequest.prototype.open;
                            var origSend = targetWin.XMLHttpRequest.prototype.send;
                            targetWin.XMLHttpRequest.prototype.open = function(method, u) {
                                this._fc_url = u;
                                return origOpen.apply(this, arguments);
                            };
                            targetWin.XMLHttpRequest.prototype.send = function(body) {
                                var self = this;
                                if (self._fc_url && self._fc_url.indexOf('getMyScheduleDetail') > -1) {
                                    if (window.AndroidBridge && window.AndroidBridge.log) {
                                        window.AndroidBridge.log('[自动监听] 页面发起课表请求: ' + self._fc_url + ', 负载: ' + body);
                                    }
                                    self.addEventListener('load', function() {
                                        if (window.AndroidBridge && window.AndroidBridge.log) {
                                            window.AndroidBridge.log('[自动监听] 捕获到课表响应: HTTP ' + self.status + ', 长度: ' + (self.responseText ? self.responseText.length : 0));
                                        }
                                        var responseData = self.responseText || '';
                                        if (!responseData && self.response && typeof self.response === 'string') {
                                            responseData = self.response;
                                        }
                                        if (responseData && responseData.indexOf('arrangedList') > -1) {
                                            targetWin.__FC_SCHEDULE_DATA__ = responseData;
                                            window.__FC_SCHEDULE_DATA__ = responseData;

                                            var isAutoEnabled = false;
                                            try {
                                                if (window.AndroidBridge && window.AndroidBridge.isAutoCapture) {
                                                    isAutoEnabled = window.AndroidBridge.isAutoCapture();
                                                } else {
                                                    isAutoEnabled = ${'$'}autoCapture;
                                                }
                                            } catch(e) {
                                                isAutoEnabled = ${'$'}autoCapture;
                                            }

                                            if (isAutoEnabled && !window.__FC_AUTO_CAPTURED__) {
                                                window.__FC_AUTO_CAPTURED__ = true;
                                                if (window.AndroidBridge && window.AndroidBridge.log) {
                                                    window.AndroidBridge.log('[自动监听] 自动捕获已开启，正立即触发自动解析与导入...');
                                                }
                                                if (window.AndroidBridge && window.AndroidBridge.onAutoCaptured) {
                                                    window.AndroidBridge.onAutoCaptured(responseData);
                                                }
                                            } else {
                                                if (window.AndroidBridge && window.AndroidBridge.log) {
                                                    window.AndroidBridge.log('[自动监听] 成功缓存课表数据，可随时点击右下角按钮提取');
                                                }
                                            }
                                        }
                                    });
                                }
                                return origSend.apply(this, arguments);
                            };
                        } catch(e) {}
                    }

                    // 拦截当前顶层窗口
                    hookContext(window);

                    // 循环遍历并渗透拦截所有子 frame
                    try {
                        for (var i = 0; i < window.frames.length; i++) {
                            hookContext(window.frames[i]);
                        }
                    } catch(e) {}

                    // 持续轮询检测新创建的 iframe
                    if (!window.__fc_iframe_watcher) {
                        window.__fc_iframe_watcher = setInterval(function() {
                            try {
                                for (var j = 0; j < window.frames.length; j++) {
                                    hookContext(window.frames[j]);
                                }
                            } catch(e) {}
                        }, 800);
                    }
                })();
            """.trimIndent()
            view?.evaluateJavascript(interceptScript, null)

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

            if (url != null) {
                val navJs = CqwlxyParser.getAutoNavigateScript(url, autoNavigate)
                if (navJs != null) {
                    view?.evaluateJavascript(navJs, null)
                    logger("🔗 [自动跳转] 正在寻找并点击教学管理系统...")
                }
            }
        }
    }

    webView.webChromeClient = object : WebChromeClient() {
        override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
            val msg = consoleMessage?.message() ?: ""
            // 过滤一下，只拦截带有 [JS] 前缀的日志，避免被网页日志刷屏
            if (msg.startsWith("[JS]")) {
                logger("📜 $msg")
            }
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