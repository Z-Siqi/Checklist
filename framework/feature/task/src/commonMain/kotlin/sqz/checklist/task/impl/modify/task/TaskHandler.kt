package sqz.checklist.task.impl.modify.task

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import sqz.checklist.data.database.Task
import sqz.checklist.task.api.modify.TaskModify
import kotlin.time.Clock

class TaskHandler(
    private val taskIn: Task?,
    private val modifyState: MutableStateFlow<TaskModify.ModifyState>,
) : TaskModify.Task {

    fun isModified(state: TaskModify.ModifyState): Boolean {
        val taskState = state.taskState ?: return false
        if (taskIn == null) {
            return taskState.description.isNotEmpty() ||
                    (taskState.type as? TaskModify.Task.ModifyType.NewTask)?.let {
                        it.isPin || it.withReminder
                    } == true
        } else {
            return taskState.description != taskIn.description
        }
    }

    override fun updateDescription(description: String) {
        modifyState.update {
            it.copy(taskState = it.taskState!!.copy(description = description))
        }
    }

    override fun onTypeValueChange(
        type: (TaskModify.Task.ModifyType.NewTask) -> TaskModify.Task.ModifyType.NewTask
    ) {
        val newType = type(modifyState.value.taskState!!.type as TaskModify.Task.ModifyType.NewTask)
        modifyState.update {
            it.copy(taskState = it.taskState!!.copy(type = newType))
        }
    }

    //-----------------------
    // Process final action
    //-----------------------
    fun checkTask(): String? {
        val taskState = modifyState.value.taskState!!
        if (taskState.description.isBlank()) {
            return "Task description cannot be empty"
        }
        return null
    }

    fun onConfirmed(): Task {
        val taskState = modifyState.value.taskState!!
        if (taskIn == null) { // new task
            return Task(
                description = taskState.description,
                createDate = Clock.System.todayIn(TimeZone.currentSystemDefault()),
                isPin = (taskState.type as TaskModify.Task.ModifyType.NewTask).isPin
            )
        }
        return taskIn.copy( // edit task
            description = taskState.description
        )
    }

    private fun onCleared() {
        if (modifyState.value.state != TaskModify.State.Loading) modifyState.update {
            it.copy(state = TaskModify.State.Loading)
        }
        modifyState.update { it.copy(taskState = null) }
    }

    fun onCanceled() {
        this.onCleared()
        println("Modify is canceled")
    }

    fun reset() {
        this.onCleared()
    }
}
