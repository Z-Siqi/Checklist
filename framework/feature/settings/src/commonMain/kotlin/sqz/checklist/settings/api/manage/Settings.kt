package sqz.checklist.settings.api.manage

import kotlinx.coroutines.flow.StateFlow
import sqz.checklist.model.settings.AppSettings
import sqz.checklist.model.settings.AppTheme

/**
 * Exposes validated settings mutations and a reactive application settings snapshot.
 *
 * Add a dedicated mutation method when a new setting is user-writable or changed by application
 * business logic. The implementation must validate the value before updating only its owning
 * settings group. Read-only derived values do not need a setter. When deleting a setting, remove
 * its method together with every caller; persistence cleanup remains the responsibility of the
 * data layer rather than this API.
 */
interface Settings {
    /** The current application settings, updated after local writes or backup restoration. */
    val state: StateFlow<AppSettings>

    /** Selects the application color theme. */
    suspend fun setTheme(theme: AppTheme)

    /** Sets the retained history item limit; zero disables history retention. */
    suspend fun setAllowedNumberOfHistory(value: Int)

    /** Controls whether history is cleared when the application leaves the foreground. */
    suspend fun setClearHistoryWhenLeaving(enabled: Boolean)

    /** Controls whether task-list undo actions are available. */
    suspend fun setUndoEnabled(enabled: Boolean)

    /** Sets picture compression in the inclusive range from 0 to 100. */
    suspend fun setPictureCompressionPercent(value: Int)

    /** Sets video compression in the inclusive range from 0 to 100. */
    suspend fun setVideoCompressionPercent(value: Int)

    /** Controls notification removal after a reminder is completed. */
    suspend fun setRemoveNotificationWhenReminded(enabled: Boolean)

    /** Controls whether missing exact-alarm permission produces a warning. */
    suspend fun setShowExactAlarmWarning(enabled: Boolean)

    /** Sets how long recently reminded tasks are retained, in milliseconds. */
    suspend fun setRecentlyRemindedKeepTimeMillis(value: Long)

    /** Controls notification removal when associated history is deleted. */
    suspend fun setRemoveNotificationWhenHistoryDeleted(enabled: Boolean)
}
