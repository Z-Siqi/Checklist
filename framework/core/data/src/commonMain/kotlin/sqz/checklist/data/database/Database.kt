package sqz.checklist.data.database

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.TypeConverters
import androidx.room.useWriterConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import sqz.checklist.data.database.dao.TaskDao
import sqz.checklist.data.database.dao.TaskDaoOld
import sqz.checklist.data.database.dao.TaskHistoryDao
import sqz.checklist.data.database.dao.TaskReminderDao
import sqz.checklist.data.database.impl.migration.MigrationTill4

const val taskDatabaseName = "task-database"

suspend fun mergeDatabaseCheckpoint(database: RoomDatabase) {
    database.useWriterConnection { conn ->
        conn.usePrepared("PRAGMA wal_checkpoint(FULL)") { it.step() }
    }
}

@Database(
    entities = [
        Task::class, TaskDetail::class, TaskReminder::class
    ], version = 4, exportSchema = false
)
@ConstructedBy(DatabaseConstructor::class)
@TypeConverters(LocalDateConverter::class)
abstract class TaskDatabase : RoomDatabase() { //TODO: Finish dao

    internal abstract fun taskDao(): TaskDao


    abstract fun taskDaoOld(): TaskDaoOld
    abstract fun taskHistoryDao(): TaskHistoryDao
    abstract fun taskReminderDao(): TaskReminderDao
}

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
expect object DatabaseConstructor : RoomDatabaseConstructor<TaskDatabase> {
    override fun initialize(): TaskDatabase
}

fun getRoomDatabase(
    builder: RoomDatabase.Builder<TaskDatabase>
): TaskDatabase {
    val build = builder
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .addMigrations(MigrationTill4.MIGRATION_1_2)
        .addMigrations(MigrationTill4.MIGRATION_2_3)
        .addMigrations(MigrationTill4.MIGRATION_3_4)
        .build()
    return build
}
