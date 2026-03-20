package com.sqz.checklist.presentation.task.modify

/**
 * Interface for the action of the task modify.
 *
 * @see TaskModifyLayout
 */
interface TaskModifyState {

    /** Request add a new task **/
    object AddTask : TaskModifyState

    /** Request edit a task with [taskId] **/
    data class EditTask(val taskId: Long) : TaskModifyState
}
