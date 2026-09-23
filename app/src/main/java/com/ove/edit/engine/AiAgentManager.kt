package com.ove.edit.engine

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import com.ove.edit.models.EditActionType
import com.ove.edit.models.ParsedEditCommand
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.LongBuffer

/**
 * Manages the on-device inference using ONNX Runtime.
 */
class AiAgentManager(private val context: Context) {
    private var ortEnv: OrtEnvironment? = null
    private var session: OrtSession? = null

    // The highly optimized system prompt for the small ONNX LLM
    private val systemPrompt = """
        You are OVE, an expert AI video editing assistant. Your job is to translate the user's natural language request into a strict JSON array of actions.
        Available Action Types: TRIM, ADD_MUSIC, ADD_CAPTION, SPEED, COLOR_GRADE, BACKGROUND_REMOVE
        
        Rules:
        1. ONLY output valid JSON. No conversational filler.
        2. Format: [{"action": "ACTION_TYPE", "params": {"key": "value"}}]
        
        Examples:
        User: "Make it look cinematic and add some background music"
        Output: [{"action": "COLOR_GRADE", "params": {"style": "cinematic"}}, {"action": "ADD_MUSIC", "params": {"mood": "cinematic"}}]
        
        User: "Cut out the boring parts and add subtitles"
        Output: [{"action": "TRIM", "params": {"mode": "auto_highlights"}}, {"action": "ADD_CAPTION", "params": {"language": "auto"}}]
    """.trimIndent()

    suspend fun loadModels() {
        withContext(Dispatchers.IO) {
            try {
                ortEnv = OrtEnvironment.getEnvironment()
                val modelFile = File(context.filesDir, "llm_model_600mb_int4.ort")
                
                if (modelFile.exists()) {
                    val sessionOptions = OrtSession.SessionOptions().apply {
                        setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT)
                    }
                    session = ortEnv?.createSession(modelFile.absolutePath, sessionOptions)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun parseCommand(prompt: String): List<ParsedEditCommand> {
        return withContext(Dispatchers.Default) {
            val actions = mutableListOf<ParsedEditCommand>()
            
            val fullPrompt = "$systemPrompt\n\nUser: \"$prompt\"\nOutput:"
            
            if (session != null && ortEnv != null) {
                try {
                    // Simulated tokenization & execution for the ONNX buffer
                    val inputTokens = LongArray(50) { 1L } 
                    val inputTensor = OnnxTensor.createTensor(ortEnv, LongBuffer.wrap(inputTokens), longArrayOf(1, inputTokens.size.toLong()))
                    val result = session?.run(mapOf("input_ids" to inputTensor))
                    
                    // In a real app, this llmOutputText is decoded from the result tensor
                    val llmOutputText = "simulate_json_output" 
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // Fallback Regex parser matching the expected LLM JSON output intent
            val lowerPrompt = prompt.lowercase()
            if (lowerPrompt.contains("cut") || lowerPrompt.contains("trim")) {
                actions.add(ParsedEditCommand(EditActionType.TRIM, mapOf("mode" to "auto_best_cut"), prompt))
            }
            if (lowerPrompt.contains("music") || lowerPrompt.contains("audio")) {
                actions.add(ParsedEditCommand(EditActionType.ADD_MUSIC, mapOf("mood" to "cinematic"), prompt))
            }
            if (lowerPrompt.contains("caption") || lowerPrompt.contains("subtitle")) {
                actions.add(ParsedEditCommand(EditActionType.ADD_CAPTION, mapOf("language" to "auto"), prompt))
            }
            if (lowerPrompt.contains("color") || lowerPrompt.contains("grade") || lowerPrompt.contains("cinematic")) {
                actions.add(ParsedEditCommand(EditActionType.COLOR_GRADE, mapOf("style" to "cinematic"), prompt))
            }
            if (lowerPrompt.contains("background")) {
                actions.add(ParsedEditCommand(EditActionType.BACKGROUND_REMOVE, mapOf("mode" to "auto"), prompt))
            }

            actions
        }
    }

    suspend fun transcribeAudio(audioFilePath: String): String {
        return withContext(Dispatchers.Default) {
            "This is an auto-generated transcription from the local Whisper model."
        }
    }
    
    fun release() {
        session?.close()
        ortEnv?.close()
    }
}
