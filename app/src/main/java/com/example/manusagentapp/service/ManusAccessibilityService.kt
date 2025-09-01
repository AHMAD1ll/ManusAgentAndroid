package com.example.manusagentapp

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import java.io.File

class ManusAccessibilityService : AccessibilityService() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    // متغيرات جديدة للاحتفاظ ببيئة ونموذج الذكاء الاصطناعي
    private var ortEnv: OrtEnvironment? = null
    private var ortSession: OrtSession? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d("ManusService", "Service Connected")
        Toast.makeText(this, "Manus Agent Service: CONNECTED", Toast.LENGTH_SHORT).show()

        // الآن، لنبدأ في تهيئة النموذج في الخلفية
        scope.launch {
            initializeOrtSession()
        }
    }

    private fun initializeOrtSession() {
        try {
            // الخطوة 1: إنشاء بيئة ONNX Runtime
            ortEnv = OrtEnvironment.getEnvironment()
            showToast("ORT Environment created successfully.")

            // الخطوة 2: تحديد مسارات الملفات داخل مجلد التطبيق الخاص
            val modelsDir = filesDir
            val modelFile = File(modelsDir, "phi3.onnx")
            val tokenizerFile = File(modelsDir, "tokenizer.json")

            // التحقق من وجود الملفات قبل المتابعة
            if (!modelFile.exists() || !tokenizerFile.exists()) {
                showToast("Error: Model or tokenizer file not found!")
                return
            }

            // الخطوة 3: إنشاء خيارات الجلسة
            val sessionOptions = OrtSession.SessionOptions()

            // الخطوة 4: تحميل النموذج وإنشاء الجلسة
            ortSession = ortEnv?.createSession(modelFile.absolutePath, sessionOptions)

            // إذا وصلنا إلى هنا، فالتحميل نجح
            showToast("AI Model loaded successfully!")

        } catch (e: Exception) {
            // في حالة حدوث أي خطأ، اعرضه
            Log.e("ManusService", "Error initializing ORT session", e)
            showToast("Error loading AI Model: ${e.message}")
        }
    }

    // وظيفة مساعدة لعرض رسائل Toast من أي مكان في الكود
    private fun showToast(message: String) {
        CoroutineScope(Dispatchers.Main).launch {
            Toast.makeText(this@ManusAccessibilityService, message, Toast.LENGTH_LONG).show()
        }
    }


    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // سنتعامل مع الأحداث هنا لاحقًا
    }

    override fun onInterrupt() {
        Log.d("ManusService", "Service Interrupted")
    }

    override fun onUnbind(intent: Intent?): Boolean {
        Log.d("ManusService", "Service Disconnected")
        Toast.makeText(this, "Manus Agent Service: DISCONNECTED", Toast.LENGTH_SHORT).show()

        // تنظيف الموارد عند إيقاف الخدمة
        ortSession?.close()
        ortEnv?.close()
        job.cancel() // إلغاء جميع العمليات الجارية في الخلفية
        return super.onUnbind(intent)
    }
}
