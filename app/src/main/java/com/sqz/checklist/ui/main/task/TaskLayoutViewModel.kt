package com.sqz.checklist.ui.main.task

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import sqz.checklist.data.preferences.PreferencesInCache
import sqz.checklist.data.preferences.PrimaryPreferences
import sqz.checklist.data.storage.audioMediaPath
import sqz.checklist.data.storage.pictureMediaPath
import sqz.checklist.data.storage.videoMediaPath
import sqz.checklist.task.api.list.TaskList
import java.io.File

@Deprecated("TODO: Deprecate this")
open class TaskLayoutViewModel : ViewModel() {

    /** Remove invalid media file from error **/
    private var _removeInvalidFile by mutableStateOf(false)
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

    private val _listConfig = MutableStateFlow(TaskList.Config())
    val listConfig = _listConfig.asStateFlow()

    fun updateListConfig(prefs: PrimaryPreferences) {
        fun Int.prefsLimit(): Int? = this.let {
            if (it >= 21) null else it
        }
        val config = TaskList.Config(
            enableUndo = !prefs.disableUndoButton(),
            autoDelIsHistoryTaskNumber = prefs.allowedNumberOfHistory().prefsLimit(),
            recentlyRemindedKeepTime = prefs.recentlyRemindedKeepTime(),
        )
        _listConfig.update { config }
    }
}
