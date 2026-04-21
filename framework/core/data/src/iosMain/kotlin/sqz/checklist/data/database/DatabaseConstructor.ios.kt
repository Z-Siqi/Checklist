package sqz.checklist.data.database

import androidx.room.RoomDatabaseConstructor

actual object DatabaseConstructor : RoomDatabaseConstructor<TaskDatabase> {
    actual override fun initialize(): TaskDatabase {
        throw NotImplementedError("Room KSP skipping on iOS")
    }
}