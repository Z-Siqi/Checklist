package sqz.checklist.data.database.model

import androidx.room.Embedded
import sqz.checklist.data.database.TaskReminder

data class ReminderViewData(
    @Embedded val reminder: TaskReminder,
    val taskDescription: String,
)
