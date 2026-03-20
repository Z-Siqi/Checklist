package sqz.checklist.data.database.model

import androidx.room.Embedded
import sqz.checklist.data.database.Task

data class TaskViewData(
    @Embedded val task: Task,
    val isDetailExist: Boolean,
    val isReminded: Boolean,
    val reminderTime: Long?,
)
