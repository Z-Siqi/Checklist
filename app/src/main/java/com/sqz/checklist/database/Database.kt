package com.sqz.checklist.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

const val taskDatabaseName = "task-database"

suspend fun mergeDatabaseCheckpoint(database: RoomDatabase) {
    withContext(Dispatchers.IO) {
        database.query(SimpleSQLiteQuery("PRAGMA wal_checkpoint(FULL)"))
    }
}

@Database(
    entities = [
        Task::class, TaskDetail::class, TaskReminder::class
    ], version = 3, exportSchema = false
)
@TypeConverters(LocalDateConverter::class)
abstract class TaskDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun taskReminderDao(): TaskReminderDao
}

private val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE taskDetail(id INTEGER NOT NULL, type TEXT NOT NULL, dataString TEXT NOT NULL, PRIMARY KEY(id))")
        db.execSQL("CREATE TABLE reminder(id INTEGER NOT NULL, description TEXT NOT NULL, reminderTime INTEGER NOT NULL, mode TEXT NOT NULL, isReminded INTEGER NOT NULL DEFAULT '0', extraText TEXT DEFAULT undefined, extraData TEXT DEFAULT undefined, longAsDelay INTEGER NOT NULL DEFAULT '0', PRIMARY KEY(id))")
        db.execSQL("CREATE TABLE task_temp (id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT, description TEXT NOT NULL, createDate TEXT NOT NULL, reminder INTEGER, doingState TEXT, isPin INTEGER NOT NULL, detail INTEGER NOT NULL, pinToTop INTEGER NOT NULL, isHistory INTEGER NOT NULL, isHistoryId INTEGER NOT NULL)")
        val cursor = db.query("SELECT * FROM task")
        while (cursor.moveToNext()) {
            val id = cursor.getInt(cursor.getColumnIndexOrThrow("id"))
            val description = cursor.getString(cursor.getColumnIndexOrThrow("description"))
            val createDate = cursor.getString(cursor.getColumnIndexOrThrow("createDate"))
            val isPin = cursor.getInt(cursor.getColumnIndexOrThrow("isPin"))
            val isHistory = cursor.getInt(cursor.getColumnIndexOrThrow("isHistory"))
            val isHistoryId = cursor.getInt(cursor.getColumnIndexOrThrow("isHistoryId"))
            val pinToTop = cursor.getInt(cursor.getColumnIndexOrThrow("pinToTop"))
            val reminder = cursor.getString(cursor.getColumnIndexOrThrow("reminder"))
            var reminderId: Int? = null
            var reminderTime: Long? = null
            var extraData: String? = null
            var uuid: String? = null
            if (!reminder.isNullOrEmpty()) {
                val parts = reminder.split(":")
                if (parts.size == 2) {
                    val leftPart = parts[0]
                    val rightPart = parts[1]
                    reminderTime = rightPart.toLongOrNull()
                    try {
                        reminderId = leftPart.toInt()
                    } catch (e: NumberFormatException) {
                        uuid = leftPart
                        reminderId = id
                    }
                    if (uuid != null) extraData = uuid
                }
            }
            db.execSQL( // INSERT to task_temp
                " INSERT INTO task_temp (id, description, createDate, reminder, doingState, isPin, detail, pinToTop, isHistory, isHistoryId) VALUES (?, ?, ?, ?, NULL, ?, 0, ?, ?, ?)",
                arrayOf(
                    id, description, createDate, reminderId, isPin, pinToTop, isHistory, isHistoryId
                )
            )
            val mode = if (uuid == null) ReminderModeType.AlarmManager.name
            else ReminderModeType.Worker.name
            if (reminderId != null && reminderTime != null) db.execSQL( // INSERT to reminder
                " INSERT INTO reminder (id, description, reminderTime, mode, isReminded, extraText, extraData, longAsDelay) VALUES (?, ?, ?, ?, 0, NULL, ?, 0)",
                arrayOf(reminderId, description, reminderTime, mode, extraData)
            )
        }
        cursor.close()
        db.execSQL("DROP TABLE task")
        db.execSQL("ALTER TABLE task_temp RENAME TO task")
        db.execSQL("ALTER TABLE task DROP pinToTop")
        db.execSQL("ALTER TABLE task DROP COLUMN isHistory")
    }
}

private val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE taskDetail ADD dataByte BLOB undefined")
    }
}

fun buildDatabase(context: Context): TaskDatabase {
    val database = Room.databaseBuilder(context, TaskDatabase::class.java, taskDatabaseName)
        .addMigrations(MIGRATION_1_2)
        .addMigrations(MIGRATION_2_3)
        .build()
    return database
}
