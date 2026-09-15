package sqz.checklist.data.settings.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.core.okio.OkioStorage
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferencesSerializer
import okio.FileSystem
import okio.Path.Companion.toPath

/** Creates the iOS settings DataStore below the application-owned [directoryPath]. */
fun createIosSettingsDataStore(directoryPath: String): DataStore<Preferences> =
    createSettingsDataStore(
        storage = OkioStorage(
            fileSystem = FileSystem.SYSTEM,
            serializer = PreferencesSerializer,
            producePath = { "$directoryPath/$SETTINGS_DATA_STORE_FILE_NAME".toPath() },
        ),
    )
