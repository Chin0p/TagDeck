package com.audiotageditor.ui.library

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.audiotageditor.data.AudioMetadata
import com.audiotageditor.data.DataRepository
import com.audiotageditor.data.TagEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LibraryScreenViewModel(private val repository: DataRepository) : ViewModel() {

    // Basic repository states
    val isLoading = repository.isLoading
    val currentFolderUri = repository.currentFolderUri

    // Selected URIs for batch operations
    private val _selectedUris = MutableStateFlow<Set<String>>(emptySet())
    val selectedUris = _selectedUris.asStateFlow()

    // Search query
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    // Filtered files combining search query and loaded files
    val filteredFiles: StateFlow<List<AudioMetadata>> = combine(
        repository.loadedFiles,
        _searchQuery
    ) { files, query ->
        if (query.isBlank()) {
            files
        } else {
            files.filter { file ->
                file.title.contains(query, ignoreCase = true) ||
                        file.artist.contains(query, ignoreCase = true) ||
                        file.fileName.contains(query, ignoreCase = true) ||
                        file.album.contains(query, ignoreCase = true)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun loadFolder(context: Context, treeUri: Uri) {
        viewModelScope.launch {
            _selectedUris.value = emptySet()
            repository.loadFolder(context, treeUri)
        }
    }

    fun loadFiles(context: Context, uris: List<Uri>) {
        viewModelScope.launch {
            _selectedUris.value = emptySet()
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
        viewModelScope.launch {
            repository.renameFiles(context, selected, template)
            _selectedUris.value = emptySet()
            onComplete()
        }
    }

    fun extractTagsFromFilenames(context: Context, pattern: String, onComplete: () -> Unit) {
        val selected = _selectedUris.value.toList()
        if (selected.isEmpty()) return
        viewModelScope.launch {
            for (uriStr in selected) {
                val uri = Uri.parse(uriStr)
                val metadata = TagEngine.readMetadata(context, uri) ?: continue
                val parsed = com.audiotageditor.data.SettingsManager.parseMetadataFromFilename(metadata.fileName, pattern)
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
            // Reload folder
            val currentFolder = currentFolderUri.value
            if (currentFolder != null && currentFolder != "Selected Files") {
                repository.loadFolder(context, Uri.parse(currentFolder))
            } else {
                // reload files if loaded individually
                val allUris = repository.loadedFiles.value.map { Uri.parse(it.uriString) }
                repository.loadFiles(context, allUris)
            }
            _selectedUris.value = emptySet()
            onComplete()
        }
    }
}
