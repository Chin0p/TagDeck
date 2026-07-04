package com.tagdeck.ui.settings

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import com.tagdeck.data.SettingsManager
import com.tagdeck.theme.ThemeManager
import com.tagdeck.theme.ThemeMode

@Stable
class SettingsScreenViewModel : ViewModel() {
    val tagToFilenameTemplate = SettingsManager.tagToFilenameTemplate
    val themeMode = ThemeManager.themeMode
    val useDynamicColor = ThemeManager.useDynamicColor

    fun setTagToFilenameTemplate(template: String) {
        SettingsManager.setTagToFilenameTemplate(template)
    }

    fun setThemeMode(mode: ThemeMode) {
        ThemeManager.setThemeMode(mode)
    }

    fun setUseDynamicColor(useDynamicColor: Boolean) {
        ThemeManager.setUseDynamicColor(useDynamicColor)
    }
}
