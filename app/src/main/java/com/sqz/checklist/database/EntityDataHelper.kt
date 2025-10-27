package com.sqz.checklist.database

import androidx.room.Embedded

data class TaskViewData(
    @Embedded val task: Task,
    val isDetailExist: Boolean,
    val isReminded: Boolean,
    val reminderTime: Long?,
)

data class HistoryIdList(
    val id: Long,
    val isHistoryId: Int,
)

enum class Table {
    Task, TaskDetail, TaskReminder
}

data class ReminderViewData(
    @Embedded val reminder: TaskReminder,
    val taskDescription: String,
)
