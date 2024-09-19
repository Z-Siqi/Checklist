package com.sqz.checklist.ui.main.task.history

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sqz.checklist.MainActivity
import com.sqz.checklist.database.Task
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
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
    fun doAllTask(doAllTaskAction: DoAllTaskAction) = viewModelScope.launch {
        when (doAllTaskAction) {
            DoAllTaskAction.Redo -> MainActivity.taskDatabase.taskDao().setAllNotHistory()
            DoAllTaskAction.Delete -> MainActivity.taskDatabase.taskDao().deleteAllHistory()
        }
        // Update to LazyColumn
        updateTaskHistoryData()
    }

    enum class DoAllTaskAction { Redo, Delete }

    /** Delete action **/
    fun deleteTask(id: Int) = viewModelScope.launch {
        // Actions
        MainActivity.taskDatabase.taskDao().delete(
            Task(id = id, description = "", createDate = LocalDate.MIN)
        )
        arrangeHistoryId()
        // Update to LazyColumn
        updateTaskHistoryData()
    }

    /** Undo to history **/
    fun changeTaskVisibilityAsUndo(id: Int) = viewModelScope.launch { // Actions
        MainActivity.taskDatabase.taskDao().setHistory(0, id)
        MainActivity.taskDatabase.taskDao().setHistoryId(0, id)
        arrangeHistoryId()
        // Update to LazyColumn
        updateTaskHistoryData()
    }

    /** Select task by id **/
    fun selectTask(id: Int) {
        if (!onSelect) {
            selectedId = id
            onSelect = true
        } else if (selectedId == id) {
            selectedId = -0
            onSelect = false
        } else {
            selectedId = id
        }
    }

    var selectedId by mutableIntStateOf(-0)
    var onSelect by mutableStateOf(false)
    var hideSelected by mutableStateOf(false)

    /** Reset select state (selectedId, onSelect, hideSelected) **/
    fun resetSelect() {
        this.selectedId = -0
        this.onSelect = false
        this.hideSelected = false
    }
}
