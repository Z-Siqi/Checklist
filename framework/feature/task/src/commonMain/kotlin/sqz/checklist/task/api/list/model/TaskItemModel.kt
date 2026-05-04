package sqz.checklist.task.api.list.model

import sqz.checklist.data.database.model.TaskViewData
import sqz.checklist.task.api.list.TaskList

interface TaskItemModel {

    /**
     * Set [ExternalRequest], then external method can handle.
     *
     * @param type the type of the task list. Expected the type from [TaskList.Inventory].
     * @throws IllegalStateException if [TaskList.getExternalRequest] is not `null`.
     */
    fun onInfoRequest(type: TaskList.ListType)

    /**
     * Set [ExternalRequest] to [ExternalRequest.Edit], then external method can handle the edit
     *  though the value from [TaskList.getExternalRequest].
     *
     * @throws IllegalStateException if [TaskList.getExternalRequest] is not `null`.
     */
    fun onEditRequest()

    /**
     * Set the [sqz.checklist.data.database.Task.isPin] to opposite value.
     */
    fun onPinRequest()

    /**
     * Set [ExternalRequest] to [ExternalRequest.RemoveReminded], then external method can handle
     *  the remove reminded though the value from [TaskList.Inventory.Default.remindedList].
     *
     * @throws IllegalStateException if [TaskList.getExternalRequest] is not `null`.
     */
    fun onRemindedRemoveRequest()

    /**
     * Set [ExternalRequest] to [ExternalRequest.Detail], then external method can show the detail
     *  of the task information.
     *
     * @throws IllegalStateException if [TaskList.getExternalRequest] is not `null`.
     */
    fun onDetailRequest()

    /**
     * Set [ExternalRequest] to [ExternalRequest.Reminder] then external method can handle the
     *  reminder though the value from [TaskList.getExternalRequest].
     *
     * @throws IllegalStateException if [TaskList.getExternalRequest] is not `null`.
     */
    fun onReminderRequest()

    /**
     * Remove the task from the list by calling
     * [sqz.checklist.data.database.repository.task.TaskRepository.removeTaskFromDefaultList].
     * Then set [TaskList.getUndoState] to `true`, then the undo state can be process.
     */
    fun onRemoveAction()

    /**
     * The task view data for the UI to handle.
     *
     * @see TaskViewData
     */
    val taskViewData: TaskViewData

    /**
     * The external request for the external method to handle.
     *
     * @param None the request is not set.
     * @param Info the request to get the task information.
     * @param Edit the request to edit the task.
     * @param Reminder the request to set the reminder for the task.
     */
    sealed interface ExternalRequest {

        data object None: ExternalRequest

        data class Info(
            val taskId: Long,
            val pinChangeAllowed: Boolean,
            val withDetail: Boolean,
        ): ExternalRequest

        data class Detail(val taskId: Long): ExternalRequest

        data class Edit(val taskId: Long): ExternalRequest

        data class Reminder(val taskId: Long): ExternalRequest

        data class RemoveReminded(val taskId: Long): ExternalRequest
    }
}
