package com.ove.edit.engine

import android.content.Context
import android.content.SharedPreferences

class SettingsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("ove_settings", Context.MODE_PRIVATE)

    var isDarkMode: Boolean
        get() = prefs.getBoolean("dark_mode", true)
        set(value) = prefs.edit().putBoolean("dark_mode", value).apply()

    var fontScale: Float
        get() = prefs.getFloat("font_scale", 1.0f)
        set(value) = prefs.edit().putFloat("font_scale", value).apply()

    var highContrast: Boolean
        get() = prefs.getBoolean("high_contrast", false)
        set(value) = prefs.edit().putBoolean("high_contrast", value).apply()

    var useSdCard: Boolean
        get() = prefs.getBoolean("use_sd_card", false)
        set(value) = prefs.edit().putBoolean("use_sd_card", value).apply()
}
