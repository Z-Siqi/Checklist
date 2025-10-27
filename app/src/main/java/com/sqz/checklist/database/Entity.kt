package com.sqz.checklist.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "task")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo val description: String,
    @ColumnInfo val createDate: LocalDate,
    @ColumnInfo val doingState: String? = null, // not implemented
    @ColumnInfo val isPin: Boolean = false,
    @ColumnInfo val isHistoryId: Int = 0,
)

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
    @ColumnInfo val longAsDelay: Boolean = false, // not implemented yet, idea: Used to control whether to follow the time zone, follow if its true
)

enum class ReminderModeType {
    Worker, AlarmManager
}

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
    @ColumnInfo val dataString: String? = null, // store file title
    @ColumnInfo val dataByte: ByteArray // store data
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as TaskDetail
        if (type != other.type) return false
        if (dataString != other.dataString) return false
        if (!dataByte.contentEquals(other.dataByte)) return false
        return true
    }

    override fun hashCode(): Int {
        return dataByte.contentHashCode()
    }
}

enum class TaskDetailType { // some part not implemented
    Text, URL, Application, Picture, Video, Audio //, ChildList, File, URI
}
