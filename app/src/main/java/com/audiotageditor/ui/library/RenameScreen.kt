package com.audiotageditor.ui.library

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.audiotageditor.data.AudioMetadata
import com.audiotageditor.data.SettingsManager

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
            "[Track] - [Title]",
            "[Artist] - [Album] - [Title]",
            "[Track] [Artist] - [Title]"
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
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
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

                                OutlinedTextField(
                                    value = renameTemplateInput,
                                    onValueChange = { renameTemplateInput = it },
                                    label = { Text("Template Pattern") },
                                    placeholder = { Text("[Artist] - [Title]") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                )

                                Text(
                                    text = "Quick Presets:",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold
                                )

                                // Presets horizontal scroll row
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    presets.forEach { preset ->
                                        SuggestionChip(
                                            onClick = { renameTemplateInput = preset },
                                            label = { Text(preset, style = MaterialTheme.typography.bodySmall) },
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                    }
                                }

                                @OptIn(ExperimentalLayoutApi::class)
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    listOf("[Artist]", "[Title]", "[Album]", "[Track]", "[Year]")
                                        .forEach { placeholder ->
                                            AssistChip(
                                                onClick = { renameTemplateInput = renameTemplateInput.plus(placeholder) },
                                                label = { Text(placeholder, style = MaterialTheme.typography.bodySmall) },
                                                shape = RoundedCornerShape(6.dp)
                                            )
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
                            var name = renameTemplateInput
                                .replace("[Artist]", file.artist.ifBlank { "Unknown Artist" }, ignoreCase = true)
                                .replace("{Artist}", file.artist.ifBlank { "Unknown Artist" }, ignoreCase = true)
                                .replace("[Title]", file.title.ifBlank { file.fileName.substringBeforeLast('.') }, ignoreCase = true)
                                .replace("{Title}", file.title.ifBlank { file.fileName.substringBeforeLast('.') }, ignoreCase = true)
                                .replace("[Album]", file.album.ifBlank { "Unknown Album" }, ignoreCase = true)
                                .replace("{Album}", file.album.ifBlank { "Unknown Album" }, ignoreCase = true)
                                .replace("[Track]", file.track.ifBlank { "01" }, ignoreCase = true)
                                .replace("{Track}", file.track.ifBlank { "01" }, ignoreCase = true)
                                .replace("[Year]", file.year.ifBlank { "2026" }, ignoreCase = true)
                                .replace("{Year}", file.year.ifBlank { "2026" }, ignoreCase = true)
                            name = name.replace(Regex("[\\\\/:*?\"<>|]"), "_")
                            val ext = file.fileName.substringAfterLast('.', "mp3")
                            "$name.$ext"
                        }

                        val isRtl = file.fileName.any { it.code in 0x0600..0x06FF || it.code in 0x0590..0x05FF }
                        CompositionLocalProvider(
                            androidx.compose.ui.platform.LocalLayoutDirection provides if (isRtl) androidx.compose.ui.unit.LayoutDirection.Rtl else androidx.compose.ui.unit.LayoutDirection.Ltr
                        ) {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
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
                                            text = if (isRtl) "$fileNewName ←" else "→ $fileNewName",
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
                }
            }
        }
    }
}
