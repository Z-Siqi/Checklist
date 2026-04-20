package sqz.checklist.task.api.list

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import sqz.checklist.task.api.list.model.TaskItemModel

interface TaskList {

    /**
     * Get the current [Inventory] state for the task list or a loading state.
     *
     * @return a [StateFlow] emitting the [Inventory] of the state.
     */
    val getTaskListInventory: StateFlow<Inventory>

    /**
     * Get the current undo state.
     *
     * Expected to show an undo UI if [getUndoState] is true.
     *
     * @return a [StateFlow] emitting the current undo state.
     */
    val getUndoState: StateFlow<Boolean>

    /**
     * Get the current external request.
     *
     * Note: Please call [resetExternalRequest] after use for reset state.
     *
     * @return a [StateFlow] emitting the current external request.
     */
    val getExternalRequest: StateFlow<TaskItemModel.ExternalRequest>

    /**
     * Reset the external request to [TaskItemModel.ExternalRequest.None].
     *
     * Note: Please call this method after use the [getExternalRequest] value.
     */
    fun resetExternalRequest()

    /**
     * Update the task list.
     *
     * This function will update the list with the new data from the database.
     * **DO NOT** use this function for keep the data up-to-date, the list expected to update
     *   the data automatically when the database is updated.
     *
     * - First time call the method will update [Inventory.Loading] to [Inventory.Default].
     * - If the method is called before, the method will update [Inventory.Default]
     *   to [Inventory.Loading], then update [Inventory.Loading] to [Inventory.Default] by getting
     *   new data from the database; In addition, the value will update to [Inventory.Search] if
     *   the value was [Inventory.Search] before called this method.
     */
    suspend fun updateList()

    /**
     * Request to undo the action from the [TaskItemModel.onRemoveAction].
     *
     * The method will move back the task from the database history list.
     *
     * @param onUndoTaskId a callback function to handle more behavior after undo.
     * @throws NullPointerException if [getUndoState] is `false`
     */
    fun requestUndo(onUndoTaskId: (Long) -> Unit)

    /**
     * Set the value to match old value for undo.
     *
     * - **Once [getUndoState] is `true`:**
     * - First call will initial set an [Any] value as a condition for stop.
     * - Second called or after, the method will match the value that set in first called; The
     *   undo state will be close and set [getUndoState] to `false` if the new value not match
     *   to the old value which set by the first call.
     * - The undo state will automatically stop and set [getUndoState] back to `false` after
     *   the [TaskItemModel.onRemoveAction] is called a few seconds.
     *
     * @param any the value to match.
     */
    fun setUndoBreakFactor(any: Any)

    /**
     * To search the task list.
     *
     * This method will update the list with the new data from the database.
     *
     * @param query the query to search:
     * - If null, the list will be reset to [Inventory.Default].
     * - If not null, the list will be set to [Inventory.Search] with the new query.
     */
    fun onSearchRequest(query: String?)

    /**
     * Check if the task list is empty.
     *
     * @return `true` if the task list is empty.
     * @throws NullPointerException if [Inventory] is [Inventory.Loading]
     */
    fun isInventoryEmpty(): Flow<Boolean>

    /**
     * The inventory of the task list.
     *
     * @param Loading the loading state, the state is the initial state.
     * @param Default the default state, it includes the primary list, the reminded list and the
     *   pinned list.
     * @param Search the search state, it includes the search query and the searched list.
     */
    sealed interface Inventory {

        data object Loading: Inventory

        data class Default(
            val primaryList: Flow<List<TaskItemModel>>,
            val remindedList: Flow<List<TaskItemModel>>,
            val pinnedList: Flow<List<TaskItemModel>>,
        ): Inventory

        data class Search(
            val searchQuery: String,
            val inSearchList: Flow<List<TaskItemModel>>,
        ): Inventory
    }

    /**
     * The type of the task list.
     */
    enum class ListType {
        Primary, Reminded, Pinned, Search
    }

    /**
     * The config for the task list.
     *
     * @param enableUndo Enable the undo feature. The [getUndoState] will be always `false`
     *   if the [enableUndo] is `false`.
     * @param autoDelIsHistoryTaskNumber Number of maximum allowed history tasks. If null will
     *   never delete history task. Otherwise, the oldest isHistory task will delete from database
     *   when the history list size is greater than the [autoDelIsHistoryTaskNumber].
     */
    data class Config(
        val enableUndo: Boolean = false,
        val autoDelIsHistoryTaskNumber: Int? = null
    )
}
