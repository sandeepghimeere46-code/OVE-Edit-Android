package com.ove.edit.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.ove.edit.engine.SettingsManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val settingsManager = SettingsManager(application)

    private val _isDarkMode = MutableStateFlow(settingsManager.isDarkMode)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode

    private val _fontScale = MutableStateFlow(settingsManager.fontScale)
    val fontScale: StateFlow<Float> = _fontScale

    private val _highContrast = MutableStateFlow(settingsManager.highContrast)
    val highContrast: StateFlow<Boolean> = _highContrast

    fun toggleDarkMode(enabled: Boolean) {
        settingsManager.isDarkMode = enabled
        _isDarkMode.value = enabled
    }

    fun setFontScale(scale: Float) {
        settingsManager.fontScale = scale
        _fontScale.value = scale
    }

    fun toggleHighContrast(enabled: Boolean) {
        settingsManager.highContrast = enabled
        _highContrast.value = enabled
    }
}
