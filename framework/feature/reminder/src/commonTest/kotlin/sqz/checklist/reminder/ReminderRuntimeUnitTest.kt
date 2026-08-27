package sqz.checklist.reminder

import kotlinx.coroutines.test.runTest
import sqz.checklist.reminder.api.reminderRuntimeProvider
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ReminderRuntimeUnitTest {

    @Test
    fun getReminder_returnsViewAndNullForUnknownTask() = runTest {
        val repository = FakeReminderRepository().apply { put(reminder(7, 42), "task") }
        val subject = reminderRuntimeProvider(repository, FakeNotificationScheduler())

        assertEquals(7, subject.getReminder(42)?.reminder?.id)
        assertNull(subject.getReminder(404))
    }

    @Test
    fun hasPendingReminder_distinguishesPendingFromTriggeredRows() = runTest {
        val repository = FakeReminderRepository().apply { put(reminder(7, 42, isReminded = true)) }
        val subject = reminderRuntimeProvider(repository, FakeNotificationScheduler())

        assertFalse(subject.hasPendingReminder())
        repository.put(reminder(8, 43))
        assertTrue(subject.hasPendingReminder())
    }

    @Test
    fun removeReminder_cancelsSchedulerBeforeDeletingData() = runTest {
        val repository = FakeReminderRepository().apply { put(reminder(7, 42)) }
        val scheduler = FakeNotificationScheduler().apply { existing += 7 }
        val subject = reminderRuntimeProvider(repository, scheduler)

        assertEquals(7, subject.removeReminder(42, true)?.id)
        assertNull(repository.getReminder(42))
        assertFalse(7 in scheduler.existing)
        assertEquals(listOf(7), scheduler.cancelled)
    }
}
