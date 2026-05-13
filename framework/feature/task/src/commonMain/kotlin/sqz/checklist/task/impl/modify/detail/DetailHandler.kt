package sqz.checklist.task.impl.modify.detail

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okio.FileNotFoundException
import okio.IOException
import sqz.checklist.data.database.TaskDetail
import sqz.checklist.data.database.TaskDetailType
import sqz.checklist.data.storage.StorageHelper.isMediaPath
import sqz.checklist.data.storage.StorageHelper.isTempPath
import sqz.checklist.data.storage.manager.StorageManager
import sqz.checklist.task.api.modify.TaskModify

internal class DetailHandler(
    private val detailIn: List<TaskDetail>?,
    private val modifyState: MutableStateFlow<TaskModify.ModifyState>,
    private val scope: CoroutineScope,
    private val storageManager: StorageManager,
) : TaskModify.Detail {
    // Selected item index
    private val _selectedItemIndex: MutableStateFlow<Int?> = MutableStateFlow(null)

    fun isModified(state: TaskModify.ModifyState): Boolean {
        val currentDetails = state.detailState
        if (detailIn == null) {
            if (!currentDetails.isNullOrEmpty() && currentDetails.last().typeState == null) {
                return false
            }
            return !currentDetails.isNullOrEmpty()
        } else {
            if (currentDetails == null) {
                return detailIn.isNotEmpty()
            } else if (currentDetails.size != detailIn.size) {
                return try {
                    currentDetails[_selectedItemIndex.value!!].typeState != null
                } catch (e: Exception) {
                    if (e !is NullPointerException && e !is IndexOutOfBoundsException) {
                        throw e
                    }
                    true
                }
            } else if (currentDetails.isEmpty() && detailIn.isEmpty()) {
                return false
            } else {
                // Sizes are equal and > 0, let's convert and check.
                // Wait, some currentDetails might be invalid, but we only compare if they differ
                // actually we can just compare the mapped values
                try {
                    val finalDetail = currentDetails.map {
                        DetailHelper.convertToDatabaseDetail(it).copy(taskId = detailIn.first().taskId)
                    }.mapIndexed { index, it -> it.copy(id = detailIn[index].id) }
                    
                    return finalDetail != detailIn
                } catch (_: Exception) {
                    // If convertToDatabaseDetail throws or fails, it implies modified or invalid
                    return true
                }
            }
        }
    }

    // Implement the functionality of the interface
    override val getSelectedItem: StateFlow<TaskModify.Detail.UIState?> = combine(
        modifyState, _selectedItemIndex
    ) { state, idx ->
        val detail = state.detailState ?: return@combine null
        if (detail.isEmpty()) return@combine null
        val getIdx = idx ?: return@combine null
        try {
            return@combine detail[getIdx]
        } catch (e: IndexOutOfBoundsException) {
            e.printStackTrace()
            return@combine null
        }
    }.stateIn(
        scope = scope,
        started = SharingStarted.Eagerly,
        initialValue = null
    )


    // Cache the original state before editing for rollback purposes
    private var _cacheSelectedItem = MutableStateFlow<TaskModify.Detail.TypeState?>(null)

    // Temp file path
    private val _temp: MutableMap<TaskModify.Detail.TypeState, String> = mutableMapOf()

    // Wait delete file path (execute delete only when final confirmed)
    private val _waitDelete: MutableList<String> = mutableListOf()

    // Process selected data
    private fun onSelectedDataChanged(
        oldState: TaskModify.Detail.TypeState?,
        newState: TaskModify.Detail.TypeState? = null
    ) {
        newState?.let { // ensure _waitDelete won't contains valid path
            if (it.containsPath()) {
                it.getFilePath()?.let { newPath ->
                    _waitDelete.remove(newPath)
                }
            }
        }

        if (oldState?.containsPath() ?: false) { // process old files
            val getPathStr = oldState.getFilePath() ?: throw IllegalStateException("No file path")
            newState?.let { // stop process if new path is same
                if (it.containsPath()) it.getFilePath()?.let { newPath ->
                    if (newPath == getPathStr) return
                }
            }
            if (!getPathStr.isTempPath() && !getPathStr.isMediaPath()) {
                throw IllegalStateException("Invalid file path")
            }
            if (getPathStr.isTempPath()) {
                if (oldState == _cacheSelectedItem.value) {
                    return
                }
                _temp.remove(oldState)?.let {
                    val delMode = StorageManager.DeleteMode.FilePath(it)
                    scope.launch { storageManager.deleteTempFile(delMode) }
                }
            } else if (getPathStr.isMediaPath()) {
                _waitDelete.add(getPathStr)
            }
        }
    }


    //-----------------------
    // Process selected item
    //-----------------------
    @Throws(IllegalStateException::class, IllegalArgumentException::class)
    override fun updateSelectedData(state: TaskModify.Detail.TypeState) {
        // Check data state is valid
        val idx = _selectedItemIndex.value ?: throw IllegalStateException("No selected item")

        // Update data
        val oldTypeState = modifyState.value.detailState!![idx].typeState
        this.onSelectedDataChanged(oldTypeState, state)
        val newState = state.let { new ->
            if (new.containsPath()) new.getFilePath()?.let { path ->
                if (path.isTempPath()) _temp[state] = path
            }
            new
        }
        modifyState.update { mdState ->
            val newDetailState = mdState.detailState!!.let {
                val toMutable = it.toMutableList()
                toMutable[idx] = it[idx].copy(typeState = newState)
                toMutable
            }
            mdState.copy(detailState = newDetailState)
        }
    }

    @Throws(IllegalStateException::class)
    override fun updateSelectedType(type: TaskDetailType) {
        val idx = _selectedItemIndex.value ?: throw IllegalStateException("No selected item")
        this.onSelectedDataChanged(modifyState.value.detailState!![idx].typeState)
        modifyState.update { mdState ->
            val newDetailState = mdState.detailState!!.let {
                val toMutable = it.toMutableList()
                toMutable[idx] = it[idx].copy(typeState = type.toEmptyDetailType())
                toMutable
            }
            mdState.copy(detailState = newDetailState)
        }
    }

    override val isDetailModified: StateFlow<Boolean> = combine(
        getSelectedItem, _cacheSelectedItem
    ) { selected, cache ->
        selected?.let {
            selected.typeState != cache
        } ?: false
    }.stateIn(scope, SharingStarted.Eagerly, false)


    //-----------------------
    // Process include list
    //-----------------------
    @Throws(
        IndexOutOfBoundsException::class,
        IllegalStateException::class,
        IllegalArgumentException::class
    )
    override fun selectItem(index: Int) {
        // Expect this will throw exception if invalid
        val getSelectedItem = modifyState.value.detailState!![index]
        // Ensure no selected item
        if (_selectedItemIndex.value != null) {
            throw IllegalArgumentException("Already have selected item")
        }
        // Cache old valid item
        getSelectedItem.typeState?.let {
            if (DetailHelper.checkTypeStateValid(it)) {
                _cacheSelectedItem.value = it
            }
        }
        _selectedItemIndex.value = index
    }

    @Throws(
        IndexOutOfBoundsException::class,
        IllegalStateException::class,
        IllegalArgumentException::class
    )
    override fun unselectItem(rollback: Boolean) {
        val idx = _selectedItemIndex.value ?: throw IndexOutOfBoundsException("No selected item")
        if (rollback) {
            if (_cacheSelectedItem.value == null) {
                this.removeItem(null)
                return
            }
            if (!DetailHelper.checkTypeStateValid(_cacheSelectedItem.value!!)) {
                throw IllegalArgumentException("Invalid cached item")
            }
            this.updateSelectedData(_cacheSelectedItem.value!!)
            _cacheSelectedItem.value = null
        } else {
            val selectedItem = modifyState.value.detailState!![idx].typeState
                ?: throw IllegalStateException("No type state")
            if (!DetailHelper.checkTypeStateValid(selectedItem)) {
                throw IllegalStateException("Invalid type state")
            }
            this.updateSelectedData(DetailHelper.formatData(selectedItem))
            _cacheSelectedItem.value = null
        }
        _selectedItemIndex.value = null
    }

    @Throws(IllegalStateException::class)
    override fun moveItem(from: Int, to: Int) {
        if (_selectedItemIndex.value != null) {
            throw IllegalStateException("Cannot move due to item is selected")
        }
        modifyState.update { state ->
            val newDetailState = state.detailState!!.let { original ->
                val toMutable = original.toMutableList()
                val item = toMutable.removeAt(from)
                toMutable.add(to, item)
                toMutable
            }
            state.copy(detailState = newDetailState)
        }
    }

    @Throws(NullPointerException::class)
    override fun removeItem(index: Int?) {
        val getIndex = index ?: _selectedItemIndex.value!!
        if (index == null) { // cancel select
            _selectedItemIndex.value = null
        }
        modifyState.update { state -> // remove from list
            val newDetailState = state.detailState!!.let {
                val toMutable = it.toMutableList()
                val removed = toMutable.removeAt(getIndex)
                if (removed.typeState?.containsPath() ?: false) {
                    val tempFile = _temp.remove(removed.typeState)
                    if (tempFile?.isTempPath() ?: false) {
                        val delFile = StorageManager.DeleteMode.FilePath(tempFile)
                        scope.launch { storageManager.deleteTempFile(delFile) }
                    } else {
                        removed.typeState.getFilePath()?.let { path -> _waitDelete.add(path) }
                    }
                }
                toMutable
            }
            state.copy(detailState = newDetailState)
        }
        _cacheSelectedItem.value = null
    }

    override fun updateItemDescription(index: Int, text: String?) {
        modifyState.update { state ->
            val newDetailState = state.detailState!!.let {
                val toMutable = it.toMutableList()
                toMutable[index] = it[index].copy(itemDescription = text)
                toMutable
            }
            state.copy(detailState = newDetailState)
        }
    }

    @Throws(IllegalArgumentException::class)
    override fun addItem() {
        if (_selectedItemIndex.value != null) {
            throw IllegalArgumentException("Not possible to add when edit a detail item!")
        }
        modifyState.value.detailState?.let {
            if (DetailHelper.listLimit(it)) {
                throw IllegalArgumentException("Max number (weight) of item reached!")
            }
        }
        val defaultUIState = TaskModify.Detail.UIState(
            itemDescription = null, typeState = null
        )
        if (modifyState.value.detailState!!.isEmpty()) {
            modifyState.update {
                it.copy(detailState = listOf(defaultUIState))
            }
            _selectedItemIndex.value = 0
        } else {
            modifyState.update {
                it.copy(detailState = it.detailState!!.plus(defaultUIState))
            }
        }
    }

    //-----------------------
    // Process final action
    //-----------------------
    fun onConfirmed(): List<TaskDetail>? {
        val details = modifyState.value.detailState.let {
            if (it.isNullOrEmpty()) return null
            it
        }
        details.forEach { detail ->
            detail.typeState?.let {
                if (!DetailHelper.checkTypeStateValid(detail.typeState)) {
                    throw IllegalStateException("Invalid type state")
                }
            }
        }
        val finalDetail: MutableList<TaskDetail> = details.map {
            DetailHelper.convertToDatabaseDetail(it)
        }.map {
            it.copy(taskId = detailIn?.first()?.taskId ?: 0)
        }.toMutableList()
        if (detailIn != null) try {
            for (i in detailIn.indices) {
                val original = detailIn[i]
                finalDetail[i] = finalDetail[i].copy(id = original.id)
            }
        } catch (_: IndexOutOfBoundsException) {
            // Expected get this exception when finalDetail size is smaller than detailIn size
        }
        return finalDetail
    }

    private suspend fun onCleared() {
        _selectedItemIndex.value = null
        _cacheSelectedItem.value = null
        _waitDelete.clear()
        for ((type, path) in _temp) { // clear temp files
            if (!type.containsPath() && !path.isTempPath()) {
                continue
            }
            val del = StorageManager.DeleteMode.FilePath(path)
            try {
                storageManager.deleteTempFile(del)
            } catch (_: FileNotFoundException) {
                // Expected get this exception
                // when confirm happened first (temp already moved to data storage)
            }
        }
        _temp.clear()
        modifyState.update { it.copy(detailState = null) }
    }

    fun onCanceled(after: () -> Unit = {}) {
        scope.launch(Dispatchers.IO) {
            this@DetailHandler.onCleared()
            after()
        }
    }

    suspend fun reset() {
        _waitDelete.forEach {
            try {
                val delMode = StorageManager.DeleteMode.FilePath(it)
                storageManager.deleteStorageFile(delMode)
                println("Deleted file: $it, but it is unexpected happen in feature module")
            } catch (_: IOException) {
                // Expected get this exception when file is not exist
            }
        }
        this.onCleared()
    }
}
