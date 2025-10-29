package com.sqz.checklist.database

import android.content.Context
import android.util.Log
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
        Log.d("database", "mergeDatabaseCheckpoint")
    }
}

@Database(
    entities = [
        Task::class, TaskDetail::class, TaskReminder::class
    ], version = 4, exportSchema = false
)
@TypeConverters(LocalDateConverter::class)
abstract class TaskDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun taskReminderDao(): TaskReminderDao
}

@Suppress("TYPE_INTERSECTION_AS_REIFIED_WARNING")
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
                    } catch (_: NumberFormatException) {
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
            val isReminded = if (reminderId != null && reminderTime != null &&
                reminderTime < System.currentTimeMillis()
            ) "1" else "0"
            if (reminderId != null && reminderTime != null) db.execSQL( // INSERT to reminder
                " INSERT INTO reminder (id, description, reminderTime, mode, isReminded, extraText, extraData, longAsDelay) VALUES (?, ?, ?, ?, $isReminded, NULL, ?, 0)",
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

private val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Update task below
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `task_new` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `description` TEXT NOT NULL,
                `createDate` TEXT NOT NULL,
                `doingState` TEXT,
                `isPin` INTEGER NOT NULL,
                `isHistoryId` INTEGER NOT NULL
            )
        """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO `task_new` (`id`, `description`, `createDate`, `doingState`, `isPin`, `isHistoryId`)
            SELECT `id`, `description`, `createDate`, `doingState`, `isPin`, `isHistoryId`
            FROM `task`
        """.trimIndent()
        )
        db.execSQL("ALTER TABLE `task` RENAME TO `task_old`")
        db.execSQL("ALTER TABLE `task_new` RENAME TO `task`")
        // Update taskDetail below
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `taskDetail_new` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `taskId` INTEGER NOT NULL,
                `type` TEXT NOT NULL,
                `description` TEXT,
                `dataString` TEXT,
                `dataByte` BLOB NOT NULL,
                FOREIGN KEY(`taskId`) REFERENCES `task`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
        """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO `taskDetail_new` (`taskId`, `type`, `description`, `dataString`, `dataByte`)
            SELECT 
                `id` AS taskId, `type`, NULL AS description,
                CASE
                    WHEN `type` = 'Text' OR `type` = 'URL' OR `type` = 'Application'
                        THEN NULL
                    ELSE `dataString`
                END AS dataString,
                CASE
                    WHEN `type` = 'Text' OR `type` = 'URL' OR `type` = 'Application'
                        THEN CAST(`dataString` AS BLOB)
                    ELSE `dataByte`
                END AS dataByte
            FROM `taskDetail`
        """.trimIndent()
        )
        db.execSQL("DROP TABLE `taskDetail`")
        db.execSQL("ALTER TABLE `taskDetail_new` RENAME TO `taskDetail`")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_taskDetail_taskId` ON `taskDetail` (`taskId`)")
        // Update reminder below
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `reminder_new` (
                `id` INTEGER NOT NULL,
                `taskId` INTEGER NOT NULL,
                `reminderTime` INTEGER NOT NULL,
                `mode` TEXT NOT NULL,
                `isReminded` INTEGER NOT NULL,
                `extraText` TEXT,
                `extraData` TEXT,
                `longAsDelay` INTEGER NOT NULL,
                PRIMARY KEY(`id`),
                FOREIGN KEY(`taskId`) REFERENCES `task`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
            )
        """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO `reminder_new` (`id`,`taskId`,`reminderTime`,`mode`,`isReminded`,`extraText`,`extraData`,`longAsDelay`)
            SELECT r.`id`, t.`id` AS taskId, r.`reminderTime`, r.`mode`, r.`isReminded`, r.`extraText`, r.`extraData`, r.`longAsDelay`
            FROM `reminder` r
            JOIN `task_old` t ON t.`reminder` = r.`id`
        """.trimIndent()
        )
        db.execSQL("DROP TABLE `task_old`") // Delete task_old
        db.execSQL("DROP TABLE `reminder`")
        db.execSQL("ALTER TABLE `reminder_new` RENAME TO `reminder`")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_reminder_taskId` ON `reminder` (`taskId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_reminder_isReminded` ON `reminder` (`isReminded`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_reminder_reminderTime` ON `reminder` (`reminderTime`)")
    }
}

fun buildDatabase(context: Context): TaskDatabase {
    val database = Room.databaseBuilder(context, TaskDatabase::class.java, taskDatabaseName)
        .addMigrations(MIGRATION_1_2)
        .addMigrations(MIGRATION_2_3)
        .addMigrations(MIGRATION_3_4)
        .build()
    return database
}
