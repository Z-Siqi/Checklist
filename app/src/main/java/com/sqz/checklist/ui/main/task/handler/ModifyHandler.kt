package com.sqz.checklist.ui.main.task.handler

import android.content.Context
import android.util.Log
import androidx.lifecycle.viewModelScope
import com.sqz.checklist.MainActivity
import com.sqz.checklist.database.DatabaseRepository
import com.sqz.checklist.database.Task
import com.sqz.checklist.database.TaskDetail
import com.sqz.checklist.database.TaskDetailType
import com.sqz.checklist.notification.PermissionState
import com.sqz.checklist.ui.main.task.TaskLayoutViewModel
import com.sqz.checklist.ui.main.task.layout.function.TaskDetailData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

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

    private val _taskDetailDataSaver = TaskDetailData.instance()
    fun taskDetailDataSaver(): TaskDetailData = _taskDetailDataSaver

    data class InModify(
        val inModifyTask: Task? = null,
        val inModifyDetail: TaskDetail? = null,
    )

    private val _inModify: MutableStateFlow<InModify?> = MutableStateFlow(null)
    val inModifyTask: StateFlow<InModify?> = _inModify.asStateFlow()

    fun requestEditTask(task: Task?) = coroutineScope.launch {
        if (task != null) _inModify.update {
            InModify(inModifyTask = task, inModifyDetail = database().getDetailData(task.id))
        } else _inModify.update { null }
    }

    /** Insert task to database **/
    suspend fun insertTask(
        description: String, pin: Boolean = false,
        detailType: TaskDetailType?, detailDataString: String?, detailDataByteArray: ByteArray?
    ): Long {
        return database().insertTaskData(
            description, isPin = pin, detailType = detailType,
            detailDataString = detailDataString, dataByte = detailDataByteArray
        ).also { requestUpdate.update { true } }
    }

    /** Edit task **/
    fun editTask(
        edit: String, detailType: TaskDetailType?,
        detailDataString: String?, detailDataByteArray: ByteArray?,
        context: Context
    ) {
        coroutineScope.launch {
            val editId = _inModify.value!!.inModifyTask!!.id
            Log.d("TEST", "$editId")
            reminderHandler.notifyManager.requestPermission(context)
            database().editTask(
                editId, edit, detailType, detailDataString, detailDataByteArray
            )
            if (database().getReminderData(editId) != null) {
                val notify = reminderHandler.notifyManager.requestPermission(context)
                if (notify == PermissionState.Notification || notify == PermissionState.Both) reminderHandler.setReminder(
                    database().getReminderData(editId)!!.reminderTime - System.currentTimeMillis(),
                    TimeUnit.MILLISECONDS, editId,
                    MainActivity.taskDatabase.taskDao().getAll(editId).description,
                    context
                ) else reminderHandler.cancelReminder(
                    editId, database().getReminderData(editId)!!.id, context
                )
            }
            _taskDetailDataSaver.releaseMemory()
            requestUpdate.update { true }
            _inModify.update { null }
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
