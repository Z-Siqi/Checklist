package sqz.checklist.data.database.repository.reminder

import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import sqz.checklist.data.database.Task
import sqz.checklist.data.database.TaskReminder
import sqz.checklist.data.database.model.ReminderViewData
import sqz.checklist.data.database.model.TaskViewData
import kotlin.time.Clock

class TaskReminderRepositoryFake : TaskReminderRepository {
    override suspend fun getRemindedTaskList(): List<TaskViewData> {
        val task1 = Task(
            id = 1, description = "Task 1",
            createDate = Clock.System.todayIn(TimeZone.currentSystemDefault()),
            doingState = null, isPin = true,
        )
        val task2 = task1.copy(id = 2, description = "Task 2")
        val taskView1 = TaskViewData(
            task = task1, isDetailExist = false, isReminded = false, reminderTime = null
        )
        val taskView2 = taskView1.copy(task = task2)
        return listOf(taskView1, taskView2)
    }

    override suspend fun getReminderViewList(noRemindedOnly: Boolean): List<ReminderViewData> {
        return listOf()
    }

    override suspend fun getReminderView(notifyId: Int): ReminderViewData? {
        return null
    }

    override suspend fun getReminder(taskId: Long): TaskReminder? {
        return null
    }

    override suspend fun updateRemindedState(notifyId: Int, isReminded: Boolean) {
        println("updateRemindedState: notifyId = $notifyId, isReminded = $isReminded")
    }

    override suspend fun deleteRemindedInfo(taskId: Long): TaskReminder? {
        println("deleteRemindedInfo: taskId = $taskId")
        return null
    }

    override suspend fun deleteReminder(notifyId: Int) {
        println("deleteReminder: notifyId = $notifyId")
    }

    override suspend fun insertReminder(reminder: TaskReminder) {
        println("insertReminder: reminder = $reminder")
    }
}
