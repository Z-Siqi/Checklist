package com.sqz.checklist.preferences

import android.content.Context

open class PreferencesAccess(private val context: Context) {
    private val preferencesFileName = "preferences"
    open fun preferencesFileName(): String = this.preferencesFileName

    /** Boolean getter. default: if no data was set **/
    fun readPreferencesState(name: String, default: Boolean = false): Boolean {
        val sharedPreferences =
            context.getSharedPreferences(this.preferencesFileName(), Context.MODE_PRIVATE)
        return sharedPreferences.getBoolean(name, default)
    }

    /** Boolean setter **/
    fun writePreferencesState(name: String, state: Boolean) {
        val sharedPreferences =
            context.getSharedPreferences(this.preferencesFileName(), Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putBoolean(name, state)
        editor.apply()
    }

    /** Int getter. default: if no data was set **/
    fun readPreferencesState(name: String, default: Int = 1): Int {
        val sharedPreferences =
            context.getSharedPreferences(this.preferencesFileName(), Context.MODE_PRIVATE)
        return sharedPreferences.getInt(name, default)
    }

    /** Int setter **/
    fun writePreferencesState(name: String, state: Int) {
        val sharedPreferences =
            context.getSharedPreferences(this.preferencesFileName(), Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putInt(name, state)
        editor.apply()
    }

    /** String getter. default: if no data was set **/
    fun readPreferencesState(name: String, default: String? = null): String? {
        val sharedPreferences =
            context.getSharedPreferences(this.preferencesFileName(), Context.MODE_PRIVATE)
        return sharedPreferences.getString(name, default)
    }

    /** String setter **/
    fun writePreferencesState(name: String, state: String?) {
        val sharedPreferences =
            context.getSharedPreferences(this.preferencesFileName(), Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString(name, state)
        editor.apply()
    }
}
