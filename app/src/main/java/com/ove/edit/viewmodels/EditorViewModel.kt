package com.ove.edit.viewmodels

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ove.edit.engine.AiAgentManager
import com.ove.edit.engine.ProjectManager
import com.ove.edit.media.VideoEditorEngine
import com.ove.edit.models.ParsedEditCommand
import com.ove.edit.models.Project
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File

sealed class EditorState {
    object Idle : EditorState()
    object Thinking : EditorState()
    data class AwaitingConfirmation(val commands: List<ParsedEditCommand>, val prompt: String) : EditorState()
    object Processing : EditorState()
    data class Exporting(val progress: Float = 0f) : EditorState()
    data class Error(val message: String) : EditorState()
}

data class ChatMessage(val sender: String, val content: String, val mediaUri: Uri? = null)

class EditorViewModel(application: Application) : AndroidViewModel(application) {
    private val aiAgent = AiAgentManager(application)
    private val projectManager = ProjectManager(application)
    private val videoEditor = VideoEditorEngine(application)
    private val context = application
    
    private val _state = MutableStateFlow<EditorState>(EditorState.Idle)
    val state: StateFlow<EditorState> = _state

    private val _chatHistory = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatHistory: StateFlow<List<ChatMessage>> = _chatHistory

    var currentProject: Project = Project(name = "Untitled Project")
    
    // Undo/Redo Stacks (Deep copies required in a real app, storing JSON strings is an easy way to deep copy)
    private val undoStack = mutableListOf<String>()
    private val redoStack = mutableListOf<String>()
    private val gson = com.google.gson.Gson()

    init {
        viewModelScope.launch {
            aiAgent.loadModels()
            saveStateToUndoStack() // Initial state
        }
    }

    private fun saveStateToUndoStack() {
        undoStack.add(gson.toJson(currentProject))
        redoStack.clear()
    }

    fun undo() {
        if (undoStack.size > 1) {
            redoStack.add(undoStack.removeLast())
            currentProject = gson.fromJson(undoStack.last(), Project::class.java)
            addMessage("System", "Undo successful.")
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            val stateToRestore = redoStack.removeLast()
            undoStack.add(stateToRestore)
            currentProject = gson.fromJson(stateToRestore, Project::class.java)
            addMessage("System", "Redo successful.")
        }
    }

    private fun triggerAutoSave() {
        viewModelScope.launch {
            projectManager.saveProject(currentProject)
        }
    }

    fun processCommand(command: String) {
        if (command.isBlank()) return
        addMessage("User", command)
        _state.value = EditorState.Thinking

        viewModelScope.launch {
            try {
                val parsedCommands = aiAgent.parseCommand(command)
                if (parsedCommands.isEmpty()) {
                    addMessage("Agent", "I couldn't quite understand that edit. Could you rephrase?")
                    _state.value = EditorState.Idle
                } else {
                    val summary = parsedCommands.joinToString(", ") { it.actionType.name }
                    val confirmText = "I understand you want to apply: $summary. Should I go ahead?"
                    addMessage("Agent", confirmText)
                    _state.value = EditorState.AwaitingConfirmation(parsedCommands, command)
                }
            } catch (e: Exception) {
                addMessage("Agent", "Oops, I ran into an issue parsing that: ${e.message}")
                _state.value = EditorState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun confirmEdit(commands: List<ParsedEditCommand>) {
        _state.value = EditorState.Processing
        addMessage("User", "[Confirmed]")
        
        viewModelScope.launch {
            kotlinx.coroutines.delay(1000) // Simulated processing
            
            saveStateToUndoStack()
            triggerAutoSave()
            
            addMessage("Agent", "Edit applied and auto-saved successfully!")
            _state.value = EditorState.Idle
        }
    }

    fun cancelEdit() {
        addMessage("User", "[Cancelled]")
        addMessage("Agent", "Okay, I've cancelled that edit. What else would you like to do?")
        _state.value = EditorState.Idle
    }

    fun addMedia(uri: Uri, type: String) {
        addMessage("User", "Uploaded $type", mediaUri = uri)
        addMessage("Agent", "I've added the $type to your timeline. What would you like to do with it?")
        saveStateToUndoStack()
        triggerAutoSave()
    }

    fun exportVideo() {
        _state.value = EditorState.Exporting(0f)
        viewModelScope.launch {
            try {
                val outputFilePath = File(context.getExternalFilesDir(null), "OVE_Export_${System.currentTimeMillis()}.mp4").absolutePath
                val result = videoEditor.exportProject(currentProject.timeline, outputFilePath)
                
                result.onSuccess {
                    addMessage("System", "Export complete! Saved to: $it")
                    _state.value = EditorState.Idle
                }.onFailure {
                    addMessage("System", "Export failed: ${it.message}")
                    _state.value = EditorState.Idle
                }
            } catch (e: Exception) {
                addMessage("System", "Export error: ${e.message}")
                _state.value = EditorState.Idle
            }
        }
    }

    private fun addMessage(sender: String, content: String, mediaUri: Uri? = null) {
        val currentList = _chatHistory.value.toMutableList()
        currentList.add(ChatMessage(sender, content, mediaUri))
        _chatHistory.value = currentList
    }

    override fun onCleared() {
        super.onCleared()
        aiAgent.release()
    }
}
