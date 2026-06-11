package com.sqz.checklist.ui.nav.group.home

import com.sqz.checklist.ui.nav.group.home.button.TaskExtendedButton
import kotlinx.coroutines.flow.StateFlow

interface HomeNavGroupInterface {

    fun updateState(taskExtendedButton: TaskExtendedButton.State)

    fun getTaskTypeRequest(): StateFlow<TaskExtendedButton.ClickRequest?>

    fun resetRequest()
}
