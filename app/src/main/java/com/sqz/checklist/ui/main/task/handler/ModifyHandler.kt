package com.sqz.checklist.ui.main.task.handler

import androidx.lifecycle.viewModelScope
import com.sqz.checklist.MainActivity
import com.sqz.checklist.ui.main.task.TaskLayoutViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import sqz.checklist.data.database.repository.DatabaseRepository

class ModifyHandler private constructor(
    private val coroutineScope: CoroutineScope,
    private val requestUpdate: MutableStateFlow<Boolean>
) {
    companion object {
        fun instance(
            viewModel: TaskLayoutViewModel, requestUpdate: MutableStateFlow<Boolean>
        ): ModifyHandler = ModifyHandler(
            viewModel.viewModelScope, requestUpdate
        )
    }

    private fun database(): DatabaseRepository = DatabaseRepository(
        MainActivity.taskDatabase.getDatabase()
    )

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
