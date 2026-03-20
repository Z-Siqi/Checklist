package sqz.checklist.data.database

import androidx.room.Room
import androidx.room.RoomDatabase
import java.io.File

fun getDatabaseBuilder(): RoomDatabase.Builder<TaskDatabase> {
    val dbFile = File(System.getProperty("java.io.tmpdir"), "$taskDatabaseName.db")
    return Room.databaseBuilder<TaskDatabase>(
        name = dbFile.absolutePath,
    )
}
