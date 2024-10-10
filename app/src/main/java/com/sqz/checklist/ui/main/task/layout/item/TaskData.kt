package com.sqz.checklist.ui.main.task.layout.item

import com.sqz.checklist.ui.reminder.ReminderData

data class TaskData(
    val reminder: ReminderData = ReminderData(),
    val editState: EditState = EditState(),
)
