package com.sqz.checklist.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    /* Get Actions */
    @Query("SELECT * FROM task WHERE isHistoryId < :allowedMaxHistoryIdNum + 1 AND isPin != :isPinNot")
    suspend fun getAll(isPinNot: Int = -1, allowedMaxHistoryIdNum: Int = 0): List<Task>

    @Query("SELECT * FROM task WHERE id = :id")
    suspend fun getAll(id: Long): Task

    @Query("SELECT * FROM task WHERE isHistoryId >= 1 ORDER BY isHistoryId")
    suspend fun getAllOrderByIsHistoryId(): List<Task>

    @Query("SELECT * FROM task WHERE isHistoryId = 0 AND reminder != ''")
    suspend fun getIsRemindedList(): List<Task>

    @Query("SELECT * FROM task WHERE isHistoryId = 0 AND description LIKE '%' || :search || '%'")
    suspend fun searchedList(search: String): List<Task>

    @Query("SELECT COUNT() FROM task WHERE reminder = :reminderId")
    suspend fun matchReminder(reminderId: Int): Int

    //@Query("SELECT * FROM taskDetail")
    //suspend fun getTaskDetail(): List<TaskDetail>


    /* Insert & Edit Actions */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(task: Task): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(vararg detail: TaskDetail)

    @Query("UPDATE task SET description = :edit WHERE id = :id")
    suspend fun editTask(id: Long, edit: String)

    @Query("UPDATE task SET isPin = :edit WHERE id = :id")
    suspend fun editTaskPin(id: Long, edit: Int)

    @Query("UPDATE task SET reminder = :notifyId WHERE id = :id")
    suspend fun insertReminder(id: Long, notifyId: Int)


    /* History-related Get Actions */
    @Query("SELECT COUNT(isHistoryId != 0) FROM task WHERE isHistoryId != 0")
    suspend fun getIsHistorySum(): Int

    @Query("SELECT isHistoryId FROM task WHERE isHistoryId != 0 ORDER BY isHistoryId DESC")
    suspend fun getIsHistoryIdTop(): Int

    @Query("SELECT id, isHistoryId FROM task WHERE isHistoryId != 0 ORDER BY isHistoryId")
    suspend fun getAllIsHistoryId(): List<HistoryIdList>

    @Query("SELECT id FROM task WHERE isHistoryId != 0 ORDER BY isHistoryId")
    suspend fun getIsHistoryBottomKeyId(): Long

    @Query("SELECT isHistoryId FROM task WHERE id = :id AND isHistoryId != 0")
    suspend fun getIsHistory(id: Long): Int


    /* History-related Set Actions */
    @Query("UPDATE task SET isHistoryId = :isHistoryId WHERE id = :id")
    suspend fun setHistoryId(isHistoryId: Int, id: Long)

    @Query("UPDATE task SET isHistoryId = 0")
    suspend fun setAllNotHistory()

    @Query("UPDATE task SET isHistoryId = :isHistoryId WHERE id = :id")
    suspend fun setIsHistoryId(isHistoryId: Int, id: Long)


    /* Delete Actions */
    @Delete
    suspend fun delete(task: Task)

    @Delete
    suspend fun delete(detail: TaskDetail)

    @Query("DELETE FROM task WHERE isHistoryId != 0")
    suspend fun deleteAllHistory()

    @Query("UPDATE task SET reminder = '' WHERE id = :id")
    suspend fun deleteReminder(id: Long)
}

@Dao
interface TaskReminderDao {
    @Query("SELECT * FROM reminder")
    suspend fun getAll(): List<TaskReminder>

    @Query("SELECT * FROM reminder WHERE id = :id")
    suspend fun getAll(id: Int): TaskReminder

    @Query("SELECT COUNT() FROM reminder WHERE isReminded = :isReminded")
    fun getIsRemindedNum(isReminded: Int): Flow<Int>

    @Query("SELECT COUNT() FROM reminder WHERE mode = :modeType AND isReminded != :withoutReminded")
    suspend fun getModeNum(modeType: ReminderModeType, withoutReminded: Int = -1): Int

    @Query("UPDATE reminder SET isReminded = :setter WHERE id = :id")
    suspend fun setIsReminded(id: Int, setter: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(vararg reminder: TaskReminder)

    @Delete
    suspend fun delete(reminder: TaskReminder)
}
