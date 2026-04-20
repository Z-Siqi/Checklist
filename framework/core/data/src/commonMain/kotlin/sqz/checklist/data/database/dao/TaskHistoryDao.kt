package sqz.checklist.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import sqz.checklist.data.database.Task
import sqz.checklist.data.database.TaskDetail

@Suppress("AndroidUnresolvedRoomSqlReference")
@Dao
interface TaskHistoryDao {

    @Query("SELECT * FROM task WHERE isHistoryId >= 1 ORDER BY isHistoryId ASC")
    fun getTaskHistoryList(): Flow<List<Task>>

    @Query("SELECT * FROM task WHERE isHistoryId >= 1 ORDER BY isHistoryId ASC")
    suspend fun getAllHistorySource(): List<Task>

    @Query("SELECT * FROM task WHERE id = :taskId")
    suspend fun getHistorySourceById(taskId: Long): Task

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
