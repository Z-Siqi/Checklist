package com.sqz.checklist.preferences

import android.content.Context
import androidx.annotation.IntRange

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

    fun recentlyRemindedKeepTime(setter: Long? = null): Long {
        val preferences = "recently_reminded_keep_time"
        if (setter != null) writePreferencesState(preferences, setter)
        return readPreferencesState(preferences, 43200000L)
    }

    fun pictureCompressionRate(@IntRange(0, 100) setter: Int? = null): Int {
        val preferences = "picture_compression_rate"
        if (setter != null) writePreferencesState(preferences, setter)
        return readPreferencesState(preferences, 20)
    }

    fun videoCompressionRate(@IntRange(0, 100) setter: Int? = null): Int {
        val preferences = "video_compression_rate"
        if (setter != null) writePreferencesState(preferences, setter)
        return readPreferencesState(preferences, 0)
    }

    fun removeNoticeInAutoDelReminded(setter: Boolean? = null): Boolean {
        val preferences = "remove_notice_in_auto_del_reminded"
        if (setter != null) writePreferencesState(preferences, setter)
        return readPreferencesState(preferences, false)
    }
}
