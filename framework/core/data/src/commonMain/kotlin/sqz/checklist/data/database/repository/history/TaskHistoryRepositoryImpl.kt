package sqz.checklist.data.database.repository.history

import kotlinx.coroutines.flow.Flow
import sqz.checklist.data.database.DatabaseProvider
import sqz.checklist.data.database.Task
import sqz.checklist.data.database.repository.deleteTaskDetailStorageFile
import sqz.checklist.data.storage.manager.StorageManager

internal class TaskHistoryRepositoryImpl(
    private val db: DatabaseProvider,
    private val storageManager: StorageManager
) : TaskHistoryRepository {
    private fun historyDao() = db.getDatabase().taskHistoryDao()

    override fun getTaskHistoryList(): Flow<List<Task>> {
        return this.historyDao().getTaskHistoryList()
    }

    override suspend fun restoreTaskFromHistoryList(taskId: Long) {
        this.historyDao().resetIsHistoryId(taskId)
    }

    override suspend fun restoreAllTaskFromHistory() {
        this.historyDao().resetAllIsHistoryTask()
    }

    private suspend fun deleteTaskAndDetail(task: Task) {
        val dao = this.historyDao()
        dao.getTaskDetailList(task.id).also { detailList ->
            if (detailList.isEmpty()) {
                return@also
            }
            detailList.forEach { taskDetail ->
                taskDetail.deleteTaskDetailStorageFile(storageManager)
            }
            dao.deleteTaskDetailByTaskId(task.id)
        }
        dao.deleteTask(task)
    }

    override suspend fun deleteOldHistoryTask(numOfAllowedHistory: Int) {
        val dao = this.historyDao()
        if (numOfAllowedHistory < 0) {
            throw IllegalArgumentException("numOfAllowedHistory can't be negative!")
        }
        val allHistorySource = dao.getAllHistorySource().let {
            it.ifEmpty { return }
        }
        if (numOfAllowedHistory == 0) {
            allHistorySource.forEach {
                this@TaskHistoryRepositoryImpl.deleteTaskAndDetail(it)
            }
            return
        }
        if (allHistorySource.size <= numOfAllowedHistory) {
            return
        }
        val toDeleteList = allHistorySource.dropLast(numOfAllowedHistory).let {
            it.ifEmpty { return }
        }
        toDeleteList.forEach {
            this@TaskHistoryRepositoryImpl.deleteTaskAndDetail(it)
        }
    }

    override suspend fun deleteFullTask(taskId: Long) {
        val task = historyDao().getHistorySourceById(taskId)
        this.deleteTaskAndDetail(task)
    }
}
