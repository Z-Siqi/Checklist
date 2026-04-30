package com.sqz.checklist.presentation.task.list

/**
 * Interface for the action of the task list.
 */
sealed interface TaskListState {

    /** No action needed **/
    data object None : TaskListState

    /**
     * Expected to refresh the list.
     *
     * This should be a temporal state, reset to [None] after the refresh
     * is finished (sent request via [TaskListRequest] to notify the external).
     */
    data object IsRefreshListRequest : TaskListState

    /**
     * Expected to switch the list to search mode.
     *
     * This should be a temporal state, reset to [None] after the refresh
     * is finished (sent request via [TaskListRequest] to notify the external).
     */
    data class IsSearchRequest(val searchState: Boolean) : TaskListState
}
