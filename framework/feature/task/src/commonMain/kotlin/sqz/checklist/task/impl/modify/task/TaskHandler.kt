package sqz.checklist.task.impl.modify.task

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import sqz.checklist.data.database.Task
import sqz.checklist.task.api.TaskModify
import kotlin.time.Clock

class TaskHandler(
    private val taskIn: Task?,
    private val modifyState: MutableStateFlow<TaskModify.ModifyState>,
) : TaskModify.Task {

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
