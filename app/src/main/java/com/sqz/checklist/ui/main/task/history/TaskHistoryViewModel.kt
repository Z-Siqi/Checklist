package com.sqz.checklist.ui.main.task.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sqz.checklist.MainActivity
import com.sqz.checklist.database.Task
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

class TaskHistoryViewModel : ViewModel() {

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
            DoTaskAction.Delete -> MainActivity.taskDatabase.taskDao().deleteAllHistory()
        }
        // Update to LazyColumn
        updateTaskHistoryData()
    }

    enum class DoTaskAction { Redo, Delete }

    /** Control the History Selection State **/
    private val _selectState = MutableStateFlow(SelectData())
    val selectState: StateFlow<SelectData> = _selectState.asStateFlow()
    fun setSelectTask(id: Int) {
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
    fun removeFromHistory(action: DoTaskAction, id: Int) = viewModelScope.launch {
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
    private fun deleteTask(id: Int) = viewModelScope.launch {
        // Actions
        MainActivity.taskDatabase.taskDao().delete(
            Task(id = id, description = "", createDate = LocalDate.MIN)
        )
        arrangeHistoryId()
        // Update to LazyColumn
        updateTaskHistoryData()
    }

    /** Undo to history **/
    private fun changeTaskVisibilityAsUndo(id: Int) = viewModelScope.launch {
        // Actions
        MainActivity.taskDatabase.taskDao().setHistory(0, id)
        MainActivity.taskDatabase.taskDao().setHistoryId(0, id)
        arrangeHistoryId()
        // Update to LazyColumn
        updateTaskHistoryData()
    }
}
