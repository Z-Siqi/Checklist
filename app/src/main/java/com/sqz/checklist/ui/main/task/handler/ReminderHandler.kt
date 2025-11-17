package com.sqz.checklist.ui.main.task.handler

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.viewModelScope
import com.sqz.checklist.MainActivity
import com.sqz.checklist.R
import com.sqz.checklist.database.DatabaseRepository
import com.sqz.checklist.database.ReminderModeType
import com.sqz.checklist.database.ReminderViewData
import com.sqz.checklist.database.TaskData
import com.sqz.checklist.database.TaskReminder
import com.sqz.checklist.notification.NotificationChannelData
import com.sqz.checklist.notification.NotificationCreator
import com.sqz.checklist.notification.NotificationData
import com.sqz.checklist.notification.NotificationReceiver
import com.sqz.checklist.notification.NotifyManager
import com.sqz.checklist.notification.PermissionState
import com.sqz.checklist.ui.main.task.TaskLayoutViewModel
import com.sqz.checklist.ui.main.task.layout.function.ReminderActionType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.concurrent.TimeUnit
import kotlin.random.Random

class ReminderHandler private constructor(
    private val coroutineScope: CoroutineScope,
    private val requestUpdate: MutableStateFlow<Boolean>,
    private val initState: MutableStateFlow<Boolean>,
) {
    companion object {
        fun instance(
            viewModel: TaskLayoutViewModel, requestUpdate: MutableStateFlow<Boolean>,
            initState: MutableStateFlow<Boolean>
        ): ReminderHandler = ReminderHandler(
            coroutineScope = viewModel.viewModelScope, requestUpdate = requestUpdate,
            initState = initState
        )
    }

    /** Get [com.sqz.checklist.database.TaskDatabase] instance **/
    private fun database(): DatabaseRepository = DatabaseRepository(
        MainActivity.taskDatabase
    )

    private val _notificationManager = MutableStateFlow(NotifyManager())
    val notifyManager = _notificationManager.value

    private var _notifyId: Int? = null
    private var _reminderActionType = MutableStateFlow(ReminderActionType.None)
    private var _taskId: Long? = null
    private var _allowDismissRequest = MutableStateFlow(true)

    val allowDismissRequest: StateFlow<Boolean> = _allowDismissRequest.asStateFlow()

    val reminderActionType: StateFlow<ReminderActionType> = _reminderActionType.asStateFlow()

    /**
     * Requests the reminder setup UI for a specific task.
     *
     * This function initiates the process for setting or canceling a reminder. It checks
     * if a reminder already exists for the given task and updates the UI state accordingly
     * to show either a "Set" or "Cancel" action.
     *
     * @param taskId The ID of the task for which the reminder is being requested.
     * @param dismiss A flag indicating whether the reminder UI should be dismissible.
     *                Defaults to `true`.
     */
    fun requestReminder(taskId: Long, dismiss: Boolean = true) = coroutineScope.launch {
        _taskId = taskId
        _allowDismissRequest.update { dismiss }
        database().getReminderData(taskId).let {
            if (it != null && !it.isReminded) {
                _notifyId = it.id
                _reminderActionType.update { ReminderActionType.Cancel }
            } else {
                _reminderActionType.update { ReminderActionType.Set }
            }
        }
    }

    /** Resets the reminder request state. */
    fun resetRequest() {
        _taskId = null
        _notifyId = null
        _reminderActionType.update { ReminderActionType.None }
    }


    /** Checks if the app has permission to set exact alarms. */
    fun isAlarmPermission(): Boolean = notifyManager.getAlarmPermission()

    /**
     * Checks and handles the initial state of notification permissions.
     *
     * This function checks for necessary notification permissions. If `init` is true,
     * it can also display a toast message to the user if permissions are missing while
     * there are active, non-reminded tasks. This is useful for alerting the user at
     * app startup that existing reminders may not fire.
     *
     * @param context The application context.
     * @param init If true, the function will perform checks related to app initialization,
     *             such as verifying permissions for existing reminders.
     * @return The current [PermissionState] (Both, Alarm, Notification, or Null).
     */
    fun notificationInitState(context: Context, init: Boolean = false): PermissionState {
        val requestPermission = if (!initState.value) PermissionState.Null else {
            notifyManager.requestPermission(context)
        }

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

    /**
     * Sets a reminder for the currently requested task.
     *
     * @param delayDuration The duration from now until the reminder.
     * @param timeUnit The unit of time for `delayDuration`.
     * @param context The application context.
     * @see setReminder
     */
    suspend fun setReminder(delayDuration: Long, timeUnit: TimeUnit, context: Context) {
        this.setReminder(delayDuration, timeUnit, _taskId!!, context)
        this.resetRequest()
    }

    /**
     * Sets a delayed notification for a specific task.
     *
     * This function creates and schedules a reminder for a given task. It handles both
     * `AlarmManager` (for precise timing when permission is granted) and `WorkManager`
     * (as a fallback). It also ensures that any existing reminder for the same task
     * is canceled before setting a new one.
     *
     * @param delayDuration The duration from now until the notification should be triggered.
     * @param timeUnit The unit of time for `delayDuration`.
     * @param id The ID of the task to set the reminder for.
     * @param context The application context.
     */
    suspend fun setReminder(delayDuration: Long, timeUnit: TimeUnit, id: Long, context: Context) {
        _notifyId = this.setAndCheckRandomId()
        val isAlarmPermission = isAlarmPermission()
        val delayTime = if (!isAlarmPermission) delayDuration else {
            System.currentTimeMillis() + delayDuration
        }
        val notification = notifyManager.createNotification(
            notifyId = _notifyId!!,
            delayDuration = delayTime,
            timeUnit = timeUnit,
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
        val notify = notification.also {
            Log.i("Notification", "Reminder is setting, isAlarmPermission: $isAlarmPermission")
        }
        val now = Calendar.getInstance()
        val remindTime = if (isAlarmPermission) delayTime else now.timeInMillis + delayDuration
        val mode = if (isAlarmPermission) ReminderModeType.AlarmManager else {
            ReminderModeType.Worker
        }
        val taskReminder = TaskReminder(
            id = _notifyId!!,
            taskId = id,
            reminderTime = remindTime,
            mode = mode,
            extraData = notify
        )
        database().insertReminderData(taskReminder)
        if (mode == ReminderModeType.Worker) {
            // if worker will sent notification immediately, this delay is needed
            // for update list correctly
            delay(100)
        }
        requestUpdate.update { true }
        _notifyId = null
    }

    /** Generate random notify id **/
    private suspend fun setAndCheckRandomId(defId: Int? = null): Int {
        if (defId == null) return this.setAndCheckRandomId( // Make a random notify id
            defId = Random.nextInt(Int.MIN_VALUE, Int.MAX_VALUE - 1)
        )
        for (data in database().getReminderData()) {
            if (data.reminder.id == defId || defId == 0) {
                return this.setAndCheckRandomId( // Make another random notify id if already exist
                    defId = Random.nextInt(Int.MIN_VALUE, Int.MAX_VALUE - 1)
                )
            }
        }
        return defId
    }

    /**
     * Updates an existing notification with new task data.
     *
     * @param notifyId The ID of the notification to update.
     * @param task The updated task data to display in the notification.
     * @param context The application context.
     */
    suspend fun updateNotification(notifyId: Int, task: TaskData, context: Context) {
        val reminder = database().getReminderData(reminderId = notifyId)
        NotifyManager.isNotificationExist(notifyId, context) { channelId, postTime ->
            NotificationCreator(context).pushedNotificationCreator(
                channel = NotificationChannelData(
                    id = channelId,
                    name = context.getString(R.string.task_reminder),
                    description = context.getString(R.string.description),
                ),
                notifyData = NotificationData(
                    id = notifyId,
                    title = task.description!!,
                    text = NotificationReceiver.notificationTextFormater(
                        text = reminder?.reminder?.extraText,
                        remindTime = postTime,
                        ctx = context,
                    )
                )
            )
        }.runCatching {
            throw NullPointerException("Notification is not exist")
        }
    }

    /**
     * Cancels a scheduled reminder for a specific task.
     *
     * This function handles the logic for canceling a reminder, whether it was scheduled
     * using `AlarmManager` or `WorkManager`. It retrieves the reminder data, identifies
     * the scheduling mode, and then calls the appropriate cancellation method on the
     * `NotifyManager`. After canceling the system-level alarm/worker, it deletes the
     * reminder information from the local database.
     *
     * @param id The ID of the task whose reminder should be canceled.
     * @param reminder The ID of the reminder (notification ID).
     * @param context The application context.
     */
    private suspend fun cancelReminderAction(id: Long, reminder: Int?, context: Context) {
        try { // Cancel sent notification
            if (reminder != 0 && reminder != null) {
                val data = MainActivity.taskDatabase.taskReminderDao().getAll(reminder)
                if (data != null) when (data.reminder.mode) {
                    ReminderModeType.AlarmManager -> notifyManager.cancelNotification(
                        data.reminder.id.toString(), context, reminder
                    )

                    ReminderModeType.Worker -> notifyManager.cancelNotification(
                        data.reminder.extraData!!, context, reminder
                    )
                }
            } else {
                Log.d("CancelFailed", "No reminder data need to cancel")
            }
            // Delete reminder info
            database().deleteReminderData(id)
        } catch (_: NoSuchFieldException) {
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
            this@ReminderHandler.cancelReminderAction(id, reminder, context)
            requestUpdate.update { true }
        }
    }

    /** Cancel history reminder **/
    fun cancelHistoryReminder(context: Context) = coroutineScope.launch(Dispatchers.IO) {
        val allIsHistoryIdList = MainActivity.taskDatabase.taskDao().getAllOrderByIsHistoryId()
        for (data in allIsHistoryIdList) {
            try {
                val reminder = database().getReminderData(taskId = data.id)!!
                cancelReminderAction(data.id, reminder.id, context)
            } catch (_: NullPointerException) {
            }
        }
    }

    /** Retrieves reminder data by notification ID. */
    suspend fun getReminderData(id: Int? = _notifyId): ReminderViewData? {
        return if (id != null) database().getReminderData(id) else null
    }


    /** Checks if a specific alarm notification is active. */
    fun checkAlarmNotification(notifyId: Int, context: Context): Boolean? {
        if (notifyManager.requestPermission(context) == PermissionState.Alarm) {
            return NotificationCreator(context).getAlarmNotificationState(notifyId)
        }
        return null
    }

    /** Force restores all scheduled notifications which cause by unexpected reason */
    fun restoreNotification(context: Context) {
        Log.e("ReminderHandler", "trying to call restoreNotification")
        coroutineScope.launch(Dispatchers.Main) {
            com.sqz.checklist.database.restoreNotification(MainActivity.taskDatabase, context)
        }
    }

    /** Gets the number of tasks that have been reminded. */
    fun getIsRemindedNum(): Flow<Int>? {
        return if (!initState.value) flowOf(0) else {
            database().getIsRemindedNum(true)
        }
    }

    /** Requests an update of the task list. */
    fun requestUpdateList() = requestUpdate.update { true }
}
