package com.tagdeck.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import java.io.File
import java.util.Locale

object StorageHelper {
    private const val TAG = "StorageHelper"

    // Supported extensions (lowercase)
    private val SUPPORTED_EXTENSIONS = setOf("mp3", "m4a", "flac", "wav", "ogg")

    // Persist permissions for selected directory Uri
    fun persistFolderPermission(context: Context, treeUri: Uri) {
        try {
            val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(treeUri, takeFlags)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to persist folder permission", e)
        }
    }

    // List supported audio files in the selected tree Uri recursively
    fun listAudioFiles(context: Context, treeUri: Uri): List<Uri> {
        val audioUris = mutableListOf<Uri>()
        try {
            val documentId = DocumentsContract.getTreeDocumentId(treeUri)
            val parentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId)
            traverseDirectory(context, treeUri, parentUri, audioUris)
        } catch (e: Exception) {
            Log.e(TAG, "Error listing audio files", e)
        }
        return audioUris
    }

    private fun traverseDirectory(context: Context, treeUri: Uri, parentUri: Uri, results: MutableList<Uri>) {
        val parentId = DocumentsContract.getDocumentId(parentUri)
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentId)

        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE
        )

        try {
            context.contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
                val idIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val mimeIndex = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)

                while (cursor.moveToNext()) {
                    val docId = cursor.getString(idIndex)
                    val displayName = cursor.getString(nameIndex)
                    val mimeType = cursor.getString(mimeIndex)
                    val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)

                    if (mimeType == DocumentsContract.Document.MIME_TYPE_DIR) {
                        // Recursively traverse subdirectory
                        traverseDirectory(context, treeUri, docUri, results)
                    } else {
                        // Check if it is a supported audio file based on extension or mime type
                        val ext = displayName.substringAfterLast('.', "").lowercase(Locale.US)
                        val isAudioMime = mimeType?.startsWith("audio/") == true
                        if (SUPPORTED_EXTENSIONS.contains(ext) || isAudioMime) {
                            results.add(docUri)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error traversing directory $parentId", e)
        }
    }
}
