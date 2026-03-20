package sqz.checklist.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import sqz.checklist.data.database.Task
import sqz.checklist.data.database.TaskDetail
import sqz.checklist.data.database.model.HistoryIdList
import sqz.checklist.data.database.model.TaskViewData

@Suppress("AndroidUnresolvedRoomSqlReference")
@Dao
interface TaskDaoOld {
    /* Get Actions */
    /**
     * Get data as [TaskViewData] list.
     * @param isPinNot is -1: return full list
     * @param isPinNot is 0: return only pinned list
     * @param allowedMaxHistoryIdNum return will include history task if more than 0
     */
    @Query(
        """
        SELECT 
            t.*,
            EXISTS(SELECT 1 FROM taskDetail d WHERE d.taskId = t.id LIMIT 1) AS isDetailExist,
            EXISTS(SELECT 1 FROM reminder r  WHERE r.taskId = t.id AND r.isReminded = 1 LIMIT 1) AS isReminded,
            (SELECT r.reminderTime FROM reminder r WHERE r.taskId = t.id) AS reminderTime
        FROM task t WHERE isHistoryId < :allowedMaxHistoryIdNum + 1 AND isPin != :isPinNot
    """
    )
    suspend fun getAll(isPinNot: Int = -1, allowedMaxHistoryIdNum: Int = 0): List<TaskViewData>

    /**
     * Get [TaskViewData] by id
     */
    @Query(
        """
        SELECT 
            t.*,
            EXISTS(SELECT 1 FROM taskDetail d WHERE d.taskId = t.id LIMIT 1) AS isDetailExist,
            EXISTS(SELECT 1 FROM reminder r  WHERE r.taskId = t.id AND r.isReminded = 1 LIMIT 1) AS isReminded,
            (SELECT r.reminderTime FROM reminder r WHERE r.taskId = t.id) AS reminderTime
        FROM task t WHERE id = :id
    """
    )
    suspend fun getAll(id: Long): TaskViewData

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
     * @param isReminded if 0 return which isReminded = false
     * @param isReminded if 1 return which isReminded = true
     * @return [TaskViewData] list but only have reminder
     */
    @Query(
        """
        SELECT 
            t.*,
            EXISTS(SELECT 1 FROM taskDetail d WHERE d.taskId = t.id LIMIT 1) AS isDetailExist,
            EXISTS(SELECT 1 FROM reminder r  WHERE r.taskId = t.id AND r.isReminded = 1 LIMIT 1) AS isReminded,
            r.reminderTime
        FROM task t INNER JOIN reminder r ON r.taskId = t.id 
        WHERE t.isHistoryId = 0 AND r.isReminded = :isReminded
        ORDER BY r.reminderTime DESC
    """
    )
    suspend fun getIsRemindedList(isReminded: Int = 1): List<TaskViewData>

    /**
     * @return [TaskViewData] list in search keywords
     */
    @Query(
        """
        SELECT 
            t.*,
            EXISTS(SELECT 1 FROM taskDetail d WHERE d.taskId = t.id LIMIT 1) AS isDetailExist,
            EXISTS(SELECT 1 FROM reminder r  WHERE r.taskId = t.id AND r.isReminded = 1 LIMIT 1) AS isReminded,
            (SELECT r.reminderTime FROM reminder r WHERE r.taskId = t.id) AS reminderTime
        FROM task t WHERE t.isHistoryId = 0 AND description LIKE '%' || :search || '%'
        ORDER BY t.createDate DESC
    """
    )
    suspend fun searchedList(search: String): List<TaskViewData>

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


    /* Edit Actions */
    /**
     * @param id to locate the task.
     * @param edit set to 0 means not pinned.
     * @param edit set to 1 means the task is pinned.
     */
    @Query("UPDATE task SET isPin = :edit WHERE id = :id")
    suspend fun editTaskPin(id: Long, edit: Int)


    /* History-related Get Actions */
    /**
     * @return the number of task in history
     */
    @Query("SELECT COUNT(isHistoryId != 0) FROM task WHERE isHistoryId != 0")
    suspend fun getIsHistorySum(): Int

    /**
     * @return the largest id of history
     */
    @Query("SELECT isHistoryId FROM task WHERE isHistoryId != 0 ORDER BY isHistoryId DESC")
    suspend fun getIsHistoryIdTop(): Int

    /**
     * @return [HistoryIdList] list
     */
    @Query("SELECT id, isHistoryId FROM task WHERE isHistoryId != 0 ORDER BY isHistoryId")
    suspend fun getAllIsHistoryId(): List<HistoryIdList>

    /**
     * @return the task with the smallest history id
     */
    @Query("SELECT * FROM task WHERE isHistoryId != 0 ORDER BY isHistoryId")
    suspend fun getIsHistoryBottomIdTask(): Task

    /**
     * @return the history id of task by id
     */
    @Query("SELECT isHistoryId FROM task WHERE id = :id AND isHistoryId != 0")
    suspend fun getIsHistory(id: Long): Int


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
