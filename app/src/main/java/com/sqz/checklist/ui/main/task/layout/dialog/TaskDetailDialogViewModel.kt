package com.sqz.checklist.ui.main.task.layout.dialog

import android.net.Uri
import android.util.Log
import android.util.Patterns
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.insert
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sqz.checklist.database.TaskDetailData
import com.sqz.checklist.database.TaskDetailType
import com.sqz.checklist.preferences.PreferencesInCache
import com.sqz.checklist.ui.common.media.AudioSelector
import com.sqz.checklist.ui.common.media.PictureSelector
import com.sqz.checklist.ui.common.media.VideoSelector
import com.sqz.checklist.ui.common.media.errUri
import com.sqz.checklist.ui.common.media.toByteArray
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class TaskDetailDialogViewModel : ViewModel() {
    private lateinit var _taskDetailList: MutableStateFlow<List<TaskDetailData>>
    private val _originalMediaURI: MutableStateFlow<List<String>> = MutableStateFlow(listOf())

    /** Init method with parameter **/
    fun init(taskDetailData: List<TaskDetailData>) { // Set list from the database
        this._taskDetailList = MutableStateFlow(taskDetailData)
        if (taskDetailData.size == 1) { // if is a single task detail
            _taskDetail.update { this._taskDetailList.value.last() }
            this.removeTaskDetailListItem(taskDetailData.size - 1)
        }
        for (i in taskDetailData) { // Set original media uri for delete removable media file
            if (i.dataByte == null) continue
            when (i.type) {
                TaskDetailType.Text -> continue
                TaskDetailType.URL -> continue
                TaskDetailType.Application -> continue
                TaskDetailType.Audio -> {}
                TaskDetailType.Picture -> {}
                TaskDetailType.Video -> {}
            }
            _originalMediaURI.update { it + (i.dataByte as ByteArray).toString(Charsets.UTF_8) }
        }
    }

    /** Default init method **/
    fun init() {
        this._taskDetailList = MutableStateFlow(listOf())
    }

    private var _isChanged: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isChanged = _isChanged.asStateFlow()

    /** Set [_isChanged] value **/
    fun isChanged(setter: Boolean) {
        _isChanged.update { setter }
    }

    /** Get final [_taskDetailList] **/
    fun getFinalTaskDetail(): List<TaskDetailData>? {
        if (this._taskDetailList.value.isEmpty()) {
            return null
        }
        return this._taskDetailList.value
    }

    /** Get [_taskDetailList] as [StateFlow] **/
    fun getTaskDetailList(): StateFlow<List<TaskDetailData>>? {
        if (this._taskDetailList.value.isEmpty()) {
            return null
        }
        return this._taskDetailList.asStateFlow()
    }

    /**
     * Determines if a new item can be added to the task detail list based on a weighted count.
     * Different item types have different "weights" (e.g., Video=5, Picture=2, Audio=3, others=1).
     *
     * @return a [StateFlow] emitting `true` if the total weight is less than 10, `false` otherwise.
     */
    fun allowAddToList(): StateFlow<Boolean> {
        val allow: StateFlow<Boolean> = this._taskDetailList.asStateFlow().map { list ->
            val video = list.count { it.type == TaskDetailType.Video }
            val audio = list.count { it.type == TaskDetailType.Audio }
            val picture = list.count { it.type == TaskDetailType.Picture }
            val size = list.size - video - audio - picture
            val videoWeight = video * 5
            val pictureWeight = picture * 2
            val audioWeight = audio * 3
            (size + videoWeight + pictureWeight + audioWeight) < 10
        }.stateIn(viewModelScope, SharingStarted.Eagerly, true)
        return allow
    }

    fun updateTaskDetailDescription(index: Int, description: String?) {
        if (!_isChanged.value) _isChanged.update { true }
        _taskDetailList.update { update ->
            val mutable = update.toMutableList()
            mutable[index] = mutable[index].copy(description = description)
            mutable
        }
    }

    /** Remove an item from [_taskDetailList], but won't update [_isChanged] **/
    private fun removeTaskDetailListItem(index: Int) {
        _taskDetailList.update { update ->
            update.filter { it != update[index] }
        }
    }

    /** Remove an item from [_taskDetailList] **/
    fun removeFromTaskDetailList(index: Int) {
        if (!_isChanged.value) _isChanged.update { true }
        this.removeTaskDetailListItem(index)
    }

    /** Move [_taskDetail] item to [_taskDetailList] **/
    fun addTaskDetailToList(index: Int? = null) {
        if (!_isChanged.value) _isChanged.update { true }
        if (index == null) _taskDetailList.update {
            it + _taskDetail.value!!
        } else _taskDetailList.update { currentList ->
            val mutable = currentList.toMutableList()
            mutable.add(index, _taskDetail.value!!)
            mutable.toList()
        }
        _taskDetail.value = null
    }

    private var _taskDetail: MutableStateFlow<TaskDetailData?> = MutableStateFlow(null)
    val taskDetail = _taskDetail.asStateFlow()

    /** Set [_taskDetail] type **/
    fun setTaskDetail(type: TaskDetailType) {
        if (!_isChanged.value) _isChanged.update { true }
        if (_taskDetail.value == null) _taskDetail.update {
            TaskDetailData(type = type, description = null, dataString = null, dataByte = null)
        } else _taskDetail.update {
            it?.copy(type = type)
        }
    }

    /** Set [_taskDetail] data **/
    fun setTaskDetail(setter: (TaskDetailData?) -> TaskDetailData?) {
        if (!_isChanged.value) _isChanged.update { true }
        if (_taskDetail.value == null) _taskDetail.update {
            val set = setter(it)
            set?.let {
                TaskDetailData(
                    type = set.type,
                    description = set.description,
                    dataString = set.dataString,
                    dataByte = set.dataByte
                )
            }
        } else _taskDetail.update {
            setter(it)
        }
        _taskDetail.update { setter(it) }
    }

    /** Move an item from task detail list to [_taskDetail] for edit **/
    fun setTaskDetailFromList(index: Int) {
        _taskDetail.update {
            _taskDetailList.value[index]
        }
        this.removeTaskDetailListItem(index)
    }

    /** Save the instance of class **/
    var functionalSaver: Any? = null

    /** Release resources **/
    @Override
    public override fun onCleared() {
        this.init()
        functionalSaver = null
        _taskDetail.value = null
        _isChanged.value = false
        _originalMediaURI.value = listOf()
        super.onCleared()
    }

    /**
     * Deletes unused media files from the cache.
     *
     * This function reads the list of file paths from the preferences cache. It then iterates
     * through them, deleting any file that is not currently in use (i.e., not in
     * [_originalMediaURI] or matching the [currentUri]). After deletion, it updates the
     * preferences cache to only contain the paths of the files that were kept.
     *
     * @param currentUri The URI of the media file currently being processed, to avoid deleting it.
     * @param prefsCache The [PreferencesInCache] instance to read from and write to.
     */
    private suspend fun deleteMediaCache(
        currentUri: Uri,
        prefsCache: PreferencesInCache
    ) = withContext(Dispatchers.Default) {
        val prefsLines =
            prefsCache.inProcessFilesPath()?.lineSequence()?.toList() ?: return@withContext
        try {
            val check = prefsLines.filter { line ->
                val file = File(line.toUri().path ?: "")
                if (file.exists()) { // if file is exist
                    if (_originalMediaURI.value.contains(line) || currentUri.toString() == line) {
                        return@filter true // skip valid uri
                    }
                    if (file.delete()) { // delete
                        return@filter false
                    }
                    Log.e("deleteMediaCache", "Failed to delete file: $file")
                    return@filter true
                }
                return@filter true
            }
            prefsCache.inProcessFilesPath(null)
            for (i in check) {
                if (!i.isBlank()) {
                    if (prefsCache.inProcessFilesPath() == null) {
                        prefsCache.inProcessFilesPath(i + "\n")
                    } else {
                        prefsCache.inProcessFilesPath(prefsCache.inProcessFilesPath() + i + "\n")
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Save unconfirmed path to preference when wrote media file to storage.
     * (on set new media files)
     *
     * **Remember** process the path once the action is ended!
     *
     * @param uri the path of the media file.
     * @param prefsCache the [PreferencesInCache] instance.
     */
    private suspend fun setCache(uri: Uri, prefsCache: PreferencesInCache) {
        withContext(Dispatchers.Default) {
            if (prefsCache.inProcessFilesPath() == null) {
                prefsCache.inProcessFilesPath(uri.toString() + "\n")
            } else {
                prefsCache.inProcessFilesPath(prefsCache.inProcessFilesPath() + uri.toString() + "\n")
            }
        }
    }

    /**
     * Handle [TaskDetailDialog] media process function.
     * @param uri the path of the media file that already write into storage.
     * @param prefsCache the [PreferencesInCache] instance.
     */
    fun onMediaSave(uri: Uri, prefsCache: PreferencesInCache) {
        _taskDetail.update {
            if (uri != errUri) {
                viewModelScope.launch {
                    this@TaskDetailDialogViewModel.setCache(uri, prefsCache)
                    this@TaskDetailDialogViewModel.deleteMediaCache(uri, prefsCache)
                }
                it?.copy(dataByte = uri.toByteArray())
            } else {
                val errTypeText = " - " + TaskDetailType.Picture.name
                val err = (uri.toByteArray()) + errTypeText.toByteArray()
                it?.copy(type = TaskDetailType.Text, dataByte = err)
            }
        }
    }

    private val ipv4Regex = Regex(
        """^(?:25[0-5]|2[0-4]\d|1?\d?\d)(?:\.(?:25[0-5]|2[0-4]\d|1?\d?\d)){3}$"""
    )

    private val ipv6UrlRegex = Regex(
        """^[A-Za-z][A-Za-z0-9+.\-]*://""" +
                """(?:[A-Za-z0-9._~!$&'()*+,;=:%\-]+@)?""" +
                """\[""" +
                """(?:(?:[0-9A-Fa-f]{1,4}:){7}[0-9A-Fa-f]{1,4}|""" +
                """(?:[0-9A-Fa-f]{1,4}:){1,7}:|""" +
                """(?:[0-9A-Fa-f]{1,4}:){1,6}:[0-9A-Fa-f]{1,4}|""" +
                """(?:[0-9A-Fa-f]{1,4}:){1,5}(?::[0-9A-Fa-f]{1,4}){1,2}|""" +
                """(?:[0-9A-Fa-f]{1,4}:){1,4}(?::[0-9A-Fa-f]{1,4}){1,3}|""" +
                """(?:[0-9A-Fa-f]{1,4}:){1,3}(?::[0-9A-Fa-f]{1,4}){1,4}|""" +
                """(?:[0-9A-Fa-f]{1,4}:){1,2}(?::[0-9A-Fa-f]{1,4}){1,5}|""" +
                """[0-9A-Fa-f]{1,4}:(?::[0-9A-Fa-f]{1,4}){1,6}|""" +
                """:(?::[0-9A-Fa-f]{1,4}){1,7}|::|""" +
                """(?:[0-9A-Fa-f]{1,4}:){6}(?:25[0-5]|2[0-4]\d|1?\d?\d)(?:\.(?:25[0-5]|2[0-4]\d|1?\d?\d)){3}|""" +
                """(?:[0-9A-Fa-f]{1,4}:){0,5}:(?:25[0-5]|2[0-4]\d|1?\d?\d)(?:\.(?:25[0-5]|2[0-4]\d|1?\d?\d)){3}|""" +
                """::(?:[0-9A-Fa-f]{1,4}:){0,5}(?:25[0-5]|2[0-4]\d|1?\d?\d)(?:\.(?:25[0-5]|2[0-4]\d|1?\d?\d)){3})""" +
                """(?:%25[0-9A-Za-z._~\-]+)?""" +
                """]""" +
                """(?::\d{1,5})?""" +
                """(?:[/?#]\S*)?$""",
        RegexOption.IGNORE_CASE
    )

    /**
     * Handle [TaskDetailDialog] non-listed dialog confirm function
     * @param getType from the [com.sqz.checklist.ui.common.dialog.DialogWithMenu] confirm lambda,
     * expected that able to convert code: `val example = type as TaskDetailType?`.
     * @param textFieldState the text from [TaskDetailDialog] non-listed dialog.
     * @param onIssue for show the issue, if `null` means general issue.
     * @param finalConfirm function ran correctly, move to next.
     */
    fun onTaskDetailConfirm(
        getType: Any?,
        textFieldState: TextFieldState,
        onIssue: (type: TaskDetailType?) -> Unit,
        finalConfirm: () -> Unit
    ) {
        fun getTextStateStr() = textFieldState.text.toString()
        val type = getType as TaskDetailType?
        if (type != null) {
            when (type) {
                TaskDetailType.URL -> {
                    val isIpv6URL = getTextStateStr().matches(ipv6UrlRegex)
                    val isURL = Patterns.WEB_URL.matcher(getTextStateStr()).matches() || isIpv6URL
                    if (!isURL) onIssue(TaskDetailType.URL) else {
                        val getStr = if (!getTextStateStr().startsWith("http") && !isIpv6URL) {
                            if (getTextStateStr().matches(ipv4Regex)) {
                                textFieldState.edit { insert(0, "http://") }
                            } else textFieldState.edit { insert(0, "https://") }
                            getTextStateStr()
                        } else getTextStateStr()
                        this.setTaskDetail { update ->
                            update?.copy(
                                type = type, dataByte = getStr.toByteArray(Charsets.UTF_8)
                            )
                        }
                        finalConfirm()
                    }
                }

                TaskDetailType.Application -> if (getTextStateStr().isEmpty()) onIssue(null) else {
                    this.setTaskDetail { update ->
                        update?.copy(
                            type = type, dataByte = getTextStateStr().toByteArray(Charsets.UTF_8)
                        )
                    }
                    finalConfirm()
                }

                TaskDetailType.Picture -> try {
                    val selector = this.functionalSaver as PictureSelector
                    if (selector.dataUri.value == null) onIssue(null) else {
                        this.setTaskDetail { update ->
                            update?.copy(
                                type = type, dataString = selector.pictureName.value,
                                dataByte = selector.dataUri.value!!
                            )
                        }
                        finalConfirm()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                TaskDetailType.Video -> try {
                    val selector = this.functionalSaver as VideoSelector
                    if (selector.dataUri.value == null) onIssue(null) else {
                        this.setTaskDetail { update ->
                            update?.copy(
                                type = type, dataString = selector.videoName.value,
                                dataByte = selector.dataUri.value!!
                            )
                        }
                        finalConfirm()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                TaskDetailType.Audio -> try {
                    val selector = this.functionalSaver as AudioSelector
                    if (selector.dataUri.value == null) onIssue(null) else {
                        this.setTaskDetail { update ->
                            update?.copy(
                                type = type, dataString = selector.audioName.value,
                                dataByte = selector.dataUri.value!!
                            )
                        }
                        finalConfirm()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                else -> if (getTextStateStr().isEmpty()) onIssue(null) else {
                    this.setTaskDetail { update ->
                        update?.copy(
                            type = type, dataByte = getTextStateStr().toByteArray(Charsets.UTF_8)
                        )
                    }
                    finalConfirm()
                }
            }
        } else onIssue(null)
    }

    /**
     * Handle when selected [TaskDetailType] changed
     * @param getType from the [com.sqz.checklist.ui.common.dialog.DialogWithMenu] confirm lambda,
     * expected that able to convert code: `val example = type as TaskDetailType?`.
     * @param textFieldState the text from [TaskDetailDialog] non-listed dialog.
     */
    fun onMenuSelectionChanged(getType: Any?, textFieldState: TextFieldState) {
        if (getType != null && this.taskDetail.value?.type != getType) {
            this.functionalSaver = null
            this.setTaskDetail { null }
            textFieldState.clearText()
        }
        val type = getType as TaskDetailType?
        type?.let { this.setTaskDetail(type = type) }
    }
}
