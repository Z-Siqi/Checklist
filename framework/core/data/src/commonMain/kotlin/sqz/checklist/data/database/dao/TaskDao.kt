package sqz.checklist.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import sqz.checklist.data.database.Task
import sqz.checklist.data.database.TaskDetail
import sqz.checklist.data.database.model.TaskViewData

@Suppress("AndroidUnresolvedRoomSqlReference")
@Dao
internal interface TaskDao {

    @Query(
        """
        SELECT 
            t.*,
            EXISTS(SELECT 1 FROM taskDetail d WHERE d.taskId = t.id LIMIT 1) AS isDetailExist,
            EXISTS(SELECT 1 FROM reminder r  WHERE r.taskId = t.id AND r.isReminded = 1 LIMIT 1) AS isReminded,
            (SELECT r.reminderTime FROM reminder r WHERE r.taskId = t.id) AS reminderTime
        FROM task t WHERE isHistoryId < 1
    """
    )
    fun getTaskList(): Flow<List<TaskViewData>>

    @Query(
        """
        SELECT 
            t.*,
            EXISTS(SELECT 1 FROM taskDetail d WHERE d.taskId = t.id LIMIT 1) AS isDetailExist,
            EXISTS(SELECT 1 FROM reminder r  WHERE r.taskId = t.id AND r.isReminded = 1 LIMIT 1) AS isReminded,
            (SELECT r.reminderTime FROM reminder r WHERE r.taskId = t.id) AS reminderTime
        FROM task t WHERE isHistoryId < 1 AND isPin = 1
    """
    )
    fun getPinnedTaskList(): Flow<List<TaskViewData>>

    @Query(
        """
        SELECT 
            t.*,
            EXISTS(SELECT 1 FROM taskDetail d WHERE d.taskId = t.id LIMIT 1) AS isDetailExist,
            EXISTS(SELECT 1 FROM reminder r  WHERE r.taskId = t.id AND r.isReminded = 1 LIMIT 1) AS isReminded,
            r.reminderTime
        FROM task t INNER JOIN reminder r ON r.taskId = t.id 
        WHERE t.isHistoryId = 0 AND r.isReminded = 1
        ORDER BY r.reminderTime DESC
    """
    )
    fun getRemindedTaskList(): Flow<List<TaskViewData>>

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
    fun searchedList(search: String): Flow<List<TaskViewData>>

    @Query("UPDATE task SET isPin = :to WHERE id = :id")
    suspend fun onTaskPinChange(id: Long, to: Int)

    @Query("SELECT isHistoryId FROM task WHERE isHistoryId != 0 ORDER BY isHistoryId DESC LIMIT 1")
    suspend fun getMaxIsHistoryId(): Int?

    @Query("UPDATE task SET isHistoryId = :isHistoryId WHERE id = :id")
    suspend fun setIsHistoryId(isHistoryId: Int, id: Long)

    @Query("SELECT COUNT(*) = 0 FROM task WHERE isHistoryId = 0")
    fun isTaskListEmpty(): Flow<Boolean>

    @Query("SELECT * FROM task WHERE id = :taskId")
    suspend fun getTask(taskId: Long): Task?

    @Query("SELECT * FROM taskDetail WHERE taskId = :taskId")
    suspend fun getTaskDetailList(taskId: Long): List<TaskDetail>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTaskDetail(vararg detail: TaskDetail)

    @Update
    suspend fun updateTask(task: Task)

    @Update
    suspend fun updateTaskDetail(taskDetail: TaskDetail)

    @Query("DELETE FROM taskDetail WHERE taskId = :taskId")
    suspend fun deleteTaskDetailByTaskId(taskId: Long)

    @Delete
    suspend fun deleteTaskDetail(detail: TaskDetail)
}
