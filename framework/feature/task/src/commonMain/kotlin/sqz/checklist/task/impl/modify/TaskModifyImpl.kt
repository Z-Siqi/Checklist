package sqz.checklist.task.impl.modify

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import sqz.checklist.data.database.Task
import sqz.checklist.data.database.TaskDetail
import sqz.checklist.data.database.TaskDetailType
import sqz.checklist.data.database.repository.task.TaskRepository
import sqz.checklist.data.storage.manager.StorageManager
import sqz.checklist.task.api.modify.TaskModify
import sqz.checklist.task.impl.modify.detail.DetailHandler
import sqz.checklist.task.impl.modify.task.TaskHandler

internal class TaskModifyImpl(
    private val taskRepository: TaskRepository,
    private val storageManager: StorageManager,
) : TaskModify {
    private val scope = CoroutineScope(SupervisorJob())

    // The current state of the task modify
    private val _modifyState = MutableStateFlow(TaskModify.ModifyState())

    // When edit a task not null, or it will be null when make a new task
    private var _fullTaskIn: Pair<Task, List<TaskDetail>?>? = null

    // Task handler
    private lateinit var _taskHandler: TaskHandler

    // Task Detail handler
    private lateinit var _detailHandler: DetailHandler

    // Initialize handlers
    private fun initHandlers() {
        _taskHandler = TaskHandler(
            taskIn = _fullTaskIn?.first,
            modifyState = _modifyState,
        )
        _detailHandler = DetailHandler(
            detailIn = _fullTaskIn?.second,
            modifyState = _modifyState,
            scope = scope,
            storageManager = storageManager,
        )
    }

    // Implement the functionality of the interface
    @Throws(IllegalStateException::class)
    override fun makeNewTask() {
        if (_modifyState.value.state != TaskModify.State.None) {
            throw IllegalStateException("TaskModify already set")
        }
        this.initHandlers()
        val modifyState = TaskModify.Task.UIState(
            description = "",
            type = TaskModify.Task.ModifyType.NewTask(),
        )
        _modifyState.update {
            TaskModify.ModifyState(
                state = TaskModify.State.Modify,
                taskState = modifyState,
                detailState = listOf()
            )
        }
    }

    // Set task modify data to enable edit
    private suspend fun setTaskModifyData(id: Long) {
        val fullTask = taskRepository.getFullTask(id)
        _fullTaskIn = Pair(fullTask.first, fullTask.second)
        val task = fullTask.first
        val detail = fullTask.second
        val taskState = TaskModify.Task.UIState(
            description = task.description,
            type = TaskModify.Task.ModifyType.EditTask(task.id),
        )
        val detailState: List<TaskModify.Detail.UIState> = detail?.let { taskDetails ->
            val list = mutableListOf<TaskModify.Detail.UIState>()
            for (data in taskDetails) {
                val typeState = when (data.type) {
                    TaskDetailType.Text -> TaskModify.Detail.TypeState.Text(
                        description = data.dataByte.decodeToString()
                    )

                    TaskDetailType.URL -> TaskModify.Detail.TypeState.URL(
                        url = data.dataByte.decodeToString()
                    )

                    TaskDetailType.Application -> TaskModify.Detail.TypeState.Application(
                        launchToken = data.dataByte
                    )

                    TaskDetailType.Picture -> TaskModify.Detail.TypeState.Picture(
                        fileName = data.dataString!!,
                        path = data.dataByte.decodeToString()
                    )

                    TaskDetailType.Video -> TaskModify.Detail.TypeState.Video(
                        fileName = data.dataString!!,
                        path = data.dataByte.decodeToString()
                    )

                    TaskDetailType.Audio -> TaskModify.Detail.TypeState.Audio(
                        fileName = data.dataString!!,
                        path = data.dataByte.decodeToString()
                    )
                }
                val uiState = TaskModify.Detail.UIState(
                    itemDescription = data.description, typeState = typeState
                )
                list.add(uiState)
            }
            list.toList()
        } ?: listOf()
        _modifyState.update {
            it.copy(taskState = taskState, detailState = detailState)
        }
    }

    // Implement the functionality of the interface
    @Throws(IllegalStateException::class, NullPointerException::class)
    override fun selectTask(id: Long) {
        if (_modifyState.value.state != TaskModify.State.None) {
            throw IllegalStateException("TaskModify already set")
        }
        _modifyState.update { it.copy(state = TaskModify.State.Loading) }
        scope.launch {
            this@TaskModifyImpl.setTaskModifyData(id)
            this@TaskModifyImpl.initHandlers()
            _modifyState.update { it.copy(state = TaskModify.State.Modify) }
        }
    }

    // Implement the functionality of the interface
    override fun cancelModify() {
        if (_modifyState.value == TaskModify.ModifyState()) {
            throw IllegalStateException("TaskModify not set")
        }
        _taskHandler.onCanceled()
        _detailHandler.onCanceled {
            _modifyState.update { it.copy(state = TaskModify.State.None) }
        }
        _fullTaskIn = null
    }

    // Implement the functionality of the interface
    @Throws(IllegalStateException::class, IllegalArgumentException::class)
    override suspend fun confirmModify(): Long {
        _modifyState.value.let { value ->
            if (value == TaskModify.ModifyState()) {
                throw IllegalStateException("TaskModify not set")
            }
            if (value.state == TaskModify.State.Loading) {
                throw IllegalStateException("TaskModify is loading")
            }
            _taskHandler.checkTask()?.let { err ->
                throw IllegalArgumentException(err)
            }
        }
        _modifyState.update { it.copy(state = TaskModify.State.Loading) }
        var taskId: Long? = null
        taskRepository.modifyTask(
            task = _taskHandler.onConfirmed(),
            detail = _detailHandler.onConfirmed()
        ).let { id ->
            taskId = id
            _taskHandler.reset()
            _detailHandler.reset()
            _modifyState.update { it.copy(state = TaskModify.State.None) }
        }
        _fullTaskIn = null
        return taskId!!
    }

    // Implement the functionality of the interface
    override val getModifyState: StateFlow<TaskModify.ModifyState> = _modifyState.asStateFlow()

    // Implement the functionality of the interface
    override val isModified: StateFlow<Boolean> = _modifyState.map { state ->
        if (state.state != TaskModify.State.Modify) return@map false
        val taskMod = if (::_taskHandler.isInitialized) _taskHandler.isModified(state) else false
        val detailMod = if (::_detailHandler.isInitialized) _detailHandler.isModified(state) else false
        taskMod || detailMod
    }.stateIn(scope, SharingStarted.Eagerly, false)

    // Implement the functionality of the interface
    override fun updateLoading(isLoading: Boolean) {
        if (_modifyState.value == TaskModify.ModifyState()) {
            throw IllegalStateException("TaskModify not set")
        }
        _modifyState.update {
            it.copy(state = if (isLoading) TaskModify.State.Loading else TaskModify.State.Modify)
        }
    }

    // Implement the functionality of the interface
    override fun taskHandler(): TaskModify.Task {
        return try {
            this._taskHandler
        } catch (e: Exception) {
            throw NullPointerException(e.message)
        }
    }

    // Implement the functionality of the interface
    override fun detailHandler(): TaskModify.Detail {
        return try {
            this._detailHandler
        } catch (e: Exception) {
            throw NullPointerException(e.message)
        }
    }
}
