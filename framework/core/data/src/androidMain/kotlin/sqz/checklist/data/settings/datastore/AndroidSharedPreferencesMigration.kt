package sqz.checklist.data.settings.datastore

import android.content.Context
import androidx.datastore.core.DataMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.mutablePreferencesOf
import sqz.checklist.model.settings.AppSettings
import sqz.checklist.model.settings.AppTheme

/** Migrates the legacy Android SharedPreferences file into the cross-platform settings model. */
internal class AndroidSharedPreferencesMigration(
    context: Context,
) : DataMigration<Preferences> {
    private val legacy = context.getSharedPreferences(LEGACY_FILE_NAME, Context.MODE_PRIVATE)

    /** Runs only when DataStore is empty and the legacy file contains persisted values. */
    override suspend fun shouldMigrate(currentData: Preferences): Boolean =
        currentData.asMap().isEmpty() && legacy.all.isNotEmpty()

    /** Maps legacy keys, including inverted booleans, into active DataStore keys. */
    override suspend fun migrate(currentData: Preferences): Preferences {
        val defaults = AppSettings()
        val migrated = defaults.copy(
            appearance = defaults.appearance.copy(
                theme = when (legacy.getInt("app_theme", 0)) {
                    1 -> AppTheme.Colorful
                    else -> AppTheme.Pastel
                },
            ),
            history = defaults.history.copy(
                allowedNumberOfHistory = legacy.getInt("allowed_number_of_history", 5)
                    .takeIf { it >= 0 } ?: defaults.history.allowedNumberOfHistory,
                clearHistoryWhenLeaving = legacy.getBoolean("clear_history_when_leaved", false),
                undoEnabled = !legacy.getBoolean("disable_undo_button", false),
            ),
            media = defaults.media.copy(
                pictureCompressionPercent = legacy.getInt("picture_compression_rate", 20)
                    .takeIf { it in 0..100 } ?: defaults.media.pictureCompressionPercent,
                videoCompressionPercent = legacy.getInt("video_compression_rate", 0)
                    .takeIf { it in 0..100 } ?: defaults.media.videoCompressionPercent,
            ),
            reminder = defaults.reminder.copy(
                removeNotificationWhenReminded =
                    !legacy.getBoolean("disable_remove_notify_in_reminded", false),
                showExactAlarmWarning =
                    !legacy.getBoolean("disable_no_schedule_exact_alarm_notice", false),
                recentlyRemindedKeepTimeMillis =
                    legacy.getLong("recently_reminded_keep_time", 43_200_000L)
                        .takeIf { it >= 0L } ?: defaults.reminder.recentlyRemindedKeepTimeMillis,
                removeNotificationWhenHistoryDeleted =
                    legacy.getBoolean("remove_notice_in_auto_del_reminded", false),
            ),
        )
        return mutablePreferencesOf().apply { write(migrated) }.toPreferences()
    }

    /** Retains the legacy file while deprecated compatibility readers remain available. */
    override suspend fun cleanUp() = Unit

    private companion object {
        const val LEGACY_FILE_NAME = "primary_preferences"
    }
}
