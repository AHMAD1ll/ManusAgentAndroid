package com.example.manusagentapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
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
import androidx.core.content.ContextCompat
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

// ... (بقية الكود في MainActivity لا يتغير) ...
@Composable
fun MainScreen() {
    val context = LocalContext.current
    var serviceEnabled by remember { mutableStateOf(false) }

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                serviceEnabled = when (intent?.getStringExtra(ManusAccessibilityService.EXTRA_STATE)) {
                    ManusAccessibilityService.STATE_CONNECTED,
                    ManusAccessibilityService.STATE_MODEL_LOAD_SUCCESS -> true
                    else -> false
                }
            }
        }
        val filter = IntentFilter(ManusAccessibilityService.ACTION_SERVICE_STATE_CHANGED)
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        serviceEnabled = isAccessibilityServiceEnabled(context)
        onDispose {
            context.unregisterReceiver(receiver)
        }
    }

    if (serviceEnabled) {
        AgentControlScreen()
    } else {
        InitialSetupScreen()
    }
}

@Composable
fun InitialSetupScreen() {
    val context = LocalContext.current
    var setupStatus by remember { mutableStateOf("التحقق من الأذونات...") }
    val requestAllFilesAccessLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { /* Handled by LaunchedEffect */ }

    LaunchedEffect(Unit) {
        if (hasAllFilesAccess()) {
            val modelsDir = context.filesDir
            val allFilesPresent = listOf("phi3.onnx", "phi3.onnx.data", "tokenizer.json")
                .all { File(modelsDir, it).exists() }
            if (allFilesPresent) {
                setupStatus = "الملفات جاهزة. يرجى تفعيل الخدمة."
            } else {
                copyModelFiles(context) { status -> setupStatus = status }
            }
        } else {
            setupStatus = "نحتاج إذن الوصول لكل الملفات لنسخ نماذج الذكاء الاصطناعي."
            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
            intent.data = Uri.parse("package:${context.packageName}")
            requestAllFilesAccessLauncher.launch(intent)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Manus Agent Setup", fontSize = 32.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(32.dp))
        Text(setupStatus, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            context.startActivity(intent)
        }) {
            Text("تفعيل خدمة الوصولية")
        }
    }
}

@Composable
fun AgentControlScreen() {
    var command by remember { mutableStateOf("") }
    var agentStatus by remember { mutableStateOf("جاهز لاستقبال الأوامر") }
    var serviceStatusColor by remember { mutableStateOf(Color.Green) }
    val context = LocalContext.current

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.getStringExtra(ManusAccessibilityService.EXTRA_STATE)) {
                    ManusAccessibilityService.STATE_MODEL_LOAD_SUCCESS -> {
                        agentStatus = intent.getStringExtra("EXTRA_MESSAGE") ?: "النموذج جاهز"
                        serviceStatusColor = Color.Green
                    }
                    ManusAccessibilityService.STATE_MODEL_LOAD_FAIL -> {
                        agentStatus = intent.getStringExtra("EXTRA_MESSAGE") ?: "فشل تحميل النموذج"
                        serviceStatusColor = Color.Red
                    }
                }
            }
        }
        val filter = IntentFilter(ManusAccessibilityService.ACTION_SERVICE_STATE_CHANGED)
        ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        onDispose { context.unregisterReceiver(receiver) }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Manus Agent", fontSize = 32.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = agentStatus, color = serviceStatusColor, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(32.dp))
        OutlinedTextField(
            value = command,
            onValueChange = { command = it },
            label = { Text("أدخل الأمر هنا") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                if (command.isNotBlank()) {
                    val intent = Intent(context, ManusAccessibilityService::class.java).apply {
                        action = ManusAccessibilityService.ACTION_COMMAND
                        putExtra(ManusAccessibilityService.EXTRA_COMMAND_TEXT, command)
                    }
                    context.startService(intent)
                }
            },
            enabled = command.isNotBlank()
        ) {
            Text("بدء المهمة")
        }
    }
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
        val filesToCopy = listOf("phi3.onnx", "phi3.onnx.data", "tokenizer.json")
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
                    FileOutputStream(destFile).use { output -> input.copyTo(output) }
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
    val settingValue = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
    return settingValue?.contains(serviceId) ?: false
}
