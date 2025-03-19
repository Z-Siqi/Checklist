package com.sqz.checklist.preferences

import android.content.Context

class PreferencesInCache(context: Context) : PreferencesAccess(context) {
    private val preferencesFileName = "cache_preferences"
    override fun preferencesFileName(): String = this.preferencesFileName

    fun waitingDeletedCacheName(setter: String? = "_!N/A"): String? {
        val preferences = "waiting_deleted_cache_name"
        if (setter != "_!N/A") writePreferencesState(preferences, setter)
        return readPreferencesState(preferences, null)
    }

    fun checkBackgroundManageApp(setter: Boolean? = false): Boolean {
        val preferences = "check_bg_manage_app"
        if (setter != null) writePreferencesState(preferences, setter)
        return readPreferencesState(preferences, false)
    }
}
