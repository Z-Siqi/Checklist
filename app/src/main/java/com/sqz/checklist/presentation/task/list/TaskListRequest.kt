package com.sqz.checklist.presentation.task.list

/**
 * Interface for the external action for the task list.
 *
 * @see sqz.checklist.task.api.list.model.TaskItemModel.ExternalRequest
 */
sealed interface TaskListRequest {

    data object SearchProcessed: TaskListRequest //TODO

    data object SearchCanceled: TaskListRequest

    data object RefreshListProcessed: TaskListRequest

    data class Info(
        val taskId: Long,
        val pinChangeAllowed: Boolean,
    ): TaskListRequest

    data class Detail(
        val taskId: Long
    ): TaskListRequest

    data class Edit(
        val taskId: Long
    ): TaskListRequest

    data class Reminder(
        val taskId: Long
    ): TaskListRequest

    data class RemoveReminded(
        val taskId: Long
    ): TaskListRequest
}
