package com.example.manusagentapp

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.manusagentapp.ui.theme.ManusAgentAppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ManusAgentAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen()
                }
            }
        }
    }
}

@Composable
fun MainScreen() {
    val context = LocalContext.current
    var copyStatus by remember { mutableStateOf("التحقق من الأذونات...") }
    var filesExist by remember { mutableStateOf(false) }
    
    // تم تعديل هذه الدالة بالكامل
    val isAccessibilityServiceEnabled by rememberUpdatedAccessibilityState()

    val coroutineScope = rememberCoroutineScope()

    val requestAllFilesAccessLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        // لا حاجة لإعادة التحقق هنا، LaunchedEffect سيتكفل بذلك
    }

    LaunchedEffect(Unit) {
        if (hasAllFilesAccess()) {
            val modelsDir = context.filesDir
            val allFilesPresent = listOf("phi3.onnx", "phi3.onnx.data", "tokenizer.json")
                .all { File(modelsDir, it).exists() }

            if (allFilesPresent) {
                filesExist = true
                copyStatus = "التهيئة مكتملة. الملفات موجودة بالفعل."
            } else {
                copyModelFiles(context) { status ->
                    copyStatus = status
                    if (status.contains("اكتملت")) {
                        filesExist = true
                    }
                }
            }
        } else {
            copyStatus = "نحتاج إذن الوصول لكل الملفات لنسخ نماذج الذكاء الاصطناعي."
            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
            intent.data = Uri.parse("package:${context.packageName}")
            requestAllFilesAccessLauncher.launch(intent)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Manus Agent", fontSize = 32.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(32.dp))

        Text(copyStatus, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(16.dp))

        if (filesExist) {
            Text(
                text = if (isAccessibilityServiceEnabled) "الخدمة نشطة" else "الخدمة متوقفة",
                color = if (isAccessibilityServiceEnabled) Color.Green else Color.Red,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                context.startActivity(intent)
            }) {
                Text("تفعيل خدمة الوصولية")
            }
        }
    }
}

// دالة جديدة ومحسنة للتحقق من حالة الخدمة
@Composable
fun rememberUpdatedAccessibilityState(): State<Boolean> {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val accessibilityState = remember { mutableStateOf(isAccessibilityServiceEnabled(context)) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                accessibilityState.value = isAccessibilityServiceEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    return accessibilityState
}


private fun hasAllFilesAccess(): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        Environment.isExternalStorageManager()
    } else {
        true
    }
}

suspend fun copyModelFiles(context: Context, onStatusUpdate: (String) -> Unit) {
    withContext(Dispatchers.IO) {
        val downloadDir = File("/storage/emulated/0/Download")
        val modelsDir = context.filesDir

        val filesToCopy = listOf(
            "phi3.onnx",
            "phi3.onnx.data",
            "tokenizer.json"
        )

        try {
            filesToCopy.forEachIndexed { index, fileName ->
                val sourceFile = File(downloadDir, fileName)
                val destFile = File(modelsDir, fileName)

                if (!sourceFile.exists()) {
                    onStatusUpdate("خطأ: لم يتم العثور على الملف المصدر ${sourceFile.path}")
                    return@withContext
                }

                onStatusUpdate("جاري نسخ الملف ${index + 1}/${filesToCopy.size}: $fileName...")

                sourceFile.inputStream().use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                }
            }
            onStatusUpdate("اكتملت عملية النسخ بنجاح!")
        } catch (e: Exception) {
            onStatusUpdate("فشل نسخ الملف: ${e.message}")
        }
    }
}

private fun isAccessibilityServiceEnabled(context: Context): Boolean {
    val serviceId = "${context.packageName}/${ManusAccessibilityService::class.java.canonicalName}"
    val settingValue = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    )
    return settingValue?.contains(serviceId) ?: false
}
