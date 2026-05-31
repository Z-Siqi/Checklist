package sqz.checklist.history.api.task

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import sqz.checklist.data.database.Task

interface TaskHistory {

    /**
     * Get the current state of the task history inventory.
     *
     * @return A [StateFlow] emitting the current [Inventory] state.
     */
    val getHistoryInventory: StateFlow<Inventory>

    /**
     * Check if the task history list is empty.
     *
     * @return `true` if the task list is empty.
     * @throws NullPointerException if [Inventory] is [Inventory.Loading]
     */
    fun isInventoryEmpty(): Flow<Boolean>

    fun selectTask(taskId: Long)

    fun deselectTask()

    /**
     * Delete the currently selected task from storage.
     *
     * @throws IllegalStateException when state is [Inventory.Loading].
     * @throws NullPointerException if no task is selected or unknown [Inventory] state
     */
    fun deleteSelectedTask()

    /**
     * Redo the currently selected task.
     *
     * @throws IllegalStateException when state is [Inventory.Loading].
     * @throws NullPointerException if no task is selected or unknown [Inventory] state
     */
    fun redoSelectedTask()

    /**
     * Delete all history task from storage forever.
     *
     * Expected only [Inventory.Default] state allow call this method.
     *
     * @throws IllegalStateException when state not [Inventory.Default] or [Inventory.Loading].
     */
    fun deleteAllHistory()

    /**
     * Redo all history tasks from storage.
     *
     * Expected only [Inventory.Default] state allow call this method.
     *
     * @throws IllegalStateException when state not [Inventory.Default].
     */
    fun redoAllHistory()

    sealed interface Inventory {

        data object Loading : Inventory

        data class Default(
            val historyList: Flow<List<Task>>,
            val selectedTaskId: Long? = null,
        ) : Inventory
    }

    data class Config(
        val autoDelIsHistoryTaskNumber: Int? = null,
    )
}
