package sqz.checklist.data.database

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase

fun getDatabaseBuilder(context: Context): RoomDatabase.Builder<TaskDatabase> {
    val appContext = context.applicationContext
    val dbFile = appContext.getDatabasePath(taskDatabaseName)
    return Room.databaseBuilder<TaskDatabase>(
        context = appContext,
        name = dbFile.absolutePath
    )
}
