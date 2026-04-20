package sqz.checklist.data.database.repository.reminder

import sqz.checklist.data.database.DatabaseProvider
import sqz.checklist.data.storage.manager.StorageManager

internal class TaskReminderRepositoryImpl(
    private val db: DatabaseProvider,
    private val storageManager: StorageManager
)  : TaskReminderRepository {

    private fun reminderDao() = db.getDatabase().taskReminderDao()

    override suspend fun deleteRemindedInfo(taskId: Long) {
        TODO("Not yet implemented")
    }
}
