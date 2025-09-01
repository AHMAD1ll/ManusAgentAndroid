package com.example.manusagentapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.manusagentapp.ui.theme.ManusAgentAppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

class MainActivity : ComponentActivity() {

    private val permissionGranted = mutableStateOf(false)

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            permissionGranted.value = isGranted
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkStoragePermission()
        setContent {
            ManusAgentAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (permissionGranted.value) {
                        MainScreen()
                    } else {
                        PermissionRequestScreen {
                            requestStoragePermission()
                        }
                    }
                }
            }
        }
    }

    private fun checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionGranted.value = true
        } else {
            permissionGranted.value = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestStoragePermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }
}

// --- File Copying Logic ---

private fun copyModelFiles(context: android.content.Context, onStatusUpdate: (String) -> Unit) {
    val modelFileNames = listOf("phi3.onnx.data", "phi3.onnx", "tokenizer.json")
    val sourceDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
    val destDir = context.filesDir // App's private directory

    onStatusUpdate("جاري التحقق من الملفات...")

    var allFilesExist = true
    for (fileName in modelFileNames) {
        val destFile = File(destDir, fileName)
        if (!destFile.exists()) {
            allFilesExist = false
            break
        }
    }

    if (allFilesExist) {
        onStatusUpdate("التهيئة مكتملة. الملفات موجودة بالفعل.")
        return
    }

    onStatusUpdate("بدء عملية النسخ لمرة واحدة...")

    for (fileName in modelFileNames) {
        val sourceFile = File(sourceDir, fileName)
        val destFile = File(destDir, fileName)

        if (!sourceFile.exists()) {
            onStatusUpdate("خطأ: لم يتم العثور على الملف المصدر ${sourceFile.path}")
            Log.e("FileCopy", "Source file not found: ${sourceFile.path}")
            return // Stop if any source file is missing
        }

        if (destFile.exists()) {
            Log.d("FileCopy", "File ${destFile.name} already exists. Skipping.")
            continue // Skip if destination file already exists
        }

        try {
            onStatusUpdate("جاري نسخ ${sourceFile.name}...")
            Log.d("FileCopy", "Copying ${sourceFile.name} to ${destFile.name}")
            sourceFile.inputStream().use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            Log.d("FileCopy", "Finished copying ${sourceFile.name}")
        } catch (e: IOException) {
            onStatusUpdate("فشل نسخ الملف: ${e.message}")
            Log.e("FileCopy", "Failed to copy file", e)
            return // Stop on error
        }
    }

    onStatusUpdate("اكتملت عملية النسخ بنجاح!")
    Log.d("FileCopy", "All files copied successfully.")
}


// --- UI Composables ---

@Composable
fun PermissionRequestScreen(onRequestPermission: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("إذن الوصول للملفات مطلوب", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(16.dp))
        Text("يحتاج التطبيق إلى إذن لقراءة ملفات النموذج (الذكاء الاصطناعي) التي قمت بتنزيلها. هذه العملية تتم لمرة واحدة فقط لتهيئة التطبيق.", style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onRequestPermission) { Text("منح الإذن") }
    }
}

@Composable
fun MainScreen() {
    val context = LocalContext.current
    var copyStatus by remember { mutableStateOf("في الانتظار...") }
    val coroutineScope = rememberCoroutineScope()

    // This effect runs once when MainScreen is first displayed
    LaunchedEffect(Unit) {
        coroutineScope.launch(Dispatchers.IO) { // Run in a background thread
            copyModelFiles(context) { status ->
                // Update UI from the background thread
                copyStatus = status
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Manus Agent", style = MaterialTheme.typography.headlineLarge)
        Spacer(modifier = Modifier.height(16.dp))

        // Display copy status and a progress indicator
        Text(copyStatus, style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(8.dp))
        if (copyStatus.contains("جاري") || copyStatus.contains("بدء")) {
            CircularProgressIndicator()
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            context.startActivity(intent)
        }) {
            Text("تفعيل خدمة الوصولية")
        }
    }
}
