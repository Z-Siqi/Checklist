package com.sqz.checklist.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SimpleSQLiteQuery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

const val taskDatabaseName = "task-database"

suspend fun mergeDatabaseCheckpoint(database: RoomDatabase) {
    withContext(Dispatchers.IO) {
        database.query(SimpleSQLiteQuery("PRAGMA wal_checkpoint(FULL)"))
    }
}

@Database(entities = [Task::class], version = 1)
@TypeConverters(LocalDateConverter::class)
abstract class TaskDatabase : RoomDatabase() {
    abstract fun taskDao() : TaskDao
}
