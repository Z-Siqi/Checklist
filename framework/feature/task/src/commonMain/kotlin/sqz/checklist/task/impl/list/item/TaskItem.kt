package sqz.checklist.task.impl.list.item

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import sqz.checklist.data.database.model.TaskViewData
import sqz.checklist.data.database.repository.task.TaskRepository
import sqz.checklist.task.api.list.TaskList
import sqz.checklist.task.api.list.model.TaskItemModel

internal class TaskItem(
    private val taskRepository: TaskRepository,
    private val scope: CoroutineScope,

    private val _externalRequest: MutableStateFlow<TaskItemModel.ExternalRequest>,
    private val _undoProcesser: UndoProcesser,

    override val taskViewData: TaskViewData,
) : TaskItemModel {

    override fun onInfoRequest(type: TaskList.ListType) {
        if (_externalRequest.value != TaskItemModel.ExternalRequest.None) {
            throw IllegalStateException("External request already exist!")
        }
        _externalRequest.update {
            TaskItemModel.ExternalRequest.Info(
                taskId = taskViewData.task.id,
                pinChangeAllowed = type == TaskList.ListType.Reminded,
                withDetail = false //taskViewData.isDetailExist
            )
        }
    }

    override fun onEditRequest() {
        if (_externalRequest.value != TaskItemModel.ExternalRequest.None) {
            throw IllegalStateException("External request already exist!")
        }
        _externalRequest.update {
            TaskItemModel.ExternalRequest.Edit(taskViewData.task.id)
        }
    }

    override fun onPinRequest() {
        scope.launch {
            taskRepository.onTaskPinChange(
                taskId = taskViewData.task.id,
                update = !taskViewData.task.isPin,
            )
        }
    }

    override fun onRemindedRemoveRequest() {
        if (_externalRequest.value != TaskItemModel.ExternalRequest.None) {
            throw IllegalStateException("External request already exist!")
        }
        _externalRequest.update {
            TaskItemModel.ExternalRequest.RemoveReminded(taskViewData.task.id)
        }
    }

    override fun onDetailRequest() {
        if (_externalRequest.value != TaskItemModel.ExternalRequest.None) {
            throw IllegalStateException("External request already exist!")
        }
        if (!taskViewData.isDetailExist) {
            throw IllegalArgumentException("Detail not exist!")
        }
        _externalRequest.update {
            TaskItemModel.ExternalRequest.Detail(taskViewData.task.id)
        }
    }

    override fun onReminderRequest() {
        if (_externalRequest.value != TaskItemModel.ExternalRequest.None) {
            throw IllegalStateException("External request already exist!")
        }
        _externalRequest.update {
            TaskItemModel.ExternalRequest.Reminder(taskViewData.task.id)
        }
    }

    override fun onRemoveAction() {
        scope.launch {
            val taskId = taskViewData.task.id
            taskRepository.removeTaskFromDefaultList(taskId)
            _undoProcesser.requestUndoState(taskId)
        }
    }
}
