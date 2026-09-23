package com.ove.edit.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ove.edit.engine.ModelDownloadManager
import com.ove.edit.viewmodels.HistoryViewModel
import com.ove.edit.viewmodels.SettingsViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun OnboardingScreen(onNavigateToModelDownload: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Welcome to OVE Edit", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Text("We need Storage, Camera, and Microphone permissions to start editing.")
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onNavigateToModelDownload,
            modifier = Modifier.semantics { contentDescription = "Grant permissions and continue to download models" }
        ) {
            Text("Grant Permissions & Continue")
        }
    }
}

@Composable
fun ModelDownloadScreen(onDownloadComplete: () -> Unit) {
    val context = LocalContext.current
    val downloadManager = remember { ModelDownloadManager(context) }
    var progress by remember { mutableStateOf(0f) }
    
    val animatedProgress by animateFloatAsState(
        targetValue = progress, 
        animationSpec = tween(durationMillis = 500, easing = LinearOutSlowInEasing)
    )

    LaunchedEffect(Unit) {
        downloadManager.downloadAllModels().collect { currentProgress ->
            progress = currentProgress
            if (currentProgress >= 1f) {
                kotlinx.coroutines.delay(500)
                onDownloadComplete()
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Downloading AI Models...", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Text("This happens once. No internet needed after this.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(24.dp))
        
        LinearProgressIndicator(
            progress = animatedProgress.coerceIn(0f, 1f), 
            modifier = Modifier.fillMaxWidth().height(8.dp).semantics { contentDescription = "Download progress ${(animatedProgress * 100).toInt()} percent" },
            strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        Text("${(animatedProgress * 100).toInt()}%", style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun HomeScreen(
    onNavigateToSettings: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToNewProject: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("OVE Edit", style = MaterialTheme.typography.headlineLarge)
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onNavigateToNewProject, 
            modifier = Modifier.fillMaxWidth().height(56.dp).semantics { contentDescription = "Start a new AI Chat project" }
        ) {
            Text("New Project (AI Chat)")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onNavigateToHistory, 
            modifier = Modifier.fillMaxWidth().height(56.dp).semantics { contentDescription = "View past projects history" }
        ) {
            Text("History (Past Projects)")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onNavigateToSettings, 
            modifier = Modifier.fillMaxWidth().height(56.dp).semantics { contentDescription = "Open app settings" }
        ) {
            Text("Settings")
        }
    }
}

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val highContrast by viewModel.highContrast.collectAsState()
    val modelStates by viewModel.modelStates.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Button(onClick = onBack, modifier = Modifier.semantics { contentDescription = "Navigate back" }) { Text("Back") }
        Spacer(modifier = Modifier.height(16.dp))
        Text("Settings", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(24.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Dark Mode")
            Switch(checked = isDarkMode, onCheckedChange = { viewModel.toggleDarkMode(it) })
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("High Contrast Mode")
            Switch(checked = highContrast, onCheckedChange = { viewModel.toggleHighContrast(it) })
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        Text("Model Manager", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Manage local offline AI models.", style = MaterialTheme.typography.bodySmall)
        Spacer(modifier = Modifier.height(16.dp))
        
        LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
            items(modelStates) { model ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(model.fileName, style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            if (model.isDownloaded) {
                                Text("Status: Downloaded", color = MaterialTheme.colorScheme.primary)
                                Button(onClick = { viewModel.deleteModel(model.fileName) }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                                    Text("Delete")
                                }
                            } else {
                                if (model.downloadProgress > 0f) {
                                    LinearProgressIndicator(progress = model.downloadProgress, modifier = Modifier.weight(1f).padding(end = 8.dp))
                                    Text("${(model.downloadProgress * 100).toInt()}%")
                                } else {
                                    Text("Status: Not Downloaded")
                                    Button(onClick = { viewModel.downloadModel(model.fileName) }) {
                                        Text("Download")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryScreen(
    onBack: () -> Unit,
    viewModel: HistoryViewModel = viewModel()
) {
    val projects by viewModel.projects.collectAsState()
    val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Button(onClick = onBack, modifier = Modifier.semantics { contentDescription = "Navigate back" }) { Text("Back") }
        Spacer(modifier = Modifier.height(16.dp))
        Text("Project History", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        if (projects.isEmpty()) {
            Text("No past projects yet.")
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(projects) { project ->
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(project.name, style = MaterialTheme.typography.titleMedium)
                            Text("Saved: ${dateFormat.format(Date(project.lastSavedAt))}", style = MaterialTheme.typography.bodySmall)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                TextButton(onClick = { viewModel.deleteProject(project.id) }) { Text("Delete") }
                            }
                        }
                    }
                }
            }
        }
    }
}
