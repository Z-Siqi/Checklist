package com.sqz.checklist.ui.main.task.history

import com.sqz.checklist.MainActivity

suspend fun arrangeHistoryId() {
    val allIsHistoryIdList = MainActivity.taskDatabase.taskDao().getAllIsHistoryId()
    val arrangeIdList = allIsHistoryIdList.mapIndexed { index, data ->
        data.copy(isHistoryId = index + 1)
    }

    for (data in arrangeIdList) {
        MainActivity.taskDatabase.taskDao().setIsHistoryId(data.isHistoryId, data.id)
    }
}
