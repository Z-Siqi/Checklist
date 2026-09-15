package sqz.checklist.data.settings.datastore

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import sqz.checklist.data.settings.repository.SettingsRepository

/** Owns the single Android settings repository and its application-scoped DataStore. */
object AndroidSettingsRepositoryProvider {
    @Volatile
    private var instance: SettingsRepository? = null

    /** Returns the process-wide repository, creating it from [context] when first requested. */
    fun get(context: Context): SettingsRepository = instance ?: synchronized(this) {
        instance ?: create(context.applicationContext).also { instance = it }
    }

    /** Creates a repository whose initial StateFlow value already reflects persisted settings. */
    private fun create(context: Context): SettingsRepository {
        val dataStore = createAndroidSettingsDataStore(context)
        val initial = runBlocking(Dispatchers.IO) { dataStore.data.first().toAppSettings() }
        return DataStoreSettingsRepository(
            dataStore = dataStore,
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
            initialSettings = initial,
        )
    }
}
