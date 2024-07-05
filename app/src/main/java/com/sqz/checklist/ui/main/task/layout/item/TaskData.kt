package com.sqz.checklist.ui.main.task.layout.item

import java.time.LocalDate

data class TaskData(
    val id: Int,
    val description: String,
    val createDate: LocalDate,
    val reminder: String?
)
