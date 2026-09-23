package com.ove.edit.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ove.edit.engine.ModelDownloadManager
import com.ove.edit.engine.SettingsManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ModelState(val fileName: String, val isDownloaded: Boolean, val downloadProgress: Float = 0f)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val settingsManager = SettingsManager(application)
    private val downloadManager = ModelDownloadManager(application)

    private val _isDarkMode = MutableStateFlow(settingsManager.isDarkMode)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode

    private val _highContrast = MutableStateFlow(settingsManager.highContrast)
    val highContrast: StateFlow<Boolean> = _highContrast

    private val _modelStates = MutableStateFlow<List<ModelState>>(emptyList())
    val modelStates: StateFlow<List<ModelState>> = _modelStates

    init {
        refreshModelStates()
    }

    fun toggleDarkMode(enabled: Boolean) {
        settingsManager.isDarkMode = enabled
        _isDarkMode.value = enabled
    }

    fun toggleHighContrast(enabled: Boolean) {
        settingsManager.highContrast = enabled
        _highContrast.value = enabled
    }

    private fun refreshModelStates() {
        val states = downloadManager.modelUrls.keys.map { fileName ->
            ModelState(fileName, downloadManager.isModelDownloaded(fileName))
        }
        _modelStates.value = states
    }

    fun deleteModel(fileName: String) {
        downloadManager.deleteModel(fileName)
        refreshModelStates()
    }

    fun downloadModel(fileName: String) {
        val url = downloadManager.modelUrls[fileName] ?: return
        
        viewModelScope.launch {
            downloadManager.downloadModel(fileName, url).collect { progress ->
                if (progress >= 1f || progress < 0f) {
                    refreshModelStates()
                } else {
                    val currentStates = _modelStates.value.toMutableList()
                    val index = currentStates.indexOfFirst { it.fileName == fileName }
                    if (index != -1) {
                        currentStates[index] = currentStates[index].copy(downloadProgress = progress)
                        _modelStates.value = currentStates
                    }
                }
            }
        }
    }
}
