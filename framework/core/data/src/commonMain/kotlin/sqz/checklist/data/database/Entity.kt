package sqz.checklist.data.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.datetime.LocalDate

/**
 * Task table database entity.
 *
 * @param id primary key.
 * @param description description of task.
 * @param createDate store task create date of the task.
 * @param doingState not implemented yet.
 * @param isPin mark task is pinned or not.
 * @param isHistoryId if > 0 means the task has been checked and should only visible in history
 *        screen. Then expected will delete the task from database if `isHistoryId` > expected value.
 */
@Entity(tableName = "task")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo val description: String,
    @ColumnInfo val createDate: LocalDate,
    @ColumnInfo val doingState: String? = null, // not implemented
    @ColumnInfo val isPin: Boolean = false,
    @ColumnInfo val isHistoryId: Int = 0,
)

/**
 * TaskReminder table database entity.
 *
 * @param id primary key.
 * @param taskId foreign key to [Task.id].
 * @param reminderTime reminder time as timestamp for the task.
 * @param mode reminder mode of the task.
 * @param isReminded if true means the task has been reminded.
 * @param extraText not implemented yet.
 * @param extraData now is for store Android WorkManager UUID or AlarmManager ID.
 * @param longAsDelay not implemented yet.
 */
@Entity(
    tableName = "reminder",
    foreignKeys = [
        ForeignKey(
            entity = Task::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.NO_ACTION
        )
    ],
    indices = [Index("taskId"), Index("isReminded"), Index("reminderTime")]
)
data class TaskReminder(
    @PrimaryKey(autoGenerate = false) val id: Int, // notifyId
    @ColumnInfo val taskId: Long,
    @ColumnInfo val reminderTime: Long,
    @ColumnInfo val mode: ReminderModeType,
    @ColumnInfo val isReminded: Boolean = false,
    @ColumnInfo val extraText: String? = null, // not implemented yet, idea: Allow adding notes to notifications
    @ColumnInfo val extraData: String? = null,
    @ColumnInfo val longAsDelay: Boolean = false, // not implemented yet, idea: Used to control whether to follow the time zone, follow if it's true
)

enum class ReminderModeType { //TODO: support KMP
    Worker, AlarmManager
}

/**
 * TaskDetail table database entity.
 *
 * @param id primary key.
 * @param taskId foreign key to [Task.id].
 * @param type type of task detail.
 * @param description description of task detail (title).
 * @param dataString store file title in case of type contain file;
 *   store platform in application type.
 * @param dataByte store data such as internal file path or text String.
 */
@Entity(
    tableName = "taskDetail",
    foreignKeys = [
        ForeignKey(
            entity = Task::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.NO_ACTION
        )
    ],
    indices = [Index("taskId")]
)
data class TaskDetail(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo val taskId: Long,
    @ColumnInfo val type: TaskDetailType,
    @ColumnInfo val description: String? = null, // store description
    @ColumnInfo val dataString: String? = null, // store file title only
    @ColumnInfo val dataByte: ByteArray // store data such as file path or text
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        other as TaskDetail
        if (id != other.id) return false
        if (taskId != other.taskId) return false
        if (type != other.type) return false
        if (description != other.description) return false
        if (dataString != other.dataString) return false
        if (!dataByte.contentEquals(other.dataByte)) return false
        return true
    }

    override fun hashCode(): Int {
        // return dataByte.contentHashCode()
        var result = id.hashCode()
        result = 31 * result + taskId.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + (description?.hashCode() ?: 0)
        result = 31 * result + (dataString?.hashCode() ?: 0)
        result = 31 * result + dataByte.contentHashCode()
        return result
    }
}

enum class TaskDetailType { // some part not implemented
    Text, URL, Application, Picture, Video, Audio //, ChildList, File
}
