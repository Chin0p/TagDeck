package com.audiotageditor.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

object SettingsManager {
    private const val PREFS_NAME = "audio_tag_editor_settings_prefs"
    private const val KEY_TAG_TO_FILENAME = "tag_to_filename_template"
    private const val KEY_FILENAME_TO_TAG = "filename_to_tag_template"

    @Volatile
    private var prefs: SharedPreferences? = null

    private val _tagToFilenameTemplate = MutableStateFlow("[Artist] - [Title]")
    val tagToFilenameTemplate: StateFlow<String> = _tagToFilenameTemplate.asStateFlow()

    private val _filenameToTagTemplate = MutableStateFlow("[Artist] - [Title]")
    val filenameToTagTemplate: StateFlow<String> = _filenameToTagTemplate.asStateFlow()

    fun init(context: Context) {
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            val sharedPrefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs = sharedPrefs
            
            _tagToFilenameTemplate.value = sharedPrefs.getString(KEY_TAG_TO_FILENAME, "[Artist] - [Title]") ?: "[Artist] - [Title]"
            _filenameToTagTemplate.value = sharedPrefs.getString(KEY_FILENAME_TO_TAG, "[Artist] - [Title]") ?: "[Artist] - [Title]"
        }
    }

    fun setTagToFilenameTemplate(template: String) {
        _tagToFilenameTemplate.value = template
        CoroutineScope(Dispatchers.IO).launch {
            prefs?.edit()?.putString(KEY_TAG_TO_FILENAME, template)?.apply()
        }
    }

    fun setFilenameToTagTemplate(template: String) {
        _filenameToTagTemplate.value = template
        CoroutineScope(Dispatchers.IO).launch {
            prefs?.edit()?.putString(KEY_FILENAME_TO_TAG, template)?.apply()
        }
    }

    // Parses tags from filename based on template pattern matching
    fun parseMetadataFromFilename(fileName: String, pattern: String): Map<String, String> {
        var finalRegexStr = ""
        var i = 0
        while (i < pattern.length) {
            if (pattern.startsWith("[Artist]", i) || pattern.startsWith("{Artist}", i)) {
                finalRegexStr += "(?<artist>.+?)"
                i += 8
            } else if (pattern.startsWith("[Title]", i) || pattern.startsWith("{Title}", i)) {
                finalRegexStr += "(?<title>.+?)"
                i += 7
            } else if (pattern.startsWith("[Album]", i) || pattern.startsWith("{Album}", i)) {
                finalRegexStr += "(?<album>.+?)"
                i += 7
            } else if (pattern.startsWith("[Track]", i) || pattern.startsWith("{Track}", i)) {
                finalRegexStr += "(?<track>\\d+?)"
                i += 7
            } else if (pattern.startsWith("[Year]", i) || pattern.startsWith("{Year}", i)) {
                finalRegexStr += "(?<year>\\d+?)"
                i += 6
            } else {
                val char = pattern[i]
                if ("\\^$.|?*+()".contains(char)) {
                    finalRegexStr += "\\" + char
                } else {
                    finalRegexStr += char
                }
                i++
            }
        }
        
        val nameWithoutExt = fileName.substringBeforeLast('.')
        try {
            val regex = Regex("^" + finalRegexStr + "$", RegexOption.IGNORE_CASE)
            val matchResult = regex.matchEntire(nameWithoutExt)
            if (matchResult != null) {
                val result = mutableMapOf<String, String>()
                if (regex.pattern.contains("<artist>")) {
                    matchResult.groups["artist"]?.value?.trim()?.let { result["artist"] = it }
                }
                if (regex.pattern.contains("<title>")) {
                    matchResult.groups["title"]?.value?.trim()?.let { result["title"] = it }
                }
                if (regex.pattern.contains("<album>")) {
                    matchResult.groups["album"]?.value?.trim()?.let { result["album"] = it }
                }
                if (regex.pattern.contains("<track>")) {
                    matchResult.groups["track"]?.value?.trim()?.let { result["track"] = it }
                }
                if (regex.pattern.contains("<year>")) {
                    matchResult.groups["year"]?.value?.trim()?.let { result["year"] = it }
                }
                return result
            }
        } catch (e: Exception) {
            Log.e("SettingsManager", "Regex compilation/match failed: $finalRegexStr", e)
        }
        return emptyMap()
    }
}
