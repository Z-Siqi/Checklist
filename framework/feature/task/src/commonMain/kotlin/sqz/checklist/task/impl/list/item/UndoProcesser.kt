package sqz.checklist.task.impl.list.item

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class UndoProcesser(
    scope: CoroutineScope,
    private val enableUndo: StateFlow<Boolean>,
    private val onReset: () -> Unit = {},
) {
    private var loopScope = CoroutineScope(SupervisorJob())

    private val _taskIdUndoWaited = MutableStateFlow<Long?>(null)

    val undoState: StateFlow<Boolean> = _taskIdUndoWaited.map { it != null }.stateIn(
        scope, SharingStarted.Eagerly, false
    )

    private var _rememberState: Any? = null
    private var _rememberInitState: Any? = null
    private var _currentLoop: Int? = null

    private fun resetUndoProcessorWithResetCallback() {
        this.resetUndoProcessor()
        onReset()
    }

    private fun startUndoLoop() {
        if (_taskIdUndoWaited.value == null) {
            throw IllegalStateException("Undo task id is null!")
        }
        if (_rememberState != null || _currentLoop != null) {
            throw IllegalStateException("Undo already start!")
        }
        loopScope.launch(Dispatchers.IO) {
            delay(1000)
            if (_rememberState != _rememberInitState) {
                this@UndoProcesser.resetUndoProcessorWithResetCallback()
                return@launch
            }
            _currentLoop = 0
            delay(500)
            while (_currentLoop!! < 7) {
                if (_rememberState != _rememberInitState) {
                    this@UndoProcesser.resetUndoProcessorWithResetCallback()
                    _currentLoop = null
                    return@launch
                }
                delay(500)
                _currentLoop = _currentLoop!! + 1
            }
            _currentLoop = null
            this@UndoProcesser.resetUndoProcessorWithResetCallback()
        }
    }

    fun setUndoBreakFactor(any: Any) {
        if (!enableUndo.value) {
            return
        }
        if (_rememberInitState != null) {
            _rememberState = any
            return
        }
        _rememberInitState = any
    }

    fun requestUndoState(taskId: Long) {
        if (!enableUndo.value) {
            this.onReset()
            return
        }
        if (_taskIdUndoWaited.value != null) {
            this.resetUndoProcessor()
        }
        _taskIdUndoWaited.update { taskId }
        this.startUndoLoop()
    }

    fun getUndoTaskId(): Long {
        _taskIdUndoWaited.value.let { taskId ->
            if (taskId == null) {
                throw NullPointerException("Undo task id is null!")
            }
            return taskId
        }
    }

    fun resetUndoProcessor() {
        this.loopScope.cancel()
        this.loopScope = CoroutineScope(SupervisorJob())
        _rememberInitState = null
        _rememberState = null
        _currentLoop = null
        _taskIdUndoWaited.value = null
    }
}
