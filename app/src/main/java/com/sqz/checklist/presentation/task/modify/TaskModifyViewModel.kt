package com.sqz.checklist.presentation.task.modify

import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sqz.checklist.common.media.compressImageInPlace
import com.sqz.checklist.common.media.compressVideoInPlace
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okio.Path.Companion.toPath
import sqz.checklist.data.database.TaskDetailType
import sqz.checklist.data.database.repository.task.TaskRepository
import sqz.checklist.data.preferences.PrimaryPreferences
import sqz.checklist.data.storage.StorageHelper.isDataPath
import sqz.checklist.data.storage.manager.StorageManager
import sqz.checklist.task.api.TaskModify
import sqz.checklist.task.api.taskModifyProvider
import kotlin.math.roundToInt

class TaskModifyViewModel(
    taskRepository: TaskRepository,
    storageManager: StorageManager,
) : ViewModel() {
    private val _taskModify = taskModifyProvider(taskRepository, storageManager)

    val taskModify = _taskModify.getModifyState

    private val _showTaskDialog = MutableStateFlow(true)

    val showTaskDialog = _showTaskDialog.asStateFlow()

    val isModified: StateFlow<Boolean> = _taskModify.isModified

    fun switchDialog() {
        if (_showTaskDialog.value) { // switch to detail dialog
            _taskModify.getModifyState.value.detailState?.let {
                if (it.isEmpty()) {
                    _taskModify.detailHandler().addItem()
                } else if (it.size == 1) {
                    _taskModify.detailHandler().selectItem(0)
                }
            }
        }
        _showTaskDialog.value = !_showTaskDialog.value
    }

    private val _loadingPercentage = MutableStateFlow<Double?>(null)

    val loadingPercentage = _loadingPercentage.asStateFlow()

    fun updateLoadingPercentage(percentage: Double?) {
        _loadingPercentage.value = percentage
    }

    fun newTask(view: View) {
        try {
            _taskModify.makeNewTask()
        } catch (_: IllegalStateException) {
            Toast.makeText(
                view.context, "WARN: Already modifying a task!", Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun editTask(id: Long, view: View) {
        try {
            _taskModify.selectTask(id)
        } catch (_: IllegalStateException) {
            Toast.makeText(
                view.context, "WARN: Already modifying a task!", Toast.LENGTH_SHORT
            ).show()
        } catch (e: Exception) {
            Toast.makeText(
                view.context, "ERROR: ${e.message}", Toast.LENGTH_LONG
            ).show()
        }
    }

    fun taskDialogHandler(): TaskModify.Task? {
        return try {
            _taskModify.taskHandler()
        } catch (_: NullPointerException) {
            Log.e("TaskModifyViewModel", "TaskModify.Task not init!")
            null
        }
    }

    fun confirmModify(
        requestReminder: (taskId: Long) -> Unit,
        onFinished: (taskId: Long) -> Unit
    ) {
        _showTaskDialog.value = true
        viewModelScope.launch(Dispatchers.IO) {
            val taskStateType = _taskModify.getModifyState.value.taskState!!.type
            val taskId = _taskModify.confirmModify()
            if (taskStateType is TaskModify.Task.ModifyType.NewTask && taskStateType.withReminder) {
                requestReminder(taskId)
            }
            onFinished(taskId)
        }
        _compressedFile.value = listOf()
    }

    fun cancelModify() {
        _showTaskDialog.value = true
        _taskModify.cancelModify()
        _compressedFile.value = listOf()
    }

    fun onDismissRequest() {
        this.cancelModify()
    }


    //-----------------------
    // Detail list
    //-----------------------
    fun addItem() {
        _taskModify.detailHandler().addItem()
        // Warn: it is expected to throw exception if this method used to create first detail
        _taskModify.detailHandler().selectItem(
            _taskModify.getModifyState.value.detailState!!.lastIndex
        )
    }

    fun updateItemDescription(index: Int, text: String?) {
        _taskModify.detailHandler().updateItemDescription(index, text)
    }

    fun selectItemInList(index: Int) {
        _taskModify.detailHandler().selectItem(index)
    }

    fun moveItemTo(fromIndex: Int, toIndex: Int) {
        _taskModify.detailHandler().moveItem(fromIndex, toIndex)
    }

    fun removeListItem(index: Int) {
        _taskModify.detailHandler().removeItem(index)
    }


    //-----------------------
    // Selected detail item
    //-----------------------
    fun getIsDetailModified(): StateFlow<Boolean> {
        return _taskModify.detailHandler().isDetailModified
    }

    fun getDetailSelectedItem(): StateFlow<TaskModify.Detail.UIState?> {
        return _taskModify.detailHandler().getSelectedItem
    }

    fun detailItemToList() {
        _taskModify.detailHandler().unselectItem()
    }

    private fun unselectItem() {
        _taskModify.detailHandler().unselectItem()
        if (_taskModify.getModifyState.value.detailState!!.size == 1) {
            this.switchDialog()
        }
    }

    private val _compressedFile = MutableStateFlow(listOf<String>())

    private fun pictureCompressor(
        preference: PrimaryPreferences, typeState: TaskModify.Detail.TypeState.Picture
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            if (typeState.path.isDataPath()) {
                this@TaskModifyViewModel.unselectItem()
                return@launch
            }
            _taskModify.updateLoading(true)
            if (!_compressedFile.value.any { it == typeState.path }) compressImageInPlace(
                path = typeState.path.toPath(),
                compression = preference.pictureCompressionRate(),
            ).also {
                if (!it) Log.w("TaskModifyViewModel", "Picture compress failed!")
                _compressedFile.update { update -> update + typeState.path }
            }
            _taskModify.updateLoading(false)
            this@TaskModifyViewModel.unselectItem()
        }
    }

    private fun videoCompressor(
        preference: PrimaryPreferences, typeState: TaskModify.Detail.TypeState.Video
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            if (typeState.path.isDataPath()) {
                this@TaskModifyViewModel.unselectItem()
                return@launch
            }
            _taskModify.updateLoading(true)
            this@TaskModifyViewModel.updateLoadingPercentage(0.0)
            if (!_compressedFile.value.any { it == typeState.path }) compressVideoInPlace(
                sourcePath = typeState.path.toPath(),
                compressionRate = preference.videoCompressionRate(),
                onProgress = {
                    val format = (it * 100).roundToInt() / 100.0
                    this@TaskModifyViewModel.updateLoadingPercentage(format)
                },
            ).also {
                if (!it) Log.w("TaskModifyViewModel", "Picture compress failed!")
                _compressedFile.update { update -> update + typeState.path }
            }
            _taskModify.updateLoading(false)
            this@TaskModifyViewModel.updateLoadingPercentage(null)
            this@TaskModifyViewModel.unselectItem()
        }
    }

    fun onDetailItemConfirm(preference: PrimaryPreferences) {
        _taskModify.detailHandler().getSelectedItem.value?.let {
            when (it.typeState) {
                is TaskModify.Detail.TypeState.Picture -> {
                    val picture = it.typeState as TaskModify.Detail.TypeState.Picture
                    this.pictureCompressor(preference, picture)
                }

                is TaskModify.Detail.TypeState.Video -> {
                    val video = it.typeState as TaskModify.Detail.TypeState.Video
                    this.videoCompressor(preference, video)
                }

                else -> {
                    this.unselectItem()
                    return
                }
            }
        } ?: this.unselectItem()
    }

    fun onDetailItemDismiss() {
        if (_taskModify.getModifyState.value.detailState!!.size == 1) {
            this.switchDialog()
        }
        _taskModify.detailHandler().unselectItem(true)
    }

    fun onDetailItemRemove() {
        if (_taskModify.getModifyState.value.detailState!!.size == 1) {
            this.switchDialog()
        }
        _taskModify.detailHandler().removeItem(null)
    }

    fun onDetailItemDataChanged(new: TaskModify.Detail.TypeState) {
        _taskModify.detailHandler().getSelectedItem.value!!.typeState?.let {
            if (it::class != new::class) {
                throw IllegalStateException("Type should not changed during update type data!")
            }
        }
        _taskModify.detailHandler().updateSelectedData(new)
    }

    fun onDetailItemTypeChange(type: TaskDetailType) {
        _taskModify.detailHandler().updateSelectedType(type)
    }
}
