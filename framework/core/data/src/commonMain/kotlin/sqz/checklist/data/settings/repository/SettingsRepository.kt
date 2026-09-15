package sqz.checklist.data.settings.repository

import kotlinx.coroutines.flow.StateFlow
import sqz.checklist.model.settings.AppSettings

/** Persists and publishes the complete set of platform-independent application settings. */
interface SettingsRepository {
    /** The latest validated settings snapshot available to application consumers. */
    val settings: StateFlow<AppSettings>

    /** Atomically transforms and persists the current settings snapshot. */
    suspend fun update(transform: (AppSettings) -> AppSettings)

    /** Atomically replaces all persisted settings, primarily during backup restoration. */
    suspend fun replace(settings: AppSettings)
}
