package com.tagdeck.ui.library

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tagdeck.data.AudioMetadata
import com.tagdeck.data.DataRepository
import com.tagdeck.data.TagEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.collection.LruCache

@Stable
class LibraryScreenViewModel(private val repository: DataRepository) : ViewModel() {

    private val thumbnailCache = LruCache<String, ImageBitmap>(120)

    suspend fun getThumbnail(context: Context, uriString: String): ImageBitmap? {
        thumbnailCache.get(uriString)?.let { return it }
        return withContext(Dispatchers.IO) {
            val bytes = repository.getAudioArt(context, uriString) ?: return@withContext null
            try {
                val options = android.graphics.BitmapFactory.Options().apply { inSampleSize = 4 }
                val bmp = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
                bmp?.asImageBitmap()?.also { thumbnailCache.put(uriString, it) }
            } catch (e: Exception) {
                null
            }
        }
    }

    val isLoading = repository.isLoading
    val currentFolderUri = repository.currentFolderUri

    private val _selectedUris = MutableStateFlow<Set<String>>(emptySet())
    val selectedUris = _selectedUris.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedFormat = MutableStateFlow("All")
    val selectedFormat = _selectedFormat.asStateFlow()

    val hasAnyLoadedFiles: StateFlow<Boolean> = repository.loadedFiles
        .map { it.isNotEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val filteredFiles: StateFlow<List<AudioMetadata>> = combine(
        repository.loadedFiles,
        _searchQuery,
        _selectedFormat
    ) { files, query, format ->
        files.filter { item ->
            val matchesQuery = query.isBlank() || 
                    item.title.contains(query, ignoreCase = true) ||
                    item.artist.contains(query, ignoreCase = true) ||
                    item.album.contains(query, ignoreCase = true) ||
                    item.fileName.contains(query, ignoreCase = true)
            
            val matchesFormat = format == "All" || 
                    item.cleanFormat.equals(format, ignoreCase = true)
            
            matchesQuery && matchesFormat
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedFormat(format: String) {
        _selectedFormat.value = format
    }

    fun loadFolder(context: Context, treeUri: Uri) {
        viewModelScope.launch {
            _selectedUris.value = emptySet()
            repository.clearSavedSessionUris()
            repository.loadFolder(context, treeUri)
        }
    }

    fun loadFiles(context: Context, uris: List<Uri>) {
        viewModelScope.launch {
            _selectedUris.value = emptySet()
            repository.clearSavedSessionUris()
            repository.loadFiles(context, uris)
        }
    }

    fun toggleSelection(uriString: String) {
        val current = _selectedUris.value.toMutableSet()
        if (current.contains(uriString)) {
            current.remove(uriString)
        } else {
            current.add(uriString)
        }
        _selectedUris.value = current
    }

    fun selectAll() {
        _selectedUris.value = filteredFiles.value.map { it.uriString }.toSet()
    }

    fun deselectAll() {
        _selectedUris.value = emptySet()
    }

    fun renameSelectedFiles(context: Context, template: String, onComplete: () -> Unit) {
        val selected = _selectedUris.value.toList()
        if (selected.isEmpty()) return
        repository.stageRenameTemplate(selected, template)
        _selectedUris.value = emptySet()
        onComplete()
    }

    val pendingTagUpdates = repository.pendingTagUpdates
    val pendingRenames = repository.pendingRenames

    fun commitPendingChanges(context: Context, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val success = repository.commitPendingChanges(context)
            onComplete(success)
        }
    }

    fun clearPendingChanges() {
        repository.clearPendingChanges()
    }

    fun clearAllLoaded() {
        _selectedUris.value = emptySet()
        repository.clearAllLoaded()
    }

    fun extractTagsFromFilenames(context: Context, pattern: String, onComplete: () -> Unit) {
        val selected = _selectedUris.value.toList()
        if (selected.isEmpty()) return
        viewModelScope.launch {
            for (uriStr in selected) {
                val uri = Uri.parse(uriStr)
                val metadata = TagEngine.readMetadata(context, uri) ?: continue
                val parsed = com.tagdeck.data.SettingsManager.parseMetadataFromFilename(metadata.fileName, pattern)
                if (parsed.isNotEmpty()) {
                    repository.updateTags(
                        context = context,
                        uris = listOf(uriStr),
                        title = parsed["title"] ?: metadata.title,
                        artist = parsed["artist"] ?: metadata.artist,
                        album = parsed["album"] ?: metadata.album,
                        year = parsed["year"] ?: metadata.year,
                        genre = metadata.genre,
                        track = parsed["track"] ?: metadata.track,
                        albumArtist = metadata.albumArtist,
                        removeCover = false
                    )
                }
            }
            val currentFolder = currentFolderUri.value
            if (currentFolder != null && currentFolder != "Selected Files") {
                repository.loadFolder(context, Uri.parse(currentFolder))
            } else {
                val allUris = repository.loadedFiles.value.map { Uri.parse(it.uriString) }
                repository.loadFiles(context, allUris)
            }
            _selectedUris.value = emptySet()
            onComplete()
        }
    }
}