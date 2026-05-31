package com.sqz.checklist.presentation.history.task

sealed interface TaskHistoryState {

    data object None : TaskHistoryState

    data object Delete : TaskHistoryState

    data object Redo : TaskHistoryState
}
