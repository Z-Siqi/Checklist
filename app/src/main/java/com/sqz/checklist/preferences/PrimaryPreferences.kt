package com.sqz.checklist.preferences

import android.content.Context

class PrimaryPreferences(context: Context) : PreferencesAccess(context) {
    private val preferencesFileName = "primary_preferences"
    override fun preferencesFileName(): String = preferencesFileName

    fun allowedNumberOfHistory(setter: Int? = null): Int {
        val preferences = "allowed_number_of_history"
        if (setter != null) writePreferencesState(preferences, setter)
        return readPreferencesState(preferences, 5)
    }

    fun disableRemoveNotifyInReminded(setter: Boolean? = null): Boolean {
        val preferences = "disable_remove_notify_in_reminded"
        if (setter != null) writePreferencesState(preferences, setter)
        return readPreferencesState(preferences, false)
    }

    fun disableNoScheduleExactAlarmNotice(setter: Boolean? = null): Boolean {
        val preferences = "disable_no_schedule_exact_alarm_notice"
        if (setter != null) writePreferencesState(preferences, setter)
        return readPreferencesState(preferences, false)
    }

    fun pictureCompressionRate(setter: Int? = null): Int {
        val preferences = "picture_compression_rate"
        if (setter != null) writePreferencesState(preferences, setter)
        return readPreferencesState(preferences, 20)
    }
}
