package com.sqz.checklist.ui.main.history.task

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sqz.checklist.MainActivity
import com.sqz.checklist.database.DatabaseRepository
import com.sqz.checklist.database.Task
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TaskHistoryViewModel: ViewModel() {
    private fun database(): DatabaseRepository = DatabaseRepository(
        MainActivity.taskDatabase
    )

    /** Load history task **/
    private val _taskHistoryData = MutableStateFlow(listOf<Task>())
    val taskHistoryData: MutableStateFlow<List<Task>> = _taskHistoryData
    fun updateTaskHistoryData() {
        viewModelScope.launch {
            _taskHistoryData.update {
                MainActivity.taskDatabase.taskDao().getAllOrderByIsHistoryId()
            }
        }
    }

    /** Redo or Delete all task from history **/
    fun doAllTask(doAllTaskAction: DoTaskAction) = viewModelScope.launch {
        when (doAllTaskAction) {
            DoTaskAction.Redo -> MainActivity.taskDatabase.taskDao().setAllNotHistory()
            DoTaskAction.Delete -> database().deleteAllHistory()
        }
        // Update to LazyColumn
        updateTaskHistoryData()
    }

    enum class DoTaskAction { Redo, Delete }

    /** Control the History Selection State **/
    private val _selectState = MutableStateFlow(SelectData())
    val selectState: StateFlow<SelectData> = _selectState.asStateFlow()
    fun setSelectTask(id: Long) {
        _selectState.update {
            if (!it.onSelect) {
                it.copy(
                    selectedId = id,
                    onSelect = true
                )
            } else if (it.selectedId == id) {
                it.copy(
                    selectedId = -0,
                    onSelect = false
                )
            } else {
                it.copy(selectedId = id)
            }
        }
    }

    /** Reset Select State as Default **/
    fun resetSelectState() {
        _selectState.value = SelectData()
    }

    /** Delete or Undo to history as id **/
    fun removeFromHistory(action: DoTaskAction, id: Long) = viewModelScope.launch {
        _selectState.update { // Hide before remove is for animation
            it.copy(hideSelected = true)
        }
        delay(80)
        when (action) {
            DoTaskAction.Delete -> deleteTask(id)
            DoTaskAction.Redo -> changeTaskVisibilityAsUndo(id)
        }
        delay(20)
        resetSelectState()
    }

    /** Delete action **/
    private suspend fun deleteTask(id: Long) {
        // Actions
        database().deleteTask(id)
        arrangeHistoryId()
        // Update to LazyColumn
        updateTaskHistoryData()
    }

    /** Undo to history **/
    private suspend fun changeTaskVisibilityAsUndo(id: Long) {
        // Actions
        MainActivity.taskDatabase.taskDao().setHistoryId(0, id)
        arrangeHistoryId()
        // Update to LazyColumn
        updateTaskHistoryData()
    }
}
