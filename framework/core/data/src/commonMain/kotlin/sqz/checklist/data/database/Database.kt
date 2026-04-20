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
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.internal.SynchronizedObject
import kotlinx.coroutines.internal.synchronized
import sqz.checklist.data.database.dao.TaskDao
import sqz.checklist.data.database.dao.TaskDaoOld
import sqz.checklist.data.database.dao.TaskHistoryDao
import sqz.checklist.data.database.dao.TaskReminderDao
import sqz.checklist.data.database.impl.migration.MigrationTill4

/** Checklist database name **/
const val taskDatabaseName = "task-database"

/** Merge database checkpoint **/
suspend fun mergeDatabaseCheckpoint(database: RoomDatabase) {
    database.useWriterConnection { conn ->
        conn.usePrepared("PRAGMA wal_checkpoint(FULL)") { it.step() }
    }
}

/**
 * Task Database
 *
 * @see RoomDatabase
 */
@Database(
    entities = [
        Task::class, TaskDetail::class, TaskReminder::class
    ], version = 4, exportSchema = false
)
@ConstructedBy(DatabaseConstructor::class)
@TypeConverters(LocalDateConverter::class)
abstract class TaskDatabase : RoomDatabase() { //TODO: Finish dao

    internal abstract fun taskDao(): TaskDao

    internal abstract fun taskHistoryDao(): TaskHistoryDao


    abstract fun taskDaoOld(): TaskDaoOld
    abstract fun taskReminderDao(): TaskReminderDao
}

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
expect object DatabaseConstructor : RoomDatabaseConstructor<TaskDatabase> {
    override fun initialize(): TaskDatabase
}

/**
 * Get and create room database
 */
private fun getRoomDatabase(
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

/**
 * Get database instance
 *
 * This will use object to ensure database instance is up-to-date and one instance only.
 *
 * **Note:** Do not use `val dao = DatabaseProvider.getDatabase().dao()`
 *   or `class ExampleClass(private val dao: Dao)` in anywhere that expected will hold data
 *   for a long time. Only use `val dao = DatabaseProvider.getDatabase().dao()`
 *   inside `fun` method for ensure the database can get data correctly once restart database
 *   (e.g. restored app data).
 *
 * @constructor Create Database builder for [DatabaseProvider.getDatabase] get current database.
 *   Use [DatabaseProvider.updateBuilder] before reopen database otherwise the app might crash.
 * @see TaskDatabase
 */
class DatabaseProvider {

    companion object {
        /** Builder instance **/
        private lateinit var _builder: RoomDatabase.Builder<TaskDatabase>

        /**
         * Update [_builder] for [getDatabase] use.
         *
         * @param builder New builder instance.
         */
        fun updateBuilder(builder: RoomDatabase.Builder<TaskDatabase>) {
            _builder = builder
        }

        /** @see [SynchronizedObject] **/
        @OptIn(InternalCoroutinesApi::class)
        private val _lock = SynchronizedObject()

        /** Database instance **/
        private var _instance: TaskDatabase? = null
    }

    @OptIn(InternalCoroutinesApi::class)
    constructor(builder: RoomDatabase.Builder<TaskDatabase>) {
        synchronized(_lock) { _builder = builder }
    }

    /**
     * Rebuild database instance.
     *
     * This method will close database and set [_instance] to null. The database will build when
     * any method called [getDatabase].
     */
    @OptIn(InternalCoroutinesApi::class)
    fun rebuild() {
        synchronized(_lock) {
            _instance?.close()
            _instance = null
        }
    }

    /**
     * Get database instance
     *
     * This will use object to ensure database instance is up-to-date and one instance only.
     *
     * @return [TaskDatabase] instance.
     */
    @OptIn(InternalCoroutinesApi::class)
    fun getDatabase(): TaskDatabase {
        return _instance ?: synchronized(_lock) {
            _instance ?: getRoomDatabase(_builder).also { _instance = it }
        }
    }
}
