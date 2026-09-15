package sqz.checklist.data.settings.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.core.FileStorage
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferencesFileSerializer
import java.io.File

/** Creates the JVM settings DataStore below the application-owned [directory]. */
fun createJvmSettingsDataStore(directory: File): DataStore<Preferences> =
    createSettingsDataStore(
        storage = FileStorage(
            serializer = PreferencesFileSerializer,
            produceFile = { directory.resolve(SETTINGS_DATA_STORE_FILE_NAME) },
        ),
    )
