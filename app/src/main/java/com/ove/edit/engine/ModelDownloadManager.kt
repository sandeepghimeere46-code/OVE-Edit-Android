package com.ove.edit.engine

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

class ModelDownloadManager(private val context: Context) {

    val modelUrls = mapOf(
        "llm_model_600mb_int4.ort" to "https://huggingface.co/Qwen/Qwen1.5-0.5B-Chat-ONNX/resolve/main/model_q4.ort",
        "whisper_small.ort" to "https://huggingface.co/Xenova/whisper-small/resolve/main/onnx/model_quantized.onnx",
        "selfie_segmenter.tflite" to "https://storage.googleapis.com/mediapipe-models/image_segmenter/selfie_segmenter/float16/latest/selfie_segmenter.tflite",
        "efficientdet_lite0.tflite" to "https://storage.googleapis.com/mediapipe-models/object_detector/efficientdet_lite0/int8/latest/efficientdet_lite0.tflite"
    )

    fun isModelDownloaded(fileName: String): Boolean {
        val file = File(context.filesDir, fileName)
        return file.exists() && file.length() > 1000000 // Basic size check > 1MB
    }

    fun deleteModel(fileName: String): Boolean {
        val file = File(context.filesDir, fileName)
        if (file.exists()) {
            return file.delete()
        }
        return false
    }

    fun downloadModel(fileName: String, urlString: String): Flow<Float> = flow {
        val file = File(context.filesDir, fileName)
        if (file.exists() && file.length() > 1000000) {
            emit(1f)
            return@flow
        }

        try {
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 15000
            connection.readTimeout = 60000
            
            // Handle redirects (HuggingFace uses 302 redirects)
            var redirect = false
            var status = connection.responseCode
            if (status != HttpURLConnection.HTTP_OK) {
                if (status == HttpURLConnection.HTTP_MOVED_TEMP
                    || status == HttpURLConnection.HTTP_MOVED_PERM
                    || status == HttpURLConnection.HTTP_SEE_OTHER) {
                    redirect = true
                }
            }
            
            val finalConnection = if (redirect) {
                val newUrl = connection.getHeaderField("Location")
                val redirectConn = URL(newUrl).openConnection() as HttpURLConnection
                redirectConn.connectTimeout = 15000
                redirectConn.readTimeout = 60000
                redirectConn
            } else {
                connection
            }

            finalConnection.connect()
            val fileLength = finalConnection.contentLength
            val input = finalConnection.inputStream
            val output = FileOutputStream(file)

            val data = ByteArray(8192)
            var totalBytesRead: Long = 0
            var bytesRead: Int
            var lastEmitTime = System.currentTimeMillis()

            while (input.read(data).also { bytesRead = it } != -1) {
                totalBytesRead += bytesRead
                output.write(data, 0, bytesRead)
                
                val currentTime = System.currentTimeMillis()
                // Throttle UI updates to every 100ms
                if (currentTime - lastEmitTime > 100) {
                    val progress = if (fileLength > 0) totalBytesRead.toFloat() / fileLength.toFloat() else 0f
                    emit(progress)
                    lastEmitTime = currentTime
                }
            }

            output.flush()
            output.close()
            input.close()
            emit(1f)
            Log.d("ModelDownloadManager", "Successfully downloaded $fileName")
        } catch (e: Exception) {
            Log.e("ModelDownloadManager", "Failed to download $fileName", e)
            file.delete() // Clean up corrupted file
            emit(-1f) // Error signal
        }
    }.flowOn(Dispatchers.IO)

    fun downloadAllModels(): Flow<Float> = flow {
        val totalFiles = modelUrls.size
        var completedFiles = 0

        for ((fileName, urlString) in modelUrls) {
            downloadModel(fileName, urlString).collect { currentFileProgress ->
                if (currentFileProgress >= 0f) {
                    val overallProgress = (completedFiles.toFloat() + currentFileProgress) / totalFiles.toFloat()
                    emit(overallProgress)
                }
            }
            completedFiles++
            emit(completedFiles.toFloat() / totalFiles.toFloat())
        }
    }.flowOn(Dispatchers.IO)
}
