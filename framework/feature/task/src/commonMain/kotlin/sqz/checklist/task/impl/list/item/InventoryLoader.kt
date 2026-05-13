package sqz.checklist.task.impl.list.item

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import sqz.checklist.data.database.model.TaskViewData
import sqz.checklist.data.database.repository.task.TaskRepository
import sqz.checklist.task.api.list.TaskList
import sqz.checklist.task.api.list.model.TaskItemModel

internal class InventoryLoader(
    private val scope: CoroutineScope,
    private val taskRepository: TaskRepository
) {
    private var _loaded: Boolean = false

    private val _listInventory: MutableStateFlow<TaskList.Inventory> = MutableStateFlow(
        TaskList.Inventory.Loading
    )

    fun getInventory(): StateFlow<TaskList.Inventory> {
        return this._listInventory.asStateFlow()
    }

    private fun setDefault(
        listMapper: (Flow<List<TaskViewData>>) -> Flow<List<TaskItemModel>>,
    ): TaskList.Inventory.Default {
        val default = TaskList.Inventory.Default(
            primaryList = listMapper(taskRepository.getTaskList()),
            remindedList = listMapper(taskRepository.getRemindedTaskList()),
            pinnedList = listMapper(taskRepository.getPinnedTaskList()),
        )
        return default
    }

    private fun setSearch(
        searchQuery: String,
        listMapper: (Flow<List<TaskViewData>>) -> Flow<List<TaskItemModel>>,
    ): TaskList.Inventory.Search {
        val search = TaskList.Inventory.Search(
            searchQuery = searchQuery, inSearchList = listMapper(
                taskRepository.getSearchedList(searchQuery)
            )
        )
        return search
    }

    suspend fun updateList(
        afterInitUpdate: () -> Unit = {},
        listMapper: (Flow<List<TaskViewData>>) -> Flow<List<TaskItemModel>>,
    ) {
        // ensure loading correctly
        if (_loaded && _listInventory.value is TaskList.Inventory.Loading) {
            println("Ignored update TaskList.Inventory due to it is still loading")
            return
        }
        // init loading
        if (_listInventory.value is TaskList.Inventory.Loading) {
            val default = this@InventoryLoader.setDefault(listMapper)
            default.primaryList.first().also {
                _listInventory.update { default }
            }
            _loaded = true
            afterInitUpdate()
            return
        }
        // update in search state
        if (_listInventory.value is TaskList.Inventory.Search) {
            val searchQuery = (_listInventory.value as TaskList.Inventory.Search).searchQuery
            _listInventory.update { TaskList.Inventory.Loading }
            val search = this.setSearch(searchQuery, listMapper)
            search.inSearchList.first().also { _listInventory.update { search } }
            return
        }
        // update in default state
        _listInventory.update { TaskList.Inventory.Loading }
        val default = this.setDefault(listMapper)
        default.primaryList.first().also { _listInventory.update { default } }
    }

    fun setSearch(
        query: String?,
        listMapper: (Flow<List<TaskViewData>>) -> Flow<List<TaskItemModel>>,
    ) {
        // set state to search
        if (_listInventory.value !is TaskList.Inventory.Search) {
            if (query != null) scope.launch {
                val search = this@InventoryLoader.setSearch(query, listMapper)
                search.inSearchList.first().also { _listInventory.update { search } }
            }
            return
        }
        // set state to default
        if (query == null) {
            if (_listInventory.value is TaskList.Inventory.Default) {
                println("Ignored update TaskList.Inventory due to not necessary")
                return
            }
            scope.launch {
                val default = this@InventoryLoader.setDefault(listMapper)
                default.primaryList.first().also { _listInventory.update { default } }
            }
            return
        }
        // update search query
        _listInventory.update { this.setSearch(query, listMapper) }
    }
}
