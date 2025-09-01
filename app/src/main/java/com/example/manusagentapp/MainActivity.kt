package com.example.manusagentapp

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.manusagentapp.service.ManusAccessibilityService
import com.example.manusagentapp.ui.theme.ManusAgentAppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ManusAgentAppTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    AppContent()
                }
            }
        }
    }
}

@Composable
fun AppContent() {
    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(false) }

    // This effect checks the permission state when the app starts or returns to focus
    LaunchedEffect(Unit) {
        while(true) {
            hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Environment.isExternalStorageManager()
            } else {
                // On older versions, the manifest permission is enough, but we'll re-check for safety
                context.checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                context.checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
            }
            delay(1000) // Re-check every second
        }
    }

    if (hasPermission) {
        MainScreen()
    } else {
        PermissionRequestScreen()
    }
}

@Composable
fun PermissionRequestScreen() {
    val context = LocalContext.current
    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("إذن إدارة الملفات مطلوب", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        Text("يحتاج التطبيق إلى إذن شامل لإدارة الملفات لقراءة نماذج الذكاء الاصطناعي من مجلد التنزيلات. هذا الإذن ضروري لعمل التطبيق.", style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        Button(onClick = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = Uri.parse("package:${context.packageName}")
                context.startActivity(intent)
            }
            // For older versions, a different mechanism would be needed, but this covers modern Android.
        }) {
            Text("الذهاب إلى الإعدادات ومنح الإذن")
        }
    }
}


@Composable
fun MainScreen() {
    val context = LocalContext.current
    var copyStatus by remember { mutableStateOf("في الانتظار...") }
    var isAccessibilityServiceEnabled by remember { mutableStateOf(isAccessibilityServiceEnabled(context)) }

    LaunchedEffect(Unit) {
        launch(Dispatchers.IO) {
            copyModelFiles(context) { status -> copyStatus = status }
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            isAccessibilityServiceEnabled = isAccessibilityServiceEnabled(context)
            delay(1000)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Manus Agent", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(16.dp))
        Text(copyStatus, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
        if (copyStatus.contains("جاري") || copyStatus.contains("بدء")) {
            Spacer(Modifier.height(8.dp))
            CircularProgressIndicator()
        }
        Spacer(Modifier.height(24.dp))
        val serviceStatusText = if (isAccessibilityServiceEnabled) "الخدمة نشطة" else "الخدمة متوقفة"
        val serviceStatusColor = if (isAccessibilityServiceEnabled) Color(0xFF4CAF50) else Color.Red
        Text(serviceStatusText, color = serviceStatusColor, style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))
        if (!isAccessibilityServiceEnabled) {
            Button(onClick = {
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                context.startActivity(intent)
            }) {
                Text("تفعيل خدمة الوصولية")
            }
        }
    }
}

fun isAccessibilityServiceEnabled(context: Context): Boolean {
    val expectedComponentName = "com.example.manusagentapp/.service.ManusAccessibilityService"
    try {
        val enabledServicesSetting = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        if (enabledServicesSetting != null) {
            val colonSplitter = TextUtils.SimpleStringSplitter(':')
            colonSplitter.setString(enabledServicesSetting)
            while (colonSplitter.hasNext()) {
                val componentName = colonSplitter.next()
                if (componentName.equals(expectedComponentName, ignoreCase = true)) {
                    return true
                }
            }
        }
    } catch (e: Exception) {
        Log.e("AccessibilityCheck", "Error checking accessibility service", e)
    }
    return false
}

private fun copyModelFiles(context: Context, onStatusUpdate: (String) -> Unit) {
    val modelFileNames = listOf("phi3.onnx.data", "phi3.onnx", "tokenizer.json")
    val sourceDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
    val destDir = context.filesDir
    onStatusUpdate("جاري التحقق من الملفات...")
    val allFilesExistInDest = modelFileNames.all { File(destDir, it).exists() }
    if (allFilesExistInDest) {
        onStatusUpdate("التهيئة مكتملة. الملفات موجودة بالفعل.")
        return
    }
    onStatusUpdate("بدء عملية النسخ لمرة واحدة...")
    for (fileName in modelFileNames) {
        val sourceFile = File(sourceDir, fileName)
        val destFile = File(destDir, fileName)
        if (!sourceFile.exists()) {
            onStatusUpdate("خطأ: لم يتم العثور على الملف ${sourceFile.name} في مجلد التنزيلات.")
            Log.e("FileCopy", "Source file not found: ${sourceFile.path}")
            return
        }
        if (destFile.exists()) {
            Log.d("FileCopy", "File ${destFile.name} already exists. Skipping.")
            continue
        }
        try {
            onStatusUpdate("جاري نسخ ${sourceFile.name}...")
            Log.d("FileCopy", "Copying ${sourceFile.name} to ${destFile.name}")
            sourceFile.inputStream().use { input -> destFile.outputStream().use { output -> input.copyTo(output) } }
            Log.d("FileCopy", "Finished copying ${sourceFile.name}")
        } catch (e: IOException) {
            onStatusUpdate("فشل نسخ الملف: ${e.message}")
            Log.e("FileCopy", "Failed to copy file", e)
            return
        }
    }
    onStatusUpdate("اكتملت عملية النسخ بنجاح!")
    Log.d("FileCopy", "All files copied successfully.")
}
