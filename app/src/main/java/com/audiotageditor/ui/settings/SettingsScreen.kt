package com.audiotageditor.ui.settings

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.audiotageditor.data.SettingsManager
import com.audiotageditor.theme.ThemeManager
import com.audiotageditor.theme.ThemeMode
import com.audiotageditor.ui.library.LibraryScreenViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: LibraryScreenViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // Collect settings states
    val tagToFilename by SettingsManager.tagToFilenameTemplate.collectAsState()
    val currentThemeMode by ThemeManager.themeMode.collectAsState()
    val useDynamicColorSetting by ThemeManager.useDynamicColor.collectAsState()

    val selectedUris by viewModel.selectedUris.collectAsState()
    val selectedCount = selectedUris.size

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Theme Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Appearance & Theme",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "Choose your preferred application visual style and color accents.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // Theme choices
                    ThemeMode.values().forEach { mode ->
                        val isSelected = currentThemeMode == mode
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { ThemeManager.setThemeMode(mode) }
                            )
                            Icon(
                                imageVector = when(mode) {
                                    ThemeMode.SYSTEM -> Icons.Default.Settings
                                    ThemeMode.LIGHT -> Icons.Default.LightMode
                                    ThemeMode.DARK -> Icons.Default.DarkMode
                                },
                                contentDescription = null,
                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = when(mode) {
                                    ThemeMode.SYSTEM -> "System Default"
                                    ThemeMode.LIGHT -> "Light Theme"
                                    ThemeMode.DARK -> "Dark Theme"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }

                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Dynamic Colors",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Use Material You dynamic color system based on wallpaper colors",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = useDynamicColorSetting,
                                onCheckedChange = { ThemeManager.setUseDynamicColor(it) }
                            )
                        }
                    }
                }
            }

            // Tag-to-Filename Renaming Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DriveFileRenameOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Tag-to-Filename Renaming",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "Configure pattern template to rename audio files dynamically based on their tags metadata.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    OutlinedTextField(
                        value = tagToFilename,
                        onValueChange = { SettingsManager.setTagToFilenameTemplate(it) },
                        label = { Text("Rename Pattern Template") },
                        placeholder = { Text("[Artist] - [Title]") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Placeholders: [Artist], [Title], [Album], [Track], [Year]",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Preset Chips
                    Text(
                        text = "Quick Presets:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val presets = listOf("[Artist] - [Title]", "[Title]", "[Track] - [Title]")
                        presets.forEach { preset ->
                            SuggestionChip(
                                onClick = { SettingsManager.setTagToFilenameTemplate(preset) },
                                label = { Text(preset) },
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Dynamic Preview
                    val previewName = remember(tagToFilename) {
                        var name = tagToFilename
                            .replace("[Artist]", "Queen", ignoreCase = true)
                            .replace("{Artist}", "Queen", ignoreCase = true)
                            .replace("[Title]", "Bohemian Rhapsody", ignoreCase = true)
                            .replace("{Title}", "Bohemian Rhapsody", ignoreCase = true)
                            .replace("[Album]", "A Night at the Opera", ignoreCase = true)
                            .replace("{Album}", "A Night at the Opera", ignoreCase = true)
                            .replace("[Track]", "05", ignoreCase = true)
                            .replace("{Track}", "05", ignoreCase = true)
                            .replace("[Year]", "1975", ignoreCase = true)
                            .replace("{Year}", "1975", ignoreCase = true)
                        name = name.replace(Regex("[\\\\/:*?\"<>|]"), "_")
                        "$name.mp3"
                    }

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Preview:",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Mock Metadata: Title=\"Bohemian Rhapsody\", Artist=\"Queen\", Track=\"05\", Year=\"1975\"",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Result: $previewName",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            viewModel.renameSelectedFiles(context, tagToFilename) {
                                Toast.makeText(context, "Successfully renamed selected files!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        enabled = selectedCount > 0,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.DriveFileRenameOutline, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (selectedCount > 0) "Rename $selectedCount Selected File(s)" else "Rename Selected (No files selected)"
                        )
                    }
                }
            }
        }
    }
}
