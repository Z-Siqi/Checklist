package com.sqz.checklist.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "task")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo val description: String,
    @ColumnInfo val createDate: LocalDate,
    @ColumnInfo val reminder: Int? = null,
    @ColumnInfo val doingState: String? = null, // not implemented
    @ColumnInfo val isPin: Boolean = false,
    @ColumnInfo val detail: Boolean = false,
    @ColumnInfo val isHistoryId: Int = 0,
)

@Entity(tableName = "reminder")
data class TaskReminder(
    @PrimaryKey(autoGenerate = false) val id: Int, // notifyId
    @ColumnInfo val description: String,
    @ColumnInfo val reminderTime: Long,
    @ColumnInfo val mode: ReminderModeType,
    @ColumnInfo val isReminded: Boolean = false,
    @ColumnInfo val extraText: String? = null, // not implemented
    @ColumnInfo val extraData: String? = null,
    @ColumnInfo val longAsDelay: Boolean = false, // Used to set a delay, not dept on TimeLong if its true (not implemented)
)

enum class ReminderModeType {
    Worker, AlarmManager
}

@Entity(tableName = "taskDetail")
data class TaskDetail(
    @PrimaryKey val id: Long, // same as task id
    @ColumnInfo val type: TaskDetailType,
    @ColumnInfo val dataString: String,
    @ColumnInfo val dataByte: ByteArray? = null
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
    Text, URL, Application, Picture, Video //, ChildList
}
