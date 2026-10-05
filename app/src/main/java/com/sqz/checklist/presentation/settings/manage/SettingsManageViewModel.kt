package com.sqz.checklist.presentation.settings.manage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import sqz.checklist.model.settings.AppSettings
import sqz.checklist.model.settings.AppTheme
import sqz.checklist.settings.api.manage.Settings

/**
 * Translates settings UI intents into feature API mutations.
 *
 * Add one semantic method when a writable setting is added; item composables must call these
 * methods instead of launching their own coroutines or depending directly on [Settings].
 */
class SettingsManageViewModel(
    private val settings: Settings,
) : ViewModel() {
    /** Latest immutable settings snapshot consumed by [SettingsManageLayout]. */
    val state: StateFlow<AppSettings> = settings.state

    /** Updates the persisted application theme. */
    fun setTheme(value: AppTheme) = update { settings.setTheme(value) }

    /** Updates the maximum retained history count. */
    fun setAllowedNumberOfHistory(value: Int) = update {
        settings.setAllowedNumberOfHistory(value)
    }

    /** Controls whether history is cleared when leaving the history screen. */
    fun setClearHistoryWhenLeaving(value: Boolean) = update {
        settings.setClearHistoryWhenLeaving(value)
    }

    /** Controls availability of history undo actions. */
    fun setUndoEnabled(value: Boolean) = update { settings.setUndoEnabled(value) }

    /** Updates image compression quality, where zero preserves the original. */
    fun setPictureCompressionPercent(value: Int) = update {
        settings.setPictureCompressionPercent(value)
    }

    /** Updates video compression quality, where zero preserves the original. */
    fun setVideoCompressionPercent(value: Int) = update {
        settings.setVideoCompressionPercent(value)
    }

    /** Controls removal of a notification after its reminder is handled. */
    fun setRemoveNotificationWhenReminded(value: Boolean) = update {
        settings.setRemoveNotificationWhenReminded(value)
    }

    /** Controls whether the exact-alarm permission warning is shown. */
    fun setShowExactAlarmWarning(value: Boolean) = update {
        settings.setShowExactAlarmWarning(value)
    }

    /** Updates how long completed reminders remain visible. */
    fun setRecentlyRemindedKeepTimeMillis(value: Long) = update {
        settings.setRecentlyRemindedKeepTimeMillis(value)
    }

    /** Controls notification removal when corresponding history is deleted. */
    fun setRemoveNotificationWhenHistoryDeleted(value: Boolean) = update {
        settings.setRemoveNotificationWhenHistoryDeleted(value)
    }

    /** Runs a feature mutation in the ViewModel lifecycle scope. */
    private fun update(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }
}
