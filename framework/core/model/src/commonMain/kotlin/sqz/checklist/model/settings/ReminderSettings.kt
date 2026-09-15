package sqz.checklist.model.settings

import kotlinx.serialization.Serializable

/** Settings that control reminder warnings, retention, and notification cleanup. */
@Serializable
data class ReminderSettings(
    val removeNotificationWhenReminded: Boolean = true,
    val showExactAlarmWarning: Boolean = true,
    val recentlyRemindedKeepTimeMillis: Long = 43_200_000L,
    val removeNotificationWhenHistoryDeleted: Boolean = false,
)
