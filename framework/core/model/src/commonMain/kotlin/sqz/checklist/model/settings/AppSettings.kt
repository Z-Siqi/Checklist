package sqz.checklist.model.settings

import kotlinx.serialization.Serializable

/**
 * Immutable platform-independent snapshot of all application settings.
 *
 * Add a setting to the smallest matching domain model and define its default there. After adding
 * it, update the DataStore key inventory and both mapper directions, the non-default mapper test
 * fixture, the settings feature mutation API when writable, and every app/feature configuration
 * projection that consumes it. Serialization automatically includes model properties, so a
 * compatible optional addition does not by itself require a backup format version change.
 *
 * Before deleting a setting, remove its consumers and mutation API, then remove both DataStore
 * mapper directions and its active key. Preserve explicit migration or deprecated-key recognition
 * when existing installations may still contain the old value.
 */
@Serializable
data class AppSettings(
    val appearance: AppearanceSettings = AppearanceSettings(),
    val history: HistorySettings = HistorySettings(),
    val media: MediaSettings = MediaSettings(),
    val reminder: ReminderSettings = ReminderSettings(),
)
