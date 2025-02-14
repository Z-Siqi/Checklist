package com.sqz.checklist.ui.main.task.layout.function

data class CheckDataState(
    val checkTaskAction: Boolean = false,
    val undoActionId: Long = -1,
    val undoButtonState: Boolean = false,
)
