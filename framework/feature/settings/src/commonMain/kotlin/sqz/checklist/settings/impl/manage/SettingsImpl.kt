package sqz.checklist.settings.impl.manage

import sqz.checklist.data.settings.repository.SettingsRepository
import sqz.checklist.model.settings.AppTheme
import sqz.checklist.settings.api.manage.Settings

/**
 * Applies settings use-case validation before delegating persistence to the repository.
 *
 * Each writable setting declared by the public `Settings` contract must have one implementation
 * here. A new method must validate its domain range before copying only the owning nested model;
 * deleting a setting requires deleting the corresponding method rather than leaving a no-op
 * compatibility path in the feature API.
 */
internal class SettingsImpl(
    private val repository: SettingsRepository,
) : Settings {
    override val state = repository.settings

    /** Persists [theme]. */
    override suspend fun setTheme(theme: AppTheme) = repository.update {
        it.copy(appearance = it.appearance.copy(theme = theme))
    }

    /** Persists a non-negative history limit. */
    override suspend fun setAllowedNumberOfHistory(value: Int) {
        require(value >= 0)
        repository.update { it.copy(history = it.history.copy(allowedNumberOfHistory = value)) }
    }

    /** Persists the clear-on-leave behavior. */
    override suspend fun setClearHistoryWhenLeaving(enabled: Boolean) = repository.update {
        it.copy(history = it.history.copy(clearHistoryWhenLeaving = enabled))
    }

    /** Persists task-list undo availability. */
    override suspend fun setUndoEnabled(enabled: Boolean) = repository.update {
        it.copy(history = it.history.copy(undoEnabled = enabled))
    }

    /** Persists a validated picture compression percentage. */
    override suspend fun setPictureCompressionPercent(value: Int) {
        require(value in 0..100)
        repository.update { it.copy(media = it.media.copy(pictureCompressionPercent = value)) }
    }

    /** Persists a validated video compression percentage. */
    override suspend fun setVideoCompressionPercent(value: Int) {
        require(value in 0..100)
        repository.update { it.copy(media = it.media.copy(videoCompressionPercent = value)) }
    }

    /** Persists completed-reminder notification removal behavior. */
    override suspend fun setRemoveNotificationWhenReminded(enabled: Boolean) = repository.update {
        it.copy(reminder = it.reminder.copy(removeNotificationWhenReminded = enabled))
    }

    /** Persists exact-alarm warning visibility. */
    override suspend fun setShowExactAlarmWarning(enabled: Boolean) = repository.update {
        it.copy(reminder = it.reminder.copy(showExactAlarmWarning = enabled))
    }

    /** Persists a non-negative recently-reminded retention duration. */
    override suspend fun setRecentlyRemindedKeepTimeMillis(value: Long) {
        require(value >= 0L)
        repository.update {
            it.copy(reminder = it.reminder.copy(recentlyRemindedKeepTimeMillis = value))
        }
    }

    /** Persists history-deletion notification removal behavior. */
    override suspend fun setRemoveNotificationWhenHistoryDeleted(enabled: Boolean)  {
        repository.update {
            it.copy(reminder = it.reminder.copy(removeNotificationWhenHistoryDeleted = enabled))
        }
    }
}
