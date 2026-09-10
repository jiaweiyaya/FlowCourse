# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# 保留行号与错误堆栈信息，方便排查崩溃
-keepattributes SourceFile,LineNumberTable

# 1. Gson 与数据实体类保护（防止备份文件与课表数据解析失败）
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class com.jiaweiya.flowcourse.Course { *; }
-keep class com.jiaweiya.flowcourse.TimetableData { *; }
-keep class com.jiaweiya.flowcourse.TimeProfile { *; }
-keep class com.jiaweiya.flowcourse.NodeTime { *; }
-keep class com.jiaweiya.flowcourse.GithubRelease { *; }
-keep class com.jiaweiya.flowcourse.BackupData { *; }

# 2. WebView 与 JavaScriptInterface 保护（防止网页探针回传失效）
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
-keep class com.jiaweiya.flowcourse.JSBridge { *; }

# 3. 腾讯 X5 TBS 内核保护
-dontwarn com.tencent.smtt.**
-keep class com.tencent.smtt.** { *; }
-keep class com.tencent.tbs.** { *; }

# 4. Glance 桌面小组件交互回调保护
-keep class * implements androidx.glance.appwidget.action.ActionCallback {
    public <init>();
}
-keep class com.jiaweiya.flowcourse.widget.** { *; }