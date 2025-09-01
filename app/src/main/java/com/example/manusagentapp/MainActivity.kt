package com.example.manusagentapp

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
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
    var hasStoragePermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) true
            else ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted -> hasStoragePermission = isGranted }
    )

    if (hasStoragePermission) {
        MainScreen()
    } else {
        PermissionRequestScreen {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
    }
}

@Composable
fun MainScreen() {
    val context = LocalContext.current
    var copyStatus by remember { mutableStateOf("في الانتظار...") }
    var isAccessibilityServiceEnabled by remember { mutableStateOf(false) }

    // This effect will run once to start the file copy process
    LaunchedEffect(Unit) {
        launch(Dispatchers.IO) {
            copyModelFiles(context) { status -> copyStatus = status }
        }
    }

    // This effect will run periodically to check the accessibility service status
    LaunchedEffect(Unit) {
        while (true) {
            isAccessibilityServiceEnabled = isAccessibilityServiceEnabled(context)
            delay(1000) // Check every second
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Manus Agent", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(16.dp))

        // --- File Copy Status ---
        Text(copyStatus, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
        if (copyStatus.contains("جاري") || copyStatus.contains("بدء")) {
            Spacer(Modifier.height(8.dp))
            CircularProgressIndicator()
        }
        Spacer(Modifier.height(24.dp))

        // --- Accessibility Service Status ---
        val serviceStatusText = if (isAccessibilityServiceEnabled) "الخدمة نشطة" else "الخدمة متوقفة"
        val serviceStatusColor = if (isAccessibilityServiceEnabled) Color(0xFF4CAF50) else Color.Red
        Text(serviceStatusText, color = serviceStatusColor, style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))

        // Show the button ONLY if the service is NOT enabled
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

// Helper function to check if the accessibility service is enabled
fun isAccessibilityServiceEnabled(context: Context): Boolean {
    val expectedComponentName = "com.example.manusagentapp/.service.ManusAccessibilityService"
    val enabledServicesSetting = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
    val colonSplitter = TextUtils.SimpleStringSplitter(':')
    colonSplitter.setString(enabledServicesSetting)
    while (colonSplitter.hasNext()) {
        val componentName = colonSplitter.next()
        if (componentName.equals(expectedComponentName, ignoreCase = true)) {
            return true
        }
    }
    return false
}

// --- Permission and File Copy Logic (No changes below this line) ---

@Composable
fun PermissionRequestScreen(onRequestPermission: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("إذن الوصول للملفات مطلوب", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        Text("يحتاج التطبيق إلى إذن لقراءة ملفات النموذج (الذكاء الاصطناعي) التي قمت بتنزيلها. هذه العملية تتم لمرة واحدة فقط لتهيئة التطبيق.", style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        Button(onClick = onRequestPermission) { Text("منح الإذن") }
    }
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
