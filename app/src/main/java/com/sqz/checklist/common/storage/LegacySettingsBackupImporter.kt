package com.sqz.checklist.common.storage

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import sqz.checklist.model.settings.AppSettings
import sqz.checklist.model.settings.AppTheme
import java.io.ByteArrayInputStream

@Deprecated(
    message = "Only use this importer for backups created before settings.json was introduced.",
)
internal object LegacySettingsBackupImporter {
    /** Converts a legacy SharedPreferences XML payload into the current settings model. */
    fun decode(payload: ByteArray): AppSettings {
        val values = readValues(payload)
        val defaults = AppSettings()
        return defaults.copy(
            appearance = defaults.appearance.copy(
                theme = if (values.int("app_theme", 0) == 1) AppTheme.Colorful else AppTheme.Pastel,
            ),
            history = defaults.history.copy(
                allowedNumberOfHistory = values.int("allowed_number_of_history", 5)
                    .takeIf { it >= 0 } ?: defaults.history.allowedNumberOfHistory,
                clearHistoryWhenLeaving = values.boolean("clear_history_when_leaved", false),
                undoEnabled = !values.boolean("disable_undo_button", false),
            ),
            media = defaults.media.copy(
                pictureCompressionPercent = values.int("picture_compression_rate", 20)
                    .takeIf { it in 0..100 } ?: defaults.media.pictureCompressionPercent,
                videoCompressionPercent = values.int("video_compression_rate", 0)
                    .takeIf { it in 0..100 } ?: defaults.media.videoCompressionPercent,
            ),
            reminder = defaults.reminder.copy(
                removeNotificationWhenReminded =
                    !values.boolean("disable_remove_notify_in_reminded", false),
                showExactAlarmWarning =
                    !values.boolean("disable_no_schedule_exact_alarm_notice", false),
                recentlyRemindedKeepTimeMillis = values.long(
                    "recently_reminded_keep_time",
                    defaults.reminder.recentlyRemindedKeepTimeMillis,
                ).takeIf { it >= 0L } ?: defaults.reminder.recentlyRemindedKeepTimeMillis,
                removeNotificationWhenHistoryDeleted =
                    values.boolean("remove_notice_in_auto_del_reminded", false),
            ),
        )
    }

    /** Reads scalar name/value attributes from a legacy SharedPreferences XML document. */
    private fun readValues(payload: ByteArray): Map<String, String> = buildMap {
        val parser = Xml.newPullParser()
        parser.setInput(ByteArrayInputStream(payload), Charsets.UTF_8.name())
        while (parser.eventType != XmlPullParser.END_DOCUMENT) {
            if (parser.eventType == XmlPullParser.START_TAG) {
                val name = parser.getAttributeValue(null, "name")
                val value = parser.getAttributeValue(null, "value")
                if (name != null && value != null) put(name, value)
            }
            parser.next()
        }
    }

    /** Reads a strict Boolean or returns [default] for missing and malformed values. */
    private fun Map<String, String>.boolean(key: String, default: Boolean) =
        this[key]?.toBooleanStrictOrNull() ?: default

    /** Reads an Int or returns [default] for missing and malformed values. */
    private fun Map<String, String>.int(key: String, default: Int) =
        this[key]?.toIntOrNull() ?: default

    /** Reads a Long or returns [default] for missing and malformed values. */
    private fun Map<String, String>.long(key: String, default: Long) =
        this[key]?.toLongOrNull() ?: default
}
