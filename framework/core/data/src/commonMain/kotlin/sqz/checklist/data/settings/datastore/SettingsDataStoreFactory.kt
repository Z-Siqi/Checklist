package sqz.checklist.data.settings.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.DataMigration
import androidx.datastore.core.Storage
import androidx.datastore.preferences.core.Preferences

const val SETTINGS_DATA_STORE_FILE_NAME = "checklist.settings.preferences_pb"

/** Creates a settings DataStore over platform-provided [storage] and optional [migrations]. */
fun createSettingsDataStore(
    storage: Storage<Preferences>,
    migrations: List<DataMigration<Preferences>> = emptyList(),
): DataStore<Preferences> = DataStoreFactory.create(
    storage = storage,
    migrations = migrations,
)
