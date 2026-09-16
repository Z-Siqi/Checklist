package sqz.checklist.data.settings.datastore

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import sqz.checklist.model.settings.AppSettings
import sqz.checklist.model.settings.AppTheme
import sqz.checklist.model.settings.AppearanceSettings
import sqz.checklist.model.settings.HistorySettings
import sqz.checklist.model.settings.MediaSettings
import sqz.checklist.model.settings.ReminderSettings

/**
 * Converts persisted preferences into a validated application settings snapshot.
 *
 * Maintenance contract for adding a setting:
 * 1. Add the property and its single source of truth default to the matching model under
 *    `sqz.checklist.model.settings`.
 * 2. Declare a stable, namespaced key in [SettingsKeys] and add it to [SettingsKeys.active].
 * 3. Read the key in this method. Missing or invalid values must fall back to the corresponding
 *    property on `defaults`, never to a duplicated literal default.
 * 4. Write the property in [write].
 * 5. Give the non-default round-trip fixture in `SettingsMapperTest` a non-default value. The
 *    active-key assertion then detects a missing key registration or write mapping.
 *
 * Maintenance contract for removing a setting:
 * 1. Stop reading it here and stop writing it in [write].
 * 2. Remove it from the settings model and [SettingsKeys.active].
 * 3. Retain its old key as an explicitly deprecated key when stored data must be cleaned up.
 *    Do not delete arbitrary unknown keys because they may belong to a newer app version.
 * 4. Remove its mutation method from the settings feature and remove all presentation/config
 *    consumers.
 *
 * A rename or semantic change is not a deletion plus an addition. Preserve the old key and add
 * an explicit, idempotent migration that converts its value before retiring it.
 */
internal fun Preferences.toAppSettings(): AppSettings {
    val defaults = AppSettings()

    val appearanceSettings = AppearanceSettings(
        theme = this[SettingsKeys.appTheme]
            ?.let { value -> AppTheme.entries.firstOrNull { it.name == value } }
            ?: defaults.appearance.theme,
    )
    val historySettings = HistorySettings(
        allowedNumberOfHistory = this[SettingsKeys.allowedNumberOfHistory]
            ?.takeIf { it in 0..21 }
            ?: defaults.history.allowedNumberOfHistory,

        clearHistoryWhenLeaving = this[SettingsKeys.clearHistoryWhenLeaving]
            ?: defaults.history.clearHistoryWhenLeaving,

        undoEnabled = this[SettingsKeys.undoEnabled]
            ?: defaults.history.undoEnabled,
    )
    val mediaSettings = MediaSettings(
        pictureCompressionPercent = this[SettingsKeys.pictureCompressionPercent]
            ?.takeIf { it in 0..100 }
            ?: defaults.media.pictureCompressionPercent,

        videoCompressionPercent = this[SettingsKeys.videoCompressionPercent]
            ?.takeIf { it in 0..100 }
            ?: defaults.media.videoCompressionPercent,
    )
    val reminderSettings = ReminderSettings(
        removeNotificationWhenReminded = this[SettingsKeys.removeNotificationWhenReminded]
            ?: defaults.reminder.removeNotificationWhenReminded,

        showExactAlarmWarning = this[SettingsKeys.showExactAlarmWarning]
            ?: defaults.reminder.showExactAlarmWarning,

        recentlyRemindedKeepTimeMillis = this[SettingsKeys.recentlyRemindedKeepTimeMillis]
            ?.takeIf { it >= 0L }
            ?: defaults.reminder.recentlyRemindedKeepTimeMillis,

        removeNotificationWhenHistoryDeleted = this[SettingsKeys.removeNotificationWhenHistoryDeleted]
            ?: defaults.reminder.removeNotificationWhenHistoryDeleted,
    )

    return AppSettings(
        appearance = appearanceSettings,
        history = historySettings,
        media = mediaSettings,
        reminder = reminderSettings,
    )
}

/**
 * Writes every active property from [settings] to its stable Preferences key.
 *
 * Every property read by [toAppSettings] must have a matching assignment here. When adding or
 * removing a property, update both functions in the same change and update the non-default
 * round-trip fixture in `SettingsMapperTest`. This function intentionally does not clear the
 * target preferences: normal updates must preserve unknown keys written by newer app versions.
 * Full backup restoration clears stale data in `DataStoreSettingsRepository.replace` before
 * invoking this function.
 */
internal fun MutablePreferences.write(settings: AppSettings) {
    settings.appearance.theme.name.let {
        this[SettingsKeys.appTheme] = it
    }
    settings.history.allowedNumberOfHistory.let {
        this[SettingsKeys.allowedNumberOfHistory] = it
    }
    settings.history.clearHistoryWhenLeaving.let {
        this[SettingsKeys.clearHistoryWhenLeaving] = it
    }
    settings.history.undoEnabled.let {
        this[SettingsKeys.undoEnabled] = it
    }
    settings.media.pictureCompressionPercent.let {
        this[SettingsKeys.pictureCompressionPercent] = it
    }
    settings.media.videoCompressionPercent.let {
        this[SettingsKeys.videoCompressionPercent] = it
    }
    settings.reminder.removeNotificationWhenReminded.let {
        this[SettingsKeys.removeNotificationWhenReminded] = it
    }
    settings.reminder.showExactAlarmWarning.let {
        this[SettingsKeys.showExactAlarmWarning] = it
    }
    settings.reminder.recentlyRemindedKeepTimeMillis.let {
        this[SettingsKeys.recentlyRemindedKeepTimeMillis] = it
    }
    settings.reminder.removeNotificationWhenHistoryDeleted.let {
        this[SettingsKeys.removeNotificationWhenHistoryDeleted] = it
    }
}
