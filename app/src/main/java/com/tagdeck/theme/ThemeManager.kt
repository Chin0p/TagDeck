package com.tagdeck.theme

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}

object ThemeManager {
    private const val PREFS_NAME = "audio_tag_editor_theme_prefs"
    private const val KEY_THEME_MODE = "theme_mode"
    private const val KEY_DYNAMIC_COLOR = "dynamic_color"

    @Volatile
    private var prefs: SharedPreferences? = null

    private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _useDynamicColor = MutableStateFlow(false)
    val useDynamicColor: StateFlow<Boolean> = _useDynamicColor.asStateFlow()

    fun init(context: Context) {
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            val sharedPrefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs = sharedPrefs
            
            val modeStr = sharedPrefs.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name
            _themeMode.value = try {
                ThemeMode.valueOf(modeStr)
            } catch (e: Exception) {
                ThemeMode.SYSTEM
            }

            _useDynamicColor.value = sharedPrefs.getBoolean(KEY_DYNAMIC_COLOR, false)
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        CoroutineScope(Dispatchers.IO).launch {
            prefs?.edit()?.putString(KEY_THEME_MODE, mode.name)?.apply()
        }
    }

    fun setUseDynamicColor(use: Boolean) {
        _useDynamicColor.value = use
        CoroutineScope(Dispatchers.IO).launch {
            prefs?.edit()?.putBoolean(KEY_DYNAMIC_COLOR, use)?.apply()
        }
    }
}
