package com.sqz.checklist.ui.main.task.handler

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.sqz.checklist.MainActivity
import com.sqz.checklist.R
import com.sqz.checklist.database.DatabaseRepository
import com.sqz.checklist.database.ReminderModeType
import com.sqz.checklist.database.TaskReminder
import com.sqz.checklist.notification.NotifyManager
import com.sqz.checklist.notification.PermissionState
import com.sqz.checklist.ui.main.task.layout.function.ReminderActionType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.concurrent.TimeUnit
import kotlin.random.Random

class ReminderHandler(
    private val coroutineScope: CoroutineScope,
    private val requestUpdate: MutableStateFlow<Boolean>
) {
    fun database(): DatabaseRepository = DatabaseRepository(
        MainActivity.taskDatabase
    )
    private val _notificationManager = MutableStateFlow(NotifyManager())
    val notifyManager = _notificationManager.value

    private var _notifyId: Int? = null
    private var _reminderActionType = MutableStateFlow(ReminderActionType.None)
    private var _taskId: Long? = null

    val reminderActionType: StateFlow<ReminderActionType> = _reminderActionType.asStateFlow()

    fun requestReminder(taskId: Long) = coroutineScope.launch {
        _taskId = taskId
        database().getReminderData(taskId).let {
            if (it != null && !it.isReminded) {
                _notifyId = it.id
                _reminderActionType.update { ReminderActionType.Cancel }
            } else {
                _reminderActionType.update { ReminderActionType.Set }
            }
        }
    }

    fun resetRequest() {
        _taskId = null
        _notifyId = null
        _reminderActionType.update { ReminderActionType.None }
    }

    fun isAlarmPermission(): Boolean = notifyManager.getAlarmPermission()

    fun notificationInitState(context: Context, init: Boolean = false): PermissionState {
        val requestPermission = notifyManager.requestPermission(context)
        fun makeToast() = Toast.makeText(
            context, context.getString(R.string.permission_lost_toast), Toast.LENGTH_LONG
        ).show()
        if (init && requestPermission != PermissionState.Both) coroutineScope.launch {
            database().getIsRemindedNum(false)?.collect {
                if (it >= 1) { // If no permission to send notification for reminder
                    val reCheck = notifyManager.requestPermission(context)
                    if (reCheck == PermissionState.Null || reCheck == PermissionState.Alarm) makeToast()
                    else if (reCheck != PermissionState.Both &&
                        database().getModeNumWithNoReminded(ReminderModeType.AlarmManager) >= 1
                    ) makeToast()
                }
            }
        }
        return requestPermission
    }

    suspend fun setReminder(delayDuration: Long, timeUnit: TimeUnit, context: Context) {
        val task = database().getTask(_taskId!!)!!
        this.setReminder(delayDuration, timeUnit, task.id, task.description, context)
        resetRequest()
    }

    /** Send a delayed notification to user **/
    suspend fun setReminder(
        delayDuration: Long, timeUnit: TimeUnit, id: Long, description: String, context: Context
    ) {
        _notifyId = Random.nextInt(Int.MIN_VALUE, Int.MAX_VALUE) // Make a random notify id
        suspend fun checkRandomId() { // If notify id is already exist
            for (data in database().getReminderData()) {
                if (data.id == _notifyId || _notifyId == 0) {
                    _notifyId = Random.nextInt(Int.MIN_VALUE, Int.MAX_VALUE)
                    checkRandomId()
                }
            }
        }
        checkRandomId()
        val delayTime = if (!isAlarmPermission()) delayDuration else {
            System.currentTimeMillis() + delayDuration
        }
        val notification = notifyManager.createNotification(
            channelId = context.getString(R.string.tasks),
            channelName = context.getString(R.string.task_reminder),
            channelDescription = context.getString(R.string.description),
            description = description, notifyId = _notifyId!!,
            delayDuration = delayTime, timeUnit = timeUnit,
            context = context
        )
        try { // remove old data first
            cancelReminder(id, database().getReminderData(id)!!.id, context)
            database().deleteReminderData(id)
        } catch (e: Exception) {
            if (e is NoSuchFieldException || e is NullPointerException) {
                Log.d("SetReminder", "New reminder is setting")
            } else throw e
        }
        coroutineScope.launch {
            val notify = notification.also { Log.i("Notification", "Reminder is setting") }
            val now = Calendar.getInstance()
            val remindTime =
                if (isAlarmPermission()) delayTime else now.timeInMillis + delayDuration
            val mode = if (isAlarmPermission()) ReminderModeType.AlarmManager else {
                ReminderModeType.Worker
            }
            val taskReminder = TaskReminder(
                _notifyId!!, description, remindTime, mode, extraData = notify
            )
            database().insertReminderData(id, taskReminder)
            requestUpdate.update { true }
            _notifyId = null
        }
    }

    /** Cancel reminder by id (suspend) **/
    private suspend fun cancelReminderAction(id: Long, reminder: Int?, context: Context) {
        try { // Cancel sent notification
            if (reminder != 0 && reminder != null) {
                val data = MainActivity.taskDatabase.taskReminderDao().getAll(reminder)
                if (data != null) when (data.mode) {
                    ReminderModeType.AlarmManager -> notifyManager.cancelNotification(
                        data.id.toString(), context, reminder
                    )

                    ReminderModeType.Worker -> notifyManager.cancelNotification(
                        data.extraData!!, context, reminder
                    )
                }
            } else {
                Log.d("CancelFailed", "No reminder data need to cancel")
            }
            // Delete reminder info
            database().deleteReminderData(id)
        } catch (e: NoSuchFieldException) {
            Log.d("DeleteReminderData", "Noting need to delete")
        } catch (e: IllegalStateException) {
            Log.w("TaskLayoutViewModel", "$e")
        } catch (e: Exception) {
            Log.e("ERROR", "$e")
        }
    }

    /** Cancel reminder by id **/
    fun cancelReminder(id: Long = _taskId!!, reminder: Int? = _notifyId, context: Context) {
        coroutineScope.launch {
            cancelReminderAction(id, reminder, context)
            requestUpdate.update { true }
        }
    }

    /** Cancel history reminder **/
    fun cancelHistoryReminder(context: Context) = coroutineScope.launch(Dispatchers.IO) {
        val allIsHistoryIdList = MainActivity.taskDatabase.taskDao().getAllOrderByIsHistoryId()
        for (data in allIsHistoryIdList) {
            if (data.reminder != 0 && data.reminder != null) cancelReminderAction(
                data.id, data.reminder, context
            )
        }
    }

    suspend fun getReminderData(taskId: Int? = _notifyId): TaskReminder? {
        return if (taskId != null) database().getReminderData(taskId) else null
    }

    fun getIsRemindedNum(): Flow<Int>? = database().getIsRemindedNum(true)
}
