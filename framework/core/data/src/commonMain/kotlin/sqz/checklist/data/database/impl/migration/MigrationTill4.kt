package sqz.checklist.data.database.impl.migration

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import sqz.checklist.data.database.ReminderModeType
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

internal object MigrationTill4 {
    @OptIn(ExperimentalTime::class)
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(connection: SQLiteConnection) {
            // 1) Create new tables
            connection.execSQL(
                "CREATE TABLE taskDetail(id INTEGER NOT NULL, type TEXT NOT NULL, dataString TEXT NOT NULL, PRIMARY KEY(id))"
            )
            connection.execSQL(
                """
                CREATE TABLE reminder(
                    id INTEGER NOT NULL, description TEXT NOT NULL,
                    reminderTime INTEGER NOT NULL,
                    mode TEXT NOT NULL,
                    isReminded INTEGER NOT NULL DEFAULT 0,
                    extraText TEXT DEFAULT NULL,
                    extraData TEXT DEFAULT NULL,
                    longAsDelay INTEGER NOT NULL DEFAULT 0,
                    PRIMARY KEY(id)
                )
                """.trimIndent()
            )
            connection.execSQL(
                """
                CREATE TABLE task_temp (
                    id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                    description TEXT NOT NULL,
                    createDate TEXT NOT NULL,
                    reminder INTEGER,
                    doingState TEXT,
                    isPin INTEGER NOT NULL,
                    detail INTEGER NOT NULL,
                    pinToTop INTEGER NOT NULL,
                    isHistory INTEGER NOT NULL,
                    isHistoryId INTEGER NOT NULL
                )
                """.trimIndent()
            )
            // 2) Prepare INSERT statements
            val insertTaskTempSql = """
                INSERT INTO task_temp
                (id, description, createDate, reminder, doingState, isPin, detail, pinToTop, isHistory, isHistoryId)
                VALUES
                (?, ?, ?, ?, NULL, ?, 0, ?, ?, ?)
            """.trimIndent()
            val insertReminderSql = """
                INSERT INTO reminder
                (id, description, reminderTime, mode, isReminded, extraText, extraData, longAsDelay)
                VALUES
                (?, ?, ?, ?, ?, NULL, ?, 0)
            """.trimIndent()
            // 3) Read old rows using SQLiteStatement
            connection.prepare(
                """
                SELECT id, description, createDate, isPin, isHistory, isHistoryId, pinToTop, reminder FROM task
                """.trimIndent()
            ).use { selectStmt ->
                connection.prepare(insertTaskTempSql).use { insertTaskStmt ->
                    connection.prepare(insertReminderSql).use { insertReminderStmt ->
                        while (selectStmt.step()) {
                            val id = selectStmt.getInt(0)
                            val description = selectStmt.getText(1)
                            val createDate = selectStmt.getText(2)
                            val isPin = selectStmt.getInt(3)
                            val isHistory = selectStmt.getInt(4)
                            val isHistoryId = selectStmt.getInt(5)
                            val pinToTop = selectStmt.getInt(6)
                            val reminder = if (selectStmt.isNull(7)) null else selectStmt.getText(7)
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
                            // 4) INSERT into task_temp
                            insertTaskStmt.reset()
                            insertTaskStmt.bindInt(1, id)
                            insertTaskStmt.bindText(2, description)
                            insertTaskStmt.bindText(3, createDate)
                            if (reminderId == null) insertTaskStmt.bindNull(4) else insertTaskStmt.bindInt(
                                4, reminderId
                            )
                            insertTaskStmt.bindInt(5, isPin)
                            insertTaskStmt.bindInt(6, pinToTop)
                            insertTaskStmt.bindInt(7, isHistory)
                            insertTaskStmt.bindInt(8, isHistoryId)
                            insertTaskStmt.step()
                            // 5) INSERT into reminder
                            val mode = if (uuid == null) ReminderModeType.AlarmManager.name
                            else ReminderModeType.Worker.name

                            val isReminded =
                                if (reminderId != null && reminderTime != null && reminderTime < Clock.System.now().toEpochMilliseconds()) 1
                                else 0

                            if (reminderId != null && reminderTime != null) {
                                insertReminderStmt.reset()
                                insertReminderStmt.bindInt(1, reminderId)
                                insertReminderStmt.bindText(2, description)
                                insertReminderStmt.bindLong(3, reminderTime)
                                insertReminderStmt.bindText(4, mode)
                                insertReminderStmt.bindInt(5, isReminded)
                                if (extraData == null) insertReminderStmt.bindNull(6) else insertReminderStmt.bindText(
                                    6, extraData
                                )
                                insertReminderStmt.step()
                            }
                        }
                    }
                }
            }
            // 6) Swap tables & alter
            connection.execSQL("DROP TABLE task")
            connection.execSQL("ALTER TABLE task_temp RENAME TO task")
            connection.execSQL("ALTER TABLE task DROP COLUMN pinToTop")
            connection.execSQL("ALTER TABLE task DROP COLUMN isHistory")
        }
    }

    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL("ALTER TABLE taskDetail ADD dataByte BLOB undefined")
        }
    }

    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(connection: SQLiteConnection) {
            // Update task below
            connection.execSQL(
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
            connection.execSQL(
                """
                INSERT INTO `task_new` (`id`, `description`, `createDate`, `doingState`, `isPin`, `isHistoryId`)
                SELECT `id`, `description`, `createDate`, `doingState`, `isPin`, `isHistoryId`
                FROM `task`
                """.trimIndent()
            )
            connection.execSQL("ALTER TABLE `task` RENAME TO `task_old`")
            connection.execSQL("ALTER TABLE `task_new` RENAME TO `task`")
            // Update taskDetail below
            connection.execSQL(
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
            connection.execSQL(
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
            connection.execSQL("DROP TABLE `taskDetail`")
            connection.execSQL("ALTER TABLE `taskDetail_new` RENAME TO `taskDetail`")
            connection.execSQL("CREATE INDEX IF NOT EXISTS `index_taskDetail_taskId` ON `taskDetail` (`taskId`)")
            // Update reminder below
            connection.execSQL(
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
            connection.execSQL(
                """
                INSERT INTO `reminder_new` (`id`,`taskId`,`reminderTime`,`mode`,`isReminded`,`extraText`,`extraData`,`longAsDelay`)
                SELECT r.`id`, t.`id` AS taskId, r.`reminderTime`, r.`mode`, r.`isReminded`, r.`extraText`, r.`extraData`, r.`longAsDelay`
                FROM `reminder` r
                JOIN `task_old` t ON t.`reminder` = r.`id`
                """.trimIndent()
            )
            connection.execSQL("DROP TABLE `task_old`") // Delete task_old
            connection.execSQL("DROP TABLE `reminder`")
            connection.execSQL("ALTER TABLE `reminder_new` RENAME TO `reminder`")
            connection.execSQL("CREATE INDEX IF NOT EXISTS `index_reminder_taskId` ON `reminder` (`taskId`)")
            connection.execSQL("CREATE INDEX IF NOT EXISTS `index_reminder_isReminded` ON `reminder` (`isReminded`)")
            connection.execSQL("CREATE INDEX IF NOT EXISTS `index_reminder_reminderTime` ON `reminder` (`reminderTime`)")
        }
    }
}
