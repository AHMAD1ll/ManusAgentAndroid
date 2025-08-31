package com.example.manusagentapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.manusagentapp.service.ManusAccessibilityService
import com.example.manusagentapp.ui.theme.ManusAgentAppTheme

class MainActivity : ComponentActivity() {
    
    private var accessibilityService: ManusAccessibilityService? = null
    private lateinit var taskReceiver: BroadcastReceiver
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // تسجيل مستقبل البث للإشعارات
        taskReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    "com.example.manusagentapp.TASK_COMPLETED" -> {
                        // معالجة إكمال المهمة
                    }
                    "com.example.manusagentapp.ERROR" -> {
                        // معالجة الأخطاء
                    }
                }
            }
        }
        
        val filter = IntentFilter().apply {
            addAction("com.example.manusagentapp.TASK_COMPLETED")
            addAction("com.example.manusagentapp.ERROR")
        }
        registerReceiver(taskReceiver, filter)
        
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
    
    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(taskReceiver)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val context = LocalContext.current
    var isServiceEnabled by remember { mutableStateOf(false) }
    var userCommand by remember { mutableStateOf("") }
    var isTaskRunning by remember { mutableStateOf(false) }
    
    // فحص حالة خدمة الوصولية
    LaunchedEffect(Unit) {
        isServiceEnabled = isAccessibilityServiceEnabled(context)
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        
        // شعار التطبيق والعنوان
        Text(
            text = "Manus Agent",
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "AI-Powered Android Automation",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // حالة خدمة الوصولية
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (isServiceEnabled) 
                    MaterialTheme.colorScheme.primaryContainer 
                else 
                    MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isServiceEnabled) "✓ الخدمة مفعلة" else "⚠ الخدمة غير مفعلة",
                    style = MaterialTheme.typography.titleMedium
                )
                
                if (!isServiceEnabled) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "يجب تمكين خدمة الوصولية لكي يعمل التطبيق",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(
                        onClick = {
                            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                            context.startActivity(intent)
                        }
                    ) {
                        Text("فتح إعدادات الوصولية")
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // واجهة إدخال الأوامر
        if (isServiceEnabled) {
            OutlinedTextField(
                value = userCommand,
                onValueChange = { userCommand = it },
                label = { Text("أدخل الأمر هنا...") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isTaskRunning,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                maxLines = 3
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = {
                        if (userCommand.isNotBlank()) {
                            // بدء المهمة
                            isTaskRunning = true
                            // TODO: استدعاء خدمة الوصولية
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !isTaskRunning && userCommand.isNotBlank()
                ) {
                    if (isTaskRunning) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(if (isTaskRunning) "جاري التنفيذ..." else "بدء المهمة")
                }
                
                if (isTaskRunning) {
                    OutlinedButton(
                        onClick = {
                            isTaskRunning = false
                            // TODO: إيقاف المهمة
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("إيقاف")
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // أمثلة على الأوامر
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "أمثلة على الأوامر:",
                        style = MaterialTheme.typography.titleMedium
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    val examples = listOf(
                        "افتح تطبيق الواتساب",
                        "أرسل رسالة إلى أحمد",
                        "افتح الكاميرا والتقط صورة",
                        "ابحث عن مطعم قريب في خرائط جوجل"
                    )
                    
                    examples.forEach { example ->
                        TextButton(
                            onClick = { userCommand = example },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "• $example",
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Start
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * فحص ما إذا كانت خدمة الوصولية مفعلة
 */
private fun isAccessibilityServiceEnabled(context: Context): Boolean {
    val accessibilityServices = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    )
    return accessibilityServices?.contains("com.example.manusagentapp/.service.ManusAccessibilityService") == true
}

