package sqz.checklist.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import sqz.checklist.data.database.Task
import sqz.checklist.data.database.TaskDetail
import sqz.checklist.data.database.model.ReminderViewData

@Suppress("AndroidUnresolvedRoomSqlReference")
@Dao
interface TaskHistoryDao {

    @Query("SELECT * FROM task WHERE isHistoryId >= 1 ORDER BY isHistoryId ASC")
    fun getTaskHistoryList(): Flow<List<Task>>

    @Query("SELECT COUNT(*) = 0 FROM task WHERE isHistoryId >= 1")
    fun isTaskHistoryListEmpty(): Flow<Boolean>

    @Query("SELECT * FROM task WHERE isHistoryId >= 1 ORDER BY isHistoryId ASC")
    suspend fun getAllHistorySource(): List<Task>

    @Query("SELECT * FROM task WHERE id = :taskId")
    suspend fun getHistorySourceById(taskId: Long): Task

    @Query(
        """
        SELECT 
            r.*,
            (SELECT t.description FROM task t WHERE t.id = r.taskId) AS taskDescription
        FROM reminder r 
        WHERE r.taskId = :taskId AND (SELECT t.isHistoryId FROM task t WHERE t.id = r.taskId) > 0
    """
    )
    suspend fun getHistoryReminderViewData(taskId: Long): ReminderViewData?

    @Query(
        """
        SELECT 
            r.*,
            (SELECT t.description FROM task t WHERE t.id = r.taskId) AS taskDescription
        FROM reminder r WHERE (SELECT t.isHistoryId FROM task t WHERE t.id = r.taskId) > 0
    """
    )
    suspend fun getAllHistoryReminderViewData(): List<ReminderViewData>

    @Query("UPDATE task SET isHistoryId = 0 WHERE id = :taskId")
    suspend fun resetIsHistoryId(taskId: Long)

    @Query("UPDATE task SET isHistoryId = 0")
    suspend fun resetAllIsHistoryTask()

    @Query("SELECT * FROM taskDetail WHERE taskId = :taskId")
    suspend fun getTaskDetailList(taskId: Long): List<TaskDetail>

    @Query("DELETE FROM taskDetail WHERE taskId = :taskId")
    suspend fun deleteTaskDetailByTaskId(taskId: Long)

    @Delete
    suspend fun deleteTask(task: Task)
}
