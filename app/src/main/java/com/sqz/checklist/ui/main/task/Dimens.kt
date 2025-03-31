package com.sqz.checklist.ui.main.task

import android.content.Context
import com.sqz.checklist.database.DatabaseRepository

/** Override for preview work **/
class TaskLayoutViewModelPreview : TaskLayoutViewModel() {
    override fun database(): DatabaseRepository {
        return DatabaseRepository(null)
    }
}

/** Default task card height in dp **/
const val CardHeight = 120

/** Task card height in dp with system font size support **/
fun cardHeight(context: Context): Int {
    val heightCalculate = CardHeight * context.resources.configuration.fontScale
    return when {
        context.resources.configuration.fontScale == 1.0f -> CardHeight
        heightCalculate < 120 -> CardHeight
        heightCalculate > 180 -> 180
        else -> heightCalculate.toInt()
    }
}
