package com.sqz.checklist.ui.main.task.handler

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.sqz.checklist.MainActivity
import com.sqz.checklist.cache.deleteAllFileWhichInProcessFilesPath
import com.sqz.checklist.database.DatabaseRepository
import com.sqz.checklist.database.Table
import com.sqz.checklist.database.Task
import com.sqz.checklist.database.TaskData
import com.sqz.checklist.database.TaskDetail
import com.sqz.checklist.database.TaskDetailData
import com.sqz.checklist.database.TaskDetailType
import com.sqz.checklist.notification.NotifyManager
import com.sqz.checklist.preferences.PreferencesInCache
import com.sqz.checklist.ui.main.task.TaskLayoutViewModel
import com.sqz.checklist.ui.main.task.layout.dialog.EditTask
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UnknownFormatConversionException

class ModifyHandler private constructor(
    private val reminderHandler: ReminderHandler,
    private val coroutineScope: CoroutineScope,
    private val requestUpdate: MutableStateFlow<Boolean>
) {
    companion object {
        fun instance(
            viewModel: TaskLayoutViewModel, requestUpdate: MutableStateFlow<Boolean>
        ): ModifyHandler = ModifyHandler(
            viewModel.reminderHandler, viewModel.viewModelScope, requestUpdate
        )
    }

    private fun database(): DatabaseRepository = DatabaseRepository(
        MainActivity.taskDatabase
    )

    private val _inModify: MutableStateFlow<EditTask?> = MutableStateFlow(null)
    val inModifyTask: StateFlow<EditTask?> = _inModify.asStateFlow()

    /**
     * Requests to edit a task.
     *
     * This function prepares a task for editing by fetching its data and details from the database.
     * The prepared task is then exposed through the `_inModify` StateFlow, allowing UI components
     * to observe it and display the editing interface. If the provided task is null, it clears the
     * current modification state.
     *
     * @param task if it is `null`, clear the current modification state, otherwise prepare
     * the task for editing.
     * @param context if it is not `null`, clear the current modification state, otherwise prepare
     * the task for editing.
     */
    fun requestEditTask(task: Task?, context: Context? = null) {
        coroutineScope.launch {
            if (task != null) {
                val taskData = TaskData(
                    description = task.description,
                    doingState = task.doingState,
                    isPin = task.isPin
                )
                val getDetailData = database().getDetailData(task.id)
                if (getDetailData == null) _inModify.update { // if not detail
                    EditTask(taskId = task.id, task = taskData)
                } else { // if got any detail
                    _inModify.update {
                        EditTask(taskId = task.id, task = taskData, detailGetter = listOf())
                    }
                    for (i in getDetailData) {
                        val taskDetail = TaskDetailData(
                            detailId = i.id,
                            type = i.type,
                            description = i.description,
                            dataString = i.dataString,
                            dataByte = i.dataByte,
                        )
                        _inModify.update {
                            it!!.copy(detailGetter = it.detailGetter?.plus(taskDetail))
                        }
                    }
                }
            } else _inModify.update {
                if (context != null) _inModify.value!!.detailGetter?.let { detail ->
                    dropValidInProcessFilesPath(
                        detail = detail,
                        prefsCache = PreferencesInCache(context)
                    )
                    deleteAllFileWhichInProcessFilesPath(context)
                }
                null
            }
        }
    }

    /**
     * Removes file paths from the in-process cache that are now associated with a saved task detail.
     *
     * When files (like images, audio, etc.) are added to a task, their paths are temporarily
     * stored in a cache (`inProcessFilesPath`). This function cleans up that cache by removing
     * the paths of files that have been successfully saved as part of a task's details. This
     * prevents them from being garbage-collected as unused files.
     *
     * @param detail The list of [TaskDetailData] that have been saved to the database.
     * @param prefsCache The [PreferencesInCache] instance used to access the cached file paths.
     */
    private suspend fun dropValidInProcessFilesPath(
        detail: List<TaskDetailData>,
        prefsCache: PreferencesInCache
    ) = withContext(Dispatchers.Main) {
        try {
            val lines = prefsCache.inProcessFilesPath()!!.lineSequence().toList()
            val check = lines.filter { line ->
                for (i in detail) {
                    when (i.type) {
                        TaskDetailType.Text -> continue
                        TaskDetailType.URL -> continue
                        TaskDetailType.Application -> continue
                        TaskDetailType.Audio -> {}
                        TaskDetailType.Picture -> {}
                        TaskDetailType.Video -> {}
                    }
                    if ((i.dataByte as ByteArray).toString(Charsets.UTF_8) == line) {
                        return@filter false
                    }
                }
                return@filter true
            }
            prefsCache.inProcessFilesPath(null)
            for (i in check) {
                if (!i.isBlank()) prefsCache.inProcessFilesPath(i + "\n")
            }
        } catch (_: Exception) {
        }
    }

    /** Insert task to database **/
    fun createTask(
        task: TaskData,
        detail: List<TaskDetailData>?,
        prefsCache: PreferencesInCache,
        requestReminder: Boolean = false,
        onRequestReminderToast: () -> Unit
    ) {
        coroutineScope.launch {
            if (!requestReminder) database().insertTask(task = task, detail = detail) else {
                val insertTask = database().insertTask(task = task, detail = detail)
                reminderHandler.requestReminder(insertTask, false).also {
                    onRequestReminderToast()
                }
            }
            requestUpdate.update { true }
            if (detail != null && detail.isNotEmpty()) {
                dropValidInProcessFilesPath(detail = detail, prefsCache = prefsCache)
                deleteAllFileWhichInProcessFilesPath(prefsCache = prefsCache)
            }
        }
    }

    /**
     * Updates the details of a task in the database based on the changes made during editing.
     *
     * This function compares the original task details (`detailGetter`) with the new details
     * (`detailSetter`) from the `onEdit` object and applies the necessary changes (insert, update, delete)
     * to the database.
     *
     * It handles several scenarios:
     * - No details exist or no changes were made.
     * - All details were deleted.
     * - New details were added to a task that previously had none.
     * - Existing details were modified, added, or removed.
     *
     * @param onEdit The [EditTask] object containing the original and modified states of the task details.
     */
    private suspend fun updateTaskDetail(onEdit: EditTask) {
        when {
            onEdit.detailSetter == null && onEdit.detailGetter == null -> return // nothing there
            onEdit.detailSetter == onEdit.detailGetter -> return // all same
        }
        if (onEdit.detailSetter == null) { // when deleted all
            val originalTaskDetailList = database().getTable(Table.TaskDetail, onEdit.taskId)
            for (i in originalTaskDetailList) {
                val originalTaskDetail = i as TaskDetail
                database().deleteTaskDetail(originalTaskDetail)
            }
            return
        }
        if (onEdit.detailGetter == null) { // when add any task detail
            database().insertTaskDetail(taskId = onEdit.taskId, taskDetail = onEdit.detailSetter)
            return
        }
        // when edit any task detail
        val originalTaskDetailList = database().getTable(Table.TaskDetail, onEdit.taskId)
        if (originalTaskDetailList.size >= onEdit.detailSetter.size) { // original size is same or larger
            for (i in originalTaskDetailList.indices) {
                val originalTaskDetail = originalTaskDetailList[i] as TaskDetail
                try {
                    val taskDetail = onEdit.detailSetter[i]
                    if (taskDetail == originalTaskDetail) {
                        continue
                    }
                    database().updateTaskDetail(
                        original = originalTaskDetail,
                        update = originalTaskDetail.copy(
                            type = taskDetail.type,
                            description = taskDetail.description,
                            dataString = taskDetail.dataString,
                            dataByte = taskDetail.dataByte as ByteArray,
                        )
                    )
                } catch (_: IndexOutOfBoundsException) {
                    database().deleteTaskDetail(originalTaskDetail)
                }
            }
            return
        } else for (i in onEdit.detailSetter.indices) { // original size is smaller
            val taskDetail = onEdit.detailSetter[i]
            try {
                val originalTaskDetail = originalTaskDetailList[i] as TaskDetail
                if (taskDetail == originalTaskDetail) {
                    continue
                }
                database().updateTaskDetail(
                    original = originalTaskDetail,
                    update = originalTaskDetail.copy(
                        type = taskDetail.type,
                        description = taskDetail.description,
                        dataString = taskDetail.dataString,
                        dataByte = taskDetail.dataByte as ByteArray,
                    )
                )
            } catch (_: IndexOutOfBoundsException) {
                database().insertTaskDetail(taskId = onEdit.taskId, taskDetail = taskDetail)
            }
            return
        }
        throw UnknownFormatConversionException("ModifyHandler.updateTaskDetail: Unknown error")
    }

    /** Edit task **/
    fun editTask(onEdit: EditTask, context: Context) {
        coroutineScope.launch {
            val originalTask = (database().getTable(Table.Task, onEdit.taskId)[0]) as Task
            database().updateTask(
                originalTask = originalTask,
                updateTask = originalTask.copy(
                    description = onEdit.task.description!!,
                    doingState = onEdit.task.doingState,
                    isPin = onEdit.task.isPin!!
                )
            )
            val prefsCache = PreferencesInCache(context)
            if (onEdit.detailSetter != null || onEdit.detailGetter != null) {
                updateTaskDetail(onEdit = onEdit)
                onEdit.detailSetter?.let {
                    dropValidInProcessFilesPath(detail = it, prefsCache = prefsCache)
                }
            }
            database().getReminderData(taskId = onEdit.taskId)?.let {
                if (NotifyManager.isNotificationExist(it.id, context)) {
                    reminderHandler.updateNotification(it.id, onEdit.task, context)
                }
            }
            requestUpdate.update { true }
            _inModify.update { null }
            if (onEdit.detailSetter != null) {
                deleteAllFileWhichInProcessFilesPath(prefsCache = prefsCache)
            }
        }
    }

    /** Set task pin **/
    fun pinState(id: Long, set: Boolean) = coroutineScope.launch {
        database().taskPin(id, set)
        requestUpdate.update { true }
    }

    /** Delete to history **/
    fun onTaskChecked(id: Long) = coroutineScope.launch {
        database().isHistoryIdToHistory(id)
        requestUpdate.update { true }
    }

    /** Undo to history **/
    fun onTaskUndoChecked(id: Long) = coroutineScope.launch {
        database().isHistoryIdToMain(id)
        requestUpdate.update { true }
    }
}
