package com.sqz.checklist.ui.nav.group.home

import androidx.lifecycle.ViewModel
import com.sqz.checklist.ui.nav.group.home.button.TaskExtendedButton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class HomeNavGroupViewModel: ViewModel(), HomeNavGroupInterface {

    // === TaskExtendedButton ===

    private val _taskNavState = MutableStateFlow<TaskExtendedButton.State?>(null)

    fun getTaskTypeState(): StateFlow<TaskExtendedButton.State?> {
        return _taskNavState.asStateFlow()
    }

    override fun updateState(taskExtendedButton: TaskExtendedButton.State) {
        _taskNavState.value = taskExtendedButton
    }

    val taskNavRequest = MutableStateFlow<TaskExtendedButton.ClickRequest?>(null)

    override fun getTaskTypeRequest(): StateFlow<TaskExtendedButton.ClickRequest?> {
        return taskNavRequest.asStateFlow()
    }

    override fun resetRequest() {
        taskNavRequest.value = null
    }
}
