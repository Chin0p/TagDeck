package com.tagdeck.ui.library

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tagdeck.data.AudioMetadata
import com.tagdeck.data.SettingsManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RenameScreen(
    selectedUris: List<String>,
    onNavigateBack: () -> Unit,
    viewModel: LibraryScreenViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val allFiles by viewModel.filteredFiles.collectAsState()
    val selectedFiles = remember(selectedUris, allFiles) {
        allFiles.filter { selectedUris.contains(it.uriString) }
    }

    val savedTemplate by SettingsManager.tagToFilenameTemplate.collectAsState()
    var renameTemplateInput by remember { mutableStateOf(savedTemplate) }

    LaunchedEffect(savedTemplate) {
        renameTemplateInput = savedTemplate
    }

    val presets = remember {
        listOf(
            "[Artist] - [Title]",
            "[Title]",
            "[Album] - [Artist] - [Title]",
            "[Track] - [Title]",
            "[Track] - [Artist] - [Title]"
        )
    }

    BottomSheetScaffold(
        topBar = {
            TopAppBar(
                title = {
                    val fileWord = if (selectedFiles.size == 1) "File" else "Files"
                    Text(
                        text = "Rename · ${selectedFiles.size} $fileWord",
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        sheetContent = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Rename Files",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Button(
                    onClick = {
                        SettingsManager.setTagToFilenameTemplate(renameTemplateInput)
                        viewModel.renameSelectedFiles(context, renameTemplateInput) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            Toast.makeText(context, "Successfully renamed files!", Toast.LENGTH_SHORT).show()
                            onNavigateBack()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DriveFileRenameOutline,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    val fileWord = if (selectedFiles.size == 1) "File" else "Files"
                    Text(
                        text = "Rename ${selectedFiles.size} $fileWord",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        },
        sheetPeekHeight = 120.dp,
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. Template Input card
                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "Rename Pattern Template",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                androidx.compose.material3.OutlinedTextField(
                                    value = renameTemplateInput,
                                    onValueChange = { renameTemplateInput = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    label = { Text("Active Pattern") },
                                    shape = RoundedCornerShape(12.dp)
                                )

                                androidx.compose.foundation.lazy.LazyRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(presets) { preset ->
                                        val isSelected = renameTemplateInput == preset
                                        val bgColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh
                                        val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                        val borderModifier = if (isSelected) Modifier else Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                                        
                                        Box(
                                            modifier = Modifier
                                                .then(borderModifier)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(bgColor)
                                                .clickable { renameTemplateInput = preset }
                                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Text(
                                                text = preset,
                                                style = MaterialTheme.typography.labelMedium,
                                                color = contentColor
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 2. Selection Title
                    item {
                        Text(
                            text = "Selected Files Preview",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // 3. File list
                    items(selectedFiles, key = { it.uriString }) { file ->
                        val fileNewName = remember(renameTemplateInput, file) {
                            computeNewFileName(file, renameTemplateInput)
                        }
                        RenamePreviewCard(file = file, newFileName = fileNewName)
                    }
                }
            }
        }
    }
}

private fun computeNewFileName(file: AudioMetadata, template: String): String {
    var name = template
        .replace("[Artist]", file.artist.ifBlank { "Unknown Artist" }, ignoreCase = true)
        .replace("[Title]", file.title.ifBlank { file.fileName.substringBeforeLast('.') }, ignoreCase = true)
        .replace("[Album]", file.album.ifBlank { "Unknown Album" }, ignoreCase = true)
        .replace("[Track]", file.track.ifBlank { "01" }, ignoreCase = true)
        .replace("[Year]", file.year.ifBlank { "2026" }, ignoreCase = true)
    name = name.replace(Regex("[\\\\/:*?\"<>|]"), "_")
    val ext = file.fileName.substringAfterLast('.', "mp3")
    return "$name.$ext"
}

@Composable
private fun RenamePreviewCard(file: AudioMetadata, newFileName: String) {
    val isRtl = file.fileName.any { it.code in 0x0600..0x06FF || it.code in 0x0590..0x05FF }
    CompositionLocalProvider(
        androidx.compose.ui.platform.LocalLayoutDirection provides if (isRtl) androidx.compose.ui.unit.LayoutDirection.Rtl else androidx.compose.ui.unit.LayoutDirection.Ltr
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = if (isRtl) Arrangement.End else Arrangement.Start
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = file.fileName,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            textDirection = TextDirection.Content
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = if (isRtl) androidx.compose.ui.text.style.TextAlign.End else androidx.compose.ui.text.style.TextAlign.Start
                    )
                    Text(
                        text = if (isRtl) "$newFileName ←" else "→ $newFileName",
                        style = MaterialTheme.typography.bodySmall.copy(
                            textDirection = TextDirection.Content,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = if (isRtl) androidx.compose.ui.text.style.TextAlign.End else androidx.compose.ui.text.style.TextAlign.Start
                    )
                }
            }
        }
    }
}
