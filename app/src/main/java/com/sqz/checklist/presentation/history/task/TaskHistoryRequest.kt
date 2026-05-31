package com.sqz.checklist.presentation.history.task

sealed interface TaskHistoryRequest {

    /**
     * Indicates that the requested state change has been processed by the feature.
     */
    data object StateProcessed : TaskHistoryRequest

    /**
     * Request to control external action buttons state.
     *
     * @param toAll control the state is to delete or redo all history task.
     *   If `null` means do NOT allow the button sent action state for delete or redo;
     *   otherwise `true` for the request can delete or redo all history task or `false` means
     *   the request is for a single history task.
     */
    data class AllowDelOrRedo(val toAll: Boolean?) : TaskHistoryRequest

    /**
     * This request is for show the task info by user request.
     */
    data class TaskInfoRequest(val taskId: Long) : TaskHistoryRequest
}
