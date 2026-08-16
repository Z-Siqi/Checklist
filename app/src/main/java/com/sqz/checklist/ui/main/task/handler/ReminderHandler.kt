package com.sqz.checklist.ui.main.task.handler

import androidx.lifecycle.viewModelScope
import com.sqz.checklist.ui.main.task.TaskLayoutViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow

@Deprecated("TODO: Refactoring this")
class ReminderHandler private constructor(
    private val coroutineScope: CoroutineScope,
    private val initState: MutableStateFlow<Boolean>,
) {
    companion object {
        fun instance(
            viewModel: TaskLayoutViewModel,
            initState: MutableStateFlow<Boolean>
        ): ReminderHandler = ReminderHandler(
            coroutineScope = viewModel.viewModelScope,
            initState = initState
        )
    }
}
