package com.sqz.checklist.presentation.task.list

/**
 * Interface for the action of the task modify.
 *
 * @see sqz.checklist.task.api.list.model.TaskItemModel.ExternalRequest
 */
sealed interface TaskListState {

    data object SearchProcessed: TaskListState //TODO

    data class Info(
        val taskId: Long,
        val pinChangeAllowed: Boolean,
    ): TaskListState

    data class Detail(
        val taskId: Long
    ): TaskListState

    data class Edit(
        val taskId: Long
    ): TaskListState

    data class Reminder(
        val taskId: Long
    ): TaskListState

    data class RemoveReminded(
        val taskId: Long
    ): TaskListState
}
