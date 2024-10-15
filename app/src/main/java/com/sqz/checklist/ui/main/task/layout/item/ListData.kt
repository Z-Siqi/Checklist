package com.sqz.checklist.ui.main.task.layout.item

import com.sqz.checklist.database.Task

data class ListData(
    val item: List<Task> = listOf(),
    val pinnedItem: List<Task> = listOf(),
    val isRemindedItem: List<Task> = listOf(),
    val inSearchItem: List<Task> = listOf(),
    val searchView: Boolean = false
)
