package sqz.checklist.model.settings

import kotlinx.serialization.Serializable

/** Settings that control task history retention and interaction. */
@Serializable
data class HistorySettings(
    val allowedNumberOfHistory: Int = 5,
    val clearHistoryWhenLeaving: Boolean = false,
    val undoEnabled: Boolean = true,
)
