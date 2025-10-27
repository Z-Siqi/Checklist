package com.sqz.checklist.preferences

import android.content.Context

class PreferencesInCache(context: Context) : PreferencesAccess(context) {
    private val preferencesFileName = "cache_preferences"
    override fun preferencesFileName(): String = this.preferencesFileName

    private val nullStringName = "_!N/A"

    fun waitingDeletedCacheName(setter: String? = nullStringName): String? {
        val preferences = "waiting_deleted_cache_name"
        if (setter != nullStringName) writePreferencesState(preferences, setter)
        return readPreferencesState(preferences, null)
    }

    fun checkBackgroundManageApp(setter: Boolean? = false): Boolean {
        val preferences = "check_bg_manage_app"
        if (setter != null) writePreferencesState(preferences, setter)
        return readPreferencesState(preferences, false)
    }

    fun errFileNameSaver(setter: String? = nullStringName): String? {
        val preferences = "err_file_name_saver"
        if (setter != nullStringName) writePreferencesState(preferences, setter)
        return readPreferencesState(preferences, null)
    }

    fun inProcessFilesPath(setter: String? = nullStringName): String? {
        val preferences = "in_process_files_path"
        if (setter != nullStringName) writePreferencesState(preferences, setter)
        return readPreferencesState(preferences, null)
    }

    fun backupSettings(setter: Boolean? = null): Boolean {
        val preferences = "backup_settings"
        if (setter != null) writePreferencesState(preferences, setter)
        return readPreferencesState(preferences, true)
    }

    fun restoreSettings(setter: Boolean? = null): Boolean {
        val preferences = "restore_settings"
        if (setter != null) writePreferencesState(preferences, setter)
        return readPreferencesState(preferences, false)
    }

    fun backupOption(setter: Int? = null): Int {
        val preferences = "backup_option"
        if (setter != null) writePreferencesState(preferences, setter)
        return readPreferencesState(preferences, 0)
    }
}
