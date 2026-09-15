package com.sqz.checklist.assembly.settings

import android.content.Context
import sqz.checklist.data.settings.datastore.AndroidSettingsRepositoryProvider
import sqz.checklist.settings.api.SettingsFeature
import sqz.checklist.settings.api.settingsProvider

/** Owns the application-wide settings feature assembled from the Android repository. */
object SettingsAssembly {
    @Volatile
    private var instance: SettingsFeature? = null

    /** Returns the process-wide feature instance initialized with application context. */
    fun get(context: Context): SettingsFeature = instance ?: synchronized(this) {
        instance ?: settingsProvider(
            AndroidSettingsRepositoryProvider.get(context.applicationContext),
        ).also { instance = it }
    }
}
