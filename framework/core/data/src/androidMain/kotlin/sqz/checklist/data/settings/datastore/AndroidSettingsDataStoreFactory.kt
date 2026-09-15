package sqz.checklist.data.settings.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.FileStorage
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferencesFileSerializer

/** Creates the Android settings DataStore in the application-private files directory. */
fun createAndroidSettingsDataStore(context: Context): DataStore<Preferences> =
    createSettingsDataStore(
        storage = FileStorage(
            serializer = PreferencesFileSerializer,
            produceFile = {
                context.applicationContext.filesDir.resolve(SETTINGS_DATA_STORE_FILE_NAME)
            },
        ),
        migrations = listOf(AndroidSharedPreferencesMigration(context.applicationContext)),
    )
