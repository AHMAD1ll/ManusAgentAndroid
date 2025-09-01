// المسار: app/src/main/java/com/example/manusagentapp/MainActivity.kt
// (فقط قم بتعديل دالة AgentControlScreen)

@Composable
fun AgentControlScreen() {
    var command by remember { mutableStateOf("") }
    var agentStatus by remember { mutableStateOf("جاهز لاستقبال الأوامر") }
    var serviceStatusColor by remember { mutableStateOf(Color.Green) }
    val context = LocalContext.current // *** تأكد من وجود هذا السطر ***

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
        Text(
            text = agentStatus,
            color = serviceStatusColor,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
        Spacer(modifier = Modifier.height(32.dp))
        OutlinedTextField(
            value = command,
            onValueChange = { command = it },
            label = { Text("أدخل الأمر هنا") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        // *** الإصلاح الرئيسي هنا: إضافة منطق الزر ***
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
            enabled = command.isNotBlank() // يتم تفعيل الزر فقط إذا كان هناك نص
        ) {
            Text("بدء المهمة")
        }
    }
}

// (باقي الملف يبقى كما هو)
