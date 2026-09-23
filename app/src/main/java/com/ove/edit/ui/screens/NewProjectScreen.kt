package com.ove.edit.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ove.edit.viewmodels.EditorState
import com.ove.edit.viewmodels.EditorViewModel

@Composable
fun PulsingThinkingIndicator() {
    val infiniteTransition = rememberInfiniteTransition()
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(8.dp)) {
        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.primary.copy(alpha = alpha))
        Spacer(modifier = Modifier.width(8.dp))
        Text("Agent is thinking...", color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha))
    }
}

@Composable
fun NewProjectScreen(
    onBack: () -> Unit,
    viewModel: EditorViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val chatHistory by viewModel.chatHistory.collectAsState()
    var inputText by remember { mutableStateOf("") }
    var isRecording by remember { mutableStateOf(false) }

    val videoPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { viewModel.addMedia(it, "video") }
    }

    val audioPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { viewModel.addMedia(it, "audio") }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Title Bar
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(onClick = onBack, modifier = Modifier.semantics { contentDescription = "Navigate back to Home" }) { Text("Back") }
            Spacer(modifier = Modifier.weight(1f))
            Text("AI Editor", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.weight(1f))
            
            IconButton(onClick = { viewModel.undo() }, modifier = Modifier.semantics { contentDescription = "Undo last edit" }) {
                Icon(Icons.Filled.Undo, contentDescription = "Undo")
            }
            IconButton(onClick = { viewModel.redo() }, modifier = Modifier.semantics { contentDescription = "Redo last edit" }) {
                Icon(Icons.Filled.Redo, contentDescription = "Redo")
            }
            Button(
                onClick = { viewModel.exportVideo() },
                modifier = Modifier.semantics { contentDescription = "Export video to gallery" },
                enabled = state == EditorState.Idle
            ) {
                Icon(Icons.Filled.Download, contentDescription = "Export", modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Export")
            }
        }

        // Upload Buttons Area
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(onClick = { videoPickerLauncher.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)) }) { Text("Upload Video") }
            Button(onClick = { audioPickerLauncher.launch("audio/*") }) { Text("Upload Audio") }
        }

        Divider()

        // Chat / Status Area
        LazyColumn(
            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
            reverseLayout = true
        ) {
            items(chatHistory.reversed()) { message ->
                AnimatedVisibility(
                    visible = true,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    modifier = Modifier.padding(vertical = 4.dp).fillMaxWidth()
                ) {
                    val isUser = message.sender == "User"
                    val isSystem = message.sender == "System"
                    
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    when {
                                        isSystem -> MaterialTheme.colorScheme.surfaceVariant
                                        isUser -> MaterialTheme.colorScheme.primaryContainer
                                        else -> MaterialTheme.colorScheme.secondaryContainer
                                    }
                                )
                                .padding(12.dp)
                        ) {
                            Column {
                                Text(
                                    text = message.content, 
                                    color = if (isUser) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                if (message.mediaUri != null && message.content.contains("video")) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    VideoPlayer(uri = message.mediaUri)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Processing State Indicator
        AnimatedVisibility(visible = state is EditorState.Thinking || state is EditorState.Processing || state is EditorState.Exporting) {
            Box(modifier = Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) {
                when (state) {
                    is EditorState.Thinking -> PulsingThinkingIndicator()
                    is EditorState.Processing -> Text("Agent is applying edit...")
                    is EditorState.Exporting -> Text("Exporting Video...")
                    else -> {}
                }
            }
        }

        // Confirmation Actions
        AnimatedVisibility(visible = state is EditorState.AwaitingConfirmation) {
            if (state is EditorState.AwaitingConfirmation) {
                val awaitState = state as EditorState.AwaitingConfirmation
                Row(modifier = Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Button(onClick = { viewModel.confirmEdit(awaitState.commands) }) { Text("Confirm") }
                    Button(onClick = { viewModel.cancelEdit() }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Cancel") }
                }
            }
        }

        // Input Area
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { 
                    isRecording = !isRecording 
                    if (!isRecording) { inputText = " (Transcribed speech...) " }
                },
                enabled = state == EditorState.Idle
            ) {
                Icon(
                    Icons.Filled.Mic, 
                    contentDescription = "Voice Command",
                    tint = if (isRecording) MaterialTheme.colorScheme.error else LocalContentColor.current
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text(if (isRecording) "Listening..." else "Type edit command...") },
                enabled = state == EditorState.Idle && !isRecording,
                shape = RoundedCornerShape(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = {
                    if (inputText.isNotBlank()) {
                        viewModel.processCommand(inputText)
                        inputText = ""
                    }
                },
                enabled = state == EditorState.Idle && inputText.isNotBlank() && !isRecording
            ) {
                Icon(Icons.Filled.Send, contentDescription = "Send", tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
