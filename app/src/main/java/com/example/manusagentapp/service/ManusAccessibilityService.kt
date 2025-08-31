package com.example.manusagentapp.service

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import com.example.manusagentapp.core.*
import kotlinx.coroutines.*

class ManusAccessibilityService : AccessibilityService() {

    private lateinit var brain: Brain
    private lateinit var eyes: Eyes
    private lateinit var hands: Hands
    private lateinit var memory: Memory

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var isAgentActive = false
    private var currentTask: Task? = null

    // --- هذا هو الجزء الجديد والمهم ---
    private val commandReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                "com.example.manusagentapp.START_TASK" -> {
                    val command = intent.getStringExtra("user_command")
                    if (!command.isNullOrBlank()) {
                        startTask(command)
                    }
                }
                "com.example.manusagentapp.STOP_TASK" -> {
                    stopTask()
                }
            }
        }
    }
    // --- نهاية الجزء الجديد ---

    override fun onServiceConnected() {
        super.onServiceConnected()

        brain = Brain(this)
        eyes = Eyes(this)
        hands = Hands(this)
        memory = Memory(this)

        serviceScope.launch {
            brain.initialize()
        }

        // --- هذا هو الجزء الجديد والمهم ---
        val intentFilter = IntentFilter().apply {
            addAction("com.example.manusagentapp.START_TASK")
            addAction("com.example.manusagentapp.STOP_TASK")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(commandReceiver, intentFilter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(commandReceiver, intentFilter)
        }
        // --- نهاية الجزء الجديد ---
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // لا تفعل شيئًا هنا حاليًا، الحلقة ستُدار يدويًا
    }

    override fun onInterrupt() {
        isAgentActive = false
    }

    private fun startTask(userCommand: String) {
        if (isAgentActive) return // منع بدء مهمة جديدة إذا كانت هناك واحدة قيد التشغيل

        serviceScope.launch {
            try {
                isAgentActive = true
                val taskPlan = brain.analyzeUserGoal(userCommand)
                currentTask = memory.startNewTask(userCommand, taskPlan)
                runAgentLoop()
            } catch (e: Exception) {
                handleError("Error starting task: ${e.message}")
            }
        }
    }

    private fun stopTask() {
        isAgentActive = false
        currentTask = null
        serviceScope.launch {
            memory.clearMemory() // مسح الذاكرة عند إيقاف المهمة
        }
    }

    private suspend fun runAgentLoop() {
        if (!isAgentActive || currentTask == null) return

        try {
            delay(1000) // انتظار قصير قبل كل إجراء
            val screenContext = eyes.analyzeCurrentScreen()
            val actionDecision = brain.decideNextAction(screenContext, currentTask!!.taskPlan)
            val success = executeAction(actionDecision)
            memory.recordExecutionStep(actionDecision, screenContext, success)

            if (actionDecision is ActionDecision.TaskCompleted || !success) {
                memory.updateTaskStatus(if (success) TaskStatus.COMPLETED else TaskStatus.FAILED)
                isAgentActive = false
                notifyUi("TASK_COMPLETED")
            } else {
                memory.advanceToNextStep()
                if (isAgentActive) {
                    runAgentLoop() // استدعاء الحلقة مرة أخرى
                }
            }
        } catch (e: Exception) {
            handleError("Error in agent loop: ${e.message}")
        }
    }

    private suspend fun executeAction(action: ActionDecision): Boolean {
        return when (action) {
            is ActionDecision.Click -> hands.clickBounds(action.bounds)
            is ActionDecision.Type -> {
                // البحث عن العقدة الصحيحة قبل الكتابة
                val node = eyes.analyzeCurrentScreen().elements.find { it.bounds == action.bounds && it.isEditable }
                if (node != null) {
                    hands.typeText(node, action.text)
                } else {
                    false
                }
            }
            is ActionDecision.Scroll -> hands.scroll(action.direction)
            is ActionDecision.Wait -> { delay(action.milliseconds); true }
            is ActionDecision.TaskCompleted -> true
            is ActionDecision.Error -> { handleError(action.message); false }
        }
    }

    private suspend fun handleError(errorMessage: String) {
        memory.updateTaskStatus(TaskStatus.FAILED)
        isAgentActive = false
        notifyUi("ERROR", "error_message" to errorMessage)
    }

    private fun notifyUi(action: String, vararg extras: Pair<String, Any?>) {
        val intent = Intent("com.example.manusagentapp.$action")
        extras.forEach { (key, value) ->
            when (value) {
                is String -> intent.putExtra(key, value)
                is Boolean -> intent.putExtra(key, value)
                is Int -> intent.putExtra(key, value)
                is Long -> intent.putExtra(key, value)
                is Float -> intent.putExtra(key, value)
            }
        }
        sendBroadcast(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(commandReceiver)
        serviceScope.cancel()
    }
}
