package com.ove.edit.engine

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

class ModelDownloadManager(private val context: Context) {

    // Real, direct CDN links bypassing the need for GitHub hosting (which limits files to 100MB)
    private val modelUrls = mapOf(
        // Small ~600MB LLM (Qwen-0.5B ONNX INT4 from HuggingFace)
        "llm_model_600mb_int4.ort" to "https://huggingface.co/Qwen/Qwen1.5-0.5B-Chat-ONNX/resolve/main/model_q4.ort",
        // Whisper Small ONNX (~250MB)
        "whisper_small.ort" to "https://huggingface.co/Xenova/whisper-small/resolve/main/onnx/model_quantized.onnx",
        // MediaPipe Image Segmenter
        "selfie_segmenter.tflite" to "https://storage.googleapis.com/mediapipe-models/image_segmenter/selfie_segmenter/float16/latest/selfie_segmenter.tflite",
        // MediaPipe Object Detector
        "efficientdet_lite0.tflite" to "https://storage.googleapis.com/mediapipe-models/object_detector/efficientdet_lite0/int8/latest/efficientdet_lite0.tflite"
    )

    fun downloadAllModels(): Flow<Float> = flow {
        val totalFiles = modelUrls.size
        var completedFiles = 0

        for ((fileName, urlString) in modelUrls) {
            val file = File(context.filesDir, fileName)
            
            // Skip download if the file already exists (size check > 1MB)
            if (file.exists() && file.length() > 1000000) {
                completedFiles++
                emit(completedFiles.toFloat() / totalFiles.toFloat())
                continue
            }

            try {
                val url = URL(urlString)
                val connection = url.openConnection() as HttpURLConnection
                connection.connect()

                val fileLength = connection.contentLength
                val input = connection.inputStream
                val output = FileOutputStream(file)

                val data = ByteArray(4096)
                var totalBytesRead: Long = 0
                var bytesRead: Int

                while (input.read(data).also { bytesRead = it } != -1) {
                    totalBytesRead += bytesRead
                    output.write(data, 0, bytesRead)
                    
                    val currentFileProgress = if (fileLength > 0) totalBytesRead.toFloat() / fileLength.toFloat() else 0f
                    val overallProgress = (completedFiles.toFloat() + currentFileProgress) / totalFiles.toFloat()
                    
                    emit(overallProgress)
                }

                output.flush()
                output.close()
                input.close()
                
            } catch (e: Exception) {
                e.printStackTrace()
            }
            
            completedFiles++
            emit(completedFiles.toFloat() / totalFiles.toFloat())
        }
    }.flowOn(Dispatchers.IO)
}
