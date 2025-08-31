package com.example.manusagentapp.core

import android.content.Context
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class ModelDownloader(private val context: Context) {

    // تعريف الروابط الثابتة للملفات
    companion object {
        private const val MODEL_URL = "https://huggingface.co/microsoft/Phi-3-mini-4k-instruct-onnx/resolve/main/cpu_and_mobile/cpu-int4-rtn-block-32-acc-level-4/phi3-mini-4k-instruct-cpu-int4-rtn-block-32-acc-level-4.onnx"
        private const val MODEL_DATA_URL = "https://huggingface.co/microsoft/Phi-3-mini-4k-instruct-onnx/resolve/main/cpu_and_mobile/cpu-int4-rtn-block-32-acc-level-4/phi3-mini-4k-instruct-cpu-int4-rtn-block-32-acc-level-4.onnx.data"
        private const val TOKENIZER_URL = "https://huggingface.co/microsoft/Phi-3-mini-4k-instruct-onnx/resolve/main/cpu_and_mobile/cpu-int4-rtn-block-32-acc-level-4/tokenizer.json"
    }

    // الحصول على المسار الآمن لحفظ الملفات داخل التطبيق
    private val modelDir = File(context.filesDir, "models")
    val modelPath = File(modelDir, "model.onnx").absolutePath
    val modelDataPath = File(modelDir, "model.onnx.data").absolutePath
    val tokenizerPath = File(modelDir, "tokenizer.json").absolutePath

    // دالة للتحقق مما إذا كانت جميع الملفات موجودة
    fun areModelsAvailable(): Boolean {
        return File(modelPath).exists() && File(modelDataPath).exists() && File(tokenizerPath).exists()
    }

    // دالة لبدء عملية التحميل (تحتاج إلى تنفيذها داخل Coroutine)
    suspend fun downloadModels(onProgress: (Float) -> Unit) {
        if (areModelsAvailable()) {
            onProgress(1f) // إذا كانت الملفات موجودة، فالتقدم هو 100%
            return
        }

        // إنشاء مجلد الحفظ إذا لم يكن موجودًا
        modelDir.mkdirs()

        // إعداد عميل Ktor للشبكة
        val client = HttpClient(Android) {
            install(HttpTimeout) {
                requestTimeoutMillis = HttpTimeout.INFINITE_TIMEOUT_MS
            }
        }

        // تحميل الملفات الثلاثة
        downloadFile(client, MODEL_URL, modelPath) { progress -> onProgress(progress * 0.1f) } // الملف الأول يمثل 10% من التقدم
        downloadFile(client, TOKENIZER_URL, tokenizerPath) { progress -> onProgress(0.1f + progress * 0.05f) } // الملف الثاني يمثل 5%
        downloadFile(client, MODEL_DATA_URL, modelDataPath) { progress -> onProgress(0.15f + progress * 0.85f) } // الملف الكبير يمثل 85%

        client.close()
    }

    // دالة مساعدة لتنزيل ملف واحد مع تتبع التقدم
    private suspend fun downloadFile(client: HttpClient, url: String, filePath: String, onProgress: (Float) -> Unit) {
        val file = File(filePath)
        
        // استخدام استجابة Ktor لتنزيل الملف كـ stream
        client.get(url) {
            onDownload { bytesSentTotal, contentLength ->
                val progress = bytesSentTotal.toFloat() / contentLength.toFloat()
                onProgress(progress)
            }
        }.body<ByteArray>().let { data ->
            withContext(Dispatchers.IO) {
                file.writeBytes(data)
            }
        }
    }
}
