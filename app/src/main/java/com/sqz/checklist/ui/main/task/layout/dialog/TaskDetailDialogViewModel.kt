package com.sqz.checklist.ui.main.task.layout.dialog

import android.net.Uri
import android.util.Patterns
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.insert
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
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TaskDetailDialogViewModel : ViewModel() {
    private lateinit var _taskDetailList: MutableStateFlow<List<TaskDetailData>>

    /** Init method with parameter **/
    fun init(taskDetailData: List<TaskDetailData>) { // Set list from the database
        this._taskDetailList = MutableStateFlow(taskDetailData)
        if (taskDetailData.size == 1) { // if is a single task detail
            _taskDetail.update { this._taskDetailList.value.last() }
            this.removeTaskDetailListItem(taskDetailData.size - 1)
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
        super.onCleared()
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
                viewModelScope.launch { setCache(uri, prefsCache) }
                it?.copy(dataByte = uri.toByteArray())
            } else {
                val errTypeText = " - " + TaskDetailType.Picture.name
                val err = (uri.toByteArray()) + errTypeText.toByteArray()
                it?.copy(type = TaskDetailType.Text, dataByte = err)
            }
        }
    }

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
                    if (!Patterns.WEB_URL.matcher(textFieldState.text.toString()).matches()) {
                        onIssue(TaskDetailType.URL)
                    } else {
                        val getStr = if (getTextStateStr().startsWith("http")) {
                            textFieldState.edit { insert(0, "http://") }
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
