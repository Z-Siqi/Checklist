package com.sqz.checklist.ui.main.task.layout.check

data class CheckDataState(
    val checkTaskAction: Boolean = false,
    val undoActionId: Int = -1,
    val undoButtonState: Boolean = false,
)
