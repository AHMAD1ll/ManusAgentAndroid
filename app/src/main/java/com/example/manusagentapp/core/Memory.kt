package com.example.manusagentapp.core

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

/**
 * الذاكرة (The Memory) - مدير المهام والحالة
 * يحتفظ بالهدف النهائي ويتتبع الخطوات المنجزة ويمنع الحلقات المفرغة
 */
class Memory(private val context: Context) {
    
    private val sharedPreferences: SharedPreferences = 
        context.getSharedPreferences("manus_agent_memory", Context.MODE_PRIVATE)
    
    private val json = Json { 
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    
    private var currentTask: Task? = null
    private val executionHistory = mutableListOf<ExecutionStep>()
    private val loopDetector = LoopDetector()
    
    /**
     * بدء مهمة جديدة
     */
    suspend fun startNewTask(userGoal: String, taskPlan: TaskPlan): Task = withContext(Dispatchers.IO) {
        val task = Task(
            id = generateTaskId(),
            userGoal = userGoal,
            taskPlan = taskPlan,
            status = TaskStatus.IN_PROGRESS,
            startTime = System.currentTimeMillis(),
            steps = mutableListOf()
        )
        
        currentTask = task
        saveTaskToStorage(task)
        
        task
    }
    
    /**
     * تسجيل خطوة تنفيذ
     */
    suspend fun recordExecutionStep(
        action: ActionDecision,
        screenContext: ScreenContext,
        success: Boolean,
        errorMessage: String? = null
    ) = withContext(Dispatchers.IO) {
        val step = ExecutionStep(
            timestamp = System.currentTimeMillis(),
            action = action,
            screenContext = screenContext,
            success = success,
            errorMessage = errorMessage
        )
        
        executionHistory.add(step)
        currentTask?.steps?.add(step)
        
        // حفظ التحديث
        currentTask?.let { saveTaskToStorage(it) }
        
        // فحص الحلقات المفرغة
        if (loopDetector.detectLoop(executionHistory)) {
            throw LoopDetectedException("تم اكتشاف حلقة مفرغة في التنفيذ")
        }
    }
    
    /**
     * تحديث حالة المهمة
     */
    suspend fun updateTaskStatus(status: TaskStatus) = withContext(Dispatchers.IO) {
        currentTask?.let { task ->
            task.status = status
            if (status == TaskStatus.COMPLETED || status == TaskStatus.FAILED) {
                task.endTime = System.currentTimeMillis()
            }
            saveTaskToStorage(task)
        }
    }
    
    /**
     * الحصول على المهمة الحالية
     */
    fun getCurrentTask(): Task? = currentTask
    
    /**
     * الحصول على تاريخ التنفيذ
     */
    fun getExecutionHistory(): List<ExecutionStep> = executionHistory.toList()
    
    /**
     * فحص ما إذا كانت المهمة مكتملة
     */
    fun isTaskCompleted(): Boolean {
        return currentTask?.status == TaskStatus.COMPLETED
    }
    
    /**
     * الحصول على الخطوة التالية في المهمة
     */
    fun getNextStep(): String? {
        return currentTask?.taskPlan?.let { plan ->
            if (!plan.isCompleted()) {
                plan.getCurrentStep()
            } else {
                null
            }
        }
    }
    
    /**
     * تقدم إلى الخطوة التالية
     */
    fun advanceToNextStep() {
        currentTask?.taskPlan?.nextStep()
    }
    
    /**
     * حفظ المهمة في التخزين المحلي
     */
    private suspend fun saveTaskToStorage(task: Task) = withContext(Dispatchers.IO) {
        try {
            val taskJson = json.encodeToString(task)
            sharedPreferences.edit()
                .putString("current_task", taskJson)
                .putLong("last_update", System.currentTimeMillis())
                .apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * استرداد المهمة من التخزين المحلي
     */
    suspend fun restoreTaskFromStorage(): Task? = withContext(Dispatchers.IO) {
        try {
            val taskJson = sharedPreferences.getString("current_task", null)
            if (taskJson != null) {
                val task = json.decodeFromString<Task>(taskJson)
                currentTask = task
                task
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * مسح الذاكرة
     */
    suspend fun clearMemory() = withContext(Dispatchers.IO) {
        currentTask = null
        executionHistory.clear()
        loopDetector.reset()
        sharedPreferences.edit().clear().apply()
    }
    
    /**
     * توليد معرف فريد للمهمة
     */
    private fun generateTaskId(): String {
        return "task_${System.currentTimeMillis()}_${(1000..9999).random()}"
    }
    
    /**
     * الحصول على إحصائيات الأداء
     */
    fun getPerformanceStats(): PerformanceStats {
        val task = currentTask ?: return PerformanceStats()
        
        val totalSteps = task.steps.size
        val successfulSteps = task.steps.count { it.success }
        val failedSteps = totalSteps - successfulSteps
        val duration = if (task.endTime != null) {
            task.endTime!! - task.startTime
        } else {
            System.currentTimeMillis() - task.startTime
        }
        
        return PerformanceStats(
            totalSteps = totalSteps,
            successfulSteps = successfulSteps,
            failedSteps = failedSteps,
            duration = duration,
            successRate = if (totalSteps > 0) successfulSteps.toFloat() / totalSteps else 0f
        )
    }
}

/**
 * مهمة
 */
@Serializable
data class Task(
    val id: String,
    val userGoal: String,
    val taskPlan: TaskPlan,
    var status: TaskStatus,
    val startTime: Long,
    var endTime: Long? = null,
    val steps: MutableList<ExecutionStep> = mutableListOf()
)

/**
 * حالة المهمة
 */
@Serializable
enum class TaskStatus {
    IN_PROGRESS,
    COMPLETED,
    FAILED,
    PAUSED
}

/**
 * خطوة تنفيذ
 */
@Serializable
data class ExecutionStep(
    val timestamp: Long,
    val action: ActionDecision,
    val screenContext: ScreenContext,
    val success: Boolean,
    val errorMessage: String? = null
)

/**
 * كاشف الحلقات المفرغة
 */
class LoopDetector {
    private val recentActions = mutableListOf<String>()
    private val maxHistorySize = 10
    private val loopThreshold = 3
    
    fun detectLoop(history: List<ExecutionStep>): Boolean {
        if (history.size < loopThreshold * 2) return false
        
        val recentActions = history.takeLast(loopThreshold * 2)
            .map { "${it.action::class.simpleName}_${it.screenContext.elements.size}" }
        
        // فحص التكرار
        val firstHalf = recentActions.take(loopThreshold)
        val secondHalf = recentActions.drop(loopThreshold)
        
        return firstHalf == secondHalf
    }
    
    fun reset() {
        recentActions.clear()
    }
}

/**
 * إحصائيات الأداء
 */
data class PerformanceStats(
    val totalSteps: Int = 0,
    val successfulSteps: Int = 0,
    val failedSteps: Int = 0,
    val duration: Long = 0,
    val successRate: Float = 0f
)

/**
 * استثناء اكتشاف الحلقة المفرغة
 */
class LoopDetectedException(message: String) : Exception(message)

