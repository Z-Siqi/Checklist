package sqz.checklist.data.preferences

import android.content.Context
import androidx.annotation.IntRange
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import sqz.checklist.data.settings.datastore.AndroidSettingsRepositoryProvider
import sqz.checklist.data.settings.repository.SettingsRepository
import sqz.checklist.model.settings.AppTheme

@Deprecated(
    message = "Use the settings feature API. This class remains for source compatibility.",
)
class PrimaryPreferences(context: Context) {
    private val repository: SettingsRepository = AndroidSettingsRepositoryProvider.get(context)

    /** Reads or updates the legacy integer representation of the selected theme. */
    fun appTheme(setter: Int? = null): Int {
        setter?.let { value -> update { settings ->
            settings.copy(
                appearance = settings.appearance.copy(
                    theme = if (value == 1) AppTheme.Colorful else AppTheme.Pastel,
                ),
            )
        } }
        return if (repository.settings.value.appearance.theme == AppTheme.Colorful) 1 else 0
    }

    /** Reads or updates the maximum retained history item count. */
    fun allowedNumberOfHistory(setter: Int? = null): Int {
        setter?.let { value -> update { settings ->
            settings.copy(history = settings.history.copy(allowedNumberOfHistory = value))
        } }
        return repository.settings.value.history.allowedNumberOfHistory
    }

    /** Reads or updates the inverse legacy notification-removal flag. */
    fun disableRemoveNotifyInReminded(setter: Boolean? = null): Boolean {
        setter?.let { value -> update { settings ->
            settings.copy(
                reminder = settings.reminder.copy(removeNotificationWhenReminded = !value),
            )
        } }
        return !repository.settings.value.reminder.removeNotificationWhenReminded
    }

    /** Reads or updates the inverse legacy exact-alarm-warning flag. */
    fun disableNoScheduleExactAlarmNotice(setter: Boolean? = null): Boolean {
        setter?.let { value -> update { settings ->
            settings.copy(reminder = settings.reminder.copy(showExactAlarmWarning = !value))
        } }
        return !repository.settings.value.reminder.showExactAlarmWarning
    }

    /** Reads or updates the recently-reminded retention duration in milliseconds. */
    fun recentlyRemindedKeepTime(setter: Long? = null): Long {
        setter?.let { value -> update { settings ->
            settings.copy(
                reminder = settings.reminder.copy(recentlyRemindedKeepTimeMillis = value),
            )
        } }
        return repository.settings.value.reminder.recentlyRemindedKeepTimeMillis
    }

    /** Reads or updates the picture compression percentage. */
    fun pictureCompressionRate(@IntRange(from = 0, to = 100) setter: Int? = null): Int {
        setter?.let { value -> update { settings ->
            settings.copy(media = settings.media.copy(pictureCompressionPercent = value))
        } }
        return repository.settings.value.media.pictureCompressionPercent
    }

    /** Reads or updates the video compression percentage. */
    fun videoCompressionRate(@IntRange(from = 0, to = 100) setter: Int? = null): Int {
        setter?.let { value -> update { settings ->
            settings.copy(media = settings.media.copy(videoCompressionPercent = value))
        } }
        return repository.settings.value.media.videoCompressionPercent
    }

    /** Reads or updates notification removal after history deletion. */
    fun removeNoticeInAutoDelReminded(setter: Boolean? = null): Boolean {
        setter?.let { value -> update { settings ->
            settings.copy(
                reminder = settings.reminder.copy(removeNotificationWhenHistoryDeleted = value),
            )
        } }
        return repository.settings.value.reminder.removeNotificationWhenHistoryDeleted
    }

    /** Reads or updates whether history is cleared when the application leaves. */
    fun clearHistoryWhenLeaved(setter: Boolean? = null): Boolean {
        setter?.let { value -> update { settings ->
            settings.copy(history = settings.history.copy(clearHistoryWhenLeaving = value))
        } }
        return repository.settings.value.history.clearHistoryWhenLeaving
    }

    /** Reads or updates the inverse legacy undo-availability flag. */
    fun disableUndoButton(setter: Boolean? = null): Boolean {
        setter?.let { value -> update { settings ->
            settings.copy(history = settings.history.copy(undoEnabled = !value))
        } }
        return !repository.settings.value.history.undoEnabled
    }

    /** Runs a compatibility update synchronously so legacy callers observe the new value. */
    private fun update(transform: (sqz.checklist.model.settings.AppSettings) -> sqz.checklist.model.settings.AppSettings) {
        runBlocking(Dispatchers.IO) { repository.update(transform) }
    }
}
