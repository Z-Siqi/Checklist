package sqz.checklist.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import sqz.checklist.data.database.Task
import sqz.checklist.data.database.TaskDetail
import sqz.checklist.data.database.model.HistoryIdList
import sqz.checklist.data.database.model.TaskViewData

@Suppress("AndroidUnresolvedRoomSqlReference")
@Dao
interface TaskDaoOld {
    /* Get Actions */
    /**
     * Get [sqz.checklist.data.database.Task] by id.
     * @param id The id of the task to retrieve.
     */
    @Query("SELECT * FROM task WHERE id = :id")
    suspend fun getTask(id: Long): Task

    /**
     * @return [Task] in history list
     */
    @Query("SELECT * FROM task WHERE isHistoryId >= 1 ORDER BY isHistoryId")
    suspend fun getAllOrderByIsHistoryId(): List<Task>

    /**
     * @return the whole [sqz.checklist.data.database.TaskDetail] list
     */
    @Query("SELECT * FROM taskDetail")
    suspend fun getTaskDetail(): List<TaskDetail>

    /**
     * @return the [TaskDetail] list by task id
     */
    @Query("SELECT * FROM taskDetail WHERE taskId = :taskId")
    suspend fun getTaskDetail(taskId: Long): List<TaskDetail>


    /* History-related Get Actions */
    /**
     * @return the number of task in history
     */
    @Query("SELECT COUNT(isHistoryId != 0) FROM task WHERE isHistoryId != 0")
    suspend fun getIsHistorySum(): Int

    /**
     * @return [HistoryIdList] list
     */
    @Query("SELECT id, isHistoryId FROM task WHERE isHistoryId != 0 ORDER BY isHistoryId")
    suspend fun getAllIsHistoryId(): List<HistoryIdList>


    /* History-related Set Actions */
    /**
     * Set the history id of the task.
     */
    @Query("UPDATE task SET isHistoryId = :isHistoryId WHERE id = :id")
    suspend fun setHistoryId(isHistoryId: Int, id: Long)

    /**
     * Set all tasks to not be in history.
     */
    @Query("UPDATE task SET isHistoryId = 0")
    suspend fun setAllNotHistory()

    /**
     * Set the history id of the task.
     */
    @Query("UPDATE task SET isHistoryId = :isHistoryId WHERE id = :id")
    suspend fun setIsHistoryId(isHistoryId: Int, id: Long)


    /* Delete Actions */
    /**
     * Delete task
     */
    @Delete
    suspend fun delete(task: Task)

    /**
     * Delete all history task
     */
    @Query("DELETE FROM task WHERE isHistoryId != 0")
    suspend fun deleteAllHistory()
}
