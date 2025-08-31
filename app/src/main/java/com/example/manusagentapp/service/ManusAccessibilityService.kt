package com.example.manusagentapp.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.example.manusagentapp.core.*
import kotlinx.coroutines.*

/**
 * خدمة الوصولية الرئيسية لـ Manus Agent
 * تدير الحلقة الأساسية: Observe -> Decide -> Act
 */
class ManusAccessibilityService : AccessibilityService() {
    
    private lateinit var brain: Brain
    private lateinit var eyes: Eyes
    private lateinit var hands: Hands
    private lateinit var memory: Memory
    
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var isAgentActive = false
    private var currentTask: Task? = null
    
    override fun onServiceConnected() {
        super.onServiceConnected()
        
        // تهيئة المكونات الأساسية
        brain = Brain(this)
        eyes = Eyes(this)
        hands = Hands(this)
        memory = Memory(this)
        
        // تهيئة العقل (LLM)
        serviceScope.launch {
            brain.initialize()
        }
        
        // استرداد المهمة المحفوظة إن وجدت
        serviceScope.launch {
            currentTask = memory.restoreTaskFromStorage()
        }
    }
    
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!isAgentActive || event == null) return
        
        // تشغيل الحلقة الأساسية عند تغيير الشاشة
        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                serviceScope.launch {
                    runAgentLoop()
                }
            }
        }
    }
    
    override fun onInterrupt() {
        // إيقاف العمليات الجارية
        isAgentActive = false
    }
    
    /**
     * بدء تنفيذ مهمة جديدة
     */
    fun startTask(userCommand: String) {
        serviceScope.launch {
            try {
                isAgentActive = true
                
                // تحليل هدف المستخدم
                val taskPlan = brain.analyzeUserGoal(userCommand)
                
                // بدء المهمة في الذاكرة
                currentTask = memory.startNewTask(userCommand, taskPlan)
                
                // تشغيل الحلقة الأساسية
                runAgentLoop()
                
            } catch (e: Exception) {
                handleError("خطأ في بدء المهمة: ${e.message}")
            }
        }
    }
    
    /**
     * إيقاف المهمة الحالية
     */
    fun stopTask() {
        isAgentActive = false
        serviceScope.launch {
            memory.updateTaskStatus(TaskStatus.PAUSED)
        }
    }
    
    /**
     * الحلقة الأساسية للوكيل: Observe -> Decide -> Act
     */
    private suspend fun runAgentLoop() {
        if (!isAgentActive || currentTask == null) return
        
        try {
            // 1. المراقبة (Observe) - تحليل الشاشة الحالية
            val screenContext = eyes.analyzeCurrentScreen()
            
            // 2. القرار (Decide) - تحديد الإجراء التالي
            val actionDecision = brain.decideNextAction(screenContext, currentTask!!.taskPlan)
            
            // 3. التنفيذ (Act) - تنفيذ الإجراء
            val success = executeAction(actionDecision)
            
            // 4. التسجيل في الذاكرة
            memory.recordExecutionStep(actionDecision, screenContext, success)
            
            // 5. فحص اكتمال المهمة
            if (actionDecision is ActionDecision.TaskCompleted) {
                memory.updateTaskStatus(TaskStatus.COMPLETED)
                isAgentActive = false
                notifyTaskCompleted()
            } else if (success) {
                // الانتقال للخطوة التالية
                memory.advanceToNextStep()
                
                // انتظار قصير قبل الخطوة التالية
                delay(1000)
                
                // تكرار الحلقة
                if (isAgentActive) {
                    runAgentLoop()
                }
            }
            
        } catch (e: LoopDetectedException) {
            handleError("تم اكتشاف حلقة مفرغة: ${e.message}")
        } catch (e: Exception) {
            handleError("خطأ في تنفيذ المهمة: ${e.message}")
        }
    }
    
    /**
     * تنفيذ إجراء محدد
     */
    private suspend fun executeAction(action: ActionDecision): Boolean {
        return when (action) {
            is ActionDecision.Click -> {
                hands.clickBounds(action.bounds)
            }
            is ActionDecision.Type -> {
                val element = UIElement(
                    id = "",
                    text = "",
                    contentDescription = "",
                    className = "",
                    bounds = action.bounds,
                    isClickable = false,
                    isEditable = true,
                    isScrollable = false,
                    isVisible = true
                )
                hands.typeText(element, action.text)
            }
            is ActionDecision.Scroll -> {
                hands.scroll(action.direction)
            }
            is ActionDecision.Wait -> {
                hands.wait(action.milliseconds)
                true
            }
            is ActionDecision.TaskCompleted -> {
                true
            }
            is ActionDecision.Error -> {
                handleError(action.message)
                false
            }
        }
    }
    
    /**
     * معالجة الأخطاء
     */
    private suspend fun handleError(errorMessage: String) {
        memory.updateTaskStatus(TaskStatus.FAILED)
        isAgentActive = false
        
        // إرسال إشعار بالخطأ
        val intent = Intent("com.example.manusagentapp.ERROR")
        intent.putExtra("error_message", errorMessage)
        sendBroadcast(intent)
    }
    
    /**
     * إشعار اكتمال المهمة
     */
    private fun notifyTaskCompleted() {
        val intent = Intent("com.example.manusagentapp.TASK_COMPLETED")
        val stats = memory.getPerformanceStats()
        intent.putExtra("success_rate", stats.successRate)
        intent.putExtra("duration", stats.duration)
        sendBroadcast(intent)
    }
    
    /**
     * الحصول على حالة الوكيل
     */
    fun getAgentStatus(): AgentStatus {
        return AgentStatus(
            isActive = isAgentActive,
            currentTask = currentTask,
            performanceStats = memory.getPerformanceStats()
        )
    }
    
    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}

/**
 * حالة الوكيل
 */
data class AgentStatus(
    val isActive: Boolean,
    val currentTask: Task?,
    val performanceStats: PerformanceStats
)

