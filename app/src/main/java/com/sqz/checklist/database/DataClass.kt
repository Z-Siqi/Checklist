package com.sqz.checklist.database

data class TaskData(
    val description: String?,
    val doingState: String? = null,
    val isPin: Boolean?,
)

data class TaskDetailData(
    val detailId: Long? = null,
    val type: TaskDetailType,
    val description: String?,
    val dataString: String?,
    val dataByte: Any?,
)
