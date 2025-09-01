package com.example.manusagentapp

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    var copyStatus by remember { mutableStateOf("التحقق من الملفات...") }
    var filesExist by remember { mutableStateOf(false) }
    val isAccessibilityServiceEnabled by isAccessibilityServiceEnabledAsState()

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permission granted, now copy files
            CoroutineScope(Dispatchers.IO).launch {
                copyModelFiles(context) { status ->
                    copyStatus = status
                    if (status.contains("مكتملة")) {
                        filesExist = true
                    }
                }
            }
        } else {
            copyStatus = "تم رفض إذن الوصول للملفات. لا يمكن متابعة التهيئة."
        }
    }

    LaunchedEffect(Unit) {
        val modelsDir = context.filesDir
        val file1 = File(modelsDir, "phi3.onnx")
        val file2 = File(modelsDir, "phi3.onnx.data")
        val file3 = File(modelsDir, "tokenizer.json")

        if (file1.exists() && file2.exists() && file3.exists()) {
            filesExist = true
            copyStatus = "التهيئة مكتملة. الملفات موجودة بالفعل."
        } else {
            requestPermissionLauncher.launch(android.Manifest.permission.READ_EXTERNAL_STORAGE)
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

@Composable
fun isAccessibilityServiceEnabledAsState(): State<Boolean> {
    val context = LocalContext.current
    val enabledState = remember { mutableStateOf(isAccessibilityServiceEnabled(context)) }

    DisposableEffect(context) {
        val accessibilityManager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as android.view.accessibility.AccessibilityManager

        val listener = android.view.accessibility.AccessibilityManager.AccessibilityStateChangeListener { isEnabled ->
            enabledState.value = isEnabled && isAccessibilityServiceEnabled(context) // Re-check our specific service
        }

        accessibilityManager.addAccessibilityStateChangeListener(listener)

        onDispose {
            accessibilityManager.removeAccessibilityStateChangeListener(listener)
        }
    }
    return enabledState
}

private fun isAccessibilityServiceEnabled(context: Context): Boolean {
    val serviceId = "${context.packageName}/${ManusAccessibilityService::class.java.canonicalName}"
    val settingValue = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    )
    return settingValue?.contains(serviceId) ?: false
}
