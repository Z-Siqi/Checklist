package sqz.checklist.data.database.repository.task

import sqz.checklist.data.database.Task
import sqz.checklist.data.database.TaskDetail
import sqz.checklist.data.database.TaskDetailType
import sqz.checklist.data.database.dao.TaskDao
import sqz.checklist.data.database.pathStringConverter
import sqz.checklist.data.storage.AppDirType
import sqz.checklist.data.storage.StorageHelper.isCachePath
import sqz.checklist.data.storage.StorageHelper.platformDataPathToSafetyPath
import sqz.checklist.data.storage.appInternalDirPath
import sqz.checklist.data.storage.manager.StorageManager

internal class TaskRepositoryImpl(
    private val taskDao: TaskDao,
    private val storageManager: StorageManager
) : TaskRepository {

    private fun List<TaskDetail>.formatTaskDetailPathToPlatform(): List<TaskDetail> {
        return this.map {
            val pathStr = it.type.pathStringConverter(it.dataByte) ?: return@map it
            val toStr = pathStr.let { let ->
                //TODO: this for Android logic need remove when next update db version
                if (let.startsWith("file:///") || let.startsWith("content://")) {
                    return@let let.replaceBefore("media", "")
                }
                return@let let
            }
            val insertPlatformPath = "${appInternalDirPath(AppDirType.Data)}/$toStr"
            return@map it.copy(dataByte = insertPlatformPath.encodeToByteArray())
        }
    }

    override suspend fun getFullTask(id: Long): Pair<Task, List<TaskDetail>?> {
        val task = taskDao.getTask(id)
        val taskDetail = taskDao.getTaskDetailList(id)
        if (task == null) {
            throw NullPointerException("No task match the id")
        }
        return if (taskDetail.isEmpty()) {
            Pair(task, null)
        } else {
            val formatToPlatform = taskDetail.formatTaskDetailPathToPlatform()
            Pair(task, formatToPlatform)
        }
    }

    /** @return Error message or `null` means no error **/
    private suspend fun checkModifyValid(newTask: Task, newDetail: List<TaskDetail>?): String? {
        val dbDetail = taskDao.getTaskDetailList(newTask.id)
        if (newTask.id != 0L) {
            val dbTask = taskDao.getTask(newTask.id)
                ?: return "Not a new task, no task match the id!"
            if (dbTask.createDate != newTask.createDate) {
                return "Create date should not be able to change!"
            }
            if (!newDetail.isNullOrEmpty() && newDetail.any { it.taskId != newTask.id }) {
                if (dbDetail.isEmpty() && newDetail.any { it.taskId != 0L }) {
                    return "TaskDetail.taskId should be 0 if no existed!"
                }
                if (dbDetail.isNotEmpty()) {
                    return "TaskDetail.taskId not match Task.id!"
                }
            }
        } else if (!newDetail.isNullOrEmpty() && newDetail.any { it.id != 0L || it.taskId != 0L }) {
            return "Detail's Task.id or TaskDetail.id is illegal!"
        }
        if (!newDetail.isNullOrEmpty() && dbDetail.isNotEmpty()) newDetail.forEach { new ->
            if (new.id != 0L && dbDetail.find { it.id == new.id } == null) {
                return "TaskDetail.id is illegal!"
            }
            when (new.type) {
                TaskDetailType.Picture -> {}
                TaskDetailType.Video -> {}
                TaskDetailType.Audio -> {}
                else -> return@forEach
            }
            new.dataByte.decodeToString().let {
                if (it.startsWith("file:///") || it.startsWith("content://")) {
                    return "TaskDetail.dataByte is URI!"
                }
                if (it.isCachePath()) {
                    return "TaskDetail.dataByte is cache path!"
                }
            }
        }
        return null
    }

    /** Update task info **/
    private suspend fun updateTask(originalTask: Task, newTask: Task) {
        val updateTaskData = originalTask.copy(
            description = newTask.description,
            doingState = newTask.doingState,
            isPin = newTask.isPin,
            isHistoryId = newTask.isHistoryId,
        )
        taskDao.updateTask(updateTaskData)
    }

    /** Usage: `taskId.updateTaskDetail(originalDetail, newDetail)` **/
    private suspend fun Long.updateTaskDetail(
        originalDetail: List<TaskDetail>, newDetail: List<TaskDetail>?
    ) {
        when {
            originalDetail.isEmpty() && newDetail.isNullOrEmpty() -> return // no detail
            originalDetail.isNotEmpty() && !newDetail.isNullOrEmpty() -> {  // details are same
                if (originalDetail == newDetail) return
            }
        }
        // all detail removed
        if (originalDetail.isNotEmpty() && newDetail.isNullOrEmpty()) {
            for (original in originalDetail) {
                original.deleteTaskDetailStorageFile(storageManager)
            }
            taskDao.deleteTaskDetailByTaskId(this)
            return
        }
        // only new detail need to add
        if (originalDetail.isEmpty() && !newDetail.isNullOrEmpty()) {
            newDetail.forEach { new ->
                val toInsert = new.moveTempToInternalStorage(storageManager)?.let {
                    new.copy(dataByte = it.encodeToByteArray())
                } ?: new
                taskDao.insertTaskDetail(toInsert.copy(taskId = this))
            }
            return
        }
        // modify detail
        suspend fun TaskDetail.toInsertTaskDetail(): TaskDetail {
            return this.moveTempToInternalStorage(storageManager)?.let {
                this.copy(dataByte = it.encodeToByteArray())
            } ?: this.type.pathStringConverter(this.dataByte)?.let {
                this.copy(dataByte = it.platformDataPathToSafetyPath().encodeToByteArray())
            } ?: this
        }
        if (originalDetail.size >= newDetail!!.size) {
            for (i in originalDetail.indices) { // original size is same or larger
                val original = originalDetail[i]
                try {
                    val new = newDetail[i]
                    if (new == original) continue
                    original.deleteTaskDetailStorageFile(storageManager, newDetail)
                    val toInsert = new.toInsertTaskDetail()
                    val update = original.copy(
                        type = toInsert.type,
                        description = toInsert.description,
                        dataString = toInsert.dataString,
                        dataByte = toInsert.dataByte,
                    )
                    taskDao.updateTaskDetail(update)
                } catch (_: IndexOutOfBoundsException) {
                    original.deleteTaskDetailStorageFile(storageManager, newDetail)
                    taskDao.deleteTaskDetail(original)
                }
            }
            return
        } else {
            for (i in newDetail.indices) { // original size is smaller
                val new = newDetail[i]
                try {
                    val original = originalDetail[i]
                    if (new == original) continue
                    original.deleteTaskDetailStorageFile(storageManager, newDetail)
                    val toInsert = new.toInsertTaskDetail()
                    val update = original.copy(
                        type = toInsert.type,
                        description = toInsert.description,
                        dataString = toInsert.dataString,
                        dataByte = toInsert.dataByte,
                    )
                    taskDao.updateTaskDetail(update)
                } catch (_: IndexOutOfBoundsException) {
                    val toInsert = new.toInsertTaskDetail()
                    taskDao.insertTaskDetail(toInsert.copy(taskId = this))
                }
            }
            return
        }
    }

    override suspend fun modifyTask(
        task: Task,
        detail: List<TaskDetail>?
    ): Long {
        this.checkModifyValid(task, detail)?.let { err ->
            throw IllegalArgumentException(err)
        }
        if (task.id == 0L) { // Insert new
            // insert task
            val newTaskId = taskDao.insertTask(task)
            // insert detail
            if (!detail.isNullOrEmpty()) detail.forEach { new ->
                val toInsert = new.moveTempToInternalStorage(storageManager)?.let {
                    new.copy(dataByte = it.encodeToByteArray())
                } ?: new
                taskDao.insertTaskDetail(toInsert.copy(taskId = newTaskId))
            }
            return newTaskId
        } else { // Update existed
            val dbTask = taskDao.getTask(task.id)
            val dbDetail = taskDao.getTaskDetailList(task.id).formatTaskDetailPathToPlatform()
            // update task
            this.updateTask(dbTask!!, task)
            // update detail
            task.id.updateTaskDetail(dbDetail, detail)
            return task.id
        }
    }
}
