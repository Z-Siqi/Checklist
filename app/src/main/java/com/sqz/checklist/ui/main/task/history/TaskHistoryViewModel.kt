package com.sqz.checklist.ui.main.task.history

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

class TaskHistoryViewModel : ViewModel() {
    /**
     * Task History select state
     **/
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
