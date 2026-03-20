package sqz.checklist.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import sqz.checklist.data.database.Task
import sqz.checklist.data.database.TaskDetail

@Suppress("AndroidUnresolvedRoomSqlReference")
@Dao
internal interface TaskDao {

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
