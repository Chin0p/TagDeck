package com.audiotageditor.ui.editor

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.audiotageditor.data.AudioMetadata

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    selectedUris: List<String>,
    onNavigateBack: () -> Unit,
    viewModel: EditorScreenViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val uiState by viewModel.uiState.collectAsState()

    val files = uiState.selectedFiles
    val isBatch = uiState.isBatchEdit
    val isLoading = uiState.isLoading
    val saveSuccess = uiState.saveSuccess
    val mixedFields = uiState.mixedFields
    val mixedFieldsAction = uiState.mixedFieldsAction

    // Form fields
    val title = uiState.title
    val artist = uiState.artist
    val album = uiState.album
    val year = uiState.year
    val genre = uiState.genre
    val track = uiState.track
    val albumArtist = uiState.albumArtist
    val comment = uiState.comment
    val description = uiState.description
    val composer = uiState.composer
    val discNumber = uiState.discNumber
    val removeCover = uiState.removeCover

    // Load files initially
    LaunchedEffect(selectedUris) {
        viewModel.loadFiles(context, selectedUris)
    }

    // Handle save success
    LaunchedEffect(saveSuccess) {
        if (saveSuccess == true) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            Toast.makeText(context, "Tags saved successfully!", Toast.LENGTH_SHORT).show()
            viewModel.resetSuccess()
            onNavigateBack()
        } else if (saveSuccess == false) {
            Toast.makeText(context, "Failed to save tags.", Toast.LENGTH_SHORT).show()
            viewModel.resetSuccess()
        }
    }

    BottomSheetScaffold(
        topBar = {
            TopAppBar(
                title = {
                    val fileWord = if (files.size == 1) "File" else "Files"
                    Text(
                        text = if (isBatch) "Batch Edit · ${files.size} $fileWord" else "Edit Tags",
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
                    text = "Save Changes",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Button(
                    onClick = { viewModel.saveChanges(context) },
                    enabled = !isLoading && files.isNotEmpty(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isBatch) "Save ${files.size} Changes" else "Save Changes",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
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
                .windowInsetsPadding(WindowInsets.systemBars)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (files.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else {
                val showRemoveCoverOption = isBatch || uiState.albumArtBytes != null
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 32.dp, start = 16.dp, end = 16.dp, top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. Remove cover option at the top of metadata fields (if supported / applicable)
                    // In batch mode, we always show it. In single mode, we show it if the file currently has cover art.
                    if (showRemoveCoverOption) {
                        item {
                            OutlinedCard(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.HideImage,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Remove Album Cover Art",
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Text(
                                            text = if (isBatch) "Strip cover photos from all selected audio files." else "Strip cover photo from this audio file.",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Switch(
                                        checked = removeCover,
                                        onCheckedChange = { viewModel.updateRemoveCover(it) }
                                    )
                                }
                            }
                        }
                    }

                    // Form Fields Header
                    item {
                        Text(
                            text = "METADATA FIELDS",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )
                    }

                    // Title (Single only)
                    if (!isBatch) {
                        item {
                            EditorTextField(
                                value = title,
                                onValueChange = { viewModel.updateTitle(it) },
                                label = "Title",
                                isMixed = false,
                                currentAction = "OVERWRITE",
                                onActionChange = {}
                            )
                        }
                    }

                    // Artist
                    item {
                        EditorTextField(
                            value = artist,
                            onValueChange = { viewModel.updateArtist(it) },
                            label = "Artist",
                            isMixed = isBatch && mixedFields.contains("artist"),
                            currentAction = mixedFieldsAction["artist"] ?: "OVERWRITE",
                            onActionChange = { action -> viewModel.setMixedFieldAction("artist", action) }
                        )
                    }

                    // Album
                    item {
                        EditorTextField(
                            value = album,
                            onValueChange = { viewModel.updateAlbum(it) },
                            label = "Album",
                            isMixed = isBatch && mixedFields.contains("album"),
                            currentAction = mixedFieldsAction["album"] ?: "OVERWRITE",
                            onActionChange = { action -> viewModel.setMixedFieldAction("album", action) }
                        )
                    }

                    // Album Artist
                    item {
                        EditorTextField(
                            value = albumArtist,
                            onValueChange = { viewModel.updateAlbumArtist(it) },
                            label = "Album Artist",
                            isMixed = isBatch && mixedFields.contains("albumArtist"),
                            currentAction = mixedFieldsAction["albumArtist"] ?: "OVERWRITE",
                            onActionChange = { action -> viewModel.setMixedFieldAction("albumArtist", action) }
                        )
                    }

                    // Genre
                    item {
                        EditorTextField(
                            value = genre,
                            onValueChange = { viewModel.updateGenre(it) },
                            label = "Genre",
                            isMixed = isBatch && mixedFields.contains("genre"),
                            currentAction = mixedFieldsAction["genre"] ?: "OVERWRITE",
                            onActionChange = { action -> viewModel.setMixedFieldAction("genre", action) }
                        )
                    }

                    // Year
                    item {
                        EditorTextField(
                            value = year,
                            onValueChange = { viewModel.updateYear(it) },
                            label = "Year",
                            isMixed = isBatch && mixedFields.contains("year"),
                            currentAction = mixedFieldsAction["year"] ?: "OVERWRITE",
                            onActionChange = { action -> viewModel.setMixedFieldAction("year", action) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }

                    // Track Number (Single only)
                    if (!isBatch) {
                        item {
                            EditorTextField(
                                value = track,
                                onValueChange = { viewModel.updateTrack(it) },
                                label = "Track Number",
                                isMixed = false,
                                currentAction = "OVERWRITE",
                                onActionChange = {},
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                        }
                    }

                    // Disc Number
                    item {
                        EditorTextField(
                            value = discNumber,
                            onValueChange = { viewModel.updateDiscNumber(it) },
                            label = "Disc Number",
                            isMixed = isBatch && mixedFields.contains("discNumber"),
                            currentAction = mixedFieldsAction["discNumber"] ?: "OVERWRITE",
                            onActionChange = { action -> viewModel.setMixedFieldAction("discNumber", action) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }

                    // Composer
                    item {
                        EditorTextField(
                            value = composer,
                            onValueChange = { viewModel.updateComposer(it) },
                            label = "Composer",
                            isMixed = isBatch && mixedFields.contains("composer"),
                            currentAction = mixedFieldsAction["composer"] ?: "OVERWRITE",
                            onActionChange = { action -> viewModel.setMixedFieldAction("composer", action) }
                        )
                    }

                    // Comment
                    item {
                        EditorTextField(
                            value = comment,
                            onValueChange = { viewModel.updateComment(it) },
                            label = "Comment",
                            isMixed = isBatch && mixedFields.contains("comment"),
                            currentAction = mixedFieldsAction["comment"] ?: "OVERWRITE",
                            onActionChange = { action -> viewModel.setMixedFieldAction("comment", action) }
                        )
                    }

                    // Description
                    item {
                        EditorTextField(
                            value = description,
                            onValueChange = { viewModel.updateDescription(it) },
                            label = "Description",
                            isMixed = isBatch && mixedFields.contains("description"),
                            currentAction = mixedFieldsAction["description"] ?: "OVERWRITE",
                            onActionChange = { action -> viewModel.setMixedFieldAction("description", action) }
                        )
                    }

                    // Advanced Technical Info Section at the bottom (Single only)
                    if (!isBatch && files.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            val file = files.first()
                            AdvancedTechnicalInfoCard(
                                format = file.format,
                                bitrate = file.bitrate,
                                sampleRate = file.sampleRate,
                                size = file.sizeFormatted,
                                duration = file.durationFormatted
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdvancedTechnicalInfoCard(
    format: String,
    bitrate: String,
    sampleRate: String,
    size: String,
    duration: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Advanced Technical Info",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TechnicalInfoItem(label = "Format / Codec", value = format.substringAfterLast("/").uppercase(), modifier = Modifier.weight(1f))
                    TechnicalInfoItem(label = "Bitrate", value = bitrate, modifier = Modifier.weight(1f))
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TechnicalInfoItem(label = "Sample Rate", value = sampleRate, modifier = Modifier.weight(1f))
                    TechnicalInfoItem(label = "File Size", value = size, modifier = Modifier.weight(1f))
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TechnicalInfoItem(label = "Duration", value = duration, modifier = Modifier.weight(1f))
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun TechnicalInfoItem(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value.ifBlank { "Unknown" },
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isMixed: Boolean,
    currentAction: String, // "KEEP", "BLANK", "OVERWRITE"
    onActionChange: (String) -> Unit,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (isMixed) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = "Mixed Values",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        if (isMixed) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Keep Chip
                InputChip(
                    selected = currentAction == "KEEP",
                    onClick = { onActionChange("KEEP") },
                    label = { Text("Keep Original", style = MaterialTheme.typography.bodySmall) },
                    colors = InputChipDefaults.inputChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    shape = RoundedCornerShape(8.dp)
                )

                // Blank Chip
                InputChip(
                    selected = currentAction == "BLANK",
                    onClick = { onActionChange("BLANK") },
                    label = { Text("Set Blank", style = MaterialTheme.typography.bodySmall) },
                    colors = InputChipDefaults.inputChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.errorContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    shape = RoundedCornerShape(8.dp)
                )

                // Overwrite / Custom Chip (optional, auto-activated when typing)
                InputChip(
                    selected = currentAction == "OVERWRITE",
                    onClick = { onActionChange("OVERWRITE") },
                    label = { Text("Overwrite", style = MaterialTheme.typography.bodySmall) },
                    colors = InputChipDefaults.inputChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
            }
        }

        val placeholderText = when {
            isMixed && currentAction == "KEEP" -> "Keeping original values (multiple detected)"
            isMixed && currentAction == "BLANK" -> "Clearing field (will be saved as blank)"
            isMixed && currentAction == "OVERWRITE" -> "Enter custom value to overwrite"
            else -> "Enter $label"
        }

        TextField(
            value = value,
            onValueChange = {
                onValueChange(it)
                if (isMixed && currentAction != "OVERWRITE") {
                    onActionChange("OVERWRITE")
                }
            },
            enabled = !isMixed || currentAction == "OVERWRITE",
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)),
            placeholder = {
                Text(
                    text = placeholderText,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp,
                    style = MaterialTheme.typography.bodyMedium.copy(textDirection = TextDirection.Content)
                )
            },
            textStyle = LocalTextStyle.current.copy(textDirection = TextDirection.Content),
            keyboardOptions = keyboardOptions,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                disabledTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent
            ),
            singleLine = true
        )
    }
}
