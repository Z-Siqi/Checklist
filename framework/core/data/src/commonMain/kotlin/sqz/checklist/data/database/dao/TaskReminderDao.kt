package sqz.checklist.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import sqz.checklist.data.database.ReminderModeType
import sqz.checklist.data.database.TaskReminder
import sqz.checklist.data.database.model.ReminderViewData

@Suppress("AndroidUnresolvedRoomSqlReference")
@Dao
interface TaskReminderDao {
    /**
     * @return all [ReminderViewData] list
     */
    @Query(
        """
        SELECT 
            r.*,
            (SELECT t.description FROM task t WHERE t.id = r.taskId) AS taskDescription
        FROM reminder r
    """
    )
    suspend fun getAll(): List<ReminderViewData>

    /**
     * @return [ReminderViewData] by id
     */
    @Query(
        """
        SELECT 
            r.*,
            (SELECT t.description FROM task t WHERE t.id = r.taskId) AS taskDescription
        FROM reminder r WHERE r.id = :notifyId
    """
    )
    suspend fun getAll(notifyId: Int): ReminderViewData?

    /**
     * @return [sqz.checklist.data.database.TaskReminder] by task id
     */
    @Query("SELECT * FROM reminder WHERE taskId = :taskId")
    suspend fun getByTaskId(taskId: Long): TaskReminder?

    /**
     * Get the number of reminders with a specific reminded status.
     * @param isReminded 0 for not reminded, 1 for reminded.
     * @return A [Flow] emitting the count of reminders.
     */
    @Query("SELECT COUNT() FROM reminder WHERE isReminded = :isReminded")
    fun getIsRemindedNum(isReminded: Int): Flow<Int>

    /**
     * Get the number of reminders for a specific mode.
     * @param modeType The type of reminder mode to count.
     * @param withoutReminded The `isReminded` status to exclude from the count (default is -1, which excludes none).
     * @return The total number of reminders for the given mode.
     */
    @Query("SELECT COUNT() FROM reminder WHERE mode = :modeType AND isReminded != :withoutReminded")
    suspend fun getModeNum(modeType: ReminderModeType, withoutReminded: Int = -1): Int

    /**
     * Update the `isReminded` status of a reminder.
     * @param id The ID of the reminder to update.
     * @param setter The new value for `isReminded` (0 for not reminded, 1 for reminded).
     */
    @Query("UPDATE reminder SET isReminded = :setter WHERE id = :id")
    suspend fun setIsReminded(id: Int, setter: Int)

    /**
     * Update the mode and extra data for a specific reminder.
     * @param id The ID of the reminder to update.
     * @param mode The new [ReminderModeType] for the reminder.
     * @param extraData The new extra data string for the reminder.
     */
    @Query("UPDATE reminder SET mode = :mode, extraData = :extraData WHERE id = :id")
    suspend fun updateMode(id: Int, mode: ReminderModeType, extraData: String)

    /**
     * Insert one or more reminders. If a reminder with the same primary key already exists,
     * it will be replaced.
     * @param reminder The reminder(s) to insert.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(vararg reminder: TaskReminder)

    /**
     * Delete a reminder.
     * @param reminder The reminder to delete.
     */
    @Delete
    suspend fun delete(reminder: TaskReminder)
}
