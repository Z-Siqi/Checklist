package com.sqz.checklist.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface TaskDao {
    /* Get Table Actions */
    @Query("SELECT * FROM task WHERE isHistory != :withoutHistory")
    suspend fun getAll(withoutHistory: Int = -1): List<Task>

    @Query("SELECT * FROM task WHERE isHistory = 1 ORDER BY isHistoryId")
    suspend fun getAllOrderByIsHistoryId(): List<Task>

    @Query("SELECT * FROM task WHERE isPin = 1")
    suspend fun getIsPinList(): List<Task>


    /* Get Value Actions */
    @Query("SELECT reminder FROM task WHERE id = :id")
    suspend fun getReminderInfo(id: Int): String?

    @Query("SELECT COUNT(isPin) FROM task WHERE isPin = 1")
    suspend fun getIsPinSum(): Int


    /* Insert & Edit Actions */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(vararg task: Task)

    @Query("UPDATE task SET description = :edit WHERE id = :id")
    suspend fun editTask(id: Int, edit: String)

    @Query("UPDATE task SET isPin = :edit WHERE id = :id")
    suspend fun editTaskPin(id: Int, edit: Int)

    @Query("UPDATE task SET reminder = :string WHERE id = :id")
    suspend fun insertReminder(id: Int, string: String)


    /* History-related Get Actions */
    @Query("SELECT COUNT(isHistory) FROM task WHERE isHistory = 1")
    suspend fun getIsHistorySum(): Int

    @Query("SELECT isHistoryId FROM task WHERE isHistory = 1 ORDER BY isHistoryId DESC")
    suspend fun getIsHistoryIdTop(): Int

    @Query("SELECT isHistoryId FROM task WHERE isHistory = 1 ORDER BY isHistoryId")
    suspend fun getIsHistoryIdBottom(): Int

    @Query("SELECT isHistoryId FROM task WHERE isHistory = 1 AND isHistoryId >= :min ORDER BY isHistoryId")
    suspend fun getAllIsHistoryId(min: Int = 0): List<Int>

    @Query("SELECT id FROM task WHERE isHistory = 1 ORDER BY isHistoryId")
    suspend fun getIsHistoryBottomKeyId(): Int

    @Query("SELECT isHistoryId FROM task WHERE id = :id")
    suspend fun getIsHistoryId(id: Int): Int

    @Query("SELECT isHistory FROM task WHERE id = :id")
    suspend fun getIsHistory(id: Int): Int // 0 = false, 1 = ture


    /* History-related Set Actions */
    @Query("UPDATE task SET isHistoryId = :isHistoryId WHERE id = :id")
    suspend fun setHistoryId(isHistoryId: Int, id: Int)

    @Query("UPDATE task SET isHistoryId = :isHistoryId WHERE isHistoryId = :byId")
    suspend fun setHistoryIdById(isHistoryId: Int, byId: Int)

    @Query("UPDATE task SET isHistory = 0, isHistoryId = 0")
    suspend fun setAllNotHistory()

    @Query("UPDATE task SET isHistory = :isHistory WHERE id = :id")
    suspend fun setHistory(isHistory: Int, id: Int)


    /* Delete Actions */
    @Delete
    suspend fun delete(task: Task)

    @Query("DELETE FROM task WHERE isHistory = 1")
    suspend fun deleteAllHistory()
}
