package com.example.manusagentapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
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
import com.example.manusagentapp.ui.theme.ManusAgentAppTheme

class MainActivity : ComponentActivity() {
    
    private lateinit var taskUpdateReceiver: BroadcastReceiver
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        taskUpdateReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                // يمكنك إضافة منطق هنا لتحديث الواجهة بناءً على حالة المهمة
            }
        }
        
        // --- هذا هو الجزء الذي تم تصحيحه ---
        val intentFilter = IntentFilter().apply {
            addAction("com.example.manusagentapp.TASK_COMPLETED")
            addAction("com.example.manusagentapp.ERROR")
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(taskUpdateReceiver, intentFilter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(taskUpdateReceiver, intentFilter)
        }
        
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
        unregisterReceiver(taskUpdateReceiver)
    }
}

@Composable
fun MainScreen() {
    val context = LocalContext.current
    var isServiceEnabled by remember { mutableStateOf(false) }
    var userCommand by remember { mutableStateOf("") }
    var isTaskRunning by remember { mutableStateOf(false) }
    
    // فحص حالة خدمة الوصولية بشكل دوري
    LaunchedEffect(key1 = isServiceEnabled) {
        isServiceEnabled = isAccessibilityServiceEnabled(context)
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        
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
                            isTaskRunning = true
                            val serviceIntent = Intent("com.example.manusagentapp.START_TASK")
                            serviceIntent.putExtra("user_command", userCommand)
                            context.sendBroadcast(serviceIntent)
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
                            val serviceIntent = Intent("com.example.manusagentapp.STOP_TASK")
                            context.sendBroadcast(serviceIntent)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("إيقاف")
                    }
                }
            }
        }
    }
}

private fun isAccessibilityServiceEnabled(context: Context): Boolean {
    val accessibilityServices = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    )
    return accessibilityServices?.contains("com.example.manusagentapp/.service.ManusAccessibilityService") == true
}
