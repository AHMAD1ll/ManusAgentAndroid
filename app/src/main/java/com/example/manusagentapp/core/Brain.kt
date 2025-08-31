package com.example.manusagentapp.core

import android.content.Context
import android.graphics.Rect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

// --- الكلاس الجديد الذي يحل المشكلة ---
@Serializable
data class SerializableRect(val left: Int, val top: Int, val right: Int, val bottom: Int)

// --- دالة مساعدة للتحويل ---
fun Rect.toSerializableRect() = SerializableRect(this.left, this.top, this.right, this.bottom)


/**
 * العقل (The Brain) - مكون LLM للذكاء الاصطناعي
 */
class Brain(private val context: Context) {
    
    private var isInitialized = false
    
    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        try {
            isInitialized = true
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    suspend fun analyzeUserGoal(userCommand: String): TaskPlan = withContext(Dispatchers.Default) {
        if (!isInitialized) {
            throw IllegalStateException("Brain not initialized")
        }
        
        val steps = when {
            userCommand.contains("فتح") -> listOf("البحث عن التطبيق", "النقر على التطبيق")
            userCommand.contains("إرسال") -> listOf("فتح تطبيق الرسائل", "كتابة الرسالة", "إرسال")
            else -> listOf("تحليل الشاشة", "تحديد الإجراء المناسب")
        }
        
        TaskPlan(
            goal = userCommand,
            steps = steps,
            currentStepIndex = 0
        )
    }
    
    suspend fun decideNextAction(
        currentScreen: ScreenContext,
        taskPlan: TaskPlan
    ): ActionDecision = withContext(Dispatchers.Default) {
        
        if (taskPlan.isCompleted()) {
            return@withContext ActionDecision.TaskCompleted
        }
        
        val currentStep = taskPlan.getCurrentStep()
        
        return@withContext when {
            currentStep.contains("البحث") -> {
                val searchElement = currentScreen.findElementByType("search")
                if (searchElement != null) {
                    ActionDecision.Click(searchElement.bounds)
                } else {
                    ActionDecision.Scroll("down")
                }
            }
            currentStep.contains("النقر") -> {
                val targetElement = currentScreen.findElementByText(taskPlan.goal)
                if (targetElement != null) {
                    ActionDecision.Click(targetElement.bounds)
                } else {
                    ActionDecision.Error("لم يتم العثور على العنصر المطلوب")
                }
            }
            currentStep.contains("كتابة") -> {
                val inputElement = currentScreen.findElementByType("input")
                if (inputElement != null) {
                    ActionDecision.Type(inputElement.bounds, "نص الرسالة")
                } else {
                    ActionDecision.Error("لم يتم العثور على حقل الإدخال")
                }
            }
            else -> ActionDecision.Wait(1000)
        }
    }
    
    suspend fun learnFromError(error: String, context: ScreenContext) {
        // TODO: تطبيق آلية التعلم الذاتي
    }
}

@Serializable
data class TaskPlan(
    val goal: String,
    val steps: List<String>,
    var currentStepIndex: Int = 0
) {
    fun getCurrentStep(): String = steps[currentStepIndex]
    
    fun nextStep() {
        if (currentStepIndex < steps.size - 1) {
            currentStepIndex++
        }
    }
    
    fun isCompleted(): Boolean = currentStepIndex >= steps.size
}

@Serializable
sealed class ActionDecision {
    @Serializable
    object TaskCompleted : ActionDecision()
    @Serializable
    data class Click(val bounds: SerializableRect) : ActionDecision() // <-- تم التغيير هنا
    @Serializable
    data class Type(val bounds: SerializableRect, val text: String) : ActionDecision() // <-- تم التغيير هنا
    @Serializable
    data class Scroll(val direction: String) : ActionDecision()
    @Serializable
    data class Wait(val milliseconds: Long) : ActionDecision()
    @Serializable
    data class Error(val message: String) : ActionDecision()
}
