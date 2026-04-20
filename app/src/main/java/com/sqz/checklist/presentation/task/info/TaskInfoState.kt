package com.sqz.checklist.presentation.task.info

/**
 * data class for the action of the task info.
 *
 * @see TaskInfoLayout
 */
data class TaskInfoState(
    val taskId: Long,
    val config: Config
) {
    sealed interface Config {

        data class TaskOnly(val pinChangeAllowed: Boolean = false) : Config

        data object DetailOnly : Config

        data class TaskAndDetail(val pinChangeAllowed: Boolean = false) : Config
    }
}
