package com.sqz.checklist.ui.main.task

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import sqz.checklist.data.preferences.PreferencesInCache
import sqz.checklist.data.storage.audioMediaPath
import sqz.checklist.data.storage.pictureMediaPath
import sqz.checklist.data.storage.videoMediaPath
import java.io.File

@Deprecated("TODO: Deprecate this")
class TaskLayoutViewModel : ViewModel() {

    /** Remove invalid media file from error **/
    private var _removeInvalidFile by mutableStateOf(false)

    @Suppress("unused")
    private fun removeInvalidFile(context: Context) = viewModelScope.launch(Dispatchers.IO) {
        val cache = PreferencesInCache(context)
        val list = listOf(pictureMediaPath, videoMediaPath, audioMediaPath)
        if (cache.errFileNameSaver() != null) cache.errFileNameSaver()?.let {
            for (data in list) {
                val mediaDir = File(context.filesDir, data)
                val file = File(mediaDir, it)
                if (file.exists()) file.delete().also { cache.errFileNameSaver(null) }
            }
        }
        _removeInvalidFile = true
    }
}
