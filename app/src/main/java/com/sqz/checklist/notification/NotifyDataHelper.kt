package com.sqz.checklist.notification

data class NotificationChannelData(
    val id: String,
    val name: String,
    val description: String,
    val importance: Int? = null,

    //not implemented below
    /*val enableVibrate: Boolean, val lockscreenVisibility: Int*/
)

//not implemented
@Suppress("unused")
data class NotificationChannelGroup(
    val id: String,
    val name: String,
    val description: String
)

data class NotificationData(
    val id: Int,
    val title: String,
    val text: String? = null,
)
