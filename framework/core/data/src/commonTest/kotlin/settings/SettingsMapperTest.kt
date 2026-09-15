package sqz.checklist.data.settings.datastore

import androidx.datastore.preferences.core.mutablePreferencesOf
import kotlin.test.Test
import kotlin.test.assertEquals
import sqz.checklist.model.settings.AppSettings
import sqz.checklist.model.settings.AppTheme
import sqz.checklist.model.settings.AppearanceSettings
import sqz.checklist.model.settings.HistorySettings
import sqz.checklist.model.settings.MediaSettings
import sqz.checklist.model.settings.ReminderSettings

class SettingsMapperTest {
    @Test
    fun emptyPreferencesUseModelDefaults() {
        assertEquals(AppSettings(), mutablePreferencesOf().toAppSettings())
    }

    @Test
    fun nonDefaultSettingsRoundTrip() {
        val expected = nonDefaultSettings()
        val preferences = mutablePreferencesOf()

        preferences.write(expected)

        assertEquals(expected, preferences.toAppSettings())
        assertEquals(SettingsKeys.active, preferences.asMap().keys)
    }

    @Test
    fun invalidPersistedValuesFallBackToDefaults() {
        val preferences = mutablePreferencesOf(
            SettingsKeys.allowedNumberOfHistory to -1,
            SettingsKeys.pictureCompressionPercent to 101,
            SettingsKeys.videoCompressionPercent to -1,
            SettingsKeys.recentlyRemindedKeepTimeMillis to -1L,
            SettingsKeys.appTheme to "MissingTheme",
        )

        assertEquals(AppSettings(), preferences.toAppSettings())
    }

    /**
     * Provides a non-default value for every persisted setting.
     *
     * A newly added setting must be represented here with a value different from its model
     * default. Otherwise a missing read or write mapping could accidentally pass the round-trip
     * test because both sides would resolve to the same default.
     */
    private fun nonDefaultSettings() = AppSettings(
        appearance = AppearanceSettings(AppTheme.Colorful),
        history = HistorySettings(
            allowedNumberOfHistory = 17,
            clearHistoryWhenLeaving = true,
            undoEnabled = false,
        ),
        media = MediaSettings(
            pictureCompressionPercent = 63,
            videoCompressionPercent = 41,
        ),
        reminder = ReminderSettings(
            removeNotificationWhenReminded = false,
            showExactAlarmWarning = false,
            recentlyRemindedKeepTimeMillis = 86_400_000L,
            removeNotificationWhenHistoryDeleted = true,
        ),
    )
}
