package sqz.checklist.task.api

import kotlinx.coroutines.flow.StateFlow
import sqz.checklist.data.database.TaskDetailType
import sqz.checklist.task.impl.modify.detail.DetailHelper
import sqz.checklist.task.impl.modify.detail.getFileInfo

interface TaskModify {

    /**
     * Use it to create a new task.
     *
     * This function will update [State] to `Modify`; App can be show modify UI once
     * the `ModifyState.state` is not `None`.
     *
     * @throws IllegalStateException if task modify already set
     */
    @Throws(IllegalStateException::class)
    fun makeNewTask()

    /**
     * Use it to edit a task.
     *
     * This function will update [State] to `Loading` or `Modify`; App can be show modify UI once
     * the `ModifyState.state` is not `None`.
     *
     * @throws IllegalStateException if task modify already set
     * @throws NullPointerException if no task match the id
     */
    @Throws(IllegalStateException::class, NullPointerException::class)
    fun selectTask(id: Long)

    /**
     * Get the current state of the task modify.
     *
     * @return a [StateFlow] emitting the current state of the task modify.
     */
    val getModifyState: StateFlow<ModifyState>

    /**
     * Update loading state for process the thing that may take a while.
     *
     * This function will update [State] to `Loading`; App can be show loading UI once
     * the `ModifyState.state` is not `None`.
     *
     * @param isLoading a boolean indicating whether to set the loading state.
     * @throws IllegalStateException if the state is None.
     */
    fun updateLoading(isLoading: Boolean)

    /**
     * Confirm the modify task.
     *
     * This function will update [State] to `None`; App can be show modify UI once
     * the `ModifyState.state` is not `None`.
     *
     * @return the task id from [sqz.checklist.data.database.Task.id]
     * @throws IllegalStateException if task modify not set
     * @throws IllegalArgumentException if [Task.UIState.description] is empty
     */
    @Throws(IllegalStateException::class, IllegalArgumentException::class)
    suspend fun confirmModify(): Long

    /**
     * Cancel the modify task.
     */
    fun cancelModify()

    /**
     * Get the task handler.
     *
     * Note: Use this only when [State] is `Modify` for safely call this.
     *
     * @return the task handler.
     * @throws NullPointerException if not call [makeNewTask] or [selectTask]
     *   before call this.
     */
    fun taskHandler(): Task

    /**
     * Get the detail handler.
     *
     * Note: Use this only when [State] is `Modify` for safely call this.
     *
     * @return the detail handler.
     * @throws NullPointerException if not call [makeNewTask] or [selectTask]
     *   before call this.
     */
    fun detailHandler(): Detail

    /** The task methods **/
    interface Task {
        fun updateDescription(description: String)

        fun onTypeValueChange(type: (ModifyType.NewTask) -> ModifyType.NewTask)

        interface ModifyType {
            data class NewTask(
                val isPin: Boolean = false,
                val withReminder: Boolean = false,
            ) : ModifyType {
                fun onPinClick(): NewTask {
                    return copy(isPin = !isPin)
                }

                fun onReminderClick(): NewTask {
                    return copy(withReminder = !withReminder)
                }
            }

            data class EditTask(
                val taskId: Long
            ) : ModifyType
        }

        data class UIState(
            val description: String,
            val type: ModifyType,
        )
    }

    /** The detail methods **/
    interface Detail {

        /**
         * Add a new detail item.
         *
         * @throws IllegalArgumentException if selected item is not null.
         */
        @Throws(IllegalArgumentException::class)
        fun addItem()

        /**
         * Update the description of a detail item.
         *
         * @param index the index of the list to update.
         * @param text the new description to update.
         */
        fun updateItemDescription(index: Int, text: String?)

        /**
         * Remove a detail item.
         *
         * @param index the index of the list to remove.
         *   If is `null` will remove current item.
         * @throws NullPointerException if [index] is `null` but no item is selected.
         */
        @Throws(NullPointerException::class)
        fun removeItem(index: Int?)

        /**
         * Move a detail item to another position.
         *
         * @param from the index of the list to move.
         * @param to the index to move to.
         * @throws IllegalStateException if no item is selected.
         */
        @Throws(IllegalStateException::class)
        fun moveItem(from: Int, to: Int)

        /**
         * Get the selected detail item.
         *
         * @return the selected detail item, or null if no item is selected.
         */
        val getSelectedItem: StateFlow<UIState?>

        /**
         * Select a detail item to edit. This method will cache selected [TypeState] for rollback.
         *
         * @param index the index of the list to select.
         * @throws IndexOutOfBoundsException if the index is out of bounds.
         * @throws IllegalStateException if the check failed.
         * @throws IllegalArgumentException if already have selected item.
         */
        @Throws(
            IndexOutOfBoundsException::class,
            IllegalStateException::class,
            IllegalArgumentException::class
        )
        fun selectItem(index: Int)

        /**
         * Unselect the current selected item for Confirm edit detail or Rollback current edit.
         *
         * **When unselected:** This function will format some type of [TypeState]
         * such as add `https://` if user forgot in url type, but this will NOT process
         * any file such as compress or check file size;
         * To process files, use [updateSelectedData] first before call this to confirm.
         *
         * **When rollback:** Use cached [TypeState] which set during [selectItem], then replace
         * current [TypeState] to the cached [TypeState] for cancel change to list.
         *
         * @param rollback if true, rollback the current edit, otherwise unselect the current item.
         * @throws IndexOutOfBoundsException if no item is selected.
         * @throws IllegalStateException if the check failed.
         * @throws IllegalArgumentException if cached [TypeState] is invalid.
         */
        @Throws(
            IndexOutOfBoundsException::class,
            IllegalStateException::class,
            IllegalArgumentException::class
        )
        fun unselectItem(rollback: Boolean = false)

        /**
         * Update the type of the selected detail item.
         *
         * This function expected to remove old [TypeState] data and add a new [TypeState] that
         * match the [TaskDetailType].
         *
         * @param type the new type to update.
         * @throws IllegalStateException if no item is selected.
         */
        @Throws(IllegalStateException::class)
        fun updateSelectedType(type: TaskDetailType)

        /**
         * Update the data of the selected detail item.
         *
         * Note:
         * call [updateSelectedType] is the suggest way to update the type of the selected item.
         *
         * @param state the new data to update.
         * @throws IllegalStateException if no item is selected.
         * @throws IllegalArgumentException if the data contain path but not a temp file path.
         */
        @Throws(IllegalStateException::class, IllegalArgumentException::class)
        fun updateSelectedData(state: TypeState)

        /**
         * Determines if the selected detail is not same with cache.
         *
         * @return a [StateFlow] emitting `true` if the selected detail is not same
         *   with cache, `false` otherwise.
         */
        val isDetailModified: StateFlow<Boolean>

        /**
         * The data of a detail item.
         *
         * Expected to implement by each [TaskDetailType]
         */
        sealed interface TypeState {

            data class Text(val description: String = "") : TypeState

            data class URL(val url: String = "") : TypeState

            @Suppress("ArrayInDataClass")
            data class Application(val launchToken: ByteArray? = null) : TypeState

            data class Picture(
                val fileName: String = "", val path: String = ""
            ) : TypeState

            data class Video(
                val fileName: String = "", val path: String = ""
            ) : TypeState

            data class Audio(
                val fileName: String = "", val path: String = ""
            ) : TypeState
        }

        data class UIState(
            val itemDescription: String? = null,
            val typeState: TypeState?,
        ) {
            fun isValidTypeState(): Boolean {
                if (this.typeState == null) return false
                return DetailHelper.checkTypeStateValid(this.typeState)
            }

            /**
             * Get the file path of the [Detail.TypeState].
             *
             * Note: this function will not check valid of the path.
             *
             * @return `Pair<filePath, fileName>` or null if the [Detail.TypeState] does not contain a path.
             */
            fun getFileInfo(): Pair<String, String>? {
                if (this.typeState == null) return null
                return this.typeState.getFileInfo()
            }
        }
    }

    /** Task modify state **/
    enum class State {
        None, Loading, Modify
    }

    /** Task modify value state **/
    data class ModifyState(
        val state: State = State.None,
        val taskState: Task.UIState? = null,
        val detailState: List<Detail.UIState>? = null
    ) {
        init {
            require(state != State.None || (taskState == null && detailState == null)) {
                "taskState and detailState must be null when state is State.None"
            }

            require(state != State.Modify || (taskState != null && detailState != null)) {
                "taskState and detailState must not be null but can be empty when state is State.Modify"
            }
        }

        /**
         * Determines if a new item can be added to the task detail list based on a weighted count.
         * Different item types have different "weights" (e.g., Video=5, Picture=2, Audio=3, others=1).
         *
         * @return a [StateFlow] emitting `true` if the total weight is less than 10, `false` otherwise.
         */
        fun allowAddToList(): Boolean {
            if (this.detailState.isNullOrEmpty()) return true
            return !DetailHelper.listLimit(this.detailState)
        }
    }
}
