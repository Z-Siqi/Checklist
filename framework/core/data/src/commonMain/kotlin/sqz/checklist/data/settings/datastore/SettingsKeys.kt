package sqz.checklist.data.settings.datastore

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

/**
 * Defines the stable persisted names and active-key inventory for application settings.
 *
 * Add a key here whenever a persistent model property is added, use a domain-prefixed name such
 * as `history.some_option`, and register it in [active]. A released key name is storage protocol:
 * renaming the Kotlin property does not justify renaming the persisted key.
 *
 * When a setting is removed, remove its key from [active] so round-trip tests reflect the current
 * model. Keep the exact legacy name in [deprecatedNames] only when cleanup or migration code still
 * needs to recognize it. Never derive cleanup candidates as "all keys not in active" because an
 * older app must not erase settings created by a newer app.
 */
internal object SettingsKeys {
    val appTheme = stringPreferencesKey("appearance.app_theme")
    val allowedNumberOfHistory = intPreferencesKey("history.allowed_number")
    val clearHistoryWhenLeaving = booleanPreferencesKey("history.clear_when_leaving")
    val undoEnabled = booleanPreferencesKey("history.undo_enabled")
    val pictureCompressionPercent = intPreferencesKey("media.picture_compression_percent")
    val videoCompressionPercent = intPreferencesKey("media.video_compression_percent")
    val removeNotificationWhenReminded = booleanPreferencesKey("reminder.remove_when_reminded")
    val showExactAlarmWarning = booleanPreferencesKey("reminder.show_exact_alarm_warning")
    val recentlyRemindedKeepTimeMillis = longPreferencesKey("reminder.keep_time_millis")
    val removeNotificationWhenHistoryDeleted =
        booleanPreferencesKey("reminder.remove_when_history_deleted")

    /** All keys that [write] must emit for a complete settings snapshot. */
    val active: Set<Preferences.Key<*>> = setOf(
        appTheme,
        allowedNumberOfHistory,
        clearHistoryWhenLeaving,
        undoEnabled,
        pictureCompressionPercent,
        videoCompressionPercent,
        removeNotificationWhenReminded,
        showExactAlarmWarning,
        recentlyRemindedKeepTimeMillis,
        removeNotificationWhenHistoryDeleted,
    )

    /** Legacy names recognized only by explicit migration or cleanup compatibility code. */
    val deprecatedNames = setOf(
        "disable_undo_button",
        "disable_remove_notify_in_reminded",
        "disable_no_schedule_exact_alarm_notice",
        "remove_notice_in_auto_del_reminded",
    )
}
