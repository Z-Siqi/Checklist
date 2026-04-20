package com.sqz.checklist.presentation.task.modify

/**
 * Interface for the action of the task modify.
 *
 * @see TaskModifyLayout
 */
sealed interface TaskModifyState {

    /** Request add a new task **/
    data object AddTask : TaskModifyState

    /** Request edit a task with [taskId] **/
    data class EditTask(val taskId: Long) : TaskModifyState
}
