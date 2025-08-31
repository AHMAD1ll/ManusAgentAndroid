package com.example.manusagentapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.manusagentapp.core.ModelDownloader
import com.example.manusagentapp.ui.theme.ManusAgentAppTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ManusAgentAppTheme {
                MainScreen()
            }
        }
    }
}

@Composable
fun MainScreen() {
    // حالة لتتبع تقدم التحميل
    var downloadProgress by remember { mutableStateOf(0f) }
    // حالة لتحديد ما إذا كان التحميل قد اكتمل
    var isDownloadComplete by remember { mutableStateOf(false) }
    // حالة لتخزين أي رسالة خطأ
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // الحصول على سياق التطبيق
    val context = androidx.compose.ui.platform.LocalContext.current
    // إنشاء نسخة من مدير التحميل
    val downloader = remember { ModelDownloader(context) }

    // استخدام LaunchedEffect لتشغيل عملية التحميل مرة واحدة عند بدء الشاشة
    LaunchedEffect(Unit) {
        // التحقق أولاً إذا كانت الملفات موجودة بالفعل لتجنب إعادة التحميل
        if (downloader.areModelsAvailable()) {
            isDownloadComplete = true
        } else {
            // بدء التحميل داخل Coroutine
            launch {
                try {
                    downloader.downloadModels { progress ->
                        downloadProgress = progress
                    }
                    isDownloadComplete = true
                } catch (e: Exception) {
                    // في حالة حدوث أي خطأ أثناء التحميل
                    errorMessage = "فشل التحميل: ${e.message}"
                }
            }
        }
    }

    // عرض الشاشة المناسبة بناءً على حالة التحميل
    if (errorMessage != null) {
        // شاشة عرض الخطأ
        ErrorScreen(message = errorMessage!!)
    } else if (isDownloadComplete) {
        // الشاشة الرئيسية للتطبيق بعد اكتمال التحميل
        AgentHomeScreen()
    } else {
        // شاشة التحميل
        LoadingScreen(progress = downloadProgress)
    }
}

@Composable
fun LoadingScreen(progress: Float) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("جاري تهيئة الذكاء الاصطناعي لأول مرة...", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(16.dp))
            // شريط التقدم الخطي
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(0.8f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            // عرض النسبة المئوية
            Text("${(progress * 100).toInt()}%")
        }
    }
}

@Composable
fun ErrorScreen(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(text = message, color = MaterialTheme.colorScheme.error)
    }
}

@Composable
fun AgentHomeScreen() {
    // هذه هي الشاشة الرئيسية لتطبيقك التي ستظهر بعد التحميل
    // سنقوم ببناء هذه الشاشة في الخطوات التالية
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("أهلاً بك في Manus Agent!", style = MaterialTheme.typography.headlineMedium)
    }
}
