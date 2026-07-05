package com.tagdeck.ui.editor

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tagdeck.data.AudioMetadata
import com.tagdeck.data.DataRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Immutable
data class EditorUiState(
    val selectedFiles: List<AudioMetadata> = emptyList(),
    val isLoading: Boolean = false,
    val saveSuccess: Boolean? = null,
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    val year: String = "",
    val genre: String = "",
    val track: String = "",
    val albumArtist: String = "",
    val comment: String = "",
    val description: String = "",
    val composer: String = "",
    val discNumber: String = "",
    val removeCover: Boolean = false,
    val isBatchEdit: Boolean = false,
    val mixedFields: Set<String> = emptySet(),
    val mixedFieldsAction: Map<String, String> = emptyMap(), // "KEEP", "BLANK", "OVERWRITE"
    val albumArtBytes: ByteArray? = null,
    val coverImageBitmap: ImageBitmap? = null,
    val filesWithCoverArtCount: Int = 0,
    val isDirty: Boolean = false
)

@Stable
class EditorScreenViewModel(private val repository: DataRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState = _uiState.asStateFlow()

    // Public state modifiers
    fun updateTitle(value: String) { _uiState.value = _uiState.value.copy(title = value, isDirty = true) }
    
    fun updateArtist(value: String) { 
        val current = _uiState.value
        _uiState.value = current.copy(
            artist = value,
            mixedFieldsAction = current.mixedFieldsAction + ("artist" to "CHOOSE"), isDirty = true
        ) 
    }
    
    fun updateAlbum(value: String) { 
        val current = _uiState.value
        _uiState.value = current.copy(
            album = value,
            mixedFieldsAction = current.mixedFieldsAction + ("album" to "CHOOSE"), isDirty = true
        ) 
    }
    
    fun updateYear(value: String) { 
        val current = _uiState.value
        _uiState.value = current.copy(
            year = value,
            mixedFieldsAction = current.mixedFieldsAction + ("year" to "CHOOSE"), isDirty = true
        ) 
    }
    
    fun updateGenre(value: String) { 
        val current = _uiState.value
        _uiState.value = current.copy(
            genre = value,
            mixedFieldsAction = current.mixedFieldsAction + ("genre" to "CHOOSE"), isDirty = true
        ) 
    }
    
    fun updateTrack(value: String) { _uiState.value = _uiState.value.copy(track = value, isDirty = true) }
    
    fun updateAlbumArtist(value: String) { 
        val current = _uiState.value
        _uiState.value = current.copy(
            albumArtist = value,
            mixedFieldsAction = current.mixedFieldsAction + ("albumArtist" to "CHOOSE"), isDirty = true
        ) 
    }

    fun updateComment(value: String) { 
        val current = _uiState.value
        _uiState.value = current.copy(
            comment = value,
            mixedFieldsAction = current.mixedFieldsAction + ("comment" to "CHOOSE"), isDirty = true
        ) 
    }

    fun updateDescription(value: String) { 
        val current = _uiState.value
        _uiState.value = current.copy(
            description = value,
            mixedFieldsAction = current.mixedFieldsAction + ("description" to "CHOOSE"), isDirty = true
        ) 
    }

    fun updateComposer(value: String) { 
        val current = _uiState.value
        _uiState.value = current.copy(
            composer = value,
            mixedFieldsAction = current.mixedFieldsAction + ("composer" to "CHOOSE"), isDirty = true
        ) 
    }

    fun updateDiscNumber(value: String) { 
        val current = _uiState.value
        _uiState.value = current.copy(
            discNumber = value,
            mixedFieldsAction = current.mixedFieldsAction + ("discNumber" to "CHOOSE"), isDirty = true
        ) 
    }
    
    fun updateRemoveCover(value: Boolean) { _uiState.value = _uiState.value.copy(removeCover = value, isDirty = true) }

    fun setMixedFieldAction(field: String, action: String) {
        val current = _uiState.value
        _uiState.value = current.copy(
            mixedFieldsAction = current.mixedFieldsAction + (field to action), isDirty = true,
            artist = if (field == "artist" && action != "CHOOSE") "" else current.artist,
            album = if (field == "album" && action != "CHOOSE") "" else current.album,
            albumArtist = if (field == "albumArtist" && action != "CHOOSE") "" else current.albumArtist,
            genre = if (field == "genre" && action != "CHOOSE") "" else current.genre,
            year = if (field == "year" && action != "CHOOSE") "" else current.year,
            comment = if (field == "comment" && action != "CHOOSE") "" else current.comment,
            description = if (field == "description" && action != "CHOOSE") "" else current.description,
            composer = if (field == "composer" && action != "CHOOSE") "" else current.composer,
            discNumber = if (field == "discNumber" && action != "CHOOSE") "" else current.discNumber
        )
    }

    fun loadFiles(context: Context, uris: List<String>) {
        val allMetadata = repository.loadedFiles.value
        val filesToEdit = allMetadata.filter { uris.contains(it.uriString) }

        if (filesToEdit.isEmpty()) return

        if (filesToEdit.size == 1) {
            val file = filesToEdit.first()
            _uiState.value = EditorUiState(
                selectedFiles = filesToEdit,
                isBatchEdit = false,
                title = file.title,
                artist = file.artist,
                album = file.album,
                year = file.year,
                genre = file.genre,
                track = file.track,
                albumArtist = file.albumArtist,
                comment = file.comment,
                description = file.description,
                composer = file.composer,
                discNumber = file.discNumber,
                removeCover = false,
                mixedFields = emptySet(),
                mixedFieldsAction = emptyMap(),
                albumArtBytes = null,
                coverImageBitmap = null,
                filesWithCoverArtCount = if (file.hasCoverArt) 1 else 0
            )

            // Load album art bytes in background and decode asynchronously
            viewModelScope.launch(Dispatchers.IO) {
                val artBytes = repository.getAudioArt(context, file.uriString)
                val bitmap = if (artBytes != null) {
                    try {
                        val bmp = BitmapFactory.decodeByteArray(artBytes, 0, artBytes.size)
                        bmp?.asImageBitmap()
                    } catch (e: Exception) {
                        null
                    }
                } else {
                    null
                }
                _uiState.value = _uiState.value.copy(
                    albumArtBytes = artBytes,
                    coverImageBitmap = bitmap
                )
            }
        } else {
            // Batch Mode: Calculate mixed fields
            val mixed = mutableSetOf<String>()
            val mixedActions = mutableMapOf<String, String>()
            
            val artistValues = filesToEdit.map { it.artist }.filter { it.isNotBlank() }.distinct()
            val finalArtist = if (artistValues.size <= 1) artistValues.firstOrNull() ?: "" else { mixed.add("artist"); mixedActions["artist"] = "KEEP"; "" }

            val albumValues = filesToEdit.map { it.album }.filter { it.isNotBlank() }.distinct()
            val finalAlbum = if (albumValues.size <= 1) albumValues.firstOrNull() ?: "" else { mixed.add("album"); mixedActions["album"] = "KEEP"; "" }

            val yearValues = filesToEdit.map { it.year }.filter { it.isNotBlank() }.distinct()
            val finalYear = if (yearValues.size <= 1) yearValues.firstOrNull() ?: "" else { mixed.add("year"); mixedActions["year"] = "KEEP"; "" }

            val genreValues = filesToEdit.map { it.genre }.filter { it.isNotBlank() }.distinct()
            val finalGenre = if (genreValues.size <= 1) genreValues.firstOrNull() ?: "" else { mixed.add("genre"); mixedActions["genre"] = "KEEP"; "" }

            val albumArtistValues = filesToEdit.map { it.albumArtist }.filter { it.isNotBlank() }.distinct()
            val finalAlbumArtist = if (albumArtistValues.size <= 1) albumArtistValues.firstOrNull() ?: "" else { mixed.add("albumArtist"); mixedActions["albumArtist"] = "KEEP"; "" }

            val commentValues = filesToEdit.map { it.comment }.filter { it.isNotBlank() }.distinct()
            val finalComment = if (commentValues.size <= 1) commentValues.firstOrNull() ?: "" else { mixed.add("comment"); mixedActions["comment"] = "KEEP"; "" }

            val descriptionValues = filesToEdit.map { it.description }.filter { it.isNotBlank() }.distinct()
            val finalDescription = if (descriptionValues.size <= 1) descriptionValues.firstOrNull() ?: "" else { mixed.add("description"); mixedActions["description"] = "KEEP"; "" }

            val composerValues = filesToEdit.map { it.composer }.filter { it.isNotBlank() }.distinct()
            val finalComposer = if (composerValues.size <= 1) composerValues.firstOrNull() ?: "" else { mixed.add("composer"); mixedActions["composer"] = "KEEP"; "" }

            val discNumberValues = filesToEdit.map { it.discNumber }.filter { it.isNotBlank() }.distinct()
            val finalDiscNumber = if (discNumberValues.size <= 1) discNumberValues.firstOrNull() ?: "" else { mixed.add("discNumber"); mixedActions["discNumber"] = "KEEP"; "" }

            val filesWithCoverArtCount = filesToEdit.count { it.hasCoverArt }

            _uiState.value = EditorUiState(
                selectedFiles = filesToEdit,
                isBatchEdit = true,
                artist = finalArtist,
                album = finalAlbum,
                year = finalYear,
                genre = finalGenre,
                albumArtist = finalAlbumArtist,
                comment = finalComment,
                description = finalDescription,
                composer = finalComposer,
                discNumber = finalDiscNumber,
                title = "",
                track = "",
                removeCover = false,
                mixedFields = mixed,
                mixedFieldsAction = mixedActions,
                albumArtBytes = null,
                coverImageBitmap = null,
                filesWithCoverArtCount = filesWithCoverArtCount
            )
        }
    }

    fun saveChanges(context: Context) {
        val state = _uiState.value
        val uris = state.selectedFiles.map { it.uriString }
        if (uris.isEmpty()) return

        _uiState.value = state.copy(isLoading = true, saveSuccess = null)

        viewModelScope.launch {
            val isBatch = state.isBatchEdit
            
            val getVal = { field: String, typedVal: String ->
                if (isBatch && state.mixedFields.contains(field)) {
                    val action = state.mixedFieldsAction[field] ?: "KEEP"
                    when (action) {
                        "BLANK" -> ""
                        "KEEP" -> null
                        else -> typedVal
                    }
                } else {
                    typedVal
                }
            }

            repository.stageTagUpdates(
                uris = uris,
                title = if (isBatch) null else state.title,
                artist = getVal("artist", state.artist),
                album = getVal("album", state.album),
                year = getVal("year", state.year),
                genre = getVal("genre", state.genre),
                track = if (isBatch) null else state.track,
                albumArtist = getVal("albumArtist", state.albumArtist),
                comment = getVal("comment", state.comment),
                description = getVal("description", state.description),
                composer = getVal("composer", state.composer),
                discNumber = getVal("discNumber", state.discNumber),
                removeCover = state.removeCover
            )
            
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                saveSuccess = true
            )
        }
    }

    fun resetSuccess() {
        _uiState.value = _uiState.value.copy(saveSuccess = null)
    }
}
