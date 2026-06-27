package com.audiotageditor.data

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import java.io.File
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

interface DataRepository {
    val loadedFiles: StateFlow<List<AudioMetadata>>
    val isLoading: StateFlow<Boolean>
    val currentFolderUri: StateFlow<String?>

    fun setSelectedUris(uris: List<String>)
    fun getSelectedUris(): List<String>

    suspend fun loadFolder(context: Context, treeUri: Uri)
    suspend fun loadFiles(context: Context, uris: List<Uri>)
    suspend fun updateTags(
        context: Context,
        uris: List<String>,
        title: String? = null,
        artist: String? = null,
        album: String? = null,
        year: String? = null,
        genre: String? = null,
        track: String? = null,
        albumArtist: String? = null,
        comment: String? = null,
        description: String? = null,
        composer: String? = null,
        discNumber: String? = null,
        removeCover: Boolean = false,
        stripAll: Boolean = false
    ): Boolean

    suspend fun renameFiles(
        context: Context,
        uris: List<String>,
        template: String
    ): Boolean

    fun getAudioArt(context: Context, uriString: String): ByteArray?
}

class DefaultDataRepository : DataRepository {
    private val TAG = "DefaultDataRepository"
    
    private fun getFilePathFromUri(context: Context, uri: Uri): String? {
        if (uri.scheme == "file") {
            return uri.path
        }
        // Try querying MediaStore
        try {
            context.contentResolver.query(uri, arrayOf(MediaStore.Audio.Media.DATA), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val columnIndex = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
                    if (columnIndex != -1) {
                        return cursor.getString(columnIndex)
                    }
                }
            }
        } catch (e: Exception) {
            // ignore
        }
        
        // Check if it's a documents provider URI
        if (DocumentsContract.isDocumentUri(context, uri)) {
            val docId = DocumentsContract.getDocumentId(uri)
            if (docId.startsWith("audio:")) {
                val id = docId.split(":")[1]
                val mediaStoreUri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    id.toLong()
                )
                try {
                    context.contentResolver.query(mediaStoreUri, arrayOf(MediaStore.Audio.Media.DATA), null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val columnIndex = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
                            if (columnIndex != -1) {
                                                        return cursor.getString(columnIndex)
                            }
                        }
                    }
                } catch (e: Exception) {
                    // ignore
                }
            }
        }
        return null
    }

    private fun forceMediaStoreUpdate(context: Context, uris: List<String>) {
        Log.d(TAG, "Starting robust MediaStore refresh for ${uris.size} URIs")
        for (uriStr in uris) {
            try {
                val uri = Uri.parse(uriStr)
                
                // Strategy 1: Direct content resolver notification
                context.contentResolver.notifyChange(uri, null)
                
                // Resolve path if possible
                val path = getFilePathFromUri(context, uri)
                
                // Strategy 2: If we have documents provider media ID, update it directly
                if (DocumentsContract.isDocumentUri(context, uri)) {
                    val docId = DocumentsContract.getDocumentId(uri)
                    if (docId.startsWith("audio:")) {
                        val id = docId.split(":")[1]
                        val mediaStoreUri = ContentUris.withAppendedId(
                            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                            id.toLong()
                        )
                        try {
                            val values = ContentValues().apply {
                                put(MediaStore.Audio.Media.DATE_MODIFIED, System.currentTimeMillis() / 1000)
                            }
                            context.contentResolver.update(mediaStoreUri, values, null, null)
                            context.contentResolver.notifyChange(mediaStoreUri, null)
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed direct MediaStore table update for $mediaStoreUri", e)
                        }
                    }
                }

                if (path != null) {
                    val file = File(path)
                    if (file.exists()) {
                        // Strategy 3: Broadcast file scan intent
                        val scanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file))
                        context.sendBroadcast(scanIntent)
                        
                        // Strategy 4: Modern MediaScannerConnection scan
                        MediaScannerConnection.scanFile(context, arrayOf(path), null) { scannedPath, scannedUri ->
                            Log.d(TAG, "MediaScanner scanned: $scannedPath to $scannedUri")
                            if (scannedUri != null) {
                                try {
                                    val values = ContentValues().apply {
                                        put(MediaStore.Audio.Media.DATE_MODIFIED, System.currentTimeMillis() / 1000)
                                    }
                                    context.contentResolver.update(scannedUri, values, null, null)
                                    context.contentResolver.notifyChange(scannedUri, null)
                                } catch (e: Exception) {
                                    Log.w(TAG, "Failed secondary MediaStore refresh on $scannedUri", e)
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error forcing MediaStore update for $uriStr", e)
            }
        }
    }

    private val _loadedFiles = MutableStateFlow<List<AudioMetadata>>(emptyList())
    override val loadedFiles: StateFlow<List<AudioMetadata>> = _loadedFiles.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    override val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _currentFolderUri = MutableStateFlow<String?>(null)
    override val currentFolderUri: StateFlow<String?> = _currentFolderUri.asStateFlow()

    private val _loadedFileUris = MutableStateFlow<List<Uri>>(emptyList())
    
    private var _selectedUrisToEdit = emptyList<String>()

    override fun setSelectedUris(uris: List<String>) {
        _selectedUrisToEdit = uris
    }

    override fun getSelectedUris(): List<String> {
        return _selectedUrisToEdit
    }

    override suspend fun loadFolder(context: Context, treeUri: Uri) {
        _isLoading.value = true
        _currentFolderUri.value = treeUri.toString()
        
        withContext(Dispatchers.IO) {
            try {
                // List files using SAF
                val fileUris = StorageHelper.listAudioFiles(context, treeUri)
                Log.d(TAG, "Found ${fileUris.size} audio files under $treeUri")
                _loadedFileUris.value = fileUris

                // Parse metadata for each file
                val metadataList = mutableListOf<AudioMetadata>()
                for (uri in fileUris) {
                    val meta = TagEngine.readMetadata(context, uri)
                    if (meta != null) {
                        metadataList.add(meta)
                    }
                }
                
                // Sort by file name
                metadataList.sortBy { it.fileName.lowercase() }
                _loadedFiles.value = metadataList
            } catch (e: Exception) {
                Log.e(TAG, "Error loading files from folder", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    override suspend fun loadFiles(context: Context, uris: List<Uri>) {
        _isLoading.value = true
        _currentFolderUri.value = "Selected Files"
        _loadedFileUris.value = uris
        
        withContext(Dispatchers.IO) {
            try {
                val metadataList = mutableListOf<AudioMetadata>()
                for (uri in uris) {
                    val meta = TagEngine.readMetadata(context, uri)
                    if (meta != null) {
                        metadataList.add(meta)
                    }
                }
                
                metadataList.sortBy { it.fileName.lowercase() }
                _loadedFiles.value = metadataList
            } catch (e: Exception) {
                Log.e(TAG, "Error loading chosen files", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    override suspend fun updateTags(
        context: Context,
        uris: List<String>,
        title: String?,
        artist: String?,
        album: String?,
        year: String?,
        genre: String?,
        track: String?,
        albumArtist: String?,
        comment: String?,
        description: String?,
        composer: String?,
        discNumber: String?,
        removeCover: Boolean,
        stripAll: Boolean
    ): Boolean {
        _isLoading.value = true
        return withContext(Dispatchers.IO) {
            var successCount = 0
            for (uriStr in uris) {
                val uri = Uri.parse(uriStr)
                val titleToSave = if (uris.size > 1) null else title
                val trackToSave = if (uris.size > 1) null else track

                val result = TagEngine.writeMetadata(
                    context = context,
                    uri = uri,
                    title = titleToSave ?: title,
                    artist = artist,
                    album = album,
                    year = year,
                    genre = genre,
                    track = trackToSave ?: track,
                    albumArtist = albumArtist,
                    comment = comment,
                    description = description,
                    composer = composer,
                    discNumber = discNumber,
                    removeCover = removeCover,
                    stripAll = stripAll
                )
                if (result) {
                    successCount++
                }
            }

            if (successCount > 0) {
                forceMediaStoreUpdate(context, uris)
                // Short coroutine delay (around 500-800ms) after refresh
                delay(600)
            }

            // Reload the source
            val folderUriStr = _currentFolderUri.value
            if (folderUriStr != null) {
                if (folderUriStr == "Selected Files") {
                    loadFiles(context, _loadedFileUris.value)
                } else {
                    val folderUri = Uri.parse(folderUriStr)
                    loadFolder(context, folderUri)
                }
            }
            
            _isLoading.value = false
            successCount > 0
        }
    }

    override suspend fun renameFiles(
        context: Context,
        uris: List<String>,
        template: String
    ): Boolean {
        _isLoading.value = true
        return withContext(Dispatchers.IO) {
            var renameCount = 0
            val updatedUris = _loadedFileUris.value.toMutableList()

            for (uriStr in uris) {
                val uri = Uri.parse(uriStr)
                val metadata = TagEngine.readMetadata(context, uri) ?: continue
                val extension = metadata.fileName.substringAfterLast('.', "")
                
                // Construct new display name based on template
                var newName = template
                    .replace("[Artist]", metadata.artist.ifBlank { "Unknown Artist" }, ignoreCase = true)
                    .replace("{Artist}", metadata.artist.ifBlank { "Unknown Artist" }, ignoreCase = true)
                    .replace("[Title]", metadata.title.ifBlank { metadata.fileName.substringBeforeLast('.') }, ignoreCase = true)
                    .replace("{Title}", metadata.title.ifBlank { metadata.fileName.substringBeforeLast('.') }, ignoreCase = true)
                    .replace("[Album]", metadata.album.ifBlank { "Unknown Album" }, ignoreCase = true)
                    .replace("{Album}", metadata.album.ifBlank { "Unknown Album" }, ignoreCase = true)
                    .replace("[Track]", metadata.track.ifBlank { "00" }, ignoreCase = true)
                    .replace("{Track}", metadata.track.ifBlank { "00" }, ignoreCase = true)
                    .replace("[Year]", metadata.year.ifBlank { "2026" }, ignoreCase = true)
                    .replace("{Year}", metadata.year.ifBlank { "2026" }, ignoreCase = true)

                // Sanitize filename to avoid invalid characters
                newName = newName.replace(Regex("[\\\\/:*?\"<>|]"), "_")
                
                val newNameWithExt = if (extension.isNotEmpty()) "$newName.$extension" else newName

                // Perform rename
                val newUri: Uri? = try {
                    if (uri.scheme == "file") {
                        val file = File(uri.path ?: continue)
                        val parent = file.parentFile
                        val newFile = File(parent, newNameWithExt)
                        if (file.renameTo(newFile)) {
                            Uri.fromFile(newFile)
                        } else {
                            null
                        }
                    } else {
                        android.provider.DocumentsContract.renameDocument(context.contentResolver, uri, newNameWithExt)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to rename $uriStr to $newNameWithExt", e)
                    null
                }

                if (newUri != null) {
                    renameCount++
                    // Update loaded list
                    val idx = updatedUris.indexOfFirst { it.toString() == uriStr }
                    if (idx != -1) {
                        updatedUris[idx] = newUri
                    }
                }
            }

            _loadedFileUris.value = updatedUris

            if (renameCount > 0) {
                forceMediaStoreUpdate(context, updatedUris.map { it.toString() })
                // Short coroutine delay (around 500-800ms) after refresh
                delay(600)
            }

            // Reload the source
            val folderUriStr = _currentFolderUri.value
            if (folderUriStr != null) {
                if (folderUriStr == "Selected Files") {
                    loadFiles(context, updatedUris)
                } else {
                    val folderUri = Uri.parse(folderUriStr)
                    loadFolder(context, folderUri)
                }
            }

            _isLoading.value = false
            renameCount > 0
        }
    }

    override fun getAudioArt(context: Context, uriString: String): ByteArray? {
        val uri = Uri.parse(uriString)
        return TagEngine.readAlbumArt(context, uri)
    }
}
