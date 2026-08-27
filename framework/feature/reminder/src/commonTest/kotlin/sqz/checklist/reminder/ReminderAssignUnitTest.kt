package sqz.checklist.reminder

import kotlinx.coroutines.test.runTest
import sqz.checklist.common.NotificationScheduler
import sqz.checklist.data.database.TaskReminder
import sqz.checklist.data.database.model.ReminderViewData
import sqz.checklist.data.database.model.TaskViewData
import sqz.checklist.data.database.repository.reminder.TaskReminderRepository
import sqz.checklist.reminder.api.reminderAssignProvider
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ReminderAssignUnitTest {

    @Test
    fun getCurrentReminder_returnsJoinedViewForTask() = runTest {
        val repository = FakeReminderRepository().apply {
            put(reminder(id = 7, taskId = 42), "buy milk")
        }
        val subject = reminderAssignProvider(repository, FakeNotificationScheduler())

        assertEquals("buy milk", subject.getCurrentReminder(42)?.taskDescription)
        assertNull(subject.getCurrentReminder(404))
    }

    @Test
    fun setReminder_insertsThenSchedulesSameNonZeroId() = runTest {
        val repository = FakeReminderRepository()
        val scheduler = FakeNotificationScheduler()
        val subject = reminderAssignProvider(repository, scheduler)

        subject.setReminder(taskId = 42, remindAtMillis = 12_345)

        val inserted = repository.getReminder(42)!!
        assertNotEquals(0, inserted.id)
        assertEquals(12_345, inserted.reminderTime)
        assertEquals(listOf(inserted.id to 12_345L), scheduler.scheduled)
    }

    @Test
    fun cancelReminder_cancelsPlatformEntryBeforeDeletingData() = runTest {
        val repository = FakeReminderRepository().apply { put(reminder(id = 7, taskId = 42)) }
        val scheduler = FakeNotificationScheduler().apply { existing += 7 }
        val subject = reminderAssignProvider(repository, scheduler)

        assertTrue(subject.cancelReminder(42))
        assertEquals(listOf(7), scheduler.cancelled)
        assertNull(repository.getReminder(42))
        assertFalse(subject.cancelReminder(42))
    }

    @Test
    fun unsentError_isTrueWhenAnyPendingReminderIsMissingFromScheduler() = runTest {
        val repository = FakeReminderRepository().apply {
            put(reminder(id = 1, taskId = 11))
            put(reminder(id = 2, taskId = 22))
        }
        val scheduler = FakeNotificationScheduler().apply { existing += 1 }
        val subject = reminderAssignProvider(repository, scheduler)

        assertTrue(subject.checkUnsentReminderError())
        scheduler.existing += 2
        assertFalse(subject.checkUnsentReminderError())
    }

    @Test
    fun unsentError_ignoresAlreadyRemindedRows() = runTest {
        val repository = FakeReminderRepository().apply {
            put(reminder(id = 1, taskId = 11, isReminded = true))
        }
        val subject = reminderAssignProvider(repository, FakeNotificationScheduler())

        assertFalse(subject.checkUnsentReminderError())
    }
}

internal fun reminder(
    id: Int,
    taskId: Long,
    reminderTime: Long = 1_000,
    isReminded: Boolean = false,
) = TaskReminder(
    id = id,
    taskId = taskId,
    reminderTime = reminderTime,
    isReminded = isReminded,
)

internal class FakeReminderRepository : TaskReminderRepository {
    private val reminders = linkedMapOf<Int, ReminderViewData>()

    fun put(reminder: TaskReminder, description: String = "task ${reminder.taskId}") {
        reminders[reminder.id] = ReminderViewData(reminder, description)
    }

    override suspend fun getRemindedTaskList(): List<TaskViewData> = emptyList()

    override suspend fun getReminderViewList(noRemindedOnly: Boolean): List<ReminderViewData> =
        reminders.values.filter { !noRemindedOnly || !it.reminder.isReminded }

    override suspend fun getReminderView(notifyId: Int): ReminderViewData? = reminders[notifyId]

    override suspend fun getReminder(taskId: Long): TaskReminder? =
        reminders.values.firstOrNull { it.reminder.taskId == taskId }?.reminder

    override suspend fun updateRemindedState(notifyId: Int, isReminded: Boolean) {
        val view = reminders.getValue(notifyId)
        reminders[notifyId] = view.copy(reminder = view.reminder.copy(isReminded = isReminded))
    }

    override suspend fun deleteRemindedInfo(taskId: Long): TaskReminder {
        val reminder = getReminder(taskId) ?: throw NullPointerException("Reminder not found")
        if (!reminder.isReminded) throw IllegalArgumentException("Reminder is still pending")
        reminders.remove(reminder.id)
        return reminder
    }

    override suspend fun deleteReminder(notifyId: Int) {
        if (reminders.remove(notifyId) == null) throw NullPointerException("Reminder not found")
    }

    override suspend fun insertReminder(reminder: TaskReminder) = put(reminder)
}

internal class FakeNotificationScheduler : NotificationScheduler {
    val existing = mutableSetOf<Int>()
    val scheduled = mutableListOf<Pair<Int, Long>>()
    val cancelled = mutableListOf<Int>()

    override suspend fun schedule(notificationId: Int, remindAtMillis: Long, silent: Boolean) {
        scheduled += notificationId to remindAtMillis
        existing += notificationId
    }

    override suspend fun reschedule(notificationId: Int, silent: Boolean) {
        existing += notificationId
    }

    override suspend fun cancel(notificationId: Int, tryRemoveDisplayed: Boolean) {
        cancelled += notificationId
        existing -= notificationId
    }

    override suspend fun cancelAll(tryRemoveDisplayed: Boolean) {
        existing.clear()
    }

    override suspend fun isExisted(notificationId: Int): Boolean = notificationId in existing
}
