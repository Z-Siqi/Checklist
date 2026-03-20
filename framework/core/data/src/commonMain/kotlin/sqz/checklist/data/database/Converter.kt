package sqz.checklist.data.database

import androidx.room.TypeConverter
import kotlinx.datetime.LocalDate
import kotlinx.datetime.format
import sqz.checklist.data.storage.audioMediaPath
import sqz.checklist.data.storage.pictureMediaPath
import sqz.checklist.data.storage.videoMediaPath

class LocalDateConverter {
    private val formatter = LocalDate.Formats.ISO

    @TypeConverter
    fun fromString(value: String?): LocalDate? {
        return value?.let {
            LocalDate.parse(it, formatter)
        }
    }

    @TypeConverter
    fun localDateToString(date: LocalDate?): String? {
        return date?.format(formatter)
    }
}

/**
 * Convert [TaskDetailType] to path if it can convert, otherwise return null.
 *
 * @return the path String or `null`
 */
fun TaskDetailType.pathStringConverter(data: ByteArray): String? {
    return when (this) {
        TaskDetailType.Text -> null
        TaskDetailType.URL -> null
        TaskDetailType.Application -> null
        TaskDetailType.Picture -> data.decodeToString()
        TaskDetailType.Video -> data.decodeToString()
        TaskDetailType.Audio -> data.decodeToString()
    }
}
