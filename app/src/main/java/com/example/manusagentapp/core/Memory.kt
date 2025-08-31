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
 */
class Memory(private val context: Context) {
    
    private val sharedPreferences: SharedPreferences = 
        context.getSharedPreferences("manus_agent_memory", Context.MODE_PRIVATE)
    
    private val json = Json { 
        ignoreUnknownKeys = true
        encodeDefaults = true
        // هذا السطر مهم للتعامل مع الكلاسات المتعددة
        serializersModule = kotlinx.serialization.modules.SerializersModule {
            polymorphic(ActionDecision::class) {
                subclass(ActionDecision.TaskCompleted::class, ActionDecision.TaskCompleted.serializer())
                subclass(ActionDecision.Click::class, ActionDecision.Click.serializer())
                subclass(ActionDecision.Type::class, ActionDecision.Type.serializer())
                subclass(ActionDecision.Scroll::class, ActionDecision.Scroll.serializer())
                subclass(ActionDecision.Wait::class, ActionDecision.Wait.serializer())
                subclass(ActionDecision.Error::class, ActionDecision.Error.serializer())
            }
        }
    }
    
    private var currentTask: Task? = null
    private val executionHistory = mutableListOf<ExecutionStep>()
    private val loopDetector = LoopDetector()
    
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
        
        currentTask?.let { saveTaskToStorage(it) }
        
        if (loopDetector.detectLoop(executionHistory)) {
            throw LoopDetectedException("تم اكتشاف حلقة مفرغة في التنفيذ")
        }
    }
    
    suspend fun updateTaskStatus(status: TaskStatus) = withContext(Dispatchers.IO) {
        currentTask?.let { task ->
            task.status = status
            if (status == TaskStatus.COMPLETED || status == TaskStatus.FAILED) {
                task.endTime = System.currentTimeMillis()
            }
            saveTaskToStorage(task)
        }
    }
    
    fun getCurrentTask(): Task? = currentTask
    
    fun getExecutionHistory(): List<ExecutionStep> = executionHistory.toList()
    
    fun isTaskCompleted(): Boolean {
        return currentTask?.status == TaskStatus.COMPLETED
    }
    
    fun getNextStep(): String? {
        return currentTask?.taskPlan?.let { plan ->
            if (!plan.isCompleted()) {
                plan.getCurrentStep()
            } else {
                null
            }
        }
    }
    
    fun advanceToNextStep() {
        currentTask?.taskPlan?.nextStep()
    }
    
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
    
    suspend fun clearMemory() = withContext(Dispatchers.IO) {
        currentTask = null
        executionHistory.clear()
        loopDetector.reset()
        sharedPreferences.edit().clear().apply()
    }
    
    private fun generateTaskId(): String {
        return "task_${System.currentTimeMillis()}_${(1000..9999).random()}"
    }
    
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

@Serializable
enum class TaskStatus {
    IN_PROGRESS,
    COMPLETED,
    FAILED,
    PAUSED
}

@Serializable
data class ExecutionStep(
    val timestamp: Long,
    val action: ActionDecision,
    val screenContext: ScreenContext,
    val success: Boolean,
    val errorMessage: String? = null
)

class LoopDetector {
    private val recentActions = mutableListOf<String>()
    private val maxHistorySize = 10
    private val loopThreshold = 3
    
    fun detectLoop(history: List<ExecutionStep>): Boolean {
        if (history.size < loopThreshold * 2) return false
        
        val recentActions = history.takeLast(loopThreshold * 2)
            .map { "${it.action::class.simpleName}_${it.screenContext.elements.size}" }
        
        val firstHalf = recentActions.take(loopThreshold)
        val secondHalf = recentActions.drop(loopThreshold)
        
        return firstHalf == secondHalf
    }
    
    fun reset() {
        recentActions.clear()
    }
}

data class PerformanceStats(
    val totalSteps: Int = 0,
    val successfulSteps: Int = 0,
    val failedSteps: Int = 0,
    val duration: Long = 0,
    val successRate: Float = 0f
)

class LoopDetectedException(message: String) : Exception(message)
