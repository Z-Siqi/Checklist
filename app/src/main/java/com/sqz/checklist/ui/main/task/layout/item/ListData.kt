package com.sqz.checklist.ui.main.task.layout.item

import com.sqz.checklist.database.TaskViewData

data class ListData(
    val unLoading: Boolean = true,
    val item: List<TaskViewData> = listOf(),
    val pinnedItem: List<TaskViewData> = listOf(),
    val isRemindedItem: List<TaskViewData> = listOf(),
    val inSearchItem: List<TaskViewData> = listOf(),
    val searchView: Boolean = false,
)
