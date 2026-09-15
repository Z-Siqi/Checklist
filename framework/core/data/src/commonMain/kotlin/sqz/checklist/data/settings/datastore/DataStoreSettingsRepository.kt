package sqz.checklist.data.settings.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import sqz.checklist.data.settings.repository.SettingsRepository
import sqz.checklist.model.settings.AppSettings

/** Implements [SettingsRepository] with one application-scoped Preferences DataStore. */
class DataStoreSettingsRepository(
    private val dataStore: DataStore<Preferences>,
    scope: CoroutineScope,
    initialSettings: AppSettings = AppSettings(),
) : SettingsRepository {
    private val mutableSettings = MutableStateFlow(initialSettings)
    override val settings: StateFlow<AppSettings> = mutableSettings.asStateFlow()

    init {
        scope.launch {
            dataStore.data.collect { preferences ->
                mutableSettings.value = preferences.toAppSettings()
            }
        }
    }

    /** Applies [transform] in a DataStore transaction and publishes the persisted result. */
    override suspend fun update(transform: (AppSettings) -> AppSettings) {
        val updated = dataStore.edit { preferences ->
            preferences.write(transform(preferences.toAppSettings()).requireValid())
        }
        mutableSettings.value = updated.toAppSettings()
    }

    /** Clears stale keys and atomically persists the validated replacement snapshot. */
    override suspend fun replace(settings: AppSettings) {
        val validated = settings.requireValid()
        val updated = dataStore.edit { preferences ->
            preferences.clear()
            preferences.write(validated)
        }
        mutableSettings.value = updated.toAppSettings()
    }
}
