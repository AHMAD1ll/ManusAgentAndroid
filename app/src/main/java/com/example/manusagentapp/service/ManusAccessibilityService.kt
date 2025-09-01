// المسار: app/src/main/java/com/example/manusagentapp/ManusAccessibilityService.kt
// (استبدل محتوى الملف بالكامل)

package com.example.manusagentapp

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File

class ManusAccessibilityService : AccessibilityService() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    private var ortEnv: OrtEnvironment? = null
    private var ortSession: OrtSession? = null

    companion object {
        const val ACTION_SERVICE_STATE_CHANGED = "com.example.manusagentapp.SERVICE_STATE_CHANGED"
        const val EXTRA_STATE = "EXTRA_STATE"
        const val STATE_CONNECTED = "CONNECTED"
        const val STATE_DISCONNECTED = "DISCONNECTED"
        const val STATE_MODEL_LOAD_FAIL = "MODEL_LOAD_FAIL"
        const val STATE_MODEL_LOAD_SUCCESS = "MODEL_LOAD_SUCCESS"
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        broadcastState(STATE_CONNECTED)
        Toast.makeText(this, "Manus Agent Service: CONNECTED", Toast.LENGTH_SHORT).show()
        initializeOrt()
    }

    private fun initializeOrt() {
        scope.launch {
            try {
                ortEnv = OrtEnvironment.getEnvironment()

                // *** الإصلاح الرئيسي هنا: الإشارة إلى ملف .onnx وليس .data ***
                val modelPath = File(filesDir, "phi3.onnx").absolutePath
                val options = OrtSession.SessionOptions()
                ortSession = ortEnv?.createSession(modelPath, options)

                // إذا وصلنا إلى هنا، تم تحميل النموذج بنجاح
                broadcastState(STATE_MODEL_LOAD_SUCCESS, "تم تحميل نموذج الذكاء الاصطناعي بنجاح!")

            } catch (e: Exception) {
                val errorMessage = "فشل تحميل النموذج: ${e.message}"
                Log.e("ManusService", errorMessage, e)
                broadcastState(STATE_MODEL_LOAD_FAIL, errorMessage)
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // سنضيف المنطق هنا لاحقًا
    }

    override fun onInterrupt() {
        // لا يستخدم حاليا
    }

    override fun onUnbind(intent: Intent?): Boolean {
        broadcastState(STATE_DISCONNECTED)
        Toast.makeText(this, "Manus Agent Service: DISCONNECTED", Toast.LENGTH_SHORT).show()
        ortSession?.close()
        // ortEnv?.close() // لا تغلق البيئة العامة إلا عند إنهاء التطبيق تمامًا
        job.cancel()
        return super.onUnbind(intent)
    }

    private fun broadcastState(state: String, message: String? = null) {
        val intent = Intent(ACTION_SERVICE_STATE_CHANGED).apply {
            putExtra(EXTRA_STATE, state)
            message?.let { putExtra("EXTRA_MESSAGE", it) }
        }
        // استخدام sendBroadcast لضمان وصول الرسالة
        sendBroadcast(intent)
    }
}
