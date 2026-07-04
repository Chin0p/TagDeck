package com.tagdeck

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.tagdeck.data.StorageHelper
import com.tagdeck.data.TagEngine
import com.tagdeck.theme.AudioTagEditorTheme
import com.tagdeck.theme.ThemeManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    ThemeManager.init(applicationContext)
    com.tagdeck.data.SettingsManager.init(applicationContext)
    
    // Clean orphaned temp files in background on startup
    lifecycleScope.launch(Dispatchers.IO) {
      TagEngine.cleanTempFiles(applicationContext)
    }

    enableEdgeToEdge()
    setContent {
      val themeMode by ThemeManager.themeMode.collectAsState()
      val useDynamicColor by ThemeManager.useDynamicColor.collectAsState()

      AudioTagEditorTheme(
        themeMode = themeMode,
        useDynamicColor = useDynamicColor
      ) { 
        Surface(
          modifier = Modifier.fillMaxSize()
        ) { 
          MainNavigation() 
        } 
      }
    }
  }
}
