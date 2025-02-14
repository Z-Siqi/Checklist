package com.sqz.checklist.preferences

import android.content.Context

open class PreferencesAccess(private val context: Context) {
    private val preferencesFileName = "preferences"
    open fun preferencesFileName(): String = this.preferencesFileName

    fun readPreferencesState(name: String, default: Boolean = false): Boolean {
        val sharedPreferences =
            context.getSharedPreferences(this.preferencesFileName(), Context.MODE_PRIVATE)
        return sharedPreferences.getBoolean(name, default)
    }

    fun writePreferencesState(name: String, state: Boolean) {
        val sharedPreferences =
            context.getSharedPreferences(this.preferencesFileName(), Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putBoolean(name, state)
        editor.apply()
    }

    fun readPreferencesState(name: String, default: Int = 1): Int {
        val sharedPreferences =
            context.getSharedPreferences(this.preferencesFileName(), Context.MODE_PRIVATE)
        return sharedPreferences.getInt(name, default)
    }

    fun writePreferencesState(name: String, state: Int) {
        val sharedPreferences =
            context.getSharedPreferences(this.preferencesFileName(), Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putInt(name, state)
        editor.apply()
    }
}
