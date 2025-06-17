package com.sqz.checklist.ui.main.task.layout.function

data class CheckDataState(
    val onCheckTask: Boolean = false,
    val toUndoId: Long = -1,
    val rememberScroll: Int? = null,
    val rememberScrollIndex: Int? = null,
)
