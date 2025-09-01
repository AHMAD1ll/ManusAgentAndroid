// المسار: app/src/main/java/com/example/manusagentapp/ManusAccessibilityService.kt
// (استبدل محتوى الملف بالكامل)

package com.example.manusagentapp

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityNodeInfo
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

    // *** جديد: لتخزين المهمة الحالية ***
    private var currentTask: String? = null

    companion object {
        // للأحداث
        const val ACTION_SERVICE_STATE_CHANGED = "com.example.manusagentapp.SERVICE_STATE_CHANGED"
        const val EXTRA_STATE = "EXTRA_STATE"
        const val STATE_CONNECTED = "CONNECTED"
        const val STATE_DISCONNECTED = "DISCONNECTED"
        const val STATE_MODEL_LOAD_FAIL = "MODEL_LOAD_FAIL"
        const val STATE_MODEL_LOAD_SUCCESS = "MODEL_LOAD_SUCCESS"

        // *** جديد: للأوامر ***
        const val ACTION_COMMAND = "com.example.manusagentapp.COMMAND"
        const val EXTRA_COMMAND_TEXT = "EXTRA_COMMAND_TEXT"
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        broadcastState(STATE_CONNECTED)
        Toast.makeText(this, "Manus Agent Service: CONNECTED", Toast.LENGTH_SHORT).show()
        initializeOrt()
    }

    // *** جديد: لمعالجة الأوامر من الواجهة ***
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_COMMAND) {
            currentTask = intent.getStringExtra(EXTRA_COMMAND_TEXT)
            Toast.makeText(this, "مهمة جديدة: $currentTask", Toast.LENGTH_SHORT).show()

            // ابدأ التفكير فورًا عن طريق فحص الشاشة الحالية
            val rootNode = rootInActiveWindow
            if (rootNode != null) {
                val screenContent = captureScreenContent(rootNode)
                Log.d("ManusService", "الشاشة الحالية:\n$screenContent")
                // TODO: أرسل screenContent + currentTask إلى النموذج
                rootNode.recycle()
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // سنقوم بتشغيل هذا المنطق عندما يتغير شيء ما على الشاشة
        if (currentTask != null && event?.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
             val rootNode = rootInActiveWindow ?: return
             val screenContent = captureScreenContent(rootNode)
             Log.d("ManusService", "تغيرت الشاشة:\n$screenContent")
             // TODO: أرسل screenContent + currentTask إلى النموذج
             rootNode.recycle()
        }
    }

    // *** جديد: دالة لالتقاط محتوى الشاشة كنص ***
    private fun captureScreenContent(node: AccessibilityNodeInfo?): String {
        if (node == null) return ""
        val builder = StringBuilder()
        traverseNode(node, builder)
        return builder.toString()
    }

    private fun traverseNode(node: AccessibilityNodeInfo, builder: StringBuilder) {
        val text = node.text?.toString()?.trim()
        val contentDesc = node.contentDescription?.toString()?.trim()

        if (!text.isNullOrEmpty()) {
            builder.append(text).append("\n")
        } else if (!contentDesc.isNullOrEmpty()) {
            builder.append(contentDesc).append("\n")
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                traverseNode(child, builder)
                child.recycle()
            }
        }
    }


    private fun initializeOrt() {
        scope.launch {
            try {
                ortEnv = OrtEnvironment.getEnvironment()
                val modelPath = File(filesDir, "phi3.onnx").absolutePath
                val options = OrtSession.SessionOptions()
                ortSession = ortEnv?.createSession(modelPath, options)
                broadcastState(STATE_MODEL_LOAD_SUCCESS, "تم تحميل نموذج الذكاء الاصطناعي بنجاح!")
            } catch (e: Exception) {
                val errorMessage = "فشل تحميل النموذج: ${e.message}"
                Log.e("ManusService", errorMessage, e)
                broadcastState(STATE_MODEL_LOAD_FAIL, errorMessage)
            }
        }
    }

    override fun onInterrupt() { /* لا يستخدم حاليا */ }

    override fun onUnbind(intent: Intent?): Boolean {
        broadcastState(STATE_DISCONNECTED)
        Toast.makeText(this, "Manus Agent Service: DISCONNECTED", Toast.LENGTH_SHORT).show()
        ortSession?.close()
        job.cancel()
        return super.onUnbind(intent)
    }

    private fun broadcastState(state: String, message: String? = null) {
        val intent = Intent(ACTION_SERVICE_STATE_CHANGED).apply {
            putExtra(EXTRA_STATE, state)
            message?.let { putExtra("EXTRA_MESSAGE", it) }
        }
        sendBroadcast(intent)
    }
}
