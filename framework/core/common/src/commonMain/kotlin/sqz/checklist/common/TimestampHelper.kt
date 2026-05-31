package sqz.checklist.common

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

object TimestampHelper {

    /** GMT: Saturday, January 1, 2000, at 12:00:00 AM **/
    const val TWENTY_FIRST_CENTURY: Long = 946684800000L

    fun toLocalDateTime(timestamp: Long): LocalDateTime {
        val toInstant = Instant.fromEpochMilliseconds(timestamp)
        return toInstant.toLocalDateTime(TimeZone.currentSystemDefault())
    }

    fun toLocalDate(timestamp: Long): LocalDate {
        return toLocalDateTime(timestamp).date
    }
}
